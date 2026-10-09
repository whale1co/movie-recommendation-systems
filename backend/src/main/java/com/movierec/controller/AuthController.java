package com.movierec.controller;

import com.movierec.common.ApiResponse;
import com.movierec.dto.request.LoginRequest;
import com.movierec.dto.request.RegisterRequest;
import com.movierec.dto.response.AuthTokenResponse;
import com.movierec.dto.response.RegisterResponse;
import com.movierec.entity.User;
import com.movierec.service.UserService;
import com.movierec.service.AuthenticationService;
import com.movierec.service.AuthSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final UserService userService;
    private final AuthenticationService authenticationService;
    private final AuthSessionService sessionService;
    private final boolean refreshCookieSecure;
    private final long refreshExpirationSeconds;

    public AuthController(UserService userService, AuthenticationService authenticationService,
                          AuthSessionService sessionService,
                          @Value("${jwt.refresh-cookie-secure:false}") boolean refreshCookieSecure,
                          @Value("${jwt.refresh-expiration-seconds:604800}") long refreshExpirationSeconds) {
        this.userService = userService;
        this.authenticationService = authenticationService;
        this.sessionService = sessionService;
        this.refreshCookieSecure = refreshCookieSecure;
        this.refreshExpirationSeconds = refreshExpirationSeconds;
    }

    @Operation(summary = "注册用户")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request.username().trim(), request.password(), request.preferences());
        ApiResponse<RegisterResponse> response =
                ApiResponse.created("注册成功", new RegisterResponse(user.getId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> login(@Valid @RequestBody LoginRequest request,
                                                                 HttpServletRequest httpRequest) {
        AuthSessionService.AuthTokens tokens = authenticationService.login(
                request.username().trim(), request.password(), httpRequest.getRemoteAddr());
        return tokenResponse("登录成功", tokens, isAdminClient(httpRequest));
    }

    @Operation(summary = "刷新访问令牌")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> refresh(HttpServletRequest httpRequest) {
        boolean adminClient = isAdminClient(httpRequest);
        String cookieName = refreshCookieName(adminClient);
        String refreshToken = readCookie(httpRequest, cookieName);
        return tokenResponse("刷新成功", authenticationService.refresh(refreshToken), adminClient);
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest httpRequest, Authentication authentication) {
        boolean adminClient = isAdminClient(httpRequest);
        String refreshToken = readCookie(httpRequest, refreshCookieName(adminClient));
        if (authentication != null && authentication.getPrincipal() instanceof User user
                && authentication.getDetails() instanceof String sessionId) {
            sessionService.revokeSession(sessionId, user.getId(), "user_logout");
        } else {
            sessionService.revokeByRefreshToken(refreshToken, "user_logout");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie(adminClient).toString())
                .body(ApiResponse.success("退出成功", null));
    }

    private ResponseEntity<ApiResponse<AuthTokenResponse>> tokenResponse(
            String message, AuthSessionService.AuthTokens tokens, boolean adminClient) {
        ResponseCookie cookie = ResponseCookie.from(refreshCookieName(adminClient), tokens.refreshToken())
                .httpOnly(true).secure(refreshCookieSecure).sameSite("Strict")
                .path("/api/v1/auth").maxAge(Duration.ofSeconds(refreshExpirationSeconds)).build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(message,
                        new AuthTokenResponse(tokens.accessToken(), tokens.accessExpiresIn())));
    }

    private ResponseCookie clearRefreshCookie(boolean adminClient) {
        return ResponseCookie.from(refreshCookieName(adminClient), "").httpOnly(true).secure(refreshCookieSecure)
                .sameSite("Strict").path("/api/v1/auth").maxAge(Duration.ZERO).build();
    }

    private boolean isAdminClient(HttpServletRequest request) {
        return "admin".equalsIgnoreCase(request.getHeader("X-Auth-Client"));
    }

    private String refreshCookieName(boolean adminClient) {
        return adminClient ? "admin_refresh_token" : "refresh_token";
    }

    private String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }
}

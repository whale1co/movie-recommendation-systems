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
import java.time.Duration;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.CookieValue;
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
        return tokenResponse("登录成功", tokens);
    }

    @Operation(summary = "刷新访问令牌")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshToken) {
        return tokenResponse("刷新成功", authenticationService.refresh(refreshToken));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof User user
                && authentication.getDetails() instanceof String sessionId) {
            sessionService.revokeSession(sessionId, user.getId(), "user_logout");
        } else {
            sessionService.revokeByRefreshToken(refreshToken, "user_logout");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                .body(ApiResponse.success("退出成功", null));
    }

    private ResponseEntity<ApiResponse<AuthTokenResponse>> tokenResponse(
            String message, AuthSessionService.AuthTokens tokens) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", tokens.refreshToken())
                .httpOnly(true).secure(refreshCookieSecure).sameSite("Strict")
                .path("/api/v1/auth").maxAge(Duration.ofSeconds(refreshExpirationSeconds)).build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(message,
                        new AuthTokenResponse(tokens.accessToken(), tokens.accessExpiresIn())));
    }

    private ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from("refresh_token", "").httpOnly(true).secure(refreshCookieSecure)
                .sameSite("Strict").path("/api/v1/auth").maxAge(Duration.ZERO).build();
    }
}

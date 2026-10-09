package com.movierec.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.movierec.common.RequestIdFilter;
import com.movierec.entity.User;
import com.movierec.exception.GlobalExceptionHandler;
import com.movierec.exception.UnauthorizedException;
import com.movierec.service.UserService;
import com.movierec.service.AuthenticationService;
import com.movierec.service.AuthSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    @Mock
    private UserService userService;
    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private AuthSessionService sessionService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AuthController(userService, authenticationService, sessionService, false, 604800))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new RequestIdFilter())
                .build();
    }

    @Test
    void registerReturnsCreatedEnvelopeAndRequestId() throws Exception {
        User user = new User();
        user.setId(7L);
        when(userService.register(any(), any(), any())).thenReturn(user);

        mockMvc.perform(post("/api/v1/auth/register")
                        .header("X-Request-ID", "test-request-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterBody("movie_fan", "movie123", "剧情"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Request-ID", "test-request-id"))
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.userId").value(7))
                .andExpect(jsonPath("$.requestId").value("test-request-id"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void registerRejectsWeakPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterBody("movie_fan", "12345678", "剧情"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.data.password").exists());
    }

    @Test
    void loginFailureUsesHttpUnauthorized() throws Exception {
        when(authenticationService.login(anyString(), anyString(), anyString()))
                .thenThrow(new UnauthorizedException("用户名或密码错误"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginBody("nobody", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void loginReturnsAccessTokenAndHttpOnlyRefreshCookie() throws Exception {
        when(authenticationService.login(anyString(), anyString(), anyString()))
                .thenReturn(new AuthSessionService.AuthTokens("access-token", "refresh-token", 900));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginBody("alice", "Password1"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("refresh_token=refresh-token"),
                        org.hamcrest.Matchers.containsString("HttpOnly"),
                        org.hamcrest.Matchers.containsString("SameSite=Strict"))))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.expiresIn").value(900))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist());
    }

    @Test
    void adminLoginUsesAnIndependentRefreshCookie() throws Exception {
        when(authenticationService.login(anyString(), anyString(), anyString()))
                .thenReturn(new AuthSessionService.AuthTokens("admin-access", "admin-refresh", 900));

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Auth-Client", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginBody("admin", "Password1"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("admin_refresh_token=admin-refresh"),
                        org.hamcrest.Matchers.containsString("HttpOnly"))));
    }

    @Test
    void refreshRotatesCookie() throws Exception {
        when(authenticationService.refresh("old-refresh"))
                .thenReturn(new AuthSessionService.AuthTokens("new-access", "new-refresh", 900));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "old-refresh")))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie",
                        org.hamcrest.Matchers.containsString("refresh_token=new-refresh")))
                .andExpect(jsonPath("$.data.accessToken").value("new-access"));
    }

    @Test
    void adminRefreshReadsAndRotatesOnlyItsOwnCookie() throws Exception {
        when(authenticationService.refresh("admin-old"))
                .thenReturn(new AuthSessionService.AuthTokens("admin-new-access", "admin-new", 900));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("X-Auth-Client", "admin")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "user-refresh"),
                                new jakarta.servlet.http.Cookie("admin_refresh_token", "admin-old")))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie",
                        org.hamcrest.Matchers.containsString("admin_refresh_token=admin-new")))
                .andExpect(jsonPath("$.data.accessToken").value("admin-new-access"));
    }

    private record RegisterBody(String username, String password, String preferences) {
    }

    private record LoginBody(String username, String password) {
    }
}

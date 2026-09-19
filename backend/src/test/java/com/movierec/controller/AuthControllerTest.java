package com.movierec.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.movierec.common.RequestIdFilter;
import com.movierec.entity.User;
import com.movierec.exception.GlobalExceptionHandler;
import com.movierec.exception.UnauthorizedException;
import com.movierec.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    @Mock
    private UserService userService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(userService))
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
        when(userService.login(any(), any()))
                .thenThrow(new UnauthorizedException("用户名或密码错误"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginBody("nobody", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    private record RegisterBody(String username, String password, String preferences) {
    }

    private record LoginBody(String username, String password) {
    }
}

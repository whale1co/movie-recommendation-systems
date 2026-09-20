package com.movierec.controller;

import com.movierec.ai.service.AiAdvisorService;
import com.movierec.common.RequestIdFilter;
import com.movierec.config.JwtAuthenticationFilter;
import com.movierec.config.SecurityConfig;
import com.movierec.service.SecurityAuditService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiAdvisorController.class)
@Import({SecurityConfig.class, RequestIdFilter.class})
@ContextConfiguration(classes = {AiAdvisorController.class, SecurityConfig.class, RequestIdFilter.class})
class AiAdvisorControllerSecurityTest {
    @Autowired MockMvc mockMvc;
    @MockBean AiAdvisorService advisorService;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean SecurityAuditService securityAuditService;

    @BeforeEach
    void allowRequestsThroughJwtFilter() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(
                ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void anonymousUserCannotCallAdvisor() throws Exception {
        mockMvc.perform(post("/api/v1/ai/advisor")
                        .contentType("application/json")
                        .content("{\"question\":\"推荐电影\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @WithMockUser(roles = "USER")
    void userWithoutAiPermissionIsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/ai/advisor")
                        .contentType("application/json")
                        .content("{\"question\":\"推荐电影\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }
}

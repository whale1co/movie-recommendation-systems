package com.movierec.config;

import com.movierec.common.RequestIdFilter;
import com.movierec.controller.HealthController;
import com.movierec.service.SecurityAuditService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
@Import({SecurityConfig.class, RequestIdFilter.class})
@ContextConfiguration(classes = {HealthController.class, SecurityConfig.class, RequestIdFilter.class})
class SecurityConfigTest {
    @org.junit.jupiter.api.Test
    void methodSecurityIsEnabled() {
        org.junit.jupiter.api.Assertions.assertTrue(SecurityConfig.class.isAnnotationPresent(EnableMethodSecurity.class));
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private SecurityAuditService securityAuditService;

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
    void protectedEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/movies/cold-start"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.requestId").exists());
    }

    @Test
    void movieDetailIsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/movies/123"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    void regularUserCannotAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/admin/health"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void developmentOriginPassesCorsPreflight() throws Exception {
        mockMvc.perform(options("/api/v1/health")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void unknownOriginFailsCorsPreflight() throws Exception {
        mockMvc.perform(options("/api/v1/health")
                        .header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}

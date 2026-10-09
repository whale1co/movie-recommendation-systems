package com.movierec.controller;

import com.movierec.ai.service.AiAdvisorService;
import com.movierec.service.AiAdvisorHistoryService;
import com.movierec.common.RequestIdFilter;
import com.movierec.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AiAdvisorControllerValidationTest {
    @Test
    void overlongQuestionReturnsBadRequest() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                        new AiAdvisorController(mock(AiAdvisorService.class), mock(AiAdvisorHistoryService.class)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new RequestIdFilter())
                .build();
        String question = "a".repeat(1001);

        mockMvc.perform(post("/api/v1/ai/advisor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"" + question + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.data.question").exists());
    }
}

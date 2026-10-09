package com.movierec.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.movierec.common.RequestIdFilter;
import com.movierec.exception.GlobalExceptionHandler;
import com.movierec.service.CommentService;
import com.movierec.service.MovieService;
import com.movierec.service.RatingService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiValidationTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void ratingOutsideRangeReturnsBadRequest() throws Exception {
        MockMvc mockMvc = mvc(new RatingController(mock(RatingService.class)));

        mockMvc.perform(post("/api/v1/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("movieId", 1, "score", 6))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.data.score").exists());
    }

    @Test
    void blankCommentReturnsBadRequest() throws Exception {
        MockMvc mockMvc = mvc(new CommentController(mock(CommentService.class)));

        mockMvc.perform(post("/api/v1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("movieId", 1, "content", "  "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.content").exists());
    }

    @Test
    void missingMovieUsesHttpNotFound() throws Exception {
        MockMvc mockMvc = mvc(new MovieController(mock(MovieService.class)));

        mockMvc.perform(get("/api/v1/movies/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("电影不存在"));
    }

    private MockMvc mvc(Object controller) {
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new RequestIdFilter())
                .build();
    }
}

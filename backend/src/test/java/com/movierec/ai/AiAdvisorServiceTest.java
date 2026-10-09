package com.movierec.ai;

import com.movierec.ai.client.LlmClient;
import com.movierec.ai.client.LlmClientException;
import com.movierec.ai.model.LlmRecommendation;
import com.movierec.ai.model.LlmRecommendationResult;
import com.movierec.ai.model.MovieIntent;
import com.movierec.ai.model.RetrievedMovie;
import com.movierec.ai.service.AiAdvisorService;
import com.movierec.ai.service.AiInputGuard;
import com.movierec.ai.service.HybridMovieRetriever;
import com.movierec.ai.service.LocalIntentParser;
import com.movierec.dto.response.AiAdvisorResponse;
import com.movierec.entity.Movie;
import com.movierec.service.AiUsageLimitService;
import com.movierec.service.AiAdvisorHistoryService;
import com.movierec.service.SecurityAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiAdvisorServiceTest {
    @Mock AiUsageLimitService usageLimitService;
    @Mock AiInputGuard inputGuard;
    @Mock LocalIntentParser localIntentParser;
    @Mock HybridMovieRetriever retriever;
    @Mock LlmClient llmClient;
    @Mock SecurityAuditService auditService;
    @Mock AiAdvisorHistoryService historyService;
    private AiAdvisorService service;

    @BeforeEach
    void setUp() {
        service = new AiAdvisorService(usageLimitService, inputGuard, localIntentParser,
                retriever, llmClient, auditService, historyService);
        when(llmClient.providerName()).thenReturn("fake");
    }

    @Test
    void dropsHallucinatedMovieIdsAndKeepsVerifiedCandidate() {
        MovieIntent intent = MovieIntent.empty();
        RetrievedMovie candidate = candidate(10L, "真实电影");
        when(localIntentParser.parse(any())).thenReturn(intent);
        when(llmClient.extractIntent(any())).thenReturn(intent);
        when(localIntentParser.merge(intent, intent)).thenReturn(intent);
        when(retriever.retrieve(7L, "推荐一部电影", intent)).thenReturn(List.of(candidate));
        when(llmClient.recommend(any(), any())).thenReturn(new LlmRecommendationResult("推荐如下", List.of(
                new LlmRecommendation(999L, "不存在", List.of()),
                new LlmRecommendation(10L, "来自候选片库", List.of("高评分")))));

        AiAdvisorResponse response = service.advise(7L, " 推荐一部电影 ");

        assertEquals(1, response.recommendations().size());
        assertEquals(10L, response.recommendations().get(0).movieId());
        assertEquals("/movie/10", response.recommendations().get(0).detailPath());
        assertTrue(response.aiGenerated());
        assertFalse(response.degraded());
        verify(usageLimitService).consume(7L);
        verify(inputGuard).validate(7L, "推荐一部电影");
    }

    @Test
    void modelFailureFallsBackToLocalCandidates() {
        MovieIntent intent = MovieIntent.empty();
        RetrievedMovie candidate = candidate(11L, "降级电影");
        when(localIntentParser.parse(any())).thenReturn(intent);
        when(llmClient.extractIntent(any())).thenThrow(new LlmClientException("timeout"));
        when(retriever.retrieve(8L, "想看经典电影", intent)).thenReturn(List.of(candidate));
        when(llmClient.recommend(any(), any())).thenThrow(new LlmClientException("timeout"));

        AiAdvisorResponse response = service.advise(8L, "想看经典电影");

        assertTrue(response.degraded());
        assertFalse(response.aiGenerated());
        assertEquals(11L, response.recommendations().get(0).movieId());
        verify(auditService).record(eq("AI_MODEL_CALL"), eq("DEGRADED"), eq(8L),
                eq("movie_advisor"), contains("provider=fake"));
    }

    private RetrievedMovie candidate(Long id, String title) {
        Movie movie = new Movie();
        movie.setId(id);
        movie.setTitle(title);
        movie.setGenre("剧情");
        movie.setRuntime(110);
        movie.setDoubanRating(new BigDecimal("8.8"));
        return new RetrievedMovie(movie, 0.9, 0.8, 1.0, 0.5, List.of("高评分"));
    }
}

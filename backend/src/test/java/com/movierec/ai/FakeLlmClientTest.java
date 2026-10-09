package com.movierec.ai;

import com.movierec.ai.client.FakeLlmClient;
import com.movierec.ai.model.LlmRecommendationResult;
import com.movierec.ai.model.RetrievedMovie;
import com.movierec.entity.Movie;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FakeLlmClientTest {
    @Test
    void onlyReturnsIdsFromProvidedCandidates() {
        Movie first = new Movie();
        first.setId(10L);
        Movie second = new Movie();
        second.setId(20L);
        List<RetrievedMovie> candidates = List.of(
                new RetrievedMovie(first, 1, 1, 1, 1, List.of()),
                new RetrievedMovie(second, 0.8, 0.8, 1, 0, List.of()));

        LlmRecommendationResult result = new FakeLlmClient().recommend("任意问题", candidates);

        assertEquals(2, result.recommendations().size());
        assertTrue(result.recommendations().stream()
                .allMatch(item -> item.movieId().equals(10L) || item.movieId().equals(20L)));
    }
}

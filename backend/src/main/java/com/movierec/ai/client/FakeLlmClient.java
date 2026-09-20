package com.movierec.ai.client;

import com.movierec.ai.model.LlmRecommendation;
import com.movierec.ai.model.LlmRecommendationResult;
import com.movierec.ai.model.MovieIntent;
import com.movierec.ai.model.RetrievedMovie;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "fake", matchIfMissing = true)
public class FakeLlmClient implements LlmClient {
    @Override
    public MovieIntent extractIntent(String question) {
        return MovieIntent.empty();
    }

    @Override
    public LlmRecommendationResult recommend(String question, List<RetrievedMovie> candidates) {
        List<LlmRecommendation> recommendations = candidates.stream().limit(5)
                .map(candidate -> new LlmRecommendation(candidate.movie().getId(),
                        "这部电影与检索到的观影需求较为匹配。", candidate.matchedCriteria()))
                .toList();
        return new LlmRecommendationResult("已根据本地片库为你筛选以下电影。", recommendations);
    }

    @Override
    public String providerName() {
        return "fake";
    }
}

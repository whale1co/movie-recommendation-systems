package com.movierec.ai.client;

import com.movierec.ai.model.LlmRecommendationResult;
import com.movierec.ai.model.MovieIntent;
import com.movierec.ai.model.RetrievedMovie;

import java.util.List;

public interface LlmClient {
    MovieIntent extractIntent(String question);

    LlmRecommendationResult recommend(String question, List<RetrievedMovie> candidates);

    String providerName();
}

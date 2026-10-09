package com.movierec.ai.model;

import java.util.List;

public record LlmRecommendationResult(String answer, List<LlmRecommendation> recommendations) {
}

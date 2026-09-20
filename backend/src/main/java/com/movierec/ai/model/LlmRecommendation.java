package com.movierec.ai.model;

import java.util.List;

public record LlmRecommendation(Long movieId, String reason, List<String> matchedCriteria) {
}

package com.movierec.dto.response;

import com.movierec.ai.model.MovieIntent;

import java.util.List;

public record AiAdvisorResponse(
        String answer,
        MovieIntent intent,
        List<AiMovieRecommendation> recommendations,
        boolean aiGenerated,
        boolean degraded,
        String provider
) {
}

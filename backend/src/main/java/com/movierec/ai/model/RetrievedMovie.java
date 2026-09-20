package com.movierec.ai.model;

import com.movierec.entity.Movie;

import java.util.List;

public record RetrievedMovie(
        Movie movie,
        double finalScore,
        double semanticScore,
        double constraintScore,
        double cfScore,
        List<String> matchedCriteria
) {
}

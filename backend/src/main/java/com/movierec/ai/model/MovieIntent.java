package com.movierec.ai.model;

import java.util.List;

public record MovieIntent(
        List<String> genres,
        List<String> excludedGenres,
        Integer maxRuntime,
        Double minRating,
        Integer minYear,
        Integer maxYear,
        List<String> keywords
) {
    public MovieIntent normalized() {
        return new MovieIntent(clean(genres), clean(excludedGenres),
                range(maxRuntime, 30, 600), range(minRating, 0.0, 10.0),
                range(minYear, 1888, 2100), range(maxYear, 1888, 2100), clean(keywords));
    }

    public static MovieIntent empty() {
        return new MovieIntent(List.of(), List.of(), null, null, null, null, List.of());
    }

    private static List<String> clean(List<String> values) {
        if (values == null) return List.of();
        return values.stream().filter(value -> value != null && !value.isBlank())
                .map(String::trim).distinct().limit(12).toList();
    }

    private static Integer range(Integer value, int min, int max) {
        return value != null && value >= min && value <= max ? value : null;
    }

    private static Double range(Double value, double min, double max) {
        return value != null && value >= min && value <= max ? value : null;
    }
}

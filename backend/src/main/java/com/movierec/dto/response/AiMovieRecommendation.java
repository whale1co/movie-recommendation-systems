package com.movierec.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AiMovieRecommendation(
        Long movieId,
        String title,
        String posterUrl,
        String genre,
        Integer runtime,
        LocalDate releaseDate,
        BigDecimal rating,
        String reason,
        List<String> matchedCriteria,
        String detailPath
) {
}

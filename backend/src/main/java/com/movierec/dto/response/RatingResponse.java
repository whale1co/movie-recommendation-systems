package com.movierec.dto.response;

import com.movierec.entity.Rating;

import java.time.LocalDateTime;

public record RatingResponse(Long ratingId, Long movieId, Double score,
                             LocalDateTime createTime, LocalDateTime updateTime) {
    public static RatingResponse from(Rating rating) {
        return new RatingResponse(rating.getId(), rating.getMovieId(), rating.getScore(),
                rating.getCreateTime(), rating.getUpdateTime());
    }
}

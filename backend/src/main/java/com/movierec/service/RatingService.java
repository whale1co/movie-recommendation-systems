package com.movierec.service;

import com.movierec.dto.RatingVO;
import com.movierec.entity.Rating;

import java.util.List;

public interface RatingService {

    Rating addRating(Long userId, Long movieId, Double score);

    Rating updateRating(Long ratingId, Long userId, Double score);

    void deleteRating(Long ratingId, Long userId);

    List<RatingVO> getUserRatings(Long userId);
}

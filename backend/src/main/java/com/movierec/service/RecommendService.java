package com.movierec.service;

import com.movierec.entity.Movie;

import java.util.List;

public interface RecommendService {

    List<Long> getUserCFRecommendations(Long userId, int topN);

    List<Movie> getRecommendations(Long userId, int topN);
}

package com.movierec.service;

import com.movierec.dto.FavoriteVO;
import com.movierec.entity.Favorite;

import java.util.List;

public interface FavoriteService {

    Favorite addFavorite(Long userId, Long movieId);

    void removeFavorite(Long userId, Long movieId);

    List<FavoriteVO> getUserFavorites(Long userId);
}

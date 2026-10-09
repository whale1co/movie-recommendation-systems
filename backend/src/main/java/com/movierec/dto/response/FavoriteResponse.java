package com.movierec.dto.response;

import com.movierec.entity.Favorite;

import java.time.LocalDateTime;

public record FavoriteResponse(Long favoriteId, Long movieId, LocalDateTime createTime) {
    public static FavoriteResponse from(Favorite favorite) {
        return new FavoriteResponse(favorite.getId(), favorite.getMovieId(), favorite.getCreateTime());
    }
}

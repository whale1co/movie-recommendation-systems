package com.movierec.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FavoriteVO {

    private Long favoriteId;

    private Long movieId;

    private String movieTitle;

    private String posterUrl;

    private BigDecimal avgRating;

    private String genre;

    private BigDecimal doubanRating;

    private LocalDateTime createTime;
}

package com.movierec.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RatingVO {

    private Long ratingId;

    private Long movieId;

    private String movieTitle;

    private String posterUrl;

    private Double score;

    private LocalDateTime createTime;
}

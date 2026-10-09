package com.movierec.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MovieDetailVO {

    private Long id;

    private String doubanId;

    private String title;

    private String director;

    private String actors;

    private String genre;

    private LocalDate releaseDate;

    private Integer runtime;

    private String summary;

    private String posterUrl;

    private BigDecimal doubanRating;

    private BigDecimal avgRating;

    private Integer ratingCount;

    private String status;
}

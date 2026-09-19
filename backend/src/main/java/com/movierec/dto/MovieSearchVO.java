package com.movierec.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MovieSearchVO {

    private Long id;

    private String title;

    private String posterUrl;

    private BigDecimal avgRating;

    private BigDecimal doubanRating;

    private String genre;

    private String releaseYear;
}

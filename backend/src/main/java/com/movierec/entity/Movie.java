package com.movierec.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("movie")
public class Movie {

    @TableId(type = IdType.AUTO)
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

    private LocalDateTime createTime;
}

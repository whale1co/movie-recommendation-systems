package com.movierec.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MyCommentVO {

    private Long commentId;

    private Long movieId;

    private String movieTitle;

    private String posterUrl;

    private String content;

    private Integer likeCount;

    private LocalDateTime createTime;
}

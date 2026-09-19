package com.movierec.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentVO {

    private Long commentId;

    private Long movieId;

    private String content;

    private String username;

    private Integer likeCount;

    private Boolean liked;

    private LocalDateTime createTime;
}

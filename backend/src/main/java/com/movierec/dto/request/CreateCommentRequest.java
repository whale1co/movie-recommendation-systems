package com.movierec.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotNull(message = "电影ID不能为空")
        @Positive(message = "电影ID必须为正数")
        Long movieId,

        @NotBlank(message = "评论内容不能为空")
        @Size(max = 1000, message = "评论内容不能超过1000个字符")
        String content
) {
}

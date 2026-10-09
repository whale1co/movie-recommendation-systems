package com.movierec.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateFavoriteRequest(
        @NotNull(message = "电影ID不能为空")
        @Positive(message = "电影ID必须为正数")
        Long movieId
) {
}

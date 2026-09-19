package com.movierec.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateRatingRequest(
        @NotNull(message = "电影ID不能为空")
        @Positive(message = "电影ID必须为正数")
        Long movieId,

        @NotNull(message = "评分不能为空")
        @DecimalMin(value = "1.0", message = "评分不能低于1分")
        @DecimalMax(value = "5.0", message = "评分不能高于5分")
        @Digits(integer = 1, fraction = 0, message = "评分必须为1到5之间的整数")
        Double score
) {
}

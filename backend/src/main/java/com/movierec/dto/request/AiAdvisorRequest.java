package com.movierec.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiAdvisorRequest(
        @NotBlank(message = "问题不能为空")
        @Size(max = 1000, message = "问题不能超过1000个字符")
        String question
) {
}

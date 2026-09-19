package com.movierec.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 2, max = 20, message = "用户名长度必须为2到20位")
        @Pattern(regexp = "^[\\p{L}\\p{N}_-]+$", message = "用户名只能包含文字、字母、数字、下划线或连字符")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 72, message = "密码长度必须为8到72位")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码必须同时包含字母和数字")
        String password,

        @Size(max = 255, message = "偏好内容不能超过255个字符")
        String preferences
) {
}

package com.movierec.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 72, message = "原密码不能超过72个字符")
        String oldPassword,

        @Size(min = 8, max = 72, message = "新密码长度必须为8到72位")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "新密码必须同时包含字母和数字")
        String newPassword,

        @Size(max = 255, message = "偏好内容不能超过255个字符")
        String preferences
) {
    @AssertTrue(message = "修改密码时必须同时提供原密码和新密码")
    public boolean isPasswordChangeComplete() {
        return (isBlank(oldPassword) && isBlank(newPassword))
                || (!isBlank(oldPassword) && !isBlank(newPassword));
    }

    @AssertTrue(message = "至少提供一个需要修改的字段")
    public boolean isAnyFieldPresent() {
        return preferences != null || !isBlank(oldPassword) || !isBlank(newPassword);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

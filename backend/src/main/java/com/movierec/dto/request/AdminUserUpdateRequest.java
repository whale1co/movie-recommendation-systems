package com.movierec.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminUserUpdateRequest(
        @Size(min = 8, max = 72) @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码必须同时包含字母和数字") String password,
        @Size(max = 255) String preferences,
        @Pattern(regexp = "USER|ADMIN", message = "角色必须为 USER 或 ADMIN") String role,
        @Pattern(regexp = "ACTIVE|DISABLED|DELETED", message = "状态不合法") String status
) {
    @AssertTrue(message = "至少提供一个需要修改的字段")
    public boolean hasChanges() {
        return password != null || preferences != null || role != null || status != null;
    }
}
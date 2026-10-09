package com.movierec.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminUserCreateRequest(
        @NotBlank @Size(min = 2, max = 20) @Pattern(regexp = "^[\\p{L}\\p{N}_-]+$") String username,
        @NotBlank @Size(min = 8, max = 72) @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$") String password,
        @Size(max = 255) String preferences,
        @Pattern(regexp = "USER|ADMIN", message = "角色必须为 USER 或 ADMIN") String role,
        @Pattern(regexp = "ACTIVE|DISABLED", message = "状态必须为 ACTIVE 或 DISABLED") String status
) {
}
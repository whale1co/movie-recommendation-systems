package com.movierec.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Fields that administrators may edit without changing crawler-owned identifiers. */
public record AdminMovieUpdateRequest(
        @Size(min = 1, max = 200) String title,
        @Size(max = 200) String director,
        @Size(max = 10000) String actors,
        @Size(max = 255) String genre,
        LocalDate releaseDate,
        @Min(1) Integer runtime,
        @Size(max = 20000) String summary,
        @Size(max = 500) String posterUrl,
        @DecimalMin("0.0") @DecimalMax("10.0") BigDecimal doubanRating,
        @Pattern(regexp = "ACTIVE|DELETED", message = "状态必须为 ACTIVE 或 DELETED") String status
) {
    @AssertTrue(message = "至少提供一个需要修改的字段")
    public boolean hasChanges() {
        return title != null || director != null || actors != null || genre != null || releaseDate != null
                || runtime != null || summary != null || posterUrl != null || doubanRating != null || status != null;
    }
}

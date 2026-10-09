package com.movierec.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record ApiResponse<T>(
        int code,
        String message,
        T data,
        String requestId,
        Instant timestamp
) {
    public static <T> ApiResponse<T> success(String message, T data) {
        return of(HttpStatus.OK, message, data);
    }

    public static <T> ApiResponse<T> created(String message, T data) {
        return of(HttpStatus.CREATED, message, data);
    }

    public static <T> ApiResponse<T> accepted(String message, T data) {
        return of(HttpStatus.ACCEPTED, message, data);
    }

    public static <T> ApiResponse<T> error(HttpStatus status, String message) {
        return of(status, message, null);
    }

    public static <T> ApiResponse<T> error(HttpStatus status, String message, T data) {
        return of(status, message, data);
    }

    private static <T> ApiResponse<T> of(HttpStatus status, String message, T data) {
        String requestId = MDC.get(RequestIdFilter.REQUEST_ID_KEY);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        return new ApiResponse<>(status.value(), message, data, requestId, Instant.now());
    }
}

package com.movierec.dto.response;

public record AuthTokenResponse(String accessToken, String tokenType, long expiresIn) {
    public AuthTokenResponse(String accessToken, long expiresIn) {
        this(accessToken, "Bearer", expiresIn);
    }
}

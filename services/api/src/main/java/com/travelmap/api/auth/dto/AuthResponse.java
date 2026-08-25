package com.travelmap.api.auth.dto;

public record AuthResponse(
        String tokenType,
        String accessToken,
        long accessExpiresInSeconds,
        String refreshToken,
        long refreshExpiresInSeconds
) {
}

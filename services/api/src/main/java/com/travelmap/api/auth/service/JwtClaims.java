package com.travelmap.api.auth.service;

import com.travelmap.api.auth.model.UserRole;

import java.util.UUID;

public record JwtClaims(UUID userId, String email, UserRole role, long expiresAtEpochSecond) {
}

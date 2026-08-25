package com.travelmap.api.auth.dto;

import com.travelmap.api.auth.model.UserEntity;

import java.util.UUID;

public record UserResponse(UUID id, String email, String role) {
    public static UserResponse from(UserEntity user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole().name());
    }
}

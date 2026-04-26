package com.restaurant.auth.dto;

import com.restaurant.auth.model.User;

import java.time.LocalDateTime;
import java.util.Set;

public record UserAdminResponse(
        Long id,
        String email,
        Set<String> roles,
        boolean emailVerified,
        LocalDateTime createdAt
) {
    public static UserAdminResponse from(User user) {
        return new UserAdminResponse(
                user.getId(),
                user.getEmail(),
                user.getRoles(),
                user.isEmailVerified(),
                user.getCreatedAt()
        );
    }
}

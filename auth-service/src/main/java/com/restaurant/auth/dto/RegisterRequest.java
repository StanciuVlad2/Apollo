package com.restaurant.auth.dto;

public record RegisterRequest(
        String email,
        String password,
        boolean skipEmailVerification
) {}

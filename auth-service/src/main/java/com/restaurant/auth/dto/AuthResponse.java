package com.restaurant.auth.dto;

public record AuthResponse(String token, long expiresInSeconds) {}

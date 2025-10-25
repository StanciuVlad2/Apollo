package com.restaurant.Apollo.Auth.dto;

public record AuthResponse(String token, long expiresInSeconds) {}

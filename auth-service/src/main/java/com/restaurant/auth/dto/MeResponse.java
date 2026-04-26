package com.restaurant.auth.dto;

public record MeResponse(boolean authenticated, String email, java.util.Set<String> roles) {}

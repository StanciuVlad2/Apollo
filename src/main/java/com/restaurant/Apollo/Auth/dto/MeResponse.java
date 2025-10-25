package com.restaurant.Apollo.Auth.dto;

public record MeResponse(boolean authenticated, String email, java.util.Set<String> roles) {}

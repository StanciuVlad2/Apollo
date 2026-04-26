package com.restaurant.shared.security;

import java.util.Set;

public record UserPrincipal(Long userId, String email, Set<String> roles) {}

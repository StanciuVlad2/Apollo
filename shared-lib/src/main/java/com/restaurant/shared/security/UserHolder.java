package com.restaurant.shared.security;

import org.springframework.security.core.context.SecurityContextHolder;

public class UserHolder {

    private UserHolder() {}

    public static UserPrincipal getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal p)) {
            throw new IllegalStateException("No authenticated user in context");
        }
        return p;
    }
}

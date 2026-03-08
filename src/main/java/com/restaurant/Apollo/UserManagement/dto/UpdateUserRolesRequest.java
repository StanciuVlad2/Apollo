package com.restaurant.Apollo.UserManagement.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UpdateUserRolesRequest(
        @NotEmpty(message = "Roles must not be empty")
        @Size(min = 1, max = 5, message = "A user must have between 1 and 5 roles")
        Set<String> roles
) {}

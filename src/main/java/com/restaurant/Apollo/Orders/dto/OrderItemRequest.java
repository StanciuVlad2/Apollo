package com.restaurant.Apollo.Orders.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OrderItemRequest(
        @NotBlank String menuItemId,
        @NotNull @Min(1) Integer quantity
) {}

package com.restaurant.operations.orders.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateOrderStatusRequest(
        @NotBlank String status
) {}

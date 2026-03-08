package com.restaurant.Apollo.Orders.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateOrderRequest(
        Long tableId,
        String notes,
        @NotEmpty @Valid List<OrderItemRequest> items
) {}

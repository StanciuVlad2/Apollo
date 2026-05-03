package com.restaurant.operations.orders.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CreateOrderRequest(
        Long tableId,
        String notes,
        String customerEmail,
        Long reservationId,
        @NotEmpty @Valid List<OrderItemRequest> items
) {}

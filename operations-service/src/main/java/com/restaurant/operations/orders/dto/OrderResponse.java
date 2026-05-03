package com.restaurant.operations.orders.dto;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long tableId,
        Long userId,
        String status,
        String notes,
        String customerEmail,
        Long reservationId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<OrderItemResponse> items,
        Double totalPrice
) {}

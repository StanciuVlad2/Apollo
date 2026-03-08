package com.restaurant.Apollo.Orders.dto;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long tableId,
        Long userId,
        String status,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<OrderItemResponse> items,
        Double totalPrice
) {}

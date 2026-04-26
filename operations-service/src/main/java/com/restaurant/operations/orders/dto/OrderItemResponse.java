package com.restaurant.operations.orders.dto;

public record OrderItemResponse(
        Long id,
        String menuItemId,
        String menuItemName,
        Integer quantity,
        Double unitPrice,
        Double subtotal
) {}

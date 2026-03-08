package com.restaurant.Apollo.Orders.dto;

public record OrderItemResponse(
        Long id,
        String menuItemId,
        String menuItemName,
        Integer quantity,
        Double unitPrice,
        Double subtotal
) {}

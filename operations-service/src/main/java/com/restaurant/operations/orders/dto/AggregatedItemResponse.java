package com.restaurant.operations.orders.dto;

public record AggregatedItemResponse(
        String menuItemId,
        String menuItemName,
        Integer totalQuantity,
        Double unitPrice,
        Double subtotal
) {}

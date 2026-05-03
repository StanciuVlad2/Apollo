package com.restaurant.operations.orders.dto;

public record TopItemData(
        String itemName,
        long quantitySold,
        double revenue
) {}

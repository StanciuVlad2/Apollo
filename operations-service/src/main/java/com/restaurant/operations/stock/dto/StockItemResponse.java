package com.restaurant.operations.stock.dto;

import com.restaurant.operations.stock.model.StockType;

public record StockItemResponse(
        String id,
        String name,
        Double quantity,
        String unit,
        Double minimumThreshold,
        boolean lowStock,
        StockType type
) {}

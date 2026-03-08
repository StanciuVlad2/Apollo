package com.restaurant.Apollo.Stock.dto;

import com.restaurant.Apollo.Stock.model.StockType;

public record StockItemResponse(
        String id,
        String name,
        Double quantity,
        String unit,
        Double minimumThreshold,
        boolean lowStock,
        StockType type
) {}

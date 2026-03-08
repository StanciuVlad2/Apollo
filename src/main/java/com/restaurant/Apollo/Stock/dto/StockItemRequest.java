package com.restaurant.Apollo.Stock.dto;

import com.restaurant.Apollo.Stock.model.StockType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StockItemRequest(
        @NotBlank(message = "Name is required")
        String name,

        @NotNull(message = "Quantity is required")
        @Min(value = 0, message = "Quantity must be non-negative")
        Double quantity,

        @NotBlank(message = "Unit is required")
        String unit,

        Double minimumThreshold,

        @NotNull(message = "Type is required")
        StockType type
) {}

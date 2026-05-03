package com.restaurant.operations.stock.dto;

import java.util.List;

public record StockImportResult(
    int created,
    int updated,
    int failed,
    List<RowError> errors
) {}

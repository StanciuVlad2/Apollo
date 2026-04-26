package com.restaurant.operations.menu.dto;

import java.util.List;

public record MenuItemResponse(
        String id,
        String name,
        String description,
        Double price,
        String category,
        Boolean available,
        String imageUrl,
        List<RecipeIngredientDto> recipe
) {}

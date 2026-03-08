package com.restaurant.Apollo.Menu.dto;

import java.util.List;

public record MenuItemResponse(
        String id,
        String name,
        String description,
        Double price,
        String category,
        Boolean available,
        List<RecipeIngredientDto> recipe
) {}

package com.restaurant.Apollo.Menu.dto;

public record RecipeIngredientDto(
        String ingredientName,
        Double quantity,
        String unit
) {}

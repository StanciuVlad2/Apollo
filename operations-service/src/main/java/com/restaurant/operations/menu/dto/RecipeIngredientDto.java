package com.restaurant.operations.menu.dto;

public record RecipeIngredientDto(
        String ingredientName,
        Double quantity,
        String unit
) {}

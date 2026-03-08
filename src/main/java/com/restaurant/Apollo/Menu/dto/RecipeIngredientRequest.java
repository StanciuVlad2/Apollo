package com.restaurant.Apollo.Menu.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RecipeIngredientRequest(
        @NotBlank String ingredientName,
        @NotNull @Min(0) Double quantity,
        @NotBlank String unit
) {}

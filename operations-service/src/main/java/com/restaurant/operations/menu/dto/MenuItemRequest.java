package com.restaurant.operations.menu.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record MenuItemRequest(
        @NotBlank String name,
        String description,

        @NotNull @Min(0) Double price,

        String category,
        Boolean available,
        String imageUrl,

        @Valid List<RecipeIngredientRequest> recipe
) {}

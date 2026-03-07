package com.restaurant.Apollo.Cocktails.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CocktailResponse {
    private String name;
    private String story;
    private List<String> ingredients;
    private String instructions;
}

package com.restaurant.operations.menu.model;

import lombok.*;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipeIngredient {

    @Field(type = FieldType.Keyword)
    private String ingredientName;

    @Field(type = FieldType.Double)
    private Double quantity;

    @Field(type = FieldType.Keyword)
    private String unit;
}

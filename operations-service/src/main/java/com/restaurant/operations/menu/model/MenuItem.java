package com.restaurant.operations.menu.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.util.ArrayList;
import java.util.List;

@Document(indexName = "menu_items")
@Setting(replicas = 0)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItem {

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String name;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Double)
    private Double price;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Boolean)
    private Boolean available;

    @Field(type = FieldType.Keyword)
    private String imageUrl;

    @Field(type = FieldType.Nested)
    @Builder.Default
    private List<RecipeIngredient> recipe = new ArrayList<>();
}

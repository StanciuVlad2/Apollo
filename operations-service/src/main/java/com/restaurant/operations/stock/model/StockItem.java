package com.restaurant.operations.stock.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

@Document(indexName = "stock_items")
@Setting(replicas = 0)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockItem {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String name;

    @Field(type = FieldType.Double)
    private Double quantity;

    @Field(type = FieldType.Keyword)
    private String unit;

    @Field(type = FieldType.Double)
    private Double minimumThreshold;

    @Field(type = FieldType.Keyword)
    private StockType type;
}

package com.restaurant.settings.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "restaurant_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantSetting {

    @Id
    @Column(name = "key", nullable = false, unique = true)
    private String key;

    @Column(name = "value", columnDefinition = "TEXT")
    private String value;
}

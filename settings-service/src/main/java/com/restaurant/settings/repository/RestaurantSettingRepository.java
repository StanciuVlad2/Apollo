package com.restaurant.settings.repository;

import com.restaurant.settings.model.RestaurantSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantSettingRepository extends JpaRepository<RestaurantSetting, String> {
}

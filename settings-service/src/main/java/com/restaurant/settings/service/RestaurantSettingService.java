package com.restaurant.settings.service;

import com.restaurant.settings.model.RestaurantSetting;
import com.restaurant.settings.repository.RestaurantSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RestaurantSettingService {

    private final RestaurantSettingRepository repository;

    public Map<String, String> getAll() {
        return repository.findAll()
                .stream()
                .collect(Collectors.toMap(RestaurantSetting::getKey, RestaurantSetting::getValue));
    }

    @Transactional
    public void upsertAll(Map<String, String> settings) {
        settings.forEach((key, value) -> {
            RestaurantSetting setting = repository.findById(key)
                    .orElse(new RestaurantSetting(key, null));
            setting.setValue(value);
            repository.save(setting);
        });
    }
}

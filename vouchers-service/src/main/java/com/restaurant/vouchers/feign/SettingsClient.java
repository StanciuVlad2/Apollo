package com.restaurant.vouchers.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@FeignClient(name = "settings-service")
public interface SettingsClient {

    @GetMapping("/api/settings")
    Map<String, String> getSettings();
}

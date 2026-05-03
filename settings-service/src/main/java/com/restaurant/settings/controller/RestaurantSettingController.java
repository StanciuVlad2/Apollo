package com.restaurant.settings.controller;

import com.restaurant.settings.service.RestaurantSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class RestaurantSettingController {

    private final RestaurantSettingService service;

    @GetMapping
    public ResponseEntity<Map<String, String>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Void> upsertAll(@RequestBody Map<String, String> settings) {
        service.upsertAll(settings);
        return ResponseEntity.ok().build();
    }
}

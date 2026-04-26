package com.restaurant.cocktails.controller;

import com.restaurant.cocktails.dto.CocktailRequest;
import com.restaurant.cocktails.dto.CocktailResponse;
import com.restaurant.cocktails.service.GeminiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cocktails")
@RequiredArgsConstructor
@Slf4j
public class CocktailController {

    private final GeminiService geminiService;

    @PostMapping("/generate")
    public ResponseEntity<CocktailResponse> generateCocktail(@Valid @RequestBody CocktailRequest request) {
        log.info("Generating cocktail for user description: {}", request.getUserDescription());
        try {
            CocktailResponse response = geminiService.generateCocktail(request.getUserDescription());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating cocktail", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

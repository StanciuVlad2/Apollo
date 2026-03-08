package com.restaurant.Apollo.Menu.controllers;

import com.restaurant.Apollo.Menu.dto.MenuItemRequest;
import com.restaurant.Apollo.Menu.dto.MenuItemResponse;
import com.restaurant.Apollo.Menu.dto.RecipeIngredientDto;
import com.restaurant.Apollo.Menu.service.MenuItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menu-items")
@RequiredArgsConstructor
public class MenuItemController {

    private final MenuItemService menuItemService;

    /** Public endpoint – returns all menu items (without recipe details). */
    @GetMapping
    public ResponseEntity<List<MenuItemResponse>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean availableOnly) {

        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(menuItemService.search(search));
        }
        if (category != null && !category.isBlank()) {
            return ResponseEntity.ok(menuItemService.getByCategory(category));
        }
        if (Boolean.TRUE.equals(availableOnly)) {
            return ResponseEntity.ok(menuItemService.getAvailable());
        }
        return ResponseEntity.ok(menuItemService.getAll());
    }

    /** Public endpoint – single item. */
    @GetMapping("/{id}")
    public ResponseEntity<MenuItemResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(menuItemService.getById(id));
    }

    /** Recipe details – MANAGER or CHEF only. */
    @GetMapping("/{id}/recipe")
    @PreAuthorize("hasRole('MANAGER') or hasRole('CHEF') or hasRole('ADMIN')")
    public ResponseEntity<List<RecipeIngredientDto>> getRecipe(@PathVariable String id) {
        return ResponseEntity.ok(menuItemService.getRecipe(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<MenuItemResponse> create(@Valid @RequestBody MenuItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(menuItemService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<MenuItemResponse> update(
            @PathVariable String id,
            @Valid @RequestBody MenuItemRequest request) {
        return ResponseEntity.ok(menuItemService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        menuItemService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

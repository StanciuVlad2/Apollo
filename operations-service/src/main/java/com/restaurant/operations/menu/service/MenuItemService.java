package com.restaurant.operations.menu.service;

import com.restaurant.operations.menu.dto.MenuItemRequest;
import com.restaurant.operations.menu.dto.MenuItemResponse;
import com.restaurant.operations.menu.dto.RecipeIngredientDto;
import com.restaurant.operations.menu.model.MenuItem;
import com.restaurant.operations.menu.model.RecipeIngredient;
import com.restaurant.operations.menu.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;

    public List<MenuItemResponse> getAll() {
        return StreamSupport.stream(menuItemRepository.findAll().spliterator(), false)
                .map(this::toResponse)
                .toList();
    }

    public List<MenuItemResponse> getAvailable() {
        return menuItemRepository.findByAvailableTrue().stream()
                .map(this::toResponse)
                .toList();
    }

    public MenuItemResponse getById(String id) {
        return menuItemRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new NoSuchElementException("Menu item not found: " + id));
    }

    public List<MenuItemResponse> search(String name) {
        return menuItemRepository.findByNameContainingIgnoreCase(name).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<MenuItemResponse> getByCategory(String category) {
        return menuItemRepository.findByCategory(category).stream()
                .map(this::toResponse)
                .toList();
    }

    public MenuItemResponse create(MenuItemRequest request) {
        if (menuItemRepository.existsByNameIgnoreCase(request.name())) {
            throw new IllegalArgumentException("Menu item already exists: " + request.name());
        }
        MenuItem item = buildMenuItem(null, request);
        return toResponse(menuItemRepository.save(item));
    }

    public MenuItemResponse update(String id, MenuItemRequest request) {
        MenuItem existing = menuItemRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Menu item not found: " + id));

        if (!existing.getName().equalsIgnoreCase(request.name())
                && menuItemRepository.existsByNameIgnoreCase(request.name())) {
            throw new IllegalArgumentException("Menu item already exists: " + request.name());
        }

        MenuItem item = buildMenuItem(id, request);
        return toResponse(menuItemRepository.save(item));
    }

    public void delete(String id) {
        if (!menuItemRepository.existsById(id)) {
            throw new NoSuchElementException("Menu item not found: " + id);
        }
        menuItemRepository.deleteById(id);
    }

    /**
     * Returns the full recipe for a specific menu item.
     * Access is restricted to MANAGER and CHEF at the controller level.
     */
    public List<RecipeIngredientDto> getRecipe(String id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Menu item not found: " + id));
        return mapRecipe(item.getRecipe());
    }

    public MenuItemResponse updateImageUrl(String id, String imageUrl) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Menu item not found: " + id));
        item.setImageUrl(imageUrl);
        return toResponse(menuItemRepository.save(item));
    }

    /**
     * Reads a menu item from ES by ID (used internally by the order service).
     */
    public MenuItem getRawById(String id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Menu item not found: " + id));
    }

    private MenuItem buildMenuItem(String id, MenuItemRequest request) {
        List<RecipeIngredient> recipe = new ArrayList<>();
        if (request.recipe() != null) {
            request.recipe().forEach(r -> recipe.add(
                    RecipeIngredient.builder()
                            .ingredientName(r.ingredientName().toLowerCase().trim())
                            .quantity(r.quantity())
                            .unit(r.unit())
                            .build()
            ));
        }
        return MenuItem.builder()
                .id(id)
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .category(request.category())
                .available(request.available() != null ? request.available() : true)
                .imageUrl(request.imageUrl())
                .recipe(recipe)
                .build();
    }

    public List<MenuItemResponse> getAllFull() {
        return StreamSupport.stream(menuItemRepository.findAll().spliterator(), false)
                .map(this::toFullResponse)
                .toList();
    }

    private MenuItemResponse toResponse(MenuItem item) {
        return new MenuItemResponse(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getPrice(),
                item.getCategory(),
                item.getAvailable(),
                item.getImageUrl(),
                List.of()
        );
    }

    private MenuItemResponse toFullResponse(MenuItem item) {
        return new MenuItemResponse(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getPrice(),
                item.getCategory(),
                item.getAvailable(),
                item.getImageUrl(),
                mapRecipe(item.getRecipe())
        );
    }

    private List<RecipeIngredientDto> mapRecipe(List<RecipeIngredient> recipe) {
        if (recipe == null) return List.of();
        return recipe.stream()
                .map(r -> new RecipeIngredientDto(r.getIngredientName(), r.getQuantity(), r.getUnit()))
                .toList();
    }
}

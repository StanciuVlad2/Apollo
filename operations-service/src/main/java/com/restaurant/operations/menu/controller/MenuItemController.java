package com.restaurant.operations.menu.controller;

import com.restaurant.operations.menu.dto.MenuItemRequest;
import com.restaurant.operations.menu.dto.MenuItemResponse;
import com.restaurant.operations.menu.dto.RecipeIngredientDto;
import com.restaurant.operations.menu.service.MenuItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import net.coobird.thumbnailator.Thumbnails;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/menu-items")
@RequiredArgsConstructor
public class MenuItemController {

    private final MenuItemService menuItemService;

    @Value("${app.upload.dir:uploads/menu-images}")
    private String uploadDir;

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

    /** Staff endpoint – returns all menu items with recipe details (MANAGER/CHEF/ADMIN). */
    @GetMapping("/staff")
    @PreAuthorize("hasAnyRole('MANAGER','CHEF','ADMIN')")
    public ResponseEntity<List<MenuItemResponse>> getAllFull() {
        return ResponseEntity.ok(menuItemService.getAllFull());
    }

    /** Public endpoint – single item. */
    @GetMapping("/{id}")
    public ResponseEntity<MenuItemResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(menuItemService.getById(id));
    }

    /** Recipe details – MANAGER or CHEF only. */
    @GetMapping("/{id}/recipe")
    @PreAuthorize("hasAnyRole('MANAGER','CHEF','ADMIN')")
    public ResponseEntity<List<RecipeIngredientDto>> getRecipe(@PathVariable String id) {
        return ResponseEntity.ok(menuItemService.getRecipe(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<MenuItemResponse> create(@Valid @RequestBody MenuItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(menuItemService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','CHEF','ADMIN')")
    public ResponseEntity<MenuItemResponse> update(
            @PathVariable String id,
            @Valid @RequestBody MenuItemRequest request) {
        return ResponseEntity.ok(menuItemService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        menuItemService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('MANAGER','CHEF','ADMIN')")
    public ResponseEntity<Map<String, String>> uploadImage(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file) throws IOException {

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only image files are allowed"));
        }

        // Always save as .jpg after resize
        String filename = UUID.randomUUID() + ".jpg";

        Path dir = Paths.get(uploadDir);
        Files.createDirectories(dir);

        // Resize to max 1200x800 keeping aspect ratio, output quality 85%
        Thumbnails.of(file.getInputStream())
                .size(1200, 800)
                .keepAspectRatio(true)
                .outputFormat("jpg")
                .outputQuality(0.85)
                .toFile(dir.resolve(filename).toFile());

        String imageUrl = "/uploads/menu-images/" + filename;
        menuItemService.updateImageUrl(id, imageUrl);

        return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
    }
}

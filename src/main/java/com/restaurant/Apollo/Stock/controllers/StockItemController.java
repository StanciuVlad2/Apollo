package com.restaurant.Apollo.Stock.controllers;

import com.restaurant.Apollo.Stock.dto.StockItemRequest;
import com.restaurant.Apollo.Stock.dto.StockItemResponse;
import com.restaurant.Apollo.Stock.service.StockItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockItemController {

    private final StockItemService stockItemService;

    @GetMapping
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<StockItemResponse>> getAll(
            @RequestParam(required = false) String search) {
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(stockItemService.search(search));
        }
        return ResponseEntity.ok(stockItemService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<StockItemResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(stockItemService.getById(id));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<StockItemResponse>> getLowStock() {
        return ResponseEntity.ok(stockItemService.getLowStock());
    }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<StockItemResponse> create(@Valid @RequestBody StockItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stockItemService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<StockItemResponse> update(
            @PathVariable String id,
            @Valid @RequestBody StockItemRequest request) {
        return ResponseEntity.ok(stockItemService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        stockItemService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

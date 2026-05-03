package com.restaurant.operations.stock.controller;

import com.restaurant.operations.stock.dto.RowError;
import com.restaurant.operations.stock.dto.StockImportResult;
import com.restaurant.operations.stock.dto.StockItemRequest;
import com.restaurant.operations.stock.dto.StockItemResponse;
import com.restaurant.operations.stock.service.StockItemService;
import com.restaurant.shared.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
public class StockItemController {

    private final StockItemService stockItemService;

    @GetMapping
    public ResponseEntity<PageResponse<StockItemResponse>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        return ResponseEntity.ok(stockItemService.getAllPaged(search, page, size));
    }

    @GetMapping("/all")
    public ResponseEntity<List<StockItemResponse>> getAllUnpaged(
            @RequestParam(required = false) String search) {
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(stockItemService.search(search));
        }
        return ResponseEntity.ok(stockItemService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StockItemResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(stockItemService.getById(id));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<StockItemResponse>> getLowStock() {
        return ResponseEntity.ok(stockItemService.getLowStock());
    }

    @PostMapping
    public ResponseEntity<StockItemResponse> create(@Valid @RequestBody StockItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stockItemService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StockItemResponse> update(
            @PathVariable String id,
            @Valid @RequestBody StockItemRequest request) {
        return ResponseEntity.ok(stockItemService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        stockItemService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/import")
    public ResponseEntity<StockImportResult> importCsv(@RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(stockItemService.importCsv(file));
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to parse CSV file: " + e.getMessage());
        }
    }
}

package com.restaurant.Apollo.Reservations.controllers;

import com.restaurant.Apollo.Reservations.dto.CreateTableRequest;
import com.restaurant.Apollo.Reservations.dto.TableResponse;
import com.restaurant.Apollo.Reservations.dto.UpdateTableRequest;
import com.restaurant.Apollo.Reservations.service.RestaurantTableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tables")
public class RestaurantTableController {
    
    private static final Logger log = LoggerFactory.getLogger(RestaurantTableController.class);

    @Autowired
    private RestaurantTableService tableService;

    @PostMapping
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public ResponseEntity<TableResponse> createTable(@RequestBody CreateTableRequest request) {
        try {
            TableResponse response = tableService.createTable(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public ResponseEntity<TableResponse> updateTable(
            @PathVariable Long id,
            @RequestBody UpdateTableRequest request) {
        log.info("Updating table {} with xPosition={}, yPosition={}", id, request.getXPosition(), request.getYPosition());
        try {
            TableResponse response = tableService.updateTable(id, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Failed to update table: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public ResponseEntity<Void> deleteTable(@PathVariable Long id) {
        try {
            tableService.deleteTable(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<TableResponse> getTable(@PathVariable Long id) {
        try {
            TableResponse response = tableService.getTable(id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<TableResponse>> getAllTables() {
        List<TableResponse> tables = tableService.getActiveTables();
        return ResponseEntity.ok(tables);
    }

    @GetMapping("/capacity/{capacity}")
    public ResponseEntity<List<TableResponse>> getTablesByCapacity(@PathVariable Integer capacity) {
        List<TableResponse> tables = tableService.getTablesByCapacity(capacity);
        return ResponseEntity.ok(tables);
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    public ResponseEntity<List<TableResponse>> getAllTablesIncludingInactive() {
        List<TableResponse> tables = tableService.getAllTables();
        return ResponseEntity.ok(tables);
    }
}

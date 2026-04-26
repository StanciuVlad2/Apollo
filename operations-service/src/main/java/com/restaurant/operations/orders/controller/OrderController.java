package com.restaurant.operations.orders.controller;

import com.restaurant.operations.orders.dto.CreateOrderRequest;
import com.restaurant.operations.orders.dto.OrderResponse;
import com.restaurant.operations.orders.dto.UpdateOrderStatusRequest;
import com.restaurant.operations.orders.service.OrderService;
import com.restaurant.shared.security.UserHolder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    @PreAuthorize("hasAnyRole('WAITER','CHEF','MANAGER','ADMIN')")
    public ResponseEntity<List<OrderResponse>> getAll(
            @RequestParam(required = false) String status) {
        if (status != null && !status.isBlank()) {
            return ResponseEntity.ok(orderService.getByStatus(status));
        }
        return ResponseEntity.ok(orderService.getAll());
    }

    @GetMapping("/my-orders")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<OrderResponse>> getMyOrders() {
        Long userId = UserHolder.getCurrentUser().userId();
        return ResponseEntity.ok(orderService.getByUserId(userId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getById(id));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> create(
            @Valid @RequestBody CreateOrderRequest request) {
        Long userId = UserHolder.getCurrentUser().userId();
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(request, userId));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('WAITER','CHEF','MANAGER','ADMIN')")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(orderService.updateStatus(id, request.status()));
    }
}

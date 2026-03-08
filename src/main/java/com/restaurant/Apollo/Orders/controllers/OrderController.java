package com.restaurant.Apollo.Orders.controllers;

import com.restaurant.Apollo.Orders.dto.CreateOrderRequest;
import com.restaurant.Apollo.Orders.dto.OrderResponse;
import com.restaurant.Apollo.Orders.dto.UpdateOrderStatusRequest;
import com.restaurant.Apollo.Orders.service.OrderService;
import com.restaurant.Apollo.UserManagement.model.User;
import com.restaurant.Apollo.UserManagement.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('WAITER','CHEF','MANAGER','ADMIN')")
    public ResponseEntity<List<OrderResponse>> getAll(
            @RequestParam(required = false) String status) {
        if (status != null && !status.isBlank()) {
            return ResponseEntity.ok(orderService.getByStatus(status));
        }
        return ResponseEntity.ok(orderService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getById(id));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> create(
            @Valid @RequestBody CreateOrderRequest request,
            Principal principal) {
        Long userId = null;
        if (principal != null) {
            userId = userRepository.findByEmail(principal.getName())
                    .map(User::getId)
                    .orElse(null);
        }
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

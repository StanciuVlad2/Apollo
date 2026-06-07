package com.restaurant.operations.orders.controller;

import com.restaurant.operations.orders.dto.CreateOrderRequest;
import com.restaurant.operations.orders.dto.OrderResponse;
import com.restaurant.operations.orders.dto.ReservationBillResponse;
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
import java.util.Objects;
import java.util.Set;

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

    private static final Set<String> STAFF_ROLES =
            Set.of("ROLE_WAITER", "ROLE_CHEF", "ROLE_MANAGER", "ROLE_ADMIN");

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> getById(@PathVariable Long id) {
        OrderResponse order = orderService.getById(id);
        boolean isStaff = UserHolder.getCurrentUser().roles().stream().anyMatch(STAFF_ROLES::contains);
        if (!isStaff && !Objects.equals(order.userId(), UserHolder.getCurrentUser().userId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(order);
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

    @PostMapping("/bill-reservation/{reservationId}")
    @PreAuthorize("hasAnyRole('WAITER','MANAGER','ADMIN')")
    public ResponseEntity<List<OrderResponse>> billReservation(@PathVariable Long reservationId) {
        return ResponseEntity.ok(orderService.billReservation(reservationId));
    }

    @GetMapping("/by-reservation/{reservationId}")
    @PreAuthorize("hasAnyRole('WAITER','MANAGER','ADMIN')")
    public ResponseEntity<ReservationBillResponse> getReservationBill(@PathVariable Long reservationId) {
        return ResponseEntity.ok(orderService.getReservationBill(reservationId));
    }
}

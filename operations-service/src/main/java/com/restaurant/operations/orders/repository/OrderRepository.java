package com.restaurant.operations.orders.repository;

import com.restaurant.operations.orders.enums.OrderStatus;
import com.restaurant.operations.orders.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByStatus(OrderStatus status);
    List<Order> findByTableId(Long tableId);
    List<Order> findAllByOrderByCreatedAtDesc();
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Order> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
}

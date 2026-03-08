package com.restaurant.Apollo.Orders.repository;

import com.restaurant.Apollo.Orders.enums.OrderStatus;
import com.restaurant.Apollo.Orders.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByStatus(OrderStatus status);
    List<Order> findByTableId(Long tableId);
    List<Order> findAllByOrderByCreatedAtDesc();
}

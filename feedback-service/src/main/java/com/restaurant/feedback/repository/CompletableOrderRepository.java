package com.restaurant.feedback.repository;

import com.restaurant.feedback.model.CompletableOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompletableOrderRepository extends JpaRepository<CompletableOrder, Long> {
    Optional<CompletableOrder> findByOrderIdAndUserId(Long orderId, Long userId);
}

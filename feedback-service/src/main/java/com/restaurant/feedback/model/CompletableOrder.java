package com.restaurant.feedback.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "completable_orders")
public class CompletableOrder {

    @Id
    private Long orderId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Instant completedAt;

    public CompletableOrder() {}

    public CompletableOrder(Long orderId, Long userId, Instant completedAt) {
        this.orderId = orderId;
        this.userId = userId;
        this.completedAt = completedAt;
    }

    public Long getOrderId() { return orderId; }
    public Long getUserId() { return userId; }
    public Instant getCompletedAt() { return completedAt; }
}

package com.restaurant.vouchers.kafka;

import java.time.Instant;

public record OrderCompletedEvent(Long orderId, Long userId, Instant completedAt) {}

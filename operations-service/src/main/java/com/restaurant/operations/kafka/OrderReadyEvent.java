package com.restaurant.operations.kafka;

import java.time.Instant;

public record OrderReadyEvent(Long orderId, Long tableId, String customerEmail, Instant readyAt) {}

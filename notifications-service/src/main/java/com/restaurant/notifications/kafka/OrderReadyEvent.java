package com.restaurant.notifications.kafka;

import java.time.Instant;

public record OrderReadyEvent(Long orderId, Long tableId, String customerEmail, Instant readyAt) {}

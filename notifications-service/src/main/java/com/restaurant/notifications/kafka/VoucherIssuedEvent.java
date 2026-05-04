package com.restaurant.notifications.kafka;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VoucherIssuedEvent(String userEmail, String code, BigDecimal value, LocalDate expiryDate) {}

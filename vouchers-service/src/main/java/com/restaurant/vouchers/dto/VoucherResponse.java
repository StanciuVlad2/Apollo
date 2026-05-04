package com.restaurant.vouchers.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record VoucherResponse(
        Long id,
        String code,
        BigDecimal value,
        LocalDate expiryDate,
        boolean used,
        boolean expired,
        LocalDateTime createdAt
) {}

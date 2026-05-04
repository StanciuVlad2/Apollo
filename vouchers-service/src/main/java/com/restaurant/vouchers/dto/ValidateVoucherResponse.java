package com.restaurant.vouchers.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ValidateVoucherResponse(String code, BigDecimal value, LocalDate expiryDate) {}

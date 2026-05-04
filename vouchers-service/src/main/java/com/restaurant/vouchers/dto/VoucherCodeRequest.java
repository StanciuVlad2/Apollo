package com.restaurant.vouchers.dto;

import jakarta.validation.constraints.NotBlank;

public record VoucherCodeRequest(@NotBlank String code) {}

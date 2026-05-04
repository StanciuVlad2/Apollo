package com.restaurant.vouchers.controller;

import com.restaurant.shared.security.UserHolder;
import com.restaurant.vouchers.dto.ValidateVoucherResponse;
import com.restaurant.vouchers.dto.VoucherCodeRequest;
import com.restaurant.vouchers.dto.VoucherResponse;
import com.restaurant.vouchers.service.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<VoucherResponse>> getMyVouchers() {
        Long userId = UserHolder.getCurrentUser().userId();
        return ResponseEntity.ok(voucherService.getVouchersForUser(userId));
    }

    @PostMapping("/validate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> validate(@Valid @RequestBody VoucherCodeRequest request) {
        try {
            ValidateVoucherResponse response = voucherService.validate(request.code());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/redeem")
    @PreAuthorize("hasAnyRole('WAITER','MANAGER','ADMIN')")
    public ResponseEntity<?> redeem(@Valid @RequestBody VoucherCodeRequest request) {
        try {
            ValidateVoucherResponse response = voucherService.redeem(request.code());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}

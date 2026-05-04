package com.restaurant.vouchers.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.vouchers.dto.ValidateVoucherResponse;
import com.restaurant.vouchers.dto.VoucherResponse;
import com.restaurant.vouchers.feign.AuthClient;
import com.restaurant.vouchers.feign.OperationsClient;
import com.restaurant.vouchers.feign.SettingsClient;
import com.restaurant.vouchers.kafka.VoucherEventProducer;
import com.restaurant.vouchers.kafka.VoucherIssuedEvent;
import com.restaurant.vouchers.model.Voucher;
import com.restaurant.vouchers.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoucherService {

    private static final String VOUCHER_RULES_KEY = "voucher.rules";
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final VoucherRepository voucherRepository;
    private final OperationsClient operationsClient;
    private final SettingsClient settingsClient;
    private final AuthClient authClient;
    private final VoucherEventProducer eventProducer;
    private final ObjectMapper objectMapper;

    @Transactional
    public void processOrderCompleted(Long orderId, Long userId) {
        if (voucherRepository.existsByOrderId(orderId)) {
            log.info("Voucher already exists for orderId={}, skipping", orderId);
            return;
        }

        double orderTotal;
        try {
            Map<String, Object> order = operationsClient.getOrder(orderId);
            orderTotal = ((Number) order.get("totalPrice")).doubleValue();
        } catch (Exception e) {
            log.warn("Could not fetch order {} for voucher check: {}", orderId, e.getMessage());
            return;
        }

        VoucherRule bestRule = findBestRule(orderTotal);
        if (bestRule == null) {
            log.debug("No voucher rule matched for orderId={} total={}", orderId, orderTotal);
            return;
        }

        String userEmail;
        try {
            userEmail = authClient.getUserEmail(userId).get("email");
        } catch (Exception e) {
            log.warn("Could not fetch email for userId={}: {}", userId, e.getMessage());
            return;
        }

        String code = generateUniqueCode();
        LocalDate expiryDate = LocalDate.now().plusDays(bestRule.expiryDays());

        Voucher voucher = Voucher.builder()
                .code(code)
                .userId(userId)
                .value(BigDecimal.valueOf(bestRule.voucherValue()))
                .expiryDate(expiryDate)
                .used(false)
                .orderId(orderId)
                .build();
        voucherRepository.save(voucher);

        eventProducer.publishVoucherIssued(new VoucherIssuedEvent(userEmail, code, voucher.getValue(), expiryDate));
        log.info("Issued voucher code={} value={} for userId={} orderId={}", code, bestRule.voucherValue(), userId, orderId);
    }

    public List<VoucherResponse> getVouchersForUser(Long userId) {
        return voucherRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public ValidateVoucherResponse validate(String code) {
        Voucher voucher = voucherRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Voucher not found"));
        if (voucher.isUsed()) throw new IllegalArgumentException("Voucher already used");
        if (voucher.getExpiryDate().isBefore(LocalDate.now())) throw new IllegalArgumentException("Voucher expired");
        return new ValidateVoucherResponse(voucher.getCode(), voucher.getValue(), voucher.getExpiryDate());
    }

    @Transactional
    public ValidateVoucherResponse redeem(String code) {
        Voucher voucher = voucherRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Voucher not found"));
        if (voucher.isUsed()) throw new IllegalArgumentException("Voucher already used");
        if (voucher.getExpiryDate().isBefore(LocalDate.now())) throw new IllegalArgumentException("Voucher expired");
        voucher.setUsed(true);
        voucherRepository.save(voucher);
        return new ValidateVoucherResponse(voucher.getCode(), voucher.getValue(), voucher.getExpiryDate());
    }

    // -- private helpers ----------------------------------------------------

    private VoucherRule findBestRule(double orderTotal) {
        try {
            Map<String, String> settings = settingsClient.getSettings();
            String rulesJson = settings.get(VOUCHER_RULES_KEY);
            if (rulesJson == null || rulesJson.isBlank()) return null;

            List<VoucherRule> rules = objectMapper.readValue(rulesJson, new TypeReference<>() {});
            return rules.stream()
                    .filter(r -> orderTotal >= r.minOrderValue())
                    .max(Comparator.comparingDouble(VoucherRule::minOrderValue))
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Could not parse voucher rules: {}", e.getMessage());
            return null;
        }
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 5; attempt++) {
            StringBuilder sb = new StringBuilder("ODIN-");
            for (int i = 0; i < 4; i++) sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
            String code = sb.toString();
            if (voucherRepository.findByCode(code).isEmpty()) return code;
        }
        throw new IllegalStateException("Could not generate unique voucher code after 5 attempts");
    }

    private VoucherResponse toResponse(Voucher v) {
        return new VoucherResponse(
                v.getId(), v.getCode(), v.getValue(), v.getExpiryDate(),
                v.isUsed(), v.getExpiryDate().isBefore(LocalDate.now()), v.getCreatedAt()
        );
    }

    private record VoucherRule(double minOrderValue, double voucherValue, int expiryDays) {}
}

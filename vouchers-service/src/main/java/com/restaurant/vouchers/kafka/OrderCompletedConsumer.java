package com.restaurant.vouchers.kafka;

import com.restaurant.vouchers.service.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCompletedConsumer {

    private final VoucherService voucherService;

    @KafkaListener(topics = "order.completed", groupId = "vouchers-service-group")
    public void handle(OrderCompletedEvent event) {
        log.info("Received order.completed orderId={} userId={}", event.orderId(), event.userId());
        try {
            voucherService.processOrderCompleted(event.orderId(), event.userId());
        } catch (Exception e) {
            log.error("Error processing voucher for orderId={}: {}", event.orderId(), e.getMessage());
        }
    }
}

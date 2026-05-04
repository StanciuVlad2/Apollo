package com.restaurant.vouchers.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class VoucherEventProducer {

    private static final String TOPIC = "voucher.issued";
    private final KafkaTemplate<String, VoucherIssuedEvent> kafkaTemplate;

    public void publishVoucherIssued(VoucherIssuedEvent event) {
        kafkaTemplate.send(TOPIC, event.code(), event);
        log.info("Published voucher.issued for code={} email={}", event.code(), event.userEmail());
    }
}

package com.restaurant.operations.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private static final String TOPIC = "order.completed";
    private final KafkaTemplate<String, OrderCompletedEvent> kafkaTemplate;

    public void publishOrderCompleted(Long orderId, Long userId) {
        OrderCompletedEvent event = new OrderCompletedEvent(orderId, userId, java.time.Instant.now());
        kafkaTemplate.send(TOPIC, orderId.toString(), event);
        log.info("Published order.completed for orderId={}", orderId);
    }
}

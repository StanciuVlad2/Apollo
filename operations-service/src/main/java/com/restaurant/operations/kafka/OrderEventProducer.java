package com.restaurant.operations.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderEventProducer {

    private static final String COMPLETED_TOPIC = "order.completed";
    private static final String READY_TOPIC = "order.ready";

    private final KafkaTemplate<String, OrderCompletedEvent> completedKafkaTemplate;
    private final KafkaTemplate<String, OrderReadyEvent> readyKafkaTemplate;

    public OrderEventProducer(
            KafkaTemplate<String, OrderCompletedEvent> completedKafkaTemplate,
            KafkaTemplate<String, OrderReadyEvent> readyKafkaTemplate) {
        this.completedKafkaTemplate = completedKafkaTemplate;
        this.readyKafkaTemplate = readyKafkaTemplate;
    }

    public void publishOrderCompleted(Long orderId, Long userId) {
        OrderCompletedEvent event = new OrderCompletedEvent(orderId, userId, java.time.Instant.now());
        completedKafkaTemplate.send(COMPLETED_TOPIC, orderId.toString(), event);
        log.info("Published order.completed for orderId={}", orderId);
    }

    public void publishOrderReady(Long orderId, Long tableId, String customerEmail) {
        OrderReadyEvent event = new OrderReadyEvent(orderId, tableId, customerEmail, java.time.Instant.now());
        readyKafkaTemplate.send(READY_TOPIC, orderId.toString(), event);
        log.info("Published order.ready for orderId={} email={}", orderId, customerEmail);
    }
}

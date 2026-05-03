package com.restaurant.reservations.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationEventProducer {

    private static final String TOPIC = "reservation";
    private final KafkaTemplate<String, ReservationEvent> kafkaTemplate;

    public void publish(ReservationEvent event) {
        kafkaTemplate.send(TOPIC, event.eventType(), event);
        log.info("Published reservation event type={} to={}", event.eventType(), event.customerEmail());
    }
}

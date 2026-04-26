package com.restaurant.feedback.kafka;

import com.restaurant.feedback.model.CompletableOrder;
import com.restaurant.feedback.repository.CompletableOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCompletedConsumer {

    private final CompletableOrderRepository completableOrderRepository;

    @KafkaListener(topics = "order.completed", groupId = "feedback-service-group")
    public void consume(OrderCompletedEvent event) {
        completableOrderRepository.save(
                new CompletableOrder(event.orderId(), event.userId(), event.completedAt()));
        log.info("Stored completable order {}", event.orderId());
    }
}

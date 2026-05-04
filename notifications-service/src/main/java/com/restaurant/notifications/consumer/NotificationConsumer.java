package com.restaurant.notifications.consumer;

import com.restaurant.notifications.kafka.OrderReadyEvent;
import com.restaurant.notifications.kafka.ReservationEvent;
import com.restaurant.notifications.kafka.VoucherIssuedEvent;
import com.restaurant.notifications.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final EmailService emailService;

    @KafkaListener(topics = "reservation", groupId = "notifications-group", containerFactory = "reservationKafkaListenerContainerFactory")
    public void handleReservationEvent(ReservationEvent event) {
        log.info("Received reservation event type={} for {}", event.eventType(), event.customerEmail());
        if ("CONFIRMED".equals(event.eventType())) {
            emailService.sendReservationConfirmed(event);
        } else if ("CANCELLED".equals(event.eventType())) {
            emailService.sendReservationCancelled(event);
        }
    }

    @KafkaListener(topics = "order.ready", groupId = "notifications-group", containerFactory = "orderReadyKafkaListenerContainerFactory")
    public void handleOrderReadyEvent(OrderReadyEvent event) {
        log.info("Received order.ready event for orderId={}", event.orderId());
        emailService.sendOrderReady(event);
    }

    @KafkaListener(topics = "voucher.issued", groupId = "notifications-group", containerFactory = "voucherIssuedKafkaListenerContainerFactory")
    public void handleVoucherIssuedEvent(VoucherIssuedEvent event) {
        log.info("Received voucher.issued event code={} email={}", event.code(), event.userEmail());
        emailService.sendVoucherIssued(event);
    }
}

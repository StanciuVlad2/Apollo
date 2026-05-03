package com.restaurant.reservations.kafka;

import java.time.LocalDate;
import java.time.LocalTime;

public record ReservationEvent(
    String eventType,
    String customerEmail,
    String customerName,
    Integer partySize,
    LocalDate reservationDate,
    LocalTime startTime,
    LocalTime endTime,
    String cancelReason
) {}

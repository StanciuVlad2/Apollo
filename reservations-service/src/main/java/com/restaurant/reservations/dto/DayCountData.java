package com.restaurant.reservations.dto;

import java.time.LocalDate;

public record DayCountData(
        LocalDate date,
        long count,
        double avgPartySize
) {}

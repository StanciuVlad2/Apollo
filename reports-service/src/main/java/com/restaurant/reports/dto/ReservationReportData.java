package com.restaurant.reports.dto;

import java.util.List;

public record ReservationReportData(
        long totalReservations,
        double avgPartySize,
        List<DayCountData> byDay
) {}

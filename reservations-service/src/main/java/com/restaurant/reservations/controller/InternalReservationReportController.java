package com.restaurant.reservations.controller;

import com.restaurant.reservations.dto.ReservationReportData;
import com.restaurant.reservations.service.ReservationReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/internal/reservations")
@RequiredArgsConstructor
public class InternalReservationReportController {

    private final ReservationReportService reservationReportService;

    @GetMapping("/report")
    public ReservationReportData getReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reservationReportService.generateReport(from, to);
    }
}

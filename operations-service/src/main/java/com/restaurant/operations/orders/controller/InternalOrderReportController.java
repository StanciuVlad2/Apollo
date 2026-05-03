package com.restaurant.operations.orders.controller;

import com.restaurant.operations.orders.dto.OrderReportData;
import com.restaurant.operations.orders.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/internal/orders")
@RequiredArgsConstructor
public class InternalOrderReportController {

    private final OrderService orderService;

    @GetMapping("/report")
    public OrderReportData getReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return orderService.generateReport(from, to);
    }
}

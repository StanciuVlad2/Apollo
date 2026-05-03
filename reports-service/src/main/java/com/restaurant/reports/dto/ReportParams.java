package com.restaurant.reports.dto;

import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

public record ReportParams(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        Preset preset
) {}

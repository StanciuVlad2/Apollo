package com.restaurant.reports.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

public record PeriodInfo(
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate from,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate to
) {}

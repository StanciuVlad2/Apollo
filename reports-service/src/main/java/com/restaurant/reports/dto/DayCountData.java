package com.restaurant.reports.dto;

import java.time.LocalDate;

public record DayCountData(LocalDate date, long count, double avgPartySize) {}

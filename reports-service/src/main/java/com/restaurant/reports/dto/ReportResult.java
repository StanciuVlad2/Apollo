package com.restaurant.reports.dto;

import java.util.List;

public record ReportResult(
        String id,
        String label,
        PeriodInfo period,
        List<ReportSection> sections
) {}

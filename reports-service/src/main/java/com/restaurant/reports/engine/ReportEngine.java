package com.restaurant.reports.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.reports.dto.ReportDefinition;
import com.restaurant.reports.dto.ReportParams;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;

@Component
public class ReportEngine {

    private final ObjectMapper objectMapper;

    public ReportEngine(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ReportDefinition loadDefinition(String reportType) {
        ClassPathResource resource = new ClassPathResource("reports/" + reportType + ".json");
        if (!resource.exists()) {
            throw new IllegalArgumentException("Report type not found: " + reportType);
        }
        try {
            return objectMapper.readValue(resource.getInputStream(), ReportDefinition.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load report definition: " + reportType, e);
        }
    }

    public LocalDate[] resolveDates(ReportParams params) {
        if (params.preset() != null) {
            LocalDate today = LocalDate.now();
            return switch (params.preset()) {
                case LAST_WEEK -> new LocalDate[]{today.minusWeeks(1), today};
                case LAST_MONTH -> new LocalDate[]{today.minusMonths(1), today};
                case LAST_THREE_MONTHS -> new LocalDate[]{today.minusMonths(3), today};
            };
        }
        if (params.from() != null && params.to() != null) {
            return new LocalDate[]{params.from(), params.to()};
        }
        throw new IllegalArgumentException("Either preset or from/to dates must be provided");
    }
}

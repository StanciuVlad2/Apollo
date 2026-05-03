package com.restaurant.reports.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReportSection(
        String type,
        String title,
        Map<String, Object> data,
        List<String> columns,
        List<List<Object>> rows
) {
    public static ReportSection summary(Map<String, Object> data) {
        return new ReportSection("SUMMARY", null, data, null, null);
    }

    public static ReportSection table(String title, List<String> columns, List<List<Object>> rows) {
        return new ReportSection("TABLE", title, null, columns, rows);
    }
}

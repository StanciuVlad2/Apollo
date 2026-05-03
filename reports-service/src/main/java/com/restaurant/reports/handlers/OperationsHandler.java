package com.restaurant.reports.handlers;

import com.restaurant.reports.dto.*;
import com.restaurant.reports.feign.OperationsClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class OperationsHandler {

    private final OperationsClient operationsClient;

    public List<ReportSection> buildSections(List<SectionDefinition> sectionDefs, LocalDate from, LocalDate to) {
        OrderReportData data = operationsClient.getOrderReport(from, to);
        List<ReportSection> result = new ArrayList<>();

        Map<String, Object> summaryAll = new LinkedHashMap<>();
        summaryAll.put("totalOrders", data.totalOrders());
        summaryAll.put("completedOrders", data.completedOrders());
        summaryAll.put("cancelledOrders", data.cancelledOrders());
        summaryAll.put("totalRevenue", data.totalRevenue());
        summaryAll.put("avgOrderValue", data.avgOrderValue());

        for (SectionDefinition def : sectionDefs) {
            if ("SUMMARY".equals(def.getType())) {
                Map<String, Object> filtered = new LinkedHashMap<>();
                for (String field : def.getFields()) {
                    if (summaryAll.containsKey(field)) {
                        filtered.put(field, summaryAll.get(field));
                    }
                }
                result.add(ReportSection.summary(filtered));
            } else if ("TABLE".equals(def.getType())) {
                int limit = def.getLimit() != null ? def.getLimit() : Integer.MAX_VALUE;
                Stream<TopItemData> stream = data.topItems().stream().limit(limit);
                List<List<Object>> rows = stream
                        .map(item -> buildRow(item, def.getFields()))
                        .toList();
                result.add(ReportSection.table(def.getTitle(), def.getFields(), rows));
            }
        }
        return result;
    }

    private List<Object> buildRow(TopItemData item, List<String> fields) {
        Map<String, Object> rowMap = new LinkedHashMap<>();
        rowMap.put("itemName", item.itemName());
        rowMap.put("quantitySold", item.quantitySold());
        rowMap.put("revenue", item.revenue());
        return fields.stream().map(f -> rowMap.getOrDefault(f, "")).toList();
    }
}

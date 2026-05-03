package com.restaurant.reports.handlers;

import com.restaurant.reports.dto.*;
import com.restaurant.reports.feign.ReservationsClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class ReservationsHandler {

    private final ReservationsClient reservationsClient;

    public List<ReportSection> buildSections(List<SectionDefinition> sectionDefs, LocalDate from, LocalDate to) {
        ReservationReportData data = reservationsClient.getReservationReport(from, to);
        List<ReportSection> result = new ArrayList<>();

        Map<String, Object> summaryAll = new LinkedHashMap<>();
        summaryAll.put("totalReservations", data.totalReservations());
        summaryAll.put("avgPartySize", data.avgPartySize());

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
                Stream<DayCountData> stream = data.byDay().stream().limit(limit);
                List<List<Object>> rows = stream
                        .map(day -> buildRow(day, def.getFields()))
                        .toList();
                result.add(ReportSection.table(def.getTitle(), def.getFields(), rows));
            }
        }
        return result;
    }

    private List<Object> buildRow(DayCountData day, List<String> fields) {
        Map<String, Object> rowMap = new LinkedHashMap<>();
        rowMap.put("date", day.date().toString());
        rowMap.put("count", day.count());
        rowMap.put("avgPartySize", day.avgPartySize());
        return fields.stream().map(f -> rowMap.getOrDefault(f, "")).toList();
    }
}

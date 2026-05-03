package com.restaurant.reports.controller;

import com.restaurant.reports.dto.*;
import com.restaurant.reports.engine.ReportEngine;
import com.restaurant.reports.handlers.OperationsHandler;
import com.restaurant.reports.handlers.ReservationsHandler;
import com.restaurant.reports.pdf.PdfGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportEngine reportEngine;
    private final OperationsHandler operationsHandler;
    private final ReservationsHandler reservationsHandler;
    private final PdfGenerator pdfGenerator;

    @GetMapping(value = "/{reportType}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ReportResult getReport(
            @PathVariable String reportType,
            ReportParams params) {
        return buildReport(reportType, params);
    }

    @GetMapping("/{reportType}/pdf")
    public ResponseEntity<byte[]> getReportPdf(
            @PathVariable String reportType,
            ReportParams params) {
        ReportResult report = buildReport(reportType, params);
        byte[] pdf = pdfGenerator.generate(report);
        String filename = reportType + "-" + report.period().from() + "-" + report.period().to() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private ReportResult buildReport(String reportType, ReportParams params) {
        ReportDefinition def = reportEngine.loadDefinition(reportType);
        LocalDate[] dates = reportEngine.resolveDates(params);
        LocalDate from = dates[0];
        LocalDate to = dates[1];

        List<ReportSection> sections = switch (def.getDataSource()) {
            case "OPERATIONS" -> operationsHandler.buildSections(def.getSections(), from, to);
            case "RESERVATIONS" -> reservationsHandler.buildSections(def.getSections(), from, to);
            default -> throw new IllegalArgumentException("Unknown dataSource: " + def.getDataSource());
        };

        return new ReportResult(def.getId(), def.getLabel(), new PeriodInfo(from, to), sections);
    }
}

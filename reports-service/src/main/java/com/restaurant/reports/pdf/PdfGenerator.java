package com.restaurant.reports.pdf;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.restaurant.reports.dto.ReportResult;
import com.restaurant.reports.dto.ReportSection;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@Component
public class PdfGenerator {

    public byte[] generate(ReportResult report) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfDocument pdfDoc = new PdfDocument(new PdfWriter(baos));
        Document document = new Document(pdfDoc);

        document.add(new Paragraph(report.label())
                .setFontSize(18).setBold().setTextAlignment(TextAlignment.CENTER));
        document.add(new Paragraph("Perioadă: " + report.period().from() + " — " + report.period().to())
                .setFontSize(11).setTextAlignment(TextAlignment.CENTER).setMarginBottom(20));

        for (ReportSection section : report.sections()) {
            if ("SUMMARY".equals(section.type())) {
                renderSummary(document, section.data());
            } else if ("TABLE".equals(section.type())) {
                if (section.title() != null) {
                    document.add(new Paragraph(section.title()).setBold().setFontSize(13).setMarginTop(15));
                }
                renderTable(document, section);
            }
        }

        document.close();
        return baos.toByteArray();
    }

    private void renderSummary(Document document, Map<String, Object> data) {
        Table table = new Table(new float[]{1, 1}).setWidth(UnitValue.createPercentValue(60));
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            table.addCell(new Cell().add(new Paragraph(entry.getKey()).setBold()));
            table.addCell(new Cell().add(new Paragraph(String.valueOf(entry.getValue()))));
        }
        document.add(table);
    }

    private void renderTable(Document document, ReportSection section) {
        if (section.columns() == null || section.rows() == null) return;
        int numCols = section.columns().size();
        Table table = new Table(numCols).setWidth(UnitValue.createPercentValue(100));

        for (String col : section.columns()) {
            table.addHeaderCell(new Cell().add(new Paragraph(col).setBold())
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY));
        }
        for (List<Object> row : section.rows()) {
            for (Object cell : row) {
                table.addCell(new Cell().add(new Paragraph(String.valueOf(cell))));
            }
        }
        document.add(table);
    }
}

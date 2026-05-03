package com.restaurant.reports.handlers;

import com.restaurant.reports.dto.*;
import com.restaurant.reports.feign.OperationsClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationsHandlerTest {

    @Mock
    private OperationsClient operationsClient;

    private OperationsHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OperationsHandler(operationsClient);
    }

    @Test
    void buildsSummarySectionWithRequestedFields() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 1, 31);
        OrderReportData data = new OrderReportData(100, 90, 10, 5000.0, 50.0,
                List.of(new TopItemData("Pizza", 30, 600.0)));

        when(operationsClient.getOrderReport(from, to)).thenReturn(data);

        SectionDefinition def = new SectionDefinition();
        def.setType("SUMMARY");
        def.setFields(List.of("totalOrders", "totalRevenue"));

        List<ReportSection> sections = handler.buildSections(List.of(def), from, to);

        assertThat(sections).hasSize(1);
        assertThat(sections.get(0).type()).isEqualTo("SUMMARY");
        assertThat(sections.get(0).data()).containsKeys("totalOrders", "totalRevenue");
        assertThat(sections.get(0).data()).doesNotContainKey("avgOrderValue");
    }

    @Test
    void buildsTableSectionWithTopItems() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 1, 31);
        List<TopItemData> items = List.of(
                new TopItemData("Pizza", 30, 600.0),
                new TopItemData("Pasta", 20, 300.0)
        );
        OrderReportData data = new OrderReportData(50, 45, 5, 900.0, 18.0, items);

        when(operationsClient.getOrderReport(from, to)).thenReturn(data);

        SectionDefinition def = new SectionDefinition();
        def.setType("TABLE");
        def.setTitle("Top Produse");
        def.setFields(List.of("itemName", "quantitySold", "revenue"));
        def.setLimit(10);

        List<ReportSection> sections = handler.buildSections(List.of(def), from, to);

        assertThat(sections).hasSize(1);
        assertThat(sections.get(0).type()).isEqualTo("TABLE");
        assertThat(sections.get(0).columns()).containsExactly("itemName", "quantitySold", "revenue");
        assertThat(sections.get(0).rows()).hasSize(2);
        assertThat(sections.get(0).rows().get(0)).containsExactly("Pizza", 30L, 600.0);
    }

    @Test
    void limitsTableRows() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 1, 31);
        List<TopItemData> items = List.of(
                new TopItemData("A", 10, 100.0),
                new TopItemData("B", 9, 90.0),
                new TopItemData("C", 8, 80.0)
        );
        OrderReportData data = new OrderReportData(30, 28, 2, 270.0, 9.0, items);
        when(operationsClient.getOrderReport(from, to)).thenReturn(data);

        SectionDefinition def = new SectionDefinition();
        def.setType("TABLE");
        def.setFields(List.of("itemName", "quantitySold", "revenue"));
        def.setLimit(2);

        List<ReportSection> sections = handler.buildSections(List.of(def), from, to);
        assertThat(sections.get(0).rows()).hasSize(2);
    }
}

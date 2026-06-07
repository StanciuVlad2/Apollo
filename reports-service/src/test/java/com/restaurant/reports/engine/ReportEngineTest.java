package com.restaurant.reports.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.restaurant.reports.dto.Preset;
import com.restaurant.reports.dto.ReportDefinition;
import com.restaurant.reports.dto.ReportParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportEngineTest {

    private ReportEngine engine;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        engine = new ReportEngine(mapper);
    }

    @Test
    void loadsDefinitionByType() {
        ReportDefinition def = engine.loadDefinition("revenue");
        assertThat(def.getId()).isEqualTo("revenue");
        assertThat(def.getDataSource()).isEqualTo("OPERATIONS");
        assertThat(def.getSections()).hasSize(2);
    }

    @Test
    void throwsForUnknownReportType() {
        assertThatThrownBy(() -> engine.loadDefinition("nonexistent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nonexistent");
    }

    @Test
    void resolvesPresetLastWeek() {
        ReportParams params = new ReportParams(null, null, Preset.LAST_WEEK);
        LocalDate[] range = engine.resolveDates(params);
        LocalDate today = LocalDate.now();
        assertThat(range[0]).isEqualTo(today.minusWeeks(1));
        assertThat(range[1]).isEqualTo(today);
    }

    @Test
    void resolvesPresetLastMonth() {
        ReportParams params = new ReportParams(null, null, Preset.LAST_MONTH);
        LocalDate[] range = engine.resolveDates(params);
        LocalDate today = LocalDate.now();
        assertThat(range[0]).isEqualTo(today.minusMonths(1));
        assertThat(range[1]).isEqualTo(today);
    }

    @Test
    void resolvesPresetLastThreeMonths() {
        ReportParams params = new ReportParams(null, null, Preset.LAST_THREE_MONTHS);
        LocalDate[] range = engine.resolveDates(params);
        LocalDate today = LocalDate.now();
        assertThat(range[0]).isEqualTo(today.minusMonths(3));
        assertThat(range[1]).isEqualTo(today);
    }

    @Test
    void resolvesCustomDates() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 1, 31);
        ReportParams params = new ReportParams(from, to, null);
        LocalDate[] range = engine.resolveDates(params);
        assertThat(range[0]).isEqualTo(from);
        assertThat(range[1]).isEqualTo(to);
    }

    @Test
    void throwsWhenNeitherPresetNorDatesProvided() {
        ReportParams params = new ReportParams(null, null, null);
        assertThatThrownBy(() -> engine.resolveDates(params))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

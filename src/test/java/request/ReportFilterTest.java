// ReportFilterTest.java
package com.meethub.domain.model.request;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ReportFilterTest {

    @Test
    void testBuilderWithAllFields() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = now.plusDays(1);

        ReportFilter filter = ReportFilter.builder()
                .dateFrom(now)
                .dateTo(tomorrow)
                .minParticipants(5)
                .maxParticipants(20)
                .minAttendanceRate(new BigDecimal("0.5"))
                .maxAttendanceRate(new BigDecimal("1.0"))
                .sortBy("date")
                .sortOrder("asc")
                .build();

        assertAll("ReportFilter builder test",
                () -> assertEquals(now, filter.getDateFrom(), "Date from should match"),
                () -> assertEquals(tomorrow, filter.getDateTo(), "Date to should match"),
                () -> assertEquals(5, filter.getMinParticipants(), "Min participants should be 5"),
                () -> assertEquals(20, filter.getMaxParticipants(), "Max participants should be 20"),
                () -> assertEquals(new BigDecimal("0.5"), filter.getMinAttendanceRate(),
                        "Min attendance rate should be 0.5"),
                () -> assertEquals(new BigDecimal("1.0"), filter.getMaxAttendanceRate(),
                        "Max attendance rate should be 1.0"),
                () -> assertEquals("date", filter.getSortBy(), "Sort by should be 'date'"),
                () -> assertEquals("asc", filter.getSortOrder(), "Sort order should be 'asc'")
        );
    }

    @Test
    void testDateFilterMethods() {
        LocalDateTime date = LocalDateTime.of(2024, 1, 15, 10, 0);

        ReportFilter filter1 = new ReportFilter();
        ReportFilter filter2 = ReportFilter.builder().dateFrom(date).build();
        ReportFilter filter3 = ReportFilter.builder().dateTo(date).build();
        ReportFilter filter4 = ReportFilter.builder()
                .dateFrom(date)
                .dateTo(date.plusDays(1))
                .build();
        ReportFilter filter5 = ReportFilter.builder()
                .dateFrom(date.plusDays(1))
                .dateTo(date)
                .build();

        assertAll("Date filter validation",
                () -> assertFalse(filter1.hasDateFilter(),
                        "Empty filter should not have date filter"),
                () -> assertTrue(filter2.hasDateFilter(),
                        "Filter with dateFrom should have date filter"),
                () -> assertTrue(filter3.hasDateFilter(),
                        "Filter with dateTo should have date filter"),
                () -> assertTrue(filter4.hasDateFilter(),
                        "Filter with both dates should have date filter"),
                () -> assertTrue(filter4.isDateRangeValid(),
                        "Valid date range should pass validation"),
                () -> assertFalse(filter5.isDateRangeValid(),
                        "Invalid date range should fail validation")
        );
    }

    @Test
    void testEqualityAndHashCode() {
        LocalDateTime date = LocalDateTime.now();
        Integer participants = 10;

        ReportFilter filter1 = ReportFilter.builder()
                .dateFrom(date)
                .minParticipants(participants)
                .maxAttendanceRate(new BigDecimal("0.8"))
                .build();

        ReportFilter filter2 = ReportFilter.builder()
                .dateFrom(date)
                .minParticipants(participants)
                .maxAttendanceRate(new BigDecimal("0.8"))
                .build();

        ReportFilter filter3 = ReportFilter.builder()
                .dateFrom(date.plusDays(1))
                .minParticipants(15)
                .build();

        assertAll("Equality tests",
                () -> assertEquals(filter1, filter2,
                        "Filters with same values should be equal"),
                () -> assertEquals(filter1.hashCode(), filter2.hashCode(),
                        "Equal filters should have same hash code"),
                () -> assertNotEquals(filter1, filter3,
                        "Filters with different values should not be equal"),
                () -> assertNotEquals(filter1.hashCode(), filter3.hashCode(),
                        "Different filters should have different hash codes"),
                () -> assertNotNull(filter1.toString(),
                        "toString should not return null"),
                () -> assertNotNull(filter2.toString(),
                        "toString should not return null for second instance")
        );
    }
}
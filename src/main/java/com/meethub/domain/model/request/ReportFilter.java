package com.meethub.domain.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportFilter {

    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;

    private Boolean includePastMeetings;
    private Boolean includeUpcomingMeetings;
    private Boolean includeCancelledMeetings;

    private Integer minParticipants;
    private Integer maxParticipants;

    private BigDecimal minAttendanceRate;
    private BigDecimal maxAttendanceRate;

    private String sortBy;
    private String sortOrder;

    // Metody pomocnicze
    public boolean hasDateFilter() {
        return dateFrom != null || dateTo != null;
    }

    public boolean isDateRangeValid() {
        if (dateFrom == null || dateTo == null) {
            return true;
        }
        return !dateFrom.isAfter(dateTo);
    }
}
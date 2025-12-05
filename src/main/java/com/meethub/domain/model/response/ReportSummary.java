// ReportSummary.java - ZMIEŃ z Double na BigDecimal
package com.meethub.domain.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;  // DODAJ

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ReportSummary {
    private Integer totalMeetings;
    private Integer totalParticipants;
    private BigDecimal avgAttendanceRate;    // ZMIENIONE z Double
    private BigDecimal avgEngagementScore;   // ZMIENIONE z Double

    public static ReportSummary empty() {
        return ReportSummary.builder()
                .totalMeetings(0)
                .totalParticipants(0)
                .avgAttendanceRate(BigDecimal.ZERO)    // ZMIENIONE
                .avgEngagementScore(BigDecimal.ZERO)   // ZMIENIONE
                .build();
    }
}
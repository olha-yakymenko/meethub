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
    private BigDecimal avgAttendanceRate;
    private BigDecimal avgEngagementScore;

    public static ReportSummary empty() {
        return ReportSummary.builder()
                .totalMeetings(0)
                .totalParticipants(0)
                .avgAttendanceRate(BigDecimal.ZERO)
                .avgEngagementScore(BigDecimal.ZERO)
                .build();
    }
}
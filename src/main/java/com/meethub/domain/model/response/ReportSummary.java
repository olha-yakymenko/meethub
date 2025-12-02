package com.meethub.domain.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ReportSummary {
    private Integer totalMeetings;
    private Integer totalParticipants;
    private Double avgAttendanceRate;
    private Double avgEngagementScore;

    public static ReportSummary empty() {
        return ReportSummary.builder()
                .totalMeetings(0)
                .totalParticipants(0)
                .avgAttendanceRate(0.0)
                .avgEngagementScore(0.0)
                .build();
    }
}

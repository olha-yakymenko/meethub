package com.meethub.domain.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrganizerReport {
    private Long organizerId;
    private LocalDateTime generatedAt;
    private ReportSummary summary;
    private List<MonthlyTrend> monthlyTrends;
}


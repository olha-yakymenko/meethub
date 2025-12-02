package com.meethub.domain.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MonthlyTrend {
    private String monthName;
    private Integer meetingsCount;
    private Double avgAttendance;
}

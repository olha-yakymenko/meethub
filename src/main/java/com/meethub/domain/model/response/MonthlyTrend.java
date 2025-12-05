// MonthlyTrend.java - ZMIEŃ z Double na BigDecimal
package com.meethub.domain.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;  // DODAJ

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MonthlyTrend {
    private String monthName;
    private Integer meetingsCount;
    private BigDecimal avgAttendance;  // ZMIENIONE z Double
}
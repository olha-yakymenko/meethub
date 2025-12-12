package com.meethub.domain.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantCountDto {
    private Long total;
    private Long confirmed;
    private Long attended;
    private Long declined;
    private Long cancelled;
    private Long invited;
    private Long pending;



    public BigDecimal getAttendanceRate() {
        return calculateRate(attended, total);
    }

    public BigDecimal getConfirmationRate() {
        return calculateRate(confirmed, total);
    }

    private BigDecimal calculateRate(Long part, Long total) {
        if (total == null || total == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(part)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }
}
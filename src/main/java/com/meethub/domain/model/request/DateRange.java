package com.meethub.domain.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 📅 Zakres dat
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DateRange {
    private LocalDateTime from;
    private LocalDateTime to;

    public boolean isWithinRange(LocalDateTime date) {
        if (from != null && date.isBefore(from)) {
            return false;
        }
        if (to != null && date.isAfter(to)) {
            return false;
        }
        return true;
    }
}

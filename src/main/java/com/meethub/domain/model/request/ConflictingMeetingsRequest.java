package com.meethub.domain.model.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Future;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConflictingMeetingsRequest {

    @NotNull(message = "Data rozpoczęcia nie może być pusta")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDate;

    @NotNull(message = "Data zakończenia nie może być pusta")
    @Future(message = "Data zakończenia musi być w przyszłości")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDate;
}

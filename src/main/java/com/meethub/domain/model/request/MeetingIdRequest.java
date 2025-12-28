package com.meethub.domain.model.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MeetingIdRequest {
    @NotNull(message = "Identyfikator spotkania nie może być pusty")
    @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
    private Long meetingId;
}
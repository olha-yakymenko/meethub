package com.meethub.domain.model.request;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class MeetingRequest {

        @Parameter(description = "ID spotkania", required = true)
        @NotNull(message = "Identyfikator spotkania nie może być pusty")
        @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
        private Long meetingId;

        public void setMeetingId(Long meetingId) {
            this.meetingId = meetingId;
        }

}

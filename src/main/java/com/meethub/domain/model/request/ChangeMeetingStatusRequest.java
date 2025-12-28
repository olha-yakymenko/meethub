package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.MeetingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeMeetingStatusRequest {

    @NotNull(message = "Status nie może być pusty")
    private MeetingStatus status;
}

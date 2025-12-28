package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.ParticipationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateParticipantStatusRequest {

    @NotNull(message = "Status nie może być pusty")
    private ParticipationStatus status;

    @Size(max = 500, message = "Komentarz nie może przekraczać 500 znaków")
    private String comment;
}

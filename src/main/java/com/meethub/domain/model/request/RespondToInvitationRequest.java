package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.ParticipationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RespondToInvitationRequest {

    @NotNull(message = "Odpowiedź nie może być pusta")
    private ParticipationStatus response;

    @Size(max = 500, message = "Komentarz nie może przekraczać 500 znaków")
    private String comment;
}

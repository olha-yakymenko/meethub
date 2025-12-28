package com.meethub.domain.model.request;

import jakarta.validation.constraints.Size;

public record ParticipationRequest(
        @Size(max = 500, message = "Powód nie może przekraczać 500 znaków")
        String reason
) {}

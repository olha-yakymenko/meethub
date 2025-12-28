package com.meethub.domain.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VirtualLocationRequest {

    @NotBlank(message = "Platforma nie może być pusta")
    @Size(min = 1, max = 50, message = "Platforma musi mieć od 1 do 50 znaków")
    private String platform;

    @NotBlank(message = "Identyfikator spotkania nie może być pusty")
    @Size(min = 1, max = 100, message = "Identyfikator spotkania musi mieć od 1 do 100 znaków")
    private String meetingId;

    @Size(max = 50, message = "Kod dostępu nie może przekraczać 50 znaków")
    private String passcode;
}

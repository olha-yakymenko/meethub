package com.meethub.domain.model.request;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;

@Data
public class UpdateProfileRequest {
    @NotBlank(message = "Imię jest wymagane")
    private String firstName;

    @NotBlank(message = "Nazwisko jest wymagane")
    private String lastName;

    @Email(message = "Nieprawidłowy format email")
    private String email;

    private String phoneNumber;
    private String timezone;
    private String language;
}

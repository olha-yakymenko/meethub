package com.meethub.domain.model.request;

import com.meethub.validation.MeethubEmail;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationRequest {

    @NotBlank(message = "Imię jest wymagane")
    @Size(max = 50, message = "Imię nie może przekraczać 50 znaków")
    private String firstName;

    @NotBlank(message = "Nazwisko jest wymagane")
    @Size(max = 50, message = "Nazwisko nie może przekraczać 50 znaków")
    private String lastName;

    @Email(message = "Nieprawidłowy format email")
    @MeethubEmail(message = "Email musi być w domenie .com")
    @NotBlank(message = "Email jest wymagany")
    private String email;

    @NotBlank(message = "Hasło jest wymagane")
    @Size(min = 8, message = "Hasło musi mieć co najmniej 8 znaków")
    private String password;

    @NotBlank(message = "Potwierdzenie hasła jest wymagane")
    private String confirmPassword;

    @Size(max = 20, message = "Numer telefonu nie może przekraczać 20 znaków")
    private String phoneNumber;
}
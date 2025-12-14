package com.meethub.domain.model.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserRegistrationRequestTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldCreateValidRegistrationRequest() {
        // Given
        var request = UserRegistrationRequest.builder()
                .firstName("Jan")
                .lastName("Kowalski")
                .email("jan.kowalski@example.com")
                .password("SecurePass123!")
                .confirmPassword("SecurePass123!")
                .phoneNumber("+48 123 456 789")
                .build();

        // When
        var violations = validator.validate(request);

        // Then
        assertAll(
                () -> assertTrue(violations.isEmpty()),
                () -> assertEquals("Jan", request.getFirstName()),
                () -> assertEquals("Kowalski", request.getLastName()),
                () -> assertEquals("jan.kowalski@example.com", request.getEmail()),
                () -> assertEquals("SecurePass123!", request.getPassword()),
                () -> assertEquals("SecurePass123!", request.getConfirmPassword()),
                () -> assertEquals("+48 123 456 789", request.getPhoneNumber())
        );
    }

    @Test
    void shouldFailWhenPasswordTooShort() {
        // Given
        var request = UserRegistrationRequest.builder()
                .firstName("Jan")
                .lastName("Kowalski")
                .email("jan@example.com")
                .password("short")
                .confirmPassword("short")
                .build();

        // When
        var violations = validator.validate(request);

        // Then
        assertAll(
                () -> assertEquals(1, violations.size()),
                () -> assertEquals("Hasło musi mieć co najmniej 8 znaków",
                        violations.iterator().next().getMessage()),
                () -> assertEquals("short", request.getPassword())
        );
    }
}
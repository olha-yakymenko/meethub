package com.meethub.domain.model.request;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import static org.junit.jupiter.api.Assertions.*;

class UpdateProfileRequestTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldCreateValidRequest() {
        // Given
        var request = new UpdateProfileRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john.doe@example.com");
        request.setPhoneNumber("+48 123 456 789");
        request.setTimezone("Europe/Warsaw");
        request.setLanguage("pl");

        // When
        var violations = validator.validate(request);

        // Then
        assertAll(
                () -> assertTrue(violations.isEmpty()),
                () -> assertEquals("John", request.getFirstName()),
                () -> assertEquals("Doe", request.getLastName()),
                () -> assertEquals("john.doe@example.com", request.getEmail()),
                () -> assertEquals("+48 123 456 789", request.getPhoneNumber()),
                () -> assertEquals("Europe/Warsaw", request.getTimezone()),
                () -> assertEquals("pl", request.getLanguage())
        );
    }

    @Test
    void shouldFailWhenRequiredFieldsAreMissing() {
        // Given
        var request = new UpdateProfileRequest();

        // When
        var violations = validator.validate(request);

        // Then
        assertAll(
                () -> assertEquals(3, violations.size()),
                () -> assertTrue(violations.stream()
                        .anyMatch(v -> v.getMessage().equals("First name is required"))),
                () -> assertTrue(violations.stream()
                        .anyMatch(v -> v.getMessage().equals("Last name is required"))),
                () -> assertTrue(violations.stream()
                        .anyMatch(v -> v.getMessage().equals("Email is required")))
        );
    }

}
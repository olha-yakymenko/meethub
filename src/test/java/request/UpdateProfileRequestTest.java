// UpdateProfileRequestTest.java
package com.meethub.domain.model.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UpdateProfileRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void testValidUpdateRequest() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Anna");
        request.setLastName("Kowalska");
        request.setEmail("anna.kowalska@example.com");
        request.setPhoneNumber("+48 987 654 321");
        request.setTimezone("Europe/Warsaw");
        request.setLanguage("pl");

        var violations = validator.validate(request);

        assertAll("Valid profile update request",
                () -> assertTrue(violations.isEmpty(),
                        "Valid request should have no violations"),
                () -> assertEquals("Anna", request.getFirstName(),
                        "First name should match"),
                () -> assertEquals("Kowalska", request.getLastName(),
                        "Last name should match"),
                () -> assertEquals("anna.kowalska@example.com", request.getEmail(),
                        "Email should match"),
                () -> assertEquals("+48 987 654 321", request.getPhoneNumber(),
                        "Phone number should match"),
                () -> assertEquals("Europe/Warsaw", request.getTimezone(),
                        "Timezone should match"),
                () -> assertEquals("pl", request.getLanguage(),
                        "Language should match")
        );
    }

    @Test
    void testValidationConstraints() {
        UpdateProfileRequest invalidEmail = new UpdateProfileRequest();
        invalidEmail.setFirstName("Valid");
        invalidEmail.setLastName("Valid");
        invalidEmail.setEmail("invalid-email");
        invalidEmail.setPhoneNumber("+48 123 456 789");

        UpdateProfileRequest invalidPhone = new UpdateProfileRequest();
        invalidPhone.setFirstName("Valid");
        invalidPhone.setLastName("Valid");
        invalidPhone.setEmail("valid@email.com");
        invalidPhone.setPhoneNumber("not-a-phone");

        UpdateProfileRequest invalidLanguage = new UpdateProfileRequest();
        invalidLanguage.setFirstName("Valid");
        invalidLanguage.setLastName("Valid");
        invalidLanguage.setEmail("valid@email.com");
        invalidLanguage.setLanguage("invalid language code");

        UpdateProfileRequest nullFields = new UpdateProfileRequest(); // All fields null

        var emailViolations = validator.validate(invalidEmail);
        var phoneViolations = validator.validate(invalidPhone);
        var languageViolations = validator.validate(invalidLanguage);
        var nullViolations = validator.validate(nullFields);

        assertAll("Constraint violations",
                () -> assertEquals(1, emailViolations.size(),
                        "Invalid email should have 1 violation"),
                () -> assertTrue(emailViolations.stream().anyMatch(v ->
                                v.getMessage().contains("Invalid email format")),
                        "Violation should mention email format"),

                () -> assertEquals(2, languageViolations.size(),
                        "Invalid language should have 2 violation"),
                () -> assertTrue(languageViolations.stream().anyMatch(v ->
                                v.getMessage().contains("Language code must be like")),
                        "Violation should mention language format"),

                () -> assertEquals(3, nullViolations.size(),
                        "Null required fields should have 3 violations"),
                () -> assertTrue(nullViolations.stream().anyMatch(v ->
                                v.getMessage().contains("First name is required")),
                        "Should have first name violation"),
                () -> assertTrue(nullViolations.stream().anyMatch(v ->
                                v.getMessage().contains("Last name is required")),
                        "Should have last name violation"),
                () -> assertTrue(nullViolations.stream().anyMatch(v ->
                                v.getMessage().contains("Email is required")),
                        "Should have email violation")
        );
    }
}
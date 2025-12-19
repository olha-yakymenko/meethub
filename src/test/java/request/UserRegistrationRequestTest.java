// UserRegistrationRequestTest.java
package com.meethub.domain.model.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserRegistrationRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void testCompleteValidRegistration() {
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .firstName("Jan")
                .lastName("Kowalski")
                .email("jan.kowalski@example.com")
                .password("SecurePass123!")
                .confirmPassword("SecurePass123!")
                .phoneNumber("+48 123 456 789")
                .build();

        var violations = validator.validate(request);

        assertAll("Complete valid registration",
                () -> assertTrue(violations.isEmpty(),
                        "Valid request should have no violations"),
                () -> assertEquals("Jan", request.getFirstName(),
                        "First name should match"),
                () -> assertEquals("Kowalski", request.getLastName(),
                        "Last name should match"),
                () -> assertEquals("jan.kowalski@example.com", request.getEmail(),
                        "Email should match"),
                () -> assertEquals("SecurePass123!", request.getPassword(),
                        "Password should match"),
                () -> assertEquals("SecurePass123!", request.getConfirmPassword(),
                        "Confirm password should match"),
                () -> assertEquals("+48 123 456 789", request.getPhoneNumber(),
                        "Phone number should match")
        );
    }

    @Test
    void testValidationConstraints() {
        UserRegistrationRequest nullFirstName = UserRegistrationRequest.builder()
                .lastName("Kowalski")
                .email("jan@example.com")
                .password("Password123")
                .confirmPassword("Password123")
                .build();

        UserRegistrationRequest invalidEmail = UserRegistrationRequest.builder()
                .firstName("Jan")
                .lastName("Kowalski")
                .email("invalid-email")
                .password("Password123")
                .confirmPassword("Password123")
                .build();

        UserRegistrationRequest shortPassword = UserRegistrationRequest.builder()
                .firstName("Jan")
                .lastName("Kowalski")
                .email("jan@example.com")
                .password("short") // Too short
                .confirmPassword("short")
                .build();

        UserRegistrationRequest mismatchedPasswords = UserRegistrationRequest.builder()
                .firstName("Jan")
                .lastName("Kowalski")
                .email("jan@example.com")
                .password("Password123")
                .confirmPassword("DifferentPassword456")
                .build();

        UserRegistrationRequest longPhone = UserRegistrationRequest.builder()
                .firstName("Jan")
                .lastName("Kowalski")
                .email("jan@example.com")
                .password("Password123")
                .confirmPassword("Password123")
                .phoneNumber("123456789012345678901") // 21 chars, max 20
                .build();

        var firstNameViolations = validator.validate(nullFirstName);
        var emailViolations = validator.validate(invalidEmail);
        var passwordViolations = validator.validate(shortPassword);
        var phoneViolations = validator.validate(longPhone);
        // Note: mismatched passwords is business logic, not validation constraint

        assertAll("Constraint violations",
                () -> assertEquals(1, firstNameViolations.size(),
                        "Null first name should have 1 violation"),
                () -> assertTrue(firstNameViolations.stream().anyMatch(v ->
                                v.getMessage().contains("Imię jest wymagane")),
                        "Violation should mention first name requirement"),

                () -> assertEquals(2, emailViolations.size(),
                        "Invalid email should have 2 violation"),
                () -> assertTrue(emailViolations.stream().anyMatch(v ->
                                v.getMessage().contains("Nieprawidłowy format email")),
                        "Violation should mention email format"),

                () -> assertEquals(1, passwordViolations.size(),
                        "Short password should have 1 violation"),
                () -> assertTrue(passwordViolations.stream().anyMatch(v ->
                                v.getMessage().contains("Hasło musi mieć co najmniej 8 znaków")),
                        "Violation should mention minimum password length"),

                () -> assertEquals(1, phoneViolations.size(),
                        "Long phone number should have 1 violation"),
                () -> assertTrue(phoneViolations.stream().anyMatch(v ->
                                v.getMessage().contains("Numer telefonu nie może przekraczać 20 znaków")),
                        "Violation should mention phone number length")
        );
    }

    @Test
    void testOptionalPhoneNumber() {
        UserRegistrationRequest withoutPhone = UserRegistrationRequest.builder()
                .firstName("Anna")
                .lastName("Nowak")
                .email("anna.nowak@example.com")
                .password("SecurePass123!")
                .confirmPassword("SecurePass123!")
                // phoneNumber is null/optional
                .build();

        var violations = validator.validate(withoutPhone);

        assertAll("Optional phone number",
                () -> assertTrue(violations.isEmpty(),
                        "Registration without phone number should be valid"),
                () -> assertNull(withoutPhone.getPhoneNumber(),
                        "Phone number should be null when not provided")
        );
    }

    @Test
    void testNameLengthConstraints() {
        UserRegistrationRequest longFirstName = UserRegistrationRequest.builder()
                .firstName("A".repeat(51)) // 51 chars, max is 50
                .lastName("Kowalski")
                .email("test@example.com")
                .password("Password123")
                .confirmPassword("Password123")
                .build();

        UserRegistrationRequest longLastName = UserRegistrationRequest.builder()
                .firstName("Jan")
                .lastName("A".repeat(51)) // 51 chars, max is 50
                .email("test@example.com")
                .password("Password123")
                .confirmPassword("Password123")
                .build();

        var firstNameViolations = validator.validate(longFirstName);
        var lastNameViolations = validator.validate(longLastName);

        assertAll("Name length constraints",
                () -> assertEquals(1, firstNameViolations.size(),
                        "Long first name should have 1 violation"),
                () -> assertTrue(firstNameViolations.stream().anyMatch(v ->
                                v.getMessage().contains("Imię nie może przekraczać 50 znaków")),
                        "Violation should mention first name max length"),

                () -> assertEquals(1, lastNameViolations.size(),
                        "Long last name should have 1 violation"),
                () -> assertTrue(lastNameViolations.stream().anyMatch(v ->
                                v.getMessage().contains("Nazwisko nie może przekraczać 50 znaków")),
                        "Violation should mention last name max length")
        );
    }

    @Test
    void testBuilderAndConstructors() {
        UserRegistrationRequest builderRequest = UserRegistrationRequest.builder()
                .firstName("Builder")
                .lastName("Test")
                .email("builder@test.com")
                .password("pass123")
                .confirmPassword("pass123")
                .phoneNumber("123456789")
                .build();

        UserRegistrationRequest allArgsConstructor = new UserRegistrationRequest(
                "Constructor", "Test", "constructor@test.com",
                "pass456", "pass456", "987654321"
        );

        UserRegistrationRequest noArgsConstructor = new UserRegistrationRequest();

        assertAll("Builder and constructors",
                () -> assertEquals("Builder", builderRequest.getFirstName(),
                        "Builder should set first name"),
                () -> assertEquals("Test", builderRequest.getLastName(),
                        "Builder should set last name"),
                () -> assertEquals("builder@test.com", builderRequest.getEmail(),
                        "Builder should set email"),
                () -> assertEquals("pass123", builderRequest.getPassword(),
                        "Builder should set password"),
                () -> assertEquals("pass123", builderRequest.getConfirmPassword(),
                        "Builder should set confirm password"),
                () -> assertEquals("123456789", builderRequest.getPhoneNumber(),
                        "Builder should set phone number"),

                () -> assertEquals("Constructor", allArgsConstructor.getFirstName(),
                        "All-args constructor should set first name"),
                () -> assertEquals("Test", allArgsConstructor.getLastName(),
                        "All-args constructor should set last name"),
                () -> assertEquals("constructor@test.com", allArgsConstructor.getEmail(),
                        "All-args constructor should set email"),
                () -> assertEquals("pass456", allArgsConstructor.getPassword(),
                        "All-args constructor should set password"),
                () -> assertEquals("pass456", allArgsConstructor.getConfirmPassword(),
                        "All-args constructor should set confirm password"),
                () -> assertEquals("987654321", allArgsConstructor.getPhoneNumber(),
                        "All-args constructor should set phone number"),

                () -> assertNull(noArgsConstructor.getFirstName(),
                        "No-args constructor should have null first name"),
                () -> assertNull(noArgsConstructor.getLastName(),
                        "No-args constructor should have null last name"),
                () -> assertNull(noArgsConstructor.getEmail(),
                        "No-args constructor should have null email"),
                () -> assertNull(noArgsConstructor.getPassword(),
                        "No-args constructor should have null password"),
                () -> assertNull(noArgsConstructor.getConfirmPassword(),
                        "No-args constructor should have null confirm password"),
                () -> assertNull(noArgsConstructor.getPhoneNumber(),
                        "No-args constructor should have null phone number")
        );
    }
}
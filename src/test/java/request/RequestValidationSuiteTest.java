package com.meethub.domain.model.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Request Validation Suite")
class RequestValidationSuiteTest {
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Nested
    @DisplayName("Login Request Tests")
    class LoginRequestTest {
        @Test
        @DisplayName("Should validate login request successfully")
        void shouldValidateLoginRequest() {
            // Given
            var request = new LoginRequest();
            request.setEmail("user@example.com");
            request.setPassword("password123");

            // When
            var violations = validator.validate(request);

            // Then
            assertAll(
                    () -> assertTrue(violations.isEmpty()),
                    () -> assertEquals("user@example.com", request.getEmail()),
                    () -> assertEquals("password123", request.getPassword())
            );
        }

        @Test
        @DisplayName("Should fail when email is blank")
        void shouldFailWhenEmailBlank() {
            // Given
            var request = new LoginRequest();
            request.setEmail(" ");
            request.setPassword("password123");

            // When
            var violations = validator.validate(request);

            // Then
            assertAll(
                    () -> assertEquals(1, violations.size()),
                    () -> assertEquals("Email is required",
                            violations.iterator().next().getMessage())
            );
        }
    }

    @Nested
    @DisplayName("Invite Participants Request Tests")
    class InviteParticipantsRequestTest {
        @Test
        @DisplayName("Should validate invite request successfully")
        void shouldValidateInviteRequest() {
            // Given
            var request = new InviteParticipantsRequest();
            request.setUserIds(List.of(1L, 2L, 3L));
            request.setMessage("Welcome to the meeting!");

            // When
            var violations = validator.validate(request);

            // Then
            assertAll(
                    () -> assertTrue(violations.isEmpty()),
                    () -> assertEquals(3, request.getUserIds().size()),
                    () -> assertEquals("Welcome to the meeting!", request.getMessage())
            );
        }

        @Test
        @DisplayName("Should fail when no users selected")
        void shouldFailWhenNoUsersSelected() {
            // Given
            var request = new InviteParticipantsRequest();
            request.setUserIds(List.of());

            // When
            var violations = validator.validate(request);

            // Then
            assertAll(
                    () -> assertEquals(1, violations.size()),
                    () -> assertEquals("At least one user must be selected",
                            violations.iterator().next().getMessage())
            );
        }
    }

    @Nested
    @DisplayName("Update User Request Tests")
    class UpdateUserRequestTest {
        @Test
        @DisplayName("Should validate update user request successfully")
        void shouldValidateUpdateUserRequest() {
            // Given
            var request = UpdateUserRequest.builder()
                    .firstName("Jan")
                    .lastName("Kowalski")
                    .phoneNumber("+48 123 456 789")
                    .build();

            // When
            var violations = validator.validate(request);

            // Then
            assertAll(
                    () -> assertTrue(violations.isEmpty()),
                    () -> assertEquals("Jan", request.getFirstName()),
                    () -> assertEquals("Kowalski", request.getLastName()),
                    () -> assertEquals("+48 123 456 789", request.getPhoneNumber())
            );
        }
    }


}
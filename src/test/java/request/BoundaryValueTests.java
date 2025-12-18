package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.MeetingType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import static org.junit.jupiter.api.Assertions.*;

class BoundaryValueTests {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @ParameterizedTest
    @ValueSource(strings = {
            "ab",
            "abc",

    })


    @Test
    void shouldValidateMaxParticipantsBoundaries() {
        // Given
        var request = CreateMeetingRequest.builder()
                .title("Boundary Test")
                .type(MeetingType.ONLINE)
                .visibility(com.meethub.domain.model.enums.MeetingVisibility.PUBLIC)
                .startDate(java.time.LocalDateTime.now().plusDays(1))
                .endDate(java.time.LocalDateTime.now().plusDays(1).plusHours(2))
                .maxParticipants(1000) // max boundary
                .build();

        // When
        var violations = validator.validate(request);

        // Then
        assertAll(
                () -> assertTrue(violations.isEmpty()),
                () -> assertEquals(1000, request.getMaxParticipants())
        );
    }

    @Test
    void shouldFailWhenExceedsMaxBoundary() {
        // Given
        var request = CreateMeetingRequest.builder()
                .title("A".repeat(201)) // exceeds max
                .type(MeetingType.ONLINE)
                .visibility(com.meethub.domain.model.enums.MeetingVisibility.PUBLIC)
                .startDate(java.time.LocalDateTime.now().plusDays(1))
                .endDate(java.time.LocalDateTime.now().plusDays(1).plusHours(2))
                .maxParticipants(1001) // exceeds max
                .build();

        // When
        var violations = validator.validate(request);

        // Then
        assertAll(
                () -> assertEquals(2, violations.size(),
                        "Should have 2 violations for exceeding boundaries"),
                () -> assertTrue(violations.stream()
                        .anyMatch(v -> v.getMessage().contains("Title must be 3-200 characters"))),
                () -> assertTrue(violations.stream()
                        .anyMatch(v -> v.getMessage().contains("Maximum participants cannot exceed 1000")))
        );
    }
}
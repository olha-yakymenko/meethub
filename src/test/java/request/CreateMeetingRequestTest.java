package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.MeetingType;
import com.meethub.domain.model.enums.MeetingVisibility;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CreateMeetingRequestTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldCreateValidMeetingRequest() {
        // Given
        var request = CreateMeetingRequest.builder()
                .title("Team Meeting")
                .description("Weekly team sync")
                .type(MeetingType.ONLINE)
                .visibility(MeetingVisibility.PUBLIC)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(1).plusHours(2))
                .maxParticipants(10)
                .tags(Set.of("team", "sync"))
                .build();

        // When
        var violations = validator.validate(request);

        // Then
        assertAll(
                () -> assertTrue(violations.isEmpty(),
                        "Should have no validation errors"),
                () -> assertEquals("Team Meeting", request.getTitle()),
                () -> assertEquals(MeetingType.ONLINE, request.getType()),
                () -> assertEquals(MeetingVisibility.PUBLIC, request.getVisibility()),
                () -> assertEquals(10, request.getMaxParticipants()),
                () -> assertTrue(request.isEndDateAfterStartDate()),
                () -> assertTrue(request.isDurationValid()),
                () -> assertFalse(request.isRecurring()),
                () -> assertTrue(request.isRecurrenceValid())
        );
    }

    @Test
    void shouldFailWhenEndDateBeforeStartDate() {
        // Given
        var request = CreateMeetingRequest.builder()
                .title("Invalid Meeting")
                .type(MeetingType.ONLINE)
                .visibility(MeetingVisibility.PUBLIC)
                .startDate(LocalDateTime.now().plusDays(2))
                .endDate(LocalDateTime.now().plusDays(1))
                .build();

        // When
        var violations = validator.validate(request);

        // Then
        assertAll(
                () -> assertEquals(1, violations.size()),
                () -> assertEquals("End date must be after start date",
                        violations.iterator().next().getMessage()),
                () -> assertFalse(request.isEndDateAfterStartDate())
        );
    }

    @Test
    void shouldValidateRecurringMeeting() {
        // Given
        var request = CreateMeetingRequest.builder()
                .title("Recurring Meeting")
                .type(MeetingType.ONLINE)
                .visibility(MeetingVisibility.PUBLIC)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(1).plusHours(1))
                .recurring(true)
                .recurrencePattern("WEEKLY:2")
                .recurrenceEndDate(LocalDateTime.now().plusMonths(3))
                .build();

        // When
        var violations = validator.validate(request);

        // Then
        assertAll(
                () -> assertTrue(violations.isEmpty()),
                () -> assertTrue(request.isRecurring()),
                () -> assertEquals("WEEKLY:2", request.getRecurrencePattern()),
                () -> assertNotNull(request.getRecurrenceEndDate()),
                () -> assertTrue(request.isRecurrenceValid())
        );
    }

    @Test
    void shouldFailWhenRecurringWithoutPattern() {
        // Given
        var request = CreateMeetingRequest.builder()
                .title("Invalid Recurring Meeting")
                .type(MeetingType.ONLINE)
                .visibility(MeetingVisibility.PUBLIC)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(1).plusHours(1))
                .recurring(true)
                .build();

        // When
        var violations = validator.validate(request);

        // Then
        assertAll(
                () -> assertEquals(1, violations.size()),
                () -> assertEquals("Recurrence pattern is required for recurring meetings",
                        violations.iterator().next().getMessage()),
                () -> assertFalse(request.isRecurrenceValid())
        );
    }

    @ParameterizedTest
    @CsvSource({
            "DAILY:1, true",
            "WEEKLY:2, true",
            "MONTHLY:1:15, true",
            "invalid, false",
            "DAILY, true"
    })
    void shouldValidateRecurrencePattern(String pattern, boolean isValid) {
        // Given
        var request = CreateMeetingRequest.builder()
                .title("Pattern Test")
                .type(MeetingType.ONLINE)
                .visibility(MeetingVisibility.PUBLIC)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(1).plusHours(1))
                .recurring(true)
                .recurrencePattern(pattern)
                .build();

        // When
        var violations = validator.validate(request);

        // Then
        assertAll(
                () -> assertEquals(isValid ? 0 : 1, violations.size()),
                () -> assertEquals(pattern, request.getRecurrencePattern())
        );
    }
}
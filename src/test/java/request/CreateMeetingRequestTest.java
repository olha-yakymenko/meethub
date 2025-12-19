// CreateMeetingRequestTest.java
package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.MeetingType;
import com.meethub.domain.model.enums.MeetingVisibility;
import org.junit.jupiter.api.Test;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CreateMeetingRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void testCompleteValidMeetingRequest() {
        LocalDateTime startDate = LocalDateTime.now().plusDays(1);
        LocalDateTime endDate = startDate.plusHours(2);

        CreateMeetingRequest request = CreateMeetingRequest.builder()
                .title("Quarterly Planning Meeting")
                .description("Discuss Q2 goals and strategies")
                .agenda("1. Review Q1 results\n2. Set Q2 objectives\n3. Assign tasks")
                .type(MeetingType.ONLINE)
                .visibility(MeetingVisibility.PUBLIC)
                .startDate(startDate)
                .endDate(endDate)
                .maxParticipants(25)
                .locationId(123L)
                .tags(Set.of("planning", "quarterly", "business"))
                .recurring(true)
                .recurrencePattern("MONTHLY:1:15")
                .recurrenceEndDate(LocalDateTime.now().plusMonths(6))
                .recurrenceExceptions(List.of("2024-04-01", "2024-07-01"))
                .categoryIds(Set.of(1L, 2L, 3L))
                .saveAsTemplate(false)
                .build();

        assertAll("Complete valid meeting request validation",
                () -> assertTrue(request.isEndDateAfterStartDate(),
                        "End date should be after start date"),
                () -> assertTrue(request.isDurationValid(),
                        "Meeting duration should not exceed 24 hours"),
                () -> assertTrue(request.isRecurrenceValid(),
                        "Recurring meeting should have valid pattern"),
                () -> assertEquals("[\"2024-04-01\",\"2024-07-01\"]",
                        request.getRecurrenceExceptionsJson(),
                        "Recurrence exceptions JSON should be properly formatted"),
                () -> assertEquals(25, request.getMaxParticipants(),
                        "Max participants should be 25"),
                () -> assertTrue(request.getTags().contains("planning"),
                        "Tags should contain 'planning'"),
                () -> assertEquals("Quarterly Planning Meeting", request.getTitle(),
                        "Title should match"),
                () -> assertTrue(validator.validate(request).isEmpty(),
                        "Valid request should have no validation violations")
        );
    }

    @Test
    void testMeetingDurationAndDateValidation() {
        LocalDateTime now = LocalDateTime.now();

        CreateMeetingRequest validMeeting = CreateMeetingRequest.builder()
                .title("Short Meeting")
                .type(MeetingType.ONLINE)
                .visibility(MeetingVisibility.PRIVATE)
                .startDate(now.plusHours(1))
                .endDate(now.plusHours(3))
                .build();

        CreateMeetingRequest longMeeting = CreateMeetingRequest.builder()
                .title("Long Meeting")
                .type(MeetingType.ONLINE)
                .visibility(MeetingVisibility.PRIVATE)
                .startDate(now)
                .endDate(now.plusHours(30))
                .build();

        CreateMeetingRequest invalidDateMeeting = CreateMeetingRequest.builder()
                .title("Invalid Date Meeting")
                .type(MeetingType.ONLINE)
                .visibility(MeetingVisibility.PRIVATE)
                .startDate(now.plusDays(2))
                .endDate(now.plusDays(1))
                .build();

        assertAll("Duration and date validation tests",
                () -> assertTrue(validMeeting.isDurationValid(),
                        "2-hour meeting should be valid"),
                () -> assertTrue(validMeeting.isEndDateAfterStartDate(),
                        "End time should be after start time"),
                () -> assertFalse(longMeeting.isDurationValid(),
                        "30-hour meeting should be invalid"),
                () -> assertFalse(invalidDateMeeting.isEndDateAfterStartDate(),
                        "End date before start date should be invalid")
        );
    }

    @Test
    void testValidationConstraints() {
        CreateMeetingRequest invalidTitle = CreateMeetingRequest.builder()
                .title("A") // Too short
                .type(MeetingType.ONLINE)
                .visibility(MeetingVisibility.PUBLIC)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(1).plusHours(1))
                .build();

        CreateMeetingRequest pastMeeting = CreateMeetingRequest.builder()
                .title("Valid Title")
                .type(MeetingType.ONLINE)
                .visibility(MeetingVisibility.PUBLIC)
                .startDate(LocalDateTime.now().minusDays(1)) // Past
                .endDate(LocalDateTime.now().plusHours(1))
                .build();

        CreateMeetingRequest tooManyParticipants = CreateMeetingRequest.builder()
                .title("Valid Title")
                .type(MeetingType.ONLINE)
                .visibility(MeetingVisibility.PUBLIC)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(1).plusHours(1))
                .maxParticipants(2000)
                .build();

        assertAll("Constraint validation tests",
                () -> assertEquals(1, validator.validate(invalidTitle).size(),
                        "Title too short should have 1 violation"),
                () -> assertEquals(2, validator.validate(pastMeeting).size(),
                        "Past start date should have 2 violation"),
                () -> assertEquals(1, validator.validate(tooManyParticipants).size(),
                        "Too many participants should have 1 violation")
        );
    }
}
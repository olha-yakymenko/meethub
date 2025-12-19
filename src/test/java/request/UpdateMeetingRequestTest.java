// UpdateMeetingRequestTest.java
package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.MeetingStatus;
import com.meethub.domain.model.enums.MeetingType;
import com.meethub.domain.model.enums.MeetingVisibility;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UpdateMeetingRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void testCompleteValidUpdateRequest() {
        LocalDateTime startDate = LocalDateTime.now().plusDays(2);
        LocalDateTime endDate = startDate.plusHours(3);

        UpdateMeetingRequest request = UpdateMeetingRequest.builder()
                .title("Updated Team Meeting")
                .description("Updated description with new agenda items")
                .agenda("1. New business\n2. Action items")
                .type(MeetingType.ONLINE)
                .visibility(MeetingVisibility.PUBLIC)
                .startDate(startDate)
                .endDate(endDate)
                .maxParticipants(30)
                .locationId(456L)
                .tags(Set.of("updated", "team", "business"))
                .recurring(true)
                .recurrencePattern("WEEKLY:2")
                .recurrenceEndDate(LocalDateTime.now().plusMonths(3))
                .recurrenceExceptionsJson("[\"2024-06-01\",\"2024-06-15\"]")
                .categoryIds(Set.of(1L, 2L))
                .status(MeetingStatus.PLANNED)
                .statusChangeReason("Meeting rescheduled")
                .build();

        var violations = validator.validate(request);

        assertAll("Complete update meeting request",
                () -> assertTrue(violations.isEmpty(),
                        "Valid request should have no violations"),
                () -> assertEquals("Updated Team Meeting", request.getTitle(),
                        "Title should match"),
                () -> assertTrue(request.getDescription().contains("Updated description"),
                        "Description should contain expected text"),
                () -> assertEquals(MeetingType.ONLINE, request.getType(),
                        "Meeting type should be BUSINESS"),
                () -> assertEquals(MeetingVisibility.PUBLIC, request.getVisibility(),
                        "Visibility should be PUBLIC"),
                () -> assertEquals(startDate, request.getStartDate(),
                        "Start date should match"),
                () -> assertEquals(endDate, request.getEndDate(),
                        "End date should match"),
                () -> assertEquals(30, request.getMaxParticipants(),
                        "Max participants should be 30"),
                () -> assertEquals(456L, request.getLocationId(),
                        "Location ID should match"),
                () -> assertEquals(3, request.getTags().size(),
                        "Should have 3 tags"),
                () -> assertTrue(request.getTags().contains("updated"),
                        "Tags should contain 'updated'"),
                () -> assertTrue(request.isRecurring(),
                        "Should be recurring"),
                () -> assertEquals("WEEKLY:2", request.getRecurrencePattern(),
                        "Recurrence pattern should match"),
                () -> assertEquals("[\"2024-06-01\",\"2024-06-15\"]",
                        request.getRecurrenceExceptionsJson(),
                        "Recurrence exceptions JSON should match"),
                () -> assertEquals(2, request.getCategoryIds().size(),
                        "Should have 2 category IDs"),
                () -> assertEquals(MeetingStatus.PLANNED, request.getStatus(),
                        "Status should be UPCOMING"),
                () -> assertEquals("Meeting rescheduled", request.getStatusChangeReason(),
                        "Status change reason should match"),
                () -> assertTrue(request.isEndDateValid(),
                        "End date should be valid (after start date)")
        );
    }

    @Test
    void testValidationConstraints() {
        LocalDateTime pastDate = LocalDateTime.now().minusDays(1);
        LocalDateTime futureDate = LocalDateTime.now().plusDays(1);

        UpdateMeetingRequest shortTitle = UpdateMeetingRequest.builder()
                .title("AB") // Too short
                .startDate(futureDate)
                .endDate(futureDate.plusHours(1))
                .build();

        UpdateMeetingRequest pastStartDate = UpdateMeetingRequest.builder()
                .title("Valid Title")
                .startDate(pastDate) // Past date
                .endDate(futureDate)
                .build();

        UpdateMeetingRequest invalidPattern = UpdateMeetingRequest.builder()
                .title("Valid Title")
                .startDate(futureDate)
                .endDate(futureDate.plusHours(1))
                .recurrencePattern("INVALID:XYZ") // Invalid pattern
                .build();

        var titleViolations = validator.validate(shortTitle);
        var dateViolations = validator.validate(pastStartDate);
        var patternViolations = validator.validate(invalidPattern);

        assertAll("Constraint violations",
                () -> assertEquals(1, titleViolations.size(),
                        "Short title should have 1 violation"),
                () -> assertTrue(titleViolations.stream().anyMatch(v ->
                                v.getMessage().contains("Title must be 3-200 characters")),
                        "Violation should mention title length"),

                () -> assertEquals(1, dateViolations.size(),
                        "Past start date should have 1 violation"),
                () -> assertTrue(dateViolations.stream().anyMatch(v ->
                                v.getMessage().contains("Start date must be in the future")),
                        "Violation should mention future date requirement"),

                () -> assertEquals(1, patternViolations.size(),
                        "Invalid pattern should have 1 violation"),
                () -> assertTrue(patternViolations.stream().anyMatch(v ->
                                v.getMessage().contains("Invalid recurrence pattern")),
                        "Violation should mention pattern validity")
        );
    }

    @Test
    void testEndDateValidation() {
        LocalDateTime startDate = LocalDateTime.now().plusDays(1);
        LocalDateTime endDate = startDate.minusHours(1); // End before start

        UpdateMeetingRequest invalidRequest = UpdateMeetingRequest.builder()
                .title("Valid Title")
                .startDate(startDate)
                .endDate(endDate)
                .build();

        UpdateMeetingRequest validRequest = UpdateMeetingRequest.builder()
                .title("Valid Title")
                .startDate(startDate)
                .endDate(startDate.plusHours(2))
                .build();

        assertAll("End date validation",
                () -> assertFalse(invalidRequest.isEndDateValid(),
                        "End date before start date should be invalid"),
                () -> assertTrue(validRequest.isEndDateValid(),
                        "End date after start date should be valid")
        );
    }
}
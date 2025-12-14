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
    void shouldCreateValidUpdateRequest() {
        // Given
        var request = UpdateMeetingRequest.builder()
                .title("Updated Meeting")
                .description("Updated description")
                .type(MeetingType.HYBRID)
                .visibility(MeetingVisibility.PRIVATE)
                .startDate(LocalDateTime.now().plusDays(2))
                .endDate(LocalDateTime.now().plusDays(2).plusHours(3))
                .maxParticipants(50)
                .status(MeetingStatus.PLANNED)
                .statusChangeReason("All participants confirmed")
                .tags(Set.of("updated", "important"))
                .build();

        // When
        var violations = validator.validate(request);

        // Then
        assertAll(
                () -> assertTrue(violations.isEmpty()),
                () -> assertEquals("Updated Meeting", request.getTitle()),
                () -> assertEquals(MeetingType.HYBRID, request.getType()),
                () -> assertEquals(MeetingVisibility.PRIVATE, request.getVisibility()),
                () -> assertEquals(50, request.getMaxParticipants()),
                () -> assertEquals(MeetingStatus.PLANNED, request.getStatus()),
                () -> assertEquals("All participants confirmed", request.getStatusChangeReason()),
                () -> assertTrue(request.isEndDateValid())
        );
    }
}
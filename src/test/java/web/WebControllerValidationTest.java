package com.meethub.controller.web;

import com.meethub.domain.model.request.CreateMeetingRequest;
import com.meethub.domain.model.request.UpdateMeetingRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WebControllerValidationTest {

    @Autowired
    private Validator validator;

    @Test
    void createMeetingRequest_shouldValidateRequiredFields() {
        // Given
        CreateMeetingRequest request = new CreateMeetingRequest();

        // When
        BindingResult result = new BeanPropertyBindingResult(request, "createMeetingRequest");
        validator.validate(request, result);

        // Then
        assertTrue(result.hasErrors());
        assertNotNull(result.getFieldError("title"));
        assertNotNull(result.getFieldError("type"));
        assertNotNull(result.getFieldError("visibility"));
        assertNotNull(result.getFieldError("startDate"));
        assertNotNull(result.getFieldError("endDate"));
    }

    @Test
    void createMeetingRequest_shouldPassValidation_whenAllFieldsValid() {
        // Given
        CreateMeetingRequest request = CreateMeetingRequest.builder()
                .title("Valid Meeting")
                .description("Valid Description")
                .type(com.meethub.domain.model.enums.MeetingType.ONLINE)
                .visibility(com.meethub.domain.model.enums.MeetingVisibility.PUBLIC)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(1).plusHours(2))
                .maxParticipants(10)
                .build();

        // When
        BindingResult result = new BeanPropertyBindingResult(request, "createMeetingRequest");
        validator.validate(request, result);

        // Then
        assertFalse(result.hasErrors());
    }

    @Test
    void createMeetingRequest_shouldValidateMaxParticipants() {
        // Given
        CreateMeetingRequest request = CreateMeetingRequest.builder()
                .title("Meeting")
                .type(com.meethub.domain.model.enums.MeetingType.ONLINE)
                .visibility(com.meethub.domain.model.enums.MeetingVisibility.PUBLIC)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(1).plusHours(2))
                .maxParticipants(0)  // Invalid - must be positive
                .build();

        // When
        BindingResult result = new BeanPropertyBindingResult(request, "createMeetingRequest");
        validator.validate(request, result);

        // Then
        assertTrue(result.hasErrors());
        assertNotNull(result.getFieldError("maxParticipants"));
    }
}
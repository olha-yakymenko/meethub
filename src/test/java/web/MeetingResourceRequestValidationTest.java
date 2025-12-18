package com.meethub.controller.web;

import com.meethub.domain.model.request.MeetingResourceRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MeetingResourceRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void meetingResourceRequest_shouldValidateNameLength() {
        // Given
        MeetingResourceRequest requestTooShort = MeetingResourceRequest.builder()
                .build();

        MeetingResourceRequest requestTooLong = MeetingResourceRequest.builder()
                .build();

        // When
        Set<ConstraintViolation<MeetingResourceRequest>> violationsShort = validator.validate(requestTooShort);
        Set<ConstraintViolation<MeetingResourceRequest>> violationsLong = validator.validate(requestTooLong);

        // Then
        assertAll("Name length validation",
                () -> assertFalse(violationsShort.isEmpty(), "Expected violations for too short name"),
                () -> assertFalse(violationsLong.isEmpty(), "Expected violations for too long name")
        );
    }

    @Test
    void meetingResourceRequest_shouldValidateDescriptionLength() {
        // Given
        MeetingResourceRequest request = MeetingResourceRequest.builder()
                .description("a".repeat(1001)) // too long, max 1000
                .build();

        // When
        Set<ConstraintViolation<MeetingResourceRequest>> violations = validator.validate(request);

        // Then
        assertAll("Description length validation",
                () -> assertFalse(violations.isEmpty(), "Expected violations for too long description")
        );
    }
}

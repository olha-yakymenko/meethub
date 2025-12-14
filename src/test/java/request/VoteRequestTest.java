package com.meethub.domain.model.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VoteRequestTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldCreateValidVoteRequest() {
        // Given
        var request = new VoteRequest();
        request.setOptionIds(List.of(1L, 2L, 3L));
        request.setPreferenceOrder(List.of(1, 2, 3));

        // When
        var violations = validator.validate(request);

        // Then
        assertAll(
                () -> assertTrue(violations.isEmpty()),
                () -> assertEquals(3, request.getOptionIds().size()),
                () -> assertEquals(3, request.getPreferenceOrder().size()),
                () -> assertTrue(request.isValidPreferenceOrder())
        );
    }

    @Test
    void shouldValidatePreferenceOrderMismatch() {
        // Given
        var request = new VoteRequest();
        request.setOptionIds(List.of(1L, 2L));
        request.setPreferenceOrder(List.of(1, 2, 3)); // Extra preference

        // Then
        assertAll(
                () -> assertFalse(request.isValidPreferenceOrder()),
                () -> assertEquals(2, request.getOptionIds().size()),
                () -> assertEquals(3, request.getPreferenceOrder().size())
        );
    }
}
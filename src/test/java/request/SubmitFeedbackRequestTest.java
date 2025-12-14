package com.meethub.domain.model.request;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import static org.junit.jupiter.api.Assertions.*;

class SubmitFeedbackRequestTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldCreateValidRequest() {
        // Given
        var request = SubmitFeedbackRequest.builder()
                .rating(5)
                .comment("Great meeting!")
                .build();

        // When
        var violations = validator.validate(request);

        // Then
        assertAll(
                () -> assertTrue(violations.isEmpty()),
                () -> assertEquals(5, request.getRating()),
                () -> assertEquals("Great meeting!", request.getComment())
        );
    }

    @Test
    void shouldFailWhenRatingIsNull() {
        // Given
        var request = SubmitFeedbackRequest.builder()
                .rating(null)
                .comment("Great meeting!")
                .build();

        // When
        var violations = validator.validate(request);

        // Then
        assertAll(
                () -> assertEquals(1, violations.size()),
                () -> assertEquals("Rating is required",
                        violations.iterator().next().getMessage()),
                () -> assertNull(request.getRating())
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 6})
    void shouldFailWhenRatingIsOutOfRange(int invalidRating) {
        // Given
        var request = SubmitFeedbackRequest.builder()
                .rating(invalidRating)
                .build();

        // When
        var violations = validator.validate(request);

        // Then
        assertAll(
                () -> assertEquals(1, violations.size()),
                () -> assertTrue(violations.iterator().next().getMessage()
                        .contains("Rating must be at least 1") ||
                        violations.iterator().next().getMessage()
                                .contains("Rating cannot exceed 5")),
                () -> assertEquals(invalidRating, request.getRating())
        );
    }
}
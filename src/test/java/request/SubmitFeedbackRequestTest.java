// SubmitFeedbackRequestTest.java
package com.meethub.domain.model.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SubmitFeedbackRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void testValidFeedbackRequest() {
        SubmitFeedbackRequest request = SubmitFeedbackRequest.builder()
                .rating(5)
                .comment("Excellent meeting! Very productive and well-organized.")
                .build();

        var violations = validator.validate(request);

        assertAll("Valid feedback request",
                () -> assertTrue(violations.isEmpty(),
                        "Valid request should have no violations"),
                () -> assertEquals(5, request.getRating(),
                        "Rating should be 5"),
                () -> assertEquals("Excellent meeting! Very productive and well-organized.",
                        request.getComment(),
                        "Comment should match")
        );
    }

    @Test
    void testValidationConstraints() {
        SubmitFeedbackRequest nullRating = SubmitFeedbackRequest.builder()
                .comment("Good meeting")
                // rating is null
                .build();

        SubmitFeedbackRequest lowRating = SubmitFeedbackRequest.builder()
                .rating(0) // Below minimum
                .comment("Test")
                .build();

        SubmitFeedbackRequest highRating = SubmitFeedbackRequest.builder()
                .rating(6) // Above maximum
                .comment("Test")
                .build();

        var nullViolations = validator.validate(nullRating);
        var lowViolations = validator.validate(lowRating);
        var highViolations = validator.validate(highRating);

        assertAll("Constraint violations",
                () -> assertEquals(1, nullViolations.size(),
                        "Null rating should have 1 violation"),
                () -> assertTrue(nullViolations.stream().anyMatch(v ->
                                v.getMessage().contains("Rating is required")),
                        "Violation should mention rating requirement"),

                () -> assertEquals(1, lowViolations.size(),
                        "Rating below minimum should have 1 violation"),
                () -> assertTrue(lowViolations.stream().anyMatch(v ->
                                v.getMessage().contains("Rating must be at least 1")),
                        "Violation should mention minimum rating"),

                () -> assertEquals(1, highViolations.size(),
                        "Rating above maximum should have 1 violation"),
                () -> assertTrue(highViolations.stream().anyMatch(v ->
                                v.getMessage().contains("Rating cannot exceed 5")),
                        "Violation should mention maximum rating")
        );
    }

    @Test
    void testBuilderAndConstructor() {
        SubmitFeedbackRequest builderRequest = SubmitFeedbackRequest.builder()
                .rating(4)
                .comment("Good")
                .build();

        SubmitFeedbackRequest constructorRequest = new SubmitFeedbackRequest(3, "Average");

        assertAll("Builder and constructor",
                () -> assertEquals(4, builderRequest.getRating(),
                        "Builder should set rating correctly"),
                () -> assertEquals("Good", builderRequest.getComment(),
                        "Builder should set comment correctly"),
                () -> assertEquals(3, constructorRequest.getRating(),
                        "Constructor should set rating correctly"),
                () -> assertEquals("Average", constructorRequest.getComment(),
                        "Constructor should set comment correctly")
        );
    }
}
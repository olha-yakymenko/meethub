package com.meethub.domain.model.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class VotingOptionRequestTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldCreateValidVotingOption() {
        // Given
        var option = new VotingOptionRequest();
        option.setOptionDate(LocalDateTime.now().plusDays(1));
        option.setDurationMinutes(60);

        // When
        var violations = validator.validate(option);

        // Then
        assertAll(
                () -> assertTrue(violations.isEmpty()),
                () -> assertTrue(option.getOptionDate().isAfter(LocalDateTime.now())),
                () -> assertEquals(60, option.getDurationMinutes())
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {14, 481})
    void shouldFailWhenDurationOutOfRange(int duration) {
        // Given
        var option = new VotingOptionRequest();
        option.setOptionDate(LocalDateTime.now().plusDays(1));
        option.setDurationMinutes(duration);

        // When
        var violations = validator.validate(option);

        // Then
        assertAll(
                () -> assertEquals(1, violations.size()),
                () -> assertEquals(duration, option.getDurationMinutes())
        );
    }
}
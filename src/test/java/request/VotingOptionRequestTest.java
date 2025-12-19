// VotingOptionRequestTest.java
package com.meethub.domain.model.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class VotingOptionRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void testValidVotingOption() {
        LocalDateTime futureDate = LocalDateTime.now().plusDays(2);

        VotingOptionRequest request = new VotingOptionRequest();
        request.setOptionDate(futureDate);
        request.setDurationMinutes(60);

        var violations = validator.validate(request);

        assertAll("Valid voting option",
                () -> assertTrue(violations.isEmpty(),
                        "Valid request should have no violations"),
                () -> assertEquals(futureDate, request.getOptionDate(),
                        "Option date should match"),
                () -> assertEquals(60, request.getDurationMinutes(),
                        "Duration should be 60 minutes")
        );
    }

    @Test
    void testValidationConstraints() {
        LocalDateTime pastDate = LocalDateTime.now().minusDays(1);

        VotingOptionRequest nullDate = new VotingOptionRequest();
        nullDate.setDurationMinutes(30);
        // optionDate is null

        VotingOptionRequest pastDateOption = new VotingOptionRequest();
        pastDateOption.setOptionDate(pastDate); // Past date
        pastDateOption.setDurationMinutes(30);

        VotingOptionRequest shortDuration = new VotingOptionRequest();
        shortDuration.setOptionDate(LocalDateTime.now().plusDays(1));
        shortDuration.setDurationMinutes(10); // Below minimum (15)

        VotingOptionRequest longDuration = new VotingOptionRequest();
        longDuration.setOptionDate(LocalDateTime.now().plusDays(1));
        longDuration.setDurationMinutes(500); // Above maximum (480)

        var nullDateViolations = validator.validate(nullDate);
        var pastDateViolations = validator.validate(pastDateOption);
        var shortDurationViolations = validator.validate(shortDuration);
        var longDurationViolations = validator.validate(longDuration);

        assertAll("Constraint violations",
                () -> assertEquals(1, nullDateViolations.size(),
                        "Null date should have 1 violation"),
                () -> assertTrue(nullDateViolations.stream().anyMatch(v ->
                                v.getMessage().contains("Data opcji jest wymagana")),
                        "Violation should mention option date requirement"),

                () -> assertEquals(1, pastDateViolations.size(),
                        "Past date should have 1 violation"),
                () -> assertTrue(pastDateViolations.stream().anyMatch(v ->
                                v.getMessage().contains("Option date must be in the future")),
                        "Violation should mention future date requirement"),

                () -> assertEquals(1, shortDurationViolations.size(),
                        "Short duration should have 1 violation"),
                () -> assertTrue(shortDurationViolations.stream().anyMatch(v ->
                                v.getMessage().contains("Czas trwania musi wynosić co najmniej 15 minut")),
                        "Violation should mention minimum duration"),

                () -> assertEquals(1, longDurationViolations.size(),
                        "Long duration should have 1 violation"),
                () -> assertTrue(longDurationViolations.stream().anyMatch(v ->
                                v.getMessage().contains("Czas trwania nie może przekraczać 8 godzin")),
                        "Violation should mention maximum duration")
        );
    }

    @Test
    void testDurationBoundaryValues() {
        LocalDateTime futureDate = LocalDateTime.now().plusDays(1);

        VotingOptionRequest minDuration = new VotingOptionRequest();
        minDuration.setOptionDate(futureDate);
        minDuration.setDurationMinutes(15); // Minimum

        VotingOptionRequest maxDuration = new VotingOptionRequest();
        maxDuration.setOptionDate(futureDate);
        maxDuration.setDurationMinutes(480); // Maximum (8 hours)

        VotingOptionRequest midDuration = new VotingOptionRequest();
        midDuration.setOptionDate(futureDate);
        midDuration.setDurationMinutes(120); // 2 hours

        var minViolations = validator.validate(minDuration);
        var maxViolations = validator.validate(maxDuration);
        var midViolations = validator.validate(midDuration);

        assertAll("Duration boundary values",
                () -> assertTrue(minViolations.isEmpty(),
                        "Minimum duration (15 minutes) should be valid"),
                () -> assertEquals(15, minDuration.getDurationMinutes(),
                        "Should accept minimum duration"),

                () -> assertTrue(maxViolations.isEmpty(),
                        "Maximum duration (480 minutes) should be valid"),
                () -> assertEquals(480, maxDuration.getDurationMinutes(),
                        "Should accept maximum duration"),

                () -> assertTrue(midViolations.isEmpty(),
                        "Mid duration (120 minutes) should be valid"),
                () -> assertEquals(120, midDuration.getDurationMinutes(),
                        "Should accept mid duration")
        );
    }

    @Test
    void testGetterSetterConsistency() {
        VotingOptionRequest request = new VotingOptionRequest();
        LocalDateTime date = LocalDateTime.of(2024, 12, 25, 14, 30);

        request.setOptionDate(date);
        request.setDurationMinutes(90);

        assertAll("Getter/Setter consistency",
                () -> assertEquals(date, request.getOptionDate(),
                        "Getter should return set date"),
                () -> assertEquals(90, request.getDurationMinutes(),
                        "Getter should return set duration"),
                () -> assertEquals(14, request.getOptionDate().getHour(),
                        "Date hour should be preserved"),
                () -> assertEquals(30, request.getOptionDate().getMinute(),
                        "Date minute should be preserved")
        );
    }
}
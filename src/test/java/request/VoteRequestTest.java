// VoteRequestTest.java
package com.meethub.domain.model.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class VoteRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void testValidVoteRequestWithPreferenceOrder() {
        VoteRequest request = new VoteRequest();
        request.setOptionIds(Arrays.asList(1L, 2L, 3L));
        request.setPreferenceOrder(Arrays.asList(1, 2, 3));

        Set<ConstraintViolation<VoteRequest>> violations = validator.validate(request);

        assertAll("Valid vote request with preference order",
                () -> assertTrue(violations.isEmpty(),
                        "Valid request should have no violations"),
                () -> assertEquals(3, request.getOptionIds().size(),
                        "Should have 3 option IDs"),
                () -> assertEquals(1L, request.getOptionIds().get(0),
                        "First option ID should be 1"),
                () -> assertEquals(2L, request.getOptionIds().get(1),
                        "Second option ID should be 2"),
                () -> assertEquals(3L, request.getOptionIds().get(2),
                        "Third option ID should be 3"),
                () -> assertEquals(3, request.getPreferenceOrder().size(),
                        "Should have 3 preference orders"),
                () -> assertEquals(1, request.getPreferenceOrder().get(0),
                        "First preference should be 1"),
                () -> assertEquals(2, request.getPreferenceOrder().get(1),
                        "Second preference should be 2"),
                () -> assertEquals(3, request.getPreferenceOrder().get(2),
                        "Third preference should be 3"),
                () -> assertTrue(request.isValidPreferenceOrder(),
                        "Preference order should be valid (equal sizes)"),
                () -> assertNotNull(request.toString(),
                        "toString() should not return null")
        );
    }

    @Test
    void testValidVoteRequestWithoutPreferenceOrder() {
        VoteRequest request = new VoteRequest();
        request.setOptionIds(Arrays.asList(5L, 10L, 15L, 20L));
        // preferenceOrder is null

        Set<ConstraintViolation<VoteRequest>> violations = validator.validate(request);

        assertAll("Valid vote request without preference order",
                () -> assertTrue(violations.isEmpty(),
                        "Request without preference order should be valid"),
                () -> assertEquals(4, request.getOptionIds().size(),
                        "Should have 4 option IDs"),
                () -> assertNull(request.getPreferenceOrder(),
                        "Preference order should be null"),
                () -> assertTrue(request.isValidPreferenceOrder(),
                        "Null preference order should be valid"),
                () -> assertTrue(request.getOptionIds().contains(10L),
                        "Should contain option ID 10"),
                () -> assertFalse(request.getOptionIds().contains(99L),
                        "Should not contain non-existent option ID")
        );
    }

    @Test
    void testValidVoteRequestWithEmptyPreferenceOrder() {
        VoteRequest request = new VoteRequest();
        request.setOptionIds(Collections.singletonList(42L));
        request.setPreferenceOrder(Collections.emptyList());

        Set<ConstraintViolation<VoteRequest>> violations = validator.validate(request);

        assertAll("Valid vote request with empty preference order",
                () -> assertTrue(violations.isEmpty(),
                        "Request with empty preference order should be valid"),
                () -> assertEquals(1, request.getOptionIds().size(),
                        "Should have 1 option ID"),
                () -> assertEquals(42L, request.getOptionIds().get(0),
                        "Option ID should be 42"),
                () -> assertTrue(request.getPreferenceOrder().isEmpty(),
                        "Preference order should be empty"),
                () -> assertTrue(request.isValidPreferenceOrder(),
                        "Empty preference order should be valid")
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testInvalidOptionIds(List<Long> invalidOptionIds) {
        VoteRequest request = new VoteRequest();
        request.setOptionIds(invalidOptionIds);

        Set<ConstraintViolation<VoteRequest>> violations = validator.validate(request);

        String violationMessage = violations.stream()
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse("");

        assertAll("Invalid option IDs - " + (invalidOptionIds == null ? "null" : "empty"),
                () -> assertFalse(violations.isEmpty(),
                        "Null or empty optionIds should have violations"),
                () -> assertTrue(violations.size() >= 1,
                        "Should have at least 1 violation"),
                () -> assertTrue(violationMessage.contains("Musisz wybrać opcje") ||
                                violationMessage.contains("At least one option must be selected"),
                        "Violation should mention option selection requirement"),
                () -> assertEquals(invalidOptionIds, request.getOptionIds(),
                        "Getter should return set value (even if null/empty)")
        );
    }

    @Test
    void testInvalidPreferenceOrderMismatch() {
        VoteRequest request = new VoteRequest();
        request.setOptionIds(Arrays.asList(1L, 2L, 3L, 4L)); // 4 options
        request.setPreferenceOrder(Arrays.asList(1, 2, 3)); // Only 3 preferences

        Set<ConstraintViolation<VoteRequest>> violations = validator.validate(request);

        assertAll("Invalid preference order - size mismatch",
                () -> assertFalse(violations.isEmpty(),
                        "Mismatched sizes should have violations"),
                () -> assertEquals(1, violations.size(),
                        "Should have exactly 1 violation for isValidPreferenceOrder()"),
                () -> assertFalse(request.isValidPreferenceOrder(),
                        "isValidPreferenceOrder() should return false"),
                () -> assertEquals(4, request.getOptionIds().size(),
                        "Should still have 4 option IDs"),
                () -> assertEquals(3, request.getPreferenceOrder().size(),
                        "Should have 3 preference orders"),
                () -> assertTrue(violations.stream()
                                .anyMatch(v -> v.getMessage().contains("Preference order must match selected options")),
                        "Violation should mention preference order mismatch")
        );
    }

    @Test
    void testInvalidPreferenceOrderLargerThanOptions() {
        VoteRequest request = new VoteRequest();
        request.setOptionIds(Collections.singletonList(1L)); // 1 option
        request.setPreferenceOrder(Arrays.asList(1, 2, 3)); // 3 preferences

        Set<ConstraintViolation<VoteRequest>> violations = validator.validate(request);

        assertAll("Invalid preference order - more preferences than options",
                () -> assertFalse(violations.isEmpty(),
                        "More preferences than options should have violations"),
                () -> assertFalse(request.isValidPreferenceOrder(),
                        "isValidPreferenceOrder() should return false"),
                () -> assertEquals(1, request.getOptionIds().size(),
                        "Should have 1 option ID"),
                () -> assertEquals(3, request.getPreferenceOrder().size(),
                        "Should have 3 preference orders")
        );
    }

    @Test
    void testValidPreferenceOrderWithNonSequentialNumbers() {
        VoteRequest request = new VoteRequest();
        request.setOptionIds(Arrays.asList(1L, 2L, 3L));
        request.setPreferenceOrder(Arrays.asList(3, 1, 2)); // Non-sequential but correct size

        Set<ConstraintViolation<VoteRequest>> violations = validator.validate(request);

        assertAll("Valid preference order with non-sequential numbers",
                () -> assertTrue(violations.isEmpty(),
                        "Non-sequential preference order should be valid if sizes match"),
                () -> assertTrue(request.isValidPreferenceOrder(),
                        "isValidPreferenceOrder() should return true"),
                () -> assertEquals(3, request.getOptionIds().size(),
                        "Should have 3 option IDs"),
                () -> assertEquals(3, request.getPreferenceOrder().size(),
                        "Should have 3 preference orders"),
                () -> assertEquals(3, request.getPreferenceOrder().get(0),
                        "First preference should be 3"),
                () -> assertEquals(1, request.getPreferenceOrder().get(1),
                        "Second preference should be 1"),
                () -> assertEquals(2, request.getPreferenceOrder().get(2),
                        "Third preference should be 2")
        );
    }

    @Test
    void testValidPreferenceOrderWithSameNumbers() {
        VoteRequest request = new VoteRequest();
        request.setOptionIds(Arrays.asList(1L, 2L, 3L));
        request.setPreferenceOrder(Arrays.asList(1, 1, 1)); // All same number

        Set<ConstraintViolation<VoteRequest>> violations = validator.validate(request);

        assertAll("Valid preference order with same numbers",
                () -> assertTrue(violations.isEmpty(),
                        "Same preference numbers should be valid if sizes match"),
                () -> assertTrue(request.isValidPreferenceOrder(),
                        "isValidPreferenceOrder() should return true"),
                () -> assertEquals(3, request.getOptionIds().size(),
                        "Should have 3 option IDs"),
                () -> assertEquals(3, request.getPreferenceOrder().size(),
                        "Should have 3 preference orders"),
                () -> assertEquals(1, request.getPreferenceOrder().get(0),
                        "All preferences should be 1"),
                () -> assertEquals(1, request.getPreferenceOrder().get(1),
                        "All preferences should be 1"),
                () -> assertEquals(1, request.getPreferenceOrder().get(2),
                        "All preferences should be 1")
        );
    }

    @Test
    void testSingleOptionVote() {
        VoteRequest request = new VoteRequest();
        request.setOptionIds(Collections.singletonList(99L));
        request.setPreferenceOrder(Collections.singletonList(1));

        Set<ConstraintViolation<VoteRequest>> violations = validator.validate(request);

        assertAll("Single option vote",
                () -> assertTrue(violations.isEmpty(),
                        "Single option vote should be valid"),
                () -> assertEquals(1, request.getOptionIds().size(),
                        "Should have 1 option ID"),
                () -> assertEquals(99L, request.getOptionIds().get(0),
                        "Option ID should be 99"),
                () -> assertEquals(1, request.getPreferenceOrder().size(),
                        "Should have 1 preference"),
                () -> assertEquals(1, request.getPreferenceOrder().get(0),
                        "Preference should be 1"),
                () -> assertTrue(request.isValidPreferenceOrder(),
                        "Single option with preference should be valid")
        );
    }

    @Test
    void testManyOptionsVote() {
        List<Long> manyOptions = new ArrayList<>();
        List<Integer> manyPreferences = new ArrayList<>();

        for (long i = 1; i <= 100; i++) {
            manyOptions.add(i);
            manyPreferences.add((int) i);
        }

        VoteRequest request = new VoteRequest();
        request.setOptionIds(manyOptions);
        request.setPreferenceOrder(manyPreferences);

        Set<ConstraintViolation<VoteRequest>> violations = validator.validate(request);

        assertAll("Many options vote",
                () -> assertTrue(violations.isEmpty(),
                        "Many options vote should be valid"),
                () -> assertEquals(100, request.getOptionIds().size(),
                        "Should have 100 option IDs"),
                () -> assertEquals(100, request.getPreferenceOrder().size(),
                        "Should have 100 preferences"),
                () -> assertEquals(1L, request.getOptionIds().get(0),
                        "First option should be 1"),
                () -> assertEquals(100L, request.getOptionIds().get(99),
                        "Last option should be 100"),
                () -> assertEquals(1, request.getPreferenceOrder().get(0),
                        "First preference should be 1"),
                () -> assertEquals(100, request.getPreferenceOrder().get(99),
                        "Last preference should be 100"),
                () -> assertTrue(request.isValidPreferenceOrder(),
                        "Many options with matching preferences should be valid")
        );
    }

    @Test
    void testPreferenceOrderWithNegativeNumbers() {
        VoteRequest request = new VoteRequest();
        request.setOptionIds(Arrays.asList(1L, 2L));
        request.setPreferenceOrder(Arrays.asList(-1, 5));

        Set<ConstraintViolation<VoteRequest>> violations = validator.validate(request);

        assertAll("Preference order with negative numbers",
                () -> assertTrue(violations.isEmpty(),
                        "Negative preference numbers should be valid (no constraint on values)"),
                () -> assertTrue(request.isValidPreferenceOrder(),
                        "isValidPreferenceOrder() should return true (only checks size)"),
                () -> assertEquals(2, request.getOptionIds().size(),
                        "Should have 2 option IDs"),
                () -> assertEquals(2, request.getPreferenceOrder().size(),
                        "Should have 2 preferences"),
                () -> assertEquals(-1, request.getPreferenceOrder().get(0),
                        "First preference can be negative"),
                () -> assertEquals(5, request.getPreferenceOrder().get(1),
                        "Second preference can be any integer")
        );
    }

    @Test
    void testPreferenceOrderWithZero() {
        VoteRequest request = new VoteRequest();
        request.setOptionIds(Arrays.asList(1L, 2L, 3L));
        request.setPreferenceOrder(Arrays.asList(0, 0, 0));

        Set<ConstraintViolation<VoteRequest>> violations = validator.validate(request);

        assertAll("Preference order with zeros",
                () -> assertTrue(violations.isEmpty(),
                        "Zero preference values should be valid"),
                () -> assertTrue(request.isValidPreferenceOrder(),
                        "isValidPreferenceOrder() should return true"),
                () -> assertEquals(3, request.getOptionIds().size(),
                        "Should have 3 option IDs"),
                () -> assertEquals(3, request.getPreferenceOrder().size(),
                        "Should have 3 preferences"),
                () -> assertEquals(0, request.getPreferenceOrder().get(0),
                        "Zero is valid preference value")
        );
    }

    @Test
    void testDuplicateOptionIds() {
        VoteRequest request = new VoteRequest();
        request.setOptionIds(Arrays.asList(1L, 1L, 2L, 2L)); // Duplicates
        request.setPreferenceOrder(Arrays.asList(1, 2, 3, 4));

        Set<ConstraintViolation<VoteRequest>> violations = validator.validate(request);

        assertAll("Duplicate option IDs",
                () -> assertTrue(violations.isEmpty(),
                        "Duplicate option IDs should be valid (no uniqueness constraint)"),
                () -> assertTrue(request.isValidPreferenceOrder(),
                        "isValidPreferenceOrder() should return true"),
                () -> assertEquals(4, request.getOptionIds().size(),
                        "Should have 4 option IDs (including duplicates)"),
                () -> assertEquals(1L, request.getOptionIds().get(0),
                        "First option should be 1"),
                () -> assertEquals(1L, request.getOptionIds().get(1),
                        "Second option can also be 1 (duplicate)"),
                () -> assertEquals(2L, request.getOptionIds().get(2),
                        "Third option should be 2"),
                () -> assertEquals(2L, request.getOptionIds().get(3),
                        "Fourth option can also be 2 (duplicate)")
        );
    }

    @Test
    void testGetterSetterConsistency() {
        VoteRequest request = new VoteRequest();

        List<Long> optionIds = Arrays.asList(10L, 20L, 30L);
        List<Integer> preferences = Arrays.asList(3, 2, 1);

        request.setOptionIds(optionIds);
        request.setPreferenceOrder(preferences);

        assertAll("Getter/Setter consistency",
                () -> assertSame(optionIds, request.getOptionIds(),
                        "Getter should return the same list instance"),
                () -> assertSame(preferences, request.getPreferenceOrder(),
                        "Getter should return the same preferences instance"),
                () -> assertEquals(3, request.getOptionIds().size(),
                        "Should return correct size"),
                () -> assertEquals(3, request.getPreferenceOrder().size(),
                        "Should return correct preferences size"),
                () -> assertEquals(10L, request.getOptionIds().get(0),
                        "Should preserve first option ID"),
                () -> assertEquals(20L, request.getOptionIds().get(1),
                        "Should preserve second option ID"),
                () -> assertEquals(30L, request.getOptionIds().get(2),
                        "Should preserve third option ID"),
                () -> assertEquals(3, request.getPreferenceOrder().get(0),
                        "Should preserve first preference"),
                () -> assertEquals(2, request.getPreferenceOrder().get(1),
                        "Should preserve second preference"),
                () -> assertEquals(1, request.getPreferenceOrder().get(2),
                        "Should preserve third preference")
        );
    }

    @Test
    void testEqualsAndHashCode() {
        VoteRequest request1 = new VoteRequest();
        request1.setOptionIds(Arrays.asList(1L, 2L));
        request1.setPreferenceOrder(Arrays.asList(1, 2));

        VoteRequest request2 = new VoteRequest();
        request2.setOptionIds(Arrays.asList(1L, 2L));
        request2.setPreferenceOrder(Arrays.asList(1, 2));

        VoteRequest request3 = new VoteRequest();
        request3.setOptionIds(Arrays.asList(3L, 4L));
        request3.setPreferenceOrder(Arrays.asList(1, 2));

        assertAll("Equals and HashCode",
                () -> assertEquals(request1, request2,
                        "Requests with same values should be equal"),
                () -> assertEquals(request1.hashCode(), request2.hashCode(),
                        "Equal requests should have same hash code"),
                () -> assertNotEquals(request1, request3,
                        "Requests with different values should not be equal"),
                () -> assertNotEquals(request1.hashCode(), request3.hashCode(),
                        "Different requests should have different hash codes"),
                () -> assertNotEquals(request1, null,
                        "Request should not equal null"),
                () -> assertNotEquals(request1, new Object(),
                        "Request should not equal different type")
        );
    }

    @Test
    void testLombokGeneratedMethods() {
        VoteRequest request = new VoteRequest();
        request.setOptionIds(Collections.singletonList(7L));

        String toString = request.toString();

        assertAll("Lombok generated methods",
                () -> assertNotNull(toString,
                        "toString() should not be null"),
                () -> assertTrue(toString.contains("VoteRequest"),
                        "toString() should contain class name"),
                () -> assertTrue(toString.contains("optionIds") || toString.contains("7"),
                        "toString() should contain field values"),
                () -> assertNotNull(request.canEqual(new VoteRequest()),
                        "canEqual() should not be null")
        );
    }

    @ParameterizedTest
    @MethodSource("provideValidPreferenceOrderScenarios")
    void testValidPreferenceOrderScenarios(List<Long> optionIds, List<Integer> preferenceOrder) {
        VoteRequest request = new VoteRequest();
        request.setOptionIds(optionIds);
        request.setPreferenceOrder(preferenceOrder);

        assertAll("Valid preference order scenario",
                () -> assertTrue(request.isValidPreferenceOrder(),
                        "Scenario should have valid preference order"),
                () -> assertEquals(optionIds.size(), request.getOptionIds().size(),
                        "Option IDs size should match"),
                () -> assertEquals(preferenceOrder == null ? 0 : preferenceOrder.size(),
                        request.getPreferenceOrder() == null ? 0 : request.getPreferenceOrder().size(),
                        "Preference order size should match")
        );
    }

    private static Stream<Arguments> provideValidPreferenceOrderScenarios() {
        return Stream.of(
                arguments(Arrays.asList(1L, 2L), Arrays.asList(1, 2)),
                arguments(Arrays.asList(1L), Collections.singletonList(1)),
                arguments(Arrays.asList(1L, 2L, 3L, 4L, 5L), Arrays.asList(5, 4, 3, 2, 1)),
                arguments(Arrays.asList(1L, 2L), null),
                arguments(Arrays.asList(1L), Collections.emptyList()),
                arguments(Arrays.asList(1L, 2L, 3L), Arrays.asList(100, 200, 300))
        );
    }

    @ParameterizedTest
    @MethodSource("provideInvalidPreferenceOrderScenarios")
    void testInvalidPreferenceOrderScenarios(List<Long> optionIds, List<Integer> preferenceOrder) {
        VoteRequest request = new VoteRequest();
        request.setOptionIds(optionIds);
        request.setPreferenceOrder(preferenceOrder);

        assertAll("Invalid preference order scenario",
                () -> assertFalse(request.isValidPreferenceOrder(),
                        "Scenario should have invalid preference order"),
                () -> assertNotEquals(
                        optionIds.size(),
                        preferenceOrder == null ? 0 : preferenceOrder.size(),
                        "Sizes should not match for invalid scenario"
                )
        );
    }

    private static Stream<Arguments> provideInvalidPreferenceOrderScenarios() {
        return Stream.of(
                arguments(Arrays.asList(1L, 2L), Arrays.asList(1)), // Missing one preference
                arguments(Arrays.asList(1L), Arrays.asList(1, 2)), // Extra preference
                arguments(Arrays.asList(1L, 2L, 3L), Arrays.asList(1, 2)), // Missing one
                arguments(Collections.singletonList(1L), Arrays.asList(1, 2, 3)) // Many extras
        );
    }
}
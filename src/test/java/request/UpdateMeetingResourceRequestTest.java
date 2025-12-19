// UpdateMeetingResourceRequestTest.java
package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.AccessLevel;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UpdateMeetingResourceRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void testValidUpdateRequest() {
        UpdateMeetingResourceRequest request = new UpdateMeetingResourceRequest();
        request.setDescription("Updated document description with more details");
        request.setTags(Set.of("report", "updated", "confidential"));
        request.setAccessLevel(AccessLevel.PRIVATE);

        var violations = validator.validate(request);

        assertAll("Valid update request",
                () -> assertTrue(violations.isEmpty(),
                        "Valid request should have no violations"),
                () -> assertEquals("Updated document description with more details",
                        request.getDescription(),
                        "Description should match"),
                () -> assertEquals(3, request.getTags().size(),
                        "Should have 3 tags"),
                () -> assertTrue(request.getTags().contains("confidential"),
                        "Tags should contain 'confidential'"),
                () -> assertEquals(AccessLevel.PRIVATE, request.getAccessLevel(),
                        "Access level should be PRIVATE")
        );
    }

    @Test
    void testValidationConstraints() {
        UpdateMeetingResourceRequest longDescription = new UpdateMeetingResourceRequest();
        longDescription.setDescription("A".repeat(1001)); // Exceeds 1000 chars
        longDescription.setAccessLevel(AccessLevel.PUBLIC);

        UpdateMeetingResourceRequest manyTags = new UpdateMeetingResourceRequest();
        manyTags.setTags(Set.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"));
        manyTags.setAccessLevel(AccessLevel.PUBLIC);

        UpdateMeetingResourceRequest nullAccessLevel = new UpdateMeetingResourceRequest();
        nullAccessLevel.setDescription("Test");
        // accessLevel is null

        var descViolations = validator.validate(longDescription);
        var tagsViolations = validator.validate(manyTags);
        var accessViolations = validator.validate(nullAccessLevel);

        assertAll("Constraint validation",
                () -> assertEquals(1, descViolations.size(),
                        "Long description should have 1 violation"),
                () -> assertTrue(descViolations.stream().anyMatch(v ->
                                v.getMessage().contains("Description cannot exceed 1000 characters")),
                        "Violation message should mention description length"),

                () -> assertEquals(1, tagsViolations.size(),
                        "Too many tags should have 1 violation"),
                () -> assertTrue(tagsViolations.stream().anyMatch(v ->
                                v.getMessage().contains("Cannot have more than 10 tags")),
                        "Violation message should mention tag limit"),

                () -> assertEquals(1, accessViolations.size(),
                        "Null access level should have 1 violation"),
                () -> assertTrue(accessViolations.stream().anyMatch(v ->
                                v.getMessage().contains("Access level is required")),
                        "Violation message should mention access level requirement")
        );
    }
}
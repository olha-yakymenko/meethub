// CreateTaskRequestTest.java
package com.meethub.domain.model.request;

import org.junit.jupiter.api.Test;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CreateTaskRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void testCompleteValidTaskRequest() {
        LocalDateTime deadline = LocalDateTime.now().plusDays(14);

        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("Implement User Authentication")
                .description("Add JWT-based authentication with refresh tokens and role-based access control")
                .deadline(deadline)
                .assignedUserIds(List.of(101L, 102L, 103L))
                .allowSelfAssignment(false)
                .maxFilesPerUser(5)
                .maxFileSize(5 * 1024 * 1024L)
                .allowedFileTypes(List.of("pdf", "docx", "zip", "jpg"))
                .build();

        var violations = validator.validate(request);

        assertAll("Complete task request validation",
                () -> assertTrue(violations.isEmpty(),
                        "Valid request should have no violations"),
                () -> assertEquals("Implement User Authentication", request.getTitle(),
                        "Title should match"),
                () -> assertTrue(request.getDescription().contains("JWT-based authentication"),
                        "Description should contain expected text"),
                () -> assertEquals(deadline, request.getDeadline(),
                        "Deadline should match"),
                () -> assertEquals(3, request.getAssignedUserIds().size(),
                        "Should have 3 assigned users"),
                () -> assertTrue(request.getAssignedUserIds().contains(102L),
                        "Should contain user ID 102"),
                () -> assertFalse(request.getAllowSelfAssignment(),
                        "Self assignment should be disabled"),
                () -> assertEquals(5, request.getMaxFilesPerUser(),
                        "Max files per user should be 5"),
                () -> assertEquals(5 * 1024 * 1024L, request.getMaxFileSize(),
                        "Max file size should be 5MB"),
                () -> assertEquals(4, request.getAllowedFileTypes().size(),
                        "Should have 4 allowed file types"),
                () -> assertTrue(request.isFileTypeAllowed("PDF"),
                        "PDF should be allowed (case-insensitive)"),
                () -> assertFalse(request.isFileTypeAllowed("exe"),
                        "EXE should not be allowed"),
                () -> assertEquals("pdf, docx, zip, jpg", request.getAllowedFileTypesAsString(),
                        "Allowed file types string should match")
        );
    }

    @Test
    void testTaskValidationConstraints() {
        LocalDateTime validDeadline = LocalDateTime.now().plusDays(1);
        LocalDateTime pastDeadline = LocalDateTime.now().minusDays(1);

        CreateTaskRequest shortTitle = CreateTaskRequest.builder()
                .title("AB") // Too short (min 3)
                .description("Valid description with enough characters")
                .deadline(validDeadline)
                .build();

        CreateTaskRequest shortDescription = CreateTaskRequest.builder()
                .title("Valid Title")
                .description("Short") // Too short (min 10)
                .deadline(validDeadline)
                .build();

        CreateTaskRequest pastDue = CreateTaskRequest.builder()
                .title("Valid Title")
                .description("Valid description with enough characters")
                .deadline(pastDeadline)
                .build();

        var titleViolations = validator.validate(shortTitle);
        var descViolations = validator.validate(shortDescription);
        var deadlineViolations = validator.validate(pastDue);

        assertAll("Constraint violation tests",
                () -> assertEquals(1, titleViolations.size(),
                        "Short title should have 1 violation"),
                () -> assertTrue(titleViolations.stream().anyMatch(v ->
                                v.getMessage().contains("Tytuł musi mieć od 3 do 255 znaków")),
                        "Violation should mention title length"),

                () -> assertEquals(1, descViolations.size(),
                        "Short description should have 1 violation"),
                () -> assertTrue(descViolations.stream().anyMatch(v ->
                                v.getMessage().contains("Opis musi mieć od 10 do 2000 znaków")),
                        "Violation should mention description length"),

                () -> assertEquals(1, deadlineViolations.size(),
                        "Past deadline should have 1 violation"),
                () -> assertTrue(deadlineViolations.stream().anyMatch(v ->
                                v.getMessage().contains("Data zakończenia musi być w przyszłości")),
                        "Violation should mention future deadline requirement")
        );
    }

    @Test
    void testTaskHelperMethods() {
        CreateTaskRequest withAssignments = CreateTaskRequest.builder()
                .title("Task with Assignments")
                .description("Test description")
                .deadline(LocalDateTime.now().plusDays(7))
                .assignedUserIds(List.of(1L, 2L, 3L))
                .allowedFileTypes(List.of("pdf", "txt"))
                .build();

        CreateTaskRequest withoutAssignments = CreateTaskRequest.builder()
                .title("Task without Assignments")
                .description("Test description")
                .deadline(LocalDateTime.now().plusDays(7))
                .build();

        CreateTaskRequest emptyFileTypes = CreateTaskRequest.builder()
                .title("Task with Empty Types")
                .description("Test description")
                .deadline(LocalDateTime.now().plusDays(7))
                .allowedFileTypes(List.of())
                .build();

        assertAll("Helper method tests",
                () -> assertTrue(withAssignments.hasAssignedUsers(),
                        "Task with assignments should return true"),
                () -> assertEquals(3, withAssignments.getAssignedUserIds().size(),
                        "Should have 3 assigned users"),

                () -> assertFalse(withoutAssignments.hasAssignedUsers(),
                        "Task without assignments should return false"),
                () -> assertNull(withoutAssignments.getAssignedUserIds(),
                        "Assigned user IDs should be null"),

                () -> assertTrue(withAssignments.isFileTypeAllowed("PDF"),
                        "PDF should be allowed"),
                () -> assertTrue(withAssignments.isFileTypeAllowed("TXT"),
                        "TXT should be allowed"),
                () -> assertFalse(withAssignments.isFileTypeAllowed("DOCX"),
                        "DOCX should not be allowed"),

                () -> assertTrue(emptyFileTypes.isFileTypeAllowed("ANYTYPE"),
                        "Empty allowed list should allow all types"),
                () -> assertEquals("Wszystkie typy plików",
                        emptyFileTypes.getAllowedFileTypesAsString(),
                        "Empty list should return 'All file types'"),
                () -> assertEquals("pdf, txt",
                        withAssignments.getAllowedFileTypesAsString(),
                        "Should return comma-separated file types")
        );
    }
}
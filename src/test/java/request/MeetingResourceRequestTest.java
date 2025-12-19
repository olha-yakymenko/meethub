// MeetingResourceRequestTest.java
package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.AccessLevel;
import org.junit.jupiter.api.Test;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MeetingResourceRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void testCompleteValidMeetingResource() {
        MockMultipartFile validFile = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "PDF content".getBytes()
        );

        MeetingResourceRequest request = MeetingResourceRequest.builder()
                .originalFilename("Quarterly_Report.pdf")
                .description("Detailed quarterly performance report with charts and analysis")
                .file(validFile)
                .tags(Set.of("report", "quarterly", "financial", "pdf"))
                .accessLevel(AccessLevel.PRIVATE)
                .build();

        var violations = validator.validate(request);

        assertAll("Complete meeting resource validation",
                () -> assertTrue(violations.isEmpty(),
                        "Valid request should have no violations"),
                () -> assertEquals("Quarterly_Report.pdf", request.getOriginalFilename(),
                        "Original filename should match"),
                () -> assertEquals("Detailed quarterly performance report with charts and analysis",
                        request.getDescription(),
                        "Description should match"),
                () -> assertNotNull(request.getFile(),
                        "File should not be null"),
                () -> assertEquals(validFile.getOriginalFilename(), request.getFile().getOriginalFilename(),
                        "File name should match"),
                () -> assertEquals(4, request.getTags().size(),
                        "Should have 4 tags"),
                () -> assertTrue(request.getTags().contains("financial"),
                        "Should contain 'financial' tag"),
                () -> assertEquals(AccessLevel.PRIVATE, request.getAccessLevel(),
                        "Access level should be PRIVATE"),
                () -> assertTrue(request.isFileSizeValid(),
                        "File size should be valid")
        );
    }

    @Test
    void testFileSizeAndValidation() {
        MockMultipartFile smallFile = new MockMultipartFile(
                "file", "small.txt", "text/plain", "small".getBytes()
        );

        MockMultipartFile exactSizeFile = new MockMultipartFile(
                "file", "exact.pdf", "application/pdf",
                new byte[10 * 1024 * 1024] // Exactly 10MB
        );

        MockMultipartFile largeFile = new MockMultipartFile(
                "file", "large.pdf", "application/pdf",
                new byte[11 * 1024 * 1024] // 11MB - exceeds limit
        );

        MeetingResourceRequest smallRequest = MeetingResourceRequest.builder()
                .originalFilename("small.txt")
                .file(smallFile)
                .accessLevel(AccessLevel.PUBLIC)
                .build();

        MeetingResourceRequest exactRequest = MeetingResourceRequest.builder()
                .originalFilename("exact.pdf")
                .file(exactSizeFile)
                .accessLevel(AccessLevel.PUBLIC)
                .build();

        MeetingResourceRequest largeRequest = MeetingResourceRequest.builder()
                .originalFilename("large.pdf")
                .file(largeFile)
                .accessLevel(AccessLevel.PUBLIC)
                .build();

        assertAll("File size validation tests",
                () -> assertTrue(smallRequest.isFileSizeValid(),
                        "Small file should be valid"),
                () -> assertTrue(exactRequest.isFileSizeValid(),
                        "Exactly 10MB file should be valid"),
                () -> assertFalse(largeRequest.isFileSizeValid(),
                        "11MB file should exceed size limit"),
                () -> assertEquals(10 * 1024 * 1024, exactSizeFile.getSize(),
                        "Exact file should be 10MB"),
                () -> assertTrue(largeFile.getSize() > 10 * 1024 * 1024,
                        "Large file should exceed 10MB")
        );
    }

    @Test
    void testConstraintViolations() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "test".getBytes()
        );

        MeetingResourceRequest missingFilename = MeetingResourceRequest.builder()
                .originalFilename("") // Empty - invalid
                .file(file)
                .accessLevel(AccessLevel.PUBLIC)
                .build();

        MeetingResourceRequest longDescription = MeetingResourceRequest.builder()
                .originalFilename("test.txt")
                .description("A".repeat(1001)) // 1001 chars - exceeds limit
                .file(file)
                .accessLevel(AccessLevel.PUBLIC)
                .build();

        MeetingResourceRequest tooManyTags = MeetingResourceRequest.builder()
                .originalFilename("test.txt")
                .file(file)
                .accessLevel(AccessLevel.PUBLIC)
                .tags(Set.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"))
                .build();

        var filenameViolations = validator.validate(missingFilename);
        var descViolations = validator.validate(longDescription);
        var tagsViolations = validator.validate(tooManyTags);

        assertAll("Constraint violation tests",
                () -> assertEquals(1, filenameViolations.size(),
                        "Empty filename should have 1 violation"),
                () -> assertTrue(filenameViolations.stream().anyMatch(v ->
                                v.getMessage().contains("Original filename is required")),
                        "Violation should mention filename requirement"),

                () -> assertEquals(1, descViolations.size(),
                        "Long description should have 1 violation"),
                () -> assertTrue(descViolations.stream().anyMatch(v ->
                                v.getMessage().contains("Description cannot exceed 1000 characters")),
                        "Violation should mention description length"),

                () -> assertEquals(1, tagsViolations.size(),
                        "Too many tags should have 1 violation"),
                () -> assertTrue(tagsViolations.stream().anyMatch(v ->
                                v.getMessage().contains("Cannot have more than 10 tags")),
                        "Violation should mention tag limit"),
                () -> assertEquals(11, tooManyTags.getTags().size(),
                        "Should have 11 tags (exceeds limit)")
        );
    }
}
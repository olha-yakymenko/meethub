package com.meethub.domain.model.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class TaskFileTest {

    @Test
    void shouldCreateTaskFileWithBuilder() {
        // Given
        TaskAssignment assignment = mock(TaskAssignment.class);
        Task task = mock(Task.class);
        User uploadedBy = mock(User.class);
        LocalDateTime uploadedAt = LocalDateTime.now();

        // When
        TaskFile file = TaskFile.builder()
                .filename("document.pdf")
                .originalFilename("My Document.pdf")
                .filePath("/uploads/tasks/1/document.pdf")
                .fileSize(1024L)
                .contentType("application/pdf")
                .uploadedAt(uploadedAt)
                .assignment(assignment)
                .task(task)
                .uploadedBy(uploadedBy)
                .build();

        // Then
        assertAll(
                () -> assertThat(file.getFilename()).isEqualTo("document.pdf"),
                () -> assertThat(file.getOriginalFilename()).isEqualTo("My Document.pdf"),
                () -> assertThat(file.getFilePath()).isEqualTo("/uploads/tasks/1/document.pdf"),
                () -> assertThat(file.getFileSize()).isEqualTo(1024L),
                () -> assertThat(file.getContentType()).isEqualTo("application/pdf"),
                () -> assertThat(file.getUploadedAt()).isEqualTo(uploadedAt),
                () -> assertThat(file.getAssignment()).isEqualTo(assignment),
                () -> assertThat(file.getTask()).isEqualTo(task),
                () -> assertThat(file.getUploadedBy()).isEqualTo(uploadedBy)
        );
    }

    @Test
    void shouldUpdateAllFields() {
        // Given
        TaskFile file = new TaskFile();
        TaskAssignment newAssignment = mock(TaskAssignment.class);
        Task newTask = mock(Task.class);
        User newUploadedBy = mock(User.class);
        LocalDateTime newUploadedAt = LocalDateTime.now();

        // When
        file.setId(1L);
        file.setFilename("updated.pdf");
        file.setOriginalFilename("Updated Document.pdf");
        file.setFilePath("/uploads/tasks/2/updated.pdf");
        file.setFileSize(2048L);
        file.setContentType("application/vnd.ms-excel");
        file.setUploadedAt(newUploadedAt);
        file.setAssignment(newAssignment);
        file.setTask(newTask);
        file.setUploadedBy(newUploadedBy);

        // Then
        assertAll(
                () -> assertThat(file.getId()).isEqualTo(1L),
                () -> assertThat(file.getFilename()).isEqualTo("updated.pdf"),
                () -> assertThat(file.getOriginalFilename()).isEqualTo("Updated Document.pdf"),
                () -> assertThat(file.getFilePath()).isEqualTo("/uploads/tasks/2/updated.pdf"),
                () -> assertThat(file.getFileSize()).isEqualTo(2048L),
                () -> assertThat(file.getContentType()).isEqualTo("application/vnd.ms-excel"),
                () -> assertThat(file.getUploadedAt()).isEqualTo(newUploadedAt),
                () -> assertThat(file.getAssignment()).isEqualTo(newAssignment),
                () -> assertThat(file.getTask()).isEqualTo(newTask),
                () -> assertThat(file.getUploadedBy()).isEqualTo(newUploadedBy)
        );
    }

    @Test
    void shouldHandleNullValues() {
        // When
        TaskFile file = TaskFile.builder()
                .filename("test.txt")
                .originalFilename("Test.txt")
                .build();

        // Then
        assertAll(
                () -> assertThat(file.getFilePath()).isNull(),
                () -> assertThat(file.getFileSize()).isNull(),
                () -> assertThat(file.getContentType()).isNull(),
                () -> assertThat(file.getUploadedAt()).isNull(),
                () -> assertThat(file.getAssignment()).isNull(),
                () -> assertThat(file.getTask()).isNull(),
                () -> assertThat(file.getUploadedBy()).isNull()
        );
    }

    @Test
    void shouldHandleDifferentFileTypes() {
        // Given
        TaskFile pdfFile = TaskFile.builder()
                .filename("document.pdf")
                .contentType("application/pdf")
                .build();

        TaskFile imageFile = TaskFile.builder()
                .filename("image.jpg")
                .contentType("image/jpeg")
                .build();

        TaskFile wordFile = TaskFile.builder()
                .filename("document.docx")
                .contentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .build();

        TaskFile excelFile = TaskFile.builder()
                .filename("spreadsheet.xlsx")
                .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .build();

        // Then
        assertAll(
                () -> assertThat(pdfFile.getContentType()).isEqualTo("application/pdf"),
                () -> assertThat(imageFile.getContentType()).isEqualTo("image/jpeg"),
                () -> assertThat(wordFile.getContentType()).contains("wordprocessingml"),
                () -> assertThat(excelFile.getContentType()).contains("spreadsheetml")
        );
    }

    @Test
    void shouldHandleLargeFileSizes() {
        // Given
        TaskFile smallFile = TaskFile.builder()
                .fileSize(1024L) // 1KB
                .build();

        TaskFile mediumFile = TaskFile.builder()
                .fileSize(1024 * 1024L) // 1MB
                .build();

        TaskFile largeFile = TaskFile.builder()
                .fileSize(1024 * 1024 * 1024L) // 1GB
                .build();

        // Then
        assertAll(
                () -> assertThat(smallFile.getFileSize()).isEqualTo(1024L),
                () -> assertThat(mediumFile.getFileSize()).isEqualTo(1024 * 1024L),
                () -> assertThat(largeFile.getFileSize()).isEqualTo(1024 * 1024 * 1024L)
        );
    }

    @Test
    void shouldImplementDataAnnotationsCorrectly() {
        // Given
        TaskAssignment assignment = mock(TaskAssignment.class);
        Task task = mock(Task.class);
        User uploadedBy = mock(User.class);
        LocalDateTime uploadedAt = LocalDateTime.now();

        // When
        TaskFile file = new TaskFile();
        file.setId(1L);
        file.setFilename("test.pdf");
        file.setOriginalFilename("Test File.pdf");
        file.setFilePath("/uploads/test.pdf");
        file.setFileSize(5000L);
        file.setContentType("application/pdf");
        file.setUploadedAt(uploadedAt);
        file.setAssignment(assignment);
        file.setTask(task);
        file.setUploadedBy(uploadedBy);

        // Then - test all getters
        assertAll(
                () -> assertThat(file.getId()).isEqualTo(1L),
                () -> assertThat(file.getFilename()).isEqualTo("test.pdf"),
                () -> assertThat(file.getOriginalFilename()).isEqualTo("Test File.pdf"),
                () -> assertThat(file.getFilePath()).isEqualTo("/uploads/test.pdf"),
                () -> assertThat(file.getFileSize()).isEqualTo(5000L),
                () -> assertThat(file.getContentType()).isEqualTo("application/pdf"),
                () -> assertThat(file.getUploadedAt()).isEqualTo(uploadedAt),
                () -> assertThat(file.getAssignment()).isEqualTo(assignment),
                () -> assertThat(file.getTask()).isEqualTo(task),
                () -> assertThat(file.getUploadedBy()).isEqualTo(uploadedBy)
        );
    }
}
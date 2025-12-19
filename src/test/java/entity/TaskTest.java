package com.meethub.domain.model.entity;

import com.meethub.domain.model.enums.TaskStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class TaskTest {

    @Test
    void shouldCreateTaskWithBuilder() {
        // Given
        Meeting meeting = mock(Meeting.class);
        User createdBy = mock(User.class);
        LocalDateTime deadline = LocalDateTime.now().plusDays(7);
        LocalDateTime createdAt = LocalDateTime.now();

        // When
        Task task = Task.builder()
                .title("Prepare presentation")
                .description("Create slides for the meeting")
                .status(TaskStatus.PENDING)
                .deadline(deadline)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .meeting(meeting)
                .createdBy(createdBy)
                .allowSelfAssignment(true)
                .allowedFileTypes("pdf,docx,jpg,png")
                .maxFilesPerUser(10)
                .maxFileSize(10 * 1024 * 1024L)
                .build();

        // Then
        assertAll(
                () -> assertThat(task.getTitle()).isEqualTo("Prepare presentation"),
                () -> assertThat(task.getDescription()).isEqualTo("Create slides for the meeting"),
                () -> assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING),
                () -> assertThat(task.getDeadline()).isEqualTo(deadline),
                () -> assertThat(task.getCreatedAt()).isEqualTo(createdAt),
                () -> assertThat(task.getUpdatedAt()).isEqualTo(createdAt),
                () -> assertThat(task.getMeeting()).isEqualTo(meeting),
                () -> assertThat(task.getCreatedBy()).isEqualTo(createdBy),
                () -> assertThat(task.getAllowSelfAssignment()).isTrue(),
                () -> assertThat(task.getAllowedFileTypes()).isEqualTo("pdf,docx,jpg,png"),
                () -> assertThat(task.getMaxFilesPerUser()).isEqualTo(10),
                () -> assertThat(task.getMaxFileSize()).isEqualTo(10 * 1024 * 1024L),
                () -> assertThat(task.getAssignments()).isEmpty(),
                () -> assertThat(task.getFiles()).isEmpty()
        );
    }

    @Test
    void shouldSetDefaultValues() {
        // When
        Task task = Task.builder()
                .title("Test Task")
                .status(TaskStatus.PENDING)
                .build();

        // Then
        assertAll(
                () -> assertThat(task.getAllowSelfAssignment()).isTrue(),
                () -> assertThat(task.getMaxFilesPerUser()).isEqualTo(10),
                () -> assertThat(task.getMaxFileSize()).isEqualTo(10 * 1024 * 1024L),
                () -> assertThat(task.getAssignments()).isNotNull(),
                () -> assertThat(task.getFiles()).isNotNull()
        );
    }

    @Test
    void shouldUpdateAllFields() {
        // Given
        Task task = new Task();
        Meeting newMeeting = mock(Meeting.class);
        User newCreatedBy = mock(User.class);
        LocalDateTime newDeadline = LocalDateTime.now().plusDays(5);
        LocalDateTime newCreatedAt = LocalDateTime.now().minusDays(1);
        LocalDateTime newUpdatedAt = LocalDateTime.now();

        // When
        task.setId(1L);
        task.setTitle("Updated Task");
        task.setDescription("Updated description");
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setDeadline(newDeadline);
        task.setCreatedAt(newCreatedAt);
        task.setUpdatedAt(newUpdatedAt);
        task.setMeeting(newMeeting);
        task.setCreatedBy(newCreatedBy);
        task.setAllowSelfAssignment(false);
        task.setAllowedFileTypes("pdf,txt");
        task.setMaxFilesPerUser(5);
        task.setMaxFileSize(5 * 1024 * 1024L);

        // Then
        assertAll(
                () -> assertThat(task.getId()).isEqualTo(1L),
                () -> assertThat(task.getTitle()).isEqualTo("Updated Task"),
                () -> assertThat(task.getDescription()).isEqualTo("Updated description"),
                () -> assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS),
                () -> assertThat(task.getDeadline()).isEqualTo(newDeadline),
                () -> assertThat(task.getCreatedAt()).isEqualTo(newCreatedAt),
                () -> assertThat(task.getUpdatedAt()).isEqualTo(newUpdatedAt),
                () -> assertThat(task.getMeeting()).isEqualTo(newMeeting),
                () -> assertThat(task.getCreatedBy()).isEqualTo(newCreatedBy),
                () -> assertThat(task.getAllowSelfAssignment()).isFalse(),
                () -> assertThat(task.getAllowedFileTypes()).isEqualTo("pdf,txt"),
                () -> assertThat(task.getMaxFilesPerUser()).isEqualTo(5),
                () -> assertThat(task.getMaxFileSize()).isEqualTo(5 * 1024 * 1024L)
        );
    }

    @Test
    void shouldHandleMeetingIdMethods() {
        // Given
        Task task = new Task();
        Long meetingId = 123L;

        // When - set meeting ID
        task.setMeetingId(meetingId);

        // Then
        assertAll(
                () -> assertThat(task.getMeetingId()).isEqualTo(meetingId),
                () -> assertThat(task.getMeeting()).isNotNull(),
                () -> assertThat(task.getMeeting().getId()).isEqualTo(meetingId)
        );

        // When - set null meeting ID
        task.setMeetingId(null);

        // Then
        assertAll(
                () -> assertThat(task.getMeetingId()).isNull(),
                () -> assertThat(task.getMeeting()).isNotNull()
        );
    }

    @Test
    void shouldHandleCreatedByIdMethods() {
        // Given
        Task task = new Task();
        Long createdById = 456L;

        // When - set created by ID
        task.setCreatedById(createdById);

        // Then
        assertAll(
                () -> assertThat(task.getCreatedById()).isEqualTo(createdById),
                () -> assertThat(task.getCreatedBy()).isNotNull(),
                () -> assertThat(task.getCreatedBy().getId()).isEqualTo(createdById)
        );

        // When - set null created by ID
        task.setCreatedById(null);

        // Then
        assertAll(
                () -> assertThat(task.getCreatedById()).isNull(),
                () -> assertThat(task.getCreatedBy()).isNotNull()
        );
    }

    @Test
    void shouldAddAssignments() {
        // Given
        Task task = new Task();
        TaskAssignment assignment1 = mock(TaskAssignment.class);
        TaskAssignment assignment2 = mock(TaskAssignment.class);

        // When
        task.getAssignments().add(assignment1);
        task.getAssignments().add(assignment2);

        // Then
        assertAll(
                () -> assertThat(task.getAssignments()).hasSize(2),
                () -> assertThat(task.getAssignments()).contains(assignment1, assignment2)
        );
    }

    @Test
    void shouldAddFiles() {
        // Given
        Task task = new Task();
        TaskFile file1 = mock(TaskFile.class);
        TaskFile file2 = mock(TaskFile.class);

        // When
        task.getFiles().add(file1);
        task.getFiles().add(file2);

        // Then
        assertAll(
                () -> assertThat(task.getFiles()).hasSize(2),
                () -> assertThat(task.getFiles()).contains(file1, file2)
        );
    }

    @Test
    void shouldHandleNullValues() {
        // When
        Task task = Task.builder()
                .title("Test Task")
                .status(TaskStatus.PENDING)
                .build();

        // Then
        assertAll(
                () -> assertThat(task.getMeeting()).isNull(),
                () -> assertThat(task.getCreatedBy()).isNull(),
                () -> assertThat(task.getDeadline()).isNull(),
                () -> assertThat(task.getCreatedAt()).isNull(),
                () -> assertThat(task.getUpdatedAt()).isNull(),
                () -> assertThat(task.getAllowedFileTypes()).isNull(),
                () -> assertThat(task.getDescription()).isNull()
        );
    }

    @Test
    void shouldHandleDifferentStatuses() {
        // Given
        Task pendingTask = Task.builder()
                .status(TaskStatus.PENDING)
                .build();

        Task inProgressTask = Task.builder()
                .status(TaskStatus.IN_PROGRESS)
                .build();

        Task completedTask = Task.builder()
                .status(TaskStatus.COMPLETED)
                .build();

        Task cancelledTask = Task.builder()
                .status(TaskStatus.CANCELLED)
                .build();

        // Then
        assertAll(
                () -> assertThat(pendingTask.getStatus()).isEqualTo(TaskStatus.PENDING),
                () -> assertThat(inProgressTask.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS),
                () -> assertThat(completedTask.getStatus()).isEqualTo(TaskStatus.COMPLETED),
                () -> assertThat(cancelledTask.getStatus()).isEqualTo(TaskStatus.CANCELLED)
        );
    }
}
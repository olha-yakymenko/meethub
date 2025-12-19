package com.meethub.domain.model.entity;

import com.meethub.domain.model.enums.AssignmentStatus;
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
class TaskAssignmentTest {

    @Test
    void shouldCreateTaskAssignmentWithBuilder() {
        // Given
        Task task = mock(Task.class);
        User user = mock(User.class);
        LocalDateTime assignedAt = LocalDateTime.now();
        LocalDateTime completedAt = assignedAt.plusDays(2);

        // When
        TaskAssignment assignment = TaskAssignment.builder()
                .task(task)
                .user(user)
                .status(AssignmentStatus.IN_PROGRESS)
                .comment("Working on the task")
                .assignedAt(assignedAt)
                .completedAt(completedAt)
                .build();

        // Then
        assertAll(
                () -> assertThat(assignment.getTask()).isEqualTo(task),
                () -> assertThat(assignment.getUser()).isEqualTo(user),
                () -> assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.IN_PROGRESS),
                () -> assertThat(assignment.getComment()).isEqualTo("Working on the task"),
                () -> assertThat(assignment.getAssignedAt()).isEqualTo(assignedAt),
                () -> assertThat(assignment.getCompletedAt()).isEqualTo(completedAt),
                () -> assertThat(assignment.getFiles()).isEmpty()
        );
    }

    @Test
    void shouldUpdateAllFields() {
        // Given
        TaskAssignment assignment = new TaskAssignment();
        Task newTask = mock(Task.class);
        User newUser = mock(User.class);
        LocalDateTime newAssignedAt = LocalDateTime.now();
        LocalDateTime newCompletedAt = newAssignedAt.plusDays(1);

        // When
        assignment.setId(1L);
        assignment.setTask(newTask);
        assignment.setUser(newUser);
        assignment.setStatus(AssignmentStatus.COMPLETED);
        assignment.setComment("Task completed successfully");
        assignment.setAssignedAt(newAssignedAt);
        assignment.setCompletedAt(newCompletedAt);

        // Then
        assertAll(
                () -> assertThat(assignment.getId()).isEqualTo(1L),
                () -> assertThat(assignment.getTask()).isEqualTo(newTask),
                () -> assertThat(assignment.getUser()).isEqualTo(newUser),
                () -> assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.COMPLETED),
                () -> assertThat(assignment.getComment()).isEqualTo("Task completed successfully"),
                () -> assertThat(assignment.getAssignedAt()).isEqualTo(newAssignedAt),
                () -> assertThat(assignment.getCompletedAt()).isEqualTo(newCompletedAt)
        );
    }

    @Test
    void shouldAddFilesToAssignment() {
        // Given
        TaskAssignment assignment = TaskAssignment.builder().build();
        TaskFile file1 = mock(TaskFile.class);
        TaskFile file2 = mock(TaskFile.class);

        // When
        assignment.getFiles().add(file1);
        assignment.getFiles().add(file2);

        // Then
        assertAll(
                () -> assertThat(assignment.getFiles()).hasSize(2),
                () -> assertThat(assignment.getFiles()).contains(file1, file2)
        );
    }

    @Test
    void shouldInitializeFilesListByDefault() {
        // When
        TaskAssignment assignment1 = new TaskAssignment();
        TaskAssignment assignment2 = TaskAssignment.builder().build();

        // Then
        assertAll(
                () -> assertThat(assignment1.getFiles()).isNotNull(),
                () -> assertThat(assignment1.getFiles()).isEmpty(),
                () -> assertThat(assignment2.getFiles()).isNotNull(),
                () -> assertThat(assignment2.getFiles()).isEmpty()
        );
    }

    @Test
    void shouldHandleNullValues() {
        // When
        TaskAssignment assignment = TaskAssignment.builder()
                .status(AssignmentStatus.PENDING)
                .build();

        // Then
        assertAll(
                () -> assertThat(assignment.getTask()).isNull(),
                () -> assertThat(assignment.getUser()).isNull(),
                () -> assertThat(assignment.getComment()).isNull(),
                () -> assertThat(assignment.getAssignedAt()).isNull(),
                () -> assertThat(assignment.getCompletedAt()).isNull()
        );
    }

    @Test
    void shouldHandleDifferentStatuses() {
        // Given
        TaskAssignment pending = TaskAssignment.builder()
                .status(AssignmentStatus.PENDING)
                .build();

        TaskAssignment inProgress = TaskAssignment.builder()
                .status(AssignmentStatus.IN_PROGRESS)
                .build();

        TaskAssignment completed = TaskAssignment.builder()
                .status(AssignmentStatus.COMPLETED)
                .build();

        TaskAssignment cancelled = TaskAssignment.builder()
                .status(AssignmentStatus.CANCELLED)
                .build();

        // Then
        assertAll(
                () -> assertThat(pending.getStatus()).isEqualTo(AssignmentStatus.PENDING),
                () -> assertThat(inProgress.getStatus()).isEqualTo(AssignmentStatus.IN_PROGRESS),
                () -> assertThat(completed.getStatus()).isEqualTo(AssignmentStatus.COMPLETED),
                () -> assertThat(cancelled.getStatus()).isEqualTo(AssignmentStatus.CANCELLED)
        );
    }

    @Test
    void shouldImplementDataAnnotationsCorrectly() {
        // Given
        Task task = mock(Task.class);
        User user = mock(User.class);
        LocalDateTime assignedAt = LocalDateTime.now();

        // When
        TaskAssignment assignment = new TaskAssignment();
        assignment.setId(1L);
        assignment.setTask(task);
        assignment.setUser(user);
        assignment.setStatus(AssignmentStatus.IN_PROGRESS);
        assignment.setComment("Started working");
        assignment.setAssignedAt(assignedAt);

        // Then - test all getters
        assertAll(
                () -> assertThat(assignment.getId()).isEqualTo(1L),
                () -> assertThat(assignment.getTask()).isEqualTo(task),
                () -> assertThat(assignment.getUser()).isEqualTo(user),
                () -> assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.IN_PROGRESS),
                () -> assertThat(assignment.getComment()).isEqualTo("Started working"),
                () -> assertThat(assignment.getAssignedAt()).isEqualTo(assignedAt),
                () -> assertThat(assignment.getFiles()).isNotNull()
        );
    }
}
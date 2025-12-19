package com.meethub.domain.model.entity;

import com.meethub.domain.model.enums.TaskPriority;
import com.meethub.domain.model.enums.TaskStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class MeetingTaskTest {

    @Test
    void shouldCreateMeetingTaskWithBuilder() {
        // Given
        Meeting meeting = mock(Meeting.class);
        User assignedUser = mock(User.class);
        LocalDateTime dueDate = LocalDateTime.now().plusDays(7);

        // When
        MeetingTask task = MeetingTask.builder()
                .meeting(meeting)
                .title("Prepare presentation")
                .description("Create slides for the quarterly review")
                .status(TaskStatus.PENDING)
                .priority(TaskPriority.HIGH)
                .assignedTo(assignedUser)
                .dueDate(dueDate)
                .progressPercentage(0)
                .build();

        // Then
        assertAll(
                () -> assertThat(task.getMeeting()).isEqualTo(meeting),
                () -> assertThat(task.getTitle()).isEqualTo("Prepare presentation"),
                () -> assertThat(task.getDescription()).isEqualTo("Create slides for the quarterly review"),
                () -> assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING),
                () -> assertThat(task.getPriority()).isEqualTo(TaskPriority.HIGH),
                () -> assertThat(task.getAssignedTo()).isEqualTo(assignedUser),
                () -> assertThat(task.getDueDate()).isEqualTo(dueDate),
                () -> assertThat(task.getProgressPercentage()).isEqualTo(0),
                () -> assertThat(task.getCreatedAt()).isNull(), // Will be set by @CreationTimestamp
                () -> assertThat(task.getUpdatedAt()).isNull()  // Will be set by @UpdateTimestamp
        );
    }

    @Test
    void shouldMarkTaskAsCompleted() {
        // Given
        MeetingTask task = new MeetingTask();
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setProgressPercentage(50);

        // When
        task.markAsCompleted();

        // Then
        assertAll(
                () -> assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED),
                () -> assertThat(task.getProgressPercentage()).isEqualTo(100),
                () -> assertThat(task.getCompletedAt()).isNotNull()
        );
    }

    @Test
    void shouldMarkTaskAsInProgress() {
        // Given
        MeetingTask task = new MeetingTask();
        task.setStatus(TaskStatus.PENDING);
        task.setProgressPercentage(0);

        // When
        task.markAsInProgress();

        // Then
        assertAll(
                () -> assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS),
                () -> assertThat(task.getProgressPercentage()).isEqualTo(10)
        );
    }

    @Test
    void shouldUpdateProgress() {
        // Given
        MeetingTask task = new MeetingTask();
        MeetingTask finalTask = task;
        // When - update to 50%
        task.updateProgress(50);

        // Then
        assertAll(
                () -> assertThat(finalTask.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS),
                () -> assertThat(finalTask.getProgressPercentage()).isEqualTo(50)
        );

    }

    @Test
    void shouldHandleInvalidProgressValues() {
        // Given
        MeetingTask task = new MeetingTask();

        // When - negative value
        task.updateProgress(-10);

        // Then
        assertAll(
                () -> assertThat(task.getProgressPercentage()).isEqualTo(0)
        );

        // When - value above 100
        task.updateProgress(150);

        // Then
        assertAll(
                () -> assertThat(task.getProgressPercentage()).isEqualTo(100)
        );
    }

    @Test
    void shouldCheckIfTaskIsCompleted() {
        // Given
        MeetingTask completedTask = MeetingTask.builder()
                .status(TaskStatus.COMPLETED)
                .build();

        MeetingTask pendingTask = MeetingTask.builder()
                .status(TaskStatus.PENDING)
                .build();

        // Then
        assertAll(
                () -> assertThat(completedTask.isCompleted()).isTrue(),
                () -> assertThat(pendingTask.isCompleted()).isFalse()
        );
    }

    @Test
    void shouldCheckIfTaskIsOverdue() {
        // Given
        LocalDateTime pastDate = LocalDateTime.now().minusDays(1);
        LocalDateTime futureDate = LocalDateTime.now().plusDays(1);

        MeetingTask overdueTask = MeetingTask.builder()
                .dueDate(pastDate)
                .status(TaskStatus.IN_PROGRESS)
                .build();

        MeetingTask notOverdueTask = MeetingTask.builder()
                .dueDate(futureDate)
                .status(TaskStatus.IN_PROGRESS)
                .build();

        MeetingTask completedTask = MeetingTask.builder()
                .dueDate(pastDate)
                .status(TaskStatus.COMPLETED)
                .build();

        MeetingTask noDueDateTask = MeetingTask.builder()
                .dueDate(null)
                .status(TaskStatus.IN_PROGRESS)
                .build();

        // Then
        assertAll(
                () -> assertThat(overdueTask.isOverdue()).isTrue(),
                () -> assertThat(notOverdueTask.isOverdue()).isFalse(),
                () -> assertThat(completedTask.isOverdue()).isFalse(),
                () -> assertThat(noDueDateTask.isOverdue()).isFalse()
        );
    }

    @Test
    void shouldCheckIfTaskIsDueSoon() {
        // Given
        LocalDateTime dueSoon = LocalDateTime.now().plusHours(12); // Within 24 hours
        LocalDateTime dueLater = LocalDateTime.now().plusDays(2);
        LocalDateTime pastDue = LocalDateTime.now().minusDays(1);

        MeetingTask dueSoonTask = MeetingTask.builder()
                .dueDate(dueSoon)
                .status(TaskStatus.IN_PROGRESS)
                .build();

        MeetingTask dueLaterTask = MeetingTask.builder()
                .dueDate(dueLater)
                .status(TaskStatus.IN_PROGRESS)
                .build();

        MeetingTask pastDueTask = MeetingTask.builder()
                .dueDate(pastDue)
                .status(TaskStatus.IN_PROGRESS)
                .build();

        MeetingTask completedTask = MeetingTask.builder()
                .dueDate(dueSoon)
                .status(TaskStatus.COMPLETED)
                .build();

        // Then
        assertAll(
                () -> assertThat(dueSoonTask.isDueSoon()).isTrue(),
                () -> assertThat(dueLaterTask.isDueSoon()).isFalse(),
                () -> assertThat(pastDueTask.isDueSoon()).isFalse(),
                () -> assertThat(completedTask.isDueSoon()).isFalse()
        );
    }

    @Test
    void shouldCheckIfTaskIsAssigned() {
        // Given
        User user = mock(User.class);

        MeetingTask assignedTask = MeetingTask.builder()
                .assignedTo(user)
                .build();

        MeetingTask unassignedTask = MeetingTask.builder()
                .assignedTo(null)
                .build();

        // Then
        assertAll(
                () -> assertThat(assignedTask.isAssigned()).isTrue(),
                () -> assertThat(unassignedTask.isAssigned()).isFalse()
        );
    }

    @Test
    void shouldCheckIfTaskIsHighPriority() {
        // Given
        MeetingTask highPriorityTask = MeetingTask.builder()
                .priority(TaskPriority.HIGH)
                .build();

        MeetingTask urgentTask = MeetingTask.builder()
                .priority(TaskPriority.URGENT)
                .build();

        MeetingTask mediumPriorityTask = MeetingTask.builder()
                .priority(TaskPriority.MEDIUM)
                .build();

        MeetingTask lowPriorityTask = MeetingTask.builder()
                .priority(TaskPriority.LOW)
                .build();

        // Then
        assertAll(
                () -> assertThat(highPriorityTask.isHighPriority()).isTrue(),
                () -> assertThat(urgentTask.isHighPriority()).isTrue(),
                () -> assertThat(mediumPriorityTask.isHighPriority()).isFalse(),
                () -> assertThat(lowPriorityTask.isHighPriority()).isFalse()
        );
    }

    @Test
    void shouldCalculateDaysUntilDue() {
        // Given
        LocalDateTime dueIn5Days = LocalDateTime.now().plusDays(5);
        LocalDateTime pastDue = LocalDateTime.now().minusDays(3);

        MeetingTask futureTask = MeetingTask.builder()
                .dueDate(dueIn5Days)
                .build();

        MeetingTask pastTask = MeetingTask.builder()
                .dueDate(pastDue)
                .build();

        MeetingTask noDueDateTask = MeetingTask.builder()
                .dueDate(null)
                .build();

        // Then
        assertAll(
                () -> assertThat(futureTask.getDaysUntilDue()).isEqualTo(4),
                () -> assertThat(pastTask.getDaysUntilDue()).isEqualTo(0),
                () -> assertThat(noDueDateTask.getDaysUntilDue()).isEqualTo(Long.MAX_VALUE)
        );
    }

    @Test
    void shouldAssignAndUnassignTask() {
        // Given
        MeetingTask task = new MeetingTask();
        User user = mock(User.class);

        // When - assign
        task.assignTo(user);

        // Then
        assertAll(
                () -> assertThat(task.getAssignedTo()).isEqualTo(user),
                () -> assertThat(task.isAssigned()).isTrue()
        );

        // When - unassign
        task.unassign();

        // Then
        assertAll(
                () -> assertThat(task.getAssignedTo()).isNull(),
                () -> assertThat(task.isAssigned()).isFalse()
        );
    }

    @Test
    void shouldCheckEqualsAndHashCode() {
        // Given
        Meeting meeting = mock(Meeting.class);

        MeetingTask task1 = new MeetingTask();
        task1.setId(1L);
        task1.setTitle("Task 1");
        task1.setMeeting(meeting);

        MeetingTask task2 = new MeetingTask();
        task2.setId(1L);
        task2.setTitle("Task 1");
        task2.setMeeting(meeting);

        MeetingTask task3 = new MeetingTask();
        task3.setId(2L);
        task3.setTitle("Task 2");
        task3.setMeeting(meeting);

        // Then
        assertAll(
                () -> assertThat(task1).isEqualTo(task2),
                () -> assertThat(task1).isNotEqualTo(task3),
                () -> assertThat(task1.hashCode()).isEqualTo(task2.hashCode()),
                () -> assertThat(task1.hashCode()).isNotEqualTo(task3.hashCode())
        );
    }
}
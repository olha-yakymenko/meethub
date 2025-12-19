package com.meethub.domain.model.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class MeetingStatusChangeTest {

    @Test
    void shouldCreateMeetingStatusChangeWithBuilder() {
        // Given
        Meeting meeting = mock(Meeting.class);
        LocalDateTime changedAt = LocalDateTime.now();

        // When
        MeetingStatusChange statusChange = MeetingStatusChange.builder()
                .meeting(meeting)
                .oldStatus("PLANNED")
                .newStatus("CONFIRMED")
                .changedByUserId(123L)
                .reason("All participants confirmed")
                .changedAt(changedAt)
                .build();

        // Then
        assertAll(
                () -> assertThat(statusChange.getMeeting()).isEqualTo(meeting),
                () -> assertThat(statusChange.getOldStatus()).isEqualTo("PLANNED"),
                () -> assertThat(statusChange.getNewStatus()).isEqualTo("CONFIRMED"),
                () -> assertThat(statusChange.getChangedByUserId()).isEqualTo(123L),
                () -> assertThat(statusChange.getReason()).isEqualTo("All participants confirmed"),
                () -> assertThat(statusChange.getChangedAt()).isEqualTo(changedAt)
        );
    }

    @Test
    void shouldCreateMeetingStatusChangeWithNullValues() {
        // Given
        Meeting meeting = mock(Meeting.class);

        // When
        MeetingStatusChange statusChange = MeetingStatusChange.builder()
                .meeting(meeting)
                .newStatus("CANCELLED")
                .changedAt(LocalDateTime.now())
                .build();

        // Then
        assertAll(
                () -> assertThat(statusChange.getMeeting()).isEqualTo(meeting),
                () -> assertThat(statusChange.getOldStatus()).isNull(),
                () -> assertThat(statusChange.getNewStatus()).isEqualTo("CANCELLED"),
                () -> assertThat(statusChange.getChangedByUserId()).isNull(),
                () -> assertThat(statusChange.getReason()).isNull(),
                () -> assertThat(statusChange.getChangedAt()).isNotNull()
        );
    }

    @Test
    void shouldUpdateAllFields() {
        // Given
        MeetingStatusChange statusChange = new MeetingStatusChange();
        Meeting newMeeting = mock(Meeting.class);
        LocalDateTime newChangedAt = LocalDateTime.now().plusHours(1);

        // When
        statusChange.setId(1L);
        statusChange.setMeeting(newMeeting);
        statusChange.setOldStatus("CONFIRMED");
        statusChange.setNewStatus("COMPLETED");
        statusChange.setChangedByUserId(456L);
        statusChange.setReason("Meeting finished successfully");
        statusChange.setChangedAt(newChangedAt);

        // Then
        assertAll(
                () -> assertThat(statusChange.getId()).isEqualTo(1L),
                () -> assertThat(statusChange.getMeeting()).isEqualTo(newMeeting),
                () -> assertThat(statusChange.getOldStatus()).isEqualTo("CONFIRMED"),
                () -> assertThat(statusChange.getNewStatus()).isEqualTo("COMPLETED"),
                () -> assertThat(statusChange.getChangedByUserId()).isEqualTo(456L),
                () -> assertThat(statusChange.getReason()).isEqualTo("Meeting finished successfully"),
                () -> assertThat(statusChange.getChangedAt()).isEqualTo(newChangedAt)
        );
    }

    @Test
    void shouldImplementDataAnnotationsCorrectly() {
        // Given
        Meeting meeting = mock(Meeting.class);
        LocalDateTime changedAt = LocalDateTime.now();

        // When
        MeetingStatusChange statusChange = new MeetingStatusChange();
        statusChange.setId(1L);
        statusChange.setMeeting(meeting);
        statusChange.setOldStatus("DRAFT");
        statusChange.setNewStatus("PUBLISHED");
        statusChange.setChangedByUserId(789L);
        statusChange.setReason("Published to calendar");
        statusChange.setChangedAt(changedAt);

        // Then - verify getters work correctly
        assertAll(
                () -> assertThat(statusChange.getId()).isEqualTo(1L),
                () -> assertThat(statusChange.getMeeting()).isEqualTo(meeting),
                () -> assertThat(statusChange.getOldStatus()).isEqualTo("DRAFT"),
                () -> assertThat(statusChange.getNewStatus()).isEqualTo("PUBLISHED"),
                () -> assertThat(statusChange.getChangedByUserId()).isEqualTo(789L),
                () -> assertThat(statusChange.getReason()).isEqualTo("Published to calendar"),
                () -> assertThat(statusChange.getChangedAt()).isEqualTo(changedAt)
        );
    }

    @Test
    void shouldHandleStatusTransition() {
        // Given
        MeetingStatusChange statusChange = MeetingStatusChange.builder()
                .oldStatus("SCHEDULED")
                .newStatus("IN_PROGRESS")
                .reason("Meeting started")
                .changedByUserId(111L)
                .build();

        // Then
        assertAll(
                () -> assertThat(statusChange.getOldStatus()).isEqualTo("SCHEDULED"),
                () -> assertThat(statusChange.getNewStatus()).isEqualTo("IN_PROGRESS"),
                () -> assertThat(statusChange.getReason()).isEqualTo("Meeting started"),
                () -> assertThat(statusChange.getChangedByUserId()).isEqualTo(111L)
        );
    }

    @Test
    void shouldCreateStatusChangeForInitialStatus() {
        // Given - Initial status change (no old status)
        MeetingStatusChange initialStatus = MeetingStatusChange.builder()
                .newStatus("CREATED")
                .reason("Meeting created")
                .changedByUserId(222L)
                .build();

        // Then
        assertAll(
                () -> assertThat(initialStatus.getOldStatus()).isNull(),
                () -> assertThat(initialStatus.getNewStatus()).isEqualTo("CREATED"),
                () -> assertThat(initialStatus.getReason()).isEqualTo("Meeting created"),
                () -> assertThat(initialStatus.getChangedByUserId()).isEqualTo(222L)
        );
    }
}
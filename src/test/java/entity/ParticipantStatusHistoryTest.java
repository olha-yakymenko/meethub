package com.meethub.domain.model.entity;

import com.meethub.domain.model.enums.ParticipationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ParticipantStatusHistoryTest {

    @Test
    void shouldCreateParticipantStatusHistoryWithBuilder() {
        // Given
        MeetingParticipant participant = mock(MeetingParticipant.class);
        LocalDateTime changedAt = LocalDateTime.now();

        // When
        ParticipantStatusHistory history = ParticipantStatusHistory.builder()
                .participant(participant)
                .oldStatus(ParticipationStatus.PENDING)
                .newStatus(ParticipationStatus.CONFIRMED)
                .comment("Participant confirmed attendance")
                .changedByUserId(123L)
                .changedAt(changedAt)
                .build();

        // Then
        assertAll(
                () -> assertThat(history.getParticipant()).isEqualTo(participant),
                () -> assertThat(history.getOldStatus()).isEqualTo(ParticipationStatus.PENDING),
                () -> assertThat(history.getNewStatus()).isEqualTo(ParticipationStatus.CONFIRMED),
                () -> assertThat(history.getComment()).isEqualTo("Participant confirmed attendance"),
                () -> assertThat(history.getChangedByUserId()).isEqualTo(123L),
                () -> assertThat(history.getChangedAt()).isEqualTo(changedAt)
        );
    }

    @Test
    void shouldCreateStatusHistoryForInitialStatus() {
        // Given - initial status (no old status)
        ParticipantStatusHistory initialHistory = ParticipantStatusHistory.builder()
                .newStatus(ParticipationStatus.INVITED)
                .comment("Participant invited to meeting")
                .changedByUserId(456L)
                .build();

        // Then
        assertAll(
                () -> assertThat(initialHistory.getOldStatus()).isNull(),
                () -> assertThat(initialHistory.getNewStatus()).isEqualTo(ParticipationStatus.INVITED),
                () -> assertThat(initialHistory.getComment()).isEqualTo("Participant invited to meeting"),
                () -> assertThat(initialHistory.getChangedByUserId()).isEqualTo(456L),
                () -> assertThat(initialHistory.getChangedAt()).isNull() // Will be set by @CreationTimestamp
        );
    }

    @Test
    void shouldUpdateAllFields() {
        // Given
        ParticipantStatusHistory history = new ParticipantStatusHistory();
        MeetingParticipant newParticipant = mock(MeetingParticipant.class);
        LocalDateTime newChangedAt = LocalDateTime.now();

        // When
        history.setId(1L);
        history.setParticipant(newParticipant);
        history.setOldStatus(ParticipationStatus.CONFIRMED);
        history.setNewStatus(ParticipationStatus.CANCELLED);
        history.setComment("Participant cancelled due to emergency");
        history.setChangedByUserId(789L);
        history.setChangedAt(newChangedAt);

        // Then
        assertAll(
                () -> assertThat(history.getId()).isEqualTo(1L),
                () -> assertThat(history.getParticipant()).isEqualTo(newParticipant),
                () -> assertThat(history.getOldStatus()).isEqualTo(ParticipationStatus.CONFIRMED),
                () -> assertThat(history.getNewStatus()).isEqualTo(ParticipationStatus.CANCELLED),
                () -> assertThat(history.getComment()).isEqualTo("Participant cancelled due to emergency"),
                () -> assertThat(history.getChangedByUserId()).isEqualTo(789L),
                () -> assertThat(history.getChangedAt()).isEqualTo(newChangedAt)
        );
    }

    @Test
    void shouldHandleNullValues() {
        // When
        ParticipantStatusHistory history = ParticipantStatusHistory.builder()
                .newStatus(ParticipationStatus.DECLINED)
                .build();

        // Then
        assertAll(
                () -> assertThat(history.getParticipant()).isNull(),
                () -> assertThat(history.getOldStatus()).isNull(),
                () -> assertThat(history.getComment()).isNull(),
                () -> assertThat(history.getChangedByUserId()).isNull(),
                () -> assertThat(history.getChangedAt()).isNull()
        );
    }

    @Test
    void shouldHandleDifferentStatusTransitions() {
        // Test various status transitions
        ParticipantStatusHistory invitedToConfirmed = ParticipantStatusHistory.builder()
                .oldStatus(ParticipationStatus.INVITED)
                .newStatus(ParticipationStatus.CONFIRMED)
                .comment("Accepted invitation")
                .build();

        ParticipantStatusHistory confirmedToDeclined = ParticipantStatusHistory.builder()
                .oldStatus(ParticipationStatus.CONFIRMED)
                .newStatus(ParticipationStatus.DECLINED)
                .comment("Cannot attend anymore")
                .build();

        ParticipantStatusHistory pendingToTentative = ParticipantStatusHistory.builder()
                .oldStatus(ParticipationStatus.PENDING)
                .newStatus(ParticipationStatus.CONFIRMED)
                .comment("Might attend")
                .build();

        // Then
        assertAll(
                () -> assertThat(invitedToConfirmed.getOldStatus()).isEqualTo(ParticipationStatus.INVITED),
                () -> assertThat(invitedToConfirmed.getNewStatus()).isEqualTo(ParticipationStatus.CONFIRMED),
                () -> assertThat(invitedToConfirmed.getComment()).isEqualTo("Accepted invitation"),

                () -> assertThat(confirmedToDeclined.getOldStatus()).isEqualTo(ParticipationStatus.CONFIRMED),
                () -> assertThat(confirmedToDeclined.getNewStatus()).isEqualTo(ParticipationStatus.DECLINED),
                () -> assertThat(confirmedToDeclined.getComment()).isEqualTo("Cannot attend anymore"),

                () -> assertThat(pendingToTentative.getOldStatus()).isEqualTo(ParticipationStatus.PENDING),
                () -> assertThat(pendingToTentative.getComment()).isEqualTo("Might attend")
        );
    }

    @Test
    void shouldSetCreationTimestampAutomatically() {
        // Given
        ParticipantStatusHistory history = new ParticipantStatusHistory();
        history.setParticipant(mock(MeetingParticipant.class));
        history.setNewStatus(ParticipationStatus.CONFIRMED);

        // When - simulate @CreationTimestamp
        LocalDateTime now = LocalDateTime.now();
        history.setChangedAt(now);

        // Then
        assertAll(
                () -> assertThat(history.getChangedAt()).isEqualTo(now)
        );
    }
}
package com.meethub.domain.model.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class WaitlistEntryTest {

    @Test
    void shouldCreateWaitlistEntryWithBuilder() {
        // Given
        Meeting meeting = mock(Meeting.class);
        User user = mock(User.class);
        LocalDateTime joinedAt = LocalDateTime.now();
        LocalDateTime notifiedAt = joinedAt.plusHours(1);

        // When
        WaitlistEntry entry = WaitlistEntry.builder()
                .meeting(meeting)
                .user(user)
                .position(1)
                .joinedAt(joinedAt)
                .notifiedAt(notifiedAt)
                .autoPromote(true)
                .build();

        // Then
        assertAll(
                () -> assertThat(entry.getMeeting()).isEqualTo(meeting),
                () -> assertThat(entry.getUser()).isEqualTo(user),
                () -> assertThat(entry.getPosition()).isEqualTo(1),
                () -> assertThat(entry.getJoinedAt()).isEqualTo(joinedAt),
                () -> assertThat(entry.getNotifiedAt()).isEqualTo(notifiedAt),
                () -> assertThat(entry.getAutoPromote()).isTrue()
        );
    }

    @Test
    void shouldUpdateAllFields() {
        // Given
        WaitlistEntry entry = new WaitlistEntry();
        Meeting newMeeting = mock(Meeting.class);
        User newUser = mock(User.class);
        LocalDateTime newJoinedAt = LocalDateTime.now().minusDays(1);
        LocalDateTime newNotifiedAt = LocalDateTime.now();

        // When
        entry.setId(1L);
        entry.setMeeting(newMeeting);
        entry.setUser(newUser);
        entry.setPosition(3);
        entry.setJoinedAt(newJoinedAt);
        entry.setNotifiedAt(newNotifiedAt);
        entry.setAutoPromote(false);

        // Then
        assertAll(
                () -> assertThat(entry.getId()).isEqualTo(1L),
                () -> assertThat(entry.getMeeting()).isEqualTo(newMeeting),
                () -> assertThat(entry.getUser()).isEqualTo(newUser),
                () -> assertThat(entry.getPosition()).isEqualTo(3),
                () -> assertThat(entry.getJoinedAt()).isEqualTo(newJoinedAt),
                () -> assertThat(entry.getNotifiedAt()).isEqualTo(newNotifiedAt),
                () -> assertThat(entry.getAutoPromote()).isFalse()
        );
    }

    @Test
    void shouldMarkAsNotified() {
        // Given
        WaitlistEntry entry = WaitlistEntry.builder()
                .meeting(mock(Meeting.class))
                .user(mock(User.class))
                .position(1)
                .build();

        // When
        entry.markAsNotified();

        // Then
        assertAll(
                () -> assertThat(entry.getNotifiedAt()).isNotNull(),
                () -> assertThat(entry.getNotifiedAt()).isBeforeOrEqualTo(LocalDateTime.now())
        );
    }

    @Test
    void shouldCheckCanBePromoted() {
        // Given
        WaitlistEntry autoPromoteEntry = WaitlistEntry.builder()
                .autoPromote(true)
                .build();

        WaitlistEntry noAutoPromoteEntry = WaitlistEntry.builder()
                .autoPromote(false)
                .build();

        // Then
        assertAll(
                () -> assertThat(autoPromoteEntry.canBePromoted()).isTrue(),
                () -> assertThat(noAutoPromoteEntry.canBePromoted()).isFalse()
        );
    }

    @Test
    void shouldHandleDifferentPositions() {
        // Given
        WaitlistEntry firstPosition = WaitlistEntry.builder()
                .position(1)
                .build();

        WaitlistEntry middlePosition = WaitlistEntry.builder()
                .position(5)
                .build();

        WaitlistEntry lastPosition = WaitlistEntry.builder()
                .position(10)
                .build();

        // Then
        assertAll(
                () -> assertThat(firstPosition.getPosition()).isEqualTo(1),
                () -> assertThat(middlePosition.getPosition()).isEqualTo(5),
                () -> assertThat(lastPosition.getPosition()).isEqualTo(10)
        );
    }

    @Test
    void shouldSetJoinedAtAutomatically() {
        // Given
        WaitlistEntry entry = new WaitlistEntry();
        entry.setMeeting(mock(Meeting.class));
        entry.setUser(mock(User.class));
        entry.setPosition(1);

        // When - simulate @CreationTimestamp
        LocalDateTime now = LocalDateTime.now();
        entry.setJoinedAt(now);

        // Then
        assertAll(
                () -> assertThat(entry.getJoinedAt()).isEqualTo(now)
        );
    }

}
package com.meethub.domain.model.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class NotificationScheduleTest {

    @Test
    void shouldCreateNotificationScheduleWithBuilder() {
        // Given
        User user = mock(User.class);
        LocalDateTime createdAt = LocalDateTime.now();

        // When
        NotificationSchedule schedule = NotificationSchedule.builder()
                .user(user)
                .scheduleType("DAILY_SUMMARY")
                .triggerTime("18:00")
                .enabled(true)
                .customSettings("{\"includeWeekends\": false}")
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();

        // Then
        assertAll(
                () -> assertThat(schedule.getUser()).isEqualTo(user),
                () -> assertThat(schedule.getScheduleType()).isEqualTo("DAILY_SUMMARY"),
                () -> assertThat(schedule.getTriggerTime()).isEqualTo("18:00"),
                () -> assertThat(schedule.getEnabled()).isTrue(),
                () -> assertThat(schedule.getCustomSettings()).isEqualTo("{\"includeWeekends\": false}"),
                () -> assertThat(schedule.getCreatedAt()).isEqualTo(createdAt),
                () -> assertThat(schedule.getUpdatedAt()).isEqualTo(createdAt)
        );
    }

    @Test
    void shouldSetDefaultValues() {
        // When
        NotificationSchedule schedule = new NotificationSchedule();

        // Then
        assertAll(
                () -> assertThat(schedule.getEnabled()).isTrue()
        );
    }

    @Test
    void shouldUpdateAllFields() {
        // Given
        NotificationSchedule schedule = new NotificationSchedule();
        User newUser = mock(User.class);
        LocalDateTime updatedAt = LocalDateTime.now();

        // When
        schedule.setId(1L);
        schedule.setUser(newUser);
        schedule.setScheduleType("MEETING_REMINDER");
        schedule.setTriggerTime("09:00");
        schedule.setEnabled(false);
        schedule.setCustomSettings("{\"remindBeforeMinutes\": 30}");
        schedule.setCreatedAt(updatedAt.minusDays(1));
        schedule.setUpdatedAt(updatedAt);

        // Then
        assertAll(
                () -> assertThat(schedule.getId()).isEqualTo(1L),
                () -> assertThat(schedule.getUser()).isEqualTo(newUser),
                () -> assertThat(schedule.getScheduleType()).isEqualTo("MEETING_REMINDER"),
                () -> assertThat(schedule.getTriggerTime()).isEqualTo("09:00"),
                () -> assertThat(schedule.getEnabled()).isFalse(),
                () -> assertThat(schedule.getCustomSettings()).isEqualTo("{\"remindBeforeMinutes\": 30}"),
                () -> assertThat(schedule.getCreatedAt()).isEqualTo(updatedAt.minusDays(1)),
                () -> assertThat(schedule.getUpdatedAt()).isEqualTo(updatedAt)
        );
    }

    @Test
    void shouldHandleNullValues() {
        // When
        NotificationSchedule schedule = NotificationSchedule.builder()
                .user(mock(User.class))
                .scheduleType("WEEKLY_DIGEST")
                .enabled(true)
                .build();

        // Then
        assertAll(
                () -> assertThat(schedule.getTriggerTime()).isNull(),
                () -> assertThat(schedule.getCustomSettings()).isNull(),
                () -> assertThat(schedule.getUpdatedAt()).isNull()
        );
    }

    @Test
    void shouldUseDefaultEnabledValue() {
        // When
        NotificationSchedule schedule1 = NotificationSchedule.builder()
                .user(mock(User.class))
                .scheduleType("TEST")
                .build();

        NotificationSchedule schedule2 = NotificationSchedule.builder()
                .user(mock(User.class))
                .scheduleType("TEST")
                .enabled(false)
                .build();

        // Then
        assertAll(
                () -> assertThat(schedule1.getEnabled()).isTrue(),
                () -> assertThat(schedule2.getEnabled()).isFalse()
        );
    }

    @Test
    void shouldSetCreationTimestamp() {
        // Given
        NotificationSchedule schedule = new NotificationSchedule();
        schedule.setUser(mock(User.class));
        schedule.setScheduleType("TEST");

        // When
        // @CreationTimestamp is handled by Hibernate, but we can set it manually
        LocalDateTime now = LocalDateTime.now();
        schedule.setCreatedAt(now);

        // Then
        assertAll(
                () -> assertThat(schedule.getCreatedAt()).isEqualTo(now),
                () -> assertThat(schedule.getUpdatedAt()).isNull()
        );
    }
}
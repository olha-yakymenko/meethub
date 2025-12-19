package com.meethub.domain.model.entity;

import com.meethub.domain.model.enums.AttendanceTokenStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class AttendanceTokenTest {

    @Test
    void shouldCreateAttendanceTokenWithBuilder() {
        // Given
        User user = mock(User.class);
        Meeting meeting = mock(Meeting.class);
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);
        LocalDateTime createdAt = LocalDateTime.now();

        // When
        AttendanceToken token = AttendanceToken.builder()
                .token("abc123")
                .user(user)
                .meeting(meeting)
                .expiresAt(expiresAt)
                .createdAt(createdAt)
                .status(AttendanceTokenStatus.ACTIVE)
                .build();

        // Then
        assertAll(
                () -> assertThat(token.getToken()).isEqualTo("abc123"),
                () -> assertThat(token.getUser()).isEqualTo(user),
                () -> assertThat(token.getMeeting()).isEqualTo(meeting),
                () -> assertThat(token.getExpiresAt()).isEqualTo(expiresAt),
                () -> assertThat(token.getCreatedAt()).isEqualTo(createdAt),
                () -> assertThat(token.getStatus()).isEqualTo(AttendanceTokenStatus.ACTIVE)
        );
    }

    @Test
    void shouldSetDefaultValuesOnCreate() {
        // Given
        AttendanceToken token = new AttendanceToken();

        // When
        token.onCreate();

        // Then
        assertAll(
                () -> assertThat(token.getCreatedAt()).isNotNull(),
                () -> assertThat(token.getStatus()).isEqualTo(AttendanceTokenStatus.ACTIVE)
        );
    }

    @Test
    void shouldBeValidWhenActiveAndNotExpired() {
        // Given
        AttendanceToken token = AttendanceToken.builder()
                .status(AttendanceTokenStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        // When & Then
        assertThat(token.isValid()).isTrue();
    }

    @Test
    void shouldBeInvalidWhenExpired() {
        // Given
        AttendanceToken token = AttendanceToken.builder()
                .status(AttendanceTokenStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();

        // When & Then
        assertThat(token.isValid()).isFalse();
    }

    @Test
    void shouldBeInvalidWhenNotActive() {
        // Given
        AttendanceToken token = AttendanceToken.builder()
                .status(AttendanceTokenStatus.USED)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        // When & Then
        assertThat(token.isValid()).isFalse();
    }
}
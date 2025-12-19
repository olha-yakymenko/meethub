package com.meethub.domain.model.entity;

import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.model.enums.PermissionLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MeetingParticipantTest {

    private User user;
    private Meeting meeting;
    private MeetingParticipant participant;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        meeting = Meeting.builder()
                .title("Test Meeting")
                .organizer(user)
                .build();

        participant = MeetingParticipant.builder()
                .id(1L)
                .meeting(meeting)
                .user(user)
                .status(ParticipationStatus.CONFIRMED)
                .permissionLevel(PermissionLevel.PARTICIPANT)
                .invitationToken("TOKEN123")
                .comment("Test comment")
                .build();
    }

    @Test
    @DisplayName("should create participant with constructor with meeting and user")
    void shouldCreateWithMeetingAndUserConstructor() {
        MeetingParticipant newParticipant = new MeetingParticipant(meeting, user);

        assertThat(newParticipant.getMeeting()).isEqualTo(meeting);
        assertThat(newParticipant.getUser()).isEqualTo(user);
        assertThat(newParticipant.getStatus()).isEqualTo(ParticipationStatus.INVITED);
        assertThat(newParticipant.getPermissionLevel()).isEqualTo(PermissionLevel.PARTICIPANT);
    }

    @Test
    @DisplayName("should create participant with full constructor")
    void shouldCreateWithFullConstructor() {
        MeetingParticipant newParticipant = new MeetingParticipant(
                meeting,
                user,
                ParticipationStatus.CONFIRMED,
                PermissionLevel.MODERATOR
        );

        assertThat(newParticipant.getMeeting()).isEqualTo(meeting);
        assertThat(newParticipant.getUser()).isEqualTo(user);
        assertThat(newParticipant.getStatus()).isEqualTo(ParticipationStatus.CONFIRMED);
        assertThat(newParticipant.getPermissionLevel()).isEqualTo(PermissionLevel.MODERATOR);
    }

    @Test
    @DisplayName("should check if participant is confirmed")
    void shouldCheckIfConfirmed() {
        participant.setStatus(ParticipationStatus.CONFIRMED);
        assertThat(participant.isConfirmed()).isTrue();

        participant.setStatus(ParticipationStatus.INVITED);
        assertThat(participant.isConfirmed()).isFalse();
    }

    @Test
    @DisplayName("should check if participant is invited")
    void shouldCheckIfInvited() {
        participant.setStatus(ParticipationStatus.INVITED);
        assertThat(participant.isInvited()).isTrue();

        participant.setStatus(ParticipationStatus.CONFIRMED);
        assertThat(participant.isInvited()).isFalse();
    }

    @Test
    @DisplayName("should have correct equals implementation")
    void shouldHaveCorrectEquals() {
        MeetingParticipant sameParticipant = MeetingParticipant.builder()
                .id(1L)
                .meeting(meeting)
                .user(user)
                .build();

        MeetingParticipant differentParticipant = MeetingParticipant.builder()
                .id(2L)
                .meeting(meeting)
                .user(user)
                .build();

        assertThat(participant).isEqualTo(sameParticipant);
        assertThat(participant).isNotEqualTo(differentParticipant);
        assertThat(participant).isNotEqualTo(null);
        assertThat(participant).isNotEqualTo(new Object());
    }

    @Test
    @DisplayName("should have correct hashCode implementation")
    void shouldHaveCorrectHashCode() {
        MeetingParticipant sameParticipant = MeetingParticipant.builder()
                .id(1L)
                .meeting(meeting)
                .user(user)
                .build();

        assertThat(participant.hashCode()).isEqualTo(sameParticipant.hashCode());
    }

    @Test
    @DisplayName("should have informative toString")
    void shouldHaveInformativeToString() {
        String toString = participant.toString();

        assertThat(toString).contains("id=1");
        assertThat(toString).contains("status=CONFIRMED");
        assertThat(toString).contains("permissionLevel=PARTICIPANT");
        assertThat(toString).contains("test@example.com");
    }

    @Test
    @DisplayName("should set and get all fields correctly")
    void shouldSetAndGetAllFields() {
        LocalDateTime now = LocalDateTime.now();

        participant.setId(2L);
        participant.setStatus(ParticipationStatus.ATTENDED);
        participant.setPermissionLevel(PermissionLevel.MODERATOR);
        participant.setComment("Updated comment");
        participant.setInvitationToken("NEW_TOKEN");
        participant.setTokenExpiresAt(now.plusDays(1));
        participant.setResponseDate(now);
        participant.setCreatedAt(now.minusHours(1));
        participant.setUpdatedAt(now);
        participant.setAttendanceConfirmedAt(now);
        participant.setAttendanceTokenUsed("ATTENDANCE_TOKEN");

        assertAll(
                () -> assertThat(participant.getId()).isEqualTo(2L),
                () -> assertThat(participant.getStatus()).isEqualTo(ParticipationStatus.ATTENDED),
                () -> assertThat(participant.getPermissionLevel()).isEqualTo(PermissionLevel.MODERATOR),
                () -> assertThat(participant.getComment()).isEqualTo("Updated comment"),
                () -> assertThat(participant.getInvitationToken()).isEqualTo("NEW_TOKEN"),
                () -> assertThat(participant.getTokenExpiresAt()).isEqualTo(now.plusDays(1)),
                () -> assertThat(participant.getResponseDate()).isEqualTo(now),
                () -> assertThat(participant.getCreatedAt()).isEqualTo(now.minusHours(1)),
                () -> assertThat(participant.getUpdatedAt()).isEqualTo(now),
                () -> assertThat(participant.getAttendanceConfirmedAt()).isEqualTo(now),
                () -> assertThat(participant.getAttendanceTokenUsed()).isEqualTo("ATTENDANCE_TOKEN")
        );
    }

    @ParameterizedTest
    @EnumSource(ParticipationStatus.class)
    @DisplayName("should handle all participation statuses")
    void shouldHandleAllParticipationStatuses(ParticipationStatus status) {
        participant.setStatus(status);

        assertThat(participant.getStatus()).isEqualTo(status);
    }

    @ParameterizedTest
    @EnumSource(PermissionLevel.class)
    @DisplayName("should handle all permission levels")
    void shouldHandleAllPermissionLevels(PermissionLevel permissionLevel) {
        participant.setPermissionLevel(permissionLevel);

        assertThat(participant.getPermissionLevel()).isEqualTo(permissionLevel);
    }

    @Test
    @DisplayName("should return null user email in toString when user is null")
    void shouldHandleNullUserInToString() {
        MeetingParticipant participantWithoutUser = MeetingParticipant.builder()
                .id(1L)
                .meeting(meeting)
                .user(null)
                .build();

        String toString = participantWithoutUser.toString();

        assertThat(toString).contains("user=null");
    }

    // Helper method for multiple assertions
    private void assertAll(Runnable... assertions) {
        for (Runnable assertion : assertions) {
            assertion.run();
        }
    }
}
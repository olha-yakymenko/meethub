package com.meethub.domain.model.entity;

import com.meethub.domain.model.enums.NotificationChannel;
import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.model.enums.PermissionLevel;
import com.meethub.domain.model.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserTest {

    private User user;

    @Mock private Meeting meeting;
    @Mock private MeetingParticipant participant;
    @Mock private MeetingParticipant confirmedParticipant;
    @Mock private MeetingParticipant moderatorParticipant;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("password123")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+48123456789")
                .role(UserRole.PARTICIPANT)
                .enabled(true)
                .twoFactorEnabled(false)
                .failedLoginAttempts(0)
                .emailNotificationsEnabled(true)
                .pushNotificationsEnabled(true)
                .smsNotificationsEnabled(false)
                .digestEnabled(true)
                .digestFrequency("DAILY")
                .timezone("Europe/Warsaw")
                .language("pl")
                .build();

        Set<NotificationChannel> channels = new HashSet<>();
        channels.add(NotificationChannel.EMAIL);
        channels.add(NotificationChannel.PUSH);
        user.setEnabledNotificationChannels(channels);
    }

    @Test
    void shouldCreateUserWithBuilder() {
        // Then
        assertAll(
                () -> assertThat(user.getId()).isEqualTo(1L),
                () -> assertThat(user.getEmail()).isEqualTo("test@example.com"),
                () -> assertThat(user.getFirstName()).isEqualTo("John"),
                () -> assertThat(user.getLastName()).isEqualTo("Doe"),
                () -> assertThat(user.getPhoneNumber()).isEqualTo("+48123456789"),
                () -> assertThat(user.getRole()).isEqualTo(UserRole.PARTICIPANT),
                () -> assertThat(user.getEnabled()).isTrue(),
                () -> assertThat(user.getTwoFactorEnabled()).isFalse(),
                () -> assertThat(user.getFailedLoginAttempts()).isEqualTo(0),
                () -> assertThat(user.getEmailNotificationsEnabled()).isTrue(),
                () -> assertThat(user.getPushNotificationsEnabled()).isTrue(),
                () -> assertThat(user.getSmsNotificationsEnabled()).isFalse(),
                () -> assertThat(user.getDigestEnabled()).isTrue(),
                () -> assertThat(user.getDigestFrequency()).isEqualTo("DAILY"),
                () -> assertThat(user.getTimezone()).isEqualTo("Europe/Warsaw"),
                () -> assertThat(user.getLanguage()).isEqualTo("pl"),
                () -> assertThat(user.getFullName()).isEqualTo("John Doe"),
                () -> assertThat(user.isNotificationChannelEnabled(NotificationChannel.EMAIL)).isTrue(),
                () -> assertThat(user.isNotificationChannelEnabled(NotificationChannel.PUSH)).isTrue()
        );
    }

    @Test
    void shouldSetDefaultValues() {
        // When
        User defaultUser = new User();

        // Then
        assertAll(
                () -> assertThat(defaultUser.getRole()).isEqualTo(UserRole.PARTICIPANT),
                () -> assertThat(defaultUser.getEnabled()).isTrue(),
                () -> assertThat(defaultUser.getTwoFactorEnabled()).isFalse(),
                () -> assertThat(defaultUser.getFailedLoginAttempts()).isEqualTo(0),
                () -> assertThat(defaultUser.getEmailNotificationsEnabled()).isTrue(),
                () -> assertThat(defaultUser.getPushNotificationsEnabled()).isTrue(),
                () -> assertThat(defaultUser.getSmsNotificationsEnabled()).isFalse(),
                () -> assertThat(defaultUser.getDigestEnabled()).isTrue(),
                () -> assertThat(defaultUser.getDigestFrequency()).isEqualTo("DAILY"),
                () -> assertThat(defaultUser.getTimezone()).isEqualTo("Europe/Warsaw"),
                () -> assertThat(defaultUser.getLanguage()).isEqualTo("pl"),
                () -> assertThat(defaultUser.getEnabledNotificationChannels()).isNotNull(),
                () -> assertThat(defaultUser.getMeetingParticipants()).isNotNull(),
                () -> assertThat(defaultUser.getPreferences()).isNotNull(),
                () -> assertThat(defaultUser.getNotifications()).isNotNull()
        );
    }

    @Test
    void shouldCheckUserRoles() {
        // Given
        User admin = User.builder().role(UserRole.ADMIN).build();
        User organizer = User.builder().role(UserRole.ORGANIZER).build();
        User participant = User.builder().role(UserRole.PARTICIPANT).build();

        // Then
        assertAll(
                () -> assertThat(admin.isAdmin()).isTrue(),
                () -> assertThat(admin.isSystemOrganizer()).isFalse(),
                () -> assertThat(admin.isSystemParticipant()).isFalse(),
                () -> assertThat(admin.isOrganizer()).isTrue(), // ADMIN can organize
                () -> assertThat(admin.isEnabled()).isTrue(),

                () -> assertThat(organizer.isSystemOrganizer()).isTrue(),
                () -> assertThat(organizer.isOrganizer()).isTrue(),
                () -> assertThat(organizer.isAdmin()).isFalse(),
                () -> assertThat(organizer.isSystemParticipant()).isFalse(),

                () -> assertThat(participant.isSystemParticipant()).isTrue(),
                () -> assertThat(participant.isOrganizer()).isFalse(),
                () -> assertThat(participant.isAdmin()).isFalse(),
                () -> assertThat(participant.isSystemOrganizer()).isFalse()
        );
    }

    @Test
    void shouldCheckOrganizerOfMeeting() {
        // Given
        User organizerUser = User.builder().id(1L).build();
        User otherUser = User.builder().id(2L).build();
        Meeting testMeeting = mock(Meeting.class);
        when(testMeeting.getOrganizer()).thenReturn(organizerUser);

        // Then
        assertAll(
                () -> assertThat(organizerUser.isOrganizerOf(testMeeting)).isTrue(),
                () -> assertThat(otherUser.isOrganizerOf(testMeeting)).isFalse(),
                () -> assertThat(organizerUser.isOrganizerOf(null)).isFalse()
        );
    }

    @Test
    void shouldGetPermissionLevelInMeeting() {
        // Given
        when(participant.getMeeting()).thenReturn(meeting);
        when(meeting.getId()).thenReturn(1L);
        when(participant.getPermissionLevel()).thenReturn(PermissionLevel.MODERATOR);

        user.getMeetingParticipants().add(participant);

        // When
        PermissionLevel level = user.getPermissionLevelInMeeting(meeting);

        // Then
        assertAll(
                () -> assertThat(level).isEqualTo(PermissionLevel.MODERATOR),
                () -> assertThat(user.getPermissionLevelInMeeting(null)).isNull()
        );
    }

    @Test
    void shouldCheckModeratorOfMeeting() {
        // Given
        when(participant.getMeeting()).thenReturn(meeting);
        when(meeting.getId()).thenReturn(1L);
        when(participant.getPermissionLevel()).thenReturn(PermissionLevel.MODERATOR);

        user.getMeetingParticipants().add(participant);

        // Then
        assertAll(
                () -> assertThat(user.isModeratorOf(meeting)).isTrue(),
                () -> assertThat(user.isModeratorOf(null)).isFalse()
        );
    }

    @Test
    void shouldGetParticipationStatus() {
        // Given
        when(participant.getMeeting()).thenReturn(meeting);
        when(meeting.getId()).thenReturn(1L);
        when(participant.getStatus()).thenReturn(ParticipationStatus.CONFIRMED);

        user.getMeetingParticipants().add(participant);

        // When
        ParticipationStatus status = user.getParticipationStatus(meeting);

        // Then
        assertAll(
                () -> assertThat(status).isEqualTo(ParticipationStatus.CONFIRMED),
                () -> assertThat(user.getParticipationStatus(null)).isNull(),
                () -> assertThat(user.isConfirmedParticipant(meeting)).isTrue(),
                () -> assertThat(user.isConfirmedParticipant(null)).isFalse()
        );
    }

    @Test
    void shouldCheckNotificationChannels() {
        // Given
        Set<NotificationChannel> channels = new HashSet<>();
        channels.add(NotificationChannel.EMAIL);
        user.setEnabledNotificationChannels(channels);

        // Then
        assertAll(
                () -> assertThat(user.isNotificationChannelEnabled(NotificationChannel.EMAIL)).isTrue(),
                () -> assertThat(user.isNotificationChannelEnabled(NotificationChannel.PUSH)).isFalse(),
                () -> assertThat(user.isNotificationChannelEnabled(NotificationChannel.IN_APP)).isFalse()
        );
    }

    @Test
    void shouldGetMeetingParticipant() {
        // Given
        when(participant.getMeeting()).thenReturn(meeting);
        when(meeting.getId()).thenReturn(1L);

        user.getMeetingParticipants().add(participant);

        // When
        MeetingParticipant found = user.getMeetingParticipant(meeting);

        // Then
        assertAll(
                () -> assertThat(found).isEqualTo(participant),
                () -> assertThat(user.getMeetingParticipant(null)).isNull()
        );
    }

    @Test
    void shouldUpdateAllFields() {
        // Given
        User updatedUser = new User();
        LocalDateTime lockedUntil = LocalDateTime.now().plusHours(1);
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        LocalDateTime updatedAt = LocalDateTime.now();

        Set<NotificationChannel> newChannels = new HashSet<>();
        newChannels.add(NotificationChannel.IN_APP);

        // When
        updatedUser.setId(2L);
        updatedUser.setEmail("updated@example.com");
        updatedUser.setPassword("newpassword");
        updatedUser.setFirstName("Jane");
        updatedUser.setLastName("Smith");
        updatedUser.setPhoneNumber("+48987654321");
        updatedUser.setRole(UserRole.ADMIN);
        updatedUser.setEnabled(false);
        updatedUser.setTwoFactorEnabled(true);
        updatedUser.setFailedLoginAttempts(3);
        updatedUser.setAccountLockedUntil(lockedUntil);
        updatedUser.setEnabledNotificationChannels(newChannels);
        updatedUser.setEmailNotificationsEnabled(false);
        updatedUser.setPushNotificationsEnabled(false);
        updatedUser.setSmsNotificationsEnabled(true);
        updatedUser.setDigestEnabled(false);
        updatedUser.setDigestFrequency("WEEKLY");
        updatedUser.setTimezone("America/New_York");
        updatedUser.setLanguage("en");
        updatedUser.setCreatedAt(createdAt);
        updatedUser.setUpdatedAt(updatedAt);

        // Then
        assertAll(
                () -> assertThat(updatedUser.getId()).isEqualTo(2L),
                () -> assertThat(updatedUser.getEmail()).isEqualTo("updated@example.com"),
                () -> assertThat(updatedUser.getFirstName()).isEqualTo("Jane"),
                () -> assertThat(updatedUser.getLastName()).isEqualTo("Smith"),
                () -> assertThat(updatedUser.getFullName()).isEqualTo("Jane Smith"),
                () -> assertThat(updatedUser.getPhoneNumber()).isEqualTo("+48987654321"),
                () -> assertThat(updatedUser.getRole()).isEqualTo(UserRole.ADMIN),
                () -> assertThat(updatedUser.getEnabled()).isFalse(),
                () -> assertThat(updatedUser.getTwoFactorEnabled()).isTrue(),
                () -> assertThat(updatedUser.getFailedLoginAttempts()).isEqualTo(3),
                () -> assertThat(updatedUser.getAccountLockedUntil()).isEqualTo(lockedUntil),
                () -> assertThat(updatedUser.isNotificationChannelEnabled(NotificationChannel.IN_APP)).isTrue(),
                () -> assertThat(updatedUser.getEmailNotificationsEnabled()).isFalse(),
                () -> assertThat(updatedUser.getPushNotificationsEnabled()).isFalse(),
                () -> assertThat(updatedUser.getSmsNotificationsEnabled()).isTrue(),
                () -> assertThat(updatedUser.getDigestEnabled()).isFalse(),
                () -> assertThat(updatedUser.getDigestFrequency()).isEqualTo("WEEKLY"),
                () -> assertThat(updatedUser.getTimezone()).isEqualTo("America/New_York"),
                () -> assertThat(updatedUser.getLanguage()).isEqualTo("en"),
                () -> assertThat(updatedUser.getCreatedAt()).isEqualTo(createdAt),
                () -> assertThat(updatedUser.getUpdatedAt()).isEqualTo(updatedAt)
        );
    }

    @Test
    void shouldHandleLockedAccount() {
        // Given
        User lockedUser = new User();
        LocalDateTime lockTime = LocalDateTime.now().plusMinutes(30);
        lockedUser.setAccountLockedUntil(lockTime);

        User notLockedUser = new User();
        notLockedUser.setAccountLockedUntil(null);

        User expiredLockUser = new User();
        expiredLockUser.setAccountLockedUntil(LocalDateTime.now().minusMinutes(30));

        // Then
        assertAll(
                () -> assertThat(lockedUser.getAccountLockedUntil()).isEqualTo(lockTime),
                () -> assertThat(notLockedUser.getAccountLockedUntil()).isNull(),
                () -> assertThat(expiredLockUser.getAccountLockedUntil()).isBefore(LocalDateTime.now())
        );
    }
}
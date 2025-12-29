// NotificationServiceImplTest.java
package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.*;
import com.meethub.domain.model.enums.*;
import com.meethub.domain.model.request.NotificationPreferencesRequest;
import com.meethub.domain.model.response.NotificationResponse;
import com.meethub.domain.model.response.UserProfileResponse;
import com.meethub.domain.repository.jpa.*;
import com.meethub.domain.service.AttendanceTokenService;
import com.meethub.domain.service.EmailService;
import com.meethub.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPreferenceRepository userPreferenceRepository;

    @Mock
    private NotificationScheduleRepository scheduleRepository;

    @Mock
    private EmailTemplateRepository emailTemplateRepository;

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private MeetingParticipantRepository participantRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private AttendanceTokenService attendanceTokenService;

    @Mock
    private AttendanceTokenRepository attendanceTokenRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User testUser;
    private Meeting testMeeting;
    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .firstName("Jan")
                .lastName("Kowalski")
                .phoneNumber("+48123456789")
                .timezone("Europe/Warsaw")
                .language("pl")
                .emailNotificationsEnabled(true)
                .pushNotificationsEnabled(true)
                .smsNotificationsEnabled(false)
                .digestEnabled(true)
                .digestFrequency("DAILY")
                .enabledNotificationChannels(Set.of(NotificationChannel.EMAIL, NotificationChannel.IN_APP))
                .createdAt(LocalDateTime.now())
                .build();

        testMeeting = Meeting.builder()
                .title("Test Meeting")
                .startDate(LocalDateTime.now().plusHours(1))
                .organizer(testUser)
                .build();

        testMeeting.setId(1L);

        testNotification = Notification.builder()
                .id(1L)
                .user(testUser)
                .title("Test Notification")
                .message("Test message")
                .type(NotificationType.MEETING_REMINDER)
                .channel(NotificationChannel.IN_APP)
                .status(NotificationStatus.SENT)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void markAsRead_ShouldMarkNotificationAsRead() {
        Long notificationId = 1L;
        Long userId = 1L;

        Notification notification = Notification.builder()
                .id(notificationId)
                .user(testUser)
                .status(NotificationStatus.SENT)
                .build();

        when(notificationRepository.findByIdAndUserId(notificationId, userId))
                .thenReturn(Optional.of(notification));

        notificationService.markAsRead(notificationId, userId);

        assertAll(
                () -> assertNotNull(notification.getReadAt()),
                () -> assertEquals(NotificationStatus.READ, notification.getStatus())
        );

        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsRead_NotificationAlreadyRead_ShouldDoNothing() {
        Long notificationId = 1L;
        Long userId = 1L;
        LocalDateTime alreadyReadAt = LocalDateTime.now().minusMinutes(5);

        Notification notification = Notification.builder()
                .id(notificationId)
                .user(testUser)
                .status(NotificationStatus.READ)
                .readAt(alreadyReadAt)
                .build();

        when(notificationRepository.findByIdAndUserId(notificationId, userId))
                .thenReturn(Optional.of(notification));

        notificationService.markAsRead(notificationId, userId);

        assertAll(
                () -> assertEquals(alreadyReadAt, notification.getReadAt())
        );

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAsRead_NotificationNotFound_ShouldThrowException() {
        Long notificationId = 999L;
        Long userId = 1L;

        when(notificationRepository.findByIdAndUserId(notificationId, userId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> notificationService.markAsRead(notificationId, userId));
    }

    @Test
    void markAllAsRead_ShouldMarkAllUnreadNotificationsAsRead() {
        Long userId = 1L;
        List<Notification> unreadNotifications = Arrays.asList(
                Notification.builder().id(1L).user(testUser).status(NotificationStatus.SENT).build(),
                Notification.builder().id(2L).user(testUser).status(NotificationStatus.SENT).build()
        );

        when(notificationRepository.findByUserIdAndStatus(userId, NotificationStatus.SENT))
                .thenReturn(unreadNotifications);

        notificationService.markAllAsRead(userId);

        unreadNotifications.forEach(notification -> {
            assertAll(
                    () -> assertNotNull(notification.getReadAt()),
                    () -> assertEquals(NotificationStatus.READ, notification.getStatus())
            );
        });

        verify(notificationRepository).saveAll(unreadNotifications);
    }

    @Test
    void getUserNotifications_ShouldReturnPageOfNotifications() {
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        List<Notification> notifications = Arrays.asList(
                testNotification,
                Notification.builder()
                        .id(2L)
                        .user(testUser)
                        .title("Another Notification")
                        .message("Another message")
                        .type(NotificationType.MEETING_UPDATE)
                        .channel(NotificationChannel.EMAIL)
                        .status(NotificationStatus.SENT)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        Page<Notification> notificationPage = new PageImpl<>(notifications, pageable, notifications.size());

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable))
                .thenReturn(notificationPage);

        Page<NotificationResponse> result = notificationService.getUserNotifications(userId, pageable);

        assertAll(
                () -> assertEquals(2, result.getTotalElements())
        );

        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Test
    void getUnreadCount_ShouldReturnCorrectCount() {
        Long userId = 1L;
        Long expectedCount = 5L;

        when(notificationRepository.countByUserIdAndStatus(userId, NotificationStatus.SENT))
                .thenReturn(expectedCount);

        Long result = notificationService.getUnreadCount(userId);

        assertAll(
                () -> assertEquals(expectedCount, result)
        );

        verify(notificationRepository).countByUserIdAndStatus(userId, NotificationStatus.SENT);
    }

    @Test
    void getUserProfileWithPreferences_ShouldReturnCompleteProfile() {
        Long userId = 1L;
        Long totalNotifications = 10L;
        Long unreadCount = 3L;
        Long upcomingMeetings = 2L;

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(notificationRepository.countByUserId(userId)).thenReturn(totalNotifications);
        when(notificationRepository.countByUserIdAndStatus(userId, NotificationStatus.SENT))
                .thenReturn(unreadCount);
        when(meetingRepository.countUpcomingMeetingsByUserId(userId))
                .thenReturn(upcomingMeetings);

        UserProfileResponse response = notificationService.getUserProfileWithPreferences(userId);

        assertAll(
                () -> assertNotNull(response),
                () -> assertEquals(userId, response.getId()),
                () -> assertEquals(testUser.getEmail(), response.getEmail()),
                () -> assertEquals(testUser.getFirstName(), response.getFirstName()),
                () -> assertEquals(testUser.getLastName(), response.getLastName()),
                () -> assertEquals(totalNotifications, response.getTotalNotifications()),
                () -> assertEquals(unreadCount, response.getUnreadNotifications()),
                () -> assertEquals(upcomingMeetings, response.getUpcomingMeetings())
        );
    }

    @Test
    void createNotificationFromTemplate_NotificationNotAllowed_ShouldReturnNull() {
        Long userId = 1L;
        String templateKey = "test_template";
        Map<String, String> variables = new HashMap<>();
        NotificationType type = NotificationType.MEETING_REMINDER;
        NotificationChannel channel = NotificationChannel.EMAIL;

        User userWithDisabledEmail = User.builder()
                .id(userId)
                .email("test@example.com")
                .firstName("Jan")
                .emailNotificationsEnabled(false)
                .enabledNotificationChannels(Set.of(NotificationChannel.IN_APP))
                .language("pl")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(userWithDisabledEmail));

        Notification result = notificationService.createNotificationFromTemplate(
                userId, templateKey, variables, type, channel);

        assertAll(
                () -> assertNull(result)
        );

        verify(emailTemplateRepository, never()).findByTemplateKeyAndLanguage(anyString(), anyString());
    }

    @Test
    void getInAppMessages_ShouldReturnMessages() {
        Long userId = 1L;
        List<String> expectedMessages = Arrays.asList("Message 1", "Message 2");

        when(notificationRepository.findInAppMessagesByUserId(userId))
                .thenReturn(expectedMessages);

        List<String> result = notificationService.getInAppMessages(userId);

        assertAll(
                () -> assertEquals(expectedMessages, result)
        );

        verify(notificationRepository).findInAppMessagesByUserId(userId);
    }

    @Test
    void getRecentInAppMessages_WithValidLimit_ShouldReturnLimitedMessages() {
        Long userId = 1L;
        int limit = 5;
        List<String> expectedMessages = Arrays.asList("Message 1", "Message 2");

        when(notificationRepository.findInAppMessagesByUserId(eq(userId), any(Pageable.class)))
                .thenReturn(expectedMessages);

        List<String> result = notificationService.getRecentInAppMessages(userId, limit);

        assertAll(
                () -> assertEquals(expectedMessages, result)
        );

        verify(notificationRepository).findInAppMessagesByUserId(eq(userId), any(Pageable.class));
    }

    @Test
    void getRecentInAppMessages_WithInvalidLimit_ShouldThrowException() {
        Long userId = 1L;
        int invalidLimit = 0;

        assertThrows(IllegalArgumentException.class,
                () -> notificationService.getRecentInAppMessages(userId, invalidLimit));
    }

    @Test
    void isNotificationAllowed_WithEnabledChannelAndPreference_ShouldReturnTrue() {
        NotificationType type = NotificationType.MEETING_REMINDER;
        NotificationChannel channel = NotificationChannel.EMAIL;

        when(userPreferenceRepository.findByUserIdAndPreferenceKey(
                testUser.getId(), "meeting_reminders"))
                .thenReturn(Optional.empty());

        boolean result = notificationService.isNotificationAllowed(testUser, type, channel);

        assertAll(
                () -> assertTrue(result)
        );
    }

    @Test
    void isNotificationAllowed_WithDisabledChannel_ShouldReturnFalse() {
        NotificationType type = NotificationType.MEETING_REMINDER;
        NotificationChannel channel = NotificationChannel.PUSH;

        boolean result = notificationService.isNotificationAllowed(testUser, type, channel);

        assertAll(
                () -> assertFalse(result)
        );
    }




    @Test
    void scheduleMeetingReminder_ShouldCreateNotificationsForEnabledChannels() {
        // Given
        Long meetingId = 1L;
        Long userId = 1L;
        LocalDateTime reminderTime = LocalDateTime.now().plusHours(1);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(testMeeting));

        // Mock preferences - allow all notifications
        when(userPreferenceRepository.findByUserIdAndPreferenceKey(userId, "meeting_reminders"))
                .thenReturn(Optional.empty());

        // When
        notificationService.scheduleMeetingReminder(meetingId, userId, reminderTime);

        // Then
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void scheduleMeetingReminder_UserNotFound_ShouldThrowException() {
        // Given
        Long meetingId = 1L;
        Long userId = 999L;
        LocalDateTime reminderTime = LocalDateTime.now().plusHours(1);

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> notificationService.scheduleMeetingReminder(meetingId, userId, reminderTime));
    }

    @Test
    void scheduleMeetingReminder_MeetingNotFound_ShouldThrowException() {
        // Given
        Long meetingId = 999L;
        Long userId = 1L;
        LocalDateTime reminderTime = LocalDateTime.now().plusHours(1);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(meetingRepository.findById(meetingId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> notificationService.scheduleMeetingReminder(meetingId, userId, reminderTime));
    }

    @Test
    void updateNotificationPreferences_ShouldUpdateUserAndPreferences() {
        // Given
        Long userId = 1L;
        NotificationPreferencesRequest request = new NotificationPreferencesRequest();
        request.setEmailNotificationsEnabled(false);
        request.setPushNotificationsEnabled(true);
        request.setMeetingReminders(false);
        request.setEnabledChannels(Set.of(NotificationChannel.IN_APP, NotificationChannel.PUSH));

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userPreferenceRepository.findByUserIdAndPreferenceKey(userId, "meeting_reminders"))
                .thenReturn(Optional.of(new UserPreference()));

        // When
        notificationService.updateNotificationPreferences(userId, request);

        // Then
        verify(userRepository).save(testUser);
        verify(userPreferenceRepository, atLeastOnce()).save(any(UserPreference.class));

        assertAll(
                () -> assertFalse(testUser.getEmailNotificationsEnabled()),
                () -> assertTrue(testUser.getPushNotificationsEnabled()),
                () -> assertEquals(Set.of(NotificationChannel.IN_APP, NotificationChannel.PUSH),
                        testUser.getEnabledNotificationChannels())
        );
    }

    @Test
    void updateNotificationPreferences_UserNotFound_ShouldThrowException() {
        // Given
        Long userId = 999L;
        NotificationPreferencesRequest request = new NotificationPreferencesRequest();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> notificationService.updateNotificationPreferences(userId, request));
    }
    @Test
    void sendParticipantJoinedNotification_ShouldCreateNotifications() {
        // Given
        User organizer = testUser;
        User participant = User.builder()
                .id(2L)
                .firstName("Anna")
                .lastName("Nowak")
                .build();

        // Mock find user by ID (ważne: organizator musi być znaleziony)
        when(userRepository.findById(organizer.getId())).thenReturn(Optional.of(organizer));

        // Mock preferences - upewnij się, że powiadomienia są dozwolone
        when(userPreferenceRepository.findByUserIdAndPreferenceKey(organizer.getId(), "meeting_updates"))
                .thenReturn(Optional.empty());

        when(participantRepository.countByMeetingIdAndStatus(eq(testMeeting.getId()), any(ParticipationStatus.class)))
                .thenReturn(5L);

        // Mock email template
        EmailTemplate template = EmailTemplate.builder()
                .bodyTemplate("{{participantName}} dołączył(a) do spotkania")
                .subject("Nowy uczestnik")
                .build();

        when(emailTemplateRepository.findByTemplateKeyAndLanguage("participant_joined", "pl"))
                .thenReturn(Optional.of(template));

        // Mock notification repository - zwróć poprawny obiekt
        Notification mockNotification = Notification.builder()
                .id(1L)
                .user(organizer)
                .title("Nowy uczestnik")
                .message("Anna Nowak dołączył(a) do spotkania")
                .type(NotificationType.MEETING_UPDATE)
                .channel(NotificationChannel.IN_APP)
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(mockNotification);

        // When
        notificationService.sendParticipantJoinedNotification(organizer, participant, testMeeting);

        // Then
        verify(notificationRepository, atLeast(1)).save(any(Notification.class));
    }

    @Test
    void sendJoinRequestNotification_ShouldCreateNotifications() {
        // Given
        User organizer = testUser;
        User requester = User.builder()
                .id(2L)
                .firstName("Anna")
                .lastName("Nowak")
                .email("anna@example.com")
                .build();

        // Upewnij się, że meeting ma ID
        testMeeting.setId(1L);

        // Mock find user by ID
        when(userRepository.findById(organizer.getId())).thenReturn(Optional.of(organizer));

        // Mock preferences - upewnij się, że powiadomienia są dozwolone
        when(userPreferenceRepository.findByUserIdAndPreferenceKey(organizer.getId(), "meeting_invitations"))
                .thenReturn(Optional.empty());

        // Mock email template
        EmailTemplate template = EmailTemplate.builder()
                .bodyTemplate("{{requesterName}} chce dołączyć do spotkania")
                .subject("Nowa prośba o dołączenie")
                .build();

        when(emailTemplateRepository.findByTemplateKeyAndLanguage("join_request", "pl"))
                .thenReturn(Optional.of(template));

        // Mock notification repository
        Notification mockNotification = Notification.builder()
                .id(1L)
                .user(organizer)
                .title("Nowa prośba o dołączenie")
                .message("Anna Nowak chce dołączyć do spotkania")
                .type(NotificationType.MEETING_INVITATION)
                .channel(NotificationChannel.IN_APP)
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(mockNotification);

        // When
        notificationService.sendJoinRequestNotification(organizer, requester, testMeeting);

        // Then
        verify(notificationRepository, atLeast(1)).save(any(Notification.class));
    }

    @Test
    void sendRequestApprovedNotification_ShouldCreateNotifications() {
        // Given
        User user = testUser;

        // Upewnij się, że meeting ma ID i organizatora
        testMeeting.setId(1L);
        testMeeting.setOrganizer(User.builder().id(3L).firstName("Organizer").lastName("Test").build());

        // Mock find user by ID
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        // Mock preferences
        when(userPreferenceRepository.findByUserIdAndPreferenceKey(user.getId(), "meeting_invitations"))
                .thenReturn(Optional.empty());

        // Mock email template
        EmailTemplate template = EmailTemplate.builder()
                .bodyTemplate("Twoja prośba została zaakceptowana")
                .subject("Prośba zaakceptowana")
                .build();

        when(emailTemplateRepository.findByTemplateKeyAndLanguage("request_approved", "pl"))
                .thenReturn(Optional.of(template));

        // Mock notification repository
        Notification mockNotification = Notification.builder()
                .id(1L)
                .user(user)
                .title("Prośba zaakceptowana")
                .message("Twoja prośba została zaakceptowana")
                .type(NotificationType.MEETING_INVITATION)
                .channel(NotificationChannel.IN_APP)
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(mockNotification);

        // When
        notificationService.sendRequestApprovedNotification(user, testMeeting);

        // Then
        verify(notificationRepository, atLeast(1)).save(any(Notification.class));
    }

    @Test
    void sendRequestRejectedNotification_ShouldCreateNotifications() {
        // Given
        User user = testUser;

        // Upewnij się, że meeting ma ID i organizatora
        testMeeting.setId(1L);
        testMeeting.setOrganizer(User.builder().id(3L).firstName("Organizer").lastName("Test").build());

        // Mock find user by ID
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        // Mock preferences
        when(userPreferenceRepository.findByUserIdAndPreferenceKey(user.getId(), "meeting_updates"))
                .thenReturn(Optional.empty());

        // Mock email template
        EmailTemplate template = EmailTemplate.builder()
                .bodyTemplate("Twoja prośba została odrzucona")
                .subject("Prośba odrzucona")
                .build();

        when(emailTemplateRepository.findByTemplateKeyAndLanguage("request_rejected", "pl"))
                .thenReturn(Optional.of(template));

        // Mock notification repository
        Notification mockNotification = Notification.builder()
                .id(1L)
                .user(user)
                .title("Prośba odrzucona")
                .message("Twoja prośba została odrzucona")
                .type(NotificationType.MEETING_UPDATE)
                .channel(NotificationChannel.IN_APP)
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(mockNotification);

        // When
        notificationService.sendRequestRejectedNotification(user, testMeeting);

        // Then
        verify(notificationRepository, atLeast(1)).save(any(Notification.class));
    }


    @Test
    void createNotificationFromTemplate_TemplateNotFound_ShouldThrowException() {
        // Given
        Long userId = 1L;
        String templateKey = "non_existent";
        Map<String, String> variables = new HashMap<>();
        NotificationType type = NotificationType.MEETING_REMINDER;
        NotificationChannel channel = NotificationChannel.EMAIL;

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userPreferenceRepository.findByUserIdAndPreferenceKey(userId, "meeting_reminders"))
                .thenReturn(Optional.empty());
        when(emailTemplateRepository.findByTemplateKeyAndLanguage(templateKey, "pl"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> notificationService.createNotificationFromTemplate(
                        userId, templateKey, variables, type, channel));
    }

    @Test
    void sendEmailNotification_ShouldSendEmailAndUpdateStatus() {
        // Given
        Notification notification = Notification.builder()
                .id(1L)
                .user(testUser)
                .title("Test notification")
                .message("Test message")
                .channel(NotificationChannel.EMAIL)
                .status(NotificationStatus.PENDING)
                .build();

        doNothing().when(emailService).sendTemplateEmail(
                anyString(), anyString(), anyString(), anyMap());

        // When
        notificationService.sendEmailNotification(notification);

        // Then
        assertAll(
                () -> assertEquals(NotificationStatus.DELIVERED, notification.getStatus()),
                () -> assertNotNull(notification.getDeliveredAt())
        );

        verify(emailService).sendTemplateEmail(
                eq("test@example.com"),
                eq("Test notification"),
                eq("notification_email"),
                anyMap());
    }

    @Test
    void sendEmailNotification_Exception_ShouldThrow() {
        // Given
        Notification notification = Notification.builder()
                .id(1L)
                .user(testUser)
                .title("Test notification")
                .message("Test message")
                .channel(NotificationChannel.EMAIL)
                .status(NotificationStatus.PENDING)
                .build();

        doThrow(new RuntimeException("Email error"))
                .when(emailService).sendTemplateEmail(anyString(), anyString(), anyString(), anyMap());

        // When & Then
        assertThrows(RuntimeException.class,
                () -> notificationService.sendEmailNotification(notification));

        // Notification should not be updated on failure
        assertEquals(NotificationStatus.PENDING, notification.getStatus());
    }

    @Test
    void sendInAppNotification_ShouldUpdateStatus() {
        // Given
        Notification notification = Notification.builder()
                .id(1L)
                .user(testUser)
                .title("Test notification")
                .message("Test message")
                .channel(NotificationChannel.IN_APP)
                .status(NotificationStatus.PENDING)
                .build();

        // When
        notificationService.sendInAppNotification(notification);

        // Then
        assertAll(
                () -> assertEquals(NotificationStatus.DELIVERED, notification.getStatus()),
                () -> assertNotNull(notification.getDeliveredAt())
        );
    }

    @Test
    void getUserPreference_ShouldReturnValue() {
        // Given
        Long userId = 1L;
        String key = "meeting_reminders";
        String expectedValue = "true";

        UserPreference preference = new UserPreference();
        preference.setPreferenceValue(expectedValue);

        when(userPreferenceRepository.findByUserIdAndPreferenceKey(userId, key))
                .thenReturn(Optional.of(preference));

        // When
        String result = notificationService.getUserPreference(testUser, key, "false");

        // Then
        assertEquals(expectedValue, result);
    }

    @Test
    void getUserPreference_NotFound_ShouldReturnDefault() {
        // Given
        String key = "non_existent";
        String defaultValue = "default";

        when(userPreferenceRepository.findByUserIdAndPreferenceKey(testUser.getId(), key))
                .thenReturn(Optional.empty());

        // When
        String result = notificationService.getUserPreference(testUser, key, defaultValue);

        // Then
        assertEquals(defaultValue, result);
    }

    @Test
    void personalizeTemplate_ShouldReplaceVariables() {
        // Given
        String template = "Hello {{name}}, welcome to {{company}}";
        Map<String, String> variables = new HashMap<>();
        variables.put("name", "Jan");
        variables.put("company", "MeetHub");

        // When
        String result = notificationService.personalizeTemplate(template, variables);

        // Then
        assertEquals("Hello Jan, welcome to MeetHub", result);
    }

    @Test
    void personalizeTemplate_NoVariables_ShouldReturnOriginal() {
        // Given
        String template = "Hello World";
        Map<String, String> variables = new HashMap<>();

        // When
        String result = notificationService.personalizeTemplate(template, variables);

        // Then
        assertEquals("Hello World", result);
    }

    @Test
    void getCurrentParticipantsCount_ShouldReturnCount() {
        // Given
        Long meetingId = 1L;
        long expectedCount = 5L;

        when(participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.CONFIRMED))
                .thenReturn(expectedCount);

        // When
        long result = notificationService.getCurrentParticipantsCount(meetingId);

        // Then
        assertEquals(expectedCount, result);
    }

    @Test
    void getCurrentParticipantsCount_Exception_ShouldReturnZero() {
        // Given
        Long meetingId = 1L;

        when(participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.CONFIRMED))
                .thenThrow(new RuntimeException("DB error"));

        // When
        long result = notificationService.getCurrentParticipantsCount(meetingId);

        // Then
        assertEquals(0L, result);
    }

    @Test
    void getPreferenceKeyForType_ShouldReturnCorrectKeys() {
        // Testowanie metody prywatnej przez refleksję
        assertAll(
                () -> assertEquals("meeting_invitations",
                        getPrivateMethod("getPreferenceKeyForType", NotificationType.class)
                                .invoke(notificationService, NotificationType.MEETING_INVITATION)),
                () -> assertEquals("meeting_reminders",
                        getPrivateMethod("getPreferenceKeyForType", NotificationType.class)
                                .invoke(notificationService, NotificationType.MEETING_REMINDER)),
                () -> assertEquals("meeting_updates",
                        getPrivateMethod("getPreferenceKeyForType", NotificationType.class)
                                .invoke(notificationService, NotificationType.MEETING_UPDATE)),
                () -> assertEquals("task_assignments",
                        getPrivateMethod("getPreferenceKeyForType", NotificationType.class)
                                .invoke(notificationService, NotificationType.TASK_ASSIGNMENT)),
                () -> assertEquals("security_alerts",
                        getPrivateMethod("getPreferenceKeyForType", NotificationType.class)
                                .invoke(notificationService, NotificationType.SECURITY_ALERT))
        );
    }

    // Metoda pomocnicza do testowania metod prywatnych
    private java.lang.reflect.Method getPrivateMethod(String methodName, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        java.lang.reflect.Method method = NotificationServiceImpl.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method;
    }

    @Test
    void sendNotificationBasedOnChannel_Email_ShouldSendEmail() {
        // Given
        Notification notification = Notification.builder()
                .id(1L)
                .user(testUser)
                .title("Test")
                .channel(NotificationChannel.EMAIL)
                .status(NotificationStatus.PENDING)
                .build();

        doNothing().when(emailService).sendTemplateEmail(anyString(), anyString(), anyString(), anyMap());

        // When
        notificationService.sendNotificationBasedOnChannel(notification);

        // Then
        verify(emailService).sendTemplateEmail(anyString(), anyString(), anyString(), anyMap());
        assertEquals(NotificationStatus.SENT, notification.getStatus());
        assertNotNull(notification.getSentAt());
    }

    @Test
    void sendNotificationBasedOnChannel_InApp_ShouldSendInApp() {
        // Given
        Notification notification = Notification.builder()
                .id(1L)
                .user(testUser)
                .title("Test")
                .channel(NotificationChannel.IN_APP)
                .status(NotificationStatus.PENDING)
                .build();

        // When
        notificationService.sendNotificationBasedOnChannel(notification);

        // Then
        assertEquals(NotificationStatus.SENT, notification.getStatus());
        assertNotNull(notification.getSentAt());
    }

    @Test
    void sendEmailForNotification_Success_ShouldSendEmail() {
        // Given
        Notification notification = Notification.builder()
                .id(1L)
                .user(testUser)
                .templateKey("test_template")
                .status(NotificationStatus.PENDING)
                .build();

        Map<String, String> variables = new HashMap<>();
        variables.put("meetingId", "123");

        EmailTemplate template = EmailTemplate.builder()
                .bodyTemplate("Body {{userName}}")
                .subject("Subject {{userName}}")
                .build();

        when(emailTemplateRepository.findByTemplateKeyAndLanguage("test_template", "pl"))
                .thenReturn(Optional.of(template));
        when(emailTemplateRepository.findByTemplateKeyAndLanguage("test_template", "pl"))
                .thenReturn(Optional.of(template));

        doNothing().when(emailService).sendHtmlEmail(anyString(), anyString(), anyString());
        when(attendanceTokenService.getTokenForUserAndMeeting(anyLong(), anyLong()))
                .thenReturn(Optional.empty());
        when(meetingRepository.findById(123L)).thenReturn(Optional.of(testMeeting));
        when(attendanceTokenService.createToken(any(User.class), any(Meeting.class)))
                .thenReturn(AttendanceToken.builder().token("TEST-TOKEN").build());

        // When
        notificationService.sendEmailForNotification(notification, testUser,
                "Subject", "Body", variables);

        // Then
        verify(emailService).sendHtmlEmail(eq("test@example.com"), anyString(), anyString());
        assertEquals(NotificationStatus.SENT, notification.getStatus());
        assertNotNull(notification.getSentAt());
    }

    @Test
    void sendEmailForNotification_Exception_ShouldMarkAsFailed() {
        // Given
        Notification notification = Notification.builder()
                .id(1L)
                .user(testUser)
                .templateKey("test_template")
                .status(NotificationStatus.PENDING)
                .build();

        when(emailTemplateRepository.findByTemplateKeyAndLanguage("test_template", "pl"))
                .thenThrow(new RuntimeException("Template error"));

        // When
        notificationService.sendEmailForNotification(notification, testUser,
                "Subject", "Body", new HashMap<>());

        // Then
        assertEquals(NotificationStatus.FAILED, notification.getStatus());
    }

    @Test
    void generateTokenIfMeetingExists_ShouldGenerateToken() {
        // Given
        Map<String, String> variables = new HashMap<>();
        variables.put("meetingId", "123");

        when(attendanceTokenService.getTokenForUserAndMeeting(anyLong(), anyLong()))
                .thenReturn(Optional.empty());
        when(meetingRepository.findById(123L)).thenReturn(Optional.of(testMeeting));
        when(attendanceTokenService.createToken(any(User.class), any(Meeting.class)))
                .thenReturn(AttendanceToken.builder().token("NEW-TOKEN").build());

        // When
        String result = notificationService.generateTokenIfMeetingExists(testUser, variables);

        // Then
        assertEquals("NEW-TOKEN", result);
    }

    @Test
    void generateTokenIfMeetingExists_ExistingToken_ShouldReturnExisting() {
        // Given
        Map<String, String> variables = new HashMap<>();
        variables.put("meetingId", "123");

        AttendanceToken existingToken = AttendanceToken.builder()
                .token("EXISTING-TOKEN")
                .build();

        when(attendanceTokenService.getTokenForUserAndMeeting(anyLong(), anyLong()))
                .thenReturn(Optional.of(existingToken));

        // When
        String result = notificationService.generateTokenIfMeetingExists(testUser, variables);

        // Then
        assertEquals("EXISTING-TOKEN", result);
    }

    @Test
    void generateTokenIfMeetingExists_NoMeetingId_ShouldReturnNull() {
        // Given
        Map<String, String> variables = new HashMap<>();

        // When
        String result = notificationService.generateTokenIfMeetingExists(testUser, variables);

        // Then
        assertNull(result);
    }

    @Test
    void extractMeetingIdFromVariables_ShouldExtractId() {
        // Given
        Map<String, String> variables = new HashMap<>();
        variables.put("meetingId", "123");

        // When
        Long result = notificationService.extractMeetingIdFromVariables(variables);

        // Then
        assertEquals(123L, result);
    }

    @Test
    void extractMeetingIdFromVariables_InvalidFormat_ShouldReturnNull() {
        // Given
        Map<String, String> variables = new HashMap<>();
        variables.put("meetingId", "not-a-number");

        // When
        Long result = notificationService.extractMeetingIdFromVariables(variables);

        // Then
        assertNull(result);
    }

    @Test
    void formatTokenForDisplay_ShouldFormatToken() {
        // Given
        String token = "123456789012";

        // When
        String result = notificationService.formatTokenForDisplay(token);

        // Then
        assertEquals("1234-5678-9012", result);
    }

    @Test
    void formatTokenForDisplay_ShortToken_ShouldReturnOriginal() {
        // Given
        String token = "123";

        // When
        String result = notificationService.formatTokenForDisplay(token);

        // Then
        assertEquals("123", result);
    }

    @Test
    void buildConfirmationLink_ShouldBuildLink() {
        // Given
        Long meetingId = 123L;
        String token = "test-token";

        // When
        String result = notificationService.buildConfirmationLink(meetingId, token);

        // Then
        assertTrue(result.contains("http://localhost:8080/meetings/123/attend?token=test-token"));
    }

}
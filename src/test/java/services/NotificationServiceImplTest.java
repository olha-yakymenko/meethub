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
}
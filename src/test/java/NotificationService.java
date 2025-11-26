//// NotificationServiceTest.java
//package com.meethub.domain.service;
//
//import com.meethub.domain.model.entity.*;
//import com.meethub.domain.model.enums.*;
//import com.meethub.domain.model.request.NotificationPreferencesRequest;
//import com.meethub.domain.model.response.NotificationResponse;
//import com.meethub.domain.repository.jpa.*;
//import com.meethub.domain.service.impl.NotificationServiceImpl;
//import com.meethub.exception.ResourceNotFoundException;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//
//import java.time.LocalDateTime;
//import java.util.*;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class NotificationServiceTest {
//
//    @Mock
//    private NotificationRepository notificationRepository;
//
//    @Mock
//    private UserRepository userRepository;
//
//    @Mock
//    private UserPreferenceRepository userPreferenceRepository;
//
//    @Mock
//    private NotificationScheduleRepository scheduleRepository;
//
//    @Mock
//    private EmailTemplateRepository emailTemplateRepository;
//
//    @Mock
//    private MeetingRepository meetingRepository;
//
//    @InjectMocks
//    private NotificationServiceImpl notificationService;
//
//    private User testUser;
//    private Notification testNotification;
//    private EmailTemplate testTemplate;
//
//    @BeforeEach
//    void setUp() {
//        testUser = User.builder()
//                .id(1L)
//                .email("test@example.com")
//                .firstName("John")
//                .lastName("Doe")
//                .emailNotificationsEnabled(true)
//                .pushNotificationsEnabled(true)
//                .digestEnabled(true)
//                .digestFrequency("DAILY")
//                .enabledNotificationChannels(Set.of(NotificationChannel.EMAIL, NotificationChannel.PUSH))
//                .build();
//
//        testNotification = Notification.builder()
//                .id(1L)
//                .user(testUser)
//                .title("Test Notification")
//                .message("This is a test notification")
//                .type(NotificationType.MEETING_INVITATION)
//                .channel(NotificationChannel.EMAIL)
//                .status(NotificationStatus.PENDING)
//                .createdAt(LocalDateTime.now())
//                .build();
//
//        testTemplate = EmailTemplate.builder()
//                .id(1L)
//                .templateKey("meeting-invitation")
//                .name("Meeting Invitation")
//                .subject("Invitation to meeting: {{meetingTitle}}")
//                .bodyTemplate("Hello {{userName}}, you are invited to {{meetingTitle}}")
//                .language("pl")
//                .isActive(true)
//                .build();
//    }
//
//    @Test
//    void createNotification_ShouldSaveAndReturnNotification() {
//        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
//
//        Notification result = notificationService.createNotification(testNotification);
//
//        assertNotNull(result);
//        assertEquals(testNotification.getId(), result.getId());
//        verify(notificationRepository).save(testNotification);
//    }
//
//    @Test
//    void sendNotification_ShouldUpdateStatusToSent() {
//        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
//        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
//
//        notificationService.sendNotification(1L);
//
//        verify(notificationRepository).save(argThat(notification ->
//                notification.getStatus() == NotificationStatus.SENT &&
//                        notification.getSentAt() != null
//        ));
//    }
//
//    @Test
//    void markAsRead_ShouldUpdateReadAtTimestamp() {
//        when(notificationRepository.findByIdAndUserId(1L, 1L))
//                .thenReturn(Optional.of(testNotification));
//        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
//
//        notificationService.markAsRead(1L, 1L);
//
//        verify(notificationRepository).save(argThat(notification ->
//                notification.getReadAt() != null &&
//                        notification.getStatus() == NotificationStatus.READ
//        ));
//    }
//
//    @Test
//    void markAsRead_NotificationNotFound_ShouldThrowException() {
//        when(notificationRepository.findByIdAndUserId(1L, 1L))
//                .thenReturn(Optional.empty());
//
//        assertThrows(ResourceNotFoundException.class, () ->
//                notificationService.markAsRead(1L, 1L));
//    }
//
//    @Test
//    void markAllAsRead_ShouldUpdateAllUserNotifications() {
//        List<Notification> notifications = Arrays.asList(
//                testNotification,
//                Notification.builder()
//                        .id(2L)
//                        .user(testUser)
//                        .title("Another Notification")
//                        .status(NotificationStatus.SENT)
//                        .build()
//        );
//
//        when(notificationRepository.findByUserIdAndStatus(1L, NotificationStatus.SENT))
//                .thenReturn(notifications);
//        when(notificationRepository.saveAll(anyList())).thenReturn(notifications);
//
//        notificationService.markAllAsRead(1L);
//
//        verify(notificationRepository).saveAll(argThat(list ->
//                list.stream().allMatch(n -> n.getReadAt() != null && n.getStatus() == NotificationStatus.READ)
//        ));
//    }
//
//    @Test
//    void getUserNotifications_ShouldReturnPagedResults() {
//        Pageable pageable = PageRequest.of(0, 10);
//        Page<Notification> notificationPage = new PageImpl<>(Collections.singletonList(testNotification));
//
//        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L, pageable))
//                .thenReturn(notificationPage);
//
//        Page<NotificationResponse> result = notificationService.getUserNotifications(1L, pageable);
//
//        assertNotNull(result);
//        assertEquals(1, result.getContent().size());
//        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(1L, pageable);
//    }
//
//    @Test
//    void getUnreadNotifications_ShouldReturnOnlyUnread() {
//        List<Notification> unreadNotifications = Collections.singletonList(testNotification);
//
//        when(notificationRepository.findByUserIdAndStatus(1L, NotificationStatus.SENT))
//                .thenReturn(unreadNotifications);
//
//        List<NotificationResponse> result = notificationService.getUnreadNotifications(1L);
//
//        assertNotNull(result);
//        assertEquals(1, result.size());
//        verify(notificationRepository).findByUserIdAndStatus(1L, NotificationStatus.SENT);
//    }
//
//    @Test
//    void getUnreadCount_ShouldReturnCorrectCount() {
//        when(notificationRepository.countByUserIdAndStatus(1L, NotificationStatus.SENT))
//                .thenReturn(5L);
//
//        Long result = notificationService.getUnreadCount(1L);
//
//        assertEquals(5L, result);
//        verify(notificationRepository).countByUserIdAndStatus(1L, NotificationStatus.SENT);
//    }
//
//    @Test
//    void createNotificationFromTemplate_ShouldCreatePersonalizedNotification() {
//        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
//        when(emailTemplateRepository.findActiveByTemplateKeyAndLanguage("meeting-invitation", "pl"))
//                .thenReturn(Optional.of(testTemplate));
//        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
//
//        Map<String, String> variables = new HashMap<>();
//        variables.put("userName", "John");
//        variables.put("meetingTitle", "Team Meeting");
//
//        Notification result = notificationService.createNotificationFromTemplate(
//                1L, "meeting-invitation", variables,
//                NotificationType.MEETING_INVITATION, NotificationChannel.EMAIL
//        );
//
//        assertNotNull(result);
//        verify(notificationRepository).save(any(Notification.class));
//    }
//
//    @Test
//    void updateNotificationPreferences_ShouldUpdateUserPreferences() {
//        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
//        when(userRepository.save(any(User.class))).thenReturn(testUser);
//
//        NotificationPreferencesRequest request = new NotificationPreferencesRequest();
//        request.setEmailNotificationsEnabled(false);
//        request.setPushNotificationsEnabled(true);
//        request.setDigestEnabled(false);
//        request.setDigestFrequency("WEEKLY");
//        request.setEnabledChannels(Set.of(NotificationChannel.PUSH));
//
//        notificationService.updateNotificationPreferences(1L, request);
//
//        verify(userRepository).save(argThat(user ->
//                !user.getEmailNotificationsEnabled() &&
//                        user.getPushNotificationsEnabled() &&
//                        !user.getDigestEnabled() &&
//                        "WEEKLY".equals(user.getDigestFrequency())
//        ));
//    }
//
//    @Test
//    void getUserProfileWithPreferences_ShouldReturnCompleteProfile() {
//        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
//        when(notificationRepository.countByUserId(1L)).thenReturn(10L);
//        when(notificationRepository.countByUserIdAndStatus(1L, NotificationStatus.SENT)).thenReturn(3L);
//        when(meetingRepository.countUpcomingMeetingsByUserId(1L)).thenReturn(2L);
//
//        var result = notificationService.getUserProfileWithPreferences(1L);
//
//        assertNotNull(result);
//        assertEquals(1L, result.getId());
//        assertEquals("test@example.com", result.getEmail());
//        assertEquals(10L, result.getTotalNotifications());
//        assertEquals(3L, result.getUnreadNotifications());
//        assertEquals(2L, result.getUpcomingMeetings());
//    }
//
//    @Test
//    void scheduleMeetingReminder_ShouldCreateScheduledNotification() {
//        Meeting meeting = Meeting.builder()
//                .title("Test Meeting")
//                .startDate(LocalDateTime.now().plusHours(2))
//                .build();
//
//        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
//        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
//        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
//
//        notificationService.scheduleMeetingReminder(1L, 1L, LocalDateTime.now().plusMinutes(30));
//
//        verify(notificationRepository).save(argThat(notification ->
//                notification.getScheduledFor() != null &&
//                        notification.getType() == NotificationType.MEETING_REMINDER
//        ));
//    }
//}
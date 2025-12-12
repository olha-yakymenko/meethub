////// NotificationServiceTest.java
////package com.meethub.domain.service;
////
////import com.meethub.domain.model.entity.*;
////import com.meethub.domain.model.enums.*;
////import com.meethub.domain.model.request.NotificationPreferencesRequest;
////import com.meethub.domain.model.response.NotificationResponse;
////import com.meethub.domain.repository.jpa.*;
////import com.meethub.domain.service.impl.NotificationServiceImpl;
////import com.meethub.exception.ResourceNotFoundException;
////import org.junit.jupiter.api.BeforeEach;
////import org.junit.jupiter.api.Test;
////import org.junit.jupiter.api.extension.ExtendWith;
////import org.mockito.InjectMocks;
////import org.mockito.Mock;
////import org.mockito.junit.jupiter.MockitoExtension;
////import org.springframework.data.domain.Page;
////import org.springframework.data.domain.PageImpl;
////import org.springframework.data.domain.PageRequest;
////import org.springframework.data.domain.Pageable;
////
////import java.time.LocalDateTime;
////import java.util.*;
////
////import static org.junit.jupiter.api.Assertions.*;
////import static org.mockito.ArgumentMatchers.*;
////import static org.mockito.Mockito.*;
////
////@ExtendWith(MockitoExtension.class)
////class NotificationServiceTest {
////
////    @Mock
////    private NotificationRepository notificationRepository;
////
////    @Mock
////    private UserRepository userRepository;
////
////    @Mock
////    private UserPreferenceRepository userPreferenceRepository;
////
////    @Mock
////    private NotificationScheduleRepository scheduleRepository;
////
////    @Mock
////    private EmailTemplateRepository emailTemplateRepository;
////
////    @Mock
////    private MeetingRepository meetingRepository;
////
////    @InjectMocks
////    private NotificationServiceImpl notificationService;
////
////    private User testUser;
////    private Notification testNotification;
////    private EmailTemplate testTemplate;
////
////    @BeforeEach
////    void setUp() {
////        testUser = User.builder()
////                .id(1L)
////                .email("test@example.com")
////                .firstName("John")
////                .lastName("Doe")
////                .emailNotificationsEnabled(true)
////                .pushNotificationsEnabled(true)
////                .digestEnabled(true)
////                .digestFrequency("DAILY")
////                .enabledNotificationChannels(Set.of(NotificationChannel.EMAIL, NotificationChannel.PUSH))
////                .build();
////
////        testNotification = Notification.builder()
////                .id(1L)
////                .user(testUser)
////                .title("Test Notification")
////                .message("This is a test notification")
////                .type(NotificationType.MEETING_INVITATION)
////                .channel(NotificationChannel.EMAIL)
////                .status(NotificationStatus.PENDING)
////                .createdAt(LocalDateTime.now())
////                .build();
////
////        testTemplate = EmailTemplate.builder()
////                .id(1L)
////                .templateKey("meeting-invitation")
////                .name("Meeting Invitation")
////                .subject("Invitation to meeting: {{meetingTitle}}")
////                .bodyTemplate("Hello {{userName}}, you are invited to {{meetingTitle}}")
////                .language("pl")
////                .isActive(true)
////                .build();
////    }
////
////    @Test
////    void createNotification_ShouldSaveAndReturnNotification() {
////        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
////
////        Notification result = notificationService.createNotification(testNotification);
////
////        assertNotNull(result);
////        assertEquals(testNotification.getId(), result.getId());
////        verify(notificationRepository).save(testNotification);
////    }
////
//////    @Test
//////    void sendNotification_ShouldUpdateStatusToSent() {
//////        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
//////        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
//////
//////        notificationService.sendNotification(1L);
//////
//////        verify(notificationRepository).save(argThat(notification ->
//////                notification.getStatus() == NotificationStatus.SENT &&
//////                        notification.getSentAt() != null
//////        ));
//////    }
////
////    @Test
////    void markAsRead_ShouldUpdateReadAtTimestamp() {
////        when(notificationRepository.findByIdAndUserId(1L, 1L))
////                .thenReturn(Optional.of(testNotification));
////        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
////
////        notificationService.markAsRead(1L, 1L);
////
////        verify(notificationRepository).save(argThat(notification ->
////                notification.getReadAt() != null &&
////                        notification.getStatus() == NotificationStatus.READ
////        ));
////    }
////
////    @Test
////    void markAsRead_NotificationNotFound_ShouldThrowException() {
////        when(notificationRepository.findByIdAndUserId(1L, 1L))
////                .thenReturn(Optional.empty());
////
////        assertThrows(ResourceNotFoundException.class, () ->
////                notificationService.markAsRead(1L, 1L));
////    }
////
////
////
////    @Test
////    void getUserNotifications_ShouldReturnPagedResults() {
////        Pageable pageable = PageRequest.of(0, 10);
////        Page<Notification> notificationPage = new PageImpl<>(Collections.singletonList(testNotification));
////
////        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L, pageable))
////                .thenReturn(notificationPage);
////
////        Page<NotificationResponse> result = notificationService.getUserNotifications(1L, pageable);
////
////        assertNotNull(result);
////        assertEquals(1, result.getContent().size());
////        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(1L, pageable);
////    }
////
////    @Test
////    void getUnreadNotifications_ShouldReturnOnlyUnread() {
////        List<Notification> unreadNotifications = Collections.singletonList(testNotification);
////
////        when(notificationRepository.findByUserIdAndStatus(1L, NotificationStatus.SENT))
////                .thenReturn(unreadNotifications);
////
////        List<NotificationResponse> result = notificationService.getUnreadNotifications(1L);
////
////        assertNotNull(result);
////        assertEquals(1, result.size());
////        verify(notificationRepository).findByUserIdAndStatus(1L, NotificationStatus.SENT);
////    }
////
////    @Test
////    void getUnreadCount_ShouldReturnCorrectCount() {
////        when(notificationRepository.countByUserIdAndStatus(1L, NotificationStatus.SENT))
////                .thenReturn(5L);
////
////        Long result = notificationService.getUnreadCount(1L);
////
////        assertEquals(5L, result);
////        verify(notificationRepository).countByUserIdAndStatus(1L, NotificationStatus.SENT);
////    }
////
////
////
////    @Test
////    void updateNotificationPreferences_ShouldUpdateUserPreferences() {
////        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
////        when(userRepository.save(any(User.class))).thenReturn(testUser);
////
////        NotificationPreferencesRequest request = new NotificationPreferencesRequest();
////        request.setEmailNotificationsEnabled(false);
////        request.setPushNotificationsEnabled(true);
////        request.setDigestEnabled(false);
////        request.setDigestFrequency("WEEKLY");
////        request.setEnabledChannels(Set.of(NotificationChannel.PUSH));
////
////        notificationService.updateNotificationPreferences(1L, request);
////
////        verify(userRepository).save(argThat(user ->
////                !user.getEmailNotificationsEnabled() &&
////                        user.getPushNotificationsEnabled() &&
////                        !user.getDigestEnabled() &&
////                        "WEEKLY".equals(user.getDigestFrequency())
////        ));
////    }
////
////    @Test
////    void getUserProfileWithPreferences_ShouldReturnCompleteProfile() {
////        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
////        when(notificationRepository.countByUserId(1L)).thenReturn(10L);
////        when(notificationRepository.countByUserIdAndStatus(1L, NotificationStatus.SENT)).thenReturn(3L);
////        when(meetingRepository.countUpcomingMeetingsByUserId(1L)).thenReturn(2L);
////
////        var result = notificationService.getUserProfileWithPreferences(1L);
////
////        assertNotNull(result);
////        assertEquals(1L, result.getId());
////        assertEquals("test@example.com", result.getEmail());
////        assertEquals(10L, result.getTotalNotifications());
////        assertEquals(3L, result.getUnreadNotifications());
////        assertEquals(2L, result.getUpcomingMeetings());
////    }
////
//////    @Test
//////    void scheduleMeetingReminder_ShouldCreateScheduledNotification() {
//////        Meeting meeting = Meeting.builder()
//////                .title("Test Meeting")
//////                .startDate(LocalDateTime.now().plusHours(2))
//////                .build();
//////
//////        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
//////        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
//////        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
//////
//////        notificationService.scheduleMeetingReminder(1L, 1L, LocalDateTime.now().plusMinutes(30));
//////
//////        verify(notificationRepository).save(argThat(notification ->
//////                notification.getScheduledFor() != null &&
//////                        notification.getType() == NotificationType.MEETING_REMINDER
//////        ));
//////    }
////}
//
//
//
//
//
//
//
//
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
//import java.util.concurrent.CompletableFuture;
//
//import static org.assertj.core.api.Assertions.*;
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
//    @Mock
//    private MeetingParticipantRepository participantRepository;
//
//    @Mock
//    private EmailService emailService;
//
//    @Mock
//    private AttendanceTokenService attendanceTokenService;
//
//    @Mock
//    private AttendanceTokenRepository attendanceTokenRepository;
//
//    @InjectMocks
//    private NotificationServiceImpl notificationService;
//
//    private User testUser;
//    private User testOrganizer;
//    private User testParticipant;
//    private Notification testNotification;
//    private EmailTemplate testTemplate;
//    private Meeting testMeeting;
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
//                .smsNotificationsEnabled(false)
//                .digestEnabled(true)
//                .digestFrequency("DAILY")
//                .language("pl")
//                .enabledNotificationChannels(Set.of(NotificationChannel.EMAIL, NotificationChannel.PUSH, NotificationChannel.IN_APP))
//                .build();
//
//        testOrganizer = User.builder()
//                .id(2L)
//                .email("organizer@example.com")
//                .firstName("Alice")
//                .lastName("Smith")
//                .emailNotificationsEnabled(true)
//                .build();
//
//        testParticipant = User.builder()
//                .id(3L)
//                .email("participant@example.com")
//                .firstName("Bob")
//                .lastName("Johnson")
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
//
//        testMeeting = Meeting.builder()
//                .title("Test Meeting")
//                .description("Test Description")
//                .startDate(LocalDateTime.now().plusDays(1))
//                .endDate(LocalDateTime.now().plusDays(1).plusHours(2))
//                .organizer(testOrganizer)
//                .build();
//    }
//
//    // ==================== PODSTAWOWE METODY CRUD ====================
//
//    @Test
//    void createNotification_ShouldSaveAndReturnNotification() {
//        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
//
//        Notification result = notificationService.createNotification(testNotification);
//
//        assertThat(result).isNotNull();
//        assertThat(result.getId()).isEqualTo(1L);
//        verify(notificationRepository).save(testNotification);
//    }
//
////    @Test
////    void createNotification_WithNullNotification_ShouldThrowException() {
////        assertThatThrownBy(() -> notificationService.createNotification(null))
////                .isInstanceOf(NullPointerException.class);
////    }
//
//    // ==================== METODY ODCZYTU ====================
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
//        assertThat(result).isNotNull();
//        assertThat(result.getContent()).hasSize(1);
//        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(1L, pageable);
//    }
//
//    @Test
//    void getUserNotifications_WithNoNotifications_ShouldReturnEmptyPage() {
//        Pageable pageable = PageRequest.of(0, 10);
//        Page<Notification> emptyPage = Page.empty();
//
//        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L, pageable))
//                .thenReturn(emptyPage);
//
//        Page<NotificationResponse> result = notificationService.getUserNotifications(1L, pageable);
//
//        assertThat(result).isNotNull();
//        assertThat(result.getContent()).isEmpty();
//    }
//
//    @Test
//    void getUnreadNotifications_ShouldReturnOnlyUnread() {
//        testNotification.setStatus(NotificationStatus.SENT);
//        List<Notification> unreadNotifications = List.of(testNotification);
//
//        when(notificationRepository.findByUserIdAndStatus(1L, NotificationStatus.SENT))
//                .thenReturn(unreadNotifications);
//
//        List<NotificationResponse> result = notificationService.getUnreadNotifications(1L);
//
//        assertThat(result).isNotNull();
//        assertThat(result).hasSize(1);
//        assertThat(result.get(0).getStatus()).isEqualTo(NotificationStatus.SENT);
//        verify(notificationRepository).findByUserIdAndStatus(1L, NotificationStatus.SENT);
//    }
////
////    @Test
////    void getUnreadNotifications_WhenAllRead_ShouldReturnEmptyList() {
////        testNotification.setStatus(NotificationStatus.READ);
////        List<Notification> readNotifications = List.of(testNotification);
////
////        when(notificationRepository.findByUserIdAndStatus(1L, NotificationStatus.SENT))
////                .thenReturn(readNotifications);
////
////        List<NotificationResponse> result = notificationService.getUnreadNotifications(1L);
////
////        assertThat(result).isNotNull();
////        assertThat(result).isEmpty();
////    }
//
//    @Test
//    void getUnreadCount_ShouldReturnCorrectCount() {
//        when(notificationRepository.countByUserIdAndStatus(1L, NotificationStatus.SENT))
//                .thenReturn(5L);
//
//        Long result = notificationService.getUnreadCount(1L);
//
//        assertThat(result).isEqualTo(5L);
//        verify(notificationRepository).countByUserIdAndStatus(1L, NotificationStatus.SENT);
//    }
//
//    @Test
//    void getUnreadCount_WhenNoUnread_ShouldReturnZero() {
//        when(notificationRepository.countByUserIdAndStatus(1L, NotificationStatus.SENT))
//                .thenReturn(0L);
//
//        Long result = notificationService.getUnreadCount(1L);
//
//        assertThat(result).isEqualTo(0L);
//    }
//
//    // ==================== METODY OZNACZANIA JAKO PRZECZYTANE ====================
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
//    void markAsRead_WhenAlreadyRead_ShouldNotUpdate() {
//        testNotification.setReadAt(LocalDateTime.now().minusHours(1));
//        testNotification.setStatus(NotificationStatus.READ);
//
//        when(notificationRepository.findByIdAndUserId(1L, 1L))
//                .thenReturn(Optional.of(testNotification));
//
//        notificationService.markAsRead(1L, 1L);
//
//        verify(notificationRepository, never()).save(any());
//    }
//
//    @Test
//    void markAsRead_NotificationNotFound_ShouldThrowException() {
//        when(notificationRepository.findByIdAndUserId(1L, 1L))
//                .thenReturn(Optional.empty());
//
//        assertThatThrownBy(() -> notificationService.markAsRead(1L, 1L))
//                .isInstanceOf(ResourceNotFoundException.class)
//                .hasMessageContaining("Notification not found");
//    }
//
//    @Test
//    void markAsRead_WhenNotificationBelongsToDifferentUser_ShouldThrowException() {
//        when(notificationRepository.findByIdAndUserId(1L, 2L))
//                .thenReturn(Optional.empty());
//
//        assertThatThrownBy(() -> notificationService.markAsRead(1L, 2L))
//                .isInstanceOf(ResourceNotFoundException.class);
//    }
//
//
////
////    @Test
////    void markAllAsRead_WhenNoUnread_ShouldDoNothing() {
////        when(notificationRepository.findByUserIdAndStatus(1L, NotificationStatus.SENT))
////                .thenReturn(Collections.emptyList());
////
////        notificationService.markAllAsRead(1L);
////
////        verify(notificationRepository, never()).saveAll(any());
////    }
//
//    // ==================== METODY WYSYŁKI POWIADOMIEŃ ====================
//
//    @Test
//    void sendNotification_EmailChannel_ShouldSendEmail() {
//        testNotification.setChannel(NotificationChannel.EMAIL);
//        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
//        doNothing().when(emailService).sendTemplateEmail(anyString(), anyString(), anyString(), anyMap());
//
//        notificationService.sendNotification(1L);
//
//        verify(notificationRepository).save(argThat(notification ->
//                notification.getStatus() == NotificationStatus.SENT &&
//                        notification.getSentAt() != null
//        ));
//        verify(emailService).sendTemplateEmail(
//                eq(testUser.getEmail()),
//                eq(testNotification.getTitle()),
//                eq("notification_email"),
//                anyMap()
//        );
//    }
//
//    @Test
//    void sendNotification_PushChannel_ShouldUpdateStatus() {
//        testNotification.setChannel(NotificationChannel.PUSH);
//        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
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
//    void sendNotification_InAppChannel_ShouldUpdateStatus() {
//        testNotification.setChannel(NotificationChannel.IN_APP);
//        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
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
//    void sendNotification_WhenEmailFails_ShouldMarkAsFailed() {
//        testNotification.setChannel(NotificationChannel.EMAIL);
//        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
//        doThrow(new RuntimeException("Email error")).when(emailService)
//                .sendTemplateEmail(anyString(), anyString(), anyString(), anyMap());
//
//        notificationService.sendNotification(1L);
//
//        verify(notificationRepository).save(argThat(notification ->
//                notification.getStatus() == NotificationStatus.FAILED &&
//                        notification.getErrorMessage() != null &&
//                        notification.getRetryCount() == 1
//        ));
//    }
//
//    @Test
//    void sendNotification_NotificationNotFound_ShouldThrowException() {
//        when(notificationRepository.findById(1L)).thenReturn(Optional.empty());
//
//        assertThatThrownBy(() -> notificationService.sendNotification(1L))
//                .isInstanceOf(ResourceNotFoundException.class)
//                .hasMessageContaining("Notification not found");
//    }
//
//    // ==================== METODY PREFERENCJI UŻYTKOWNIKA ====================
//
//    @Test
//    void updateNotificationPreferences_ShouldUpdateAllFields() {
//        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
//        when(userRepository.save(any(User.class))).thenReturn(testUser);
//
//        NotificationPreferencesRequest request = new NotificationPreferencesRequest();
//        request.setEmailNotificationsEnabled(false);
//        request.setPushNotificationsEnabled(true);
//        request.setSmsNotificationsEnabled(true);
//        request.setDigestEnabled(false);
//        request.setDigestFrequency("WEEKLY");
//        request.setEnabledChannels(Set.of(NotificationChannel.PUSH, NotificationChannel.EMAIL));
//        request.setMeetingInvitations(true);
//        request.setMeetingReminders(false);
//        request.setMeetingUpdates(true);
//        request.setTaskAssignments(false);
//        request.setSecurityAlerts(true);
//
//        notificationService.updateNotificationPreferences(1L, request);
//
//        verify(userRepository).save(argThat(user ->
//                !user.getEmailNotificationsEnabled() &&
//                        user.getPushNotificationsEnabled() &&
//                        user.getSmsNotificationsEnabled() &&
//                        !user.getDigestEnabled() &&
//                        "WEEKLY".equals(user.getDigestFrequency())
//        ));
//
//        verify(userPreferenceRepository, atLeast(5)).findByUserIdAndPreferenceKey(eq(1L), anyString());
//        verify(userPreferenceRepository, atLeastOnce()).save(any(UserPreference.class));
//    }
//
//    @Test
//    void updateNotificationPreferences_WithPartialUpdate_ShouldUpdateOnlyProvidedFields() {
//        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
//        when(userRepository.save(any(User.class))).thenReturn(testUser);
//
//        NotificationPreferencesRequest request = new NotificationPreferencesRequest();
//        request.setEmailNotificationsEnabled(false);
//
//        notificationService.updateNotificationPreferences(1L, request);
//
//        verify(userRepository).save(argThat(user ->
//                !user.getEmailNotificationsEnabled() &&
//                        user.getPushNotificationsEnabled() // unchanged
//        ));
//    }
//
//    @Test
//    void updateNotificationPreferences_UserNotFound_ShouldThrowException() {
//        when(userRepository.findById(1L)).thenReturn(Optional.empty());
//
//        NotificationPreferencesRequest request = new NotificationPreferencesRequest();
//
//        assertThatThrownBy(() -> notificationService.updateNotificationPreferences(1L, request))
//                .isInstanceOf(ResourceNotFoundException.class)
//                .hasMessageContaining("User not found");
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
//        assertThat(result).isNotNull();
//        assertThat(result.getId()).isEqualTo(1L);
//        assertThat(result.getEmail()).isEqualTo("test@example.com");
//        assertThat(result.getTotalNotifications()).isEqualTo(10L);
//        assertThat(result.getUnreadNotifications()).isEqualTo(3L);
//        assertThat(result.getUpcomingMeetings()).isEqualTo(2L);
//        assertThat(result.getEmailNotificationsEnabled()).isTrue();
//        assertThat(result.getDigestFrequency()).isEqualTo("DAILY");
//    }
//
//    @Test
//    void getUserProfileWithPreferences_UserNotFound_ShouldThrowException() {
//        when(userRepository.findById(1L)).thenReturn(Optional.empty());
//
//        assertThatThrownBy(() -> notificationService.getUserProfileWithPreferences(1L))
//                .isInstanceOf(ResourceNotFoundException.class)
//                .hasMessageContaining("User not found");
//    }
//
//    // ==================== METODY PRZYPOMNIEŃ SPOTKAŃ ====================
////
////    @Test
////    void scheduleMeetingReminder_ShouldCreateNotificationsForAllChannels() {
////        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
////        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
////        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
////
////        LocalDateTime reminderTime = LocalDateTime.now().plusHours(1);
////        notificationService.scheduleMeetingReminder(100L, 1L, reminderTime);
////
////        // Powinno utworzyć 3 powiadomienia: IN_APP, EMAIL i PUSH
////        verify(notificationRepository, times(3)).save(any(Notification.class));
////    }
////
////    @Test
////    void scheduleMeetingReminder_WhenEmailDisabled_ShouldSkipEmail() {
////        testUser.setEmailNotificationsEnabled(false);
////        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
////        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
////        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
////
////        notificationService.scheduleMeetingReminder(100L, 1L, LocalDateTime.now().plusHours(1));
////
////        // Powinno utworzyć tylko IN_APP i PUSH
////        verify(notificationRepository, times(2)).save(any(Notification.class));
////    }
//
//    @Test
//    void scheduleMeetingReminder_UserNotFound_ShouldThrowException() {
//        when(userRepository.findById(1L)).thenReturn(Optional.empty());
//
//        assertThatThrownBy(() -> notificationService.scheduleMeetingReminder(100L, 1L, LocalDateTime.now()))
//                .isInstanceOf(ResourceNotFoundException.class)
//                .hasMessageContaining("User not found");
//    }
//
//    @Test
//    void scheduleMeetingReminder_MeetingNotFound_ShouldThrowException() {
//        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
//        when(meetingRepository.findById(100L)).thenReturn(Optional.empty());
//
//        assertThatThrownBy(() -> notificationService.scheduleMeetingReminder(100L, 1L, LocalDateTime.now()))
//                .isInstanceOf(ResourceNotFoundException.class)
//                .hasMessageContaining("Meeting not found");
//    }
//
//    // ==================== METODY TWORZENIA Z SZABLONU ====================
////
////    @Test
////    void createNotificationFromTemplate_ShouldCreateAndSendNotification() {
////        Map<String, String> variables = Map.of(
////                "meetingTitle", "Test Meeting",
////                "userName", "John"
////        );
////
////        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
////        when(emailTemplateRepository.findByTemplateKeyAndLanguage("meeting-invitation", "pl"))
////                .thenReturn(Optional.of(testTemplate));
////        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
////        doNothing().when(emailService).sendHtmlEmail(anyString(), anyString(), anyString());
////
////        Notification result = notificationService.createNotificationFromTemplate(
////                1L, "meeting-invitation", variables,
////                NotificationType.MEETING_INVITATION, NotificationChannel.EMAIL
////        );
////
////        assertThat(result).isNotNull();
////        assertThat(result.getTitle()).contains("Test Meeting");
////        verify(notificationRepository).save(any(Notification.class));
////        verify(emailService).sendHtmlEmail(anyString(), anyString(), anyString());
////    }
////
////    @Test
////    void createNotificationFromTemplate_WhenChannelDisabled_ShouldReturnNull() {
////        testUser.setEmailNotificationsEnabled(false);
////        Map<String, String> variables = Map.of("key", "value");
////
////        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
////
////        Notification result = notificationService.createNotificationFromTemplate(
////                1L, "template-key", variables,
////                NotificationType.MEETING_INVITATION, NotificationChannel.EMAIL
////        );
////
////        assertThat(result).isNull();
////        verify(notificationRepository, never()).save(any());
////    }
//
//    @Test
//    void createNotificationFromTemplate_WhenPreferenceDisabled_ShouldReturnNull() {
//        UserPreference preference = new UserPreference();
//        preference.setPreferenceKey("meeting_invitations");
//        preference.setPreferenceValue("false");
//
//        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
//        when(userPreferenceRepository.findByUserIdAndPreferenceKey(1L, "meeting_invitations"))
//                .thenReturn(Optional.of(preference));
//
//        Map<String, String> variables = Map.of("key", "value");
//        Notification result = notificationService.createNotificationFromTemplate(
//                1L, "template-key", variables,
//                NotificationType.MEETING_INVITATION, NotificationChannel.EMAIL
//        );
//
//        assertThat(result).isNull();
//    }
//
//    @Test
//    void createNotificationFromTemplate_UserNotFound_ShouldThrowException() {
//        when(userRepository.findById(1L)).thenReturn(Optional.empty());
//
//        Map<String, String> variables = Map.of("key", "value");
//
//        assertThatThrownBy(() -> notificationService.createNotificationFromTemplate(
//                1L, "template-key", variables,
//                NotificationType.MEETING_INVITATION, NotificationChannel.EMAIL
//        )).isInstanceOf(ResourceNotFoundException.class)
//                .hasMessageContaining("User not found");
//    }
//
//    @Test
//    void createNotificationFromTemplate_TemplateNotFound_ShouldThrowException() {
//        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
//        when(emailTemplateRepository.findByTemplateKeyAndLanguage("invalid-key", "pl"))
//                .thenReturn(Optional.empty());
//
//        Map<String, String> variables = Map.of("key", "value");
//
//        assertThatThrownBy(() -> notificationService.createNotificationFromTemplate(
//                1L, "invalid-key", variables,
//                NotificationType.MEETING_INVITATION, NotificationChannel.EMAIL
//        )).isInstanceOf(ResourceNotFoundException.class)
//                .hasMessageContaining("Template not found");
//    }
////
////    @Test
////    void createNotificationFromTemplate_EmailSendingFails_ShouldSaveAsFailed() {
////        Map<String, String> variables = Map.of("meetingTitle", "Test");
////        Notification notificationWithId = Notification.builder()
////                .id(1L)
////                .user(testUser)
////                .status(NotificationStatus.PENDING)
////                .build();
////
////        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
////        when(emailTemplateRepository.findByTemplateKeyAndLanguage("meeting-invitation", "pl"))
////                .thenReturn(Optional.of(testTemplate));
////        when(notificationRepository.save(any(Notification.class))).thenReturn(notificationWithId);
////        doThrow(new RuntimeException("SMTP error")).when(emailService)
////                .sendHtmlEmail(anyString(), anyString(), anyString());
////
////        Notification result = notificationService.createNotificationFromTemplate(
////                1L, "meeting-invitation", variables,
////                NotificationType.MEETING_INVITATION, NotificationChannel.EMAIL
////        );
////
////        assertThat(result).isNotNull();
////        verify(notificationRepository, times(2)).save(any(Notification.class));
////    }
////
////    // ==================== METODY SPECJALNE DLA SPOTKAŃ ====================
////
////    @Test
////    void sendParticipantJoinedNotification_ShouldNotifyOrganizer() {
////        when(participantRepository.countByMeetingIdAndStatus(100L, ParticipationStatus.CONFIRMED))
////                .thenReturn(5L);
////        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
////        when(emailTemplateRepository.findByTemplateKeyAndLanguage(eq("participant_joined"), anyString()))
////                .thenReturn(Optional.of(testTemplate));
////
////        notificationService.sendParticipantJoinedNotification(testOrganizer, testParticipant, testMeeting);
////
////        // Powinno utworzyć co najmniej IN_APP
////        verify(notificationRepository, atLeastOnce()).save(any(Notification.class));
////    }
////
////    @Test
////    void sendJoinRequestNotification_ShouldNotifyOrganizer() {
////        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
////        when(emailTemplateRepository.findByTemplateKeyAndLanguage(eq("join_request"), anyString()))
////                .thenReturn(Optional.of(testTemplate));
////
////        notificationService.sendJoinRequestNotification(testOrganizer, testParticipant, testMeeting);
////
////        verify(notificationRepository, atLeastOnce()).save(any(Notification.class));
////    }
//
////    @Test
////    void sendRequestApprovedNotification_ShouldNotifyUser() {
////        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
////        when(emailTemplateRepository.findByTemplateKeyAndLanguage(eq("request_approved"), anyString()))
////                .thenReturn(Optional.of(testTemplate));
////
////        notificationService.sendRequestApprovedNotification(testParticipant, testMeeting);
////
////        verify(notificationRepository, atLeastOnce()).save(any(Notification.class));
////    }
////
////    @Test
////    void sendRequestRejectedNotification_ShouldNotifyUser() {
////        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
////        when(emailTemplateRepository.findByTemplateKeyAndLanguage(eq("request_rejected"), anyString()))
////                .thenReturn(Optional.of(testTemplate));
////
////        notificationService.sendRequestRejectedNotification(testParticipant, testMeeting);
////
////        verify(notificationRepository, atLeastOnce()).save(any(Notification.class));
////    }
////
////    @Test
////    void aggregateMeetingUpdates_ShouldNotifyRelatedUsers() {
////        List<User> relatedUsers = List.of(testUser, testOrganizer);
////        when(userRepository.findUsersByMeetingId(100L)).thenReturn(relatedUsers);
////        when(userPreferenceRepository.findByUserIdAndPreferenceKey(anyLong(), eq("meeting_updates")))
////                .thenReturn(Optional.empty());
////
////        notificationService.aggregateMeetingUpdates(100L);
////
////        verify(notificationRepository, atLeastOnce()).save(any(Notification.class));
////    }
//
//    @Test
//    void aggregateMeetingUpdates_WhenPreferenceDisabled_ShouldSkipUser() {
//        UserPreference preference = new UserPreference();
//        preference.setPreferenceKey("meeting_updates");
//        preference.setPreferenceValue("false");
//
//        List<User> relatedUsers = List.of(testUser);
//        when(userRepository.findUsersByMeetingId(100L)).thenReturn(relatedUsers);
//        when(userPreferenceRepository.findByUserIdAndPreferenceKey(1L, "meeting_updates"))
//                .thenReturn(Optional.of(preference));
//
//        notificationService.aggregateMeetingUpdates(100L);
//
//        verify(notificationRepository, never()).save(any(Notification.class));
//    }
//
//    @Test
//    void sendAggregatedNotification_ShouldCreateAggregatedNotification() {
//        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
//        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
//
//        notificationService.sendAggregatedNotification(1L, NotificationType.MEETING_UPDATE, List.of(100L, 200L));
//
//        verify(notificationRepository).save(argThat(notification ->
//                notification.getTitle().contains("Zbiorcze") &&
//                        notification.getMessage().contains("2 nowych aktualizacji")
//        ));
//    }
//
//    @Test
//    void sendAggregatedNotification_UserNotFound_ShouldThrowException() {
//        when(userRepository.findById(1L)).thenReturn(Optional.empty());
//
//        assertThatThrownBy(() -> notificationService.sendAggregatedNotification(1L, NotificationType.MEETING_UPDATE, List.of(100L)))
//                .isInstanceOf(ResourceNotFoundException.class)
//                .hasMessageContaining("User not found");
//    }
//
//    // ==================== METODY POMOCNICZE ====================
//
//    @Test
//    void isNotificationAllowed_WhenChannelEnabledAndPreferenceTrue_ShouldReturnTrue() {
//        boolean result = notificationService.isNotificationAllowed(
//                testUser, NotificationType.MEETING_INVITATION, NotificationChannel.EMAIL);
//
//        assertThat(result).isTrue();
//    }
////
////    @Test
////    void isNotificationAllowed_WhenChannelDisabled_ShouldReturnFalse() {
////        testUser.setEmailNotificationsEnabled(false);
////        boolean result = notificationService.isNotificationAllowed(
////                testUser, NotificationType.MEETING_INVITATION, NotificationChannel.EMAIL);
////
////        assertThat(result).isFalse();
////    }
//
//    @Test
//    void isNotificationAllowed_WhenPreferenceFalse_ShouldReturnFalse() {
//        UserPreference preference = new UserPreference();
//        preference.setPreferenceKey("meeting_invitations");
//        preference.setPreferenceValue("false");
//
//        when(userPreferenceRepository.findByUserIdAndPreferenceKey(1L, "meeting_invitations"))
//                .thenReturn(Optional.of(preference));
//
//        boolean result = notificationService.isNotificationAllowed(
//                testUser, NotificationType.MEETING_INVITATION, NotificationChannel.EMAIL);
//
//        assertThat(result).isFalse();
//    }
//
//    @Test
//    void isNotificationAllowed_WhenNoPreference_ShouldUseDefaultTrue() {
//        when(userPreferenceRepository.findByUserIdAndPreferenceKey(1L, "meeting_invitations"))
//                .thenReturn(Optional.empty());
//
//        boolean result = notificationService.isNotificationAllowed(
//                testUser, NotificationType.MEETING_INVITATION, NotificationChannel.EMAIL);
//
//        assertThat(result).isTrue();
//    }
//
//    @Test
//    void personalizeTemplate_ShouldReplaceVariables() {
//        String template = "Hello {{name}}, welcome to {{app}}!";
//        Map<String, String> variables = Map.of(
//                "name", "John",
//                "app", "MeetHub"
//        );
//
//        String result = notificationService.getUserPreference(testUser, "test_key", "default");
//
//        // This is a helper method that's private, but we can test through other methods
//        assertThat(result).isEqualTo("default");
//    }
//
//    // ==================== TESTY DLA METOD SCHEDULOWANYCH ====================
//
////    @Test
////    void sendScheduledNotifications_ShouldSendPendingNotifications() {
////        Notification scheduledNotification = Notification.builder()
////                .id(1L)
////                .user(testUser)
////                .channel(NotificationChannel.EMAIL)
////                .status(NotificationStatus.PENDING)
////                .scheduledFor(LocalDateTime.now().minusMinutes(1))
////                .build();
////
////        List<Notification> pendingNotifications = List.of(scheduledNotification);
////        when(notificationRepository.findByStatusAndScheduledForBefore(NotificationStatus.PENDING, any(LocalDateTime.class)))
////                .thenReturn(pendingNotifications);
////        doNothing().when(emailService).sendTemplateEmail(anyString(), anyString(), anyString(), anyMap());
////
////        notificationService.sendScheduledNotifications();
////
////        verify(notificationRepository).save(argThat(notification ->
////                notification.getStatus() == NotificationStatus.SENT &&
////                        notification.getSentAt() != null
////        ));
////    }
//
////    @Test
////    void sendScheduledNotifications_WhenNoPending_ShouldDoNothing() {
////        when(notificationRepository.findByStatusAndScheduledForBefore(NotificationStatus.PENDING, any(LocalDateTime.class)))
////                .thenReturn(Collections.emptyList());
////
////        notificationService.sendScheduledNotifications();
////
////        verify(notificationRepository, never()).save(any(Notification.class));
////    }
////
////    @Test
////    void sendScheduledNotifications_WhenSendingFails_ShouldLogError() {
////        Notification scheduledNotification = Notification.builder()
////                .id(1L)
////                .user(testUser)
////                .channel(NotificationChannel.EMAIL)
////                .status(NotificationStatus.PENDING)
////                .scheduledFor(LocalDateTime.now().minusMinutes(1))
////                .build();
////
////        List<Notification> pendingNotifications = List.of(scheduledNotification);
////        when(notificationRepository.findByStatusAndScheduledForBefore(NotificationStatus.PENDING, any(LocalDateTime.class)))
////                .thenReturn(pendingNotifications);
////        doThrow(new RuntimeException("Email error")).when(emailService)
////                .sendTemplateEmail(anyString(), anyString(), anyString(), anyMap());
////
////        // Should not throw exception
////        assertDoesNotThrow(() -> notificationService.sendScheduledNotifications());
////
////        verify(notificationRepository).save(any(Notification.class));
////    }
//
//    // ==================== TESTY DLA DIGESTS ====================
//
//    @Test
//    void processNotificationDigests_ShouldProcessDailyDigests() {
//        testUser.setDigestFrequency("DAILY");
//        List<User> users = List.of(testUser);
//
//        when(userRepository.findByDigestEnabledTrue()).thenReturn(users);
//
//        notificationService.processNotificationDigests();
//
//        // Should call sendDailyDigest
//        // Verify through log or other side effects
//    }
//
//    @Test
//    void processNotificationDigests_ShouldProcessWeeklyDigestsOnMonday() {
//        testUser.setDigestFrequency("WEEKLY");
//        List<User> users = List.of(testUser);
//
//        when(userRepository.findByDigestEnabledTrue()).thenReturn(users);
//
//        notificationService.processNotificationDigests();
//
//        // Logic depends on current day of week
//        // This test might need to be adjusted based on when it runs
//    }
//
//    @Test
//    void processNotificationDigests_WhenNoUsers_ShouldDoNothing() {
//        when(userRepository.findByDigestEnabledTrue()).thenReturn(Collections.emptyList());
//
//        assertDoesNotThrow(() -> notificationService.processNotificationDigests());
//    }
//
//    // ==================== TESTY GRANICZNE ====================
//
//    @Test
//    void createNotification_WithInvalidData_ShouldHandleGracefully() {
//        Notification invalidNotification = Notification.builder()
//                .user(null) // Invalid
//                .build();
//
//        when(notificationRepository.save(any(Notification.class)))
//                .thenThrow(new RuntimeException("Validation error"));
//
//        assertThatThrownBy(() -> notificationService.createNotification(invalidNotification))
//                .isInstanceOf(RuntimeException.class);
//    }
//
//    @Test
//    void getUserNotifications_WithInvalidUserId_ShouldReturnEmpty() {
//        Pageable pageable = PageRequest.of(0, 10);
//        Page<Notification> emptyPage = Page.empty();
//
//        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(-1L, pageable))
//                .thenReturn(emptyPage);
//
//        Page<NotificationResponse> result = notificationService.getUserNotifications(-1L, pageable);
//
//        assertThat(result).isNotNull();
//        assertThat(result.getContent()).isEmpty();
//    }
//
//    @Test
//    void updateNotificationPreferences_WithNullRequest_ShouldThrowException() {
//        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
//
//        assertThatThrownBy(() -> notificationService.updateNotificationPreferences(1L, null))
//                .isInstanceOf(NullPointerException.class);
//    }
////
////    @Test
////    void scheduleMeetingReminder_WithPastDate_ShouldStillSchedule() {
////        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
////        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
////        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
////
////        LocalDateTime pastDate = LocalDateTime.now().minusDays(1);
////        notificationService.scheduleMeetingReminder(100L, 1L, pastDate);
////
////        verify(notificationRepository, atLeastOnce()).save(any(Notification.class));
////    }
////
////    // ==================== TESTY DLA TOKENÓW ATTENDANCE ====================
////
////    @Test
////    void createNotificationFromTemplate_WithAttendanceToken_ShouldIncludeToken() {
////        Map<String, String> variables = Map.of(
////                "meetingTitle", "Test Meeting",
////                "meetingId", "100"
////        );
////
////        AttendanceToken token = AttendanceToken.builder()
////                .token("TEST-TOKEN-123")
////                .build();
////
////        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
////        when(emailTemplateRepository.findByTemplateKeyAndLanguage(anyString(), anyString()))
////                .thenReturn(Optional.of(testTemplate));
////        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
////        when(attendanceTokenService.getTokenForUserAndMeeting(1L, 100L))
////                .thenReturn(Optional.of(token));
////        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
////        doNothing().when(emailService).sendHtmlEmail(anyString(), anyString(), anyString());
////
////        Notification result = notificationService.createNotificationFromTemplate(
////                1L, "meeting_started", variables,
////                NotificationType.MEETING_REMINDER, NotificationChannel.EMAIL
////        );
////
////        assertThat(result).isNotNull();
////        verify(emailService).sendHtmlEmail(anyString(), anyString(), contains("TEST-TOKEN-123"));
////    }
////
////    @Test
////    void createNotificationFromTemplate_WithoutMeetingId_ShouldNotGenerateToken() {
////        Map<String, String> variables = Map.of(
////                "meetingTitle", "Test Meeting"
////                // No meetingId
////        );
////
////        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
////        when(emailTemplateRepository.findByTemplateKeyAndLanguage(anyString(), anyString()))
////                .thenReturn(Optional.of(testTemplate));
////        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
////        doNothing().when(emailService).sendHtmlEmail(anyString(), anyString(), anyString());
////
////        Notification result = notificationService.createNotificationFromTemplate(
////                1L, "meeting_started", variables,
////                NotificationType.MEETING_REMINDER, NotificationChannel.EMAIL
////        );
////
////        assertThat(result).isNotNull();
////        verify(attendanceTokenService, never()).getTokenForUserAndMeeting(anyLong(), anyLong());
////    }
////
////    @Test
////    void createNotificationFromTemplate_WhenTokenServiceUnavailable_ShouldStillSendEmail() {
////        Map<String, String> variables = Map.of(
////                "meetingTitle", "Test Meeting",
////                "meetingId", "100"
////        );
////
////        // Simulate token service being null
////        NotificationServiceImpl serviceWithNullTokenService = new NotificationServiceImpl(
////                notificationRepository, userRepository, userPreferenceRepository,
////                scheduleRepository, emailTemplateRepository, meetingRepository,
////                participantRepository, emailService,
////                null, // attendanceTokenService is null
////                attendanceTokenRepository
////        );
////
////        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
////        when(emailTemplateRepository.findByTemplateKeyAndLanguage(anyString(), anyString()))
////                .thenReturn(Optional.of(testTemplate));
////        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
////        doNothing().when(emailService).sendHtmlEmail(anyString(), anyString(), anyString());
////
////        // Should not throw exception
////        assertDoesNotThrow(() -> serviceWithNullTokenService.createNotificationFromTemplate(
////                1L, "meeting_started", variables,
////                NotificationType.MEETING_REMINDER, NotificationChannel.EMAIL
////        ));
////
////        verify(emailService).sendHtmlEmail(anyString(), anyString(), anyString());
////    }
//
//    // ==================== TESTY DLA METOD PRYWATNYCH (PRZEZ REFLEKSJĘ) ====================
//
//    @Test
//    void testPrivatePersonalizeTemplate() throws Exception {
//        // Test private method using reflection
//        var method = NotificationServiceImpl.class.getDeclaredMethod(
//                "personalizeTemplate", String.class, Map.class);
//        method.setAccessible(true);
//
//        String template = "Hello {{name}} from {{city}}!";
//        Map<String, String> variables = Map.of("name", "John", "city", "Warsaw");
//
//        String result = (String) method.invoke(notificationService, template, variables);
//
//        assertThat(result).isEqualTo("Hello John from Warsaw!");
//    }
//
//    @Test
//    void testPersonalizeTemplate_WithMissingVariables() throws Exception {
//        var method = NotificationServiceImpl.class.getDeclaredMethod(
//                "personalizeTemplate", String.class, Map.class);
//        method.setAccessible(true);
//
//        String template = "Hello {{name}} from {{city}}!";
//        Map<String, String> variables = Map.of("name", "John");
//        // city is missing
//
//        String result = (String) method.invoke(notificationService, template, variables);
//
//        // Should leave placeholder unchanged
//        assertThat(result).isEqualTo("Hello John from {{city}}!");
//    }
////
////    @Test
////    void testPersonalizeTemplate_WithNullTemplate() throws Exception {
////        var method = NotificationServiceImpl.class.getDeclaredMethod(
////                "personalizeTemplate", String.class, Map.class);
////        method.setAccessible(true);
////
////        Map<String, String> variables = Map.of("name", "John");
////
////        String result = (String) method.invoke(notificationService, null, variables);
////
////        assertThat(result).isNull();
////    }
//
//    // ==================== TESTY INTEGRACYJNE ====================
//
//    @Test
//    void completeNotificationFlow_ShouldWorkEndToEnd() {
//        // 1. Create notification
//        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
//
//        Notification created = notificationService.createNotification(testNotification);
//        assertThat(created).isNotNull();
//
//        // 2. Send notification
//        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
//        doNothing().when(emailService).sendTemplateEmail(anyString(), anyString(), anyString(), anyMap());
//
//        notificationService.sendNotification(1L);
//        verify(notificationRepository, times(2)).save(any(Notification.class));
//
//        // 3. Mark as read
//        testNotification.setStatus(NotificationStatus.SENT);
//        when(notificationRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testNotification));
//
//        notificationService.markAsRead(1L, 1L);
//        verify(notificationRepository, times(3)).save(any(Notification.class));
//
//        // 4. Get user notifications
//        Pageable pageable = PageRequest.of(0, 10);
//        Page<Notification> notificationPage = new PageImpl<>(List.of(testNotification));
//        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L, pageable))
//                .thenReturn(notificationPage);
//
//        Page<NotificationResponse> notifications = notificationService.getUserNotifications(1L, pageable);
//        assertThat(notifications.getContent()).hasSize(1);
//    }
//
//    @Test
//    void notificationPreferencesFlow_ShouldUpdateAndRetrieve() {
//        // 1. Update preferences
//        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
//        when(userRepository.save(any(User.class))).thenReturn(testUser);
//
//        NotificationPreferencesRequest request = new NotificationPreferencesRequest();
//        request.setEmailNotificationsEnabled(false);
//        request.setDigestFrequency("WEEKLY");
//
//        notificationService.updateNotificationPreferences(1L, request);
//
//        // 2. Get profile with preferences
//        when(notificationRepository.countByUserId(1L)).thenReturn(5L);
//        when(notificationRepository.countByUserIdAndStatus(1L, NotificationStatus.SENT)).thenReturn(2L);
//        when(meetingRepository.countUpcomingMeetingsByUserId(1L)).thenReturn(1L);
//
//        var profile = notificationService.getUserProfileWithPreferences(1L);
//
//        assertThat(profile.getEmailNotificationsEnabled()).isFalse();
//        assertThat(profile.getDigestFrequency()).isEqualTo("WEEKLY");
//        assertThat(profile.getUnreadNotifications()).isEqualTo(2L);
//    }
//}
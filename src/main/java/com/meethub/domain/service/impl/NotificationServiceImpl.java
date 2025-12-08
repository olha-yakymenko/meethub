// NotificationServiceImpl.java
package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.*;
import com.meethub.domain.model.enums.*;
import com.meethub.domain.model.request.NotificationPreferencesRequest;
import com.meethub.domain.model.response.NotificationResponse;
import com.meethub.domain.model.response.UserProfileResponse;
import com.meethub.domain.repository.jpa.*;
import com.meethub.domain.service.AttendanceTokenService;
import com.meethub.domain.service.EmailService;
import com.meethub.domain.service.MeetingSchedulerService;
import com.meethub.domain.service.NotificationService;
import com.meethub.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final NotificationScheduleRepository scheduleRepository;
    private final EmailTemplateRepository emailTemplateRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository participantRepository;
    private final EmailService emailService;

    private final AttendanceTokenService attendanceTokenService;
    private final AttendanceTokenRepository attendanceTokenRepository;

    @Override
    public Notification createNotification(Notification notification) {
        return notificationRepository.save(notification);
    }

    @Override
    public void sendNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        try {
            switch (notification.getChannel()) {
                case EMAIL:
                    sendEmailNotification(notification);
                    break;
                case PUSH:
                    sendPushNotification(notification);
                    break;
                case SMS:
                    sendSmsNotification(notification);
                    break;
                case IN_APP:
                    sendInAppNotification(notification);
                    break;
            }

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());

        } catch (Exception e) {
            log.error("Failed to send notification: {}", e.getMessage());
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
            notification.setRetryCount(notification.getRetryCount() + 1);
        }

        notificationRepository.save(notification);
    }

    @Override
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            notification.setStatus(NotificationStatus.READ);
            notificationRepository.save(notification);
        }
    }

    @Override
    public void markAllAsRead(Long userId) {
        List<Notification> unreadNotifications = notificationRepository.findByUserIdAndStatus(userId, NotificationStatus.SENT);
        LocalDateTime now = LocalDateTime.now();

        unreadNotifications.forEach(notification -> {
            notification.setReadAt(now);
            notification.setStatus(NotificationStatus.READ);
        });

        notificationRepository.saveAll(unreadNotifications);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToNotificationResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndStatus(userId, NotificationStatus.SENT)
                .stream()
                .map(this::mapToNotificationResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndStatus(userId, NotificationStatus.SENT);
    }

//    @Override
//    public Notification createNotificationFromTemplate(Long userId, String templateKey,
//                                                       Map<String, String> variables,
//                                                       NotificationType type,
//                                                       NotificationChannel channel) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
//
//        // 1. ZAWSZE twórz powiadomienie IN_APP (do bazy)
//        Notification notification = createInAppNotification(user, templateKey, variables, type);
//
//        // 2. Jeśli channel = EMAIL i użytkownik ma włączone emaile - wyślij email
//        if (channel == NotificationChannel.EMAIL) {
//            if (isNotificationAllowed(user, type, NotificationChannel.EMAIL)) {
//                sendEmailFromTemplate(user, templateKey, variables, type);
//            }
//        }
//        // 3. Jeśli channel = PUSH i użytkownik ma włączone pushy - dodaj powiadomienie push
//        else if (channel == NotificationChannel.PUSH) {
//            if (isNotificationAllowed(user, type, NotificationChannel.PUSH)) {
//                // Możesz dodać logikę wysyłania push notification
//                log.info("Push notification would be sent to user: {}", userId);
//            }
//        }
//
//        return notification;
//    }

    @Override
    public void scheduleMeetingReminder(Long meetingId, Long userId, LocalDateTime reminderTime) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        Map<String, String> variables = new HashMap<>();
        variables.put("meetingTitle", meeting.getTitle());
        variables.put("meetingDate", meeting.getStartDate().toString());
        variables.put("userName", user.getFirstName());
        variables.put("meetingId", meeting.getId().toString());

        // 1. Zawsze tworzymy powiadomienie IN_APP w bazie
        Notification inAppNotification = Notification.builder()
                .user(user)
                .title("Przypomnienie o spotkaniu: " + meeting.getTitle())
                .message("Spotkanie: " + meeting.getTitle() + " rozpocznie się " + meeting.getStartDate())
                .type(NotificationType.MEETING_REMINDER)
                .channel(NotificationChannel.IN_APP)
                .scheduledFor(reminderTime)
                .templateVariables(variables)
                .status(NotificationStatus.PENDING)
                .referenceId(meetingId)
                .referenceType("MEETING")
                .build();

        notificationRepository.save(inAppNotification);

        // 2. Jeśli użytkownik ma włączone emaile - planujemy email
        if (isNotificationAllowed(user, NotificationType.MEETING_REMINDER, NotificationChannel.EMAIL)) {
            Notification emailNotification = Notification.builder()
                    .user(user)
                    .title("Przypomnienie o spotkaniu: " + meeting.getTitle())
                    .message("")
                    .type(NotificationType.MEETING_REMINDER)
                    .channel(NotificationChannel.EMAIL)
                    .scheduledFor(reminderTime)
                    .templateVariables(variables)
                    .status(NotificationStatus.PENDING)
                    .referenceId(meetingId)
                    .referenceType("MEETING")
                    .build();

            notificationRepository.save(emailNotification);
        }

        // 3. Jeśli użytkownik ma włączone powiadomienia push - planujemy push
        if (isNotificationAllowed(user, NotificationType.MEETING_REMINDER, NotificationChannel.PUSH)) {
            Notification pushNotification = Notification.builder()
                    .user(user)
                    .title("Przypomnienie o spotkaniu")
                    .message("Spotkanie " + meeting.getTitle() + " wkrótce się rozpocznie")
                    .type(NotificationType.MEETING_REMINDER)
                    .channel(NotificationChannel.PUSH)
                    .scheduledFor(reminderTime)
                    .templateVariables(variables)
                    .status(NotificationStatus.PENDING)
                    .referenceId(meetingId)
                    .referenceType("MEETING")
                    .build();

            notificationRepository.save(pushNotification);
        }
    }

    @Scheduled(fixedRate = 60000) // Co minutę
    @Override
    public void sendScheduledNotifications() {
        LocalDateTime now = LocalDateTime.now();
        List<Notification> scheduledNotifications = notificationRepository
                .findByStatusAndScheduledForBefore(NotificationStatus.PENDING, now);

        for (Notification notification : scheduledNotifications) {
            try {
                sendNotificationBasedOnChannel(notification);
            } catch (Exception e) {
                log.error("Failed to send scheduled notification {}: {}",
                        notification.getId(), e.getMessage());
            }
        }
    }

    private void sendNotificationBasedOnChannel(Notification notification) {
        switch (notification.getChannel()) {
            case EMAIL:
                sendEmailNotification(notification);
                break;
            case PUSH:
                sendPushNotification(notification);
                break;
            case SMS:
                sendSmsNotification(notification);
                break;
            case IN_APP:
                sendInAppNotification(notification);
                break;
        }

        notification.setStatus(NotificationStatus.SENT);
        notification.setSentAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Scheduled(cron = "0 0 8 * * ?") // Codziennie o 8:00
    @Override
    public void processNotificationDigests() {
        LocalDateTime now = LocalDateTime.now();
        List<User> usersWithDigest = userRepository.findByDigestEnabledTrue();

        for (User user : usersWithDigest) {
            if ("DAILY".equals(user.getDigestFrequency())) {
                sendDailyDigest(user, now);
            } else if ("WEEKLY".equals(user.getDigestFrequency()) && now.getDayOfWeek().getValue() == 1) {
                sendWeeklyDigest(user, now);
            }
        }
    }

    @Override
    public void updateNotificationPreferences(Long userId, NotificationPreferencesRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getEmailNotificationsEnabled() != null) {
            user.setEmailNotificationsEnabled(request.getEmailNotificationsEnabled());
        }
        if (request.getPushNotificationsEnabled() != null) {
            user.setPushNotificationsEnabled(request.getPushNotificationsEnabled());
        }
        if (request.getSmsNotificationsEnabled() != null) {
            user.setSmsNotificationsEnabled(request.getSmsNotificationsEnabled());
        }
        if (request.getDigestEnabled() != null) {
            user.setDigestEnabled(request.getDigestEnabled());
        }
        if (request.getDigestFrequency() != null) {
            user.setDigestFrequency(request.getDigestFrequency());
        }
        if (request.getEnabledChannels() != null) {
            user.setEnabledNotificationChannels(request.getEnabledChannels());
        }

        updateUserPreference(user, "meeting_invitations",
                request.getMeetingInvitations() != null ? request.getMeetingInvitations().toString() : null);
        updateUserPreference(user, "meeting_reminders",
                request.getMeetingReminders() != null ? request.getMeetingReminders().toString() : null);
        updateUserPreference(user, "meeting_updates",
                request.getMeetingUpdates() != null ? request.getMeetingUpdates().toString() : null);
        updateUserPreference(user, "task_assignments",
                request.getTaskAssignments() != null ? request.getTaskAssignments().toString() : null);
        updateUserPreference(user, "security_alerts",
                request.getSecurityAlerts() != null ? request.getSecurityAlerts().toString() : null);

        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfileWithPreferences(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setRole(user.getRole().name());
        response.setTimezone(user.getTimezone());
        response.setLanguage(user.getLanguage());
        response.setCreatedAt(user.getCreatedAt());

        response.setEmailNotificationsEnabled(user.getEmailNotificationsEnabled());
        response.setPushNotificationsEnabled(user.getPushNotificationsEnabled());
        response.setSmsNotificationsEnabled(user.getSmsNotificationsEnabled());
        response.setDigestEnabled(user.getDigestEnabled());
        response.setDigestFrequency(user.getDigestFrequency());
        response.setEnabledChannels(user.getEnabledNotificationChannels());

        response.setTotalNotifications(notificationRepository.countByUserId(userId));
        response.setUnreadNotifications(getUnreadCount(userId));
        response.setUpcomingMeetings(meetingRepository.countUpcomingMeetingsByUserId(userId));

        return response;
    }

    @Override
    public void aggregateMeetingUpdates(Long meetingId) {
        List<User> relatedUsers = userRepository.findUsersByMeetingId(meetingId);

        for (User user : relatedUsers) {
            if (getUserPreference(user, "meeting_updates", "true").equals("true")) {
                sendAggregatedNotification(user.getId(), NotificationType.MEETING_UPDATE, List.of(meetingId));
            }
        }
    }

    @Override
    public void sendAggregatedNotification(Long userId, NotificationType type, List<Long> referenceIds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String title = getAggregatedTitle(type, referenceIds.size());
        String message = getAggregatedMessage(type, referenceIds);

        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .channel(NotificationChannel.IN_APP)
                .status(NotificationStatus.PENDING)
                .build();

        notificationRepository.save(notification);
    }

    @Override
    public void sendParticipantJoinedNotification(User organizer, User participant, Meeting meeting) {
        log.info("Sending participant joined notification for meeting {} to organizer {}",
                meeting.getId(), organizer.getId());

        Map<String, String> variables = new HashMap<>();
        variables.put("organizerName", organizer.getFirstName());
        variables.put("participantName", participant.getFullName());
        variables.put("meetingTitle", meeting.getTitle());
        variables.put("meetingDate", meeting.getStartDate().toString());
        variables.put("currentParticipants", String.valueOf(getCurrentParticipantsCount(meeting.getId())));

        // Zawsze tworzymy powiadomienie IN_APP
        createNotificationFromTemplate(
                organizer.getId(),
                "participant_joined",
                variables,
                NotificationType.MEETING_UPDATE,
                NotificationChannel.IN_APP
        );

        // Jeśli organizator ma włączone emaile - wyślij też email
        if (isNotificationAllowed(organizer, NotificationType.MEETING_UPDATE, NotificationChannel.EMAIL)) {
            createNotificationFromTemplate(
                    organizer.getId(),
                    "participant_joined",
                    variables,
                    NotificationType.MEETING_UPDATE,
                    NotificationChannel.EMAIL
            );
        }
    }

    @Override
    public void sendJoinRequestNotification(User organizer, User requester, Meeting meeting) {
        log.info("Sending join request notification for meeting {} to organizer {}",
                meeting.getId(), organizer.getId());

        Map<String, String> variables = new HashMap<>();
        variables.put("organizerName", organizer.getFirstName());
        variables.put("requesterName", requester.getFullName());
        variables.put("meetingTitle", meeting.getTitle());
        variables.put("meetingDate", meeting.getStartDate().toString());
        variables.put("requesterEmail", requester.getEmail());

        // Zawsze IN_APP
        createNotificationFromTemplate(
                organizer.getId(),
                "join_request",
                variables,
                NotificationType.MEETING_INVITATION,
                NotificationChannel.IN_APP
        );

        // Email jeśli włączone
        if (isNotificationAllowed(organizer, NotificationType.MEETING_INVITATION, NotificationChannel.EMAIL)) {
            createNotificationFromTemplate(
                    organizer.getId(),
                    "join_request",
                    variables,
                    NotificationType.MEETING_INVITATION,
                    NotificationChannel.EMAIL
            );
        }
    }

    @Override
    public void sendRequestApprovedNotification(User user, Meeting meeting) {
        log.info("Sending request approved notification for meeting {} to user {}",
                meeting.getId(), user.getId());

        Map<String, String> variables = new HashMap<>();
        variables.put("userName", user.getFirstName());
        variables.put("meetingTitle", meeting.getTitle());
        variables.put("meetingDate", meeting.getStartDate().toString());
        variables.put("organizerName", meeting.getOrganizer().getFullName());

        String locationString = "Online";
        if (meeting.getLocation() != null) {
            locationString = meeting.getLocation().getName() != null ?
                    meeting.getLocation().getName() :
                    meeting.getLocation().toString();
        }
        variables.put("meetingLocation", locationString);

        // Zawsze IN_APP
        createNotificationFromTemplate(
                user.getId(),
                "request_approved",
                variables,
                NotificationType.MEETING_INVITATION,
                NotificationChannel.IN_APP
        );

        // Email jeśli włączone
        if (isNotificationAllowed(user, NotificationType.MEETING_INVITATION, NotificationChannel.EMAIL)) {
            createNotificationFromTemplate(
                    user.getId(),
                    "request_approved",
                    variables,
                    NotificationType.MEETING_INVITATION,
                    NotificationChannel.EMAIL
            );
        }
    }

    @Override
    public void sendRequestRejectedNotification(User user, Meeting meeting) {
        log.info("Sending request rejected notification for meeting {} to user {}",
                meeting.getId(), user.getId());

        Map<String, String> variables = new HashMap<>();
        variables.put("userName", user.getFirstName());
        variables.put("meetingTitle", meeting.getTitle());
        variables.put("meetingDate", meeting.getStartDate().toString());
        variables.put("organizerName", meeting.getOrganizer().getFullName());

        // Zawsze IN_APP
        createNotificationFromTemplate(
                user.getId(),
                "request_rejected",
                variables,
                NotificationType.MEETING_UPDATE,
                NotificationChannel.IN_APP
        );

        // Email jeśli włączone
        if (isNotificationAllowed(user, NotificationType.MEETING_UPDATE, NotificationChannel.EMAIL)) {
            createNotificationFromTemplate(
                    user.getId(),
                    "request_rejected",
                    variables,
                    NotificationType.MEETING_UPDATE,
                    NotificationChannel.EMAIL
            );
        }
    }





//    @Override
//    public Notification createNotificationFromTemplate(Long userId, String templateKey,
//                                                       Map<String, String> variables,
//                                                       NotificationType type,
//                                                       NotificationChannel channel) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
//
//        // 1. ZAWSZE twórz powiadomienie IN_APP (do bazy)
//        Notification notification = createInAppNotification(user, templateKey, variables, type);
//
//        try {
//            // 2. Jeśli channel = EMAIL i użytkownik ma włączone emaile - wyślij email
//            if (channel == NotificationChannel.EMAIL) {
//                if (isNotificationAllowed(user, type, NotificationChannel.EMAIL)) {
//                    sendEmailFromTemplate(user, templateKey, variables, type);
//                }
//            }
//            // 3. Jeśli channel = PUSH i użytkownik ma włączone pushy - dodaj powiadomienie push
//            else if (channel == NotificationChannel.PUSH) {
//                if (isNotificationAllowed(user, type, NotificationChannel.PUSH)) {
//                    log.info("Push notification would be sent to user: {}", userId);
//                }
//            }
//        } catch (Exception e) {
//            log.error("Failed to send external notification for user {}: {}", userId, e.getMessage());
//            // NIE RZUCAJ WYJĄTKU DALEJ - notyfikacja IN_APP już została zapisana
//            // Powiadomienia zewnętrzne (email, push) mogą się nie udać, ale to nie powinno
//            // blokować głównej logiki biznesowej
//        }
//
//        return notification;
//    }



//    @Override
//    public Notification createNotificationFromTemplate(Long userId, String templateKey,
//                                                       Map<String, String> variables,
//                                                       NotificationType type,
//                                                       NotificationChannel channel) {
//        log.info("🎯 Tworzenie powiadomienia: user={}, template={}, type={}, channel={}",
//                userId, templateKey, type, channel);
//
//        User user = null;
//        Notification inAppNotification = null;
//
//        try {
//            // ========== 1. POBRANIE UŻYTKOWNIKA ==========
//            user = userRepository.findById(userId)
//                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
//
//        } catch (ResourceNotFoundException e) {
//            log.error("❌ User not found for notification: {}", userId);
//            return null;
//        } catch (Exception e) {
//            log.error("❌ Error finding user {}: {}", userId, e.getMessage());
//            return null;
//        }
//
//        // ========== 2. TWORZENIE POWIADOMIENIA IN_APP (ZAPIS DO BAZY) ==========
//        try {
//            inAppNotification = createInAppNotification(user, templateKey, variables, type);
//            log.info("✅ UTWORZONO powiadomienie IN_APP ID={} dla użytkownika {}",
//                    inAppNotification.getId(), userId);
//
//        } catch (Exception e) {
//            log.error("❌ Critical error creating IN_APP notification: {}", e.getMessage(), e);
//            return null; // Nie możemy kontynuować bez IN_APP
//        }
//
//        // ========== 3. DODATKOWE KANAŁY (NIEZALEŻNE) ==========
//        try {
//            if (channel == NotificationChannel.EMAIL) {
//                if (isNotificationAllowed(user, type, NotificationChannel.EMAIL)) {
//                    log.debug("🔄 Próba wysłania EMAIL do {}", user.getEmail());
//                    sendEmailFromTemplate(user, templateKey, variables, type);
//                } else {
//                    log.debug("⏭️ Pomijam EMAIL - użytkownik nie ma włączonych emaili");
//                }
//            }
//            else if (channel == NotificationChannel.PUSH) {
//                if (isNotificationAllowed(user, type, NotificationChannel.PUSH)) {
//                    log.info("📱 Push notification would be sent to user: {}", userId);
//                    // Tutaj dodaj logikę push jeśli potrzebujesz
//                }
//            }
//            else if (channel == NotificationChannel.SMS) {
//                if (isNotificationAllowed(user, type, NotificationChannel.SMS)) {
//                    log.info("📱 SMS notification would be sent to user: {}", userId);
//                    // Tutaj dodaj logikę SMS jeśli potrzebujesz
//                }
//            }
//
//        } catch (Exception e) {
//            // NIE RZUCAJ WYJĄTKU - tylko loguj błąd
//            log.error("❌ Błąd w dodatkowych kanałach dla użytkownika {}: {}",
//                    userId, e.getMessage());
//            // IN_APP już zapisane, więc kontynuujemy
//        }
//
//        return inAppNotification; // Zawsze zwracaj IN_APP, nawet jeśli inne kanały się nie udały
//    }

//dziala ale nie dla mail
//    @Override
//    public Notification createNotificationFromTemplate(Long userId, String templateKey,
//                                                       Map<String, String> variables,
//                                                       NotificationType type,
//                                                       NotificationChannel channel) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
//
//        // Sprawdź preferencje użytkownika
//        if (!isNotificationAllowed(user, type, channel)) {
//            log.debug("Notification not allowed for user {}: type={}, channel={}", userId, type, channel);
//            return null;
//        }
//
//        EmailTemplate template = emailTemplateRepository.findByTemplateKeyAndLanguage(templateKey, user.getLanguage())
//                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
//
//        // Personalizuj wiadomość
//        String personalizedMessage = personalizeTemplate(template.getBodyTemplate(), variables);
//        String personalizedTitle = personalizeTemplate(template.getSubject(), variables);
//
//        Notification notification = Notification.builder()
//                .user(user)
//                .title(personalizedTitle)
//                .message(personalizedMessage)
//                .type(type)
//                .channel(channel)
//                .templateKey(templateKey)
//                .templateVariables(variables)
//                .status(NotificationStatus.PENDING)
//                .build();
//
//        return notificationRepository.save(notification);
//    }





//dziala ale bez tokenu
//    @Override
//    public Notification createNotificationFromTemplate(Long userId, String templateKey,
//                                                       Map<String, String> variables,
//                                                       NotificationType type,
//                                                       NotificationChannel channel) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
//
//        // Sprawdź preferencje użytkownika
//        if (!isNotificationAllowed(user, type, channel)) {
//            log.debug("Notification not allowed for user {}: type={}, channel={}", userId, type, channel);
//            return null;
//        }
//
//        EmailTemplate template = emailTemplateRepository.findByTemplateKeyAndLanguage(templateKey, user.getLanguage())
//                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
//
//        // Personalizuj wiadomość
//        String personalizedMessage = personalizeTemplate(template.getBodyTemplate(), variables);
//        String personalizedTitle = personalizeTemplate(template.getSubject(), variables);
//
//        Notification notification = Notification.builder()
//                .user(user)
//                .title(personalizedTitle)
//                .message(personalizedMessage)
//                .type(type)
//                .channel(channel)
//                .templateKey(templateKey)
//                .templateVariables(variables)
//                .status(NotificationStatus.PENDING)
//                .build();
//
//        Notification savedNotification = notificationRepository.save(notification);
//
//        // WYSYŁANIE EMAILA JEŚLI TO KANAŁ EMAIL
//        if (channel == NotificationChannel.EMAIL) {
//            sendEmailForNotification(savedNotification, user, personalizedTitle, personalizedMessage);
//        }
//
//        return savedNotification;
//    }
//
//    /**
//     * Wysyła email dla notyfikacji
//     */
//    private void sendEmailForNotification(Notification notification,
//                                          User user,
//                                          String subject,
//                                          String htmlContent) {
//        try {
//            // WYWOŁAJ NOWĄ METODĘ Z EmailService
//            emailService.sendHtmlEmail(
//                    user.getEmail(),
//                    subject,
//                    htmlContent  // To jest już spersonalizowany HTML z bazy
//            );
//
//            // Aktualizuj status notyfikacji
//            notification.setStatus(NotificationStatus.SENT);
//            notification.setSentAt(LocalDateTime.now());
//            notificationRepository.save(notification);
//
//            log.info("📧 Email wysłany do {} (user ID: {})",
//                    user.getEmail(), user.getId());
//
//        } catch (Exception e) {
//            log.error("❌ Błąd wysyłki email do {}: {}",
//                    user.getEmail(), e.getMessage());
//
//            // Aktualizuj status notyfikacji na FAILED
//            notification.setStatus(NotificationStatus.FAILED);
//            notificationRepository.save(notification);
//
//            // Rzuć wyjątek tylko jeśli chcesz żeby cała operacja się wycofała
//            // throw new RuntimeException("Failed to send email", e);
//        }
//    }
//
//


    @Override
    public Notification createNotificationFromTemplate(Long userId, String templateKey,
                                                       Map<String, String> variables,
                                                       NotificationType type,
                                                       NotificationChannel channel) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Sprawdź preferencje użytkownika
        if (!isNotificationAllowed(user, type, channel)) {
            log.debug("Notification not allowed for user {}: type={}, channel={}", userId, type, channel);
            return null;
        }

        EmailTemplate template = emailTemplateRepository.findByTemplateKeyAndLanguage(templateKey, user.getLanguage())
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));

        // Personalizuj wiadomość
        String personalizedMessage = personalizeTemplate(template.getBodyTemplate(), variables);
        String personalizedTitle = personalizeTemplate(template.getSubject(), variables);

        Notification notification = Notification.builder()
                .user(user)
                .title(personalizedTitle)
                .message(personalizedMessage)
                .type(type)
                .channel(channel)
                .templateKey(templateKey)
                .templateVariables(variables)
                .status(NotificationStatus.PENDING)
                .build();

        Notification savedNotification = notificationRepository.save(notification);

        // WYSYŁANIE EMAILA JEŚLI TO KANAŁ EMAIL
        if (channel == NotificationChannel.EMAIL) {
            // PRZEKAŻ ZMIENNE DO METODY
            sendEmailForNotification(savedNotification, user, personalizedTitle, personalizedMessage, variables);
        }

        return savedNotification;
    }

    private void sendEmailForNotification(Notification notification,
                                          User user,
                                          String subject,
                                          String htmlContent,
                                          Map<String, String> templateVariables) {
        try {
            // ========== 1. PRZYGOTUJ ZMIENNE ==========
            Map<String, String> emailVariables = new HashMap<>();

            // Skopiuj oryginalne zmienne
            if (templateVariables != null) {
                emailVariables.putAll(templateVariables);
            }

            // Dodaj standardowe zmienne
            emailVariables.put("userName", user.getFirstName());
            emailVariables.put("userEmail", user.getEmail());
            emailVariables.put("currentYear", String.valueOf(LocalDateTime.now().getYear()));
            emailVariables.put("companyName", "MeetHub");

            // ========== 2. DODAJ TOKEN TYLKO DLA WYMAGANYCH SZABLONÓW ==========
            String token = null;
//            if (isTokenRequiredTemplate(notification.getTemplateKey())) {
                token = generateTokenIfMeetingExists(user, templateVariables);
                log.info("Aktualny token 1,", token);

                if (token != null) {
                    // Dodaj zmienne tokenu (szablon z bazy użyje {{#if attendanceToken}})
                    emailVariables.put("attendanceToken", token);
                    emailVariables.put("attendanceTokenFormatted", formatTokenForDisplay(token));
                    emailVariables.put("confirmationLink", buildConfirmationLink(
                            extractMeetingIdFromVariables(templateVariables), token));
                    emailVariables.put("token", token); // alias

                    log.info("🔐 Dodano token do emaila dla {}: {}",
                            user.getEmail(), formatTokenForDisplay(token));
                }
//            }

            // ========== 3. ZNAJDŹ I PERSONALIZUJ SZABLON Z BAZY ==========
            EmailTemplate emailTemplate = emailTemplateRepository
                    .findByTemplateKeyAndLanguage(notification.getTemplateKey(), user.getLanguage())
                    .orElseGet(() -> emailTemplateRepository
                            .findByTemplateKeyAndLanguage(notification.getTemplateKey(), "pl")
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Template not found: " + notification.getTemplateKey())));

            // Personalizuj używając zmiennych (w tym token jeśli istnieje)
            String personalizedSubject = personalizeTemplate(emailTemplate.getSubject(), emailVariables);
            String personalizedBody = personalizeTemplate(emailTemplate.getBodyTemplate(), emailVariables);

            // ========== 4. WYŚLIJ EMAIL ==========
            emailService.sendHtmlEmail(
                    user.getEmail(),
                    personalizedSubject,
                    personalizedBody // Szablon z bazy już obsługuje token przez {{#if attendanceToken}}
            );

            // ========== 5. AKTUALIZUJ STATUS ==========
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);

            log.info("📧 Email wysłany do {} (szablon: {})",
                    user.getEmail(), notification.getTemplateKey());

        } catch (Exception e) {
            log.error("❌ Błąd wysyłki email do {}: {}",
                    user.getEmail(), e.getMessage(), e);

            notification.setStatus(NotificationStatus.FAILED);
            notificationRepository.save(notification);
        }
    }


    /**
     * Sprawdza czy szablon wymaga tokenu
     */
    private boolean isTokenRequiredTemplate(String templateKey) {
        return "meeting_started".equals(templateKey) ||
                "meeting_started_participant".equals(templateKey);
    }

    /**
     * Generuje token tylko jeśli to spotkanie i mamy meetingId
     */
    private String generateTokenIfMeetingExists(User user, Map<String, String> variables) {
        if (variables == null) return null;

        Long meetingId = extractMeetingIdFromVariables(variables);
        if (meetingId == null) {
            log.warn("⚠️ Brak meetingId dla generacji tokenu");
            return null;
        }

        try {
            // Sprawdź czy serwis tokenów jest dostępny
            if (attendanceTokenService == null) {
                log.warn("⚠️ AttendanceTokenService nie jest dostępny");
                return null;
            }

            // Znajdź istniejący token lub stwórz nowy
            Optional<AttendanceToken> existingToken = attendanceTokenService
                    .getTokenForUserAndMeeting(user.getId(), meetingId);

            if (existingToken.isPresent()) {
                return existingToken.get().getToken();
            }

            // Stwórz nowy token
            Meeting meeting = meetingRepository.findById(meetingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

            AttendanceToken newToken = attendanceTokenService.createToken(user, meeting);
            return newToken.getToken();

        } catch (Exception e) {
            log.error("❌ Błąd generowania tokenu: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Wyciąga meetingId z zmiennych
     */
    private Long extractMeetingIdFromVariables(Map<String, String> variables) {
        if (variables == null) return null;

        try {
            // Spróbuj różne klucze
            if (variables.containsKey("meetingId")) {
                return Long.parseLong(variables.get("meetingId"));
            }
            if (variables.containsKey("referenceId")) {
                return Long.parseLong(variables.get("referenceId"));
            }
        } catch (NumberFormatException e) {
            log.warn("❌ Nieprawidłowy format meetingId: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Formatuje token do czytelnej postaci
     */
    private String formatTokenForDisplay(String token) {
        if (token == null || token.length() < 12) return token;
        // Format: XXXX-XXXX-XXXX
        return token.substring(0, 4) + "-" +
                token.substring(4, 8) + "-" +
                token.substring(8, 12);
    }

    /**
     * Buduje link potwierdzający
     */
    private String buildConfirmationLink(Long meetingId, String token) {
        // Możesz dodać konfigurację z application.properties
        String baseUrl = "http://localhost:8080";
        return baseUrl + "/api/v1/meetings/" + meetingId +
                "/confirm-attendance?token=" + token;
    }


    private void sendEmailFromTemplate(User user, String templateKey,
                                       Map<String, String> variables,
                                       NotificationType type) {
        String userEmail = user.getEmail();
        log.info("📧 Próba wysłania email do {} (szablon: {})", userEmail, templateKey);

        try {
            // 1. Znajdź szablon w bazie danych
            Optional<EmailTemplate> templateOpt = emailTemplateRepository
                    .findByTemplateKeyAndLanguage(templateKey, user.getLanguage());

            if (!templateOpt.isPresent()) {
                log.warn("⚠️ Szablon '{}' nie znaleziony w DB. Pomijam email.", templateKey);
                return; // Wyjdź BEZ wyjątku
            }

            EmailTemplate template = templateOpt.get();

            // 2. Personalizuj temat i treść z szablonu DB
            String subject = personalizeTemplate(template.getSubject(), variables);
            String body = personalizeTemplate(template.getBodyTemplate(), variables);

            // 3. Przygotuj zmienne dla Thymeleaf - DODAJ WSZYSTKIE POTRZEBNE
            Map<String, Object> emailVariables = new HashMap<>();

            // Podstawowe zmienne
            emailVariables.put("userName", user.getFirstName());
            emailVariables.put("organizerName", user.getFirstName()); // Dla szablonu meeting_started.html
            emailVariables.put("userEmail", userEmail);
            emailVariables.put("subject", subject);
            emailVariables.put("currentYear", LocalDateTime.now().getYear());

            // Skopiuj wszystkie zmienne z oryginalnego mapy
            emailVariables.putAll(variables);

            // Dodaj specjalne zmienne
            if (variables.containsKey("meetingTitle")) {
                emailVariables.put("meetingTitle", variables.get("meetingTitle"));
            }
            if (variables.containsKey("meetingTime")) {
                emailVariables.put("meetingTime", variables.get("meetingTime"));
            }
            if (variables.containsKey("meetingDate")) {
                emailVariables.put("meetingDate", variables.get("meetingDate"));
                emailVariables.put("meetingTime", variables.get("meetingDate")); // Dla kompatybilności
            }
            if (variables.containsKey("location")) {
                emailVariables.put("location", variables.get("location"));
            }
            if (variables.containsKey("meetingId")) {
                emailVariables.put("meetingId", variables.get("meetingId"));
            }

            // 4. Użyj POPRAWNEGO szablonu Thymeleaf
            String thymeleafTemplate = "meeting_started"; // To musi być plik meeting_started.html w templates/email/
            log.info("   📄 Używam szablonu Thymeleaf: {}", thymeleafTemplate);

            // 5. Wyślij email - NIE RZUCAJ WYJĄTKU nawet jeśli się nie uda
            try {
                emailService.sendTemplateEmail(
                        userEmail,
                        subject,
                        thymeleafTemplate, // "meeting_started" -> szuka templates/email/meeting_started.html
                        emailVariables
                );
                log.info("✅ Wywołano sendTemplateEmail dla {}", userEmail);

            } catch (Exception emailException) {
                log.error("❌ Błąd sendTemplateEmail (tylko log): {}", emailException.getMessage());
                // NIE RZUCAJ DALEJ - email to dodatkowa funkcja, nie główna logika
            }

        } catch (Exception e) {
            log.error("❌ Błąd w sendEmailFromTemplate (tylko log): {}", e.getMessage());
            // NIE RZUCAJ WYJĄTKU DALEJ - to nie może zepsuc głównej transakcji
        }
    }

    public boolean isNotificationAllowed(User user, NotificationType type, NotificationChannel channel) {
        // Sprawdź czy kanał jest włączony globalnie
        if (!user.isNotificationChannelEnabled(channel)) {
            return false;
        }

        // Sprawdź preferencje dla konkretnego typu
        String preferenceKey = getPreferenceKeyForType(type);
        String preferenceValue = getUserPreference(user, preferenceKey, "true");

        return "true".equalsIgnoreCase(preferenceValue);
    }

    private String getPreferenceKeyForType(NotificationType type) {
        switch (type) {
            case MEETING_INVITATION: return "meeting_invitations";
            case MEETING_REMINDER: return "meeting_reminders";
            case MEETING_UPDATE: return "meeting_updates";
            case TASK_ASSIGNMENT: return "task_assignments";
            case SECURITY_ALERT: return "security_alerts";
            default: return type.name().toLowerCase();
        }
    }

    public String getUserPreference(User user, String key, String defaultValue) {
        return userPreferenceRepository.findByUserIdAndPreferenceKey(user.getId(), key)
                .map(UserPreference::getPreferenceValue)
                .orElse(defaultValue);
    }

    private void updateUserPreference(User user, String key, String value) {
        if (value == null) return;

        userPreferenceRepository.findByUserIdAndPreferenceKey(user.getId(), key)
                .ifPresentOrElse(
                        preference -> {
                            preference.setPreferenceValue(value);
                            userPreferenceRepository.save(preference);
                        },
                        () -> {
                            UserPreference newPreference = new UserPreference();
                            newPreference.setUser(user);
                            newPreference.setPreferenceKey(key);
                            newPreference.setPreferenceValue(value);
                            userPreferenceRepository.save(newPreference);
                        }
                );
    }

    private String personalizeTemplate(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }

    private void sendEmailNotification(Notification notification) {
        try {
            Map<String, Object> variables = new HashMap<>();
            if (notification.getTemplateVariables() != null) {
                variables.putAll(notification.getTemplateVariables());
            }

            variables.put("notificationTitle", notification.getTitle());
            variables.put("notificationMessage", notification.getMessage());

            emailService.sendTemplateEmail(
                    notification.getUser().getEmail(),
                    notification.getTitle(),
                    "notification_email",
                    variables
            );

            notification.setDeliveredAt(LocalDateTime.now());
            notification.setStatus(NotificationStatus.DELIVERED);
            log.info("✅ Email notification sent to {}", notification.getUser().getEmail());

        } catch (Exception e) {
            log.error("❌ Failed to send email to {}: {}",
                    notification.getUser().getEmail(), e.getMessage());
            throw e;
        }
    }

    private void sendPushNotification(Notification notification) {
        // Implementacja wysyłki powiadomień push (np. Firebase Cloud Messaging)
        // Tutaj zapisujemy do bazy jako dostarczone, w rzeczywistości
        // wysyłamy do serwisu push notifications
        notification.setDeliveredAt(LocalDateTime.now());
        notification.setStatus(NotificationStatus.DELIVERED);
        log.info("📱 Push notification sent to user: {}", notification.getUser().getId());
    }

    private void sendSmsNotification(Notification notification) {
        if (notification.getUser().getPhoneNumber() != null) {
            // Implementacja wysyłki SMS
            log.info("📱 SMS sent to: {}", notification.getUser().getPhoneNumber());
            notification.setDeliveredAt(LocalDateTime.now());
            notification.setStatus(NotificationStatus.DELIVERED);
        }
    }

    private void sendInAppNotification(Notification notification) {
        // Powiadomienia in-app są automatycznie "dostarczone" po zapisaniu do bazy
        notification.setDeliveredAt(LocalDateTime.now());
        notification.setStatus(NotificationStatus.DELIVERED);
    }

    private NotificationResponse mapToNotificationResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setTitle(notification.getTitle());
        response.setMessage(notification.getMessage());
        response.setType(notification.getType());
        response.setStatus(notification.getStatus());
        response.setChannel(notification.getChannel());
        response.setCreatedAt(notification.getCreatedAt());
        response.setReadAt(notification.getReadAt());
        response.setTemplateVariables(notification.getTemplateVariables());
        response.setReferenceId(notification.getReferenceId());
        response.setReferenceType(notification.getReferenceType());
        return response;
    }

    private void sendDailyDigest(User user, LocalDateTime date) {
        // Implementacja daily digest
        log.info("Sending daily digest to user: {}", user.getEmail());
    }

    private void sendWeeklyDigest(User user, LocalDateTime date) {
        // Implementacja weekly digest
        log.info("Sending weekly digest to user: {}", user.getEmail());
    }

    private String getAggregatedTitle(NotificationType type, int count) {
        switch (type) {
            case MEETING_UPDATE:
                return "Zbiorcze aktualizacje spotkań (" + count + ")";
            default:
                return "Zbiorcze powiadomienia (" + count + ")";
        }
    }

    private String getAggregatedMessage(NotificationType type, List<Long> referenceIds) {
        return "Masz " + referenceIds.size() + " nowych aktualizacji. Sprawdź szczegóły w aplikacji.";
    }

    private long getCurrentParticipantsCount(Long meetingId) {
        try {
            return participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.CONFIRMED);
        } catch (Exception e) {
            log.warn("Could not get participants count for meeting {}", meetingId, e);
            return 0L;
        }
    }
}















//// NotificationServiceImpl.java
//package com.meethub.domain.service.impl;
//
//import com.meethub.domain.model.entity.*;
//import com.meethub.domain.model.enums.*;
//import com.meethub.domain.model.request.NotificationPreferencesRequest;
//import com.meethub.domain.model.response.NotificationResponse;
//import com.meethub.domain.model.response.UserProfileResponse;
//import com.meethub.domain.repository.jpa.*;
//import com.meethub.domain.service.MeetingSchedulerService;
//import com.meethub.domain.service.NotificationService;
//import com.meethub.exception.ResourceNotFoundException;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import com.meethub.domain.service.EmailService; // DODAJ
//import org.springframework.mail.javamail.JavaMailSender; // DODAJ
//import org.springframework.mail.javamail.MimeMessageHelper; // DODAJ
//import jakarta.mail.internet.MimeMessage; // DODAJ
//
//
//import java.time.LocalDateTime;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//@Transactional
//public class NotificationServiceImpl implements NotificationService {
//
//    private final NotificationRepository notificationRepository;
//    private final UserRepository userRepository;
//    private final UserPreferenceRepository userPreferenceRepository;
//    private final NotificationScheduleRepository scheduleRepository;
//    private final EmailTemplateRepository emailTemplateRepository;
//    private final MeetingRepository meetingRepository;
//
//    private final MeetingSchedulerService meetingSchedulerService;
//    private final MeetingParticipantRepository participantRepository;
//
//    private final EmailService emailService;
//    private final JavaMailSender javaMailSender;
//
//    @Override
//    public Notification createNotification(Notification notification) {
//        return notificationRepository.save(notification);
//    }
//
//    @Override
//    public void sendNotification(Long notificationId) {
//        Notification notification = notificationRepository.findById(notificationId)
//                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
//
//        try {
//            switch (notification.getChannel()) {
//                case EMAIL:
//                    sendEmailNotification(notification);
//                    break;
//                case PUSH:
//                    sendPushNotification(notification);
//                    break;
//                case SMS:
//                    sendSmsNotification(notification);
//                    break;
//                case IN_APP:
//                    sendInAppNotification(notification);
//                    break;
//            }
//
//            notification.setStatus(NotificationStatus.SENT);
//            notification.setSentAt(LocalDateTime.now());
//
//        } catch (Exception e) {
//            log.error("Failed to send notification: {}", e.getMessage());
//            notification.setStatus(NotificationStatus.FAILED);
//            notification.setErrorMessage(e.getMessage());
//            notification.setRetryCount(notification.getRetryCount() + 1);
//        }
//
//        notificationRepository.save(notification);
//    }
//
//    @Override
//    public void markAsRead(Long notificationId, Long userId) {
//        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
//                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
//
//        if (notification.getReadAt() == null) {
//            notification.setReadAt(LocalDateTime.now());
//            notification.setStatus(NotificationStatus.READ);
//            notificationRepository.save(notification);
//        }
//    }
//
//    @Override
//    public void markAllAsRead(Long userId) {
//        List<Notification> unreadNotifications = notificationRepository.findByUserIdAndStatus(userId, NotificationStatus.SENT);
//        LocalDateTime now = LocalDateTime.now();
//
//        unreadNotifications.forEach(notification -> {
//            notification.setReadAt(now);
//            notification.setStatus(NotificationStatus.READ);
//        });
//
//        notificationRepository.saveAll(unreadNotifications);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public Page<NotificationResponse> getUserNotifications(Long userId, Pageable pageable) {
//        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
//                .map(this::mapToNotificationResponse);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<NotificationResponse> getUnreadNotifications(Long userId) {
//        return notificationRepository.findByUserIdAndStatus(userId, NotificationStatus.SENT)
//                .stream()
//                .map(this::mapToNotificationResponse)
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public Long getUnreadCount(Long userId) {
//        return notificationRepository.countByUserIdAndStatus(userId, NotificationStatus.SENT);
//    }
//
//    @Override
//    public Notification createNotificationFromTemplate(Long userId, String templateKey,
//                                                       Map<String, String> variables,
//                                                       NotificationType type,
//                                                       NotificationChannel channel) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
//
//        // Sprawdź preferencje użytkownika
//        if (!isNotificationAllowed(user, type, channel)) {
//            log.debug("Notification not allowed for user {}: type={}, channel={}", userId, type, channel);
//            return null;
//        }
//
//        EmailTemplate template = emailTemplateRepository.findByTemplateKeyAndLanguage(templateKey, user.getLanguage())
//                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
//
//        // Personalizuj wiadomość
//        String personalizedMessage = personalizeTemplate(template.getBodyTemplate(), variables);
//        String personalizedTitle = personalizeTemplate(template.getSubject(), variables);
//
//        Notification notification = Notification.builder()
//                .user(user)
//                .title(personalizedTitle)
//                .message(personalizedMessage)
//                .type(type)
//                .channel(channel)
//                .templateKey(templateKey)
//                .templateVariables(variables)
//                .status(NotificationStatus.PENDING)
//                .build();
//
//        return notificationRepository.save(notification);
//    }
//
//    @Override
//    public void scheduleMeetingReminder(Long meetingId, Long userId, LocalDateTime reminderTime) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
//
//        Meeting meeting = meetingRepository.findById(meetingId)
//                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
//
//        if (!isNotificationAllowed(user, NotificationType.MEETING_REMINDER, NotificationChannel.EMAIL)) {
//            return;
//        }
//
//        Map<String, String> variables = new HashMap<>();
//        variables.put("meetingTitle", meeting.getTitle());
//        variables.put("meetingDate", meeting.getStartDate().toString());
//        variables.put("userName", user.getFirstName());
//
//        Notification notification = Notification.builder()
//                .user(user)
//                .title("Przypomnienie o spotkaniu: " + meeting.getTitle())
//                .type(NotificationType.MEETING_REMINDER)
//                .channel(NotificationChannel.EMAIL)
//                .scheduledFor(reminderTime)
//                .templateVariables(variables)
//                .status(NotificationStatus.PENDING)
//                .build();
//
//        notificationRepository.save(notification);
//    }
//
//    @Scheduled(fixedRate = 60000) // Co minutę
//    @Override
//    public void sendScheduledNotifications() {
//        LocalDateTime now = LocalDateTime.now();
//        List<Notification> scheduledNotifications = notificationRepository
//                .findByStatusAndScheduledForBefore(NotificationStatus.PENDING, now);
//
//        scheduledNotifications.forEach(notification -> {
//            sendNotification(notification.getId());
//        });
//    }
//
//    @Scheduled(cron = "0 0 8 * * ?") // Codziennie o 8:00
//    @Override
//    public void processNotificationDigests() {
//        LocalDateTime now = LocalDateTime.now();
//
//        // Znajdź użytkowników z włączonymi digestami
//        List<User> usersWithDigest = userRepository.findByDigestEnabledTrue();
//
//        for (User user : usersWithDigest) {
//            if ("DAILY".equals(user.getDigestFrequency())) {
//                sendDailyDigest(user, now);
//            } else if ("WEEKLY".equals(user.getDigestFrequency())) {
//                // Sprawdź czy to początek tygodnia
//                if (now.getDayOfWeek().getValue() == 1) { // Poniedziałek
//                    sendWeeklyDigest(user, now);
//                }
//            }
//        }
//    }
//
//    @Override
//    public void updateNotificationPreferences(Long userId, NotificationPreferencesRequest request) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
//
//        if (request.getEmailNotificationsEnabled() != null) {
//            user.setEmailNotificationsEnabled(request.getEmailNotificationsEnabled());
//        }
//        if (request.getPushNotificationsEnabled() != null) {
//            user.setPushNotificationsEnabled(request.getPushNotificationsEnabled());
//        }
//        if (request.getSmsNotificationsEnabled() != null) {
//            user.setSmsNotificationsEnabled(request.getSmsNotificationsEnabled());
//        }
//        if (request.getDigestEnabled() != null) {
//            user.setDigestEnabled(request.getDigestEnabled());
//        }
//        if (request.getDigestFrequency() != null) {
//            user.setDigestFrequency(request.getDigestFrequency());
//        }
//        if (request.getEnabledChannels() != null) {
//            user.setEnabledNotificationChannels(request.getEnabledChannels());
//        }
//
//        // Zapisz preferencje dla konkretnych typów
//        updateUserPreference(user, "meeting_invitations",
//                request.getMeetingInvitations() != null ? request.getMeetingInvitations().toString() : null);
//        updateUserPreference(user, "meeting_reminders",
//                request.getMeetingReminders() != null ? request.getMeetingReminders().toString() : null);
//        updateUserPreference(user, "meeting_updates",
//                request.getMeetingUpdates() != null ? request.getMeetingUpdates().toString() : null);
//        updateUserPreference(user, "task_assignments",
//                request.getTaskAssignments() != null ? request.getTaskAssignments().toString() : null);
//        updateUserPreference(user, "security_alerts",
//                request.getSecurityAlerts() != null ? request.getSecurityAlerts().toString() : null);
//
//        userRepository.save(user);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public UserProfileResponse getUserProfileWithPreferences(Long userId) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
//
//        UserProfileResponse response = new UserProfileResponse();
//        response.setId(user.getId());
//        response.setEmail(user.getEmail());
//        response.setFirstName(user.getFirstName());
//        response.setLastName(user.getLastName());
//        response.setPhoneNumber(user.getPhoneNumber());
//        response.setRole(user.getRole().name());
//        response.setTimezone(user.getTimezone());
//        response.setLanguage(user.getLanguage());
//        response.setCreatedAt(user.getCreatedAt());
//
//        // Preferencje powiadomień
//        response.setEmailNotificationsEnabled(user.getEmailNotificationsEnabled());
//        response.setPushNotificationsEnabled(user.getPushNotificationsEnabled());
//        response.setSmsNotificationsEnabled(user.getSmsNotificationsEnabled());
//        response.setDigestEnabled(user.getDigestEnabled());
//        response.setDigestFrequency(user.getDigestFrequency());
//        response.setEnabledChannels(user.getEnabledNotificationChannels());
//
//        // Statystyki
//        response.setTotalNotifications(notificationRepository.countByUserId(userId));
//        response.setUnreadNotifications(getUnreadCount(userId));
//        response.setUpcomingMeetings(meetingRepository.countUpcomingMeetingsByUserId(userId));
//
//        return response;
//    }
//
//    @Override
//    public void aggregateMeetingUpdates(Long meetingId) {
//        // Znajdź wszystkich użytkowników związanych ze spotkaniem
//        List<User> relatedUsers = userRepository.findUsersByMeetingId(meetingId);
//
//        for (User user : relatedUsers) {
//            if (getUserPreference(user, "meeting_updates", "true").equals("true")) {
//                // Tutaj logika agregacji wielu zmian w jedno powiadomienie
//                sendAggregatedNotification(user.getId(), NotificationType.MEETING_UPDATE, List.of(meetingId));
//            }
//        }
//    }
//
//    @Override
//    public void sendAggregatedNotification(Long userId, NotificationType type, List<Long> referenceIds) {
//        // Implementacja agregowanych powiadomień
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
//
//        String title = getAggregatedTitle(type, referenceIds.size());
//        String message = getAggregatedMessage(type, referenceIds);
//
//        Notification notification = Notification.builder()
//                .user(user)
//                .title(title)
//                .message(message)
//                .type(type)
//                .channel(NotificationChannel.IN_APP) // Domyślnie in-app dla agregowanych
//                .status(NotificationStatus.PENDING)
//                .build();
//
//        notificationRepository.save(notification);
//    }
//
//    // Metody pomocnicze
//    public boolean isNotificationAllowed(User user, NotificationType type, NotificationChannel channel) {
//        if (!user.isNotificationChannelEnabled(channel)) {
//            return false;
//        }
//
//        // Sprawdź preferencje dla konkretnego typu
//        String preferenceKey = getPreferenceKeyForType(type);
//        String preferenceValue = getUserPreference(user, preferenceKey, "true");
//
//        return "true".equals(preferenceValue);
//    }
//
//    private String getPreferenceKeyForType(NotificationType type) {
//        switch (type) {
//            case MEETING_INVITATION: return "meeting_invitations";
//            case MEETING_REMINDER: return "meeting_reminders";
//            case MEETING_UPDATE: return "meeting_updates";
//            case TASK_ASSIGNMENT: return "task_assignments";
//            case SECURITY_ALERT: return "security_alerts";
//            default: return type.name().toLowerCase();
//        }
//    }
//
//    public String getUserPreference(User user, String key, String defaultValue) {
//        return userPreferenceRepository.findByUserIdAndPreferenceKey(user.getId(), key)
//                .map(UserPreference::getPreferenceValue)
//                .orElse(defaultValue);
//    }
//
//    private void updateUserPreference(User user, String key, String value) {
//        if (value == null) return;
//
//        userPreferenceRepository.findByUserIdAndPreferenceKey(user.getId(), key)
//                .ifPresentOrElse(
//                        preference -> {
//                            preference.setPreferenceValue(value);
//                            userPreferenceRepository.save(preference);
//                        },
//                        () -> {
//                            UserPreference newPreference = new UserPreference();
//                            newPreference.setUser(user);
//                            newPreference.setPreferenceKey(key);
//                            newPreference.setPreferenceValue(value);
//                            userPreferenceRepository.save(newPreference);
//                        }
//                );
//    }
//
//    private String personalizeTemplate(String template, Map<String, String> variables) {
//        String result = template;
//        for (Map.Entry<String, String> entry : variables.entrySet()) {
//            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
//        }
//        return result;
//    }
//
//    private NotificationResponse mapToNotificationResponse(Notification notification) {
//        NotificationResponse response = new NotificationResponse();
//        response.setId(notification.getId());
//        response.setTitle(notification.getTitle());
//        response.setMessage(notification.getMessage());
//        response.setType(notification.getType());
//        response.setStatus(notification.getStatus());
//        response.setChannel(notification.getChannel());
//        response.setCreatedAt(notification.getCreatedAt());
//        response.setReadAt(notification.getReadAt());
//        response.setTemplateVariables(notification.getTemplateVariables());
//        response.setReferenceId(notification.getReferenceId());
//        response.setReferenceType(notification.getReferenceType());
//        return response;
//    }
//
////    private void sendEmailNotification(Notification notification) {
////        // Implementacja wysyłki email
////        log.info("Sending email notification to: {}", notification.getUser().getEmail());
////        // Tutaj integracja z serwisem email (np. JavaMailSender, SendGrid, etc.)
////    }
//
//    private void sendEmailNotification(Notification notification) {
//        log.info("Sending email notification to: {}", notification.getUser().getEmail());
//
//        try {
//            // Tutaj rzeczywista implementacja wysyłki email
//            // Przykład z JavaMailSender:
//        /*
//        SimpleMailMessage message = new SimpleMailMessage();
//        message.setTo(notification.getUser().getEmail());
//        message.setSubject(notification.getTitle());
//        message.setText(notification.getMessage());
//        message.setFrom("noreply@meethub.com");
//
//        javaMailSender.send(message);
//        */
//
//            notification.setDeliveredAt(LocalDateTime.now());
//            notification.setStatus(NotificationStatus.DELIVERED);
//            log.info("✅ Email sent successfully to {}", notification.getUser().getEmail());
//
//        } catch (Exception e) {
//            log.error("❌ Failed to send email to {}: {}",
//                    notification.getUser().getEmail(), e.getMessage());
//            throw e; // Rzuć wyjątek, żeby został obsłużony w sendNotification
//        }
//    }
//
////    @Override
////    public void sendParticipantJoinedNotification(User organizer, User participant, Meeting meeting) {
////        log.info("Sending participant joined notification for meeting {} to organizer {}",
////                meeting.getId(), organizer.getId());
////
////        if (!isNotificationAllowed(organizer, NotificationType.MEETING_UPDATE, NotificationChannel.EMAIL)) {
////            return;
////        }
////
////        // Użyj istniejącej metody getCurrentParticipantsCount z rzeczywistym repozytorium
////        long currentParticipants = participantRepository
////                .findByMeetingIdAndStatus(meeting.getId(), ParticipationStatus.CONFIRMED)
////                .size();
////
////        Map<String, String> variables = new HashMap<>();
////        variables.put("organizerName", organizer.getFirstName());
////        variables.put("participantName", participant.getFullName());
////        variables.put("meetingTitle", meeting.getTitle());
////        variables.put("meetingDate", meeting.getStartDate().toString());
////        variables.put("currentParticipants", String.valueOf(currentParticipants));
////        variables.put("maxParticipants", meeting.getMaxParticipants() != null ?
////                String.valueOf(meeting.getMaxParticipants()) : "Brak limitu");
////
////        createNotificationFromTemplate(
////                organizer.getId(),
////                "participant_joined",
////                variables,
////                NotificationType.MEETING_UPDATE,
////                NotificationChannel.EMAIL
////        );
////    }
////
//
//    private void sendPushNotification(Notification notification) {
//        // Implementacja powiadomień push
//        log.info("Sending push notification to user: {}", notification.getUser().getId());
//    }
//
//    private void sendSmsNotification(Notification notification) {
//        // Implementacja SMS
//        if (notification.getUser().getPhoneNumber() != null) {
//            log.info("Sending SMS to: {}", notification.getUser().getPhoneNumber());
//        }
//    }
//
//    private void sendInAppNotification(Notification notification) {
//        // Powiadomienia in-app są automatycznie "dostarczone"
//        notification.setDeliveredAt(LocalDateTime.now());
//        notification.setStatus(NotificationStatus.DELIVERED);
//    }
//
//    private void sendDailyDigest(User user, LocalDateTime date) {
//        // Implementacja daily digest
//        log.info("Sending daily digest to user: {}", user.getEmail());
//    }
//
//    private void sendWeeklyDigest(User user, LocalDateTime date) {
//        // Implementacja weekly digest
//        log.info("Sending weekly digest to user: {}", user.getEmail());
//    }
//
//    private String getAggregatedTitle(NotificationType type, int count) {
//        switch (type) {
//            case MEETING_UPDATE:
//                return "Aggregowane aktualizacje spotkań (" + count + ")";
//            default:
//                return "Aggregowane powiadomienia (" + count + ")";
//        }
//    }
//
//    private String getAggregatedMessage(NotificationType type, List<Long> referenceIds) {
//        // Stwórz zwięzłą wiadomość agregującą wiele zdarzeń
//        return "Masz " + referenceIds.size() + " nowych aktualizacji. Sprawdź szczegóły w aplikacji.";
//    }
//
//
//    @Override
//    public void sendParticipantJoinedNotification(User organizer, User participant, Meeting meeting) {
//        log.info("Sending participant joined notification for meeting {} to organizer {}", meeting.getId(), organizer.getId());
//
//        if (!isNotificationAllowed(organizer, NotificationType.MEETING_UPDATE, NotificationChannel.EMAIL)) {
//            return;
//        }
//
//        Map<String, String> variables = new HashMap<>();
//        variables.put("organizerName", organizer.getFirstName());
//        variables.put("participantName", participant.getFullName());
//        variables.put("meetingTitle", meeting.getTitle());
//        variables.put("meetingDate", meeting.getStartDate().toString());
//        variables.put("currentParticipants", String.valueOf(getCurrentParticipantsCount(meeting.getId())));
//
//        createNotificationFromTemplate(
//                organizer.getId(),
//                "participant_joined",
//                variables,
//                NotificationType.MEETING_UPDATE,
//                NotificationChannel.EMAIL
//        );
//    }
//
//    @Override
//    public void sendJoinRequestNotification(User organizer, User requester, Meeting meeting) {
//        log.info("Sending join request notification for meeting {} to organizer {}", meeting.getId(), organizer.getId());
//
//        if (!isNotificationAllowed(organizer, NotificationType.MEETING_INVITATION, NotificationChannel.EMAIL)) {
//            return;
//        }
//
//        Map<String, String> variables = new HashMap<>();
//        variables.put("organizerName", organizer.getFirstName());
//        variables.put("requesterName", requester.getFullName());
//        variables.put("meetingTitle", meeting.getTitle());
//        variables.put("meetingDate", meeting.getStartDate().toString());
//        variables.put("requesterEmail", requester.getEmail());
//
//        createNotificationFromTemplate(
//                organizer.getId(),
//                "join_request",
//                variables,
//                NotificationType.MEETING_INVITATION,
//                NotificationChannel.EMAIL
//        );
//    }
//
//    @Override
//    public void sendRequestApprovedNotification(User user, Meeting meeting) {
//        log.info("Sending request approved notification for meeting {} to user {}", meeting.getId(), user.getId());
//
//        if (!isNotificationAllowed(user, NotificationType.MEETING_INVITATION, NotificationChannel.EMAIL)) {
//            return;
//        }
//
//        Map<String, String> variables = new HashMap<>();
//        variables.put("userName", user.getFirstName());
//        variables.put("meetingTitle", meeting.getTitle());
//        variables.put("meetingDate", meeting.getStartDate().toString());
//        variables.put("organizerName", meeting.getOrganizer().getFullName());
//
//        // Poprawka dla lokalizacji
//        String locationString = "Online";
//        if (meeting.getLocation() != null) {
//            locationString = meeting.getLocation().getName() != null ?
//                    meeting.getLocation().getName() :
//                    meeting.getLocation().toString();
//        }
//        variables.put("meetingLocation", locationString);
//
//        createNotificationFromTemplate(
//                user.getId(),
//                "request_approved",
//                variables,
//                NotificationType.MEETING_INVITATION,
//                NotificationChannel.EMAIL
//        );
//    }
//    @Override
//    public void sendRequestRejectedNotification(User user, Meeting meeting) {
//        log.info("Sending request rejected notification for meeting {} to user {}", meeting.getId(), user.getId());
//
//        if (!isNotificationAllowed(user, NotificationType.MEETING_UPDATE, NotificationChannel.EMAIL)) {
//            return;
//        }
//
//        Map<String, String> variables = new HashMap<>();
//        variables.put("userName", user.getFirstName());
//        variables.put("meetingTitle", meeting.getTitle());
//        variables.put("meetingDate", meeting.getStartDate().toString());
//        variables.put("organizerName", meeting.getOrganizer().getFullName());
//
//        createNotificationFromTemplate(
//                user.getId(),
//                "request_rejected",
//                variables,
//                NotificationType.MEETING_UPDATE,
//                NotificationChannel.EMAIL
//        );
//    }
//
//    // Metody pomocnicze
//    private long getCurrentParticipantsCount(Long meetingId) {
//        // Zakładając, że masz odpowiednie repozytorium
//        // W rzeczywistości potrzebujesz metody do zliczania potwierdzonych uczestników
//        try {
//            // Tymczasowe rozwiązanie - zwróć przykładową wartość
//            return 1L;
//        } catch (Exception e) {
//            log.warn("Could not get participants count for meeting {}", meetingId, e);
//            return 0L;
//        }
//    }
//
//
//
//
//    // NotificationServiceImpl.java - zmodyfikuj te metody:
//
//    @Override
//    public Notification createNotificationFromTemplate(Long userId, String templateKey,
//                                                       Map<String, String> variables,
//                                                       NotificationType type,
//                                                       NotificationChannel channel) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
//
//        // 1. ZAWSZE twórz powiadomienie in-app (zapis do bazy)
//        Notification notification = createInAppNotification(user, templateKey, variables, type);
//
//        // 2. TYLKO JEŚLI użytkownik ma włączone maile i channel to EMAIL - wyślij maila
//        if (channel == NotificationChannel.EMAIL && user.isEmailNotificationsEnabled()) {
//            if (user.isNotificationTypeEnabled(type)) {
//                sendEmailNotification(user, templateKey, variables, type);
//            }
//        }
//
//        // 3. Dla innych kanałów (PUSH, SMS) - tu możesz dodać logikę
//
//        return notification;
//    }
//
//    /**
//     * Tworzy powiadomienie in-app (zapis do bazy)
//     */
//    private Notification createInAppNotification(User user, String templateKey,
//                                                 Map<String, String> variables,
//                                                 NotificationType type) {
//        // Znajdź template
//        EmailTemplate template = emailTemplateRepository
//                .findByTemplateKeyAndLanguage(templateKey, user.getLanguage())
//                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
//
//        // Personalizuj
//        String personalizedMessage = personalizeTemplate(template.getBodyTemplate(), variables);
//        String personalizedTitle = personalizeTemplate(template.getSubject(), variables);
//
//        // Stwórz i zapisz powiadomienie IN_APP
//        Notification notification = Notification.builder()
//                .user(user)
//                .title(personalizedTitle)
//                .message(personalizedMessage)
//                .type(type)
//                .channel(NotificationChannel.IN_APP) // ZAWSZE in-app do bazy
//                .templateKey(templateKey)
//                .templateVariables(variables)
//                .status(NotificationStatus.PENDING)
//                .build();
//
//        return notificationRepository.save(notification);
//    }
//
//    /**
//     * Wysyła email (jeśli użytkownik ma włączone)
//     */
//    private void sendEmailNotification(User user, String templateKey,
//                                       Map<String, String> variables,
//                                       NotificationType type) {
//        try {
//            // Przygotuj zmienne dla szablonu email
//            Map<String, Object> emailVariables = new HashMap<>(variables);
//            emailVariables.put("userName", user.getFirstName());
//            emailVariables.put("userEmail", user.getEmail());
//
//            // Znajdź template email
//            EmailTemplate emailTemplate = emailTemplateRepository
//                    .findByTemplateKeyAndLanguage(templateKey + "_email", user.getLanguage())
//                    .orElseGet(() -> emailTemplateRepository
//                            .findByTemplateKeyAndLanguage(templateKey, user.getLanguage())
//                            .orElseThrow(() -> new ResourceNotFoundException("Template not found")));
//
//            // Personalizuj tytuł
//            String subject = personalizeTemplate(emailTemplate.getSubject(), variables);
//
//            // Wyślij email
//            emailService.sendTemplateEmail(
//                    user.getEmail(),
//                    subject,
//                    templateKey + "_email", // np. "meeting_reminder_email.html"
//                    emailVariables
//            );
//
//            log.info("📧 Email wysłany do {}: {}", user.getEmail(), subject);
//
//        } catch (Exception e) {
//            log.error("❌ Błąd podczas wysyłki email do {}: {}",
//                    user.getEmail(), e.getMessage(), e);
//            // Nie rzucaj wyjątku - powiadomienie in-app i tak zostało zapisane
//        }
//    }
//
//    @Override
//    public void scheduleMeetingReminder(Long meetingId, Long userId, LocalDateTime reminderTime) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
//
//        Meeting meeting = meetingRepository.findById(meetingId)
//                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
//
//        Map<String, String> variables = new HashMap<>();
//        variables.put("meetingTitle", meeting.getTitle());
//        variables.put("meetingDate", meeting.getStartDate().toString());
//        variables.put("userName", user.getFirstName());
//        variables.put("reminderTime", reminderTime.toString());
//
//        // 1. Zawsze tworzymy powiadomienie in-app
//        Notification notification = Notification.builder()
//                .user(user)
//                .title("Przypomnienie o spotkaniu: " + meeting.getTitle())
//                .type(NotificationType.MEETING_REMINDER)
//                .channel(NotificationChannel.IN_APP) // ZAWSZE in-app
//                .scheduledFor(reminderTime)
//                .templateVariables(variables)
//                .status(NotificationStatus.PENDING)
//                .build();
//
//        notificationRepository.save(notification);
//
//        // 2. TYLKO jeśli użytkownik ma włączone maile - planujemy email
//        if (user.isEmailNotificationsEnabled() &&
//                user.isNotificationTypeEnabled(NotificationType.MEETING_REMINDER)) {
//
//            // Możesz dodać logikę planowania emaila
//            scheduleEmailReminder(user, meeting, reminderTime);
//        }
//    }
//
//    private void scheduleEmailReminder(User user, Meeting meeting, LocalDateTime reminderTime) {
//        // Tutaj logika planowania emaila (możesz użyć ScheduledExecutorService)
//        log.info("📅 Zaplanowano email reminder dla {} o {}",
//                user.getEmail(), reminderTime);
//
//        // Możesz dodać do kolejki lub użyć @Scheduled
//    }
//
//
//}
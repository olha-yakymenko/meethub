// NotificationServiceImpl.java
package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.*;
import com.meethub.domain.model.enums.*;
import com.meethub.domain.model.request.NotificationPreferencesRequest;
import com.meethub.domain.model.response.NotificationResponse;
import com.meethub.domain.model.response.UserProfileResponse;
import com.meethub.domain.repository.jpa.*;
import com.meethub.domain.service.NotificationService;
import com.meethub.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
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

        return notificationRepository.save(notification);
    }

    @Override
    public void scheduleMeetingReminder(Long meetingId, Long userId, LocalDateTime reminderTime) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        if (!isNotificationAllowed(user, NotificationType.MEETING_REMINDER, NotificationChannel.EMAIL)) {
            return;
        }

        Map<String, String> variables = new HashMap<>();
        variables.put("meetingTitle", meeting.getTitle());
        variables.put("meetingDate", meeting.getStartDate().toString());
        variables.put("userName", user.getFirstName());

        Notification notification = Notification.builder()
                .user(user)
                .title("Przypomnienie o spotkaniu: " + meeting.getTitle())
                .type(NotificationType.MEETING_REMINDER)
                .channel(NotificationChannel.EMAIL)
                .scheduledFor(reminderTime)
                .templateVariables(variables)
                .status(NotificationStatus.PENDING)
                .build();

        notificationRepository.save(notification);
    }

    @Scheduled(fixedRate = 60000) // Co minutę
    @Override
    public void sendScheduledNotifications() {
        LocalDateTime now = LocalDateTime.now();
        List<Notification> scheduledNotifications = notificationRepository
                .findByStatusAndScheduledForBefore(NotificationStatus.PENDING, now);

        scheduledNotifications.forEach(notification -> {
            sendNotification(notification.getId());
        });
    }

    @Scheduled(cron = "0 0 8 * * ?") // Codziennie o 8:00
    @Override
    public void processNotificationDigests() {
        LocalDateTime now = LocalDateTime.now();

        // Znajdź użytkowników z włączonymi digestami
        List<User> usersWithDigest = userRepository.findByDigestEnabledTrue();

        for (User user : usersWithDigest) {
            if ("DAILY".equals(user.getDigestFrequency())) {
                sendDailyDigest(user, now);
            } else if ("WEEKLY".equals(user.getDigestFrequency())) {
                // Sprawdź czy to początek tygodnia
                if (now.getDayOfWeek().getValue() == 1) { // Poniedziałek
                    sendWeeklyDigest(user, now);
                }
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

        // Zapisz preferencje dla konkretnych typów
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

        // Preferencje powiadomień
        response.setEmailNotificationsEnabled(user.getEmailNotificationsEnabled());
        response.setPushNotificationsEnabled(user.getPushNotificationsEnabled());
        response.setSmsNotificationsEnabled(user.getSmsNotificationsEnabled());
        response.setDigestEnabled(user.getDigestEnabled());
        response.setDigestFrequency(user.getDigestFrequency());
        response.setEnabledChannels(user.getEnabledNotificationChannels());

        // Statystyki
        response.setTotalNotifications(notificationRepository.countByUserId(userId));
        response.setUnreadNotifications(getUnreadCount(userId));
        response.setUpcomingMeetings(meetingRepository.countUpcomingMeetingsByUserId(userId));

        return response;
    }

    @Override
    public void aggregateMeetingUpdates(Long meetingId) {
        // Znajdź wszystkich użytkowników związanych ze spotkaniem
        List<User> relatedUsers = userRepository.findUsersByMeetingId(meetingId);

        for (User user : relatedUsers) {
            if (getUserPreference(user, "meeting_updates", "true").equals("true")) {
                // Tutaj logika agregacji wielu zmian w jedno powiadomienie
                sendAggregatedNotification(user.getId(), NotificationType.MEETING_UPDATE, List.of(meetingId));
            }
        }
    }

    @Override
    public void sendAggregatedNotification(Long userId, NotificationType type, List<Long> referenceIds) {
        // Implementacja agregowanych powiadomień
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String title = getAggregatedTitle(type, referenceIds.size());
        String message = getAggregatedMessage(type, referenceIds);

        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .channel(NotificationChannel.IN_APP) // Domyślnie in-app dla agregowanych
                .status(NotificationStatus.PENDING)
                .build();

        notificationRepository.save(notification);
    }

    // Metody pomocnicze
    public boolean isNotificationAllowed(User user, NotificationType type, NotificationChannel channel) {
        if (!user.isNotificationChannelEnabled(channel)) {
            return false;
        }

        // Sprawdź preferencje dla konkretnego typu
        String preferenceKey = getPreferenceKeyForType(type);
        String preferenceValue = getUserPreference(user, preferenceKey, "true");

        return "true".equals(preferenceValue);
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

    private void sendEmailNotification(Notification notification) {
        // Implementacja wysyłki email
        log.info("Sending email notification to: {}", notification.getUser().getEmail());
        // Tutaj integracja z serwisem email (np. JavaMailSender, SendGrid, etc.)
    }

    private void sendPushNotification(Notification notification) {
        // Implementacja powiadomień push
        log.info("Sending push notification to user: {}", notification.getUser().getId());
    }

    private void sendSmsNotification(Notification notification) {
        // Implementacja SMS
        if (notification.getUser().getPhoneNumber() != null) {
            log.info("Sending SMS to: {}", notification.getUser().getPhoneNumber());
        }
    }

    private void sendInAppNotification(Notification notification) {
        // Powiadomienia in-app są automatycznie "dostarczone"
        notification.setDeliveredAt(LocalDateTime.now());
        notification.setStatus(NotificationStatus.DELIVERED);
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
                return "Aggregowane aktualizacje spotkań (" + count + ")";
            default:
                return "Aggregowane powiadomienia (" + count + ")";
        }
    }

    private String getAggregatedMessage(NotificationType type, List<Long> referenceIds) {
        // Stwórz zwięzłą wiadomość agregującą wiele zdarzeń
        return "Masz " + referenceIds.size() + " nowych aktualizacji. Sprawdź szczegóły w aplikacji.";
    }


    @Override
    public void sendParticipantJoinedNotification(User organizer, User participant, Meeting meeting) {
        log.info("Sending participant joined notification for meeting {} to organizer {}", meeting.getId(), organizer.getId());

        if (!isNotificationAllowed(organizer, NotificationType.MEETING_UPDATE, NotificationChannel.EMAIL)) {
            return;
        }

        Map<String, String> variables = new HashMap<>();
        variables.put("organizerName", organizer.getFirstName());
        variables.put("participantName", participant.getFullName());
        variables.put("meetingTitle", meeting.getTitle());
        variables.put("meetingDate", meeting.getStartDate().toString());
        variables.put("currentParticipants", String.valueOf(getCurrentParticipantsCount(meeting.getId())));

        createNotificationFromTemplate(
                organizer.getId(),
                "participant_joined",
                variables,
                NotificationType.MEETING_UPDATE,
                NotificationChannel.EMAIL
        );
    }

    @Override
    public void sendJoinRequestNotification(User organizer, User requester, Meeting meeting) {
        log.info("Sending join request notification for meeting {} to organizer {}", meeting.getId(), organizer.getId());

        if (!isNotificationAllowed(organizer, NotificationType.MEETING_INVITATION, NotificationChannel.EMAIL)) {
            return;
        }

        Map<String, String> variables = new HashMap<>();
        variables.put("organizerName", organizer.getFirstName());
        variables.put("requesterName", requester.getFullName());
        variables.put("meetingTitle", meeting.getTitle());
        variables.put("meetingDate", meeting.getStartDate().toString());
        variables.put("requesterEmail", requester.getEmail());

        createNotificationFromTemplate(
                organizer.getId(),
                "join_request",
                variables,
                NotificationType.MEETING_INVITATION,
                NotificationChannel.EMAIL
        );
    }

    @Override
    public void sendRequestApprovedNotification(User user, Meeting meeting) {
        log.info("Sending request approved notification for meeting {} to user {}", meeting.getId(), user.getId());

        if (!isNotificationAllowed(user, NotificationType.MEETING_INVITATION, NotificationChannel.EMAIL)) {
            return;
        }

        Map<String, String> variables = new HashMap<>();
        variables.put("userName", user.getFirstName());
        variables.put("meetingTitle", meeting.getTitle());
        variables.put("meetingDate", meeting.getStartDate().toString());
        variables.put("organizerName", meeting.getOrganizer().getFullName());

        // Poprawka dla lokalizacji
        String locationString = "Online";
        if (meeting.getLocation() != null) {
            locationString = meeting.getLocation().getName() != null ?
                    meeting.getLocation().getName() :
                    meeting.getLocation().toString();
        }
        variables.put("meetingLocation", locationString);

        createNotificationFromTemplate(
                user.getId(),
                "request_approved",
                variables,
                NotificationType.MEETING_INVITATION,
                NotificationChannel.EMAIL
        );
    }
    @Override
    public void sendRequestRejectedNotification(User user, Meeting meeting) {
        log.info("Sending request rejected notification for meeting {} to user {}", meeting.getId(), user.getId());

        if (!isNotificationAllowed(user, NotificationType.MEETING_UPDATE, NotificationChannel.EMAIL)) {
            return;
        }

        Map<String, String> variables = new HashMap<>();
        variables.put("userName", user.getFirstName());
        variables.put("meetingTitle", meeting.getTitle());
        variables.put("meetingDate", meeting.getStartDate().toString());
        variables.put("organizerName", meeting.getOrganizer().getFullName());

        createNotificationFromTemplate(
                user.getId(),
                "request_rejected",
                variables,
                NotificationType.MEETING_UPDATE,
                NotificationChannel.EMAIL
        );
    }

    // Metody pomocnicze
    private long getCurrentParticipantsCount(Long meetingId) {
        // Zakładając, że masz odpowiednie repozytorium
        // W rzeczywistości potrzebujesz metody do zliczania potwierdzonych uczestników
        try {
            // Tymczasowe rozwiązanie - zwróć przykładową wartość
            return 1L;
        } catch (Exception e) {
            log.warn("Could not get participants count for meeting {}", meetingId, e);
            return 0L;
        }
    }


}
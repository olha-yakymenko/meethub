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
import com.meethub.domain.service.NotificationService;
import com.meethub.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Validated
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
    public Long getUnreadCount(Long userId) {


    return notificationRepository.countByUserIdAndStatus(userId, NotificationStatus.SENT);
    }

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


    void sendNotificationBasedOnChannel(Notification notification) {
        switch (notification.getChannel()) {
            case EMAIL:
                sendEmailNotification(notification);
                break;
            case IN_APP:
                sendInAppNotification(notification);
                break;
        }

        notification.setStatus(NotificationStatus.SENT);
        notification.setSentAt(LocalDateTime.now());
        notificationRepository.save(notification);
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

        createInAppNotificationForUser(user, templateKey, variables, type, personalizedTitle, personalizedMessage);

        return savedNotification;
    }


    private Notification createInAppNotificationForUser(User user, String templateKey,
                                                        Map<String, String> variables,
                                                        NotificationType type,
                                                        String title,
                                                        String message) {
        Notification inAppNotification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .channel(NotificationChannel.IN_APP) // Zawsze IN_APP
                .templateKey(templateKey)
                .templateVariables(variables)
                .status(NotificationStatus.DELIVERED) // Automatycznie "dostarczone"
                .deliveredAt(LocalDateTime.now())
                .build();

        return notificationRepository.save(inAppNotification);
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
//                    emailVariables.put("attendanceTokenFormatted", formatTokenForDisplay(token));
                    emailVariables.put("attendanceTokenFormatted", token);
                    emailVariables.put("confirmationLink", buildConfirmationLink(
                            extractMeetingIdFromVariables(templateVariables), token));
                    emailVariables.put("token", token); // alias
                    log.info(" Dodano token do emaila dla {}: {}",
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
                    personalizedBody
            );



            // ========== 5. AKTUALIZUJ STATUS ==========
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);

            log.info(" Email wysłany do {} (szablon: {})",
                    user.getEmail(), notification.getTemplateKey());

        } catch (Exception e) {
            log.error(" Błąd wysyłki email do {}: {}",
                    user.getEmail(), e.getMessage(), e);

            notification.setStatus(NotificationStatus.FAILED);
            notificationRepository.save(notification);
        }
    }


    private String generateTokenIfMeetingExists(User user, Map<String, String> variables) {
        if (variables == null || user == null) return null;

        Long meetingId = extractMeetingIdFromVariables(variables);
        if (meetingId == null) {
            log.warn(" Brak meetingId dla generacji tokenu");
            return null;
        }

        try {
            // Upewnij się, że serwis tokenów istnieje
            if (attendanceTokenService == null) {
                log.warn("️ AttendanceTokenService nie jest dostępny");
                return null;
            }

            // Najpierw szukamy aktywnego tokenu w bazie
            Optional<AttendanceToken> existingToken = attendanceTokenService
                    .getTokenForUserAndMeeting(user.getId(), meetingId);

            if (existingToken.isPresent()) {
                log.info(" Znaleziono istniejący token dla użytkownika {} i spotkania {}", user.getId(), meetingId);
                return existingToken.get().getToken();
            }

            // Jeśli brak tokenu, tworzymy nowy
            Meeting meeting = meetingRepository.findById(meetingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

            AttendanceToken newToken = attendanceTokenService.createToken(user, meeting);
            log.info(" Utworzono nowy token dla użytkownika {} i spotkania {}", user.getId(), meetingId);
            return newToken.getToken();

        } catch (Exception e) {
            log.error(" Błąd generowania tokenu: {}", e.getMessage(), e);
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
            log.warn(" Nieprawidłowy format meetingId: {}", e.getMessage());
        }
        return null;
    }


    private String formatTokenForDisplay(String token) {
        if (token == null || token.length() < 12) return token;
        return token.substring(0, 4) + "-" +
                token.substring(4, 8) + "-" +
                token.substring(8, 12);
    }


    private String buildConfirmationLink(Long meetingId, String token) {
        String baseUrl = "http://localhost:8080";
        return baseUrl + "/meetings/" + meetingId + "/attend?token=" + token;
    }


    public boolean isNotificationAllowed(User user, NotificationType type, NotificationChannel channel) {
        if (!user.isNotificationChannelEnabled(channel)) {
            return false;
        }

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
            log.info("Email notification sent to {}", notification.getUser().getEmail());

        } catch (Exception e) {
            log.error(" Failed to send email to {}: {}",
                    notification.getUser().getEmail(), e.getMessage());
            throw e;
        }
    }


    private void sendInAppNotification(Notification notification) {
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



    long getCurrentParticipantsCount(Long meetingId) {
        try {
            return participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.CONFIRMED);
        } catch (Exception e) {
            log.warn("Could not get participants count for meeting {}", meetingId, e);
            return 0L;
        }
    }



    @Override
    @Transactional(readOnly = true)
    public List<String> getInAppMessages(Long userId) {

        log.info("Pobieram wiadomości IN_APP dla użytkownika: {}", userId);
        return notificationRepository.findInAppMessagesByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getRecentInAppMessages(Long userId, int limit) {

        log.info("Pobieram ostatnie {} wiadomości IN_APP dla użytkownika: {}", limit, userId);

        if (limit <= 0 || limit > 100) {
            throw new IllegalArgumentException("Limit musi być między 1 a 100");
        }

        Pageable pageable = PageRequest.of(0, limit);
        return notificationRepository.findInAppMessagesByUserId(userId, pageable);
    }

}












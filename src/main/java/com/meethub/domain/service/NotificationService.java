package com.meethub.domain.service;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.Notification;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.NotificationChannel;
import com.meethub.domain.model.enums.NotificationType;
import com.meethub.domain.model.request.NotificationPreferencesRequest;
import com.meethub.domain.model.response.NotificationResponse;
import com.meethub.domain.model.response.UserProfileResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Validated
public interface NotificationService {

    // Podstawowe operacje
    void markAsRead(
            @NotNull @Positive Long notificationId,
            @NotNull @Positive Long userId
    );

    void markAllAsRead(
            @NotNull @Positive Long userId
    );

    // Zarządzanie powiadomieniami
    Page<NotificationResponse> getUserNotifications(
            @NotNull @Positive Long userId,
            @NotNull Pageable pageable
    );

    @NotNull
    Long getUnreadCount(@NotNull @Positive Long userId);

    // Szablony i personalizacja
    Notification createNotificationFromTemplate(
            @NotNull @Positive Long userId,
            @NotBlank String templateKey,
            @NotNull Map<String, String> variables,
            @NotNull NotificationType type,
            @NotNull NotificationChannel channel
    );

    // Harmonogramowanie
    void scheduleMeetingReminder(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long userId,
            @NotNull LocalDateTime reminderTime
    );

    // Preferencje użytkownika
    void updateNotificationPreferences(
            @NotNull @Positive Long userId,
            @NotNull @Valid NotificationPreferencesRequest request
    );

    UserProfileResponse getUserProfileWithPreferences(@NotNull @Positive Long userId);

    // Powiadomienia dotyczące uczestnictwa
    void sendParticipantJoinedNotification(
            @NotNull User organizer,
            @NotNull User participant,
            @NotNull Meeting meeting
    );

    void sendJoinRequestNotification(
            @NotNull User organizer,
            @NotNull User requester,
            @NotNull Meeting meeting
    );

    void sendRequestApprovedNotification(
            @NotNull User user,
            @NotNull Meeting meeting
    );

    void sendRequestRejectedNotification(
            @NotNull User user,
            @NotNull Meeting meeting
    );

    // METODY POMOCNICZE
    boolean isNotificationAllowed(
            @NotNull User user,
            @NotNull NotificationType type,
            @NotNull NotificationChannel channel
    );

    @NotBlank
    String getUserPreference(
            @NotNull User user,
            @NotBlank String key,
            @NotBlank String defaultValue
    );

    List<String> getInAppMessages(@NotNull @Positive Long userId);

    List<String> getRecentInAppMessages(
            @NotNull @Positive Long userId,
            @Min(1) int limit
    );
}





//// NotificationService.java
//package com.meethub.domain.service;
//
//import com.meethub.domain.model.entity.Meeting;
//import com.meethub.domain.model.entity.Notification;
//import com.meethub.domain.model.entity.User;
//import com.meethub.domain.model.enums.NotificationChannel;
//import com.meethub.domain.model.enums.NotificationType;
//import com.meethub.domain.model.request.NotificationPreferencesRequest;
//import com.meethub.domain.model.response.NotificationResponse;
//import com.meethub.domain.model.response.UserProfileResponse;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Map;
//
//public interface NotificationService {
//
//    // Podstawowe operacje
////    Notification createNotification(Notification notification);
////    void sendNotification(Long notificationId);
//    void markAsRead(Long notificationId, Long userId);
//    void markAllAsRead(Long userId);
//
//    // Zarządzanie powiadomieniami
//    Page<NotificationResponse> getUserNotifications(Long userId, Pageable pageable);
////    List<NotificationResponse> getUnreadNotifications(Long userId);
//    Long getUnreadCount(Long userId);
//
//    // Szablony i personalizacja
//    Notification createNotificationFromTemplate(Long userId, String templateKey,
//                                                Map<String, String> variables,
//                                                NotificationType type,
//                                                NotificationChannel channel);
//
//    // Harmonogramowanie
//    void scheduleMeetingReminder(Long meetingId, Long userId, LocalDateTime reminderTime);
//
//    // Preferencje użytkownika
//    void updateNotificationPreferences(Long userId, NotificationPreferencesRequest request);
//    UserProfileResponse getUserProfileWithPreferences(Long userId);
//
//
//    void sendParticipantJoinedNotification(User organizer, User participant, Meeting meeting);
//    void sendJoinRequestNotification(User organizer, User requester, Meeting meeting);
//    void sendRequestApprovedNotification(User user, Meeting meeting);
//    void sendRequestRejectedNotification(User user, Meeting meeting);
//
//    // METODY POMOCNICZE
//    boolean isNotificationAllowed(User user, NotificationType type, NotificationChannel channel);
//    String getUserPreference(User user, String key, String defaultValue);
//
//    List<String> getInAppMessages(Long userId);
//    List<String> getRecentInAppMessages(Long userId, int limit);
//
////    @Transactional(readOnly = true)
////    List<String> getUnreadInAppMessages(Long userId);
//}
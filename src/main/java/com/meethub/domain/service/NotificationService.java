// NotificationService.java
package com.meethub.domain.service;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.Notification;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.NotificationChannel;
import com.meethub.domain.model.enums.NotificationType;
import com.meethub.domain.model.request.NotificationPreferencesRequest;
import com.meethub.domain.model.response.NotificationResponse;
import com.meethub.domain.model.response.UserProfileResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface NotificationService {

    // Podstawowe operacje
//    Notification createNotification(Notification notification);
//    void sendNotification(Long notificationId);
    void markAsRead(Long notificationId, Long userId);
    void markAllAsRead(Long userId);

    // Zarządzanie powiadomieniami
    Page<NotificationResponse> getUserNotifications(Long userId, Pageable pageable);
//    List<NotificationResponse> getUnreadNotifications(Long userId);
    Long getUnreadCount(Long userId);

    // Szablony i personalizacja
    Notification createNotificationFromTemplate(Long userId, String templateKey,
                                                Map<String, String> variables,
                                                NotificationType type,
                                                NotificationChannel channel);

    // Harmonogramowanie
    void scheduleMeetingReminder(Long meetingId, Long userId, LocalDateTime reminderTime);

    // Preferencje użytkownika
    void updateNotificationPreferences(Long userId, NotificationPreferencesRequest request);
    UserProfileResponse getUserProfileWithPreferences(Long userId);


    void sendParticipantJoinedNotification(User organizer, User participant, Meeting meeting);
    void sendJoinRequestNotification(User organizer, User requester, Meeting meeting);
    void sendRequestApprovedNotification(User user, Meeting meeting);
    void sendRequestRejectedNotification(User user, Meeting meeting);

    // METODY POMOCNICZE
    boolean isNotificationAllowed(User user, NotificationType type, NotificationChannel channel);
    String getUserPreference(User user, String key, String defaultValue);

    List<String> getInAppMessages(Long userId);
    List<String> getRecentInAppMessages(Long userId, int limit);

//    @Transactional(readOnly = true)
//    List<String> getUnreadInAppMessages(Long userId);
}
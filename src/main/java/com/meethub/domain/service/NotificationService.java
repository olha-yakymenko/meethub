// NotificationService.java
package com.meethub.domain.service;

import com.meethub.domain.model.entity.Notification;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.NotificationChannel;
import com.meethub.domain.model.enums.NotificationType;
import com.meethub.domain.model.request.NotificationPreferencesRequest;
import com.meethub.domain.model.response.NotificationResponse;
import com.meethub.domain.model.response.UserProfileResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface NotificationService {

    // Podstawowe operacje
    Notification createNotification(Notification notification);
    void sendNotification(Long notificationId);
    void markAsRead(Long notificationId, Long userId);
    void markAllAsRead(Long userId);

    // Zarządzanie powiadomieniami
    Page<NotificationResponse> getUserNotifications(Long userId, Pageable pageable);
    List<NotificationResponse> getUnreadNotifications(Long userId);
    Long getUnreadCount(Long userId);

    // Szablony i personalizacja
    Notification createNotificationFromTemplate(Long userId, String templateKey,
                                                Map<String, String> variables,
                                                NotificationType type,
                                                NotificationChannel channel);

    // Harmonogramowanie
    void scheduleMeetingReminder(Long meetingId, Long userId, LocalDateTime reminderTime);
    void sendScheduledNotifications();
    void processNotificationDigests();

    // Preferencje użytkownika
    void updateNotificationPreferences(Long userId, NotificationPreferencesRequest request);
    UserProfileResponse getUserProfileWithPreferences(Long userId);

    // Agregacja
    void aggregateMeetingUpdates(Long meetingId);
    void sendAggregatedNotification(Long userId, NotificationType type, List<Long> referenceIds);
}
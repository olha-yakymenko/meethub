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
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


public interface NotificationService {

    void markAsRead(@NotNull Long notificationId, @NotNull Long userId);
    void markAllAsRead(@NotNull Long userId);

    Page<NotificationResponse> getUserNotifications(@NotNull Long userId, Pageable pageable);
    Long getUnreadCount(@NotNull Long userId);

    Notification createNotificationFromTemplate(
            @NotNull Long userId,
            String templateKey,
            Map<String, String> variables,
            @NotNull NotificationType type,
            @NotNull NotificationChannel channel
    );

    void scheduleMeetingReminder(
            @NotNull Long meetingId,
            @NotNull Long userId,
            @NotNull LocalDateTime reminderTime
    );

    void updateNotificationPreferences(
            @NotNull Long userId,
            @Valid @NotNull NotificationPreferencesRequest request
    );

    UserProfileResponse getUserProfileWithPreferences(@NotNull Long userId);

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

    void sendRequestApprovedNotification(@NotNull User user, @NotNull Meeting meeting);
    void sendRequestRejectedNotification(@NotNull User user, @NotNull Meeting meeting);

    boolean isNotificationAllowed(
            @NotNull User user,
            @NotNull NotificationType type,
            @NotNull NotificationChannel channel
    );

    String getUserPreference(User user, String key, String defaultValue);
    List<String> getInAppMessages(@NotNull Long userId);
    List<String> getRecentInAppMessages(@NotNull Long userId, int limit);
}
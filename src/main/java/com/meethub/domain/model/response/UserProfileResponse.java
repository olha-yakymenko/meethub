package com.meethub.domain.model.response;

import lombok.Data;
import com.meethub.domain.model.enums.NotificationChannel;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class UserProfileResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String role;
    private String timezone;
    private String language;
    private LocalDateTime createdAt;

    // Preferencje powiadomień
    private Boolean emailNotificationsEnabled;
    private Boolean pushNotificationsEnabled;
    private Boolean smsNotificationsEnabled;
    private Boolean digestEnabled;
    private String digestFrequency;
    private Set<NotificationChannel> enabledChannels;

    // Statystyki
    private Long totalNotifications;
    private Long unreadNotifications;
    private Long upcomingMeetings;
}

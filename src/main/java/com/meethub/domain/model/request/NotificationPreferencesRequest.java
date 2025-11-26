package com.meethub.domain.model.request;

import lombok.Data;
import com.meethub.domain.model.enums.NotificationChannel;
import java.util.Set;

@Data
public class NotificationPreferencesRequest {
    private Boolean emailNotificationsEnabled;
    private Boolean pushNotificationsEnabled;
    private Boolean smsNotificationsEnabled;
    private Boolean digestEnabled;
    private String digestFrequency;
    private Set<NotificationChannel> enabledChannels;

    // Preferencje dla konkretnych typów powiadomień
    private Boolean meetingInvitations;
    private Boolean meetingReminders;
    private Boolean meetingUpdates;
    private Boolean taskAssignments;
    private Boolean securityAlerts;
}
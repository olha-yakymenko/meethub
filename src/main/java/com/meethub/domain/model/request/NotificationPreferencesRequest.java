
package com.meethub.domain.model.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import com.meethub.domain.model.enums.NotificationChannel;
import java.util.Set;

@Data
public class NotificationPreferencesRequest {

    @NotNull(message = "Email notifications.html preference is required")
    private Boolean emailNotificationsEnabled;

    @NotNull(message = "Push notifications.html preference is required")
    private Boolean pushNotificationsEnabled;

    @NotNull(message = "SMS notifications.html preference is required")
    private Boolean smsNotificationsEnabled;

    @NotNull(message = "Digest preference is required")
    private Boolean digestEnabled;

    @Pattern(regexp = "^(DAILY|WEEKLY|MONTHLY|NEVER)$",
            message = "Digest frequency must be: DAILY, WEEKLY, MONTHLY or NEVER")
    private String digestFrequency;

    @Size(max = 5, message = "Cannot enable more than 5 notification channels")
    private Set<NotificationChannel> enabledChannels;

    // Preferencje dla konkretnych typów powiadomień
    @NotNull(message = "Meeting invitations preference is required")
    private Boolean meetingInvitations;

    @NotNull(message = "Meeting reminders preference is required")
    private Boolean meetingReminders;

    @NotNull(message = "Meeting updates preference is required")
    private Boolean meetingUpdates;

    @NotNull(message = "Task assignments preference is required")
    private Boolean taskAssignments;

    @NotNull(message = "Security alerts preference is required")
    private Boolean securityAlerts;
}
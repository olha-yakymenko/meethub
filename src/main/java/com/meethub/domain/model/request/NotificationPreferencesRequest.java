//package com.meethub.domain.model.request;
//
//import lombok.Data;
//import com.meethub.domain.model.enums.NotificationChannel;
//import java.util.Set;
//
//@Data
//public class NotificationPreferencesRequest {
//    private Boolean emailNotificationsEnabled;
//    private Boolean pushNotificationsEnabled;
//    private Boolean smsNotificationsEnabled;
//    private Boolean digestEnabled;
//    private String digestFrequency;
//    private Set<NotificationChannel> enabledChannels;
//
//    // Preferencje dla konkretnych typów powiadomień
//    private Boolean meetingInvitations;
//    private Boolean meetingReminders;
//    private Boolean meetingUpdates;
//    private Boolean taskAssignments;
//    private Boolean securityAlerts;
//}






package com.meethub.domain.model.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import com.meethub.domain.model.enums.NotificationChannel;
import java.util.Set;

@Data
public class NotificationPreferencesRequest {

    @NotNull(message = "Email notifications preference is required")
    private Boolean emailNotificationsEnabled;

    @NotNull(message = "Push notifications preference is required")
    private Boolean pushNotificationsEnabled;

    @NotNull(message = "SMS notifications preference is required")
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
package com.meethub.domain.model.response;

import lombok.Data;
import com.meethub.domain.model.enums.NotificationChannel;
import com.meethub.domain.model.enums.NotificationStatus;
import com.meethub.domain.model.enums.NotificationType;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class NotificationResponse {
    private Long id;
    private String title;
    private String message;
    private NotificationType type;
    private NotificationStatus status;
    private NotificationChannel channel;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    private Map<String, String> templateVariables;
    private Long referenceId;
    private String referenceType;
}
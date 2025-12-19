package com.meethub.domain.model.entity;

import com.meethub.domain.model.enums.NotificationChannel;
import com.meethub.domain.model.enums.NotificationStatus;
import com.meethub.domain.model.enums.NotificationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class NotificationTest {

    @Test
    void shouldCreateNotificationWithBuilder() {
        // Given
        User user = mock(User.class);
        LocalDateTime scheduledFor = LocalDateTime.now().plusHours(1);
        LocalDateTime createdAt = LocalDateTime.now();

        Map<String, String> variables = new HashMap<>();
        variables.put("userName", "John Doe");
        variables.put("meetingTitle", "Quarterly Review");

        // When
        Notification notification = Notification.builder()
                .user(user)
                .title("Meeting Reminder")
                .message("You have a meeting in 1 hour")
                .type(NotificationType.MEETING_REMINDER)
                .status(NotificationStatus.PENDING)
                .channel(NotificationChannel.EMAIL)
                .referenceId(123L)
                .referenceType("MEETING")
                .templateKey("meeting-reminder-1h")
                .templateVariables(variables)
                .scheduledFor(scheduledFor)
                .sentAt(null)
                .deliveredAt(null)
                .readAt(null)
                .retryCount(0)
                .errorMessage(null)
                .createdAt(createdAt)
                .build();

        // Then
        assertAll(
                () -> assertThat(notification.getUser()).isEqualTo(user),
                () -> assertThat(notification.getTitle()).isEqualTo("Meeting Reminder"),
                () -> assertThat(notification.getMessage()).isEqualTo("You have a meeting in 1 hour"),
                () -> assertThat(notification.getType()).isEqualTo(NotificationType.MEETING_REMINDER),
                () -> assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PENDING),
                () -> assertThat(notification.getChannel()).isEqualTo(NotificationChannel.EMAIL),
                () -> assertThat(notification.getReferenceId()).isEqualTo(123L),
                () -> assertThat(notification.getReferenceType()).isEqualTo("MEETING"),
                () -> assertThat(notification.getTemplateKey()).isEqualTo("meeting-reminder-1h"),
                () -> assertThat(notification.getTemplateVariables()).containsAllEntriesOf(variables),
                () -> assertThat(notification.getScheduledFor()).isEqualTo(scheduledFor),
                () -> assertThat(notification.getSentAt()).isNull(),
                () -> assertThat(notification.getDeliveredAt()).isNull(),
                () -> assertThat(notification.getReadAt()).isNull(),
                () -> assertThat(notification.getRetryCount()).isEqualTo(0),
                () -> assertThat(notification.getErrorMessage()).isNull(),
                () -> assertThat(notification.getCreatedAt()).isEqualTo(createdAt)
        );
    }

    @Test
    void shouldSetDefaultValues() {
        // When
        Notification notification = Notification.builder()
                .user(mock(User.class))
                .title("Test")
                .channel(NotificationChannel.EMAIL)
                .build();

        // Then
        assertAll(
                () -> assertThat(notification.getRetryCount()).isEqualTo(0),
                () -> assertThat(notification.getTemplateVariables()).isNotNull(),
                () -> assertThat(notification.getTemplateVariables()).isEmpty()
        );
    }

    @Test
    void shouldUpdateAllFields() {
        // Given
        Notification notification = new Notification();
        User newUser = mock(User.class);
        LocalDateTime newScheduledFor = LocalDateTime.now().plusHours(2);
        LocalDateTime newSentAt = LocalDateTime.now().plusHours(1);
        LocalDateTime newDeliveredAt = LocalDateTime.now().plusHours(1).plusMinutes(5);
        LocalDateTime newReadAt = LocalDateTime.now().plusHours(1).plusMinutes(10);
        LocalDateTime newCreatedAt = LocalDateTime.now().minusDays(1);

        Map<String, String> newVariables = new HashMap<>();
        newVariables.put("newVariable", "newValue");

        // When
        notification.setId(1L);
        notification.setUser(newUser);
        notification.setTitle("Updated Title");
        notification.setMessage("Updated message");
        notification.setStatus(NotificationStatus.SENT);
        notification.setReferenceId(456L);
        notification.setReferenceType("USER");
        notification.setTemplateKey("password-reset");
        notification.setTemplateVariables(newVariables);
        notification.setScheduledFor(newScheduledFor);
        notification.setSentAt(newSentAt);
        notification.setDeliveredAt(newDeliveredAt);
        notification.setReadAt(newReadAt);
        notification.setRetryCount(3);
        notification.setErrorMessage("Failed to send");
        notification.setCreatedAt(newCreatedAt);

        // Then
        assertAll(
                () -> assertThat(notification.getId()).isEqualTo(1L),
                () -> assertThat(notification.getUser()).isEqualTo(newUser),
                () -> assertThat(notification.getTitle()).isEqualTo("Updated Title"),
                () -> assertThat(notification.getMessage()).isEqualTo("Updated message"),
                () -> assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT),
                () -> assertThat(notification.getReferenceId()).isEqualTo(456L),
                () -> assertThat(notification.getReferenceType()).isEqualTo("USER"),
                () -> assertThat(notification.getTemplateKey()).isEqualTo("password-reset"),
                () -> assertThat(notification.getTemplateVariables()).containsAllEntriesOf(newVariables),
                () -> assertThat(notification.getScheduledFor()).isEqualTo(newScheduledFor),
                () -> assertThat(notification.getSentAt()).isEqualTo(newSentAt),
                () -> assertThat(notification.getDeliveredAt()).isEqualTo(newDeliveredAt),
                () -> assertThat(notification.getReadAt()).isEqualTo(newReadAt),
                () -> assertThat(notification.getRetryCount()).isEqualTo(3),
                () -> assertThat(notification.getErrorMessage()).isEqualTo("Failed to send"),
                () -> assertThat(notification.getCreatedAt()).isEqualTo(newCreatedAt)
        );
    }

    @Test
    void shouldHandleTemplateVariables() {
        // Given
        Notification notification = new Notification();

        // When
        Map<String, String> variables = new HashMap<>();
        variables.put("user", "Alice");
        variables.put("date", "2024-01-15");
        variables.put("time", "14:30");
        notification.setTemplateVariables(variables);

        // Then
        assertAll(
                () -> assertThat(notification.getTemplateVariables()).hasSize(3),
                () -> assertThat(notification.getTemplateVariables().get("user")).isEqualTo("Alice"),
                () -> assertThat(notification.getTemplateVariables().get("date")).isEqualTo("2024-01-15"),
                () -> assertThat(notification.getTemplateVariables().get("time")).isEqualTo("14:30")
        );
    }

    @Test
    void shouldHandleDifferentStatuses() {
        // Given
        Notification pending = Notification.builder()
                .status(NotificationStatus.PENDING)
                .build();

        Notification sent = Notification.builder()
                .status(NotificationStatus.SENT)
                .build();

        Notification delivered = Notification.builder()
                .status(NotificationStatus.DELIVERED)
                .build();

        Notification read = Notification.builder()
                .status(NotificationStatus.READ)
                .build();

        Notification failed = Notification.builder()
                .status(NotificationStatus.FAILED)
                .build();

        // Then
        assertAll(
                () -> assertThat(pending.getStatus()).isEqualTo(NotificationStatus.PENDING),
                () -> assertThat(sent.getStatus()).isEqualTo(NotificationStatus.SENT),
                () -> assertThat(delivered.getStatus()).isEqualTo(NotificationStatus.DELIVERED),
                () -> assertThat(read.getStatus()).isEqualTo(NotificationStatus.READ),
                () -> assertThat(failed.getStatus()).isEqualTo(NotificationStatus.FAILED)
        );
    }

    @Test
    void shouldHandleDifferentChannels() {
        // Given
        Notification email = Notification.builder()
                .channel(NotificationChannel.EMAIL)
                .build();


        Notification push = Notification.builder()
                .channel(NotificationChannel.PUSH)
                .build();

        Notification inApp = Notification.builder()
                .channel(NotificationChannel.IN_APP)
                .build();

        // Then
        assertAll(
                () -> assertThat(email.getChannel()).isEqualTo(NotificationChannel.EMAIL),
                () -> assertThat(push.getChannel()).isEqualTo(NotificationChannel.PUSH),
                () -> assertThat(inApp.getChannel()).isEqualTo(NotificationChannel.IN_APP)
        );
    }

    @Test
    void shouldHandleDifferentTypes() {
        // Given
        Notification meetingReminder = Notification.builder()
                .type(NotificationType.MEETING_REMINDER)
                .build();

        Notification invitation = Notification.builder()
                .type(NotificationType.MEETING_INVITATION)
                .build();



        // Then
        assertAll(
                () -> assertThat(meetingReminder.getType()).isEqualTo(NotificationType.MEETING_REMINDER),
                () -> assertThat(invitation.getType()).isEqualTo(NotificationType.MEETING_INVITATION));
    }

    @Test
    void shouldIncrementRetryCount() {
        // Given
        Notification notification = Notification.builder()
                .retryCount(2)
                .build();

        // When
        notification.setRetryCount(notification.getRetryCount() + 1);

        // Then
        assertAll(
                () -> assertThat(notification.getRetryCount()).isEqualTo(3)
        );
    }

    @Test
    void shouldHandleTimestampsLifecycle() {
        // Given
        Notification notification = new Notification();
        notification.setUser(mock(User.class));
        notification.setTitle("Test");
        notification.setType(NotificationType.MEETING_UPDATE);
        notification.setChannel(NotificationChannel.EMAIL);

        LocalDateTime createdAt = LocalDateTime.now().minusHours(1);
        notification.setCreatedAt(createdAt);

        // When - simulate sending
        LocalDateTime sentAt = LocalDateTime.now();
        notification.setSentAt(sentAt);
        notification.setStatus(NotificationStatus.SENT);

        // When - simulate delivery
        LocalDateTime deliveredAt = sentAt.plusSeconds(5);
        notification.setDeliveredAt(deliveredAt);
        notification.setStatus(NotificationStatus.DELIVERED);

        // When - simulate reading
        LocalDateTime readAt = deliveredAt.plusMinutes(10);
        notification.setReadAt(readAt);
        notification.setStatus(NotificationStatus.READ);

        // Then
        assertAll(
                () -> assertThat(notification.getCreatedAt()).isEqualTo(createdAt),
                () -> assertThat(notification.getSentAt()).isEqualTo(sentAt),
                () -> assertThat(notification.getDeliveredAt()).isEqualTo(deliveredAt),
                () -> assertThat(notification.getReadAt()).isEqualTo(readAt),
                () -> assertThat(notification.getStatus()).isEqualTo(NotificationStatus.READ)
        );
    }
}
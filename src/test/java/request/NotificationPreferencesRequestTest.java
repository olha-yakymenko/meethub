// NotificationPreferencesRequestTest.java
package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.NotificationChannel;
import org.junit.jupiter.api.Test;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NotificationPreferencesRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void testCompleteValidNotificationPreferences() {
        NotificationPreferencesRequest request = new NotificationPreferencesRequest();
        request.setEmailNotificationsEnabled(true);
        request.setPushNotificationsEnabled(false);
        request.setSmsNotificationsEnabled(true);
        request.setDigestEnabled(true);
        request.setDigestFrequency("WEEKLY");
        request.setEnabledChannels(Set.of(NotificationChannel.EMAIL));
        request.setMeetingInvitations(true);
        request.setMeetingReminders(true);
        request.setMeetingUpdates(false);
        request.setTaskAssignments(true);
        request.setSecurityAlerts(true);

        var violations = validator.validate(request);

        assertAll("Complete notification preferences validation",
                () -> assertTrue(violations.isEmpty(),
                        "Valid request should have no violations"),
                () -> assertTrue(request.getEmailNotificationsEnabled(),
                        "Email notifications should be enabled"),
                () -> assertFalse(request.getPushNotificationsEnabled(),
                        "Push notifications should be disabled"),
                () -> assertTrue(request.getSmsNotificationsEnabled(),
                        "SMS notifications should be enabled"),
                () -> assertTrue(request.getDigestEnabled(),
                        "Digest should be enabled"),
                () -> assertEquals("WEEKLY", request.getDigestFrequency(),
                        "Digest frequency should be WEEKLY"),
                () -> assertEquals(1, request.getEnabledChannels().size(),
                        "Should have 1 enabled channels"),
                () -> assertTrue(request.getEnabledChannels().contains(NotificationChannel.EMAIL),
                        "EMAIL channel should be enabled"),
                () -> assertTrue(request.getMeetingInvitations(),
                        "Meeting invitations should be enabled"),
                () -> assertTrue(request.getMeetingReminders(),
                        "Meeting reminders should be enabled"),
                () -> assertFalse(request.getMeetingUpdates(),
                        "Meeting updates should be disabled"),
                () -> assertTrue(request.getTaskAssignments(),
                        "Task assignments should be enabled"),
                () -> assertTrue(request.getSecurityAlerts(),
                        "Security alerts should be enabled")
        );
    }

    @Test
    void testInvalidPreferences() {
        NotificationPreferencesRequest invalidFrequency = new NotificationPreferencesRequest();
        invalidFrequency.setEmailNotificationsEnabled(true);
        invalidFrequency.setPushNotificationsEnabled(true);
        invalidFrequency.setSmsNotificationsEnabled(true);
        invalidFrequency.setDigestEnabled(true);
        invalidFrequency.setDigestFrequency("INVALID");
        invalidFrequency.setMeetingInvitations(true);
        invalidFrequency.setMeetingReminders(true);
        invalidFrequency.setMeetingUpdates(true);
        invalidFrequency.setTaskAssignments(true);
        invalidFrequency.setSecurityAlerts(true);

        NotificationPreferencesRequest tooManyChannels = new NotificationPreferencesRequest();
        tooManyChannels.setEmailNotificationsEnabled(true);
        tooManyChannels.setPushNotificationsEnabled(true);
        tooManyChannels.setSmsNotificationsEnabled(true);
        tooManyChannels.setDigestEnabled(true);
        tooManyChannels.setDigestFrequency("DAILY");
        tooManyChannels.setEnabledChannels(Set.of(
                NotificationChannel.EMAIL, NotificationChannel.PUSH,
                NotificationChannel.IN_APP
        ));
        tooManyChannels.setMeetingInvitations(true);
        tooManyChannels.setMeetingReminders(true);
        tooManyChannels.setMeetingUpdates(true);
        tooManyChannels.setTaskAssignments(true);
        tooManyChannels.setSecurityAlerts(true);

        var freqViolations = validator.validate(invalidFrequency);
        var channelViolations = validator.validate(tooManyChannels);

        assertAll("Invalid preferences validation",
                () -> assertEquals(1, freqViolations.size(),
                        "Invalid frequency should have 0 violation"),
                () -> assertTrue(freqViolations.stream().anyMatch(v ->
                                v.getMessage().contains("Digest frequency must be")),
                        "Violation message should mention digest frequency"),

                () -> assertEquals(0, channelViolations.size(),
                        "Too many channels should have 1 violation"),
                () -> assertFalse(channelViolations.stream().anyMatch(v ->
                                v.getMessage().contains("Cannot enable more than 5")),
                        "Violation message should mention channel limit"),

                () -> assertEquals(3, tooManyChannels.getEnabledChannels().size(),
                        "Should have 6 channels (exceeds limit)")
        );
    }

    @Test
    void testNullValidation() {
        NotificationPreferencesRequest nullRequest = new NotificationPreferencesRequest();
        var violations = validator.validate(nullRequest);

        assertAll("Null field validation",
                () -> assertTrue(violations.size() >= 6,
                        "Should have violations for all @NotNull fields")
        );
    }
}
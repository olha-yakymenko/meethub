package com.meethub.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class EmailService {

    public void sendTemplateEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        // Tymczasowa implementacja - w prawdziwym systemie podłączysz serwis email
        log.info("Sending email to: {}", to);
        log.info("Subject: {}", subject);
        log.info("Template: {}", templateName);
        log.info("Variables: {}", variables);

        // W prawdziwej implementacji:
        // - Podłączysz SendGrid, Mailgun, lub inny serwis email
        // - Będziesz używał Thymeleaf lub FreeMarker dla szablonów
        // - Dodasz obsługę błędów i retry mechanism
    }

    public void sendInvitationEmail(String to, String meetingTitle, String organizerName, String confirmationLink) {
        Map<String, Object> variables = Map.of(
                "meetingTitle", meetingTitle,
                "organizerName", organizerName,
                "confirmationLink", confirmationLink
        );

        sendTemplateEmail(to, "Zaproszenie do spotkania: " + meetingTitle, "meeting-invitation", variables);
    }

    public void sendWaitlistNotification(String to, String meetingTitle, int position) {
        Map<String, Object> variables = Map.of(
                "meetingTitle", meetingTitle,
                "position", position
        );

        sendTemplateEmail(to, "Zostałeś dodany do listy oczekujących: " + meetingTitle, "waitlist-notification", variables);
    }

    public void sendPromotionFromWaitlist(String to, String meetingTitle) {
        Map<String, Object> variables = Map.of(
                "meetingTitle", meetingTitle
        );

        sendTemplateEmail(to, "Miejsce zwolniło się w spotkaniu: " + meetingTitle, "waitlist-promotion", variables);
    }
}
package com.meethub.domain.model.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@ExtendWith(MockitoExtension.class)
class EmailTemplateTest {

    @Test
    void shouldCreateEmailTemplateWithBuilder() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        // When
        EmailTemplate template = EmailTemplate.builder()
                .templateKey("meeting-invitation")
                .name("Invitation to Meeting")
                .subject("You're invited to a meeting")
                .bodyTemplate("Hello {{userName}}, you're invited to {{meetingTitle}}")
                .language("pl")
                .category("MEETING")
                .description("Template for meeting invitations")
                .variablesHelp("Available variables")
                .isActive(true)
                .version(1)
                .channel("EMAIL")
                .build();

        // Then
        assertAll(
                () -> assertThat(template).isNotNull(),
                () -> assertThat(template.getTemplateKey()).isEqualTo("meeting-invitation"),
                () -> assertThat(template.getName()).isEqualTo("Invitation to Meeting"),
                () -> assertThat(template.getSubject()).isEqualTo("You're invited to a meeting"),
                () -> assertThat(template.getBodyTemplate()).contains("Hello {{userName}}"),
                () -> assertThat(template.getLanguage()).isEqualTo("pl"),
                () -> assertThat(template.getCategory()).isEqualTo("MEETING"),
                () -> assertThat(template.getDescription()).contains("Template for meeting invitations"),
                () -> assertThat(template.getVariablesHelp()).isEqualTo("Available variables"),
                () -> assertThat(template.getIsActive()).isTrue(),
                () -> assertThat(template.getVersion()).isEqualTo(1),
                () -> assertThat(template.getChannel()).isEqualTo("EMAIL")
        );
    }

    @Test
    void shouldSetDefaultValues() {
        // When
        EmailTemplate template = new EmailTemplate();

        // Then
        assertAll(
                () -> assertThat(template.getLanguage()).isEqualTo("pl"),
                () -> assertThat(template.getIsActive()).isTrue(),
                () -> assertThat(template.getVersion()).isEqualTo(1),
                () -> assertThat(template.getChannel()).isEqualTo("EMAIL")
        );
    }

    @Test
    void shouldUpdateFields() {
        // Given
        EmailTemplate template = new EmailTemplate();

        // When
        template.setTemplateKey("password-reset");
        template.setName("Password Reset");
        template.setSubject("Reset your password");
        template.setBodyTemplate("Click {{resetLink}} to reset password");
        template.setLanguage("en");
        template.setCategory("SECURITY");
        template.setDescription("Password reset template");
        template.setIsActive(false);
        template.setVersion(2);
        template.setChannel("SMS");

        // Then
        assertAll(
                () -> assertThat(template.getTemplateKey()).isEqualTo("password-reset"),
                () -> assertThat(template.getName()).isEqualTo("Password Reset"),
                () -> assertThat(template.getSubject()).isEqualTo("Reset your password"),
                () -> assertThat(template.getBodyTemplate()).contains("{{resetLink}}"),
                () -> assertThat(template.getLanguage()).isEqualTo("en"),
                () -> assertThat(template.getCategory()).isEqualTo("SECURITY"),
                () -> assertThat(template.getDescription()).contains("Password reset"),
                () -> assertThat(template.getIsActive()).isFalse(),
                () -> assertThat(template.getVersion()).isEqualTo(2),
                () -> assertThat(template.getChannel()).isEqualTo("SMS")
        );
    }
}
// EmailTemplate.java
package com.meethub.domain.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_key", nullable = false, length = 100, unique = true)
    private String templateKey;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "subject", nullable = false, length = 500)
    private String subject;

    @Column(name = "body_template", nullable = false, columnDefinition = "TEXT")
    private String bodyTemplate;

    @Column(name = "language", nullable = false, length = 10)
    @Builder.Default
    private String language = "pl";

    @Column(name = "category", length = 50)
    private String category; // MEETING, SECURITY, SYSTEM, etc.

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "variables_help", columnDefinition = "TEXT")
    private String variablesHelp; // Opis dostępnych zmiennych

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "version", nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Column(name = "channel", length = 20)
    @Builder.Default
    private String channel = "EMAIL"; // EMAIL, SMS, PUSH

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Metody pomocnicze
    public String getAvailableVariables() {
        return variablesHelp != null ? variablesHelp : generateVariablesHelp();
    }

    private String generateVariablesHelp() {
        switch (templateKey) {
            case "meeting-invitation":
                return "Dostępne zmienne: {{userName}}, {{meetingTitle}}, {{organizerName}}, {{meetingDate}}, {{meetingTime}}, {{meetingDescription}}, {{confirmationLink}}, {{meetingLocation}}";
            case "meeting-reminder":
                return "Dostępne zmienne: {{userName}}, {{meetingTitle}}, {{meetingDate}}, {{meetingTime}}, {{meetingLocation}}, {{meetingLink}}";
            case "password-reset":
                return "Dostępne zmienne: {{userName}}, {{resetLink}}, {{expirationTime}}";
            default:
                return "Brak informacji o zmiennych";
        }
    }
}
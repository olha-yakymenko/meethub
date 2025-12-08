package com.meethub.domain.model.projection;

import com.meethub.domain.model.enums.ParticipationStatus;

import java.time.Duration;
import java.time.LocalDateTime;

// ParticipantProjection.java
public interface ParticipantProjection {
    Long getId();
    Long getUserId();
    String getFirstName();
    String getLastName();
    String getEmail();
    ParticipationStatus getStatus();
    LocalDateTime getInvitedAt();
    LocalDateTime getRespondedAt();
    LocalDateTime getAttendedAt();
    LocalDateTime getLeftAt();

    // Metody domyślne dla obliczonych pól
    default String getFullName() {
        return getFirstName() + " " + getLastName();
    }

    default Duration getResponseTime() {
        if (getInvitedAt() != null && getRespondedAt() != null) {
            return Duration.between(getInvitedAt(), getRespondedAt());
        }
        return null;
    }

    default Long getResponseTimeMinutes() {
        Duration duration = getResponseTime();
        return duration != null ? duration.toMinutes() : null;
    }

    default boolean isActive() {
        return getStatus() != null && getStatus().isActive();
    }
}
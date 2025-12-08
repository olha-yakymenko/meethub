package com.meethub.domain.model.projection;

import com.meethub.domain.model.enums.ParticipationStatus;

import java.time.LocalDateTime;
public interface ParticipantProjection {
    Long getId();          // p.id
    String getEmail();     // u.email
    String getStatus();    // p.status (ENUM → String)

    String getFirstName(); // u.firstName
    String getLastName();  // u.lastName

    default String getFullName() {
        return getFirstName() + " " + getLastName();
    }
}


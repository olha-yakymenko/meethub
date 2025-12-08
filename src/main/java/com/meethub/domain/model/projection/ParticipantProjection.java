package com.meethub.domain.model.projection;
//
//import com.meethub.domain.model.enums.ParticipationStatus;
//import java.time.Duration;
//import java.time.LocalDateTime;
//
//public interface ParticipantProjection {
//    Long getId();
//    Long getUserId();
//    String getEmail();
//    ParticipationStatus getStatus();
//    LocalDateTime getInvitedAt();
//    LocalDateTime getRespondedAt();
//    LocalDateTime getAttendedAt();
//    LocalDateTime getLeftAt();
//
//    // Zagnieżdżona projekcja dla User
//    UserInfo getUser();
//
//    default String getFullName() {
//        return getUser() != null ? getUser().getFirstName() + " " + getUser().getLastName() : "Nieznany";
//    }
//
//    default Duration getResponseTime() {
//        if (getInvitedAt() != null && getRespondedAt() != null) {
//            return Duration.between(getInvitedAt(), getRespondedAt());
//        }
//        return null;
//    }
//
//    default Long getResponseTimeMinutes() {
//        Duration duration = getResponseTime();
//        return duration != null ? duration.toMinutes() : null;
//    }
//
//    default boolean isActive() {
//        return getStatus() != null && getStatus().isActive();
//    }
//
//    interface UserInfo {
//        String getFirstName();
//        String getLastName();
//        Long getId();
//        Long getUserId();
//        String getEmail();
//        ParticipationStatus getStatus();
//        LocalDateTime getInvitedAt();
//        LocalDateTime getRespondedAt();
//        LocalDateTime getAttendedAt();
//        LocalDateTime getLeftAt();
//    }
//}


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


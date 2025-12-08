package com.meethub.domain.model.response;

import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.model.enums.PermissionLevel;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantResponse {
    private Long id;
    private UserResponse user;
    private MeetingResponse meeting;

    public Long getId() {
        return id;
    }

    public UserResponse getUser() {
        return user;
    }

    public MeetingResponse getMeeting() {
        return meeting;
    }

    public ParticipationStatus getStatus() {
        return status;
    }

    public PermissionLevel getPermissionLevel() {
        return permissionLevel;
    }

    public String getComment() {
        return comment;
    }

    public LocalDateTime getResponseDate() {
        return responseDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    private ParticipationStatus status;
    private PermissionLevel permissionLevel;
    private String comment;
    private LocalDateTime responseDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
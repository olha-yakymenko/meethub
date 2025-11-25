package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.PermissionLevel;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class InviteParticipantsRequest {

    @NotNull(message = "User IDs are required")
    private List<Long> userIds;

    private PermissionLevel permissionLevel = PermissionLevel.PARTICIPANT;

    private String message;
}
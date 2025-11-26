package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.model.enums.PermissionLevel;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateParticipantRequest {

    @NotNull(message = "Status is required")
    private ParticipationStatus status;

    @NotNull(message = "Permission level is required")
    private PermissionLevel permissionLevel;
    private String comment;
    private Boolean sendNotification = true;
}
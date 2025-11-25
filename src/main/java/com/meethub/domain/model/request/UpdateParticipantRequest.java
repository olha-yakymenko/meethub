package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.model.enums.PermissionLevel;
import lombok.Data;

@Data
public class UpdateParticipantRequest {

    private ParticipationStatus status;
    private PermissionLevel permissionLevel;
    private String comment;
    private Boolean sendNotification = true;
}
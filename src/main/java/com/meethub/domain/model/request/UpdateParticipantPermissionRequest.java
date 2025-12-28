package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.PermissionLevel;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateParticipantPermissionRequest {

    @NotNull(message = "Poziom uprawnień nie może być pusty")
    private PermissionLevel permissionLevel;
}

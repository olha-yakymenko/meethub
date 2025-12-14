//package com.meethub.domain.model.request;
//
//import com.meethub.domain.model.enums.PermissionLevel;
//import jakarta.validation.constraints.NotNull;
//import lombok.Data;
//
//import java.util.List;
//
//@Data
//public class InviteParticipantsRequest {
//
//    @NotNull(message = "User IDs are required")
//    private List<Long> userIds;
//
//    private PermissionLevel permissionLevel = PermissionLevel.PARTICIPANT;
//
//    private String message;
//}


package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.PermissionLevel;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Data
public class InviteParticipantsRequest {

    @NotNull(message = "User IDs are required")
    @NotEmpty(message = "At least one user must be selected")
    private List<Long> userIds;

    @NotNull(message = "Permission level is required")
    private PermissionLevel permissionLevel = PermissionLevel.PARTICIPANT;

    @Size(max = 500, message = "Message cannot exceed 500 characters")
    private String message;
}
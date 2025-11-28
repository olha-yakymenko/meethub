// MeetingParticipationInfo.java
package com.meethub.domain.model.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class MeetingParticipationInfo {
    private boolean isOrganizer;
    private boolean isParticipant;
    private boolean isRelated;
    private String participantRole;
    private List<String> permissions;
    private boolean canEdit;
    private boolean canDelete;
    private boolean canManageParticipants;
    private boolean canJoin;
    private boolean canViewDetails;
    private boolean canUpload;
    private boolean canDownload;

}
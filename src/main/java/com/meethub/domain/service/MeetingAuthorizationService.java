// MeetingAuthorizationService.java
package com.meethub.domain.service;

import com.meethub.domain.model.enums.ResourceAccessLevel;
import com.meethub.domain.model.response.MeetingParticipationInfo;
import jakarta.validation.constraints.NotNull;


public interface MeetingAuthorizationService {

    MeetingParticipationInfo getUserMeetingPermissions(
            @NotNull Long meetingId,
            @NotNull Long userId
    );

    boolean canUserEditMeeting(@NotNull Long meetingId, @NotNull Long userId);
    boolean canUserDeleteMeeting(@NotNull Long meetingId, @NotNull Long userId);
    boolean canUserManageParticipants(@NotNull Long meetingId, @NotNull Long userId);
    boolean canUserJoinMeeting(@NotNull Long meetingId, @NotNull Long userId);
    boolean canUserViewResource(@NotNull Long meetingId, @NotNull Long userId);
    boolean canUserDownloadResource(@NotNull Long meetingId, @NotNull Long userId);
    boolean canUserUploadResource(@NotNull Long meetingId, @NotNull Long userId);

    boolean canUserDeleteResource(
            @NotNull Long meetingId,
            @NotNull Long resourceId,
            @NotNull Long userId
    );

    ResourceAccessLevel getUserResourceAccessLevel(@NotNull Long meetingId, @NotNull Long userId);

    boolean hasResourceAccess(
            @NotNull Long meetingId,
            @NotNull Long userId,
            @NotNull ResourceAccessLevel requiredLevel
    );

    boolean canUserComment(@NotNull Long meetingId, @NotNull Long userId);
    boolean canUserViewParticipants(@NotNull Long meetingId, @NotNull Long userId);
}
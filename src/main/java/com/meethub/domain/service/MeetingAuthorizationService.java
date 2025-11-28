// MeetingAuthorizationService.java
package com.meethub.domain.service;

import com.meethub.domain.model.enums.ResourceAccessLevel;
import com.meethub.domain.model.response.MeetingParticipationInfo;
import org.springframework.transaction.annotation.Transactional;

public interface MeetingAuthorizationService {
    MeetingParticipationInfo getUserMeetingPermissions(Long meetingId, Long userId);
    boolean canUserEditMeeting(Long meetingId, Long userId);
    boolean canUserDeleteMeeting(Long meetingId, Long userId);
    boolean canUserManageParticipants(Long meetingId, Long userId);
    boolean canUserJoinMeeting(Long meetingId, Long userId);
    boolean canUserViewResource(Long meetingId, Long userId);
    boolean canUserDownloadResource(Long meetingId, Long userId);
    boolean canUserUploadResource(Long meetingId, Long userId);
    boolean canUserDeleteResource(Long meetingId, Long resourceId, Long userId);
    ResourceAccessLevel getUserResourceAccessLevel(Long meetingId, Long userId);

    @Transactional(readOnly = true)
    boolean hasResourceAccess(Long meetingId, Long userId, ResourceAccessLevel requiredLevel);

    @Transactional(readOnly = true)
    boolean canUserComment(Long meetingId, Long userId);

    boolean canUserViewParticipants(Long meetingId, Long userId);
}

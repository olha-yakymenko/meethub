// MeetingAuthorizationService.java
package com.meethub.domain.service;

import com.meethub.domain.model.enums.ResourceAccessLevel;
import com.meethub.domain.model.response.MeetingParticipationInfo;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Validated
public interface MeetingAuthorizationService {

    MeetingParticipationInfo getUserMeetingPermissions(
            @NotNull(message = "ID spotkania nie może być puste")
            @Positive(message = "ID spotkania musi być liczbą dodatnią")
            Long meetingId,

            @NotNull(message = "ID użytkownika nie może być puste")
            @Positive(message = "ID użytkownika musi być liczbą dodatnią")
            Long userId
    );


    @Transactional(readOnly = true)
    boolean canUserEditMeeting(
            @NotNull(message = "ID spotkania nie może być puste") @Positive Long meetingId,
            @NotNull(message = "ID użytkownika nie może być puste") @Positive Long userId
    );

    @Transactional(readOnly = true)
    boolean canUserDeleteMeeting(
            @NotNull(message = "ID spotkania nie może być puste") @Positive Long meetingId,
            @NotNull(message = "ID użytkownika nie może być puste") @Positive Long userId
    );

    @Transactional(readOnly = true)
    boolean canUserManageParticipants(
            @NotNull(message = "ID spotkania nie może być puste") @Positive Long meetingId,
            @NotNull(message = "ID użytkownika nie może być puste") @Positive Long userId
    );

    @Transactional(readOnly = true)
    boolean canUserJoinMeeting(
            @NotNull(message = "ID spotkania nie może być puste") @Positive Long meetingId,
            @NotNull(message = "ID użytkownika nie może być puste") @Positive Long userId
    );

    @Transactional(readOnly = true)
    boolean canUserViewResource(
            @NotNull(message = "ID spotkania nie może być puste") @Positive Long meetingId,
            @NotNull(message = "ID użytkownika nie może być puste") @Positive Long userId
    );

    @Transactional(readOnly = true)
    boolean canUserDownloadResource(
            @NotNull(message = "ID spotkania nie może być puste") @Positive Long meetingId,
            @NotNull(message = "ID użytkownika nie może być puste") @Positive Long userId
    );

    @Transactional(readOnly = true)
    boolean canUserUploadResource(
            @NotNull(message = "ID spotkania nie może być puste") @Positive Long meetingId,
            @NotNull(message = "ID użytkownika nie może być puste") @Positive Long userId
    );

    @Transactional(readOnly = true)
    boolean canUserDeleteResource(
            @NotNull(message = "ID spotkania nie może być puste") @Positive Long meetingId,
            @NotNull(message = "ID zasobu nie może być puste") @Positive Long resourceId,
            @NotNull(message = "ID użytkownika nie może być puste") @Positive Long userId
    );

    @Transactional(readOnly = true)
    ResourceAccessLevel getUserResourceAccessLevel(
            @NotNull(message = "ID spotkania nie może być puste") @Positive Long meetingId,
            @NotNull(message = "ID użytkownika nie może być puste") @Positive Long userId
    );

    @Transactional(readOnly = true)
    boolean hasResourceAccess(
            @NotNull(message = "ID spotkania nie może być puste") @Positive Long meetingId,
            @NotNull(message = "ID użytkownika nie może być puste") @Positive Long userId,
            @NotNull(message = "Poziom dostępu nie może być pusty") ResourceAccessLevel requiredLevel
    );

    @Transactional(readOnly = true)
    boolean canUserComment(
            @NotNull(message = "ID spotkania nie może być puste") @Positive Long meetingId,
            @NotNull(message = "ID użytkownika nie może być puste") @Positive Long userId
    );

    @Transactional(readOnly = true)
    boolean canUserViewParticipants(
            @NotNull(message = "ID spotkania nie może być puste") @Positive Long meetingId,
            @NotNull(message = "ID użytkownika nie może być puste") @Positive Long userId
    );
}



//// MeetingAuthorizationService.java
//package com.meethub.domain.service;
//
//import com.meethub.domain.model.enums.ResourceAccessLevel;
//import com.meethub.domain.model.response.MeetingParticipationInfo;
//import org.springframework.transaction.annotation.Transactional;
//
//public interface MeetingAuthorizationService {
//    MeetingParticipationInfo getUserMeetingPermissions(Long meetingId, Long userId);
//    boolean canUserEditMeeting(Long meetingId, Long userId);
//    boolean canUserDeleteMeeting(Long meetingId, Long userId);
//    boolean canUserManageParticipants(Long meetingId, Long userId);
//    boolean canUserJoinMeeting(Long meetingId, Long userId);
//    boolean canUserViewResource(Long meetingId, Long userId);
//    boolean canUserDownloadResource(Long meetingId, Long userId);
//    boolean canUserUploadResource(Long meetingId, Long userId);
//    boolean canUserDeleteResource(Long meetingId, Long resourceId, Long userId);
//    ResourceAccessLevel getUserResourceAccessLevel(Long meetingId, Long userId);
//
//    @Transactional(readOnly = true)
//    boolean hasResourceAccess(Long meetingId, Long userId, ResourceAccessLevel requiredLevel);
//
//    @Transactional(readOnly = true)
//    boolean canUserComment(Long meetingId, Long userId);
//
//    boolean canUserViewParticipants(Long meetingId, Long userId);
//}

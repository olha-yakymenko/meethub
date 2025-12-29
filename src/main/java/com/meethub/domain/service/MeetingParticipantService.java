// MeetingParticipantService.java
package com.meethub.domain.service;

import com.meethub.domain.model.dto.ParticipantCountDto;
import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.MeetingParticipant;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.model.enums.PermissionLevel;
import com.meethub.domain.model.projection.ParticipantProjection;
import com.meethub.domain.model.request.InviteParticipantsRequest;
import com.meethub.domain.model.request.UpdateParticipantRequest;
import com.meethub.domain.model.response.ParticipantResponse;
import com.meethub.domain.model.response.UserResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.core.io.ByteArrayResource;

import java.util.List;
import java.util.Map;


public interface MeetingParticipantService {

    List<ParticipantProjection> getMeetingParticipants(@NotNull Long meetingId);
    ParticipantCountDto getParticipantCounts(@NotNull Long meetingId);

    MeetingParticipant inviteParticipant(
            @NotNull Long meetingId,
            @NotNull Long userId,
            @NotNull Long organizerId
    );

    List<MeetingParticipant> inviteMultipleParticipants(
            @NotNull Long meetingId,
            @Valid InviteParticipantsRequest request,
            @NotNull Long organizerId
    );

    MeetingParticipant updateParticipantStatus(
            @NotNull Long meetingId,
            @NotNull Long participantId,
            @NotNull ParticipationStatus status,
            String comment,
            @NotNull Long userId
    );

    MeetingParticipant updateParticipantPermission(
            @NotNull Long meetingId,
            @NotNull Long participantId,
            @NotNull PermissionLevel permissionLevel,
            @NotNull Long organizerId
    );

    void removeParticipant(
            @NotNull Long meetingId,
            @NotNull Long participantId,
            @NotNull Long organizerId
    );

    ParticipantResponse updateParticipant(
            @NotNull Long participantId,
            @Valid UpdateParticipantRequest request
    );

    MeetingParticipant joinPublicMeeting(@NotNull Long meetingId, @NotNull Long userId);
    MeetingParticipant requestToJoinPrivateMeeting(@NotNull Long meetingId, @NotNull Long userId);

    void approveJoinRequest(@NotNull Long meetingId, @NotNull Long participantId, @NotNull Long organizerId);
    void rejectJoinRequest(@NotNull Long meetingId, @NotNull Long participantId, @NotNull Long organizerId);

    MeetingParticipant acceptInvitationByToken(String token);
    boolean isUserParticipant(@NotNull Long meetingId, @NotNull Long userId);
    boolean isViewer(@NotNull Long meetingId, @NotNull Long userId);
    boolean canUserEditMeeting(@NotNull Long meetingId, @NotNull Long userId);
    MeetingParticipant markAsAttended(@NotNull Long meetingId, @NotNull Long userId);
    void joinMeeting(@NotNull Long userId, @NotNull Long meetingId);
    void leaveMeeting(@NotNull Long userId, @NotNull Long meetingId);

    List<UserResponse> searchUsersForInvitation(String query, @NotNull Long meetingId);
    boolean hasAvailableSpots(@NotNull Long meetingId);
    List<ParticipantResponse> getPendingRequests(@NotNull Long meetingId);
    boolean isUserPendingApproval(@NotNull Long meetingId, @NotNull Long userId);
    PermissionLevel getParticipantPermissionLevel(@NotNull Long meetingId, @NotNull Long userId);
    List<ParticipantResponse> getUserInvitations(@NotNull Long userId);

    void respondToInvitation(
            @NotNull Long participantId,
            @NotNull ParticipationStatus response,
            String comment,
            @NotNull Long userId
    );

    List<ParticipantResponse> getConfirmedParticipants(@NotNull Long meetingId);
    Map<String, Long> getParticipantStatistics(@NotNull Long meetingId);
    boolean hasAccessToMeeting(@NotNull Long meetingId, @NotNull Long userId);
    boolean isOrganizer(@NotNull Long meetingId, @NotNull Long userId);
    boolean canEditParticipant(@NotNull Long meetingId, @NotNull Long participantId, @NotNull Long userId);
    boolean canRemoveParticipant(@NotNull Long meetingId, @NotNull Long participantId, @NotNull Long userId);

    List<ParticipantResponse> inviteParticipants(
            @NotNull Long meetingId,
            @Valid InviteParticipantsRequest request
    );

    ParticipantResponse getParticipant(@NotNull Long participantId);
    void removeParticipant(@NotNull Long participantId);
    ParticipantResponse confirmParticipation(String token, String comment);
    ParticipantResponse declineParticipation(String token, String comment);

    ParticipantStats getMeetingStats(@NotNull Long meetingId);
    Map<String, Object> getDetailedStats(@NotNull Long meetingId);
    ByteArrayResource exportParticipantsToCsv(@NotNull Long meetingId);
    void addOrganizerAsParticipant(@NotNull Meeting meeting, @NotNull User organizer);
    void confirmAttendance(@NotNull Long participantId, String inputToken);

    ParticipantResponse getParticipantInfo(@NotNull Long userId, @NotNull Long meetingId);
    boolean isParticipant(@NotNull Long meetingId, @NotNull Long userId);
    boolean isConfirmedParticipant(@NotNull Long meetingId, @NotNull Long userId);
    boolean isPendingParticipant(@NotNull Long meetingId, @NotNull Long userId);
    boolean isInvitedParticipant(@NotNull Long meetingId, @NotNull Long userId);
    boolean isDeclinedParticipant(@NotNull Long meetingId, @NotNull Long userId);
    boolean isWaitingListParticipant(@NotNull Long meetingId, @NotNull Long userId);
    boolean isUnrelatedUser(@NotNull Long meetingId, @NotNull Long userId);

    interface ParticipantStats {
        long getTotalInvited();
        long getTotalConfirmed();
    }
}
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;

@Validated
public interface MeetingParticipantService {

    List<ParticipantProjection> getMeetingParticipants(
            @NotNull(message = "ID spotkania nie może być puste") @Positive Long meetingId
    );

    ParticipantCountDto getParticipantCounts(
            @NotNull(message = "ID spotkania nie może być puste") @Positive Long meetingId
    );

    MeetingParticipant inviteParticipant(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long userId,
            @NotNull @Positive Long organizerId
    );

    List<MeetingParticipant> inviteMultipleParticipants(
            @NotNull @Positive Long meetingId,
            @Valid InviteParticipantsRequest request,
            @NotNull @Positive Long organizerId
    );

    MeetingParticipant updateParticipantStatus(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long participantId,
            @NotNull ParticipationStatus status,
            String comment,
            @NotNull @Positive Long userId
    );

    MeetingParticipant updateParticipantPermission(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long participantId,
            @NotNull PermissionLevel permissionLevel,
            @NotNull @Positive Long organizerId
    );

    void removeParticipant(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long participantId,
            @NotNull @Positive Long organizerId
    );

    ParticipantResponse updateParticipant(
            @NotNull @Positive Long participantId,
            @Valid UpdateParticipantRequest request
    );

    MeetingParticipant joinPublicMeeting(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long userId
    );

    MeetingParticipant requestToJoinPrivateMeeting(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long userId
    );

    void approveJoinRequest(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long participantId,
            @NotNull @Positive Long organizerId
    );

    void rejectJoinRequest(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long participantId,
            @NotNull @Positive Long organizerId
    );

    MeetingParticipant acceptInvitationByToken(
            @NotBlank(message = "Token nie może być pusty") String token
    );

    boolean isUserParticipant(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long userId
    );

    boolean isViewer(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long userId
    );

    boolean canUserEditMeeting(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long userId
    );

    MeetingParticipant markAsAttended(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long userId
    );

    void joinMeeting(
            @NotNull @Positive Long userId,
            @NotNull @Positive Long meetingId
    );

    void leaveMeeting(
            @NotNull @Positive Long userId,
            @NotNull @Positive Long meetingId
    );

    List<UserResponse> searchUsersForInvitation(
            @NotBlank String query,
            @NotNull @Positive Long meetingId
    );

    boolean hasAvailableSpots(@NotNull @Positive Long meetingId);

    List<ParticipantResponse> getPendingRequests(@NotNull @Positive Long meetingId);

    boolean isUserPendingApproval(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long userId
    );

    PermissionLevel getParticipantPermissionLevel(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long userId
    );

    List<ParticipantResponse> getUserInvitations(@NotNull @Positive Long userId);

    void respondToInvitation(
            @NotNull @Positive Long participantId,
            @NotNull ParticipationStatus response,
            String comment,
            @NotNull @Positive Long userId
    );

    List<ParticipantResponse> getConfirmedParticipants(@NotNull @Positive Long meetingId);

    Map<String, Long> getParticipantStatistics(@NotNull @Positive Long meetingId);

    boolean hasAccessToMeeting(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long userId
    );

    boolean isOrganizer(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long userId
    );

    boolean canEditParticipant(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long participantId,
            @NotNull @Positive Long userId
    );

    boolean canRemoveParticipant(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long participantId,
            @NotNull @Positive Long userId
    );

    List<ParticipantResponse> inviteParticipants(
            @NotNull @Positive Long meetingId,
            @Valid InviteParticipantsRequest request
    );

    ParticipantResponse getParticipant(@NotNull @Positive Long participantId);

    void removeParticipant(@NotNull @Positive Long participantId);

    ParticipantResponse confirmParticipation(
            @NotBlank String token,
            String comment
    );

    ParticipantResponse declineParticipation(
            @NotBlank String token,
            String comment
    );

    ParticipantStats getMeetingStats(@NotNull @Positive Long meetingId);

    Map<String, Object> getDetailedStats(@NotNull @Positive Long meetingId);

    ByteArrayResource exportParticipantsToCsv(@NotNull @Positive Long meetingId);

    void addOrganizerAsParticipant(@NotNull Meeting meeting, @NotNull User organizer);

    @Transactional
    void confirmAttendance(
            @NotNull @Positive Long participantId,
            @NotBlank String inputToken
    );

    interface ParticipantStats {
        long getTotalInvited();
        long getTotalConfirmed();
    }

    ParticipantResponse getParticipantInfo(
            @NotNull @Positive Long userId,
            @NotNull @Positive Long meetingId
    );

    boolean isParticipant(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long userId
    );

    boolean isConfirmedParticipant(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long userId
    );

    boolean isPendingParticipant(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long userId
    );

    boolean isInvitedParticipant(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long userId
    );

    boolean isDeclinedParticipant(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long userId
    );

    boolean isWaitingListParticipant(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long userId
    );

    boolean isUnrelatedUser(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long userId
    );
}




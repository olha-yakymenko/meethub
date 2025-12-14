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
        long getWaitlistCount();
        long getPendingCount();
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






//
//
//package com.meethub.domain.service;
//
//import com.meethub.domain.model.dto.ParticipantCountDto;
//import com.meethub.domain.model.entity.Meeting;
//import com.meethub.domain.model.entity.MeetingParticipant;
//import com.meethub.domain.model.entity.User;
//import com.meethub.domain.model.enums.ParticipationStatus;
//import com.meethub.domain.model.enums.PermissionLevel;
//import com.meethub.domain.model.projection.ParticipantProjection;
//import com.meethub.domain.model.request.InviteParticipantsRequest;
//import com.meethub.domain.model.request.UpdateParticipantRequest;
//import com.meethub.domain.model.response.ParticipantResponse;
//import com.meethub.domain.model.response.UserResponse;
//import org.springframework.core.io.ByteArrayResource;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//import java.util.Map;
//
//public interface MeetingParticipantService {
//    List<ParticipantProjection> getMeetingParticipants(Long meetingId);
//
//    ParticipantCountDto getParticipantCounts(Long meetingId);
//
//
//    MeetingParticipant inviteParticipant(Long meetingId, Long userId, Long organizerId);
//    List<MeetingParticipant> inviteMultipleParticipants(Long meetingId, InviteParticipantsRequest request, Long organizerId);
//    MeetingParticipant updateParticipantStatus(Long meetingId, Long participantId, ParticipationStatus status, String comment, Long userId);
//    MeetingParticipant updateParticipantPermission(Long meetingId, Long participantId, PermissionLevel permissionLevel, Long organizerId);
//    void removeParticipant(Long meetingId, Long participantId, Long organizerId);
//
//    ParticipantResponse updateParticipant(Long participantId, UpdateParticipantRequest request);
//
//
//    // Nowe metody dla różnych typów spotkań
//    MeetingParticipant joinPublicMeeting(Long meetingId, Long userId);
//    MeetingParticipant requestToJoinPrivateMeeting(Long meetingId, Long userId);
//    void approveJoinRequest(Long meetingId, Long participantId, Long organizerId);
//    void rejectJoinRequest(Long meetingId, Long participantId, Long organizerId);
//
//    MeetingParticipant acceptInvitationByToken(String token);
//    boolean isUserParticipant(Long meetingId, Long userId);
//
//    boolean isViewer(Long meetingId, Long userId);
//
//    boolean canUserEditMeeting(Long meetingId, Long userId);
//
//    MeetingParticipant markAsAttended(Long meetingId, Long userId);
//
//    // Zachowaj istniejące metody dla kompatybilności
//    void joinMeeting(Long userId, Long meetingId);
//    void leaveMeeting(Long userId, Long meetingId);
//
//    List<UserResponse> searchUsersForInvitation(String query, Long meetingId);
//
//    // Metody pomocnicze
//    boolean hasAvailableSpots(Long meetingId);
//    List<ParticipantResponse> getPendingRequests(Long meetingId);
//    boolean isUserPendingApproval(Long meetingId, Long userId);
//
//    PermissionLevel getParticipantPermissionLevel(Long meetingId, Long userId);
//
//    List<ParticipantResponse> getUserInvitations(Long userId);
//
//    void respondToInvitation(Long participantId, ParticipationStatus response, String comment, Long userId);
//    List<ParticipantResponse> getConfirmedParticipants(Long meetingId);
//    Map<String, Long> getParticipantStatistics(Long meetingId);
//
//
//    boolean hasAccessToMeeting(Long meetingId, Long userId);
//    boolean isOrganizer(Long meetingId, Long userId);
//    boolean canEditParticipant(Long meetingId, Long participantId, Long userId);
//    boolean canRemoveParticipant(Long meetingId, Long participantId, Long userId);
//
//    // Metody dla web controller
//    List<ParticipantResponse> inviteParticipants(Long meetingId, InviteParticipantsRequest request);
//    ParticipantResponse getParticipant(Long participantId);
//    void removeParticipant(Long participantId);
//
//    // Metody dla tokenów (web)
//    ParticipantResponse confirmParticipation(String token, String comment);
//    ParticipantResponse declineParticipation(String token, String comment);
//
//    // Statystyki dla web
//    ParticipantStats getMeetingStats(Long meetingId);
//    Map<String, Object> getDetailedStats(Long meetingId);
//    ByteArrayResource exportParticipantsToCsv(Long meetingId);
//
//    void addOrganizerAsParticipant(Meeting meeting, User organizer);
//
//    @Transactional
//    void confirmAttendance(Long participantId, String inputToken);
//
//    // Klasa dla statystyk
//    interface ParticipantStats {
//        long getTotalInvited();
//        long getTotalConfirmed();
//        long getWaitlistCount();
//        long getPendingCount();
//    }
//
//    ParticipantResponse getParticipantInfo(Long userId, Long meetingId);
//
//
//    boolean isParticipant(Long meetingId, Long userId);
//
//
//
//    boolean isConfirmedParticipant(Long meetingId, Long userId);
//    boolean isPendingParticipant(Long meetingId, Long userId);
//    boolean isInvitedParticipant(Long meetingId, Long userId);
//    boolean isDeclinedParticipant(Long meetingId, Long userId);
//    boolean isWaitingListParticipant(Long meetingId, Long userId);
//    boolean isUnrelatedUser(Long meetingId, Long userId);
//}
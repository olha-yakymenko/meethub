//package com.meethub.domain.service;
//
//import com.meethub.domain.model.entity.MeetingParticipant;
//import com.meethub.domain.model.enums.ParticipationStatus;
//import com.meethub.domain.model.enums.PermissionLevel;
//import com.meethub.domain.model.request.InviteParticipantsRequest;
//import com.meethub.domain.model.response.ParticipantResponse;
//import com.meethub.domain.model.response.UserResponse;
//
//import java.util.List;
//
//
//public interface MeetingParticipantService {
//    List<ParticipantResponse> getMeetingParticipants(Long meetingId);
//    MeetingParticipant inviteParticipant(Long meetingId, Long userId, Long organizerId);
//    List<MeetingParticipant> inviteMultipleParticipants(Long meetingId, InviteParticipantsRequest request, Long organizerId);
//    MeetingParticipant updateParticipantStatus(Long meetingId, Long participantId, ParticipationStatus status, String comment, Long userId);
//    MeetingParticipant updateParticipantPermission(Long meetingId, Long participantId, PermissionLevel permissionLevel, Long organizerId);
//    void removeParticipant(Long meetingId, Long participantId, Long organizerId);
//    MeetingParticipant joinPublicMeeting(Long meetingId, Long userId);
//    MeetingParticipant acceptInvitationByToken(String token);
//    boolean isUserParticipant(Long meetingId, Long userId);
//    boolean canUserEditMeeting(Long meetingId, Long userId);
//
//    void joinMeeting(Long userId, Long meetingId);
//
//    void leaveMeeting(Long userId, Long meetingId);
//    List<UserResponse> searchUsersForInvitation(String query, Long meetingId);
//}





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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

public interface MeetingParticipantService {
    List<ParticipantProjection> getMeetingParticipants(Long meetingId);

    ParticipantCountDto getParticipantCounts(Long meetingId);


    MeetingParticipant inviteParticipant(Long meetingId, Long userId, Long organizerId);
    List<MeetingParticipant> inviteMultipleParticipants(Long meetingId, InviteParticipantsRequest request, Long organizerId);
    MeetingParticipant updateParticipantStatus(Long meetingId, Long participantId, ParticipationStatus status, String comment, Long userId);
    MeetingParticipant updateParticipantPermission(Long meetingId, Long participantId, PermissionLevel permissionLevel, Long organizerId);
    void removeParticipant(Long meetingId, Long participantId, Long organizerId);

    ParticipantResponse updateParticipant(Long participantId, UpdateParticipantRequest request);


    // Nowe metody dla różnych typów spotkań
    MeetingParticipant joinPublicMeeting(Long meetingId, Long userId);
    MeetingParticipant requestToJoinPrivateMeeting(Long meetingId, Long userId);
    void approveJoinRequest(Long meetingId, Long participantId, Long organizerId);
    void rejectJoinRequest(Long meetingId, Long participantId, Long organizerId);

    MeetingParticipant acceptInvitationByToken(String token);
    boolean isUserParticipant(Long meetingId, Long userId);

    boolean isViewer(Long meetingId, Long userId);

    boolean canUserEditMeeting(Long meetingId, Long userId);

    MeetingParticipant markAsAttended(Long meetingId, Long userId);

    // Zachowaj istniejące metody dla kompatybilności
    void joinMeeting(Long userId, Long meetingId);
    void leaveMeeting(Long userId, Long meetingId);

    List<UserResponse> searchUsersForInvitation(String query, Long meetingId);

    // Metody pomocnicze
    boolean hasAvailableSpots(Long meetingId);
    List<ParticipantResponse> getPendingRequests(Long meetingId);
    boolean isUserPendingApproval(Long meetingId, Long userId);

    PermissionLevel getParticipantPermissionLevel(Long meetingId, Long userId);

    List<ParticipantResponse> getUserInvitations(Long userId);

    void respondToInvitation(Long participantId, ParticipationStatus response, String comment, Long userId);
    List<ParticipantResponse> getConfirmedParticipants(Long meetingId);
    Map<String, Long> getParticipantStatistics(Long meetingId);


    boolean hasAccessToMeeting(Long meetingId, Long userId);
    boolean isOrganizer(Long meetingId, Long userId);
    boolean canEditParticipant(Long meetingId, Long participantId, Long userId);
    boolean canRemoveParticipant(Long meetingId, Long participantId, Long userId);

    // Metody dla web controller
    List<ParticipantResponse> inviteParticipants(Long meetingId, InviteParticipantsRequest request);
    ParticipantResponse getParticipant(Long participantId);
    void removeParticipant(Long participantId);

    // Metody dla tokenów (web)
    ParticipantResponse confirmParticipation(String token, String comment);
    ParticipantResponse declineParticipation(String token, String comment);

    // Statystyki dla web
    ParticipantStats getMeetingStats(Long meetingId);
    Map<String, Object> getDetailedStats(Long meetingId);
    ByteArrayResource exportParticipantsToCsv(Long meetingId);

    void addOrganizerAsParticipant(Meeting meeting, User organizer);

    @Transactional
    void confirmAttendance(Long participantId, String inputToken);

    // Klasa dla statystyk
    interface ParticipantStats {
        long getTotalInvited();
        long getTotalConfirmed();
        long getWaitlistCount();
        long getPendingCount();
    }

    ParticipantResponse getParticipantInfo(Long userId, Long meetingId);


    boolean isParticipant(Long meetingId, Long userId);



    boolean isConfirmedParticipant(Long meetingId, Long userId);
    boolean isPendingParticipant(Long meetingId, Long userId);
    boolean isInvitedParticipant(Long meetingId, Long userId);
    boolean isDeclinedParticipant(Long meetingId, Long userId);
    boolean isWaitingListParticipant(Long meetingId, Long userId);
    boolean isUnrelatedUser(Long meetingId, Long userId);
}
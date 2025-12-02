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

import com.meethub.domain.model.entity.MeetingParticipant;
import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.model.enums.PermissionLevel;
import com.meethub.domain.model.request.InviteParticipantsRequest;
import com.meethub.domain.model.request.UpdateParticipantRequest;
import com.meethub.domain.model.response.ParticipantResponse;
import com.meethub.domain.model.response.UserResponse;
import org.springframework.core.io.ByteArrayResource;

import java.util.List;
import java.util.Map;

public interface MeetingParticipantService {
    List<ParticipantResponse> getMeetingParticipants(Long meetingId);
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
    boolean canUserEditMeeting(Long meetingId, Long userId);

    // Zachowaj istniejące metody dla kompatybilności
    void joinMeeting(Long userId, Long meetingId);
    void leaveMeeting(Long userId, Long meetingId);

    List<UserResponse> searchUsersForInvitation(String query, Long meetingId);

    // Metody pomocnicze
    boolean hasAvailableSpots(Long meetingId);
    boolean canUserJoinMeeting(Long meetingId, Long userId);
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
    ParticipantResponse setTentative(String token, String comment);

    // Statystyki dla web
    ParticipantStats getMeetingStats(Long meetingId);
    Map<String, Object> getDetailedStats(Long meetingId);
    ByteArrayResource exportParticipantsToCsv(Long meetingId);

    // Klasa dla statystyk
    interface ParticipantStats {
        long getTotalInvited();
        long getTotalConfirmed();
        long getWaitlistCount();
        long getPendingCount();
    }
}
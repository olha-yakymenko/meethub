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
import com.meethub.domain.model.response.ParticipantResponse;
import com.meethub.domain.model.response.UserResponse;

import java.util.List;

public interface MeetingParticipantService {
    List<ParticipantResponse> getMeetingParticipants(Long meetingId);
    MeetingParticipant inviteParticipant(Long meetingId, Long userId, Long organizerId);
    List<MeetingParticipant> inviteMultipleParticipants(Long meetingId, InviteParticipantsRequest request, Long organizerId);
    MeetingParticipant updateParticipantStatus(Long meetingId, Long participantId, ParticipationStatus status, String comment, Long userId);
    MeetingParticipant updateParticipantPermission(Long meetingId, Long participantId, PermissionLevel permissionLevel, Long organizerId);
    void removeParticipant(Long meetingId, Long participantId, Long organizerId);

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

    // W MeetingParticipantService dodaj:
}
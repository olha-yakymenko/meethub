////package com.meethub.domain.service;
////
////import com.meethub.domain.model.entity.MeetingParticipant;
////import com.meethub.domain.model.enums.ParticipationStatus;
////import com.meethub.domain.model.request.InviteParticipantsRequest;
////import com.meethub.domain.model.request.UpdateParticipantRequest;
////import com.meethub.domain.model.response.ParticipantResponse;
////import org.springframework.transaction.annotation.Transactional;
////
////import java.util.List;
////
////public interface ParticipantService {
////
////    // Existing methods
////    List<ParticipantResponse> inviteParticipants(Long meetingId, InviteParticipantsRequest request);
////    ParticipantResponse updateParticipant(Long participantId, UpdateParticipantRequest request);
////    void removeParticipant(Long participantId);
////    ParticipantResponse getParticipant(Long participantId);
////    List<ParticipantResponse> getMeetingParticipants(Long meetingId);
////    List<ParticipantResponse> getWaitlist(Long meetingId);
////    String generateInvitationToken(Long participantId);
////    boolean validateInvitationToken(String token);
////    ParticipantStats getMeetingStats(Long meetingId);
////    ParticipantResponse confirmParticipation(String token, String comment);
////    ParticipantResponse declineParticipation(String token, String comment);
////    ParticipantResponse setTentative(String token, String comment);
////    ParticipantResponse addToWaitlist(Long meetingId, Long userId);
////
////    // New security methods
////    boolean hasAccessToMeeting(Long meetingId, Long userId);
////    boolean isOrganizer(Long meetingId, Long userId);
////    boolean canEditParticipant(Long meetingId, Long participantId, Long userId);
////    boolean canRemoveParticipant(Long meetingId, Long participantId, Long userId);
////
////    // Additional utility methods
////    boolean isUserParticipant(Long userId, Long meetingId);
////    ParticipationStatus getUserParticipationStatus(Long userId, Long meetingId);
////
////    @Transactional
////    ParticipantResponse inviteParticipant(Long meetingId, Long userId, Long organizerId);
////
////    @Transactional
////    void removeFromMeeting(Long meetingId, Long userId, Long removerId);
////
////    @Transactional
////    ParticipantResponse joinMeeting(Long userId, Long meetingId);
////
////    @Transactional
////    void leaveMeeting(Long userId, Long meetingId);
////
////    interface ParticipantStats {
////        int getTotalInvited();
////        int getTotalConfirmed();
////        int getTotalDeclined();
////        int getWaitlistCount();
////        int getAvailableSpots();
////    }
////
////
////    MeetingParticipant joinPublicMeeting(Long meetingId, Long userId);
////    MeetingParticipant requestToJoinPrivateMeeting(Long meetingId, Long userId);
////    void approveJoinRequest(Long meetingId, Long participantId, Long organizerId);
////    void rejectJoinRequest(Long meetingId, Long participantId, Long organizerId);
////}
//
//
//
//package com.meethub.domain.service;
//
//import com.meethub.domain.model.entity.MeetingParticipant;
//import com.meethub.domain.model.enums.ParticipationStatus;
//import com.meethub.domain.model.enums.PermissionLevel;
//import com.meethub.domain.model.request.InviteParticipantsRequest;
//import com.meethub.domain.model.request.UpdateParticipantRequest;
//import com.meethub.domain.model.response.ParticipantResponse;
//import com.meethub.domain.model.response.UserResponse;
//
//import java.util.List;
//
//public interface ParticipantService {
//
//    // METODY DLA UCZESTNIKÓW
//    List<ParticipantResponse> getMeetingParticipants(Long meetingId);
//    MeetingParticipant inviteParticipant(Long meetingId, Long userId, Long organizerId);
//    List<MeetingParticipant> inviteMultipleParticipants(Long meetingId, InviteParticipantsRequest request, Long organizerId);
//    MeetingParticipant updateParticipantStatus(Long meetingId, Long participantId, ParticipationStatus status, String comment, Long userId);
//    MeetingParticipant updateParticipantPermission(Long meetingId, Long participantId, PermissionLevel permissionLevel, Long organizerId);
//    void removeParticipant(Long meetingId, Long participantId, Long organizerId);
//
//    // NOWE METODY DLA RÓŻNYCH TYPÓW SPOTKAŃ
//    MeetingParticipant joinPublicMeeting(Long meetingId, Long userId);
//    MeetingParticipant requestToJoinPrivateMeeting(Long meetingId, Long userId);
//    void approveJoinRequest(Long meetingId, Long participantId, Long organizerId);
//    void rejectJoinRequest(Long meetingId, Long participantId, Long organizerId);
//
//    // METODY DLA WEB CONTROLLERÓW
//    boolean hasAccessToMeeting(Long meetingId, Long userId);
//    boolean isOrganizer(Long meetingId, Long userId);
//    ParticipantStats getMeetingStats(Long meetingId);
//    boolean canEditParticipant(Long meetingId, Long participantId, Long userId);
//    boolean canRemoveParticipant(Long meetingId, Long participantId, Long userId);
//    ParticipantResponse getParticipant(Long participantId);
//    List<ParticipantResponse> inviteParticipants(Long meetingId, InviteParticipantsRequest request);
//    ParticipantResponse updateParticipant(Long participantId, UpdateParticipantRequest request);
//    void removeParticipant(Long participantId);
//
//    // METODY DLA TOKENÓW
//    MeetingParticipant acceptInvitationByToken(String token);
//    ParticipantResponse confirmParticipation(String token, String comment);
//    ParticipantResponse declineParticipation(String token, String comment);
//    ParticipantResponse setTentative(String token, String comment);
//
//    // METODY POMOCNICZE
//    boolean isUserParticipant(Long meetingId, Long userId);
//    boolean isUserParticipantOfMeeting(Long meetingId, Long userId);
//    boolean canUserEditMeeting(Long meetingId, Long userId);
//    void joinMeeting(Long userId, Long meetingId);
//    void leaveMeeting(Long userId, Long meetingId);
//    List<UserResponse> searchUsersForInvitation(String query, Long meetingId);
//
//    // METODY DLA SPRAWDZANIA DOSTĘPNOŚCI
//    boolean hasAvailableSpots(Long meetingId);
//    boolean canUserJoinMeeting(Long meetingId, Long userId);
//    List<ParticipantResponse> getPendingRequests(Long meetingId);
//    boolean isUserPendingApproval(Long meetingId, Long userId);
//
//    // KLASA DLA STATYSTYK
//    class ParticipantStats {
//        private final long totalInvited;
//        private final long totalConfirmed;
//        private final long waitlistCount;
//        private final long pendingCount;
//
//        public ParticipantStats(long totalInvited, long totalConfirmed, long waitlistCount, long pendingCount) {
//            this.totalInvited = totalInvited;
//            this.totalConfirmed = totalConfirmed;
//            this.waitlistCount = waitlistCount;
//            this.pendingCount = pendingCount;
//        }
//
//        // gettery
//        public long getTotalInvited() { return totalInvited; }
//        public long getTotalConfirmed() { return totalConfirmed; }
//        public long getWaitlistCount() { return waitlistCount; }
//        public long getPendingCount() { return pendingCount; }
//    }
//}










package com.meethub.domain.service;

import com.meethub.domain.model.entity.MeetingParticipant;
import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.model.enums.PermissionLevel;
import com.meethub.domain.model.request.InviteParticipantsRequest;
import com.meethub.domain.model.request.UpdateParticipantRequest;
import com.meethub.domain.model.response.ParticipantResponse;
import com.meethub.domain.model.response.UserResponse;

import java.util.List;

public interface ParticipantService {

    // METODY DLA UCZESTNIKÓW
    List<ParticipantResponse> getMeetingParticipants(Long meetingId);
    MeetingParticipant inviteParticipant(Long meetingId, Long userId, Long organizerId);
    List<MeetingParticipant> inviteMultipleParticipants(Long meetingId, InviteParticipantsRequest request, Long organizerId);
    MeetingParticipant updateParticipantStatus(Long meetingId, Long participantId, ParticipationStatus status, String comment, Long userId);
    MeetingParticipant updateParticipantPermission(Long meetingId, Long participantId, PermissionLevel permissionLevel, Long organizerId);
    void removeParticipant(Long meetingId, Long participantId, Long organizerId);

    // NOWE METODY DLA RÓŻNYCH TYPÓW SPOTKAŃ
    MeetingParticipant joinPublicMeeting(Long meetingId, Long userId);
    MeetingParticipant requestToJoinPrivateMeeting(Long meetingId, Long userId);
    void approveJoinRequest(Long meetingId, Long participantId, Long organizerId);
    void rejectJoinRequest(Long meetingId, Long participantId, Long organizerId);

    // METODY DLA WEB CONTROLLERÓW
    boolean hasAccessToMeeting(Long meetingId, Long userId);
    boolean isOrganizer(Long meetingId, Long userId);
    ParticipantStats getMeetingStats(Long meetingId);
    boolean canEditParticipant(Long meetingId, Long participantId, Long userId);
    boolean canRemoveParticipant(Long meetingId, Long participantId, Long userId);
    ParticipantResponse getParticipant(Long participantId);
    List<ParticipantResponse> inviteParticipants(Long meetingId, InviteParticipantsRequest request);
    ParticipantResponse updateParticipant(Long participantId, UpdateParticipantRequest request);
    void removeParticipant(Long participantId);

    // METODY DLA TOKENÓW
    MeetingParticipant acceptInvitationByToken(String token);
    ParticipantResponse confirmParticipation(String token, String comment);
    ParticipantResponse declineParticipation(String token, String comment);
    ParticipantResponse setTentative(String token, String comment);

    // METODY POMOCNICZE
    boolean isUserParticipant(Long meetingId, Long userId);
    boolean isUserParticipantOfMeeting(Long meetingId, Long userId);
    boolean canUserEditMeeting(Long meetingId, Long userId);
    void joinMeeting(Long userId, Long meetingId);
    void leaveMeeting(Long userId, Long meetingId);
    List<UserResponse> searchUsersForInvitation(String query, Long meetingId);

    // METODY DLA SPRAWDZANIA DOSTĘPNOŚCI
    boolean hasAvailableSpots(Long meetingId);
    boolean canUserJoinMeeting(Long meetingId, Long userId);
    List<ParticipantResponse> getPendingRequests(Long meetingId);
    boolean isUserPendingApproval(Long meetingId, Long userId);

    // KLASA DLA STATYSTYK
    interface ParticipantStats {
        long getTotalInvited();
        long getTotalConfirmed();
        long getWaitlistCount();
        long getPendingCount();
    }
}
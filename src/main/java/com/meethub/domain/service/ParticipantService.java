package com.meethub.domain.service;

import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.model.request.InviteParticipantsRequest;
import com.meethub.domain.model.request.UpdateParticipantRequest;
import com.meethub.domain.model.response.ParticipantResponse;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ParticipantService {

    // Existing methods
    List<ParticipantResponse> inviteParticipants(Long meetingId, InviteParticipantsRequest request);
    ParticipantResponse updateParticipant(Long participantId, UpdateParticipantRequest request);
    void removeParticipant(Long participantId);
    ParticipantResponse getParticipant(Long participantId);
    List<ParticipantResponse> getMeetingParticipants(Long meetingId);
    List<ParticipantResponse> getWaitlist(Long meetingId);
    String generateInvitationToken(Long participantId);
    boolean validateInvitationToken(String token);
    ParticipantStats getMeetingStats(Long meetingId);
    ParticipantResponse confirmParticipation(String token, String comment);
    ParticipantResponse declineParticipation(String token, String comment);
    ParticipantResponse setTentative(String token, String comment);
    ParticipantResponse addToWaitlist(Long meetingId, Long userId);

    // New security methods
    boolean hasAccessToMeeting(Long meetingId, Long userId);
    boolean isOrganizer(Long meetingId, Long userId);
    boolean canEditParticipant(Long meetingId, Long participantId, Long userId);
    boolean canRemoveParticipant(Long meetingId, Long participantId, Long userId);

    // Additional utility methods
    boolean isUserParticipant(Long userId, Long meetingId);
    ParticipationStatus getUserParticipationStatus(Long userId, Long meetingId);

    @Transactional
    ParticipantResponse inviteParticipant(Long meetingId, Long userId, Long organizerId);

    @Transactional
    void removeFromMeeting(Long meetingId, Long userId, Long removerId);

    @Transactional
    ParticipantResponse joinMeeting(Long userId, Long meetingId);

    @Transactional
    void leaveMeeting(Long userId, Long meetingId);

    interface ParticipantStats {
        int getTotalInvited();
        int getTotalConfirmed();
        int getTotalDeclined();
        int getWaitlistCount();
        int getAvailableSpots();
    }
}
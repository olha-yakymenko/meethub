package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.*;
import com.meethub.domain.model.enums.MeetingVisibility;
import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.model.enums.PermissionLevel;
import com.meethub.domain.model.request.InviteParticipantsRequest;
import com.meethub.domain.model.response.ParticipantResponse;
import com.meethub.domain.model.response.UserResponse;
import com.meethub.domain.repository.jpa.*;
import com.meethub.domain.service.EmailService;
import com.meethub.domain.service.MeetingParticipantService;
import com.meethub.domain.service.NotificationService;
import com.meethub.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MeetingParticipantServiceImpl implements MeetingParticipantService {

    private final MeetingParticipantRepository participantRepository;
    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;
    private final ParticipantStatusHistoryRepository statusHistoryRepository;
    private final WaitlistEntryRepository waitlistEntryRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    @Override
    public List<ParticipantResponse> getMeetingParticipants(Long meetingId) {
        List<MeetingParticipant> participants = participantRepository.findByMeetingId(meetingId);
        return participants.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public MeetingParticipant inviteParticipant(Long meetingId, Long userId, Long organizerId) {
        Meeting meeting = meetingRepository.findByIdAndOrganizerId(meetingId, organizerId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found or access denied"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Sprawdź czy już nie jest uczestnikiem
        if (participantRepository.findByMeetingIdAndUserId(meetingId, userId).isPresent()) {
            throw new IllegalArgumentException("User is already a participant");
        }

        // Sprawdź limit uczestników
        if (isMeetingFull(meetingId)) {
            return addToWaitlist(meeting, user);
        }

        MeetingParticipant participant = MeetingParticipant.builder()
                .meeting(meeting)
                .user(user)
                .status(ParticipationStatus.INVITED)
                .permissionLevel(PermissionLevel.PARTICIPANT)
                .invitationToken(generateInvitationToken())
                .tokenExpiresAt(LocalDateTime.now().plusDays(7))
                .build();

        MeetingParticipant savedParticipant = participantRepository.save(participant);

        // Wyślij zaproszenie
        sendInvitationEmail(savedParticipant);

        return savedParticipant;
    }

    @Override
    public List<MeetingParticipant> inviteMultipleParticipants(Long meetingId, InviteParticipantsRequest request, Long organizerId) {
        return request.getUserIds().stream()
                .map(userId -> inviteParticipant(meetingId, userId, organizerId))
                .toList();
    }

    @Override
    public MeetingParticipant updateParticipantStatus(Long meetingId, Long participantId,
                                                      ParticipationStatus status, String comment, Long userId) {

        MeetingParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));

        ParticipationStatus oldStatus = participant.getStatus();

        // Sprawdź uprawnienia
        if (!hasPermissionToUpdateStatus(participant, userId)) {
            throw new SecurityException("No permission to update participant status");
        }

        participant.setStatus(status);
        participant.setComment(comment);
        participant.setResponseDate(LocalDateTime.now());

        MeetingParticipant updatedParticipant = participantRepository.save(participant);

        // Zapisz historię
        saveStatusHistory(participant, oldStatus, status, comment, userId);

        // Jeśli potwierdzono i był na liście oczekujących
        if (status == ParticipationStatus.CONFIRMED && isOnWaitlist(meetingId, participant.getUser().getId())) {
            removeFromWaitlist(meetingId, participant.getUser().getId());
        }

        return updatedParticipant;
    }

    @Override
    public MeetingParticipant updateParticipantPermission(Long meetingId, Long participantId, PermissionLevel permissionLevel, Long organizerId) {
        // Tylko organizator może zmieniać uprawnienia
        meetingRepository.findByIdAndOrganizerId(meetingId, organizerId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found or access denied"));

        MeetingParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));

        participant.setPermissionLevel(permissionLevel);
        return participantRepository.save(participant);
    }

    @Override
    public void removeParticipant(Long meetingId, Long participantId, Long organizerId) {
        // Tylko organizator może usuwać uczestników
        meetingRepository.findByIdAndOrganizerId(meetingId, organizerId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found or access denied"));

        participantRepository.deleteById(participantId);
    }

    // NOWE METODY DLA RÓŻNYCH TYPÓW SPOTKAŃ

    @Override
    public MeetingParticipant joinPublicMeeting(Long meetingId, Long userId) {
        log.info("Attempting to join public meeting {} by user {}", meetingId, userId);

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        // Sprawdź czy spotkanie jest publiczne
        if (meeting.getVisibility() != MeetingVisibility.PUBLIC) {
            throw new SecurityException("Meeting is not public");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Sprawdź czy już nie jest uczestnikiem
        Optional<MeetingParticipant> existingParticipant = participantRepository.findByMeetingIdAndUserId(meetingId, userId);
        if (existingParticipant.isPresent()) {
            MeetingParticipant participant = existingParticipant.get();
            if (participant.getStatus() == ParticipationStatus.CONFIRMED) {
                throw new IllegalArgumentException("User is already a confirmed participant");
            }
            // Jeśli jest zaproszony lub oczekujący, zmień status na potwierdzony
            participant.setStatus(ParticipationStatus.CONFIRMED);
            return participantRepository.save(participant);
        }

        // Sprawdź czy są jeszcze miejsca
        if (!hasAvailableSpots(meetingId)) {
            throw new IllegalArgumentException("No available spots in this meeting");
        }

        // Dodaj użytkownika jako potwierdzonego uczestnika
        MeetingParticipant participant = MeetingParticipant.builder()
                .meeting(meeting)
                .user(user)
                .status(ParticipationStatus.CONFIRMED)
                .permissionLevel(PermissionLevel.PARTICIPANT)
                .build();

        MeetingParticipant savedParticipant = participantRepository.save(participant);

        log.info("User {} successfully joined public meeting {}", userId, meetingId);

        // Wyślij powiadomienie do organizatora
        notificationService.sendParticipantJoinedNotification(meeting.getOrganizer(), user, meeting);

        return savedParticipant;
    }

    @Override
    public MeetingParticipant requestToJoinPrivateMeeting(Long meetingId, Long userId) {
        log.info("User {} requesting to join private meeting {}", userId, meetingId);

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        // Sprawdź czy spotkanie jest prywatne
        if (meeting.getVisibility() != MeetingVisibility.PRIVATE) {
            throw new SecurityException("Meeting is not private");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Sprawdź czy użytkownik już nie wysłał prośby
        Optional<MeetingParticipant> existingParticipant = participantRepository.findByMeetingIdAndUserId(meetingId, userId);
        if (existingParticipant.isPresent()) {
            MeetingParticipant participant = existingParticipant.get();
            if (participant.getStatus() == ParticipationStatus.PENDING) {
                throw new IllegalArgumentException("You have already sent a join request for this meeting");
            }
            if (participant.getStatus() == ParticipationStatus.CONFIRMED) {
                throw new IllegalArgumentException("You are already a participant of this meeting");
            }
            // Jeśli ma inny status, zmień na PENDING
            participant.setStatus(ParticipationStatus.PENDING);
            return participantRepository.save(participant);
        }

        // Dodaj użytkownika jako oczekującego na potwierdzenie
        MeetingParticipant participant = MeetingParticipant.builder()
                .meeting(meeting)
                .user(user)
                .status(ParticipationStatus.PENDING)
                .permissionLevel(PermissionLevel.PARTICIPANT)
                .build();

        MeetingParticipant savedParticipant = participantRepository.save(participant);

        log.info("Join request created for user {} to meeting {}", userId, meetingId);

        // Wyślij powiadomienie do organizatora
        notificationService.sendJoinRequestNotification(meeting.getOrganizer(), user, meeting);

        return savedParticipant;
    }

    @Override
    public void approveJoinRequest(Long meetingId, Long participantId, Long organizerId) {
        log.info("Approving join request {} for meeting {} by organizer {}", participantId, meetingId, organizerId);

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        // Sprawdź czy użytkownik jest organizatorem
        if (!meeting.getOrganizer().getId().equals(organizerId)) {
            throw new SecurityException("Only organizer can approve join requests");
        }

        MeetingParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));

        // Sprawdź czy uczestnik należy do tego spotkania
        if (!participant.getMeeting().getId().equals(meetingId)) {
            throw new IllegalArgumentException("Participant does not belong to this meeting");
        }

        // Sprawdź czy status to PENDING
        if (participant.getStatus() != ParticipationStatus.PENDING) {
            throw new IllegalArgumentException("Participant is not pending approval");
        }

        // Sprawdź czy są jeszcze miejsca
        if (!hasAvailableSpots(meetingId)) {
            throw new IllegalArgumentException("No available spots in this meeting");
        }

        ParticipationStatus oldStatus = participant.getStatus();
        participant.setStatus(ParticipationStatus.CONFIRMED);
        participantRepository.save(participant);

        // Zapisz historię
        saveStatusHistory(participant, oldStatus, ParticipationStatus.CONFIRMED,
                "Join request approved by organizer", organizerId);

        // Wyślij powiadomienie do użytkownika
        notificationService.sendRequestApprovedNotification(participant.getUser(), meeting);

        log.info("Join request approved for participant {} in meeting {}", participantId, meetingId);
    }

    @Override
    public void rejectJoinRequest(Long meetingId, Long participantId, Long organizerId) {
        log.info("Rejecting join request {} for meeting {} by organizer {}", participantId, meetingId, organizerId);

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        // Sprawdź czy użytkownik jest organizatorem
        if (!meeting.getOrganizer().getId().equals(organizerId)) {
            throw new SecurityException("Only organizer can reject join requests");
        }

        MeetingParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));

        // Sprawdź czy uczestnik należy do tego spotkania
        if (!participant.getMeeting().getId().equals(meetingId)) {
            throw new IllegalArgumentException("Participant does not belong to this meeting");
        }

        // Sprawdź czy status to PENDING
        if (participant.getStatus() != ParticipationStatus.PENDING) {
            throw new IllegalArgumentException("Participant is not pending approval");
        }

        ParticipationStatus oldStatus = participant.getStatus();

        // Zmień status na odrzucony lub usuń uczestnika
        participant.setStatus(ParticipationStatus.DECLINED);
        participantRepository.save(participant);

        // Zapisz historię
        saveStatusHistory(participant, oldStatus, ParticipationStatus.DECLINED,
                "Join request rejected by organizer", organizerId);

        // Wyślij powiadomienie do użytkownika
        notificationService.sendRequestRejectedNotification(participant.getUser(), meeting);

        log.info("Join request rejected for participant {} in meeting {}", participantId, meetingId);
    }

    // METODY POMOCNICZE

    @Override
    public boolean hasAvailableSpots(Long meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        if (meeting.getMaxParticipants() == null) {
            return true; // Brak limitu
        }

        long confirmedCount = participantRepository.countByMeetingIdAndStatus(
                meetingId, ParticipationStatus.CONFIRMED);

        return confirmedCount < meeting.getMaxParticipants();
    }

    @Override
    public boolean canUserJoinMeeting(Long meetingId, Long userId) {
        if (userId == null) return false;

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        // Sprawdź czy użytkownik już jest uczestnikiem
        if (isUserParticipant(meetingId, userId)) {
            return false;
        }

        // Sprawdź typ spotkania
        if (meeting.getVisibility() == MeetingVisibility.INVITE_ONLY) {
            return false; // Tylko dla zaproszonych
        }

        // Sprawdź dostępność miejsc
        return hasAvailableSpots(meetingId);
    }

    @Override
    public List<ParticipantResponse> getPendingRequests(Long meetingId) {
        List<MeetingParticipant> pendingParticipants = participantRepository
                .findByMeetingIdAndStatus(meetingId, ParticipationStatus.PENDING);

        return pendingParticipants.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public boolean isUserPendingApproval(Long meetingId, Long userId) {
        return participantRepository.findByMeetingIdAndUserId(meetingId, userId)
                .map(participant -> participant.getStatus() == ParticipationStatus.PENDING)
                .orElse(false);
    }

    // POZOSTAŁE METODY (zachowaj istniejące)

    @Override
    public MeetingParticipant acceptInvitationByToken(String token) {
        // Zachowaj istniejącą implementację
        MeetingParticipant participant = participantRepository.findByInvitationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid invitation token"));

        if (participant.getTokenExpiresAt() != null &&
                participant.getTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Invitation token has expired");
        }

        if (isMeetingFull(participant.getMeeting().getId())) {
            if (!isOnWaitlist(participant.getMeeting().getId(), participant.getUser().getId())) {
                addToWaitlist(participant.getMeeting(), participant.getUser());
            }
            throw new IllegalArgumentException("Meeting is full. You have been added to waitlist.");
        }

        ParticipationStatus oldStatus = participant.getStatus();
        participant.setStatus(ParticipationStatus.CONFIRMED);
        participant.setResponseDate(LocalDateTime.now());
        participant.setInvitationToken(null);

        MeetingParticipant updatedParticipant = participantRepository.save(participant);

        saveStatusHistory(participant, oldStatus, ParticipationStatus.CONFIRMED,
                "Accepted via invitation token", participant.getUser().getId());

        return updatedParticipant;
    }

    @Override
    public boolean isUserParticipant(Long meetingId, Long userId) {
        return participantRepository.findByMeetingIdAndUserId(meetingId, userId).isPresent();
    }

    @Override
    public boolean canUserEditMeeting(Long meetingId, Long userId) {
        return participantRepository.findByMeetingIdAndUserId(meetingId, userId)
                .map(participant ->
                        participant.getPermissionLevel() == PermissionLevel.MODERATOR ||
                                participant.getPermissionLevel() == PermissionLevel.CONTRIBUTOR)
                .orElse(false);
    }

    @Override
    public void joinMeeting(Long userId, Long meetingId) {
        // Uniwersalna metoda join - automatycznie wybiera odpowiednią strategię
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        switch (meeting.getVisibility()) {
            case PUBLIC:
                joinPublicMeeting(meetingId, userId);
                break;
            case PRIVATE:
                requestToJoinPrivateMeeting(meetingId, userId);
                break;
            case INVITE_ONLY:
                throw new SecurityException("This meeting is invite-only");
            default:
                throw new SecurityException("Unknown meeting visibility");
        }
    }

    @Override
    public void leaveMeeting(Long userId, Long meetingId) {
        // Zachowaj istniejącą implementację
        MeetingParticipant participant = participantRepository.findByMeetingIdAndUserId(meetingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));

        boolean wasConfirmed = participant.getStatus() == ParticipationStatus.CONFIRMED;

        participantRepository.delete(participant);

        if (wasConfirmed) {
            promoteNextFromWaitlist(meetingId);
        }
    }

    @Override
    public List<UserResponse> searchUsersForInvitation(String query, Long meetingId) {
        List<User> users = userRepository.findByEmailContainingOrFirstNameContainingOrLastNameContaining(
                query, query, query);

        return users.stream()
                .filter(user -> !isUserAlreadyParticipant(meetingId, user.getId()))
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    // PRIVATE METHODS - zachowaj istniejące implementacje

    private ParticipantResponse mapToResponse(MeetingParticipant participant) {
        ParticipantResponse response = new ParticipantResponse();
        response.setId(participant.getId());
        response.setStatus(participant.getStatus());
        response.setPermissionLevel(participant.getPermissionLevel());
        response.setCreatedAt(participant.getCreatedAt());
        // Dodaj informacje o użytkowniku
        response.setUser(mapToUserResponse(participant.getUser()));

        return response;
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .phoneNumber(user.getPhoneNumber())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private boolean isUserAlreadyParticipant(Long meetingId, Long userId) {
        return participantRepository.existsByMeetingIdAndUserId(meetingId, userId);
    }

    private boolean isMeetingFull(Long meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        if (meeting.getMaxParticipants() == null) {
            return false;
        }

        long confirmedCount = participantRepository.countByMeetingIdAndStatus(
                meetingId, ParticipationStatus.CONFIRMED);

        return confirmedCount >= meeting.getMaxParticipants();
    }

    private MeetingParticipant addToWaitlist(Meeting meeting, User user) {
        if (waitlistEntryRepository.existsByMeetingIdAndUserId(meeting.getId(), user.getId())) {
            throw new IllegalArgumentException("User is already on waitlist");
        }

        Integer nextPosition = waitlistEntryRepository.findMaxPositionByMeetingId(meeting.getId())
                .orElse(0) + 1;

        WaitlistEntry waitlistEntry = WaitlistEntry.builder()
                .meeting(meeting)
                .user(user)
                .position(nextPosition)
                .build();

        waitlistEntryRepository.save(waitlistEntry);

        MeetingParticipant participant = MeetingParticipant.builder()
                .meeting(meeting)
                .user(user)
                .status(ParticipationStatus.WAITING_LIST)
                .permissionLevel(PermissionLevel.PARTICIPANT)
                .build();

        return participantRepository.save(participant);
    }

    private void saveStatusHistory(MeetingParticipant participant, ParticipationStatus oldStatus,
                                   ParticipationStatus newStatus, String comment, Long changedBy) {
        ParticipantStatusHistory history = ParticipantStatusHistory.builder()
                .participant(participant)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .comment(comment)
                .changedByUserId(changedBy)
                .build();

        statusHistoryRepository.save(history);
    }

    private String generateInvitationToken() {
        return UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
    }

    private void sendInvitationEmail(MeetingParticipant participant) {
        try {
            String confirmationLink = "http://yourapp.com/meetings/" +
                    participant.getMeeting().getId() +
                    "/participants/confirm/" +
                    participant.getInvitationToken();

            emailService.sendTemplateEmail(
                    participant.getUser().getEmail(),
                    "Zaproszenie do spotkania: " + participant.getMeeting().getTitle(),
                    "meeting-invitation",
                    Map.of(
                            "meetingTitle", participant.getMeeting().getTitle(),
                            "organizerName", participant.getMeeting().getOrganizer().getFullName(),
                            "confirmationLink", confirmationLink,
                            "meetingDate", participant.getMeeting().getStartDate()
                    )
            );
        } catch (Exception e) {
            log.error("Failed to send invitation email to {}",
                    participant.getUser().getEmail(), e);
        }
    }

    private boolean isOnWaitlist(Long meetingId, Long userId) {
        if (meetingId == null || userId == null) {
            return false;
        }
        return waitlistEntryRepository.existsByMeetingIdAndUserId(meetingId, userId) ||
                participantRepository.findByMeetingIdAndUserId(meetingId, userId)
                        .map(participant -> participant.getStatus() == ParticipationStatus.WAITING_LIST)
                        .orElse(false);
    }

    private void removeFromWaitlist(Long meetingId, Long userId) {
        if (meetingId == null || userId == null) {
            return;
        }
        try {
            Optional<WaitlistEntry> waitlistEntryOpt = waitlistEntryRepository
                    .findByMeetingIdAndUserId(meetingId, userId);

            if (waitlistEntryOpt.isEmpty()) {
                return;
            }

            WaitlistEntry waitlistEntry = waitlistEntryOpt.get();
            Integer removedPosition = waitlistEntry.getPosition();

            waitlistEntryRepository.delete(waitlistEntry);

            if (removedPosition != null) {
                List<WaitlistEntry> entriesToUpdate = waitlistEntryRepository
                        .findByMeetingIdAndPositionGreaterThan(meetingId, removedPosition);

                for (WaitlistEntry entry : entriesToUpdate) {
                    entry.setPosition(entry.getPosition() - 1);
                    waitlistEntryRepository.save(entry);
                }
            }
        } catch (Exception e) {
            log.error("Error removing user {} from waitlist for meeting {}: {}",
                    userId, meetingId, e.getMessage(), e);
        }
    }

    private boolean hasPermissionToUpdateStatus(MeetingParticipant participant, Long userId) {
        if (userId == null) {
            return false;
        }
        if (participant.getMeeting().getOrganizer().getId().equals(userId)) {
            return true;
        }
        if (participant.getUser().getId().equals(userId)) {
            return true;
        }
        Optional<MeetingParticipant> userParticipant = participantRepository.findByMeetingIdAndUserId(
                participant.getMeeting().getId(), userId);

        return userParticipant.isPresent() &&
                userParticipant.get().getPermissionLevel() == PermissionLevel.MODERATOR;
    }

    private void promoteNextFromWaitlist(Long meetingId) {
        if (isMeetingFull(meetingId)) {
            return;
        }

        Optional<WaitlistEntry> nextEntry = waitlistEntryRepository
                .findFirstByMeetingIdOrderByPositionAsc(meetingId);

        if (nextEntry.isPresent()) {
            WaitlistEntry entry = nextEntry.get();
            Optional<MeetingParticipant> participantOpt = participantRepository
                    .findByMeetingIdAndUserId(meetingId, entry.getUser().getId());

            MeetingParticipant participant;
            if (participantOpt.isPresent()) {
                participant = participantOpt.get();
                participant.setStatus(ParticipationStatus.CONFIRMED);
            } else {
                participant = MeetingParticipant.builder()
                        .meeting(entry.getMeeting())
                        .user(entry.getUser())
                        .status(ParticipationStatus.CONFIRMED)
                        .permissionLevel(PermissionLevel.PARTICIPANT)
                        .build();
            }

            participantRepository.save(participant);
            removeFromWaitlist(meetingId, entry.getUser().getId());

            try {
                emailService.sendTemplateEmail(
                        entry.getUser().getEmail(),
                        "Miejsce zwolniło się w spotkaniu: " + entry.getMeeting().getTitle(),
                        "waitlist-promotion",
                        Map.of(
                                "meetingTitle", entry.getMeeting().getTitle(),
                                "organizerName", entry.getMeeting().getOrganizer().getFullName()
                        )
                );
            } catch (Exception e) {
                log.warn("Failed to send promotion notification to {}", entry.getUser().getEmail(), e);
            }
        }
    }
}
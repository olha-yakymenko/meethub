//package com.meethub.domain.service.impl;
//
//import com.meethub.domain.model.entity.*;
//import com.meethub.domain.model.enums.MeetingVisibility;
//import com.meethub.domain.model.enums.ParticipationStatus;
//import com.meethub.domain.model.enums.PermissionLevel;
//import com.meethub.domain.model.mapper.MeetingMapper;
//import com.meethub.domain.model.request.InviteParticipantsRequest;
//import com.meethub.domain.model.response.ParticipantResponse;
//import com.meethub.domain.model.response.UserResponse;
//import com.meethub.domain.repository.jpa.*;
//import com.meethub.domain.service.EmailService;
//import com.meethub.domain.service.MeetingParticipantService;
//import com.meethub.domain.service.NotificationService;
//import com.meethub.exception.ResourceNotFoundException;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//@Transactional
//@Slf4j
//public class MeetingParticipantServiceImpl implements MeetingParticipantService {
//
//    private final MeetingParticipantRepository participantRepository;
//    private final MeetingRepository meetingRepository;
//    private final UserRepository userRepository;
//    private final ParticipantStatusHistoryRepository statusHistoryRepository;
//    private final WaitlistEntryRepository waitlistEntryRepository;
//    private final EmailService emailService;
//    private final NotificationService notificationService;
//    private final MeetingMapper meetingMapper;
//
//    @Override
//    public List<ParticipantResponse> getMeetingParticipants(Long meetingId) {
//        List<MeetingParticipant> participants = participantRepository.findByMeetingId(meetingId);
//        return participants.stream()
//                .map(this::mapToResponse)
//                .toList();
//    }
//
//    @Override
//    public MeetingParticipant inviteParticipant(Long meetingId, Long userId, Long organizerId) {
//        Meeting meeting = meetingRepository.findByIdAndOrganizerId(meetingId, organizerId)
//                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found or access denied"));
//
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
//
//        // Sprawdź czy już nie jest uczestnikiem
//        if (participantRepository.findByMeetingIdAndUserId(meetingId, userId).isPresent()) {
//            throw new IllegalArgumentException("User is already a participant");
//        }
//
//        // Sprawdź limit uczestników
//        if (isMeetingFull(meetingId)) {
//            return addToWaitlist(meeting, user);
//        }
//
//        MeetingParticipant participant = MeetingParticipant.builder()
//                .meeting(meeting)
//                .user(user)
//                .status(ParticipationStatus.INVITED)
//                .permissionLevel(PermissionLevel.PARTICIPANT)
//                .invitationToken(generateInvitationToken())
//                .tokenExpiresAt(LocalDateTime.now().plusDays(7))
//                .build();
//
//        MeetingParticipant savedParticipant = participantRepository.save(participant);
//
//        // Wyślij zaproszenie
//        sendInvitationEmail(savedParticipant);
//
//        return savedParticipant;
//    }
//
//    @Override
//    public List<MeetingParticipant> inviteMultipleParticipants(Long meetingId, InviteParticipantsRequest request, Long organizerId) {
//        return request.getUserIds().stream()
//                .map(userId -> inviteParticipant(meetingId, userId, organizerId))
//                .toList();
//    }
//
//    @Override
//    public MeetingParticipant updateParticipantStatus(Long meetingId, Long participantId,
//                                                      ParticipationStatus status, String comment, Long userId) {
//
//        MeetingParticipant participant = participantRepository.findById(participantId)
//                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));
//
//        ParticipationStatus oldStatus = participant.getStatus();
//
//        // Sprawdź uprawnienia
//        if (!hasPermissionToUpdateStatus(participant, userId)) {
//            throw new SecurityException("No permission to update participant status");
//        }
//
//        participant.setStatus(status);
//        participant.setComment(comment);
//        participant.setResponseDate(LocalDateTime.now());
//
//        MeetingParticipant updatedParticipant = participantRepository.save(participant);
//
//        // Zapisz historię
//        saveStatusHistory(participant, oldStatus, status, comment, userId);
//
//        // Jeśli potwierdzono i był na liście oczekujących
//        if (status == ParticipationStatus.CONFIRMED && isOnWaitlist(meetingId, participant.getUser().getId())) {
//            removeFromWaitlist(meetingId, participant.getUser().getId());
//        }
//
//        return updatedParticipant;
//    }
//
//    @Override
//    public MeetingParticipant updateParticipantPermission(Long meetingId, Long participantId, PermissionLevel permissionLevel, Long organizerId) {
//        // Tylko organizator może zmieniać uprawnienia
//        meetingRepository.findByIdAndOrganizerId(meetingId, organizerId)
//                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found or access denied"));
//
//        MeetingParticipant participant = participantRepository.findById(participantId)
//                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));
//
//        participant.setPermissionLevel(permissionLevel);
//        return participantRepository.save(participant);
//    }
//
//    @Override
//    public void removeParticipant(Long meetingId, Long participantId, Long organizerId) {
//        // Tylko organizator może usuwać uczestników
//        meetingRepository.findByIdAndOrganizerId(meetingId, organizerId)
//                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found or access denied"));
//
//        participantRepository.deleteById(participantId);
//    }
//
//    // NOWE METODY DLA RÓŻNYCH TYPÓW SPOTKAŃ
//
//    @Override
//    public MeetingParticipant joinPublicMeeting(Long meetingId, Long userId) {
//        log.info("Attempting to join public meeting {} by user {}", meetingId, userId);
//
//        Meeting meeting = meetingRepository.findById(meetingId)
//                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
//
//        // Sprawdź czy spotkanie jest publiczne
//        if (meeting.getVisibility() != MeetingVisibility.PUBLIC) {
//            throw new SecurityException("Meeting is not public");
//        }
//
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
//
//        // Sprawdź czy już nie jest uczestnikiem
//        Optional<MeetingParticipant> existingParticipant = participantRepository.findByMeetingIdAndUserId(meetingId, userId);
//        if (existingParticipant.isPresent()) {
//            MeetingParticipant participant = existingParticipant.get();
//            if (participant.getStatus() == ParticipationStatus.CONFIRMED) {
//                throw new IllegalArgumentException("User is already a confirmed participant");
//            }
//            // Jeśli jest zaproszony lub oczekujący, zmień status na potwierdzony
//            participant.setStatus(ParticipationStatus.CONFIRMED);
//            return participantRepository.save(participant);
//        }
//
//        // Sprawdź czy są jeszcze miejsca
//        if (!hasAvailableSpots(meetingId)) {
//            throw new IllegalArgumentException("No available spots in this meeting");
//        }
//
//        // Dodaj użytkownika jako potwierdzonego uczestnika
//        MeetingParticipant participant = MeetingParticipant.builder()
//                .meeting(meeting)
//                .user(user)
//                .status(ParticipationStatus.CONFIRMED)
//                .permissionLevel(PermissionLevel.PARTICIPANT)
//                .build();
//
//        MeetingParticipant savedParticipant = participantRepository.save(participant);
//
//        log.info("User {} successfully joined public meeting {}", userId, meetingId);
//
//        // Wyślij powiadomienie do organizatora
//        notificationService.sendParticipantJoinedNotification(meeting.getOrganizer(), user, meeting);
//
//        return savedParticipant;
//    }
//
//    @Override
//    public MeetingParticipant requestToJoinPrivateMeeting(Long meetingId, Long userId) {
//        log.info("User {} requesting to join private meeting {}", userId, meetingId);
//
//        Meeting meeting = meetingRepository.findById(meetingId)
//                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
//
//        // Sprawdź czy spotkanie jest prywatne
//        if (meeting.getVisibility() != MeetingVisibility.PRIVATE) {
//            throw new SecurityException("Meeting is not private");
//        }
//
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
//
//        // Sprawdź czy użytkownik już nie wysłał prośby
//        Optional<MeetingParticipant> existingParticipant = participantRepository.findByMeetingIdAndUserId(meetingId, userId);
//        if (existingParticipant.isPresent()) {
//            MeetingParticipant participant = existingParticipant.get();
//            if (participant.getStatus() == ParticipationStatus.PENDING) {
//                throw new IllegalArgumentException("You have already sent a join request for this meeting");
//            }
//            if (participant.getStatus() == ParticipationStatus.CONFIRMED) {
//                throw new IllegalArgumentException("You are already a participant of this meeting");
//            }
//            // Jeśli ma inny status, zmień na PENDING
//            participant.setStatus(ParticipationStatus.PENDING);
//            return participantRepository.save(participant);
//        }
//
//        // Dodaj użytkownika jako oczekującego na potwierdzenie
//        MeetingParticipant participant = MeetingParticipant.builder()
//                .meeting(meeting)
//                .user(user)
//                .status(ParticipationStatus.PENDING)
//                .permissionLevel(PermissionLevel.PARTICIPANT)
//                .build();
//
//        MeetingParticipant savedParticipant = participantRepository.save(participant);
//
//        log.info("Join request created for user {} to meeting {}", userId, meetingId);
//
//        // Wyślij powiadomienie do organizatora
//        notificationService.sendJoinRequestNotification(meeting.getOrganizer(), user, meeting);
//
//        return savedParticipant;
//    }
//
//    @Override
//    public void approveJoinRequest(Long meetingId, Long participantId, Long organizerId) {
//        log.info("Approving join request {} for meeting {} by organizer {}", participantId, meetingId, organizerId);
//
//        Meeting meeting = meetingRepository.findById(meetingId)
//                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
//
//        // Sprawdź czy użytkownik jest organizatorem
//        if (!meeting.getOrganizer().getId().equals(organizerId)) {
//            throw new SecurityException("Only organizer can approve join requests");
//        }
//
//        MeetingParticipant participant = participantRepository.findById(participantId)
//                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));
//
//        // Sprawdź czy uczestnik należy do tego spotkania
//        if (!participant.getMeeting().getId().equals(meetingId)) {
//            throw new IllegalArgumentException("Participant does not belong to this meeting");
//        }
//
//        // Sprawdź czy status to PENDING
//        if (participant.getStatus() != ParticipationStatus.PENDING) {
//            throw new IllegalArgumentException("Participant is not pending approval");
//        }
//
//        // Sprawdź czy są jeszcze miejsca
//        if (!hasAvailableSpots(meetingId)) {
//            throw new IllegalArgumentException("No available spots in this meeting");
//        }
//
//        ParticipationStatus oldStatus = participant.getStatus();
//        participant.setStatus(ParticipationStatus.CONFIRMED);
//        participantRepository.save(participant);
//
//        // Zapisz historię
//        saveStatusHistory(participant, oldStatus, ParticipationStatus.CONFIRMED,
//                "Join request approved by organizer", organizerId);
//
//        // Wyślij powiadomienie do użytkownika
//        notificationService.sendRequestApprovedNotification(participant.getUser(), meeting);
//
//        log.info("Join request approved for participant {} in meeting {}", participantId, meetingId);
//    }
//
//    @Override
//    public void rejectJoinRequest(Long meetingId, Long participantId, Long organizerId) {
//        log.info("Rejecting join request {} for meeting {} by organizer {}", participantId, meetingId, organizerId);
//
//        Meeting meeting = meetingRepository.findById(meetingId)
//                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
//
//        // Sprawdź czy użytkownik jest organizatorem
//        if (!meeting.getOrganizer().getId().equals(organizerId)) {
//            throw new SecurityException("Only organizer can reject join requests");
//        }
//
//        MeetingParticipant participant = participantRepository.findById(participantId)
//                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));
//
//        // Sprawdź czy uczestnik należy do tego spotkania
//        if (!participant.getMeeting().getId().equals(meetingId)) {
//            throw new IllegalArgumentException("Participant does not belong to this meeting");
//        }
//
//        // Sprawdź czy status to PENDING
//        if (participant.getStatus() != ParticipationStatus.PENDING) {
//            throw new IllegalArgumentException("Participant is not pending approval");
//        }
//
//        ParticipationStatus oldStatus = participant.getStatus();
//
//        // Zmień status na odrzucony lub usuń uczestnika
//        participant.setStatus(ParticipationStatus.DECLINED);
//        participantRepository.save(participant);
//
//        // Zapisz historię
//        saveStatusHistory(participant, oldStatus, ParticipationStatus.DECLINED,
//                "Join request rejected by organizer", organizerId);
//
//        // Wyślij powiadomienie do użytkownika
//        notificationService.sendRequestRejectedNotification(participant.getUser(), meeting);
//
//        log.info("Join request rejected for participant {} in meeting {}", participantId, meetingId);
//    }
//
//    // METODY POMOCNICZE
//
//    @Override
//    public boolean hasAvailableSpots(Long meetingId) {
//        Meeting meeting = meetingRepository.findById(meetingId)
//                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
//
//        if (meeting.getMaxParticipants() == null) {
//            return true; // Brak limitu
//        }
//
//        long confirmedCount = participantRepository.countByMeetingIdAndStatus(
//                meetingId, ParticipationStatus.CONFIRMED);
//
//        return confirmedCount < meeting.getMaxParticipants();
//    }
//
//    @Override
//    public boolean canUserJoinMeeting(Long meetingId, Long userId) {
//        if (userId == null) return false;
//
//        Meeting meeting = meetingRepository.findById(meetingId)
//                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
//
//        // Sprawdź czy użytkownik już jest uczestnikiem
//        if (isUserParticipant(meetingId, userId)) {
//            return false;
//        }
//
//        // Sprawdź typ spotkania
//        if (meeting.getVisibility() == MeetingVisibility.INVITE_ONLY) {
//            return false; // Tylko dla zaproszonych
//        }
//
//        // Sprawdź dostępność miejsc
//        return hasAvailableSpots(meetingId);
//    }
//
//    @Override
//    public List<ParticipantResponse> getPendingRequests(Long meetingId) {
//        List<MeetingParticipant> pendingParticipants = participantRepository
//                .findByMeetingIdAndStatus(meetingId, ParticipationStatus.PENDING);
//
//        return pendingParticipants.stream()
//                .map(this::mapToResponse)
//                .toList();
//    }
//
//    @Override
//    public boolean isUserPendingApproval(Long meetingId, Long userId) {
//        return participantRepository.findByMeetingIdAndUserId(meetingId, userId)
//                .map(participant -> participant.getStatus() == ParticipationStatus.PENDING)
//                .orElse(false);
//    }
//
//    // POZOSTAŁE METODY (zachowaj istniejące)
//
//    @Override
//    public MeetingParticipant acceptInvitationByToken(String token) {
//        // Zachowaj istniejącą implementację
//        MeetingParticipant participant = participantRepository.findByInvitationToken(token)
//                .orElseThrow(() -> new ResourceNotFoundException("Invalid invitation token"));
//
//        if (participant.getTokenExpiresAt() != null &&
//                participant.getTokenExpiresAt().isBefore(LocalDateTime.now())) {
//            throw new IllegalArgumentException("Invitation token has expired");
//        }
//
//        if (isMeetingFull(participant.getMeeting().getId())) {
//            if (!isOnWaitlist(participant.getMeeting().getId(), participant.getUser().getId())) {
//                addToWaitlist(participant.getMeeting(), participant.getUser());
//            }
//            throw new IllegalArgumentException("Meeting is full. You have been added to waitlist.");
//        }
//
//        ParticipationStatus oldStatus = participant.getStatus();
//        participant.setStatus(ParticipationStatus.CONFIRMED);
//        participant.setResponseDate(LocalDateTime.now());
//        participant.setInvitationToken(null);
//
//        MeetingParticipant updatedParticipant = participantRepository.save(participant);
//
//        saveStatusHistory(participant, oldStatus, ParticipationStatus.CONFIRMED,
//                "Accepted via invitation token", participant.getUser().getId());
//
//        return updatedParticipant;
//    }
//
//    @Override
//    public boolean isUserParticipant(Long meetingId, Long userId) {
//        return participantRepository.findByMeetingIdAndUserId(meetingId, userId).isPresent();
//    }
//
//    @Override
//    public boolean canUserEditMeeting(Long meetingId, Long userId) {
//        return participantRepository.findByMeetingIdAndUserId(meetingId, userId)
//                .map(participant ->
//                        participant.getPermissionLevel() == PermissionLevel.MODERATOR ||
//                                participant.getPermissionLevel() == PermissionLevel.CONTRIBUTOR)
//                .orElse(false);
//    }
//
//    @Override
//    public void joinMeeting(Long userId, Long meetingId) {
//        // Uniwersalna metoda join - automatycznie wybiera odpowiednią strategię
//        Meeting meeting = meetingRepository.findById(meetingId)
//                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
//
//        switch (meeting.getVisibility()) {
//            case PUBLIC:
//                joinPublicMeeting(meetingId, userId);
//                break;
//            case PRIVATE:
//                requestToJoinPrivateMeeting(meetingId, userId);
//                break;
//            case INVITE_ONLY:
//                throw new SecurityException("This meeting is invite-only");
//            default:
//                throw new SecurityException("Unknown meeting visibility");
//        }
//    }
//
//    @Override
//    public void leaveMeeting(Long userId, Long meetingId) {
//        // Zachowaj istniejącą implementację
//        MeetingParticipant participant = participantRepository.findByMeetingIdAndUserId(meetingId, userId)
//                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));
//
//        boolean wasConfirmed = participant.getStatus() == ParticipationStatus.CONFIRMED;
//
//        participantRepository.delete(participant);
//
//        if (wasConfirmed) {
//            promoteNextFromWaitlist(meetingId);
//        }
//    }
//
//    @Override
//    public List<UserResponse> searchUsersForInvitation(String query, Long meetingId) {
//        List<User> users = userRepository.findByEmailContainingOrFirstNameContainingOrLastNameContaining(
//                query, query, query);
//
//        return users.stream()
//                .filter(user -> !isUserAlreadyParticipant(meetingId, user.getId()))
//                .map(this::mapToUserResponse)
//                .collect(Collectors.toList());
//    }
//
//    // PRIVATE METHODS - zachowaj istniejące implementacje
//
////    private ParticipantResponse mapToResponse(MeetingParticipant participant) {
////        ParticipantResponse response = new ParticipantResponse();
////        response.setId(participant.getId());
////        response.setStatus(participant.getStatus());
////        response.setPermissionLevel(participant.getPermissionLevel());
////        response.setCreatedAt(participant.getCreatedAt());
////        // Dodaj informacje o użytkowniku
////        response.setUser(mapToUserResponse(participant.getUser()));
////
////        return response;
////    }
//
//    private ParticipantResponse mapToResponse(MeetingParticipant participant) {
//        ParticipantResponse response = new ParticipantResponse();
//        response.setId(participant.getId());
//        response.setStatus(participant.getStatus());
//        response.setPermissionLevel(participant.getPermissionLevel());
//        response.setComment(participant.getComment());
//        response.setResponseDate(participant.getResponseDate());
//        response.setCreatedAt(participant.getCreatedAt());
//        response.setUpdatedAt(participant.getUpdatedAt());
//        response.setUser(mapToUserResponse(participant.getUser()));
//
//        // ✅ DODAJ MAPPING DLA MEETING
//        if (participant.getMeeting() != null) {
//            response.setMeeting(meetingMapper.toResponse(participant.getMeeting()));
//        }
//
//        return response;
//    }
//
//    private UserResponse mapToUserResponse(User user) {
//        return UserResponse.builder()
//                .id(user.getId())
//                .email(user.getEmail())
//                .firstName(user.getFirstName())
//                .lastName(user.getLastName())
//                .role(user.getRole())
//                .phoneNumber(user.getPhoneNumber())
//                .createdAt(user.getCreatedAt())
//                .build();
//    }
//
//    private boolean isUserAlreadyParticipant(Long meetingId, Long userId) {
//        return participantRepository.existsByMeetingIdAndUserId(meetingId, userId);
//    }
//
//    private boolean isMeetingFull(Long meetingId) {
//        Meeting meeting = meetingRepository.findById(meetingId)
//                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
//
//        if (meeting.getMaxParticipants() == null) {
//            return false;
//        }
//
//        long confirmedCount = participantRepository.countByMeetingIdAndStatus(
//                meetingId, ParticipationStatus.CONFIRMED);
//
//        return confirmedCount >= meeting.getMaxParticipants();
//    }
//
//    private MeetingParticipant addToWaitlist(Meeting meeting, User user) {
//        if (waitlistEntryRepository.existsByMeetingIdAndUserId(meeting.getId(), user.getId())) {
//            throw new IllegalArgumentException("User is already on waitlist");
//        }
//
//        Integer nextPosition = waitlistEntryRepository.findMaxPositionByMeetingId(meeting.getId())
//                .orElse(0) + 1;
//
//        WaitlistEntry waitlistEntry = WaitlistEntry.builder()
//                .meeting(meeting)
//                .user(user)
//                .position(nextPosition)
//                .build();
//
//        waitlistEntryRepository.save(waitlistEntry);
//
//        MeetingParticipant participant = MeetingParticipant.builder()
//                .meeting(meeting)
//                .user(user)
//                .status(ParticipationStatus.WAITING_LIST)
//                .permissionLevel(PermissionLevel.PARTICIPANT)
//                .build();
//
//        return participantRepository.save(participant);
//    }
//
//    private void saveStatusHistory(MeetingParticipant participant, ParticipationStatus oldStatus,
//                                   ParticipationStatus newStatus, String comment, Long changedBy) {
//        ParticipantStatusHistory history = ParticipantStatusHistory.builder()
//                .participant(participant)
//                .oldStatus(oldStatus)
//                .newStatus(newStatus)
//                .comment(comment)
//                .changedByUserId(changedBy)
//                .build();
//
//        statusHistoryRepository.save(history);
//    }
//
//    private String generateInvitationToken() {
//        return UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
//    }
//
//    private void sendInvitationEmail(MeetingParticipant participant) {
//        try {
//            String confirmationLink = "http://yourapp.com/meetings/" +
//                    participant.getMeeting().getId() +
//                    "/participants/confirm/" +
//                    participant.getInvitationToken();
//
//            emailService.sendTemplateEmail(
//                    participant.getUser().getEmail(),
//                    "Zaproszenie do spotkania: " + participant.getMeeting().getTitle(),
//                    "meeting-invitation",
//                    Map.of(
//                            "meetingTitle", participant.getMeeting().getTitle(),
//                            "organizerName", participant.getMeeting().getOrganizer().getFullName(),
//                            "confirmationLink", confirmationLink,
//                            "meetingDate", participant.getMeeting().getStartDate()
//                    )
//            );
//        } catch (Exception e) {
//            log.error("Failed to send invitation email to {}",
//                    participant.getUser().getEmail(), e);
//        }
//    }
//
//    private boolean isOnWaitlist(Long meetingId, Long userId) {
//        if (meetingId == null || userId == null) {
//            return false;
//        }
//        return waitlistEntryRepository.existsByMeetingIdAndUserId(meetingId, userId) ||
//                participantRepository.findByMeetingIdAndUserId(meetingId, userId)
//                        .map(participant -> participant.getStatus() == ParticipationStatus.WAITING_LIST)
//                        .orElse(false);
//    }
//
//    private void removeFromWaitlist(Long meetingId, Long userId) {
//        if (meetingId == null || userId == null) {
//            return;
//        }
//        try {
//            Optional<WaitlistEntry> waitlistEntryOpt = waitlistEntryRepository
//                    .findByMeetingIdAndUserId(meetingId, userId);
//
//            if (waitlistEntryOpt.isEmpty()) {
//                return;
//            }
//
//            WaitlistEntry waitlistEntry = waitlistEntryOpt.get();
//            Integer removedPosition = waitlistEntry.getPosition();
//
//            waitlistEntryRepository.delete(waitlistEntry);
//
//            if (removedPosition != null) {
//                List<WaitlistEntry> entriesToUpdate = waitlistEntryRepository
//                        .findByMeetingIdAndPositionGreaterThan(meetingId, removedPosition);
//
//                for (WaitlistEntry entry : entriesToUpdate) {
//                    entry.setPosition(entry.getPosition() - 1);
//                    waitlistEntryRepository.save(entry);
//                }
//            }
//        } catch (Exception e) {
//            log.error("Error removing user {} from waitlist for meeting {}: {}",
//                    userId, meetingId, e.getMessage(), e);
//        }
//    }
//
//    private boolean hasPermissionToUpdateStatus(MeetingParticipant participant, Long userId) {
//        if (userId == null) {
//            return false;
//        }
//        if (participant.getMeeting().getOrganizer().getId().equals(userId)) {
//            return true;
//        }
//        if (participant.getUser().getId().equals(userId)) {
//            return true;
//        }
//        Optional<MeetingParticipant> userParticipant = participantRepository.findByMeetingIdAndUserId(
//                participant.getMeeting().getId(), userId);
//
//        return userParticipant.isPresent() &&
//                userParticipant.get().getPermissionLevel() == PermissionLevel.MODERATOR;
//    }
//
//    private void promoteNextFromWaitlist(Long meetingId) {
//        if (isMeetingFull(meetingId)) {
//            return;
//        }
//
//        Optional<WaitlistEntry> nextEntry = waitlistEntryRepository
//                .findFirstByMeetingIdOrderByPositionAsc(meetingId);
//
//        if (nextEntry.isPresent()) {
//            WaitlistEntry entry = nextEntry.get();
//            Optional<MeetingParticipant> participantOpt = participantRepository
//                    .findByMeetingIdAndUserId(meetingId, entry.getUser().getId());
//
//            MeetingParticipant participant;
//            if (participantOpt.isPresent()) {
//                participant = participantOpt.get();
//                participant.setStatus(ParticipationStatus.CONFIRMED);
//            } else {
//                participant = MeetingParticipant.builder()
//                        .meeting(entry.getMeeting())
//                        .user(entry.getUser())
//                        .status(ParticipationStatus.CONFIRMED)
//                        .permissionLevel(PermissionLevel.PARTICIPANT)
//                        .build();
//            }
//
//            participantRepository.save(participant);
//            removeFromWaitlist(meetingId, entry.getUser().getId());
//
//            try {
//                emailService.sendTemplateEmail(
//                        entry.getUser().getEmail(),
//                        "Miejsce zwolniło się w spotkaniu: " + entry.getMeeting().getTitle(),
//                        "waitlist-promotion",
//                        Map.of(
//                                "meetingTitle", entry.getMeeting().getTitle(),
//                                "organizerName", entry.getMeeting().getOrganizer().getFullName()
//                        )
//                );
//            } catch (Exception e) {
//                log.warn("Failed to send promotion notification to {}", entry.getUser().getEmail(), e);
//            }
//        }
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public PermissionLevel getParticipantPermissionLevel(Long meetingId, Long userId) {
//        log.debug("Getting permission level for user {} in meeting {}", userId, meetingId);
//
//        if (userId == null || meetingId == null) {
//            log.debug("Null parameters provided - returning default PARTICIPANT level");
//            return PermissionLevel.PARTICIPANT;
//        }
//
//        try {
//            // ✅ SPRAWDŹ CZY UŻYTKOWNIK JEST ORGANIZATOREM
//            Meeting meeting = meetingRepository.findById(meetingId)
//                    .orElseThrow(() -> new ResourceNotFoundException("Meeting not found with id: " + meetingId));
//
//            if (meeting.getOrganizer() != null && meeting.getOrganizer().getId().equals(userId)) {
//                log.debug("User {} is organizer of meeting {} - returning ORGANIZER level", userId, meetingId);
//                return PermissionLevel.ORGANIZER; // LUB MODERATOR JEŚLI NIE MASZ ORGANIZER W ENUM
//            }
//
//            // ✅ SPRAWDŹ UPRAWNIENIA UCZESTNIKA
//            Optional<MeetingParticipant> participantOpt = participantRepository.findByMeetingIdAndUserId(meetingId, userId);
//
//            if (participantOpt.isPresent()) {
//                MeetingParticipant participant = participantOpt.get();
//                PermissionLevel level = participant.getPermissionLevel();
//                log.debug("Found participant permission level: {} for user {} in meeting {}", level, userId, meetingId);
//                return level;
//            }
//
//            // ✅ UŻYTKOWNIK NIE JEST UCZESTNIKIEM - ZWRÓĆ DOMYŚLNY POZIOM
//            log.debug("User {} is not a participant of meeting {} - returning default PARTICIPANT level", userId, meetingId);
//            return PermissionLevel.PARTICIPANT;
//
//        } catch (ResourceNotFoundException e) {
//            log.warn("Meeting not found while checking permission level: {}", e.getMessage());
//            return PermissionLevel.PARTICIPANT;
//        } catch (Exception e) {
//            log.error("Error getting permission level for user {} in meeting {}: {}",
//                    userId, meetingId, e.getMessage(), e);
//            return PermissionLevel.PARTICIPANT; // BEZPIECZNY DOMYŚLNY POZIOM
//        }
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<ParticipantResponse> getUserInvitations(Long userId) {
//        log.info("Getting invitations for user: {}", userId);
//
//        List<MeetingParticipant> invitations = participantRepository.findByUserIdAndStatus(
//                userId, ParticipationStatus.INVITED);
//
//        return invitations.stream()
//                .map(this::mapToResponse)
//                .toList();
//    }
//
//    @Override
//    @Transactional
//    public void respondToInvitation(Long participantId, ParticipationStatus response, String comment, Long userId) {
//        log.info("User {} responding to invitation {} with status: {}", userId, participantId, response);
//
//        MeetingParticipant participant = participantRepository.findById(participantId)
//                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));
//
//        // Sprawdź czy użytkownik jest właścicielem tego zaproszenia
//        if (!participant.getUser().getId().equals(userId)) {
//            throw new SecurityException("No permission to respond to this invitation");
//        }
//
//        // Sprawdź czy status to nadal INVITED
//        if (participant.getStatus() != ParticipationStatus.INVITED) {
//            throw new IllegalArgumentException("Invitation has already been responded to");
//        }
//
//        ParticipationStatus oldStatus = participant.getStatus();
//        participant.setStatus(response);
//        participant.setComment(comment);
//        participant.setResponseDate(LocalDateTime.now());
//
//        participantRepository.save(participant);
//
//        // Zapisz historię
//        saveStatusHistory(participant, oldStatus, response,
//                "User responded: " + comment, userId);
//
//        // Wyślij powiadomienie do organizatora
////        if (response == ParticipationStatus.CONFIRMED) {
////            notificationService.sendInvitationAcceptedNotification(
////                    participant.getMeeting().getOrganizer(),
////                    participant.getUser(),
////                    participant.getMeeting()
////            );
////        } else if (response == ParticipationStatus.DECLINED) {
////            notificationService.sendInvitationDeclinedNotification(
////                    participant.getMeeting().getOrganizer(),
////                    participant.getUser(),
////                    participant.getMeeting()
////            );
////        }
//
//        log.info("User {} responded to invitation {} with status: {}", userId, participantId, response);
//    }
//
//
////    @Override
////    @Transactional(readOnly = true)
////    public List<ParticipantResponse> getConfirmedParticipants(Long meetingId) {
////        log.info("Getting confirmed participants for meeting: {}", meetingId);
////
////        try {
////            // ✅ Opcja B: Filtrowanie w bazie danych - LEPSZE WYDAJNOŚCIOWO
////            List<MeetingParticipant> confirmedParticipants = participantRepository
////                    .findByMeetingIdAndStatus(meetingId, ParticipationStatus.CONFIRMED);
////
////            List<ParticipantResponse> result = confirmedParticipants.stream()
////                    .map(this::mapToResponse)
////                    .collect(Collectors.toList());
////
////            log.info("Found {} confirmed participants for meeting {}", result.size(), meetingId);
////            return result;
////
////        } catch (Exception e) {
////            log.error("Error getting confirmed participants for meeting {}: {}", meetingId, e.getMessage(), e);
////            return Collections.emptyList();
////        }
////    }
//
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<ParticipantResponse> getConfirmedParticipants(Long meetingId) {
//        log.info("Getting confirmed participants for meeting: {}", meetingId);
//
//        try {
//            // ✅ Pobierz spotkanie aby dostać organizatora
//            Meeting meeting = meetingRepository.findById(meetingId)
//                    .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
//
//            // ✅ Pobierz potwierdzonych uczestników z bazy
//            List<MeetingParticipant> confirmedParticipants = participantRepository
//                    .findByMeetingIdAndStatus(meetingId, ParticipationStatus.CONFIRMED);
//
//            List<ParticipantResponse> result = confirmedParticipants.stream()
//                    .map(this::mapToResponse)
//                    .collect(Collectors.toList());
//
//            // ✅ DODAJ ORGANIZATORA JAKO UCZESTNIKA (jeśli go nie ma)
//            boolean organizerIsInList = result.stream()
//                    .anyMatch(p -> p.getUser().getId().equals(meeting.getOrganizer().getId()));
//
//            if (!organizerIsInList) {
//                ParticipantResponse organizerParticipant = createOrganizerParticipant(meeting);
//                result.add(organizerParticipant);
//                log.info("Added organizer as participant for meeting {}", meetingId);
//            }
//
//            log.info("Found {} confirmed participants for meeting {} (including organizer)",
//                    result.size(), meetingId);
//            return result;
//
//        } catch (Exception e) {
//            log.error("Error getting confirmed participants for meeting {}: {}", meetingId, e.getMessage(), e);
//            return Collections.emptyList();
//        }
//    }
//
//    private ParticipantResponse createOrganizerParticipant(Meeting meeting) {
//        return ParticipantResponse.builder()
//                .id(-1L) // tymczasowe ID (organizator nie ma rekordu w meeting_participants)
//                .user(mapToUserResponse(meeting.getOrganizer()))
//                .status(ParticipationStatus.CONFIRMED)
//                .permissionLevel(PermissionLevel.ORGANIZER)
//                .createdAt(meeting.getCreatedAt())
//                .updatedAt(meeting.getUpdatedAt())
//                .build();
//    }
//
//
//
////    @Override
////    @Transactional(readOnly = true)
////    public Map<String, Long> getParticipantStatistics(Long meetingId) {
////        log.info("Getting participant statistics for meeting: {}", meetingId);
////
////        Map<String, Long> stats = new HashMap<>();
////
////        try {
////            // ✅ Opcja A: Użyj metod repozytorium (najszybsze)
////            stats.put("total", participantRepository.countByMeetingId(meetingId));
////            stats.put("confirmed", participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.CONFIRMED));
////            stats.put("pending", participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.PENDING));
////            stats.put("invited", participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.INVITED));
////            stats.put("waiting", participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.WAITING_LIST));
////            stats.put("declined", participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.DECLINED));
////
////            log.info("Participant stats for meeting {}: {}", meetingId, stats);
////
////        } catch (Exception e) {
////            log.error("Error getting participant statistics for meeting {}: {}", meetingId, e.getMessage(), e);
////            // ✅ Zwróć bezpieczne domyślne wartości
////            return getDefaultStats();
////        }
////
////        return stats;
////    }
//
//
//    @Override
//    @Transactional(readOnly = true)
//    public Map<String, Long> getParticipantStatistics(Long meetingId) {
//        log.info("Getting participant statistics for meeting: {}", meetingId);
//
//        Map<String, Long> stats = new HashMap<>();
//
//        try {
//            // ✅ Pobierz spotkanie
//            Meeting meeting = meetingRepository.findById(meetingId)
//                    .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
//
//            // ✅ Podstawowe statystyki z bazy
//            long totalFromDb = participantRepository.countByMeetingId(meetingId);
//            long confirmedFromDb = participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.CONFIRMED);
//
//            // ✅ Sprawdź czy organizator jest już w uczestnikach
//            boolean organizerIsParticipant = participantRepository
//                    .findByMeetingIdAndUserId(meetingId, meeting.getOrganizer().getId())
//                    .isPresent();
//
//            // ✅ DOSTOSUJ STATYSTYKI - uwzględnij organizatora
//            stats.put("total", organizerIsParticipant ? totalFromDb : totalFromDb + 1);
//            stats.put("confirmed", organizerIsParticipant ? confirmedFromDb : confirmedFromDb + 1);
//            stats.put("pending", participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.PENDING));
//            stats.put("invited", participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.INVITED));
//            stats.put("waiting", participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.WAITING_LIST));
//            stats.put("declined", participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.DECLINED));
//            stats.put("organizerIncluded", organizerIsParticipant ? 1L : 0L); // dla debugu
//
//            log.info("Participant stats for meeting {}: {} (organizer included: {})",
//                    meetingId, stats, !organizerIsParticipant);
//
//        } catch (Exception e) {
//            log.error("Error getting participant statistics for meeting {}: {}", meetingId, e.getMessage(), e);
//            return getDefaultStats();
//        }
//
//        return stats;
//    }
//
//    private Map<String, Long> getDefaultStats() {
//        Map<String, Long> defaultStats = new HashMap<>();
//        defaultStats.put("total", 0L);
//        defaultStats.put("confirmed", 0L);
//        defaultStats.put("pending", 0L);
//        defaultStats.put("invited", 0L);
//        defaultStats.put("waiting", 0L);
//        defaultStats.put("declined", 0L);
//        return defaultStats;
//    }
//
//}













package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.*;
import com.meethub.domain.model.enums.MeetingVisibility;
import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.model.enums.PermissionLevel;
import com.meethub.domain.model.mapper.MeetingMapper;
import com.meethub.domain.model.request.InviteParticipantsRequest;
import com.meethub.domain.model.request.UpdateParticipantRequest;
import com.meethub.domain.model.response.ParticipantResponse;
import com.meethub.domain.model.response.UserResponse;
import com.meethub.domain.repository.jpa.*;
import com.meethub.domain.service.EmailService;
import com.meethub.domain.service.MeetingParticipantService;
import com.meethub.domain.service.NotificationService;
import com.meethub.exception.ResourceNotFoundException;
import com.meethub.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
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
    private final MeetingMapper meetingMapper;

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
    public ParticipantResponse updateParticipant(Long participantId, UpdateParticipantRequest request) {
        // Ta metoda potrzebuje klasy UpdateParticipantRequest
        // Stwórz ją lub użyj istniejącej logiki
        MeetingParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));

        participant.setStatus(request.getStatus());
        participant.setPermissionLevel(request.getPermissionLevel());
        participant.setComment(request.getComment());

        MeetingParticipant updated = participantRepository.save(participant);
        return mapToResponse(updated);
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

//    private ParticipantResponse mapToResponse(MeetingParticipant participant) {
//        ParticipantResponse response = new ParticipantResponse();
//        response.setId(participant.getId());
//        response.setStatus(participant.getStatus());
//        response.setPermissionLevel(participant.getPermissionLevel());
//        response.setCreatedAt(participant.getCreatedAt());
//        // Dodaj informacje o użytkowniku
//        response.setUser(mapToUserResponse(participant.getUser()));
//
//        return response;
//    }

    private ParticipantResponse mapToResponse(MeetingParticipant participant) {
        ParticipantResponse response = new ParticipantResponse();
        response.setId(participant.getId());
        response.setStatus(participant.getStatus());
        response.setPermissionLevel(participant.getPermissionLevel());
        response.setComment(participant.getComment());
        response.setResponseDate(participant.getResponseDate());
        response.setCreatedAt(participant.getCreatedAt());
        response.setUpdatedAt(participant.getUpdatedAt());
        response.setUser(mapToUserResponse(participant.getUser()));

        // ✅ DODAJ MAPPING DLA MEETING
        if (participant.getMeeting() != null) {
            response.setMeeting(meetingMapper.toResponse(participant.getMeeting()));
        }

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

    @Override
    @Transactional(readOnly = true)
    public PermissionLevel getParticipantPermissionLevel(Long meetingId, Long userId) {
        log.debug("Getting permission level for user {} in meeting {}", userId, meetingId);

        if (userId == null || meetingId == null) {
            log.debug("Null parameters provided - returning default PARTICIPANT level");
            return PermissionLevel.PARTICIPANT;
        }

        try {
            // ✅ SPRAWDŹ CZY UŻYTKOWNIK JEST ORGANIZATOREM
            Meeting meeting = meetingRepository.findById(meetingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Meeting not found with id: " + meetingId));

            if (meeting.getOrganizer() != null && meeting.getOrganizer().getId().equals(userId)) {
                log.debug("User {} is organizer of meeting {} - returning ORGANIZER level", userId, meetingId);
                return PermissionLevel.ORGANIZER; // LUB MODERATOR JEŚLI NIE MASZ ORGANIZER W ENUM
            }

            // ✅ SPRAWDŹ UPRAWNIENIA UCZESTNIKA
            Optional<MeetingParticipant> participantOpt = participantRepository.findByMeetingIdAndUserId(meetingId, userId);

            if (participantOpt.isPresent()) {
                MeetingParticipant participant = participantOpt.get();
                PermissionLevel level = participant.getPermissionLevel();
                log.debug("Found participant permission level: {} for user {} in meeting {}", level, userId, meetingId);
                return level;
            }

            // ✅ UŻYTKOWNIK NIE JEST UCZESTNIKIEM - ZWRÓĆ DOMYŚLNY POZIOM
            log.debug("User {} is not a participant of meeting {} - returning default PARTICIPANT level", userId, meetingId);
            return PermissionLevel.PARTICIPANT;

        } catch (ResourceNotFoundException e) {
            log.warn("Meeting not found while checking permission level: {}", e.getMessage());
            return PermissionLevel.PARTICIPANT;
        } catch (Exception e) {
            log.error("Error getting permission level for user {} in meeting {}: {}",
                    userId, meetingId, e.getMessage(), e);
            return PermissionLevel.PARTICIPANT; // BEZPIECZNY DOMYŚLNY POZIOM
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipantResponse> getUserInvitations(Long userId) {
        log.info("Getting invitations for user: {}", userId);

        List<MeetingParticipant> invitations = participantRepository.findByUserIdAndStatus(
                userId, ParticipationStatus.INVITED);

        return invitations.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public void respondToInvitation(Long participantId, ParticipationStatus response, String comment, Long userId) {
        log.info("User {} responding to invitation {} with status: {}", userId, participantId, response);

        MeetingParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        // Sprawdź czy użytkownik jest właścicielem tego zaproszenia
        if (!participant.getUser().getId().equals(userId)) {
            throw new SecurityException("No permission to respond to this invitation");
        }

        // Sprawdź czy status to nadal INVITED
        if (participant.getStatus() != ParticipationStatus.INVITED) {
            throw new IllegalArgumentException("Invitation has already been responded to");
        }

        ParticipationStatus oldStatus = participant.getStatus();
        participant.setStatus(response);
        participant.setComment(comment);
        participant.setResponseDate(LocalDateTime.now());

        participantRepository.save(participant);

        // Zapisz historię
        saveStatusHistory(participant, oldStatus, response,
                "User responded: " + comment, userId);

        // Wyślij powiadomienie do organizatora
//        if (response == ParticipationStatus.CONFIRMED) {
//            notificationService.sendInvitationAcceptedNotification(
//                    participant.getMeeting().getOrganizer(),
//                    participant.getUser(),
//                    participant.getMeeting()
//            );
//        } else if (response == ParticipationStatus.DECLINED) {
//            notificationService.sendInvitationDeclinedNotification(
//                    participant.getMeeting().getOrganizer(),
//                    participant.getUser(),
//                    participant.getMeeting()
//            );
//        }

        log.info("User {} responded to invitation {} with status: {}", userId, participantId, response);
    }


//    @Override
//    @Transactional(readOnly = true)
//    public List<ParticipantResponse> getConfirmedParticipants(Long meetingId) {
//        log.info("Getting confirmed participants for meeting: {}", meetingId);
//
//        try {
//            // ✅ Opcja B: Filtrowanie w bazie danych - LEPSZE WYDAJNOŚCIOWO
//            List<MeetingParticipant> confirmedParticipants = participantRepository
//                    .findByMeetingIdAndStatus(meetingId, ParticipationStatus.CONFIRMED);
//
//            List<ParticipantResponse> result = confirmedParticipants.stream()
//                    .map(this::mapToResponse)
//                    .collect(Collectors.toList());
//
//            log.info("Found {} confirmed participants for meeting {}", result.size(), meetingId);
//            return result;
//
//        } catch (Exception e) {
//            log.error("Error getting confirmed participants for meeting {}: {}", meetingId, e.getMessage(), e);
//            return Collections.emptyList();
//        }
//    }


    @Override
    @Transactional(readOnly = true)
    public List<ParticipantResponse> getConfirmedParticipants(Long meetingId) {
        log.info("Getting confirmed participants for meeting: {}", meetingId);

        try {
            // ✅ Pobierz spotkanie aby dostać organizatora
            Meeting meeting = meetingRepository.findById(meetingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

            // ✅ Pobierz potwierdzonych uczestników z bazy
            List<MeetingParticipant> confirmedParticipants = participantRepository
                    .findByMeetingIdAndStatus(meetingId, ParticipationStatus.CONFIRMED);

            List<ParticipantResponse> result = confirmedParticipants.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());

            // ✅ DODAJ ORGANIZATORA JAKO UCZESTNIKA (jeśli go nie ma)
            boolean organizerIsInList = result.stream()
                    .anyMatch(p -> p.getUser().getId().equals(meeting.getOrganizer().getId()));

            if (!organizerIsInList) {
                ParticipantResponse organizerParticipant = createOrganizerParticipant(meeting);
                result.add(organizerParticipant);
                log.info("Added organizer as participant for meeting {}", meetingId);
            }

            log.info("Found {} confirmed participants for meeting {} (including organizer)",
                    result.size(), meetingId);
            return result;

        } catch (Exception e) {
            log.error("Error getting confirmed participants for meeting {}: {}", meetingId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private ParticipantResponse createOrganizerParticipant(Meeting meeting) {
        return ParticipantResponse.builder()
                .id(-1L) // tymczasowe ID (organizator nie ma rekordu w meeting_participants)
                .user(mapToUserResponse(meeting.getOrganizer()))
                .status(ParticipationStatus.CONFIRMED)
                .permissionLevel(PermissionLevel.ORGANIZER)
                .createdAt(meeting.getCreatedAt())
                .updatedAt(meeting.getUpdatedAt())
                .build();
    }



//    @Override
//    @Transactional(readOnly = true)
//    public Map<String, Long> getParticipantStatistics(Long meetingId) {
//        log.info("Getting participant statistics for meeting: {}", meetingId);
//
//        Map<String, Long> stats = new HashMap<>();
//
//        try {
//            // ✅ Opcja A: Użyj metod repozytorium (najszybsze)
//            stats.put("total", participantRepository.countByMeetingId(meetingId));
//            stats.put("confirmed", participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.CONFIRMED));
//            stats.put("pending", participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.PENDING));
//            stats.put("invited", participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.INVITED));
//            stats.put("waiting", participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.WAITING_LIST));
//            stats.put("declined", participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.DECLINED));
//
//            log.info("Participant stats for meeting {}: {}", meetingId, stats);
//
//        } catch (Exception e) {
//            log.error("Error getting participant statistics for meeting {}: {}", meetingId, e.getMessage(), e);
//            // ✅ Zwróć bezpieczne domyślne wartości
//            return getDefaultStats();
//        }
//
//        return stats;
//    }


    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getParticipantStatistics(Long meetingId) {
        log.info("Getting participant statistics for meeting: {}", meetingId);

        Map<String, Long> stats = new HashMap<>();

        try {
            // ✅ Pobierz spotkanie
            Meeting meeting = meetingRepository.findById(meetingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

            // ✅ Podstawowe statystyki z bazy
            long totalFromDb = participantRepository.countByMeetingId(meetingId);
            long confirmedFromDb = participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.CONFIRMED);

            // ✅ Sprawdź czy organizator jest już w uczestnikach
            boolean organizerIsParticipant = participantRepository
                    .findByMeetingIdAndUserId(meetingId, meeting.getOrganizer().getId())
                    .isPresent();

            // ✅ DOSTOSUJ STATYSTYKI - uwzględnij organizatora
            stats.put("total", organizerIsParticipant ? totalFromDb : totalFromDb + 1);
            stats.put("confirmed", organizerIsParticipant ? confirmedFromDb : confirmedFromDb + 1);
            stats.put("pending", participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.PENDING));
            stats.put("invited", participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.INVITED));
            stats.put("waiting", participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.WAITING_LIST));
            stats.put("declined", participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.DECLINED));
            stats.put("organizerIncluded", organizerIsParticipant ? 1L : 0L); // dla debugu

            log.info("Participant stats for meeting {}: {} (organizer included: {})",
                    meetingId, stats, !organizerIsParticipant);

        } catch (Exception e) {
            log.error("Error getting participant statistics for meeting {}: {}", meetingId, e.getMessage(), e);
            return getDefaultStats();
        }

        return stats;
    }

    private Map<String, Long> getDefaultStats() {
        Map<String, Long> defaultStats = new HashMap<>();
        defaultStats.put("total", 0L);
        defaultStats.put("confirmed", 0L);
        defaultStats.put("pending", 0L);
        defaultStats.put("invited", 0L);
        defaultStats.put("waiting", 0L);
        defaultStats.put("declined", 0L);
        return defaultStats;
    }


    @Override
    public boolean hasAccessToMeeting(Long meetingId, Long userId) {
        // Sprawdź czy użytkownik jest organizatorem lub uczestnikiem
        if (isOrganizer(meetingId, userId)) {
            return true;
        }
        return isUserParticipant(meetingId, userId);
    }

    @Override
    public boolean isOrganizer(Long meetingId, Long userId) {
        try {
            Meeting meeting = meetingRepository.findById(meetingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
            return meeting.getOrganizer().getId().equals(userId);
        } catch (Exception e) {
            log.error("Error checking if user {} is organizer of meeting {}", userId, meetingId, e);
            return false;
        }
    }

    @Override
    public boolean canEditParticipant(Long meetingId, Long participantId, Long userId) {
        // Tylko organizator może edytować uczestników
        return isOrganizer(meetingId, userId);
    }

    @Override
    public boolean canRemoveParticipant(Long meetingId, Long participantId, Long userId) {
        // Tylko organizator może usuwać uczestników
        return isOrganizer(meetingId, userId);
    }

    @Override
    public List<ParticipantResponse> inviteParticipants(Long meetingId, InviteParticipantsRequest request) {
        // Deleguj do istniejącej metody
        List<MeetingParticipant> invited = inviteMultipleParticipants(meetingId, request, getCurrentUserId());
        return invited.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ParticipantResponse getParticipant(Long participantId) {
        MeetingParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));
        return mapToResponse(participant);
    }



    @Override
    public void removeParticipant(Long participantId) {
        participantRepository.deleteById(participantId);
    }

    @Override
    public ParticipantResponse confirmParticipation(String token, String comment) {
        MeetingParticipant participant = acceptInvitationByToken(token);
        return mapToResponse(participant);
    }

    @Override
    public ParticipantResponse declineParticipation(String token, String comment) {
        MeetingParticipant participant = participantRepository.findByInvitationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid invitation token"));

        participant.setStatus(ParticipationStatus.DECLINED);
        participant.setComment(comment);
        participant.setResponseDate(LocalDateTime.now());

        MeetingParticipant updated = participantRepository.save(participant);
        return mapToResponse(updated);
    }

    @Override
    public ParticipantResponse setTentative(String token, String comment) {
        MeetingParticipant participant = participantRepository.findByInvitationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid invitation token"));

        participant.setStatus(ParticipationStatus.TENTATIVE);
        participant.setComment(comment);
        participant.setResponseDate(LocalDateTime.now());

        MeetingParticipant updated = participantRepository.save(participant);
        return mapToResponse(updated);
    }

    @Override
    public MeetingParticipantService.ParticipantStats getMeetingStats(Long meetingId) {
        Map<String, Long> stats = getParticipantStatistics(meetingId);

        return new MeetingParticipantService.ParticipantStats() {
            @Override
            public long getTotalInvited() {
                return stats.getOrDefault("invited", 0L);
            }

            @Override
            public long getTotalConfirmed() {
                return stats.getOrDefault("confirmed", 0L);
            }

            @Override
            public long getWaitlistCount() {
                return stats.getOrDefault("waiting", 0L);
            }

            @Override
            public long getPendingCount() {
                return stats.getOrDefault("pending", 0L);
            }
        };
    }

    @Override
    public Map<String, Object> getDetailedStats(Long meetingId) {
        Map<String, Long> basicStats = getParticipantStatistics(meetingId);
        List<ParticipantResponse> participants = getMeetingParticipants(meetingId);

        Map<String, Object> detailedStats = new HashMap<>(basicStats);
        detailedStats.put("participants", participants);
        detailedStats.put("meetingId", meetingId);

        return detailedStats;
    }

    @Override
    public ByteArrayResource exportParticipantsToCsv(Long meetingId) {
        // Implementacja eksportu do CSV
        List<ParticipantResponse> participants = getMeetingParticipants(meetingId);
        StringBuilder csv = new StringBuilder();

        // Nagłówek
        csv.append("ID,Imię,Nazwisko,Email,Status,Uprawnienia,Data odpowiedzi\n");

        // Dane
        for (ParticipantResponse p : participants) {
            csv.append(p.getId()).append(",");
            csv.append(p.getUser().getFirstName()).append(",");
            csv.append(p.getUser().getLastName()).append(",");
            csv.append(p.getUser().getEmail()).append(",");
            csv.append(p.getStatus()).append(",");
            csv.append(p.getPermissionLevel()).append(",");
            csv.append(p.getResponseDate() != null ? p.getResponseDate().toString() : "").append("\n");
        }

        return new ByteArrayResource(csv.toString().getBytes());
    }

    private Long getCurrentUserId() {
        // Implementacja pobierania ID aktualnego użytkownika
        // To zależy od Twojego systemu autentykacji
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            // Pobierz ID użytkownika
            return ((CustomUserDetailsService.CustomUserDetails) authentication.getPrincipal()).getId();
        }
        throw new SecurityException("User not authenticated");
    }

}

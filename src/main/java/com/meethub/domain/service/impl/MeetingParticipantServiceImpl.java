package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.*;
import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.model.enums.PermissionLevel;
import com.meethub.domain.model.request.InviteParticipantsRequest;
import com.meethub.domain.model.response.ParticipantResponse;
import com.meethub.domain.model.response.UserResponse;
import com.meethub.domain.repository.jpa.*;
import com.meethub.domain.service.EmailService;
import com.meethub.domain.service.MeetingParticipantService;
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
    private final MeetingParticipantRepository meetingParticipantRepository;

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

    @Override
    public MeetingParticipant joinPublicMeeting(Long meetingId, Long userId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        // Sprawdź czy spotkanie jest publiczne
        if (!meeting.getVisibility().name().equals("PUBLIC")) {
            throw new SecurityException("Meeting is not public");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Sprawdź czy już nie jest uczestnikiem
        if (participantRepository.findByMeetingIdAndUserId(meetingId, userId).isPresent()) {
            throw new IllegalArgumentException("User is already a participant");
        }

        MeetingParticipant participant = new MeetingParticipant(meeting, user);
        participant.setStatus(ParticipationStatus.CONFIRMED);
        return participantRepository.save(participant);
    }

    @Override
    public MeetingParticipant acceptInvitationByToken(String token) {
        MeetingParticipant participant = participantRepository.findByInvitationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid invitation token"));

        if (participant.getTokenExpiresAt() != null &&
                participant.getTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Invitation token has expired");
        }

        // Sprawdź czy spotkanie ma jeszcze miejsce
        if (isMeetingFull(participant.getMeeting().getId())) {
            // Jeśli jest pełne, dodaj do listy oczekujących zamiast zgłaszać błąd
            if (!isOnWaitlist(participant.getMeeting().getId(), participant.getUser().getId())) {
                addToWaitlist(participant.getMeeting(), participant.getUser());
            }
            throw new IllegalArgumentException("Meeting is full. You have been added to waitlist.");
        }

        ParticipationStatus oldStatus = participant.getStatus();
        participant.setStatus(ParticipationStatus.CONFIRMED);
        participant.setResponseDate(LocalDateTime.now());
        participant.setInvitationToken(null); // Unieważnij token po użyciu

        MeetingParticipant updatedParticipant = participantRepository.save(participant);

        // Zapisz historię statusu
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

    private ParticipantResponse mapToResponse(MeetingParticipant participant) {
        ParticipantResponse response = new ParticipantResponse();
        response.setId(participant.getId());
        response.setStatus(participant.getStatus());
        response.setPermissionLevel(participant.getPermissionLevel());
        response.setCreatedAt(participant.getCreatedAt());
        return response;
    }

    @Override
    public void joinMeeting(Long userId, Long meetingId) {
        joinPublicMeeting(meetingId, userId);
    }

    @Override
    public void leaveMeeting(Long userId, Long meetingId) {
        MeetingParticipant participant = participantRepository.findByMeetingIdAndUserId(meetingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));

        // Sprawdź czy uczestnik był potwierdzony
        boolean wasConfirmed = participant.getStatus() == ParticipationStatus.CONFIRMED;

        participantRepository.delete(participant);

        // Jeśli był potwierdzony i spotkanie ma limit, promuj następną osobę z waitlist
        if (wasConfirmed) {
            promoteNextFromWaitlist(meetingId);
        }
    }

    /**
     * Promuj następną osobę z listy oczekujących gdy zwolni się miejsce
     */
    private void promoteNextFromWaitlist(Long meetingId) {
        if (isMeetingFull(meetingId)) {
            return; // Nadal pełne, nie promuj
        }

        Optional<WaitlistEntry> nextEntry = waitlistEntryRepository
                .findFirstByMeetingIdOrderByPositionAsc(meetingId);

        if (nextEntry.isPresent()) {
            WaitlistEntry entry = nextEntry.get();

            // Znajdź lub utwórz uczestnika
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

            // Usuń z listy oczekujących
            removeFromWaitlist(meetingId, entry.getUser().getId());

            // Wyślij powiadomienie o promocji
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

            log.info("Promoted user {} from waitlist for meeting {}", entry.getUser().getId(), meetingId);
        }
    }


    private boolean isMeetingFull(Long meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        if (meeting.getMaxParticipants() == null) {
            return false; // Brak limitu
        }

        long confirmedCount = participantRepository.countByMeetingIdAndStatus(
                meetingId, ParticipationStatus.CONFIRMED);

        return confirmedCount >= meeting.getMaxParticipants();
    }

    private MeetingParticipant addToWaitlist(Meeting meeting, User user) {
        // Sprawdź czy już jest na liście oczekujących
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

        // Utwórz uczestnika ze statusem WAITING_LIST (poprawna nazwa zgodna z Twoim kodem)
        MeetingParticipant participant = MeetingParticipant.builder()
                .meeting(meeting)
                .user(user)
                .status(ParticipationStatus.WAITING_LIST) // Używaj WAITING_LIST zamiast WAITLIST
                .permissionLevel(PermissionLevel.PARTICIPANT)
                .build();

        MeetingParticipant savedParticipant = participantRepository.save(participant);

        // Wyślij powiadomienie o dodaniu do listy oczekujących
        try {
            emailService.sendTemplateEmail(
                    user.getEmail(),
                    "Zostałeś dodany do listy oczekujących: " + meeting.getTitle(),
                    "waitlist-notification",
                    Map.of(
                            "meetingTitle", meeting.getTitle(),
                            "position", nextPosition,
                            "organizerName", meeting.getOrganizer().getFullName()
                    )
            );
        } catch (Exception e) {
            log.warn("Failed to send waitlist notification to {}", user.getEmail(), e);
        }

        return savedParticipant;
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

    // Nowe metody dla grup uczestników
    @Transactional
    public void inviteUserGroup(Long meetingId, String groupName, Long organizerId) {
        Meeting meeting = meetingRepository.findByIdAndOrganizerId(meetingId, organizerId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found or access denied"));

        // Znajdź użytkowników w grupie
        List<User> groupUsers = userRepository.findByGroupName(groupName);

        if (groupUsers.isEmpty()) {
            log.warn("No users found in group: {}", groupName);
            return;
        }

        log.info("Inviting {} users from group {} to meeting {}", groupUsers.size(), groupName, meetingId);

        int successCount = 0;
        int failCount = 0;

        for (User user : groupUsers) {
            try {
                // Sprawdź czy już nie jest uczestnikiem
                if (!participantRepository.existsByMeetingIdAndUserId(meetingId, user.getId())) {
                    inviteParticipant(meetingId, user.getId(), organizerId);
                    successCount++;
                } else {
                    log.debug("User {} is already a participant of meeting {}", user.getEmail(), meetingId);
                }
            } catch (Exception e) {
                log.warn("Failed to invite user {} to meeting {}: {}",
                        user.getEmail(), meetingId, e.getMessage());
                failCount++;
            }
        }

        log.info("Group invitation completed: {} successful, {} failed", successCount, failCount);
    }


    /**
     * Sprawdza czy użytkownik jest na liście oczekujących dla danego spotkania
     */
    private boolean isOnWaitlist(Long meetingId, Long userId) {
        if (meetingId == null || userId == null) {
            return false;
        }

        // Sprawdź bezpośrednio w repozytorium listy oczekujących
        boolean onWaitlist = waitlistEntryRepository.existsByMeetingIdAndUserId(meetingId, userId);

        if (onWaitlist) {
            return true;
        }

        // Dodatkowe sprawdzenie przez status uczestnika (dla bezpieczeństwa)
        return participantRepository.findByMeetingIdAndUserId(meetingId, userId)
                .map(participant ->
                        participant.getStatus() == ParticipationStatus.WAITING_LIST)
                .orElse(false);
    }

    /**
     * Usuwa użytkownika z listy oczekujących i aktualizuje pozycje innych
     */
    @Transactional
    private void removeFromWaitlist(Long meetingId, Long userId) {
        if (meetingId == null || userId == null) {
            log.warn("Attempted to remove from waitlist with null meetingId or userId");
            return;
        }

        try {
            log.info("Removing user {} from waitlist for meeting {}", userId, meetingId);

            // 1. Znajdź pozycję usuwanego użytkownika
            Optional<WaitlistEntry> waitlistEntryOpt = waitlistEntryRepository
                    .findByMeetingIdAndUserId(meetingId, userId);

            if (waitlistEntryOpt.isEmpty()) {
                log.warn("User {} not found on waitlist for meeting {}", userId, meetingId);
                return;
            }

            WaitlistEntry waitlistEntry = waitlistEntryOpt.get();
            Integer removedPosition = waitlistEntry.getPosition();

            // 2. Usuń wpis z listy oczekujących
            waitlistEntryRepository.delete(waitlistEntry);
            log.debug("Deleted waitlist entry for user {} at position {}", userId, removedPosition);

            // 3. Zaktualizuj pozycje pozostałych użytkowników (zmniejsz o 1 tych z wyższą pozycją)
            if (removedPosition != null) {
                List<WaitlistEntry> entriesToUpdate = waitlistEntryRepository
                        .findByMeetingIdAndPositionGreaterThan(meetingId, removedPosition);

                for (WaitlistEntry entry : entriesToUpdate) {
                    entry.setPosition(entry.getPosition() - 1);
                    waitlistEntryRepository.save(entry);
                }
                log.debug("Updated positions for {} waitlist entries after removal", entriesToUpdate.size());
            }

            log.info("Successfully removed user {} from waitlist for meeting {}", userId, meetingId);

        } catch (Exception e) {
            log.error("Error removing user {} from waitlist for meeting {}: {}",
                    userId, meetingId, e.getMessage(), e);
            throw new RuntimeException("Failed to remove from waitlist: " + e.getMessage(), e);
        }
    }


    /**
     * Sprawdza czy użytkownik ma uprawnienia do zmiany statusu uczestnika
     */
    private boolean hasPermissionToUpdateStatus(MeetingParticipant participant, Long userId) {
        if (userId == null) {
            return false;
        }

        // 1. Organizator spotkania może zmieniać status wszystkim uczestnikom
        if (participant.getMeeting().getOrganizer().getId().equals(userId)) {
            return true;
        }

        // 2. Użytkownik może zmieniać własny status
        if (participant.getUser().getId().equals(userId)) {
            return true;
        }

        // 3. Sprawdź czy użytkownik jest moderatorem tego spotkania
        Optional<MeetingParticipant> userParticipant = participantRepository.findByMeetingIdAndUserId(
                participant.getMeeting().getId(), userId);

        if (userParticipant.isPresent()) {
            PermissionLevel userPermission = userParticipant.get().getPermissionLevel();
            return userPermission == PermissionLevel.MODERATOR;
        }

        return false;
    }


    @Override
    public List<UserResponse> searchUsersForInvitation(String query, Long meetingId) {
        // Przykładowa implementacja - dostosuj do swoich repozytoriów
        List<User> users = userRepository.findByEmailContainingOrFirstNameContainingOrLastNameContaining(
                query, query, query);

        return users.stream()
                .filter(user -> !isUserAlreadyParticipant(meetingId, user.getId()))
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    private boolean isUserAlreadyParticipant(Long meetingId, Long userId) {
        return meetingParticipantRepository.existsByMeetingIdAndUserId(meetingId, userId);
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


}
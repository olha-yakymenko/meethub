//// src/main/java/com/meethub/domain/service/impl/ParticipantServiceImpl.java
//package com.meethub.domain.service.impl;
//
//import com.meethub.domain.model.entity.Meeting;
//import com.meethub.domain.model.entity.MeetingParticipant;
//import com.meethub.domain.model.entity.User;
//import com.meethub.domain.model.enums.MeetingVisibility;
//import com.meethub.domain.model.enums.ParticipationStatus;
//import com.meethub.domain.model.enums.PermissionLevel;
//import com.meethub.domain.model.request.InviteParticipantsRequest;
//import com.meethub.domain.model.request.UpdateParticipantRequest;
//import com.meethub.domain.model.response.ParticipantResponse;
//import com.meethub.domain.model.response.UserResponse;
//import com.meethub.domain.repository.jpa.MeetingParticipantRepository;
//import com.meethub.domain.repository.jpa.MeetingRepository;
//import com.meethub.domain.repository.jpa.UserRepository;
//import com.meethub.domain.service.NotificationService;
//import com.meethub.domain.service.ParticipantService;
//import com.meethub.exception.ResourceNotFoundException;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Optional;
//import java.util.UUID;
//import java.util.stream.Collectors;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class ParticipantServiceImpl implements ParticipantService {
//
//    private final MeetingParticipantRepository participantRepository;
//    private final MeetingRepository meetingRepository;
//    private final UserRepository userRepository;
//    private final NotificationService notificationService;
//
//    @Override
//    @Transactional
//    public List<ParticipantResponse> inviteParticipants(Long meetingId, InviteParticipantsRequest request) {
//        Meeting meeting = meetingRepository.findById(meetingId)
//                .orElseThrow(() -> new IllegalArgumentException("Spotkanie nie znalezione"));
//
//        return request.getUserIds().stream()
//                .map(userId -> {
//                    User user = userRepository.findById(userId)
//                            .orElseThrow(() -> new IllegalArgumentException("Użytkownik nie znaleziony: " + userId));
//
//                    // Sprawdź czy użytkownik już jest zaproszony
//                    if (participantRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
//                        throw new IllegalArgumentException("Użytkownik " + user.getEmail() + " już jest zaproszony");
//                    }
//
//                    MeetingParticipant participant = createParticipant(meeting, user);
//                    MeetingParticipant saved = participantRepository.save(participant);
//
//                    log.info("Zaproszono użytkownika {} na spotkanie {}", user.getEmail(), meeting.getTitle());
//                    return mapToResponse(saved);
//                })
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    @Transactional
//    public ParticipantResponse updateParticipant(Long participantId, UpdateParticipantRequest request) {
//        MeetingParticipant participant = participantRepository.findById(participantId)
//                .orElseThrow(() -> new IllegalArgumentException("Uczestnik nie znaleziony"));
//
//        if (request.getStatus() != null) {
//            participant.setStatus(request.getStatus());
//            participant.setResponseDate(LocalDateTime.now());
//        }
//
//        if (request.getPermissionLevel() != null) {
//            participant.setPermissionLevel(request.getPermissionLevel());
//        }
//
//        if (request.getComment() != null) {
//            participant.setComment(request.getComment());
//        }
//
//        MeetingParticipant updated = participantRepository.save(participant);
//        return mapToResponse(updated);
//    }
//
//    @Override
//    @Transactional
//    public ParticipantResponse confirmParticipation(String token, String comment) {
//        MeetingParticipant participant = participantRepository.findByInvitationToken(token)
//                .orElseThrow(() -> new IllegalArgumentException("Nieprawidłowy token zaproszenia"));
//
//        if (!isTokenValid(participant)) {
//            throw new IllegalArgumentException("Token wygasł");
//        }
//
//        participant.confirmParticipation();
//        participant.setComment(comment);
//        participant.setInvitationToken(null); // Zużyj token
//
//        MeetingParticipant updated = participantRepository.save(participant);
//        log.info("Użytkownik {} potwierdził udział w spotkaniu {}",
//                participant.getUser().getEmail(), participant.getMeeting().getTitle());
//
//        return mapToResponse(updated);
//    }
//
//    @Override
//    @Transactional
//    public ParticipantResponse declineParticipation(String token, String comment) {
//        MeetingParticipant participant = participantRepository.findByInvitationToken(token)
//                .orElseThrow(() -> new IllegalArgumentException("Nieprawidłowy token zaproszenia"));
//
//        if (!isTokenValid(participant)) {
//            throw new IllegalArgumentException("Token wygasł");
//        }
//
//        participant.declineParticipation();
//        participant.setComment(comment);
//        participant.setInvitationToken(null);
//
//        MeetingParticipant updated = participantRepository.save(participant);
//        return mapToResponse(updated);
//    }
//
//    @Override
//    @Transactional
//    public ParticipantResponse setTentative(String token, String comment) {
//        MeetingParticipant participant = participantRepository.findByInvitationToken(token)
//                .orElseThrow(() -> new IllegalArgumentException("Nieprawidłowy token zaproszenia"));
//
//        if (!isTokenValid(participant)) {
//            throw new IllegalArgumentException("Token wygasł");
//        }
//
//        participant.setTentative();
//        participant.setComment(comment);
//
//        MeetingParticipant updated = participantRepository.save(participant);
//        return mapToResponse(updated);
//    }
//
////    @Override
////    @Transactional
////    public ParticipantResponse addToWaitlist(Long meetingId, Long userId) {
////        Meeting meeting = meetingRepository.findById(meetingId)
////                .orElseThrow(() -> new IllegalArgumentException("Spotkanie nie znalezione"));
////        User user = userRepository.findById(userId)
////                .orElseThrow(() -> new IllegalArgumentException("Użytkownik nie znaleziony"));
////
////        // Sprawdź czy już jest na liście
////        if (participantRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
////            throw new IllegalArgumentException("Użytkownik już jest na liście uczestników");
////        }
////
////        MeetingParticipant waitlisted = MeetingParticipant.builder()
////                .meeting(meeting)
////                .user(user)
////                .status(ParticipationStatus.WAITING_LIST)
////                .permissionLevel(PermissionLevel.PARTICIPANT)
////                .build();
////
////        MeetingParticipant saved = participantRepository.save(waitlisted);
////        return mapToResponse(saved);
////    }
//
//    // Metody pomocnicze
//    private MeetingParticipant createParticipant(Meeting meeting, User user) {
//        String token = UUID.randomUUID().toString();
//
//        return MeetingParticipant.builder()
//                .meeting(meeting)
//                .user(user)
//                .status(ParticipationStatus.INVITED)
//                .permissionLevel(PermissionLevel.PARTICIPANT)
//                .invitationToken(token)
//                .tokenExpiresAt(LocalDateTime.now().plusDays(7))
//                .build();
//    }
//
//    private boolean isTokenValid(MeetingParticipant participant) {
//        return participant.getTokenExpiresAt() != null &&
//                participant.getTokenExpiresAt().isAfter(LocalDateTime.now());
//    }
//
//    private ParticipantResponse mapToResponse(MeetingParticipant participant) {
//        return ParticipantResponse.builder()
//                .id(participant.getId())
//                .user(mapUserToResponse(participant.getUser()))
//                .status(participant.getStatus())
//                .permissionLevel(participant.getPermissionLevel())
//                .comment(participant.getComment())
//                .responseDate(participant.getResponseDate())
//                .createdAt(participant.getCreatedAt())
//                .updatedAt(participant.getUpdatedAt())
//                .build();
//    }
//
//    private UserResponse mapUserToResponse(User user) {
//        return UserResponse.builder()
//                .id(user.getId())
//                .email(user.getEmail())
//                .firstName(user.getFirstName())
//                .lastName(user.getLastName())
//                .phoneNumber(user.getPhoneNumber())
//                .role(user.getRole())
//                .createdAt(user.getCreatedAt())
//                .build();
//    }
//
//    // Implementacja pozostałych metod
//    @Override
//    public void removeParticipant(Long participantId) {
//        participantRepository.deleteById(participantId);
//    }
//
//    @Override
//    public ParticipantResponse getParticipant(Long participantId) {
//        MeetingParticipant participant = participantRepository.findById(participantId)
//                .orElseThrow(() -> new IllegalArgumentException("Uczestnik nie znaleziony"));
//        return mapToResponse(participant);
//    }
//
//    @Override
//    public List<ParticipantResponse> getMeetingParticipants(Long meetingId) {
//        return participantRepository.findByMeetingId(meetingId).stream()
//                .map(this::mapToResponse)
//                .collect(Collectors.toList());
//    }
////
////    @Override
////    public List<ParticipantResponse> getWaitlist(Long meetingId) {
////        return participantRepository.findByMeetingIdAndStatus(meetingId, ParticipationStatus.WAITING_LIST).stream()
////                .map(this::mapToResponse)
////                .collect(Collectors.toList());
////    }
////
////    @Override
////    public String generateInvitationToken(Long participantId) {
////        MeetingParticipant participant = participantRepository.findById(participantId)
////                .orElseThrow(() -> new IllegalArgumentException("Uczestnik nie znaleziony"));
////
////        String token = UUID.randomUUID().toString();
////        participant.setInvitationToken(token);
////        participant.setTokenExpiresAt(LocalDateTime.now().plusDays(7));
////
////        participantRepository.save(participant);
////        return token;
////    }
////
////    @Override
////    public boolean validateInvitationToken(String token) {
////        return participantRepository.findByInvitationToken(token)
////                .map(this::isTokenValid)
////                .orElse(false);
////    }
//
//    @Override
//    public ParticipantStats getMeetingStats(Long meetingId) {
//        return new ParticipantStats() {
//            @Override
//            public long getTotalInvited() {
//                return participantRepository.findByMeetingId(meetingId).size();
//            }
//
//            @Override
//            public long getTotalConfirmed() {
//                return (int) participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.CONFIRMED);
//            }
////
////            @Override
////            public int getTotalDeclined() {
////                return (int) participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.DECLINED);
////            }
//
//            @Override
//            public long getWaitlistCount() {
//                return (int) participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.WAITING_LIST);
//            }
//
////            @Override
////            public int getAvailableSpots() {
////                Meeting meeting = meetingRepository.findById(meetingId).orElse(null);
////                if (meeting == null || meeting.getMaxParticipants() == null) {
////                    return Integer.MAX_VALUE;
////                }
////                int confirmed = getTotalConfirmed();
////                return Math.max(0, meeting.getMaxParticipants() - confirmed);
////            }
//        };
//    }
//
//    @Override
//    public boolean hasAccessToMeeting(Long meetingId, Long userId) {
//        try {
//            Meeting meeting = meetingRepository.findById(meetingId)
//                    .orElseThrow(() -> new IllegalArgumentException("Spotkanie nie znalezione"));
//
//            // Organizer zawsze ma dostęp
//            if (meeting.getOrganizer().getId().equals(userId)) {
//                return true;
//            }
//
//            // Dla spotkań publicznych - każdy ma dostęp do podglądu uczestników
//            if (meeting.getVisibility() == MeetingVisibility.PUBLIC) {
//                return true;
//            }
//
//            // Dla spotkań prywatnych - tylko uczestnicy i organizator
//            return participantRepository.existsByMeetingIdAndUserId(meetingId, userId);
//
//        } catch (Exception e) {
//            log.error("Błąd podczas sprawdzania dostępu do spotkania: {}", e.getMessage());
//            return false;
//        }
//    }
//
//    @Override
//    public boolean isOrganizer(Long meetingId, Long userId) {
//        try {
//            Meeting meeting = meetingRepository.findById(meetingId)
//                    .orElseThrow(() -> new IllegalArgumentException("Spotkanie nie znalezione"));
//            return meeting.getOrganizer().getId().equals(userId);
//        } catch (Exception e) {
//            log.error("Błąd podczas sprawdzania czy użytkownik jest organizatorem: {}", e.getMessage());
//            return false;
//        }
//    }
//
//    @Override
//    public boolean canEditParticipant(Long meetingId, Long participantId, Long userId) {
//        try {
//            // Organizer może edytować każdego uczestnika
//            if (isOrganizer(meetingId, userId)) {
//                return true;
//            }
//
//            // Użytkownik może edytować swój własny udział
//            MeetingParticipant participant = participantRepository.findById(participantId)
//                    .orElseThrow(() -> new IllegalArgumentException("Uczestnik nie znaleziony"));
//
//            return participant.getUser().getId().equals(userId);
//
//        } catch (Exception e) {
//            log.error("Błąd podczas sprawdzania uprawnień edycji: {}", e.getMessage());
//            return false;
//        }
//    }
//
//    @Override
//    public boolean canRemoveParticipant(Long meetingId, Long participantId, Long userId) {
//        try {
//            // Tylko organizer może usuwać uczestników
//            if (!isOrganizer(meetingId, userId)) {
//                return false;
//            }
//
//            // Organizer nie może usunąć samego siebie
//            MeetingParticipant participant = participantRepository.findById(participantId)
//                    .orElseThrow(() -> new IllegalArgumentException("Uczestnik nie znaleziony"));
//
//            return !participant.getUser().getId().equals(userId);
//
//        } catch (Exception e) {
//            log.error("Błąd podczas sprawdzania uprawnień usuwania: {}", e.getMessage());
//            return false;
//        }
//    }
//
//    @Override
//    public boolean isUserParticipant(Long userId, Long meetingId) {
//        return participantRepository.existsByMeetingIdAndUserId(meetingId, userId);
//    }
////
////    @Override
////    public ParticipationStatus getUserParticipationStatus(Long userId, Long meetingId) {
////        Optional<MeetingParticipant> participant = participantRepository.findByMeetingIdAndUserId(meetingId, userId);
////        return participant.map(MeetingParticipant::getStatus).orElse(null);
////    }
//
//    // ========== DODATKOWE METODY POMOCNICZE ==========
//
//    @Transactional
//    @Override
//    public ParticipantResponse inviteParticipant(Long meetingId, Long userId, Long organizerId) {
//        // Sprawdź czy użytkownik ma uprawnienia do zapraszania
//        if (!isOrganizer(meetingId, organizerId)) {
//            throw new SecurityException("Tylko organizator może zapraszać uczestników");
//        }
//
//        Meeting meeting = meetingRepository.findById(meetingId)
//                .orElseThrow(() -> new IllegalArgumentException("Spotkanie nie znalezione"));
//
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new IllegalArgumentException("Użytkownik nie znaleziony"));
//
//        // Sprawdź czy użytkownik już jest zaproszony
//        if (participantRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
//            throw new IllegalArgumentException("Użytkownik " + user.getEmail() + " już jest zaproszony");
//        }
//
//        MeetingParticipant participant = createParticipant(meeting, user);
//        MeetingParticipant saved = participantRepository.save(participant);
//
//        log.info("Organizator {} zaprosił użytkownika {} na spotkanie {}",
//                organizerId, user.getEmail(), meeting.getTitle());
//
//        return mapToResponse(saved);
//    }
//
////    @Transactional
////    @Override
////    public void removeFromMeeting(Long meetingId, Long userId, Long removerId) {
////        // Sprawdź uprawnienia
////        if (!isOrganizer(meetingId, removerId) && !userId.equals(removerId)) {
////            throw new SecurityException("Nie masz uprawnień do usunięcia tego uczestnika");
////        }
////
////        Optional<MeetingParticipant> participant = participantRepository.findByMeetingIdAndUserId(meetingId, userId);
////        if (participant.isPresent()) {
////            participantRepository.delete(participant.get());
////            log.info("Usunięto użytkownika {} ze spotkania {}", userId, meetingId);
////        } else {
////            throw new IllegalArgumentException("Użytkownik nie jest uczestnikiem tego spotkania");
////        }
////    }
//
//    @Transactional
//    @Override
//    public ParticipantResponse joinMeeting(Long userId, Long meetingId) {
//        Meeting meeting = meetingRepository.findById(meetingId)
//                .orElseThrow(() -> new IllegalArgumentException("Spotkanie nie znalezione"));
//
//        // Sprawdź czy spotkanie pozwala na dołączanie
//        if (meeting.getVisibility() != MeetingVisibility.PUBLIC) {
//            throw new SecurityException("To spotkanie nie pozwala na publiczne dołączanie");
//        }
//
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new IllegalArgumentException("Użytkownik nie znaleziony"));
//
//        // Sprawdź czy użytkownik już jest zapisany
//        if (participantRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
//            throw new IllegalArgumentException("Już jesteś zapisany na to spotkanie");
//        }
//
//        MeetingParticipant participant = MeetingParticipant.builder()
//                .meeting(meeting)
//                .user(user)
//                .status(ParticipationStatus.CONFIRMED)
//                .permissionLevel(PermissionLevel.PARTICIPANT)
//                .responseDate(LocalDateTime.now())
//                .build();
//
//        MeetingParticipant saved = participantRepository.save(participant);
//        log.info("Użytkownik {} dołączył do spotkania {}", user.getEmail(), meeting.getTitle());
//
//        return mapToResponse(saved);
//    }
//
//    @Transactional
//    @Override
//    public void leaveMeeting(Long userId, Long meetingId) {
//        Optional<MeetingParticipant> participant = participantRepository.findByMeetingIdAndUserId(meetingId, userId);
//        if (participant.isPresent()) {
//            // Organizer nie może opuścić spotkania - musi je usunąć lub przekazać organizację
//            Meeting meeting = meetingRepository.findById(meetingId)
//                    .orElseThrow(() -> new IllegalArgumentException("Spotkanie nie znalezione"));
//
//            if (meeting.getOrganizer().getId().equals(userId)) {
//                throw new SecurityException("Organizator nie może opuścić spotkania. Usuń spotkanie lub przekaż organizację.");
//            }
//
//            participantRepository.delete(participant.get());
//            log.info("Użytkownik {} opuścił spotkanie {}", userId, meetingId);
//        } else {
//            throw new IllegalArgumentException("Nie jesteś uczestnikiem tego spotkania");
//        }
//    }
//
//
//    // W ParticipantServiceImpl.java dodaj te metody:
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
//        try {
//            notificationService.sendParticipantJoinedNotification(meeting.getOrganizer(), user, meeting);
//        } catch (Exception e) {
//            log.warn("Failed to send notification: {}", e.getMessage());
//        }
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
//        try {
//            notificationService.sendJoinRequestNotification(meeting.getOrganizer(), user, meeting);
//        } catch (Exception e) {
//            log.warn("Failed to send notification: {}", e.getMessage());
//        }
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
////        saveStatusHistory(participant, oldStatus, ParticipationStatus.CONFIRMED,
////                "Join request approved by organizer", organizerId);
//
//        // Wyślij powiadomienie do użytkownika
//        try {
//            notificationService.sendRequestApprovedNotification(participant.getUser(), meeting);
//        } catch (Exception e) {
//            log.warn("Failed to send notification: {}", e.getMessage());
//        }
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
//        // Zmień status na odrzucony
//        participant.setStatus(ParticipationStatus.DECLINED);
//        participant.setResponseDate(LocalDateTime.now());
//        participantRepository.save(participant);
//
//        // Zapisz historię
////        saveStatusHistory(participant, oldStatus, ParticipationStatus.DECLINED,
////                "Join request rejected by organizer", organizerId);
//
//        // Wyślij powiadomienie do użytkownika
//        try {
//            notificationService.sendRequestRejectedNotification(participant.getUser(), meeting);
//        } catch (Exception e) {
//            log.warn("Failed to send notification: {}", e.getMessage());
//        }
//
//        log.info("Join request rejected for participant {} in meeting {}", participantId, meetingId);
//    }
//
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
//}



















package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.MeetingParticipant;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.MeetingVisibility;
import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.model.enums.PermissionLevel;
import com.meethub.domain.model.request.InviteParticipantsRequest;
import com.meethub.domain.model.request.UpdateParticipantRequest;
import com.meethub.domain.model.response.ParticipantResponse;
import com.meethub.domain.model.response.UserResponse;
import com.meethub.domain.repository.jpa.MeetingParticipantRepository;
import com.meethub.domain.repository.jpa.MeetingRepository;
import com.meethub.domain.repository.jpa.UserRepository;
import com.meethub.domain.service.NotificationService;
import com.meethub.domain.service.ParticipantService;
import com.meethub.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipantServiceImpl implements ParticipantService {

    private final MeetingParticipantRepository participantRepository;
    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // ========== IMPLEMENTACJE METOD Z INTERFEJSU ==========

    @Override
    @Transactional(readOnly = true)
    public List<ParticipantResponse> getMeetingParticipants(Long meetingId) {
        return participantRepository.findByMeetingId(meetingId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MeetingParticipant inviteParticipant(Long meetingId, Long userId, Long organizerId) {
        // Sprawdź czy użytkownik ma uprawnienia do zapraszania
        if (!isOrganizer(meetingId, organizerId)) {
            throw new SecurityException("Tylko organizator może zapraszać uczestników");
        }

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("Spotkanie nie znalezione"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Użytkownik nie znaleziony"));

        // Sprawdź czy użytkownik już jest zaproszony
        if (participantRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
            throw new IllegalArgumentException("Użytkownik " + user.getEmail() + " już jest zaproszony");
        }

        MeetingParticipant participant = createParticipant(meeting, user);
        MeetingParticipant saved = participantRepository.save(participant);

        log.info("Organizator {} zaprosił użytkownika {} na spotkanie {}",
                organizerId, user.getEmail(), meeting.getTitle());

        return saved;
    }

    @Override
    @Transactional
    public List<MeetingParticipant> inviteMultipleParticipants(Long meetingId, InviteParticipantsRequest request, Long organizerId) {
        return request.getUserIds().stream()
                .map(userId -> inviteParticipant(meetingId, userId, organizerId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MeetingParticipant updateParticipantStatus(Long meetingId, Long participantId, ParticipationStatus status, String comment, Long userId) {
        MeetingParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new IllegalArgumentException("Uczestnik nie znaleziony"));

        // Sprawdź uprawnienia
        if (!canEditParticipant(meetingId, participantId, userId)) {
            throw new SecurityException("Nie masz uprawnień do edycji tego uczestnika");
        }

        participant.setStatus(status);
        participant.setComment(comment);
        participant.setResponseDate(LocalDateTime.now());

        return participantRepository.save(participant);
    }

    @Override
    @Transactional
    public MeetingParticipant updateParticipantPermission(Long meetingId, Long participantId, PermissionLevel permissionLevel, Long organizerId) {
        // Sprawdź czy użytkownik jest organizatorem
        if (!isOrganizer(meetingId, organizerId)) {
            throw new SecurityException("Tylko organizator może zmieniać uprawnienia");
        }

        MeetingParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new IllegalArgumentException("Uczestnik nie znaleziony"));

        participant.setPermissionLevel(permissionLevel);
        return participantRepository.save(participant);
    }

    @Override
    @Transactional
    public void removeParticipant(Long meetingId, Long participantId, Long organizerId) {
        // Sprawdź czy użytkownik jest organizatorem
        if (!isOrganizer(meetingId, organizerId)) {
            throw new SecurityException("Tylko organizator może usuwać uczestników");
        }

        participantRepository.deleteById(participantId);
    }

    @Override
    @Transactional(noRollbackFor = {SecurityException.class, IllegalArgumentException.class})
    public MeetingParticipant joinPublicMeeting(Long meetingId, Long userId) {
        log.info("🎯 === JOIN PUBLIC MEETING START ===");
        log.info("📊 Meeting ID: {}, User ID: {}", meetingId, userId);

        try {
            log.info("🔍 Step 1: Searching for meeting with ID: {}", meetingId);
            Meeting meeting = meetingRepository.findById(meetingId)
                    .orElseThrow(() -> {
                        log.error("❌ Meeting not found with ID: {}", meetingId);
                        return new ResourceNotFoundException("Spotkanie nie zostało znalezione");
                    });
            log.info("✅ Meeting found: ID={}, Title='{}', Visibility={}",
                    meeting.getId(), meeting.getTitle(), meeting.getVisibility());

            // Sprawdź czy spotkanie jest publiczne
            log.info("🔍 Step 2: Checking meeting visibility");
            if (meeting.getVisibility() != MeetingVisibility.PUBLIC) {
                log.warn("⚠️ Meeting is not PUBLIC. Actual visibility: {}", meeting.getVisibility());
                throw new SecurityException("To spotkanie nie jest publiczne");
            }
            log.info("✅ Meeting is PUBLIC - proceeding");

            log.info("🔍 Step 3: Searching for user with ID: {}", userId);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.error("❌ User not found with ID: {}", userId);
                        return new ResourceNotFoundException("Użytkownik nie został znaleziony");
                    });
            log.info("✅ User found: ID={}, Email='{}'", user.getId(), user.getEmail());

            // Sprawdź czy już nie jest uczestnikiem
            log.info("🔍 Step 4: Checking if user is already a participant");
            Optional<MeetingParticipant> existingParticipant = participantRepository.findByMeetingIdAndUserId(meetingId, userId);
            if (existingParticipant.isPresent()) {
                MeetingParticipant participant = existingParticipant.get();
                log.info("📋 Existing participant found: ID={}, Status={}",
                        participant.getId(), participant.getStatus());

                if (participant.getStatus() == ParticipationStatus.CONFIRMED) {
                    log.warn("⚠️ User is already CONFIRMED participant");
                    throw new IllegalArgumentException("Jesteś już uczestnikiem tego spotkania");
                }

                // Jeśli jest zaproszony lub oczekujący, zmień status na potwierdzony
                log.info("🔄 Updating existing participant status from {} to CONFIRMED", participant.getStatus());
                participant.setStatus(ParticipationStatus.CONFIRMED);
                participant.setResponseDate(LocalDateTime.now());
                MeetingParticipant updated = participantRepository.save(participant);
                log.info("✅ Existing participant updated successfully");
                return updated;
            }
            log.info("✅ No existing participant found - creating new one");

            // Sprawdź czy są jeszcze miejsca
            log.info("🔍 Step 5: Checking available spots");
            boolean hasSpots = hasAvailableSpots(meetingId);
            log.info("📊 Available spots check result: {}", hasSpots);

            if (!hasSpots) {
                log.warn("⚠️ No available spots for meeting: {}", meetingId);
                throw new IllegalArgumentException("Brak wolnych miejsc na tym spotkaniu");
            }
            log.info("✅ Spots available - proceeding");

            // Dodaj użytkownika jako potwierdzonego uczestnika
            log.info("🔍 Step 6: Creating new participant");
            MeetingParticipant participant = MeetingParticipant.builder()
                    .meeting(meeting)
                    .user(user)
                    .status(ParticipationStatus.CONFIRMED)
                    .permissionLevel(PermissionLevel.PARTICIPANT)
                    .responseDate(LocalDateTime.now())
                    .build();
            log.info("📝 Participant object created: Meeting={}, User={}, Status=CONFIRMED",
                    meeting.getId(), user.getId());

            log.info("💾 Step 7: Saving participant to database");
            MeetingParticipant savedParticipant = participantRepository.save(participant);
            log.info("✅ Participant saved successfully: ID={}", savedParticipant.getId());

            log.info("🎉 User {} successfully joined public meeting {}", userId, meetingId);

            // WYŚLIJ POWIADOMIENIE PO ZAPISANIU UCZESTNIKA - BEZ WPŁYWU NA TRANSAKCJĘ

            log.info("🎯 === JOIN PUBLIC MEETING SUCCESS ===");
            return savedParticipant;

        } catch (SecurityException | IllegalArgumentException e) {
            log.warn("🚨 Business exception in joinPublicMeeting: {}", e.getMessage());
            log.info("🎯 === JOIN PUBLIC MEETING BUSINESS EXCEPTION ===");
            throw e;
        } catch (Exception e) {
            log.error("💥 UNEXPECTED ERROR in joinPublicMeeting - Meeting: {}, User: {}", meetingId, userId, e);
            log.info("🎯 === JOIN PUBLIC MEETING UNEXPECTED ERROR ===");
            throw e;
        }
    }


    @Override
    @Transactional(noRollbackFor = {SecurityException.class, IllegalArgumentException.class})
    public MeetingParticipant requestToJoinPrivateMeeting(Long meetingId, Long userId) {
        log.info("User {} requesting to join private meeting {}", userId, meetingId);

        try {
            Meeting meeting = meetingRepository.findById(meetingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Spotkanie nie zostało znalezione"));

            // Sprawdź czy spotkanie jest prywatne
            if (meeting.getVisibility() != MeetingVisibility.PRIVATE) {
                throw new SecurityException("To spotkanie nie jest prywatne");
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Użytkownik nie został znaleziony"));

            // Sprawdź czy użytkownik już nie wysłał prośby
            Optional<MeetingParticipant> existingParticipant = participantRepository.findByMeetingIdAndUserId(meetingId, userId);
            if (existingParticipant.isPresent()) {
                MeetingParticipant participant = existingParticipant.get();
                if (participant.getStatus() == ParticipationStatus.PENDING) {
                    throw new IllegalArgumentException("Już wysłałeś prośbę o dołączenie do tego spotkania");
                }
                if (participant.getStatus() == ParticipationStatus.CONFIRMED) {
                    throw new IllegalArgumentException("Jesteś już uczestnikiem tego spotkania");
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
            try {
                notificationService.sendJoinRequestNotification(meeting.getOrganizer(), user, meeting);
            } catch (Exception e) {
                log.warn("Failed to send notification: {}", e.getMessage());
            }

            return savedParticipant;

        } catch (Exception e) {
            log.error("Error requesting to join private meeting {} by user {}: {}", meetingId, userId, e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional(noRollbackFor = {SecurityException.class, IllegalArgumentException.class})
    public void approveJoinRequest(Long meetingId, Long participantId, Long organizerId) {
        log.info("🎯 === APPROVE JOIN REQUEST START ===");
        log.info("📊 Meeting: {}, Participant: {}, Organizer: {}", meetingId, participantId, organizerId);

        try {
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

            log.info("✅ All checks passed - approving participant");
            participant.setStatus(ParticipationStatus.CONFIRMED);
            participantRepository.save(participant);
            log.info("✅ Participant approved successfully");

            // Wyślij powiadomienie do użytkownika - BEZ WPŁYWU NA TRANSAKCJĘ
            log.info("🔔 Scheduling notification (non-critical)");

            log.info("🎯 === APPROVE JOIN REQUEST SUCCESS ===");

        } catch (SecurityException | IllegalArgumentException e) {
            log.warn("🚨 Business exception in approveJoinRequest: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("💥 UNEXPECTED ERROR in approveJoinRequest: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional(noRollbackFor = {SecurityException.class, IllegalArgumentException.class})
    public void rejectJoinRequest(Long meetingId, Long participantId, Long organizerId) {
        log.info("🎯 === REJECT JOIN REQUEST START ===");
        log.info("📊 Meeting: {}, Participant: {}, Organizer: {}", meetingId, participantId, organizerId);

        try {
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

            log.info("✅ All checks passed - rejecting participant");
            participant.setStatus(ParticipationStatus.DECLINED);
            participant.setResponseDate(LocalDateTime.now());
            participantRepository.save(participant);
            log.info("✅ Participant rejected successfully");

            // Wyślij powiadomienie do użytkownika - BEZ WPŁYWU NA TRANSAKCJĘ
            log.info("🔔 Scheduling notification (non-critical)");

            log.info("🎯 === REJECT JOIN REQUEST SUCCESS ===");

        } catch (SecurityException | IllegalArgumentException e) {
            log.warn("🚨 Business exception in rejectJoinRequest: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("💥 UNEXPECTED ERROR in rejectJoinRequest: {}", e.getMessage(), e);
            throw e;
        }
    }



    @Override
    @Transactional(readOnly = true)
    public boolean hasAccessToMeeting(Long meetingId, Long userId) {
        try {
            Meeting meeting = meetingRepository.findById(meetingId)
                    .orElseThrow(() -> new IllegalArgumentException("Spotkanie nie znalezione"));

            // Organizer zawsze ma dostęp
            if (meeting.getOrganizer().getId().equals(userId)) {
                return true;
            }

            // Dla spotkań publicznych - każdy ma dostęp do podglądu uczestników
            if (meeting.getVisibility() == MeetingVisibility.PUBLIC) {
                return true;
            }

            // Dla spotkań prywatnych - tylko uczestnicy i organizator
            return participantRepository.existsByMeetingIdAndUserId(meetingId, userId);

        } catch (Exception e) {
            log.error("Błąd podczas sprawdzania dostępu do spotkania: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isOrganizer(Long meetingId, Long userId) {
        try {
            Meeting meeting = meetingRepository.findById(meetingId)
                    .orElseThrow(() -> new IllegalArgumentException("Spotkanie nie znalezione"));
            return meeting.getOrganizer().getId().equals(userId);
        } catch (Exception e) {
            log.error("Błąd podczas sprawdzania czy użytkownik jest organizatorem: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ParticipantStats getMeetingStats(Long meetingId) {
        return new ParticipantStats() {
            @Override
            public long getTotalInvited() {
                return participantRepository.findByMeetingId(meetingId).size();
            }

            @Override
            public long getTotalConfirmed() {
                return participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.CONFIRMED);
            }

            @Override
            public long getWaitlistCount() {
                return participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.WAITING_LIST);
            }

            @Override
            public long getPendingCount() {
                return participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.PENDING);
            }
        };
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canEditParticipant(Long meetingId, Long participantId, Long userId) {
        try {
            // Organizer może edytować każdego uczestnika
            if (isOrganizer(meetingId, userId)) {
                return true;
            }

            // Użytkownik może edytować swój własny udział
            MeetingParticipant participant = participantRepository.findById(participantId)
                    .orElseThrow(() -> new IllegalArgumentException("Uczestnik nie znaleziony"));

            return participant.getUser().getId().equals(userId);

        } catch (Exception e) {
            log.error("Błąd podczas sprawdzania uprawnień edycji: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canRemoveParticipant(Long meetingId, Long participantId, Long userId) {
        try {
            // Tylko organizer może usuwać uczestników
            if (!isOrganizer(meetingId, userId)) {
                return false;
            }

            // Organizer nie może usunąć samego siebie
            MeetingParticipant participant = participantRepository.findById(participantId)
                    .orElseThrow(() -> new IllegalArgumentException("Uczestnik nie znaleziony"));

            return !participant.getUser().getId().equals(userId);

        } catch (Exception e) {
            log.error("Błąd podczas sprawdzania uprawnień usuwania: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ParticipantResponse getParticipant(Long participantId) {
        MeetingParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new IllegalArgumentException("Uczestnik nie znaleziony"));
        return mapToResponse(participant);
    }

    @Override
    @Transactional
    public List<ParticipantResponse> inviteParticipants(Long meetingId, InviteParticipantsRequest request) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("Spotkanie nie znalezione"));

        return request.getUserIds().stream()
                .map(userId -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("Użytkownik nie znaleziony: " + userId));

                    // Sprawdź czy użytkownik już jest zaproszony
                    if (participantRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
                        throw new IllegalArgumentException("Użytkownik " + user.getEmail() + " już jest zaproszony");
                    }

                    MeetingParticipant participant = createParticipant(meeting, user);
                    MeetingParticipant saved = participantRepository.save(participant);

                    log.info("Zaproszono użytkownika {} na spotkanie {}", user.getEmail(), meeting.getTitle());
                    return mapToResponse(saved);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParticipantResponse updateParticipant(Long participantId, UpdateParticipantRequest request) {
        MeetingParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new IllegalArgumentException("Uczestnik nie znaleziony"));

        if (request.getStatus() != null) {
            participant.setStatus(request.getStatus());
            participant.setResponseDate(LocalDateTime.now());
        }

        if (request.getPermissionLevel() != null) {
            participant.setPermissionLevel(request.getPermissionLevel());
        }

        if (request.getComment() != null) {
            participant.setComment(request.getComment());
        }

        MeetingParticipant updated = participantRepository.save(participant);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void removeParticipant(Long participantId) {
        participantRepository.deleteById(participantId);
    }

    @Override
    @Transactional
    public MeetingParticipant acceptInvitationByToken(String token) {
        MeetingParticipant participant = participantRepository.findByInvitationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Nieprawidłowy token zaproszenia"));

        if (!isTokenValid(participant)) {
            throw new IllegalArgumentException("Token wygasł");
        }

        participant.confirmParticipation();
        participant.setInvitationToken(null); // Zużyj token

        MeetingParticipant updated = participantRepository.save(participant);
        log.info("Użytkownik {} potwierdził udział w spotkaniu {}",
                participant.getUser().getEmail(), participant.getMeeting().getTitle());

        return updated;
    }

    @Override
    @Transactional
    public ParticipantResponse confirmParticipation(String token, String comment) {
        MeetingParticipant participant = participantRepository.findByInvitationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Nieprawidłowy token zaproszenia"));

        if (!isTokenValid(participant)) {
            throw new IllegalArgumentException("Token wygasł");
        }

        participant.confirmParticipation();
        participant.setComment(comment);
        participant.setInvitationToken(null); // Zużyj token

        MeetingParticipant updated = participantRepository.save(participant);
        log.info("Użytkownik {} potwierdził udział w spotkaniu {}",
                participant.getUser().getEmail(), participant.getMeeting().getTitle());

        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public ParticipantResponse declineParticipation(String token, String comment) {
        MeetingParticipant participant = participantRepository.findByInvitationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Nieprawidłowy token zaproszenia"));

        if (!isTokenValid(participant)) {
            throw new IllegalArgumentException("Token wygasł");
        }

        participant.declineParticipation();
        participant.setComment(comment);
        participant.setInvitationToken(null);

        MeetingParticipant updated = participantRepository.save(participant);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public ParticipantResponse setTentative(String token, String comment) {
        MeetingParticipant participant = participantRepository.findByInvitationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Nieprawidłowy token zaproszenia"));

        if (!isTokenValid(participant)) {
            throw new IllegalArgumentException("Token wygasł");
        }

        participant.setTentative();
        participant.setComment(comment);

        MeetingParticipant updated = participantRepository.save(participant);
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserParticipant(Long meetingId, Long userId) {
        return participantRepository.existsByMeetingIdAndUserId(meetingId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserParticipantOfMeeting(Long meetingId, Long userId) {
        return isUserParticipant(meetingId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUserEditMeeting(Long meetingId, Long userId) {
        return isOrganizer(meetingId, userId);
    }

    @Override
    @Transactional
    public void joinMeeting(Long userId, Long meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("Spotkanie nie znalezione"));

        // Sprawdź czy spotkanie pozwala na dołączanie
        if (meeting.getVisibility() != MeetingVisibility.PUBLIC) {
            throw new SecurityException("To spotkanie nie pozwala na publiczne dołączanie");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Użytkownik nie znaleziony"));

        // Sprawdź czy użytkownik już jest zapisany
        if (participantRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
            throw new IllegalArgumentException("Już jesteś zapisany na to spotkanie");
        }

        MeetingParticipant participant = MeetingParticipant.builder()
                .meeting(meeting)
                .user(user)
                .status(ParticipationStatus.CONFIRMED)
                .permissionLevel(PermissionLevel.PARTICIPANT)
                .responseDate(LocalDateTime.now())
                .build();

        participantRepository.save(participant);
        log.info("Użytkownik {} dołączył do spotkania {}", user.getEmail(), meeting.getTitle());
    }

    @Override
    @Transactional
    public void leaveMeeting(Long userId, Long meetingId) {
        Optional<MeetingParticipant> participant = participantRepository.findByMeetingIdAndUserId(meetingId, userId);
        if (participant.isPresent()) {
            // Organizer nie może opuścić spotkania - musi je usunąć lub przekazać organizację
            Meeting meeting = meetingRepository.findById(meetingId)
                    .orElseThrow(() -> new IllegalArgumentException("Spotkanie nie znalezione"));

            if (meeting.getOrganizer().getId().equals(userId)) {
                throw new SecurityException("Organizator nie może opuścić spotkania. Usuń spotkanie lub przekaż organizację.");
            }

            participantRepository.delete(participant.get());
            log.info("Użytkownik {} opuścił spotkanie {}", userId, meetingId);
        } else {
            throw new IllegalArgumentException("Nie jesteś uczestnikiem tego spotkania");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> searchUsersForInvitation(String query, Long meetingId) {
        List<User> users = userRepository.findByEmailContainingOrFirstNameContainingOrLastNameContaining(
                query, query, query);

        return users.stream()
                .filter(user -> !participantRepository.existsByMeetingIdAndUserId(meetingId, user.getId()))
                .map(this::mapUserToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public List<ParticipantResponse> getPendingRequests(Long meetingId) {
        List<MeetingParticipant> pendingParticipants = participantRepository
                .findByMeetingIdAndStatus(meetingId, ParticipationStatus.PENDING);

        return pendingParticipants.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserPendingApproval(Long meetingId, Long userId) {
        return participantRepository.findByMeetingIdAndUserId(meetingId, userId)
                .map(participant -> participant.getStatus() == ParticipationStatus.PENDING)
                .orElse(false);
    }

    // ========== METODY POMOCNICZE ==========

    private MeetingParticipant createParticipant(Meeting meeting, User user) {
        String token = UUID.randomUUID().toString();

        return MeetingParticipant.builder()
                .meeting(meeting)
                .user(user)
                .status(ParticipationStatus.INVITED)
                .permissionLevel(PermissionLevel.PARTICIPANT)
                .invitationToken(token)
                .tokenExpiresAt(LocalDateTime.now().plusDays(7))
                .build();
    }

    private boolean isTokenValid(MeetingParticipant participant) {
        return participant.getTokenExpiresAt() != null &&
                participant.getTokenExpiresAt().isAfter(LocalDateTime.now());
    }

    private ParticipantResponse mapToResponse(MeetingParticipant participant) {
        return ParticipantResponse.builder()
                .id(participant.getId())
                .user(mapUserToResponse(participant.getUser()))
                .status(participant.getStatus())
                .permissionLevel(participant.getPermissionLevel())
                .comment(participant.getComment())
                .responseDate(participant.getResponseDate())
                .createdAt(participant.getCreatedAt())
                .updatedAt(participant.getUpdatedAt())
                .build();
    }

    private UserResponse mapUserToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
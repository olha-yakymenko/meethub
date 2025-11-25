// src/main/java/com/meethub/domain/service/impl/ParticipantServiceImpl.java
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
import com.meethub.domain.service.ParticipantService;
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
    @Transactional
    public ParticipantResponse addToWaitlist(Long meetingId, Long userId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("Spotkanie nie znalezione"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Użytkownik nie znaleziony"));

        // Sprawdź czy już jest na liście
        if (participantRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
            throw new IllegalArgumentException("Użytkownik już jest na liście uczestników");
        }

        MeetingParticipant waitlisted = MeetingParticipant.builder()
                .meeting(meeting)
                .user(user)
                .status(ParticipationStatus.WAITING_LIST)
                .permissionLevel(PermissionLevel.PARTICIPANT)
                .build();

        MeetingParticipant saved = participantRepository.save(waitlisted);
        return mapToResponse(saved);
    }

    // Metody pomocnicze
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

    // Implementacja pozostałych metod
    @Override
    public void removeParticipant(Long participantId) {
        participantRepository.deleteById(participantId);
    }

    @Override
    public ParticipantResponse getParticipant(Long participantId) {
        MeetingParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new IllegalArgumentException("Uczestnik nie znaleziony"));
        return mapToResponse(participant);
    }

    @Override
    public List<ParticipantResponse> getMeetingParticipants(Long meetingId) {
        return participantRepository.findByMeetingId(meetingId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ParticipantResponse> getWaitlist(Long meetingId) {
        return participantRepository.findByMeetingIdAndStatus(meetingId, ParticipationStatus.WAITING_LIST).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public String generateInvitationToken(Long participantId) {
        MeetingParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new IllegalArgumentException("Uczestnik nie znaleziony"));

        String token = UUID.randomUUID().toString();
        participant.setInvitationToken(token);
        participant.setTokenExpiresAt(LocalDateTime.now().plusDays(7));

        participantRepository.save(participant);
        return token;
    }

    @Override
    public boolean validateInvitationToken(String token) {
        return participantRepository.findByInvitationToken(token)
                .map(this::isTokenValid)
                .orElse(false);
    }

    @Override
    public ParticipantStats getMeetingStats(Long meetingId) {
        return new ParticipantStats() {
            @Override
            public int getTotalInvited() {
                return participantRepository.findByMeetingId(meetingId).size();
            }

            @Override
            public int getTotalConfirmed() {
                return (int) participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.CONFIRMED);
            }

            @Override
            public int getTotalDeclined() {
                return (int) participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.DECLINED);
            }

            @Override
            public int getWaitlistCount() {
                return (int) participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.WAITING_LIST);
            }

            @Override
            public int getAvailableSpots() {
                Meeting meeting = meetingRepository.findById(meetingId).orElse(null);
                if (meeting == null || meeting.getMaxParticipants() == null) {
                    return Integer.MAX_VALUE;
                }
                int confirmed = getTotalConfirmed();
                return Math.max(0, meeting.getMaxParticipants() - confirmed);
            }
        };
    }

    @Override
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
    public boolean isUserParticipant(Long userId, Long meetingId) {
        return participantRepository.existsByMeetingIdAndUserId(meetingId, userId);
    }

    @Override
    public ParticipationStatus getUserParticipationStatus(Long userId, Long meetingId) {
        Optional<MeetingParticipant> participant = participantRepository.findByMeetingIdAndUserId(meetingId, userId);
        return participant.map(MeetingParticipant::getStatus).orElse(null);
    }

    // ========== DODATKOWE METODY POMOCNICZE ==========

    @Transactional
    @Override
    public ParticipantResponse inviteParticipant(Long meetingId, Long userId, Long organizerId) {
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

        return mapToResponse(saved);
    }

    @Transactional
    @Override
    public void removeFromMeeting(Long meetingId, Long userId, Long removerId) {
        // Sprawdź uprawnienia
        if (!isOrganizer(meetingId, removerId) && !userId.equals(removerId)) {
            throw new SecurityException("Nie masz uprawnień do usunięcia tego uczestnika");
        }

        Optional<MeetingParticipant> participant = participantRepository.findByMeetingIdAndUserId(meetingId, userId);
        if (participant.isPresent()) {
            participantRepository.delete(participant.get());
            log.info("Usunięto użytkownika {} ze spotkania {}", userId, meetingId);
        } else {
            throw new IllegalArgumentException("Użytkownik nie jest uczestnikiem tego spotkania");
        }
    }

    @Transactional
    @Override
    public ParticipantResponse joinMeeting(Long userId, Long meetingId) {
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

        MeetingParticipant saved = participantRepository.save(participant);
        log.info("Użytkownik {} dołączył do spotkania {}", user.getEmail(), meeting.getTitle());

        return mapToResponse(saved);
    }

    @Transactional
    @Override
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


}
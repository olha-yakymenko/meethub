
package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.MeetingParticipant;
import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.repository.jpa.MeetingParticipantRepository;
import com.meethub.domain.repository.jpa.MeetingRepository;
import com.meethub.domain.repository.jpa.UserRepository;
import com.meethub.domain.service.ParticipationService;
import com.meethub.exception.BusinessException;
import com.meethub.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParticipationServiceImpl implements ParticipationService {

    private final MeetingParticipantRepository participantRepository;
    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;

    // ========== STATUS UCZESTNICTWA ==========

    @Override
    @Transactional
    public MeetingParticipant confirmParticipation(Long meetingId, Long userId) {
        log.info("Confirming participation for meeting: {}, user: {}", meetingId, userId);

        MeetingParticipant participant = getParticipant(meetingId, userId);

        // Sprawdź czy można potwierdzić
        if (participant.getStatus() == ParticipationStatus.CONFIRMED) {
            throw new BusinessException("Participation already confirmed");
        }

        if (participant.getStatus() == ParticipationStatus.DECLINED) {
            throw new BusinessException("Cannot confirm declined participation");
        }

        // Sprawdź limit uczestników
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        if (meeting.getMaxParticipants() != null) {
            long confirmedCount = participantRepository.countByMeetingIdAndStatusIn(
                    meetingId, Arrays.asList(ParticipationStatus.CONFIRMED, ParticipationStatus.ATTENDED));

            if (confirmedCount >= meeting.getMaxParticipants()) {
                throw new BusinessException("Meeting has reached maximum participants");
            }
        }

        participant.setStatus(ParticipationStatus.CONFIRMED);
//        participant.setResponseAt(LocalDateTime.now());

        return participantRepository.save(participant);
    }

    @Override
    @Transactional
    public MeetingParticipant declineParticipation(Long meetingId, Long userId) {
        log.info("Declining participation for meeting: {}, user: {}", meetingId, userId);

        MeetingParticipant participant = getParticipant(meetingId, userId);

        participant.setStatus(ParticipationStatus.DECLINED);
//        participant.setResponseAt(LocalDateTime.now());

        return participantRepository.save(participant);
    }


    // ========== OBECNOŚĆ ==========

    @Override
    @Transactional
    public MeetingParticipant markAsAttended(Long meetingId, Long userId) {
        log.info("Marking as attended for meeting: {}, user: {}", meetingId, userId);

        MeetingParticipant participant = getParticipant(meetingId, userId);

        if (participant.getStatus() != ParticipationStatus.CONFIRMED) {
            throw new BusinessException("Only confirmed or tentative participants can be marked as attended");
        }

        participant.setStatus(ParticipationStatus.ATTENDED);
//        participant.setJoinedAt(LocalDateTime.now());

        return participantRepository.save(participant);
    }

    // ========== STATYSTYKI ==========

    @Override
    @Transactional(readOnly = true)
    public Map<ParticipationStatus, Long> getResponseStatistics(Long meetingId) {
        List<MeetingParticipant> participants = participantRepository.findByMeetingId(meetingId);

        return participants.stream()
                .collect(Collectors.groupingBy(
                        MeetingParticipant::getStatus,
                        Collectors.counting()
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Double getAverageResponseTime(Long meetingId) {
        Double avgHours = participantRepository.findAverageResponseTimeHours(meetingId);
        return avgHours != null ? avgHours : 0.0;
    }


    @Override
    @Transactional(readOnly = true)
    public boolean isUserParticipant(Long meetingId, Long userId) {
        return participantRepository.findByMeetingIdAndUserId(meetingId, userId).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserConfirmed(Long meetingId, Long userId) {
        return participantRepository.findByMeetingIdAndUserId(meetingId, userId)
                .map(p -> p.getStatus() == ParticipationStatus.CONFIRMED ||
                        p.getStatus() == ParticipationStatus.ATTENDED)
                .orElse(false);
    }

    // ========== OPERACJE DLA ORGANIZATORA ==========

    @Override
    @Transactional
    public MeetingParticipant updateUserStatus(Long meetingId, Long userId, ParticipationStatus status) {
        log.info("Updating status for meeting: {}, user: {}, status: {}", meetingId, userId, status);

        MeetingParticipant participant = getParticipant(meetingId, userId);

        // Walidacja zmian
        if (status == ParticipationStatus.CONFIRMED && participant.getStatus() == ParticipationStatus.DECLINED) {
            throw new BusinessException("Cannot confirm declined participation");
        }

        participant.setStatus(status);

        // Ustaw responseAt jeśli status to potwierdzenie/odrzucenie
        if (status == ParticipationStatus.CONFIRMED ||
                status == ParticipationStatus.DECLINED) {
//            participant.setResponseAt(LocalDateTime.now());
        }

        // Ustaw joinedAt jeśli oznaczono jako obecny
        if (status == ParticipationStatus.ATTENDED) {
//            participant.setJoinedAt(LocalDateTime.now());
        }

        return participantRepository.save(participant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeetingParticipant> getMeetingParticipants(Long meetingId) {
        return participantRepository.findByMeetingId(meetingId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeetingParticipant> getConfirmedParticipants(Long meetingId) {
        List<MeetingParticipant> participants = participantRepository.findByMeetingId(meetingId);

        return participants.stream()
                .filter(p -> p.getStatus() == ParticipationStatus.CONFIRMED ||
                        p.getStatus() == ParticipationStatus.ATTENDED)
                .collect(Collectors.toList());
    }

    // ========== LISTA REZERWOWA ==========

    @Override
    @Transactional
    public MeetingParticipant addToWaitingList(Long meetingId, Long userId) {
        log.info("Adding to waiting list for meeting: {}, user: {}", meetingId, userId);

        // Sprawdź czy już jest uczestnikiem
        Optional<MeetingParticipant> existing = participantRepository
                .findByMeetingIdAndUserId(meetingId, userId);

        if (existing.isPresent()) {
            throw new BusinessException("User is already a participant");
        }

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        com.meethub.domain.model.entity.User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        MeetingParticipant participant = MeetingParticipant.builder()
                .meeting(meeting)
                .user(user)
                .status(ParticipationStatus.PENDING)
//                .invitedAt(LocalDateTime.now())
                .build();

        return participantRepository.save(participant);
    }

    @Override
    @Transactional
    public MeetingParticipant promoteFromWaitingList(Long meetingId, Long userId) {
        log.info("Promoting from waiting list for meeting: {}, user: {}", meetingId, userId);

        MeetingParticipant participant = getParticipant(meetingId, userId);

        if (participant.getStatus() != ParticipationStatus.PENDING) {
            throw new BusinessException("User is not on waiting list");
        }

        // Sprawdź czy są jeszcze miejsca
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        if (meeting.getMaxParticipants() != null) {
            long confirmedCount = participantRepository.countByMeetingIdAndStatusIn(
                    meetingId, Arrays.asList(ParticipationStatus.CONFIRMED, ParticipationStatus.ATTENDED));

            if (confirmedCount >= meeting.getMaxParticipants()) {
                throw new BusinessException("No available spots in the meeting");
            }
        }

        participant.setStatus(ParticipationStatus.CONFIRMED);
//        participant.setResponseAt(LocalDateTime.now());

        return participantRepository.save(participant);
    }

    // ========== METODY POMOCNICZE ==========

    MeetingParticipant getParticipant(Long meetingId, Long userId) {
        return participantRepository.findByMeetingIdAndUserId(meetingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Participation not found"));
    }

    void validateCanChangeStatus(MeetingParticipant participant, ParticipationStatus newStatus) {
        ParticipationStatus currentStatus = participant.getStatus();

        // Nie można zmienić statusu jeśli już odrzucono
        if (currentStatus == ParticipationStatus.DECLINED &&
                newStatus == ParticipationStatus.CONFIRMED) {
            throw new BusinessException("Cannot confirm declined participation");
        }

        // Nie można zmienić statusu jeśli już potwierdzono
        if (currentStatus == ParticipationStatus.CONFIRMED &&
                newStatus == ParticipationStatus.DECLINED) {
            throw new BusinessException("Cannot decline confirmed participation. Please cancel instead.");
        }
    }
}




//package com.meethub.domain.service.impl;
//
//import com.meethub.domain.model.entity.Meeting;
//import com.meethub.domain.model.entity.MeetingParticipant;
//import com.meethub.domain.model.enums.ParticipationStatus;
//import com.meethub.domain.repository.jpa.MeetingParticipantRepository;
//import com.meethub.domain.repository.jpa.MeetingRepository;
//import com.meethub.domain.repository.jpa.UserRepository;
//import com.meethub.domain.service.ParticipationService;
//import com.meethub.exception.BusinessException;
//import com.meethub.exception.ResourceNotFoundException;
//import jakarta.validation.constraints.Max;
//import jakarta.validation.constraints.Min;
//import jakarta.validation.constraints.NotNull;
//import jakarta.validation.constraints.Positive;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//import java.time.temporal.ChronoUnit;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class ParticipationServiceImpl implements ParticipationService {
//
//    private final MeetingParticipantRepository participantRepository;
//    private final MeetingRepository meetingRepository;
//    private final UserRepository userRepository;
//
//    // ========== STATUS UCZESTNICTWA ==========
//
//    @Override
//    @Transactional
//    public MeetingParticipant confirmParticipation(
//            @NotNull(message = "Identyfikator spotkania nie może być pusty")
//            @Positive(message = "Identyfikator spotkania musi być liczbą dodatnią")
//            Long meetingId,
//
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//
//        log.info("Confirming participation for meeting: {}, user: {}", meetingId, userId);
//
//        validateMeetingAndUserIds(meetingId, userId);
//
//        MeetingParticipant participant = getParticipant(meetingId, userId);
//
//        // Sprawdź czy można potwierdzić
//        if (participant.getStatus() == ParticipationStatus.CONFIRMED) {
//            throw new BusinessException("Participation already confirmed");
//        }
//
//        if (participant.getStatus() == ParticipationStatus.DECLINED) {
//            throw new BusinessException("Cannot confirm declined participation");
//        }
//
//        // Sprawdź limit uczestników
//        Meeting meeting = meetingRepository.findById(meetingId)
//                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
//
//        validateMeetingCapacity(meeting);
//
//        participant.setStatus(ParticipationStatus.CONFIRMED);
//
//        return participantRepository.save(participant);
//    }
//
//    @Override
//    @Transactional
//    public MeetingParticipant declineParticipation(
//            @NotNull(message = "Identyfikator spotkania nie może być pusty")
//            @Positive(message = "Identyfikator spotkania musi być liczbą dodatnią")
//            Long meetingId,
//
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//
//        log.info("Declining participation for meeting: {}, user: {}", meetingId, userId);
//
//        validateMeetingAndUserIds(meetingId, userId);
//
//        MeetingParticipant participant = getParticipant(meetingId, userId);
//
//        participant.setStatus(ParticipationStatus.DECLINED);
//
//        return participantRepository.save(participant);
//    }
//
//    // ========== OBECNOŚĆ ==========
//
//    @Override
//    @Transactional
//    public MeetingParticipant markAsAttended(
//            @NotNull(message = "Identyfikator spotkania nie może być pusty")
//            @Positive(message = "Identyfikator spotkania musi być liczbą dodatnią")
//            Long meetingId,
//
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//
//        log.info("Marking as attended for meeting: {}, user: {}", meetingId, userId);
//
//        validateMeetingAndUserIds(meetingId, userId);
//
//        MeetingParticipant participant = getParticipant(meetingId, userId);
//
//        if (participant.getStatus() != ParticipationStatus.CONFIRMED) {
//            throw new BusinessException("Only confirmed or tentative participants can be marked as attended");
//        }
//
//        participant.setStatus(ParticipationStatus.ATTENDED);
//
//        return participantRepository.save(participant);
//    }
//
//    // ========== STATYSTYKI ==========
//
//    @Override
//    @Transactional(readOnly = true)
//    public Map<ParticipationStatus, Long> getResponseStatistics(
//            @NotNull(message = "Identyfikator spotkania nie może być pusty")
//            @Positive(message = "Identyfikator spotkania musi być liczbą dodatnią")
//            Long meetingId) {
//
//        validateMeetingId(meetingId);
//
//        List<MeetingParticipant> participants = participantRepository.findByMeetingId(meetingId);
//
//        return participants.stream()
//                .collect(Collectors.groupingBy(
//                        MeetingParticipant::getStatus,
//                        Collectors.counting()
//                ));
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public Double getAverageResponseTime(
//            @NotNull(message = "Identyfikator spotkania nie może być pusty")
//            @Positive(message = "Identyfikator spotkania musi być liczbą dodatnią")
//            Long meetingId) {
//
//        validateMeetingId(meetingId);
//
//        Double avgHours = participantRepository.findAverageResponseTimeHours(meetingId);
//        return avgHours != null ? avgHours : 0.0;
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public boolean isUserParticipant(
//            @NotNull(message = "Identyfikator spotkania nie może być pusty")
//            @Positive(message = "Identyfikator spotkania musi być liczbą dodatnią")
//            Long meetingId,
//
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//
//        validateMeetingAndUserIds(meetingId, userId);
//
//        return participantRepository.findByMeetingIdAndUserId(meetingId, userId).isPresent();
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public boolean isUserConfirmed(
//            @NotNull(message = "Identyfikator spotkania nie może być pusty")
//            @Positive(message = "Identyfikator spotkania musi być liczbą dodatnią")
//            Long meetingId,
//
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//
//        validateMeetingAndUserIds(meetingId, userId);
//
//        return participantRepository.findByMeetingIdAndUserId(meetingId, userId)
//                .map(p -> p.getStatus() == ParticipationStatus.CONFIRMED ||
//                        p.getStatus() == ParticipationStatus.ATTENDED)
//                .orElse(false);
//    }
//
//    // ========== OPERACJE DLA ORGANIZATORA ==========
//
//    @Override
//    @Transactional
//    public MeetingParticipant updateUserStatus(
//            @NotNull(message = "Identyfikator spotkania nie może być pusty")
//            @Positive(message = "Identyfikator spotkania musi być liczbą dodatnią")
//            Long meetingId,
//
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId,
//
//            @NotNull(message = "Status uczestnictwa nie może być pusty")
//            ParticipationStatus status) {
//
//        log.info("Updating status for meeting: {}, user: {}, status: {}", meetingId, userId, status);
//
//        validateMeetingAndUserIds(meetingId, userId);
//        validateParticipationStatus(status);
//
//        MeetingParticipant participant = getParticipant(meetingId, userId);
//
//        // Walidacja zmian
//        validateStatusChange(participant.getStatus(), status);
//
//        participant.setStatus(status);
//
//        return participantRepository.save(participant);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<MeetingParticipant> getMeetingParticipants(
//            @NotNull(message = "Identyfikator spotkania nie może być pusty")
//            @Positive(message = "Identyfikator spotkania musi być liczbą dodatnią")
//            Long meetingId) {
//
//        validateMeetingId(meetingId);
//
//        return participantRepository.findByMeetingId(meetingId);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<MeetingParticipant> getConfirmedParticipants(
//            @NotNull(message = "Identyfikator spotkania nie może być pusty")
//            @Positive(message = "Identyfikator spotkania musi być liczbą dodatnią")
//            Long meetingId) {
//
//        validateMeetingId(meetingId);
//
//        List<MeetingParticipant> participants = participantRepository.findByMeetingId(meetingId);
//
//        return participants.stream()
//                .filter(p -> p.getStatus() == ParticipationStatus.CONFIRMED ||
//                        p.getStatus() == ParticipationStatus.ATTENDED)
//                .collect(Collectors.toList());
//    }
//
//    // ========== LISTA REZERWOWA ==========
//
//    @Override
//    @Transactional
//    public MeetingParticipant addToWaitingList(
//            @NotNull(message = "Identyfikator spotkania nie może być pusty")
//            @Positive(message = "Identyfikator spotkania musi być liczbą dodatnią")
//            Long meetingId,
//
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//
//        log.info("Adding to waiting list for meeting: {}, user: {}", meetingId, userId);
//
//        validateMeetingAndUserIds(meetingId, userId);
//
//        // Sprawdź czy już jest uczestnikiem
//        Optional<MeetingParticipant> existing = participantRepository
//                .findByMeetingIdAndUserId(meetingId, userId);
//
//        if (existing.isPresent()) {
//            throw new BusinessException("User is already a participant");
//        }
//
//        Meeting meeting = meetingRepository.findById(meetingId)
//                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
//
//        com.meethub.domain.model.entity.User user = userRepository.findById(userId)
//                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
//
//        validateUserCanBeAddedToWaitingList(meeting, user);
//
//        MeetingParticipant participant = MeetingParticipant.builder()
//                .meeting(meeting)
//                .user(user)
//                .status(ParticipationStatus.WAITING_LIST)
//                .build();
//
//        return participantRepository.save(participant);
//    }
//
//    @Override
//    @Transactional
//    public MeetingParticipant promoteFromWaitingList(
//            @NotNull(message = "Identyfikator spotkania nie może być pusty")
//            @Positive(message = "Identyfikator spotkania musi być liczbą dodatnią")
//            Long meetingId,
//
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//
//        log.info("Promoting from waiting list for meeting: {}, user: {}", meetingId, userId);
//
//        validateMeetingAndUserIds(meetingId, userId);
//
//        MeetingParticipant participant = getParticipant(meetingId, userId);
//
//        if (participant.getStatus() != ParticipationStatus.WAITING_LIST) {
//            throw new BusinessException("User is not on waiting list");
//        }
//
//        // Sprawdź czy są jeszcze miejsca
//        Meeting meeting = meetingRepository.findById(meetingId)
//                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
//
//        validateMeetingCapacityForPromotion(meeting);
//
//        participant.setStatus(ParticipationStatus.CONFIRMED);
//
//        return participantRepository.save(participant);
//    }
//
//    // ========== METODY POMOCNICZE ==========
//
//    MeetingParticipant getParticipant(
//            @NotNull(message = "Identyfikator spotkania nie może być pusty")
//            @Positive(message = "Identyfikator spotkania musi być liczbą dodatnią")
//            Long meetingId,
//
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//
//        validateMeetingAndUserIds(meetingId, userId);
//
//        return participantRepository.findByMeetingIdAndUserId(meetingId, userId)
//                .orElseThrow(() -> new ResourceNotFoundException("Participation not found"));
//    }
//
//    void validateCanChangeStatus(
//            @NotNull(message = "Uczestnik nie może być pusty")
//            MeetingParticipant participant,
//
//            @NotNull(message = "Nowy status nie może być pusty")
//            ParticipationStatus newStatus) {
//
//        ParticipationStatus currentStatus = participant.getStatus();
//
//        // Nie można zmienić statusu jeśli już odrzucono
//        if (currentStatus == ParticipationStatus.DECLINED &&
//                newStatus == ParticipationStatus.CONFIRMED) {
//            throw new BusinessException("Cannot confirm declined participation");
//        }
//
//        // Nie można zmienić statusu jeśli już potwierdzono
//        if (currentStatus == ParticipationStatus.CONFIRMED &&
//                newStatus == ParticipationStatus.DECLINED) {
//            throw new BusinessException("Cannot decline confirmed participation. Please cancel instead.");
//        }
//    }
//
//    // ========== METODY WALIDACYJNE ==========
//
//    /**
//     * Walidacja identyfikatora spotkania
//     */
//    private void validateMeetingId(
//            @NotNull(message = "Identyfikator spotkania nie może być pusty")
//            @Positive(message = "Identyfikator spotkania musi być liczbą dodatnią")
//            Long meetingId) {
//
//        if (meetingId == null) {
//            throw new IllegalArgumentException("Identyfikator spotkania nie może być pusty");
//        }
//
//        if (meetingId <= 0) {
//            throw new IllegalArgumentException("Identyfikator spotkania musi być liczbą dodatnią");
//        }
//    }
//
//    /**
//     * Walidacja identyfikatora użytkownika
//     */
//    private void validateUserId(
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//
//        if (userId == null) {
//            throw new IllegalArgumentException("Identyfikator użytkownika nie może być pusty");
//        }
//
//        if (userId <= 0) {
//            throw new IllegalArgumentException("Identyfikator użytkownika musi być liczbą dodatnią");
//        }
//    }
//
//    /**
//     * Walidacja identyfikatorów spotkania i użytkownika
//     */
//    private void validateMeetingAndUserIds(
//            @NotNull(message = "Identyfikator spotkania nie może być pusty")
//            @Positive(message = "Identyfikator spotkania musi być liczbą dodatnią")
//            Long meetingId,
//
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//
//        validateMeetingId(meetingId);
//        validateUserId(userId);
//    }
//
//    /**
//     * Walidacja statusu uczestnictwa
//     */
//    private void validateParticipationStatus(
//            @NotNull(message = "Status uczestnictwa nie może być pusty")
//            ParticipationStatus status) {
//
//        if (status == null) {
//            throw new IllegalArgumentException("Status uczestnictwa nie może być pusty");
//        }
//    }
//
//    /**
//     * Walidacja zmiany statusu
//     */
//    private void validateStatusChange(
//            @NotNull(message = "Aktualny status nie może być pusty")
//            ParticipationStatus currentStatus,
//
//            @NotNull(message = "Nowy status nie może być pusty")
//            ParticipationStatus newStatus) {
//
//        // Nie można zmienić statusu jeśli już odrzucono
//        if (currentStatus == ParticipationStatus.DECLINED &&
//                newStatus == ParticipationStatus.CONFIRMED) {
//            throw new BusinessException("Cannot confirm declined participation");
//        }
//
//        // Nie można zmienić statusu jeśli już potwierdzono
//        if (currentStatus == ParticipationStatus.CONFIRMED &&
//                newStatus == ParticipationStatus.DECLINED) {
//            throw new BusinessException("Cannot decline confirmed participation. Please cancel instead.");
//        }
//
//        // Nie można zmienić na ten sam status
//        if (currentStatus == newStatus) {
//            throw new BusinessException("User already has this status");
//        }
//    }
//
//    /**
//     * Walidacja pojemności spotkania
//     */
//    private void validateMeetingCapacity(
//            @NotNull(message = "Spotkanie nie może być puste")
//            Meeting meeting) {
//
//        if (meeting.getMaxParticipants() != null) {
//            long confirmedCount = participantRepository.countByMeetingIdAndStatusIn(
//                    meeting.getId(), Arrays.asList(ParticipationStatus.CONFIRMED, ParticipationStatus.ATTENDED));
//
//            if (confirmedCount >= meeting.getMaxParticipants()) {
//                throw new BusinessException("Meeting has reached maximum participants");
//            }
//        }
//    }
//
//    /**
//     * Walidacja pojemności spotkania dla promocji z listy rezerwowej
//     */
//    private void validateMeetingCapacityForPromotion(
//            @NotNull(message = "Spotkanie nie może być puste")
//            Meeting meeting) {
//
//        if (meeting.getMaxParticipants() != null) {
//            long confirmedCount = participantRepository.countByMeetingIdAndStatusIn(
//                    meeting.getId(), Arrays.asList(ParticipationStatus.CONFIRMED, ParticipationStatus.ATTENDED));
//
//            if (confirmedCount >= meeting.getMaxParticipants()) {
//                throw new BusinessException("No available spots in the meeting");
//            }
//        }
//    }
//
//    /**
//     * Walidacja użytkownika do dodania na listę rezerwową
//     */
//    private void validateUserCanBeAddedToWaitingList(
//            @NotNull(message = "Spotkanie nie może być puste")
//            Meeting meeting,
//
//            @NotNull(message = "Użytkownik nie może być pusty")
//            com.meethub.domain.model.entity.User user) {
//
//        // Sprawdź czy spotkanie ma już rozpoczęte
//        if (meeting.getStartDate() != null && meeting.getStartDate().isBefore(LocalDateTime.now())) {
//            throw new BusinessException("Cannot add to waiting list for past meetings");
//        }
//
//        // Sprawdź czy użytkownik nie jest już na liście rezerwowej
//        boolean alreadyOnWaitingList = participantRepository.existsByMeetingIdAndUserIdAndStatus(
//                meeting.getId(), user.getId(), ParticipationStatus.WAITING_LIST);
//
//        if (alreadyOnWaitingList) {
//            throw new BusinessException("User is already on waiting list");
//        }
//    }
//
//
//    /**
//     * Walidacja statusu przed oznaczaniem jako obecny
//     */
//    private void validateCanMarkAsAttended(
//            @NotNull(message = "Uczestnik nie może być pusty")
//            MeetingParticipant participant) {
//
//        if (participant.getStatus() != ParticipationStatus.CONFIRMED) {
//            throw new BusinessException("Only confirmed participants can be marked as attended");
//        }
//
//        // Sprawdź czy spotkanie się już odbywa/odbyło
//        Meeting meeting = participant.getMeeting();
//        if (meeting.getStartDate() != null &&
//                meeting.getStartDate().isAfter(LocalDateTime.now())) {
//            throw new BusinessException("Cannot mark as attended before meeting starts");
//        }
//    }
//
//    /**
//     * Walidacja listy uczestników
//     */
//    private void validateParticipantsList(
//            @NotNull(message = "Lista uczestników nie może być pusta")
//            List<MeetingParticipant> participants) {
//
//        if (participants == null) {
//            throw new IllegalArgumentException("Lista uczestników nie może być pusta");
//        }
//
//        // Sprawdź unikalność użytkowników
//        long uniqueUserCount = participants.stream()
//                .map(p -> p.getUser().getId())
//                .distinct()
//                .count();
//
//        if (uniqueUserCount != participants.size()) {
//            throw new IllegalArgumentException("Lista uczestników zawiera duplikaty użytkowników");
//        }
//    }
//
//    /**
//     * Walidacja maksymalnej liczby uczestników
//     */
//    private void validateMaxParticipants(
//            @Min(value = 1, message = "Maksymalna liczba uczestników musi być co najmniej 1")
//            @Max(value = 1000, message = "Maksymalna liczba uczestników nie może przekraczać 1000")
//            Integer maxParticipants) {
//
//        if (maxParticipants != null) {
//            if (maxParticipants < 1) {
//                throw new IllegalArgumentException("Maksymalna liczba uczestników musi być co najmniej 1");
//            }
//
//            if (maxParticipants > 1000) {
//                throw new IllegalArgumentException("Maksymalna liczba uczestników nie może przekraczać 1000");
//            }
//        }
//    }
//}










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
                .status(ParticipationStatus.WAITING_LIST)
//                .invitedAt(LocalDateTime.now())
                .build();

        return participantRepository.save(participant);
    }

    @Override
    @Transactional
    public MeetingParticipant promoteFromWaitingList(Long meetingId, Long userId) {
        log.info("Promoting from waiting list for meeting: {}, user: {}", meetingId, userId);

        MeetingParticipant participant = getParticipant(meetingId, userId);

        if (participant.getStatus() != ParticipationStatus.WAITING_LIST) {
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

    private MeetingParticipant getParticipant(Long meetingId, Long userId) {
        return participantRepository.findByMeetingIdAndUserId(meetingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Participation not found"));
    }

    private void validateCanChangeStatus(MeetingParticipant participant, ParticipationStatus newStatus) {
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
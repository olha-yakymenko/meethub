package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.MeetingParticipant;
import com.meethub.domain.model.enums.MeetingStatus;
import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.model.response.DashboardStatsResponse;
import com.meethub.domain.repository.jpa.MeetingRepository;
import com.meethub.domain.repository.jpa.MeetingParticipantRepository;
import com.meethub.domain.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository participantRepository;

    @Override
    public DashboardStatsResponse getUserDashboardStats(Long userId) {
        log.info("Calculating dashboard stats for user: {}", userId);

        try {
            // 1. Spotkania gdzie użytkownik jest ORGANIZATOREM
            List<Meeting> organizedMeetings = meetingRepository.findByOrganizerId(userId);

            // 2. Spotkania gdzie użytkownik jest UCZESTNIKIEM (nie organizatorem)
            List<MeetingParticipant> participantMeetings = participantRepository.findByUserId(userId).stream()
                    .filter(p -> !p.getMeeting().getOrganizer().getId().equals(userId))
                    .filter(p -> p.getStatus() == ParticipationStatus.CONFIRMED)
                    .collect(Collectors.toList());

            // 3. Łączna liczba wszystkich spotkań użytkownika
            long totalMeetings = organizedMeetings.size() + participantMeetings.size();

            // 4. Nadchodzące spotkania
            long upcomingMeetings = countUpcomingMeetings(organizedMeetings, participantMeetings);

            // 5. Liczba WSZYSTKICH spotkań zorganizowanych
            long organizedMeetingsCount = organizedMeetings.size();

            // 6. Liczba unikalnych uczestników we wszystkich zorganizowanych spotkaniach
            long participantsCount = participantRepository.countByMeetingOrganizerId(userId);

            // 7. STATYSTYKI CZASOWE - NOWE!
            LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
            LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
            LocalDateTime weekStart = LocalDateTime.now().minusDays(7);
            LocalDateTime monthStart = LocalDateTime.now().minusDays(30);

            long meetingsToday = countMeetingsInPeriod(organizedMeetings, participantMeetings, todayStart, todayEnd);
            long meetingsThisWeek = countMeetingsInPeriod(organizedMeetings, participantMeetings, weekStart, LocalDateTime.now());
            long meetingsThisMonth = countMeetingsInPeriod(organizedMeetings, participantMeetings, monthStart, LocalDateTime.now());

            return DashboardStatsResponse.builder()
                    .totalMeetings(totalMeetings)
                    .upcomingMeetings(upcomingMeetings)
                    .participantsCount(participantsCount)
                    .organizedMeetings(organizedMeetingsCount)
                    .meetingsToday(meetingsToday)
                    .meetingsThisWeek(meetingsThisWeek)
                    .meetingsThisMonth(meetingsThisMonth)
                    .build();

        } catch (Exception e) {
            log.error("Error calculating dashboard stats for user {}: {}", userId, e.getMessage(), e);
            // Zwróć domyślne wartości w przypadku błędu
            return DashboardStatsResponse.builder()
                    .totalMeetings(0L)
                    .upcomingMeetings(0L)
                    .participantsCount(0L)
                    .organizedMeetings(0L)
                    .meetingsToday(0L)
                    .meetingsThisWeek(0L)
                    .meetingsThisMonth(0L)
                    .build();
        }
    }

    private long countUpcomingMeetings(List<Meeting> organizedMeetings, List<MeetingParticipant> participantMeetings) {
        long upcomingOrganized = organizedMeetings.stream()
                .filter(m -> m.getStartDate() != null && m.getStartDate().isAfter(LocalDateTime.now()))
                .count();

        long upcomingParticipant = participantMeetings.stream()
                .filter(p -> p.getMeeting() != null &&
                        p.getMeeting().getStartDate() != null &&
                        p.getMeeting().getStartDate().isAfter(LocalDateTime.now()))
                .count();

        return upcomingOrganized + upcomingParticipant;
    }

    private long countMeetingsInPeriod(List<Meeting> organizedMeetings,
                                       List<MeetingParticipant> participantMeetings,
                                       LocalDateTime start,
                                       LocalDateTime end) {

        // Spotkania zorganizowane w okresie - LICZONE POPRAWNIE
        long organizedInPeriod = organizedMeetings.stream()
                .filter(m -> m.getStartDate() != null && m.getEndDate() != null)
                .filter(m -> {
                    LocalDateTime meetingStart = m.getStartDate();
                    LocalDateTime meetingEnd = m.getEndDate();

                    // Spotkanie jest w okresie jeśli:
                    // 1. Zaczyna się w okresie LUB
                    // 2. Kończy się w okresie LUB
                    // 3. Trwa przez cały okres LUB
                    // 4. Zaczyna się przed i kończy po okresie
                    return (meetingStart.isAfter(start) && meetingStart.isBefore(end)) ||
                            (meetingEnd.isAfter(start) && meetingEnd.isBefore(end)) ||
                            (meetingStart.isBefore(start) && meetingEnd.isAfter(end)) ||
                            (meetingStart.isEqual(start) || meetingEnd.isEqual(end));
                })
                .count();

        // Spotkania jako uczestnik w okresie
        long participantInPeriod = participantMeetings.stream()
                .filter(p -> p.getMeeting() != null &&
                        p.getMeeting().getStartDate() != null &&
                        p.getMeeting().getEndDate() != null)
                .filter(p -> {
                    Meeting m = p.getMeeting();
                    LocalDateTime meetingStart = m.getStartDate();
                    LocalDateTime meetingEnd = m.getEndDate();

                    return (meetingStart.isAfter(start) && meetingStart.isBefore(end)) ||
                            (meetingEnd.isAfter(start) && meetingEnd.isBefore(end)) ||
                            (meetingStart.isBefore(start) && meetingEnd.isAfter(end)) ||
                            (meetingStart.isEqual(start) || meetingEnd.isEqual(end));
                })
                .count();

        log.debug("Meetings in period {} to {}: organized={}, participant={}",
                start, end, organizedInPeriod, participantInPeriod);

        return organizedInPeriod + participantInPeriod;
    }
}
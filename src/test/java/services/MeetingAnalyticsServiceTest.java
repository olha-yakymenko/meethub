package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.*;
import com.meethub.domain.model.enums.*;
import com.meethub.domain.model.request.ReportFilter;
import com.meethub.domain.model.response.OrganizerReport;
import com.meethub.domain.repository.jpa.FeedbackRepository;
import com.meethub.domain.repository.jpa.MeetingRepository;
import com.meethub.domain.repository.jpa.MeetingStatisticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingAnalyticsServiceTest {

    @Mock
    private MeetingStatisticsRepository statisticsRepository;

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private FeedbackRepository feedbackRepository;

    @InjectMocks
    private MeetingAnalyticsServiceImpl analyticsService;

    private Meeting meeting;
    private User organizer;
    private User participant;
    private MeetingParticipant meetingParticipant;

    @BeforeEach
    void setUp() {
        // Create organizer
        organizer = new User();
        organizer.setId(1L);
        organizer.setFirstName("Organizer");
        organizer.setLastName("Test");

        // Create participant
        participant = new User();
        participant.setId(2L);
        participant.setFirstName("Participant");
        participant.setLastName("Test");

        // Create meeting
        meeting = new Meeting();
        meeting.setId(100L);
        meeting.setTitle("Test Meeting");
        meeting.setDescription("Test meeting description");
        meeting.setStartDate(LocalDateTime.now().minusDays(2));
        meeting.setEndDate(LocalDateTime.now().minusDays(1));
        meeting.setOrganizer(organizer);
        meeting.setStatus(MeetingStatus.COMPLETED);

        // Create meeting participant
        meetingParticipant = new MeetingParticipant();
        meetingParticipant.setId(200L);
        meetingParticipant.setMeeting(meeting);
        meetingParticipant.setUser(participant);
        meetingParticipant.setStatus(ParticipationStatus.ATTENDED);

        // Set participants to meeting
        Set<MeetingParticipant> participants = new HashSet<>();
        participants.add(meetingParticipant);
        meeting.setParticipants(participants);
    }

    @Test
    void generateMeetingStatistics_shouldCreateNewStatistics_whenNotExist() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(meeting));
        when(statisticsRepository.findByMeetingId(100L)).thenReturn(Optional.empty());

        // Mock feedback repository
        when(feedbackRepository.findAverageRatingByMeetingId(100L)).thenReturn(5.0);
        when(feedbackRepository.countByMeetingId(100L)).thenReturn(1L);

        MeetingStatistics savedStats = MeetingStatistics.builder()
                .id(1L)
                .meeting(meeting)
                .totalParticipants(1)
                .attendedParticipants(1)
                .confirmedParticipants(1)
                .declinedParticipants(0)
                .pendingParticipants(0)
                .attendanceRate(BigDecimal.valueOf(100.0))
                .confirmationRate(BigDecimal.valueOf(100.0))
                .averageRating(BigDecimal.valueOf(5.0))
                .feedbackCount(1)
                .generatedAt(LocalDateTime.now())
                .build();

        when(statisticsRepository.save(any(MeetingStatistics.class))).thenReturn(savedStats);

        // When
        MeetingStatistics result = analyticsService.generateMeetingStatistics(100L);

        // Then - single assertion with assertAll
        assertAll(
                () -> assertNotNull(result, "Statistics should not be null"),
                () -> assertEquals(100L, result.getMeeting().getId(), "Meeting ID should match"),
                () -> assertEquals(1, result.getTotalParticipants(), "Should have 1 participant"),
                () -> assertEquals(1, result.getAttendedParticipants(), "Should have 1 attended"),
                () -> assertEquals(BigDecimal.valueOf(100.0), result.getAttendanceRate(), "Attendance rate should be 100%"),
                () -> assertEquals(BigDecimal.valueOf(5.0), result.getAverageRating(), "Average rating should be 5.0")
        );

        verify(meetingRepository).findById(100L);
        verify(statisticsRepository).findByMeetingId(100L);
        verify(statisticsRepository).save(any(MeetingStatistics.class));
    }

    @Test
    void generateMeetingStatistics_shouldUpdateExistingStatistics_whenAlreadyExist() {
        // Given - existing statistics
        MeetingStatistics existingStats = MeetingStatistics.builder()
                .id(1L)
                .meeting(meeting)
                .totalParticipants(1)
                .attendedParticipants(1)
                .confirmedParticipants(1)
                .declinedParticipants(0)
                .pendingParticipants(0)
                .attendanceRate(BigDecimal.valueOf(100.0))
                .confirmationRate(BigDecimal.valueOf(100.0))
                .generatedAt(LocalDateTime.now().minusDays(1))
                .build();

        when(meetingRepository.findById(100L)).thenReturn(Optional.of(meeting));
        when(statisticsRepository.findByMeetingId(100L)).thenReturn(Optional.of(existingStats));
        when(feedbackRepository.findAverageRatingByMeetingId(100L)).thenReturn(5.0);
        when(feedbackRepository.countByMeetingId(100L)).thenReturn(1L);

        MeetingStatistics updatedStats = MeetingStatistics.builder()
                .id(1L)
                .meeting(meeting)
                .totalParticipants(1)
                .attendedParticipants(1)
                .confirmedParticipants(1)
                .declinedParticipants(0)
                .pendingParticipants(0)
                .attendanceRate(BigDecimal.valueOf(100.0))
                .confirmationRate(BigDecimal.valueOf(100.0))
                .averageRating(BigDecimal.valueOf(5.0))
                .feedbackCount(1)
                .generatedAt(LocalDateTime.now())
                .build();

        when(statisticsRepository.save(any(MeetingStatistics.class))).thenReturn(updatedStats);

        // When
        MeetingStatistics result = analyticsService.generateMeetingStatistics(100L);

        // Then - single assertion
        assertThat(result)
                .isNotNull()
                .extracting(MeetingStatistics::getId)
                .isEqualTo(1L);

        verify(statisticsRepository).save(existingStats);
    }

    @Test
    void generateMeetingStatistics_shouldHandleMeetingWithoutParticipants() {
        // Given - meeting without participants
        Meeting emptyMeeting = new Meeting();
        emptyMeeting.setId(200L);
        emptyMeeting.setTitle("Empty Meeting");
        emptyMeeting.setStartDate(LocalDateTime.now().minusDays(1));
        emptyMeeting.setEndDate(LocalDateTime.now().minusHours(12));
        emptyMeeting.setOrganizer(organizer);
        emptyMeeting.setStatus(MeetingStatus.COMPLETED);
        emptyMeeting.setParticipants(Collections.emptySet());

        when(meetingRepository.findById(200L)).thenReturn(Optional.of(emptyMeeting));
        when(statisticsRepository.findByMeetingId(200L)).thenReturn(Optional.empty());
        when(feedbackRepository.findAverageRatingByMeetingId(200L)).thenReturn(null);
        when(feedbackRepository.countByMeetingId(200L)).thenReturn(0L);

        MeetingStatistics savedStats = MeetingStatistics.builder()
                .id(2L)
                .meeting(emptyMeeting)
                .totalParticipants(0)
                .attendedParticipants(0)
                .confirmedParticipants(0)
                .declinedParticipants(0)
                .pendingParticipants(0)
                .attendanceRate(BigDecimal.ZERO)
                .confirmationRate(BigDecimal.ZERO)
                .averageRating(BigDecimal.ZERO)
                .feedbackCount(0)
                .generatedAt(LocalDateTime.now())
                .build();

        when(statisticsRepository.save(any(MeetingStatistics.class))).thenReturn(savedStats);

        // When
        MeetingStatistics result = analyticsService.generateMeetingStatistics(200L);

        // Then - assertAll for multiple related assertions
        assertAll(
                () -> assertEquals(0, result.getTotalParticipants(), "Should have 0 participants"),
                () -> assertEquals(BigDecimal.ZERO, result.getAttendanceRate(), "Attendance should be 0%"),
                () -> assertEquals(0, result.getFeedbackCount(), "Should have 0 feedbacks"),
                () -> assertEquals(BigDecimal.ZERO, result.getAverageRating(), "Average rating should be 0")
        );
    }

    @Test
    void getMeetingStatistics_shouldReturnStatistics_whenExist() {
        // Given
        MeetingStatistics stats = MeetingStatistics.builder()
                .id(1L)
                .meeting(meeting)
                .totalParticipants(1)
                .attendedParticipants(1)
                .attendanceRate(BigDecimal.valueOf(100.0))
                .generatedAt(LocalDateTime.now())
                .build();

        when(statisticsRepository.findByMeetingId(100L)).thenReturn(Optional.of(stats));

        // When
        Optional<MeetingStatistics> result = analyticsService.getMeetingStatistics(100L);

        // Then - single assertion with detailed message
        assertThat(result)
                .isPresent()
                .hasValueSatisfying(s ->
                        assertEquals(100L, s.getMeeting().getId(), "Should return statistics for correct meeting")
                );
    }

    @Test
    void getMeetingStatistics_shouldReturnEmpty_whenNotExist() {
        // Given
        when(statisticsRepository.findByMeetingId(999L)).thenReturn(Optional.empty());

        // When
        Optional<MeetingStatistics> result = analyticsService.getMeetingStatistics(999L);

        // Then - single assertion
        assertThat(result).isEmpty();
    }

    @Test
    void deleteMeetingStatistics_shouldCallRepository() {
        // When
        analyticsService.deleteMeetingStatistics(100L);

        // Then - verify repository method was called
        verify(statisticsRepository).deleteByMeetingId(100L);
    }

    @Test
    void generateOrganizerReport_shouldReturnValidReport() {
        // Given
        MeetingStatistics stats = MeetingStatistics.builder()
                .id(1L)
                .meeting(meeting)
                .totalParticipants(1)
                .attendedParticipants(1)
                .attendanceRate(BigDecimal.valueOf(100.00).setScale(2, RoundingMode.HALF_UP))
                .generatedAt(LocalDateTime.now())
                .build();

        List<MeetingStatistics> statsList = Collections.singletonList(stats);
        when(statisticsRepository.findByOrganizerId(1L)).thenReturn(statsList);

        // When
        OrganizerReport report = analyticsService.generateOrganizerReport(1L, null);

        // Then - assertAll for report validation
        assertAll(
                () -> assertNotNull(report, "Report should not be null"),
                () -> assertEquals(1L, report.getOrganizerId(), "Organizer ID should match"),
                () -> assertEquals(1, report.getTotalMeetings(), "Should have 1 meeting"),
                () -> assertEquals(1, report.getTotalParticipants(), "Should have 1 participant"),
                () -> assertEquals(1, report.getTotalAttended(), "Should have 1 attended"),
                () -> assertEquals(new BigDecimal("100.00"), report.getAverageAttendanceRate(),
                        "Average attendance should be 100%")
        );
    }

    @Test
    void generateOrganizerReport_shouldFilterByDate() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        MeetingStatistics stats1 = MeetingStatistics.builder()
                .id(1L)
                .meeting(createMeetingWithDate(now.minusDays(10)))
                .totalParticipants(1)
                .attendedParticipants(1)
                .attendanceRate(BigDecimal.valueOf(100.0))
                .build();

        MeetingStatistics stats2 = MeetingStatistics.builder()
                .id(2L)
                .meeting(createMeetingWithDate(now.minusDays(30))) // Outside date range
                .totalParticipants(1)
                .attendedParticipants(1)
                .attendanceRate(BigDecimal.valueOf(100.0))
                .build();

        List<MeetingStatistics> allStats = Arrays.asList(stats1, stats2);
        when(statisticsRepository.findByOrganizerId(1L)).thenReturn(allStats);

        ReportFilter filter = new ReportFilter();
        filter.setDateFrom(now.minusDays(20)); // Only include meetings from last 20 days
        filter.setDateTo(now);

        // When
        OrganizerReport report = analyticsService.generateOrganizerReport(1L, filter);

        // Then - should only include stats1 (within date range)
        assertAll(
                () -> assertEquals(1, report.getTotalMeetings(), "Should only include 1 meeting in date range"),
                () -> assertEquals(1, report.getTotalParticipants(), "Should have 1 participant")
        );
    }

    private Meeting createMeetingWithDate(LocalDateTime date) {
        Meeting meeting = new Meeting();
        meeting.setId(1L);
        meeting.setTitle("Meeting");
        meeting.setStartDate(date);
        meeting.setOrganizer(organizer);
        return meeting;
    }

    @Test
    void getStatisticsOverview_shouldReturnCompleteMap() {
        // Given
        MeetingStatistics stats = MeetingStatistics.builder()
                .id(1L)
                .meeting(meeting)
                .totalParticipants(1)
                .attendedParticipants(1)
                .confirmedParticipants(1)
                .attendanceRate(BigDecimal.valueOf(100.0))
                .confirmationRate(BigDecimal.valueOf(100.0))
                .avgResponseTimeMinutes(BigDecimal.valueOf(30.0))
                .generatedAt(LocalDateTime.now())
                .status(MeetingStatistics.StatisticsStatus.FINAL)
                .finalized(true)
                .build();

        when(statisticsRepository.findByMeetingId(100L)).thenReturn(Optional.of(stats));

        // When
        Map<String, Object> overview = analyticsService.getStatisticsOverview(100L);

        // Then - single assertion checking all required keys
        assertThat(overview)
                .containsKeys(
                        "meetingId", "attendanceRate", "totalParticipants",
                        "attendedParticipants", "confirmedParticipants",
                        "avgResponseTime", "generatedAt", "status", "finalized"
                );
    }

    @Test
    void getRecentStatistics_shouldReturnLimitedResults() {
        // Given
        List<MeetingStatistics> allStats = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            MeetingStatistics stats = MeetingStatistics.builder()
                    .id((long) i)
                    .meeting(meeting)
                    .generatedAt(LocalDateTime.now().minusHours(i))
                    .build();
            allStats.add(stats);
        }

        when(statisticsRepository.findAll()).thenReturn(allStats);

        // When
        List<MeetingStatistics> recent = analyticsService.getRecentStatistics(5);

        // Then - should return only 5 most recent
        assertAll(
                () -> assertEquals(5, recent.size(), "Should return only 5 results"),
                () -> assertTrue(recent.get(0).getGeneratedAt().isAfter(recent.get(4).getGeneratedAt()),
                        "Should be sorted by date descending")
        );
    }

    @Test
    void refreshAllStatistics_shouldProcessAllMeetings() {
        // Given
        MeetingStatistics stats = MeetingStatistics.builder()
                .id(1L)
                .meeting(meeting)
                .build();

        List<MeetingStatistics> allStats = Collections.singletonList(stats);
        when(statisticsRepository.findAll()).thenReturn(allStats);
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(meeting));
        when(statisticsRepository.findByMeetingId(100L)).thenReturn(Optional.of(stats));
        when(feedbackRepository.findAverageRatingByMeetingId(100L)).thenReturn(5.0);
        when(feedbackRepository.countByMeetingId(100L)).thenReturn(1L);

        // When
        analyticsService.refreshAllStatistics();

        // Then - verify generateMeetingStatistics was called
        verify(statisticsRepository, atLeastOnce()).save(any(MeetingStatistics.class));
    }

    @Test
    void exportMeetingStatisticsToCsv_shouldReturnNonEmptyByteArray() {
        // Given
        MeetingStatistics stats = MeetingStatistics.builder()
                .id(1L)
                .meeting(meeting)
                .totalParticipants(1)
                .attendedParticipants(1)
                .attendanceRate(BigDecimal.valueOf(100.0))
                .confirmationRate(BigDecimal.valueOf(100.0))
                .avgResponseTimeMinutes(BigDecimal.valueOf(30.0))
                .generatedAt(LocalDateTime.now())
                .build();

        when(statisticsRepository.findByMeetingId(100L)).thenReturn(Optional.of(stats));

        // When
        byte[] csv = analyticsService.exportMeetingStatisticsToCsv(100L);

        // Then - single assertion
        assertAll(
                () -> assertNotNull(csv, "CSV should not be null"),
                () -> assertTrue(csv.length > 0, "CSV should not be empty")
        );
    }

    @Test
    void getAverageResponseTime_shouldReturnValue() {
        // Given
        MeetingStatistics stats = MeetingStatistics.builder()
                .id(1L)
                .meeting(meeting)
                .avgResponseTimeMinutes(BigDecimal.valueOf(45.5))
                .build();

        when(statisticsRepository.findByMeetingId(100L)).thenReturn(Optional.of(stats));

        // When
        BigDecimal avgResponseTime = analyticsService.getAverageResponseTime(100L);

        // Then
        assertThat(avgResponseTime).isEqualTo(BigDecimal.valueOf(45.5));
    }
}
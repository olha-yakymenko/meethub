package com.meethub.domain.service.impl;

import com.meethub.domain.model.dto.ParticipantCountDto;
import com.meethub.domain.model.entity.*;
import com.meethub.domain.model.enums.*;
import com.meethub.domain.model.request.ReportFilter;
import com.meethub.domain.model.response.OrganizerReport;
import com.meethub.domain.repository.jpa.FeedbackRepository;
import com.meethub.domain.repository.jpa.MeetingRepository;
import com.meethub.domain.repository.jpa.MeetingStatisticsRepository;
import com.meethub.domain.service.MeetingParticipantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
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


    private ParticipantCountDto participantCounts;

    @Mock
    private MeetingParticipantService meetingParticipantService;

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
        participantCounts = new ParticipantCountDto(10L, 8L, 9L, 1L, 0L, (long) 80.0, (long) 90.0);
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
//
//    @Test
//    void generateOrganizerReport_shouldReturnValidReport() {
//        // Given
//        MeetingStatistics stats = MeetingStatistics.builder()
//                .id(1L)
//                .meeting(meeting)
//                .totalParticipants(1)
//                .attendedParticipants(1)
//                .attendanceRate(BigDecimal.valueOf(100.00).setScale(2, RoundingMode.HALF_UP))
//                .generatedAt(LocalDateTime.now())
//                .build();
//
//        List<MeetingStatistics> statsList = Collections.singletonList(stats);
//        when(statisticsRepository.findByOrganizerId(1L)).thenReturn(statsList);
//
//        // When
//        OrganizerReport report = analyticsService.generateOrganizerReport(1L, null);
//
//        // Then - assertAll for report validation
//        assertAll(
//                () -> assertNotNull(report, "Report should not be null"),
//                () -> assertEquals(1L, report.getOrganizerId(), "Organizer ID should match"),
//                () -> assertEquals(1, report.getTotalMeetings(), "Should have 1 meeting"),
//                () -> assertEquals(1, report.getTotalParticipants(), "Should have 1 participant"),
//                () -> assertEquals(1, report.getTotalAttended(), "Should have 1 attended"),
//                () -> assertEquals(new BigDecimal("100.00"), report.getAverageAttendanceRate(),
//                        "Average attendance should be 100%")
//        );
//    }

//    @Test
//    void generateOrganizerReport_shouldFilterByDate() {
//        // Given
//        LocalDateTime now = LocalDateTime.now();
//
//        MeetingStatistics stats1 = MeetingStatistics.builder()
//                .id(1L)
//                .meeting(createMeetingWithDate(now.minusDays(10)))
//                .totalParticipants(1)
//                .attendedParticipants(1)
//                .attendanceRate(BigDecimal.valueOf(100.0))
//                .build();
//
//        MeetingStatistics stats2 = MeetingStatistics.builder()
//                .id(2L)
//                .meeting(createMeetingWithDate(now.minusDays(30))) // Outside date range
//                .totalParticipants(1)
//                .attendedParticipants(1)
//                .attendanceRate(BigDecimal.valueOf(100.0))
//                .build();
//
//        List<MeetingStatistics> allStats = Arrays.asList(stats1, stats2);
//        when(statisticsRepository.findByOrganizerId(1L)).thenReturn(allStats);
//
//        ReportFilter filter = new ReportFilter();
//        filter.setDateFrom(now.minusDays(20)); // Only include meetings from last 20 days
//        filter.setDateTo(now);
//
//        // When
//        OrganizerReport report = analyticsService.generateOrganizerReport(1L, filter);
//
//        // Then - should only include stats1 (within date range)
//        assertAll(
//                () -> assertEquals(1, report.getTotalMeetings(), "Should only include 1 meeting in date range"),
//                () -> assertEquals(1, report.getTotalParticipants(), "Should have 1 participant")
//        );
//    }

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


    @Test
    void generateMeetingStatistics_shouldCreateNewStatistics_whenNotExists() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(meeting));
        when(meetingParticipantService.getParticipantCounts(100L)).thenReturn(participantCounts);
        when(feedbackRepository.findAverageRatingByMeetingId(100L)).thenReturn(4.5);
        when(feedbackRepository.countByMeetingId(100L)).thenReturn(5L);
        when(statisticsRepository.findByMeetingId(100L)).thenReturn(Optional.empty());
        when(statisticsRepository.save(any(MeetingStatistics.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        MeetingStatistics result = analyticsService.generateMeetingStatistics(100L);

        // Then
        assertAll(
                () -> assertNotNull(result, "Statistics should not be null"),
                () -> assertEquals(100L, result.getMeeting().getId(), "Should be for correct meeting"),
                () -> assertEquals(10, result.getTotalParticipants(), "Total participants should match"),
                () -> assertEquals(9, result.getAttendedParticipants(), "Attended participants should match"),
                () -> assertEquals(new BigDecimal("90.00"), result.getAttendanceRate(), "Attendance rate should match"),
                () -> assertEquals(new BigDecimal("4.50"), result.getAverageRating(), "Average rating should match"),
                () -> assertEquals(5, result.getFeedbackCount(), "Feedback count should match"),
                () -> assertNotNull(result.getGeneratedAt(), "Generated timestamp should be set"),
                () -> assertEquals(MeetingStatistics.StatisticsStatus.FINAL, result.getStatus(), "Status should be FINAL for completed meeting")
        );

        verify(statisticsRepository).save(any(MeetingStatistics.class));
    }

    @Test
    void generateMeetingStatistics_shouldUpdateExistingStatistics_whenExists() {
        // Given
        MeetingStatistics existingStats = MeetingStatistics.builder()
                .id(1L)
                .meeting(meeting)
                .totalParticipants(5)
                .attendedParticipants(4)
                .attendanceRate(new BigDecimal("80.00"))
                .build();

        when(meetingRepository.findById(100L)).thenReturn(Optional.of(meeting));
        when(meetingParticipantService.getParticipantCounts(100L)).thenReturn(participantCounts);
        when(feedbackRepository.findAverageRatingByMeetingId(100L)).thenReturn(4.5);
        when(feedbackRepository.countByMeetingId(100L)).thenReturn(5L);
        when(statisticsRepository.findByMeetingId(100L)).thenReturn(Optional.of(existingStats));
        when(statisticsRepository.save(any(MeetingStatistics.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        MeetingStatistics result = analyticsService.generateMeetingStatistics(100L);

        // Then
        assertAll(
                () -> assertEquals(1L, result.getId(), "Should update existing statistics"),
                () -> assertEquals(10, result.getTotalParticipants(), "Total participants should be updated"),
                () -> assertEquals(9, result.getAttendedParticipants(), "Attended participants should be updated")
        );

        verify(statisticsRepository).save(existingStats);
    }

    @Test
    void generateMeetingStatistics_shouldHandleNullParticipantCounts() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(meeting));
        when(meetingParticipantService.getParticipantCounts(100L)).thenReturn(null);
        when(feedbackRepository.findAverageRatingByMeetingId(100L)).thenReturn(null);
        when(feedbackRepository.countByMeetingId(100L)).thenReturn(null);
        when(statisticsRepository.findByMeetingId(100L)).thenReturn(Optional.empty());
        when(statisticsRepository.save(any(MeetingStatistics.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        MeetingStatistics result = analyticsService.generateMeetingStatistics(100L);

        // Then
        assertAll(
                () -> assertEquals(0, result.getTotalParticipants(), "Should handle null participant counts"),
                () -> assertEquals(0, result.getAttendedParticipants(), "Should handle null participant counts"),
                () -> assertEquals(0, result.getFeedbackCount(), "Should handle null feedback count"),
                () -> assertEquals(BigDecimal.ZERO, result.getAverageRating(), "Should handle null rating")
        );
    }

    @Test
    void generateMeetingStatistics_shouldHandleFeedbackException() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(meeting));
        when(meetingParticipantService.getParticipantCounts(100L)).thenReturn(participantCounts);
        when(feedbackRepository.findAverageRatingByMeetingId(100L)).thenThrow(new RuntimeException("DB Error"));
        when(statisticsRepository.findByMeetingId(100L)).thenReturn(Optional.empty());
        when(statisticsRepository.save(any(MeetingStatistics.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        MeetingStatistics result = analyticsService.generateMeetingStatistics(100L);

        // Then
        assertAll(
                () -> assertNotNull(result, "Should handle feedback exception gracefully"),
                () -> assertEquals(BigDecimal.ZERO, result.getAverageRating(), "Rating should be zero on exception"),
                () -> assertEquals(0, result.getFeedbackCount(), "Feedback count should be zero on exception")
        );
    }

    @Test
    void generateMeetingStatistics_shouldSetStatusBasedOnMeetingTime() {
        // Test dla spotkania przyszłego (DRAFT)
        Meeting futureMeeting = new Meeting();
        futureMeeting.setId(200L);
        futureMeeting.setTitle("Future Meeting");
        futureMeeting.setStartDate(LocalDateTime.now().plusDays(1));
        futureMeeting.setEndDate(LocalDateTime.now().plusDays(2));
        futureMeeting.setOrganizer(organizer);

        when(meetingRepository.findById(200L)).thenReturn(Optional.of(futureMeeting));
        when(meetingParticipantService.getParticipantCounts(200L)).thenReturn(new ParticipantCountDto(5L, 0L, 5L, 0L, 0L, (long) 0.0, (long) 100.0));
        when(statisticsRepository.findByMeetingId(200L)).thenReturn(Optional.empty());
        when(statisticsRepository.save(any(MeetingStatistics.class))).thenAnswer(inv -> inv.getArgument(0));

        MeetingStatistics futureStats = analyticsService.generateMeetingStatistics(200L);
        assertEquals(MeetingStatistics.StatisticsStatus.DRAFT, futureStats.getStatus(), "Future meeting should be DRAFT");

        // Test dla spotkania trwającego (PRELIMINARY)
        Meeting ongoingMeeting = new Meeting();
        ongoingMeeting.setId(300L);
        ongoingMeeting.setTitle("Ongoing Meeting");
        ongoingMeeting.setStartDate(LocalDateTime.now().minusHours(1));
        ongoingMeeting.setEndDate(LocalDateTime.now().plusHours(1));
        ongoingMeeting.setOrganizer(organizer);

        when(meetingRepository.findById(300L)).thenReturn(Optional.of(ongoingMeeting));
        when(meetingParticipantService.getParticipantCounts(300L)).thenReturn(new ParticipantCountDto(5L, 3L, 5L, 0L, 0L, (long) 60.0, (long) 100.0));
        when(statisticsRepository.findByMeetingId(300L)).thenReturn(Optional.empty());

        MeetingStatistics ongoingStats = analyticsService.generateMeetingStatistics(300L);
        assertEquals(MeetingStatistics.StatisticsStatus.PRELIMINARY, ongoingStats.getStatus(), "Ongoing meeting should be PRELIMINARY");
    }
//
//    @Test
//    void generateOrganizerReport_shouldCalculateCorrectAverage() {
//        // Given
//        List<MeetingStatistics> statsList = Arrays.asList(
//                MeetingStatistics.builder()
//                        .meeting(meeting)
//                        .totalParticipants(10)
//                        .attendedParticipants(8)
//                        .attendanceRate(new BigDecimal("80.00"))
//                        .build(),
//                MeetingStatistics.builder()
//                        .meeting(createMeeting(LocalDateTime.now().minusDays(3)))
//                        .totalParticipants(20)
//                        .attendedParticipants(15)
//                        .attendanceRate(new BigDecimal("75.00"))
//                        .build()
//        );
//
//        when(statisticsRepository.findByOrganizerId(1L)).thenReturn(statsList);
//
//        // When
//        OrganizerReport report = analyticsService.generateOrganizerReport(1L, null);
//
//        // Then
//        assertAll(
//                () -> assertEquals(2, report.getTotalMeetings(), "Should include 2 meetings"),
//                () -> assertEquals(30, report.getTotalParticipants(), "Total participants should be 30"),
//                () -> assertEquals(23, report.getTotalAttended(), "Total attended should be 23"),
//                () -> assertEquals(new BigDecimal("77.50"), report.getAverageAttendanceRate(),
//                        "Average attendance should be 77.5% (mean of 80% and 75%)")
//        );
//    }
//

    @Test
    void getMeetingStatisticsByOrganizer_shouldReturnAllStatistics() {
        // Given
        List<MeetingStatistics> expectedStats = Arrays.asList(
                MeetingStatistics.builder().id(1L).meeting(meeting).build(),
                MeetingStatistics.builder().id(2L).meeting(meeting).build()
        );

        when(statisticsRepository.findByOrganizerId(1L)).thenReturn(expectedStats);

        // When
        List<MeetingStatistics> result = analyticsService.getMeetingStatisticsByOrganizer(1L);

        // Then
        assertEquals(2, result.size(), "Should return all statistics for organizer");
        verify(statisticsRepository).findByOrganizerId(1L);
    }

    @Test
    void getAverageResponseTime_shouldReturnZeroWhenNoStatistics() {
        // Given
        when(statisticsRepository.findByMeetingId(999L)).thenReturn(Optional.empty());

        // When
        BigDecimal result = analyticsService.getAverageResponseTime(999L);

        // Then
        assertEquals(BigDecimal.ZERO, result, "Should return zero when no statistics found");
    }

    @Test
    void getRecentStatistics_shouldReturnLimitedAndSortedResults() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        List<MeetingStatistics> allStats = Arrays.asList(
                MeetingStatistics.builder().id(1L).generatedAt(now.minusDays(3)).build(),
                MeetingStatistics.builder().id(2L).generatedAt(now.minusDays(1)).build(),
                MeetingStatistics.builder().id(3L).generatedAt(now.minusDays(2)).build(),
                MeetingStatistics.builder().id(4L).generatedAt(now.minusHours(6)).build(),
                MeetingStatistics.builder().id(5L).generatedAt(now.minusHours(12)).build()
        );

        when(statisticsRepository.findAll()).thenReturn(allStats);

        // When
        List<MeetingStatistics> result = analyticsService.getRecentStatistics(3);

        // Then
        assertAll(
                () -> assertEquals(3, result.size(), "Should return only 3 results"),
                () -> assertEquals(4L, result.get(0).getId(), "Most recent should be first"),
                () -> assertEquals(5L, result.get(1).getId(), "Second most recent should be second"),
                () -> assertEquals(2L, result.get(2).getId(), "Third most recent should be third")
        );
    }

    @Test
    void getRecentStatistics_shouldHandleEmptyRepository() {
        // Given
        when(statisticsRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        List<MeetingStatistics> result = analyticsService.getRecentStatistics(5);

        // Then
        assertTrue(result.isEmpty(), "Should return empty list for empty repository");
    }

    @Test
    void refreshAllStatistics_shouldRefreshAllMeetings() {
        // Given
        List<MeetingStatistics> allStats = Arrays.asList(
                MeetingStatistics.builder().id(1L).meeting(meeting).build(),
                MeetingStatistics.builder().id(2L).meeting(meeting).build()
        );

        when(statisticsRepository.findAll()).thenReturn(allStats);
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(meeting));
        when(meetingParticipantService.getParticipantCounts(100L)).thenReturn(participantCounts);

        // When
        analyticsService.refreshAllStatistics();

        // Then
        verify(meetingRepository, times(2)).findById(100L);
        verify(statisticsRepository, times(2)).save(any(MeetingStatistics.class));
    }

    @Test
    void refreshAllStatistics_shouldHandleExceptionsGracefully() {
        // Given
        MeetingStatistics validStats = MeetingStatistics.builder()
                .id(1L)
                .meeting(meeting)
                .build();

        MeetingStatistics invalidStats = MeetingStatistics.builder()
                .id(2L)
                .meeting(null) // Meeting is null
                .build();

        List<MeetingStatistics> allStats = Arrays.asList(validStats, invalidStats);

        when(statisticsRepository.findAll()).thenReturn(allStats);
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(meeting));
        when(meetingParticipantService.getParticipantCounts(100L)).thenReturn(participantCounts);

        // When - should not throw exception
        assertDoesNotThrow(() -> analyticsService.refreshAllStatistics());

        // Then - should refresh valid stats only
        verify(meetingRepository).findById(100L);
        verify(statisticsRepository).save(any(MeetingStatistics.class));
    }

    @Test
    void generateMeetingStatistics_shouldThrowExceptionWhenMeetingNotFound() {
        // Given
        when(meetingRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> analyticsService.generateMeetingStatistics(999L));
        assertEquals("Meeting not found", exception.getMessage());
    }

//    @Test
//    void exportMeetingStatisticsToPdf_shouldThrowExceptionWhenNoStatistics() {
//        // Given
//        when(statisticsRepository.findByMeetingId(999L)).thenReturn(Optional.empty());
//
//        // When & Then
//        RuntimeException exception = assertThrows(RuntimeException.class,
//                () -> analyticsService.exportMeetingStatisticsToPdf(999L));
//        assertTrue(exception.getMessage().contains("No statistics found"));
//    }

    @Test
    void exportMeetingStatisticsToCsv_shouldThrowExceptionWhenNoStatistics() {
        // Given
        when(statisticsRepository.findByMeetingId(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> analyticsService.exportMeetingStatisticsToCsv(999L));
        assertTrue(exception.getMessage().contains("No statistics found"));
    }

    @Test
    void getStatisticsOverview_shouldThrowExceptionWhenNoStatistics() {
        // Given
        when(statisticsRepository.findByMeetingId(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> analyticsService.getStatisticsOverview(999L));
        assertTrue(exception.getMessage().contains("No statistics found"));
    }
//
//    @Test
//    void testFilterStatistics_withDateFilters() {
//        // Given
//        LocalDateTime now = LocalDateTime.now();
//        MeetingStatistics stats1 = MeetingStatistics.builder()
//                .meeting(createMeeting(now.minusDays(5)))
//                .build();
//        MeetingStatistics stats2 = MeetingStatistics.builder()
//                .meeting(createMeeting(now.minusDays(15)))
//                .build();
//        MeetingStatistics stats3 = MeetingStatistics.builder()
//                .meeting(createMeeting(now.minusDays(25)))
//                .build();
//
//        List<MeetingStatistics> allStats = Arrays.asList(stats1, stats2, stats3);
//
//        ReportFilter filter = new ReportFilter();
//        filter.setDateFrom(now.minusDays(20));
//        filter.setDateTo(now.minusDays(10));
//
//        // When
//        Integer filtered = analyticsService.generateOrganizerReport(1L, filter)
//                .getTotalMeetings(); // Using this to trigger filtering
//
//        // Mock setup for the test
//        when(statisticsRepository.findByOrganizerId(1L)).thenReturn(allStats);
//
//        // Get the actual report
//        OrganizerReport report = analyticsService.generateOrganizerReport(1L, filter);
//
//        // Then - should only include stats2 (within date range)
//        assertEquals(1, report.getTotalMeetings(), "Should only include 1 meeting within date range");
//    }
//
//    @Test
//    void testFilterStatistics_withNullMeeting() {
//        // Given
//        MeetingStatistics statsWithNullMeeting = MeetingStatistics.builder()
//                .meeting(null) // Meeting is null
//                .build();
//
//        MeetingStatistics statsWithMeeting = MeetingStatistics.builder()
//                .meeting(createMeeting(LocalDateTime.now()))
//                .build();
//
//        List<MeetingStatistics> allStats = Arrays.asList(statsWithNullMeeting, statsWithMeeting);
//
//        ReportFilter filter = new ReportFilter();
//        filter.setDateFrom(LocalDateTime.now().minusDays(1));
//
//        // Mock
//        when(statisticsRepository.findByOrganizerId(1L)).thenReturn(allStats);
//
//        // When
//        OrganizerReport report = analyticsService.generateOrganizerReport(1L, filter);
//
//        // Then - should exclude stats with null meeting
//        assertEquals(1, report.getTotalMeetings(), "Should exclude statistics with null meeting");
//    }
//


    @Test
    void exportMeetingStatisticsToCsv_shouldReturnValidCsvContent() {
        // Given
        MeetingStatistics stats = MeetingStatistics.builder()
                .id(1L)
                .meeting(meeting)
                .totalParticipants(10)
                .attendedParticipants(8)
                .attendanceRate(new BigDecimal("80.00"))
                .confirmedParticipants(9)
                .confirmationRate(new BigDecimal("90.00"))
                .avgResponseTimeMinutes(new BigDecimal("45.5"))
                .generatedAt(LocalDateTime.now())
                .build();

        when(statisticsRepository.findByMeetingId(100L)).thenReturn(Optional.of(stats));

        // When
        byte[] csvBytes = analyticsService.exportMeetingStatisticsToCsv(100L);
        String csv = new String(csvBytes);

        // Then
        assertAll(
                () -> assertNotNull(csvBytes, "CSV should not be null"),
                () -> assertTrue(csv.contains("Meeting Statistics"), "Should contain header"),
                () -> assertTrue(csv.contains("Meeting ID: 100"), "Should contain meeting ID"),
                () -> assertTrue(csv.contains("Total Participants: 10"), "Should contain total participants"),
                () -> assertTrue(csv.contains("Attendance Rate: 80.00%"), "Should contain attendance rate"),
                () -> assertTrue(csv.contains("Average Response Time: 45.5 minutes"), "Should contain response time")
        );
    }

    @Test
    void testCalculateDerivedMetricsIsCalled() {
        // Given
        MeetingStatistics existingStats = mock(MeetingStatistics.class);

        when(meetingRepository.findById(100L)).thenReturn(Optional.of(meeting));
        when(meetingParticipantService.getParticipantCounts(100L)).thenReturn(participantCounts);
        when(feedbackRepository.findAverageRatingByMeetingId(100L)).thenReturn(4.5);
        when(feedbackRepository.countByMeetingId(100L)).thenReturn(5L);
        when(statisticsRepository.findByMeetingId(100L)).thenReturn(Optional.of(existingStats));
        when(statisticsRepository.save(any(MeetingStatistics.class))).thenReturn(existingStats);

        // When
        analyticsService.generateMeetingStatistics(100L);

        // Then - verify calculateDerivedMetrics was called
        verify(existingStats).calculateDerivedMetrics();
    }

    private Meeting createMeeting(LocalDateTime date) {
        Meeting m = new Meeting();
        m.setId(1L);
        m.setTitle("Meeting");
        m.setStartDate(date);
        m.setOrganizer(organizer);
        return m;
    }



    @Test
    void exportMeetingStatisticsToPdf_shouldThrowExceptionWhenNoStatistics() {
        // Given
        when(statisticsRepository.findByMeetingId(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> analyticsService.exportMeetingStatisticsToPdf(999L));
        assertTrue(exception.getMessage().contains("No statistics found"));
    }



    @Test
    void addAttendanceChart_shouldHandleTotalZero() {
        // Test the else branch when total = 0
        MeetingStatistics statsWithZero = MeetingStatistics.builder()
                .id(1L)
                .meeting(meeting)
                .totalParticipants(0)  // Total = 0 triggers else branch
                .attendedParticipants(0)
                .confirmedParticipants(0)
                .declinedParticipants(0)
                .pendingParticipants(0)
                .status(MeetingStatistics.StatisticsStatus.FINAL) // DODAJ STATUS
                .generatedAt(LocalDateTime.now())
                .build();

        when(statisticsRepository.findByMeetingId(100L)).thenReturn(Optional.of(statsWithZero));

        // Should handle gracefully without exception
        assertDoesNotThrow(() -> {
            byte[] pdf = analyticsService.exportMeetingStatisticsToPdf(100L);
            assertNotNull(pdf);
        });
    }

    @Test
    void addAttendanceChart_shouldHandleNullValues() {
        // Test with all null values
        MeetingStatistics statsWithNulls = MeetingStatistics.builder()
                .id(1L)
                .meeting(meeting)
                .totalParticipants(null)
                .attendedParticipants(null)
                .confirmedParticipants(null)
                .declinedParticipants(null)
                .pendingParticipants(null)
                .status(MeetingStatistics.StatisticsStatus.FINAL) // DODAJ STATUS
                .generatedAt(LocalDateTime.now())
                .build();

        when(statisticsRepository.findByMeetingId(100L)).thenReturn(Optional.of(statsWithNulls));

        assertDoesNotThrow(() -> {
            byte[] pdf = analyticsService.exportMeetingStatisticsToPdf(100L);
            assertNotNull(pdf);
        });
    }

    @Test
    void addAttendanceChart_shouldHandlePartialNullValues() {
        // Test with mixed null and non-null values
        MeetingStatistics stats = MeetingStatistics.builder()
                .id(1L)
                .meeting(meeting)
                .totalParticipants(10)
                .attendedParticipants(8)
                .confirmedParticipants(null)  // Partial null
                .declinedParticipants(1)
                .pendingParticipants(null)    // Partial null
                .status(MeetingStatistics.StatisticsStatus.FINAL) // DODAJ STATUS
                .generatedAt(LocalDateTime.now())
                .build();

        when(statisticsRepository.findByMeetingId(100L)).thenReturn(Optional.of(stats));

        assertDoesNotThrow(() -> {
            byte[] pdf = analyticsService.exportMeetingStatisticsToPdf(100L);
            assertNotNull(pdf);
        });
    }

    @Test
    void addBasicStatistics_shouldHandleAllNullAndZeroValues() {
        // Test addBasicStatistics with various null scenarios
        MeetingStatistics statsWithAllNulls = MeetingStatistics.builder()
                .id(1L)
                .meeting(meeting)
                .totalParticipants(null)
                .attendedParticipants(null)
                .confirmedParticipants(null)
                .declinedParticipants(null)
                .pendingParticipants(null)
                .attendanceRate(null)
                .confirmationRate(null)
                .averageRating(null)
                .feedbackCount(null)
                .avgResponseTimeMinutes(null)
                .status(MeetingStatistics.StatisticsStatus.FINAL) // DODAJ STATUS
                .generatedAt(LocalDateTime.now())
                .build();

        when(statisticsRepository.findByMeetingId(100L)).thenReturn(Optional.of(statsWithAllNulls));

        assertDoesNotThrow(() -> {
            byte[] pdf = analyticsService.exportMeetingStatisticsToPdf(100L);
            assertNotNull(pdf);
        }, "Should handle all null values in basic statistics");
    }

    @Test
    void addBasicStatistics_shouldHandleZeroRatingAndFeedback() {
        // Test edge cases for rating and feedback
        MeetingStatistics statsWithZeros = MeetingStatistics.builder()
                .id(1L)
                .meeting(meeting)
                .totalParticipants(10)
                .attendedParticipants(8)
                .averageRating(BigDecimal.ZERO) // Zero rating
                .feedbackCount(0) // Zero feedback
                .avgResponseTimeMinutes(BigDecimal.ZERO) // Zero response time
                .status(MeetingStatistics.StatisticsStatus.FINAL) // DODAJ STATUS
                .generatedAt(LocalDateTime.now())
                .build();

        when(statisticsRepository.findByMeetingId(100L)).thenReturn(Optional.of(statsWithZeros));

        assertDoesNotThrow(() -> {
            byte[] pdf = analyticsService.exportMeetingStatisticsToPdf(100L);
            assertNotNull(pdf);
        }, "Should handle zero values for rating and feedback");
    }

    @Test
    void addBasicStatistics_shouldHandlePositiveRatingAndFeedback() {
        // Test with positive values
        MeetingStatistics statsWithValues = MeetingStatistics.builder()
                .id(1L)
                .meeting(meeting)
                .totalParticipants(10)
                .attendedParticipants(8)
                .averageRating(new BigDecimal("4.75")) // Positive rating
                .feedbackCount(5) // Positive feedback count
                .avgResponseTimeMinutes(new BigDecimal("30.5")) // Positive response time
                .status(MeetingStatistics.StatisticsStatus.FINAL) // DODAJ STATUS
                .generatedAt(LocalDateTime.now())
                .build();

        when(statisticsRepository.findByMeetingId(100L)).thenReturn(Optional.of(statsWithValues));

        assertDoesNotThrow(() -> {
            byte[] pdf = analyticsService.exportMeetingStatisticsToPdf(100L);
            assertNotNull(pdf);
        }, "Should handle positive values for rating and feedback");
    }


    @Test
    void addSummary_shouldHandleNullStatus() {
        // Test with null status (edge case)
        MeetingStatistics nullStatus = MeetingStatistics.builder()
                .id(1L)
                .meeting(meeting)
                .attendanceRate(new BigDecimal("85.00"))
                .averageRating(new BigDecimal("4.5"))
                .status(null) // Null status - should be handled
                .generatedAt(LocalDateTime.now())
                .build();

        when(statisticsRepository.findByMeetingId(100L)).thenReturn(Optional.of(nullStatus));

        // Should either handle gracefully or throw expected exception
        try {
            byte[] pdf = analyticsService.exportMeetingStatisticsToPdf(100L);
            assertNotNull(pdf);
        } catch (RuntimeException e) {
            // If it throws exception, it should be for null status
            assertTrue(e.getMessage().contains("status") || e.getMessage().contains("null"));
        }
    }

    @Test
    void addMeetingInfo_shouldHandleNullMeetingDetails() {
        // Test with meeting having null fields
        Meeting incompleteMeeting = new Meeting();
        incompleteMeeting.setId(1L);
        incompleteMeeting.setTitle(null); // Null title
        incompleteMeeting.setOrganizer(organizer);
        incompleteMeeting.setStartDate(null); // Null start date
        incompleteMeeting.setEndDate(null); // Null end date

        MeetingStatistics stats = MeetingStatistics.builder()
                .id(1L)
                .meeting(incompleteMeeting)
                .totalParticipants(10)
                .attendedParticipants(8)
                .status(MeetingStatistics.StatisticsStatus.FINAL)
                .generatedAt(LocalDateTime.now())
                .build();

        when(statisticsRepository.findByMeetingId(100L)).thenReturn(Optional.of(stats));

        assertDoesNotThrow(() -> {
            byte[] pdf = analyticsService.exportMeetingStatisticsToPdf(100L);
            assertNotNull(pdf);
        }, "Should handle null meeting details");
    }


    @Test
    void testPdfGenerationForVariousMeetingStatuses() {
        // Test PDF generation for meetings with different statuses

        // Completed meeting (should have end date in past)
        Meeting completedMeeting = new Meeting();
        completedMeeting.setId(1L);
        completedMeeting.setTitle("Completed Meeting");
        completedMeeting.setOrganizer(organizer);
        completedMeeting.setStartDate(LocalDateTime.now().minusDays(2));
        completedMeeting.setEndDate(LocalDateTime.now().minusDays(1));

        MeetingStatistics statsForCompleted = MeetingStatistics.builder()
                .id(1L)
                .meeting(completedMeeting)
                .totalParticipants(10)
                .attendedParticipants(8)
                .status(MeetingStatistics.StatisticsStatus.FINAL)
                .generatedAt(LocalDateTime.now())
                .build();

        // Ongoing meeting
        Meeting ongoingMeeting = new Meeting();
        ongoingMeeting.setId(2L);
        ongoingMeeting.setTitle("Ongoing Meeting");
        ongoingMeeting.setOrganizer(organizer);
        ongoingMeeting.setStartDate(LocalDateTime.now().minusHours(2));
        ongoingMeeting.setEndDate(LocalDateTime.now().plusHours(2));

        MeetingStatistics statsForOngoing = MeetingStatistics.builder()
                .id(2L)
                .meeting(ongoingMeeting)
                .totalParticipants(10)
                .attendedParticipants(5)
                .status(MeetingStatistics.StatisticsStatus.PRELIMINARY)
                .generatedAt(LocalDateTime.now())
                .build();

        when(statisticsRepository.findByMeetingId(100L)).thenReturn(Optional.of(statsForCompleted));
        when(statisticsRepository.findByMeetingId(200L)).thenReturn(Optional.of(statsForOngoing));

        assertAll(
                () -> assertDoesNotThrow(() -> {
                    byte[] pdf1 = analyticsService.exportMeetingStatisticsToPdf(100L);
                    assertNotNull(pdf1);
                }, "Should handle completed meeting"),
                () -> assertDoesNotThrow(() -> {
                    byte[] pdf2 = analyticsService.exportMeetingStatisticsToPdf(200L);
                    assertNotNull(pdf2);
                }, "Should handle ongoing meeting")
        );
    }
}
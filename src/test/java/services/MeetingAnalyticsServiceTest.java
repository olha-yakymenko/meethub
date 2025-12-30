package com.meethub.domain.service.impl;

import com.meethub.domain.model.dto.OrganizerReportStats;
import com.meethub.domain.model.dto.ParticipantCountDto;
import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.MeetingStatistics;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.MeetingStatus;
import com.meethub.domain.model.request.ReportFilter;
import com.meethub.domain.model.response.OrganizerReport;
import com.meethub.domain.repository.jpa.FeedbackRepository;
import com.meethub.domain.repository.jpa.MeetingParticipantRepository;
import com.meethub.domain.repository.jpa.MeetingRepository;
import com.meethub.domain.repository.jpa.MeetingStatisticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingAnalyticsServiceTest {

    @Mock
    private MeetingStatisticsRepository statisticsRepository;

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private MeetingParticipantRepository meetingParticipantRepository;

    @Mock
    private FeedbackRepository feedbackRepository;

    @InjectMocks
    private MeetingAnalyticsServiceImpl analyticsService;

    private Meeting meeting;
    private User organizer;
    private ParticipantCountDto participantCounts;

    @BeforeEach
    void setUp() {
        // Create organizer
        organizer = new User();
        organizer.setId(1L);
        organizer.setFirstName("Organizer");
        organizer.setLastName("Test");

        // Create meeting
        meeting = new Meeting();
        meeting.setId(100L);
        meeting.setTitle("Test Meeting");
        meeting.setDescription("Test meeting description");
        meeting.setStartDate(LocalDateTime.now().minusDays(2));
        meeting.setEndDate(LocalDateTime.now().minusDays(1));
        meeting.setOrganizer(organizer);
        meeting.setStatus(MeetingStatus.COMPLETED);

        // Create participant counts
        participantCounts = ParticipantCountDto.builder()
                .total(10L)
                .confirmed(8L)
                .attended(6L)
                .declined(2L)
                .cancelled(0L)
                .invited(0L)
                .pending(0L)
                .build();
    }

    // ========== generateMeetingStatistics ==========

    @Test
    void generateMeetingStatistics_shouldCreateNewStatistics_whenNotExists() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(meeting));
        when(meetingParticipantRepository.getParticipantCounts(100L)).thenReturn(participantCounts);
        when(feedbackRepository.findAverageRatingByMeetingId(100L)).thenReturn(4.5);
        when(feedbackRepository.countByMeetingId(100L)).thenReturn(5L);
        when(statisticsRepository.findByMeetingId(100L)).thenReturn(Optional.empty());
        when(statisticsRepository.save(any(MeetingStatistics.class))).thenAnswer(inv -> {
            MeetingStatistics stats = inv.getArgument(0);
            stats.setId(1L);
            return stats;
        });

        // When
        MeetingStatistics result = analyticsService.generateMeetingStatistics(100L);

        // Then
        assertAll(
                () -> assertNotNull(result, "Statistics should not be null"),
                () -> assertEquals(100L, result.getMeeting().getId(), "Should be for correct meeting"),
                () -> assertEquals(10, result.getTotalParticipants(), "Total participants should match"),
                () -> assertEquals(6, result.getAttendedParticipants(), "Attended participants should match"),
                () -> assertEquals(8, result.getConfirmedParticipants(), "Confirmed participants should match"),
                () -> assertEquals(2, result.getDeclinedParticipants(), "Declined participants should match"),
                () -> assertEquals(new BigDecimal("60.00"), result.getAttendanceRate(), "Attendance rate should be 60%"),
                () -> assertEquals(new BigDecimal("80.00"), result.getConfirmationRate(), "Confirmation rate should be 80%"),
                () -> assertEquals(new BigDecimal("4.50"), result.getAverageRating(), "Average rating should match"),
                () -> assertEquals(5, result.getFeedbackCount(), "Feedback count should match"),
                () -> assertNotNull(result.getGeneratedAt(), "Generated timestamp should be set"),
                () -> assertEquals(MeetingStatistics.StatisticsStatus.FINAL, result.getStatus(), "Status should be FINAL for completed meeting"),
                () -> assertTrue(result.getFinalized(), "Should be finalized for completed meeting")
        );

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

    @Test
    void generateMeetingStatistics_shouldHandleFeedbackException() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(meeting));
        when(meetingParticipantRepository.getParticipantCounts(100L)).thenReturn(participantCounts);
        when(feedbackRepository.findAverageRatingByMeetingId(100L)).thenThrow(new RuntimeException("DB Error"));
        when(statisticsRepository.findByMeetingId(100L)).thenReturn(Optional.empty());
        when(statisticsRepository.save(any(MeetingStatistics.class))).thenAnswer(inv -> {
            MeetingStatistics stats = inv.getArgument(0);
            stats.setId(1L);
            return stats;
        });

        // When
        MeetingStatistics result = analyticsService.generateMeetingStatistics(100L);

        // Then
        assertAll(
                () -> assertNotNull(result, "Should handle feedback exception gracefully"),
                () -> assertEquals(BigDecimal.ZERO, result.getAverageRating(), "Rating should be zero on exception"),
                () -> assertEquals(0, result.getFeedbackCount(), "Feedback count should be zero on exception"),
                () -> assertEquals(10, result.getTotalParticipants(), "Should still have participant counts")
        );
    }

    @Test
    void generateMeetingStatistics_shouldSetStatusBasedOnMeetingTime() {
        // Test for future meeting (DRAFT)
        Meeting futureMeeting = new Meeting();
        futureMeeting.setId(200L);
        futureMeeting.setTitle("Future Meeting");
        futureMeeting.setStartDate(LocalDateTime.now().plusDays(1));
        futureMeeting.setEndDate(LocalDateTime.now().plusDays(2));
        futureMeeting.setOrganizer(organizer);
        futureMeeting.setStatus(MeetingStatus.PLANNED);

        ParticipantCountDto futureCounts = ParticipantCountDto.builder()
                .total(5L)
                .confirmed(5L)
                .attended(0L)
                .declined(0L)
                .cancelled(0L)
                .invited(0L)
                .pending(0L)
                .build();

        when(meetingRepository.findById(200L)).thenReturn(Optional.of(futureMeeting));
        when(meetingParticipantRepository.getParticipantCounts(200L)).thenReturn(futureCounts);
        when(statisticsRepository.findByMeetingId(200L)).thenReturn(Optional.empty());
        when(statisticsRepository.save(any(MeetingStatistics.class))).thenAnswer(inv -> {
            MeetingStatistics stats = inv.getArgument(0);
            stats.setId(200L);
            return stats;
        });

        MeetingStatistics futureStats = analyticsService.generateMeetingStatistics(200L);
        assertEquals(MeetingStatistics.StatisticsStatus.DRAFT, futureStats.getStatus(), "Future meeting should be DRAFT");

        // Test for ongoing meeting (PRELIMINARY)
        Meeting ongoingMeeting = new Meeting();
        ongoingMeeting.setId(300L);
        ongoingMeeting.setTitle("Ongoing Meeting");
        ongoingMeeting.setStartDate(LocalDateTime.now().minusHours(1));
        ongoingMeeting.setEndDate(LocalDateTime.now().plusHours(1));
        ongoingMeeting.setOrganizer(organizer);
        ongoingMeeting.setStatus(MeetingStatus.COMPLETED);

        ParticipantCountDto ongoingCounts = ParticipantCountDto.builder()
                .total(5L)
                .confirmed(5L)
                .attended(3L)
                .declined(0L)
                .cancelled(0L)
                .invited(0L)
                .pending(0L)
                .build();

        when(meetingRepository.findById(300L)).thenReturn(Optional.of(ongoingMeeting));
        when(meetingParticipantRepository.getParticipantCounts(300L)).thenReturn(ongoingCounts);
        when(statisticsRepository.findByMeetingId(300L)).thenReturn(Optional.empty());

        MeetingStatistics ongoingStats = analyticsService.generateMeetingStatistics(300L);
        assertEquals(MeetingStatistics.StatisticsStatus.PRELIMINARY, ongoingStats.getStatus(), "Ongoing meeting should be PRELIMINARY");
    }

    // ========== getMeetingStatistics ==========

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

        // Then
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

        // Then
        assertThat(result).isEmpty();
    }

    // ========== deleteMeetingStatistics ==========

    @Test
    void deleteMeetingStatistics_shouldCallRepository() {
        // When
        analyticsService.deleteMeetingStatistics(100L);

        // Then
        verify(statisticsRepository).deleteByMeetingId(100L);
    }

    // ========== generateOrganizerReport ==========

    @Test
    void generateOrganizerReport_shouldReturnValidReport() {
        // Given
        OrganizerReportStats stats = new OrganizerReportStats(
                2L,
                new BigDecimal("85.50"),
                30L,
                25L
        );

        when(statisticsRepository.getOrganizerReportStats(1L)).thenReturn(stats);

        // When
        OrganizerReport report = analyticsService.generateOrganizerReport(1L, null);

        // Then
        assertAll(
                () -> assertNotNull(report, "Report should not be null"),
                () -> assertEquals(1L, report.getOrganizerId(), "Organizer ID should match"),
                () -> assertEquals(2, report.getTotalMeetings(), "Should have 2 meetings"),
                () -> assertEquals(30, report.getTotalParticipants(), "Should have 30 participants"),
                () -> assertEquals(25, report.getTotalAttended(), "Should have 25 attended"),
                () -> assertEquals(new BigDecimal("85.50"), report.getAverageAttendanceRate(),
                        "Average attendance should be 85.5%")
        );
    }

    @Test
    void generateOrganizerReport_shouldFilterByDate() {
        // Given
        OrganizerReportStats stats = new OrganizerReportStats(
                1L,
                new BigDecimal("90.00"),
                15L,
                14L
        );

        ReportFilter filter = new ReportFilter();
        filter.setDateFrom(LocalDateTime.now().minusDays(30));
        filter.setDateTo(LocalDateTime.now());

        when(statisticsRepository.getOrganizerReportStatsByDateRange(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(stats);

        // When
        OrganizerReport report = analyticsService.generateOrganizerReport(1L, filter);

        // Then
        assertAll(
                () -> assertEquals(1, report.getTotalMeetings(), "Should only include 1 meeting in date range"),
                () -> assertEquals(15, report.getTotalParticipants(), "Should have 15 participants")
        );
    }

    @Test
    void generateOrganizerReport_shouldHandleNullStats() {
        // Given
        when(statisticsRepository.getOrganizerReportStats(1L)).thenReturn(null);

        // When
        OrganizerReport report = analyticsService.generateOrganizerReport(1L, null);

        // Then
        assertAll(
                () -> assertNotNull(report),
                () -> assertEquals(0, report.getTotalMeetings()),
                () -> assertEquals(0, report.getTotalParticipants()),
                () -> assertEquals(0, report.getTotalAttended()),
                () -> assertEquals(BigDecimal.ZERO, report.getAverageAttendanceRate())
        );
    }

    // ========== getStatisticsOverview ==========

    @Test
    void getStatisticsOverview_shouldReturnCompleteMap() {
        // Given
        MeetingStatistics stats = MeetingStatistics.builder()
                .id(1L)
                .meeting(meeting)
                .totalParticipants(10)
                .attendedParticipants(8)
                .confirmedParticipants(9)
                .attendanceRate(BigDecimal.valueOf(80.0))
                .confirmationRate(BigDecimal.valueOf(90.0))
                .avgResponseTimeMinutes(BigDecimal.valueOf(30.0))
                .generatedAt(LocalDateTime.now())
                .status(MeetingStatistics.StatisticsStatus.FINAL)
                .finalized(true)
                .build();

        when(statisticsRepository.findByMeetingId(100L)).thenReturn(Optional.of(stats));

        // When
        Map<String, Object> overview = analyticsService.getStatisticsOverview(100L);

        // Then
        assertThat(overview)
                .containsKeys(
                        "meetingId", "attendanceRate", "totalParticipants",
                        "attendedParticipants", "confirmedParticipants",
                        "avgResponseTime", "generatedAt", "status", "finalized"
                );

        assertAll(
                () -> assertEquals(100L, overview.get("meetingId")),
                () -> assertEquals(new BigDecimal("80.0"), overview.get("attendanceRate")),
                () -> assertEquals(10, overview.get("totalParticipants")),
                () -> assertEquals(8, overview.get("attendedParticipants")),
                () -> assertEquals(9, overview.get("confirmedParticipants")),
                () -> assertEquals(new BigDecimal("30.0"), overview.get("avgResponseTime")),
                () -> assertEquals("FINAL", overview.get("status")),
                () -> assertEquals(true, overview.get("finalized"))
        );
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

    // ========== getRecentStatistics ==========

    @Test
    void getRecentStatistics_shouldReturnLimitedStatistics() {
        // Given
        List<MeetingStatistics> recentStats = Arrays.asList(
                createTestStatistics(6L, 12, 9, new BigDecimal("75.00")),
                createTestStatistics(5L, 30, 25, new BigDecimal("83.33")),
                createTestStatistics(4L, 25, 20, new BigDecimal("80.00"))
        );

        PageRequest pageRequest = PageRequest.of(0, 3);
        when(statisticsRepository.findRecentStatistics(pageRequest)).thenReturn(recentStats);

        // When
        List<MeetingStatistics> result = analyticsService.getRecentStatistics(3);

        // Then
        assertAll(
                () -> assertEquals(3, result.size()),
                () -> assertEquals(6L, result.get(0).getMeeting().getId()),
                () -> assertEquals(5L, result.get(1).getMeeting().getId()),
                () -> assertEquals(4L, result.get(2).getMeeting().getId())
        );
    }

    @Test
    void getRecentStatistics_shouldReturnAll_whenLimitGreaterThanSize() {
        // Given
        List<MeetingStatistics> allStats = Arrays.asList(
                createTestStatistics(1L, 10, 8, new BigDecimal("80.00")),
                createTestStatistics(2L, 20, 15, new BigDecimal("75.00"))
        );

        PageRequest pageRequest = PageRequest.of(0, 10);
        when(statisticsRepository.findRecentStatistics(pageRequest)).thenReturn(allStats);

        // When
        List<MeetingStatistics> result = analyticsService.getRecentStatistics(10);

        // Then
        assertEquals(2, result.size());
    }

    @Test
    void getRecentStatistics_shouldHandleEmptyRepository() {
        // Given
        PageRequest pageRequest = PageRequest.of(0, 5);
        when(statisticsRepository.findRecentStatistics(pageRequest)).thenReturn(Collections.emptyList());

        // When
        List<MeetingStatistics> result = analyticsService.getRecentStatistics(5);

        // Then
        assertAll(
                () -> assertNotNull(result),
                () -> assertTrue(result.isEmpty())
        );
    }

    @Test
    void getRecentStatistics_shouldReturnLimitedAndSortedResults() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        MeetingStatistics stats1 = createTestStatistics(1L, 10, 8, new BigDecimal("80.00"));
        stats1.setGeneratedAt(now.minusDays(3));

        MeetingStatistics stats2 = createTestStatistics(2L, 20, 15, new BigDecimal("75.00"));
        stats2.setGeneratedAt(now.minusDays(1));

        MeetingStatistics stats3 = createTestStatistics(3L, 15, 10, new BigDecimal("66.67"));
        stats3.setGeneratedAt(now.minusDays(2));

        MeetingStatistics stats4 = createTestStatistics(4L, 25, 20, new BigDecimal("80.00"));
        stats4.setGeneratedAt(now.minusHours(6));

        MeetingStatistics stats5 = createTestStatistics(5L, 30, 25, new BigDecimal("83.33"));
        stats5.setGeneratedAt(now.minusHours(12));

        List<MeetingStatistics> recentStats = Arrays.asList(stats4, stats5, stats2); // Most recent 3

        PageRequest pageRequest = PageRequest.of(0, 3);
        when(statisticsRepository.findRecentStatistics(pageRequest)).thenReturn(recentStats);

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

    // ========== getAverageResponseTime ==========

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
    void getAverageResponseTime_shouldReturnZeroWhenNoStatistics() {
        // Given
        when(statisticsRepository.findByMeetingId(999L)).thenReturn(Optional.empty());

        // When
        BigDecimal result = analyticsService.getAverageResponseTime(999L);

        // Then
        assertEquals(BigDecimal.ZERO, result, "Should return zero when no statistics found");
    }

    @Test
    void getAverageResponseTime_shouldReturnZeroWhenResponseTimeIsNull() {
        // Given
        MeetingStatistics stats = MeetingStatistics.builder()
                .id(1L)
                .meeting(meeting)
                .avgResponseTimeMinutes(null)
                .build();

        when(statisticsRepository.findByMeetingId(100L)).thenReturn(Optional.of(stats));

        // When
        BigDecimal result = analyticsService.getAverageResponseTime(100L);

        // Then
        assertEquals(BigDecimal.ZERO, result);
    }

    // ========== getMeetingStatisticsByOrganizer ==========

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
    void getMeetingStatisticsByOrganizer_shouldReturnEmptyList_whenNoStatistics() {
        // Given
        when(statisticsRepository.findByOrganizerId(1L)).thenReturn(Collections.emptyList());

        // When
        List<MeetingStatistics> result = analyticsService.getMeetingStatisticsByOrganizer(1L);

        // Then
        assertAll(
                () -> assertNotNull(result),
                () -> assertTrue(result.isEmpty())
        );
    }

    // ========== exportMeetingStatisticsToCsv ==========

    @Test
    void exportMeetingStatisticsToCsv_shouldReturnNonEmptyByteArray() {
        // Given
        MeetingStatistics stats = MeetingStatistics.builder()
                .id(1L)
                .meeting(meeting)
                .totalParticipants(10)
                .attendedParticipants(8)
                .attendanceRate(BigDecimal.valueOf(80.0))
                .confirmationRate(BigDecimal.valueOf(90.0))
                .avgResponseTimeMinutes(BigDecimal.valueOf(30.0))
                .generatedAt(LocalDateTime.now())
                .build();

        when(statisticsRepository.findByMeetingId(100L)).thenReturn(Optional.of(stats));

        // When
        byte[] csv = analyticsService.exportMeetingStatisticsToCsv(100L);

        // Then
        assertAll(
                () -> assertNotNull(csv, "CSV should not be null"),
                () -> assertTrue(csv.length > 0, "CSV should not be empty")
        );
    }

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
    void exportMeetingStatisticsToCsv_shouldThrowExceptionWhenNoStatistics() {
        // Given
        when(statisticsRepository.findByMeetingId(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> analyticsService.exportMeetingStatisticsToCsv(999L));
        assertTrue(exception.getMessage().contains("No statistics found"));
    }

    // ========== exportMeetingStatisticsToPdf ==========

    @Test
    void exportMeetingStatisticsToPdf_shouldGeneratePdfBytes() {
        // Given
        MeetingStatistics stats = MeetingStatistics.builder()
                .id(1L)
                .meeting(meeting)
                .totalParticipants(10)
                .attendedParticipants(8)
                .attendanceRate(new BigDecimal("80.00"))
                .generatedAt(LocalDateTime.now())
                .status(MeetingStatistics.StatisticsStatus.FINAL)
                .build();

        when(statisticsRepository.findByMeetingId(100L)).thenReturn(Optional.of(stats));

        // When
        byte[] pdfBytes = analyticsService.exportMeetingStatisticsToPdf(100L);

        // Then
        assertAll(
                () -> assertNotNull(pdfBytes),
                () -> assertTrue(pdfBytes.length > 0)
        );
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
    void testPdfGenerationForVariousMeetingStatuses() {
        // Test PDF generation for meetings with different statuses

        // Completed meeting
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

    // ========== exportReportToCsv ==========

    @Test
    void exportReportToCsv_shouldGenerateCsvBytes() {
        // Given
        OrganizerReportStats stats = new OrganizerReportStats(
                2L,
                new BigDecimal("77.50"),
                30L,
                23L
        );

        when(statisticsRepository.getOrganizerReportStats(1L)).thenReturn(stats);

        // When
        byte[] csvBytes = analyticsService.exportReportToCsv(1L, null);
        String csvString = new String(csvBytes);

        // Then
        assertAll(
                () -> assertNotNull(csvBytes),
                () -> assertTrue(csvBytes.length > 0),
                () -> assertTrue(csvString.contains("Organizer Report")),
                () -> assertTrue(csvString.contains("Organizer ID: 1")),
                () -> assertTrue(csvString.contains("Total Meetings: 2"))
        );
    }

    // ========== exportReportToPdf ==========

    @Test
    void exportReportToPdf_shouldGeneratePdfBytes() {
        // Given
        OrganizerReportStats stats = new OrganizerReportStats(
                2L,
                new BigDecimal("77.50"),
                30L,
                23L
        );

        when(statisticsRepository.getOrganizerReportStats(1L)).thenReturn(stats);

        // When
        byte[] pdfBytes = analyticsService.exportReportToPdf(1L, null);

        // Then
        assertAll(
                () -> assertNotNull(pdfBytes),
                () -> assertTrue(pdfBytes.length > 0)
        );
    }

    // ========== refreshAllStatistics ==========

    @Test
    void refreshAllStatistics_shouldRefreshAllMeetings() {
        // Given
        MeetingStatistics stats1 = createTestStatistics(1L, 10, 8, new BigDecimal("80.00"));
        MeetingStatistics stats2 = createTestStatistics(2L, 20, 15, new BigDecimal("75.00"));

        List<MeetingStatistics> allStats = Arrays.asList(stats1, stats2);

        when(statisticsRepository.findAll()).thenReturn(allStats);

        // Mock for first meeting
        Meeting meeting1 = createTestMeeting(1L);
        Meeting meeting2 = createTestMeeting(2L);

        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting1));
        when(meetingRepository.findById(2L)).thenReturn(Optional.of(meeting2));

        ParticipantCountDto counts1 = ParticipantCountDto.builder()
                .total(10L).confirmed(8L).attended(8L).declined(2L)
                .cancelled(0L).invited(0L).pending(0L).build();

        ParticipantCountDto counts2 = ParticipantCountDto.builder()
                .total(20L).confirmed(18L).attended(15L).declined(2L)
                .cancelled(0L).invited(0L).pending(0L).build();

        when(meetingParticipantRepository.getParticipantCounts(1L)).thenReturn(counts1);
        when(meetingParticipantRepository.getParticipantCounts(2L)).thenReturn(counts2);

        when(feedbackRepository.findAverageRatingByMeetingId(1L)).thenReturn(4.5);
        when(feedbackRepository.findAverageRatingByMeetingId(2L)).thenReturn(4.0);
        when(feedbackRepository.countByMeetingId(1L)).thenReturn(5L);
        when(feedbackRepository.countByMeetingId(2L)).thenReturn(3L);

        when(statisticsRepository.findByMeetingId(1L)).thenReturn(Optional.of(stats1));
        when(statisticsRepository.findByMeetingId(2L)).thenReturn(Optional.of(stats2));

        when(statisticsRepository.save(any(MeetingStatistics.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        analyticsService.refreshAllStatistics();

        // Then
        verify(statisticsRepository, times(2)).save(any(MeetingStatistics.class));
        verify(meetingRepository, times(2)).findById(anyLong());
    }

    @Test
    void refreshAllStatistics_shouldHandleNullMeeting() {
        // Given
        MeetingStatistics statsWithNullMeeting = MeetingStatistics.builder()
                .totalParticipants(10)
                .build();

        List<MeetingStatistics> allStats = List.of(statsWithNullMeeting);

        when(statisticsRepository.findAll()).thenReturn(allStats);

        // When
        analyticsService.refreshAllStatistics();

        // Then
        verify(statisticsRepository, never()).save(any());
    }

    @Test
    void refreshAllStatistics_shouldHandleExceptionsGracefully() {
        // Given
        MeetingStatistics stats1 = createTestStatistics(1L, 10, 8, new BigDecimal("80.00"));
        MeetingStatistics stats2 = createTestStatistics(2L, 20, 15, new BigDecimal("75.00"));

        List<MeetingStatistics> allStats = Arrays.asList(stats1, stats2);

        when(statisticsRepository.findAll()).thenReturn(allStats);

        // First meeting - success
        Meeting meeting1 = createTestMeeting(1L);
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting1));

        ParticipantCountDto counts1 = ParticipantCountDto.builder()
                .total(10L).confirmed(8L).attended(8L).declined(2L)
                .cancelled(0L).invited(0L).pending(0L).build();

        when(meetingParticipantRepository.getParticipantCounts(1L)).thenReturn(counts1);
        when(feedbackRepository.findAverageRatingByMeetingId(1L)).thenReturn(4.5);
        when(feedbackRepository.countByMeetingId(1L)).thenReturn(5L);
        when(statisticsRepository.findByMeetingId(1L)).thenReturn(Optional.of(stats1));

        // Second meeting - error
        when(meetingRepository.findById(2L)).thenThrow(new RuntimeException("DB Error"));

        when(statisticsRepository.save(any(MeetingStatistics.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        assertDoesNotThrow(() -> analyticsService.refreshAllStatistics());

        // Then - should still process first one
        verify(statisticsRepository, times(1)).save(any(MeetingStatistics.class));
    }

    // ========== Private Helper Methods ==========

    private Meeting createTestMeeting(Long id) {
        User organizer = new User();
        organizer.setId(1L);
        organizer.setFirstName("Organizer");
        organizer.setLastName("Test");

        Meeting meeting = new Meeting();
        meeting.setId(id);
        meeting.setTitle("Meeting " + id);
        meeting.setDescription("Description " + id);
        meeting.setStartDate(LocalDateTime.now().minusDays(id));
        meeting.setEndDate(LocalDateTime.now().minusDays(id).plusHours(2));
        meeting.setOrganizer(organizer);
        meeting.setStatus(MeetingStatus.COMPLETED);
        return meeting;
    }

    private MeetingStatistics createTestStatistics(Long meetingId, int total, int attended, BigDecimal attendanceRate) {
        Meeting meeting = createTestMeeting(meetingId);

        MeetingStatistics stats = MeetingStatistics.builder()
                .meeting(meeting)
                .totalParticipants(total)
                .attendedParticipants(attended)
                .confirmedParticipants((int) (total * 0.9))
                .declinedParticipants((int) (total * 0.1))
                .attendanceRate(attendanceRate)
                .confirmationRate(new BigDecimal("90.00"))
                .averageRating(new BigDecimal("4.0"))
                .feedbackCount(3)
                .avgResponseTimeMinutes(new BigDecimal("60.0"))
                .generatedAt(LocalDateTime.now())
                .status(MeetingStatistics.StatisticsStatus.FINAL)
                .finalized(true)
                .build();

        stats.setId(meetingId);
        return stats;
    }

    // ========== Test for getDateRangeText private method ==========

    @Test
    void getDateRangeText_shouldHandleAllBranches() throws Exception {
        // Use reflection to test private method
        Method getDateRangeTextMethod = MeetingAnalyticsServiceImpl.class
                .getDeclaredMethod("getDateRangeText", ReportFilter.class);
        getDateRangeTextMethod.setAccessible(true);

        // Test 1: filter = null
        String result1 = (String) getDateRangeTextMethod.invoke(analyticsService, (Object) null);
        assertEquals("Cały okres", result1);

        // Test 2: filter with both dates null
        ReportFilter emptyFilter = new ReportFilter();
        String result2 = (String) getDateRangeTextMethod.invoke(analyticsService, emptyFilter);
        assertEquals("Cały okres", result2);

        // Test 3: filter with only dateFrom
        ReportFilter fromOnlyFilter = new ReportFilter();
        fromOnlyFilter.setDateFrom(LocalDateTime.of(2024, 3, 15, 10, 30));
        String result3 = (String) getDateRangeTextMethod.invoke(analyticsService, fromOnlyFilter);
        assertEquals("15.03.2024 - nieokreślony", result3);

        // Test 4: filter with only dateTo
        ReportFilter toOnlyFilter = new ReportFilter();
        toOnlyFilter.setDateTo(LocalDateTime.of(2024, 3, 31, 23, 59));
        String result4 = (String) getDateRangeTextMethod.invoke(analyticsService, toOnlyFilter);
        assertEquals("nieokreślony - 31.03.2024", result4);

        // Test 5: filter with both dates
        ReportFilter fullFilter = new ReportFilter();
        fullFilter.setDateFrom(LocalDateTime.of(2024, 3, 1, 0, 0));
        fullFilter.setDateTo(LocalDateTime.of(2024, 3, 31, 23, 59));
        String result5 = (String) getDateRangeTextMethod.invoke(analyticsService, fullFilter);
        assertEquals("01.03.2024 - 31.03.2024", result5);
    }

    // ========== Edge Case Tests ==========

    @Test
    void testCalculateDerivedMetricsIsCalled() {
        // Given
        MeetingStatistics existingStats = mock(MeetingStatistics.class);

        when(meetingRepository.findById(100L)).thenReturn(Optional.of(meeting));
        when(meetingParticipantRepository.getParticipantCounts(100L)).thenReturn(participantCounts);
        when(feedbackRepository.findAverageRatingByMeetingId(100L)).thenReturn(4.5);
        when(feedbackRepository.countByMeetingId(100L)).thenReturn(5L);
        when(statisticsRepository.findByMeetingId(100L)).thenReturn(Optional.of(existingStats));
        when(statisticsRepository.save(any(MeetingStatistics.class))).thenReturn(existingStats);

        // When
        analyticsService.generateMeetingStatistics(100L);

        // Then
        verify(existingStats).calculateDerivedMetrics();
    }

    @Test
    void testPdfGenerationWithVariousDataScenarios() {
        // Test with null values
        MeetingStatistics nullStats = MeetingStatistics.builder()
                .id(1L)
                .meeting(meeting)
                .totalParticipants(null)
                .attendedParticipants(null)
                .attendanceRate(null)
                .status(MeetingStatistics.StatisticsStatus.FINAL)
                .generatedAt(LocalDateTime.now())
                .build();

        // Test with zero values
        MeetingStatistics zeroStats = MeetingStatistics.builder()
                .id(2L)
                .meeting(meeting)
                .totalParticipants(0)
                .attendedParticipants(0)
                .attendanceRate(BigDecimal.ZERO)
                .status(MeetingStatistics.StatisticsStatus.FINAL)
                .generatedAt(LocalDateTime.now())
                .build();

        when(statisticsRepository.findByMeetingId(100L)).thenReturn(Optional.of(nullStats));
        when(statisticsRepository.findByMeetingId(200L)).thenReturn(Optional.of(zeroStats));

        assertAll(
                () -> assertDoesNotThrow(() -> {
                    byte[] pdf1 = analyticsService.exportMeetingStatisticsToPdf(100L);
                    assertNotNull(pdf1);
                }, "Should handle null values"),
                () -> assertDoesNotThrow(() -> {
                    byte[] pdf2 = analyticsService.exportMeetingStatisticsToPdf(200L);
                    assertNotNull(pdf2);
                }, "Should handle zero values")
        );
    }

    @Test
    void testOrganizerReportWithZeroMeetings() {
        // Given
        when(statisticsRepository.getOrganizerReportStats(1L)).thenReturn(null);

        // When
        OrganizerReport report = analyticsService.generateOrganizerReport(1L, null);

        // Then
        assertAll(
                () -> assertEquals(0, report.getTotalMeetings()),
                () -> assertEquals(0, report.getTotalParticipants()),
                () -> assertEquals(0, report.getTotalAttended()),
                () -> assertEquals(BigDecimal.ZERO, report.getAverageAttendanceRate())
        );
    }

    @Test
    void testRefreshAllStatisticsWithEmptyList() {
        // Given
        when(statisticsRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        analyticsService.refreshAllStatistics();

        // Then
        verify(statisticsRepository, never()).save(any());
    }
}
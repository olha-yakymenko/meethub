package com.meethub.domain.service.impl;

import com.meethub.domain.model.dto.ParticipantCountDto;
import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.MeetingStatistics;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.MeetingStatus;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingAnalyticsServiceImplTest {

    @Mock
    private MeetingStatisticsRepository statisticsRepository;

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private MeetingParticipantService meetingParticipantService;

    @Mock
    private FeedbackRepository feedbackRepository;

    @InjectMocks
    private MeetingAnalyticsServiceImpl analyticsService;

    private Meeting testMeeting;
    private User testOrganizer;
    private MeetingStatistics testStatistics;

    @BeforeEach
    void setUp() {
        testOrganizer = new User();
        testOrganizer.setId(1L);
        testOrganizer.setFirstName("John");
        testOrganizer.setLastName("Doe");
        testOrganizer.setEmail("john@example.com");

        testMeeting = new Meeting();
        testMeeting.setId(1L);
        testMeeting.setTitle("Test Meeting");
        testMeeting.setDescription("Test Description");
        testMeeting.setStartDate(LocalDateTime.now().minusDays(1));
        testMeeting.setEndDate(LocalDateTime.now().minusHours(1));
        testMeeting.setOrganizer(testOrganizer);
        testMeeting.setStatus(MeetingStatus.COMPLETED);

        testStatistics = MeetingStatistics.builder()
                .meeting(testMeeting)
                .totalParticipants(10)
                .confirmedParticipants(8)
                .attendedParticipants(6)
                .declinedParticipants(2)
                .pendingParticipants(0)
                .attendanceRate(new BigDecimal("60.00"))
                .confirmationRate(new BigDecimal("80.00"))
                .averageRating(new BigDecimal("4.5"))
                .feedbackCount(5)
                .avgResponseTimeMinutes(new BigDecimal("120.5"))
                .generatedAt(LocalDateTime.now().minusHours(2))
                .lastCalculatedAt(LocalDateTime.now().minusHours(2))
                .updatedAt(LocalDateTime.now().minusHours(2))
                .status(MeetingStatistics.StatisticsStatus.FINAL)
                .finalized(true)
                .build();
        testStatistics.setId(1L);
    }

    @Test
    void generateMeetingStatistics_shouldCreateNewStatistics_whenNotExists() {
        ParticipantCountDto participantCounts = ParticipantCountDto.builder()
                .total(10L)
                .confirmed(8L)
                .attended(6L)
                .declined(2L)
                .pending(0L)
                .build();

        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));
        when(meetingParticipantService.getParticipantCounts(1L)).thenReturn(participantCounts);
        when(feedbackRepository.findAverageRatingByMeetingId(1L)).thenReturn(4.5);
        when(feedbackRepository.countByMeetingId(1L)).thenReturn(5L);
        when(statisticsRepository.findByMeetingId(1L)).thenReturn(Optional.empty());
        when(statisticsRepository.save(any(MeetingStatistics.class))).thenAnswer(invocation -> {
            MeetingStatistics stats = invocation.getArgument(0);
            stats.setId(1L);
            return stats;
        });

        MeetingStatistics result = analyticsService.generateMeetingStatistics(1L);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(testMeeting, result.getMeeting()),
                () -> assertEquals(10, result.getTotalParticipants()),
                () -> assertEquals(6, result.getAttendedParticipants()),
                () -> assertEquals(new BigDecimal("60.00"), result.getAttendanceRate()),
                () -> assertEquals(new BigDecimal("80.00"), result.getConfirmationRate()),
                () -> assertEquals(new BigDecimal("4.50"), result.getAverageRating()),
                () -> assertEquals(5, result.getFeedbackCount()),
                () -> assertEquals(MeetingStatistics.StatisticsStatus.FINAL, result.getStatus()),
                () -> assertTrue(result.getFinalized())
        );

        verify(statisticsRepository).save(any(MeetingStatistics.class));
    }

    @Test
    void generateMeetingStatistics_shouldUpdateExistingStatistics() {
        ParticipantCountDto participantCounts = ParticipantCountDto.builder()
                .total(15L)
                .confirmed(12L)
                .attended(10L)
                .declined(3L)
                .pending(0L)
                .build();

        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));
        when(meetingParticipantService.getParticipantCounts(1L)).thenReturn(participantCounts);
        when(feedbackRepository.findAverageRatingByMeetingId(1L)).thenReturn(4.7);
        when(feedbackRepository.countByMeetingId(1L)).thenReturn(8L);
        when(statisticsRepository.findByMeetingId(1L)).thenReturn(Optional.of(testStatistics));
        when(statisticsRepository.save(any(MeetingStatistics.class))).thenReturn(testStatistics);

        MeetingStatistics result = analyticsService.generateMeetingStatistics(1L);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(15, result.getTotalParticipants()),
                () -> assertEquals(10, result.getAttendedParticipants()),
                () -> assertEquals(12, result.getConfirmedParticipants()),
                () -> assertEquals(3, result.getDeclinedParticipants()),
                () -> assertEquals(new BigDecimal("66.67"), result.getAttendanceRate()),
                () -> assertEquals(new BigDecimal("80.00"), result.getConfirmationRate()),
                () -> assertEquals(new BigDecimal("4.70"), result.getAverageRating()),
                () -> assertEquals(8, result.getFeedbackCount()),
                () -> assertNotNull(result.getUpdatedAt())
        );

        verify(statisticsRepository).save(testStatistics);
    }

    @Test
    void generateMeetingStatistics_shouldHandleNullParticipantCounts() {
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));
        when(meetingParticipantService.getParticipantCounts(1L)).thenReturn(null);
        when(feedbackRepository.findAverageRatingByMeetingId(1L)).thenReturn(null);
        when(feedbackRepository.countByMeetingId(1L)).thenReturn(null);
        when(statisticsRepository.findByMeetingId(1L)).thenReturn(Optional.empty());
        when(statisticsRepository.save(any(MeetingStatistics.class))).thenAnswer(invocation -> {
            MeetingStatistics stats = invocation.getArgument(0);
            stats.setId(1L);
            return stats;
        });

        MeetingStatistics result = analyticsService.generateMeetingStatistics(1L);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(0, result.getTotalParticipants()),
                () -> assertEquals(0, result.getAttendedParticipants()),
                () -> assertEquals(0, result.getConfirmedParticipants()),
                () -> assertEquals(BigDecimal.ZERO, result.getAttendanceRate()),
                () -> assertEquals(BigDecimal.ZERO, result.getAverageRating()),
                () -> assertEquals(0, result.getFeedbackCount())
        );
    }

    @Test
    void generateMeetingStatistics_shouldSetCorrectStatusForDraftMeeting() {
        testMeeting.setStartDate(LocalDateTime.now().plusDays(1));
        testMeeting.setEndDate(LocalDateTime.now().plusDays(2));

        ParticipantCountDto participantCounts = ParticipantCountDto.builder()
                .total(5L)
                .confirmed(3L)
                .attended(0L)
                .declined(0L)
                .pending(2L)
                .build();

        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));
        when(meetingParticipantService.getParticipantCounts(1L)).thenReturn(participantCounts);
        when(feedbackRepository.findAverageRatingByMeetingId(1L)).thenReturn(null);
        when(feedbackRepository.countByMeetingId(1L)).thenReturn(0L);
        when(statisticsRepository.findByMeetingId(1L)).thenReturn(Optional.empty());
        when(statisticsRepository.save(any(MeetingStatistics.class))).thenAnswer(invocation -> {
            MeetingStatistics stats = invocation.getArgument(0);
            stats.setId(1L);
            return stats;
        });

        MeetingStatistics result = analyticsService.generateMeetingStatistics(1L);

        assertAll(
                () -> assertEquals(MeetingStatistics.StatisticsStatus.DRAFT, result.getStatus()),
                () -> assertFalse(result.getFinalized())
        );
    }

    @Test
    void generateMeetingStatistics_shouldSetCorrectStatusForPreliminaryMeeting() {
        testMeeting.setStartDate(LocalDateTime.now().minusHours(2));
        testMeeting.setEndDate(LocalDateTime.now().plusHours(1));

        ParticipantCountDto participantCounts = ParticipantCountDto.builder()
                .total(5L)
                .confirmed(3L)
                .attended(2L)
                .declined(1L)
                .pending(1L)
                .build();

        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));
        when(meetingParticipantService.getParticipantCounts(1L)).thenReturn(participantCounts);
        when(feedbackRepository.findAverageRatingByMeetingId(1L)).thenReturn(null);
        when(feedbackRepository.countByMeetingId(1L)).thenReturn(0L);
        when(statisticsRepository.findByMeetingId(1L)).thenReturn(Optional.empty());
        when(statisticsRepository.save(any(MeetingStatistics.class))).thenAnswer(invocation -> {
            MeetingStatistics stats = invocation.getArgument(0);
            stats.setId(1L);
            return stats;
        });

        MeetingStatistics result = analyticsService.generateMeetingStatistics(1L);

        assertAll(
                () -> assertEquals(MeetingStatistics.StatisticsStatus.PRELIMINARY, result.getStatus()),
                () -> assertFalse(result.getFinalized())
        );
    }

    @Test
    void getMeetingStatistics_shouldReturnStatistics_whenExists() {
        when(statisticsRepository.findByMeetingId(1L)).thenReturn(Optional.of(testStatistics));

        Optional<MeetingStatistics> result = analyticsService.getMeetingStatistics(1L);

        assertAll(
                () -> assertTrue(result.isPresent()),
                () -> assertEquals(testStatistics, result.get())
        );
    }

    @Test
    void getMeetingStatistics_shouldReturnEmpty_whenNotExists() {
        when(statisticsRepository.findByMeetingId(1L)).thenReturn(Optional.empty());

        Optional<MeetingStatistics> result = analyticsService.getMeetingStatistics(1L);

        assertAll(
                () -> assertTrue(result.isEmpty())
        );
    }

    @Test
    void deleteMeetingStatistics_shouldDelete_whenMeetingIdExists() {
        doNothing().when(statisticsRepository).deleteByMeetingId(1L);

        analyticsService.deleteMeetingStatistics(1L);

        verify(statisticsRepository).deleteByMeetingId(1L);
    }

    @Test
    void generateOrganizerReport_shouldGenerateReportWithoutFilter() {
        List<MeetingStatistics> statsList = Arrays.asList(
                createTestStatistics(1L, 10, 8, new BigDecimal("80.00")),
                createTestStatistics(2L, 20, 15, new BigDecimal("75.00"))
        );

        when(statisticsRepository.findByOrganizerId(1L)).thenReturn(statsList);

        OrganizerReport report = analyticsService.generateOrganizerReport(1L, null);

        assertAll(
                () -> assertNotNull(report),
                () -> assertEquals(1L, report.getOrganizerId()),
                () -> assertEquals(2, report.getTotalMeetings()),
                () -> assertEquals(30, report.getTotalParticipants()),
                () -> assertEquals(23, report.getTotalAttended()),
                () -> assertEquals(new BigDecimal("77.50"), report.getAverageAttendanceRate()),
                () -> assertNotNull(report.getGeneratedAt())
        );

        verify(statisticsRepository).findByOrganizerId(1L);
    }

    @Test
    void generateOrganizerReport_shouldGenerateReportWithDateFilter() {
        List<MeetingStatistics> statsList = Arrays.asList(
                createTestStatistics(1L, 10, 8, new BigDecimal("80.00")),
                createTestStatistics(2L, 20, 15, new BigDecimal("75.00")),
                createTestStatistics(3L, 15, 10, new BigDecimal("66.67"))
        );

        ReportFilter filter = new ReportFilter();
        filter.setDateFrom(LocalDateTime.now().minusMonths(1));
        filter.setDateTo(LocalDateTime.now());

        when(statisticsRepository.findByOrganizerId(1L)).thenReturn(statsList);

        OrganizerReport report = analyticsService.generateOrganizerReport(1L, filter);

        assertAll(
                () -> assertNotNull(report),
                () -> assertEquals(3, report.getTotalMeetings())
        );
    }

    @Test
    void generateOrganizerReport_shouldFilterStatisticsByDate() {
        MeetingStatistics stats1 = createTestStatistics(1L, 10, 8, new BigDecimal("80.00"));
        MeetingStatistics stats2 = createTestStatistics(2L, 20, 15, new BigDecimal("75.00"));

        stats1.getMeeting().setStartDate(LocalDateTime.now().minusDays(5));
        stats2.getMeeting().setStartDate(LocalDateTime.now().plusDays(5));

        List<MeetingStatistics> statsList = Arrays.asList(stats1, stats2);

        ReportFilter filter = new ReportFilter();
        filter.setDateFrom(LocalDateTime.now().minusDays(10));
        filter.setDateTo(LocalDateTime.now());

        when(statisticsRepository.findByOrganizerId(1L)).thenReturn(statsList);

        OrganizerReport report = analyticsService.generateOrganizerReport(1L, filter);

        assertAll(
                () -> assertEquals(1, report.getTotalMeetings()),
                () -> assertEquals(10, report.getTotalParticipants())
        );
    }

    @Test
    void getMeetingStatisticsByOrganizer_shouldReturnStatisticsList() {
        List<MeetingStatistics> statsList = Arrays.asList(
                createTestStatistics(1L, 10, 8, new BigDecimal("80.00")),
                createTestStatistics(2L, 20, 15, new BigDecimal("75.00"))
        );

        when(statisticsRepository.findByOrganizerId(1L)).thenReturn(statsList);

        List<MeetingStatistics> result = analyticsService.getMeetingStatisticsByOrganizer(1L);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(2, result.size()),
                () -> assertEquals(statsList, result)
        );
    }

    @Test
    void getMeetingStatisticsByOrganizer_shouldReturnEmptyList_whenNoStatistics() {
        when(statisticsRepository.findByOrganizerId(1L)).thenReturn(Collections.emptyList());

        List<MeetingStatistics> result = analyticsService.getMeetingStatisticsByOrganizer(1L);

        assertAll(
                () -> assertNotNull(result),
                () -> assertTrue(result.isEmpty())
        );
    }

    @Test
    void exportReportToCsv_shouldGenerateCsvBytes() {
        List<MeetingStatistics> statsList = Arrays.asList(
                createTestStatistics(1L, 10, 8, new BigDecimal("80.00"))
        );

        when(statisticsRepository.findByOrganizerId(1L)).thenReturn(statsList);

        byte[] csvBytes = analyticsService.exportReportToCsv(1L, null);

        String csvString = new String(csvBytes);

        assertAll(
                () -> assertNotNull(csvBytes),
                () -> assertTrue(csvBytes.length > 0),
                () -> assertTrue(csvString.contains("Organizer Report")),
                () -> assertTrue(csvString.contains("Organizer ID: 1")),
                () -> assertTrue(csvString.contains("Total Meetings: 1")),
                () -> assertTrue(csvString.contains("Average Attendance Rate: 80.00%"))
        );
    }

    @Test
    void exportMeetingStatisticsToCsv_shouldGenerateCsvBytes() {
        when(statisticsRepository.findByMeetingId(1L)).thenReturn(Optional.of(testStatistics));

        byte[] csvBytes = analyticsService.exportMeetingStatisticsToCsv(1L);

        String csvString = new String(csvBytes);

        assertAll(
                () -> assertNotNull(csvBytes),
                () -> assertTrue(csvBytes.length > 0),
                () -> assertTrue(csvString.contains("Meeting Statistics")),
                () -> assertTrue(csvString.contains("Meeting ID: 1")),
                () -> assertTrue(csvString.contains("Total Participants: 10")),
                () -> assertTrue(csvString.contains("Attended: 6"))
        );
    }

    @Test
    void getAverageResponseTime_shouldReturnResponseTime_whenStatisticsExist() {
        when(statisticsRepository.findByMeetingId(1L)).thenReturn(Optional.of(testStatistics));

        BigDecimal responseTime = analyticsService.getAverageResponseTime(1L);

        assertAll(
                () -> assertNotNull(responseTime),
                () -> assertEquals(new BigDecimal("120.5"), responseTime)
        );
    }

    @Test
    void getAverageResponseTime_shouldReturnZero_whenNoStatistics() {
        when(statisticsRepository.findByMeetingId(1L)).thenReturn(Optional.empty());

        BigDecimal responseTime = analyticsService.getAverageResponseTime(1L);

        assertAll(
                () -> assertEquals(BigDecimal.ZERO, responseTime)
        );
    }

    @Test
    void getAverageResponseTime_shouldReturnZero_whenResponseTimeIsNull() {
        testStatistics.setAvgResponseTimeMinutes(null);
        when(statisticsRepository.findByMeetingId(1L)).thenReturn(Optional.of(testStatistics));

        BigDecimal responseTime = analyticsService.getAverageResponseTime(1L);

        assertAll(
                () -> assertEquals(BigDecimal.ZERO, responseTime)
        );
    }

    @Test
    void getRecentStatistics_shouldReturnLimitedStatistics() {
        List<MeetingStatistics> allStats = Arrays.asList(
                createTestStatistics(1L, 10, 8, new BigDecimal("80.00")),
                createTestStatistics(2L, 20, 15, new BigDecimal("75.00")),
                createTestStatistics(3L, 15, 10, new BigDecimal("66.67")),
                createTestStatistics(4L, 25, 20, new BigDecimal("80.00")),
                createTestStatistics(5L, 30, 25, new BigDecimal("83.33")),
                createTestStatistics(6L, 12, 9, new BigDecimal("75.00"))
        );

        when(statisticsRepository.findAll()).thenReturn(allStats);

        List<MeetingStatistics> result = analyticsService.getRecentStatistics(3);

        assertAll(
                () -> assertEquals(3, result.size()),
                () -> assertEquals(6L, result.get(0).getMeeting().getId()),
                () -> assertEquals(5L, result.get(1).getMeeting().getId()),
                () -> assertEquals(4L, result.get(2).getMeeting().getId())
        );
    }

    @Test
    void getRecentStatistics_shouldReturnAll_whenLimitGreaterThanSize() {
        List<MeetingStatistics> allStats = Arrays.asList(
                createTestStatistics(1L, 10, 8, new BigDecimal("80.00")),
                createTestStatistics(2L, 20, 15, new BigDecimal("75.00"))
        );

        when(statisticsRepository.findAll()).thenReturn(allStats);

        List<MeetingStatistics> result = analyticsService.getRecentStatistics(10);

        assertAll(
                () -> assertEquals(2, result.size())
        );
    }

    @Test
    void getRecentStatistics_shouldReturnEmptyList_whenNoStatistics() {
        when(statisticsRepository.findAll()).thenReturn(Collections.emptyList());

        List<MeetingStatistics> result = analyticsService.getRecentStatistics(5);

        assertAll(
                () -> assertNotNull(result),
                () -> assertTrue(result.isEmpty())
        );
    }

    @Test
    void getStatisticsOverview_shouldReturnOverviewMap() {
        when(statisticsRepository.findByMeetingId(1L)).thenReturn(Optional.of(testStatistics));

        Map<String, Object> overview = analyticsService.getStatisticsOverview(1L);

        assertAll(
                () -> assertNotNull(overview),
                () -> assertEquals(1L, overview.get("meetingId")),
                () -> assertEquals(new BigDecimal("60.00"), overview.get("attendanceRate")),
                () -> assertEquals(10, overview.get("totalParticipants")),
                () -> assertEquals(6, overview.get("attendedParticipants")),
                () -> assertEquals(8, overview.get("confirmedParticipants")),
                () -> assertEquals(new BigDecimal("120.5"), overview.get("avgResponseTime")),
                () -> assertEquals("FINAL", overview.get("status")),
                () -> assertEquals(true, overview.get("finalized"))
        );
    }

    @Test
    void getStatisticsOverview_shouldThrowException_whenNoStatistics() {
        when(statisticsRepository.findByMeetingId(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> analyticsService.getStatisticsOverview(1L));
    }

    @Test
    void refreshAllStatistics_shouldRefreshAllStatistics() {
        List<MeetingStatistics> allStats = Arrays.asList(
                testStatistics,
                createTestStatistics(2L, 15, 10, new BigDecimal("66.67"))
        );

        when(statisticsRepository.findAll()).thenReturn(allStats);
        when(meetingRepository.findById(anyLong())).thenReturn(Optional.of(testMeeting));

        ParticipantCountDto participantCounts = ParticipantCountDto.builder()
                .total(10L)
                .confirmed(8L)
                .attended(6L)
                .declined(2L)
                .pending(0L)
                .build();

        when(meetingParticipantService.getParticipantCounts(anyLong())).thenReturn(participantCounts);
        when(feedbackRepository.findAverageRatingByMeetingId(anyLong())).thenReturn(4.5);
        when(feedbackRepository.countByMeetingId(anyLong())).thenReturn(5L);
        when(statisticsRepository.findByMeetingId(anyLong())).thenReturn(Optional.of(testStatistics));
        when(statisticsRepository.save(any(MeetingStatistics.class))).thenReturn(testStatistics);

        analyticsService.refreshAllStatistics();

        verify(statisticsRepository, times(2)).save(any(MeetingStatistics.class));
    }

    @Test
    void refreshAllStatistics_shouldHandleNullMeeting() {
        MeetingStatistics statsWithNullMeeting = MeetingStatistics.builder()
                .totalParticipants(10)
                .build();

        List<MeetingStatistics> allStats = List.of(statsWithNullMeeting);

        when(statisticsRepository.findAll()).thenReturn(allStats);

        analyticsService.refreshAllStatistics();

        verify(statisticsRepository, never()).save(any());
    }

    @Test
    void refreshAllStatistics_shouldHandleExceptionsGracefully() {
        List<MeetingStatistics> allStats = Arrays.asList(
                testStatistics,
                createTestStatistics(2L, 15, 10, new BigDecimal("66.67"))
        );

        when(statisticsRepository.findAll()).thenReturn(allStats);

        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));

        ParticipantCountDto participantCounts = ParticipantCountDto.builder()
                .total(10L)
                .confirmed(8L)
                .attended(6L)
                .declined(2L)
                .pending(0L)
                .build();

        when(meetingParticipantService.getParticipantCounts(1L)).thenReturn(participantCounts);
        when(statisticsRepository.findByMeetingId(1L)).thenReturn(Optional.of(testStatistics));

        when(meetingRepository.findById(2L)).thenThrow(new RuntimeException("DB Error"));

        when(statisticsRepository.save(any(MeetingStatistics.class))).thenReturn(testStatistics);

        assertDoesNotThrow(() -> analyticsService.refreshAllStatistics());

        verify(statisticsRepository, times(1)).save(any(MeetingStatistics.class));
    }

    @Test
    void exportReportToPdf_shouldGeneratePdfBytes() {
        List<MeetingStatistics> statsList = Arrays.asList(
                createTestStatistics(1L, 10, 8, new BigDecimal("80.00"))
        );

        when(statisticsRepository.findByOrganizerId(1L)).thenReturn(statsList);

        byte[] pdfBytes = analyticsService.exportReportToPdf(1L, null);

        assertAll(
                () -> assertNotNull(pdfBytes),
                () -> assertTrue(pdfBytes.length > 0)
        );
    }

    @Test
    void exportMeetingStatisticsToPdf_shouldGeneratePdfBytes() {
        when(statisticsRepository.findByMeetingId(1L)).thenReturn(Optional.of(testStatistics));

        byte[] pdfBytes = analyticsService.exportMeetingStatisticsToPdf(1L);

        assertAll(
                () -> assertNotNull(pdfBytes),
                () -> assertTrue(pdfBytes.length > 0)
        );
    }

    @Test
    void exportMeetingStatisticsToPdf_shouldThrowException_whenNoStatistics() {
        when(statisticsRepository.findByMeetingId(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> analyticsService.exportMeetingStatisticsToPdf(1L));
    }

    @Test
    void generateMeetingStatistics_shouldHandleZeroTotalParticipants() {
        ParticipantCountDto participantCounts = ParticipantCountDto.builder()
                .total(0L)
                .confirmed(0L)
                .attended(0L)
                .declined(0L)
                .pending(0L)
                .build();

        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));
        when(meetingParticipantService.getParticipantCounts(1L)).thenReturn(participantCounts);
        when(feedbackRepository.findAverageRatingByMeetingId(1L)).thenReturn(null);
        when(feedbackRepository.countByMeetingId(1L)).thenReturn(0L);
        when(statisticsRepository.findByMeetingId(1L)).thenReturn(Optional.empty());
        when(statisticsRepository.save(any(MeetingStatistics.class))).thenAnswer(invocation -> {
            MeetingStatistics stats = invocation.getArgument(0);
            stats.setId(1L);
            return stats;
        });

        MeetingStatistics result = analyticsService.generateMeetingStatistics(1L);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(0, result.getTotalParticipants()),
                () -> assertEquals(BigDecimal.ZERO, result.getAttendanceRate()),
                () -> assertEquals(BigDecimal.ZERO, result.getConfirmationRate())
        );
    }

    @Test
    void getStatisticsOverview_shouldHandleNullValuesInStatistics() {
        MeetingStatistics stats = MeetingStatistics.builder()
                .meeting(testMeeting)
                .totalParticipants(null)
                .attendedParticipants(null)
                .attendanceRate(null)
                .avgResponseTimeMinutes(null)
                .status(MeetingStatistics.StatisticsStatus.DRAFT)
                .finalized(false)
                .build();

        when(statisticsRepository.findByMeetingId(1L)).thenReturn(Optional.of(stats));

        Map<String, Object> overview = analyticsService.getStatisticsOverview(1L);

        assertAll(
                () -> assertNotNull(overview),
                () -> assertEquals(1L, overview.get("meetingId")),
                () -> assertNull(overview.get("attendanceRate")),
                () -> assertNull(overview.get("totalParticipants")),
                () -> assertNull(overview.get("attendedParticipants")),
                () -> assertNull(overview.get("avgResponseTime")),
                () -> assertEquals("DRAFT", overview.get("status")),
                () -> assertEquals(false, overview.get("finalized"))
        );
    }

    private MeetingStatistics createTestStatistics(Long meetingId, int total, int attended, BigDecimal attendanceRate) {
        Meeting meeting = new Meeting();
        meeting.setId(meetingId);
        meeting.setTitle("Meeting " + meetingId);
        meeting.setStartDate(LocalDateTime.now());
        meeting.setEndDate(LocalDateTime.now().plusHours(2));
        meeting.setOrganizer(testOrganizer);

        MeetingStatistics stats = MeetingStatistics.builder()
                .meeting(meeting)
                .totalParticipants(total)
                .attendedParticipants(attended)
                .confirmedParticipants((int) (total * 0.8))
                .declinedParticipants((int) (total * 0.2))
                .attendanceRate(attendanceRate)
                .confirmationRate(new BigDecimal("80.00"))
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
}
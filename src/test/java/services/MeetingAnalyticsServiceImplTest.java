package com.meethub.domain.service.impl;

import com.itextpdf.text.Chunk;
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

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
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

    private Meeting meeting;
    private Meeting testMeeting;
    private User testOrganizer;
    private MeetingStatistics testStatistics;

    private User organizer;

    private ParticipantCountDto participantCounts;

    @BeforeEach
    void setUp() {

        organizer = new User();
        organizer.setId(1L);
        organizer.setFirstName("Organizer");
        organizer.setLastName("Test");


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

        meeting = new Meeting();
        meeting.setId(100L);
        meeting.setTitle("Test Meeting");
        meeting.setDescription("Test meeting description");
        meeting.setStartDate(LocalDateTime.now().minusDays(2));
        meeting.setEndDate(LocalDateTime.now().minusDays(1));
        meeting.setOrganizer(organizer);
        meeting.setStatus(MeetingStatus.COMPLETED);

        participantCounts = new ParticipantCountDto(10L, 8L, 9L, 1L, 0L, (long) 80.0, (long) 90.0);

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

//    @Test
//    void generateMeetingStatistics_shouldHandleNullParticipantCounts() {
//        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));
//        when(meetingParticipantService.getParticipantCounts(1L)).thenReturn(null);
//        when(feedbackRepository.findAverageRatingByMeetingId(1L)).thenReturn(null);
//        when(feedbackRepository.countByMeetingId(1L)).thenReturn(null);
//        when(statisticsRepository.findByMeetingId(1L)).thenReturn(Optional.empty());
//        when(statisticsRepository.save(any(MeetingStatistics.class))).thenAnswer(invocation -> {
//            MeetingStatistics stats = invocation.getArgument(0);
//            stats.setId(1L);
//            return stats;
//        });
//
//        MeetingStatistics result = analyticsService.generateMeetingStatistics(1L);
//
//        assertAll(
//                () -> assertNotNull(result),
//                () -> assertEquals(0, result.getTotalParticipants()),
//                () -> assertEquals(0, result.getAttendedParticipants()),
//                () -> assertEquals(0, result.getConfirmedParticipants()),
//                () -> assertEquals(BigDecimal.ZERO, result.getAttendanceRate()),
//                () -> assertEquals(BigDecimal.ZERO, result.getAverageRating()),
//                () -> assertEquals(0, result.getFeedbackCount())
//        );
//    }

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


//    @Test
//    void generateMeetingStatistics_shouldCreateNewStatistics_whenNotExists() {
//        // Given
//        when(meetingRepository.findById(100L)).thenReturn(Optional.of(meeting));
//        when(meetingParticipantService.getParticipantCounts(100L)).thenReturn(participantCounts);
//        when(feedbackRepository.findAverageRatingByMeetingId(100L)).thenReturn(4.5);
//        when(feedbackRepository.countByMeetingId(100L)).thenReturn(5L);
//        when(statisticsRepository.findByMeetingId(100L)).thenReturn(Optional.empty());
//        when(statisticsRepository.save(any(MeetingStatistics.class))).thenAnswer(inv -> inv.getArgument(0));
//
//        // When
//        MeetingStatistics result = analyticsService.generateMeetingStatistics(100L);
//
//        // Then
//        assertAll(
//                () -> assertNotNull(result, "Statistics should not be null"),
//                () -> assertEquals(100L, result.getMeeting().getId(), "Should be for correct meeting"),
//                () -> assertEquals(10, result.getTotalParticipants(), "Total participants should match"),
//                () -> assertEquals(9, result.getAttendedParticipants(), "Attended participants should match"),
//                () -> assertEquals(new BigDecimal("90.00"), result.getAttendanceRate(), "Attendance rate should match"),
//                () -> assertEquals(new BigDecimal("4.50"), result.getAverageRating(), "Average rating should match"),
//                () -> assertEquals(5, result.getFeedbackCount(), "Feedback count should match"),
//                () -> assertNotNull(result.getGeneratedAt(), "Generated timestamp should be set"),
//                () -> assertEquals(MeetingStatistics.StatisticsStatus.FINAL, result.getStatus(), "Status should be FINAL for completed meeting")
//        );
//
//        verify(statisticsRepository).save(any(MeetingStatistics.class));
//    }

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

    @Test
    void generateOrganizerReport_shouldCalculateCorrectAverage() {
        // Given
        List<MeetingStatistics> statsList = Arrays.asList(
                MeetingStatistics.builder()
                        .meeting(meeting)
                        .totalParticipants(10)
                        .attendedParticipants(8)
                        .attendanceRate(new BigDecimal("80.00"))
                        .build(),
                MeetingStatistics.builder()
                        .meeting(createMeeting(LocalDateTime.now().minusDays(3)))
                        .totalParticipants(20)
                        .attendedParticipants(15)
                        .attendanceRate(new BigDecimal("75.00"))
                        .build()
        );

        when(statisticsRepository.findByOrganizerId(1L)).thenReturn(statsList);

        // When
        OrganizerReport report = analyticsService.generateOrganizerReport(1L, null);

        // Then
        assertAll(
                () -> assertEquals(2, report.getTotalMeetings(), "Should include 2 meetings"),
                () -> assertEquals(30, report.getTotalParticipants(), "Total participants should be 30"),
                () -> assertEquals(23, report.getTotalAttended(), "Total attended should be 23"),
                () -> assertEquals(new BigDecimal("77.50"), report.getAverageAttendanceRate(),
                        "Average attendance should be 77.5% (mean of 80% and 75%)")
        );
    }


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

//    @Test
//    void refreshAllStatistics_shouldHandleExceptionsGracefully() {
//        // Given
//        MeetingStatistics validStats = MeetingStatistics.builder()
//                .id(1L)
//                .meeting(meeting)
//                .build();
//
//        MeetingStatistics invalidStats = MeetingStatistics.builder()
//                .id(2L)
//                .meeting(null) // Meeting is null
//                .build();
//
//        List<MeetingStatistics> allStats = Arrays.asList(validStats, invalidStats);
//
//        when(statisticsRepository.findAll()).thenReturn(allStats);
//        when(meetingRepository.findById(100L)).thenReturn(Optional.of(meeting));
//        when(meetingParticipantService.getParticipantCounts(100L)).thenReturn(participantCounts);
//
//        // When - should not throw exception
//        assertDoesNotThrow(() -> analyticsService.refreshAllStatistics());
//
//        // Then - should refresh valid stats only
//        verify(meetingRepository).findById(100L);
//        verify(statisticsRepository).save(any(MeetingStatistics.class));
//    }

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

    @Test
    void testFilterStatistics_withDateFilters() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        MeetingStatistics stats1 = MeetingStatistics.builder()
                .meeting(createMeeting(now.minusDays(5)))
                .build();
        MeetingStatistics stats2 = MeetingStatistics.builder()
                .meeting(createMeeting(now.minusDays(15)))
                .build();
        MeetingStatistics stats3 = MeetingStatistics.builder()
                .meeting(createMeeting(now.minusDays(25)))
                .build();

        List<MeetingStatistics> allStats = Arrays.asList(stats1, stats2, stats3);

        ReportFilter filter = new ReportFilter();
        filter.setDateFrom(now.minusDays(20));
        filter.setDateTo(now.minusDays(10));

        // When
        Integer filtered = analyticsService.generateOrganizerReport(1L, filter)
                .getTotalMeetings(); // Using this to trigger filtering

        // Mock setup for the test
        when(statisticsRepository.findByOrganizerId(1L)).thenReturn(allStats);

        // Get the actual report
        OrganizerReport report = analyticsService.generateOrganizerReport(1L, filter);

        // Then - should only include stats2 (within date range)
        assertEquals(1, report.getTotalMeetings(), "Should only include 1 meeting within date range");
    }

    @Test
    void testFilterStatistics_withNullMeeting() {
        // Given
        MeetingStatistics statsWithNullMeeting = MeetingStatistics.builder()
                .meeting(null) // Meeting is null
                .build();

        MeetingStatistics statsWithMeeting = MeetingStatistics.builder()
                .meeting(createMeeting(LocalDateTime.now()))
                .build();

        List<MeetingStatistics> allStats = Arrays.asList(statsWithNullMeeting, statsWithMeeting);

        ReportFilter filter = new ReportFilter();
        filter.setDateFrom(LocalDateTime.now().minusDays(1));

        // Mock
        when(statisticsRepository.findByOrganizerId(1L)).thenReturn(allStats);

        // When
        OrganizerReport report = analyticsService.generateOrganizerReport(1L, filter);

        // Then - should exclude stats with null meeting
        assertEquals(1, report.getTotalMeetings(), "Should exclude statistics with null meeting");
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


    // Helper method to create MeetingStatistics with required fields
    private MeetingStatistics createCompleteMeetingStats() {
        return MeetingStatistics.builder()
                .id(1L)
                .meeting(meeting)
                .totalParticipants(10)
                .attendedParticipants(8)
                .confirmedParticipants(9)
                .declinedParticipants(1)
                .pendingParticipants(0)
                .attendanceRate(new BigDecimal("80.00"))
                .confirmationRate(new BigDecimal("90.00"))
                .averageRating(new BigDecimal("4.5"))
                .feedbackCount(5)
                .avgResponseTimeMinutes(new BigDecimal("30.5"))
                .status(MeetingStatistics.StatisticsStatus.FINAL)
                .generatedAt(LocalDateTime.now())
                .finalized(true)
                .build();
    }

    @Test
    void generateMeetingStatisticsPdf_shouldHandleCompleteData() {
        // Test with complete, valid data
        MeetingStatistics completeStats = createCompleteMeetingStats();

        when(statisticsRepository.findByMeetingId(100L)).thenReturn(Optional.of(completeStats));

        assertDoesNotThrow(() -> {
            byte[] pdf = analyticsService.exportMeetingStatisticsToPdf(100L);
            assertNotNull(pdf);
            assertTrue(pdf.length > 1000, "PDF should have reasonable size for complete data");
        });
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




    @Test
    void addSummary_shouldHandleAllAttendanceRateBranches() {
        // Test 1: Wysoka frekwencja (>= 80)
        MeetingStatistics highAttendance = MeetingStatistics.builder()
                .id(1L)
                .meeting(createTestMeeting())
                .attendanceRate(new BigDecimal("85.00")) // >= 80
                .averageRating(new BigDecimal("4.5"))
                .feedbackCount(10)
                .attendedParticipants(8)
                .status(MeetingStatistics.StatisticsStatus.FINAL)
                .generatedAt(LocalDateTime.now())
                .build();

        // Test 2: Średnia frekwencja (>= 60 i < 80)
        MeetingStatistics mediumAttendance = MeetingStatistics.builder()
                .id(2L)
                .meeting(createTestMeeting())
                .attendanceRate(new BigDecimal("70.00")) // >= 60 i < 80
                .averageRating(new BigDecimal("4.5"))
                .feedbackCount(10)
                .attendedParticipants(8)
                .status(MeetingStatistics.StatisticsStatus.FINAL)
                .generatedAt(LocalDateTime.now())
                .build();

        // Test 3: Niska frekwencja (< 60)
        MeetingStatistics lowAttendance = MeetingStatistics.builder()
                .id(3L)
                .meeting(createTestMeeting())
                .attendanceRate(new BigDecimal("50.00")) // < 60
                .averageRating(new BigDecimal("4.5"))
                .feedbackCount(10)
                .attendedParticipants(8)
                .status(MeetingStatistics.StatisticsStatus.FINAL)
                .generatedAt(LocalDateTime.now())
                .build();

        // Test 4: Frekwencja null
        MeetingStatistics nullAttendance = MeetingStatistics.builder()
                .id(4L)
                .meeting(createTestMeeting())
                .attendanceRate(null) // null
                .averageRating(new BigDecimal("4.5"))
                .feedbackCount(10)
                .attendedParticipants(8)
                .status(MeetingStatistics.StatisticsStatus.FINAL)
                .generatedAt(LocalDateTime.now())
                .build();

        when(statisticsRepository.findByMeetingId(100L)).thenReturn(Optional.of(highAttendance));
        when(statisticsRepository.findByMeetingId(200L)).thenReturn(Optional.of(mediumAttendance));
        when(statisticsRepository.findByMeetingId(300L)).thenReturn(Optional.of(lowAttendance));
        when(statisticsRepository.findByMeetingId(400L)).thenReturn(Optional.of(nullAttendance));

        assertAll(
                () -> assertDoesNotThrow(() -> analyticsService.exportMeetingStatisticsToPdf(100L),
                        "Should handle high attendance rate (>= 80)"),
                () -> assertDoesNotThrow(() -> analyticsService.exportMeetingStatisticsToPdf(200L),
                        "Should handle medium attendance rate (>= 60 and < 80)"),
                () -> assertDoesNotThrow(() -> analyticsService.exportMeetingStatisticsToPdf(300L),
                        "Should handle low attendance rate (< 60)"),
                () -> assertDoesNotThrow(() -> analyticsService.exportMeetingStatisticsToPdf(400L),
                        "Should handle null attendance rate")
        );
    }



    private Meeting createTestMeeting() {
        User organizer = new User();
        organizer.setId(1L);
        organizer.setFirstName("John");
        organizer.setLastName("Doe");

        Meeting meeting = new Meeting();
        meeting.setId(1L);
        meeting.setTitle("Test Meeting");
        meeting.setOrganizer(organizer);
        meeting.setStartDate(LocalDateTime.now().minusDays(1));
        meeting.setEndDate(LocalDateTime.now().minusHours(1));
        meeting.setStatus(MeetingStatus.COMPLETED);
        return meeting;
    }



    @Test
    void addSummary_shouldHandleAllRatingBranches() {
        // Test 1: Wysoka ocena (>= 4.0)
        MeetingStatistics highRating = MeetingStatistics.builder()
                .id(1L)
                .meeting(createTestMeeting())
                .attendanceRate(new BigDecimal("85.00"))
                .averageRating(new BigDecimal("4.5")) // >= 4.0
                .feedbackCount(10)
                .attendedParticipants(8)
                .status(MeetingStatistics.StatisticsStatus.FINAL)
                .generatedAt(LocalDateTime.now())
                .build();

        // Test 2: Średnia ocena (>= 3.0 i < 4.0)
        MeetingStatistics mediumRating = MeetingStatistics.builder()
                .id(2L)
                .meeting(createTestMeeting())
                .attendanceRate(new BigDecimal("85.00"))
                .averageRating(new BigDecimal("3.5")) // >= 3.0 i < 4.0
                .feedbackCount(10)
                .attendedParticipants(8)
                .status(MeetingStatistics.StatisticsStatus.FINAL)
                .generatedAt(LocalDateTime.now())
                .build();

        // Test 3: Niska ocena (< 3.0)
        MeetingStatistics lowRating = MeetingStatistics.builder()
                .id(3L)
                .meeting(createTestMeeting())
                .attendanceRate(new BigDecimal("85.00"))
                .averageRating(new BigDecimal("2.5")) // < 3.0
                .feedbackCount(10)
                .attendedParticipants(8)
                .status(MeetingStatistics.StatisticsStatus.FINAL)
                .generatedAt(LocalDateTime.now())
                .build();

        // Test 4: Ocena null
        MeetingStatistics nullRating = MeetingStatistics.builder()
                .id(4L)
                .meeting(createTestMeeting())
                .attendanceRate(new BigDecimal("85.00"))
                .averageRating(null) // null
                .feedbackCount(10)
                .attendedParticipants(8)
                .status(MeetingStatistics.StatisticsStatus.FINAL)
                .generatedAt(LocalDateTime.now())
                .build();

        // Test 5: Ocena zero
        MeetingStatistics zeroRating = MeetingStatistics.builder()
                .id(5L)
                .meeting(createTestMeeting())
                .attendanceRate(new BigDecimal("85.00"))
                .averageRating(BigDecimal.ZERO) // zero
                .feedbackCount(10)
                .attendedParticipants(8)
                .status(MeetingStatistics.StatisticsStatus.FINAL)
                .generatedAt(LocalDateTime.now())
                .build();

        when(statisticsRepository.findByMeetingId(100L)).thenReturn(Optional.of(highRating));
        when(statisticsRepository.findByMeetingId(200L)).thenReturn(Optional.of(mediumRating));
        when(statisticsRepository.findByMeetingId(300L)).thenReturn(Optional.of(lowRating));
        when(statisticsRepository.findByMeetingId(400L)).thenReturn(Optional.of(nullRating));
        when(statisticsRepository.findByMeetingId(500L)).thenReturn(Optional.of(zeroRating));

        assertAll(
                () -> assertDoesNotThrow(() -> analyticsService.exportMeetingStatisticsToPdf(100L),
                        "Should handle high rating (>= 4.0)"),
                () -> assertDoesNotThrow(() -> analyticsService.exportMeetingStatisticsToPdf(200L),
                        "Should handle medium rating (>= 3.0 and < 4.0)"),
                () -> assertDoesNotThrow(() -> analyticsService.exportMeetingStatisticsToPdf(300L),
                        "Should handle low rating (< 3.0)"),
                () -> assertDoesNotThrow(() -> analyticsService.exportMeetingStatisticsToPdf(400L),
                        "Should handle null rating"),
                () -> assertDoesNotThrow(() -> analyticsService.exportMeetingStatisticsToPdf(500L),
                        "Should handle zero rating (<= 0)")
        );
    }

    @Test
    void addSummary_shouldHandleFeedbackCountScenarios() {
        // Test 1: feedbackCount > 0, attendedParticipants > 0
        MeetingStatistics withFeedback = MeetingStatistics.builder()
                .id(1L)
                .meeting(createTestMeeting())
                .attendanceRate(new BigDecimal("85.00"))
                .averageRating(new BigDecimal("4.5"))
                .feedbackCount(8) // > 0
                .attendedParticipants(10) // > 0
                .status(MeetingStatistics.StatisticsStatus.FINAL)
                .generatedAt(LocalDateTime.now())
                .build();

        // Test 2: feedbackCount > 0, attendedParticipants = 0
        MeetingStatistics feedbackNoAttended = MeetingStatistics.builder()
                .id(2L)
                .meeting(createTestMeeting())
                .attendanceRate(new BigDecimal("85.00"))
                .averageRating(new BigDecimal("4.5"))
                .feedbackCount(5) // > 0
                .attendedParticipants(0) // = 0
                .status(MeetingStatistics.StatisticsStatus.FINAL)
                .generatedAt(LocalDateTime.now())
                .build();

        // Test 3: feedbackCount = 0
        MeetingStatistics noFeedback = MeetingStatistics.builder()
                .id(3L)
                .meeting(createTestMeeting())
                .attendanceRate(new BigDecimal("85.00"))
                .averageRating(new BigDecimal("4.5"))
                .feedbackCount(0) // = 0
                .attendedParticipants(10)
                .status(MeetingStatistics.StatisticsStatus.FINAL)
                .generatedAt(LocalDateTime.now())
                .build();

        // Test 4: feedbackCount null
        MeetingStatistics nullFeedback = MeetingStatistics.builder()
                .id(4L)
                .meeting(createTestMeeting())
                .attendanceRate(new BigDecimal("85.00"))
                .averageRating(new BigDecimal("4.5"))
                .feedbackCount(null) // null
                .attendedParticipants(10)
                .status(MeetingStatistics.StatisticsStatus.FINAL)
                .generatedAt(LocalDateTime.now())
                .build();

        when(statisticsRepository.findByMeetingId(100L)).thenReturn(Optional.of(withFeedback));
        when(statisticsRepository.findByMeetingId(200L)).thenReturn(Optional.of(feedbackNoAttended));
        when(statisticsRepository.findByMeetingId(300L)).thenReturn(Optional.of(noFeedback));
        when(statisticsRepository.findByMeetingId(400L)).thenReturn(Optional.of(nullFeedback));

        assertAll(
                () -> assertDoesNotThrow(() -> analyticsService.exportMeetingStatisticsToPdf(100L),
                        "Should handle feedback count > 0 with attended participants"),
                () -> assertDoesNotThrow(() -> analyticsService.exportMeetingStatisticsToPdf(200L),
                        "Should handle feedback count > 0 with no attended participants"),
                () -> assertDoesNotThrow(() -> analyticsService.exportMeetingStatisticsToPdf(300L),
                        "Should handle feedback count = 0"),
                () -> assertDoesNotThrow(() -> analyticsService.exportMeetingStatisticsToPdf(400L),
                        "Should handle null feedback count")
        );
    }

    @Test
    void addSummary_shouldHandleEmptySummaryPoints() {
        // Statystyki bez żadnych danych do podsumowania
        MeetingStatistics emptyStats = MeetingStatistics.builder()
                .id(1L)
                .meeting(createTestMeeting())
                .attendanceRate(null)
                .averageRating(null)
                .feedbackCount(0)
                .attendedParticipants(null)
                .status(MeetingStatistics.StatisticsStatus.FINAL)
                .generatedAt(LocalDateTime.now())
                .build();

        when(statisticsRepository.findByMeetingId(100L)).thenReturn(Optional.of(emptyStats));

        assertDoesNotThrow(() -> {
            byte[] pdf = analyticsService.exportMeetingStatisticsToPdf(100L);
            assertNotNull(pdf);
        }, "Should handle empty summary points");
    }

    @Test
    void addSummary_shouldHandleAllCombinations() {
        // Różne kombinacje danych
        MeetingStatistics stats1 = MeetingStatistics.builder()
                .id(1L)
                .meeting(createTestMeeting())
                .attendanceRate(new BigDecimal("90.00")) // Wysoka
                .averageRating(new BigDecimal("4.8"))   // Wysoka
                .feedbackCount(15)
                .attendedParticipants(12)
                .status(MeetingStatistics.StatisticsStatus.FINAL)
                .generatedAt(LocalDateTime.now())
                .build();

        MeetingStatistics stats2 = MeetingStatistics.builder()
                .id(2L)
                .meeting(createTestMeeting())
                .attendanceRate(new BigDecimal("65.00")) // Średnia
                .averageRating(new BigDecimal("2.8"))   // Niska
                .feedbackCount(3)
                .attendedParticipants(5)
                .status(MeetingStatistics.StatisticsStatus.FINAL)
                .generatedAt(LocalDateTime.now())
                .build();

        when(statisticsRepository.findByMeetingId(100L)).thenReturn(Optional.of(stats1));
        when(statisticsRepository.findByMeetingId(200L)).thenReturn(Optional.of(stats2));

        assertAll(
                () -> assertDoesNotThrow(() -> analyticsService.exportMeetingStatisticsToPdf(100L),
                        "Should handle combination: high attendance + high rating"),
                () -> assertDoesNotThrow(() -> analyticsService.exportMeetingStatisticsToPdf(200L),
                        "Should handle combination: medium attendance + low rating")
        );
    }

    // ========== Testy dla createSeparator() ==========

    @Test
    void createSeparator_shouldReturnValidChunk() throws Exception {
        // Użyj refleksji do testowania prywatnej metody
        Method createSeparatorMethod = MeetingAnalyticsServiceImpl.class
                .getDeclaredMethod("createSeparator");
        createSeparatorMethod.setAccessible(true);

        Chunk separator = (Chunk) createSeparatorMethod.invoke(analyticsService);

        assertAll(
                () -> assertNotNull(separator, "Separator should not be null"),
                () -> assertEquals("________________________________________________________________",
                        separator.getContent(), "Should return correct separator content"),
                () -> assertEquals(100f, separator.getHorizontalScaling(), 0.01f,
                        "Should have correct horizontal scaling")
        );
    }

    // ========== Testy dla HeaderFooterPageEvent ==========

    @Test
    void testHeaderFooterPageEventLifecycle() {
        // Ten test jest trudniejszy do napisania, ponieważ HeaderFooterPageEvent jest klasą wewnętrzną
        // i zależy od iText PDF. Możemy przetestować ogólną funkcjonalność PDF generation.

        MeetingStatistics stats = MeetingStatistics.builder()
                .id(1L)
                .meeting(createTestMeeting())
                .totalParticipants(10)
                .attendedParticipants(8)
                .status(MeetingStatistics.StatisticsStatus.FINAL)
                .generatedAt(LocalDateTime.now())
                .build();

        when(statisticsRepository.findByMeetingId(100L)).thenReturn(Optional.of(stats));

        // Test czy PDF generuje się bez wyjątków (co wskazuje, że HeaderFooterPageEvent działa)
        assertDoesNotThrow(() -> {
            byte[] pdf = analyticsService.exportMeetingStatisticsToPdf(100L);
            assertNotNull(pdf);
            assertTrue(pdf.length > 0, "PDF should not be empty");
        }, "Should generate PDF with header/footer without exceptions");
    }

    @Test
    void testPdfGenerationWithVariousPageCounts() {
        // Test PDF generation with different data sizes that might affect page count
        MeetingStatistics minimalStats = MeetingStatistics.builder()
                .id(1L)
                .meeting(createTestMeeting())
                .totalParticipants(1)
                .attendedParticipants(1)
                .status(MeetingStatistics.StatisticsStatus.FINAL)
                .generatedAt(LocalDateTime.now())
                .build();

        MeetingStatistics extensiveStats = MeetingStatistics.builder()
                .id(2L)
                .meeting(createTestMeeting())
                .totalParticipants(100)
                .attendedParticipants(80)
                .confirmedParticipants(90)
                .declinedParticipants(5)
                .pendingParticipants(5)
                .attendanceRate(new BigDecimal("80.00"))
                .confirmationRate(new BigDecimal("90.00"))
                .averageRating(new BigDecimal("4.5"))
                .feedbackCount(25)
                .avgResponseTimeMinutes(new BigDecimal("45.5"))
                .status(MeetingStatistics.StatisticsStatus.FINAL)
                .generatedAt(LocalDateTime.now())
                .build();

        when(statisticsRepository.findByMeetingId(100L)).thenReturn(Optional.of(minimalStats));
        when(statisticsRepository.findByMeetingId(200L)).thenReturn(Optional.of(extensiveStats));

        assertAll(
                () -> assertDoesNotThrow(() -> {
                    byte[] pdf1 = analyticsService.exportMeetingStatisticsToPdf(100L);
                    assertNotNull(pdf1);
                }, "Should generate PDF with minimal data (likely 1 page)"),
                () -> assertDoesNotThrow(() -> {
                    byte[] pdf2 = analyticsService.exportMeetingStatisticsToPdf(200L);
                    assertNotNull(pdf2);
                    // Extensive data might generate more pages
                }, "Should generate PDF with extensive data (possibly multiple pages)")
        );
    }

    // ========== Testy dla getDateRangeText() ==========

    @Test
    void getDateRangeText_shouldHandleAllBranches() throws Exception {
        // Użyj refleksji do testowania prywatnej metody
        Method getDateRangeTextMethod = MeetingAnalyticsServiceImpl.class
                .getDeclaredMethod("getDateRangeText", ReportFilter.class);
        getDateRangeTextMethod.setAccessible(true);

        // Test 1: filter = null
        String result1 = (String) getDateRangeTextMethod.invoke(analyticsService, (Object) null);
        assertEquals("Cały okres", result1, "Should return 'Cały okres' for null filter");

        // Test 2: filter z obiema datami null
        ReportFilter emptyFilter = new ReportFilter();
        String result2 = (String) getDateRangeTextMethod.invoke(analyticsService, emptyFilter);
        assertEquals("Cały okres", result2, "Should return 'Cały okres' for filter with both dates null");

        // Test 3: filter tylko z dateFrom
        ReportFilter fromOnlyFilter = new ReportFilter();
        fromOnlyFilter.setDateFrom(LocalDateTime.of(2024, 3, 15, 10, 30));
        String result3 = (String) getDateRangeTextMethod.invoke(analyticsService, fromOnlyFilter);
        assertEquals("15.03.2024 - nieokreślony", result3, "Should handle only dateFrom");

        // Test 4: filter tylko z dateTo
        ReportFilter toOnlyFilter = new ReportFilter();
        toOnlyFilter.setDateTo(LocalDateTime.of(2024, 3, 31, 23, 59));
        String result4 = (String) getDateRangeTextMethod.invoke(analyticsService, toOnlyFilter);
        assertEquals("nieokreślony - 31.03.2024", result4, "Should handle only dateTo");

        // Test 5: filter z obiema datami
        ReportFilter fullFilter = new ReportFilter();
        fullFilter.setDateFrom(LocalDateTime.of(2024, 3, 1, 0, 0));
        fullFilter.setDateTo(LocalDateTime.of(2024, 3, 31, 23, 59));
        String result5 = (String) getDateRangeTextMethod.invoke(analyticsService, fullFilter);
        assertEquals("01.03.2024 - 31.03.2024", result5, "Should handle both dates");

        // Test 6: filter z datami w różnych miesiącach
        ReportFilter crossMonthFilter = new ReportFilter();
        crossMonthFilter.setDateFrom(LocalDateTime.of(2024, 2, 28, 14, 0));
        crossMonthFilter.setDateTo(LocalDateTime.of(2024, 3, 15, 16, 30));
        String result6 = (String) getDateRangeTextMethod.invoke(analyticsService, crossMonthFilter);
        assertEquals("28.02.2024 - 15.03.2024", result6, "Should handle cross-month dates");

        // Test 7: filter z tymi samymi datami
        ReportFilter sameDateFilter = new ReportFilter();
        LocalDateTime sameDate = LocalDateTime.of(2024, 5, 20, 12, 0);
        sameDateFilter.setDateFrom(sameDate);
        sameDateFilter.setDateTo(sameDate);
        String result7 = (String) getDateRangeTextMethod.invoke(analyticsService, sameDateFilter);
        assertEquals("20.05.2024 - 20.05.2024", result7, "Should handle same date for from and to");
    }

    @Test
    void getDateRangeText_shouldHandleEdgeCases() throws Exception {
        Method getDateRangeTextMethod = MeetingAnalyticsServiceImpl.class
                .getDeclaredMethod("getDateRangeText", ReportFilter.class);
        getDateRangeTextMethod.setAccessible(true);

        // Test z datą na granicy miesiąca
        ReportFilter edgeDateFilter = new ReportFilter();
        edgeDateFilter.setDateFrom(LocalDateTime.of(2024, 12, 31, 23, 59));
        edgeDateFilter.setDateTo(LocalDateTime.of(2025, 1, 1, 0, 0));
        String result = (String) getDateRangeTextMethod.invoke(analyticsService, edgeDateFilter);
        assertEquals("31.12.2024 - 01.01.2025", result, "Should handle year boundary dates");

        // Test z datą null w jednym polu, ale nie w drugim
        ReportFilter partialNullFilter = new ReportFilter();
        partialNullFilter.setDateFrom(null);
        partialNullFilter.setDateTo(LocalDateTime.now());
        String result2 = (String) getDateRangeTextMethod.invoke(analyticsService, partialNullFilter);
        assertTrue(result2.startsWith("nieokreślony"), "Should handle partial null dates");
    }

    @Test
    void testFormatDecimalThroughPdfGeneration() {
        MeetingStatistics stats = MeetingStatistics.builder()
                .id(1L)
                .meeting(createTestMeeting())
                .totalParticipants(10)
                .attendedParticipants(8)
                .attendanceRate(new BigDecimal("85.50"))
                .averageRating(new BigDecimal("4.75"))
                .feedbackCount(5)
                .status(MeetingStatistics.StatisticsStatus.FINAL)
                .generatedAt(LocalDateTime.now())
                .build();

        when(statisticsRepository.findByMeetingId(100L)).thenReturn(Optional.of(stats));

        assertDoesNotThrow(() -> {
            byte[] pdf = analyticsService.exportMeetingStatisticsToPdf(100L);
            assertNotNull(pdf);
            assertTrue(pdf.length > 0);
        }, "Should generate PDF with formatted decimals and percentages");
    }

    @Test
    void testPdfGenerationExceptionHandling() {
        // Test czy wyjątki w PDF generation są właściwie obsługiwane
        MeetingStatistics stats = MeetingStatistics.builder()
                .id(1L)
                .meeting(null) // Celowo null, aby sprawdzić obsługę wyjątków
                .status(null)  // Celowo null
                .generatedAt(null) // Celowo null
                .build();

        when(statisticsRepository.findByMeetingId(100L)).thenReturn(Optional.of(stats));

        // Powinno rzucić wyjątek, ale być właściwie obsłużone przez metodę
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> analyticsService.exportMeetingStatisticsToPdf(100L));

        assertTrue(exception.getMessage().contains("Failed to generate PDF"),
                "Should throw exception with proper message");
    }

    @Test
    void testOrganizerReportPdfGeneration() {
        // Test generowania raportu organizatora PDF
        MeetingStatistics stats = MeetingStatistics.builder()
                .id(1L)
                .meeting(createTestMeeting())
                .totalParticipants(10)
                .attendedParticipants(8)
                .attendanceRate(new BigDecimal("80.00"))
                .status(MeetingStatistics.StatisticsStatus.FINAL)
                .generatedAt(LocalDateTime.now())
                .build();

        when(statisticsRepository.findByOrganizerId(1L)).thenReturn(java.util.Collections.singletonList(stats));

        assertDoesNotThrow(() -> {
            byte[] pdf = analyticsService.exportReportToPdf(1L, null);
            assertNotNull(pdf);
            assertTrue(pdf.length > 0, "Organizer report PDF should not be empty");
        }, "Should generate organizer report PDF without exceptions");
    }
}

package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.*;
import com.meethub.domain.model.enums.*;
import com.meethub.domain.model.request.ReportFilter;
import com.meethub.domain.model.response.OrganizerReport;
import com.meethub.domain.model.response.ReportSummary;
import com.meethub.domain.repository.jpa.*;
import com.meethub.exception.BusinessException;
import com.meethub.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingAnalyticsServiceImplTest {

    @Mock private MeetingRepository meetingRepository;
    @Mock private MeetingParticipantRepository participantRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private TaskAssignmentRepository assignmentRepository;
    @Mock private MeetingStatisticsRepository statisticsRepository;
    @Mock private FeedbackRepository feedbackRepository;

    @InjectMocks private MeetingAnalyticsServiceImpl analyticsService;

    private Meeting testMeeting;
    private User testOrganizer;
    private MeetingStatistics testStatistics;
    private MeetingParticipant testParticipant;

    @BeforeEach
    void setUp() {
        testOrganizer = new User();
        testOrganizer.setId(1L);
        testOrganizer.setFirstName("John");
        testOrganizer.setLastName("Doe");
        testOrganizer.setEmail("john@example.com");

        // Tworzenie Meeting bez użycia .id() w builderze
        testMeeting = Meeting.builder()
                .title("Test Meeting")
                .description("Test Description")
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().minusHours(1))
                .type(MeetingType.ONLINE) // Dodaj typ
                .visibility(MeetingVisibility.PUBLIC) // Dodaj widoczność
                .organizer(testOrganizer)
                .build();
        testMeeting.setId(1L); // Ustawiamy ID bezpośrednio
        testMeeting.setStatus(MeetingStatus.COMPLETED);

        testStatistics = MeetingStatistics.builder()
                .meeting(testMeeting)
                .totalParticipants(10)
                .confirmedParticipants(8)
                .attendedParticipants(6)
                .attendanceRate(new BigDecimal("60.00"))
                .confirmationRate(new BigDecimal("80.00"))
                .engagementScore(new BigDecimal("75.00"))
                .taskCompletionRate(new BigDecimal("80.00"))
                .avgFeedbackRating(new BigDecimal("4.5"))
                .feedbackCount(5)
                .noShowCount(2)
                .generatedAt(LocalDateTime.now().minusHours(2))
                .updatedAt(LocalDateTime.now().minusHours(2))
                .build();
        testStatistics.setId(1L);

        testParticipant = MeetingParticipant.builder()
                .meeting(testMeeting)
                .user(testOrganizer) // Używamy istniejącego użytkownika
                .status(ParticipationStatus.ATTENDED)
                .build();
        testParticipant.setId(1L);
    }

    // ========== TESTY generateMeetingStatistics ==========

    @Test
    void generateMeetingStatistics_shouldCreateStatistics_whenValidMeeting() {
        // Given
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.findByMeetingId(1L)).thenReturn(List.of(testParticipant));
        when(taskRepository.findByMeetingId(1L)).thenReturn(Collections.emptyList());
        when(feedbackRepository.countByMeetingId(1L)).thenReturn(0L);
        when(feedbackRepository.findAverageRatingByMeetingId(1L)).thenReturn(null);
        when(statisticsRepository.findByMeetingId(1L)).thenReturn(Optional.empty());
        when(statisticsRepository.save(any(MeetingStatistics.class))).thenAnswer(invocation -> {
            MeetingStatistics stats = invocation.getArgument(0);
            stats.setId(1L);
            return stats;
        });

        // When
        MeetingStatistics result = analyticsService.generateMeetingStatistics(1L);

        // Then
        assertNotNull(result);
        assertEquals(testMeeting, result.getMeeting());
        assertEquals(1, result.getTotalParticipants());
        verify(statisticsRepository).save(any(MeetingStatistics.class));
    }

//    @Test
//    void generateMeetingStatistics_shouldUpdateExistingStatistics() {
//        // Given
//        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));
//        when(participantRepository.findByMeetingId(1L)).thenReturn(List.of(testParticipant));
//        when(taskRepository.findByMeetingId(1L)).thenReturn(Collections.emptyList());
//        when(feedbackRepository.countByMeetingId(1L)).thenReturn(0L);
//        when(feedbackRepository.findAverageRatingByMeetingId(1L)).thenReturn(null);
//        when(statisticsRepository.findByMeetingId(1L)).thenReturn(Optional.of(testStatistics));
//        when(statisticsRepository.save(any(MeetingStatistics.class))).thenReturn(testStatistics);
//
//        // When
//        MeetingStatistics result = analyticsService.generateMeetingStatistics(1L);
//
//        // Then
//        assertNotNull(result);
//        assertEquals(testStatistics, result);
//        verify(statisticsRepository).save(any(MeetingStatistics.class));
//    }

    @Test
    void generateMeetingStatistics_shouldThrow_whenMeetingNotFound() {
        // Given
        when(meetingRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> analyticsService.generateMeetingStatistics(999L));
    }

    // ========== TESTY getMeetingStatistics ==========

    @Test
    void getMeetingStatistics_shouldReturnStatistics_whenExists() {
        // Given
        when(statisticsRepository.findByMeetingId(1L)).thenReturn(Optional.of(testStatistics));

        // When
        Optional<MeetingStatistics> result = analyticsService.getMeetingStatistics(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testStatistics, result.get());
    }

    @Test
    void getMeetingStatistics_shouldReturnEmpty_whenNotExists() {
        // Given
        when(statisticsRepository.findByMeetingId(1L)).thenReturn(Optional.empty());

        // When
        Optional<MeetingStatistics> result = analyticsService.getMeetingStatistics(1L);

        // Then
        assertTrue(result.isEmpty());
    }

    // ========== TESTY deleteMeetingStatistics ==========

    @Test
    void deleteMeetingStatistics_shouldDelete_whenStatisticsExist() {
        // Given
        when(statisticsRepository.findByMeetingId(1L)).thenReturn(Optional.of(testStatistics));
        doNothing().when(statisticsRepository).delete(testStatistics);

        // When
        analyticsService.deleteMeetingStatistics(1L);

        // Then
        verify(statisticsRepository).delete(testStatistics);
    }

    @Test
    void deleteMeetingStatistics_shouldDoNothing_whenStatisticsNotExist() {
        // Given
        when(statisticsRepository.findByMeetingId(1L)).thenReturn(Optional.empty());

        // When
        analyticsService.deleteMeetingStatistics(1L);

        // Then
        verify(statisticsRepository, never()).delete(any());
    }

    // ========== TESTY generateOrganizerReport ==========

    @Test
    void generateOrganizerReport_shouldGenerateReport_whenValidOrganizer() {
        // Given
        Long organizerId = 1L;
        ReportFilter filter = new ReportFilter();

        List<Meeting> meetings = List.of(testMeeting);
        when(meetingRepository.findByOrganizerId(organizerId)).thenReturn(meetings);
        when(statisticsRepository.findByMeetingId(anyLong())).thenReturn(Optional.of(testStatistics));

        // When
        OrganizerReport report = analyticsService.generateOrganizerReport(organizerId, filter);

        // Then
        assertNotNull(report);
        assertEquals(organizerId, report.getOrganizerId());
        assertNotNull(report.getSummary());
        assertNotNull(report.getGeneratedAt());
        assertEquals(1, report.getSummary().getTotalMeetings());
    }

    @Test
    void generateOrganizerReport_shouldHandleEmptyMeetings() {
        // Given
        Long organizerId = 1L;
        ReportFilter filter = new ReportFilter();
        when(meetingRepository.findByOrganizerId(organizerId)).thenReturn(Collections.emptyList());

        // When
        OrganizerReport report = analyticsService.generateOrganizerReport(organizerId, filter);

        // Then
        assertNotNull(report);
        assertEquals(organizerId, report.getOrganizerId());
        assertEquals(0, report.getSummary().getTotalMeetings());
    }

    // ========== TESTY getMeetingStatisticsByOrganizer ==========

    @Test
    void getMeetingStatisticsByOrganizer_shouldReturnStatisticsList() {
        // Given
        Long organizerId = 1L;
        List<Meeting> meetings = List.of(testMeeting);
        when(meetingRepository.findByOrganizerId(organizerId)).thenReturn(meetings);
        when(statisticsRepository.findByMeetingId(1L)).thenReturn(Optional.of(testStatistics));

        // When
        List<MeetingStatistics> result = analyticsService.getMeetingStatisticsByOrganizer(organizerId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testStatistics, result.get(0));
    }

    @Test
    void getMeetingStatisticsByOrganizer_shouldFilterOutNullStatistics() {
        // Given
        Long organizerId = 1L;
        Meeting meeting2 = Meeting.builder()
                .title("Another Meeting")
                .type(MeetingType.ONLINE)
                .visibility(MeetingVisibility.PUBLIC)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusHours(1))
                .organizer(testOrganizer)
                .build();
        meeting2.setId(2L);

        List<Meeting> meetings = List.of(testMeeting, meeting2);
        when(meetingRepository.findByOrganizerId(organizerId)).thenReturn(meetings);
        when(statisticsRepository.findByMeetingId(1L)).thenReturn(Optional.of(testStatistics));
        when(statisticsRepository.findByMeetingId(2L)).thenReturn(Optional.empty());

        // When
        List<MeetingStatistics> result = analyticsService.getMeetingStatisticsByOrganizer(organizerId);

        // Then
        assertEquals(1, result.size());
        assertEquals(testStatistics, result.get(0));
    }

    // ========== TESTY refreshAllStatistics ==========

    @Test
    void refreshAllStatistics_shouldRefreshCompletedMeetings() {
        // Given
        testMeeting.setStatus(MeetingStatus.COMPLETED);
        List<Meeting> completedMeetings = List.of(testMeeting);

        when(meetingRepository.findByStatus(MeetingStatus.COMPLETED)).thenReturn(completedMeetings);
        when(statisticsRepository.findByMeetingId(1L)).thenReturn(Optional.empty());

        // Mockowanie dla generateMeetingStatistics
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.findByMeetingId(1L)).thenReturn(List.of(testParticipant));
        when(taskRepository.findByMeetingId(1L)).thenReturn(Collections.emptyList());
        when(feedbackRepository.countByMeetingId(1L)).thenReturn(0L);
        when(feedbackRepository.findAverageRatingByMeetingId(1L)).thenReturn(null);
        when(statisticsRepository.save(any(MeetingStatistics.class))).thenAnswer(invocation -> {
            MeetingStatistics stats = invocation.getArgument(0);
            stats.setId(1L);
            return stats;
        });

        // When
        analyticsService.refreshAllStatistics();

        // Then
        verify(statisticsRepository, times(1)).save(any(MeetingStatistics.class));
    }

    @Test
    void refreshAllStatistics_shouldSkipUpToDateStatistics() {
        // Given
        testMeeting.setStatus(MeetingStatus.COMPLETED);
        List<Meeting> completedMeetings = List.of(testMeeting);

        when(meetingRepository.findByStatus(MeetingStatus.COMPLETED)).thenReturn(completedMeetings);

        MeetingStatistics freshStats = MeetingStatistics.builder()
                .meeting(testMeeting)
                .updatedAt(LocalDateTime.now().minusMinutes(30))
                .build();
        freshStats.setId(1L);
        when(statisticsRepository.findByMeetingId(1L)).thenReturn(Optional.of(freshStats));

        // When
        analyticsService.refreshAllStatistics();

        // Then - nie powinno wywołać generateMeetingStatistics
        verify(meetingRepository, never()).findById(anyLong());
        verify(statisticsRepository, never()).save(any());
    }

    @Test
    void refreshAllStatistics_shouldHandleEmptyList() {
        // Given
        when(meetingRepository.findByStatus(MeetingStatus.COMPLETED)).thenReturn(Collections.emptyList());

        // When
        analyticsService.refreshAllStatistics();

        // Then
        verify(statisticsRepository, never()).save(any());
    }

    @Test
    void refreshAllStatistics_shouldHandleExceptionsGracefully() {
        // Given
        Meeting meetingWithError = Meeting.builder()
                .title("Error Meeting")
                .type(MeetingType.ONLINE)
                .visibility(MeetingVisibility.PUBLIC)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusHours(1))
                .organizer(testOrganizer)
                .build();
        meetingWithError.setId(999L);
        meetingWithError.setStatus(MeetingStatus.COMPLETED);

        testMeeting.setStatus(MeetingStatus.COMPLETED);
        List<Meeting> completedMeetings = List.of(testMeeting, meetingWithError);

        when(meetingRepository.findByStatus(MeetingStatus.COMPLETED)).thenReturn(completedMeetings);
        when(statisticsRepository.findByMeetingId(1L)).thenReturn(Optional.empty());
        when(statisticsRepository.findByMeetingId(999L)).thenReturn(Optional.empty());

        // Pierwsze spotkanie OK
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.findByMeetingId(1L)).thenReturn(List.of(testParticipant));
        when(taskRepository.findByMeetingId(1L)).thenReturn(Collections.emptyList());
        when(feedbackRepository.countByMeetingId(1L)).thenReturn(0L);
        when(feedbackRepository.findAverageRatingByMeetingId(1L)).thenReturn(null);

        // Drugie spotkanie rzuci wyjątek
        when(meetingRepository.findById(999L)).thenThrow(new ResourceNotFoundException("Not found"));

        when(statisticsRepository.save(any(MeetingStatistics.class))).thenAnswer(invocation -> {
            MeetingStatistics stats = invocation.getArgument(0);
            stats.setId(1L);
            return stats;
        });

        // When
        assertDoesNotThrow(() -> analyticsService.refreshAllStatistics());

        // Then
        verify(statisticsRepository, times(1)).save(any(MeetingStatistics.class));
    }

    // ========== TESTY getRecentStatistics ==========

    @Test
    void getRecentStatistics_shouldReturnLimitedStatistics() {
        // Given
        int limit = 5;
        Pageable pageable = PageRequest.of(0, limit);
        List<MeetingStatistics> statsList = List.of(testStatistics);

        when(statisticsRepository.findTopNByOrderByGeneratedAtDesc(pageable)).thenReturn(statsList);

        // When
        List<MeetingStatistics> result = analyticsService.getRecentStatistics(limit);

        // Then
        assertEquals(1, result.size());
        assertEquals(testStatistics, result.get(0));
    }

    // ========== TESTY getStatisticsOverview ==========

    @Test
    void getStatisticsOverview_shouldReturnOverview_whenStatisticsExist() {
        // Given
        when(statisticsRepository.findByMeetingId(1L)).thenReturn(Optional.of(testStatistics));

        // When
        Map<String, Object> overview = analyticsService.getStatisticsOverview(1L);

        // Then
        assertNotNull(overview);
        assertEquals(1L, overview.get("meetingId"));
        assertEquals("Test Meeting", overview.get("meetingTitle"));
        assertEquals(new BigDecimal("60.00"), overview.get("attendanceRate"));
        assertEquals(new BigDecimal("75.00"), overview.get("engagementScore"));
        assertEquals(5, overview.get("feedbackCount"));
        assertNotNull(overview.get("grade"));
    }

    @Test
    void getStatisticsOverview_shouldReturnEmptyMap_whenNoStatistics() {
        // Given
        when(statisticsRepository.findByMeetingId(1L)).thenReturn(Optional.empty());

        // When
        Map<String, Object> overview = analyticsService.getStatisticsOverview(1L);

        // Then
        assertNotNull(overview);
        assertTrue(overview.isEmpty());
    }

    // ========== TESTY calculateEngagementScore ==========

    @Test
    void calculateEngagementScore_shouldReturnZero_whenNoParticipants() {
        // When
        BigDecimal score = analyticsService.calculateEngagementScore(1L, Collections.emptyList());

        // Then
        assertEquals(BigDecimal.ZERO, score);
    }

    @Test
    void calculateTaskCompletionRate_shouldReturnZero_whenNoTasks() {
        // When
        BigDecimal rate = analyticsService.calculateTaskCompletionRate(Collections.emptyList());

        // Then
        assertEquals(BigDecimal.ZERO, rate);
    }

    @Test
    void calculateAverageRating_shouldReturnZero_whenNoFeedback() {
        // Given
        when(feedbackRepository.findAverageRatingByMeetingId(1L)).thenReturn(null);

        // When
        BigDecimal rating = analyticsService.calculateAverageRating(1L);

        // Then
        assertEquals(BigDecimal.ZERO, rating);
    }

    // ========== TESTY calculateOrganizerSummary ==========

    @Test
    void calculateOrganizerSummary_shouldReturnEmpty_whenNoMeetings() {
        // When
        ReportSummary summary = analyticsService.calculateOrganizerSummary(Collections.emptyList());

        // Then
        assertNotNull(summary);
        assertEquals(0, summary.getTotalMeetings());
        assertEquals(0, summary.getTotalParticipants());
        assertEquals(BigDecimal.ZERO, summary.getAvgAttendanceRate());
        assertEquals(BigDecimal.ZERO, summary.getAvgEngagementScore());
    }

    @Test
    void calculateOrganizerSummary_shouldCalculateCorrectAverages() {
        // Given
        MeetingStatistics stats1 = MeetingStatistics.builder()
                .totalParticipants(10)
                .attendanceRate(new BigDecimal("70.00"))
                .engagementScore(new BigDecimal("80.00"))
                .build();
        stats1.setId(1L);

        MeetingStatistics stats2 = MeetingStatistics.builder()
                .totalParticipants(20)
                .attendanceRate(new BigDecimal("80.00"))
                .engagementScore(new BigDecimal("90.00"))
                .build();
        stats2.setId(2L);

        Meeting meeting1 = Meeting.builder()
                .title("Meeting 1")
                .type(MeetingType.ONLINE)
                .visibility(MeetingVisibility.PUBLIC)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusHours(1))
                .organizer(testOrganizer)
                .build();
        meeting1.setId(1L);

        Meeting meeting2 = Meeting.builder()
                .title("Meeting 2")
                .type(MeetingType.ONLINE)
                .visibility(MeetingVisibility.PUBLIC)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusHours(1))
                .organizer(testOrganizer)
                .build();
        meeting2.setId(2L);

        when(statisticsRepository.findByMeetingId(anyLong()))
                .thenReturn(Optional.of(stats1))
                .thenReturn(Optional.of(stats2));

        // When
        ReportSummary summary = analyticsService.calculateOrganizerSummary(List.of(meeting1, meeting2));

        // Then
        assertEquals(2, summary.getTotalMeetings());
        assertEquals(30, summary.getTotalParticipants());
        assertEquals(new BigDecimal("75.00"), summary.getAvgAttendanceRate());
        assertEquals(new BigDecimal("85.00"), summary.getAvgEngagementScore());
    }

    @Test
    void calculateOrganizerSummary_shouldSkipMeetingsWithoutStatistics() {
        // Given
        MeetingStatistics stats = MeetingStatistics.builder()
                .totalParticipants(10)
                .attendanceRate(new BigDecimal("70.00"))
                .engagementScore(new BigDecimal("80.00"))
                .build();
        stats.setId(1L);

        Meeting meetingWithStats = Meeting.builder()
                .title("Meeting 1")
                .type(MeetingType.ONLINE)
                .visibility(MeetingVisibility.PUBLIC)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusHours(1))
                .organizer(testOrganizer)
                .build();
        meetingWithStats.setId(1L);

        Meeting meetingWithoutStats = Meeting.builder()
                .title("Meeting 2")
                .type(MeetingType.ONLINE)
                .visibility(MeetingVisibility.PUBLIC)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusHours(1))
                .organizer(testOrganizer)
                .build();
        meetingWithoutStats.setId(2L);

        when(statisticsRepository.findByMeetingId(anyLong()))
                .thenAnswer(invocation -> {
                    Long meetingId = invocation.getArgument(0);
                    if (meetingId == 1L) {
                        return Optional.of(stats);
                    } else {
                        return Optional.empty();
                    }
                });

        // When
        ReportSummary summary = analyticsService.calculateOrganizerSummary(List.of(meetingWithStats, meetingWithoutStats));

        // Then
        assertEquals(2, summary.getTotalMeetings());
        assertEquals(10, summary.getTotalParticipants());
        assertEquals(new BigDecimal("70.00"), summary.getAvgAttendanceRate());
        assertEquals(new BigDecimal("80.00"), summary.getAvgEngagementScore());
    }

    // ========== TESTY getAverageResponseTime ==========

    @Test
    void getAverageResponseTime_shouldReturnZero_whenNoParticipants() {
        // Given
        when(participantRepository.findByMeetingId(1L)).thenReturn(Collections.emptyList());

        // When
        BigDecimal responseTime = analyticsService.getAverageResponseTime(1L);

        // Then
        assertEquals(BigDecimal.ZERO, responseTime);
    }

    // ========== TESTY calculateGrade ==========

    @Test
    void calculateGrade_shouldReturnGradeA_whenHighScores() {
        // Given
        MeetingStatistics stats = MeetingStatistics.builder()
                .attendanceRate(new BigDecimal("95.00"))
                .engagementScore(new BigDecimal("90.00"))
                .build();

        // When
        String grade = analyticsService.calculateGrade(stats);

        // Then
        assertEquals("A", grade);
    }

    @Test
    void calculateGrade_shouldReturnNA_whenNullValues() {
        // Given
        MeetingStatistics stats = MeetingStatistics.builder()
                .attendanceRate(null)
                .engagementScore(new BigDecimal("90.00"))
                .build();

        // When
        String grade = analyticsService.calculateGrade(stats);

        // Then
        assertEquals("N/A", grade);
    }
}
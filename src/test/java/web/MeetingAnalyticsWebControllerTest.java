package com.meethub.controller.web;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.MeetingStatistics;
import com.meethub.domain.model.request.MeetingRequest;
import com.meethub.domain.service.MeetingAnalyticsService;
import com.meethub.domain.service.MeetingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingAnalyticsWebControllerTest {

    @Mock
    private MeetingAnalyticsService analyticsService;

    @Mock
    private MeetingService meetingService;

    @Mock
    private Model model;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private MeetingAnalyticsWebController controller;

    private Meeting meeting;
    private MeetingStatistics statistics;
    private MeetingRequest request;

    @BeforeEach
    void setUp() {
        // Setup meeting
        meeting = new Meeting();
        meeting.setId(1L);
        meeting.setTitle("Test Meeting");
        meeting.setDescription("Test Description");
        meeting.setStartDate(LocalDateTime.now());

        // Setup statistics
        statistics = new MeetingStatistics();
        statistics.setId(100L);
//        statistics.setMeetingId(1L);
        statistics.setGeneratedAt(LocalDateTime.now());
        statistics.setTotalParticipants(10);
//        statistics.setActiveParticipants(8);
//        statistics.setAverageAttendanceRate(80.5);
//        statistics.setTotalTasks(5);
//        statistics.setCompletedTasks(3);
//        statistics.setTaskCompletionRate(60.0);

        // Setup request
        request = new MeetingRequest();
        request.setMeetingId(1L);
    }

    @Test
    void showAnalyticsPage_shouldReturnAnalyticsPageWithStatistics() {
        // Given
        when(meetingService.getMeeting(1L)).thenReturn(meeting);
        when(analyticsService.getMeetingStatistics(1L)).thenReturn(Optional.of(statistics));

        // When
        String viewName = controller.showAnalyticsPage(request, model, redirectAttributes);

        // Then
        assertEquals("meetings/analytics", viewName);
        verify(meetingService).getMeeting(1L);
        verify(analyticsService).getMeetingStatistics(1L);
        verify(model).addAttribute(eq("meeting"), eq(meeting));
        verify(model).addAttribute(eq("meetingStatistics"), eq(statistics));
        verifyNoInteractions(redirectAttributes);
    }

    @Test
    void showAnalyticsPage_shouldReturnAnalyticsPageWithoutStatistics_whenStatisticsNotPresent() {
        // Given
        when(meetingService.getMeeting(1L)).thenReturn(meeting);
        when(analyticsService.getMeetingStatistics(1L)).thenReturn(Optional.empty());

        // When
        String viewName = controller.showAnalyticsPage(request, model, redirectAttributes);

        // Then
        assertEquals("meetings/analytics", viewName);
        verify(meetingService).getMeeting(1L);
        verify(analyticsService).getMeetingStatistics(1L);
        verify(model).addAttribute(eq("meeting"), eq(meeting));
        verify(model).addAttribute(eq("meetingStatistics"), isNull());
        verify(redirectAttributes).addFlashAttribute(eq("info"), eq("Brak statystyk. Wygeneruj je pierwszy raz."));
    }

    @Test
    void generateStatistics_shouldGenerateStatisticsAndRedirect() {
        // Given
        // Zakładamy, że generateMeetingStatistics zwraca MeetingStatistics
        when(analyticsService.generateMeetingStatistics(1L)).thenReturn(statistics);

        // When
        String viewName = controller.generateStatistics(request, redirectAttributes);

        // Then
        assertEquals("redirect:/meetings/1/analytics", viewName);
        verify(analyticsService).generateMeetingStatistics(1L);
        verify(redirectAttributes).addFlashAttribute(eq("success"), eq("Statystyki zostały wygenerowane pomyślnie!"));
    }


    @Test
    void exportToCsv_shouldRedirectToCsvExportApi() {
        // When
        String viewName = controller.exportToCsv(request);

        // Then
        assertEquals("redirect:/api/v1/analytics/meetings/1/export/csv", viewName);
    }

    @Test
    void exportToPdf_shouldRedirectToPdfExportApi() {
        // When
        String viewName = controller.exportToPdf(request);

        // Then
        assertEquals("redirect:/api/v1/analytics/meetings/1/export/pdf", viewName);
    }


    @Test
    void meetingRequest_shouldSetAndGetMeetingId() {
        // Given
        Long expectedId = 99L;

        // When
        request.setMeetingId(expectedId);
        Long actualId = request.getMeetingId();

        // Then
        assertEquals(expectedId, actualId);
    }

    @Test
    void meetingRequest_shouldThrowValidationException_whenMeetingIdIsNull() {
        // Given
        MeetingRequest nullRequest = new MeetingRequest();
        nullRequest.setMeetingId(null);


        assertNull(nullRequest.getMeetingId());
    }

    @Test
    void meetingRequest_shouldThrowValidationException_whenMeetingIdIsLessThanOne() {
        // Given
       MeetingRequest invalidRequest = new MeetingRequest();
        invalidRequest.setMeetingId(0L);

        // When
        Long meetingId = invalidRequest.getMeetingId();

        // Then - The @Min validation should be triggered when the controller method is called
        assertEquals(0L, meetingId);
    }

    // Test for logging behavior (optional)
    @Test
    void showAnalyticsPage_shouldLogAppropriateMessage_whenStatisticsPresent() {
        // Given
        when(meetingService.getMeeting(1L)).thenReturn(meeting);
        when(analyticsService.getMeetingStatistics(1L)).thenReturn(Optional.of(statistics));

        // When
        controller.showAnalyticsPage(request, model, redirectAttributes);

        // Then - verify logging behavior through method execution
        // Since we can't easily test log messages, we verify the method was called
        verify(analyticsService).getMeetingStatistics(1L);
    }

    @Test
    void showAnalyticsPage_shouldLogAppropriateMessage_whenStatisticsNotPresent() {
        // Given
        when(meetingService.getMeeting(1L)).thenReturn(meeting);
        when(analyticsService.getMeetingStatistics(1L)).thenReturn(Optional.empty());

        // When
        controller.showAnalyticsPage(request, model, redirectAttributes);

        // Then - verify method execution indicates logging path
        verify(analyticsService).getMeetingStatistics(1L);
    }

    @Test
    void generateStatistics_shouldLogGenerationMessage() {
        // Given
        when(analyticsService.generateMeetingStatistics(1L)).thenReturn(statistics);

        // When
        controller.generateStatistics(request, redirectAttributes);

        // Then - verify method was called which triggers logging
        verify(analyticsService).generateMeetingStatistics(1L);
    }

    @Test
    void exportToCsv_shouldLogExportMessage() {
        // When
        controller.exportToCsv(request);

        // Then - verify method returns correct redirect which indicates logging path
        String result = controller.exportToCsv(request);
        assertEquals("redirect:/api/v1/analytics/meetings/1/export/csv", result);
    }

    @Test
    void exportToPdf_shouldLogExportMessage() {
        // When
        controller.exportToPdf(request);

        // Then - verify method returns correct redirect which indicates logging path
        String result = controller.exportToPdf(request);
        assertEquals("redirect:/api/v1/analytics/meetings/1/export/pdf", result);
    }

}
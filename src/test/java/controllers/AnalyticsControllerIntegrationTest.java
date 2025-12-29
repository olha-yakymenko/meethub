package com.meethub.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.MeetingStatistics;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.MeetingStatus;
import com.meethub.domain.model.enums.MeetingType;
import com.meethub.domain.model.enums.MeetingVisibility;
import com.meethub.domain.model.request.MeetingIdRequest;
import com.meethub.domain.model.response.MeetingStatisticsResponse;
import com.meethub.domain.repository.jpa.MeetingRepository;
import com.meethub.domain.repository.jpa.MeetingStatisticsRepository;
import com.meethub.domain.repository.jpa.UserRepository;
import com.meethub.domain.service.MeetingAnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class AnalyticsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private MeetingAnalyticsService analyticsService;

    private User testUser;
    private Meeting testMeeting;
    private MeetingStatistics mockStatistics;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .password("password")
                .build();
        testUser = userRepository.save(testUser);

        testMeeting = Meeting.builder()
                .title("Test Meeting")
                .description("Test Description")
                .startDate(LocalDateTime.now().minusDays(2))
                .endDate(LocalDateTime.now().minusDays(1))
                .organizer(testUser)
                .type(MeetingType.ONLINE)
                .visibility(MeetingVisibility.PUBLIC)
                .build();
        testMeeting = meetingRepository.save(testMeeting);

        // Mock MeetingStatistics
        mockStatistics = MeetingStatistics.builder()
                .id(1L)
                .meeting(testMeeting)
                .totalParticipants(10)
                .attendedParticipants(8)
                .confirmedParticipants(9)
                .declinedParticipants(1)
                .pendingParticipants(0)
                .attendanceRate(BigDecimal.valueOf(80.0))
                .confirmationRate(BigDecimal.valueOf(90.0))
                .avgResponseTimeMinutes(BigDecimal.valueOf(15.5))
                .averageRating(BigDecimal.valueOf(4.5))
                .feedbackCount(7)
                .status(MeetingStatistics.StatisticsStatus.FINAL)
                .finalized(true)
                .generatedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .additionalMetrics(Map.of("key", "value"))
                .build();
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void generateMeetingStatistics_shouldReturnStatistics() throws Exception {
        // Given
        when(analyticsService.generateMeetingStatistics(anyLong())).thenReturn(mockStatistics);
        MeetingIdRequest request = new MeetingIdRequest();
        request.setMeetingId(testMeeting.getId());

        // When & Then
        mockMvc.perform(post("/api/v1/analytics/meetings/statistics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Statystyki wygenerowane")))
                .andExpect(jsonPath("$.data.meetingId", is(testMeeting.getId().intValue())))
                .andExpect(jsonPath("$.data.meetingTitle", is("Test Meeting")))
                .andExpect(jsonPath("$.data.totalParticipants", is(10)))
                .andExpect(jsonPath("$.data.attendanceRate", is(80.0)))
                .andExpect(jsonPath("$.data.finalized", is(true)));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void getMeetingReport_shouldReturnReport() throws Exception {
        // Given
        when(analyticsService.generateMeetingStatistics(anyLong())).thenReturn(mockStatistics);
        MeetingIdRequest request = new MeetingIdRequest();
        request.setMeetingId(testMeeting.getId());

        // When & Then
        mockMvc.perform(post("/api/v1/analytics/meetings/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Raport spotkania wygenerowany")))
                .andExpect(jsonPath("$.data.meetingId", is(testMeeting.getId().intValue())))
                .andExpect(jsonPath("$.data.meetingTitle", is("Test Meeting")));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void exportMeetingCsv_withPost_shouldReturnCsvFile() throws Exception {
        // Given
        byte[] csvData = "id,title,participants\n1,Test Meeting,10".getBytes();
        when(analyticsService.exportMeetingStatisticsToCsv(anyLong())).thenReturn(csvData);
        MeetingIdRequest request = new MeetingIdRequest();
        request.setMeetingId(testMeeting.getId());

        // When & Then
        mockMvc.perform(post("/api/v1/analytics/meetings/export/csv")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString(".csv")))
                .andExpect(content().bytes(csvData));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void exportMeetingPdf_withPost_shouldReturnPdfFile() throws Exception {
        // Given
        byte[] pdfData = "PDF_CONTENT".getBytes();
        when(analyticsService.exportMeetingStatisticsToPdf(anyLong())).thenReturn(pdfData);
        MeetingIdRequest request = new MeetingIdRequest();
        request.setMeetingId(testMeeting.getId());

        // When & Then
        mockMvc.perform(post("/api/v1/analytics/meetings/export/pdf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", is("application/pdf")))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString(".pdf")))
                .andExpect(content().bytes(pdfData));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void exportMeetingCsv_withGet_shouldReturnCsvFile() throws Exception {
        // Given
        byte[] csvData = "id,title,participants\n1,Test Meeting,10".getBytes();
        when(analyticsService.exportMeetingStatisticsToCsv(anyLong())).thenReturn(csvData);

        // When & Then
        mockMvc.perform(get("/api/v1/analytics/meetings/{meetingId}/export/csv", testMeeting.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString(".csv")))
                .andExpect(content().bytes(csvData));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void exportMeetingPdf_withGet_shouldReturnPdfFile() throws Exception {
        // Given
        byte[] pdfData = "PDF_CONTENT".getBytes();
        when(analyticsService.exportMeetingStatisticsToPdf(anyLong())).thenReturn(pdfData);

        // When & Then
        mockMvc.perform(get("/api/v1/analytics/meetings/{meetingId}/export/pdf", testMeeting.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", is("application/pdf")))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString(".pdf")))
                .andExpect(content().bytes(pdfData));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void exportMeetingCsv_withInvalidMeetingId_shouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/analytics/meetings/{meetingId}/export/csv", 0))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void exportMeetingPdf_withInvalidMeetingId_shouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/analytics/meetings/{meetingId}/export/pdf", -1))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void convertToResponse_shouldReturnNullForNullInput() {
        // Given
        AnalyticsController controller = new AnalyticsController(analyticsService);

        // When
        MeetingStatisticsResponse response = controller.convertToResponse(null);

        // Then
        assert response == null;
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void convertToResponse_shouldHandleNullMeeting() throws Exception {
        // Given
        AnalyticsController controller = new AnalyticsController(analyticsService);
        MeetingStatistics statsWithoutMeeting = MeetingStatistics.builder()
                .id(1L)
                .meeting(null)
                .totalParticipants(5)
                .attendedParticipants(4)
                .build();

        // When
        MeetingStatisticsResponse response = controller.convertToResponse(statsWithoutMeeting);

        // Then
        assert response != null;
        assert response.getMeetingId() == null;
        assert response.getMeetingTitle() == null;
        assert response.getTotalParticipants() == 5;
    }
}
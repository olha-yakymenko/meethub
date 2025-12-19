package com.meethub.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.MeetingStatistics;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.MeetingStatus;
import com.meethub.domain.model.enums.MeetingType;
import com.meethub.domain.model.enums.MeetingVisibility;
import com.meethub.domain.model.response.ApiResponse;
import com.meethub.domain.repository.jpa.MeetingRepository;
import com.meethub.domain.repository.jpa.MeetingStatisticsRepository;
import com.meethub.domain.repository.jpa.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
//@ActiveProfiles("test")
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

    private User testUser;
    private Meeting testMeeting;

    @Autowired
    private MeetingStatisticsRepository statisticsRepository;

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
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void generateMeetingStatistics_shouldReturnStatistics() throws Exception {
        mockMvc.perform(post("/api/v1/analytics/meetings/{meetingId}/statistics", testMeeting.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("wygenerowane")))
                .andExpect(jsonPath("$.data", notNullValue()));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void getMeetingReport_shouldReturnReport() throws Exception {
        mockMvc.perform(post("/api/v1/analytics/meetings/{meetingId}/statistics", testMeeting.getId()));

        mockMvc.perform(get("/api/v1/analytics/meetings/{meetingId}/report", testMeeting.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", notNullValue()));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void exportMeetingStatisticsToCsv_shouldReturnCsvFile() throws Exception {
        mockMvc.perform(post("/api/v1/analytics/meetings/{meetingId}/statistics", testMeeting.getId()));

        mockMvc.perform(get("/api/v1/analytics/meetings/{meetingId}/export/csv", testMeeting.getId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().exists("Content-Disposition"));
    }


    @Test
    @WithMockUser(username = "test@example.com")
    void statisticsForCompletedMeeting_shouldHaveCorrectStatus() throws Exception {
        testMeeting.setStatus(MeetingStatus.COMPLETED);
        meetingRepository.save(testMeeting);

        mockMvc.perform(post("/api/v1/analytics/meetings/{meetingId}/statistics", testMeeting.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("FINAL")));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void exportMeetingPdf_shouldReturnPdfFile() throws Exception {
        // Given
        mockMvc.perform(post("/api/v1/analytics/meetings/{meetingId}/statistics", testMeeting.getId()));

        // When & Then
        mockMvc.perform(get("/api/v1/analytics/meetings/{meetingId}/export/pdf", testMeeting.getId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString("statystyki_spotkania_" + testMeeting.getId() + "_")))
                .andExpect(header().string("Content-Disposition", containsString(".pdf")))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }
}








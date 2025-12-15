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
        // First generate statistics
        mockMvc.perform(post("/api/v1/analytics/meetings/{meetingId}/statistics", testMeeting.getId()));

        // Then get report
        mockMvc.perform(get("/api/v1/analytics/meetings/{meetingId}/report", testMeeting.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", notNullValue()));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void exportMeetingStatisticsToCsv_shouldReturnCsvFile() throws Exception {
        // First generate statistics
        mockMvc.perform(post("/api/v1/analytics/meetings/{meetingId}/statistics", testMeeting.getId()));

        // Then export
        mockMvc.perform(get("/api/v1/analytics/meetings/{meetingId}/export/csv", testMeeting.getId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void getOrganizerReport_shouldReturnReport() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/organizers/{organizerId}/report", testUser.getId())
                        .param("dateRange.from", LocalDateTime.now().minusMonths(1).toString())
                        .param("dateRange.to", LocalDateTime.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.organizerId", is(testUser.getId().intValue())));
    }

}








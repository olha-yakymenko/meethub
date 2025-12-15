package com.meethub.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meethub.domain.model.entity.MeetingParticipant;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.service.ParticipationService;
import com.meethub.domain.repository.jpa.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@AutoConfigureMockMvc
//@ActiveProfiles("test")
@Transactional
@Rollback
class ParticipationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private ParticipationService participationService;

    private User testUser;
    private MeetingParticipant mockParticipant;
    private final Long MEETING_ID = 1L;

    @BeforeEach
    void setUp() {
        // 1. Sprawdź czy użytkownik z data.sql już istnieje
        Optional<User> existingUser = userRepository.findByEmail("test.user@example.com");

        if (existingUser.isPresent()) {
            // Użyj istniejącego użytkownika z data.sql
            testUser = existingUser.get();
        } else {
            // Jeśli nie istnieje (np. w testach bez data.sql), utwórz nowego z unikalnym emailem
            String uniqueEmail = "test.user." + UUID.randomUUID() + "@example.com";
            testUser = User.builder()
                    .firstName("Test")
                    .lastName("User")
                    .email(uniqueEmail)
                    .password("password")
                    .build();
            testUser = userRepository.save(testUser);
        }

        // Debug: sprawdź ID użytkownika
        System.out.println("Test User ID: " + testUser.getId());
        System.out.println("Test User Email: " + testUser.getEmail());

        mockParticipant = MeetingParticipant.builder()
                .id(1L)
                .status(ParticipationStatus.CONFIRMED)
                .build();
    }
//
//    @Test
//    @WithMockUser(username = "test.user@example.com")
//    void declineParticipation_ShouldReturnDeclinedParticipant() throws Exception {
//        // Given
//        mockParticipant.setStatus(ParticipationStatus.DECLINED);
//        when(participationService.declineParticipation(eq(MEETING_ID), eq(testUser.getId())))
//                .thenReturn(mockParticipant);
//
//        // When & Then
//        mockMvc.perform(post("/api/v1/participations/meetings/{meetingId}/decline", MEETING_ID)
//                        .with(csrf())
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success", is(true)))
//                .andExpect(jsonPath("$.message", is("Participation declined")));
//    }

    @Test
    @WithMockUser(username = "test.user@example.com")
    void getResponseStatistics_ShouldReturnStatistics() throws Exception {
        // Given
        Map<ParticipationStatus, Long> mockStats = new HashMap<>();
        mockStats.put(ParticipationStatus.CONFIRMED, 5L);
        mockStats.put(ParticipationStatus.DECLINED, 2L);
        mockStats.put(ParticipationStatus.PENDING, 3L);

        when(participationService.getResponseStatistics(MEETING_ID))
                .thenReturn(mockStats);

        // When & Then
        mockMvc.perform(get("/api/v1/participations/meetings/{meetingId}/response-stats", MEETING_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Statystyki odpowiedzi pobrane pomyślnie")))
                .andExpect(jsonPath("$.data.CONFIRMED", is(5)))
                .andExpect(jsonPath("$.data.DECLINED", is(2)))
                .andExpect(jsonPath("$.data.PENDING", is(3)));
    }

    @Test
    @WithMockUser(username = "test.user@example.com")
    void getAverageResponseTime_ShouldReturnAverageTime() throws Exception {
        // Given
        Double averageTime = 2.5;
        when(participationService.getAverageResponseTime(MEETING_ID))
                .thenReturn(averageTime);

        // When & Then
        mockMvc.perform(get("/api/v1/participations/meetings/{meetingId}/avg-response-time", MEETING_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Średni czas odpowiedzi pobrany pomyślnie")))
                .andExpect(jsonPath("$.data", is(2.5)));
    }

    @Test
    @WithMockUser(username = "test.user@example.com")
    void getAverageResponseTime_WhenNoResponses_ShouldReturnNull() throws Exception {
        // Given
        when(participationService.getAverageResponseTime(MEETING_ID))
                .thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/v1/participations/meetings/{meetingId}/avg-response-time", MEETING_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @WithMockUser(username = "test.user@example.com")
    void getResponseStatistics_WhenNoStatistics_ShouldReturnEmptyMap() throws Exception {
        // Given
        Map<ParticipationStatus, Long> emptyStats = new HashMap<>();
        when(participationService.getResponseStatistics(MEETING_ID))
                .thenReturn(emptyStats);

        // When & Then
        mockMvc.perform(get("/api/v1/participations/meetings/{meetingId}/response-stats", MEETING_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data").isEmpty());
    }

//    // Dodatkowe testy dla lepszego pokrycia
//    @Test
//    @WithMockUser(username = "test.user@example.com")
//    void confirmParticipation_ShouldReturnConfirmedParticipant() throws Exception {
//        // Given
//        mockParticipant.setStatus(ParticipationStatus.CONFIRMED);
//        when(participationService.confirmParticipation(eq(MEETING_ID), eq(testUser.getId())))
//                .thenReturn(mockParticipant);
//
//        // When & Then
//        mockMvc.perform(post("/api/v1/participations/meetings/{meetingId}/confirm", MEETING_ID)
//                        .with(csrf())
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success", is(true)))
//                .andExpect(jsonPath("$.message", is("Participation confirmed")));
//    }

//    @Test
//    @WithMockUser(username = "test.user@example.com")
//    void getParticipationStatus_ShouldReturnStatus() throws Exception {
//        // Given
//        when(participationService.getParticipationStatus(eq(MEETING_ID), eq(testUser.getId())))
//                .thenReturn(ParticipationStatus.CONFIRMED);
//
//        // When & Then
//        mockMvc.perform(get("/api/v1/participations/meetings/{meetingId}/status", MEETING_ID)
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success", is(true)))
//                .andExpect(jsonPath("$.data", is("CONFIRMED")));
//    }
}
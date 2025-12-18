package com.meethub.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.request.CreateMeetingRequest;
import com.meethub.domain.model.response.MeetingResponse;
import com.meethub.domain.service.MeetingService;
import com.meethub.security.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import security.WithCustomUser;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MeetingController.class)
class MeetingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MeetingService meetingService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private MeetingResponse mockMeeting;

    @BeforeEach
    void setUp() {
        mockMeeting = new MeetingResponse();
        mockMeeting.setId(1L);
        mockMeeting.setTitle("Test Meeting");
        mockMeeting.setDescription("Test Description");
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void getMeeting_ShouldReturnMeetingDetails() throws Exception {
        when(meetingService.getMeetingById(1L)).thenReturn(mockMeeting);

        // When & Then
        mockMvc.perform(get("/api/v1/meetings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Szczegóły spotkania pobrane pomyślnie"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Test Meeting"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void getMeeting_ShouldReturnBadRequest_WhenInvalidMeetingId() throws Exception {
        mockMvc.perform(get("/api/v1/meetings/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }


    @Test
    void getUserMeetings_ShouldReturnUnauthorized_WhenUserNotLoggedIn() throws Exception {
        // When & Then - brak autoryzacji
        mockMvc.perform(get("/api/v1/meetings/my-meetings")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isUnauthorized()); // 401 zamiast 400
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void findNearbyMeetings_ShouldReturnLocationBasedResults() throws Exception {
        // Given
        List<MeetingResponse> meetings = Arrays.asList(mockMeeting);
        when(meetingService.findNearbyMeetings(52.2297, 21.0122, 5000.0))
                .thenReturn(meetings);

        // When & Then
        mockMvc.perform(get("/api/v1/meetings/nearby")
                        .param("latitude", "52.2297")
                        .param("longitude", "21.0122")
                        .param("radius", "5000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Spotkania w pobliżu znalezione pomyślnie"))
                .andExpect(jsonPath("$.data[0].title").value("Test Meeting"));
    }

    @ParameterizedTest
    @MethodSource("provideInvalidLocationParameters")
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void findNearbyMeetings_ShouldValidateParameters(
            Double latitude, Double longitude, Double radius, int expectedStatus) throws Exception {
        // When & Then
        var request = get("/api/v1/meetings/nearby");

        if (latitude != null) {
            request.param("latitude", latitude.toString());
        }
        if (longitude != null) {
            request.param("longitude", longitude.toString());
        }
        if (radius != null) {
            request.param("radius", radius.toString());
        }

        mockMvc.perform(request)
                .andExpect(status().is(expectedStatus));
    }

    private static Stream<Arguments> provideInvalidLocationParameters() {
        return Stream.of(
                // latitude, longitude, radius, expectedStatus
                Arguments.of(91.0, 21.0122, 5000.0, 400), // Nieprawidłowa latitude
                Arguments.of(52.2297, 181.0, 5000.0, 400), // Nieprawidłowa longitude
                Arguments.of(52.2297, 21.0122, 50.0, 400), // Promień za mały
                Arguments.of(52.2297, 21.0122, 200000.0, 400) // Promień za duży
        );
    }

//    @Test
//    @WithMockUser(username = "test@example.com", roles = {"USER"})
//    void deleteMeeting_ShouldReturnSuccess_WhenAuthorized() throws Exception {
//        // When & Then
//        mockMvc.perform(delete("/api/v1/meetings/1"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.message").value("Spotkanie usunięte pomyślnie"));
//
//        verify(meetingService).deleteMeeting(1L, 1L);
//    }
//
//    @Test
//    void deleteMeeting_ShouldReturnUnauthorized_WhenUserNotLoggedIn() throws Exception {
//        // When & Then
//        mockMvc.perform(delete("/api/v1/meetings/1"))
//                .andExpect(status().isUnauthorized()); // 401 zamiast 400
//    }
//
//    @Test
//    @WithMockUser(username = "test@example.com", roles = {"USER"})
//    void updateMeeting_ShouldUpdateAndReturnMeeting() throws Exception {
//        // Given
//        UpdateMeetingRequest request = new UpdateMeetingRequest();
//        request.setTitle("Updated Title");
//        request.setDescription("Updated Description");
//
//        MeetingResponse updatedMeeting = new MeetingResponse();
//        updatedMeeting.setId(1L);
//        updatedMeeting.setTitle("Updated Title");
//
//        when(meetingService.updateMeeting(eq(1L), any(UpdateMeetingRequest.class), eq(1L)))
//                .thenReturn(updatedMeeting);
//
//        // When & Then
//        mockMvc.perform(put("/api/v1/meetings/1")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.message").value("Spotkanie zaktualizowane pomyślnie"))
//                .andExpect(jsonPath("$.data.title").value("Updated Title"));
//    }

    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void getUpcomingPublicMeetings_ShouldWorkWithAuthentication() throws Exception {
        // Given
        List<MeetingResponse> meetings = Arrays.asList(mockMeeting);
        when(meetingService.getUpcomingPublicMeetings()).thenReturn(meetings);

        // When & Then
        mockMvc.perform(get("/api/v1/meetings/public/upcoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Nadchodzące spotkania publiczne pobrane pomyślnie"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void searchMeetings_ShouldReturnFilteredResults() throws Exception {
        // Given
        Page<MeetingResponse> page = new PageImpl<>(
                Arrays.asList(mockMeeting),
                PageRequest.of(0, 10),
                1
        );

        when(meetingService.getFilteredMeetings(anyString(), anyString(), anyString(), any(PageRequest.class)))
                .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/v1/meetings/search")
                        .param("query", "test")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Wyniki wyszukiwania pobrane pomyślnie"));
    }
}
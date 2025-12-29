package com.meethub.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.model.enums.PermissionLevel;
import com.meethub.domain.model.projection.ParticipantProjection;
import com.meethub.domain.model.request.InviteParticipantsRequest;
import com.meethub.domain.model.response.ParticipantResponse;
import com.meethub.domain.model.response.UserResponse;
import com.meethub.domain.service.MeetingParticipantService;
import com.meethub.exception.BusinessException;
import com.meethub.exception.ResourceNotFoundException;
import com.meethub.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import security.WithUserId;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MeetingParticipantControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MeetingParticipantService participantService;

    @BeforeEach
    void setUp() {
        // Reset mocków przed każdym testem
        reset(participantService);
    }

    // ==============================
    // TESTY DLA ENDPOINTU /join (POPRAWIONE)
    // ==============================

    @Test
    @WithUserId(123)
    void joinMeeting_ShouldReturnSuccess_WhenUserIsAuthenticated() throws Exception {
        // Given
        Long meetingId = 456L;

        when(participantService.joinPublicMeeting(anyLong(), anyLong())).thenReturn(null);

        // When & Then
        mockMvc.perform(post("/api/v1/meetings/{meetingId}/participants/join", meetingId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Dołączono do spotkania pomyślnie"));

        verify(participantService, times(1)).joinPublicMeeting(eq(meetingId), eq(123L));
    }

    @Test
    @WithUserId(0)
    void joinMeeting_ShouldReturnBadRequest_WhenUserIdIsInvalid() throws Exception {
        Long meetingId = 456L;

        // When & Then - powinno być 400 z powodu walidacji @Min(1)
        mockMvc.perform(post("/api/v1/meetings/{meetingId}/participants/join", meetingId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(participantService, never()).joinPublicMeeting(anyLong(), anyLong());
    }


    @Test
    @WithUserId(123)
    void getParticipants_ShouldReturnEmptyList_WhenNoParticipants() throws Exception {
        // Given
        Long meetingId = 456L;

        when(participantService.getMeetingParticipants(meetingId))
                .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/v1/meetings/{meetingId}/participants", meetingId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));

        verify(participantService, times(1)).getMeetingParticipants(meetingId);
    }

    // ==============================
    // TESTY DLA ENDPOINTU /invite
    // ==============================

    @Test
    @WithUserId(123)
    void inviteParticipants_ShouldReturnSuccess_WithValidRequest() throws Exception {
        // Given
        Long meetingId = 456L;

        // Użyj konstruktora z 3 parametrami
        InviteParticipantsRequest request = InviteParticipantsRequest.builder()
                .userIds(Arrays.asList(100L, 101L, 102L))
                .permissionLevel(PermissionLevel.PARTICIPANT) // Domyślna wartość
                .message("Zaproszenie do spotkania")
                .build();


        // When & Then
        mockMvc.perform(post("/api/v1/meetings/{meetingId}/participants/invite", meetingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(participantService, times(1)).inviteMultipleParticipants(
                eq(meetingId), any(InviteParticipantsRequest.class), eq(123L));
    }



    @Test
    @WithUserId(123)
    void inviteParticipants_ShouldReturnBadRequest_WhenUserIdsIsNull() throws Exception {
        // Given
        Long meetingId = 456L;

        // Request z null listą - użyj Map zamiast klasy
        String requestBody = "{\"userIds\": null, \"message\": \"Test\"}";

        // When & Then
        mockMvc.perform(post("/api/v1/meetings/{meetingId}/participants/invite", meetingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(participantService, never()).inviteMultipleParticipants(
                anyLong(), any(InviteParticipantsRequest.class), anyLong());
    }


    // ==============================
    // TESTY Z WYJĄTKAMI (NOWE)
    // ==============================

    @Test
    @WithUserId(123)
    void getParticipants_ShouldReturnNotFound_WhenMeetingDoesNotExist() throws Exception {
        // Given
        Long meetingId = 999L;

        when(participantService.getMeetingParticipants(meetingId))
                .thenThrow(new ResourceNotFoundException("Meeting not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/meetings/{meetingId}/participants", meetingId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()); // 404 dzięki GlobalExceptionHandler

        verify(participantService, times(1)).getMeetingParticipants(meetingId);
    }

    @Test
    @WithUserId(123)
    void joinMeeting_ShouldReturnConflict_WhenUserAlreadyJoined() throws Exception {
        // Given
        Long meetingId = 456L;

        when(participantService.joinPublicMeeting(meetingId, 123L))
                .thenThrow(new BusinessException("User already joined this meeting"));

        // When & Then
        mockMvc.perform(post("/api/v1/meetings/{meetingId}/participants/join", meetingId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict()); // 409 dzięki GlobalExceptionHandler

        verify(participantService, times(1)).joinPublicMeeting(meetingId, 123L);
    }



    // ==============================
    // TESTY BEZPIECZEŃSTWA
    // ==============================

    @Test
    @WithUserId(123)
    void getParticipants_ShouldReturn404_WhenMeetingNotFound() throws Exception {
        // Given
        Long meetingId = 456L;

        when(participantService.getMeetingParticipants(meetingId))
                .thenThrow(new ResourceNotFoundException("Meeting not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/meetings/{meetingId}/participants", meetingId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserId(123)
    void getParticipants_ShouldReturn400_WhenValidationException() throws Exception {
        // Given
        Long meetingId = 456L;

        when(participantService.getMeetingParticipants(meetingId))
                .thenThrow(new ValidationException("Invalid data"));

        // When & Then
        mockMvc.perform(get("/api/v1/meetings/{meetingId}/participants", meetingId))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserId(123)
    void getParticipants_ShouldReturn409_WhenBusinessException() throws Exception {
        // Given
        Long meetingId = 456L;

        when(participantService.getMeetingParticipants(meetingId))
                .thenThrow(new BusinessException("Business rule violation"));

        // When & Then
        mockMvc.perform(get("/api/v1/meetings/{meetingId}/participants", meetingId))
                .andExpect(status().isConflict());
    }

    // ==============================
    // TESTY WALIDACJI PARAMETRÓW PATH
    // ==============================

    @Test
    @WithUserId(123)
    void shouldReturnBadRequest_ForInvalidMeetingIdInPath() throws Exception {
        // Testuj nieprawidłowe wartości w ścieżce

        // meetingId = 0
        mockMvc.perform(get("/api/v1/meetings/{meetingId}/participants", 0)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // meetingId = -1
        mockMvc.perform(get("/api/v1/meetings/{meetingId}/participants", -1)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

}
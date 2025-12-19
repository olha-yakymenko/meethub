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
        // Given - userId = 0 (mniejsze niż @Min(1))
        Long meetingId = 456L;

        // When & Then - powinno być 400 z powodu walidacji @Min(1)
        mockMvc.perform(post("/api/v1/meetings/{meetingId}/participants/join", meetingId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(participantService, never()).joinPublicMeeting(anyLong(), anyLong());
    }

    // ==============================
    // TESTY DLA ENDPOINTU GET / (POPRAWIONE)
    // ==============================

//    @Test
//    @WithUserId(123)
//    void getParticipants_ShouldReturnSuccess_WithValidMeetingId() throws Exception {
//        // Given
//        Long meetingId = 456L;
//
//        // Używamy prawdziwego obiektu zamiast mocka
//        ParticipantProjection participant1 = new ParticipantProjection() {
//            @Override public Long getId() { return 1L; }
//            @Override public ParticipationStatus getStatus() { return ParticipationStatus.CONFIRMED; }
//        };
//
//        ParticipantProjection participant2 = new ParticipantProjection() {
//            @Override public Long getId() { return 2L; }
//            @Override public ParticipationStatus getStatus() { return ParticipationStatus.PENDING; }
//        };
//
//        List<ParticipantProjection> participants = Arrays.asList(participant1, participant2);
//
//        when(participantService.getMeetingParticipants(meetingId)).thenReturn(participants);
//
//        // When & Then - sprawdzamy tylko status i strukturę
//        mockMvc.perform(get("/api/v1/meetings/{meetingId}/participants", meetingId)
//                        .contentType(MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.data").isArray());
//
//        verify(participantService, times(1)).getMeetingParticipants(meetingId);
//    }

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
    // TESTY DLA ENDPOINTU /respond (POPRAWIONE)
    // ==============================

    @Test
    @WithUserId(123)
    void respondToInvitation_ShouldReturnSuccess_WithValidParameters() throws Exception {
        // Given
        Long participantId = 456L;
        ParticipationStatus response = ParticipationStatus.CONFIRMED;
        String comment = "Chętnie wezmę udział";

        doNothing().when(participantService).respondToInvitation(
                anyLong(), any(ParticipationStatus.class), anyString(), anyLong());

        // When & Then
        mockMvc.perform(post("/api/v1/meetings/{meetingId}/participants/invitations/{participantId}/respond",
                        1L, participantId)
                        .param("response", response.name())
                        .param("comment", comment)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(participantService, times(1)).respondToInvitation(
                eq(participantId), eq(response), eq(comment), eq(123L));
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

    @Test
    @WithUserId(123)
    void updateParticipantStatus_ShouldReturnBadRequest_WhenStatusIsInvalid() throws Exception {
        // Given
        Long meetingId = 456L;
        Long participantId = 789L;
        String invalidStatus = "INVALID_STATUS";

        // When & Then - Spring nie może przekonwertować stringa na enum
        mockMvc.perform(patch("/api/v1/meetings/{meetingId}/participants/{participantId}/status",
                        meetingId, participantId)
                        .param("status", invalidStatus)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(participantService, never()).updateParticipantStatus(
                anyLong(), anyLong(), any(), anyString(), anyLong());
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

    // Test dla niepoprawnego statusu enum
    @Test
    @WithUserId(123)
    void updateParticipantStatus_ShouldReturn400_WhenStatusIsInvalidEnum() throws Exception {
        // Given
        Long meetingId = 456L;
        Long participantId = 789L;
        String invalidStatus = "INVALID_STATUS"; // Nie istnieje w ParticipationStatus

        // When & Then - Spring nie może przekonwertować na enum
        // To powoduje MethodArgumentTypeMismatchException, które powinno dać 400
        mockMvc.perform(patch("/api/v1/meetings/{meetingId}/participants/{participantId}/status",
                        meetingId, participantId)
                        .param("status", invalidStatus)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(participantService, never()).updateParticipantStatus(
                anyLong(), anyLong(), any(), anyString(), anyLong());
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
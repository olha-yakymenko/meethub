package com.meethub.controller.api;

import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.model.enums.PermissionLevel;
import com.meethub.domain.model.projection.ParticipantProjection;
import com.meethub.domain.model.request.InviteParticipantsRequest;
import com.meethub.domain.model.response.ApiResponse;
import com.meethub.domain.model.response.ParticipantResponse;
import com.meethub.domain.model.response.UserResponse;
import com.meethub.domain.service.MeetingParticipantService;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingParticipantControllerTest {

    @Mock
    private MeetingParticipantService participantService;

    @InjectMocks
    private MeetingParticipantController controller;

    private Long meetingId = 1L;
    private Long participantId = 1L;
    private Long userId = 1L;

    @Test
    void testGetParticipants_Success() {
        List<ParticipantProjection> participants = Collections.emptyList();
        when(participantService.getMeetingParticipants(meetingId)).thenReturn(participants);

        ResponseEntity<ApiResponse<List<ParticipantProjection>>> response =
                controller.getParticipants(meetingId);

        assertAll(
                () -> assertEquals(200, response.getStatusCodeValue()),
                () -> assertTrue(response.getBody().isSuccess())
        );
    }

    @Test
    void testInviteParticipants_Success() {
        InviteParticipantsRequest request = new InviteParticipantsRequest();

        ResponseEntity<ApiResponse<Void>> response =
                controller.inviteParticipants(meetingId, request, userId);

        assertAll(
                () -> assertEquals(200, response.getStatusCodeValue()),
                () -> assertTrue(response.getBody().isSuccess())
        );
    }



    @Test
    void testJoinMeeting_Success() {
        ResponseEntity<ApiResponse<Void>> response =
                controller.joinMeeting(meetingId, userId);

        assertAll(
                () -> assertEquals(200, response.getStatusCodeValue()),
                () -> assertTrue(response.getBody().isSuccess())
        );
    }


    @Test
    void testUpdateParticipantStatus_Success() {
        ResponseEntity<ApiResponse<Void>> response =
                controller.updateParticipantStatus(meetingId, participantId,
                        ParticipationStatus.CONFIRMED, "comment", userId);

        assertAll(
                () -> assertEquals(200, response.getStatusCodeValue()),
                () -> assertTrue(response.getBody().isSuccess())
        );
    }


    @Test
    void testUpdateParticipantPermission_Success() {
        ResponseEntity<ApiResponse<Void>> response =
                controller.updateParticipantPermission(meetingId, participantId,
                        PermissionLevel.CONTRIBUTOR, userId);

        assertAll(
                () -> assertEquals(200, response.getStatusCodeValue()),
                () -> assertTrue(response.getBody().isSuccess())
        );
    }

    @Test
    void testRemoveParticipant_Success() {
        ResponseEntity<ApiResponse<Void>> response =
                controller.removeParticipant(meetingId, participantId, userId);

        assertAll(
                () -> assertEquals(200, response.getStatusCodeValue()),
                () -> assertTrue(response.getBody().isSuccess())
        );
    }

    @Test
    void testAcceptInvitationByToken_Success() {
        String token = "valid_token_12345678901234567890123456789012";

        ResponseEntity<ApiResponse<Void>> response =
                controller.acceptInvitationByToken(token);

        assertAll(
                () -> assertEquals(200, response.getStatusCodeValue()),
                () -> assertTrue(response.getBody().isSuccess())
        );
    }


    @Test
    void testSearchUsers_Success() {
        List<UserResponse> users = Collections.emptyList();
        when(participantService.searchUsersForInvitation("test", meetingId))
                .thenReturn(users);

        ResponseEntity<ApiResponse<List<UserResponse>>> response =
                controller.searchUsers("test", meetingId);

        assertAll(
                () -> assertEquals(200, response.getStatusCodeValue()),
                () -> assertTrue(response.getBody().isSuccess())
        );
    }


    @Test
    void testGetUserInvitations_Success() {
        List<ParticipantResponse> invitations = Collections.emptyList();
        when(participantService.getUserInvitations(userId)).thenReturn(invitations);

        ResponseEntity<ApiResponse<List<ParticipantResponse>>> response =
                controller.getUserInvitations(userId);

        assertAll(
                () -> assertEquals(200, response.getStatusCodeValue()),
                () -> assertTrue(response.getBody().isSuccess())
        );
    }


    @Test
    void testRespondToInvitation_Success() {
        ResponseEntity<ApiResponse<Void>> response =
                controller.respondToInvitation(participantId,
                        ParticipationStatus.CONFIRMED, "comment", userId);

        assertAll(
                () -> assertEquals(200, response.getStatusCodeValue()),
                () -> assertTrue(response.getBody().isSuccess())
        );
    }

    @Test
    void testSearchUsers_WithResults() {
        List<UserResponse> users = List.of(
                new UserResponse(1L, "test1@example.com", "John", "Doe", null),
                new UserResponse(2L, "test2@example.com", "Jane", "Smith", null)
        );
        when(participantService.searchUsersForInvitation("john", meetingId))
                .thenReturn(users);

        ResponseEntity<ApiResponse<List<UserResponse>>> response =
                controller.searchUsers("john", meetingId);

        assertAll(
                () -> assertEquals(200, response.getStatusCodeValue()),
                () -> assertEquals(2, response.getBody().getData().size())
        );
    }

    @Test
    void testGetUserInvitations_WithData() {
        ParticipantResponse invitation1 = new ParticipantResponse();
        ParticipantResponse invitation2 = new ParticipantResponse();
        List<ParticipantResponse> invitations = List.of(invitation1, invitation2);

        when(participantService.getUserInvitations(userId)).thenReturn(invitations);

        ResponseEntity<ApiResponse<List<ParticipantResponse>>> response =
                controller.getUserInvitations(userId);

        assertAll(
                () -> assertEquals(200, response.getStatusCodeValue()),
                () -> assertEquals(2, response.getBody().getData().size())
        );
    }

    @Test
    void testGetParticipants_WithData() {
        ParticipantProjection participant1 = mock(ParticipantProjection.class);
        ParticipantProjection participant2 = mock(ParticipantProjection.class);
        List<ParticipantProjection> participants = List.of(participant1, participant2);

        when(participantService.getMeetingParticipants(meetingId)).thenReturn(participants);

        ResponseEntity<ApiResponse<List<ParticipantProjection>>> response =
                controller.getParticipants(meetingId);

        assertAll(
                () -> assertEquals(200, response.getStatusCodeValue()),
                () -> assertEquals(2, response.getBody().getData().size())
        );
    }

}
package com.meethub.controller.web;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.model.enums.PermissionLevel;
import com.meethub.domain.model.projection.ParticipantProjection;
import com.meethub.domain.model.request.InviteParticipantsRequest;
import com.meethub.domain.model.request.UpdateParticipantRequest;
import com.meethub.domain.model.response.ParticipantResponse;
import com.meethub.domain.service.MeetingParticipantService;
import com.meethub.security.CustomUserDetailsService.CustomUserDetails;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParticipantControllerTest {

    @Mock
    private MeetingParticipantService participantService;

    @Mock
    private Model model;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private ParticipantController controller;

    private CustomUserDetails userDetails;
    private Long meetingId = 1L;
    private Long participantId = 1L;
    private Long userId = 1L;

    @BeforeEach
    void setUp() {
        userDetails = mock(CustomUserDetails.class);
        lenient().when(userDetails.getId()).thenReturn(userId);
        lenient().when(userDetails.getUsername()).thenReturn("test@example.com");

        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }


    @Test
    void testGetParticipants_NoAccess() {
        when(participantService.hasAccessToMeeting(meetingId, userId)).thenReturn(false);

        String viewName = controller.getParticipants(meetingId, userDetails, model);

        assertEquals("error/403", viewName);
    }

    @Test
    void testShowInviteForm_Success() {
        when(participantService.isOrganizer(meetingId, userId)).thenReturn(true);

        String viewName = controller.showInviteForm(meetingId, userDetails, model);

        assertEquals("meetings/participants/invite", viewName);
    }

    @Test
    void testShowInviteForm_NotOrganizer() {
        when(participantService.isOrganizer(meetingId, userId)).thenReturn(false);

        String redirect = controller.showInviteForm(meetingId, userDetails, model);

        assertEquals("redirect:/meetings/1/participants", redirect);
    }

    @Test
    void testInviteParticipants_Success() {
        InviteParticipantsRequest request = new InviteParticipantsRequest();
        when(bindingResult.hasErrors()).thenReturn(false);
        when(participantService.isOrganizer(meetingId, userId)).thenReturn(true);

        List<ParticipantResponse> invited = List.of(new ParticipantResponse());
        when(participantService.inviteParticipants(meetingId, request)).thenReturn(invited);

        String redirect = controller.inviteParticipants(meetingId, request, bindingResult, userDetails, redirectAttributes, model);

        assertEquals("redirect:/meetings/1/participants", redirect);
    }

    @Test
    void testInviteParticipants_ValidationErrors() {
        InviteParticipantsRequest request = new InviteParticipantsRequest();
        when(bindingResult.hasErrors()).thenReturn(true);

        String redirect = controller.inviteParticipants(meetingId, request, bindingResult, userDetails, redirectAttributes, model);

        assertEquals("redirect:/meetings/1/participants/invite", redirect);
    }


    @Test
    void testShowEditForm_Success() {
        when(participantService.canEditParticipant(meetingId, participantId, userId)).thenReturn(true);

        ParticipantResponse participant = new ParticipantResponse();
        when(participantService.getParticipant(participantId)).thenReturn(participant);

        String viewName = controller.showEditForm(meetingId, participantId, userDetails, model);

        assertEquals("meetings/participants/edit", viewName);
    }

    @Test
    void testShowEditForm_NoPermission() {
        when(participantService.canEditParticipant(meetingId, participantId, userId)).thenReturn(false);

        String redirect = controller.showEditForm(meetingId, participantId, userDetails, model);

        assertEquals("redirect:/meetings/1/participants", redirect);
    }

    @Test
    void testUpdateParticipant_Success() {
        UpdateParticipantRequest request = new UpdateParticipantRequest();
        when(bindingResult.hasErrors()).thenReturn(false);
        when(participantService.canEditParticipant(meetingId, participantId, userId)).thenReturn(true);

        String redirect = controller.updateParticipant(meetingId, participantId, request, bindingResult, userDetails, redirectAttributes);

        assertEquals("redirect:/meetings/1/participants", redirect);
    }

    @Test
    void testUpdateParticipant_ValidationErrors() {
        UpdateParticipantRequest request = new UpdateParticipantRequest();
        when(bindingResult.hasErrors()).thenReturn(true);

        String redirect = controller.updateParticipant(meetingId, participantId, request, bindingResult, userDetails, redirectAttributes);

        assertEquals("redirect:/meetings/1/participants", redirect);
    }

    @Test
    void testRemoveParticipant_Success() {
        when(participantService.canRemoveParticipant(meetingId, participantId, userId)).thenReturn(true);

        assertDoesNotThrow(() -> {
            String redirect = controller.removeParticipant(meetingId, participantId, userDetails, redirectAttributes);
            assertEquals("redirect:/meetings/1/participants", redirect);
        });
    }

    @Test
    void testRemoveParticipant_NoPermission() {
        when(participantService.canRemoveParticipant(meetingId, participantId, userId)).thenReturn(false);

        assertDoesNotThrow(() -> {
            String redirect = controller.removeParticipant(meetingId, participantId, userDetails, redirectAttributes);
            assertEquals("redirect:/meetings/1/participants", redirect);
        });
    }

    @Test
    void testConfirmParticipation_Success() {
        String token = "valid_token_12345678901234567890123456789012";
        ParticipantResponse participant = new ParticipantResponse();
        when(participantService.confirmParticipation(token, null)).thenReturn(participant);

        String viewName = controller.confirmParticipation(token, null, model);

        assertEquals("participants/confirmation-success", viewName);
    }

    @Test
    void testConfirmParticipation_Exception() {
        String token = "invalid_token";
        when(participantService.confirmParticipation(token, null))
                .thenThrow(new RuntimeException("Test exception"));

        String viewName = controller.confirmParticipation(token, null, model);

        assertEquals("meetings/participants/confirmation-error", viewName);
    }

    @Test
    void testDeclineParticipation_Success() {
        String token = "valid_token_12345678901234567890123456789012";
        ParticipantResponse participant = new ParticipantResponse();
        when(participantService.declineParticipation(token, null)).thenReturn(participant);

        String viewName = controller.declineParticipation(token, null, model);

        assertEquals("meetings/participants/confirmation-success", viewName);
    }

    @Test
    void testJoinMeeting_Success() {
        assertDoesNotThrow(() -> {
            String redirect = controller.joinMeeting(meetingId, userDetails, redirectAttributes);
            assertEquals("redirect:/meetings/1", redirect);
        });
    }

    @Test
    void testJoinMeeting_ValidationException() {
        assertDoesNotThrow(() -> {
            String redirect = controller.joinMeeting(null, userDetails, redirectAttributes);
            assertEquals("redirect:/meetings/null", redirect);
        });
    }

    @Test
    void testApproveJoinRequest_Success() {
        assertDoesNotThrow(() -> {
            String redirect = controller.approveJoinRequest(meetingId, participantId, userDetails, redirectAttributes);
            assertEquals("redirect:/meetings/1/participants", redirect);
        });
    }

    @Test
    void testRejectJoinRequest_Success() {
        assertDoesNotThrow(() -> {
            String redirect = controller.rejectJoinRequest(meetingId, participantId, userDetails, redirectAttributes);
            assertEquals("redirect:/meetings/1/participants", redirect);
        });
    }

    @Test
    void testLeaveMeeting_Success() {
        assertDoesNotThrow(() -> {
            String redirect = controller.leaveMeeting(meetingId, userDetails, redirectAttributes);
            assertEquals("redirect:/meetings/1", redirect);
        });
    }

    @Test
    void testExportParticipants_Success() {
        when(participantService.isOrganizer(meetingId, userId)).thenReturn(true);

        ByteArrayResource resource = new ByteArrayResource("test".getBytes());
        when(participantService.exportParticipantsToCsv(meetingId)).thenReturn(resource);

        ResponseEntity<?> response = controller.exportParticipants(meetingId, userDetails);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testExportParticipants_NotOrganizer() {
        when(participantService.isOrganizer(meetingId, userId)).thenReturn(false);

        ResponseEntity<?> response = controller.exportParticipants(meetingId, userDetails);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void testExportParticipants_Exception() {
        when(participantService.isOrganizer(meetingId, userId)).thenReturn(true);
        when(participantService.exportParticipantsToCsv(meetingId))
                .thenThrow(new RuntimeException("Test exception"));

        ResponseEntity<?> response = controller.exportParticipants(meetingId, userDetails);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }


    @Test
    void testShowStats_NotOrganizer() {
        when(participantService.isOrganizer(meetingId, userId)).thenReturn(false);

        String viewName = controller.showStats(meetingId, userDetails, model);

        assertEquals("error/403", viewName);
    }

    @Test
    void testShowStats_Exception() {
        when(participantService.isOrganizer(meetingId, userId)).thenReturn(true);
        when(participantService.getDetailedStats(meetingId))
                .thenThrow(new RuntimeException("Test exception"));

        String viewName = controller.showStats(meetingId, userDetails, model);

        assertEquals("meetings/participants/stats", viewName);
    }

    @Test
    void testGetParticipants_Exception() {
        when(participantService.hasAccessToMeeting(meetingId, userId)).thenReturn(true);
        when(participantService.getMeetingParticipants(meetingId))
                .thenThrow(new RuntimeException("Test exception"));

        String viewName = controller.getParticipants(meetingId, userDetails, model);

        assertEquals("meetings/participants/list", viewName);
    }

    @Test
    void testShowInviteForm_Exception() {
        when(participantService.isOrganizer(meetingId, userId))
                .thenThrow(new RuntimeException("Test exception"));

        String redirect = controller.showInviteForm(meetingId, userDetails, model);

        assertEquals("redirect:/meetings/1/participants?error=Nie można załadować formularza", redirect);
    }

    @Test
    void testInviteParticipants_Exception() {
        InviteParticipantsRequest request = new InviteParticipantsRequest();
        when(bindingResult.hasErrors()).thenReturn(false);
        when(participantService.isOrganizer(meetingId, userId)).thenReturn(true);
        when(participantService.inviteParticipants(meetingId, request))
                .thenThrow(new RuntimeException("Test exception"));

        String redirect = controller.inviteParticipants(meetingId, request, bindingResult, userDetails, redirectAttributes, model);

        assertEquals("redirect:/meetings/1/participants/invite", redirect);
    }

    @Test
    void testShowEditForm_Exception() {
        when(participantService.canEditParticipant(meetingId, participantId, userId))
                .thenThrow(new RuntimeException("Test exception"));

        String redirect = controller.showEditForm(meetingId, participantId, userDetails, model);

        assertEquals("redirect:/meetings/1/participants?error=Nie można załadować formularza", redirect);
    }

    @Test
    void testUpdateParticipant_Exception() {
        UpdateParticipantRequest request = new UpdateParticipantRequest();
        when(bindingResult.hasErrors()).thenReturn(false);
        when(participantService.canEditParticipant(meetingId, participantId, userId)).thenReturn(true);
        doThrow(new RuntimeException("Test exception"))
                .when(participantService).updateParticipant(participantId, request);

        assertDoesNotThrow(() -> {
            String redirect = controller.updateParticipant(meetingId, participantId, request, bindingResult, userDetails, redirectAttributes);
            assertEquals("redirect:/meetings/1/participants", redirect);
        });
    }

    @Test
    void testRemoveParticipant_Exception() {
        when(participantService.canRemoveParticipant(meetingId, participantId, userId)).thenReturn(true);
        doThrow(new RuntimeException("Test exception"))
                .when(participantService).removeParticipant(participantId);

        assertDoesNotThrow(() -> {
            String redirect = controller.removeParticipant(meetingId, participantId, userDetails, redirectAttributes);
            assertEquals("redirect:/meetings/1/participants", redirect);
        });
    }

    @Test
    void testConfirmParticipation_ValidationException() {
        String token = "short";

        String viewName = controller.confirmParticipation(token, null, model);

        assertEquals("meetings/participants/confirmation-error", viewName);
    }

    @Test
    void testJoinMeeting_Exception() {
        doThrow(new RuntimeException("Test exception"))
                .when(participantService).joinMeeting(meetingId, userId);

        String redirect = controller.joinMeeting(meetingId, userDetails, redirectAttributes);

        assertEquals("redirect:/meetings/1", redirect);
    }

    @Test
    void testApproveJoinRequest_Exception() {
        doThrow(new RuntimeException("Test exception"))
                .when(participantService).approveJoinRequest(meetingId, participantId, userId);

        String redirect = controller.approveJoinRequest(meetingId, participantId, userDetails, redirectAttributes);

        assertEquals("redirect:/meetings/1/participants", redirect);
    }

    @Test
    void testRejectJoinRequest_Exception() {
        doThrow(new RuntimeException("Test exception"))
                .when(participantService).rejectJoinRequest(meetingId, participantId, userId);

        String redirect = controller.rejectJoinRequest(meetingId, participantId, userDetails, redirectAttributes);

        assertEquals("redirect:/meetings/1/participants", redirect);
    }

    @Test
    void testLeaveMeeting_Exception() {
        doThrow(new RuntimeException("Test exception"))
                .when(participantService).leaveMeeting(userId, meetingId);

        String redirect = controller.leaveMeeting(meetingId, userDetails, redirectAttributes);

        assertEquals("redirect:/meetings/1", redirect);
    }

    @Test
    void testTokenValidation() {
        String invalidToken = "too_short";

        String viewName = controller.confirmParticipation(invalidToken, "comment", model);

        assertEquals("meetings/participants/confirmation-error", viewName);
    }

    @Test
    void testExportParticipants_ValidationException() {
        ResponseEntity<?> response = controller.exportParticipants(null, userDetails);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

}
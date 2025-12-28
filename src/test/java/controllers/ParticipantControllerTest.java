package com.meethub.controller.web;

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
import java.util.HashMap;
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
        assertEquals("error/403", controller.getParticipants(meetingId, userDetails, model));
    }

    @Test
    void testShowInviteForm_Success() {
        when(participantService.isOrganizer(meetingId, userId)).thenReturn(true);
        assertEquals("meetings/participants/invite", controller.showInviteForm(meetingId, userDetails, model));
    }

    @Test
    void testShowInviteForm_NotOrganizer() {
        when(participantService.isOrganizer(meetingId, userId)).thenReturn(false);
        assertEquals("redirect:/meetings/1/participants", controller.showInviteForm(meetingId, userDetails, model));
    }

    @Test
    void testInviteParticipants_Success() {
        InviteParticipantsRequest request = new InviteParticipantsRequest();
        when(bindingResult.hasErrors()).thenReturn(false);
        when(participantService.isOrganizer(meetingId, userId)).thenReturn(true);
        when(participantService.inviteParticipants(meetingId, request)).thenReturn(List.of(new ParticipantResponse()));
        assertEquals("redirect:/meetings/1/participants", controller.inviteParticipants(meetingId, request, bindingResult, userDetails, redirectAttributes));
    }

    @Test
    void testShowEditForm_Success() {
        when(participantService.canEditParticipant(meetingId, participantId, userId)).thenReturn(true);
        when(participantService.getParticipant(participantId)).thenReturn(new ParticipantResponse());
        assertEquals("meetings/participants/edit", controller.showEditForm(meetingId, participantId, userDetails, model));
    }

    @Test
    void testShowEditForm_NoPermission() {
        when(participantService.canEditParticipant(meetingId, participantId, userId)).thenReturn(false);
        assertEquals("redirect:/meetings/1/participants", controller.showEditForm(meetingId, participantId, userDetails, model));
    }

    @Test
    void testUpdateParticipant_Success() {
        UpdateParticipantRequest request = new UpdateParticipantRequest();
        when(bindingResult.hasErrors()).thenReturn(false);
        when(participantService.canEditParticipant(meetingId, participantId, userId)).thenReturn(true);
        assertEquals("redirect:/meetings/1/participants", controller.updateParticipant(meetingId, participantId, request, bindingResult, userDetails, redirectAttributes));
    }

    @Test
    void testRemoveParticipant_Success() {
        when(participantService.canRemoveParticipant(meetingId, participantId, userId)).thenReturn(true);
        assertEquals("redirect:/meetings/1/participants", controller.removeParticipant(meetingId, participantId, userDetails, redirectAttributes));
    }

    @Test
    void testRemoveParticipant_NoPermission() {
        when(participantService.canRemoveParticipant(meetingId, participantId, userId)).thenReturn(false);
        assertEquals("redirect:/meetings/1/participants", controller.removeParticipant(meetingId, participantId, userDetails, redirectAttributes));
    }

    @Test
    void testConfirmParticipation_Success() {
        String token = "valid_token_12345678901234567890123456789012";
        when(participantService.confirmParticipation(token, null)).thenReturn(new ParticipantResponse());
        assertEquals("participants/confirmation-success", controller.confirmParticipation(token, null, model));
    }

    @Test
    void testDeclineParticipation_Success() {
        String token = "valid_token_12345678901234567890123456789012";
        when(participantService.declineParticipation(token, null)).thenReturn(new ParticipantResponse());
        assertEquals("meetings/participants/confirmation-success", controller.declineParticipation(token, null, model));
    }

    @Test
    void testJoinMeeting_Success() {
        assertEquals("redirect:/meetings/1", controller.joinMeeting(meetingId, userDetails, redirectAttributes));
    }

    @Test
    void testJoinMeeting_ValidationException() {
        assertEquals("redirect:/meetings/null", controller.joinMeeting(null, userDetails, redirectAttributes));
    }

    @Test
    void testApproveJoinRequest_Success() {
        assertEquals("redirect:/meetings/1/participants", controller.approveJoinRequest(meetingId, participantId, userDetails, redirectAttributes));
    }

    @Test
    void testRejectJoinRequest_Success() {
        assertEquals("redirect:/meetings/1/participants", controller.rejectJoinRequest(meetingId, participantId, userDetails, redirectAttributes));
    }

    @Test
    void testLeaveMeeting_Success() {
        assertEquals("redirect:/meetings/1", controller.leaveMeeting(meetingId, userDetails, redirectAttributes));
    }

    @Test
    void testExportParticipants_Success() {
        when(participantService.isOrganizer(meetingId, userId)).thenReturn(true);
        when(participantService.exportParticipantsToCsv(meetingId)).thenReturn(new ByteArrayResource("test".getBytes()));
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
    void testGetParticipants_Success() {
        when(participantService.hasAccessToMeeting(meetingId, userId)).thenReturn(true);
        when(participantService.getMeetingParticipants(meetingId)).thenReturn(Collections.emptyList());
        when(participantService.getMeetingStats(meetingId)).thenReturn(mock(MeetingParticipantService.ParticipantStats.class));
        when(participantService.isOrganizer(meetingId, userId)).thenReturn(true);
        assertEquals("meetings/participants/list", controller.getParticipants(meetingId, userDetails, model));
    }

    @Test
    void testShowStats_Success() {
        when(participantService.isOrganizer(meetingId, userId)).thenReturn(true);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalParticipants", 10);
        stats.put("confirmedParticipants", 8);
        stats.put("pendingParticipants", 2);

        when(participantService.getDetailedStats(meetingId)).thenReturn(stats);

        String viewName = controller.showStats(meetingId, userDetails, model);

        assertEquals("meetings/participants/stats", viewName);
        verify(model).addAttribute("stats", stats);
        verify(model).addAttribute("meetingId", meetingId);
    }

    @Test
    void testShowStats_NotOrganizer() {
        when(participantService.isOrganizer(meetingId, userId)).thenReturn(false);

        String viewName = controller.showStats(meetingId, userDetails, model);

        assertEquals("error/403", viewName);
        verify(model).addAttribute("error", "Brak uprawnień");
    }


}

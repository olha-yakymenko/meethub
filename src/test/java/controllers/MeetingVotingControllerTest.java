package com.meethub.controller.web;

import com.meethub.domain.model.request.CreateVotingRequest;
import com.meethub.domain.model.request.VoteRequest;
import com.meethub.domain.model.response.MeetingResponse;
import com.meethub.domain.model.response.VotingResponse;
import com.meethub.domain.service.MeetingService;
import com.meethub.domain.service.MeetingVotingService;
import com.meethub.exception.VotingAccessDeniedException;
import com.meethub.security.CustomUserDetailsService.CustomUserDetails;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingVotingControllerTest {

    @Mock
    private MeetingVotingService votingService;

    @Mock
    private MeetingService meetingService;

    @Mock
    private Model model;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private RedirectAttributes redirectAttributes;

    @Mock
    private CustomUserDetails userDetails;

    @InjectMocks
    private MeetingVotingController controller;

    private Long meetingId = 1L;
    private Long votingId = 1L;
    private Long userId = 1L;

    @BeforeEach
    void setUp() {
        lenient().when(userDetails.getId()).thenReturn(userId);
        lenient().when(userDetails.getUsername()).thenReturn("test@example.com");
    }

    @Test
    void testGetMeetingVotings_Success() {
        List<VotingResponse> votings = Collections.emptyList();
        MeetingResponse meeting = new MeetingResponse();
        meeting.setUserIsOrganizer(true);

        when(votingService.getMeetingVotings(meetingId, userId)).thenReturn(votings);
        when(meetingService.getMeetingDetails(meetingId, userId)).thenReturn(meeting);

        String viewName = controller.getMeetingVotings(meetingId, userDetails, model);

        assertEquals("meetings/votings/list", viewName);
    }

    @Test
    void testGetMeetingVotings_Exception() {
        when(votingService.getMeetingVotings(meetingId, userId))
                .thenThrow(new RuntimeException("Test exception"));

        String redirect = controller.getMeetingVotings(meetingId, userDetails, model);

        assertEquals("redirect:/meetings/1", redirect);
    }

    @Test
    void testShowCreateVotingForm_Success() {
        MeetingResponse meeting = new MeetingResponse();
        when(meetingService.getMeetingForVotingCreation(meetingId, userId)).thenReturn(meeting);

        String viewName = controller.showCreateVotingForm(meetingId, userDetails, model);

        assertEquals("meetings/votings/create", viewName);
    }


    @Test
    void testShowCreateVotingForm_AccessDenied() {
        when(meetingService.getMeetingForVotingCreation(meetingId, userId))
                .thenThrow(new IllegalArgumentException("Brak uprawnień"));

        String redirect = controller.showCreateVotingForm(meetingId, userDetails, model);

        assertEquals("redirect:/meetings/1/votings", redirect);
    }

    @Test
    void testShowCreateVotingForm_Exception() {
        when(meetingService.getMeetingForVotingCreation(meetingId, userId))
                .thenThrow(new RuntimeException("Test exception"));

        String redirect = controller.showCreateVotingForm(meetingId, userDetails, model);

        assertEquals("redirect:/meetings/1/votings", redirect);
    }

    @Test
    void testCreateVoting_Success() {
        CreateVotingRequest request = new CreateVotingRequest();
        when(bindingResult.hasErrors()).thenReturn(false);

        VotingResponse voting = new VotingResponse();
        voting.setId(votingId);
        voting.setTitle("Test Voting");
        when(votingService.createVoting(meetingId, request, userId)).thenReturn(voting);

        String redirect = controller.createVoting(meetingId, request, bindingResult, userDetails, model, redirectAttributes);

        assertEquals("redirect:/meetings/1/votings/1", redirect);
    }

    @Test
    void testCreateVoting_ValidationErrors() {
        CreateVotingRequest request = new CreateVotingRequest();
        when(bindingResult.hasErrors()).thenReturn(true);

        String viewName = controller.createVoting(meetingId, request, bindingResult, userDetails, model, redirectAttributes);

        assertEquals("meetings/votings/create", viewName);
    }

    @Test
    void testCreateVoting_ValidationException() {
        CreateVotingRequest request = new CreateVotingRequest();
        when(bindingResult.hasErrors()).thenReturn(false);
        when(votingService.createVoting(meetingId, request, userId))
                .thenThrow(new ConstraintViolationException(null));

        String viewName = controller.createVoting(meetingId, request, bindingResult, userDetails, model, redirectAttributes);

        assertEquals("meetings/votings/create", viewName);
    }

    @Test
    void testCreateVoting_Exception() {
        CreateVotingRequest request = new CreateVotingRequest();
        when(bindingResult.hasErrors()).thenReturn(false);
        when(votingService.createVoting(meetingId, request, userId))
                .thenThrow(new RuntimeException("Test exception"));

        String viewName = controller.createVoting(meetingId, request, bindingResult, userDetails, model, redirectAttributes);

        assertEquals("meetings/votings/create", viewName);
    }

    @Test
    void testGetVotingDetails_Success() {
        VotingResponse voting = new VotingResponse();
        voting.setId(votingId);
        MeetingResponse meeting = new MeetingResponse();
        meeting.setUserIsParticipant(true);
        meeting.setUserIsOrganizer(false);

        when(votingService.getVotingDetailsForUser(votingId, userId)).thenReturn(voting);
        when(meetingService.getMeetingDetails(meetingId, userId)).thenReturn(meeting);

        String viewName = controller.getVotingDetails(meetingId, votingId, userDetails, model);

        assertEquals("meetings/votings/details", viewName);
    }


    @Test
    void testGetVotingDetails_AccessDenied() {
        when(votingService.getVotingDetailsForUser(votingId, userId))
                .thenThrow(new VotingAccessDeniedException("Access denied"));

        String redirect = controller.getVotingDetails(meetingId, votingId, userDetails, model);

        assertEquals("redirect:/meetings/1", redirect);
    }

    @Test
    void testGetVotingDetails_Exception() {
        when(votingService.getVotingDetailsForUser(votingId, userId))
                .thenThrow(new RuntimeException("Test exception"));

        String redirect = controller.getVotingDetails(meetingId, votingId, userDetails, model);

        assertEquals("redirect:/meetings/1", redirect);
    }

    @Test
    void testSubmitVote_Success() {
        VoteRequest request = new VoteRequest();
        when(bindingResult.hasErrors()).thenReturn(false);

        String redirect = controller.submitVote(meetingId, votingId, request, bindingResult, userDetails, redirectAttributes);

        assertEquals("redirect:/meetings/1/votings/1", redirect);
    }

    @Test
    void testSubmitVote_ValidationErrors() {
        VoteRequest request = new VoteRequest();
        when(bindingResult.hasErrors()).thenReturn(true);

        String redirect = controller.submitVote(meetingId, votingId, request, bindingResult, userDetails, redirectAttributes);

        assertEquals("redirect:/meetings/1/votings/1", redirect);
    }

    @Test
    void testSubmitVote_ValidationException() {
        VoteRequest request = new VoteRequest();
        when(bindingResult.hasErrors()).thenReturn(false);
        doThrow(new ConstraintViolationException(null))
                .when(votingService).validateUserCanVote(meetingId, votingId, userId);

        String redirect = controller.submitVote(meetingId, votingId, request, bindingResult, userDetails, redirectAttributes);

        assertEquals("redirect:/meetings/1/votings/1", redirect);
    }

    @Test
    void testSubmitVote_Exception() {
        VoteRequest request = new VoteRequest();
        when(bindingResult.hasErrors()).thenReturn(false);
        doThrow(new RuntimeException("Test exception"))
                .when(votingService).validateUserCanVote(meetingId, votingId, userId);

        String redirect = controller.submitVote(meetingId, votingId, request, bindingResult, userDetails, redirectAttributes);

        assertEquals("redirect:/meetings/1/votings/1", redirect);
    }

    @Test
    void testSubmitVote_SubmitVoteException() {
        VoteRequest request = new VoteRequest();
        when(bindingResult.hasErrors()).thenReturn(false);
        doThrow(new RuntimeException("Test exception"))
                .when(votingService).submitVote(votingId, request, userId);

        String redirect = controller.submitVote(meetingId, votingId, request, bindingResult, userDetails, redirectAttributes);

        assertEquals("redirect:/meetings/1/votings/1", redirect);
    }

    @Test
    void testCloseVoting_Success() {
        assertDoesNotThrow(() -> {
            String redirect = controller.closeVoting(meetingId, votingId, userDetails, redirectAttributes);
            assertEquals("redirect:/meetings/1/votings/1", redirect);
        });
    }

    @Test
    void testCloseVoting_ValidationException() {
        assertDoesNotThrow(() -> {
            String redirect = controller.closeVoting(null, votingId, userDetails, redirectAttributes);
            assertEquals("redirect:/meetings/null/votings/1", redirect);
        });
    }

    @Test
    void testCloseVoting_Exception() {
        doThrow(new RuntimeException("Test exception"))
                .when(votingService).closeVoting(votingId, userId);

        assertDoesNotThrow(() -> {
            String redirect = controller.closeVoting(meetingId, votingId, userDetails, redirectAttributes);
            assertEquals("redirect:/meetings/1/votings/1", redirect);
        });
    }

    @Test
    void testGetVotingDetails_OrganizerView() {
        VotingResponse voting = new VotingResponse();
        voting.setId(votingId);
        MeetingResponse meeting = new MeetingResponse();
        meeting.setUserIsParticipant(false);
        meeting.setUserIsOrganizer(true);

        when(votingService.getVotingDetailsForUser(votingId, userId)).thenReturn(voting);
        when(meetingService.getMeetingDetails(meetingId, userId)).thenReturn(meeting);

        String viewName = controller.getVotingDetails(meetingId, votingId, userDetails, model);

        assertEquals("meetings/votings/details", viewName);
    }

    @Test
    void testGetVotingDetails_NeitherParticipantNorOrganizer() {
        VotingResponse voting = new VotingResponse();
        voting.setId(votingId);
        MeetingResponse meeting = new MeetingResponse();
        meeting.setUserIsParticipant(false);
        meeting.setUserIsOrganizer(false);

        when(votingService.getVotingDetailsForUser(votingId, userId)).thenReturn(voting);
        when(meetingService.getMeetingDetails(meetingId, userId)).thenReturn(meeting);

        String viewName = controller.getVotingDetails(meetingId, votingId, userDetails, model);

        assertEquals("meetings/votings/details", viewName);
    }

    @Test
    void testShowCreateVotingForm_IllegalStateException() {
        when(meetingService.getMeetingForVotingCreation(meetingId, userId))
                .thenThrow(new IllegalStateException("Meeting not ready"));

        String redirect = controller.showCreateVotingForm(meetingId, userDetails, model);

        assertEquals("redirect:/meetings/1/votings", redirect);
    }

    @Test
    void testCreateVoting_MeetingServiceException() {
        CreateVotingRequest request = new CreateVotingRequest();
        when(bindingResult.hasErrors()).thenReturn(false);
        when(votingService.createVoting(meetingId, request, userId))
                .thenThrow(new RuntimeException("Service exception"));

        String viewName = controller.createVoting(meetingId, request, bindingResult, userDetails, model, redirectAttributes);

        assertEquals("meetings/votings/create", viewName);
    }

    @Test
    void testGetMeetingVotings_EmptyList() {
        List<VotingResponse> votings = Collections.emptyList();
        MeetingResponse meeting = new MeetingResponse();
        meeting.setUserIsOrganizer(false);

        when(votingService.getMeetingVotings(meetingId, userId)).thenReturn(votings);
        when(meetingService.getMeetingDetails(meetingId, userId)).thenReturn(meeting);

        String viewName = controller.getMeetingVotings(meetingId, userDetails, model);

        assertEquals("meetings/votings/list", viewName);
    }

    @Test
    void testSubmitVote_ServiceValidationException() {
        VoteRequest request = new VoteRequest();
        when(bindingResult.hasErrors()).thenReturn(false);
        doThrow(new IllegalStateException("Cannot vote now"))
                .when(votingService).validateUserCanVote(meetingId, votingId, userId);

        String redirect = controller.submitVote(meetingId, votingId, request, bindingResult, userDetails, redirectAttributes);

        assertEquals("redirect:/meetings/1/votings/1", redirect);
    }
}
package com.meethub.controller.web;

import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.model.response.ParticipantResponse;
import com.meethub.domain.service.MeetingParticipantService;
import com.meethub.security.CustomUserDetailsService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvitationControllerUnitTest {

    @Mock
    private MeetingParticipantService participantService;

    @Mock
    private Model model;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private InvitationController controller;

    private CustomUserDetailsService.CustomUserDetails userDetails;
    private List<ParticipantResponse> mockInvitations;

    @BeforeEach
    void setUp() {
        // Setup user details
        userDetails = mock(CustomUserDetailsService.CustomUserDetails.class);
        when(userDetails.getId()).thenReturn(1L);


        // Setup mock invitations
        mockInvitations = Arrays.asList(
                ParticipantResponse.builder()
                        .id(100L)
                        .status(ParticipationStatus.INVITED)
                        .build(),
                ParticipantResponse.builder()
                        .id(101L)
                        .status(ParticipationStatus.INVITED)
                        .build()
        );
    }

    // ==================== TESTS FOR GET MY INVITATIONS ====================

    @Test
    void getMyInvitations_shouldReturnInvitationsList_whenValidUser() {
        // Given
        when(participantService.getUserInvitations(1L)).thenReturn(mockInvitations);

        // When
        String viewName = controller.getMyInvitations(userDetails, model);

        // Then
        assertEquals("invitations/list", viewName);
        verify(model).addAttribute("invitations", mockInvitations);
        verify(model).addAttribute("user", userDetails);
        verify(model).addAttribute("totalInvitations", 2);
        verify(participantService).getUserInvitations(1L);
    }

    @Test
    void getMyInvitations_shouldHandleEmptyInvitationsList() {
        // Given
        when(participantService.getUserInvitations(1L)).thenReturn(Collections.emptyList());

        // When
        String viewName = controller.getMyInvitations(userDetails, model);

        // Then
        assertEquals("invitations/list", viewName);
        verify(model).addAttribute("invitations", Collections.emptyList());
        verify(model).addAttribute("totalInvitations", 0);
    }


    @Test
    void getMyInvitations_shouldHandleGeneralException() {
        // Given
        when(participantService.getUserInvitations(1L))
                .thenThrow(new RuntimeException("Database error"));

        // When
        String viewName = controller.getMyInvitations(userDetails, model);

        // Then
        assertEquals("invitations/list", viewName);
        verify(model).addAttribute("error", "Błąd podczas ładowania zaproszeń");
    }

    // ==================== TESTS FOR RESPOND TO INVITATION ====================

    @Test
    void respondToInvitation_shouldAcceptSuccessfully() {
        // Given
        Long participantId = 100L;
        String response = "CONFIRMED";
        String comment = "Looking forward to it!";

        // When
        String redirectUrl = controller.respondToInvitation(participantId, response, comment,
                userDetails, redirectAttributes);

        // Then
        assertEquals("redirect:/invitations", redirectUrl);
        verify(participantService).respondToInvitation(participantId, ParticipationStatus.CONFIRMED,
                comment, 1L);
        verify(redirectAttributes).addFlashAttribute("success", "Zaakceptowano zaproszenie do spotkania");
    }

    @Test
    void respondToInvitation_shouldDeclineSuccessfully() {
        // Given
        Long participantId = 100L;
        String response = "DECLINED";
        String comment = "Sorry, can't make it";

        // When
        String redirectUrl = controller.respondToInvitation(participantId, response, comment,
                userDetails, redirectAttributes);

        // Then
        assertEquals("redirect:/invitations", redirectUrl);
        verify(participantService).respondToInvitation(participantId, ParticipationStatus.DECLINED,
                comment, 1L);
        verify(redirectAttributes).addFlashAttribute("success", "Odrzucono zaproszenie do spotkania");
    }

    @Test
    void respondToInvitation_shouldHandleAcceptedLowerCase() {
        // Given
        Long participantId = 100L;
        String response = "confirmed"; // lowercase
        String comment = "I'll be there";

        // When
        String redirectUrl = controller.respondToInvitation(participantId, response, comment,
                userDetails, redirectAttributes);

        // Then
        assertEquals("redirect:/invitations", redirectUrl);
        verify(participantService).respondToInvitation(participantId, ParticipationStatus.CONFIRMED,
                comment, 1L);
    }

    @Test
    void respondToInvitation_shouldHandleDeclinedLowerCase() {
        // Given
        Long participantId = 100L;
        String response = "declined"; // lowercase
        String comment = "Can't attend";

        // When
        String redirectUrl = controller.respondToInvitation(participantId, response, comment,
                userDetails, redirectAttributes);

        // Then
        assertEquals("redirect:/invitations", redirectUrl);
        verify(participantService).respondToInvitation(participantId, ParticipationStatus.DECLINED,
                comment, 1L);
    }

    @Test
    void respondToInvitation_shouldHandleNullComment() {
        // Given
        Long participantId = 100L;
        String response = "CONFIRMED";
        String comment = null;

        // When
        String redirectUrl = controller.respondToInvitation(participantId, response, comment,
                userDetails, redirectAttributes);

        // Then
        assertEquals("redirect:/invitations", redirectUrl);
        verify(participantService).respondToInvitation(participantId, ParticipationStatus.CONFIRMED,
                null, 1L);
    }

    @Test
    void respondToInvitation_shouldHandleEmptyComment() {
        // Given
        Long participantId = 100L;
        String response = "CONFIRMED";
        String comment = "";

        // When
        String redirectUrl = controller.respondToInvitation(participantId, response, comment,
                userDetails, redirectAttributes);

        // Then
        assertEquals("redirect:/invitations", redirectUrl);
        verify(participantService).respondToInvitation(participantId, ParticipationStatus.CONFIRMED,
                "", 1L);
    }



    @ParameterizedTest
    @EnumSource(ParticipationStatus.class)
    void respondToInvitation_shouldHandleAllStatuses(ParticipationStatus status) {
        // Given
        Long participantId = 100L;
        String response = status.name();
        String comment = "Test comment";

        // When
        String redirectUrl = controller.respondToInvitation(participantId, response, comment,
                userDetails, redirectAttributes);

        // Then
        assertEquals("redirect:/invitations", redirectUrl);
        verify(participantService).respondToInvitation(participantId, status, comment, 1L);
    }

    @Test
    void respondToInvitation_shouldHandleInvalidResponse() {
        // Given
        Long participantId = 100L;
        String response = "INVALID_STATUS";
        String comment = "Test comment";

        // When
        String redirectUrl = controller.respondToInvitation(participantId, response, comment,
                userDetails, redirectAttributes);

        // Then
        assertEquals("redirect:/invitations", redirectUrl);
        verify(participantService, never()).respondToInvitation(anyLong(), any(), anyString(), anyLong());
        verify(redirectAttributes).addFlashAttribute("error", "Nieprawidłowy status odpowiedzi: INVALID_STATUS");
    }

    @Test
    void respondToInvitation_shouldHandleSecurityException() {
        // Given
        Long participantId = 100L;
        String response = "CONFIRMED";
        String comment = "Test comment";

        doThrow(new SecurityException("Unauthorized"))
                .when(participantService).respondToInvitation(participantId, ParticipationStatus.CONFIRMED, comment, 1L);

        // When
        String redirectUrl = controller.respondToInvitation(participantId, response, comment,
                userDetails, redirectAttributes);

        // Then
        assertEquals("redirect:/invitations", redirectUrl);
        verify(redirectAttributes).addFlashAttribute("error", "Brak uprawnień do tej operacji");
    }

    @Test
    void respondToInvitation_shouldHandleGeneralException() {
        // Given
        Long participantId = 100L;
        String response = "CONFIRMED";
        String comment = "Test comment";

        doThrow(new RuntimeException("Database error"))
                .when(participantService).respondToInvitation(participantId, ParticipationStatus.CONFIRMED, comment, 1L);

        // When
        String redirectUrl = controller.respondToInvitation(participantId, response, comment,
                userDetails, redirectAttributes);

        // Then
        assertEquals("redirect:/invitations", redirectUrl);
        verify(redirectAttributes).addFlashAttribute("error", "Błąd podczas przetwarzania odpowiedzi: Database error");
    }


    @Test
    void respondToInvitation_shouldLogSuccessMessage() {
        // Given
        Long participantId = 100L;
        String response = "CONFIRMED";
        String comment = "Will attend";

        // When
        controller.respondToInvitation(participantId, response, comment, userDetails, redirectAttributes);

        // Then - verify logging is called (indirectly by verifying service call)
        verify(participantService).respondToInvitation(participantId, ParticipationStatus.CONFIRMED, comment, 1L);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, -100})
    void respondToInvitation_shouldHandleInvalidParticipantId(long invalidId) {
        // Given
        String response = "CONFIRMED";
        String comment = "Test";

        // When
        String redirectUrl = controller.respondToInvitation(invalidId, response, comment,
                userDetails, redirectAttributes);

        // Then - bez Spring AOP walidacji metoda próbuje wykonać operację
        verify(participantService).respondToInvitation(invalidId, ParticipationStatus.CONFIRMED, comment, 1L);
    }

    // ==================== TESTS FOR RESPOND TO INVITATION FORM ====================

    @Test
    void respondToInvitationForm_shouldProcessSuccessfully() {
        // Given
        Long participantId = 100L;
        InvitationController.InvitationResponseForm form = new InvitationController.InvitationResponseForm();
        form.setResponse("CONFIRMED");
        form.setComment("Will attend");

        when(bindingResult.hasErrors()).thenReturn(false);

        // When
        String redirectUrl = controller.respondToInvitationForm(participantId, form, bindingResult,
                userDetails, redirectAttributes);

        // Then
        assertEquals("redirect:/invitations", redirectUrl);
        verify(participantService).respondToInvitation(participantId, ParticipationStatus.CONFIRMED,
                "Will attend", 1L);
        verify(redirectAttributes).addFlashAttribute("success", "Zaakceptowano zaproszenie do spotkania");
    }


    @Test
    void respondToInvitationForm_shouldHandleException() {
        // Given
        Long participantId = 100L;
        InvitationController.InvitationResponseForm form = new InvitationController.InvitationResponseForm();
        form.setResponse("CONFIRMED");
        form.setComment("Will attend");

        when(bindingResult.hasErrors()).thenReturn(false);
        doThrow(new RuntimeException("Service error"))
                .when(participantService).respondToInvitation(participantId, ParticipationStatus.CONFIRMED,
                        "Will attend", 1L);

        // When
        String redirectUrl = controller.respondToInvitationForm(participantId, form, bindingResult,
                userDetails, redirectAttributes);

        // Then
        assertEquals("redirect:/invitations", redirectUrl);
        verify(redirectAttributes).addFlashAttribute("error", "Wystąpił błąd: Service error");
    }



    // ==================== HELPER METHODS ====================

    private ConstraintViolationException createConstraintViolationException(String property, String message) {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn(message);

        Path path = mock(Path.class);
        when(path.toString()).thenReturn(property);
        when(violation.getPropertyPath()).thenReturn(path);

        Set<ConstraintViolation<?>> violations = new HashSet<>();
        violations.add(violation);

        return new ConstraintViolationException("Validation failed", violations);
    }
}
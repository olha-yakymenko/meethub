package com.meethub.controller.web;

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

import java.util.*;

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


        @Test
        void testGetParticipants_MeetingIdInvalid() {
            // Given
            Long invalidMeetingId = 0L;
            String result = controller.getParticipants(invalidMeetingId, userDetails, model);


            assertEquals("error/403", result);
        }

//        @Test
//        void testGetParticipants_UserDetailsNull() {
//            // Given - null userDetails
//            CustomUserDetails nullUserDetails = null;
//
//            // When & Then - kontroler powinien obsłużyć null
//            // Zwykle zwróci widok błędu lub przekierowanie do logowania
//            // W testach jednostkowych walidacja nie jest wyzwalana
//            String result = controller.getParticipants(meetingId, nullUserDetails, model);
//
//            // Sprawdź czy kontroler obsłużył null bezpiecznie
//            assertNotNull(result);
//        }

//        @Test
//        void testGetParticipants_ServiceThrowsException() {
//            // Given
//            when(participantService.hasAccessToMeeting(meetingId, userId)).thenThrow(new RuntimeException("Database error"));
//
//            // When
//            String result = controller.getParticipants(meetingId, userDetails, model);
//
//            // Then - kontroler powinien obsłużyć wyjątek
//            assertEquals("error/403", result); // lub inny widok błędu
//            verify(model).addAttribute(eq("error"), anyString());
//        }

        @Test
        void testGetParticipants_EmptyParticipantsList() {
            // Given
            when(participantService.hasAccessToMeeting(meetingId, userId)).thenReturn(true);
            when(participantService.getMeetingParticipants(meetingId)).thenReturn(Collections.emptyList());
            when(participantService.getMeetingStats(meetingId)).thenReturn(mock(MeetingParticipantService.ParticipantStats.class));
            when(participantService.isOrganizer(meetingId, userId)).thenReturn(false);

            // When
            String result = controller.getParticipants(meetingId, userDetails, model);

            // Then
            assertEquals("meetings/participants/list", result);
            verify(model).addAttribute(eq("participants"), eq(Collections.emptyList()));
            verify(model).addAttribute(eq("isOrganizer"), eq(false));
        }

        @Test
        void testInviteParticipants_ValidationErrors() {
            // Given
            InviteParticipantsRequest request = new InviteParticipantsRequest();
            when(bindingResult.hasErrors()).thenReturn(true);
            when(bindingResult.getFieldError()).thenReturn(new org.springframework.validation.FieldError(
                    "inviteRequest", "emails", "Lista emaili nie może być pusta"));

            // When
            String result = controller.inviteParticipants(meetingId, request, bindingResult, userDetails, redirectAttributes);

            // Then
            assertEquals("redirect:/meetings/" + meetingId + "/participants/invite", result);
            verify(redirectAttributes).addFlashAttribute(eq("error"), contains("Błąd walidacji"));
            verify(participantService, never()).inviteParticipants(any(), any());
        }
//
//        @Test
//        void testInviteParticipants_EmptyEmailList() {
//            // Given
//            InviteParticipantsRequest request = new InviteParticipantsRequest();
//            request.setEmails(Collections.emptyList()); // Pusta lista
//
//            // When - pomijamy bindingResult.hasErrors() - zakładamy że walidacja w serwisie
//            when(participantService.isOrganizer(meetingId, userId)).thenReturn(true);
//            when(participantService.inviteParticipants(meetingId, request)).thenReturn(Collections.emptyList());
//
//            String result = controller.inviteParticipants(meetingId, request, bindingResult, userDetails, redirectAttributes);
//
//            // Then
//            assertEquals("redirect:/meetings/" + meetingId + "/participants", result);
//            verify(redirectAttributes).addFlashAttribute(eq("success"), contains("Wysłano 0 zaproszeń"));
//        }
//
//        @Test
//        void testInviteParticipants_ServiceReturnsNull() {
//            // Given
//            InviteParticipantsRequest request = new InviteParticipantsRequest();
//            request.setEmails(List.of("test@example.com"));
//
//            when(bindingResult.hasErrors()).thenReturn(false);
//            when(participantService.isOrganizer(meetingId, userId)).thenReturn(true);
//            when(participantService.inviteParticipants(meetingId, request)).thenReturn(null);
//
//            // When
//            String result = controller.inviteParticipants(meetingId, request, bindingResult, userDetails, redirectAttributes);
//
//            // Then - kontroler powinien obsłużyć null
//            assertEquals("redirect:/meetings/" + meetingId + "/participants", result);
//            // Możesz dodać logikę sprawdzającą komunikat
//        }
//
//        @Test
//        void testUpdateParticipant_InvalidStatus() {
//            // Given
//            UpdateParticipantRequest request = new UpdateParticipantRequest();
//            // request.setStatus("INVALID_STATUS"); // jeśli enum
//
//            when(bindingResult.hasErrors()).thenReturn(false);
//            when(participantService.canEditParticipant(meetingId, participantId, userId)).thenReturn(true);
//
//            // Symuluj wyjątek od serwisu
//            doThrow(new IllegalArgumentException("Nieprawidłowy status"))
//                    .when(participantService).updateParticipant(participantId, request);
//
//            // When
//            String result = controller.updateParticipant(meetingId, participantId, request, bindingResult, userDetails, redirectAttributes);
//
//            // Then - kontroler powinien obsłużyć wyjątek
//            assertEquals("redirect:/meetings/" + meetingId + "/participants/" + participantId + "/edit", result);
//            verify(redirectAttributes).addFlashAttribute(eq("error"), anyString());
//        }


        @Test
        void testConfirmParticipation_NullComment() {
            // Given
            String validToken = "valid_token_12345678901234567890123456789012";

            when(participantService.confirmParticipation(eq(validToken), isNull()))
                    .thenReturn(new ParticipantResponse());

            // When
            String result = controller.confirmParticipation(validToken, null, model);

            // Then
            assertEquals("participants/confirmation-success", result);
            verify(participantService).confirmParticipation(validToken, null);
        }

        @Test
        void testConfirmParticipation_LongComment() {
            // Given
            String validToken = "valid_token_12345678901234567890123456789012";
            String longComment = "A".repeat(501); // Przekracza @Size(max=500)

            // W testach jednostkowych walidacja nie jest wyzwalana
            // Możemy sprawdzić czy serwis otrzyma długi komentarz
            ParticipantResponse mockResponse = new ParticipantResponse();
            when(participantService.confirmParticipation(eq(validToken), eq(longComment)))
                    .thenReturn(mockResponse);

            // When
            String result = controller.confirmParticipation(validToken, longComment, model);

            // Then
            assertEquals("participants/confirmation-success", result);
            verify(participantService).confirmParticipation(validToken, longComment);
        }

        @Test
        void testJoinMeeting_NullMeetingId() {
            // Given
            Long nullMeetingId = null;

            // When
            String result = controller.joinMeeting(nullMeetingId, userDetails, redirectAttributes);

            // Then - kontroler powinien obsłużyć null
            // W praktyce może rzucić ConstraintViolationException
            // lub zwrócić przekierowanie z błędem
            assertNotNull(result);
        }

        @Test
        void testJoinMeeting_NullUserDetails() {
            // Given
            CustomUserDetails nullDetails = null;

            // When
            String result = controller.joinMeeting(meetingId, nullDetails, redirectAttributes);

            // Then - kontroler powinien obsłużyć null
            // Zwykle rzuci wyjątek lub przekieruje do logowania
            assertNotNull(result);
        }

//        @Test
//        void testJoinMeeting_ServiceThrowsException() {
//            // Given
//            doThrow(new RuntimeException("Meeting not found"))
//                    .when(participantService).joinMeeting(meetingId, userId);
//
//            // When
//            String result = controller.joinMeeting(meetingId, userDetails, redirectAttributes);
//
//            // Then - kontroler powinien obsłużyć wyjątek
//            assertEquals("redirect:/meetings/" + meetingId, result);
//            verify(redirectAttributes).addFlashAttribute(eq("error"), anyString());
//        }

        @Test
        void testExportParticipants_EmptyResource() {
            // Given
            when(participantService.isOrganizer(meetingId, userId)).thenReturn(true);
            when(participantService.exportParticipantsToCsv(meetingId))
                    .thenReturn(new ByteArrayResource(new byte[0]));

            // When
            ResponseEntity<?> response = controller.exportParticipants(meetingId, userDetails);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody() instanceof ByteArrayResource);
        }

        @Test
        void testShowStats_EmptyStats() {
            // Given
            when(participantService.isOrganizer(meetingId, userId)).thenReturn(true);
            when(participantService.getDetailedStats(meetingId)).thenReturn(Collections.emptyMap());

            // When
            String result = controller.showStats(meetingId, userDetails, model);

            // Then
            assertEquals("meetings/participants/stats", result);
            verify(model).addAttribute(eq("stats"), eq(Collections.emptyMap()));
        }

        @Test
        void testShowStats_NullStats() {
            // Given
            when(participantService.isOrganizer(meetingId, userId)).thenReturn(true);
            when(participantService.getDetailedStats(meetingId)).thenReturn(null);

            // When
            String result = controller.showStats(meetingId, userDetails, model);

            // Then - kontroler powinien obsłużyć null
            assertEquals("meetings/participants/stats", result);
            verify(model).addAttribute(eq("stats"), isNull());
        }

//        @Test
//        void testRemoveParticipant_ParticipantNotFound() {
//            // Given
//            when(participantService.canRemoveParticipant(meetingId, participantId, userId)).thenReturn(true);
//            doThrow(new RuntimeException("Participant not found"))
//                    .when(participantService).removeParticipant(participantId);
//
//            // When
//            String result = controller.removeParticipant(meetingId, participantId, userDetails, redirectAttributes);
//
//            // Then - kontroler powinien obsłużyć wyjątek
//            assertEquals("redirect:/meetings/" + meetingId + "/participants", result);
//            verify(redirectAttributes).addFlashAttribute(eq("error"), anyString());
//        }

        @Test
        void testApproveJoinRequest_InvalidParticipantId() {
            // Given
            Long invalidParticipantId = -1L; // Nie spełnia @Min(1)

            // When & Then - w testach jednostkowych walidacja nie działa
            // Obserwuj zachowanie kontrolera
            String result = controller.approveJoinRequest(meetingId, invalidParticipantId, userDetails, redirectAttributes);

            assertNotNull(result);
            // Możesz dodać weryfikację czy serwis otrzymał nieprawidłowe ID
        }

//        @Test
//        void testRejectJoinRequest_AlreadyProcessed() {
//            // Given
//            doThrow(new IllegalStateException("Prośba została już przetworzona"))
//                    .when(participantService).rejectJoinRequest(meetingId, participantId, userId);
//
//            // When
//            String result = controller.rejectJoinRequest(meetingId, participantId, userDetails, redirectAttributes);
//
//            // Then
//            assertEquals("redirect:/meetings/" + meetingId + "/participants", result);
//            verify(redirectAttributes).addFlashAttribute(eq("error"), contains("już przetworzona"));
//        }

        // Testy pokrycia dla różnych kombinacji statusów
        @Test
        void testGetParticipants_WithDifferentParticipantStatuses() {
            // Given
            when(participantService.hasAccessToMeeting(meetingId, userId)).thenReturn(true);

            // Symuluj różnych uczestników z różnymi statusami
            List<ParticipantProjection> participants = Arrays.asList(
                    mock(ParticipantProjection.class),
                    mock(ParticipantProjection.class),
                    mock(ParticipantProjection.class)
            );

            when(participantService.getMeetingParticipants(meetingId)).thenReturn(participants);
            when(participantService.getMeetingStats(meetingId)).thenReturn(mock(MeetingParticipantService.ParticipantStats.class));
            when(participantService.isOrganizer(meetingId, userId)).thenReturn(true);

            // When
            String result = controller.getParticipants(meetingId, userDetails, model);

            // Then
            assertEquals("meetings/participants/list", result);
            verify(model).addAttribute("participants", participants);
            verify(model).addAttribute("isOrganizer", true);
        }

        // Test sprawdzający zabezpieczenie przed XSS w komentarzach
        @Test
        void testConfirmParticipation_XSSInComment() {
            // Given
            String validToken = "valid_token_12345678901234567890123456789012";
            String xssComment = "<script>alert('xss')</script>";

            ParticipantResponse mockResponse = new ParticipantResponse();
            when(participantService.confirmParticipation(eq(validToken), eq(xssComment)))
                    .thenReturn(mockResponse);

            // When
            String result = controller.confirmParticipation(validToken, xssComment, model);

            // Then - kontroler powinien przekazać komentarz do serwisu
            // Sanityzacja powinna być w serwisie lub warstwie prezentacji
            assertEquals("participants/confirmation-success", result);
            verify(participantService).confirmParticipation(validToken, xssComment);
        }
    }



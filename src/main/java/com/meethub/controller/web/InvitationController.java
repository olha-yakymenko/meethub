package com.meethub.controller.web;

import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.model.response.ParticipantResponse;
import com.meethub.domain.service.MeetingParticipantService;
import com.meethub.security.CustomUserDetailsService.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final MeetingParticipantService participantService;

    @GetMapping
    public String getMyInvitations(@AuthenticationPrincipal CustomUserDetails userDetails,
                                   Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            List<ParticipantResponse> invitations = participantService.getUserInvitations(userDetails.getId());

            model.addAttribute("invitations", invitations);
            model.addAttribute("user", userDetails);
            model.addAttribute("totalInvitations", invitations.size());

            return "invitations/list";

        } catch (Exception e) {
            log.error("Error loading invitations for user: {}", userDetails.getId(), e);
            model.addAttribute("error", "Błąd podczas ładowania zaproszeń");
            return "invitations/list";
        }
    }

    @PostMapping("/{participantId}/respond")
    public String respondToInvitation(@PathVariable Long participantId,
                                      @RequestParam String response,
                                      @RequestParam(required = false) String comment,
                                      @AuthenticationPrincipal CustomUserDetails userDetails,
                                      RedirectAttributes redirectAttributes) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            ParticipationStatus status = ParticipationStatus.valueOf(response.toUpperCase());
            participantService.respondToInvitation(participantId, status, comment, userDetails.getId());

            String message = getResponseMessage(status);
            redirectAttributes.addFlashAttribute("success", message);

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Nieprawidłowy status odpowiedzi");
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("error", "Brak uprawnień do tej operacji");
        } catch (Exception e) {
            log.error("Error responding to invitation: {}", participantId, e);
            redirectAttributes.addFlashAttribute("error", "Błąd podczas przetwarzania odpowiedzi: " + e.getMessage());
        }

        return "redirect:/invitations";
    }

    private String getResponseMessage(ParticipationStatus status) {
        switch (status) {
            case CONFIRMED:
                return "Zaakceptowano zaproszenie do spotkania";
            case DECLINED:
                return "Odrzucono zaproszenie do spotkania";
            default:
                return "Zaktualizowano status zaproszenia";
        }
    }
}
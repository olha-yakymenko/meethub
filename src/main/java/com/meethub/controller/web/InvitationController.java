package com.meethub.controller.web;

import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.model.response.ParticipantResponse;
import com.meethub.domain.service.MeetingParticipantService;
import com.meethub.security.CustomUserDetailsService.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Zaproszenia", description = "Zarządzanie zaproszeniami na spotkania")
public class InvitationController {

    private final MeetingParticipantService participantService;

    @GetMapping
    @Operation(summary = "Wyświetla moje zaproszenia",
            description = "Pobiera listę wszystkich zaproszeń dla zalogowanego użytkownika i wyświetla je na stronie.")
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
    @Operation(summary = "Odpowiada na zaproszenie",
            description = "Pozwala użytkownikowi zaakceptować lub odrzucić zaproszenie na spotkanie oraz opcjonalnie dodać komentarz.")
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

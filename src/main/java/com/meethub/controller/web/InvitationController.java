package com.meethub.controller.web;

import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.model.response.ParticipantResponse;
import com.meethub.domain.service.MeetingParticipantService;
import com.meethub.security.CustomUserDetailsService.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Validated
@Slf4j
@Controller
@RequestMapping("/invitations")
@RequiredArgsConstructor
@Tag(name = "Zaproszenia", description = "Strony web do zarządzania zaproszeniami na spotkania")
public class InvitationController {

    private final MeetingParticipantService participantService;

    @GetMapping
    @Operation(summary = "Wyświetla moje zaproszenia",
            description = "Pobiera listę wszystkich zaproszeń dla zalogowanego użytkownika i wyświetla je na stronie.")
    public String getMyInvitations(
            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetails userDetails,
            Model model) {

        try {
            log.info("Pobieranie zaproszeń dla użytkownika {}", userDetails.getId());
            List<ParticipantResponse> invitations = participantService.getUserInvitations(userDetails.getId());

            model.addAttribute("invitations", invitations);
            model.addAttribute("user", userDetails);
            model.addAttribute("totalInvitations", invitations.size());

            log.info("Wyświetlono {} zaproszeń dla użytkownika {}", invitations.size(), userDetails.getId());
            return "invitations/list";

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji użytkownika: {}", e.getMessage());
            model.addAttribute("error", "Błąd autentykacji użytkownika");
            return "redirect:/login";

        } catch (Exception e) {
            log.error("Błąd podczas ładowania zaproszeń dla użytkownika: {}", userDetails.getId(), e);
            model.addAttribute("error", "Błąd podczas ładowania zaproszeń");
            return "invitations/list";
        }
    }

    @PostMapping("/{participantId}/respond")
    @Operation(summary = "Odpowiada na zaproszenie",
            description = "Pozwala użytkownikowi zaakceptować lub odrzucić zaproszenie na spotkanie oraz opcjonalnie dodać komentarz.")
    public String respondToInvitation(
            @PathVariable @NotNull(message = "Identyfikator uczestnika nie może być pusty")
            @Min(value = 1, message = "Identyfikator uczestnika musi być liczbą dodatnią")
            Long participantId,

            @RequestParam @NotBlank(message = "Odpowiedź nie może być pusta")
            String response,

            @RequestParam(required = false)
            @Size(max = 500, message = "Komentarz nie może przekraczać 500 znaków")
            String comment,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetails userDetails,

            RedirectAttributes redirectAttributes) {

        try {
            log.info("Przetwarzanie odpowiedzi na zaproszenie {} przez użytkownika {}", participantId, userDetails.getId());

            ParticipationStatus status = ParticipationStatus.valueOf(response.toUpperCase());
            participantService.respondToInvitation(participantId, status, comment, userDetails.getId());

            String message = getResponseMessage(status);
            redirectAttributes.addFlashAttribute("success", message);

            log.info("Użytkownik {} odpowiedział na zaproszenie {} statusem {}",
                    userDetails.getId(), participantId, status);

        } catch (jakarta.validation.ConstraintViolationException e) {
            String errorMessage = e.getConstraintViolations().stream()
                    .map(violation -> violation.getMessage())
                    .findFirst()
                    .orElse("Nieprawidłowe dane wejściowe");
            log.warn("Błąd walidacji odpowiedzi na zaproszenie: {}", errorMessage);
            redirectAttributes.addFlashAttribute("error", errorMessage);

        } catch (IllegalArgumentException e) {
            log.warn("Nieprawidłowy status odpowiedzi '{}' dla zaproszenia {}", response, participantId);
            redirectAttributes.addFlashAttribute("error", "Nieprawidłowy status odpowiedzi: " + response);

        } catch (SecurityException e) {
            log.warn("Brak uprawnień dla użytkownika {} do odpowiedzi na zaproszenie {}",
                    userDetails.getId(), participantId);
            redirectAttributes.addFlashAttribute("error", "Brak uprawnień do tej operacji");

        } catch (Exception e) {
            log.error("Błąd podczas przetwarzania odpowiedzi na zaproszenie {}: {}", participantId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Błąd podczas przetwarzania odpowiedzi: " + e.getMessage());
        }

        return "redirect:/invitations";
    }

    // Alternatywna wersja z obiektem formularza i BindingResult
    @PostMapping("/{participantId}/respond-form")
    public String respondToInvitationForm(
            @PathVariable @NotNull @Min(1) Long participantId,
            @Valid @ModelAttribute("invitationResponse") InvitationResponseForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal @NotNull CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            log.warn("Błędy walidacji formularza odpowiedzi na zaproszenie {}: {}",
                    participantId, bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.invitationResponse", bindingResult);
            redirectAttributes.addFlashAttribute("invitationResponse", form);
            return "redirect:/invitations";
        }

        try {
            ParticipationStatus status = ParticipationStatus.valueOf(form.getResponse().toUpperCase());
            participantService.respondToInvitation(participantId, status, form.getComment(), userDetails.getId());

            String message = getResponseMessage(status);
            redirectAttributes.addFlashAttribute("success", message);

        } catch (Exception e) {
            log.error("Błąd podczas przetwarzania odpowiedzi na zaproszenie {}: {}", participantId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Wystąpił błąd: " + e.getMessage());
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

    public static class InvitationResponseForm {

        @NotBlank(message = "Odpowiedź nie może być pusta")
        private String response;

        @Size(max = 500, message = "Komentarz nie może przekraczać 500 znaków")
        private String comment;

        // getters i setters
        public String getResponse() {
            return response;
        }

        public void setResponse(String response) {
            this.response = response;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }
    }
}


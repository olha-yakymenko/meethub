package com.meethub.controller.web;

import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.model.enums.PermissionLevel;
import com.meethub.domain.model.request.InviteParticipantsRequest;
import com.meethub.domain.model.request.UpdateParticipantRequest;
import com.meethub.domain.model.response.ParticipantResponse;
import com.meethub.domain.service.MeetingService;
import com.meethub.domain.service.ParticipantService;
import com.meethub.security.CustomUserDetailsService.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/meetings/{meetingId}/participants")
@RequiredArgsConstructor
public class ParticipantController {

    private final ParticipantService participantService;
    private final MeetingService meetingService;

    @GetMapping
    public String getParticipants(@PathVariable Long meetingId,
                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                  Model model) {
        try {
            // Sprawdź uprawnienia użytkownika do tego spotkania
            boolean hasAccess = participantService.hasAccessToMeeting(meetingId, userDetails.getId());
            if (!hasAccess) {
                model.addAttribute("error", "Nie masz dostępu do tej listy uczestników");
                return "error/403";
            }

            List<ParticipantResponse> participants = participantService.getMeetingParticipants(meetingId);
            ParticipantService.ParticipantStats stats = participantService.getMeetingStats(meetingId);

            model.addAttribute("participants", participants);
            model.addAttribute("stats", stats);
            model.addAttribute("meetingId", meetingId);
            model.addAttribute("isOrganizer", participantService.isOrganizer(meetingId, userDetails.getId()));

            return "participants/list";
        } catch (Exception e) {
            log.error("Błąd podczas ładowania uczestników dla meetingId: {}", meetingId, e);
            model.addAttribute("error", "Błąd podczas ładowania listy uczestników");
            return "participants/list";
        }
    }

    @GetMapping("/invite")
    public String showInviteForm(@PathVariable Long meetingId,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 Model model) {
        try {
            // Sprawdź czy użytkownik jest organizatorem
            if (!participantService.isOrganizer(meetingId, userDetails.getId())) {
                model.addAttribute("error", "Tylko organizator może zapraszać uczestników");
                return "redirect:/meetings/" + meetingId + "/participants";
            }

            model.addAttribute("meetingId", meetingId);
            model.addAttribute("inviteRequest", new InviteParticipantsRequest());

            return "participants/invite";
        } catch (Exception e) {
            log.error("Błąd podczas ładowania formularza zaproszeń", e);
            return "redirect:/meetings/" + meetingId + "/participants?error=Nie można załadować formularza";
        }
    }

    @PostMapping("/invite")
    public String inviteParticipants(@PathVariable Long meetingId,
                                     @Valid @ModelAttribute("inviteRequest") InviteParticipantsRequest request,
                                     BindingResult bindingResult,
                                     @AuthenticationPrincipal CustomUserDetails userDetails,
                                     RedirectAttributes redirectAttributes,
                                     Model model) {  // Dodaj Model parameter

        try {
            // Walidacja
            if (bindingResult.hasErrors()) {
                redirectAttributes.addFlashAttribute("error",
                        "Błąd walidacji: " + bindingResult.getFieldError().getDefaultMessage());
                return "redirect:/meetings/" + meetingId + "/participants/invite";
            }

            // Sprawdź uprawnienia
            if (!participantService.isOrganizer(meetingId, userDetails.getId())) {
                redirectAttributes.addFlashAttribute("error", "Tylko organizator może zapraszać uczestników");
                return "redirect:/meetings/" + meetingId + "/participants";
            }

            List<ParticipantResponse> invited = participantService.inviteParticipants(meetingId, request);

            redirectAttributes.addFlashAttribute("success",
                    "Wysłano " + invited.size() + " zaproszeń");

        } catch (Exception e) {
            log.error("Błąd podczas zapraszania uczestników do meetingId: {}", meetingId, e);
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas zapraszania: " + e.getMessage());
            // W przypadku błędu wróć do formularza zamiast listy
            return "redirect:/meetings/" + meetingId + "/participants/invite";
        }

        return "redirect:/meetings/" + meetingId + "/participants";
    }

//    @GetMapping("/{participantId}/edit")
//    public String showEditForm(@PathVariable Long meetingId,
//                               @PathVariable Long participantId,
//                               @AuthenticationPrincipal CustomUserDetails userDetails,
//                               Model model) {
//        try {
//            // Sprawdź uprawnienia
//            if (!participantService.canEditParticipant(meetingId, participantId, userDetails.getId())) {
//                model.addAttribute("error", "Nie masz uprawnień do edycji tego uczestnika");
//                return "redirect:/meetings/" + meetingId + "/participants";
//            }
//
//            ParticipantResponse participant = participantService.getParticipant(participantId);
//            model.addAttribute("participant", participant);
//            model.addAttribute("meetingId", meetingId);
//            model.addAttribute("participantId", participantId);
//            model.addAttribute("updateRequest", new UpdateParticipantRequest());
//
//            // Dodaj enumy do modelu
//            model.addAttribute("participantStatuses",
//                    java.util.Arrays.asList(com.meethub.domain.model.enums.ParticipationStatus.values()));
//            model.addAttribute("permissionLevels",
//                    java.util.Arrays.asList(com.meethub.domain.model.enums.PermissionLevel.values()));
//
//            return "participants/edit";
//        } catch (Exception e) {
//            log.error("Błąd podczas ładowania formularza edycji", e);
//            return "redirect:/meetings/" + meetingId + "/participants?error=Nie można załadować formularza";
//        }
//    }



    @GetMapping("/{participantId}/edit")
    public String showEditForm(@PathVariable Long meetingId,
                               @PathVariable Long participantId,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               Model model) {
        try {
            // Sprawdź uprawnienia
            if (!participantService.canEditParticipant(meetingId, participantId, userDetails.getId())) {
                model.addAttribute("error", "Nie masz uprawnień do edycji tego uczestnika");
                return "redirect:/meetings/" + meetingId + "/participants";
            }

            ParticipantResponse participant = participantService.getParticipant(participantId);

            // Debug - sprawdźmy co mamy w participant
            log.info("Participant status: {}", participant.getStatus());
            log.info("Participant permission level: {}", participant.getPermissionLevel());

            // Pobierz enumy
            ParticipationStatus[] statuses = ParticipationStatus.values();
            PermissionLevel[] levels = PermissionLevel.values();

            // Debug - sprawdźmy enumy
            log.info("Available statuses: {}", Arrays.toString(statuses));
            log.info("Available permission levels: {}", Arrays.toString(levels));

            model.addAttribute("participant", participant);
            model.addAttribute("meetingId", meetingId);
            model.addAttribute("participantId", participantId);
            model.addAttribute("updateRequest", new UpdateParticipantRequest());

            // Dodaj enumy do modelu
            model.addAttribute("participantStatuses", statuses);
            model.addAttribute("permissionLevels", levels);

            return "participants/edit";
        } catch (Exception e) {
            log.error("Błąd podczas ładowania formularza edycji", e);
            return "redirect:/meetings/" + meetingId + "/participants?error=Nie można załadować formularza";
        }
    }

    @PostMapping("/{participantId}/update")
    public String updateParticipant(@PathVariable Long meetingId,
                                    @PathVariable Long participantId,
                                    @Valid @ModelAttribute("updateRequest") UpdateParticipantRequest request,
                                    BindingResult bindingResult,
                                    @AuthenticationPrincipal CustomUserDetails userDetails,
                                    RedirectAttributes redirectAttributes) {
        try {
            // Walidacja
            if (bindingResult.hasErrors()) {
                redirectAttributes.addFlashAttribute("error",
                        "Błąd walidacji: " + bindingResult.getFieldError().getDefaultMessage());
                return "redirect:/meetings/" + meetingId + "/participants/" + participantId + "/edit";
            }

            // Sprawdź uprawnienia
            if (!participantService.canEditParticipant(meetingId, participantId, userDetails.getId())) {
                redirectAttributes.addFlashAttribute("error", "Nie masz uprawnień do edycji tego uczestnika");
                return "redirect:/meetings/" + meetingId + "/participants";
            }

            ParticipantResponse updated = participantService.updateParticipant(participantId, request);

            redirectAttributes.addFlashAttribute("success", "Zaktualizowano uczestnika");
        } catch (Exception e) {
            log.error("Błąd podczas aktualizacji uczestnika: {}", participantId, e);
            redirectAttributes.addFlashAttribute("error", "Błąd podczas aktualizacji: " + e.getMessage());
        }

        return "redirect:/meetings/" + meetingId + "/participants";
    }

    @PostMapping("/{participantId}/remove")
    public String removeParticipant(@PathVariable Long meetingId,
                                    @PathVariable Long participantId,
                                    @AuthenticationPrincipal CustomUserDetails userDetails,
                                    RedirectAttributes redirectAttributes) {
        try {
            // Sprawdź uprawnienia
            if (!participantService.canRemoveParticipant(meetingId, participantId, userDetails.getId())) {
                redirectAttributes.addFlashAttribute("error", "Nie masz uprawnień do usunięcia tego uczestnika");
                return "redirect:/meetings/" + meetingId + "/participants";
            }

            participantService.removeParticipant(participantId);
            redirectAttributes.addFlashAttribute("success", "Usunięto uczestnika");
        } catch (Exception e) {
            log.error("Błąd podczas usuwania uczestnika: {}", participantId, e);
            redirectAttributes.addFlashAttribute("error", "Błąd podczas usuwania: " + e.getMessage());
        }

        return "redirect:/meetings/" + meetingId + "/participants";
    }

    // Publiczne endpointy do potwierdzania przez token
    @GetMapping("/confirm/{token}")
    public String confirmParticipation(@PathVariable String token,
                                       @RequestParam(required = false) String comment,
                                       Model model) {
        try {
            ParticipantResponse participant = participantService.confirmParticipation(token, comment);
            model.addAttribute("success", "Potwierdzono udział w spotkaniu");
            model.addAttribute("participant", participant);
            return "participants/confirmation-success";
        } catch (Exception e) {
            log.error("Błąd podczas potwierdzania udziału z tokenem: {}", token, e);
            model.addAttribute("error", "Błąd podczas potwierdzania: " + e.getMessage());
            return "participants/confirmation-error";
        }
    }

    @GetMapping("/decline/{token}")
    public String declineParticipation(@PathVariable String token,
                                       @RequestParam(required = false) String comment,
                                       Model model) {
        try {
            ParticipantResponse participant = participantService.declineParticipation(token, comment);
            model.addAttribute("success", "Odrzucono zaproszenie na spotkanie");
            model.addAttribute("participant", participant);
            return "participants/confirmation-success";
        } catch (Exception e) {
            log.error("Błąd podczas odrzucania zaproszenia z tokenem: {}", token, e);
            model.addAttribute("error", "Błąd podczas odrzucania: " + e.getMessage());
            return "participants/confirmation-error";
        }
    }

    @GetMapping("/tentative/{token}")
    public String setTentative(@PathVariable String token,
                               @RequestParam(required = false) String comment,
                               Model model) {
        try {
            ParticipantResponse participant = participantService.setTentative(token, comment);
            model.addAttribute("success", "Ustawiono status 'Tentative' dla spotkania");
            model.addAttribute("participant", participant);
            return "participants/confirmation-success";
        } catch (Exception e) {
            log.error("Błąd podczas ustawiania statusu tentative z tokenem: {}", token, e);
            model.addAttribute("error", "Błąd podczas ustawiania statusu: " + e.getMessage());
            return "participants/confirmation-error";
        }
    }
}
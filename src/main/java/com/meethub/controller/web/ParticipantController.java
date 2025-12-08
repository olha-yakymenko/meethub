package com.meethub.controller.web;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.model.enums.PermissionLevel;
import com.meethub.domain.model.projection.ParticipantProjection;
import com.meethub.domain.model.request.InviteParticipantsRequest;
import com.meethub.domain.model.request.UpdateParticipantRequest;
import com.meethub.domain.model.response.ParticipantResponse;
import com.meethub.domain.repository.jpa.MeetingRepository;
import com.meethub.domain.service.MeetingService;
import com.meethub.domain.service.MeetingParticipantService;
import com.meethub.security.CustomUserDetailsService.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    private final MeetingParticipantService participantService;
    private final MeetingService meetingService;
    private final MeetingRepository meetingRepository;

    @GetMapping
    @Operation(summary = "Lista uczestników spotkania", description = "Zwraca listę wszystkich uczestników spotkania oraz statystyki. Wymagane odpowiednie uprawnienia.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista uczestników zwrócona pomyślnie"),
            @ApiResponse(responseCode = "403", description = "Brak uprawnień do wyświetlenia uczestników")
    })
    public String getParticipants(@Parameter(description = "ID spotkania") @PathVariable Long meetingId,
                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                  Model model) {
        try {
            boolean hasAccess = participantService.hasAccessToMeeting(meetingId, userDetails.getId());
            if (!hasAccess) {
                model.addAttribute("error", "Nie masz dostępu do tej listy uczestników");
                return "error/403";
            }

            List<ParticipantProjection> participants = participantService.getMeetingParticipants(meetingId);
            MeetingParticipantService.ParticipantStats stats = participantService.getMeetingStats(meetingId);

            model.addAttribute("participants", participants);
            model.addAttribute("stats", stats);
            model.addAttribute("meetingId", meetingId);
            model.addAttribute("isOrganizer", participantService.isOrganizer(meetingId, userDetails.getId()));

            return "meetings/participants/list";
        } catch (Exception e) {
            log.error("Błąd podczas ładowania uczestników dla meetingId: {}", meetingId, e);
            model.addAttribute("error", "Błąd podczas ładowania listy uczestników");
            return "meetings/participants/list";
        }
    }

    @GetMapping("/invite")
    @Operation(summary = "Formularz zaproszeń uczestników", description = "Wyświetla formularz do zapraszania nowych uczestników spotkania. Tylko dla organizatora.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Formularz zaproszeń wyświetlony pomyślnie"),
            @ApiResponse(responseCode = "403", description = "Brak uprawnień do zapraszania uczestników")
    })
    public String showInviteForm(@Parameter(description = "ID spotkania") @PathVariable Long meetingId,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 Model model) {
        try {
            if (!participantService.isOrganizer(meetingId, userDetails.getId())) {
                model.addAttribute("error", "Tylko organizator może zapraszać uczestników");
                return "redirect:/meetings/" + meetingId + "/participants";
            }

            model.addAttribute("meetingId", meetingId);
            model.addAttribute("inviteRequest", new InviteParticipantsRequest());

            return "meetings/participants/invite";
        } catch (Exception e) {
            log.error("Błąd podczas ładowania formularza zaproszeń", e);
            return "redirect:/meetings/" + meetingId + "/participants?error=Nie można załadować formularza";
        }
    }

    @PostMapping("/invite")
    @Operation(summary = "Wyślij zaproszenia do uczestników", description = "Pozwala organizatorowi wysłać zaproszenia do nowych uczestników spotkania.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Przekierowanie po wysłaniu zaproszeń"),
            @ApiResponse(responseCode = "403", description = "Brak uprawnień do wysłania zaproszeń")
    })
    public String inviteParticipants(@Parameter(description = "ID spotkania") @PathVariable Long meetingId,
                                     @Valid @ModelAttribute("inviteRequest") InviteParticipantsRequest request,
                                     BindingResult bindingResult,
                                     @AuthenticationPrincipal CustomUserDetails userDetails,
                                     RedirectAttributes redirectAttributes,
                                     Model model) {
        try {
            if (bindingResult.hasErrors()) {
                redirectAttributes.addFlashAttribute("error",
                        "Błąd walidacji: " + bindingResult.getFieldError().getDefaultMessage());
                return "redirect:/meetings/" + meetingId + "/participants/invite";
            }

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
            return "redirect:/meetings/" + meetingId + "/participants/invite";
        }

        return "redirect:/meetings/" + meetingId + "/participants";
    }

    @GetMapping("/{participantId}/edit")
    @Operation(summary = "Formularz edycji uczestnika", description = "Wyświetla formularz do edycji danych uczestnika spotkania. Wymagane odpowiednie uprawnienia.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Formularz wyświetlony pomyślnie"),
            @ApiResponse(responseCode = "403", description = "Brak uprawnień do edycji uczestnika")
    })
    public String showEditForm(@Parameter(description = "ID spotkania") @PathVariable Long meetingId,
                               @Parameter(description = "ID uczestnika") @PathVariable Long participantId,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               Model model) {
        try {
            if (!participantService.canEditParticipant(meetingId, participantId, userDetails.getId())) {
                model.addAttribute("error", "Nie masz uprawnień do edycji tego uczestnika");
                return "redirect:/meetings/" + meetingId + "/participants";
            }

            ParticipantResponse participant = participantService.getParticipant(participantId);

            log.info("Participant status: {}", participant.getStatus());
            log.info("Participant permission level: {}", participant.getPermissionLevel());

            ParticipationStatus[] statuses = ParticipationStatus.values();
            PermissionLevel[] levels = PermissionLevel.values();

            log.info("Available statuses: {}", Arrays.toString(statuses));
            log.info("Available permission levels: {}", Arrays.toString(levels));

            model.addAttribute("participant", participant);
            model.addAttribute("meetingId", meetingId);
            model.addAttribute("participantId", participantId);
            model.addAttribute("updateRequest", new UpdateParticipantRequest());

            model.addAttribute("participantStatuses", statuses);
            model.addAttribute("permissionLevels", levels);

            return "meetings/participants/edit";
        } catch (Exception e) {
            log.error("Błąd podczas ładowania formularza edycji", e);
            return "redirect:/meetings/" + meetingId + "/participants?error=Nie można załadować formularza";
        }
    }

    @PostMapping("/{participantId}/update")
    @Operation(summary = "Aktualizuj uczestnika", description = "Aktualizuje dane uczestnika spotkania. Wymagane odpowiednie uprawnienia.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Przekierowanie po aktualizacji"),
            @ApiResponse(responseCode = "403", description = "Brak uprawnień do aktualizacji uczestnika")
    })
    public String updateParticipant(@Parameter(description = "ID spotkania") @PathVariable Long meetingId,
                                    @Parameter(description = "ID uczestnika") @PathVariable Long participantId,
                                    @Valid @ModelAttribute("updateRequest") UpdateParticipantRequest request,
                                    BindingResult bindingResult,
                                    @AuthenticationPrincipal CustomUserDetails userDetails,
                                    RedirectAttributes redirectAttributes) {
        try {
            if (bindingResult.hasErrors()) {
                redirectAttributes.addFlashAttribute("error",
                        "Błąd walidacji: " + bindingResult.getFieldError().getDefaultMessage());
                return "redirect:/meetings/" + meetingId + "/participants/" + participantId + "/edit";
            }

            if (!participantService.canEditParticipant(meetingId, participantId, userDetails.getId())) {
                redirectAttributes.addFlashAttribute("error", "Nie masz uprawnień do edycji tego uczestnika");
                return "redirect:/meetings/" + meetingId + "/participants";
            }

            participantService.updateParticipant(participantId, request);
            redirectAttributes.addFlashAttribute("success", "Zaktualizowano uczestnika");
        } catch (Exception e) {
            log.error("Błąd podczas aktualizacji uczestnika: {}", participantId, e);
            redirectAttributes.addFlashAttribute("error", "Błąd podczas aktualizacji: " + e.getMessage());
        }

        return "redirect:/meetings/" + meetingId + "/participants";
    }

    @PostMapping("/{participantId}/remove")
    @Operation(summary = "Usuń uczestnika", description = "Usuwa uczestnika ze spotkania. Wymagane odpowiednie uprawnienia.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Przekierowanie po usunięciu uczestnika"),
            @ApiResponse(responseCode = "403", description = "Brak uprawnień do usunięcia uczestnika")
    })
    public String removeParticipant(@Parameter(description = "ID spotkania") @PathVariable Long meetingId,
                                    @Parameter(description = "ID uczestnika") @PathVariable Long participantId,
                                    @AuthenticationPrincipal CustomUserDetails userDetails,
                                    RedirectAttributes redirectAttributes) {
        try {
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

    @GetMapping("/confirm/{token}")
    @Operation(summary = "Potwierdź udział uczestnika", description = "Potwierdza udział uczestnika w spotkaniu na podstawie tokena.")
    public String confirmParticipation(@Parameter(description = "Token uczestnika") @PathVariable String token,
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
            return "meetings/participants/confirmation-error";
        }
    }

    @GetMapping("/decline/{token}")
    @Operation(summary = "Odrzuć udział uczestnika", description = "Odrzuca udział uczestnika w spotkaniu na podstawie tokena.")
    public String declineParticipation(@Parameter(description = "Token uczestnika") @PathVariable String token,
                                       @RequestParam(required = false) String comment,
                                       Model model) {
        try {
            ParticipantResponse participant = participantService.declineParticipation(token, comment);
            model.addAttribute("success", "Odrzucono zaproszenie na spotkanie");
            model.addAttribute("participant", participant);
            return "meetings/participants/confirmation-success";
        } catch (Exception e) {
            log.error("Błąd podczas odrzucania zaproszenia z tokenem: {}", token, e);
            model.addAttribute("error", "Błąd podczas odrzucania: " + e.getMessage());
            return "meetings/participants/confirmation-error";
        }
    }

    @PostMapping("/join")
    @Operation(summary = "Dołącz do spotkania", description = "Pozwala zalogowanemu użytkownikowi dołączyć do spotkania publicznego lub wysłać prośbę do spotkania prywatnego.")
    public String joinMeeting(@Parameter(description = "ID spotkania") @PathVariable Long meetingId,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        try {
            if (userDetails == null) {
                redirectAttributes.addFlashAttribute("error", "Musisz być zalogowany");
                return "redirect:/login";
            }

            Meeting meeting = meetingRepository.findById(meetingId)
                    .orElseThrow(() -> new RuntimeException("Spotkanie nie zostało znalezione"));

            boolean isParticipant = participantService.isParticipant(meetingId, userDetails.getId());

            if (isParticipant) {
                redirectAttributes.addFlashAttribute("info", "Już jesteś uczestnikiem tego spotkania");
                return "redirect:/meetings/" + meetingId;
            }

            switch (meeting.getVisibility()) {
                case PUBLIC:
                    participantService.joinPublicMeeting(meetingId, userDetails.getId());
                    redirectAttributes.addFlashAttribute("success", "Dołączono do spotkania publicznego");
                    break;
                case PRIVATE:
                    participantService.requestToJoinPrivateMeeting(meetingId, userDetails.getId());
                    redirectAttributes.addFlashAttribute("success", "Wysłano prośbę o dołączenie do spotkania prywatnego");
                    break;
                case INVITE_ONLY:
                    redirectAttributes.addFlashAttribute("error", "To spotkanie jest dostępne tylko dla zaproszonych");
                    break;
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Błąd: " + e.getMessage());
        }

        return "redirect:/meetings/" + meetingId;
    }

    @PostMapping("/{participantId}/approve")
    @Operation(summary = "Akceptuj prośbę o dołączenie", description = "Pozwala organizatorowi zaakceptować prośbę o dołączenie uczestnika do spotkania.")
    public String approveJoinRequest(@Parameter(description = "ID spotkania") @PathVariable Long meetingId,
                                     @Parameter(description = "ID uczestnika") @PathVariable Long participantId,
                                     @AuthenticationPrincipal CustomUserDetails userDetails,
                                     RedirectAttributes redirectAttributes) {
        try {
            participantService.approveJoinRequest(meetingId, participantId, userDetails.getId());
            redirectAttributes.addFlashAttribute("message", "Zaakceptowano prośbę o dołączenie");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Błąd podczas akceptowania: " + e.getMessage());
        }

        return "redirect:/meetings/" + meetingId + "/participants";
    }

    @PostMapping("/{participantId}/reject")
    @Operation(summary = "Odrzuć prośbę o dołączenie", description = "Pozwala organizatorowi odrzucić prośbę o dołączenie uczestnika do spotkania.")
    public String rejectJoinRequest(@Parameter(description = "ID spotkania") @PathVariable Long meetingId,
                                    @Parameter(description = "ID uczestnika") @PathVariable Long participantId,
                                    @AuthenticationPrincipal CustomUserDetails userDetails,
                                    RedirectAttributes redirectAttributes) {
        try {
            participantService.rejectJoinRequest(meetingId, participantId, userDetails.getId());
            redirectAttributes.addFlashAttribute("message", "Odrzucono prośbę o dołączenie");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Błąd podczas odrzucania: " + e.getMessage());
        }

        return "redirect:/meetings/" + meetingId + "/participants";
    }

    @PostMapping("/leave")
    @Operation(summary = "Opuść spotkanie", description = "Pozwala uczestnikowi opuścić spotkanie.")
    public String leaveMeeting(@Parameter(description = "ID spotkania") @PathVariable Long meetingId,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        try {
            participantService.leaveMeeting(userDetails.getId(), meetingId);
            redirectAttributes.addFlashAttribute("message", "Opuszczono spotkanie");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Błąd podczas opuszczania spotkania: " + e.getMessage());
        }

        return "redirect:/meetings/" + meetingId;
    }

    @GetMapping("/export")
    @Operation(summary = "Eksport uczestników", description = "Eksportuje listę uczestników spotkania w formacie CSV. Tylko dla organizatora.")
    public ResponseEntity<Resource> exportParticipants(@Parameter(description = "ID spotkania") @PathVariable Long meetingId,
                                                       @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (!participantService.isOrganizer(meetingId, userDetails.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        ByteArrayResource resource = participantService.exportParticipantsToCsv(meetingId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=participants_" + meetingId + ".csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }

    @GetMapping("/stats")
    @Operation(summary = "Statystyki uczestników", description = "Wyświetla szczegółowe statystyki uczestników spotkania. Tylko dla organizatora.")
    public String showStats(@Parameter(description = "ID spotkania") @PathVariable Long meetingId,
                            @AuthenticationPrincipal CustomUserDetails userDetails,
                            Model model) {
        if (!participantService.isOrganizer(meetingId, userDetails.getId())) {
            model.addAttribute("error", "Brak uprawnień");
            return "error/403";
        }

        var stats = participantService.getDetailedStats(meetingId);
        model.addAttribute("stats", stats);
        model.addAttribute("meetingId", meetingId);

        return "meetings/participants/stats";
    }
}

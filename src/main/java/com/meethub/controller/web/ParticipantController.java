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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Validated
@Slf4j
@Controller
@RequestMapping("/meetings/{meetingId}/participants")
@RequiredArgsConstructor
public class ParticipantController {

    private final MeetingParticipantService participantService;

    @GetMapping
    @Operation(summary = "Lista uczestników spotkania", description = "Zwraca listę wszystkich uczestników spotkania oraz statystyki. Wymagane odpowiednie uprawnienia.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista uczestników zwrócona pomyślnie"),
            @ApiResponse(responseCode = "403", description = "Brak uprawnień do wyświetlenia uczestników")
    })
    public String getParticipants(
            @Parameter(description = "ID spotkania")
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetails userDetails,
            Model model) {

        Long userId = userDetails.getId();
        log.info("Wyświetlanie listy uczestników spotkania ID={} przez użytkownika {}", meetingId, userId);

        boolean hasAccess = participantService.hasAccessToMeeting(meetingId, userId);
        if (!hasAccess) {
            log.warn("Brak dostępu użytkownika {} do uczestników spotkania {}", userId, meetingId);
            model.addAttribute("error", "Nie masz dostępu do tej listy uczestników");
            return "error/403";
        }

        List<ParticipantProjection> participants = participantService.getMeetingParticipants(meetingId);
        MeetingParticipantService.ParticipantStats stats = participantService.getMeetingStats(meetingId);

        model.addAttribute("participants", participants);
        model.addAttribute("stats", stats);
        model.addAttribute("meetingId", meetingId);
        model.addAttribute("isOrganizer", participantService.isOrganizer(meetingId, userId));

        log.info("Wyświetlono {} uczestników spotkania ID={}", participants.size(), meetingId);
        return "meetings/participants/list";
    }

    @GetMapping("/invite")
    @Operation(summary = "Formularz zaproszeń uczestników", description = "Wyświetla formularz do zapraszania nowych uczestników spotkania. Tylko dla organizatora.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Formularz zaproszeń wyświetlony pomyślnie"),
            @ApiResponse(responseCode = "403", description = "Brak uprawnień do zapraszania uczestników")
    })
    public String showInviteForm(
            @Parameter(description = "ID spotkania")
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetails userDetails,
            Model model) {

        Long userId = userDetails.getId();
        log.info("Wyświetlanie formularza zaproszeń dla spotkania ID={} przez użytkownika {}", meetingId, userId);

        if (!participantService.isOrganizer(meetingId, userId)) {
            log.warn("Brak uprawnień użytkownika {} do zapraszania w spotkaniu {}", userId, meetingId);
            model.addAttribute("error", "Tylko organizator może zapraszać uczestników");
            return "redirect:/meetings/" + meetingId + "/participants";
        }

        model.addAttribute("meetingId", meetingId);
        model.addAttribute("inviteRequest", new InviteParticipantsRequest());

        return "meetings/participants/invite";
    }

    @PostMapping("/invite")
    @Operation(summary = "Wyślij zaproszenia do uczestników", description = "Pozwala organizatorowi wysłać zaproszenia do nowych uczestników spotkania.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Przekierowanie po wysłaniu zaproszeń"),
            @ApiResponse(responseCode = "403", description = "Brak uprawnień do wysłania zaproszeń")
    })
    public String inviteParticipants(
            @Parameter(description = "ID spotkania")
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @Valid @ModelAttribute("inviteRequest") InviteParticipantsRequest request,
            BindingResult bindingResult,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetails userDetails,

            RedirectAttributes redirectAttributes) {

        Long userId = userDetails.getId();
        log.info("Zapraszanie uczestników do spotkania ID={} przez użytkownika {}", meetingId, userId);

        if (bindingResult.hasErrors()) {
            log.warn("Błędy walidacji formularza zaproszeń: {}", bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("error",
                    "Błąd walidacji: " + bindingResult.getFieldError().getDefaultMessage());
            return "redirect:/meetings/" + meetingId + "/participants/invite";
        }

        if (!participantService.isOrganizer(meetingId, userId)) {
            log.warn("Brak uprawnień użytkownika {} do zapraszania w spotkaniu {}", userId, meetingId);
            redirectAttributes.addFlashAttribute("error", "Tylko organizator może zapraszać uczestników");
            return "redirect:/meetings/" + meetingId + "/participants";
        }

        List<ParticipantResponse> invited = participantService.inviteParticipants(meetingId, request);
        redirectAttributes.addFlashAttribute("success",
                "Wysłano " + invited.size() + " zaproszeń");

        log.info("Wysłano {} zaproszeń do spotkania ID={}", invited.size(), meetingId);

        return "redirect:/meetings/" + meetingId + "/participants";
    }

    @GetMapping("/{participantId}/edit")
    @Operation(summary = "Formularz edycji uczestnika", description = "Wyświetla formularz do edycji danych uczestnika spotkania. Wymagane odpowiednie uprawnienia.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Formularz wyświetlony pomyślnie"),
            @ApiResponse(responseCode = "403", description = "Brak uprawnień do edycji uczestnika")
    })
    public String showEditForm(
            @Parameter(description = "ID spotkania")
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @Parameter(description = "ID uczestnika")
            @PathVariable @NotNull(message = "Identyfikator uczestnika nie może być pusty")
            @Min(value = 1, message = "Identyfikator uczestnika musi być liczbą dodatnią")
            Long participantId,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetails userDetails,
            Model model) {

        Long userId = userDetails.getId();
        log.info("Wyświetlanie formularza edycji uczestnika ID={} w spotkaniu ID={} przez użytkownika {}",
                participantId, meetingId, userId);

        if (!participantService.canEditParticipant(meetingId, participantId, userId)) {
            log.warn("Brak uprawnień użytkownika {} do edycji uczestnika {} w spotkaniu {}",
                    userId, participantId, meetingId);
            model.addAttribute("error", "Nie masz uprawnień do edycji tego uczestnika");
            return "redirect:/meetings/" + meetingId + "/participants";
        }

        ParticipantResponse participant = participantService.getParticipant(participantId);
        ParticipationStatus[] statuses = ParticipationStatus.values();
        PermissionLevel[] levels = PermissionLevel.values();

        model.addAttribute("participant", participant);
        model.addAttribute("meetingId", meetingId);
        model.addAttribute("participantId", participantId);
        model.addAttribute("updateRequest", new UpdateParticipantRequest());
        model.addAttribute("participantStatuses", statuses);
        model.addAttribute("permissionLevels", levels);

        return "meetings/participants/edit";
    }

    @PostMapping("/{participantId}/update")
    @Operation(summary = "Aktualizuj uczestnika", description = "Aktualizuje dane uczestnika spotkania. Wymagane odpowiednie uprawnienia.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Przekierowanie po aktualizacji"),
            @ApiResponse(responseCode = "403", description = "Brak uprawnień do aktualizacji uczestnika")
    })
    public String updateParticipant(
            @Parameter(description = "ID spotkania")
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @Parameter(description = "ID uczestnika")
            @PathVariable @NotNull(message = "Identyfikator uczestnika nie może być pusty")
            @Min(value = 1, message = "Identyfikator uczestnika musi być liczbą dodatnią")
            Long participantId,

            @Valid @ModelAttribute("updateRequest") UpdateParticipantRequest request,
            BindingResult bindingResult,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetails userDetails,

            RedirectAttributes redirectAttributes) {

        Long userId = userDetails.getId();
        log.info("Aktualizacja uczestnika ID={} w spotkaniu ID={} przez użytkownika {}",
                participantId, meetingId, userId);

        if (bindingResult.hasErrors()) {
            log.warn("Błędy walidacji formularza aktualizacji uczestnika: {}", bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("error",
                    "Błąd walidacji: " + bindingResult.getFieldError().getDefaultMessage());
            return "redirect:/meetings/" + meetingId + "/participants/" + participantId + "/edit";
        }

        if (!participantService.canEditParticipant(meetingId, participantId, userId)) {
            log.warn("Brak uprawnień użytkownika {} do aktualizacji uczestnika {} w spotkaniu {}",
                    userId, participantId, meetingId);
            redirectAttributes.addFlashAttribute("error", "Nie masz uprawnień do edycji tego uczestnika");
            return "redirect:/meetings/" + meetingId + "/participants";
        }

        participantService.updateParticipant(participantId, request);
        redirectAttributes.addFlashAttribute("success", "Zaktualizowano uczestnika");
        log.info("Uczestnik ID={} zaktualizowany pomyślnie", participantId);

        return "redirect:/meetings/" + meetingId + "/participants";
    }

    @PostMapping("/{participantId}/remove")
    @Operation(summary = "Usuń uczestnika", description = "Usuwa uczestnika ze spotkania. Wymagane odpowiednie uprawnienia.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Przekierowanie po usunięciu uczestnika"),
            @ApiResponse(responseCode = "403", description = "Brak uprawnień do usunięcia uczestnika")
    })
    public String removeParticipant(
            @Parameter(description = "ID spotkania")
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @Parameter(description = "ID uczestnika")
            @PathVariable @NotNull(message = "Identyfikator uczestnika nie może być pusty")
            @Min(value = 1, message = "Identyfikator uczestnika musi być liczbą dodatnią")
            Long participantId,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        Long userId = userDetails.getId();
        log.info("Usuwanie uczestnika ID={} ze spotkania ID={} przez użytkownika {}",
                participantId, meetingId, userId);

        if (!participantService.canRemoveParticipant(meetingId, participantId, userId)) {
            log.warn("Brak uprawnień użytkownika {} do usunięcia uczestnika {} ze spotkania {}",
                    userId, participantId, meetingId);
            redirectAttributes.addFlashAttribute("error", "Nie masz uprawnień do usunięcia tego uczestnika");
            return "redirect:/meetings/" + meetingId + "/participants";
        }

        participantService.removeParticipant(participantId);
        redirectAttributes.addFlashAttribute("success", "Usunięto uczestnika");
        log.info("Uczestnik ID={} usunięty pomyślnie", participantId);

        return "redirect:/meetings/" + meetingId + "/participants";
    }

    @GetMapping("/confirm/{token}")
    @Operation(summary = "Potwierdź udział uczestnika", description = "Potwierdza udział uczestnika w spotkaniu na podstawie tokena.")
    public String confirmParticipation(
            @Parameter(description = "Token uczestnika")
            @PathVariable @NotBlank(message = "Token nie może być pusty")
            @Size(min = 32, max = 64, message = "Token musi mieć od 32 do 64 znaków")
            String token,

            @RequestParam(required = false)
            @Size(max = 500, message = "Komentarz nie może przekraczać 500 znaków")
            String comment,

            Model model) {

        log.info("Potwierdzanie udziału przez token: {}", token.substring(0, Math.min(10, token.length())) + "...");

        ParticipantResponse participant = participantService.confirmParticipation(token, comment);
        model.addAttribute("success", "Potwierdzono udział w spotkaniu");
        model.addAttribute("participant", participant);

        log.info("Udział potwierdzony dla uczestnika ID={} przez token", participant.getId());
        return "participants/confirmation-success";
    }

    @GetMapping("/decline/{token}")
    @Operation(summary = "Odrzuć udział uczestnika", description = "Odrzuca udział uczestnika w spotkaniu na podstawie tokena.")
    public String declineParticipation(
            @Parameter(description = "Token uczestnika")
            @PathVariable @NotBlank(message = "Token nie może być pusty")
            @Size(min = 32, max = 64, message = "Token musi mieć od 32 do 64 znaków")
            String token,

            @RequestParam(required = false)
            @Size(max = 500, message = "Komentarz nie może przekraczać 500 znaków")
            String comment,

            Model model) {

        log.info("Odrzucanie udziału przez token: {}", token.substring(0, Math.min(10, token.length())) + "...");

        ParticipantResponse participant = participantService.declineParticipation(token, comment);
        model.addAttribute("success", "Odrzucono zaproszenie na spotkanie");
        model.addAttribute("participant", participant);

        log.info("Udział odrzucony dla uczestnika ID={} przez token", participant.getId());
        return "meetings/participants/confirmation-success";
    }

    @PostMapping("/join")
    public String joinMeeting(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        Long userId = userDetails != null ? userDetails.getId() : null;
        log.info("Dołączanie do spotkania ID={} przez użytkownika {}", meetingId, userId);

        participantService.joinMeeting(meetingId, userId);

        redirectAttributes.addFlashAttribute("success", "Operacja wykonana pomyślnie");
        log.info("Dołączono do spotkania ID={} przez użytkownika {}", meetingId, userId);

        return "redirect:/meetings/" + meetingId;
    }

    @PostMapping("/{participantId}/approve")
    @Operation(summary = "Akceptuj prośbę o dołączenie", description = "Pozwala organizatorowi zaakceptować prośbę o dołączenie uczestnika do spotkania.")
    public String approveJoinRequest(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @PathVariable @NotNull(message = "Identyfikator uczestnika nie może być pusty")
            @Min(value = 1, message = "Identyfikator uczestnika musi być liczbą dodatnią")
            Long participantId,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        log.info("Akceptowanie prośby o dołączenie uczestnika ID={} do spotkania ID={} przez użytkownika {}",
                participantId, meetingId, userDetails.getId());

        participantService.approveJoinRequest(meetingId, participantId, userDetails.getId());
        redirectAttributes.addFlashAttribute("message", "Zaakceptowano prośbę o dołączenie");
        log.info("Prośba o dołączenie uczestnika ID={} zaakceptowana", participantId);

        return "redirect:/meetings/" + meetingId + "/participants";
    }

    @PostMapping("/{participantId}/reject")
    @Operation(summary = "Odrzuć prośbę o dołączenie", description = "Pozwala organizatorowi odrzucić prośbę o dołączenie uczestnika do spotkania.")
    public String rejectJoinRequest(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @PathVariable @NotNull(message = "Identyfikator uczestnika nie może być pusty")
            @Min(value = 1, message = "Identyfikator uczestnika musi być liczbą dodatnią")
            Long participantId,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        log.info("Odrzucanie prośby o dołączenie uczestnika ID={} do spotkania ID={} przez użytkownika {}",
                participantId, meetingId, userDetails.getId());

        participantService.rejectJoinRequest(meetingId, participantId, userDetails.getId());
        redirectAttributes.addFlashAttribute("message", "Odrzucono prośbę o dołączenie");
        log.info("Prośba o dołączenie uczestnika ID={} odrzucona", participantId);

        return "redirect:/meetings/" + meetingId + "/participants";
    }

    @PostMapping("/leave")
    @Operation(summary = "Opuść spotkanie", description = "Pozwala uczestnikowi opuścić spotkanie.")
    public String leaveMeeting(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        log.info("Opuszczanie spotkania ID={} przez użytkownika {}", meetingId, userDetails.getId());

        participantService.leaveMeeting(userDetails.getId(), meetingId);
        redirectAttributes.addFlashAttribute("message", "Opuszczono spotkanie");
        log.info("Użytkownik {} opuścił spotkanie {}", userDetails.getId(), meetingId);

        return "redirect:/meetings/" + meetingId;
    }

    @GetMapping("/export")
    @Operation(summary = "Eksport uczestników", description = "Eksportuje listę uczestników spotkania w formacie CSV. Tylko dla organizatora.")
    public ResponseEntity<Resource> exportParticipants(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetails userDetails) {

        Long userId = userDetails.getId();
        log.info("Eksport uczestników spotkania ID={} przez użytkownika {}", meetingId, userId);

        if (!participantService.isOrganizer(meetingId, userId)) {
            log.warn("Brak uprawnień użytkownika {} do eksportu uczestników spotkania {}", userId, meetingId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        ByteArrayResource resource = participantService.exportParticipantsToCsv(meetingId);

        log.info("Wyeksportowano uczestników spotkania ID={} do CSV", meetingId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=participants_" + meetingId + ".csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }

    @GetMapping("/stats")
    @Operation(summary = "Statystyki uczestników", description = "Wyświetla szczegółowe statystyki uczestników spotkania. Tylko dla organizatora.")
    public String showStats(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetails userDetails,
            Model model) {

        Long userId = userDetails.getId();
        log.info("Wyświetlanie statystyk uczestników spotkania ID={} przez użytkownika {}", meetingId, userId);

        if (!participantService.isOrganizer(meetingId, userId)) {
            log.warn("Brak uprawnień użytkownika {} do statystyk uczestników spotkania {}", userId, meetingId);
            model.addAttribute("error", "Brak uprawnień");
            return "error/403";
        }

        var stats = participantService.getDetailedStats(meetingId);
        model.addAttribute("stats", stats);
        model.addAttribute("meetingId", meetingId);

        log.info("Wyświetlono statystyki uczestników spotkania ID={}", meetingId);
        return "meetings/participants/stats";
    }
}
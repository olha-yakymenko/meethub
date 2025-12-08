package com.meethub.controller.web;

import com.meethub.domain.model.request.MeetingResourceRequest;
import com.meethub.domain.service.MeetingResourceService;
import com.meethub.domain.service.MeetingService;
import com.meethub.security.CustomUserDetailsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/meetings/{meetingId}/resources")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Zasoby Spotkania", description = "Zarządzanie zasobami spotkań: dodawanie, podgląd, pobieranie i usuwanie")
public class MeetingResourceWebController {

    private final MeetingService meetingService;
    private final MeetingResourceService meetingResourceService;

    @GetMapping("/add")
    @Operation(
            summary = "Formularz dodawania zasobu",
            description = "Wyświetla formularz umożliwiający dodanie nowego zasobu do spotkania."
    )
    public String showAddResourceForm(
            @Parameter(description = "ID spotkania, do którego dodawany jest zasób", required = true)
            @PathVariable Long meetingId,
            Model model,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            var meeting = meetingService.getMeeting(meetingId);
            model.addAttribute("meeting", meeting);

            if (!model.containsAttribute("meetingResourceRequest")) {
                model.addAttribute("meetingResourceRequest", new MeetingResourceRequest());
            }

            return "meetings/resources/add-resource";
        } catch (Exception e) {
            log.error("Error showing add resource form for meeting: {}", meetingId, e);
            return "redirect:/meetings?error=Spotkanie nie istnieje";
        }
    }

    @PostMapping("/add")
    @Operation(
            summary = "Dodaj zasób do spotkania",
            description = "Dodaje nowy zasób do spotkania. W przypadku błędów walidacji zwraca formularz z komunikatami."
    )
    public String addResource(
            @Parameter(description = "ID spotkania, do którego dodawany jest zasób", required = true)
            @PathVariable Long meetingId,
            @Parameter(description = "Dane zasobu spotkania", required = true)
            @ModelAttribute @Valid MeetingResourceRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        Long userId = userDetails.getId();
        log.info("Adding resource for user ID: {}", userId);

        try {
            var meeting = meetingService.getMeeting(meetingId);
            model.addAttribute("meeting", meeting);

            if (result.hasErrors()) {
                log.warn("Validation errors for resource request: {}", result.getAllErrors());
                return "meetings/resources/add-resource";
            }

            meetingResourceService.addResource(meetingId, request, userId);
            redirectAttributes.addFlashAttribute("success", "Zasób został dodany pomyślnie");
            return "redirect:/meetings/" + meetingId + "/resources";

        } catch (Exception e) {
            log.error("Error adding resource to meeting: {}", meetingId, e);
            model.addAttribute("error", "Błąd podczas dodawania zasobu: " + e.getMessage());
            return "meetings/resources/add-resource";
        }
    }

    @GetMapping
    @Operation(
            summary = "Lista zasobów spotkania",
            description = "Wyświetla listę wszystkich zasobów powiązanych ze spotkaniem."
    )
    public String getMeetingResources(
            @Parameter(description = "ID spotkania, którego zasoby mają zostać wyświetlone", required = true)
            @PathVariable Long meetingId,
            Model model,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            var meeting = meetingService.getMeeting(meetingId);
            var resources = meetingResourceService.getMeetingResources(meetingId, userDetails.getId());

            model.addAttribute("meeting", meeting);
            model.addAttribute("resources", resources);
            model.addAttribute("resourcesCount", resources != null ? resources.size() : 0);

            return "meetings/resources/resources-list";
        } catch (Exception e) {
            log.error("Error getting resources for meeting: {}", meetingId, e);
            return "redirect:/meetings?error=Nie udało się pobrać zasobów";
        }
    }

    @PostMapping("/{resourceId}/delete")
    @Operation(
            summary = "Usuń zasób",
            description = "Usuwa wskazany zasób ze spotkania."
    )
    public String deleteResource(
            @Parameter(description = "ID spotkania", required = true)
            @PathVariable Long meetingId,
            @Parameter(description = "ID zasobu do usunięcia", required = true)
            @PathVariable Long resourceId,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            meetingResourceService.deleteResource(resourceId, userDetails.getId());
            redirectAttributes.addFlashAttribute("success", "Zasób został usunięty pomyślnie");
        } catch (Exception e) {
            log.error("Error deleting resource: {}", resourceId, e);
            redirectAttributes.addFlashAttribute("error", "Błąd podczas usuwania zasobu: " + e.getMessage());
        }

        return "redirect:/meetings/" + meetingId + "/resources";
    }

    @GetMapping("/{resourceId}/download")
    @Operation(
            summary = "Pobierz zasób",
            description = "Przekierowuje do API umożliwiającego pobranie pliku zasobu."
    )
    public String downloadResourcePage(
            @Parameter(description = "ID spotkania", required = true)
            @PathVariable Long meetingId,
            @Parameter(description = "ID zasobu do pobrania", required = true)
            @PathVariable Long resourceId,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        return "redirect:/api/meetings/" + meetingId + "/resources/" + resourceId + "/download";
    }

    @GetMapping("/{resourceId}/preview")
    @Operation(
            summary = "Podgląd zasobu",
            description = "Przekierowuje do API umożliwiającego podgląd zasobu."
    )
    public String previewResourcePage(
            @Parameter(description = "ID spotkania", required = true)
            @PathVariable Long meetingId,
            @Parameter(description = "ID zasobu do podglądu", required = true)
            @PathVariable Long resourceId,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        return "redirect:/api/meetings/" + meetingId + "/resources/" + resourceId + "/preview";
    }
}

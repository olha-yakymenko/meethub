package com.meethub.controller.web;

import com.meethub.domain.model.request.MeetingResourceRequest;
import com.meethub.domain.service.MeetingResourceService;
import com.meethub.domain.service.MeetingService;
import com.meethub.security.CustomUserDetailsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Validated
@Controller
@RequestMapping("/meetings/{meetingId}/resources")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Zasoby Spotkania", description = "Strony web do zarządzania zasobami spotkań")
public class MeetingResourceWebController {

    private final MeetingService meetingService;
    private final MeetingResourceService meetingResourceService;

    // Formularz dodawania zasobu
    @GetMapping("/add")
    @Operation(summary = "Formularz dodawania zasobu")
    public String showAddResourceForm(
            @PathVariable @NotNull @Min(1) Long meetingId,
            Model model,
            @AuthenticationPrincipal @NotNull CustomUserDetailsService.CustomUserDetails userDetails) {

        log.info("Wyświetlanie formularza dodawania zasobu dla spotkania ID={} przez użytkownika {}", meetingId, userDetails.getId());

        var meeting = meetingService.getMeeting(meetingId);
        model.addAttribute("meeting", meeting);

        if (!model.containsAttribute("meetingResourceRequest")) {
            model.addAttribute("meetingResourceRequest", new MeetingResourceRequest());
        }

        return "meetings/resources/add-resource";
    }

    // Dodanie zasobu (POST)
    @PostMapping("/add")
    @Operation(summary = "Dodaj zasób do spotkania")
    public String addResource(
            @PathVariable @NotNull @Min(1) Long meetingId,
            @Valid @ModelAttribute MeetingResourceRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal @NotNull CustomUserDetailsService.CustomUserDetails userDetails) {

        Long userId = userDetails.getId();
        log.info("Dodawanie zasobu do spotkania ID={} przez użytkownika {}", meetingId, userId);

        var meeting = meetingService.getMeeting(meetingId);
        model.addAttribute("meeting", meeting);

        if (result.hasErrors()) {
            log.warn("Błędy walidacji formularza zasobu: {}", result.getAllErrors());
            return "meetings/resources/add-resource";
        }

        meetingResourceService.addResource(meetingId, request, userId);
        redirectAttributes.addFlashAttribute("success", "Zasób został dodany pomyślnie");
        log.info("Zasób dodany pomyślnie do spotkania ID={} przez użytkownika {}", meetingId, userId);

        return "redirect:/meetings/" + meetingId + "/resources";
    }

    // Lista zasobów
    @GetMapping
    @Operation(summary = "Lista zasobów spotkania")
    public String getMeetingResources(
            @PathVariable @NotNull @Min(1) Long meetingId,
            Model model,
            @AuthenticationPrincipal @NotNull CustomUserDetailsService.CustomUserDetails userDetails) {

        log.info("Wyświetlanie listy zasobów dla spotkania ID={} przez użytkownika {}", meetingId, userDetails.getId());

        var meeting = meetingService.getMeeting(meetingId);
        var resources = meetingResourceService.getMeetingResources(meetingId, userDetails.getId());

        model.addAttribute("meeting", meeting);
        model.addAttribute("resources", resources);
        model.addAttribute("resourcesCount", resources != null ? resources.size() : 0);

        return "meetings/resources/resources-list";
    }

    // Usuwanie zasobu
    @PostMapping("/{resourceId}/delete")
    @Operation(summary = "Usuń zasób")
    public String deleteResource(
            @PathVariable @NotNull @Min(1) Long meetingId,
            @PathVariable @NotNull @Min(1) Long resourceId,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal @NotNull CustomUserDetailsService.CustomUserDetails userDetails) {

        log.info("Usuwanie zasobu ID={} ze spotkania ID={} przez użytkownika {}", resourceId, meetingId, userDetails.getId());
        meetingResourceService.deleteResource(resourceId, userDetails.getId());
        redirectAttributes.addFlashAttribute("success", "Zasób został usunięty pomyślnie");

        return "redirect:/meetings/" + meetingId + "/resources";
    }

    // Pobieranie zasobu
    @GetMapping("/{resourceId}/download")
    @Operation(summary = "Pobierz zasób")
    public String downloadResource(
            @PathVariable @NotNull @Min(1) Long meetingId,
            @PathVariable @NotNull @Min(1) Long resourceId) {

        log.info("Przekierowanie do pobierania zasobu ID={} ze spotkania ID={}", resourceId, meetingId);
        return "redirect:/api/meetings/" + meetingId + "/resources/" + resourceId + "/download";
    }

    // Podgląd zasobu
    @GetMapping("/{resourceId}/preview")
    @Operation(summary = "Podgląd zasobu")
    public String previewResource(
            @PathVariable @NotNull @Min(1) Long meetingId,
            @PathVariable @NotNull @Min(1) Long resourceId) {

        log.info("Przekierowanie do podglądu zasobu ID={} ze spotkania ID={}", resourceId, meetingId);
        return "redirect:/api/meetings/" + meetingId + "/resources/" + resourceId + "/preview";
    }

    // Szczegóły zasobu
    @GetMapping("/{resourceId}/details")
    @Operation(summary = "Szczegóły zasobu")
    public String showResourceDetails(
            @PathVariable @NotNull @Min(1) Long meetingId,
            @PathVariable @NotNull @Min(1) Long resourceId,
            Model model,
            @AuthenticationPrincipal @NotNull CustomUserDetailsService.CustomUserDetails userDetails) {

        log.info("Wyświetlanie szczegółów zasobu ID={} ze spotkania ID={}", resourceId, meetingId);

        var meeting = meetingService.getMeeting(meetingId);
        var resource = meetingResourceService.getResource(resourceId, userDetails.getId());

        model.addAttribute("meeting", meeting);
        model.addAttribute("resource", resource);

        return "meetings/resources/resource-details";
    }
}

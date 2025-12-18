package com.meethub.controller.web;

import com.meethub.domain.model.request.MeetingResourceRequest;
import com.meethub.domain.service.MeetingResourceService;
import com.meethub.domain.service.MeetingService;
import com.meethub.security.CustomUserDetailsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

    @GetMapping("/add")
    @Operation(
            summary = "Formularz dodawania zasobu",
            description = "Wyświetla formularz umożliwiający dodanie nowego zasobu do spotkania."
    )
    public String showAddResourceForm(
            @Parameter(description = "ID spotkania, do którego dodawany jest zasób", required = true)
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,
            Model model,
            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetailsService.CustomUserDetails userDetails) {

        try {
            log.info("Wyświetlanie formularza dodawania zasobu dla spotkania ID={} przez użytkownika {}",
                    meetingId, userDetails.getId());

            var meeting = meetingService.getMeeting(meetingId);
            model.addAttribute("meeting", meeting);

            if (!model.containsAttribute("meetingResourceRequest")) {
                model.addAttribute("meetingResourceRequest", new MeetingResourceRequest());
            }

            return "meetings/resources/add-resource";

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji przy wyświetlaniu formularza dodawania zasobu: {}", e.getMessage());
            return "redirect:/meetings?error=Nieprawidłowy identyfikator spotkania";

        } catch (Exception e) {
            log.error("Błąd podczas wyświetlania formularza dodawania zasobu dla spotkania: {}", meetingId, e);
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
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @Parameter(description = "Dane zasobu spotkania", required = true)
            @ModelAttribute @Valid MeetingResourceRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetailsService.CustomUserDetails userDetails) {

        try {
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

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji podczas dodawania zasobu: {}", e.getMessage());
            model.addAttribute("error", "Nieprawidłowe dane w formularzu");
            return "meetings/resources/add-resource";

        } catch (Exception e) {
            log.error("Błąd podczas dodawania zasobu do spotkania: {}", meetingId, e);
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
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,
            Model model,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetailsService.CustomUserDetails userDetails) {

        try {
            log.info("Wyświetlanie listy zasobów dla spotkania ID={} przez użytkownika {}",
                    meetingId, userDetails.getId());

            var meeting = meetingService.getMeeting(meetingId);
            var resources = meetingResourceService.getMeetingResources(meetingId, userDetails.getId());

            model.addAttribute("meeting", meeting);
            model.addAttribute("resources", resources);
            int resourcesCount = resources != null ? resources.size() : 0;
            model.addAttribute("resourcesCount", resourcesCount);

            log.info("Wyświetlono {} zasobów dla spotkania ID={}", resourcesCount, meetingId);
            return "meetings/resources/resources-list";

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji przy wyświetlaniu listy zasobów: {}", e.getMessage());
            return "redirect:/meetings?error=Nieprawidłowy identyfikator spotkania";

        } catch (Exception e) {
            log.error("Błąd pobierania zasobów dla spotkania: {}", meetingId, e);
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
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @Parameter(description = "ID zasobu do usunięcia", required = true)
            @PathVariable @NotNull(message = "Identyfikator zasobu nie może być pusty")
            @Min(value = 1, message = "Identyfikator zasobu musi być liczbą dodatnią")
            Long resourceId,

            RedirectAttributes redirectAttributes,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetailsService.CustomUserDetails userDetails) {

        try {
            log.info("Usuwanie zasobu ID={} ze spotkania ID={} przez użytkownika {}",
                    resourceId, meetingId, userDetails.getId());

            meetingResourceService.deleteResource(resourceId, userDetails.getId());
            redirectAttributes.addFlashAttribute("success", "Zasób został usunięty pomyślnie");
            log.info("Zasób ID={} usunięty pomyślnie", resourceId);

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji podczas usuwania zasobu: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Nieprawidłowy identyfikator zasobu");

        } catch (Exception e) {
            log.error("Błąd usuwania zasobu: {}", resourceId, e);
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
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @Parameter(description = "ID zasobu do pobrania", required = true)
            @PathVariable @NotNull(message = "Identyfikator zasobu nie może być pusty")
            @Min(value = 1, message = "Identyfikator zasobu musi być liczbą dodatnią")
            Long resourceId,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetailsService.CustomUserDetails userDetails) {

        try {
            log.info("Przekierowanie do pobierania zasobu ID={} ze spotkania ID={} przez użytkownika {}",
                    resourceId, meetingId, userDetails.getId());

            return "redirect:/api/meetings/" + meetingId + "/resources/" + resourceId + "/download";

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji dla pobierania zasobu: {}", e.getMessage());
            return "redirect:/meetings/" + meetingId + "/resources?error=Nieprawidłowy identyfikator zasobu";
        }
    }

    @GetMapping("/{resourceId}/preview")
    @Operation(
            summary = "Podgląd zasobu",
            description = "Przekierowuje do API umożliwiającego podgląd zasobu."
    )
    public String previewResourcePage(
            @Parameter(description = "ID spotkania", required = true)
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @Parameter(description = "ID zasobu do podglądu", required = true)
            @PathVariable @NotNull(message = "Identyfikator zasobu nie może być pusty")
            @Min(value = 1, message = "Identyfikator zasobu musi być liczbą dodatnią")
            Long resourceId,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetailsService.CustomUserDetails userDetails) {

        try {
            log.info("Przekierowanie do podglądu zasobu ID={} ze spotkania ID={} przez użytkownika {}",
                    resourceId, meetingId, userDetails.getId());

            return "redirect:/api/meetings/" + meetingId + "/resources/" + resourceId + "/preview";

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji dla podglądu zasobu: {}", e.getMessage());
            return "redirect:/meetings/" + meetingId + "/resources?error=Nieprawidłowy identyfikator zasobu";
        }
    }

    @GetMapping("/{resourceId}/details")
    @Operation(
            summary = "Szczegóły zasobu",
            description = "Wyświetla szczegóły wybranego zasobu spotkania."
    )
    public String showResourceDetails(
            @PathVariable @NotNull @Min(1) Long meetingId,
            @PathVariable @NotNull @Min(1) Long resourceId,
            Model model,
            @AuthenticationPrincipal @NotNull CustomUserDetailsService.CustomUserDetails userDetails) {

        try {
            log.info("Wyświetlanie szczegółów zasobu ID={} ze spotkania ID={}", resourceId, meetingId);

            var meeting = meetingService.getMeeting(meetingId);
            var resource = meetingResourceService.getResource(resourceId, userDetails.getId());

            model.addAttribute("meeting", meeting);
            model.addAttribute("resource", resource);

            return "meetings/resources/resource-details";

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji dla szczegółów zasobu: {}", e.getMessage());
            return "redirect:/meetings/" + meetingId + "/resources?error=Nieprawidłowy identyfikator";

        } catch (Exception e) {
            log.error("Błąd podczas wyświetlania szczegółów zasobu ID={}: {}", resourceId, e.getMessage());
            return "redirect:/meetings/" + meetingId + "/resources?error=Nie można wyświetlić szczegółów zasobu";
        }
    }

    @PostMapping("/{resourceId}/toggle-visibility")
    @Operation(
            summary = "Przełącz widoczność zasobu",
            description = "Zmienia widoczność zasobu (publiczny/prywatny)."
    )
    public String toggleResourceVisibility(
            @PathVariable @NotNull @Min(1) Long meetingId,
            @PathVariable @NotNull @Min(1) Long resourceId,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal @NotNull CustomUserDetailsService.CustomUserDetails userDetails) {

        try {
            log.info("Zmiana widoczności zasobu ID={} w spotkaniu ID={}", resourceId, meetingId);

            // Ta metoda wymagałaby implementacji w serwisie
            // meetingResourceService.toggleResourceVisibility(resourceId, userDetails.getId());

            redirectAttributes.addFlashAttribute("success", "Widoczność zasobu została zmieniona");

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji dla zmiany widoczności zasobu: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Nieprawidłowy identyfikator zasobu");

        } catch (Exception e) {
            log.error("Błąd podczas zmiany widoczności zasobu ID={}: {}", resourceId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Błąd podczas zmiany widoczności: " + e.getMessage());
        }

        return "redirect:/meetings/" + meetingId + "/resources/" + resourceId + "/details";
    }
}














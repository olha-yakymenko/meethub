package com.meethub.controller.web;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.MeetingStatistics;
import com.meethub.domain.service.MeetingAnalyticsService;
import com.meethub.domain.service.MeetingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Validated
@Controller
@RequestMapping("/meetings/{meetingId}/analytics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Analityka Spotkania", description = "Strony web do przeglądania i generowania statystyk spotkań")
public class MeetingAnalyticsWebController {

    private final MeetingAnalyticsService analyticsService;
    private final MeetingService meetingService;

    @GetMapping
    @Operation(
            summary = "Strona analityki spotkania",
            description = "Wyświetla stronę z informacjami o statystykach spotkania. Jeśli statystyki nie istnieją, informuje użytkownika, że należy je wygenerować."
    )
    public String showAnalyticsPage(
            @Parameter(description = "ID spotkania, którego statystyki mają zostać wyświetlone", required = true)
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            log.info("Wyświetlanie strony analityki dla spotkania ID={}", meetingId);

            Meeting meeting = meetingService.getMeeting(meetingId);
            model.addAttribute("meeting", meeting);

            Optional<MeetingStatistics> statsOpt = analyticsService.getMeetingStatistics(meetingId);

            if (statsOpt.isPresent()) {
                MeetingStatistics stats = statsOpt.get();
                model.addAttribute("meetingStatistics", stats);
                log.info("Znaleziono statystyki dla spotkania {}: {} wpisów", meetingId, stats.getId());
            } else {
                model.addAttribute("meetingStatistics", null);
                redirectAttributes.addFlashAttribute("info",
                        "Brak statystyk. Wygeneruj je pierwszy raz.");
                log.info("Brak statystyk dla spotkania {}, wymagane wygenerowanie", meetingId);
            }

            return "meetings/analytics";

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji ID spotkania: {} - {}", meetingId, e.getMessage());
            redirectAttributes.addFlashAttribute("error",
                    "Nieprawidłowy identyfikator spotkania");
            return "redirect:/meetings";

        } catch (Exception e) {
            log.error("Błąd ładowania strony analityki dla spotkania {}: {}", meetingId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error",
                    "Nie można załadować statystyk: " + e.getMessage());
            return "redirect:/meetings/" + meetingId;
        }
    }

    @PostMapping("/generate")
    @Operation(
            summary = "Generuj lub odśwież statystyki spotkania",
            description = "Generuje statystyki spotkania i odświeża je. Po wygenerowaniu przekierowuje z powrotem do strony analityki."
    )
    public String generateStatistics(
            @Parameter(description = "ID spotkania, dla którego generowane są statystyki", required = true)
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,
            RedirectAttributes redirectAttributes) {

        try {
            log.info("Generowanie statystyk dla spotkania ID={}", meetingId);
            analyticsService.generateMeetingStatistics(meetingId);
            redirectAttributes.addFlashAttribute("success",
                    "Statystyki zostały wygenerowane pomyślnie!");
            log.info("Statystyki dla spotkania {} wygenerowane pomyślnie", meetingId);

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji ID spotkania podczas generowania statystyk: {} - {}", meetingId, e.getMessage());
            redirectAttributes.addFlashAttribute("error",
                    "Nieprawidłowy identyfikator spotkania");

        } catch (Exception e) {
            log.error("Błąd podczas generowania statystyk dla spotkania {}: {}", meetingId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas generowania statystyk: " + e.getMessage());
        }
        return "redirect:/meetings/" + meetingId + "/analytics";
    }

    @GetMapping("/export/csv")
    @Operation(
            summary = "Eksport statystyk do CSV",
            description = "Przekierowuje użytkownika do API generującego plik CSV ze statystykami spotkania."
    )
    public String exportToCsv(
            @Parameter(description = "ID spotkania, którego statystyki mają zostać wyeksportowane", required = true)
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId) {

        try {
            log.info("Przekierowanie do eksportu CSV dla spotkania ID={}", meetingId);
            return "redirect:/api/v1/analytics/meetings/" + meetingId + "/export/csv";

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji ID spotkania dla eksportu CSV: {} - {}", meetingId, e.getMessage());
            return "redirect:/meetings/" + meetingId + "/analytics?error=Nieprawidłowy identyfikator spotkania";
        }
    }

    @GetMapping("/export/pdf")
    @Operation(
            summary = "Eksport statystyk do PDF",
            description = "Przekierowuje użytkownika do API generującego plik PDF ze statystykami spotkania."
    )
    public String exportToPdf(
            @Parameter(description = "ID spotkania, którego statystyki mają zostać wyeksportowane", required = true)
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId) {

        try {
            log.info("Przekierowanie do eksportu PDF dla spotkania ID={}", meetingId);
            return "redirect:/api/v1/analytics/meetings/" + meetingId + "/export/pdf";

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji ID spotkania dla eksportu PDF: {} - {}", meetingId, e.getMessage());
            return "redirect:/meetings/" + meetingId + "/analytics?error=Nieprawidłowy identyfikator spotkania";
        }
    }

    @GetMapping("/compare")
    @Operation(
            summary = "Porównaj statystyki",
            description = "Wyświetla stronę do porównania statystyk bieżącego spotkania z innymi spotkaniami."
    )
    public String compareStatistics(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,
            Model model) {

        try {
            log.info("Wyświetlanie strony porównania statystyk dla spotkania ID={}", meetingId);
            Meeting meeting = meetingService.getMeeting(meetingId);
            model.addAttribute("meeting", meeting);

            return "meetings/analytics-compare";

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji ID spotkania dla porównania statystyk: {} - {}", meetingId, e.getMessage());
            return "redirect:/meetings/" + meetingId + "/analytics?error=Nieprawidłowy identyfikator spotkania";

        } catch (Exception e) {
            log.error("Błąd podczas ładowania strony porównania statystyk dla spotkania {}: {}", meetingId, e.getMessage(), e);
            return "redirect:/meetings/" + meetingId + "/analytics?error=Błąd ładowania strony porównania";
        }
    }
}







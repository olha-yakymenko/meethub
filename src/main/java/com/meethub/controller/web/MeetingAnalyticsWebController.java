package com.meethub.controller.web;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.MeetingStatistics;
import com.meethub.domain.service.MeetingAnalyticsService;
import com.meethub.domain.service.MeetingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

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
            @PathVariable Long meetingId,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            Meeting meeting = meetingService.getMeeting(meetingId);
            model.addAttribute("meeting", meeting);

            Optional<MeetingStatistics> statsOpt = analyticsService.getMeetingStatistics(meetingId);

            if (statsOpt.isPresent()) {
                model.addAttribute("meetingStatistics", statsOpt.get());
            } else {
                model.addAttribute("meetingStatistics", null);
                redirectAttributes.addFlashAttribute("info",
                        "Brak statystyk. Wygeneruj je pierwszy raz.");
            }

            return "meetings/analytics";
        } catch (Exception e) {
            log.error("Error loading analytics page for meeting {}: {}", meetingId, e.getMessage());
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
            @PathVariable Long meetingId,
            RedirectAttributes redirectAttributes) {
        try {
            analyticsService.generateMeetingStatistics(meetingId);
            redirectAttributes.addFlashAttribute("success",
                    "Statystyki zostały wygenerowane pomyślnie!");
        } catch (Exception e) {
            log.error("Error generating statistics for meeting {}: {}", meetingId, e.getMessage());
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas generowania statystyk: " + e.getMessage());
        }
        return "redirect:/meetings/" + meetingId;
    }

    @GetMapping("/export/csv")
    @Operation(
            summary = "Eksport statystyk do CSV",
            description = "Przekierowuje użytkownika do API generującego plik CSV ze statystykami spotkania."
    )
    public String exportToCsv(
            @Parameter(description = "ID spotkania, którego statystyki mają zostać wyeksportowane", required = true)
            @PathVariable Long meetingId) {
        return "redirect:/api/v1/analytics/meetings/" + meetingId + "/export/csv";
    }

    @GetMapping("/export/pdf")
    @Operation(
            summary = "Eksport statystyk do PDF",
            description = "Przekierowuje użytkownika do API generującego plik PDF ze statystykami spotkania."
    )
    public String exportToPdf(
            @Parameter(description = "ID spotkania, którego statystyki mają zostać wyeksportowane", required = true)
            @PathVariable Long meetingId) {
        return "redirect:/api/v1/analytics/meetings/" + meetingId + "/export/pdf";
    }
}

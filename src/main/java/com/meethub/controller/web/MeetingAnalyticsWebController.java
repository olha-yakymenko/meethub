// com.meethub.controller.web.MeetingAnalyticsWebController.java
package com.meethub.controller.web;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.MeetingStatistics;
import com.meethub.domain.service.MeetingAnalyticsService;
import com.meethub.domain.service.MeetingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/meetings/{meetingId}/analytics")
@RequiredArgsConstructor
@Slf4j
public class MeetingAnalyticsWebController {

    private final MeetingAnalyticsService analyticsService;
    private final MeetingService meetingService;

    // Strona główna analityki (HTML)
    @GetMapping
    public String showAnalyticsPage(@PathVariable Long meetingId,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        try {
            // Pobierz spotkanie
            Meeting meeting = meetingService.getMeeting(meetingId);
            model.addAttribute("meeting", meeting);

            // Pobierz statystyki
            Optional<MeetingStatistics> statsOpt = analyticsService.getMeetingStatistics(meetingId);

            if (statsOpt.isPresent()) {
                model.addAttribute("meetingStatistics", statsOpt.get());
            } else {
                model.addAttribute("meetingStatistics", null);
                redirectAttributes.addFlashAttribute("info",
                        "Brak statystyk. Wygeneruj je pierwszy raz.");
            }

            return "meetings/analytics"; // meetings/analytics.html

        } catch (Exception e) {
            log.error("Error loading analytics page for meeting {}: {}",
                    meetingId, e.getMessage());
            redirectAttributes.addFlashAttribute("error",
                    "Nie można załadować statystyk: " + e.getMessage());
            return "redirect:/meetings/" + meetingId;
        }
    }

    // Generuj/odśwież statystyki (POST dla Thymeleaf)
    @PostMapping("/generate")
    public String generateStatistics(@PathVariable Long meetingId,
                                     RedirectAttributes redirectAttributes) {
        try {
            analyticsService.generateMeetingStatistics(meetingId);
            redirectAttributes.addFlashAttribute("success",
                    "Statystyki zostały wygenerowane pomyślnie!");

        } catch (Exception e) {
            log.error("Error generating statistics for meeting {}: {}",
                    meetingId, e.getMessage());
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas generowania statystyk: " + e.getMessage());
        }

        return "redirect:/meetings/" + meetingId + "/analytics";
    }

    // Eksport CSV (przekierowanie do API)
    @GetMapping("/export/csv")
    public String exportToCsv(@PathVariable Long meetingId) {
        // Przekieruj do endpointu API
        return "redirect:/api/v1/analytics/meetings/" + meetingId + "/export/csv";
    }

    // Eksport PDF (przekierowanie do API)
    @GetMapping("/export/pdf")
    public String exportToPdf(@PathVariable Long meetingId) {
        // Przekieruj do endpointu API
        return "redirect:/api/v1/analytics/meetings/" + meetingId + "/export/pdf";
    }
}
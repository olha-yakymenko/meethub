package com.meethub.controller.web;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.MeetingStatistics;
import com.meethub.domain.model.request.MeetingRequest;
import com.meethub.domain.service.MeetingAnalyticsService;
import com.meethub.domain.service.MeetingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
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
    public String showAnalyticsPage(@Validated @ModelAttribute MeetingRequest request,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {

        Long meetingId = request.getMeetingId();
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
    }

    @PostMapping("/generate")
    @Operation(
            summary = "Generuj lub odśwież statystyki spotkania",
            description = "Generuje statystyki spotkania i odświeża je. Po wygenerowaniu przekierowuje z powrotem do strony analityki."
    )
    public String generateStatistics(@Validated @ModelAttribute MeetingRequest request,
                                     RedirectAttributes redirectAttributes) {

        Long meetingId = request.getMeetingId();
        log.info("Generowanie statystyk dla spotkania ID={}", meetingId);
        analyticsService.generateMeetingStatistics(meetingId);
        redirectAttributes.addFlashAttribute("success",
                "Statystyki zostały wygenerowane pomyślnie!");
        log.info("Statystyki dla spotkania {} wygenerowane pomyślnie", meetingId);

        return "redirect:/meetings/" + meetingId + "/analytics";
    }

    @GetMapping("/export/csv")
    @Operation(
            summary = "Eksport statystyk do CSV",
            description = "Przekierowuje użytkownika do API generującego plik CSV ze statystykami spotkania."
    )
    public String exportToCsv(@Validated @ModelAttribute MeetingRequest request) {

        Long meetingId = request.getMeetingId();
        log.info("Przekierowanie do eksportu CSV dla spotkania ID={}", meetingId);
        return "redirect:/api/v1/analytics/meetings/" + meetingId + "/export/csv";
    }

    @GetMapping("/export/pdf")
    @Operation(
            summary = "Eksport statystyk do PDF",
            description = "Przekierowuje użytkownika do API generującego plik PDF ze statystykami spotkania."
    )
    public String exportToPdf(@Validated @ModelAttribute MeetingRequest request) {

        Long meetingId = request.getMeetingId();
        log.info("Przekierowanie do eksportu PDF dla spotkania ID={}", meetingId);
        return "redirect:/api/v1/analytics/meetings/" + meetingId + "/export/pdf";
    }




}

package com.meethub.controller.api;

import com.meethub.domain.service.MeetingMarkService;
import com.meethub.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/meetings")
@RequiredArgsConstructor
public class MeetingMarkController {

    private final MeetingMarkService meetingMarkService;

    @PostMapping("/{meetingId}/important")
    @PreAuthorize("isAuthenticated()")
    public String markAsImportant(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
            @PathVariable Long meetingId,
            RedirectAttributes redirectAttributes) {

        Long currentUserId = userDetails.getId();
        log.info("User {} marking meeting {} as important", currentUserId, meetingId);

        try {
            meetingMarkService.markAsImportant(currentUserId, meetingId);
            redirectAttributes.addFlashAttribute("success", "Spotkanie oznaczone jako ważne");
        } catch (Exception e) {
            log.error("Error marking meeting as important", e);
            redirectAttributes.addFlashAttribute("error", "Nie udało się oznaczyć spotkania jako ważne: " + e.getMessage());
        }

        return "redirect:/meetings/" + meetingId;
    }

    @PostMapping("/{meetingId}/important/toggle")
    @PreAuthorize("isAuthenticated()")
    public String toggleImportant(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
            @PathVariable Long meetingId,
            RedirectAttributes redirectAttributes) {

        Long currentUserId = userDetails.getId();
        log.info("User {} toggling important status for meeting {}", currentUserId, meetingId);

        try {
            boolean newStatus = meetingMarkService.toggleImportant(currentUserId, meetingId);
            String message = newStatus ?
                    "Spotkanie oznaczone jako ważne" : "Spotkanie odznaczone z ważnych";
            redirectAttributes.addFlashAttribute("success", message);
        } catch (Exception e) {
            log.error("Error toggling important status", e);
            redirectAttributes.addFlashAttribute("error", "Nie udało się zmienić statusu: " + e.getMessage());
        }

        return "redirect:/meetings/" + meetingId;
    }

    @DeleteMapping("/{meetingId}/important")
    @PreAuthorize("isAuthenticated()")
    public String unmarkAsImportant(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
            @PathVariable Long meetingId,
            RedirectAttributes redirectAttributes) {

        Long currentUserId = userDetails.getId();
        log.info("User {} unmarking meeting {} as important", currentUserId, meetingId);

        try {
            meetingMarkService.unmarkAsImportant(currentUserId, meetingId);
            redirectAttributes.addFlashAttribute("success", "Spotkanie odznaczone z ważnych");
        } catch (Exception e) {
            log.error("Error unmarking meeting as important", e);
            redirectAttributes.addFlashAttribute("error", "Nie udało się odznaczyć spotkania: " + e.getMessage());
        }

        return "redirect:/meetings/" + meetingId;
    }

    @GetMapping("/{meetingId}/important/check")
    @PreAuthorize("isAuthenticated()")
    public String isMeetingImportant(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
            @PathVariable Long meetingId,
            Model model) {

        Long currentUserId = userDetails.getId();
        log.debug("Checking if meeting {} is important for user {}", meetingId, currentUserId);

        boolean isImportant = meetingMarkService.isMeetingImportantForUser(currentUserId, meetingId);
        model.addAttribute("isImportant", isImportant);

        // Możesz tu zwrócić fragment widoku zamiast pełnego przekierowania
        return "fragments :: important-status"; // przykładowy fragment
    }

    @GetMapping("/important")
    @PreAuthorize("isAuthenticated()")
    public String getImportantMeetings(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
            Model model) {

        Long currentUserId = userDetails.getId();
        log.debug("Fetching important meetings for user {}", currentUserId);

        List<Long> importantMeetingIds = meetingMarkService.getImportantMeetingIdsForUser(currentUserId);

        model.addAttribute("importantMeetingIds", importantMeetingIds);
        model.addAttribute("count", importantMeetingIds.size());

        return "meetings/important";
    }
}



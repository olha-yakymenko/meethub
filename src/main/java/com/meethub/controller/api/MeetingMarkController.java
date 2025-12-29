package com.meethub.controller.api;

import com.meethub.domain.model.request.MeetingMarkRequest;
import com.meethub.domain.model.response.MeetingMarkResponse;
import com.meethub.domain.service.MeetingMarkService;
import com.meethub.security.CustomUserDetailsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<MeetingMarkResponse> markAsImportant(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
            @PathVariable Long meetingId,
            @RequestBody(required = false) MeetingMarkRequest request) {

        Long currentUserId = userDetails.getId();
        log.info("User {} marking meeting {} as important", currentUserId, meetingId);

        try {
            meetingMarkService.markAsImportant(currentUserId, meetingId);
            log.info("Spotkanie {} oznaczone jako ważne przez użytkownika {}", meetingId, currentUserId);

            MeetingMarkResponse response = new MeetingMarkResponse(
                    meetingId,
                    currentUserId,
                    true,
                    "Spotkanie oznaczone jako ważne"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Nie udało się oznaczyć spotkania jako ważne", e);

            MeetingMarkResponse response = new MeetingMarkResponse(
                    meetingId,
                    currentUserId,
                    false,
                    "Nie udało się oznaczyć spotkania jako ważne: " + e.getMessage()
            );

            return ResponseEntity.status(500).body(response);
        }
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

}



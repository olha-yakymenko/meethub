package com.meethub.controller.web;

import com.meethub.domain.service.NotificationService;
import com.meethub.security.CustomUserDetailsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Powiadomienia Web", description = "Widoki powiadomień")
public class NotificationWebController {

    private final NotificationService notificationService;

    @GetMapping("/in-app")
    @Operation(summary = "Strona powiadomień IN_APP",
            description = "Wyświetla stronę z powiadomieniami IN_APP użytkownika")
    public String inAppNotificationsPage(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
            Model model) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            List<String> messages = notificationService.getInAppMessages(userDetails.getId());
            model.addAttribute("messages", messages);
            model.addAttribute("pageTitle", "Moje powiadomienia");
            model.addAttribute("totalCount", messages.size());

        } catch (Exception e) {
            log.error("Błąd pobierania powiadomień: {}", e.getMessage());
            model.addAttribute("error", "Nie udało się pobrać powiadomień");
        }

        return "notifications/in-app";
    }

    @GetMapping("/in-app/recent")
    @Operation(summary = "Ostatnie powiadomienia",
            description = "Wyświetla stronę z ostatnimi powiadomieniami")
    public String recentNotificationsPage(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
            @RequestParam(defaultValue = "10") int limit,
            Model model) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            if (limit < 1 || limit > 100) {
                limit = 10; // Domyślna wartość
            }

            List<String> messages = notificationService
                    .getRecentInAppMessages(userDetails.getId(), limit);

            model.addAttribute("messages", messages);
            model.addAttribute("pageTitle", "Ostatnie powiadomienia");
            model.addAttribute("limit", limit);
            model.addAttribute("totalCount", messages.size());

        } catch (Exception e) {
            log.error("Błąd pobierania ostatnich powiadomień: {}", e.getMessage());
            model.addAttribute("error", "Nie udało się pobrać ostatnich powiadomień");
        }

        return "notifications/recent";
    }
}
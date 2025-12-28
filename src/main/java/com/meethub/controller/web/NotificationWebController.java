package com.meethub.controller.web;

import com.meethub.domain.service.NotificationService;
import com.meethub.security.CustomUserDetailsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Validated
@Slf4j
@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Powiadomienia Web", description = "Strony web do zarządzania powiadomieniami")
public class NotificationWebController {

    private final NotificationService notificationService;

    @GetMapping("/in-app")
    @Operation(summary = "Strona powiadomień IN_APP",
            description = "Wyświetla stronę z powiadomieniami IN_APP użytkownika")
    public String inAppNotificationsPage(
            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetailsService.CustomUserDetails userDetails,
            Model model) {

        Long userId = userDetails.getId();
        log.info("Wyświetlanie strony powiadomień IN_APP dla użytkownika {}", userId);

        List<String> messages = notificationService.getInAppMessages(userId);
        model.addAttribute("messages", messages);
        model.addAttribute("pageTitle", "Moje powiadomienia");
        model.addAttribute("totalCount", messages.size());

        log.info("Wyświetlono {} powiadomień IN_APP dla użytkownika {}", messages.size(), userId);
        return "notifications.html/in-app";
    }

    @GetMapping("/in-app/recent")
    @Operation(summary = "Ostatnie powiadomienia",
            description = "Wyświetla stronę z ostatnimi powiadomieniami")
    public String recentNotificationsPage(
            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetailsService.CustomUserDetails userDetails,

            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "Limit musi być co najmniej 1")
            @Max(value = 100, message = "Limit nie może przekraczać 100")
            int limit,

            Model model) {

        Long userId = userDetails.getId();
        log.info("Wyświetlanie ostatnich {} powiadomień dla użytkownika {}", limit, userId);

        List<String> messages = notificationService.getRecentInAppMessages(userId, limit);

        model.addAttribute("messages", messages);
        model.addAttribute("pageTitle", "Ostatnie powiadomienia");
        model.addAttribute("limit", limit);
        model.addAttribute("totalCount", messages.size());

        log.info("Wyświetlono {} ostatnich powiadomień dla użytkownika {}", messages.size(), userId);
        return "notifications.html/recent";
    }
}
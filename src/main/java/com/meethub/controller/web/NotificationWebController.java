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

@Validated // DODANE - walidacja dla kontrolera webowego
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

        try {
            Long userId = userDetails.getId();
            log.info("Wyświetlanie strony powiadomień IN_APP dla użytkownika {}", userId);

            List<String> messages = notificationService.getInAppMessages(userId);
            model.addAttribute("messages", messages);
            model.addAttribute("pageTitle", "Moje powiadomienia");
            model.addAttribute("totalCount", messages.size());

            log.info("Wyświetlono {} powiadomień IN_APP dla użytkownika {}", messages.size(), userId);
            return "notifications/in-app";

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji użytkownika na stronie powiadomień: {}", e.getMessage());
            model.addAttribute("error", "Błąd autentykacji użytkownika");
            return "redirect:/login";

        } catch (Exception e) {
            log.error("Błąd pobierania powiadomień dla użytkownika {}: {}",
                    userDetails != null ? userDetails.getId() : "null", e.getMessage(), e);
            model.addAttribute("error", "Nie udało się pobrać powiadomień");
            return "notifications/in-app";
        }
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

        try {
            Long userId = userDetails.getId();
            log.info("Wyświetlanie ostatnich {} powiadomień dla użytkownika {}", limit, userId);

            List<String> messages = notificationService.getRecentInAppMessages(userId, limit);

            model.addAttribute("messages", messages);
            model.addAttribute("pageTitle", "Ostatnie powiadomienia");
            model.addAttribute("limit", limit);
            model.addAttribute("totalCount", messages.size());

            log.info("Wyświetlono {} ostatnich powiadomień dla użytkownika {}", messages.size(), userId);
            return "notifications/recent";

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji parametrów na stronie ostatnich powiadomień: {}", e.getMessage());
            model.addAttribute("error", "Nieprawidłowe parametry wyświetlania");
            return "notifications/recent";

        } catch (Exception e) {
            log.error("Błąd pobierania ostatnich powiadomień dla użytkownika {}: {}",
                    userDetails != null ? userDetails.getId() : "null", e.getMessage(), e);
            model.addAttribute("error", "Nie udało się pobrać ostatnich powiadomień");
            return "notifications/recent";
        }
    }
}











//package com.meethub.controller.web;
//
//import com.meethub.domain.service.NotificationService;
//import com.meethub.security.CustomUserDetailsService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//
//import java.util.List;
//
//@Slf4j
//@Controller
//@RequestMapping("/notifications")
//@RequiredArgsConstructor
//@Tag(name = "Powiadomienia Web", description = "Widoki powiadomień")
//public class NotificationWebController {
//
//    private final NotificationService notificationService;
//
//    @GetMapping("/in-app")
//    @Operation(summary = "Strona powiadomień IN_APP",
//            description = "Wyświetla stronę z powiadomieniami IN_APP użytkownika")
//    public String inAppNotificationsPage(
//            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
//            Model model) {
//
//        if (userDetails == null) {
//            return "redirect:/login";
//        }
//
//        try {
//            List<String> messages = notificationService.getInAppMessages(userDetails.getId());
//            model.addAttribute("messages", messages);
//            model.addAttribute("pageTitle", "Moje powiadomienia");
//            model.addAttribute("totalCount", messages.size());
//
//        } catch (Exception e) {
//            log.error("Błąd pobierania powiadomień: {}", e.getMessage());
//            model.addAttribute("error", "Nie udało się pobrać powiadomień");
//        }
//
//        return "notifications/in-app";
//    }
//
//    @GetMapping("/in-app/recent")
//    @Operation(summary = "Ostatnie powiadomienia",
//            description = "Wyświetla stronę z ostatnimi powiadomieniami")
//    public String recentNotificationsPage(
//            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
//            @RequestParam(defaultValue = "10") int limit,
//            Model model) {
//
//        if (userDetails == null) {
//            return "redirect:/login";
//        }
//
//        try {
//            if (limit < 1 || limit > 100) {
//                limit = 10; // Domyślna wartość
//            }
//
//            List<String> messages = notificationService
//                    .getRecentInAppMessages(userDetails.getId(), limit);
//
//            model.addAttribute("messages", messages);
//            model.addAttribute("pageTitle", "Ostatnie powiadomienia");
//            model.addAttribute("limit", limit);
//            model.addAttribute("totalCount", messages.size());
//
//        } catch (Exception e) {
//            log.error("Błąd pobierania ostatnich powiadomień: {}", e.getMessage());
//            model.addAttribute("error", "Nie udało się pobrać ostatnich powiadomień");
//        }
//
//        return "notifications/recent";
//    }
//}
package com.meethub.controller.web;

import com.meethub.domain.model.request.NotificationPreferencesRequest;
import com.meethub.domain.model.request.UpdateProfileRequest;
import com.meethub.domain.model.response.NotificationResponse;
import com.meethub.domain.model.response.UserProfileResponse;
import com.meethub.domain.service.NotificationService;
import com.meethub.domain.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final NotificationService notificationService;
    private final UserService userService;

    @GetMapping
    @Operation(summary = "Profil użytkownika", description = "Wyświetla profil zalogowanego użytkownika wraz z ostatnimi powiadomieniami.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profil użytkownika wyświetlony pomyślnie"),
            @ApiResponse(responseCode = "302", description = "Użytkownik niezalogowany, przekierowanie do logowania")
    })
    public String profile(@Parameter(hidden = true) Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        Long userId = userService.getUserIdByEmail(principal.getName());
        UserProfileResponse profile = notificationService.getUserProfileWithPreferences(userId);
        model.addAttribute("user", profile);

        Page<NotificationResponse> notifications = notificationService.getUserNotifications(
                userId, PageRequest.of(0, 5));
        model.addAttribute("recentNotifications", notifications.getContent());
        model.addAttribute("unreadCount", notificationService.getUnreadCount(userId));

        return "user/profile";
    }

    @GetMapping("/notifications")
    @Operation(summary = "Lista powiadomień", description = "Wyświetla listę powiadomień użytkownika z paginacją.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista powiadomień wyświetlona pomyślnie"),
            @ApiResponse(responseCode = "302", description = "Użytkownik niezalogowany, przekierowanie do logowania")
    })
    public String notifications(
            @Parameter(hidden = true) Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {

        if (principal == null) {
            return "redirect:/login";
        }

        Long userId = userService.getUserIdByEmail(principal.getName());
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationResponse> notifications = notificationService.getUserNotifications(userId, pageable);

        model.addAttribute("notifications", notifications);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", notifications.getTotalPages());
        model.addAttribute("unreadCount", notificationService.getUnreadCount(userId));

        return "user/notifications";
    }

    @GetMapping("/settings")
    @Operation(summary = "Ustawienia profilu", description = "Wyświetla formularz ustawień użytkownika i preferencje powiadomień.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Formularz ustawień wyświetlony pomyślnie"),
            @ApiResponse(responseCode = "302", description = "Użytkownik niezalogowany, przekierowanie do logowania")
    })
    public String settings(@Parameter(hidden = true) Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        Long userId = userService.getUserIdByEmail(principal.getName());
        UserProfileResponse profile = notificationService.getUserProfileWithPreferences(userId);
        model.addAttribute("user", profile);
        model.addAttribute("preferencesRequest", new NotificationPreferencesRequest());
        model.addAttribute("updateProfileRequest", new UpdateProfileRequest()); // Dodajemy pusty obiekt dla formularza
        return "user/settings";
    }


    @PostMapping("/update")
    @Operation(summary = "Aktualizacja profilu", description = "Aktualizuje dane profilu zalogowanego użytkownika.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Profil zaktualizowany pomyślnie lub przekierowanie do logowania"),
            @ApiResponse(responseCode = "200", description = "Błąd walidacji formularza, wyświetlenie formularza z błędami")
    })
    public String updateProfile(
            @Parameter(description = "Dane aktualizacji profilu") @Valid @ModelAttribute UpdateProfileRequest request,
            BindingResult result,
            @Parameter(hidden = true) Principal principal,
            RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Proszę poprawić błędy w formularzu");
            return "redirect:/profile/settings";
        }

        try {
            Long userId = userService.getUserIdByEmail(principal.getName());
            // Aktualizacja danych użytkownika w UserService (implementacja wymagana)
            redirectAttributes.addFlashAttribute("success", "Profil został zaktualizowany");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Błąd podczas aktualizacji profilu: " + e.getMessage());
        }

        return "redirect:/profile";
    }


    @PostMapping("/notifications/preferences")
    @Operation(summary = "Aktualizacja preferencji powiadomień", description = "Aktualizuje preferencje powiadomień zalogowanego użytkownika.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Preferencje zaktualizowane lub przekierowanie do logowania"),
            @ApiResponse(responseCode = "200", description = "Błąd walidacji formularza, wyświetlenie formularza z błędami")
    })
    public String updateNotificationPreferences(
            @Parameter(description = "Preferencje powiadomień") @Valid @ModelAttribute("preferencesRequest") NotificationPreferencesRequest request,
            BindingResult result,
            @Parameter(hidden = true) Principal principal,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            Long userId = userService.getUserIdByEmail(principal.getName());
            UserProfileResponse profile = notificationService.getUserProfileWithPreferences(userId);
            model.addAttribute("user", profile);
            model.addAttribute("updateProfileRequest", new UpdateProfileRequest());
            model.addAttribute("error", "Proszę poprawić błędy w preferencjach powiadomień");
            return "user/settings";
        }

        try {
            Long userId = userService.getUserIdByEmail(principal.getName());
            notificationService.updateNotificationPreferences(userId, request);
            redirectAttributes.addFlashAttribute("success", "Preferencje powiadomień zostały zaktualizowane");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Błąd podczas aktualizacji preferencji: " + e.getMessage());
        }

        return "redirect:/profile/settings";
    }

    @PostMapping("/notifications/mark-all-read")
    @Operation(summary = "Oznacz wszystkie powiadomienia jako przeczytane", description = "Oznacza wszystkie powiadomienia zalogowanego użytkownika jako przeczytane.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Wszystkie powiadomienia oznaczone lub przekierowanie do logowania")
    })
    public String markAllNotificationsAsRead(
            @Parameter(hidden = true) Principal principal,
            RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/login";
        }

        Long userId = userService.getUserIdByEmail(principal.getName());
        notificationService.markAllAsRead(userId);
        redirectAttributes.addFlashAttribute("success", "Wszystkie powiadomienia oznaczone jako przeczytane");
        return "redirect:/profile/notifications";
    }

    @PostMapping("/notifications/{id}/read")
    @Operation(summary = "Oznacz powiadomienie jako przeczytane", description = "Oznacza konkretne powiadomienie użytkownika jako przeczytane.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Powiadomienie oznaczone jako przeczytane lub przekierowanie do logowania")
    })
    public String markNotificationAsRead(
            @Parameter(description = "ID powiadomienia") @PathVariable Long id,
            @Parameter(hidden = true) Principal principal,
            RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/login";
        }

        Long userId = userService.getUserIdByEmail(principal.getName());
        notificationService.markAsRead(id, userId);
        redirectAttributes.addFlashAttribute("success", "Powiadomienie oznaczone jako przeczytane");
        return "redirect:/profile/notifications";
    }
}










//package com.meethub.controller.web;
//
//import com.meethub.domain.model.request.NotificationPreferencesRequest;
//import com.meethub.domain.model.request.UpdateProfileRequest;
//import com.meethub.domain.model.response.NotificationResponse;
//import com.meethub.domain.model.response.UserProfileResponse;
//import com.meethub.domain.service.NotificationService;
//import com.meethub.domain.service.UserService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.Parameter;
//import io.swagger.v3.oas.annotations.responses.ApiResponse;
//import io.swagger.v3.oas.annotations.responses.ApiResponses;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.validation.BindingResult;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.servlet.mvc.support.RedirectAttributes;
//
//import java.security.Principal;
//
//@Controller
//@RequestMapping("/profile")
//@RequiredArgsConstructor
//public class UserProfileController {
//
//    private final NotificationService notificationService;
//    private final UserService userService;
//
//    @GetMapping
//    @Operation(summary = "Profil użytkownika", description = "Wyświetla profil zalogowanego użytkownika wraz z ostatnimi powiadomieniami.")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "Profil użytkownika wyświetlony pomyślnie"),
//            @ApiResponse(responseCode = "302", description = "Użytkownik niezalogowany, przekierowanie do logowania")
//    })
//    public String profile(@Parameter(hidden = true) Principal principal, Model model) {
//        if (principal == null) {
//            return "redirect:/login";
//        }
//
//        Long userId = userService.getUserIdByEmail(principal.getName());
//        UserProfileResponse profile = notificationService.getUserProfileWithPreferences(userId);
//        model.addAttribute("user", profile);
//
//        Page<NotificationResponse> notifications = notificationService.getUserNotifications(
//                userId, PageRequest.of(0, 5));
//        model.addAttribute("recentNotifications", notifications.getContent());
//        model.addAttribute("unreadCount", notificationService.getUnreadCount(userId));
//
//        return "user/profile";
//    }
//
//    @GetMapping("/notifications")
//    @Operation(summary = "Lista powiadomień", description = "Wyświetla listę powiadomień użytkownika z paginacją.")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "Lista powiadomień wyświetlona pomyślnie"),
//            @ApiResponse(responseCode = "302", description = "Użytkownik niezalogowany, przekierowanie do logowania")
//    })
//    public String notifications(
//            @Parameter(hidden = true) Principal principal,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "20") int size,
//            Model model) {
//
//        if (principal == null) {
//            return "redirect:/login";
//        }
//
//        Long userId = userService.getUserIdByEmail(principal.getName());
//        Pageable pageable = PageRequest.of(page, size);
//        Page<NotificationResponse> notifications = notificationService.getUserNotifications(userId, pageable);
//
//        model.addAttribute("notifications", notifications);
//        model.addAttribute("currentPage", page);
//        model.addAttribute("totalPages", notifications.getTotalPages());
//        model.addAttribute("unreadCount", notificationService.getUnreadCount(userId));
//
//        return "user/notifications";
//    }
//
//    @GetMapping("/settings")
//    @Operation(summary = "Ustawienia profilu", description = "Wyświetla formularz ustawień użytkownika i preferencje powiadomień.")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "Formularz ustawień wyświetlony pomyślnie"),
//            @ApiResponse(responseCode = "302", description = "Użytkownik niezalogowany, przekierowanie do logowania")
//    })
//    public String settings(@Parameter(hidden = true) Principal principal, Model model) {
//        if (principal == null) {
//            return "redirect:/login";
//        }
//
//        Long userId = userService.getUserIdByEmail(principal.getName());
//        UserProfileResponse profile = notificationService.getUserProfileWithPreferences(userId);
//        model.addAttribute("user", profile);
//        model.addAttribute("preferencesRequest", new NotificationPreferencesRequest());
//        return "user/settings";
//    }
//
//    @PostMapping("/update")
//    @Operation(summary = "Aktualizacja profilu", description = "Aktualizuje dane profilu zalogowanego użytkownika.")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "302", description = "Profil zaktualizowany pomyślnie lub przekierowanie do logowania"),
//            @ApiResponse(responseCode = "200", description = "Błąd walidacji formularza, wyświetlenie formularza z błędami")
//    })
//    public String updateProfile(
//            @Parameter(description = "Dane aktualizacji profilu") @Valid @ModelAttribute UpdateProfileRequest request,
//            BindingResult result,
//            @Parameter(hidden = true) Principal principal,
//            RedirectAttributes redirectAttributes) {
//
//        if (principal == null) {
//            return "redirect:/login";
//        }
//
//        if (result.hasErrors()) {
//            redirectAttributes.addFlashAttribute("error", "Proszę poprawić błędy w formularzu");
//            return "redirect:/profile/settings";
//        }
//
//        try {
//            Long userId = userService.getUserIdByEmail(principal.getName());
//            // Aktualizacja danych użytkownika w UserService (implementacja wymagana)
//            redirectAttributes.addFlashAttribute("success", "Profil został zaktualizowany");
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("error", "Błąd podczas aktualizacji profilu: " + e.getMessage());
//        }
//
//        return "redirect:/profile";
//    }
//
//    @PostMapping("/notifications/preferences")
//    @Operation(summary = "Aktualizacja preferencji powiadomień", description = "Aktualizuje preferencje powiadomień zalogowanego użytkownika.")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "302", description = "Preferencje zaktualizowane lub przekierowanie do logowania")
//    })
//    public String updateNotificationPreferences(
//            @Parameter(description = "Preferencje powiadomień") @ModelAttribute NotificationPreferencesRequest request,
//            @Parameter(hidden = true) Principal principal,
//            RedirectAttributes redirectAttributes) {
//
//        if (principal == null) {
//            return "redirect:/login";
//        }
//
//        try {
//            Long userId = userService.getUserIdByEmail(principal.getName());
//            notificationService.updateNotificationPreferences(userId, request);
//            redirectAttributes.addFlashAttribute("success", "Preferencje powiadomień zostały zaktualizowane");
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("error", "Błąd podczas aktualizacji preferencji: " + e.getMessage());
//        }
//
//        return "redirect:/profile/settings";
//    }
//
//    @PostMapping("/notifications/mark-all-read")
//    @Operation(summary = "Oznacz wszystkie powiadomienia jako przeczytane", description = "Oznacza wszystkie powiadomienia zalogowanego użytkownika jako przeczytane.")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "302", description = "Wszystkie powiadomienia oznaczone lub przekierowanie do logowania")
//    })
//    public String markAllNotificationsAsRead(
//            @Parameter(hidden = true) Principal principal,
//            RedirectAttributes redirectAttributes) {
//
//        if (principal == null) {
//            return "redirect:/login";
//        }
//
//        Long userId = userService.getUserIdByEmail(principal.getName());
//        notificationService.markAllAsRead(userId);
//        redirectAttributes.addFlashAttribute("success", "Wszystkie powiadomienia oznaczone jako przeczytane");
//        return "redirect:/profile/notifications";
//    }
//
//    @PostMapping("/notifications/{id}/read")
//    @Operation(summary = "Oznacz powiadomienie jako przeczytane", description = "Oznacza konkretne powiadomienie użytkownika jako przeczytane.")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "302", description = "Powiadomienie oznaczone jako przeczytane lub przekierowanie do logowania")
//    })
//    public String markNotificationAsRead(
//            @Parameter(description = "ID powiadomienia") @PathVariable Long id,
//            @Parameter(hidden = true) Principal principal,
//            RedirectAttributes redirectAttributes) {
//
//        if (principal == null) {
//            return "redirect:/login";
//        }
//
//        Long userId = userService.getUserIdByEmail(principal.getName());
//        notificationService.markAsRead(id, userId);
//        redirectAttributes.addFlashAttribute("success", "Powiadomienie oznaczone jako przeczytane");
//        return "redirect:/profile/notifications";
//    }
//}

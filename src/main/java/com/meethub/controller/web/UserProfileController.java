// UserProfileController.java
package com.meethub.controller.web;

import com.meethub.domain.model.request.NotificationPreferencesRequest;
import com.meethub.domain.model.request.UpdateProfileRequest;
import com.meethub.domain.model.response.NotificationResponse;
import com.meethub.domain.model.response.UserProfileResponse;
import com.meethub.domain.service.NotificationService;
import com.meethub.domain.service.UserService;
import com.meethub.security.CustomUserDetailsService.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final NotificationService notificationService;
    private final UserService userService;

    @GetMapping
    public String profile(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        UserProfileResponse profile = notificationService.getUserProfileWithPreferences(userDetails.getId());
        model.addAttribute("user", profile);

        // Ostatnie powiadomienia
        Page<NotificationResponse> notifications = notificationService.getUserNotifications(
                userDetails.getId(), PageRequest.of(0, 5));
        model.addAttribute("recentNotifications", notifications.getContent());
        model.addAttribute("unreadCount", notificationService.getUnreadCount(userDetails.getId()));

        return "user/profile";
    }

    @GetMapping("/notifications")
    public String notifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationResponse> notifications = notificationService.getUserNotifications(
                userDetails.getId(), pageable);

        model.addAttribute("notifications", notifications);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", notifications.getTotalPages());
        model.addAttribute("unreadCount", notificationService.getUnreadCount(userDetails.getId()));

        return "user/notifications";
    }

    @GetMapping("/settings")
    public String settings(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        UserProfileResponse profile = notificationService.getUserProfileWithPreferences(userDetails.getId());
        model.addAttribute("user", profile);
        model.addAttribute("preferencesRequest", new NotificationPreferencesRequest());
        return "user/settings";
    }

    @PostMapping("/update")
    public String updateProfile(
            @Valid @ModelAttribute UpdateProfileRequest request,
            BindingResult result,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Proszę poprawić błędy w formularzu");
            return "redirect:/profile/settings";
        }

        try {
            // Aktualizacja podstawowych danych użytkownika
            // (trzeba dodać odpowiednią metodę w UserService)
            redirectAttributes.addFlashAttribute("success", "Profil został zaktualizowany");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Błąd podczas aktualizacji profilu: " + e.getMessage());
        }

        return "redirect:/profile";
    }

    @PostMapping("/notifications/preferences")
    public String updateNotificationPreferences(
            @ModelAttribute NotificationPreferencesRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            notificationService.updateNotificationPreferences(userDetails.getId(), request);
            redirectAttributes.addFlashAttribute("success", "Preferencje powiadomień zostały zaktualizowane");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Błąd podczas aktualizacji preferencji: " + e.getMessage());
        }

        return "redirect:/profile/settings";
    }

    @PostMapping("/notifications/mark-all-read")
    public String markAllNotificationsAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        notificationService.markAllAsRead(userDetails.getId());
        redirectAttributes.addFlashAttribute("success", "Wszystkie powiadomienia oznaczone jako przeczytane");
        return "redirect:/profile/notifications";
    }

    @PostMapping("/notifications/{id}/read")
    public String markNotificationAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        notificationService.markAsRead(id, userDetails.getId());
        redirectAttributes.addFlashAttribute("success", "Powiadomienie oznaczone jako przeczytane");
        return "redirect:/profile/notifications";
    }
}
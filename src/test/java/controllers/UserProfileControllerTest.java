package com.meethub.controller.web;

import com.meethub.domain.model.request.NotificationPreferencesRequest;
import com.meethub.domain.model.request.NotificationsListRequest;
import com.meethub.domain.model.request.UpdateProfileRequest;
import com.meethub.domain.model.response.NotificationResponse;
import com.meethub.domain.model.response.UserProfileResponse;
import com.meethub.domain.service.NotificationService;
import com.meethub.domain.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileControllerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserService userService;

    @Mock
    private Model model;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private RedirectAttributes redirectAttributes;

    @Mock
    private Principal principal;

    @InjectMocks
    private UserProfileController controller;

    private final Long userId = 1L;
    private final String userEmail = "test@example.com";
    private NotificationsListRequest notificationsListRequest;

    @BeforeEach
    void setUp() {
        lenient().when(principal.getName()).thenReturn(userEmail);

        // Inicjalizacja DTO dla testów
        notificationsListRequest = new NotificationsListRequest();
        notificationsListRequest.setPage(0);
        notificationsListRequest.setSize(20);
    }

    @Test
    void testProfile_Success() {
        when(userService.getUserIdByEmail(userEmail)).thenReturn(userId);

        UserProfileResponse profile = new UserProfileResponse();
        when(notificationService.getUserProfileWithPreferences(userId)).thenReturn(profile);

        List<NotificationResponse> notifications = Collections.emptyList();
        Page<NotificationResponse> notificationsPage = new PageImpl<>(notifications);
        when(notificationService.getUserNotifications(eq(userId), any(Pageable.class)))
                .thenReturn(notificationsPage);

        when(notificationService.getUnreadCount(userId)).thenReturn(5L);

        String viewName = controller.profile(principal, model);

        assertEquals("user/profile", viewName);

        verify(model).addAttribute("user", profile);
        verify(model).addAttribute("recentNotifications", notifications);
        verify(model).addAttribute("unreadCount", 5L);
    }

    @Test
    void testProfile_PrincipalNull() {
        String viewName = controller.profile(null, model);

        assertEquals("redirect:/login", viewName);
        verifyNoInteractions(userService, notificationService);
    }

    @Test
    void testNotifications_Success() {
        when(userService.getUserIdByEmail(userEmail)).thenReturn(userId);
        when(bindingResult.hasErrors()).thenReturn(false);

        List<NotificationResponse> notifications = Collections.emptyList();
        Page<NotificationResponse> notificationsPage = new PageImpl<>(notifications, PageRequest.of(0, 20), 100);
        when(notificationService.getUserNotifications(eq(userId), any(Pageable.class)))
                .thenReturn(notificationsPage);

        when(notificationService.getUnreadCount(userId)).thenReturn(3L);

        String viewName = controller.notifications(notificationsListRequest, bindingResult, principal, model);

        assertEquals("user/notifications", viewName);

        verify(model).addAttribute("notifications", notificationsPage);
        verify(model).addAttribute("currentPage", 0);
        verify(model).addAttribute("totalPages", notificationsPage.getTotalPages());
        verify(model).addAttribute("unreadCount", 3L);
    }

    @Test
    void testNotifications_PrincipalNull() {
        String viewName = controller.notifications(notificationsListRequest, bindingResult, null, model);

        assertEquals("redirect:/login", viewName);
        verifyNoInteractions(userService, notificationService);
    }

    @Test
    void testNotifications_ValidationErrors() {
        when(bindingResult.hasErrors()).thenReturn(true);

        String viewName = controller.notifications(notificationsListRequest, bindingResult, principal, model);

        assertEquals("user/notifications", viewName);
        verify(model).addAttribute("error", "Nieprawidłowe parametry paginacji");
        verifyNoInteractions(userService, notificationService);
    }

    @Test
    void testNotifications_WithDifferentPageAndSize() {
        // Ustaw inne wartości w DTO
        notificationsListRequest.setPage(2);
        notificationsListRequest.setSize(10);

        when(userService.getUserIdByEmail(userEmail)).thenReturn(userId);
        when(bindingResult.hasErrors()).thenReturn(false);

        List<NotificationResponse> notifications = List.of(
                new NotificationResponse(),
                new NotificationResponse()
        );
        Page<NotificationResponse> notificationsPage = new PageImpl<>(notifications, PageRequest.of(2, 10), 50);
        when(notificationService.getUserNotifications(eq(userId), any(Pageable.class)))
                .thenReturn(notificationsPage);

        when(notificationService.getUnreadCount(userId)).thenReturn(2L);

        String viewName = controller.notifications(notificationsListRequest, bindingResult, principal, model);

        assertEquals("user/notifications", viewName);

        // Sprawdź czy użyto poprawne parametry paginacji
        verify(notificationService).getUserNotifications(eq(userId), argThat(pageable ->
                pageable.getPageNumber() == 2 && pageable.getPageSize() == 10
        ));

        verify(model).addAttribute("currentPage", 2);
    }

    @Test
    void testNotifications_PageOutOfRange() {
        notificationsListRequest.setPage(10); // Wysoka strona

        when(userService.getUserIdByEmail(userEmail)).thenReturn(userId);
        when(bindingResult.hasErrors()).thenReturn(false);

        Page<NotificationResponse> emptyPage = Page.empty();
        when(notificationService.getUserNotifications(eq(userId), any(Pageable.class)))
                .thenReturn(emptyPage);

        when(notificationService.getUnreadCount(userId)).thenReturn(0L);

        String viewName = controller.notifications(notificationsListRequest, bindingResult, principal, model);

        assertEquals("user/notifications", viewName);
        verify(model).addAttribute("notifications", emptyPage);
        verify(model).addAttribute("currentPage", 10);
    }

    @Test
    void testSettings_Success() {
        when(userService.getUserIdByEmail(userEmail)).thenReturn(userId);

        UserProfileResponse profile = new UserProfileResponse();
        when(notificationService.getUserProfileWithPreferences(userId)).thenReturn(profile);

        String viewName = controller.settings(principal, model);

        assertEquals("user/settings", viewName);

        verify(model).addAttribute("user", profile);
        verify(model).addAttribute(eq("preferencesRequest"), any(NotificationPreferencesRequest.class));
        verify(model).addAttribute(eq("updateProfileRequest"), any(UpdateProfileRequest.class));
    }

    @Test
    void testSettings_PrincipalNull() {
        String viewName = controller.settings(null, model);

        assertEquals("redirect:/login", viewName);
        verifyNoInteractions(userService, notificationService);
    }

    @Test
    void testUpdateProfile_Success() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        when(bindingResult.hasErrors()).thenReturn(false);
        when(userService.getUserIdByEmail(userEmail)).thenReturn(userId);

        String redirect = controller.updateProfile(request, bindingResult, principal, redirectAttributes);

        assertEquals("redirect:/profile", redirect);
        verify(redirectAttributes).addFlashAttribute("success", "Profil został zaktualizowany");
    }

    @Test
    void testUpdateProfile_PrincipalNull() {
        UpdateProfileRequest request = new UpdateProfileRequest();

        String redirect = controller.updateProfile(request, bindingResult, null, redirectAttributes);

        assertEquals("redirect:/login", redirect);
        verifyNoInteractions(userService);
    }

    @Test
    void testUpdateProfile_ValidationErrors() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        when(bindingResult.hasErrors()).thenReturn(true);

        String redirect = controller.updateProfile(request, bindingResult, principal, redirectAttributes);

        assertEquals("redirect:/profile/settings", redirect);
        verify(redirectAttributes).addFlashAttribute("error", "Proszę poprawić błędy w formularzu");
        verifyNoInteractions(userService);
    }


    @Test
    void testUpdateNotificationPreferences_Success() {
        NotificationPreferencesRequest request = new NotificationPreferencesRequest();
        when(bindingResult.hasErrors()).thenReturn(false);
        when(userService.getUserIdByEmail(userEmail)).thenReturn(userId);

        String redirect = controller.updateNotificationPreferences(request, bindingResult, principal, model, redirectAttributes);

        assertEquals("redirect:/profile/settings", redirect);
        verify(notificationService).updateNotificationPreferences(userId, request);
        verify(redirectAttributes).addFlashAttribute("success", "Preferencje powiadomień zostały zaktualizowane");
    }

    @Test
    void testUpdateNotificationPreferences_PrincipalNull() {
        NotificationPreferencesRequest request = new NotificationPreferencesRequest();

        String redirect = controller.updateNotificationPreferences(request, bindingResult, null, model, redirectAttributes);

        assertEquals("redirect:/login", redirect);
        verifyNoInteractions(userService, notificationService);
    }

    @Test
    void testUpdateNotificationPreferences_ValidationErrors() {
        NotificationPreferencesRequest request = new NotificationPreferencesRequest();
        when(bindingResult.hasErrors()).thenReturn(true);
        when(userService.getUserIdByEmail(userEmail)).thenReturn(userId);

        UserProfileResponse profile = new UserProfileResponse();
        when(notificationService.getUserProfileWithPreferences(userId)).thenReturn(profile);

        String viewName = controller.updateNotificationPreferences(request, bindingResult, principal, model, redirectAttributes);

        assertEquals("user/settings", viewName);

        verify(model).addAttribute("user", profile);
        verify(model).addAttribute(eq("updateProfileRequest"), any(UpdateProfileRequest.class));
        verify(model).addAttribute("error", "Proszę poprawić błędy w preferencjach powiadomień");
        verifyNoInteractions(redirectAttributes);
    }

    @Test
    void testUpdateNotificationPreferences_ServiceException() {
        NotificationPreferencesRequest request = new NotificationPreferencesRequest();
        when(bindingResult.hasErrors()).thenReturn(false);
        when(userService.getUserIdByEmail(userEmail)).thenReturn(userId);

        doThrow(new RuntimeException("Service error")).when(notificationService)
                .updateNotificationPreferences(anyLong(), any());

        String redirect = controller.updateNotificationPreferences(request, bindingResult, principal, model, redirectAttributes);

        assertEquals("redirect:/profile/settings", redirect);
        verify(redirectAttributes).addFlashAttribute("error", "Błąd podczas aktualizacji preferencji: Service error");
    }

    @Test
    void testMarkAllNotificationsAsRead_Success() {
        when(userService.getUserIdByEmail(userEmail)).thenReturn(userId);

        String redirect = controller.markAllNotificationsAsRead(principal, redirectAttributes);

        assertEquals("redirect:/profile/notifications", redirect);
        verify(notificationService).markAllAsRead(userId);
        verify(redirectAttributes).addFlashAttribute("success", "Wszystkie powiadomienia oznaczone jako przeczytane");
    }

    @Test
    void testMarkAllNotificationsAsRead_PrincipalNull() {
        String redirect = controller.markAllNotificationsAsRead(null, redirectAttributes);

        assertEquals("redirect:/login", redirect);
        verifyNoInteractions(userService, notificationService);
    }

    @Test
    void testMarkNotificationAsRead_Success() {
        Long notificationId = 1L;
        when(userService.getUserIdByEmail(userEmail)).thenReturn(userId);

        String redirect = controller.markNotificationAsRead(notificationId, principal, redirectAttributes);

        assertEquals("redirect:/profile/notifications", redirect);
        verify(notificationService).markAsRead(notificationId, userId);
        verify(redirectAttributes).addFlashAttribute("success", "Powiadomienie oznaczone jako przeczytane");
    }

    @Test
    void testMarkNotificationAsRead_PrincipalNull() {
        Long notificationId = 1L;

        String redirect = controller.markNotificationAsRead(notificationId, null, redirectAttributes);

        assertEquals("redirect:/login", redirect);
        verifyNoInteractions(userService, notificationService);
    }


    @Test
    void testNotifications_EdgeCases() {
        // Test dla minimalnych wartości
        notificationsListRequest.setPage(0);
        notificationsListRequest.setSize(1);

        when(userService.getUserIdByEmail(userEmail)).thenReturn(userId);
        when(bindingResult.hasErrors()).thenReturn(false);

        List<NotificationResponse> singleNotification = List.of(new NotificationResponse());
        Page<NotificationResponse> notificationsPage = new PageImpl<>(singleNotification, PageRequest.of(0, 1), 1);
        when(notificationService.getUserNotifications(eq(userId), any(Pageable.class)))
                .thenReturn(notificationsPage);

        when(notificationService.getUnreadCount(userId)).thenReturn(1L);

        String viewName = controller.notifications(notificationsListRequest, bindingResult, principal, model);

        assertEquals("user/notifications", viewName);

        // Test sprawdza czy działa z size=1
        verify(notificationService).getUserNotifications(eq(userId), argThat(pageable ->
                pageable.getPageSize() == 1
        ));
    }

}
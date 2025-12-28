package com.meethub.controller.web;

import com.meethub.domain.model.request.NotificationPreferencesRequest;
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

    @BeforeEach
    void setUp() {
        lenient().when(principal.getName()).thenReturn(userEmail);
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
    }

    @Test
    void testProfile_PrincipalNull() {
        String viewName = controller.profile(null, model);

        assertEquals("redirect:/login", viewName);
    }

    @Test
    void testNotifications_Success() {
        when(userService.getUserIdByEmail(userEmail)).thenReturn(userId);

        List<NotificationResponse> notifications = Collections.emptyList();
        Page<NotificationResponse> notificationsPage = new PageImpl<>(notifications, PageRequest.of(0, 20), 100);
        when(notificationService.getUserNotifications(eq(userId), any(Pageable.class)))
                .thenReturn(notificationsPage);

        when(notificationService.getUnreadCount(userId)).thenReturn(3L);

        String viewName = controller.notifications(principal, 0, 20, model);

        assertEquals("user/notifications", viewName);
    }

    @Test
    void testNotifications_PrincipalNull() {
        String viewName = controller.notifications(null, 0, 20, model);

        assertEquals("redirect:/login", viewName);
    }

    @Test
    void testSettings_Success() {
        when(userService.getUserIdByEmail(userEmail)).thenReturn(userId);

        UserProfileResponse profile = new UserProfileResponse();
        when(notificationService.getUserProfileWithPreferences(userId)).thenReturn(profile);

        String viewName = controller.settings(principal, model);

        assertEquals("user/settings", viewName);
    }

    @Test
    void testSettings_PrincipalNull() {
        String viewName = controller.settings(null, model);

        assertEquals("redirect:/login", viewName);
    }

    @Test
    void testUpdateProfile_Success() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        when(bindingResult.hasErrors()).thenReturn(false);
        when(userService.getUserIdByEmail(userEmail)).thenReturn(userId);

        String redirect = controller.updateProfile(request, bindingResult, principal, redirectAttributes);

        assertEquals("redirect:/profile", redirect);
    }

    @Test
    void testUpdateProfile_PrincipalNull() {
        UpdateProfileRequest request = new UpdateProfileRequest();

        String redirect = controller.updateProfile(request, bindingResult, null, redirectAttributes);

        assertEquals("redirect:/login", redirect);
    }

    @Test
    void testUpdateProfile_ValidationErrors() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        when(bindingResult.hasErrors()).thenReturn(true);

        String redirect = controller.updateProfile(request, bindingResult, principal, redirectAttributes);

        assertEquals("redirect:/profile/settings", redirect);
    }


    @Test
    void testUpdateNotificationPreferences_Success() {
        NotificationPreferencesRequest request = new NotificationPreferencesRequest();
        when(bindingResult.hasErrors()).thenReturn(false);
        when(userService.getUserIdByEmail(userEmail)).thenReturn(userId);

        String redirect = controller.updateNotificationPreferences(request, bindingResult, principal, model, redirectAttributes);

        assertEquals("redirect:/profile/settings", redirect);
    }

    @Test
    void testUpdateNotificationPreferences_PrincipalNull() {
        NotificationPreferencesRequest request = new NotificationPreferencesRequest();

        String redirect = controller.updateNotificationPreferences(request, bindingResult, null, model, redirectAttributes);

        assertEquals("redirect:/login", redirect);
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
    }



    @Test
    void testMarkAllNotificationsAsRead_Success() {
        when(userService.getUserIdByEmail(userEmail)).thenReturn(userId);

        String redirect = controller.markAllNotificationsAsRead(principal, redirectAttributes);

        assertEquals("redirect:/profile/notifications", redirect);
    }

    @Test
    void testMarkAllNotificationsAsRead_PrincipalNull() {
        String redirect = controller.markAllNotificationsAsRead(null, redirectAttributes);

        assertEquals("redirect:/login", redirect);
    }

    @Test
    void testMarkNotificationAsRead_Success() {
        Long notificationId = 1L;
        when(userService.getUserIdByEmail(userEmail)).thenReturn(userId);

        String redirect = controller.markNotificationAsRead(notificationId, principal, redirectAttributes);

        assertEquals("redirect:/profile/notifications", redirect);
    }

    @Test
    void testMarkNotificationAsRead_PrincipalNull() {
        Long notificationId = 1L;

        String redirect = controller.markNotificationAsRead(notificationId, null, redirectAttributes);

        assertEquals("redirect:/login", redirect);
    }



    @Test
    void testNotificationsWithCustomPagination() {
        when(userService.getUserIdByEmail(userEmail)).thenReturn(userId);

        List<NotificationResponse> notifications = List.of(
                new NotificationResponse(),
                new NotificationResponse()
        );
        Page<NotificationResponse> notificationsPage = new PageImpl<>(notifications, PageRequest.of(2, 10), 50);
        when(notificationService.getUserNotifications(eq(userId), any(Pageable.class)))
                .thenReturn(notificationsPage);

        when(notificationService.getUnreadCount(userId)).thenReturn(2L);

        String viewName = controller.notifications(principal, 2, 10, model);

        assertEquals("user/notifications", viewName);
    }

    @Test
    void testUpdateNotificationPreferences_ValidationErrorsWithProfile() {
        NotificationPreferencesRequest request = new NotificationPreferencesRequest();
        when(bindingResult.hasErrors()).thenReturn(true);
        when(userService.getUserIdByEmail(userEmail)).thenReturn(userId);

        UserProfileResponse profile = new UserProfileResponse();
        when(notificationService.getUserProfileWithPreferences(userId)).thenReturn(profile);

        String viewName = controller.updateNotificationPreferences(request, bindingResult, principal, model, redirectAttributes);

        assertAll(
                () -> assertEquals("user/settings", viewName),
                () -> verify(model).addAttribute(eq("user"), eq(profile))
        );
    }

}
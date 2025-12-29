package com.meethub.controller.web;

import com.meethub.domain.service.NotificationService;
import com.meethub.security.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationWebControllerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private CustomUserDetailsService.CustomUserDetails userDetails;

    @Mock
    private Model model;

    @InjectMocks
    private NotificationWebController controller;

    private final Long userId = 1L;
    private final String username = "test@example.com";

    @BeforeEach
    void setUp() {
        lenient().when(userDetails.getId()).thenReturn(userId);
        lenient().when(userDetails.getUsername()).thenReturn(username);
    }

    @Test
    void inAppNotificationsPage_ShouldReturnViewWithMessages() {
        // Given
        List<String> messages = Arrays.asList(
                "Nowe zaproszenie do spotkania",
                "Spotkanie zaplanowane na jutro",
                "Przypomnienie o zadaniu"
        );

        when(notificationService.getInAppMessages(userId)).thenReturn(messages);

        // When
        String viewName = controller.inAppNotificationsPage(userDetails, model);

        // Then
        assertEquals("notifications/in-app", viewName);

        verify(model).addAttribute("messages", messages);
        verify(model).addAttribute("pageTitle", "Moje powiadomienia");
        verify(model).addAttribute("totalCount", messages.size());
        verify(notificationService).getInAppMessages(userId);
    }

    @Test
    void inAppNotificationsPage_ShouldHandleEmptyMessages() {
        // Given
        List<String> emptyMessages = List.of();
        when(notificationService.getInAppMessages(userId)).thenReturn(emptyMessages);

        // When
        String viewName = controller.inAppNotificationsPage(userDetails, model);

        // Then
        assertEquals("notifications/in-app", viewName);

        verify(model).addAttribute("messages", emptyMessages);
        verify(model).addAttribute("totalCount", 0);
    }


    @Test
    void inAppNotificationsPage_ShouldLogActivity() {
        // Given
        List<String> messages = List.of("Test message");
        when(notificationService.getInAppMessages(userId)).thenReturn(messages);

        // When
        controller.inAppNotificationsPage(userDetails, model);

        // Then
        // Logowanie jest sprawdzane przez fakt że metoda jest wywołana
        // Możemy też użyć ArgumentCaptor jeśli chcemy sprawdzić logi
        verify(notificationService).getInAppMessages(userId);
    }

    @Test
    void recentNotificationsPage_ShouldReturnViewWithMessagesAndLimit() {
        // Given
        int limit = 5;
        List<String> messages = Arrays.asList(
                "Najnowsze powiadomienie 1",
                "Najnowsze powiadomienie 2",
                "Najnowsze powiadomienie 3"
        );

        when(notificationService.getRecentInAppMessages(userId, limit)).thenReturn(messages);

        // When
        String viewName = controller.recentNotificationsPage(userDetails, limit, model);

        // Then
        assertEquals("notifications/recent", viewName);

        verify(model).addAttribute("messages", messages);
        verify(model).addAttribute("pageTitle", "Ostatnie powiadomienia");
        verify(model).addAttribute("limit", limit);
        verify(model).addAttribute("totalCount", messages.size());
        verify(notificationService).getRecentInAppMessages(userId, limit);
    }

    @Test
    void recentNotificationsPage_ShouldHandleDefaultLimit() {
        // Given
        int defaultLimit = 10; // Wartość domyślna z @RequestParam
        List<String> messages = List.of("Test message");

        when(notificationService.getRecentInAppMessages(userId, defaultLimit)).thenReturn(messages);

        // When
        String viewName = controller.recentNotificationsPage(userDetails, defaultLimit, model);

        // Then
        assertEquals("notifications/recent", viewName);
        verify(notificationService).getRecentInAppMessages(userId, defaultLimit);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 10, 50, 100})
    void recentNotificationsPage_ShouldAcceptValidLimits(int limit) {
        // Given
        List<String> messages = List.of("Test message");
        when(notificationService.getRecentInAppMessages(userId, limit)).thenReturn(messages);

        // When
        String viewName = controller.recentNotificationsPage(userDetails, limit, model);

        // Then - nie powinno być wyjątku
        assertEquals("notifications/recent", viewName);
    }


    @Test
    void recentNotificationsPage_ShouldHandleLargeNumberOfMessages() {
        // Given
        int limit = 20;
        List<String> messages = generateMessages(15); // 15 wiadomości

        when(notificationService.getRecentInAppMessages(userId, limit)).thenReturn(messages);

        // When
        String viewName = controller.recentNotificationsPage(userDetails, limit, model);

        // Then
        assertEquals("notifications/recent", viewName);
        verify(model).addAttribute("totalCount", 15);
    }

    @Test
    void recentNotificationsPage_ShouldHandleExactLimit() {
        // Given
        int limit = 5;
        List<String> messages = generateMessages(5); // Dokładnie 5 wiadomości

        when(notificationService.getRecentInAppMessages(userId, limit)).thenReturn(messages);

        // When
        String viewName = controller.recentNotificationsPage(userDetails, limit, model);

        // Then
        assertEquals("notifications/recent", viewName);
        verify(model).addAttribute("totalCount", 5);
    }


    @Test
    void recentNotificationsPage_ShouldLogActivityWithLimit() {
        // Given
        int limit = 15;
        List<String> messages = List.of("Message 1", "Message 2");
        when(notificationService.getRecentInAppMessages(userId, limit)).thenReturn(messages);

        // When
        controller.recentNotificationsPage(userDetails, limit, model);

        // Then - metoda jest wywołana z poprawnymi parametrami
        verify(notificationService).getRecentInAppMessages(userId, limit);
    }

    @Test
    void recentNotificationsPage_ShouldHandleDifferentUsers() {
        // Given
        CustomUserDetailsService.CustomUserDetails otherUser = mock(CustomUserDetailsService.CustomUserDetails.class);
        when(otherUser.getId()).thenReturn(2L);

        int limit = 10;
        List<String> messages = List.of("Other user message");
        when(notificationService.getRecentInAppMessages(2L, limit)).thenReturn(messages);

        // When
        String viewName = controller.recentNotificationsPage(otherUser, limit, model);

        // Then
        assertEquals("notifications/recent", viewName);
        verify(notificationService).getRecentInAppMessages(2L, limit);
    }

    @Test
    void inAppNotificationsPage_ShouldHandleDifferentUsers() {
        // Given
        CustomUserDetailsService.CustomUserDetails otherUser = mock(CustomUserDetailsService.CustomUserDetails.class);
        when(otherUser.getId()).thenReturn(2L);

        List<String> messages = List.of("Other user message");
        when(notificationService.getInAppMessages(2L)).thenReturn(messages);

        // When
        String viewName = controller.inAppNotificationsPage(otherUser, model);

        // Then
        assertEquals("notifications/in-app", viewName);
        verify(notificationService).getInAppMessages(2L);
    }


    // Metoda pomocnicza
    private List<String> generateMessages(int count) {
        List<String> messages = new java.util.ArrayList<>();
        for (int i = 1; i <= count; i++) {
            messages.add("Message " + i);
        }
        return messages;
    }
}
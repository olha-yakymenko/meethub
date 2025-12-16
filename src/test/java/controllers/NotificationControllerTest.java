package com.meethub.controller.api;

import com.meethub.domain.service.NotificationService;
import com.meethub.security.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

//    @Test
//    @WithMockUser(username = "user@example.com")
//    void getInAppMessages_ShouldReturnMessages() throws Exception {
//        // Given
//        List<String> messages = Arrays.asList(
//                "Meeting reminder: Team sync at 10 AM",
//                "New feedback received",
//                "Meeting invitation: Project Review"
//        );
//
//        when(notificationService.getInAppMessages(1L)).thenReturn(messages);
//
//        // When & Then
//        mockMvc.perform(get("/api/v1/notifications/in-app/messages"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$").isArray())
//                .andExpect(jsonPath("$.length()").value(3))
//                .andExpect(jsonPath("$[0]").value("Meeting reminder: Team sync at 10 AM"));
//    }

//    @Test
//    void getInAppMessages_ShouldReturnUnauthorized_WhenNotAuthenticated() throws Exception {
//        // When & Then
//        mockMvc.perform(get("/api/v1/notifications/in-app/messages"))
//                .andExpect(status().isUnauthorized());
//    }

//    @Test
//    @WithMockUser(username = "user@example.com")
//    void getRecentInAppMessages_ShouldReturnLimitedMessages() throws Exception {
//        // Given
//        List<String> messages = Arrays.asList("Message 1", "Message 2", "Message 3");
//        when(notificationService.getRecentInAppMessages(1L, 3)).thenReturn(messages);
//
//        // When & Then
//        mockMvc.perform(get("/api/v1/notifications/in-app/messages/recent")
//                        .param("limit", "3"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.length()").value(3));
//    }
//
//    @Test
//    @WithMockUser(username = "user@example.com")
//    void getRecentInAppMessages_ShouldUseDefaultLimit() throws Exception {
//        // Given
//        List<String> messages = Arrays.asList("Message 1", "Message 2");
//        when(notificationService.getRecentInAppMessages(1L, 10)).thenReturn(messages);
//
//        // When & Then
//        mockMvc.perform(get("/api/v1/notifications/in-app/messages/recent"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.length()").value(2));
//    }

    @Test
    @WithMockUser(username = "user@example.com")
    void getRecentInAppMessages_ShouldValidateLimitParameter() throws Exception {
        // When & Then - Limit too high
        mockMvc.perform(get("/api/v1/notifications/in-app/messages/recent")
                        .param("limit", "101")) // Max is 100
                .andExpect(status().isBadRequest());

        // When & Then - Limit too low
        mockMvc.perform(get("/api/v1/notifications/in-app/messages/recent")
                        .param("limit", "0")) // Min is 1
                .andExpect(status().isBadRequest());

        verify(notificationService, never()).getRecentInAppMessages(anyLong(), anyInt());
    }
}
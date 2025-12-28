package com.meethub.controller.api;

import com.meethub.domain.service.NotificationService;
import com.meethub.security.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import security.WithCustomUser;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    // ===================== AUTHORIZED =====================

    @Test
    @WithCustomUser(id = 1L, email = "user@example.com")
    void getInAppMessages_ShouldReturnMessages() throws Exception {
        // Given
        List<String> messages = Arrays.asList(
                "Meeting reminder: Team sync at 10 AM",
                "New feedback received",
                "Meeting invitation: Project Review"
        );

        when(notificationService.getInAppMessages(1L)).thenReturn(messages);

        // When & Then
        mockMvc.perform(get("/api/v1/notifications/in-app/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0]").value("Meeting reminder: Team sync at 10 AM"));
    }

    @Test
    @WithCustomUser(id = 1L)
    void getRecentInAppMessages_ShouldReturnLimitedMessages() throws Exception {
        // Given
        List<String> messages = Arrays.asList("Message 1", "Message 2", "Message 3");
        when(notificationService.getRecentInAppMessages(1L, 3)).thenReturn(messages);

        // When & Then
        mockMvc.perform(get("/api/v1/notifications/in-app/messages/recent")
                        .param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @WithCustomUser(id = 1L)
    void getRecentInAppMessages_ShouldValidateLimitParameter() throws Exception {
        // limit > 100
        mockMvc.perform(get("/api/v1/notifications/in-app/messages/recent")
                        .param("limit", "101"))
                .andExpect(status().isBadRequest());

        // limit < 1
        mockMvc.perform(get("/api/v1/notifications/in-app/messages/recent")
                        .param("limit", "0"))
                .andExpect(status().isBadRequest());

        verify(notificationService, never())
                .getRecentInAppMessages(anyLong(), anyInt());
    }

}

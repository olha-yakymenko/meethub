package com.meethub.controller.web;

import com.meethub.domain.service.NotificationService;
import com.meethub.security.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import security.WithCustomUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationWebController.class)
class NotificationWebControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithCustomUser(id=1L, email = "test@example.com")
    void recentNotificationsPage_ShouldRedirect_WhenLimitTooLow() throws Exception {
        mockMvc.perform(get("/notifications/in-app/recent")
                        .param("limit", "0"))
                .andExpect(status().is3xxRedirection()) // 302 zamiast 400
                .andExpect(redirectedUrl("/meetings"))
                .andExpect(flash().attributeExists("error"))
                .andExpect(flash().attribute("error", "Limit musi być co najmniej 1"));
    }

    @Test
    @WithCustomUser(id=1L, email = "test@example.com")
    void recentNotificationsPage_ShouldRedirect_WhenLimitTooHigh() throws Exception {
        mockMvc.perform(get("/notifications/in-app/recent")
                        .param("limit", "101"))
                .andExpect(status().is3xxRedirection()) // 302 zamiast 400
                .andExpect(redirectedUrl("/meetings"))
                .andExpect(flash().attributeExists("error"))
                .andExpect(flash().attribute("error", "Limit nie może przekraczać 100"));
    }


    @Test
    @WithCustomUser(id=1L, email = "test@example.com")
    void recentNotificationsPage_ShouldReturnView_WhenValidLimit() throws Exception {
        mockMvc.perform(get("/notifications/in-app/recent")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("notifications/recent"));
    }

    @Test
    @WithCustomUser(id=1L, email = "test@example.com")
    void inAppNotificationsPage_ShouldReturnView_WhenUserAuthenticated() throws Exception {
        mockMvc.perform(get("/notifications/in-app"))
                .andExpect(status().isOk())
                .andExpect(view().name("notifications/in-app"));
    }


}
package com.meethub.controller.web;

import com.meethub.domain.model.entity.User;
import com.meethub.domain.service.MeetingService;
import com.meethub.exception.BusinessException;
import com.meethub.security.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import security.WithCustomUser;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class WebControllerErrorHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MeetingService meetingService;


    @Test
    @WithCustomUser(id = 1L, email = "user@example.com")
    void meetingDetails_shouldHandleMeetingNotFound() throws Exception {
        // Given
        Long nonExistentId = 999L;
        when(meetingService.getMeetingById(nonExistentId))
                .thenThrow(new RuntimeException("Meeting not found"));

        // When & Then
        mockMvc.perform(get("/meetings/{id}", nonExistentId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/meetings"));}

    @Test
    @WithCustomUser(id = 1L, email = "user@example.com")
    void attendMeeting_shouldHandleInvalidToken() throws Exception {
        // Given
        String invalidToken = "invalid-token-123";

        // When & Then
        mockMvc.perform(post("/meetings/{id}/attend", 1L)
                        .param("token", invalidToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/meetings/1"))
                .andExpect(flash().attributeExists("error"));
    }
}
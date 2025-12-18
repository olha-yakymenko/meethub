package com.meethub.controller.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.request.MeetingResourceRequest;
import com.meethub.domain.model.response.MeetingResourceResponse;
import com.meethub.security.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import security.WithCustomUser;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MeetingResourceWebControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private com.meethub.domain.service.MeetingService meetingService;

    @MockBean
    private com.meethub.domain.service.MeetingResourceService meetingResourceService;

    @Autowired
    private ObjectMapper objectMapper;

    private com.meethub.domain.model.entity.Meeting mockMeeting;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Setup mock meeting
        mockMeeting = new com.meethub.domain.model.entity.Meeting();
        mockMeeting.setId(100L);
        mockMeeting.setTitle("Test Meeting");

            mockMvc = MockMvcBuilders
                    .webAppContextSetup(context)
                    .apply(springSecurity())
                    .build();
    }

    // ==================== TESTS FOR SHOW ADD RESOURCE FORM ====================

    @Test
    @WithCustomUser(id = 1L, email = "user@example.com")
    void showAddResourceForm_shouldReturnFormPage() throws Exception {
        // Given
        Long meetingId = 100L;
        when(meetingService.getMeeting(meetingId)).thenReturn(mockMeeting);

        // When & Then
        mockMvc.perform(get("/meetings/{meetingId}/resources/add", meetingId))
                .andExpect(status().isOk())
                .andExpect(view().name("meetings/resources/add-resource"))
                .andExpect(model().attributeExists("meeting"))
                .andExpect(model().attributeExists("meetingResourceRequest"));
    }

    @Test
    @WithCustomUser(id = 1L, email = "user@example.com")
    void getMeetingResources_shouldHandleEmptyResourcesList() throws Exception {
        // Given
        Long meetingId = 100L;
        when(meetingService.getMeeting(meetingId)).thenReturn(mockMeeting);
        when(meetingResourceService.getMeetingResources(meetingId, 1L))
                .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/meetings/{meetingId}/resources", meetingId))
                .andExpect(status().isOk())
                .andExpect(view().name("meetings/resources/resources-list"))
                .andExpect(model().attribute("resourcesCount", 0));
    }

    // ==================== TESTS FOR DELETE RESOURCE ====================

    @Test
    @WithCustomUser(id = 1L, email = "user@example.com")
    void deleteResource_shouldDeleteSuccessfully() throws Exception {
        // Given
        Long meetingId = 100L;
        Long resourceId = 200L;

        // When & Then
        mockMvc.perform(post("/meetings/{meetingId}/resources/{resourceId}/delete",
                        meetingId, resourceId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/meetings/" + meetingId + "/resources"))
                .andExpect(flash().attributeExists("success"));
    }

    // ==================== TESTS FOR DOWNLOAD RESOURCE PAGE ====================

    @Test
    @WithCustomUser(id = 1L, email = "user@example.com")
    void downloadResourcePage_shouldRedirectToApi() throws Exception {
        // Given
        Long meetingId = 100L;
        Long resourceId = 200L;

        // When & Then
        mockMvc.perform(get("/meetings/{meetingId}/resources/{resourceId}/download",
                        meetingId, resourceId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/api/meetings/" + meetingId + "/resources/" + resourceId + "/download"));
    }

    // ==================== TESTS FOR PREVIEW RESOURCE PAGE ====================

    @Test
    @WithCustomUser(id = 1L, email = "user@example.com")
    void previewResourcePage_shouldRedirectToApi() throws Exception {
        // Given
        Long meetingId = 100L;
        Long resourceId = 200L;

        // When & Then
        mockMvc.perform(get("/meetings/{meetingId}/resources/{resourceId}/preview",
                        meetingId, resourceId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/api/meetings/" + meetingId + "/resources/" + resourceId + "/preview"));
    }

    // ==================== TESTS FOR SHOW RESOURCE DETAILS ====================

    @Test
    @WithCustomUser(id = 1L, email = "user@example.com")
    void showResourceDetails_shouldHandleResourceNotFound() throws Exception {
        // Given
        Long meetingId = 100L;
        Long resourceId = 999L;
        when(meetingService.getMeeting(meetingId)).thenReturn(mockMeeting);
        when(meetingResourceService.getResource(resourceId, 1L))
                .thenThrow(new RuntimeException("Resource not found"));

        // When & Then
        mockMvc.perform(get("/meetings/{meetingId}/resources/{resourceId}/details",
                        meetingId, resourceId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/meetings/" + meetingId + "/resources?error=Nie można wyświetlić szczegółów zasobu"));
    }

    // ==================== TESTS FOR TOGGLE RESOURCE VISIBILITY ====================

    @Test
    @WithCustomUser(id = 1L, email = "user@example.com")
    void toggleResourceVisibility_shouldRedirectToDetails() throws Exception {
        // Given
        Long meetingId = 100L;
        Long resourceId = 200L;

        // When & Then
        mockMvc.perform(post("/meetings/{meetingId}/resources/{resourceId}/toggle-visibility",
                        meetingId, resourceId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/meetings/" + meetingId + "/resources/" + resourceId + "/details"))
                .andExpect(flash().attributeExists("success"));
    }

    // ==================== TESTS FOR ERROR HANDLING ====================

    @Test
    @WithCustomUser(id = 1L, email = "user@example.com")
    void endpoints_shouldHandleServiceExceptionsGracefully() throws Exception {
        // Test various endpoints that might throw exceptions

        // Test getMeetingResources with service exception
        Long meetingId = 100L;
        when(meetingService.getMeeting(meetingId))
                .thenThrow(new RuntimeException("Database connection failed"));

        mockMvc.perform(get("/meetings/{meetingId}/resources", meetingId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/meetings?error=Nie udało się pobrać zasobów"));
    }

}
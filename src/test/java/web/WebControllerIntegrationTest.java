package com.meethub.controller.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.MeetingType;
import com.meethub.domain.model.request.CreateMeetingRequest;
import com.meethub.domain.model.request.UpdateMeetingRequest;
import com.meethub.domain.model.response.MeetingResponse;
import com.meethub.domain.model.response.UserResponse;
import com.meethub.domain.service.MeetingService;
import com.meethub.domain.service.UserService;
import com.meethub.security.CustomUserDetailsService.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import security.WithCustomUser;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private MeetingService meetingService;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithCustomUser(id = 1L, email = "user@example.com")
    void home_shouldReturnDashboard_whenAuthenticated() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    @WithCustomUser(id = 1L, email = "user@example.com")
    void home_shouldReturnIndex_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));
    }

    @Test
    @WithCustomUser(id = 1L, email = "user@example.com")
    void createMeeting_shouldReturnCreateForm() throws Exception {
        mockMvc.perform(get("/meetings/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("meetings/create"))
                .andExpect(model().attributeExists("createMeetingRequest"));
    }

    @Test
    @WithCustomUser(id = 1L, email = "user@example.com")
    void joinMeeting_shouldJoinAndRedirect() throws Exception {
        mockMvc.perform(post("/meetings/{id}/join", 1L))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/meetings/1"));
    }

    @Test
    @WithCustomUser(id = 1L, email = "user@example.com")
    void templates_shouldReturnTemplatesList() throws Exception {
        // Given
        List<MeetingResponse> templates = Arrays.asList(
                MeetingResponse.builder().id(1L).title("Template 1").isTemplate(true).build(),
                MeetingResponse.builder().id(2L).title("Template 2").isTemplate(true).build()
        );

        when(meetingService.getMeetingTemplates(anyLong())).thenReturn(templates);

        // When & Then
        mockMvc.perform(get("/meetings/templates"))
                .andExpect(status().isOk())
                .andExpect(view().name("meetings/templates"))
                .andExpect(model().attributeExists("templates"));
    }

    @Test
    @WithCustomUser(id = 1L, email = "user@example.com")
    void searchUsers_shouldReturnJson() throws Exception {
        // Given
        List<UserResponse> users = Arrays.asList(
                UserResponse.builder().id(1L).firstName("user1").build(),
                UserResponse.builder().id(2L).firstName("user2").build()
        );

        when(userService.searchUsers(anyString())).thenReturn(users);

        // When & Then
        mockMvc.perform(get("/api/users/search")
                        .param("query", "user"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));}

}
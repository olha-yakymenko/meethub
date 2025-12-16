package com.meethub.controller.web;

import com.meethub.domain.model.response.MeetingResponse;
import com.meethub.domain.model.enums.MeetingStatus;
import com.meethub.domain.model.enums.MeetingType;
import com.meethub.domain.model.enums.MeetingVisibility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class WebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private com.meethub.domain.service.MeetingService meetingService;

    @MockBean
    private com.meethub.domain.service.MeetingParticipantService meetingParticipantService;


    @Test
    void testDashboard_Unauthenticated() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    // 2. Testy listy spotkań
    @Test
    @WithMockUser(username = "user@example.com")
    void testMeetingsList_Authenticated() throws Exception {
        // Setup
        MeetingResponse meeting1 = MeetingResponse.builder()
                .id(1L)
                .title("Meeting 1")
                .type(MeetingType.IN_PERSON)
                .status(MeetingStatus.PLANNED)
                .visibility(MeetingVisibility.PUBLIC)
                .build();

        MeetingResponse meeting2 = MeetingResponse.builder()
                .id(2L)
                .title("Meeting 2")
                .type(MeetingType.ONLINE)
                .status(MeetingStatus.PLANNED)
                .visibility(MeetingVisibility.PRIVATE)
                .build();

        List<MeetingResponse> meetings = Arrays.asList(meeting1, meeting2);
        Page<MeetingResponse> mockPage = new PageImpl<>(meetings, PageRequest.of(0, 3), 2);

        when(meetingService.getFilteredMeetings(anyString(), anyString(), anyString(), any()))
                .thenReturn(mockPage);

        when(meetingParticipantService.isConfirmedParticipant(anyLong(), anyLong()))
                .thenReturn(false);
        when(meetingParticipantService.isPendingParticipant(anyLong(), anyLong()))
                .thenReturn(false);
        when(meetingParticipantService.isInvitedParticipant(anyLong(), anyLong()))
                .thenReturn(false);
        when(meetingParticipantService.isDeclinedParticipant(anyLong(), anyLong()))
                .thenReturn(false);
        when(meetingParticipantService.isWaitingListParticipant(anyLong(), anyLong()))
                .thenReturn(false);

        // Test
        mockMvc.perform(get("/meetings")
                        .param("page", "0")
                        .param("size", "3")
                        .param("search", "test")
                        .param("type", "IN_PERSON")
                        .param("status", "SCHEDULED"))
                .andExpect(status().isOk())
                .andExpect(view().name("meetings/list"))
                .andExpect(model().attributeExists("meetings"))
                .andExpect(model().attributeExists("currentPage"))
                .andExpect(model().attributeExists("totalPages"))
                .andExpect(model().attributeExists("searchParam"));
    }

    @Test
    void testMeetingsList_Unauthenticated() throws Exception {
        // Setup - zwraca publiczne spotkania
        MeetingResponse publicMeeting = MeetingResponse.builder()
                .id(1L)
                .title("Public Meeting")
                .visibility(MeetingVisibility.PUBLIC)
                .build();

        when(meetingService.getUpcomingPublicMeetings())
                .thenReturn(Collections.singletonList(publicMeeting));

        mockMvc.perform(get("/meetings"))
                .andExpect(status().isOk())
                .andExpect(view().name("meetings/list"))
                .andExpect(model().attributeExists("meetings"))
                .andExpect(model().attribute("userId", (Object) null));
    }


    @Test
    void testTemplatesPage_Unauthenticated() throws Exception {
        mockMvc.perform(get("/meetings/templates"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(
            username = "poweruser@example.com",
            authorities = {"ROLE_USER", "ROLE_ORGANIZER", "ROLE_MODERATOR"}
    )
    void testUserWithMultipleAuthorities() throws Exception {
        mockMvc.perform(get("/meetings"))
                .andExpect(status().isOk())
                .andExpect(view().name("meetings/list"));
    }

    // 8. Testy walidacji i błędów
    @Test
    @WithMockUser(username = "user@example.com")
    void testMeetingDetails_NotFound() throws Exception {
        when(meetingService.getMeetingById(999L))
                .thenThrow(new RuntimeException("Meeting not found"));

        mockMvc.perform(get("/meetings/999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/meetings"));
    }


    // 9. Testy paginacji
    @Test
    @WithMockUser(username = "user@example.com")
    void testMeetingsWithPagination() throws Exception {
        // Setup puste strony dla testów paginacji
        Page<MeetingResponse> emptyPage = Page.empty(PageRequest.of(10, 10));

        when(meetingService.getFilteredMeetings(anyString(), anyString(), anyString(), any()))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/meetings")
                        .param("page", "10")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("meetings/list"))
                .andExpect(model().attribute("currentPage", 10))
                .andExpect(model().attribute("totalPages", 0));
    }

}
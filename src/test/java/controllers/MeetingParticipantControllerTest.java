//package com.meethub.controller.api;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.meethub.domain.model.enums.ParticipationStatus;
//import com.meethub.domain.model.enums.PermissionLevel;
//import com.meethub.domain.model.projection.ParticipantProjection;
//import com.meethub.domain.model.request.InviteParticipantsRequest;
//import com.meethub.domain.model.response.ParticipantResponse;
//import com.meethub.domain.model.response.UserResponse;
//import com.meethub.domain.service.MeetingParticipantService;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.http.MediaType;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.util.Arrays;
//import java.util.List;
//
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@WebMvcTest(MeetingParticipantController.class)
//class MeetingParticipantControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @MockBean
//    private MeetingParticipantService participantService;
//
//    @Test
//    @WithMockUser(username = "organizer@example.com", roles = {"USER"})
//    void getParticipants_ShouldReturnParticipantsList() throws Exception {
//        // Given
//        ParticipantProjection participant1 = new ParticipantProjection() {
//            @Override public Long getId() { return 1L; }
//            @Override
//            public String getUsername() { return "user1"; }
//            @Override public String getEmail() { return "user1@example.com"; }
//            @Override public ParticipationStatus getStatus() { return ParticipationStatus.CONFIRMED; }
//        };
//
//        List<ParticipantProjection> participants = Arrays.asList(participant1);
//        when(participantService.getMeetingParticipants(1L)).thenReturn(participants);
//
//        // When & Then
//        mockMvc.perform(get("/api/v1/meetings/1/participants"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.data").isArray())
//                .andExpect(jsonPath("$.data[0].email").value("user1@example.com"));
//    }
//
//    @Test
//    @WithMockUser(username = "organizer@example.com", roles = {"USER"})
//    void inviteParticipants_ShouldInviteMultipleUsers() throws Exception {
//        // Given
//        InviteParticipantsRequest request = new InviteParticipantsRequest();
//        request.setUserIds(Arrays.asList(2L, 3L, 4L));
//        request.setMessage("Please join our meeting");
//
//        // When & Then
//        mockMvc.perform(post("/api/v1/meetings/1/participants/invite")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request))
//                        .with(request -> {
//                            request.setAttribute("userId", 1L);
//                            return request;
//                        }))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.message").value("Uczestnicy zaproszeni pomyślnie"));
//
//        verify(participantService).inviteMultipleParticipants(eq(1L), any(InviteParticipantsRequest.class), eq(1L));
//    }
//
//    @Test
//    @WithMockUser(username = "user@example.com", roles = {"USER"})
//    void joinMeeting_ShouldAllowJoiningPublicMeeting() throws Exception {
//        // When & Then
//        mockMvc.perform(post("/api/v1/meetings/1/participants/join")
//                        .with(request -> {
//                            request.setAttribute("userId", 2L);
//                            return request;
//                        }))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true));
//
//        verify(participantService).joinPublicMeeting(1L, 2L);
//    }
//
//    @Test
//    @WithMockUser(username = "organizer@example.com", roles = {"USER"})
//    void updateParticipantStatus_ShouldUpdateStatusWithComment() throws Exception {
//        // When & Then
//        mockMvc.perform(patch("/api/v1/meetings/1/participants/2/status")
//                        .param("status", "CONFIRMED")
//                        .param("comment", "Welcome to the meeting!")
//                        .with(request -> {
//                            request.setAttribute("userId", 1L);
//                            return request;
//                        }))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true));
//
//        verify(participantService).updateParticipantStatus(eq(1L), eq(2L),
//                eq(ParticipationStatus.CONFIRMED), eq("Welcome to the meeting!"), eq(1L));
//    }
//
//    @Test
//    @WithMockUser(username = "organizer@example.com", roles = {"USER"})
//    void updateParticipantPermission_ShouldUpdatePermissionLevel() throws Exception {
//        // When & Then
//        mockMvc.perform(patch("/api/v1/meetings/1/participants/2/permission")
//                        .param("permissionLevel", "CO_ORGANIZER")
//                        .with(request -> {
//                            request.setAttribute("userId", 1L);
//                            return request;
//                        }))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true));
//
//        verify(participantService).updateParticipantPermission(eq(1L), eq(2L),
//                eq(PermissionLevel.CO_ORGANIZER), eq(1L));
//    }
//
//    @Test
//    @WithMockUser(username = "organizer@example.com", roles = {"USER"})
//    void removeParticipant_ShouldRemoveParticipant() throws Exception {
//        // When & Then
//        mockMvc.perform(delete("/api/v1/meetings/1/participants/2")
//                        .with(request -> {
//                            request.setAttribute("userId", 1L);
//                            return request;
//                        }))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true));
//
//        verify(participantService).removeParticipant(1L, 2L, 1L);
//    }
//
//    @Test
//    @WithMockUser(username = "user@example.com", roles = {"USER"})
//    void getUserInvitations_ShouldReturnUserInvitations() throws Exception {
//        // Given
//        ParticipantResponse invitation = new ParticipantResponse();
//        invitation.setId(1L);
//        invitation.setMeetingId(1L);
//        invitation.setMeetingTitle("Team Meeting");
//
//        List<ParticipantResponse> invitations = Arrays.asList(invitation);
//        when(participantService.getUserInvitations(1L)).thenReturn(invitations);
//
//        // When & Then
//        mockMvc.perform(get("/api/v1/meetings/1/participants/invitations")
//                        .with(request -> {
//                            request.setAttribute("userId", 1L);
//                            return request;
//                        }))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.data").isArray());
//    }
//
//    @Test
//    void acceptInvitationByToken_ShouldWorkWithoutAuth() throws Exception {
//        // Given
//        String validToken = "abc123def456ghi789jkl012mno345pqr678stu901vwx234yz567";
//
//        // When & Then
//        mockMvc.perform(post("/api/v1/meetings/1/participants/invitations/{token}/accept", validToken))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true));
//
//        verify(participantService).acceptInvitationByToken(validToken);
//    }
//
//    @Test
//    @WithMockUser(username = "user@example.com", roles = {"USER"})
//    void searchUsers_ShouldReturnFilteredUsers() throws Exception {
//        // Given
//        UserResponse user1 = new UserResponse();
//        user1.setId(2L);
//        user1.setUsername("john_doe");
//
//        List<UserResponse> users = Arrays.asList(user1);
//        when(participantService.searchUsersForInvitation("john", 1L)).thenReturn(users);
//
//        // When & Then
//        mockMvc.perform(get("/api/v1/meetings/1/participants/search-users")
//                        .param("query", "john"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.data[0].username").value("john_doe"));
//    }
//}
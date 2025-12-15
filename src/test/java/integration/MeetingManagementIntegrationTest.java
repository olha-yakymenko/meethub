//package com.meethub.controller.integration;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.meethub.domain.model.entity.User;
//import com.meethub.domain.model.enums.UserRole;
//import com.meethub.domain.model.request.CreateMeetingRequest;
//import com.meethub.domain.model.request.SubmitFeedbackRequest;
//import com.meethub.domain.repository.jpa.UserRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.ResultActions;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//@ActiveProfiles("test")
//@Transactional
//class MeetingManagementIntegrationTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @Autowired
//    private UserRepository userRepository;
//
//    @Autowired
//    private PasswordEncoder passwordEncoder;
//
//    @Autowired
//
//    private String organizerToken;
//    private String participantToken;
//    private Long meetingId;
//
//    @BeforeEach
//    void setUp() {
//        // Create organizer user
//        User organizer = User.builder()
//                .email("organizer@test.com")
//                .password(passwordEncoder.encode("password123"))
//                .role(UserRole.PARTICIPANT)
//                .build();
//        userRepository.save(organizer);
//
//        // Create participant user
//        User participant = User.builder()
//                .email("participant@test.com")
//                .password(passwordEncoder.encode("password123"))
//                .role(UserRole.PARTICIPANT)
//                .build();
//        userRepository.save(participant);
//    }
//
//    @Test
//    void completeMeetingWorkflow_ShouldWorkEndToEnd() throws Exception {
//        // 1. Organizer creates a meeting
//        meetingId = createMeeting();
//
//        // 2. Organizer invites participant
//        inviteParticipant();
//
//        // 3. Participant accepts invitation
//        // (Simulated through direct status update by organizer for simplicity)
//        updateParticipantStatus();
//
//        // 4. Participant submits feedback after meeting
//        submitFeedback();
//
//        // 5. Organizer retrieves meeting feedback
//        getMeetingFeedbacks();
//    }
//
//    private Long createMeeting() throws Exception {
//        CreateMeetingRequest request = new CreateMeetingRequest();
//        request.setTitle("Integration Test Meeting");
//        request.setDescription("End-to-end testing meeting");
//
//
//        String response = mockMvc.perform(post("/api/v1/meetings")
//                        .header("Authorization", "Bearer " + organizerToken)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.data.id").exists())
//                .andReturn()
//                .getResponse()
//                .getContentAsString();
//
//        return objectMapper.readTree(response).path("data").path("id").asLong();
//    }
//
//    private void inviteParticipant() throws Exception {
//        String inviteRequest = """
//            {
//                "userIds": [2],
//                "message": "Please join our meeting"
//            }
//            """;
//
//        mockMvc.perform(post("/api/v1/meetings/{meetingId}/participants/invite", meetingId)
//                        .header("Authorization", "Bearer " + organizerToken)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(inviteRequest))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true));
//    }
//
//    private void updateParticipantStatus() throws Exception {
//        mockMvc.perform(patch("/api/v1/meetings/{meetingId}/participants/{participantId}/status",
//                        meetingId, 2L)
//                        .header("Authorization", "Bearer " + organizerToken)
//                        .param("status", "CONFIRMED")
//                        .param("comment", "Welcome!"))
//                .andExpect(status().isOk());
//    }
//
//    private void submitFeedback() throws Exception {
//        SubmitFeedbackRequest request = SubmitFeedbackRequest.builder()
//                .rating(5)
//                .comment("Excellent integration test meeting!")
//                .build();
//
//        mockMvc.perform(post("/api/v1/feedbacks/meetings/{meetingId}/submit", meetingId)
//                        .header("Authorization", "Bearer " + participantToken)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true));
//    }
//
//    private void getMeetingFeedbacks() throws Exception {
//        mockMvc.perform(get("/api/v1/feedbacks/meetings/{meetingId}", meetingId)
//                        .header("Authorization", "Bearer " + organizerToken))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success").value(true))
//                .andExpect(jsonPath("$.data").isArray())
//                .andExpect(jsonPath("$.data[0].rating").value(5))
//                .andExpect(jsonPath("$.data[0].comment").value("Excellent integration test meeting!"));
//    }
//}
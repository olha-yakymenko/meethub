//package com.meethub.controller.api;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.meethub.domain.model.entity.Feedback;
//import com.meethub.domain.model.entity.Meeting;
//import com.meethub.domain.model.entity.User;
//import com.meethub.domain.model.enums.MeetingStatus;
//import com.meethub.domain.model.enums.UserRole;
//import com.meethub.domain.model.enums.MeetingType;
//import com.meethub.domain.model.enums.MeetingVisibility;
//import com.meethub.domain.model.request.SubmitFeedbackRequest;
//import com.meethub.domain.repository.jpa.MeetingRepository;
//import com.meethub.domain.repository.jpa.UserRepository;
//import com.meethub.domain.service.FeedbackService;
//import com.meethub.security.CustomUserDetailsService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.http.MediaType;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.util.LinkedMultiValueMap;
//import org.springframework.util.MultiValueMap;
//
//import java.time.LocalDateTime;
//import java.util.Arrays;
//import java.util.List;
//
//// Hamcrest matchers for JSON assertions
//import static org.hamcrest.Matchers.containsString;
//import static org.hamcrest.Matchers.hasSize;
//import static org.hamcrest.Matchers.is;
//
//// Mockito matchers (explicit imports to avoid conflicts)
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyLong;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.ArgumentMatchers.eq;
//
//// Mockito verification methods
//import static org.mockito.Mockito.doNothing;
//import static org.mockito.Mockito.doThrow;
//import static org.mockito.Mockito.lenient;
//import static org.mockito.Mockito.never;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//// Spring MVC test methods
//import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//@ActiveProfiles("test")
//@Transactional
//class FeedbackControllerTest {
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
//    private MeetingRepository meetingRepository;
//
//    @MockBean
//    private FeedbackService feedbackService;
//
//    @MockBean
//    private CustomUserDetailsService customUserDetailsService;
//
//    private User testUser;
//    private Meeting testMeeting;
//
//    @BeforeEach
//    void setUp() {
//        // Tworzenie i zapisanie rzeczywistego użytkownika w bazie danych
//        testUser = User.builder()
//                .firstName("John")
//                .lastName("Doe")
//                .email("john.doe@example.com")
//                .password("hashedPassword123")
//                .role(UserRole.PARTICIPANT)
//                .enabled(true)
//                .build();
//        testUser = userRepository.save(testUser);
//
//        // Tworzenie i zapisanie rzeczywistego spotkania w bazie danych
//        testMeeting = Meeting.builder()
//                .title("Test Meeting")
//                .description("Test Description")
//                .startDate(LocalDateTime.now().minusDays(2))
//                .endDate(LocalDateTime.now().minusDays(1))
//                .organizer(testUser)
//                .type(MeetingType.ONLINE)
//                .visibility(MeetingVisibility.PUBLIC)
//                .build();
//        testMeeting = meetingRepository.save(testMeeting);
//
//        // Konfiguracja mocka dla CustomUserDetailsService
//        com.meethub.domain.model.entity.User mockUserEntity = new com.meethub.domain.model.entity.User();
//        mockUserEntity.setId(testUser.getId());
//        mockUserEntity.setEmail(testUser.getEmail());
//        mockUserEntity.setFirstName(testUser.getFirstName());
//        mockUserEntity.setLastName(testUser.getLastName());
//        mockUserEntity.setPassword(testUser.getPassword());
//        mockUserEntity.setRole(testUser.getRole());
//        mockUserEntity.setEnabled(testUser.getEnabled());
//        mockUserEntity.setAccountLockedUntil(null);
//
//        CustomUserDetailsService.CustomUserDetails mockUserDetails =
//                new CustomUserDetailsService.CustomUserDetails(mockUserEntity);
//
//        lenient().when(customUserDetailsService.loadUserByUsername(anyString()))
//                .thenReturn(mockUserDetails);
//    }
//
//    @Test
//    @WithMockUser(username = "john.doe@example.com", roles = "PARTICIPANT")
//    void submitFeedback_ValidRequest_ShouldReturnSuccess() throws Exception {
//        // Given
//        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
//        params.add("rating", "4");
//        params.add("comment", "Great meeting!");
//
//        doNothing().when(feedbackService).submitFeedback(
//                eq(testMeeting.getId()),
//                eq(testUser.getId()),
//                any(SubmitFeedbackRequest.class));
//
//        // When & Then
//        mockMvc.perform(post("/api/v1/feedbacks/meetings/{meetingId}", testMeeting.getId())
//                        .with(csrf())
//                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
//                        .params(params))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success", is(true)))
//                .andExpect(jsonPath("$.message", is("Opinia została dodana")));
//
//        verify(feedbackService, times(1))
//                .submitFeedback(
//                        eq(testMeeting.getId()),
//                        eq(testUser.getId()),
//                        any(SubmitFeedbackRequest.class));
//    }
//
//    @Test
//    @WithMockUser(username = "john.doe@example.com", roles = "PARTICIPANT")
//    void submitFeedback_InvalidMeetingId_ShouldReturnBadRequest() throws Exception {
//        // Given
//        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
//        params.add("rating", "4");
//        params.add("comment", "Comment");
//
//        // When & Then
//        mockMvc.perform(post("/api/v1/feedbacks/meetings/{meetingId}", 0)
//                        .with(csrf())
//                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
//                        .params(params))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.success", is(false)))
//                .andExpect(jsonPath("$.message", containsString("Nieprawidłowe ID spotkania")));
//
//        verify(feedbackService, never()).submitFeedback(anyLong(), anyLong(), any());
//    }
//
//    @Test
//    @WithMockUser(username = "john.doe@example.com", roles = "PARTICIPANT")
//    void submitFeedback_RatingBelowMinimum_ShouldReturnBadRequest() throws Exception {
//        // Given
//        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
//        params.add("rating", "0");
//        params.add("comment", "Comment");
//
//        // When & Then
//        mockMvc.perform(post("/api/v1/feedbacks/meetings/{meetingId}", testMeeting.getId())
//                        .with(csrf())
//                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
//                        .params(params))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.success", is(false)))
//                .andExpect(jsonPath("$.message", is("Ocena musi być w zakresie 1-5")));
//
//        verify(feedbackService, never()).submitFeedback(anyLong(), anyLong(), any());
//    }
//
//    @Test
//    @WithMockUser(username = "john.doe@example.com", roles = "PARTICIPANT")
//    void submitFeedback_RatingAboveMaximum_ShouldReturnBadRequest() throws Exception {
//        // Given
//        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
//        params.add("rating", "6");
//        params.add("comment", "Comment");
//
//        // When & Then
//        mockMvc.perform(post("/api/v1/feedbacks/meetings/{meetingId}", testMeeting.getId())
//                        .with(csrf())
//                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
//                        .params(params))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.success", is(false)))
//                .andExpect(jsonPath("$.message", is("Ocena musi być w zakresie 1-5")));
//
//        verify(feedbackService, never()).submitFeedback(anyLong(), anyLong(), any());
//    }
//
//    @Test
//    @WithMockUser(username = "john.doe@example.com", roles = "PARTICIPANT")
//    void submitFeedback_RatingNull_ShouldReturnBadRequest() throws Exception {
//        // Given
//        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
//        params.add("comment", "Comment without rating");
//
//        // When & Then
//        mockMvc.perform(post("/api/v1/feedbacks/meetings/{meetingId}", testMeeting.getId())
//                        .with(csrf())
//                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
//                        .params(params))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.success", is(false)))
//                .andExpect(jsonPath("$.message", is("Ocena musi być w zakresie 1-5")));
//
//        verify(feedbackService, never()).submitFeedback(anyLong(), anyLong(), any());
//    }
//
//    @Test
//    @WithMockUser(username = "john.doe@example.com", roles = "PARTICIPANT")
//    void submitFeedback_WithoutComment_ShouldSuccess() throws Exception {
//        // Given
//        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
//        params.add("rating", "5");
//
//        doNothing().when(feedbackService).submitFeedback(
//                eq(testMeeting.getId()), eq(testUser.getId()), any(SubmitFeedbackRequest.class));
//
//        // When & Then
//        mockMvc.perform(post("/api/v1/feedbacks/meetings/{meetingId}", testMeeting.getId())
//                        .with(csrf())
//                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
//                        .params(params))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success", is(true)))
//                .andExpect(jsonPath("$.message", is("Opinia została dodana")));
//
//        verify(feedbackService, times(1))
//                .submitFeedback(eq(testMeeting.getId()), eq(testUser.getId()), any(SubmitFeedbackRequest.class));
//    }
//
//    @Test
//    void submitFeedback_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
//        // Given
//        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
//        params.add("rating", "3");
//        params.add("comment", "Test comment");
//
//        // When & Then - bez @WithMockUser
//        mockMvc.perform(post("/api/v1/feedbacks/meetings/{meetingId}", testMeeting.getId())
//                        .with(csrf())
//                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
//                        .params(params))
//                .andExpect(status().isUnauthorized());
//
//        verify(feedbackService, never()).submitFeedback(anyLong(), anyLong(), any());
//    }
//
//    @Test
//    @WithMockUser(username = "john.doe@example.com", roles = "PARTICIPANT")
//    void submitFeedback_ServiceThrowsException_ShouldReturnInternalServerError() throws Exception {
//        // Given
//        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
//        params.add("rating", "4");
//        params.add("comment", "Test comment");
//
//        doThrow(new RuntimeException("Database error"))
//                .when(feedbackService).submitFeedback(
//                        eq(testMeeting.getId()), eq(testUser.getId()), any(SubmitFeedbackRequest.class));
//
//        // When & Then
//        mockMvc.perform(post("/api/v1/feedbacks/meetings/{meetingId}", testMeeting.getId())
//                        .with(csrf())
//                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
//                        .params(params))
//                .andExpect(status().isInternalServerError())
//                .andExpect(jsonPath("$.success", is(false)))
//                .andExpect(jsonPath("$.message", containsString("Database error")));
//
//        verify(feedbackService, times(1))
//                .submitFeedback(eq(testMeeting.getId()), eq(testUser.getId()), any(SubmitFeedbackRequest.class));
//    }
//
//    @Test
//    @WithMockUser(username = "john.doe@example.com", roles = "PARTICIPANT")
//    void getMeetingFeedbacks_ValidMeetingId_ShouldReturnFeedbacks() throws Exception {
//        // Given
//        Feedback feedback1 = new Feedback();
//        feedback1.setId(1L);
//        feedback1.setRating(4);
//        feedback1.setComment("Good meeting");
//
//        Feedback feedback2 = new Feedback();
//        feedback2.setId(2L);
//        feedback2.setRating(5);
//        feedback2.setComment("Excellent!");
//
//        List<Feedback> feedbacks = Arrays.asList(feedback1, feedback2);
//
//        when(feedbackService.getMeetingFeedbacks(testMeeting.getId())).thenReturn(feedbacks);
//
//        // When & Then
//        mockMvc.perform(get("/api/v1/feedbacks/meetings/{meetingId}", testMeeting.getId()))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success", is(true)))
//                .andExpect(jsonPath("$.message", is("Feedbacks retrieved")))
//                .andExpect(jsonPath("$.data", hasSize(2)))
//                .andExpect(jsonPath("$.data[0].id", is(1)))
//                .andExpect(jsonPath("$.data[0].rating", is(4)))
//                .andExpect(jsonPath("$.data[0].comment", is("Good meeting")))
//                .andExpect(jsonPath("$.data[1].id", is(2)))
//                .andExpect(jsonPath("$.data[1].rating", is(5)))
//                .andExpect(jsonPath("$.data[1].comment", is("Excellent!")));
//
//        verify(feedbackService, times(1)).getMeetingFeedbacks(testMeeting.getId());
//    }
//
//    @Test
//    @WithMockUser(username = "john.doe@example.com", roles = "PARTICIPANT")
//    void getMeetingFeedbacks_NoFeedbacks_ShouldReturnEmptyList() throws Exception {
//        // Given
//        when(feedbackService.getMeetingFeedbacks(testMeeting.getId())).thenReturn(Arrays.asList());
//
//        // When & Then
//        mockMvc.perform(get("/api/v1/feedbacks/meetings/{meetingId}", testMeeting.getId()))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success", is(true)))
//                .andExpect(jsonPath("$.message", is("Feedbacks retrieved")))
//                .andExpect(jsonPath("$.data", hasSize(0)));
//
//        verify(feedbackService, times(1)).getMeetingFeedbacks(testMeeting.getId());
//    }
//
//    @Test
//    @WithMockUser(username = "john.doe@example.com", roles = "PARTICIPANT")
//    void getMeetingFeedbacks_InvalidMeetingId_ShouldReturnEmptyList() throws Exception {
//        // Given
//        Long invalidMeetingId = 999L;
//        when(feedbackService.getMeetingFeedbacks(invalidMeetingId)).thenReturn(Arrays.asList());
//
//        // When & Then
//        mockMvc.perform(get("/api/v1/feedbacks/meetings/{meetingId}", invalidMeetingId))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success", is(true)))
//                .andExpect(jsonPath("$.data", hasSize(0)));
//
//        verify(feedbackService, times(1)).getMeetingFeedbacks(invalidMeetingId);
//    }
//
//    @Test
//    @WithMockUser(username = "john.doe@example.com", roles = {"PARTICIPANT", "ADMIN"})
//    void submitFeedback_WithAdminRole_ShouldSuccess() throws Exception {
//        // Given
//        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
//        params.add("rating", "3");
//        params.add("comment", "Admin feedback");
//
//        doNothing().when(feedbackService).submitFeedback(
//                eq(testMeeting.getId()), eq(testUser.getId()), any(SubmitFeedbackRequest.class));
//
//        // When & Then
//        mockMvc.perform(post("/api/v1/feedbacks/meetings/{meetingId}", testMeeting.getId())
//                        .with(csrf())
//                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
//                        .params(params))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success", is(true)));
//
//        verify(feedbackService, times(1))
//                .submitFeedback(eq(testMeeting.getId()), eq(testUser.getId()), any(SubmitFeedbackRequest.class));
//    }
//
//    @Test
//    @WithMockUser(username = "different.user@example.com", roles = "PARTICIPANT")
//    void submitFeedback_DifferentUser_ShouldUseCorrectUserId() throws Exception {
//        // Given
//        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
//        params.add("rating", "4");
//        params.add("comment", "Feedback from different user");
//
//        // Tworzenie innego użytkownika w bazie
//        User differentUser = User.builder()
//                .firstName("Different")
//                .lastName("User")
//                .email("different.user@example.com")
//                .password("hashedPassword456")
//                .role(UserRole.PARTICIPANT)
//                .enabled(true)
//                .build();
//        differentUser = userRepository.save(differentUser);
//
//        // Mock dla różnych użytkowników
//        com.meethub.domain.model.entity.User mockDifferentUserEntity = new com.meethub.domain.model.entity.User();
//        mockDifferentUserEntity.setId(differentUser.getId());
//        mockDifferentUserEntity.setEmail(differentUser.getEmail());
//        mockDifferentUserEntity.setFirstName(differentUser.getFirstName());
//        mockDifferentUserEntity.setLastName(differentUser.getLastName());
//        mockDifferentUserEntity.setPassword(differentUser.getPassword());
//        mockDifferentUserEntity.setRole(differentUser.getRole());
//        mockDifferentUserEntity.setEnabled(differentUser.getEnabled());
//
//        CustomUserDetailsService.CustomUserDetails differentUserDetails =
//                new CustomUserDetailsService.CustomUserDetails(mockDifferentUserEntity);
//
//        when(customUserDetailsService.loadUserByUsername("different.user@example.com"))
//                .thenReturn(differentUserDetails);
//
//        doNothing().when(feedbackService).submitFeedback(
//                eq(testMeeting.getId()), eq(differentUser.getId()), any(SubmitFeedbackRequest.class));
//
//        // When & Then
//        mockMvc.perform(post("/api/v1/feedbacks/meetings/{meetingId}", testMeeting.getId())
//                        .with(csrf())
//                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
//                        .params(params))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success", is(true)));
//
//        verify(feedbackService, times(1))
//                .submitFeedback(eq(testMeeting.getId()), eq(differentUser.getId()), any(SubmitFeedbackRequest.class));
//    }
//
//    @Test
//    @WithMockUser(username = "john.doe@example.com", roles = "PARTICIPANT")
//    void submitFeedback_WithoutCsrf_ShouldReturnForbidden() throws Exception {
//        // Given
//        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
//        params.add("rating", "4");
//        params.add("comment", "Test without CSRF");
//
//        // When & Then - bez .with(csrf())
//        mockMvc.perform(post("/api/v1/feedbacks/meetings/{meetingId}", testMeeting.getId())
//                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
//                        .params(params))
//                .andExpect(status().isForbidden());
//
//        verify(feedbackService, never()).submitFeedback(anyLong(), anyLong(), any());
//    }
//
//    @Test
//    @WithMockUser(username = "john.doe@example.com", roles = "PARTICIPANT")
//    void submitFeedback_UserAccountLockedInPast_ShouldStillWork() throws Exception {
//        // Given
//        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
//        params.add("rating", "3");
//        params.add("comment", "Test with previously locked account");
//
//        // Aktualizacja użytkownika z zablokowanym kontem w przeszłości
//        testUser.setAccountLockedUntil(LocalDateTime.now().minusDays(1));
//        testUser = userRepository.save(testUser);
//
//        doNothing().when(feedbackService).submitFeedback(
//                eq(testMeeting.getId()), eq(testUser.getId()), any(SubmitFeedbackRequest.class));
//
//        // When & Then - konto zablokowane w przeszłości powinno działać
//        mockMvc.perform(post("/api/v1/feedbacks/meetings/{meetingId}", testMeeting.getId())
//                        .with(csrf())
//                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
//                        .params(params))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success", is(true)));
//
//        verify(feedbackService, times(1))
//                .submitFeedback(eq(testMeeting.getId()), eq(testUser.getId()), any(SubmitFeedbackRequest.class));
//    }
//
//    @Test
//    @WithMockUser(username = "john.doe@example.com", roles = "PARTICIPANT")
//    void submitFeedback_UserAccountDisabled_ShouldStillWork() throws Exception {
//        // Given
//        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
//        params.add("rating", "2");
//        params.add("comment", "Test with disabled account");
//
//        // Aktualizacja użytkownika z wyłączonym kontem
//        testUser.setEnabled(false);
//        testUser = userRepository.save(testUser);
//
//        doNothing().when(feedbackService).submitFeedback(
//                eq(testMeeting.getId()), eq(testUser.getId()), any(SubmitFeedbackRequest.class));
//
//        // When & Then
//        mockMvc.perform(post("/api/v1/feedbacks/meetings/{meetingId}", testMeeting.getId())
//                        .with(csrf())
//                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
//                        .params(params))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.success", is(true)));
//
//        verify(feedbackService, times(1))
//                .submitFeedback(eq(testMeeting.getId()), eq(testUser.getId()), any(SubmitFeedbackRequest.class));
//    }
//}
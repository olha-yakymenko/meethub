package com.meethub.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meethub.domain.model.entity.Feedback;
import com.meethub.domain.model.request.SubmitFeedbackRequest;
import com.meethub.domain.model.response.ApiResponse;
import com.meethub.domain.service.FeedbackService;
import com.meethub.security.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(FeedbackController.class)
class FeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FeedbackService feedbackService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;


    @Test
    void submitFeedback_ShouldReturnBadRequest_WhenRatingOutOfRange() throws Exception {
        // Given
        SubmitFeedbackRequest request = SubmitFeedbackRequest.builder()
                .rating(6)  // Nieprawidłowa ocena (max 5)
                .comment("Too high rating")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/feedbacks/meetings/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify - serwis nie powinien być wywołany przy błędnej walidacji
        verify(feedbackService, never()).submitFeedback(anyLong(), anyLong(), any());
    }

    @Test
    void getMeetingFeedbacks_ShouldReturnFeedbacksList() throws Exception {
        // Given
        Feedback feedback1 = new Feedback();
        feedback1.setId(1L);
        feedback1.setRating(5);
        feedback1.setComment("Great!");

        Feedback feedback2 = new Feedback();
        feedback2.setId(2L);
        feedback2.setRating(4);
        feedback2.setComment("Good");

        List<Feedback> feedbacks = Arrays.asList(feedback1, feedback2);
        when(feedbackService.getMeetingFeedbacks(1L)).thenReturn(feedbacks);

        // When & Then - GET bez autoryzacji (publiczny endpoint)
        mockMvc.perform(get("/api/v1/feedbacks/meetings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].rating").value(5))
                .andExpect(jsonPath("$.data[1].rating").value(4));
    }

    @Test
    void getMeetingFeedbacks_ShouldReturnEmptyList_WhenNoFeedbacks() throws Exception {
        // Given
        when(feedbackService.getMeetingFeedbacks(1L)).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/v1/feedbacks/meetings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void submitFeedback_ShouldHandleServiceException() throws Exception {
        // Given
        SubmitFeedbackRequest request = SubmitFeedbackRequest.builder()
                .rating(5)
                .comment("Excellent meeting!")
                .build();

        doThrow(new RuntimeException("Service error"))
                .when(feedbackService).submitFeedback(anyLong(), anyLong(), any());

        // When & Then
        mockMvc.perform(post("/api/v1/feedbacks/meetings/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @ParameterizedTest
    @MethodSource("provideInvalidFeedbackData")
    void submitFeedback_ShouldValidateInputParameters(
            Integer rating, String comment, int expectedStatus) throws Exception {
        // Given
        SubmitFeedbackRequest request = SubmitFeedbackRequest.builder()
                .rating(rating)
                .comment(comment)
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/feedbacks/meetings/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(expectedStatus));
    }

    private static Stream<Arguments> provideInvalidFeedbackData() {
        return Stream.of(
                // rating, comment, expectedStatus
                Arguments.of(null, "No rating", 400),           // rating wymagane
                Arguments.of(0, "Too low", 400),                // min 1
                Arguments.of(6, "Too high", 400),               // max 5
                Arguments.of(5, "A".repeat(1001), 400)         // za długi komentarz
        );
    }

    @Test
    void submitFeedback_ShouldReturnBadRequest_WhenMeetingIdInvalid() throws Exception {
        // Given
        SubmitFeedbackRequest request = SubmitFeedbackRequest.builder()
                .rating(5)
                .comment("Test")
                .build();

        // When & Then - Nieprawidłowe ID spotkania
        mockMvc.perform(post("/api/v1/feedbacks/meetings/abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // 400 - nie można sparsować 'abc' do Long
    }

    @Test
    void submitFeedback_ShouldWorkWithDifferentUsers() throws Exception {
        // Given
        SubmitFeedbackRequest request = SubmitFeedbackRequest.builder()
                .rating(4)
                .comment("Good")
                .build();

        // Zmień użytkownika w kontekście
        CustomUserDetailsService.CustomUserDetails otherUser =
                mock(CustomUserDetailsService.CustomUserDetails.class);
        when(otherUser.getId()).thenReturn(2L);
        when(otherUser.getUsername()).thenReturn("other@example.com");
        when(otherUser.getAuthorities()).thenReturn((Collection)
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(otherUser, null, otherUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // When & Then
        mockMvc.perform(post("/api/v1/feedbacks/meetings/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Verify - upewnij się że użyto ID nowego użytkownika
        verify(feedbackService).submitFeedback(eq(1L), eq(2L), any(SubmitFeedbackRequest.class));
    }

    @Test
    void submitFeedback_ShouldValidateEmptyRequest() throws Exception {
        // Given - pusty JSON
        String emptyJson = "{}";

        // When & Then
        mockMvc.perform(post("/api/v1/feedbacks/meetings/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emptyJson))
                .andExpect(status().isBadRequest());
    }

}
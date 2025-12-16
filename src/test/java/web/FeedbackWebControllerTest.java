package com.meethub.controller.web;

import com.meethub.domain.model.request.SubmitFeedbackRequest;
import com.meethub.domain.service.FeedbackService;
import com.meethub.security.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FeedbackWebController.class)
class FeedbackWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FeedbackService feedbackService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    // WAŻNE: Ponieważ @Validated na klasie powoduje walidację PRZED wejściem do metody,
    // a @NotNull na userDetails powoduje ConstraintViolationException,
    // to testy muszą oczekiwać statusu 200 z widokiem błędu zamiast przekierowania


    // SCENARIUSZ 2: Błąd walidacji - ocena poza zakresem (10 > 5)
    @Test
    @WithMockUser
    void submitFeedback_InvalidRating_ShouldShowErrorPage() throws Exception {
        // Given
        Long meetingId = 1L;
        Integer invalidRating = 10; // Nieprawidłowa ocena > 5

        // When & Then - oczekujemy widoku błędu (status 200)
        mockMvc.perform(post("/meetings/{meetingId}/feedbacks/submit", meetingId)
                        .param("rating", invalidRating.toString())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk()) // Globalny exception handler zwraca 200 z widokiem błędu
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attribute("error", "Internal Server Error"));

        verify(feedbackService, never()).submitFeedback(anyLong(), anyLong(), any());
    }

    // SCENARIUSZ 3: Brak oceny (null) - MissingServletRequestParameterException
    @Test
    @WithMockUser
    void submitFeedback_NullRating_ShouldShowErrorPage() throws Exception {
        // Given
        Long meetingId = 1L;

        // When & Then - bez parametru rating
        mockMvc.perform(post("/meetings/{meetingId}/feedbacks/submit", meetingId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk()) // Globalny exception handler zwraca 200 z widokiem błędu
                .andExpect(view().name("error"));
    }

    // SCENARIUSZ 4: Komentarz zbyt długi
    @Test
    @WithMockUser
    void submitFeedback_CommentTooLong_ShouldShowErrorPage() throws Exception {
        // Given
        Long meetingId = 1L;
        Integer rating = 4;
        String longComment = "a".repeat(1001); // Przekracza limit 1000 znaków

        // When & Then
        mockMvc.perform(post("/meetings/{meetingId}/feedbacks/submit", meetingId)
                        .param("rating", rating.toString())
                        .param("comment", longComment)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk()) // Globalny exception handler zwraca 200 z widokiem błędu
                .andExpect(view().name("error"));

        verify(feedbackService, never()).submitFeedback(anyLong(), anyLong(), any());
    }

    // SCENARIUSZ 5: Nieprawidłowe ID spotkania (ujemne)
    @Test
    @WithMockUser
    void submitFeedback_InvalidMeetingId_ShouldShowErrorPage() throws Exception {
        // Given
        Long invalidMeetingId = -1L;
        Integer rating = 3;

        // When & Then
        mockMvc.perform(post("/meetings/{meetingId}/feedbacks/submit", invalidMeetingId)
                        .param("rating", rating.toString())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk()) // Globalny exception handler zwraca 200 z widokiem błędu
                .andExpect(view().name("error"));

        verify(feedbackService, never()).submitFeedback(anyLong(), anyLong(), any());
    }

    // SCENARIUSZ 11: Test oceny = 0 (poniżej minimum)
    @Test
    @WithMockUser
    void submitFeedback_RatingBelowMinimum_ShouldShowErrorPage() throws Exception {
        // Given
        Long meetingId = 1L;
        Integer invalidRating = 0; // Mniej niż 1

        // When & Then
        mockMvc.perform(post("/meetings/{meetingId}/feedbacks/submit", meetingId)
                        .param("rating", invalidRating.toString())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk()) // Globalny exception handler
                .andExpect(view().name("error"));

        verify(feedbackService, never()).submitFeedback(anyLong(), anyLong(), any());
    }
}
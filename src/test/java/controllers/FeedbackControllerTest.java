package com.meethub.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meethub.domain.model.entity.Feedback;
import com.meethub.domain.model.request.SubmitFeedbackRequest;
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
import org.springframework.security.test.context.support.WithMockUser;
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

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = {"PARTICIPANT"})
    void submitFeedbackJson_ShouldReturnBadRequest_WhenRatingOutOfRange() throws Exception {
        // Given
        SubmitFeedbackRequest request = SubmitFeedbackRequest.builder()
                .rating(6) // Invalid: should be 1-5
                .comment("Too high rating")
                .build();

        // When & Then - sprawdź poprawną strukturę błędu
        mockMvc.perform(post("/api/v1/feedbacks/meetings/1/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Walidacja danych nie powiodła się"));

        verify(feedbackService, never()).submitFeedback(anyLong(), anyLong(), any());
    }

    @Test
    void submitFeedbackJson_ShouldReturnSuccess_WhenValidRequest() throws Exception {
        // Given
        SubmitFeedbackRequest request = SubmitFeedbackRequest.builder()
                .rating(5)
                .comment("Excellent meeting!")
                .build();

        // Mock CustomUserDetails
        CustomUserDetailsService.CustomUserDetails userDetails = mock(CustomUserDetailsService.CustomUserDetails.class);
        when(userDetails.getId()).thenReturn(1L);
        when(userDetails.getUsername()).thenReturn("test@example.com");

        // Ustaw uprawnienia
        Collection<SimpleGrantedAuthority> authorities =
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_PARTICIPANT"));
        when(userDetails.getAuthorities()).thenReturn((Collection) authorities);

        // Ustaw SecurityContext z CustomUserDetails
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // When & Then
        mockMvc.perform(post("/api/v1/feedbacks/meetings/1/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Opinia została pomyślnie dodana"));

        verify(feedbackService).submitFeedback(eq(1L), eq(1L), any(SubmitFeedbackRequest.class));
    }

    @Test
    void submitFeedbackJson_ShouldReturnBadRequest_WhenUserNotLoggedIn() throws Exception {
        // Given
        SubmitFeedbackRequest request = SubmitFeedbackRequest.builder()
                .rating(4)
                .comment("Good meeting")
                .build();

        // Wyczyść SecurityContext - symuluj niezalogowanego użytkownika
        SecurityContextHolder.clearContext();

        // When & Then - oczekuj poprawnej struktury błędu dla niezalogowanego użytkownika
        mockMvc.perform(post("/api/v1/feedbacks/meetings/1/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Walidacja danych nie powiodła się"));

    }

    @Test
    @WithMockUser(username = "test@example.com", roles = {"PARTICIPANT"})
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

        // When & Then
        mockMvc.perform(get("/api/v1/feedbacks/meetings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].rating").value(5))
                .andExpect(jsonPath("$.data[1].rating").value(4));
    }

    @ParameterizedTest
    @MethodSource("provideInvalidFeedbackData")
    @WithMockUser(username = "test@example.com", roles = {"USER"})
    void submitFeedbackJson_ShouldValidateInputParameters(
            Integer rating, String comment, int expectedStatus) throws Exception {
        // Given
        SubmitFeedbackRequest request = SubmitFeedbackRequest.builder()
                .rating(rating)
                .comment(comment)
                .build();

        // When & Then - dla testów walidacji sprawdzaj tylko status
        var result = mockMvc.perform(post("/api/v1/feedbacks/meetings/1/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(expectedStatus));

        // Dla błędów 400 sprawdź strukturę błędu
        if (expectedStatus == 400) {
            result.andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"));
        }
    }

    private static Stream<Arguments> provideInvalidFeedbackData() {
        return Stream.of(
                // rating, comment, expectedStatus
                Arguments.of(null, "No rating", 400),
                Arguments.of(0, "Too low", 400),
                Arguments.of(6, "Too high", 400),
                Arguments.of(5, "A".repeat(1001), 400) // Too long comment
        );
    }
}
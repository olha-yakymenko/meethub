package com.meethub.controller.web;

import com.meethub.domain.model.request.SubmitFeedbackRequest;
import com.meethub.domain.service.FeedbackService;
import com.meethub.security.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FeedbackWebControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FeedbackService feedbackService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;


    // SCENARIUSZ 2: Test walidacji z ujemnym ID spotkania
    @Test
    @WithMockUser
    void submitFeedback_NegativeMeetingId_ShouldShowErrorPage() throws Exception {
        // Given
        Long invalidMeetingId = -5L;
        Integer rating = 3;

        // When & Then - walidacja zwraca ConstraintViolationException, które jest łapane
        // przez globalny exception handler i zwraca widok błędu
        mockMvc.perform(post("/meetings/{meetingId}/feedbacks/submit", invalidMeetingId)
                        .param("rating", rating.toString())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk()) // Globalny exception handler zwraca 200
                .andExpect(view().name("error"));
    }

}
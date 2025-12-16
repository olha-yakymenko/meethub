package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.Feedback;
import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.request.SubmitFeedbackRequest;
import com.meethub.domain.repository.jpa.FeedbackRepository;
import com.meethub.domain.repository.jpa.MeetingRepository;
import com.meethub.domain.repository.jpa.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class FeedbackServiceImplIntegrationTest {

    @Autowired
    private FeedbackServiceImpl feedbackService;

    @MockBean
    private FeedbackRepository feedbackRepository;

    @MockBean
    private MeetingRepository meetingRepository;

    @MockBean
    private UserRepository userRepository;

    @Test
    void shouldCompleteFeedbackFlow() {
        // Given - Meeting and user
        Meeting meeting = Meeting.builder()
                .title("Test Meeting")
                .build();

        User user = User.builder()
                .id(1L)
                .firstName("Test")
                .email("test@example.com")
                .build();

        SubmitFeedbackRequest request = SubmitFeedbackRequest.builder()
                .rating(5)
                .comment("Excellent meeting!")
                .build();

        // Mock repository responses
        when(meetingRepository.findById(1L))
                .thenReturn(Optional.of(meeting));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(feedbackRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.empty());

        Feedback savedFeedback = Feedback.builder()
                .id(1L)
                .meeting(meeting)
                .user(user)
                .rating(5)
                .comment("Excellent meeting!")
                .createdAt(LocalDateTime.now())
                .build();

        when(feedbackRepository.save(any(Feedback.class)))
                .thenReturn(savedFeedback);

        // When - Submit feedback
        Feedback submittedFeedback = feedbackService.submitFeedback(1L, 1L, request);

        // Then - Verify submission
        assertAll("Feedback submission",
                () -> assertNotNull(submittedFeedback),
                () -> assertEquals(5, submittedFeedback.getRating()),
                () -> assertEquals(meeting, submittedFeedback.getMeeting()),
                () -> assertEquals(user, submittedFeedback.getUser())
        );

        // Given - Get feedbacks
        List<Feedback> feedbacks = Arrays.asList(savedFeedback);
        when(feedbackRepository.findByMeetingId(1L))
                .thenReturn(feedbacks);

        // When - Get meeting feedbacks
        List<Feedback> retrievedFeedbacks = feedbackService.getMeetingFeedbacks(1L);

        // Then - Verify retrieval
        assertAll("Feedback retrieval",
                () -> assertNotNull(retrievedFeedbacks),
                () -> assertEquals(1, retrievedFeedbacks.size()),
                () -> assertEquals(savedFeedback, retrievedFeedbacks.get(0))
        );

        // When - Get user feedback
        when(feedbackRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(savedFeedback));

        Feedback userFeedback = feedbackService.getUserFeedback(1L, 1L);

        // Then
        assertNotNull(userFeedback);
        assertEquals(savedFeedback, userFeedback);

        // When - Get average rating
        when(feedbackRepository.findAverageRatingByMeetingId(1L))
                .thenReturn(5.0);

        Double averageRating = feedbackService.getAverageRating(1L);

        // Then
        assertEquals(5.0, averageRating, 0.001);
    }

    @Test
    void shouldHandleMultipleFeedbacks_ForAverageCalculation() {
        // Given - Meeting with multiple feedbacks
        Meeting meeting = Meeting.builder()
                .title("Team Meeting")
                .build();

        // Mock average calculation
        when(feedbackRepository.findAverageRatingByMeetingId(1L))
                .thenReturn(3.75); // (5 + 4 + 3 + 3) / 4 = 3.75

        // When
        Double average = feedbackService.getAverageRating(1L);

        // Then
        assertEquals(3.75, average, 0.001);
    }

    @Test
    void shouldPreventDuplicateFeedback_FromSameUser() {
        // Given - Existing feedback
        Meeting meeting = Meeting.builder()
                .build();

        User user = User.builder()
                .id(1L)
                .build();

        Feedback existingFeedback = Feedback.builder()
                .id(1L)
                .meeting(meeting)
                .user(user)
                .rating(4)
                .build();

        when(meetingRepository.findById(1L))
                .thenReturn(Optional.of(meeting));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(feedbackRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(existingFeedback));

        SubmitFeedbackRequest newRequest = SubmitFeedbackRequest.builder()
                .rating(5)
                .comment("Trying to submit again")
                .build();

        // When & Then - Should throw exception
        Exception exception = assertThrows(Exception.class, () -> {
            feedbackService.submitFeedback(1L, 1L, newRequest);
        });

        assertTrue(exception.getMessage().contains("already submitted"));
        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void shouldHandleLargeNumberOfFeedbacks() {
        // Given - Many feedbacks
        Meeting meeting = Meeting.builder()
                .build();

        List<Feedback> manyFeedbacks = Arrays.asList(
                Feedback.builder().id(1L).rating(4).build(),
                Feedback.builder().id(2L).rating(5).build(),
                Feedback.builder().id(3L).rating(3).build(),
                Feedback.builder().id(4L).rating(4).build(),
                Feedback.builder().id(5L).rating(5).build()
        );

        when(feedbackRepository.findByMeetingId(1L))
                .thenReturn(manyFeedbacks);

        // When
        List<Feedback> result = feedbackService.getMeetingFeedbacks(1L);

        // Then
        assertEquals(5, result.size());
    }
}
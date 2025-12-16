package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.Feedback;
import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.request.SubmitFeedbackRequest;
import com.meethub.domain.repository.jpa.FeedbackRepository;
import com.meethub.domain.repository.jpa.MeetingRepository;
import com.meethub.domain.repository.jpa.UserRepository;
import com.meethub.exception.BusinessException;
import com.meethub.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceImplTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FeedbackServiceImpl feedbackService;

    private Meeting testMeeting;
    private User testUser;
    private SubmitFeedbackRequest validRequest;
    private Feedback testFeedback;

    @BeforeEach
    void setUp() {
        testMeeting = Meeting.builder()
                .title("Spotkanie testowe")
                .startDate(LocalDateTime.now().minusHours(2))
                .endDate(LocalDateTime.now().minusHours(1))
                .build();

        testUser = User.builder()
                .id(1L)
                .firstName("Jan")
                .lastName("Kowalski")
                .email("jan.kowalski@example.com")
                .build();

        validRequest = SubmitFeedbackRequest.builder()
                .rating(4)
                .comment("Bardzo dobre spotkanie")
                .build();

        testFeedback = Feedback.builder()
                .id(1L)
                .meeting(testMeeting)
                .user(testUser)
                .rating(4)
                .comment("Bardzo dobre spotkanie")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void shouldSubmitFeedback_Successfully() {
        // Given
        when(meetingRepository.findById(1L))
                .thenReturn(Optional.of(testMeeting));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(testUser));

        when(feedbackRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.empty());

        when(feedbackRepository.save(any(Feedback.class)))
                .thenAnswer(invocation -> {
                    Feedback feedback = invocation.getArgument(0);
                    feedback.setId(1L);
                    return feedback;
                });

        // When
        Feedback result = feedbackService.submitFeedback(1L, 1L, validRequest);

        // Then
        assertAll("Feedback submission validation",
                () -> assertNotNull(result, "Result should not be null"),
                () -> assertEquals(testMeeting, result.getMeeting(), "Meeting should match"),
                () -> assertEquals(testUser, result.getUser(), "User should match"),
                () -> assertEquals(4, result.getRating(), "Rating should match"),
                () -> assertEquals("Bardzo dobre spotkanie", result.getComment(), "Comment should match"),
                () -> assertNotNull(result.getCreatedAt(), "CreatedAt should be set")
        );

        verify(feedbackRepository, times(1)).save(any(Feedback.class));
    }

    @Test
    void shouldThrowException_WhenMeetingNotFound() {
        // Given
        when(meetingRepository.findById(1L))
                .thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> feedbackService.submitFeedback(1L, 1L, validRequest)
        );

        assertEquals("Meeting not found", exception.getMessage());
        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void shouldThrowException_WhenUserNotFound() {
        // Given
        when(meetingRepository.findById(1L))
                .thenReturn(Optional.of(testMeeting));

        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> feedbackService.submitFeedback(1L, 1L, validRequest)
        );

        assertEquals("User not found", exception.getMessage());
        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void shouldThrowException_WhenFeedbackAlreadyExists() {
        // Given
        when(meetingRepository.findById(1L))
                .thenReturn(Optional.of(testMeeting));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(testUser));

        when(feedbackRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testFeedback));

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> feedbackService.submitFeedback(1L, 1L, validRequest)
        );

        assertEquals("You have already submitted feedback", exception.getMessage());
        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void shouldThrowException_WhenRatingTooLow() {
        // Given
        SubmitFeedbackRequest invalidRequest = SubmitFeedbackRequest.builder()
                .rating(0)
                .comment("Zbyt niska ocena")
                .build();

        when(meetingRepository.findById(1L))
                .thenReturn(Optional.of(testMeeting));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(testUser));

        when(feedbackRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> feedbackService.submitFeedback(1L, 1L, invalidRequest)
        );

        assertEquals("Rating must be between 1 and 5", exception.getMessage());
        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void shouldThrowException_WhenRatingTooHigh() {
        // Given
        SubmitFeedbackRequest invalidRequest = SubmitFeedbackRequest.builder()
                .rating(6)
                .comment("Zbyt wysoka ocena")
                .build();

        when(meetingRepository.findById(1L))
                .thenReturn(Optional.of(testMeeting));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(testUser));

        when(feedbackRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.empty());

        // When & Then
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> feedbackService.submitFeedback(1L, 1L, invalidRequest)
        );

        assertEquals("Rating must be between 1 and 5", exception.getMessage());
        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void shouldAcceptValidRatings() {
        // Test all valid rating values (1-5)
        for (int rating = 1; rating <= 5; rating++) {
            // Given
            SubmitFeedbackRequest request = SubmitFeedbackRequest.builder()
                    .rating(rating)
                    .comment("Test rating " + rating)
                    .build();

            when(meetingRepository.findById(1L))
                    .thenReturn(Optional.of(testMeeting));

            when(userRepository.findById(1L))
                    .thenReturn(Optional.of(testUser));

            when(feedbackRepository.findByMeetingIdAndUserId(1L, 1L))
                    .thenReturn(Optional.empty());

            when(feedbackRepository.save(any(Feedback.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Feedback result = feedbackService.submitFeedback(1L, 1L, request);

            // Then
            assertEquals(rating, result.getRating(),
                    "Should accept rating value: " + rating);

            // Reset mocks for next iteration
            reset(meetingRepository, userRepository, feedbackRepository);
        }
    }

    @Test
    void shouldSubmitFeedback_WithNullComment() {
        // Given
        SubmitFeedbackRequest requestWithNullComment = SubmitFeedbackRequest.builder()
                .rating(5)
                .comment(null)
                .build();

        when(meetingRepository.findById(1L))
                .thenReturn(Optional.of(testMeeting));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(testUser));

        when(feedbackRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.empty());

        when(feedbackRepository.save(any(Feedback.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Feedback result = feedbackService.submitFeedback(1L, 1L, requestWithNullComment);

        // Then
        assertAll("Feedback with null comment",
                () -> assertNotNull(result),
                () -> assertEquals(5, result.getRating()),
                () -> assertNull(result.getComment())
        );
    }

    @Test
    void shouldSubmitFeedback_WithEmptyComment() {
        // Given
        SubmitFeedbackRequest requestWithEmptyComment = SubmitFeedbackRequest.builder()
                .rating(3)
                .comment("")
                .build();

        when(meetingRepository.findById(1L))
                .thenReturn(Optional.of(testMeeting));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(testUser));

        when(feedbackRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.empty());

        when(feedbackRepository.save(any(Feedback.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Feedback result = feedbackService.submitFeedback(1L, 1L, requestWithEmptyComment);

        // Then
        assertAll("Feedback with empty comment",
                () -> assertNotNull(result),
                () -> assertEquals(3, result.getRating()),
                () -> assertEquals("", result.getComment())
        );
    }

    @Test
    void shouldGetMeetingFeedbacks_Successfully() {
        // Given
        Feedback feedback1 = Feedback.builder()
                .id(1L)
                .rating(4)
                .comment("Dobrze")
                .build();

        Feedback feedback2 = Feedback.builder()
                .id(2L)
                .rating(5)
                .comment("Świetnie")
                .build();

        List<Feedback> expectedFeedbacks = Arrays.asList(feedback1, feedback2);

        when(feedbackRepository.findByMeetingId(1L))
                .thenReturn(expectedFeedbacks);

        // When
        List<Feedback> result = feedbackService.getMeetingFeedbacks(1L);

        // Then
        assertAll("Get meeting feedbacks",
                () -> assertNotNull(result),
                () -> assertEquals(2, result.size()),
                () -> assertEquals(expectedFeedbacks, result)
        );

        verify(feedbackRepository, times(1)).findByMeetingId(1L);
    }

    @Test
    void shouldReturnEmptyList_WhenNoFeedbacks() {
        // Given
        when(feedbackRepository.findByMeetingId(1L))
                .thenReturn(Arrays.asList());

        // When
        List<Feedback> result = feedbackService.getMeetingFeedbacks(1L);

        // Then
        assertAll("Empty feedbacks list",
                () -> assertNotNull(result),
                () -> assertTrue(result.isEmpty())
        );
    }

    @Test
    void shouldGetUserFeedback_Successfully() {
        // Given
        when(feedbackRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testFeedback));

        // When
        Feedback result = feedbackService.getUserFeedback(1L, 1L);

        // Then
        assertAll("Get user feedback",
                () -> assertNotNull(result),
                () -> assertEquals(testFeedback, result),
                () -> assertEquals(4, result.getRating())
        );
    }

    @Test
    void shouldReturnNull_WhenUserFeedbackNotFound() {
        // Given
        when(feedbackRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.empty());

        // When
        Feedback result = feedbackService.getUserFeedback(1L, 1L);

        // Then
        assertNull(result, "Should return null when feedback not found");
    }

    @Test
    void shouldGetAverageRating_WhenFeedbacksExist() {
        // Given
        when(feedbackRepository.findAverageRatingByMeetingId(1L))
                .thenReturn(4.25);

        // When
        Double result = feedbackService.getAverageRating(1L);

        // Then
        assertEquals(4.25, result, 0.001, "Average rating should match");
    }

    @Test
    void shouldGetAverageRating_ZeroWhenNoFeedbacks() {
        // Given
        when(feedbackRepository.findAverageRatingByMeetingId(1L))
                .thenReturn(null);

        // When
        Double result = feedbackService.getAverageRating(1L);

        // Then
        assertEquals(0.0, result, 0.001, "Should return 0.0 when no feedbacks");
    }

    @Test
    void shouldGetAverageRating_WhenOnlyOneFeedback() {
        // Given
        when(feedbackRepository.findAverageRatingByMeetingId(1L))
                .thenReturn(3.0);

        // When
        Double result = feedbackService.getAverageRating(1L);

        // Then
        assertEquals(3.0, result, 0.001, "Single feedback rating should be returned");
    }

    @Test
    void shouldHandleDifferentRatingValues_InAverageCalculation() {
        // Test various average calculations
        double[][] testCases = {
                {5.0, 5.0},     // Perfect score
                {1.0, 1.0},     // Minimum score
                {3.5, 3.5},     // Decimal average
                {0.0, 0.0}      // Null handling (should return 0.0)
        };

        for (double[] testCase : testCases) {
            double input = testCase[0];
            double expected = testCase[1];

            when(feedbackRepository.findAverageRatingByMeetingId(1L))
                    .thenReturn(input == 0.0 ? null : input);

            // When
            Double result = feedbackService.getAverageRating(1L);

            // Then
            assertEquals(expected, result, 0.001,
                    "Average calculation for input: " + input);

            reset(feedbackRepository);
        }
    }

    @Test
    void shouldLogAppropriately_WhenSubmittingFeedback() {
        // Given
        when(meetingRepository.findById(1L))
                .thenReturn(Optional.of(testMeeting));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(testUser));

        when(feedbackRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.empty());

        when(feedbackRepository.save(any(Feedback.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        feedbackService.submitFeedback(1L, 1L, validRequest);

        // Then - Logging should not throw exceptions
        // We can verify the method completes successfully
        verify(feedbackRepository, times(1)).save(any(Feedback.class));
    }

    @Test
    void shouldPreserveFeedbackOrder_WhenRetrieving() {
        // Given
        Feedback feedback1 = Feedback.builder()
                .id(1L)
                .rating(1)
                .createdAt(LocalDateTime.now().minusHours(2))
                .build();

        Feedback feedback2 = Feedback.builder()
                .id(2L)
                .rating(5)
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();

        Feedback feedback3 = Feedback.builder()
                .id(3L)
                .rating(3)
                .createdAt(LocalDateTime.now())
                .build();

        List<Feedback> expectedOrder = Arrays.asList(feedback1, feedback2, feedback3);

        when(feedbackRepository.findByMeetingId(1L))
                .thenReturn(expectedOrder);

        // When
        List<Feedback> result = feedbackService.getMeetingFeedbacks(1L);

        // Then
        assertEquals(expectedOrder, result,
                "Should preserve the order returned by repository");
    }

    @Test
    void shouldHandleMultipleUsers_SubmittingFeedback() {
        // Test scenario with multiple users submitting feedback for same meeting

        User user2 = User.builder()
                .id(2L)
                .firstName("Anna")
                .lastName("Nowak")
                .build();

        SubmitFeedbackRequest user2Request = SubmitFeedbackRequest.builder()
                .rating(5)
                .comment("Rewelacja!")
                .build();

        // First user submits feedback
        when(meetingRepository.findById(1L))
                .thenReturn(Optional.of(testMeeting));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(testUser));

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(user2));

        when(feedbackRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(testFeedback)); // After first submission

        when(feedbackRepository.findByMeetingIdAndUserId(1L, 2L))
                .thenReturn(Optional.empty());

        when(feedbackRepository.save(any(Feedback.class)))
                .thenAnswer(invocation -> {
                    Feedback feedback = invocation.getArgument(0);
                    feedback.setId(feedback.getUser().getId());
                    return feedback;
                });

        // When - User 1 submits
        Feedback result1 = feedbackService.submitFeedback(1L, 1L, validRequest);

        // Then
        assertNotNull(result1);
        assertEquals(1L, result1.getId());

        // When - User 2 submits
        Feedback result2 = feedbackService.submitFeedback(1L, 2L, user2Request);

        // Then
        assertNotNull(result2);
        assertEquals(2L, result2.getId());

        // Verify both were saved
        verify(feedbackRepository, times(2)).save(any(Feedback.class));
    }
}
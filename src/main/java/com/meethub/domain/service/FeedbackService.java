


package com.meethub.domain.service;

import com.meethub.domain.model.entity.Feedback;
import com.meethub.domain.model.request.SubmitFeedbackRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
public interface FeedbackService {

    Feedback submitFeedback(
            @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Positive(message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
            Long userId,

            @Valid
            @NotNull(message = "Żądanie feedbacku nie może być puste")
            SubmitFeedbackRequest request
    );

    List<Feedback> getMeetingFeedbacks(
            @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Positive(message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId
    );

    Feedback getUserFeedback(
            @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Positive(message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
            Long userId
    );

    Double getAverageRating(
            @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Positive(message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId
    );
}

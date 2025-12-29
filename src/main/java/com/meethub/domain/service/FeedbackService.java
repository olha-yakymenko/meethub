// FeedbackService.java
package com.meethub.domain.service;

import com.meethub.domain.model.entity.Feedback;
import com.meethub.domain.model.request.SubmitFeedbackRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;
public interface FeedbackService {

    Feedback submitFeedback(
            Long meetingId,
            Long userId,
            @Valid @NotNull SubmitFeedbackRequest request
    );

    List<Feedback> getMeetingFeedbacks(Long meetingId);

    Feedback getUserFeedback(Long meetingId, Long userId);

    Double getAverageRating(Long meetingId);
}
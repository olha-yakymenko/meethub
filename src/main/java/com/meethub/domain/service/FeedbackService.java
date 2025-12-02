package com.meethub.domain.service;

import com.meethub.domain.model.entity.Feedback;
import com.meethub.domain.model.request.SubmitFeedbackRequest;
import java.util.List;

public interface FeedbackService {
    Feedback submitFeedback(Long meetingId, Long userId, SubmitFeedbackRequest request);
    List<Feedback> getMeetingFeedbacks(Long meetingId);
    Feedback getUserFeedback(Long meetingId, Long userId);
    Double getAverageRating(Long meetingId);
}
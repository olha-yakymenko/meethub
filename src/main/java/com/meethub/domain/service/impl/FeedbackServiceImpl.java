package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.Feedback;
import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.request.SubmitFeedbackRequest;
import com.meethub.domain.repository.jpa.FeedbackRepository;
import com.meethub.domain.repository.jpa.MeetingRepository;
import com.meethub.domain.repository.jpa.UserRepository;
import com.meethub.domain.service.FeedbackService;
import com.meethub.exception.BusinessException;
import com.meethub.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Feedback submitFeedback(Long meetingId, Long userId, SubmitFeedbackRequest request) {
        log.info("Submitting feedback for meeting: {} by user: {}", meetingId, userId);

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Sprawdź czy użytkownik uczestniczył
        // (możesz dodać własną logikę walidacji)

        // Sprawdź czy już istnieje feedback
        feedbackRepository.findByMeetingIdAndUserId(meetingId, userId)
                .ifPresent(f -> {
                    throw new BusinessException("You have already submitted feedback");
                });

        Feedback feedback = Feedback.builder()
                .meeting(meeting)
                .user(user)
                .rating(request.getRating())
                .comment(request.getComment())
                .createdAt(LocalDateTime.now())
                .build();

        return feedbackRepository.save(feedback);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Feedback> getMeetingFeedbacks(Long meetingId) {
        return feedbackRepository.findByMeetingId(meetingId);
    }

    @Override
    @Transactional(readOnly = true)
    public Feedback getUserFeedback(Long meetingId, Long userId) {
        return feedbackRepository.findByMeetingIdAndUserId(meetingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Double getAverageRating(Long meetingId) {
        Double avg = feedbackRepository.findAverageRatingByMeetingId(meetingId);
        return avg != null ? avg : 0.0;
    }
}
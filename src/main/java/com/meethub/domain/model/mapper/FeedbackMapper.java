package com.meethub.domain.model.mapper;

import com.meethub.domain.model.entity.Feedback;
import com.meethub.domain.model.response.FeedbackDto;
import org.springframework.stereotype.Component;

@Component
public class FeedbackMapper {

    public FeedbackDto toDto(Feedback feedback) {
        if (feedback == null) {
            return null;
        }

        return FeedbackDto.builder()
                .id(feedback.getId())
                .meetingId(feedback.getMeeting() != null ? feedback.getMeeting().getId() : null)
                .meetingTitle(feedback.getMeeting() != null ? feedback.getMeeting().getTitle() : null)
                .userId(feedback.getUser() != null ? feedback.getUser().getId() : null)
                .userFullName(feedback.getUser() != null ?
                        feedback.getUser().getFirstName() + " " + feedback.getUser().getLastName() : null)
                .userEmail(feedback.getUser() != null ? feedback.getUser().getEmail() : null)
                .rating(feedback.getRating())
                .comment(feedback.getComment())
                .createdAt(feedback.getCreatedAt())
                .updatedAt(feedback.getUpdatedAt())
                .build();
    }
}
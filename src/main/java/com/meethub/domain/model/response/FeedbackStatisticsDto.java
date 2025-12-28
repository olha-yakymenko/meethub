package com.meethub.domain.model.response;

public class FeedbackStatisticsDto {
    private Double averageRating;
    private Long feedbackCount;

    public FeedbackStatisticsDto(Double averageRating, Long feedbackCount) {
        this.averageRating = averageRating;
        this.feedbackCount = feedbackCount;
    }

    // getters
    public Double getAverageRating() { return averageRating; }
    public Long getFeedbackCount() { return feedbackCount; }
}

package com.meethub.domain.model.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatisticsResponse {
    private Long totalMeetings;
    private Long completedMeetings;
    private Long cancelledMeetings;
    private Double averageDuration;
    private Long totalParticipants;
}
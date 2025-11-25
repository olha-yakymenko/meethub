package com.meethub.domain.model.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardStatsResponse {
    private Long totalMeetings;
    private Long upcomingMeetings;
    private Long participantsCount;
    private Long organizedMeetings;

    // Możesz dodać więcej statystyk:
    private Long invitedMeetings;
    private Long confirmedMeetings;
    private Double averageParticipants;

    private Long meetingsToday;
    private Long meetingsThisWeek;
    private Long meetingsThisMonth;
}
package com.meethub.domain.model.response;

import com.meethub.domain.model.request.ReportFilter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizerReport {

    private Long organizerId;
    private LocalDateTime generatedAt;
    private ReportFilter filterApplied;

    // Podsumowanie
    private Integer totalMeetings;
    private BigDecimal averageAttendanceRate;
    private Integer totalParticipants;
    private Integer totalAttended;
    private Integer totalConfirmed;
    private Integer totalDeclined;

    // Szczegóły spotkań
    private List<MeetingDetail> meetingDetails = new ArrayList<>();

    // Statystyki trendów
    private BigDecimal attendanceTrend; // % zmiany w porównaniu do poprzedniego okresu
    private BigDecimal participantTrend;
    private Integer meetingsThisMonth;
    private Integer meetingsPreviousMonth;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MeetingDetail {
        private Long meetingId;
        private String meetingTitle;
        private LocalDateTime meetingDate;
        private BigDecimal attendanceRate;
        private Integer totalParticipants;
        private Integer attendedParticipants;
        private Integer confirmedParticipants;
        private Integer declinedParticipants;
        private BigDecimal avgResponseTime;
        private BigDecimal averageRating;
        private Integer feedbackCount;
    }
}
// com.meethub.domain.model.response.MeetingStatisticsResponse.java
package com.meethub.domain.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingStatisticsResponse {
    private Long id;
    private Long meetingId;
    private String meetingTitle;

    // Podstawowe metryki
    private Integer totalParticipants;
    private Integer attendedParticipants;
    private Integer confirmedParticipants;
    private Integer declinedParticipants;
    private Integer pendingParticipants;

    // Frekwencja
    private BigDecimal attendanceRate;
    private BigDecimal confirmationRate;

    // Czasy odpowiedzi
    private BigDecimal avgResponseTimeMinutes;

    // Feedback
    private BigDecimal averageRating;
    private Integer feedbackCount;

    // Statusy
    private String status;
    private Boolean finalized;

    // Timestamps
    private LocalDateTime generatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Dodatkowe metryki (opcjonalnie)
    private Map<String, Object> additionalMetrics;
}
package com.meethub.domain.model.entity;

import com.meethub.domain.model.entity.MeetingStatistics.StatisticsStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class MeetingStatisticsTest {

    @Test
    void shouldCreateMeetingStatisticsWithBuilder() {
        // Given
        Meeting meeting = mock(Meeting.class);
        LocalDateTime generatedAt = LocalDateTime.now();
        LocalDateTime validUntil = generatedAt.plusDays(7);

        // When
        MeetingStatistics statistics = MeetingStatistics.builder()
                .meeting(meeting)
                .totalParticipants(50)
                .attendedParticipants(40)
                .confirmedParticipants(45)
                .declinedParticipants(3)
                .pendingParticipants(2)
                .attendanceRate(new BigDecimal("80.00"))
                .confirmationRate(new BigDecimal("90.00"))
                .avgResponseTimeMinutes(new BigDecimal("120.50"))
                .averageRating(new BigDecimal("4.25"))
                .feedbackCount(35)
                .totalCost(new BigDecimal("5000.00"))
                .status(StatisticsStatus.FINAL)
                .finalized(true)
                .generatedAt(generatedAt)
                .validUntil(validUntil)
                .build();

        // Then
        assertAll(
                () -> assertThat(statistics.getMeeting()).isEqualTo(meeting),
                () -> assertThat(statistics.getTotalParticipants()).isEqualTo(50),
                () -> assertThat(statistics.getAttendedParticipants()).isEqualTo(40),
                () -> assertThat(statistics.getConfirmedParticipants()).isEqualTo(45),
                () -> assertThat(statistics.getDeclinedParticipants()).isEqualTo(3),
                () -> assertThat(statistics.getPendingParticipants()).isEqualTo(2),
                () -> assertThat(statistics.getAttendanceRate()).isEqualTo(new BigDecimal("80.00")),
                () -> assertThat(statistics.getConfirmationRate()).isEqualTo(new BigDecimal("90.00")),
                () -> assertThat(statistics.getAvgResponseTimeMinutes()).isEqualTo(new BigDecimal("120.50")),
                () -> assertThat(statistics.getAverageRating()).isEqualTo(new BigDecimal("4.25")),
                () -> assertThat(statistics.getFeedbackCount()).isEqualTo(35),
                () -> assertThat(statistics.getTotalCost()).isEqualTo(new BigDecimal("5000.00")),
                () -> assertThat(statistics.getStatus()).isEqualTo(StatisticsStatus.FINAL),
                () -> assertThat(statistics.getFinalized()).isTrue(),
                () -> assertThat(statistics.getGeneratedAt()).isEqualTo(generatedAt),
                () -> assertThat(statistics.getValidUntil()).isEqualTo(validUntil),
                () -> assertThat(statistics.getAdditionalMetrics()).isNotNull()
        );
    }

    @Test
    void shouldCalculateDerivedMetrics() {
        // Given
        MeetingStatistics statistics = MeetingStatistics.builder()
                .totalParticipants(100)
                .attendedParticipants(80)
                .confirmedParticipants(90)
                .totalCost(new BigDecimal("1000.00"))
                .build();

        // When
        statistics.calculateDerivedMetrics();

        // Then
        assertAll(
                () -> assertThat(statistics.getAttendanceRate()).isEqualTo(new BigDecimal("80.00")),
                () -> assertThat(statistics.getConfirmationRate()).isEqualTo(new BigDecimal("90.00")),
                () -> assertThat(statistics.getCostPerParticipant()).isEqualTo(new BigDecimal("12.50"))
        );
    }

    @Test
    void shouldHandleZeroParticipantsInCalculations() {
        // Given
        MeetingStatistics statistics = MeetingStatistics.builder()
                .totalParticipants(0)
                .attendedParticipants(0)
                .confirmedParticipants(0)
                .totalCost(BigDecimal.ZERO)
                .build();

        // When
        statistics.calculateDerivedMetrics();

        // Then
        assertAll(
                () -> assertThat(statistics.getAttendanceRate()).isEqualTo(BigDecimal.ZERO),
                () -> assertThat(statistics.getConfirmationRate()).isEqualTo(BigDecimal.ZERO),
                () -> assertThat(statistics.getCostPerParticipant()).isEqualTo(BigDecimal.ZERO)
        );
    }

    @Test
    void shouldUpdateFromParticipantData() {
        // Given
        MeetingStatistics statistics = new MeetingStatistics();
        MeetingStatistics.ParticipantData data = MeetingStatistics.ParticipantData.builder()
                .totalCount(50)
                .attendedCount(40)
                .confirmedCount(45)
                .declinedCount(3)
                .pendingCount(2)
                .avgResponseTime(new BigDecimal("120.50"))
                .avgParticipationTime(new BigDecimal("60.75"))
                .build();

        // When
        statistics.updateFromParticipantData(data);

        // Then
        assertAll(
                () -> assertThat(statistics.getTotalParticipants()).isEqualTo(50),
                () -> assertThat(statistics.getAttendedParticipants()).isEqualTo(40),
                () -> assertThat(statistics.getConfirmedParticipants()).isEqualTo(45),
                () -> assertThat(statistics.getDeclinedParticipants()).isEqualTo(3),
                () -> assertThat(statistics.getPendingParticipants()).isEqualTo(2),
                () -> assertThat(statistics.getAvgResponseTimeMinutes()).isEqualTo(new BigDecimal("120.50")),
                () -> assertThat(statistics.getAvgParticipationDurationMinutes()).isEqualTo(new BigDecimal("60.75"))
        );
    }

    @Test
    void shouldCheckIfExpired() {
        // Given - expired
        MeetingStatistics expired = MeetingStatistics.builder()
                .validUntil(LocalDateTime.now().minusDays(1))
                .build();

        // Given - not expired
        MeetingStatistics notExpired = MeetingStatistics.builder()
                .validUntil(LocalDateTime.now().plusDays(1))
                .build();

        // Given - no expiration date
        MeetingStatistics noExpiration = MeetingStatistics.builder()
                .validUntil(null)
                .build();

        // Then
        assertAll(
                () -> assertThat(expired.isExpired()).isTrue(),
                () -> assertThat(notExpired.isExpired()).isFalse(),
                () -> assertThat(noExpiration.isExpired()).isFalse()
        );
    }

    @Test
    void shouldCheckIfNeedsRefresh() {
        // Given - needs refresh (never calculated)
        MeetingStatistics neverCalculated = MeetingStatistics.builder()
                .lastCalculatedAt(null)
                .status(StatisticsStatus.DRAFT)
                .build();

        // Given - needs refresh (old calculation)
        MeetingStatistics oldCalculation = MeetingStatistics.builder()
                .lastCalculatedAt(LocalDateTime.now().minusHours(1))
                .status(StatisticsStatus.PRELIMINARY)
                .build();

        // Given - doesn't need refresh (final)
        MeetingStatistics finalStats = MeetingStatistics.builder()
                .lastCalculatedAt(LocalDateTime.now().minusDays(1))
                .status(StatisticsStatus.FINAL)
                .build();

        // Given - doesn't need refresh (archived)
        MeetingStatistics archived = MeetingStatistics.builder()
                .lastCalculatedAt(LocalDateTime.now().minusDays(2))
                .status(StatisticsStatus.ARCHIVED)
                .build();

        // Then
        assertAll(
                () -> assertThat(neverCalculated.needsRefresh()).isTrue(),
                () -> assertThat(oldCalculation.needsRefresh()).isTrue(),
                () -> assertThat(finalStats.needsRefresh()).isFalse(),
                () -> assertThat(archived.needsRefresh()).isFalse()
        );
    }

    @Test
    void shouldSetDefaultValuesOnCreate() {
        // Given
        MeetingStatistics statistics = new MeetingStatistics();

        // When
        statistics.onCreate();

        // Then
        assertAll(
                () -> assertThat(statistics.getCreatedAt()).isNotNull(),
                () -> assertThat(statistics.getUpdatedAt()).isNotNull(),
                () -> assertThat(statistics.getGeneratedAt()).isNotNull(),
                () -> assertThat(statistics.getLastCalculatedAt()).isNotNull(),
                () -> assertThat(statistics.getStatus()).isEqualTo(StatisticsStatus.DRAFT),
                () -> assertThat(statistics.getFinalized()).isFalse(),
                () -> assertThat(statistics.getAdditionalMetrics()).isNotNull(),
                () -> assertThat(statistics.getVersion()).isNull() // Version is managed by JPA
        );
    }

    @Test
    void shouldUpdateTimestampOnUpdate() {
        // Given
        MeetingStatistics statistics = new MeetingStatistics();
        statistics.onCreate();
        LocalDateTime initialUpdateTime = statistics.getUpdatedAt();

        // When
        statistics.onUpdate();

        // Then
        assertAll(
                () -> assertThat(statistics.getUpdatedAt()).isAfter(initialUpdateTime)
        );
    }

    @Test
    void shouldHandleAdditionalMetrics() {
        // Given
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("engagement_score", 85.5);
        metrics.put("satisfaction_index", 4.2);
        metrics.put("topics_covered", Arrays.asList("topic1", "topic2"));

        MeetingStatistics statistics = MeetingStatistics.builder()
                .additionalMetrics(metrics)
                .build();

        // When
        Map<String, Object> additionalMetrics = statistics.getAdditionalMetrics();

        // Then
        assertAll(
                () -> assertThat(additionalMetrics).hasSize(3),
                () -> assertThat(additionalMetrics.get("engagement_score")).isEqualTo(85.5),
                () -> assertThat(additionalMetrics.get("satisfaction_index")).isEqualTo(4.2),
                () -> assertThat(additionalMetrics.get("topics_covered")).asList().contains("topic1", "topic2")
        );
    }

    @Test
    void shouldSetGeneratedAtIfNotSet() {
        // Given - not set
        MeetingStatistics notSet = new MeetingStatistics();

        // Given - already set
        MeetingStatistics alreadySet = new MeetingStatistics();
        LocalDateTime existingTime = LocalDateTime.now().minusHours(2);
        alreadySet.setGeneratedAt(existingTime);

        // When
        notSet.setGeneratedAtIfNotSet();
        alreadySet.setGeneratedAtIfNotSet();

        // Then
        assertAll(
                () -> assertThat(notSet.getGeneratedAt()).isNotNull(),
                () -> assertThat(alreadySet.getGeneratedAt()).isEqualTo(existingTime)
        );
    }

    @Test
    void shouldCalculateCostPerParticipant() {
        // Given
        MeetingStatistics statsWithCost = MeetingStatistics.builder()
                .totalCost(new BigDecimal("1000.00"))
                .attendedParticipants(25)
                .build();

        MeetingStatistics statsZeroAttended = MeetingStatistics.builder()
                .totalCost(new BigDecimal("500.00"))
                .attendedParticipants(0)
                .build();

        MeetingStatistics statsNullCost = MeetingStatistics.builder()
                .totalCost(null)
                .attendedParticipants(10)
                .build();

        // When
        statsWithCost.calculateDerivedMetrics();
        statsZeroAttended.calculateDerivedMetrics();
        statsNullCost.calculateDerivedMetrics();

        // Then
        assertAll(
                () -> assertThat(statsWithCost.getCostPerParticipant()).isEqualTo(new BigDecimal("40.00")),
                () -> assertThat(statsZeroAttended.getCostPerParticipant()).isEqualTo(BigDecimal.ZERO),
                () -> assertThat(statsNullCost.getCostPerParticipant()).isEqualTo(BigDecimal.ZERO)
        );
    }
}
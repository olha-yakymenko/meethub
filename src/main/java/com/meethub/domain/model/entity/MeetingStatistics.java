//package com.meethub.domain.model.entity;
//
//import jakarta.persistence.*;
//import lombok.*;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//
//
//
//
//@Entity
//@Table(name = "meeting_statistics")
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class MeetingStatistics {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @OneToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "meeting_id", nullable = false, unique = true)
//    private Meeting meeting;
//
//    @Column(name = "total_participants")
//    private Integer totalParticipants;
//
//    @Column(name = "confirmed_participants")
//    private Integer confirmedParticipants;
//
//    @Column(name = "attended_participants")
//    private Integer attendedParticipants;
//
//    // ZMIENIĆ NA BigDecimal:
//    @Column(name = "attendance_rate", precision = 5, scale = 2)
//    private BigDecimal attendanceRate; // ZMIENIONE z Double
//
//    @Column(name = "confirmation_rate", precision = 5, scale = 2)
//    private BigDecimal confirmationRate; // ZMIENIONE z Double
//
//    @Column(name = "avg_response_time_hours", precision = 8, scale = 2)
//    private BigDecimal avgResponseTimeHours; // ZMIENIONE z Double
//
//    @Column(name = "engagement_score", precision = 5, scale = 2)
//    private BigDecimal engagementScore; // ZMIENIONE z Double
//
//    @Column(name = "task_completion_rate", precision = 5, scale = 2)
//    private BigDecimal taskCompletionRate; // ZMIENIONE z Double
//
//    @Column(name = "avg_feedback_rating", precision = 3, scale = 2)
//    private BigDecimal avgFeedbackRating; // ZMIENIONE z Double
//
//    // Reszta pól pozostaje bez zmian
//    @Column(name = "no_show_count")
//    private Integer noShowCount;
//
//    @Column(name = "feedback_count")
//    private Integer feedbackCount;
//
//    @Column(name = "generated_at", nullable = false)
//    private LocalDateTime generatedAt;
//
//    @Column(name = "created_at", nullable = false)
//    private LocalDateTime createdAt;
//
//    @Column(name = "updated_at", nullable = false)
//    private LocalDateTime updatedAt;
//
//    @PrePersist
//    protected void onCreate() {
//        createdAt = LocalDateTime.now();
//        updatedAt = LocalDateTime.now();
//        if (generatedAt == null) {
//            generatedAt = LocalDateTime.now();
//        }
//    }
//
//    @PreUpdate
//    protected void onUpdate() {
//        updatedAt = LocalDateTime.now();
//    }
//}







package com.meethub.domain.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "meeting_statistics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "meeting")
public class MeetingStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relacja z spotkaniem
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", unique = true, nullable = false)
    private Meeting meeting;

    // Podstawowe metryki
    @Column(name = "total_participants", nullable = false)
    private Integer totalParticipants;

    @Column(name = "attended_participants", nullable = false)
    private Integer attendedParticipants;

    @Column(name = "confirmed_participants")
    private Integer confirmedParticipants;

    @Column(name = "declined_participants")
    private Integer declinedParticipants;

    @Column(name = "pending_participants")
    private Integer pendingParticipants;

    // Frekwencja
    @Column(name = "attendance_rate", precision = 5, scale = 2)
    private BigDecimal attendanceRate; // ZMIENIONE z Double na BigDecimal

    @Column(name = "confirmation_rate", precision = 5, scale = 2)
    private BigDecimal confirmationRate; // ZMIENIONE z Double na BigDecimal

    // Czasy odpowiedzi
    @Column(name = "avg_response_time_minutes", precision = 10, scale = 2)
    private BigDecimal avgResponseTimeMinutes;

    @Column(name = "min_response_time_minutes", precision = 10, scale = 2)
    private BigDecimal minResponseTimeMinutes;

    @Column(name = "max_response_time_minutes", precision = 10, scale = 2)
    private BigDecimal maxResponseTimeMinutes;

    // Czas trwania i uczestnictwa
    @Column(name = "avg_join_delay_minutes", precision = 10, scale = 2)
    private BigDecimal avgJoinDelayMinutes;

    @Column(name = "avg_participation_duration_minutes", precision = 10, scale = 2)
    private BigDecimal avgParticipationDurationMinutes;

    @Column(name = "total_meeting_duration_minutes")
    private Integer totalMeetingDurationMinutes;

    // Maksymalna liczba uczestników jednocześnie
    @Column(name = "max_concurrent_participants")
    private Integer maxConcurrentParticipants;

    // Statystyki czasu rzeczywistego (dla spotkań w trakcie)
    @Column(name = "current_participants")
    private Integer currentParticipants;

    @Column(name = "peak_participants_today")
    private Integer peakParticipantsToday;

    // Feedback i oceny
    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating;

    @Column(name = "feedback_count")
    private Integer feedbackCount;

    @Column(name = "positive_feedback_count")
    private Integer positiveFeedbackCount;

    @Column(name = "negative_feedback_count")
    private Integer negativeFeedbackCount;

    // Koszty (jeśli dotyczy)
    @Column(name = "total_cost", precision = 15, scale = 2)
    private BigDecimal totalCost;

    @Column(name = "cost_per_participant", precision = 10, scale = 2)
    private BigDecimal costPerParticipant;

    // Statusy
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatisticsStatus status;

    @Column(name = "is_finalized", nullable = false)
    private Boolean finalized = false;

    @Column(name = "data_quality_score", precision = 3, scale = 2)
    private BigDecimal dataQualityScore;

    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "last_calculated_at")
    private LocalDateTime lastCalculatedAt;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    // Dodatkowe metryki w formacie JSON - używamy JdbcTypeCode z Hibernate 6
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "additional_metrics", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> additionalMetrics = new HashMap<>();

    // Wersja dla optymistycznej blokady
    @Version
    @Column(name = "version")
    private Integer version;

    // Enumeracje
    public enum StatisticsStatus {
        DRAFT,              // Statystyki w trakcie tworzenia
        CALCULATING,        // Trwa obliczanie
        PRELIMINARY,        // Wstępne dane (spotkanie w trakcie)
        PARTIAL,            // Częściowe dane
        FINAL,              // Ostateczne dane po spotkaniu
        VERIFIED,           // Zweryfikowane przez system
        ARCHIVED,           // Zarchiwizowane
        ERROR               // Błąd w obliczeniach
    }

    // Metody pomocnicze
    public void calculateDerivedMetrics() {
        // Oblicz frekwencję
        if (totalParticipants != null && totalParticipants > 0) {
            if (attendedParticipants != null) {
                BigDecimal attendance = new BigDecimal(attendedParticipants)
                        .multiply(new BigDecimal("100"))
                        .divide(new BigDecimal(totalParticipants), 2, RoundingMode.HALF_UP);
                this.attendanceRate = attendance;
            }
            if (confirmedParticipants != null) {
                BigDecimal confirmation = new BigDecimal(confirmedParticipants)
                        .multiply(new BigDecimal("100"))
                        .divide(new BigDecimal(totalParticipants), 2, RoundingMode.HALF_UP);
                this.confirmationRate = confirmation;
            }
        } else {
            this.attendanceRate = BigDecimal.ZERO;
            this.confirmationRate = BigDecimal.ZERO;
        }

        // Oblicz koszt na uczestnika
        if (totalCost != null && attendedParticipants != null && attendedParticipants > 0) {
            this.costPerParticipant = totalCost.divide(
                    new BigDecimal(attendedParticipants), 2, RoundingMode.HALF_UP);
        } else {
            this.costPerParticipant = BigDecimal.ZERO;
        }

        // Oblicz średnią ocenę
        if (feedbackCount != null && feedbackCount > 0 && averageRating != null) {
            // Utrzymaj istniejącą średnią ocenę
        } else {
            this.averageRating = BigDecimal.ZERO;
        }
    }

    public void updateFromParticipantData(ParticipantData participantData) {
        this.totalParticipants = participantData.getTotalCount();
        this.attendedParticipants = participantData.getAttendedCount();
        this.confirmedParticipants = participantData.getConfirmedCount();
        this.declinedParticipants = participantData.getDeclinedCount();
        this.pendingParticipants = participantData.getPendingCount();

        if (participantData.getAvgResponseTime() != null) {
            this.avgResponseTimeMinutes = participantData.getAvgResponseTime();
        }

        if (participantData.getAvgParticipationTime() != null) {
            this.avgParticipationDurationMinutes = participantData.getAvgParticipationTime();
        }

        calculateDerivedMetrics();
    }

    public boolean isExpired() {
        if (validUntil == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(validUntil);
    }

    public boolean needsRefresh() {
        if (status == StatisticsStatus.FINAL || status == StatisticsStatus.ARCHIVED) {
            return false;
        }

        if (lastCalculatedAt == null) {
            return true;
        }

        LocalDateTime refreshThreshold = LocalDateTime.now().minusMinutes(30);
        return lastCalculatedAt.isBefore(refreshThreshold) || isExpired();
    }

    public void setGeneratedAtIfNotSet() {
        if (this.generatedAt == null) {
            this.generatedAt = LocalDateTime.now();
        }
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (generatedAt == null) {
            generatedAt = LocalDateTime.now();
        }
        if (lastCalculatedAt == null) {
            lastCalculatedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = StatisticsStatus.DRAFT;
        }
        if (finalized == null) {
            finalized = false;
        }

        calculateDerivedMetrics();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        lastCalculatedAt = LocalDateTime.now();

        calculateDerivedMetrics();
    }

    // Wewnętrzna klasa dla danych uczestników
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantData {
        private int totalCount;
        private int attendedCount;
        private int confirmedCount;
        private int declinedCount;
        private int pendingCount;
        private BigDecimal avgResponseTime;
        private BigDecimal avgParticipationTime;
    }
}
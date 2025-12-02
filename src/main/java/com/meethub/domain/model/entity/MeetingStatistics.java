package com.meethub.domain.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;




@Entity
@Table(name = "meeting_statistics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false, unique = true)
    private Meeting meeting;

    @Column(name = "total_participants")
    private Integer totalParticipants;

    @Column(name = "confirmed_participants")
    private Integer confirmedParticipants;

    @Column(name = "attended_participants")
    private Integer attendedParticipants;

    // ZMIENIĆ NA BigDecimal:
    @Column(name = "attendance_rate", precision = 5, scale = 2)
    private BigDecimal attendanceRate; // ZMIENIONE z Double

    @Column(name = "confirmation_rate", precision = 5, scale = 2)
    private BigDecimal confirmationRate; // ZMIENIONE z Double

    @Column(name = "avg_response_time_hours", precision = 8, scale = 2)
    private BigDecimal avgResponseTimeHours; // ZMIENIONE z Double

    @Column(name = "engagement_score", precision = 5, scale = 2)
    private BigDecimal engagementScore; // ZMIENIONE z Double

    @Column(name = "task_completion_rate", precision = 5, scale = 2)
    private BigDecimal taskCompletionRate; // ZMIENIONE z Double

    @Column(name = "avg_feedback_rating", precision = 3, scale = 2)
    private BigDecimal avgFeedbackRating; // ZMIENIONE z Double

    // Reszta pól pozostaje bez zmian
    @Column(name = "no_show_count")
    private Integer noShowCount;

    @Column(name = "feedback_count")
    private Integer feedbackCount;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (generatedAt == null) {
            generatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
package com.meethub.domain.model.entity;

// 📊 Encja statystyk spotkania
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

    @Column(name = "attendance_rate")
    private Double attendanceRate; // % obecności

    @Column(name = "confirmation_rate")
    private Double confirmationRate; // % potwierdzeń

    @Column(name = "avg_response_time_hours")
    private Double avgResponseTimeHours; // Średni czas odpowiedzi

    @Column(name = "no_show_count")
    private Integer noShowCount; // Nie przyszli mimo potwierdzenia

    @Column(name = "engagement_score")
    private Double engagementScore; // 0-100 punktów zaangażowania

    @Column(name = "task_completion_rate")
    private Double taskCompletionRate; // % wykonanych zadań

    @Column(name = "feedback_count")
    private Integer feedbackCount;

    @Column(name = "avg_feedback_rating")
    private Double avgFeedbackRating;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;
}

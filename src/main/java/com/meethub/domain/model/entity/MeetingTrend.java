//package com.meethub.domain.model.entity;
//
//
//
//import jakarta.persistence.*;
//import lombok.*;
//
//import java.time.LocalDate;
//
//// 📈 Encja historycznych danych dla trendów
//@Entity
//@Table(name = "meeting_trends")
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class MeetingTrend {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "organizer_id", nullable = false)
//    private User organizer;
//
//    @Column(name = "period_date", nullable = false)
//    private LocalDate periodDate; // np. 2024-01-01 dla stycznia 2024
//
//    @Column(name = "period_type", nullable = false)
//    @Enumerated(EnumType.STRING)
//    private PeriodType periodType; // MONTHLY, QUARTERLY, YEARLY
//
//    @Column(name = "meetings_count")
//    private Integer meetingsCount;
//
//    @Column(name = "avg_attendance_rate")
//    private Double avgAttendanceRate;
//
//    @Column(name = "total_participants")
//    private Integer totalParticipants;
//
//    @Column(name = "avg_engagement_score")
//    private Double avgEngagementScore;
//
//    @Column(name = "avg_feedback_rating")
//    private Double avgFeedbackRating;
//
//    @Column(name = "most_popular_type")
//    private String mostPopularType; // Najpopularniejszy typ spotkań
//
//    @Column(name = "peak_day")
//    private String peakDay; // Dzień tygodnia z najwięcej spotkaniami
//}

package com.meethub.domain.model.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

// 🎯 Encja predykcji
@Entity
@Table(name = "meeting_predictions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;

    @Column(name = "prediction_date", nullable = false)
    private LocalDate predictionDate;

    @Column(name = "predicted_meetings")
    private Integer predictedMeetings; // Przewidywana liczba spotkań

    @Column(name = "predicted_participants")
    private Integer predictedParticipants;

    @Column(name = "predicted_attendance_rate")
    private Double predictedAttendanceRate;

    @Column(name = "best_day")
    private String bestDay; // Najlepszy dzień na spotkania

    @Column(name = "best_time")
    private String bestTime; // Najlepsza godzina

    @Column(name = "confidence_score")
    private Double confidenceScore; // Pewność predykcji 0-100%

    @Column(name = "factors_json", columnDefinition = "TEXT")
    private String factorsJson; // JSON z czynnikami wpływającymi
}
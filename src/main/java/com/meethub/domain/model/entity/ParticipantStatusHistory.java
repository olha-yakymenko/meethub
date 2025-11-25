package com.meethub.domain.model.entity;

import com.meethub.domain.model.enums.ParticipationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "participant_status_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipantStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id")
    private MeetingParticipant participant;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status")
    private ParticipationStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status")
    private ParticipationStatus newStatus;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "changed_by_user_id")
    private Long changedByUserId;

    @CreationTimestamp
    @Column(name = "changed_at")
    private LocalDateTime changedAt;
}
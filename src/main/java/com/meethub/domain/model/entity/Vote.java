package com.meethub.domain.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "votes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voting_id", nullable = false)
    private MeetingVoting voting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", nullable = false)
    private VotingOption option;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "vote_weight")
    private Integer voteWeight;

    @Column(name = "preference_order")
    private Integer preferenceOrder;

    @Column(name = "voted_at", nullable = false)
    private LocalDateTime votedAt;

    @PrePersist
    protected void onCreate() {
        votedAt = LocalDateTime.now();
    }
}

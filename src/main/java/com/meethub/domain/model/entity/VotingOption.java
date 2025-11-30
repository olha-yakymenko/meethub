package com.meethub.domain.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "voting_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VotingOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voting_id", nullable = false)
    private MeetingVoting voting;

    @Column(name = "option_date", nullable = false)
    private LocalDateTime optionDate;

    @Column(name = "option_duration_minutes")
    private Integer durationMinutes;

    @Column(name = "is_suggested")
    private Boolean isSuggested;

    @Column(name = "suggested_by")
    private Long suggestedBy;

    @OneToMany(mappedBy = "option", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Vote> votes = new ArrayList<>();
}
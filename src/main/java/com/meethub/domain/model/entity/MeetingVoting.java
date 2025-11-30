// MeetingVoting.java
package com.meethub.domain.model.entity;

import com.meethub.domain.model.enums.VotingStatus;
import com.meethub.domain.model.enums.VotingType;
import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "meeting_votings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class MeetingVoting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VotingStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VotingType type;

    @Column(name = "max_choices")
    private Integer maxChoices;

    @Column(name = "allow_suggestions")
    private Boolean allowSuggestions;

    @Column(name = "deadline_date")
    private LocalDateTime deadlineDate;

    @Column(name = "auto_close")
    private Boolean autoClose;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "voting", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<VotingOption> options = new ArrayList<>();

    @OneToMany(mappedBy = "voting", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Vote> votes = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}



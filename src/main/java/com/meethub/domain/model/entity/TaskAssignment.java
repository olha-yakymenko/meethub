package com.meethub.domain.model.entity;

import com.meethub.domain.model.enums.AssignmentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "task_assignments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING) // DODAJ TE ADNOTACJĘ
    private AssignmentStatus status; // DODAJ TO POLE

    private String comment;
    private LocalDateTime completedAt;
    private LocalDateTime assignedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL)
    @Builder.Default
    private List<TaskFile> files = new ArrayList<>();
}
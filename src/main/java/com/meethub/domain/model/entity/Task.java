package com.meethub.domain.model.entity;

import com.meethub.domain.model.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tasks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;

    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    private LocalDateTime deadline;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL)
    @Builder.Default
    private List<TaskAssignment> assignments = new ArrayList<>();

    // PROSTSZE ROZWIĄZANIE - przechowuj jako tekst w jednej kolumnie
    @Builder.Default
    private Boolean allowSelfAssignment = true;

    // ZAMIENIĆ NA POJEDYNCZĄ KOLUMNĘ
    @Column(name = "allowed_file_types", length = 500)
    private String allowedFileTypes; // np. "pdf,docx,jpg,png"


    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL)
    @Builder.Default
    private List<TaskFile> files = new ArrayList<>();

    @Builder.Default
    private Integer maxFilesPerUser = 10;

    @Builder.Default
    private Long maxFileSize = 10 * 1024 * 1024L;




    public Long getMeetingId() {
        return meeting != null ? meeting.getId() : null;
    }

    public void setMeetingId(Long meetingId) {
        if (this.meeting == null) {
            this.meeting = new Meeting();
        }
        this.meeting.setId(meetingId);
    }

    public Long getCreatedById() {
        return createdBy != null ? createdBy.getId() : null;
    }

    public void setCreatedById(Long createdById) {
        if (this.createdBy == null) {
            this.createdBy = new User();
        }
        this.createdBy.setId(createdById);
    }
}
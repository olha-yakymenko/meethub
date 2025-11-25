package com.meethub.domain.model.entity;

import com.meethub.domain.model.enums.TaskPriority;
import com.meethub.domain.model.enums.TaskStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "meeting_tasks")
public class MeetingTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status = TaskStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskPriority priority = TaskPriority.MEDIUM;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "progress_percentage")
    private Integer progressPercentage = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // GETTERS
    public Long getId() { return id; }
    public Meeting getMeeting() { return meeting; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public TaskStatus getStatus() { return status; }
    public TaskPriority getPriority() { return priority; }
    public User getAssignedTo() { return assignedTo; }
    public LocalDateTime getDueDate() { return dueDate; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public Integer getProgressPercentage() { return progressPercentage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // SETTERS
    public void setId(Long id) { this.id = id; }
    public void setMeeting(Meeting meeting) { this.meeting = meeting; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }
    public void setAssignedTo(User assignedTo) { this.assignedTo = assignedTo; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public void setProgressPercentage(Integer progressPercentage) { this.progressPercentage = progressPercentage; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Konstruktory
    public MeetingTask() {}

    public MeetingTask(Meeting meeting, String title) {
        this.meeting = meeting;
        this.title = title;
        this.status = TaskStatus.PENDING;
        this.priority = TaskPriority.MEDIUM;
        this.progressPercentage = 0;
    }

    public MeetingTask(Meeting meeting, String title, String description, TaskPriority priority, User assignedTo) {
        this.meeting = meeting;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.assignedTo = assignedTo;
        this.status = TaskStatus.PENDING;
        this.progressPercentage = 0;
    }

    // Metody pomocnicze
    public void markAsCompleted() {
        this.status = TaskStatus.COMPLETED;
        this.progressPercentage = 100;
        this.completedAt = LocalDateTime.now();
    }

    public void markAsInProgress() {
        this.status = TaskStatus.IN_PROGRESS;
        if (this.progressPercentage == 0) {
            this.progressPercentage = 10; // Minimalny postęp przy rozpoczęciu
        }
    }

    public void markAsPending() {
        this.status = TaskStatus.PENDING;
    }

    public void cancel() {
        this.status = TaskStatus.CANCELLED;
    }

    public void block() {
        this.status = TaskStatus.BLOCKED;
    }

    public void updateProgress(int progress) {
        if (progress < 0) progress = 0;
        if (progress > 100) progress = 100;

        this.progressPercentage = progress;

        if (progress == 100) {
            markAsCompleted();
        } else if (progress > 0) {
            this.status = TaskStatus.IN_PROGRESS;
        } else {
            this.status = TaskStatus.PENDING;
        }
    }

    public void assignTo(User user) {
        this.assignedTo = user;
    }

    public void unassign() {
        this.assignedTo = null;
    }

    public boolean isCompleted() {
        return TaskStatus.COMPLETED.equals(status);
    }

    public boolean isOverdue() {
        return dueDate != null && dueDate.isBefore(LocalDateTime.now()) && !isCompleted();
    }

    public boolean isDueSoon() {
        if (dueDate == null || isCompleted()) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dueSoonThreshold = now.plusDays(1); // Zadanie jest "wkrótce" jeśli termin za 1 dzień
        return dueDate.isBefore(dueSoonThreshold) && dueDate.isAfter(now);
    }

    public boolean isAssigned() {
        return assignedTo != null;
    }

    public boolean isHighPriority() {
        return TaskPriority.HIGH.equals(priority) || TaskPriority.URGENT.equals(priority);
    }

    public String getStatusDisplayName() {
        return status != null ? status.getDisplayName() : "Unknown";
    }

    public String getPriorityDisplayName() {
        return priority != null ? priority.getDisplayName() : "Unknown";
    }

    public long getDaysUntilDue() {
        if (dueDate == null) return Long.MAX_VALUE;

        LocalDateTime now = LocalDateTime.now();
        if (dueDate.isBefore(now)) return 0;

        return java.time.Duration.between(now, dueDate).toDays();
    }

    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MeetingTask that = (MeetingTask) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(title, that.title) &&
                Objects.equals(meeting, that.meeting);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, meeting);
    }

    // toString
    @Override
    public String toString() {
        return "MeetingTask{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", status=" + status +
                ", priority=" + priority +
                ", progressPercentage=" + progressPercentage +
                ", assignedTo=" + (assignedTo != null ? assignedTo.getEmail() : "null") +
                '}';
    }

    // Builder pattern
    public static MeetingTaskBuilder builder() {
        return new MeetingTaskBuilder();
    }

    public static class MeetingTaskBuilder {
        private Meeting meeting;
        private String title;
        private String description;
        private TaskStatus status = TaskStatus.PENDING;
        private TaskPriority priority = TaskPriority.MEDIUM;
        private User assignedTo;
        private LocalDateTime dueDate;
        private Integer progressPercentage = 0;

        public MeetingTaskBuilder meeting(Meeting meeting) {
            this.meeting = meeting;
            return this;
        }

        public MeetingTaskBuilder title(String title) {
            this.title = title;
            return this;
        }

        public MeetingTaskBuilder description(String description) {
            this.description = description;
            return this;
        }

        public MeetingTaskBuilder status(TaskStatus status) {
            this.status = status;
            return this;
        }

        public MeetingTaskBuilder priority(TaskPriority priority) {
            this.priority = priority;
            return this;
        }

        public MeetingTaskBuilder assignedTo(User assignedTo) {
            this.assignedTo = assignedTo;
            return this;
        }

        public MeetingTaskBuilder dueDate(LocalDateTime dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public MeetingTaskBuilder progressPercentage(Integer progressPercentage) {
            this.progressPercentage = progressPercentage;
            return this;
        }

        public MeetingTask build() {
            MeetingTask task = new MeetingTask();
            task.setMeeting(meeting);
            task.setTitle(title);
            task.setDescription(description);
            task.setStatus(status);
            task.setPriority(priority);
            task.setAssignedTo(assignedTo);
            task.setDueDate(dueDate);
            task.setProgressPercentage(progressPercentage);
            return task;
        }
    }
}
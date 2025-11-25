package com.meethub.domain.model.entity;

import com.meethub.domain.model.enums.MeetingStatus;
import com.meethub.domain.model.enums.MeetingType;
import com.meethub.domain.model.enums.MeetingVisibility;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "meetings", indexes = {
        @Index(name = "idx_meeting_organizer", columnList = "organizer_id"),
        @Index(name = "idx_meeting_status", columnList = "status"),
        @Index(name = "idx_meeting_start_date", columnList = "start_date"),
        @Index(name = "idx_meeting_end_date", columnList = "end_date"),
        @Index(name = "idx_meeting_visibility", columnList = "visibility"),
        @Index(name = "idx_meeting_created", columnList = "created_at")
})
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String agenda;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MeetingType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MeetingStatus status = MeetingStatus.PLANNED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MeetingVisibility visibility;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MeetingParticipant> participants = new HashSet<>();

    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MeetingResource> resources = new HashSet<>();

    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MeetingTask> tasks = new HashSet<>();

    @ElementCollection
    @CollectionTable(
            name = "meeting_tags",
            joinColumns = @JoinColumn(name = "meeting_id"),
            indexes = @Index(name = "idx_meeting_tags_tag", columnList = "tag")
    )
    @Column(name = "tag", length = 50)
    private Set<String> tags = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // GETTERS
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getAgenda() { return agenda; }
    public MeetingType getType() { return type; }
    public MeetingStatus getStatus() { return status; }
    public MeetingVisibility getVisibility() { return visibility; }
    public LocalDateTime getStartDate() { return startDate; }
    public LocalDateTime getEndDate() { return endDate; }
    public Integer getMaxParticipants() { return maxParticipants; }
    public User getOrganizer() { return organizer; }
    public Location getLocation() { return location; }
    public Set<MeetingParticipant> getParticipants() { return participants; }
    public Set<MeetingResource> getResources() { return resources; }
    public Set<MeetingTask> getTasks() { return tasks; }
    public Set<String> getTags() { return tags; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // SETTERS
    public void setId(Long id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setAgenda(String agenda) { this.agenda = agenda; }
    public void setType(MeetingType type) { this.type = type; }
    public void setStatus(MeetingStatus status) { this.status = status; }
    public void setVisibility(MeetingVisibility visibility) { this.visibility = visibility; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    public void setMaxParticipants(Integer maxParticipants) { this.maxParticipants = maxParticipants; }
    public void setOrganizer(User organizer) { this.organizer = organizer; }
    public void setLocation(Location location) { this.location = location; }
    public void setParticipants(Set<MeetingParticipant> participants) { this.participants = participants; }
    public void setResources(Set<MeetingResource> resources) { this.resources = resources; }
    public void setTasks(Set<MeetingTask> tasks) { this.tasks = tasks; }
    public void setTags(Set<String> tags) { this.tags = tags; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Konstruktory
    public Meeting() {
        // Domyślny konstruktor wymagany przez JPA
    }

    public Meeting(String title, MeetingType type, MeetingVisibility visibility,
                   LocalDateTime startDate, LocalDateTime endDate, User organizer) {
        this.title = title;
        this.type = type;
        this.visibility = visibility;
        this.startDate = startDate;
        this.endDate = endDate;
        this.organizer = organizer;
        this.status = MeetingStatus.PLANNED;
    }

    // Metody pomocnicze
    public void addParticipant(MeetingParticipant participant) {
        if (participants == null) {
            participants = new HashSet<>();
        }
        participants.add(participant);
        participant.setMeeting(this);
    }

    public void removeParticipant(MeetingParticipant participant) {
        if (participants != null) {
            participants.remove(participant);
            participant.setMeeting(null);
        }
    }

    public void addResource(MeetingResource resource) {
        if (resources == null) {
            resources = new HashSet<>();
        }
        resources.add(resource);
        resource.setMeeting(this);
    }

    public void removeResource(MeetingResource resource) {
        if (resources != null) {
            resources.remove(resource);
            resource.setMeeting(null);
        }
    }

    public void addTask(MeetingTask task) {
        if (tasks == null) {
            tasks = new HashSet<>();
        }
        tasks.add(task);
        task.setMeeting(this);
    }

    public void removeTask(MeetingTask task) {
        if (tasks != null) {
            tasks.remove(task);
            task.setMeeting(null);
        }
    }

    public void addTag(String tag) {
        if (tags == null) {
            tags = new HashSet<>();
        }
        tags.add(tag);
    }

    public void removeTag(String tag) {
        if (tags != null) {
            tags.remove(tag);
        }
    }

    // Business logic methods
    public boolean isUpcoming() {
        return startDate != null && startDate.isAfter(LocalDateTime.now());
    }

    public boolean isOngoing() {
        if (startDate == null || endDate == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return !startDate.isAfter(now) && !endDate.isBefore(now);
    }

    public boolean isPast() {
        return endDate != null && endDate.isBefore(LocalDateTime.now());
    }

    public boolean hasAvailableSpots() {
        if (maxParticipants == null) {
            return true;
        }
        if (participants == null) {
            return true;
        }
        long confirmedParticipants = participants.stream()
                .filter(p -> p != null && "CONFIRMED".equals(p.getStatus().name()))
                .count();
        return confirmedParticipants < maxParticipants;
    }

    public int getConfirmedParticipantsCount() {
        if (participants == null) {
            return 0;
        }
        return (int) participants.stream()
                .filter(p -> p != null && "CONFIRMED".equals(p.getStatus().name()))
                .count();
    }

    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Meeting meeting = (Meeting) o;
        return Objects.equals(id, meeting.id) &&
                Objects.equals(title, meeting.title) &&
                Objects.equals(startDate, meeting.startDate) &&
                Objects.equals(organizer, meeting.organizer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, startDate, organizer);
    }

    // toString
    @Override
    public String toString() {
        return "Meeting{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", type=" + type +
                ", status=" + status +
                ", visibility=" + visibility +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", maxParticipants=" + maxParticipants +
                '}';
    }

    // Builder pattern
    public static MeetingBuilder builder() {
        return new MeetingBuilder();
    }

    public static class MeetingBuilder {
        private String title;
        private String description;
        private String agenda;
        private MeetingType type;
        private MeetingVisibility visibility;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private Integer maxParticipants;
        private User organizer;
        private Location location;

        public MeetingBuilder title(String title) {
            this.title = title;
            return this;
        }

        public MeetingBuilder description(String description) {
            this.description = description;
            return this;
        }

        public MeetingBuilder agenda(String agenda) {
            this.agenda = agenda;
            return this;
        }

        public MeetingBuilder type(MeetingType type) {
            this.type = type;
            return this;
        }

        public MeetingBuilder visibility(MeetingVisibility visibility) {
            this.visibility = visibility;
            return this;
        }

        public MeetingBuilder startDate(LocalDateTime startDate) {
            this.startDate = startDate;
            return this;
        }

        public MeetingBuilder endDate(LocalDateTime endDate) {
            this.endDate = endDate;
            return this;
        }

        public MeetingBuilder maxParticipants(Integer maxParticipants) {
            this.maxParticipants = maxParticipants;
            return this;
        }

        public MeetingBuilder organizer(User organizer) {
            this.organizer = organizer;
            return this;
        }

        public MeetingBuilder location(Location location) {
            this.location = location;
            return this;
        }

        public Meeting build() {
            Meeting meeting = new Meeting();
            meeting.setTitle(title);
            meeting.setDescription(description);
            meeting.setAgenda(agenda);
            meeting.setType(type);
            meeting.setVisibility(visibility);
            meeting.setStartDate(startDate);
            meeting.setEndDate(endDate);
            meeting.setMaxParticipants(maxParticipants);
            meeting.setOrganizer(organizer);
            meeting.setLocation(location);
            return meeting;
        }
    }
}
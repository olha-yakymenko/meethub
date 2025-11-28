package com.meethub.domain.model.entity;

import com.meethub.domain.model.enums.AccessLevel;
import com.meethub.domain.model.enums.ResourceType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "meeting_resources")
public class MeetingResource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @Column(nullable = false, length = 255)
    private String filename;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 50)
    private ResourceType resourceType;

    @ElementCollection
    @CollectionTable(name = "resource_tags", joinColumns = @JoinColumn(name = "resource_id"))
    @Column(name = "tag", length = 100)
    private Set<String> tags = new HashSet<>();

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "is_current", nullable = false)
    private Boolean isCurrent = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false, length = 20)
    private AccessLevel accessLevel = AccessLevel.PARTICIPANTS;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "download_count", nullable = false)
    private Integer downloadCount = 0;

    @Column(name = "description")
    private String description;

    // GETTERS
    public Long getId() { return id; }
    public Meeting getMeeting() { return meeting; }
    public String getFilename() { return filename; }
    public String getOriginalFilename() { return originalFilename; }
    public String getFilePath() { return filePath; }
    public Long getFileSize() { return fileSize; }
    public String getMimeType() { return mimeType; }
    public ResourceType getResourceType() { return resourceType; }
    public Set<String> getTags() { return tags; }
    public Integer getVersion() { return version; }
    public Boolean getIsCurrent() { return isCurrent; }
    public User getUploadedBy() { return uploadedBy; }
    public AccessLevel getAccessLevel() { return accessLevel; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }


    // SETTERS
    public void setId(Long id) { this.id = id; }
    public void setMeeting(Meeting meeting) { this.meeting = meeting; }
    public void setFilename(String filename) { this.filename = filename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public void setResourceType(ResourceType resourceType) { this.resourceType = resourceType; }
    public void setTags(Set<String> tags) { this.tags = tags; }
    public void setVersion(Integer version) { this.version = version; }
    public void setIsCurrent(Boolean isCurrent) { this.isCurrent = isCurrent; }
    public void setUploadedBy(User uploadedBy) { this.uploadedBy = uploadedBy; }
    public void setAccessLevel(AccessLevel accessLevel) { this.accessLevel = accessLevel; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }



    public Integer getDownloadCount() { return downloadCount; }
    public void setDownloadCount(Integer downloadCount) { this.downloadCount = downloadCount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    // Metoda do inkrementacji licznika pobrań
    public void incrementDownloadCount() {
        this.downloadCount++;
    }

    // Konstruktory
    public MeetingResource() {}

    public MeetingResource(Meeting meeting, String filename, String originalFilename,
                           String filePath, ResourceType resourceType, User uploadedBy) {
        this.meeting = meeting;
        this.filename = filename;
        this.originalFilename = originalFilename;
        this.filePath = filePath;
        this.resourceType = resourceType;
        this.uploadedBy = uploadedBy;
    }

    // Metody pomocnicze
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

    public void incrementVersion() {
        this.version = version + 1;
    }

    public void archive() {
        this.isCurrent = false;
    }

    public void restore() {
        this.isCurrent = true;
    }

    public String getFileSizeFormatted() {
        if (fileSize == null) return "0 B";

        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else if (fileSize < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", fileSize / (1024.0 * 1024.0 * 1024.0));
        }
    }

    public boolean isImage() {
        return resourceType == ResourceType.IMAGE ||
                (mimeType != null && mimeType.startsWith("image/"));
    }

    public boolean isDocument() {
        return resourceType == ResourceType.DOCUMENT ||
                (mimeType != null && (mimeType.contains("pdf") ||
                        mimeType.contains("word") || mimeType.contains("document")));
    }

    public boolean isPresentation() {
        return resourceType == ResourceType.PRESENTATION ||
                (mimeType != null && mimeType.contains("presentation"));
    }

//    public boolean canUserAccess(User user, Meeting meeting) {
//        if (accessLevel == AccessLevel.PUBLIC) {
//            return true;
//        }
//        if (accessLevel == AccessLevel.PARTICIPANTS) {
//            return meeting.getParticipants().stream()
//                    .anyMatch(p -> p.getUser().equals(user) && p.isConfirmed());
//        }
//        if (accessLevel == AccessLevel.ORGANIZERS) {
//            return meeting.getOrganizer().equals(user);
//        }
//        if (accessLevel == AccessLevel.PRIVATE) {
//            return uploadedBy.equals(user);
//        }
//        return false;
//    }


    public boolean canUserAccess(User user, Meeting meeting) {
        if (user == null || meeting == null) {
            return false;
        }

        if (accessLevel == AccessLevel.PUBLIC) {
            return true;
        }

        if (accessLevel == AccessLevel.PARTICIPANTS) {
            // ✅ ORGANIZATOR MA DOSTĘP DO WSZYSTKICH ZASOBÓW PARTICIPANTS
            boolean isOrganizer = meeting.getOrganizer().equals(user);
            if (isOrganizer) {
                return true;
            }

            // ✅ SPRAWDŹ CZY JEST UCZESTNIKIEM
            return meeting.getParticipants().stream()
                    .anyMatch(p -> p.getUser().equals(user) && p.isConfirmed());
        }

        if (accessLevel == AccessLevel.ORGANIZERS) {
            return meeting.getOrganizer().equals(user);
        }

        if (accessLevel == AccessLevel.PRIVATE) {
            // ✅ ORGANIZATOR MA DOSTĘP DO WSZYSTKICH ZASOBÓW (NAWET PRIVATE)
            boolean isOrganizer = meeting.getOrganizer().equals(user);
            if (isOrganizer) {
                return true;
            }

            return uploadedBy.equals(user);
        }

        return false;
    }

    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MeetingResource that = (MeetingResource) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(filename, that.filename) &&
                Objects.equals(meeting, that.meeting);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, filename, meeting);
    }

    // toString
    @Override
    public String toString() {
        return "MeetingResource{" +
                "id=" + id +
                ", filename='" + filename + '\'' +
                ", originalFilename='" + originalFilename + '\'' +
                ", resourceType=" + resourceType +
                ", version=" + version +
                ", isCurrent=" + isCurrent +
                '}';
    }

    // Builder pattern
    public static MeetingResourceBuilder builder() {
        return new MeetingResourceBuilder();
    }

    public static class MeetingResourceBuilder {
        private Meeting meeting;
        private String filename;
        private String originalFilename;
        private String filePath;
        private Long fileSize;
        private String mimeType;
        private ResourceType resourceType;
        private User uploadedBy;
        private AccessLevel accessLevel = AccessLevel.PARTICIPANTS;

        public MeetingResourceBuilder meeting(Meeting meeting) {
            this.meeting = meeting;
            return this;
        }

        public MeetingResourceBuilder filename(String filename) {
            this.filename = filename;
            return this;
        }

        public MeetingResourceBuilder originalFilename(String originalFilename) {
            this.originalFilename = originalFilename;
            return this;
        }

        public MeetingResourceBuilder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public MeetingResourceBuilder fileSize(Long fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public MeetingResourceBuilder mimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public MeetingResourceBuilder resourceType(ResourceType resourceType) {
            this.resourceType = resourceType;
            return this;
        }

        public MeetingResourceBuilder uploadedBy(User uploadedBy) {
            this.uploadedBy = uploadedBy;
            return this;
        }

        public MeetingResourceBuilder accessLevel(AccessLevel accessLevel) {
            this.accessLevel = accessLevel;
            return this;
        }

        public MeetingResource build() {
            MeetingResource resource = new MeetingResource();
            resource.setMeeting(meeting);
            resource.setFilename(filename);
            resource.setOriginalFilename(originalFilename);
            resource.setFilePath(filePath);
            resource.setFileSize(fileSize);
            resource.setMimeType(mimeType);
            resource.setResourceType(resourceType);
            resource.setUploadedBy(uploadedBy);
            resource.setAccessLevel(accessLevel);
            return resource;
        }
    }
}
package com.meethub.domain.model.response;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.enums.MeetingStatus;
import com.meethub.domain.model.enums.MeetingType;
import com.meethub.domain.model.enums.MeetingVisibility;
import com.meethub.domain.model.enums.ParticipationStatus;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
//@SuperBuilder(toBuilder = true)
public class MeetingResponse {
    private Long id;
    private String title;
    private String description;
    private String agenda;
    private MeetingType type;
    private MeetingStatus status;
    private MeetingVisibility visibility;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer maxParticipants;
    private UserResponse organizer;
    private LocationResponse location;
    private Set<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Integer confirmedParticipantsCount;
    private Integer waitingListCount;
    private Integer availableSpots;

    // ✅ ISTNIEJĄCE POLA TRANSIENT
    private boolean userIsParticipant = false;
    private boolean userIsOrganizer = false;
    private ParticipationStatus userParticipationStatus;

    // ✅ DODATKOWE POLA TRANSIENT DLA UI
    private boolean canJoin = false;
    private boolean canLeave = false;
    private boolean canEdit = false;
    private boolean canDelete = false;

    // ✅ NOWE POLA DLA PREWZYJNEGO STATUSU UCZESTNICTWA
    private boolean userIsConfirmed = false;
    private boolean userIsPending = false;
    private boolean userIsInvited = false;
    private boolean userIsDeclined = false;
    private boolean userIsWaiting = false;
    private boolean userIsViewer = false;
    private boolean userIsUnrelated = false;
    private String userRole = "VIEWER";

    // ✅ DODANE POLA DLA NOWYCH FUNKCJI
    private boolean recurring = false;
    private String recurrencePattern;
    private LocalDateTime recurrenceEndDate;
    private List<String> recurrenceExceptions;

    private Set<CategoryResponse> categories;
    private boolean isTemplate = false;
    private Long originalMeetingId;

    private List<StatusChangeResponse> statusHistory;

    // ✅ Konstruktor bezargumentowy
    public MeetingResponse() {
    }

    // ✅ Konstruktor z wszystkimi polami
    public MeetingResponse(
            Long id, String title, String description, String agenda,
            MeetingType type, MeetingStatus status, MeetingVisibility visibility,
            LocalDateTime startDate, LocalDateTime endDate, Integer maxParticipants,
            UserResponse organizer, LocationResponse location, Set<String> tags,
            LocalDateTime createdAt, LocalDateTime updatedAt,
            Integer confirmedParticipantsCount, Integer waitingListCount,
            Integer availableSpots,
            boolean userIsParticipant, boolean userIsOrganizer,
            ParticipationStatus userParticipationStatus,
            boolean canJoin, boolean canLeave, boolean canEdit, boolean canDelete,
            boolean userIsConfirmed, boolean userIsPending, boolean userIsInvited,
            boolean userIsDeclined, boolean userIsWaiting, boolean userIsViewer,
            boolean userIsUnrelated, String userRole,
            boolean recurring, String recurrencePattern, LocalDateTime recurrenceEndDate,
            List<String> recurrenceExceptions, Set<CategoryResponse> categories,
            boolean isTemplate, Long originalMeetingId, List<StatusChangeResponse> statusHistory) {

        this.id = id;
        this.title = title;
        this.description = description;
        this.agenda = agenda;
        this.type = type;
        this.status = status;
        this.visibility = visibility;
        this.startDate = startDate;
        this.endDate = endDate;
        this.maxParticipants = maxParticipants;
        this.organizer = organizer;
        this.location = location;
        this.tags = tags;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.confirmedParticipantsCount = confirmedParticipantsCount;
        this.waitingListCount = waitingListCount;
        this.availableSpots = availableSpots;
        this.userIsParticipant = userIsParticipant;
        this.userIsOrganizer = userIsOrganizer;
        this.userParticipationStatus = userParticipationStatus;
        this.canJoin = canJoin;
        this.canLeave = canLeave;
        this.canEdit = canEdit;
        this.canDelete = canDelete;
//        this.userIsConfirmed = userIsConfirmed;
        this.userIsPending = userIsPending;
        this.userIsInvited = userIsInvited;
        this.userIsDeclined = userIsDeclined;
        this.userIsWaiting = userIsWaiting;
        this.userIsViewer = userIsViewer;
        this.userIsUnrelated = userIsUnrelated;
        this.userRole = userRole;
        this.recurring = recurring;
        this.recurrencePattern = recurrencePattern;
        this.recurrenceEndDate = recurrenceEndDate;
        this.recurrenceExceptions = recurrenceExceptions;
        this.categories = categories;
        this.isTemplate = isTemplate;
        this.originalMeetingId = originalMeetingId;
        this.statusHistory = statusHistory;
    }

    // ✅ GETTERY I SETTERY (Lombok powinien wygenerować, ale tu dla pewności)

    // ✅ METODY POMOCNICZE DLA NOWYCH FUNKCJI
    public boolean hasRecurrenceEnded() {
        if (!recurring || recurrenceEndDate == null) return false;
        return recurrenceEndDate.isBefore(LocalDateTime.now());
    }

    public boolean isSeries() {
        return recurring && recurrencePattern != null;
    }

    public String getRecurrenceDisplayName() {
        if (!recurring || recurrencePattern == null) return "Brak";

        String[] parts = recurrencePattern.split(":");
        String frequency = parts[0];

        switch (frequency) {
            case "DAILY": return "Codziennie";
            case "WEEKLY":
                int weeks = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                return weeks == 1 ? "Co tydzień" : "Co " + weeks + " tygodnie";
            case "MONTHLY":
                int months = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                if (parts.length > 2) {
                    int day = Integer.parseInt(parts[2]);
                    return months == 1 ? "Co miesiąc (dzień " + day + ")" : "Co " + months + " miesięcy (dzień " + day + ")";
                }
                return months == 1 ? "Co miesiąc" : "Co " + months + " miesięcy";
            case "YEARLY":
                int years = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                return years == 1 ? "Co rok" : "Co " + years + " lat";
            default: return "Niestandardowe";
        }
    }

    public boolean hasCategories() {
        return categories != null && !categories.isEmpty();
    }

    public boolean isCopy() {
        return originalMeetingId != null;
    }

    public boolean hasStatusHistory() {
        return statusHistory != null && !statusHistory.isEmpty();
    }

    // ✅ Reszta istniejących metod...
    public boolean hasAvailableSpots() {
        return availableSpots == null || availableSpots > 0;
    }

    public boolean isPublic() {
        return visibility != null && visibility.equals(MeetingVisibility.PUBLIC);
    }

    public boolean isPrivate() {
        return visibility != null && visibility.equals(MeetingVisibility.PRIVATE);
    }

    public boolean isUpcoming() {
        return startDate != null && startDate.isAfter(LocalDateTime.now());
    }

    public boolean isOngoing() {
        LocalDateTime now = LocalDateTime.now();
        return startDate != null && endDate != null &&
                startDate.isBefore(now) && endDate.isAfter(now);
    }

    public boolean isCompleted() {
        return endDate != null && endDate.isBefore(LocalDateTime.now()) ||
                (status != null && status.equals(MeetingStatus.COMPLETED));
    }

    public boolean isCancelled() {
        return status != null && status.equals(MeetingStatus.CANCELLED);
    }

    public boolean isOnline() {
        return type != null && type.equals(MeetingType.ONLINE);
    }

    public boolean isPhysical() {
        return type != null && type.equals(MeetingType.PHYSICAL);
    }

    public boolean isHybrid() {
        return type != null && type.equals(MeetingType.HYBRID);
    }

    // ✅ Metody dla UI
    public String getUserRoleBadgeColor() {
        switch (userRole) {
            case "ORGANIZER": return "danger";
            case "CONFIRMED": return "success";
            case "PENDING": return "warning";
            case "INVITED": return "primary";
            case "VIEWER": return "info";
            case "DECLINED": return "secondary";
            case "WAITING_LIST": return "dark";
            default: return "light";
        }
    }

    public String getUserRoleDisplayName() {
        switch (userRole) {
            case "ORGANIZER": return "Organizator";
            case "CONFIRMED": return "Uczestnik";
            case "PENDING": return "Oczekujący";
            case "INVITED": return "Zaproszony";
            case "VIEWER": return "Obserwator";
            case "DECLINED": return "Odmówił";
            case "WAITING_LIST": return "Lista oczekujących";
            default: return "Obserwator";
        }
    }

    // ✅ STATYCZNY BUILDER METODA
    public static MeetingResponseBuilder builder() {
        return new MeetingResponseBuilder();
    }

    // ✅ PUBLICZNA KLASA BUILDERA
    public static class MeetingResponseBuilder {
        private Long id;
        private String title;
        private String description;
        private String agenda;
        private MeetingType type;
        private MeetingStatus status;
        private MeetingVisibility visibility;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private Integer maxParticipants;
        private UserResponse organizer;
        private LocationResponse location;
        private Set<String> tags;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private Integer confirmedParticipantsCount;
        private Integer waitingListCount;
        private Integer availableSpots;
        private boolean userIsParticipant = false;
        private boolean userIsOrganizer = false;
        private ParticipationStatus userParticipationStatus;
        private boolean canJoin = false;
        private boolean canLeave = false;
        private boolean canEdit = false;
        private boolean canDelete = false;
        private boolean userIsConfirmed = false;
        private boolean userIsPending = false;
        private boolean userIsInvited = false;
        private boolean userIsDeclined = false;
        private boolean userIsWaiting = false;
        private boolean userIsViewer = false;
        private boolean userIsUnrelated = false;
        private String userRole = "VIEWER";
        private boolean recurring = false;
        private String recurrencePattern;
        private LocalDateTime recurrenceEndDate;
        private List<String> recurrenceExceptions;
        private Set<CategoryResponse> categories;
        private boolean isTemplate = false;
        private Long originalMeetingId;
        private List<StatusChangeResponse> statusHistory;

        public MeetingResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }



        public MeetingResponseBuilder title(String title) {
            this.title = title;
            return this;
        }

        public MeetingResponseBuilder description(String description) {
            this.description = description;
            return this;
        }

        public MeetingResponseBuilder agenda(String agenda) {
            this.agenda = agenda;
            return this;
        }

        public MeetingResponseBuilder type(MeetingType type) {
            this.type = type;
            return this;
        }

        public MeetingResponseBuilder status(MeetingStatus status) {
            this.status = status;
            return this;
        }

        public MeetingResponseBuilder visibility(MeetingVisibility visibility) {
            this.visibility = visibility;
            return this;
        }

        public MeetingResponseBuilder startDate(LocalDateTime startDate) {
            this.startDate = startDate;
            return this;
        }

        public MeetingResponseBuilder endDate(LocalDateTime endDate) {
            this.endDate = endDate;
            return this;
        }

        public MeetingResponseBuilder maxParticipants(Integer maxParticipants) {
            this.maxParticipants = maxParticipants;
            return this;
        }

        public MeetingResponseBuilder organizer(UserResponse organizer) {
            this.organizer = organizer;
            return this;
        }

        public MeetingResponseBuilder location(LocationResponse location) {
            this.location = location;
            return this;
        }

        public MeetingResponseBuilder tags(Set<String> tags) {
            this.tags = tags;
            return this;
        }

        public MeetingResponseBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public MeetingResponseBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public MeetingResponseBuilder confirmedParticipantsCount(Integer confirmedParticipantsCount) {
            this.confirmedParticipantsCount = confirmedParticipantsCount;
            return this;
        }

        public MeetingResponseBuilder waitingListCount(Integer waitingListCount) {
            this.waitingListCount = waitingListCount;
            return this;
        }

        public MeetingResponseBuilder availableSpots(Integer availableSpots) {
            this.availableSpots = availableSpots;
            return this;
        }

        public MeetingResponseBuilder userIsParticipant(boolean userIsParticipant) {
            this.userIsParticipant = userIsParticipant;
            return this;
        }

        public MeetingResponseBuilder userIsOrganizer(boolean userIsOrganizer) {
            this.userIsOrganizer = userIsOrganizer;
            return this;
        }

        public MeetingResponseBuilder userParticipationStatus(ParticipationStatus userParticipationStatus) {
            this.userParticipationStatus = userParticipationStatus;
            return this;
        }

        public MeetingResponseBuilder canJoin(boolean canJoin) {
            this.canJoin = canJoin;
            return this;
        }

        public MeetingResponseBuilder canLeave(boolean canLeave) {
            this.canLeave = canLeave;
            return this;
        }

        public MeetingResponseBuilder canEdit(boolean canEdit) {
            this.canEdit = canEdit;
            return this;
        }

        public MeetingResponseBuilder canDelete(boolean canDelete) {
            this.canDelete = canDelete;
            return this;
        }

        public MeetingResponseBuilder userIsConfirmed(boolean userIsConfirmed) {
            this.userIsConfirmed = userIsConfirmed;
            return this;
        }

        public MeetingResponseBuilder userIsPending(boolean userIsPending) {
            this.userIsPending = userIsPending;
            return this;
        }

        public MeetingResponseBuilder userIsInvited(boolean userIsInvited) {
            this.userIsInvited = userIsInvited;
            return this;
        }

        public MeetingResponseBuilder userIsDeclined(boolean userIsDeclined) {
            this.userIsDeclined = userIsDeclined;
            return this;
        }

        public MeetingResponseBuilder userIsWaiting(boolean userIsWaiting) {
            this.userIsWaiting = userIsWaiting;
            return this;
        }

        public MeetingResponseBuilder userIsViewer(boolean userIsViewer) {
            this.userIsViewer = userIsViewer;
            return this;
        }

        public MeetingResponseBuilder userIsUnrelated(boolean userIsUnrelated) {
            this.userIsUnrelated = userIsUnrelated;
            return this;
        }

        public MeetingResponseBuilder userRole(String userRole) {
            this.userRole = userRole;
            return this;
        }

        public MeetingResponseBuilder recurring(boolean recurring) {
            this.recurring = recurring;
            return this;
        }

        public MeetingResponseBuilder recurrencePattern(String recurrencePattern) {
            this.recurrencePattern = recurrencePattern;
            return this;
        }

        public MeetingResponseBuilder recurrenceEndDate(LocalDateTime recurrenceEndDate) {
            this.recurrenceEndDate = recurrenceEndDate;
            return this;
        }

        public MeetingResponseBuilder recurrenceExceptions(List<String> recurrenceExceptions) {
            this.recurrenceExceptions = recurrenceExceptions;
            return this;
        }

        public MeetingResponseBuilder categories(Set<CategoryResponse> categories) {
            this.categories = categories;
            return this;
        }

        public MeetingResponseBuilder isTemplate(boolean isTemplate) {
            this.isTemplate = isTemplate;
            return this;
        }

        public MeetingResponseBuilder originalMeetingId(Long originalMeetingId) {
            this.originalMeetingId = originalMeetingId;
            return this;
        }

        public MeetingResponseBuilder statusHistory(List<StatusChangeResponse> statusHistory) {
            this.statusHistory = statusHistory;
            return this;
        }

        public MeetingResponse build() {
            return new MeetingResponse(
                    id, title, description, agenda, type, status, visibility,
                    startDate, endDate, maxParticipants, organizer, location, tags,
                    createdAt, updatedAt, confirmedParticipantsCount, waitingListCount,
                    availableSpots, userIsParticipant, userIsOrganizer, userParticipationStatus,
                    canJoin, canLeave, canEdit, canDelete, userIsConfirmed, userIsPending,
                    userIsInvited, userIsDeclined, userIsWaiting, userIsViewer, userIsUnrelated,
                    userRole, recurring, recurrencePattern, recurrenceEndDate, recurrenceExceptions,
                    categories, isTemplate, originalMeetingId, statusHistory
            );
        }
    }


    public static MeetingResponse fromEntity(Meeting meeting, Long currentUserId) {
        if (meeting == null) return null;

        boolean isOrganizer = meeting.getOrganizer() != null
                && meeting.getOrganizer().getId().equals(currentUserId);
        boolean isParticipant = meeting.getParticipants() != null
                && meeting.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(currentUserId));

        Integer confirmedCount = meeting.getParticipants() != null
                ? (int) meeting.getParticipants().stream()
                .filter(p -> p.getStatus() == ParticipationStatus.CONFIRMED)
                .count()
                : 0;

        Integer waitingCount = meeting.getParticipants() != null
                ? (int) meeting.getParticipants().stream()
                .filter(p -> p.getStatus() == ParticipationStatus.PENDING)
                .count()
                : 0;

        return MeetingResponse.builder()
                .id(meeting.getId())
                .title(meeting.getTitle())
                .description(meeting.getDescription())
                .agenda(meeting.getAgenda())
                .type(meeting.getType())
                .status(meeting.getStatus())
                .visibility(meeting.getVisibility())
                .startDate(meeting.getStartDate())
                .endDate(meeting.getEndDate())
                .maxParticipants(meeting.getMaxParticipants())
                .organizer(meeting.getOrganizer() != null
                        ? UserResponse.builder()
                        .id(meeting.getOrganizer().getId())
                        .firstName(meeting.getOrganizer().getFirstName())
                        .lastName(meeting.getOrganizer().getLastName())
                        .email(meeting.getOrganizer().getEmail())
                        .build()
                        : null)
                .tags(meeting.getTags())
                .createdAt(meeting.getCreatedAt())
                .updatedAt(meeting.getUpdatedAt())
                .recurring(meeting.isRecurring())
                .recurrencePattern(meeting.getRecurrencePattern())
                .recurrenceEndDate(meeting.getRecurrenceEndDate())
                .isTemplate(meeting.isTemplate())
                .originalMeetingId(meeting.getOriginalMeetingId())
                .categories(meeting.getCategories() != null
                        ? meeting.getCategories().stream()
                        .map(c -> CategoryResponse.builder()
                                .id(c.getId())
                                .name(c.getName())
                                .build())
                        .collect(Collectors.toSet())
                        : null)
                .userIsOrganizer(isOrganizer)
                .userIsParticipant(isParticipant)
                .confirmedParticipantsCount(confirmedCount)
                .waitingListCount(waitingCount)
                .availableSpots(meeting.getMaxParticipants() != null
                        ? meeting.getMaxParticipants() - confirmedCount
                        : null)
                .build();
    }






}



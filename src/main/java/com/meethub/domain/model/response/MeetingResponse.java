////package com.meethub.domain.model.response;
////
////import com.meethub.domain.model.enums.MeetingStatus;
////import com.meethub.domain.model.enums.MeetingType;
////import com.meethub.domain.model.enums.MeetingVisibility;
////import lombok.Data;
////
////import java.time.LocalDateTime;
////import java.util.Set;
////
////@Data
////public class MeetingResponse {
////    private Long id;
////    private String title;
////    private String description;
////
////    public Long getId() {
////        return id;
////    }
////
////    public String getTitle() {
////        return title;
////    }
////
////    public String getDescription() {
////        return description;
////    }
////
////    public MeetingType getType() {
////        return type;
////    }
////
////    public MeetingStatus getStatus() {
////        return status;
////    }
////
////    public String getAgenda() {
////        return agenda;
////    }
////
////    public MeetingVisibility getVisibility() {
////        return visibility;
////    }
////
////    public LocalDateTime getStartDate() {
////        return startDate;
////    }
////
////    public LocalDateTime getEndDate() {
////        return endDate;
////    }
////
////    public Integer getMaxParticipants() {
////        return maxParticipants;
////    }
////
////    public UserResponse getOrganizer() {
////        return organizer;
////    }
////
////    public LocationResponse getLocation() {
////        return location;
////    }
////
////    public Set<String> getTags() {
////        return tags;
////    }
////
////    public LocalDateTime getCreatedAt() {
////        return createdAt;
////    }
////
////    public LocalDateTime getUpdatedAt() {
////        return updatedAt;
////    }
////
////    public Integer getConfirmedParticipantsCount() {
////        return confirmedParticipantsCount;
////    }
////
////    public Integer getWaitingListCount() {
////        return waitingListCount;
////    }
////
////    public Integer getAvailableSpots() {
////        return availableSpots;
////    }
////
////    private String agenda;
////    private MeetingType type;
////    private MeetingStatus status;
////    private MeetingVisibility visibility;
////    private LocalDateTime startDate;
////    private LocalDateTime endDate;
////    private Integer maxParticipants;
////    private UserResponse organizer;
////    private LocationResponse location;
////    private Set<String> tags;
////    private LocalDateTime createdAt;
////    private LocalDateTime updatedAt;
////
////    private Integer confirmedParticipantsCount;
////    private Integer waitingListCount;
////    private Integer availableSpots;
////
////    // Metoda pomocnicza
////    public boolean hasAvailableSpots() {
////        return availableSpots == null || availableSpots > 0;
////    }
////}
//
//
//
//
//
//
//
//
//package com.meethub.domain.model.response;
//
//import com.meethub.domain.model.enums.MeetingStatus;
//import com.meethub.domain.model.enums.MeetingType;
//import com.meethub.domain.model.enums.MeetingVisibility;
//import com.meethub.domain.model.enums.ParticipationStatus;
//import lombok.Data;
//
//import java.time.LocalDateTime;
//import java.util.Set;
//
//@Data
//public class MeetingResponse {
//    private Long id;
//    private String title;
//    private String description;
//    private String agenda;
//    private MeetingType type;
//    private MeetingStatus status;
//    private MeetingVisibility visibility;
//    private LocalDateTime startDate;
//    private LocalDateTime endDate;
//    private Integer maxParticipants;
//    private UserResponse organizer;
//    private LocationResponse location;
//    private Set<String> tags;
//    private LocalDateTime createdAt;
//    private LocalDateTime updatedAt;
//
//    private Integer confirmedParticipantsCount;
//    private Integer waitingListCount;
//    private Integer availableSpots;
//
//    // ✅ DODANE POLA TRANSIENT DLA UCZESTNICTWA UŻYTKOWNIKA
//    private boolean userIsParticipant = false;
//    private boolean userIsOrganizer = false;
//    private ParticipationStatus userParticipationStatus;
//
//    // ✅ DODATKOWE POLA TRANSIENT DLA UI
//    private boolean canJoin = false;
//    private boolean canLeave = false;
//    private boolean canEdit = false;
//    private boolean canDelete = false;
//
//    // Gettery - jeśli Lombok nie generuje poprawnie dla pól boolean
//    public boolean isUserIsParticipant() {
//        return userIsParticipant;
//    }
//
//    public boolean isUserIsOrganizer() {
//        return userIsOrganizer;
//    }
//
//    public boolean isCanJoin() {
//        return canJoin;
//    }
//
//    public boolean isCanLeave() {
//        return canLeave;
//    }
//
//    public boolean isCanEdit() {
//        return canEdit;
//    }
//
//    public boolean isCanDelete() {
//        return canDelete;
//    }
//
//    // ✅ Settery dla pól transient
//    public void setUserIsParticipant(boolean userIsParticipant) {
//        this.userIsParticipant = userIsParticipant;
//    }
//
//    public void setUserIsOrganizer(boolean userIsOrganizer) {
//        this.userIsOrganizer = userIsOrganizer;
//    }
//
//    public void setUserParticipationStatus(ParticipationStatus userParticipationStatus) {
//        this.userParticipationStatus = userParticipationStatus;
//    }
//
//    public void setCanJoin(boolean canJoin) {
//        this.canJoin = canJoin;
//    }
//
//    public void setCanLeave(boolean canLeave) {
//        this.canLeave = canLeave;
//    }
//
//    public void setCanEdit(boolean canEdit) {
//        this.canEdit = canEdit;
//    }
//
//    public void setCanDelete(boolean canDelete) {
//        this.canDelete = canDelete;
//    }
//
//    // ✅ Metoda pomocnicza do sprawdzania dostępności miejsc
//    public boolean hasAvailableSpots() {
//        return availableSpots == null || availableSpots > 0;
//    }
//
//    // ✅ Metoda pomocnicza - czy spotkanie jest publiczne
//    public boolean isPublic() {
//        return visibility != null && visibility.equals(MeetingVisibility.PUBLIC);
//    }
//
//    // ✅ Metoda pomocnicza - czy spotkanie jest prywatne
//    public boolean isPrivate() {
//        return visibility != null && visibility.equals(MeetingVisibility.PRIVATE);
//    }
//
//    // ✅ Metoda pomocnicza - czy spotkanie nadchodzące
//    public boolean isUpcoming() {
//        return startDate != null && startDate.isAfter(LocalDateTime.now());
//    }
//
//    // ✅ Metoda pomocnicza - czy spotkanie w trakcie
//    public boolean isOngoing() {
//        LocalDateTime now = LocalDateTime.now();
//        return startDate != null && endDate != null &&
//                startDate.isBefore(now) && endDate.isAfter(now);
//    }
//
//    // ✅ Metoda pomocnicza - czy spotkanie zakończone
//    public boolean isCompleted() {
//        return endDate != null && endDate.isBefore(LocalDateTime.now()) ||
//                (status != null && status.equals(MeetingStatus.COMPLETED));
//    }
//
//    // ✅ Metoda pomocnicza - czy spotkanie anulowane
//    public boolean isCancelled() {
//        return status != null && status.equals(MeetingStatus.CANCELLED);
//    }
//
//    // ✅ Metoda pomocnicza - czy spotkanie wirtualne
//    public boolean isOnline() {
//        return type != null && type.equals(MeetingType.ONLINE);
//    }
//
//    // ✅ Metoda pomocnicza - czy spotkanie fizyczne
//    public boolean isPhysical() {
//        return type != null && type.equals(MeetingType.PHYSICAL);
//    }
//
//    // ✅ Metoda pomocnicza - czy spotkanie hybrydowe
//    public boolean isHybrid() {
//        return type != null && type.equals(MeetingType.HYBRID);
//    }
//
//    // ✅ Metoda pomocnicza - formatowana data rozpoczęcia
//    public String getFormattedStartDate() {
//        if (startDate == null) return "";
//        // Możesz użyć DateTimeFormatter jeśli potrzebujesz
//        return startDate.toString(); // Lub inny format
//    }
//
//    // ✅ Metoda pomocnicza - formatowana data zakończenia
//    public String getFormattedEndDate() {
//        if (endDate == null) return "";
//        return endDate.toString(); // Lub inny format
//    }
//
//    // ✅ Metoda pomocnicza - czy użytkownik może dołączyć
//    public boolean canUserJoin(Long userId) {
//        if (userId == null) return false;
//        if (isCancelled()) return false;
//        if (!hasAvailableSpots()) return false;
//        if (isCompleted()) return false;
//
//        // Sprawdź czy już jest uczestnikiem
//        if (userIsParticipant) return false;
//
//        // Sprawdź czy jest organizatorem
//        if (userIsOrganizer) return false;
//
//        // Dla publicznych - zawsze może dołączyć
//        if (isPublic()) return true;
//
//        // Dla prywatnych - tylko jeśli jest zaproszony (ale to już inna logika)
//        return false;
//    }
//
//    // ✅ Metoda pomocnicza - czy użytkownik może opuścić
//    public boolean canUserLeave(Long userId) {
//        if (userId == null) return false;
//        if (isCancelled()) return false;
//        if (isCompleted()) return false;
//
//        return userIsParticipant && !userIsOrganizer;
//    }
//
//    // ✅ Metoda pomocnicza - czy użytkownik może edytować
//    public boolean canUserEdit(Long userId) {
//        if (userId == null) return false;
//        if (isCompleted()) return false;
//        if (isCancelled()) return false;
//
//        return userIsOrganizer ||
//                (organizer != null && organizer.getId() != null && organizer.getId().equals(userId));
//    }
//
//    // ✅ Metoda pomocnicza - czy użytkownik może usuwać
//    public boolean canUserDelete(Long userId) {
//        if (userId == null) return false;
//
//        return userIsOrganizer ||
//                (organizer != null && organizer.getId() != null && organizer.getId().equals(userId));
//    }
//
//    // ✅ Builder pattern dla łatwego tworzenia (opcjonalnie)
//    public static MeetingResponseBuilder builder() {
//        return new MeetingResponseBuilder();
//    }
//
//    public static class MeetingResponseBuilder {
//        private MeetingResponse meetingResponse = new MeetingResponse();
//
//        public MeetingResponseBuilder id(Long id) {
//            meetingResponse.id = id;
//            return this;
//        }
//
//        public MeetingResponseBuilder title(String title) {
//            meetingResponse.title = title;
//            return this;
//        }
//
//        public MeetingResponseBuilder description(String description) {
//            meetingResponse.description = description;
//            return this;
//        }
//
//        public MeetingResponseBuilder agenda(String agenda) {
//            meetingResponse.agenda = agenda;
//            return this;
//        }
//
//        public MeetingResponseBuilder type(MeetingType type) {
//            meetingResponse.type = type;
//            return this;
//        }
//
//        public MeetingResponseBuilder status(MeetingStatus status) {
//            meetingResponse.status = status;
//            return this;
//        }
//
//        public MeetingResponseBuilder visibility(MeetingVisibility visibility) {
//            meetingResponse.visibility = visibility;
//            return this;
//        }
//
//        public MeetingResponseBuilder startDate(LocalDateTime startDate) {
//            meetingResponse.startDate = startDate;
//            return this;
//        }
//
//        public MeetingResponseBuilder endDate(LocalDateTime endDate) {
//            meetingResponse.endDate = endDate;
//            return this;
//        }
//
//        public MeetingResponseBuilder maxParticipants(Integer maxParticipants) {
//            meetingResponse.maxParticipants = maxParticipants;
//            return this;
//        }
//
//        public MeetingResponseBuilder organizer(UserResponse organizer) {
//            meetingResponse.organizer = organizer;
//            return this;
//        }
//
//        public MeetingResponseBuilder location(LocationResponse location) {
//            meetingResponse.location = location;
//            return this;
//        }
//
//        public MeetingResponseBuilder tags(Set<String> tags) {
//            meetingResponse.tags = tags;
//            return this;
//        }
//
//        public MeetingResponseBuilder createdAt(LocalDateTime createdAt) {
//            meetingResponse.createdAt = createdAt;
//            return this;
//        }
//
//        public MeetingResponseBuilder updatedAt(LocalDateTime updatedAt) {
//            meetingResponse.updatedAt = updatedAt;
//            return this;
//        }
//
//        public MeetingResponseBuilder confirmedParticipantsCount(Integer confirmedParticipantsCount) {
//            meetingResponse.confirmedParticipantsCount = confirmedParticipantsCount;
//            return this;
//        }
//
//        public MeetingResponseBuilder waitingListCount(Integer waitingListCount) {
//            meetingResponse.waitingListCount = waitingListCount;
//            return this;
//        }
//
//        public MeetingResponseBuilder availableSpots(Integer availableSpots) {
//            meetingResponse.availableSpots = availableSpots;
//            return this;
//        }
//
//        // Buildery dla pól transient
//        public MeetingResponseBuilder userIsParticipant(boolean userIsParticipant) {
//            meetingResponse.userIsParticipant = userIsParticipant;
//            return this;
//        }
//
//        public MeetingResponseBuilder userIsOrganizer(boolean userIsOrganizer) {
//            meetingResponse.userIsOrganizer = userIsOrganizer;
//            return this;
//        }
//
//        public MeetingResponseBuilder userParticipationStatus(ParticipationStatus userParticipationStatus) {
//            meetingResponse.userParticipationStatus = userParticipationStatus;
//            return this;
//        }
//
//        public MeetingResponseBuilder canJoin(boolean canJoin) {
//            meetingResponse.canJoin = canJoin;
//            return this;
//        }
//
//        public MeetingResponseBuilder canLeave(boolean canLeave) {
//            meetingResponse.canLeave = canLeave;
//            return this;
//        }
//
//        public MeetingResponseBuilder canEdit(boolean canEdit) {
//            meetingResponse.canEdit = canEdit;
//            return this;
//        }
//
//        public MeetingResponseBuilder canDelete(boolean canDelete) {
//            meetingResponse.canDelete = canDelete;
//            return this;
//        }
//
//        public MeetingResponse build() {
//            return meetingResponse;
//        }
//    }
//}







package com.meethub.domain.model.response;

import com.meethub.domain.model.enums.MeetingStatus;
import com.meethub.domain.model.enums.MeetingType;
import com.meethub.domain.model.enums.MeetingVisibility;
import com.meethub.domain.model.enums.ParticipationStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
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
    private String userRole = "VIEWER"; // "ORGANIZER", "CONFIRMED_PARTICIPANT", "VIEWER", itd.

    // ✅ GETTERY I SETTERY DLA NOWYCH PÓŁ
    public boolean isUserIsConfirmed() { return userIsConfirmed; }
    public void setUserIsConfirmed(boolean userIsConfirmed) { this.userIsConfirmed = userIsConfirmed; }

    public boolean isUserIsPending() { return userIsPending; }
    public void setUserIsPending(boolean userIsPending) { this.userIsPending = userIsPending; }

    public boolean isUserIsInvited() { return userIsInvited; }
    public void setUserIsInvited(boolean userIsInvited) { this.userIsInvited = userIsInvited; }

    public boolean isUserIsDeclined() { return userIsDeclined; }
    public void setUserIsDeclined(boolean userIsDeclined) { this.userIsDeclined = userIsDeclined; }

    public boolean isUserIsWaiting() { return userIsWaiting; }
    public void setUserIsWaiting(boolean userIsWaiting) { this.userIsWaiting = userIsWaiting; }

    public boolean isUserIsViewer() { return userIsViewer; }
    public void setUserIsViewer(boolean userIsViewer) { this.userIsViewer = userIsViewer; }

    public boolean isUserIsUnrelated() { return userIsUnrelated; }
    public void setUserIsUnrelated(boolean userIsUnrelated) { this.userIsUnrelated = userIsUnrelated; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    // ✅ METODY POMOCNICZE DLA UI
    public boolean canUserJoinPublic() {
        return userIsViewer && visibility == MeetingVisibility.PUBLIC;
    }

    public boolean canUserRequestPrivate() {
        return userIsViewer && visibility == MeetingVisibility.PRIVATE;
    }

    public boolean isInviteOnly() {
        return visibility == MeetingVisibility.INVITE_ONLY;
    }

    public boolean showJoinButton() {
        return canUserJoinPublic() || canUserRequestPrivate();
    }

    public boolean showLeaveButton() {
        return userIsConfirmed && !userIsOrganizer;
    }

    public boolean showAcceptDeclineButtons() {
        return userIsInvited;
    }

    public boolean showWaitingBadge() {
        return userIsPending || userIsWaiting;
    }

    public String getUserRoleBadgeColor() {
        switch (userRole) {
            case "ORGANIZER": return "danger";
            case "CONFIRMED_PARTICIPANT": return "success";
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
            case "CONFIRMED_PARTICIPANT": return "Uczestnik";
            case "PENDING": return "Oczekujący";
            case "INVITED": return "Zaproszony";
            case "VIEWER": return "Obserwator";
            case "DECLINED": return "Odmówił";
            case "WAITING_LIST": return "Lista oczekujących";
            default: return "Obserwator";
        }
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

    // ✅ Builder pattern - rozszerz o nowe pola
    public static MeetingResponseBuilder builder() {
        return new MeetingResponseBuilder();
    }

    public static class MeetingResponseBuilder {
        private MeetingResponse meetingResponse = new MeetingResponse();

        // ... istniejące buildery ...
        public MeetingResponseBuilder id(Long id) {
            meetingResponse.id = id;
            return this;
        }

        // ... inne buildery ...

        // ✅ Buildery dla nowych pól
        public MeetingResponseBuilder userIsConfirmed(boolean userIsConfirmed) {
            meetingResponse.userIsConfirmed = userIsConfirmed;
            return this;
        }

        public MeetingResponseBuilder userIsPending(boolean userIsPending) {
            meetingResponse.userIsPending = userIsPending;
            return this;
        }

        public MeetingResponseBuilder userIsInvited(boolean userIsInvited) {
            meetingResponse.userIsInvited = userIsInvited;
            return this;
        }

        public MeetingResponseBuilder userIsDeclined(boolean userIsDeclined) {
            meetingResponse.userIsDeclined = userIsDeclined;
            return this;
        }

        public MeetingResponseBuilder userIsWaiting(boolean userIsWaiting) {
            meetingResponse.userIsWaiting = userIsWaiting;
            return this;
        }

        public MeetingResponseBuilder userIsViewer(boolean userIsViewer) {
            meetingResponse.userIsViewer = userIsViewer;
            return this;
        }

        public MeetingResponseBuilder userIsUnrelated(boolean userIsUnrelated) {
            meetingResponse.userIsUnrelated = userIsUnrelated;
            return this;
        }

        public MeetingResponseBuilder userRole(String userRole) {
            meetingResponse.userRole = userRole;
            return this;
        }

        public MeetingResponse build() {
            return meetingResponse;
        }
    }
}
//package com.meethub.domain.model.entity;
//
//import com.meethub.domain.model.enums.ParticipationStatus;
//import jakarta.persistence.*;
//import lombok.*;
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "meeting_participants")
//@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
//public class MeetingParticipant {
//
//    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "meeting_id", nullable = false)
//    private Meeting meeting;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "user_id", nullable = false)
//    private User user;
//
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
//    private ParticipationStatus status;
//
//    // Kiedy użytkownik został zaproszony
//    @Column(name = "invited_at")
//    private LocalDateTime invitedAt;
//
//    // Kiedy użytkownik odpowiedział (potwierdził/odrzucił)
//    @Column(name = "response_at")
//    private LocalDateTime responseAt;
//
//    // Kiedy faktycznie dołączył do spotkania
//    @Column(name = "joined_at")
//    private LocalDateTime joinedAt;
//
//    // Kiedy opuścił spotkanie
//    @Column(name = "left_at")
//    private LocalDateTime leftAt;
//
//    // Notatki organizatora
//    @Column(name = "notes", columnDefinition = "TEXT")
//    private String notes;
//
//    @PrePersist
//    protected void onCreate() {
//        if (invitedAt == null) {
//            invitedAt = LocalDateTime.now();
//        }
//        if (status == null) {
//            status = ParticipationStatus.INVITED;
//        }
//    }
//
//    // Metody pomocnicze
//    public void confirmParticipation() {
//        this.status = ParticipationStatus.CONFIRMED;
//        this.responseAt = LocalDateTime.now();
//    }
//
//    public boolean isConfirmed(){
//        return this.status == ParticipationStatus.CONFIRMED;
//    }
//
//    public void declineParticipation() {
//        this.status = ParticipationStatus.DECLINED;
//        this.responseAt = LocalDateTime.now();
//    }
//
//    public void markAsAttended() {
//        this.status = ParticipationStatus.ATTENDED;
//        this.joinedAt = LocalDateTime.now();
//    }
//
//    public void markAsLeft() {
//        this.leftAt = LocalDateTime.now();
//    }
//
//    public Long getResponseTimeHours() {
//        if (invitedAt == null || responseAt == null) {
//            return null;
//        }
//        return java.time.Duration.between(invitedAt, responseAt).toHours();
//    }
//
//    public Long getAttendanceDurationMinutes() {
//        if (joinedAt == null || leftAt == null) {
//            return null;
//        }
//        return java.time.Duration.between(joinedAt, leftAt).toMinutes();
//    }
//}





//package com.meethub.domain.model.entity;
//
//import com.meethub.domain.model.enums.ParticipationStatus;
//import com.meethub.domain.model.enums.PermissionLevel;
//import jakarta.persistence.*;
//import lombok.*;
//import org.hibernate.annotations.CreationTimestamp;
//import org.hibernate.annotations.UpdateTimestamp;
//
//import java.time.LocalDateTime;
//import java.util.Objects;
//
//@Entity
//@Table(name = "meeting_participants")
//@Getter
//@Setter
//@AllArgsConstructor
//@Builder
//public class MeetingParticipant {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "meeting_id", nullable = false)
//    private Meeting meeting;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "user_id", nullable = false)
//    private User user;
//
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false, length = 20)
//    private ParticipationStatus status = ParticipationStatus.INVITED;
//
//    @Enumerated(EnumType.STRING)
//    @Column(name = "permission_level", nullable = false, length = 20)
//    private PermissionLevel permissionLevel = PermissionLevel.PARTICIPANT;
//
//    @Column(columnDefinition = "TEXT")
//    private String comment;
//
//    @Column(name = "invitation_token", unique = true, length = 100)
//    private String invitationToken;
//
//    @Column(name = "token_expires_at")
//    private LocalDateTime tokenExpiresAt;
//
//    @Column(name = "response_date")
//    private LocalDateTime responseDate;
//
//    @CreationTimestamp
//    @Column(name = "created_at", nullable = false, updatable = false)
//    private LocalDateTime createdAt;
//
//    @UpdateTimestamp
//    @Column(name = "updated_at", nullable = false)
//    private LocalDateTime updatedAt;
//
//    // Konstruktory
//    public MeetingParticipant() {}
//
//    public MeetingParticipant(Meeting meeting, User user) {
//        this.meeting = meeting;
//        this.user = user;
//        this.status = ParticipationStatus.INVITED;
//        this.permissionLevel = PermissionLevel.PARTICIPANT;
//    }
//
//    public MeetingParticipant(Meeting meeting, User user, ParticipationStatus status, PermissionLevel permissionLevel) {
//        this.meeting = meeting;
//        this.user = user;
//        this.status = status;
//        this.permissionLevel = permissionLevel;
//    }
//
//    // Metody pomocnicze
//    public void confirmParticipation() {
//        this.status = ParticipationStatus.CONFIRMED;
//        this.responseDate = LocalDateTime.now();
//    }
//
//    public void declineParticipation() {
//        this.status = ParticipationStatus.DECLINED;
//        this.responseDate = LocalDateTime.now();
//    }
//
//    public void setTentative() {
//        this.status = ParticipationStatus.TENTATIVE;
//        this.responseDate = LocalDateTime.now();
//    }
//
//    public boolean isConfirmed() {
//        return ParticipationStatus.CONFIRMED.equals(status);
//    }
//
//    public boolean isInvited() {
//        return ParticipationStatus.INVITED.equals(status);
//    }
//
//    public boolean canEditMeeting() {
//        return PermissionLevel.MODERATOR.equals(permissionLevel) ||
//                PermissionLevel.CONTRIBUTOR.equals(permissionLevel);
//    }
//
//    public boolean canViewDetails() {
//        return !ParticipationStatus.DECLINED.equals(status);
//    }
//
//    // equals and hashCode
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//        MeetingParticipant that = (MeetingParticipant) o;
//        return Objects.equals(id, that.id) &&
//                Objects.equals(meeting, that.meeting) &&
//                Objects.equals(user, that.user);
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(id, meeting, user);
//    }
//
//    // toString
//    @Override
//    public String toString() {
//        return "MeetingParticipant{" +
//                "id=" + id +
//                ", status=" + status +
//                ", permissionLevel=" + permissionLevel +
//                ", user=" + (user != null ? user.getEmail() : "null") +
//                '}';
//    }
//}










package com.meethub.domain.model.entity;

import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.model.enums.PermissionLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "meeting_participants")
@Getter
@Setter
@NoArgsConstructor // ✅ DODAJ
@AllArgsConstructor
@Builder
public class MeetingParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ParticipationStatus status = ParticipationStatus.INVITED;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission_level", nullable = false, length = 20)
    private PermissionLevel permissionLevel = PermissionLevel.PARTICIPANT;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "invitation_token", unique = true, length = 100)
    private String invitationToken;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(name = "response_date")
    private LocalDateTime responseDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "attendance_confirmed_at")
    private LocalDateTime attendanceConfirmedAt;

    @Column(name = "attendance_token_used")
    private String attendanceTokenUsed;

    // Konstruktory
    public MeetingParticipant(Meeting meeting, User user) {
        this.meeting = meeting;
        this.user = user;
        this.status = ParticipationStatus.INVITED;
        this.permissionLevel = PermissionLevel.PARTICIPANT;
    }

    public MeetingParticipant(Meeting meeting, User user, ParticipationStatus status, PermissionLevel permissionLevel) {
        this.meeting = meeting;
        this.user = user;
        this.status = status;
        this.permissionLevel = permissionLevel;
    }

    // Metody pomocnicze
    public void confirmParticipation() {
        this.status = ParticipationStatus.CONFIRMED;
        this.responseDate = LocalDateTime.now();
    }

    public void declineParticipation() {
        this.status = ParticipationStatus.DECLINED;
        this.responseDate = LocalDateTime.now();
    }


    public boolean isConfirmed() {
        return ParticipationStatus.CONFIRMED.equals(status);
    }

    public boolean isInvited() {
        return ParticipationStatus.INVITED.equals(status);
    }

    public boolean canEditMeeting() {
        return PermissionLevel.MODERATOR.equals(permissionLevel) ||
                PermissionLevel.CONTRIBUTOR.equals(permissionLevel);
    }

    public boolean canViewDetails() {
        return !ParticipationStatus.DECLINED.equals(status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MeetingParticipant that = (MeetingParticipant) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // toString
    @Override
    public String toString() {
        return "MeetingParticipant{" +
                "id=" + id +
                ", status=" + status +
                ", permissionLevel=" + permissionLevel +
                ", user=" + (user != null ? user.getEmail() : "null") +
                '}';
    }
}
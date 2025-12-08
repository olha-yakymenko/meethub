//
//package com.meethub.domain.model.entity;
//
//import com.meethub.domain.model.enums.UserRole;
//import com.meethub.domain.model.enums.NotificationChannel;
//import com.meethub.domain.model.enums.PermissionLevel;
//import com.meethub.domain.model.enums.ParticipationStatus;
//import jakarta.persistence.*;
//import lombok.*;
//import org.hibernate.annotations.CreationTimestamp;
//import org.hibernate.annotations.UpdateTimestamp;
//
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//
//@Entity
//@Table(name = "users")
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class User {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(nullable = false, unique = true)
//    private String email;
//
//    @Column(nullable = false)
//    private String password;
//
//    @Column(name = "first_name", nullable = false, length = 100)
//    private String firstName;
//
//    @Column(name = "last_name", nullable = false, length = 100)
//    private String lastName;
//
//    @Column(name = "phone_number", length = 20)
//    private String phoneNumber;
//
//    // Główna rola systemowa użytkownika - ZMIENIONE NA "role" dla kompatybilności z repozytorium
//    @Enumerated(EnumType.STRING)
//    @Column(name = "role", nullable = false, length = 20) // Nazwa kolumny musi być "role"
//    @Builder.Default
//    private UserRole role = UserRole.PARTICIPANT;
//
//    @Column(nullable = false)
//    @Builder.Default
//    private Boolean enabled = true;
//
//    @Column(name = "two_factor_enabled", nullable = false)
//    @Builder.Default
//    private Boolean twoFactorEnabled = false;
//
//    @Column(name = "failed_login_attempts", nullable = false)
//    @Builder.Default
//    private Integer failedLoginAttempts = 0;
//
//    @Column(name = "account_locked_until")
//    private LocalDateTime accountLockedUntil;
//
//    // Nowe pola dla powiadomień
//    @ElementCollection
//    @CollectionTable(name = "user_notification_channels", joinColumns = @JoinColumn(name = "user_id"))
//    @Column(name = "channel")
//    @Enumerated(EnumType.STRING)
//    @Builder.Default
//    private Set<NotificationChannel> enabledNotificationChannels = new HashSet<>();
//
//    @Column(name = "email_notifications_enabled")
//    @Builder.Default
//    private Boolean emailNotificationsEnabled = true;
//
//    @Column(name = "push_notifications_enabled")
//    @Builder.Default
//    private Boolean pushNotificationsEnabled = true;
//
//    @Column(name = "sms_notifications_enabled")
//    @Builder.Default
//    private Boolean smsNotificationsEnabled = false;
//
//    @Column(name = "digest_enabled")
//    @Builder.Default
//    private Boolean digestEnabled = true;
//
//    @Column(name = "digest_frequency")
//    @Builder.Default
//    private String digestFrequency = "DAILY";
//
//    @Column(name = "timezone", length = 50)
//    @Builder.Default
//    private String timezone = "Europe/Warsaw";
//
//    @Column(name = "language", length = 10)
//    @Builder.Default
//    private String language = "pl";
//
//    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    @Builder.Default
//    private List<MeetingParticipant> meetingParticipants = new ArrayList<>();
//
//    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    @Builder.Default
//    private List<UserPreference> preferences = new ArrayList<>();
//
//    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    @Builder.Default
//    private List<Notification> notifications = new ArrayList<>();
//
//    @CreationTimestamp
//    @Column(name = "created_at", nullable = false, updatable = false)
//    private LocalDateTime createdAt;
//
//    @UpdateTimestamp
//    @Column(name = "updated_at", nullable = false)
//    private LocalDateTime updatedAt;
//
//    // ========== METODY POMOCNICZE ==========
//
//    public String getFullName() {
//        return firstName + " " + lastName;
//    }
//
//    // Sprawdza czy użytkownik może organizować spotkania (na podstawie roli systemowej)
//    public boolean canOrganizeMeetings() {
//        return UserRole.ORGANIZER.equals(role) || UserRole.ADMIN.equals(role);
//    }
//
//    // Sprawdza czy użytkownik ma uprawnienia administracyjne
//    public boolean hasAdminPrivileges() {
//        return UserRole.ADMIN.equals(role);
//    }
//
//    // Sprawdza czy użytkownik jest moderatorem systemowym
//    public boolean isSystemModerator() {
//        return UserRole.MODERATOR.equals(role) || UserRole.ADMIN.equals(role);
//    }
//
//    // Sprawdza czy użytkownik jest organizatorem (dla kompatybilności)
//    public boolean isOrganizer() {
//        return UserRole.ORGANIZER.equals(role) || UserRole.ADMIN.equals(role);
//    }
//
//    // Sprawdza czy użytkownik może zarządzać spotkaniami (dla kompatybilności)
//    public boolean canManageMeetings() {
//        return UserRole.ORGANIZER.equals(role) ||
//                UserRole.MODERATOR.equals(role) ||
//                UserRole.ADMIN.equals(role);
//    }
//
//    // Pobiera poziom uprawnień użytkownika w konkretnym spotkaniu
//    public PermissionLevel getPermissionLevelInMeeting(Meeting meeting) {
//        if (meeting == null) return null;
//
//        return meetingParticipants.stream()
//                .filter(participant -> participant.getMeeting().getId().equals(meeting.getId()))
//                .findFirst()
//                .map(MeetingParticipant::getPermissionLevel)
//                .orElse(null);
//    }
//
//    // Sprawdza czy użytkownik może zarządzać konkretnym spotkaniem
//    public boolean canManageMeeting(Meeting meeting) {
//        if (meeting == null) return false;
//
//        // Admin systemowy może zarządzać wszystkimi spotkaniami
//        if (hasAdminPrivileges()) return true;
//
//        // Organizator spotkania może nim zarządzać
//        if (meeting.getOrganizer().getId().equals(this.id)) return true;
//
//        // Sprawdź poziom uprawnień w spotkaniu
//        PermissionLevel permissionLevel = getPermissionLevelInMeeting(meeting);
//        return permissionLevel != null &&
//                (PermissionLevel.MODERATOR.equals(permissionLevel));
//    }
//
//    // Sprawdza czy użytkownik jest organizatorem konkretnego spotkania
//    public boolean isOrganizerOf(Meeting meeting) {
//        if (meeting == null) return false;
//        return meeting.getOrganizer().getId().equals(this.id);
//    }
//
//    // Sprawdza czy użytkownik jest moderatorem konkretnego spotkania
//    public boolean isModeratorOf(Meeting meeting) {
//        if (meeting == null) return false;
//        PermissionLevel permissionLevel = getPermissionLevelInMeeting(meeting);
//        return PermissionLevel.MODERATOR.equals(permissionLevel) || isOrganizerOf(meeting);
//    }
//
//    // Sprawdza status uczestnictwa użytkownika w spotkaniu
//    public ParticipationStatus getParticipationStatus(Meeting meeting) {
//        if (meeting == null) return null;
//
//        return meetingParticipants.stream()
//                .filter(participant -> participant.getMeeting().getId().equals(meeting.getId()))
//                .findFirst()
//                .map(MeetingParticipant::getStatus)
//                .orElse(null);
//    }
//
//    // Sprawdza czy użytkownik jest potwierdzonym uczestnikiem spotkania
//    public boolean isConfirmedParticipant(Meeting meeting) {
//        ParticipationStatus status = getParticipationStatus(meeting);
//        return ParticipationStatus.CONFIRMED.equals(status);
//    }
//
//    public boolean isNotificationChannelEnabled(NotificationChannel channel) {
//        return enabledNotificationChannels.contains(channel);
//    }
//
//    // Metoda pomocnicza do znajdowania uczestnictwa w spotkaniu
//    public MeetingParticipant getMeetingParticipant(Meeting meeting) {
//        if (meeting == null) return null;
//
//        return meetingParticipants.stream()
//                .filter(participant -> participant.getMeeting().getId().equals(meeting.getId()))
//                .findFirst()
//                .orElse(null);
//    }
//
//    // ========== METODY DLA ŁATWEGO SPRAWDZANIA RÓL SYSTEMOWYCH ==========
//
//    public boolean isAdmin() {
//        return UserRole.ADMIN.equals(role);
//    }
//
//    public boolean isSystemOrganizer() {
//        return UserRole.ORGANIZER.equals(role);
//    }
//
//
//    public boolean isSystemParticipant() {
//        return UserRole.PARTICIPANT.equals(role);
//    }
//
//
//
//
//
//}











package com.meethub.domain.model.entity;

import com.meethub.domain.model.enums.UserRole;
import com.meethub.domain.model.enums.NotificationChannel;
import com.meethub.domain.model.enums.PermissionLevel;
import com.meethub.domain.model.enums.ParticipationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    // Główna rola systemowa użytkownika
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.PARTICIPANT;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "two_factor_enabled", nullable = false)
    @Builder.Default
    private Boolean twoFactorEnabled = false;

    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    @Column(name = "account_locked_until")
    private LocalDateTime accountLockedUntil;

    // Nowe pola dla powiadomień
    @ElementCollection
    @CollectionTable(name = "user_notification_channels", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "channel")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<NotificationChannel> enabledNotificationChannels = new HashSet<>();

    @Column(name = "email_notifications_enabled")
    @Builder.Default
    private Boolean emailNotificationsEnabled = true;

    @Column(name = "push_notifications_enabled")
    @Builder.Default
    private Boolean pushNotificationsEnabled = true;

    @Column(name = "sms_notifications_enabled")
    @Builder.Default
    private Boolean smsNotificationsEnabled = false;

    @Column(name = "digest_enabled")
    @Builder.Default
    private Boolean digestEnabled = true;

    @Column(name = "digest_frequency")
    @Builder.Default
    private String digestFrequency = "DAILY";

    @Column(name = "timezone", length = 50)
    @Builder.Default
    private String timezone = "Europe/Warsaw";

    @Column(name = "language", length = 10)
    @Builder.Default
    private String language = "pl";

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<MeetingParticipant> meetingParticipants = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<UserPreference> preferences = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Notification> notifications = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ========== METODY POMOCNICZE ==========

    public String getFullName() {
        return firstName + " " + lastName;
    }


    // Sprawdza czy użytkownik jest organizatorem (dla kompatybilności)
    public boolean isOrganizer() {
        return UserRole.ORGANIZER.equals(role) || UserRole.ADMIN.equals(role);
    }

    // Pobiera poziom uprawnień użytkownika w konkretnym spotkaniu
    public PermissionLevel getPermissionLevelInMeeting(Meeting meeting) {
        if (meeting == null) return null;

        return meetingParticipants.stream()
                .filter(participant -> participant.getMeeting().getId().equals(meeting.getId()))
                .findFirst()
                .map(MeetingParticipant::getPermissionLevel)
                .orElse(null);
    }


    // Sprawdza czy użytkownik jest organizatorem konkretnego spotkania
    public boolean isOrganizerOf(Meeting meeting) {
        if (meeting == null) return false;
        return meeting.getOrganizer().getId().equals(this.id);
    }

    // Sprawdza czy użytkownik jest moderatorem konkretnego spotkania
    public boolean isModeratorOf(Meeting meeting) {
        if (meeting == null) return false;
        PermissionLevel permissionLevel = getPermissionLevelInMeeting(meeting);
        return PermissionLevel.MODERATOR.equals(permissionLevel) || isOrganizerOf(meeting);
    }

    // Sprawdza status uczestnictwa użytkownika w spotkaniu
    public ParticipationStatus getParticipationStatus(Meeting meeting) {
        if (meeting == null) return null;

        return meetingParticipants.stream()
                .filter(participant -> participant.getMeeting().getId().equals(meeting.getId()))
                .findFirst()
                .map(MeetingParticipant::getStatus)
                .orElse(null);
    }

    // Sprawdza czy użytkownik jest potwierdzonym uczestnikiem spotkania
    public boolean isConfirmedParticipant(Meeting meeting) {
        ParticipationStatus status = getParticipationStatus(meeting);
        return ParticipationStatus.CONFIRMED.equals(status);
    }

    public boolean isNotificationChannelEnabled(NotificationChannel channel) {
        return enabledNotificationChannels.contains(channel);
    }

    public boolean isEnabled() {
        return enabled;
    }

    // Metoda pomocnicza do znajdowania uczestnictwa w spotkaniu
    public MeetingParticipant getMeetingParticipant(Meeting meeting) {
        if (meeting == null) return null;

        return meetingParticipants.stream()
                .filter(participant -> participant.getMeeting().getId().equals(meeting.getId()))
                .findFirst()
                .orElse(null);
    }

    // ========== METODY DLA ŁATWEGO SPRAWDZANIA RÓL SYSTEMOWYCH ==========

    public boolean isAdmin() {
        return UserRole.ADMIN.equals(role);
    }

    public boolean isSystemOrganizer() {
        return UserRole.ORGANIZER.equals(role);
    }

    public boolean isSystemParticipant() {
        return UserRole.PARTICIPANT.equals(role);
    }
}
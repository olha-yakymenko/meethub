package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.MeetingParticipant;
import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.model.enums.PermissionLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {

    List<MeetingParticipant> findByMeetingId(Long meetingId);

    @Query("SELECT mp FROM MeetingParticipant mp WHERE mp.user.id = :userId AND mp.meeting.organizer.id != :organizerId")
    List<MeetingParticipant> findByUserIdAndOrganizerIdNot(@Param("userId") Long userId,
                                                           @Param("organizerId") Long organizerId);

    List<MeetingParticipant> findByUserId(Long userId);

    List<MeetingParticipant> findByMeetingIdAndStatus(Long meetingId, ParticipationStatus status);

    long countByMeetingIdAndStatus(Long meetingId, ParticipationStatus status);

    Optional<MeetingParticipant> findByInvitationToken(String invitationToken);

    @Query("SELECT COUNT(mp) FROM MeetingParticipant mp WHERE mp.meeting.id = :meetingId AND mp.status = 'CONFIRMED'")
    long countConfirmedParticipants(@Param("meetingId") Long meetingId);

    boolean existsByMeetingIdAndUserId(Long meetingId, Long userId);

    void deleteByMeetingIdAndUserId(Long meetingId, Long userId);

    @Query("SELECT COUNT(mp) FROM MeetingParticipant mp WHERE mp.meeting.organizer.id = :organizerId")
    long countByMeetingOrganizerId(@Param("organizerId") Long organizerId);

    @Query("SELECT mp FROM MeetingParticipant mp WHERE mp.meeting.id = :meetingId AND mp.user.id = :userId")
    Optional<MeetingParticipant> findByMeetingIdAndUserId(@Param("meetingId") Long meetingId, @Param("userId") Long userId);

    /**
     * Sprawdź czy użytkownik jest organizatorem spotkania
     */
    @Query("SELECT COUNT(mp) > 0 FROM MeetingParticipant mp WHERE mp.meeting.id = :meetingId AND mp.user.id = :userId AND mp.permissionLevel = 'ORGANIZER'")
    boolean isUserOrganizer(@Param("meetingId") Long meetingId, @Param("userId") Long userId);

    /**
     * Znajdź uczestników z określonym poziomem uprawnień
     */
    List<MeetingParticipant> findByMeetingIdAndPermissionLevel(Long meetingId, PermissionLevel permissionLevel);

    /**
     * Policz uczestników z określonym statusem i uprawnieniami
     */
    long countByMeetingIdAndStatusAndPermissionLevel(Long meetingId, ParticipationStatus status, PermissionLevel permissionLevel);
}
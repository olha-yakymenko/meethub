
package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.MeetingParticipant;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.ParticipationStatus;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {

    // ✅ Te metody są OK (nie używają @Query)
    List<MeetingParticipant> findByMeetingId(Long meetingId);

    // ✅ POPRAWIONE: Usuń @Query dla tej metody (użyj query derivation)
    List<MeetingParticipant> findByUserId(Long userId);

    List<MeetingParticipant> findByMeetingIdAndStatus(Long meetingId, ParticipationStatus status);

    long countByMeetingIdAndStatus(Long meetingId, ParticipationStatus status);

    Optional<MeetingParticipant> findByInvitationToken(String invitationToken);

    boolean existsByMeetingIdAndUserId(Long meetingId, Long userId);


    // ✅ POPRAWIONE: Ta metoda jest OK
    @Query("SELECT COUNT(mp) FROM MeetingParticipant mp WHERE mp.meeting.organizer.id = :organizerId")
    long countByMeetingOrganizerId(@Param("organizerId") Long organizerId);

    // ✅ POPRAWIONE: Zmień na query derivation
    Optional<MeetingParticipant> findByMeetingIdAndUserId(Long meetingId, Long userId);

    List<MeetingParticipant> findByUserIdAndStatus(Long userId, ParticipationStatus status);

    long countByMeetingId(Long meetingId);

    // ✅ POPRAWIONE: Ta metoda jest już OK
    @Query(value = "SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (mp.response_date - mp.created_at)) / 3600.0), 0.0) " +
            "FROM meeting_participants mp " +
            "WHERE mp.meeting_id = :meetingId AND mp.response_date IS NOT NULL", nativeQuery = true)
    Double findAverageResponseTimeHours(@Param("meetingId") Long meetingId);


    long countByMeetingIdAndStatusIn(Long meetingId, List<ParticipationStatus> statuses);


    boolean existsByMeetingIdAndUserIdAndStatus(Long meetingId, Long userId, ParticipationStatus status);


    @Query("SELECT p FROM MeetingParticipant p " +
            "WHERE p.meeting.id = :meetingId " +
            "AND p.status = 'CONFIRMED' AND p.status = 'ORGANIZER'")
    List<MeetingParticipant> findConfirmedParticipantsByMeetingId(@Param("meetingId") Long meetingId);
}
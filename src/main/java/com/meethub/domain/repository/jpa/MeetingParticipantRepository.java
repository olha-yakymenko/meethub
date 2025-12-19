
package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.dto.ParticipantCountDto;
import com.meethub.domain.model.entity.MeetingParticipant;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.model.projection.ParticipantProjection;
import com.meethub.domain.model.response.ParticipantResponse;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {

    List<MeetingParticipant> findByMeetingId(Long meetingId);

    List<MeetingParticipant> findByUserId(Long userId);

    List<MeetingParticipant> findByMeetingIdAndStatus(Long meetingId, ParticipationStatus status);

    long countByMeetingIdAndStatus(Long meetingId, ParticipationStatus status);

    Optional<MeetingParticipant> findByInvitationToken(String invitationToken);

    boolean existsByMeetingIdAndUserId(Long meetingId, Long userId);


    @Query("SELECT COUNT(mp) FROM MeetingParticipant mp WHERE mp.meeting.organizer.id = :organizerId")
    long countByMeetingOrganizerId(@Param("organizerId") Long organizerId);

    Optional<MeetingParticipant> findByMeetingIdAndUserId(Long meetingId, Long userId);

    List<MeetingParticipant> findByUserIdAndStatus(Long userId, ParticipationStatus status);

    long countByMeetingId(Long meetingId);

    @Query(value = "SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (mp.response_date - mp.created_at)) / 3600.0), 0.0) " +
            "FROM meeting_participants mp " +
            "WHERE mp.meeting_id = :meetingId AND mp.response_date IS NOT NULL", nativeQuery = true)
    Double findAverageResponseTimeHours(@Param("meetingId") Long meetingId);


    long countByMeetingIdAndStatusIn(Long meetingId, List<ParticipationStatus> statuses);


    boolean existsByMeetingIdAndUserIdAndStatus(Long meetingId, Long userId, ParticipationStatus status);


    boolean existsByMeetingIdAndUserIdAndStatusIn(Long meetingId, Long userId, List<ParticipationStatus> statuses);


    @Query("""
    SELECT
        p.id AS id,
        u.email AS email,
        p.status AS status,
        u.firstName AS firstName,
        u.lastName AS lastName
    FROM MeetingParticipant p
    JOIN p.user u
    WHERE p.meeting.id = :meetingId
    ORDER BY u.lastName, u.firstName
""")
    List<ParticipantProjection> findParticipantsProjection(@Param("meetingId") Long meetingId);


    @Query("""
    SELECT
        p.id as id,
            u.id as userId,
            u.firstName as firstName,
            u.lastName as lastName,
            u.email as email,
            p.status as status
    FROM MeetingParticipant p
    JOIN p.user u
    WHERE p.meeting.id = :meetingId
    ORDER BY 
        CASE p.status 
            WHEN 'CONFIRMED' THEN 1
            WHEN 'ATTENDED' THEN 2
            WHEN 'PENDING' THEN 3
            WHEN 'INVITED' THEN 4
            WHEN 'DECLINED' THEN 5
            ELSE 6
        END,
        u.lastName, u.firstName
""")
    List<ParticipantProjection> findAllParticipantsByMeetingId(@Param("meetingId") Long meetingId);


    @Query("""
        SELECT new com.meethub.domain.model.dto.ParticipantCountDto(
            COUNT(p),
            SUM(CASE WHEN p.status IN ('CONFIRMED', 'ATTENDED') THEN 1 ELSE 0 END),
            SUM(CASE WHEN p.status = 'ATTENDED' THEN 1 ELSE 0 END),
            SUM(CASE WHEN p.status = 'DECLINED' THEN 1 ELSE 0 END),
            SUM(CASE WHEN p.status = 'CANCELLED' THEN 1 ELSE 0 END),
            SUM(CASE WHEN p.status = 'INVITED' THEN 1 ELSE 0 END),
            SUM(CASE WHEN p.status = 'PENDING' OR p.status IS NULL THEN 1 ELSE 0 END)
        )
        FROM MeetingParticipant p
        WHERE p.meeting.id = :meetingId
    """)
    ParticipantCountDto getParticipantCounts(@Param("meetingId") Long meetingId);

    Optional<MeetingParticipant> findByIdAndInvitationToken(Long participantId, String invitationToken);

}
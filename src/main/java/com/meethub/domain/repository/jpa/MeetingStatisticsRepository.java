package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.dto.OrganizerReportStats;
import com.meethub.domain.model.dto.ParticipantCountDto;
import com.meethub.domain.model.entity.MeetingStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingStatisticsRepository extends JpaRepository<MeetingStatistics, Long> {

    Optional<MeetingStatistics> findByMeetingId(Long meetingId);

    @Modifying
    @Query("DELETE FROM MeetingStatistics ms WHERE ms.meeting.id = :meetingId")
    void deleteByMeetingId(@Param("meetingId") Long meetingId);

    @Query("SELECT ms FROM MeetingStatistics ms WHERE ms.meeting.organizer.id = :organizerId")
    List<MeetingStatistics> findByOrganizerId(@Param("organizerId") Long organizerId);


    @Query("""
    SELECT new com.meethub.domain.model.dto.OrganizerReportStats(
        COUNT(ms),
        AVG(ms.attendanceRate),
        SUM(ms.totalParticipants),
        SUM(ms.attendedParticipants)
    )
    FROM MeetingStatistics ms
    WHERE ms.meeting.organizer.id = :organizerId
""")
    OrganizerReportStats getOrganizerReportStats(
            @Param("organizerId") Long organizerId
    );


    @Query("SELECT ms FROM MeetingStatistics ms ORDER BY ms.generatedAt DESC")
    List<MeetingStatistics> findRecentStatistics(Pageable pageable);


    @Query("""
SELECT NEW com.meethub.domain.model.dto.OrganizerReportStats(
    COUNT(ms),
    COALESCE(CAST(AVG(ms.attendanceRate) AS java.math.BigDecimal), 0),
    COALESCE(SUM(ms.totalParticipants), 0),
    COALESCE(SUM(ms.attendedParticipants), 0)
)
FROM MeetingStatistics ms
WHERE ms.meeting.organizer.id = :organizerId
  AND (:dateFrom IS NULL OR ms.generatedAt >= :dateFrom)
  AND (:dateTo IS NULL OR ms.generatedAt <= :dateTo)
""")
    OrganizerReportStats getOrganizerReportStatsByDateRange(
            @Param("organizerId") Long organizerId,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo
    );
}
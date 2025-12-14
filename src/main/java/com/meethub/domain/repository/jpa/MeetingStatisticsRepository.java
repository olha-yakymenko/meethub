package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.MeetingStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
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
}
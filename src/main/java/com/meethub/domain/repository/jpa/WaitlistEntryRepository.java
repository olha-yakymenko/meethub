package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.WaitlistEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, Long> {

    @Query("SELECT MAX(w.position) FROM WaitlistEntry w WHERE w.meeting.id = :meetingId")
    Optional<Integer> findMaxPositionByMeetingId(@Param("meetingId") Long meetingId);

    @Query("SELECT COUNT(w) > 0 FROM WaitlistEntry w WHERE w.meeting.id = :meetingId AND w.user.id = :userId")
    boolean existsByMeetingIdAndUserId(@Param("meetingId") Long meetingId, @Param("userId") Long userId);

    @Query("SELECT w FROM WaitlistEntry w WHERE w.meeting.id = :meetingId ORDER BY w.position ASC LIMIT 1")
    Optional<WaitlistEntry> findFirstByMeetingIdOrderByPositionAsc(@Param("meetingId") Long meetingId);

    List<WaitlistEntry> findByUserId(Long userId);


    @Query("SELECT w FROM WaitlistEntry w WHERE w.meeting.id = :meetingId AND w.position > :position ORDER BY w.position ASC")
    List<WaitlistEntry> findByMeetingIdAndPositionGreaterThan(@Param("meetingId") Long meetingId,
                                                              @Param("position") Integer position);

    @Query("SELECT w FROM WaitlistEntry w WHERE w.meeting.id = :meetingId AND w.user.id = :userId")
    Optional<WaitlistEntry> findByMeetingIdAndUserId(@Param("meetingId") Long meetingId,
                                                     @Param("userId") Long userId);

}
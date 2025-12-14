package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.ParticipantStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParticipantStatusHistoryRepository extends JpaRepository<ParticipantStatusHistory, Long> {

    @Query("SELECT h FROM ParticipantStatusHistory h WHERE h.participant.meeting.id = :meetingId ORDER BY h.changedAt DESC")
    List<ParticipantStatusHistory> findByMeetingId(@Param("meetingId") Long meetingId);

}
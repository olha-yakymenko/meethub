// MeetingVotingRepository.java
package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.MeetingVoting;
import com.meethub.domain.model.entity.Vote;
import com.meethub.domain.model.entity.VotingOption;
import com.meethub.domain.model.enums.VotingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingVotingRepository extends JpaRepository<MeetingVoting, Long> {

    List<MeetingVoting> findByMeetingId(Long meetingId);

    @Query("SELECT v FROM MeetingVoting v WHERE v.deadlineDate < :now AND v.status = 'ACTIVE'")
    List<MeetingVoting> findExpiredVotings(@Param("now") LocalDateTime now);


    @Query("SELECT COUNT(v) > 0 FROM MeetingVoting v WHERE v.meeting.id = :meetingId AND v.status = 'ACTIVE'")
    boolean hasActiveVoting(@Param("meetingId") Long meetingId);

}

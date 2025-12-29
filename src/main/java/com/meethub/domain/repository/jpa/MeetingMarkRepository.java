package com.meethub.domain.repository.jpa;


import com.meethub.domain.model.entity.MeetingMark;
import com.meethub.domain.model.id.MeetingMarkId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface MeetingMarkRepository extends JpaRepository<MeetingMark, MeetingMarkId> {

    List<MeetingMark> findByUserId(Long userId);

    List<MeetingMark> findByMeetingId(Long meetingId);

    boolean existsByUserIdAndMeetingId(Long userId, Long meetingId);

    @Modifying
    @Transactional
    @Query("DELETE FROM MeetingMark mm WHERE mm.user.id = :userId AND mm.meeting.id = :meetingId")
    void deleteByUserIdAndMeetingId(@Param("userId") Long userId, @Param("meetingId") Long meetingId);

    @Query("SELECT mm.meeting.id FROM MeetingMark mm WHERE mm.user.id = :userId")
    List<Long> findImportantMeetingIdsByUserId(@Param("userId") Long userId);

}
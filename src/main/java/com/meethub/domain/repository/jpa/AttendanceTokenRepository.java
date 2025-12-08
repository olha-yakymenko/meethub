package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.AttendanceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface AttendanceTokenRepository extends JpaRepository<AttendanceToken, Long> {

    Optional<AttendanceToken> findByToken(String token);

    Optional<AttendanceToken> findByTokenAndMeetingId(String token, Long meetingId);

    Optional<AttendanceToken> findByUserIdAndMeetingId(Long userId, Long meetingId);

    @Query("SELECT at FROM AttendanceToken at WHERE at.token = :token AND at.status = 'ACTIVE'")
    Optional<AttendanceToken> findActiveByToken(@Param("token") String token);

    @Query("SELECT at FROM AttendanceToken at WHERE at.user.id = :userId AND at.meeting.id = :meetingId AND at.status = 'ACTIVE'")
    Optional<AttendanceToken> findActiveByUserAndMeeting(@Param("userId") Long userId,
                                                         @Param("meetingId") Long meetingId);
}
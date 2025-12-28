package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.Feedback;
import com.meethub.domain.model.response.FeedbackStatisticsDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findByMeetingId(Long meetingId);

    List<Feedback> findByUserId(Long userId);

    Optional<Feedback> findByMeetingIdAndUserId(Long meetingId, Long userId);

    @Query("SELECT AVG(f.rating) FROM Feedback f WHERE f.meeting.id = :meetingId")
    Double findAverageRatingByMeetingId(@Param("meetingId") Long meetingId);

    @Query("SELECT COUNT(f) FROM Feedback f WHERE f.meeting.id = :meetingId")
    Long countByMeetingId(@Param("meetingId") Long meetingId);

//    @Query("SELECT new com.meethub.domain.model.dto.FeedbackStatisticsDto(AVG(f.rating), COUNT(f)) " +
//            "FROM Feedback f WHERE f.meeting.id = :meetingId")
//    FeedbackStatisticsDto findStatisticsByMeetingId(@Param("meetingId") Long meetingId);

}
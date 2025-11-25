package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.ParticipantStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParticipantStatusHistoryRepository extends JpaRepository<ParticipantStatusHistory, Long> {

    /**
     * Znajdź historię statusów dla uczestnika posortowaną od najnowszych
     */
    List<ParticipantStatusHistory> findByParticipantIdOrderByChangedAtDesc(Long participantId);

    /**
     * Znajdź historię statusów dla spotkania
     */
    @Query("SELECT h FROM ParticipantStatusHistory h WHERE h.participant.meeting.id = :meetingId ORDER BY h.changedAt DESC")
    List<ParticipantStatusHistory> findByMeetingId(@Param("meetingId") Long meetingId);

    /**
     * Znajdź ostatnią zmianę statusu dla uczestnika
     */
    @Query("SELECT h FROM ParticipantStatusHistory h WHERE h.participant.id = :participantId ORDER BY h.changedAt DESC LIMIT 1")
    ParticipantStatusHistory findLatestByParticipantId(@Param("participantId") Long participantId);

    /**
     * Sprawdź czy uczestnik miał już dany status
     */
    @Query("SELECT COUNT(h) > 0 FROM ParticipantStatusHistory h WHERE h.participant.id = :participantId AND h.newStatus = :status")
    boolean hasParticipantHadStatus(@Param("participantId") Long participantId, @Param("status") String status);

    /**
     * Usuń historię starszą niż podana data
     */
    void deleteByChangedAtBefore(java.time.LocalDateTime date);
}
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

    /**
     * Znajdź wpisy na liście oczekujących dla spotkania posortowane według pozycji
     */
    List<WaitlistEntry> findByMeetingIdOrderByPositionAsc(Long meetingId);

    /**
     * Znajdź maksymalną pozycję na liście oczekujących dla spotkania
     */
    @Query("SELECT MAX(w.position) FROM WaitlistEntry w WHERE w.meeting.id = :meetingId")
    Optional<Integer> findMaxPositionByMeetingId(@Param("meetingId") Long meetingId);

    /**
     * Sprawdź czy użytkownik jest już na liście oczekujących dla spotkania
     */
    @Query("SELECT COUNT(w) > 0 FROM WaitlistEntry w WHERE w.meeting.id = :meetingId AND w.user.id = :userId")
    boolean existsByMeetingIdAndUserId(@Param("meetingId") Long meetingId, @Param("userId") Long userId);

    /**
     * Usuń użytkownika z listy oczekujących spotkania
     */
    @Modifying
    @Query("DELETE FROM WaitlistEntry w WHERE w.meeting.id = :meetingId AND w.user.id = :userId")
    void deleteByMeetingIdAndUserId(@Param("meetingId") Long meetingId, @Param("userId") Long userId);

    /**
     * Znajdź pierwszy wpis na liście oczekujących (najwyższy priorytet)
     */
    @Query("SELECT w FROM WaitlistEntry w WHERE w.meeting.id = :meetingId ORDER BY w.position ASC LIMIT 1")
    Optional<WaitlistEntry> findFirstByMeetingIdOrderByPositionAsc(@Param("meetingId") Long meetingId);

    /**
     * Znajdź pozycję użytkownika na liście oczekujących
     */
    @Query("SELECT w.position FROM WaitlistEntry w WHERE w.meeting.id = :meetingId AND w.user.id = :userId")
    Optional<Integer> findPositionByMeetingIdAndUserId(@Param("meetingId") Long meetingId, @Param("userId") Long userId);

    /**
     * Zaktualizuj pozycje po usunięciu wpisu
     */
    @Modifying
    @Query("UPDATE WaitlistEntry w SET w.position = w.position - 1 WHERE w.meeting.id = :meetingId AND w.position > :position")
    void updatePositionsAfterDelete(@Param("meetingId") Long meetingId, @Param("position") Integer position);

    /**
     * Policz liczbę osób na liście oczekujących dla spotkania
     */
    long countByMeetingId(Long meetingId);

    /**
     * Znajdź wpisy na liście oczekujących dla użytkownika
     */
    List<WaitlistEntry> findByUserId(Long userId);

    /**
     * Znajdź wpisy które powinny być powiadomione (np. gdy zwolni się miejsce)
     */
    @Query("SELECT w FROM WaitlistEntry w WHERE w.meeting.id = :meetingId AND w.position <= :maxPosition AND w.notifiedAt IS NULL")
    List<WaitlistEntry> findUnnotifiedEntriesUpToPosition(@Param("meetingId") Long meetingId, @Param("maxPosition") Integer maxPosition);

    /**
     * Znajdź wpisy na liście oczekujących z pozycją większą niż podana
     */
    @Query("SELECT w FROM WaitlistEntry w WHERE w.meeting.id = :meetingId AND w.position > :position ORDER BY w.position ASC")
    List<WaitlistEntry> findByMeetingIdAndPositionGreaterThan(@Param("meetingId") Long meetingId,
                                                              @Param("position") Integer position);

    /**
     * Znajdź wpis na liście oczekujących po meetingId i userId
     */
    @Query("SELECT w FROM WaitlistEntry w WHERE w.meeting.id = :meetingId AND w.user.id = :userId")
    Optional<WaitlistEntry> findByMeetingIdAndUserId(@Param("meetingId") Long meetingId,
                                                     @Param("userId") Long userId);

}
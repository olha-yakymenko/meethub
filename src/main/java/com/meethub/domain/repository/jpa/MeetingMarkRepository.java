package com.meethub.domain.repository.jpa;


import com.meethub.domain.model.entity.MeetingMark;
import com.meethub.domain.model.entity.MeetingMarkId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface MeetingMarkRepository extends JpaRepository<MeetingMark, MeetingMarkId> {

    // Znajdź wszystkie oznaczenia dla użytkownika
    List<MeetingMark> findByUserId(Long userId);

    // Znajdź wszystkie oznaczenia dla spotkania
    List<MeetingMark> findByMeetingId(Long meetingId);

    // Sprawdź czy spotkanie jest oznaczone przez użytkownika
    boolean existsByUserIdAndMeetingId(Long userId, Long meetingId);

    // Znajdź konkretne oznaczenie
    Optional<MeetingMark> findByUserIdAndMeetingId(Long userId, Long meetingId);

    // Usuń oznaczenie
    @Modifying
    @Transactional
    @Query("DELETE FROM MeetingMark mm WHERE mm.user.id = :userId AND mm.meeting.id = :meetingId")
    void deleteByUserIdAndMeetingId(@Param("userId") Long userId, @Param("meetingId") Long meetingId);

    // Pobierz ID ważnych spotkań dla użytkownika
    @Query("SELECT mm.meeting.id FROM MeetingMark mm WHERE mm.user.id = :userId")
    List<Long> findImportantMeetingIdsByUserId(@Param("userId") Long userId);

    // Liczba oznaczeń dla spotkania
    Long countByMeetingId(Long meetingId);
}
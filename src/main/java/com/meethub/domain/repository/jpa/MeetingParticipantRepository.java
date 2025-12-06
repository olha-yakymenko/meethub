//package com.meethub.domain.repository.jpa;
//
//import com.meethub.domain.model.entity.MeetingParticipant;
//import com.meethub.domain.model.entity.User;
//import com.meethub.domain.model.enums.ParticipationStatus;
//import com.meethub.domain.model.enums.PermissionLevel;
//import jakarta.transaction.Transactional;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Modifying;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Optional;
//
//@Repository
//public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {
//
//    List<MeetingParticipant> findByMeetingId(Long meetingId);
//
//    @Query("SELECT mp FROM MeetingParticipant mp WHERE mp.user.id = :userId AND mp.meeting.organizer.id != :organizerId")
//    List<MeetingParticipant> findByUserIdAndOrganizerIdNot(@Param("userId") Long userId,
//                                                           @Param("organizerId") Long organizerId);
//
//    List<MeetingParticipant> findByUserId(Long userId);
//
//    List<MeetingParticipant> findByMeetingIdAndStatus(Long meetingId, ParticipationStatus status);
//
//    long countByMeetingIdAndStatus(Long meetingId, ParticipationStatus status);
//
//    Optional<MeetingParticipant> findByInvitationToken(String invitationToken);
//
//    @Query("SELECT COUNT(mp) FROM MeetingParticipant mp WHERE mp.meeting.id = :meetingId AND mp.status = 'CONFIRMED'")
//    long countConfirmedParticipants(@Param("meetingId") Long meetingId);
//
//    boolean existsByMeetingIdAndUserId(Long meetingId, Long userId);
//
//    void deleteByMeetingIdAndUserId(Long meetingId, Long userId);
//
//    @Query("SELECT COUNT(mp) FROM MeetingParticipant mp WHERE mp.meeting.organizer.id = :organizerId")
//    long countByMeetingOrganizerId(@Param("organizerId") Long organizerId);
//
//    @Query("SELECT mp FROM MeetingParticipant mp WHERE mp.meeting.id = :meetingId AND mp.user.id = :userId")
//    Optional<MeetingParticipant> findByMeetingIdAndUserId(@Param("meetingId") Long meetingId, @Param("userId") Long userId);
//
//    List<MeetingParticipant> findByUserIdAndStatus(Long userId, ParticipationStatus status);
//
//
//
//        // ✅ Metody do statystyk
//        long countByMeetingId(Long meetingId);
//
//    // Dodaj tę metodę do MeetingParticipantRepository.java:
//    @Query("SELECT mp.user FROM MeetingParticipant mp WHERE mp.meeting.id = :meetingId AND mp.status = 'CONFIRMED'")
//    List<User> findUsersByMeetingId(@Param("meetingId") Long meetingId);
//
//
//    @Query("SELECT mp FROM MeetingParticipant mp WHERE mp.meeting.id = :meetingId AND mp.responseDate IS NOT NULL")
//    List<MeetingParticipant> findRespondedParticipants(@Param("meetingId") Long meetingId);
//
//    // ✅ POPRAWIONE: Użyj responseDate zamiast responseTimestamp
//    @Query(value = "SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (mp.response_date - mp.created_at)) / 3600.0), 0.0) " +
//            "FROM meeting_participants mp " +
//            "WHERE mp.meeting_id = :meetingId AND mp.response_date IS NOT NULL",
//            nativeQuery = true)
//    Double findAverageResponseTimeHours(@Param("meetingId") Long meetingId);
//
//    // Aktualizuj czas odpowiedzi
//    @Transactional
//    @Modifying
//    @Query("UPDATE MeetingParticipant mp SET mp.responseAt = :responseAt, mp.status = :status " +
//            "WHERE mp.meeting.id = :meetingId AND mp.user.id = :userId")
//    int updateResponse(@Param("meetingId") Long meetingId,
//                       @Param("userId") Long userId,
//                       @Param("responseAt") LocalDateTime responseAt,
//                       @Param("status") String status);
//
//    // Dodaj tę metodę
//    long countByMeetingIdAndStatusIn(Long meetingId, List<ParticipationStatus> statuses);
//}







package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.MeetingParticipant;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.ParticipationStatus;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {

    // ✅ Te metody są OK (nie używają @Query)
    List<MeetingParticipant> findByMeetingId(Long meetingId);

    // ✅ POPRAWIONE: Usuń @Query dla tej metody (użyj query derivation)
    List<MeetingParticipant> findByUserId(Long userId);

    // ✅ POPRAWIONE: Zmień na query derivation
    List<MeetingParticipant> findByUserIdAndMeetingOrganizerIdNot(Long userId, Long organizerId);

    List<MeetingParticipant> findByMeetingIdAndStatus(Long meetingId, ParticipationStatus status);

    long countByMeetingIdAndStatus(Long meetingId, ParticipationStatus status);

    Optional<MeetingParticipant> findByInvitationToken(String invitationToken);

    // ✅ POPRAWIONE: Użyj wartości enuma zamiast stringa
    @Query("SELECT COUNT(mp) FROM MeetingParticipant mp WHERE mp.meeting.id = :meetingId AND mp.status = com.meethub.domain.model.enums.ParticipationStatus.CONFIRMED")
    long countConfirmedParticipants(@Param("meetingId") Long meetingId);

    boolean existsByMeetingIdAndUserId(Long meetingId, Long userId);

    void deleteByMeetingIdAndUserId(Long meetingId, Long userId);

    // ✅ POPRAWIONE: Ta metoda jest OK
    @Query("SELECT COUNT(mp) FROM MeetingParticipant mp WHERE mp.meeting.organizer.id = :organizerId")
    long countByMeetingOrganizerId(@Param("organizerId") Long organizerId);

    // ✅ POPRAWIONE: Zmień na query derivation
    Optional<MeetingParticipant> findByMeetingIdAndUserId(Long meetingId, Long userId);

    List<MeetingParticipant> findByUserIdAndStatus(Long userId, ParticipationStatus status);

    long countByMeetingId(Long meetingId);

    // ✅ POPRAWIONE: Użyj wartości enuma
    @Query("SELECT mp.user FROM MeetingParticipant mp WHERE mp.meeting.id = :meetingId AND mp.status = com.meethub.domain.model.enums.ParticipationStatus.CONFIRMED")
    List<User> findUsersByMeetingId(@Param("meetingId") Long meetingId);

    // ✅ POPRAWIONE: Użyj responseDate (poprawne pole)
    @Query("SELECT mp FROM MeetingParticipant mp WHERE mp.meeting.id = :meetingId AND mp.responseDate IS NOT NULL")
    List<MeetingParticipant> findRespondedParticipants(@Param("meetingId") Long meetingId);

    // ✅ POPRAWIONE: Ta metoda jest już OK
    @Query(value = "SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (mp.response_date - mp.created_at)) / 3600.0), 0.0) " +
            "FROM meeting_participants mp " +
            "WHERE mp.meeting_id = :meetingId AND mp.response_date IS NOT NULL", nativeQuery = true)
    Double findAverageResponseTimeHours(@Param("meetingId") Long meetingId);

    // ✅ POPRAWIONE: Użyj responseDate zamiast responseAt
    @Transactional
    @Modifying
    @Query("UPDATE MeetingParticipant mp SET mp.responseDate = :responseDate, mp.status = :status " +
            "WHERE mp.meeting.id = :meetingId AND mp.user.id = :userId")
    int updateResponse(@Param("meetingId") Long meetingId,
                       @Param("userId") Long userId,
                       @Param("responseDate") LocalDateTime responseDate,
                       @Param("status") ParticipationStatus status);  // Użyj enuma

    long countByMeetingIdAndStatusIn(Long meetingId, List<ParticipationStatus> statuses);


    boolean existsByMeetingIdAndUserIdAndStatus(Long meetingId, Long userId, ParticipationStatus status);

    // Opcjonalnie - jeśli chcesz precyzyjniejsze metody
    default boolean isUserConfirmed(Long meetingId, Long userId) {
        return existsByMeetingIdAndUserIdAndStatus(meetingId, userId, ParticipationStatus.CONFIRMED);
    }

    default boolean isUserPending(Long meetingId, Long userId) {
        return existsByMeetingIdAndUserIdAndStatus(meetingId, userId, ParticipationStatus.PENDING);
    }

    default boolean isUserInvited(Long meetingId, Long userId) {
        return existsByMeetingIdAndUserIdAndStatus(meetingId, userId, ParticipationStatus.INVITED);
    }

    default boolean isUserDeclined(Long meetingId, Long userId) {
        return existsByMeetingIdAndUserIdAndStatus(meetingId, userId, ParticipationStatus.DECLINED);
    }

    default boolean isUserWaiting(Long meetingId, Long userId) {
        return existsByMeetingIdAndUserIdAndStatus(meetingId, userId, ParticipationStatus.WAITING_LIST);
    }
}
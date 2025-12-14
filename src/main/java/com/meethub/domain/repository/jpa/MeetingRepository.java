package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.enums.MeetingStatus;
import com.meethub.domain.model.enums.MeetingType;
import com.meethub.domain.model.enums.MeetingVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long>, JpaSpecificationExecutor<Meeting> {

    // ✅ DODAJ BRAKUJĄCE METODY:
    Page<Meeting> findAll(Specification<Meeting> spec, Pageable pageable);

    // ✅ NOWE METODY DO DODANIA:

    /**
     * Znajdź serię spotkań cyklicznych (oryginalne + wszystkie wystąpienia)
     */
    @Query("SELECT m FROM Meeting m WHERE m.originalMeetingId = :originalMeetingId OR m.id = :originalMeetingId")
    List<Meeting> findByOriginalMeetingIdOrId(
            @Param("originalMeetingId") Long originalMeetingId);

    /**
     * Znajdź nadchodzące spotkania cykliczne dla użytkownika
     */
    @Query("""
        SELECT DISTINCT m FROM Meeting m 
        JOIN m.participants mp 
        WHERE mp.user.id = :userId 
        AND m.recurring = true 
        AND m.startDate > :now 
        AND m.status = 'PLANNED'
        ORDER BY m.startDate ASC
        """)
    List<Meeting> findUpcomingRecurringMeetingsForUser(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now);

    /**
     * Znajdź ostatnie wystąpienie w serii cyklicznej
     */
    @Query("""
        SELECT m FROM Meeting m 
        WHERE m.originalMeetingId = :originalMeetingId 
        ORDER BY m.startDate DESC 
        LIMIT 1
        """)
    Optional<Meeting> findLastOccurrenceByOriginalMeetingId(
            @Param("originalMeetingId") Long originalMeetingId);

    // ✅ ISTNIEJĄCE METODY (pozostałe bez zmian):
    Page<Meeting> findByOrganizerId(Long organizerId, Pageable pageable);

    List<Meeting> findByOrganizerId(Long organizerId);

    @Query("SELECT m FROM Meeting m WHERE m.visibility = 'PUBLIC' AND m.startDate > :startDate")
    List<Meeting> findUpcomingPublicMeetings(@Param("startDate") LocalDateTime startDate);

    @Query(value = """
        SELECT m.* FROM meetings m 
        JOIN meeting_participants mp ON m.id = mp.meeting_id 
        WHERE mp.user_id = :userId AND mp.status = 'CONFIRMED'
        AND m.start_date BETWEEN :startDate AND :endDate
        """
            , nativeQuery = true)
    List<Meeting> findConfirmedMeetingsForUserInPeriod(@Param("userId") Long userId,
                                                       @Param("startDate") LocalDateTime startDate,
                                                       @Param("endDate") LocalDateTime endDate);

    Optional<Meeting> findByIdAndOrganizerId(Long id, Long organizerId);

    @Query("SELECT COUNT(m) FROM Meeting m JOIN m.participants mp WHERE mp.user.id = :userId AND m.startDate > CURRENT_TIMESTAMP")
    Long countUpcomingMeetingsByUserId(@Param("userId") Long userId);

    List<Meeting> findByOrganizerIdAndTemplateTrue(Long organizerId);

    /**
     * Znajdź szablon po ID
     */
    Optional<Meeting> findByIdAndTemplateTrue(Long id);

    /**
     * Znajdź powtarzające się spotkania z datą zakończenia po danej dacie
     */
    List<Meeting> findByRecurringTrueAndRecurrenceEndDateAfter(LocalDateTime date);

    /**
     * Znajdź wszystkie wystąpienia serii
     */
    List<Meeting> findByOriginalMeetingId(Long originalMeetingId);

    /**
     * Znajdź spotkania według kategorii
     */
    @Query("SELECT m FROM Meeting m JOIN m.categories c WHERE c.id = :categoryId")
    Page<Meeting> findByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);

    /**
     * Znajdź spotkania według tagu
     */
    @Query("SELECT m FROM Meeting m WHERE :tag MEMBER OF m.tags")
    Page<Meeting> findByTag(@Param("tag") String tag, Pageable pageable);

    List<Meeting> findByStatusAndEndDateBefore(MeetingStatus status, LocalDateTime time);

    @Query("SELECT m FROM Meeting m " +
            "WHERE m.status = :status " +
            "AND m.startDate BETWEEN :from AND :to " +
            "ORDER BY m.startDate ASC")
    List<Meeting> findByStatusAndStartDateBetween(
            @Param("status") MeetingStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT m FROM Meeting m " +
            "WHERE m.status = :status " +
            "AND m.updatedAt < :cutoff " +
            "ORDER BY m.updatedAt ASC")
    List<Meeting> findByStatusAndUpdatedAtBefore(
            @Param("status") MeetingStatus status,
            @Param("cutoff") LocalDateTime cutoff);

    // Do statystyk
    @Query("SELECT COUNT(m) FROM Meeting m " +
            "WHERE m.organizer.id = :userId " +
            "AND m.startDate > :now " +
            "AND m.status = 'PLANNED'")
    Long countUpcomingMeetingsByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query("SELECT m.id FROM Meeting m")
    List<Long> findAllMeetingIds();

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END " +
            "FROM Meeting m WHERE m.id = :meetingId AND m.organizer.id = :userId")
    boolean isUserOrganizer(@Param("meetingId") Long meetingId, @Param("userId") Long userId);
}









//package com.meethub.domain.repository.jpa;
//
//import com.meethub.domain.model.entity.Meeting;
//import com.meethub.domain.model.enums.MeetingStatus;
//import com.meethub.domain.model.enums.MeetingType;
//import com.meethub.domain.model.enums.MeetingVisibility;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.domain.Specification;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Optional;
//
//@Repository
//public interface MeetingRepository extends JpaRepository<Meeting, Long>, JpaSpecificationExecutor<Meeting> {
//
//    // ✅ DODAJ BRAKUJĄCE METODY:
//    Page<Meeting> findAll(Specification<Meeting> spec, Pageable pageable);
//
//    // ✅ ISTNIEJĄCE METODY (pozostałe bez zmian):
//    Page<Meeting> findByOrganizerId(Long organizerId, Pageable pageable);
//
//    List<Meeting> findByOrganizerId(Long organizerId);
//
//    @Query("SELECT m FROM Meeting m WHERE m.visibility = 'PUBLIC' AND m.startDate > :startDate")
//    List<Meeting> findUpcomingPublicMeetings(@Param("startDate") LocalDateTime startDate);
//
//    @Query(value = """
//        SELECT m.* FROM meetings m
//        JOIN meeting_participants mp ON m.id = mp.meeting_id
//        WHERE mp.user_id = :userId AND mp.status = 'CONFIRMED'
//        AND m.start_date BETWEEN :startDate AND :endDate
//        """
//            , nativeQuery = true)
//    List<Meeting> findConfirmedMeetingsForUserInPeriod(@Param("userId") Long userId,
//                                                       @Param("startDate") LocalDateTime startDate,
//                                                       @Param("endDate") LocalDateTime endDate);
//
//    Optional<Meeting> findByIdAndOrganizerId(Long id, Long organizerId);
//
//    @Query("SELECT COUNT(m) FROM Meeting m JOIN m.participants mp WHERE mp.user.id = :userId AND m.startDate > CURRENT_TIMESTAMP")
//    Long countUpcomingMeetingsByUserId(@Param("userId") Long userId);
//
//    List<Meeting> findByOrganizerIdAndTemplateTrue(Long organizerId);
//
//    /**
//     * Znajdź szablon po ID
//     */
//    Optional<Meeting> findByIdAndTemplateTrue(Long id);
//
//    /**
//     * Znajdź powtarzające się spotkania z datą zakończenia po danej dacie
//     */
//    List<Meeting> findByRecurringTrueAndRecurrenceEndDateAfter(LocalDateTime date);
//
//    /**
//     * Znajdź wszystkie wystąpienia serii
//     */
//    List<Meeting> findByOriginalMeetingId(Long originalMeetingId);
//
//    /**
//     * Znajdź spotkania według kategorii
//     */
//    @Query("SELECT m FROM Meeting m JOIN m.categories c WHERE c.id = :categoryId")
//    Page<Meeting> findByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);
//
//    /**
//     * Znajdź spotkania według tagu
//     */
//    @Query("SELECT m FROM Meeting m WHERE :tag MEMBER OF m.tags")
//    Page<Meeting> findByTag(@Param("tag") String tag, Pageable pageable);
//
//
//
//    List<Meeting> findByStatusAndEndDateBefore(MeetingStatus status, LocalDateTime time);
//
//
//
//    @Query("SELECT m FROM Meeting m " +
//            "WHERE m.status = :status " +
//            "AND m.startDate BETWEEN :from AND :to " +
//            "ORDER BY m.startDate ASC")
//    List<Meeting> findByStatusAndStartDateBetween(
//            @Param("status") MeetingStatus status,
//            @Param("from") LocalDateTime from,
//            @Param("to") LocalDateTime to);
//
//    @Query("SELECT m FROM Meeting m " +
//            "WHERE m.status = :status " +
//            "AND m.updatedAt < :cutoff " +
//            "ORDER BY m.updatedAt ASC")
//    List<Meeting> findByStatusAndUpdatedAtBefore(
//            @Param("status") MeetingStatus status,
//            @Param("cutoff") LocalDateTime cutoff);
//
//    // Do statystyk
//    @Query("SELECT COUNT(m) FROM Meeting m " +
//            "WHERE m.organizer.id = :userId " +
//            "AND m.startDate > :now " +
//            "AND m.status = 'PLANNED'")
//    Long countUpcomingMeetingsByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);
//
//
//    @Query("SELECT m.id FROM Meeting m")
//    List<Long> findAllMeetingIds();
//}
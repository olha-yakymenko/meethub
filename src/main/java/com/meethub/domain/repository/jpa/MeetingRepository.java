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

    /**
     * Znajdź spotkania o określonym statusie
     */
    List<Meeting> findByStatus(MeetingStatus status);

    /**
     * Znajdź spotkania o określonym statusie z paginacją
     */
    Page<Meeting> findByStatus(MeetingStatus status, Pageable pageable);

    /**
     * Znajdź zakończone spotkania (status = COMPLETED)
     */
    @Query("SELECT m FROM Meeting m WHERE m.status = 'COMPLETED'")
    List<Meeting> findCompletedMeetings();

    /**
     * Znajdź zakończone spotkania z paginacją
     */
    @Query("SELECT m FROM Meeting m WHERE m.status = 'COMPLETED'")
    Page<Meeting> findCompletedMeetings(Pageable pageable);

    /**
     * Znajdź zakończone spotkania, które zakończyły się przed określoną datą
     */
    @Query("SELECT m FROM Meeting m WHERE m.status = 'COMPLETED' AND m.endDate <= :endDate")
    List<Meeting> findCompletedMeetingsBefore(@Param("endDate") LocalDateTime endDate);

    /**
     * Znajdź zakończone spotkania z zakresu dat
     */
    @Query("SELECT m FROM Meeting m WHERE m.status = 'COMPLETED' AND m.endDate BETWEEN :startDate AND :endDate")
    List<Meeting> findCompletedMeetingsInDateRange(@Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);

    /**
     * Znajdź zakończone spotkania dla określonego organizatora
     */
    @Query("SELECT m FROM Meeting m WHERE m.organizer.id = :organizerId AND m.status = 'COMPLETED'")
    List<Meeting> findCompletedMeetingsByOrganizer(@Param("organizerId") Long organizerId);

    /**
     * Znajdź zakończone spotkania, które nie mają statystyk
     * (użyteczne do refreshAllStatistics)
     */
    @Query(value = """
        SELECT m.* FROM meetings m 
        LEFT JOIN meeting_statistics ms ON m.id = ms.meeting_id 
        WHERE m.status = 'COMPLETED' 
        AND ms.id IS NULL
        """
            , nativeQuery = true)
    List<Meeting> findCompletedMeetingsWithoutStatistics();

    /**
     * Znajdź zakończone spotkania ze starszymi statystykami
     * (statystyki starsze niż określona data)
     */
    @Query(value = """
        SELECT m.* FROM meetings m 
        JOIN meeting_statistics ms ON m.id = ms.meeting_id 
        WHERE m.status = 'COMPLETED' 
        AND ms.updated_at < :olderThan
        """
            , nativeQuery = true)
    List<Meeting> findCompletedMeetingsWithOutdatedStatistics(@Param("olderThan") LocalDateTime olderThan);

    /**
     * Znajdź spotkania z określonym statusem dla organizatora
     */
    @Query("SELECT m FROM Meeting m WHERE m.organizer.id = :organizerId AND m.status = :status")
    List<Meeting> findByOrganizerIdAndStatus(@Param("organizerId") Long organizerId,
                                             @Param("status") String status);

    // ✅ ISTNIEJĄCE METODY (pozostałe bez zmian):
    Page<Meeting> findByOrganizerId(Long organizerId, Pageable pageable);

    List<Meeting> findByOrganizerId(Long organizerId);

    List<Meeting> findByOrganizerIdOrderByStartDateDesc(Long organizerId);

    List<Meeting> findByOrganizerIdAndStatus(Long organizerId, MeetingStatus status);

    Page<Meeting> findByOrganizerIdAndStatus(Long organizerId, MeetingStatus status, Pageable pageable);

    List<Meeting> findByOrganizerIdAndStatusIn(Long organizerId, List<MeetingStatus> statuses);

    Page<Meeting> findByVisibilityAndStartDateAfter(MeetingVisibility visibility, LocalDateTime startDate, Pageable pageable);

    @Query("SELECT m FROM Meeting m WHERE m.organizer.id = :organizerId AND m.status IN :statuses")
    List<Meeting> findByOrganizerAndStatusIn(@Param("organizerId") Long organizerId,
                                             @Param("statuses") List<MeetingStatus> statuses);

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

    long countByOrganizerIdAndStatus(Long organizerId, MeetingStatus status);

    @Query("SELECT m FROM Meeting m WHERE m.organizer.id = :organizerId AND m.startDate BETWEEN :startDate AND :endDate")
    List<Meeting> findByOrganizerIdAndDateRange(@Param("organizerId") Long organizerId,
                                                @Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    @Query("SELECT m FROM Meeting m WHERE m.organizer.id = :organizerId AND m.status = :status ORDER BY m.startDate ASC")
    List<Meeting> findByOrganizerIdAndStatusOrderByStartDateAsc(@Param("organizerId") Long organizerId,
                                                                @Param("status") MeetingStatus status);

    @Query("SELECT m FROM Meeting m WHERE m.organizer.id = :organizerId AND LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Meeting> findByOrganizerIdAndTitleContaining(@Param("organizerId") Long organizerId,
                                                      @Param("keyword") String keyword);

    long countByOrganizerId(Long organizerId);

    @Query("SELECT COUNT(m) FROM Meeting m WHERE m.organizer.id = :organizerId AND m.startDate > :now")
    long countUpcomingMeetingsByOrganizerId(@Param("organizerId") Long organizerId, @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(m) FROM Meeting m JOIN m.participants mp WHERE mp.user.id = :userId AND m.startDate > CURRENT_TIMESTAMP")
    Long countUpcomingMeetingsByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(m) FROM Meeting m JOIN m.participants mp WHERE mp.user.id = :userId AND m.startDate > :now")
    Long countUpcomingMeetingsByUserIdAndDate(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query("SELECT m FROM Meeting m " +
            "JOIN m.participants mp " +
            "JOIN mp.user u " +
            "WHERE u.id = :userId AND m.startDate > :now " +
            "ORDER BY m.startDate ASC")
    List<Meeting> findUpcomingMeetingsByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query("SELECT m FROM Meeting m WHERE " +
            "(:search IS NULL OR LOWER(m.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(m.description) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "(:type IS NULL OR m.type = :type) AND " +
            "(:status IS NULL OR m.status = :status)")
    Page<Meeting> findByFilters(@Param("search") String search,
                                @Param("type") MeetingType type,
                                @Param("status") MeetingStatus status,
                                Pageable pageable);

    default List<Meeting> findUpcomingPublicMeetings() {
        return findUpcomingPublicMeetings(LocalDateTime.now());
    }

    @Query("SELECT m FROM Meeting m WHERE m.startDate BETWEEN :start AND :end AND m.status = 'CONFIRMED'")
    List<Meeting> findMeetingsStartingBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(m) FROM Meeting m WHERE m.status = 'CONFIRMED'")
    Long countConfirmedMeetings();

    @Query("SELECT m FROM Meeting m JOIN m.tags t WHERE t = :tag")
    List<Meeting> findByTag(@Param("tag") String tag);


    /**
     * Znajdź szablony użytkownika
     */
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

    /**
     * Znajdź spotkania użytkownika według statusu
     */
    @Query("SELECT m FROM Meeting m WHERE m.status = :status AND m.organizer.id = :organizerId")
    Page<Meeting> findByStatusAndOrganizer(@Param("status") String status,
                                           @Param("organizerId") Long organizerId,
                                           Pageable pageable);
}
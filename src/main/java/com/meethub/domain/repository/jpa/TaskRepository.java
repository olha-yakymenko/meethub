// TaskRepository.java
package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.Task;
import com.meethub.domain.model.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // Podstawowe zapytania
    List<Task> findByMeetingId(Long meetingId);
    List<Task> findByCreatedById(Long userId);
    List<Task> findByStatus(TaskStatus status);

    // Zapytania z paginacją
    Page<Task> findByMeetingId(Long meetingId, Pageable pageable);
    Page<Task> findByCreatedById(Long userId, Pageable pageable);
    Page<Task> findByStatus(TaskStatus status, Pageable pageable);

    // Zaawansowane zapytania
    @Query("SELECT t FROM Task t WHERE t.meeting.id = :meetingId AND t.status = :status ORDER BY t.createdAt DESC")
    List<Task> findTasksByMeetingAndStatus(@Param("meetingId") Long meetingId, @Param("status") TaskStatus status);

    @Query("SELECT t FROM Task t WHERE t.deadline < :now AND t.status IN (com.meethub.domain.model.enums.TaskStatus.TODO, com.meethub.domain.model.enums.TaskStatus.IN_PROGRESS)")
    List<Task> findOverdueTasks(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.meeting.id = :meetingId AND t.status = :status")
    Long countByMeetingIdAndStatus(@Param("meetingId") Long meetingId, @Param("status") TaskStatus status);

    @Query("SELECT t FROM Task t WHERE t.meeting.id = :meetingId AND LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Task> searchTasksByTitle(@Param("meetingId") Long meetingId, @Param("keyword") String keyword);

    // Sprawdzenie uprawnień
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM Task t WHERE t.id = :taskId AND t.meeting.organizer.id = :userId")
    boolean isUserTaskOrganizer(@Param("taskId") Long taskId, @Param("userId") Long userId);

    // Statystyki
    @Query("SELECT t.status, COUNT(t) FROM Task t WHERE t.meeting.id = :meetingId GROUP BY t.status")
    List<Object[]> getTaskStatusStats(@Param("meetingId") Long meetingId);
}
// TaskAssignmentRepository.java
package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.TaskAssignment;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.AssignmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, Long> {

    // Podstawowe zapytania
    List<TaskAssignment> findByUserId(Long userId);
    List<TaskAssignment> findByTaskId(Long taskId);
    Optional<TaskAssignment> findByTaskIdAndUserId(Long taskId, Long userId);
    List<TaskAssignment> findByStatus(AssignmentStatus status);

    // Zapytania z paginacją
    Page<TaskAssignment> findByUserId(Long userId, Pageable pageable);
    Page<TaskAssignment> findByTaskId(Long taskId, Pageable pageable);
    Page<TaskAssignment> findByStatus(AssignmentStatus status, Pageable pageable);

    // Zaawansowane zapytania
    @Query("SELECT ta FROM TaskAssignment ta WHERE ta.task.meeting.id = :meetingId")
    List<TaskAssignment> findByMeetingId(@Param("meetingId") Long meetingId);

    @Query("SELECT ta FROM TaskAssignment ta WHERE ta.task.id = :taskId AND ta.status = :status")
    List<TaskAssignment> findByTaskIdAndStatus(@Param("taskId") Long taskId, @Param("status") AssignmentStatus status);

    @Query("SELECT ta FROM TaskAssignment ta WHERE ta.user.id = :userId AND ta.status = :status")
    List<TaskAssignment> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") AssignmentStatus status);

    @Query("SELECT COUNT(ta) FROM TaskAssignment ta WHERE ta.task.id = :taskId AND ta.status = :status")
    Long countByTaskIdAndStatus(@Param("taskId") Long taskId, @Param("status") AssignmentStatus status);

    @Query("SELECT ta FROM TaskAssignment ta WHERE ta.task.id = :taskId AND ta.user.id = :userId")
    Optional<TaskAssignment> findUserAssignmentForTask(@Param("taskId") Long taskId, @Param("userId") Long userId);

    // Statystyki
    @Query("SELECT ta.status, COUNT(ta) FROM TaskAssignment ta WHERE ta.task.id = :taskId GROUP BY ta.status")
    List<Object[]> getAssignmentStatusStats(@Param("taskId") Long taskId);

    @Query("SELECT COUNT(ta) FROM TaskAssignment ta WHERE ta.task.meeting.id = :meetingId")
    Long countTotalAssignmentsByMeeting(@Param("meetingId") Long meetingId);

    // Sprawdzenie istnienia
    boolean existsByTaskIdAndUserId(Long taskId, Long userId);

    @Query("SELECT a.user FROM TaskAssignment a WHERE a.task.id = :taskId")
    List<User> findAssignedUsersByTaskId(@Param("taskId") Long taskId);

}
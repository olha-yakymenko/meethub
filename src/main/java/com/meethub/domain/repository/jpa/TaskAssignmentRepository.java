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

    List<TaskAssignment> findByUserId(Long userId);
    List<TaskAssignment> findByTaskId(Long taskId);

    Page<TaskAssignment> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT ta FROM TaskAssignment ta WHERE ta.task.meeting.id = :meetingId")
    List<TaskAssignment> findByMeetingId(@Param("meetingId") Long meetingId);

    @Query("SELECT a.user FROM TaskAssignment a WHERE a.task.id = :taskId")
    List<User> findAssignedUsersByTaskId(@Param("taskId") Long taskId);

}
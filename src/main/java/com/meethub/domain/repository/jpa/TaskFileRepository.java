//// TaskFileRepository.java
//package com.meethub.domain.repository.jpa;
//
//import com.meethub.domain.model.entity.TaskFile;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//import java.util.Optional;
//
//@Repository
//public interface TaskFileRepository extends JpaRepository<TaskFile, Long> {
//
//    // Podstawowe zapytania
//    List<TaskFile> findByAssignmentId(Long assignmentId);
//
//    // Zaawansowane zapytania
//    @Query("SELECT tf FROM TaskFile tf WHERE tf.assignment.user.id = :userId")
//    List<TaskFile> findByUserId(@Param("userId") Long userId);
//
//    @Query("SELECT tf FROM TaskFile tf WHERE tf.assignment.task.meeting.id = :meetingId")
//    List<TaskFile> findByMeetingId(@Param("meetingId") Long meetingId);
//
//}




package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.TaskFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskFileRepository extends JpaRepository<TaskFile, Long> {

    List<TaskFile> findByTaskId(Long taskId);

    List<TaskFile> findByTaskIdAndUploadedById(Long taskId, Long userId);

    List<TaskFile> findByTaskIdAndAssignmentIsNull(Long taskId);

    List<TaskFile> findByAssignmentId(Long assignmentId);

    List<TaskFile> findByUploadedById(Long userId);


    @Query("SELECT tf FROM TaskFile tf WHERE tf.assignment.task.meeting.id = :meetingId")
    List<TaskFile> findByMeetingId(@Param("meetingId") Long meetingId);

    @Query("SELECT COUNT(tf) FROM TaskFile tf WHERE tf.task.id = :taskId AND tf.uploadedBy.id = :userId")
    int countFilesByUserAndTask(@Param("taskId") Long taskId, @Param("userId") Long userId);
}
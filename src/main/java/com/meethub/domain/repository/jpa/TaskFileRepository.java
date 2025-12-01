// TaskFileRepository.java
package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.TaskFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskFileRepository extends JpaRepository<TaskFile, Long> {

    // Podstawowe zapytania
    List<TaskFile> findByAssignmentId(Long assignmentId);
    Optional<TaskFile> findByFilename(String filename);
    List<TaskFile> findByAssignmentTaskId(Long taskId);

    // Zapytania z paginacją
    Page<TaskFile> findByAssignmentId(Long assignmentId, Pageable pageable);
    Page<TaskFile> findByAssignmentTaskId(Long taskId, Pageable pageable);

    // Zaawansowane zapytania
    @Query("SELECT tf FROM TaskFile tf WHERE tf.assignment.user.id = :userId")
    List<TaskFile> findByUserId(@Param("userId") Long userId);

    @Query("SELECT tf FROM TaskFile tf WHERE tf.assignment.task.meeting.id = :meetingId")
    List<TaskFile> findByMeetingId(@Param("meetingId") Long meetingId);

    @Query("SELECT tf FROM TaskFile tf WHERE tf.assignment.id = :assignmentId AND LOWER(tf.originalFilename) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<TaskFile> searchFilesByFilename(@Param("assignmentId") Long assignmentId, @Param("keyword") String keyword);

    @Query("SELECT SUM(tf.fileSize) FROM TaskFile tf WHERE tf.assignment.task.id = :taskId")
    Long getTotalFileSizeByTask(@Param("taskId") Long taskId);

    @Query("SELECT SUM(tf.fileSize) FROM TaskFile tf WHERE tf.assignment.user.id = :userId")
    Long getTotalFileSizeByUser(@Param("userId") Long userId);

    // Statystyki
    @Query("SELECT tf.contentType, COUNT(tf) FROM TaskFile tf WHERE tf.assignment.task.id = :taskId GROUP BY tf.contentType")
    List<Object[]> getFileTypeStats(@Param("taskId") Long taskId);

    // Sprawdzenie uprawnień
    @Query("SELECT CASE WHEN COUNT(tf) > 0 THEN true ELSE false END FROM TaskFile tf WHERE tf.id = :fileId AND (tf.assignment.user.id = :userId OR tf.assignment.task.meeting.organizer.id = :userId)")
    boolean canUserAccessFile(@Param("fileId") Long fileId, @Param("userId") Long userId);
}
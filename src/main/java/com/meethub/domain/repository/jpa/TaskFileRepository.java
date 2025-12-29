
package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.TaskFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskFileRepository extends JpaRepository<TaskFile, Long> {

    @Query("SELECT tf FROM TaskFile tf WHERE tf.assignment.task.meeting.id = :meetingId")
    List<TaskFile> findByMeetingId(@Param("meetingId") Long meetingId);

}
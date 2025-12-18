package com.meethub.domain.service;

import com.meethub.domain.model.entity.Task;
import com.meethub.domain.model.entity.TaskAssignment;
import com.meethub.domain.model.entity.TaskFile;
import com.meethub.domain.model.enums.AssignmentStatus;
import com.meethub.domain.model.request.CreateTaskRequest;
import com.meethub.domain.model.request.UpdateTaskRequest;
import com.meethub.domain.model.response.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.core.io.Resource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Validated
public interface TaskService {

     Task createTask(
            @NotNull CreateTaskRequest request,
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long organizerId
    );

    List<Task> getMeetingTasks(
            @NotNull @Positive Long meetingId
    );

    List<Task> getUserCreatedTasks(
            @NotNull @Positive Long userId
    );

    Task getTaskById(
            @NotNull @Positive Long taskId
    );

    void deleteTask(
            @NotNull @Positive Long taskId,
            @NotNull @Positive Long userId
    );

    // Przypisania zadań
    TaskAssignment assignTask(
            @NotNull @Positive Long taskId,
            @NotNull @Positive Long userId,
            @NotNull @Positive Long organizerId
    );

    TaskAssignment assignTaskToCurrentUser(
            @NotNull @Positive Long taskId,
            @NotNull @Positive Long userId
    );

    List<TaskAssignment> getUserAssignments(
            @NotNull @Positive Long userId
    );

    List<TaskAssignment> getTaskAssignments(
            @NotNull @Positive Long taskId
    );

    TaskAssignment getAssignmentById(
            @NotNull @Positive Long assignmentId
    );

    TaskAssignment updateAssignmentStatus(
            @NotNull @Positive Long assignmentId,
            @NotNull AssignmentStatus status,
            @NotNull @Positive Long userId
    );

    TaskAssignment updateAssignmentComment(
            @NotNull @Positive Long assignmentId,
            String comment,
            @NotNull @Positive Long userId
    );

    void removeAssignment(
            @NotNull @Positive Long assignmentId,
            @NotNull @Positive Long userId
    );

    // Pliki
    TaskFile uploadFile(
            @NotNull @Positive Long assignmentId,
            @NotNull MultipartFile file,
            @NotNull @Positive Long userId
    );

    @Transactional(readOnly = true)
    TaskFile getFileById(Long fileId);

    @Transactional
    TaskFile uploadFileToAssignment(Long assignmentId, MultipartFile file, Long userId, String description);

    @Transactional
    TaskFile uploadFileToTask(Long taskId, MultipartFile file, Long userId, String description);

    Resource downloadFile(
            @NotNull @Positive Long fileId,
            @NotNull @Positive Long userId
    );

    List<TaskFile> getAssignmentFiles(
            @NotNull @Positive Long assignmentId,
            @NotNull @Positive Long userId
    );

    @Transactional(readOnly = true)
    List<TaskFile> getTaskFiles(Long taskId, Long userId);

    @Transactional(readOnly = true)
    List<TaskFile> getAllTaskFilesForOrganizer(Long taskId, Long userId);

    void deleteFile(
            @NotNull @Positive Long fileId,
            @NotNull @Positive Long userId
    );

     List<TaskAssignment> getAssignmentsByStatus(
            @NotNull @Positive Long taskId,
            @NotNull AssignmentStatus status
    );

    Long countCompletedAssignments(
            @NotNull @Positive Long taskId
    );

    Long countTotalAssignments(
            @NotNull @Positive Long taskId
    );

    // Walidacje
    boolean canUserManageTask(
            @NotNull @Positive Long taskId,
            @NotNull @Positive Long userId
    );

    boolean canUserAccessAssignment(
            @NotNull @Positive Long assignmentId,
            @NotNull @Positive Long userId
    );

    Task updateTaskWithRequest(
            @NotNull @Positive Long taskId,
            @NotNull UpdateTaskRequest request,
            @NotNull @Positive Long userId
    );

    @Transactional(readOnly = true)
    MeetingTasksResponse getMeetingTasksForUser(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long userId
    );

    @Transactional(readOnly = true)
    MeetingTaskFormResponse getTaskCreationFormData(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long userId
    );

    @Transactional(readOnly = true)
    MeetingTaskDetailsResponse getTaskDetailsForUser(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long taskId,
            @NotNull @Positive Long userId
    );

    MeetingTaskEditResponse getTaskForEditing(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long taskId,
            @NotNull @Positive Long userId
    );

    MeetingTaskAssignmentsResponse getTaskAssignmentsForUser(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long taskId,
            @NotNull @Positive Long userId
    );
//
//    @Transactional(readOnly = true)
//    List<TaskFile> getTaskFiles(Long taskId, Long userId);

//    @Transactional
//    TaskFile uploadTaskFile(Long taskId, MultipartFile file, Long userId, String description);

    @Transactional(readOnly = true)
    List<TaskFile> getUserFilesForTask(Long taskId, Long userId);

    @Transactional(readOnly = true)
    boolean canUserUploadToTask(Long taskId, Long userId);

    @Transactional(readOnly = true)
    boolean canUserViewTaskFiles(Long taskId, Long userId);

}









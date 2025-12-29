// TaskService.java
package com.meethub.domain.service;

import com.meethub.domain.model.entity.Task;
import com.meethub.domain.model.entity.TaskAssignment;
import com.meethub.domain.model.entity.TaskFile;
import com.meethub.domain.model.enums.AssignmentStatus;
import com.meethub.domain.model.request.CreateTaskRequest;
import com.meethub.domain.model.request.UpdateTaskRequest;
import com.meethub.domain.model.response.*;
import jakarta.validation.constraints.NotNull;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


public interface TaskService {

    Task createTask(
            CreateTaskRequest request,
            @NotNull Long meetingId,
            @NotNull Long organizerId
    );

    List<Task> getMeetingTasks(@NotNull Long meetingId);
    List<Task> getUserCreatedTasks(@NotNull Long userId);
    Task getTaskById(@NotNull Long taskId);
    void deleteTask(@NotNull Long taskId, @NotNull Long userId);

    TaskAssignment assignTask(
            @NotNull Long taskId,
            @NotNull Long userId,
            @NotNull Long organizerId
    );

    TaskAssignment assignTaskToCurrentUser(@NotNull Long taskId, @NotNull Long userId);
    List<TaskAssignment> getUserAssignments(@NotNull Long userId);
    List<TaskAssignment> getTaskAssignments(@NotNull Long taskId);
    TaskAssignment getAssignmentById(@NotNull Long assignmentId);

    TaskAssignment updateAssignmentStatus(
            @NotNull Long assignmentId,
            @NotNull AssignmentStatus status,
            @NotNull Long userId
    );

    TaskAssignment updateAssignmentComment(
            @NotNull Long assignmentId,
            String comment,
            @NotNull Long userId
    );

    void removeAssignment(@NotNull Long assignmentId, @NotNull Long userId);

    TaskFile uploadFile(
            @NotNull Long assignmentId,
            MultipartFile file,
            @NotNull Long userId
    );

    TaskFile getFileById(Long fileId);
    TaskFile uploadFileToAssignment(Long assignmentId, MultipartFile file, Long userId, String description);
    TaskFile uploadFileToTask(Long taskId, MultipartFile file, Long userId, String description);

    Resource downloadFile(@NotNull Long fileId, @NotNull Long userId);
    List<TaskFile> getAssignmentFiles(@NotNull Long assignmentId, @NotNull Long userId);
    List<TaskFile> getTaskFiles(Long taskId, Long userId);
    List<TaskFile> getAllTaskFilesForOrganizer(Long taskId, Long userId);
    void deleteFile(@NotNull Long fileId, @NotNull Long userId);

    List<TaskAssignment> getAssignmentsByStatus(@NotNull Long taskId, @NotNull AssignmentStatus status);
    Long countCompletedAssignments(@NotNull Long taskId);
    Long countTotalAssignments(@NotNull Long taskId);

    boolean canUserManageTask(@NotNull Long taskId, @NotNull Long userId);
    boolean canUserAccessAssignment(@NotNull Long assignmentId, @NotNull Long userId);

    Task updateTaskWithRequest(
            @NotNull Long taskId,
            UpdateTaskRequest request,
            @NotNull Long userId
    );

    MeetingTasksResponse getMeetingTasksForUser(@NotNull Long meetingId, @NotNull Long userId);
    MeetingTaskFormResponse getTaskCreationFormData(@NotNull Long meetingId, @NotNull Long userId);
    MeetingTaskDetailsResponse getTaskDetailsForUser(@NotNull Long meetingId, @NotNull Long taskId, @NotNull Long userId);
    MeetingTaskEditResponse getTaskForEditing(@NotNull Long meetingId, @NotNull Long taskId, @NotNull Long userId);
    MeetingTaskAssignmentsResponse getTaskAssignmentsForUser(@NotNull Long meetingId, @NotNull Long taskId, @NotNull Long userId);
    List<TaskFile> getUserFilesForTask(Long taskId, Long userId);

    boolean canUserUploadToTask(Long taskId, Long userId);
    boolean canUserViewTaskFiles(Long taskId, Long userId);
}
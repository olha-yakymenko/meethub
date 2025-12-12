// TaskService.java
package com.meethub.domain.service;

import com.meethub.domain.model.entity.Task;
import com.meethub.domain.model.entity.TaskAssignment;
import com.meethub.domain.model.entity.TaskFile;
import com.meethub.domain.model.enums.AssignmentStatus;
import com.meethub.domain.model.request.CreateTaskRequest;
import com.meethub.domain.model.request.UpdateTaskRequest;
import com.meethub.domain.model.response.*;
import org.springframework.core.io.Resource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TaskService {

    // Zarządzanie zadaniami
    Task createTask(CreateTaskRequest request, Long meetingId, Long organizerId);
    List<Task> getMeetingTasks(Long meetingId);
    List<Task> getUserCreatedTasks(Long userId);
    Task getTaskById(Long taskId);
    void deleteTask(Long taskId, Long userId);

    // Przypisania zadań
    TaskAssignment assignTask(Long taskId, Long userId, Long organizerId);
    TaskAssignment assignTaskToCurrentUser(Long taskId, Long userId);
    List<TaskAssignment> getUserAssignments(Long userId);
    List<TaskAssignment> getTaskAssignments(Long taskId);
    TaskAssignment getAssignmentById(Long assignmentId);
    TaskAssignment updateAssignmentStatus(Long assignmentId, AssignmentStatus status, Long userId);
    TaskAssignment updateAssignmentComment(Long assignmentId, String comment, Long userId);
    void removeAssignment(Long assignmentId, Long userId);

    // Pliki
    TaskFile uploadFile(Long assignmentId, MultipartFile file, Long userId);
    Resource downloadFile(Long fileId, Long userId);
    List<TaskFile> getAssignmentFiles(Long assignmentId, Long userId);
    void deleteFile(Long fileId, Long userId);

    // Statusy i statystyki
    List<TaskAssignment> getAssignmentsByStatus(Long taskId, AssignmentStatus status);
    Long countCompletedAssignments(Long taskId);
    Long countTotalAssignments(Long taskId);

    // Walidacje
    boolean canUserManageTask(Long taskId, Long userId);
    boolean canUserAccessAssignment(Long assignmentId, Long userId);

    Task updateTaskWithRequest(Long taskId, UpdateTaskRequest request, Long userId);

    @Transactional(readOnly = true)
    MeetingTasksResponse getMeetingTasksForUser(Long meetingId, Long userId);

    @Transactional(readOnly = true)
    MeetingTaskFormResponse getTaskCreationFormData(Long meetingId, Long userId);

    // TaskServiceImpl.java
    @Transactional(readOnly = true)
    MeetingTaskDetailsResponse getTaskDetailsForUser(Long meetingId, Long taskId, Long userId);

    // TaskService.java
    MeetingTaskEditResponse getTaskForEditing(Long meetingId, Long taskId, Long userId);

    // TaskService.java
    MeetingTaskAssignmentsResponse getTaskAssignmentsForUser(Long meetingId, Long taskId, Long userId);

}
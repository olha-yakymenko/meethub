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

    // Zarządzanie zadaniami
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

    Resource downloadFile(
            @NotNull @Positive Long fileId,
            @NotNull @Positive Long userId
    );

    List<TaskFile> getAssignmentFiles(
            @NotNull @Positive Long assignmentId,
            @NotNull @Positive Long userId
    );

    void deleteFile(
            @NotNull @Positive Long fileId,
            @NotNull @Positive Long userId
    );

    // Statusy i statystyki
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
}










//// TaskService.java
//package com.meethub.domain.service;
//
//import com.meethub.domain.model.entity.Task;
//import com.meethub.domain.model.entity.TaskAssignment;
//import com.meethub.domain.model.entity.TaskFile;
//import com.meethub.domain.model.enums.AssignmentStatus;
//import com.meethub.domain.model.request.CreateTaskRequest;
//import com.meethub.domain.model.request.UpdateTaskRequest;
//import com.meethub.domain.model.response.*;
//import org.springframework.core.io.Resource;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.util.List;
//
//public interface TaskService {
//
//    // Zarządzanie zadaniami
//    Task createTask(CreateTaskRequest request, Long meetingId, Long organizerId);
//    List<Task> getMeetingTasks(Long meetingId);
//    List<Task> getUserCreatedTasks(Long userId);
//    Task getTaskById(Long taskId);
//    void deleteTask(Long taskId, Long userId);
//
//    // Przypisania zadań
//    TaskAssignment assignTask(Long taskId, Long userId, Long organizerId);
//    TaskAssignment assignTaskToCurrentUser(Long taskId, Long userId);
//    List<TaskAssignment> getUserAssignments(Long userId);
//    List<TaskAssignment> getTaskAssignments(Long taskId);
//    TaskAssignment getAssignmentById(Long assignmentId);
//    TaskAssignment updateAssignmentStatus(Long assignmentId, AssignmentStatus status, Long userId);
//    TaskAssignment updateAssignmentComment(Long assignmentId, String comment, Long userId);
//    void removeAssignment(Long assignmentId, Long userId);
//
//    // Pliki
//    TaskFile uploadFile(Long assignmentId, MultipartFile file, Long userId);
//    Resource downloadFile(Long fileId, Long userId);
//    List<TaskFile> getAssignmentFiles(Long assignmentId, Long userId);
//    void deleteFile(Long fileId, Long userId);
//
//    // Statusy i statystyki
//    List<TaskAssignment> getAssignmentsByStatus(Long taskId, AssignmentStatus status);
//    Long countCompletedAssignments(Long taskId);
//    Long countTotalAssignments(Long taskId);
//
//    // Walidacje
//    boolean canUserManageTask(Long taskId, Long userId);
//    boolean canUserAccessAssignment(Long assignmentId, Long userId);
//
//    Task updateTaskWithRequest(Long taskId, UpdateTaskRequest request, Long userId);
//
//    @Transactional(readOnly = true)
//    MeetingTasksResponse getMeetingTasksForUser(Long meetingId, Long userId);
//
//    @Transactional(readOnly = true)
//    MeetingTaskFormResponse getTaskCreationFormData(Long meetingId, Long userId);
//
//    // TaskServiceImpl.java
//    @Transactional(readOnly = true)
//    MeetingTaskDetailsResponse getTaskDetailsForUser(Long meetingId, Long taskId, Long userId);
//
//    // TaskService.java
//    MeetingTaskEditResponse getTaskForEditing(Long meetingId, Long taskId, Long userId);
//
//    // TaskService.java
//    MeetingTaskAssignmentsResponse getTaskAssignmentsForUser(Long meetingId, Long taskId, Long userId);
//
//}
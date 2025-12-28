package com.meethub.controller.web;

import com.meethub.domain.model.entity.*;
import com.meethub.domain.model.request.CreateTaskRequest;
import com.meethub.domain.model.request.UpdateTaskRequest;
import com.meethub.domain.model.response.*;
import com.meethub.domain.service.TaskService;
import com.meethub.security.CustomUserDetailsService.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingTaskControllerTest {

    @Mock
    private TaskService taskService;

    @Mock
    private Model model;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private MeetingTaskController controller;

    private CustomUserDetails userDetails;
    private Meeting meeting;
    private Task task;
    private Long meetingId = 1L;
    private Long taskId = 1L;
    private Long userId = 1L;

    @BeforeEach
    void setUp() {
        userDetails = mock(CustomUserDetails.class);
        lenient().when(userDetails.getId()).thenReturn(userId);
        lenient().when(userDetails.getUsername()).thenReturn("test@example.com");

        meeting = new Meeting();
        meeting.setId(meetingId);
        meeting.setTitle("Test Meeting");

        User organizer = new User();
        organizer.setId(userId);
        meeting.setOrganizer(organizer);

        task = new Task();
        task.setId(taskId);
        task.setTitle("Test Task");
        task.setDeadline(LocalDateTime.now().plusDays(7));

        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void testGetMeetingTasks_Success() {
        MeetingTasksResponse response = MeetingTasksResponse.builder()
                .meeting(meeting)
                .tasks(List.of(task))
                .isOrganizer(true)
                .build();

        when(taskService.getMeetingTasksForUser(meetingId, userId)).thenReturn(response);

        String viewName = controller.getMeetingTasks(meetingId, userDetails, model);

        assertEquals("meetings/tasks/list", viewName);
    }

    @Test
    void testShowCreateTaskForm_Success() {
        MeetingTaskFormResponse response = MeetingTaskFormResponse.builder()
                .meeting(meeting)
                .createTaskRequest(new CreateTaskRequest())
                .build();

        when(taskService.getTaskCreationFormData(meetingId, userId)).thenReturn(response);

        String viewName = controller.showCreateTaskForm(meetingId, userDetails, model);

        assertEquals("meetings/tasks/create", viewName);
    }

    @Test
    void testCreateTask_Success() {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("New Task");

        when(bindingResult.hasErrors()).thenReturn(false);
        when(taskService.createTask(any(), eq(meetingId), eq(userId))).thenReturn(task);

        String redirect = controller.createTask(meetingId, request, bindingResult, userDetails, redirectAttributes);

        assertEquals("redirect:/meetings/1/tasks/1", redirect);
    }

    @Test
    void testCreateTask_ValidationErrors() {
        CreateTaskRequest request = new CreateTaskRequest();
        when(bindingResult.hasErrors()).thenReturn(true);

        String redirect = controller.createTask(meetingId, request, bindingResult, userDetails, redirectAttributes);

        assertEquals("redirect:/meetings/1/tasks/create", redirect);
    }

    @Test
    void testGetTaskDetails_Success() {
        MeetingTaskDetailsResponse response = MeetingTaskDetailsResponse.builder()
                .meeting(meeting)
                .task(task)
                .isOrganizer(true)
                .userId(userId)
                .build();

        when(taskService.getTaskDetailsForUser(meetingId, taskId, userId)).thenReturn(response);

        String viewName = controller.getTaskDetails(meetingId, taskId, userDetails, model);

        assertEquals("meetings/tasks/details", viewName);
    }

    @Test
    void testShowEditTaskForm_Success() {
        MeetingTaskEditResponse response = MeetingTaskEditResponse.builder()
                .meeting(meeting)
                .task(task)
                .formattedDeadline("2024-12-31 23:59")
                .build();

        when(taskService.getTaskForEditing(meetingId, taskId, userId)).thenReturn(response);

        String viewName = controller.showEditTaskForm(meetingId, taskId, userDetails, model);

        assertEquals("meetings/tasks/edit", viewName);
    }

    @Test
    void testUpdateTask_Success() {
        UpdateTaskRequest request = new UpdateTaskRequest();
        when(bindingResult.hasErrors()).thenReturn(false);
        when(taskService.updateTaskWithRequest(eq(taskId), any(), eq(userId))).thenReturn(task);

        String redirect = controller.updateTask(meetingId, taskId, request, bindingResult, userDetails, redirectAttributes);

        assertEquals("redirect:/meetings/1/tasks/1", redirect);
    }

    @Test
    void testUpdateTask_ValidationErrors() {
        UpdateTaskRequest request = new UpdateTaskRequest();
        when(bindingResult.hasErrors()).thenReturn(true);

        String redirect = controller.updateTask(meetingId, taskId, request, bindingResult, userDetails, redirectAttributes);

        assertEquals("redirect:/meetings/1/tasks/1/edit", redirect);
    }

    @Test
    void testDeleteTask_Success() {
        String redirect = controller.deleteTask(meetingId, taskId, userDetails, redirectAttributes);
        assertEquals("redirect:/meetings/1/tasks", redirect);
    }

    @Test
    void testAssignSelfToTask_Success() {
        String redirect = controller.assignSelfToTask(meetingId, taskId, userDetails, redirectAttributes);
        assertEquals("redirect:/meetings/1/tasks/1", redirect);
    }

    @Test
    void testUpdateAssignmentComment_Success() {
        String redirect = controller.updateAssignmentComment(meetingId, taskId, 1L, "Test comment", userDetails, redirectAttributes);
        assertEquals("redirect:/meetings/1/tasks/1", redirect);
    }

    @Test
    void testShowAssignUsersForm_Success() {
        MeetingTaskAssignmentsResponse response = MeetingTaskAssignmentsResponse.builder()
                .meeting(meeting)
                .task(task)
                .availableUsers(new ArrayList<>())
                .assignedUsers(new ArrayList<>())
                .assignments(new ArrayList<>())
                .build();

        when(taskService.getTaskAssignmentsForUser(meetingId, taskId, userId)).thenReturn(response);

        String viewName = controller.showAssignUsersForm(meetingId, taskId, userDetails, model);

        assertEquals("meetings/tasks/assign", viewName);
    }

    @Test
    void testAssignUserToTask_Success() {
        String redirect = controller.assignUserToTask(meetingId, taskId, 2L, userDetails, redirectAttributes);
        assertEquals("redirect:/meetings/1/tasks/1/assign", redirect);
    }

    @Test
    void testRemoveAssignment_Success() {
        String redirect = controller.removeAssignment(meetingId, taskId, 1L, userDetails, redirectAttributes);
        assertEquals("redirect:/meetings/1/tasks/1/assign", redirect);
    }

    @Test
    void testUpdateAssignmentStatus_Success() {
        String redirect = controller.updateAssignmentStatus(meetingId, taskId, 1L, "IN_PROGRESS", userDetails, redirectAttributes);
        assertEquals("redirect:/meetings/1/tasks/1", redirect);
    }

    @Test
    void testUpdateAssignmentStatus_InvalidStatus() {
        String redirect = controller.updateAssignmentStatus(meetingId, taskId, 1L, "INVALID_STATUS", userDetails, redirectAttributes);
        assertEquals("redirect:/meetings/1/tasks/1", redirect);
    }

    @Test
    void testGetTaskFiles_Success() {
        MeetingTaskDetailsResponse detailsResponse = MeetingTaskDetailsResponse.builder()
                .meeting(meeting)
                .task(task)
                .isOrganizer(true)
                .userId(userId)
                .build();

        List<TaskFile> files = List.of(new TaskFile());

        when(taskService.getTaskDetailsForUser(meetingId, taskId, userId)).thenReturn(detailsResponse);
        when(taskService.getAllTaskFilesForOrganizer(taskId, userId)).thenReturn(files);
        when(taskService.canUserUploadToTask(taskId, userId)).thenReturn(true);

        String viewName = controller.getTaskFiles(meetingId, taskId, userDetails, model);

        assertEquals("meetings/tasks/files", viewName);
    }

    @Test
    void testGetTaskFiles_UserView() {
        MeetingTaskDetailsResponse detailsResponse = MeetingTaskDetailsResponse.builder()
                .meeting(meeting)
                .task(task)
                .isOrganizer(false)
                .userId(userId)
                .build();

        List<TaskFile> files = List.of(new TaskFile());

        when(taskService.getTaskDetailsForUser(meetingId, taskId, userId)).thenReturn(detailsResponse);
        when(taskService.getUserFilesForTask(taskId, userId)).thenReturn(files);
        when(taskService.canUserUploadToTask(taskId, userId)).thenReturn(true);

        String viewName = controller.getTaskFiles(meetingId, taskId, userDetails, model);

        assertEquals("meetings/tasks/files", viewName);
    }

    @Test
    void testDownloadTaskFile_Success() {
        Resource resource = mock(Resource.class);
        TaskFile taskFile = new TaskFile();
        taskFile.setId(1L);
        taskFile.setOriginalFilename("test.txt");
        taskFile.setContentType("text/plain");

        when(taskService.downloadFile(anyLong(), eq(userId))).thenReturn(resource);
        when(taskService.getFileById(anyLong())).thenReturn(taskFile);

        ResponseEntity<Resource> response = controller.downloadTaskFile(meetingId, taskId, 1L, userDetails);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getHeaders().get(HttpHeaders.CONTENT_DISPOSITION));
    }

    @Test
    void testDeleteTaskFile_Success() {
        String redirect = controller.deleteTaskFile(meetingId, taskId, 1L, userDetails, redirectAttributes);
        assertEquals("redirect:/meetings/1/tasks/1/files", redirect);
    }

    @Test
    void testGetOrganizerTaskView_Success() {
        MeetingTaskDetailsResponse detailsResponse = MeetingTaskDetailsResponse.builder()
                .meeting(meeting)
                .task(task)
                .isOrganizer(true)
                .build();

        List<TaskFile> files = new ArrayList<>();
        List<TaskAssignment> assignments = new ArrayList<>();

        when(taskService.getTaskDetailsForUser(meetingId, taskId, userId)).thenReturn(detailsResponse);
        when(taskService.getAllTaskFilesForOrganizer(taskId, userId)).thenReturn(files);
        when(taskService.getTaskAssignments(taskId)).thenReturn(assignments);

        String viewName = controller.getOrganizerTaskView(meetingId, taskId, userDetails, model);

        assertEquals("meetings/tasks/organizer-view", viewName);
    }

    @Test
    void testGetOrganizerTaskView_NotOrganizer() {
        MeetingTaskDetailsResponse detailsResponse = MeetingTaskDetailsResponse.builder()
                .meeting(meeting)
                .task(task)
                .isOrganizer(false)
                .build();

        when(taskService.getTaskDetailsForUser(meetingId, taskId, userId)).thenReturn(detailsResponse);

        String redirect = controller.getOrganizerTaskView(meetingId, taskId, userDetails, model);

        assertEquals("redirect:/meetings/1/tasks/1", redirect);
    }
}
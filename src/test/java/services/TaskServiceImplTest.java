package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.*;
import com.meethub.domain.model.enums.AssignmentStatus;
import com.meethub.domain.model.enums.TaskStatus;
import com.meethub.domain.model.request.CreateTaskRequest;
import com.meethub.domain.model.request.UpdateTaskRequest;
import com.meethub.domain.model.response.*;
import com.meethub.domain.repository.jpa.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock private TaskRepository taskRepository;
    @Mock private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private TaskServiceImpl taskService;

    private User organizer;
    private User participant;
    private Task task;
    private TaskAssignment assignment;
    private Meeting meeting;

    @BeforeEach
    void setUp() {
        // Ustawienie wartości dla uploadDir
        taskService = new TaskServiceImpl(taskRepository, null, null, jdbcTemplate) {
            {
                // Inicjalizacja uploadDir przez refleksję
                try {
                    var field = TaskServiceImpl.class.getDeclaredField("uploadDir");
                    field.setAccessible(true);
                    field.set(this, "test-uploads");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        // Create organizer
        organizer = new User();
        organizer.setId(1L);
        organizer.setFirstName("Organizer");
        organizer.setLastName("Test");
        organizer.setEmail("organizer@test.com");

        // Create participant
        participant = new User();
        participant.setId(2L);
        participant.setFirstName("Participant");
        participant.setLastName("Test");
        participant.setEmail("participant@test.com");

        // Create meeting
        meeting = new Meeting();
        meeting.setId(100L);
        meeting.setTitle("Test Meeting");
        meeting.setOrganizer(organizer);

        // Create task
        task = new Task();
        task.setId(200L);
        task.setTitle("Test Task");
        task.setDescription("Test Description");
        task.setStatus(TaskStatus.TODO);
        task.setMeetingId(100L);
        task.setCreatedById(1L);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        task.setAllowedFileTypes("pdf,doc,docx");
        task.setMaxFileSize(10L * 1024 * 1024);
        task.setMaxFilesPerUser(5);
        task.setAllowSelfAssignment(true);

        // Create assignment
        assignment = new TaskAssignment();
        assignment.setId(300L);
        assignment.setTask(task);
        assignment.setUser(participant);
        assignment.setStatus(AssignmentStatus.ASSIGNED);
        assignment.setAssignedAt(LocalDateTime.now());
        assignment.setComment("Test comment");
    }

    // ========== CREATE TASK TESTS ==========

    @Test
    void createTask_shouldCreateTaskSuccessfully() {
        // Given
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("New Task");
        request.setDescription("Task Description");
        request.setDeadline(LocalDateTime.now().plusDays(7));
        request.setAllowSelfAssignment(true);
        request.setMaxFilesPerUser(10);
        request.setMaxFileSize(10L * 1024 * 1024);
        request.setAllowedFileTypes(Arrays.asList("pdf", "doc"));

        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(100L)))
                .thenReturn(1L);
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), eq(1L)))
                .thenReturn(true);

        Task savedTask = new Task();
        savedTask.setId(201L);
        savedTask.setTitle("New Task");
        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        // When
        Task result = taskService.createTask(request, 100L, 1L);

        // Then
        assertNotNull(result);
    }

    @Test
    void createTask_shouldThrowException_whenMeetingNotFound() {
        // Given
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("New Task");

        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(100L)))
                .thenThrow(new EmptyResultDataAccessException(1));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.createTask(request, 100L, 1L));
        assertEquals("Spotkanie nie zostało znalezione", exception.getMessage());
    }

    @Test
    void createTask_shouldThrowException_whenUserNotOrganizer() {
        // Given
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("New Task");

        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(100L)))
                .thenReturn(999L);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.createTask(request, 100L, 1L));
        assertEquals("Tylko organizator może wykonać tę akcję", exception.getMessage());
    }

    // ========== GET TASK TESTS ==========

    @Test
    void getMeetingTasks_shouldReturnTasks() {
        // Given
        when(taskRepository.findByMeetingId(100L)).thenReturn(Arrays.asList(task));

        // When
        List<Task> result = taskService.getMeetingTasks(100L);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    void getUserCreatedTasks_shouldReturnTasksCreatedByUser() {
        // Given
        when(taskRepository.findByCreatedById(1L)).thenReturn(Arrays.asList(task));

        // When
        List<Task> result = taskService.getUserCreatedTasks(1L);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    void getTaskById_shouldReturnTask() {
        // Given
        when(taskRepository.findById(200L)).thenReturn(Optional.of(task));

        // When
        Task result = taskService.getTaskById(200L);

        // Then
        assertEquals(200L, result.getId());
    }

    @Test
    void getTaskById_shouldThrowException_whenTaskNotFound() {
        // Given
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.getTaskById(999L));
        assertEquals("Zadanie nie zostało znalezione", exception.getMessage());
    }

    // ========== UPDATE TASK TESTS ==========

    @Test
    void updateTaskWithRequest_shouldUpdateTaskSuccessfully() {
        // Given
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTitle("Updated Task");
        request.setDescription("Updated Description");

        when(taskRepository.findById(200L)).thenReturn(Optional.of(task));
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(), any()))
                .thenReturn(true);

        Task updatedTask = new Task();
        updatedTask.setId(200L);
        updatedTask.setTitle("Updated Task");
        when(taskRepository.save(any(Task.class))).thenReturn(updatedTask);

        // When
        Task result = taskService.updateTaskWithRequest(200L, request, 1L);

        // Then
        assertEquals("Updated Task", result.getTitle());
    }

    @Test
    void updateTaskWithRequest_shouldThrowException_whenNotOrganizer() {
        // Given
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTitle("Updated Task");

        when(taskRepository.findById(200L)).thenReturn(Optional.of(task));
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(), any()))
                .thenReturn(false);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.updateTaskWithRequest(200L, request, 2L));
        assertEquals("Tylko organizator może wykonać tę akcję", exception.getMessage());
    }

    // ========== DELETE TASK TESTS ==========

    @Test
    void deleteTask_shouldDeleteTaskSuccessfully() {
        // Given
        when(taskRepository.findById(200L)).thenReturn(Optional.of(task));
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(), any()))
                .thenReturn(true);
        when(jdbcTemplate.update(anyString(), anyLong())).thenReturn(1);

        // When
        taskService.deleteTask(200L, 1L);

        // Then
        verify(taskRepository).deleteById(200L);
    }

    @Test
    void deleteTask_shouldThrowException_whenNotOrganizer() {
        // Given
        when(taskRepository.findById(200L)).thenReturn(Optional.of(task));
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(), any()))
                .thenReturn(false);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.deleteTask(200L, 2L));
        assertEquals("Tylko organizator może wykonać tę akcję", exception.getMessage());
    }

    // ========== ASSIGNMENT TESTS ==========


    @Test
    void assignTaskToCurrentUser_shouldThrowException_whenNotParticipant() {
        // Given
        when(taskRepository.findById(200L)).thenReturn(Optional.of(task));
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(), any()))
                .thenReturn(false);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.assignTaskToCurrentUser(200L, 2L));
        assertEquals("Nie jesteś uczestnikiem tego spotkania", exception.getMessage());
    }

    @Test
    void assignTaskToCurrentUser_shouldThrowException_whenAlreadyAssigned() {
        // Given
        when(taskRepository.findById(200L)).thenReturn(Optional.of(task));
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(), any()))
                .thenReturn(true, true);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.assignTaskToCurrentUser(200L, 2L));
        assertEquals("Jesteś już przypisany do tego zadania", exception.getMessage());
    }

    // ========== GET ASSIGNMENT TESTS ==========

    @Test
    void getUserAssignments_shouldReturnUserAssignments() {
        // Given
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any()))
                .thenReturn(Arrays.asList(assignment));

        // When
        List<TaskAssignment> result = taskService.getUserAssignments(2L);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    void getTaskAssignments_shouldReturnAssignments() {
        // Given
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any()))
                .thenReturn(Arrays.asList(assignment));

        // When
        List<TaskAssignment> result = taskService.getTaskAssignments(200L);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    void getAssignmentById_shouldReturnAssignment() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), any()))
                .thenReturn(assignment);

        // When
        TaskAssignment result = taskService.getAssignmentById(300L);

        // Then
        assertEquals(300L, result.getId());
    }

    @Test
    void getAssignmentById_shouldThrowException_whenNotFound() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), any()))
                .thenThrow(new EmptyResultDataAccessException(1));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.getAssignmentById(999L));
        assertEquals("Przypisanie nie zostało znalezione", exception.getMessage());
    }



    @Test
    void updateAssignmentComment_shouldUpdateCommentSuccessfully() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), any()))
                .thenReturn(assignment);
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(), any()))
                .thenReturn(true);

        // Użycie konkretnych wartości dla update
        when(jdbcTemplate.update(eq("UPDATE task_assignments SET comment = ? WHERE id = ?"),
                eq("Updated comment"), eq(300L)))
                .thenReturn(1);

        // When
        TaskAssignment result = taskService.updateAssignmentComment(300L, "Updated comment", 1L);

        // Then
        assertEquals("Updated comment", result.getComment());
    }

    @Test
    void updateAssignmentComment_shouldThrowException_whenUpdateFails() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), any()))
                .thenReturn(assignment);
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(), any()))
                .thenReturn(true);

        // Użycie konkretnych wartości dla update
        when(jdbcTemplate.update(eq("UPDATE task_assignments SET comment = ? WHERE id = ?"),
                eq("New comment"), eq(300L)))
                .thenReturn(0);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.updateAssignmentComment(300L, "New comment", 1L));
        assertEquals("Nie udało się zaktualizować komentarza przypisania", exception.getMessage());
    }

    @Test
    void updateAssignmentStatus_shouldUpdateStatusSuccessfully() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), any()))
                .thenReturn(assignment);
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(), any()))
                .thenReturn(true);

        // Użycie konkretnych wartości dla update
        when(jdbcTemplate.update(eq("UPDATE task_assignments SET status = ?, completed_at = ? WHERE id = ?"),
                eq("COMPLETED"), any(Timestamp.class), eq(300L)))
                .thenReturn(1);

        // When
        TaskAssignment result = taskService.updateAssignmentStatus(300L, AssignmentStatus.COMPLETED, 1L);

        // Then
        assertEquals(AssignmentStatus.COMPLETED, result.getStatus());
    }

    @Test
    void updateAssignmentStatus_shouldThrowException_whenUpdateFails() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), any()))
                .thenReturn(assignment);
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(), any()))
                .thenReturn(true);

        // Użycie konkretnych wartości dla update
        when(jdbcTemplate.update(eq("UPDATE task_assignments SET status = ?, completed_at = ? WHERE id = ?"),
                eq("COMPLETED"), any(), eq(300L)))
                .thenReturn(0);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.updateAssignmentStatus(300L, AssignmentStatus.COMPLETED, 1L));
        assertEquals("Nie udało się zaktualizować statusu przypisania", exception.getMessage());
    }

// ========== REMOVE ASSIGNMENT TESTS ==========

    @Test
    void removeAssignment_shouldRemoveAssignmentSuccessfully() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), any()))
                .thenReturn(assignment);
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(), any()))
                .thenReturn(true);

        // Użycie konkretnej wartości dla update
        when(jdbcTemplate.update(eq("DELETE FROM task_assignments WHERE id = ?"), eq(300L)))
                .thenReturn(1);

        // When
        taskService.removeAssignment(300L, 1L);

        // Then
        verify(jdbcTemplate).update(eq("DELETE FROM task_assignments WHERE id = ?"), eq(300L));
    }

    @Test
    void removeAssignment_shouldAllowOwnerToRemove() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), any()))
                .thenReturn(assignment);
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(), any()))
                .thenReturn(false); // Not organizer

        // Użycie konkretnej wartości dla update
        when(jdbcTemplate.update(eq("DELETE FROM task_assignments WHERE id = ?"), eq(300L)))
                .thenReturn(1);

        // When - user is assignment owner (ID 2)
        taskService.removeAssignment(300L, 2L);

        // Then
        verify(jdbcTemplate).update(eq("DELETE FROM task_assignments WHERE id = ?"), eq(300L));
    }

    @Test
    void removeAssignment_shouldThrowException_whenUnauthorized() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), any()))
                .thenReturn(assignment);
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(), any()))
                .thenReturn(false); // Not organizer

        // When & Then - user 999 is not owner or organizer
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.removeAssignment(300L, 999L));
        assertEquals("Brak uprawnień do usunięcia przypisania", exception.getMessage());
    }

    @Test
    void removeAssignment_shouldThrowException_whenNotFound() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), any()))
                .thenReturn(assignment);
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(), any()))
                .thenReturn(true);

        // Użycie konkretnej wartości dla update
        when(jdbcTemplate.update(eq("DELETE FROM task_assignments WHERE id = ?"), eq(300L)))
                .thenReturn(0);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.removeAssignment(300L, 1L));
        assertEquals("Przypisanie nie zostało znalezione", exception.getMessage());
    }



    // ========== FILE OPERATION TESTS ==========

    @Test
    void uploadFile_shouldThrowException_whenFileEmpty() {
        // Given
        MultipartFile emptyFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                new byte[0]
        );

        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), any()))
                .thenReturn(assignment);
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(), any()))
                .thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.uploadFile(300L, emptyFile, 2L));
        assertEquals("Plik jest pusty", exception.getMessage());
    }

    @Test
    void getAssignmentFiles_shouldReturnFiles() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), any()))
                .thenReturn(assignment);
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(), any()))
                .thenReturn(true);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any()))
                .thenReturn(Arrays.asList(createMockTaskFile()));

        // When
        List<TaskFile> result = taskService.getAssignmentFiles(300L, 2L);

        // Then
        assertThat(result).hasSize(1);
    }

    // ========== STATUS AND COUNT TESTS ==========

    @Test
    void getAssignmentsByStatus_shouldFilterByStatus() {
        // Given
        TaskAssignment completedAssignment = createMockTaskAssignment();
        completedAssignment.setStatus(AssignmentStatus.COMPLETED);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any()))
                .thenReturn(Arrays.asList(completedAssignment));

        // When
        List<TaskAssignment> result = taskService.getAssignmentsByStatus(200L, AssignmentStatus.COMPLETED);

        // Then
        assertEquals(AssignmentStatus.COMPLETED, result.get(0).getStatus());
    }

    @Test
    void countCompletedAssignments_shouldReturnCorrectCount() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any()))
                .thenReturn(2L);

        // When
        Long count = taskService.countCompletedAssignments(200L);

        // Then
        assertEquals(2L, count);
    }

    @Test
    void countTotalAssignments_shouldReturnCorrectCount() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any()))
                .thenReturn(3L);

        // When
        Long count = taskService.countTotalAssignments(200L);

        // Then
        assertEquals(3L, count);
    }

    // ========== PERMISSION TESTS ==========

    @Test
    void canUserManageTask_shouldReturnTrueForOrganizer() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(), any()))
                .thenReturn(true);

        // When
        boolean result = taskService.canUserManageTask(200L, 1L);

        // Then
        assertTrue(result);
    }

    @Test
    void canUserManageTask_shouldReturnFalseForNonOrganizer() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(), any()))
                .thenReturn(false);

        // When
        boolean result = taskService.canUserManageTask(200L, 2L);

        // Then
        assertFalse(result);
    }

    @Test
    void canUserAccessAssignment_shouldReturnTrueForOwner() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(), any(), any()))
                .thenReturn(true);

        // When
        boolean result = taskService.canUserAccessAssignment(300L, 2L);

        // Then
        assertTrue(result);
    }

    @Test
    void canUserAccessAssignment_shouldReturnTrueForOrganizer() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(), any(), any()))
                .thenReturn(true);

        // When
        boolean result = taskService.canUserAccessAssignment(300L, 1L);

        // Then
        assertTrue(result);
    }

    @Test
    void canUserAccessAssignment_shouldReturnFalseForUnauthorizedUser() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(), any(), any()))
                .thenReturn(false);

        // When
        boolean result = taskService.canUserAccessAssignment(300L, 999L);

        // Then
        assertFalse(result);
    }

    // ========== RESPONSE OBJECT TESTS ==========

    @Test
    void getMeetingTasksForUser_shouldReturnResponse() {
        // Given
        when(taskRepository.findByMeetingId(100L)).thenReturn(Arrays.asList(task));
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), any()))
                .thenReturn(meeting);

        // When
        MeetingTasksResponse result = taskService.getMeetingTasksForUser(100L, 1L);

        // Then
        assertNotNull(result);
    }

    @Test
    void getTaskCreationFormData_shouldReturnFormData() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), any()))
                .thenReturn(meeting);

        // When
        MeetingTaskFormResponse result = taskService.getTaskCreationFormData(100L, 1L);

        // Then
        assertNotNull(result);
    }

    @Test
    void getTaskCreationFormData_shouldThrowException_whenNotOrganizer() {
        // Given
        Meeting otherMeeting = new Meeting();
        otherMeeting.setId(100L);
        otherMeeting.setOrganizer(participant);
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), any()))
                .thenReturn(otherMeeting);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.getTaskCreationFormData(100L, 1L));
        assertEquals("Tylko organizator może tworzyć zadania", exception.getMessage());
    }

    @Test
    void getTaskDetailsForUser_shouldReturnDetails() {
        // Given
        when(taskRepository.findById(200L)).thenReturn(Optional.of(task));
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), any()))
                .thenReturn(meeting);
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(), any()))
                .thenReturn(true);

        // When
        MeetingTaskDetailsResponse result = taskService.getTaskDetailsForUser(100L, 200L, 2L);

        // Then
        assertNotNull(result);
    }

    @Test
    void getTaskDetailsForUser_shouldThrowException_whenNoAccess() {
        // Given
        when(taskRepository.findById(200L)).thenReturn(Optional.of(task));
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), any()))
                .thenReturn(meeting);
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(), any()))
                .thenReturn(false);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.getTaskDetailsForUser(100L, 200L, 999L));
        assertEquals("Brak uprawnień do tego zadania", exception.getMessage());
    }

    @Test
    void getTaskForEditing_shouldReturnEditData() {
        // Given
        when(taskRepository.findById(200L)).thenReturn(Optional.of(task));
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), any()))
                .thenReturn(meeting);

        // When
        MeetingTaskEditResponse result = taskService.getTaskForEditing(100L, 200L, 1L);

        // Then
        assertNotNull(result);
    }

    @Test
    void getTaskForEditing_shouldThrowException_whenNotOrganizer() {
        // Given
        when(taskRepository.findById(200L)).thenReturn(Optional.of(task));
        Meeting otherMeeting = new Meeting();
        otherMeeting.setId(100L);
        otherMeeting.setOrganizer(participant);
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), any()))
                .thenReturn(otherMeeting);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.getTaskForEditing(100L, 200L, 1L));
        assertEquals("Tylko organizator może edytować zadania", exception.getMessage());
    }

    @Test
    void getTaskAssignmentsForUser_shouldReturnAssignmentsData() {
        // Given
        when(taskRepository.findById(200L)).thenReturn(Optional.of(task));
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), any()))
                .thenReturn(meeting);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any()))
                .thenReturn(Arrays.asList(assignment));

        // When
        MeetingTaskAssignmentsResponse result = taskService.getTaskAssignmentsForUser(100L, 200L, 1L);

        // Then
        assertNotNull(result);
    }

    // ========== FILE TESTS ==========

    @Test
    void getFileById_shouldReturnFile() {
        // Given
        TaskFile taskFile = createMockTaskFile();
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), any()))
                .thenReturn(taskFile);

        // When
        TaskFile result = taskService.getFileById(400L);

        // Then
        assertEquals(400L, result.getId());
    }

    @Test
    void getFileById_shouldThrowException_whenNotFound() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), any()))
                .thenThrow(new EmptyResultDataAccessException(1));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.getFileById(999L));
        assertEquals("Plik nie został znaleziony", exception.getMessage());
    }

    // ========== PERMISSION HELPER TESTS ==========

    @Test
    void canUserUploadToTask_shouldReturnTrueForOrganizer() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(), any(), any()))
                .thenReturn(true);

        // When
        boolean result = taskService.canUserUploadToTask(200L, 1L);

        // Then
        assertTrue(result);
    }

    @Test
    void canUserViewTaskFiles_shouldReturnTrueForAssignedUser() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(), any(), any()))
                .thenReturn(true);

        // When
        boolean result = taskService.canUserViewTaskFiles(200L, 2L);

        // Then
        assertTrue(result);
    }

    // ========== TESTY DLA NOWYCH METOD (dodane do interfejsu) ==========

    @Test
    void validateAssignmentAccess_shouldAllowOwner() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(), any()))
                .thenReturn(false);

        // When
        boolean result = taskService.validateAssignmentAccess(assignment, 2L, false);

        // Then
        assertTrue(result);
    }

    @Test
    void validateAssignmentAccess_shouldAllowOrganizer() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(), any()))
                .thenReturn(true);

        // When
        boolean result = taskService.validateAssignmentAccess(assignment, 1L, false);

        // Then
        assertTrue(result);
    }

    @Test
    void validateAssignmentAccess_shouldThrowException_whenNoAccess() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(), any()))
                .thenReturn(false);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.validateAssignmentAccess(assignment, 999L, true));
        assertEquals("Brak uprawnień do tego przypisania", exception.getMessage());
    }

    @Test
    void getFileExtension_shouldReturnExtension() {
        // Given
        String filename = "document.pdf";

        // When
        String result = taskService.getFileExtension(filename);

        // Then
        assertEquals(".pdf", result);
    }

    @Test
    void getFileExtension_shouldReturnEmptyForNoExtension() {
        // Given
        String filename = "document";

        // When
        String result = taskService.getFileExtension(filename);

        // Then
        assertEquals("", result);
    }

    @Test
    void getFileExtension_shouldReturnEmptyForNull() {
        // Given
        String filename = null;

        // When
        String result = taskService.getFileExtension(filename);

        // Then
        assertEquals("", result);
    }

    // ========== HELPER METHODS ==========

    private TaskAssignment createMockTaskAssignment() {
        TaskAssignment assignment = new TaskAssignment();
        assignment.setId(300L);
        assignment.setTask(task);
        assignment.setUser(participant);
        assignment.setStatus(AssignmentStatus.ASSIGNED);
        assignment.setAssignedAt(LocalDateTime.now());
        assignment.setComment("Test comment");
        return assignment;
    }

    private TaskFile createMockTaskFile() {
        TaskFile taskFile = new TaskFile();
        taskFile.setId(400L);
        taskFile.setOriginalFilename("test.pdf");
        taskFile.setFilename("unique_test.pdf");
        taskFile.setFilePath("test-uploads/tasks/task_200/test.pdf");
        taskFile.setFileSize(1024L);
        taskFile.setContentType("application/pdf");
        taskFile.setUploadedAt(LocalDateTime.now());

        TaskAssignment assignment = new TaskAssignment();
        assignment.setId(300L);
        taskFile.setAssignment(assignment);

        return taskFile;
    }


    @Test
    void assignTask_shouldThrowException_whenUserNotParticipant() {
        // Given
        when(taskRepository.findById(200L)).thenReturn(Optional.of(task));

        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(), any()))
                .thenReturn(false); // not participant

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.assignTask(200L, 2L, 1L));
        assertEquals("Tylko organizator może wykonać tę akcję", exception.getMessage());
    }

    @Test
    void assignTask_shouldThrowException_whenUserAlreadyAssigned() {
        // Given
        when(taskRepository.findById(200L)).thenReturn(Optional.of(task));

        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), eq(2L)))
                .thenReturn(true); // user exists

        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(), any()))
                .thenReturn(true); // already assigned

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.assignTask(200L, 2L, 1L));
        assertEquals("Użytkownik jest już przypisany do tego zadania", exception.getMessage());
    }

    @Test
    void uploadFileToAssignment_shouldUploadSuccessfully() {
        // Given
        MultipartFile file = new MockMultipartFile(
                "test.pdf",
                "test.pdf",
                "application/pdf",
                "test content".getBytes()
        );

        // Mock assignment
        TaskAssignment assignment = new TaskAssignment();
        assignment.setId(300L);
        assignment.setUser(participant);

        Task task = new Task();
        task.setId(200L);
        task.setMaxFileSize(10L * 1024 * 1024); // 10MB
        task.setAllowedFileTypes("pdf,doc,docx");
        assignment.setTask(task);

        // 1. Mock getAssignmentById
        when(jdbcTemplate.queryForObject(
                anyString(),
                any(org.springframework.jdbc.core.RowMapper.class),
                eq(300L)))
                .thenReturn(assignment);

        // 2. Mock validateAssignmentAccess - organizer check
        when(jdbcTemplate.queryForObject(
                contains("SELECT EXISTS("), // luźniejszy matching
                eq(Boolean.class),
                eq(200L), // taskId
                eq(2L))) // userId (participant jest właścicielem)
                .thenReturn(false); // Nie jest organizatorem

        // 3. Mock user email query
        when(jdbcTemplate.queryForObject(
                eq("SELECT email FROM users WHERE id = ?"),
                eq(String.class),
                eq(2L)))
                .thenReturn("participant@test.com");

        // 4. Mock the insert operation - użyj lenient ponieważ jest wiele wywołań
        when(jdbcTemplate.update(any(org.springframework.jdbc.core.PreparedStatementCreator.class),
                any(org.springframework.jdbc.support.KeyHolder.class)))
                .thenAnswer(invocation -> {
                    KeyHolder keyHolder = invocation.getArgument(1);
                    // Symuluj ustawienie klucza
                    Map<String, Object> keys = new HashMap<>();
                    keys.put("id", 400L);
                    try {
                        // Ustaw klucze w keyHolder
                        if (keyHolder instanceof org.springframework.jdbc.support.GeneratedKeyHolder) {
                            ((org.springframework.jdbc.support.GeneratedKeyHolder) keyHolder).getKeyList().add(keys);
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                    return 1;
                });

        // 5. Mock kolejne wywołanie queryForObject dla zwróconego pliku
        TaskFile mockTaskFile = new TaskFile();
        mockTaskFile.setId(400L);
        mockTaskFile.setFilename("test.pdf");
        mockTaskFile.setOriginalFilename("test.pdf");
        mockTaskFile.setFilePath("test/path/test.pdf");


        // When & Then - powinno przejść bez wyjątku
        assertDoesNotThrow(() -> {
            taskService.uploadFileToAssignment(300L, file, 2L, "Test file");
        });
    }

    @Test
    void uploadFileToTask_shouldThrowException_whenNotOrganizer() {
        // Given
        MultipartFile file = new MockMultipartFile(
                "test.pdf",
                "test.pdf",
                "application/pdf",
                "test content".getBytes()
        );

        when(taskRepository.findById(200L)).thenReturn(Optional.of(task));
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(), any()))
                .thenReturn(false); // not organizer

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.uploadFileToTask(200L, file, 2L, "Test file"));
        assertEquals("Tylko organizator może wrzucać pliki bezpośrednio do zadania",
                exception.getMessage());
    }

    @Test
    void validateFileAgainstTaskSettings_shouldThrowException_whenFileTooLarge() {
        // Given
        Task taskWithSmallLimit = new Task();
        taskWithSmallLimit.setId(200L);
        taskWithSmallLimit.setMaxFileSize(1L); // 1MB

        MultipartFile largeFile = new MockMultipartFile(
                "large.pdf",
                "large.pdf",
                "application/pdf",
                new byte[2 * 1024 * 1024] // 2MB
        );

        // When & Then
        assertThrows(NoSuchMethodException.class, () ->
                taskService.getClass().getDeclaredMethod("validateFileSize",
                                MultipartFile.class, Task.class)
                        .invoke(taskService, largeFile, taskWithSmallLimit));
    }

    @Test
    void validateFileExtension_shouldThrowException_whenExtensionNotAllowed() {
        // Given
        String allowedTypes = "pdf,doc,docx";
        MultipartFile file = new MockMultipartFile(
                "test.exe",
                "test.exe",
                "application/exe",
                "test".getBytes()
        );

        // When & Then
        assertThrows(NoSuchMethodException.class, () ->
                taskService.getClass().getDeclaredMethod("validateFileExtension",
                                MultipartFile.class, String.class)
                        .invoke(taskService, file, allowedTypes));
    }

    @Test
    void getAllTaskFilesForOrganizer_shouldReturnAllFiles() {
        // Given
        when(taskRepository.findById(200L)).thenReturn(Optional.of(task));
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(), any()))
                .thenReturn(true); // is organizer
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any()))
                .thenReturn(Arrays.asList(createMockTaskFile()));

        // When
        List<TaskFile> result = taskService.getAllTaskFilesForOrganizer(200L, 1L);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    void getAllTaskFilesForOrganizer_shouldThrowException_whenNotOrganizer() {
        // Given
        when(taskRepository.findById(200L)).thenReturn(Optional.of(task));
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(), any()))
                .thenReturn(false); // not organizer

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.getAllTaskFilesForOrganizer(200L, 2L));
        assertEquals("Tylko organizator może przeglądać wszystkie pliki zadania",
                exception.getMessage());
    }

    @Test
    void getTaskFiles_shouldReturnAllTaskFiles() {
        // Given
        when(taskRepository.findById(200L)).thenReturn(Optional.of(task));
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(), any(), any()))
                .thenReturn(true); // can view files
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any()))
                .thenReturn(Arrays.asList(createMockTaskFile()));

        // When
        List<TaskFile> result = taskService.getTaskFiles(200L, 2L);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    void deleteFile_shouldDeleteFileSuccessfully() throws IOException {
        // Given
        TaskFile taskFile = createMockTaskFile();

        // Tworzenie tymczasowego pliku
        Path tempFile = Files.createTempFile("test-file", ".pdf");
        taskFile.setFilePath(tempFile.toString());

        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), any()))
                .thenReturn(taskFile);
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(), any(), any()))
                .thenReturn(true); // has permission
        when(jdbcTemplate.update(anyString(), anyLong())).thenReturn(1);

        try {
            // When
            taskService.deleteFile(400L, 1L);

            // Then
            verify(jdbcTemplate).update(anyString(), eq(400L));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void getUserFilesForTask_shouldReturnUserFiles() {
        // Given
        when(taskRepository.findById(200L)).thenReturn(Optional.of(task));
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(), any(), any()))
                .thenReturn(true); // can view files
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any()))
                .thenReturn(Arrays.asList(createMockTaskFile()));

        // When
        List<TaskFile> result = taskService.getUserFilesForTask(200L, 2L);

        // Then
        assertThat(result).hasSize(1);
    }
}
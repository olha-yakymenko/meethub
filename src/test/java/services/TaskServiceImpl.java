package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.*;
import com.meethub.domain.model.enums.AssignmentStatus;
import com.meethub.domain.model.enums.TaskStatus;
import com.meethub.domain.model.request.CreateTaskRequest;
import com.meethub.domain.model.request.UpdateTaskRequest;
import com.meethub.domain.repository.jpa.*;
import com.meethub.domain.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock private TaskRepository taskRepository;
    @Mock private TaskAssignmentRepository assignmentRepository;
    @Mock private TaskFileRepository fileRepository;
    @Mock private UserRepository userRepository;
    @Mock private MeetingRepository meetingRepository;
    @Mock private MeetingParticipantRepository participantRepository;

    @InjectMocks
    private TaskServiceImpl taskService;

    private User organizer;
    private User participant;
    private Meeting meeting;
    private Task task;
    private TaskAssignment assignment;

    @BeforeEach
    void setUp() {
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
        meeting.setStartDate(LocalDateTime.now().plusDays(1));

        // Create task
        task = Task.builder()
                .id(200L)
                .title("Test Task")
                .description("Test Description")
                .status(TaskStatus.TODO)
                .meeting(meeting)
                .createdBy(organizer)
                .createdAt(LocalDateTime.now())
                .build();

        // Create assignment
        assignment = TaskAssignment.builder()
                .id(300L)
                .task(task)
                .user(participant)
                .status(AssignmentStatus.ASSIGNED)
                .assignedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createTask_shouldCreateTaskSuccessfully() {
        // Given
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("New Task");
        request.setDescription("Task Description");
        request.setDeadline(LocalDateTime.now().plusDays(7));

        when(meetingRepository.findById(100L)).thenReturn(Optional.of(meeting));
        when(userRepository.findById(1L)).thenReturn(Optional.of(organizer));

        Task savedTask = Task.builder()
                .id(201L)
                .title("New Task")
                .description("Task Description")
                .status(TaskStatus.TODO)
                .meeting(meeting)
                .createdBy(organizer)
                .createdAt(LocalDateTime.now())
                .build();

        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        // When
        Task result = taskService.createTask(request, 100L, 1L);

        // Then
        assertAll(
                () -> assertNotNull(result, "Task should not be null"),
                () -> assertEquals("New Task", result.getTitle(), "Title should match"),
                () -> assertEquals("Task Description", result.getDescription(), "Description should match"),
                () -> assertEquals(TaskStatus.TODO, result.getStatus(), "Status should be TODO"),
                () -> assertEquals(meeting, result.getMeeting(), "Meeting should match"),
                () -> assertEquals(organizer, result.getCreatedBy(), "Creator should match")
        );

        verify(meetingRepository).findById(100L);
        verify(userRepository).findById(1L);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void createTask_shouldThrowException_whenMeetingNotFound() {
        // Given
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("New Task");

        when(meetingRepository.findById(100L)).thenReturn(Optional.empty());

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

        Meeting otherMeeting = new Meeting();
        otherMeeting.setId(100L);
        otherMeeting.setOrganizer(participant); // Different organizer

        when(meetingRepository.findById(100L)).thenReturn(Optional.of(otherMeeting));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.createTask(request, 100L, 1L)); // User 1 is not organizer

        assertEquals("Tylko organizator może wykonać tę akcję", exception.getMessage());
    }

    @Test
    void getMeetingTasks_shouldReturnTasks() {
        // Given
        List<Task> tasks = Arrays.asList(task);
        when(taskRepository.findByMeetingId(100L)).thenReturn(tasks);

        // When
        List<Task> result = taskService.getMeetingTasks(100L);

        // Then
        assertAll(
                () -> assertNotNull(result, "Result should not be null"),
                () -> assertEquals(1, result.size(), "Should return 1 task"),
                () -> assertEquals(task.getId(), result.get(0).getId(), "Task ID should match")
        );
    }

    @Test
    void getTaskById_shouldReturnTask() {
        // Given
        when(taskRepository.findById(200L)).thenReturn(Optional.of(task));

        // When
        Task result = taskService.getTaskById(200L);

        // Then
        assertEquals(task.getId(), result.getId(), "Should return correct task");
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



    @Test
    void assignTask_shouldAssignTaskToUser() {
        // Given
        when(taskRepository.findById(200L)).thenReturn(Optional.of(task));
        when(userRepository.findById(2L)).thenReturn(Optional.of(participant));
        when(participantRepository.existsByMeetingIdAndUserId(100L, 2L)).thenReturn(true);
        when(assignmentRepository.findByTaskIdAndUserId(200L, 2L)).thenReturn(Optional.empty());

        TaskAssignment newAssignment = TaskAssignment.builder()
                .id(301L)
                .task(task)
                .user(participant)
                .status(AssignmentStatus.ASSIGNED)
                .assignedAt(LocalDateTime.now())
                .build();

        when(assignmentRepository.save(any(TaskAssignment.class))).thenReturn(newAssignment);

        // When
        TaskAssignment result = taskService.assignTask(200L, 2L, 1L);

        // Then - single assertion with detailed message
        assertThat(result)
                .isNotNull()
                .extracting(TaskAssignment::getTask, TaskAssignment::getUser)
                .containsExactly(task, participant);
    }



    @Test
    void assignTaskToCurrentUser_shouldAllowSelfAssignment() {
        // Given
        when(taskRepository.findById(200L)).thenReturn(Optional.of(task));
        when(participantRepository.existsByMeetingIdAndUserId(100L, 2L)).thenReturn(true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(participant));
        when(assignmentRepository.findByTaskIdAndUserId(200L, 2L)).thenReturn(Optional.empty());

        TaskAssignment newAssignment = TaskAssignment.builder()
                .id(301L)
                .task(task)
                .user(participant)
                .status(AssignmentStatus.ASSIGNED)
                .assignedAt(LocalDateTime.now())
                .build();

        when(assignmentRepository.save(any(TaskAssignment.class))).thenReturn(newAssignment);

        // When
        TaskAssignment result = taskService.assignTaskToCurrentUser(200L, 2L);

        // Then
        assertAll(
                () -> assertNotNull(result, "Assignment should not be null"),
                () -> assertEquals(task, result.getTask(), "Task should match"),
                () -> assertEquals(participant, result.getUser(), "User should match"),
                () -> assertEquals(AssignmentStatus.ASSIGNED, result.getStatus(), "Status should be ASSIGNED")
        );
    }


    @Test
    void updateAssignmentStatus_shouldThrowException_whenUserNotAuthorized() {
        // Given
        when(assignmentRepository.findById(300L)).thenReturn(Optional.of(assignment));

        // When & Then - User 999 is not owner or organizer
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.updateAssignmentStatus(300L, AssignmentStatus.COMPLETED, 999L));

        assertEquals("Brak uprawnień do tego przypisania", exception.getMessage());
    }

    @Test
    void removeAssignment_shouldThrowException_whenNotAuthorized() {
        // Given
        User unauthorizedUser = new User();
        unauthorizedUser.setId(999L);

        when(assignmentRepository.findById(300L)).thenReturn(Optional.of(assignment));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.removeAssignment(300L, 999L));

        assertEquals("Brak uprawnień do usunięcia przypisania", exception.getMessage());
    }

    @Test
    void getTaskAssignments_shouldReturnAssignments() {
        // Given
        List<TaskAssignment> assignments = Arrays.asList(assignment);
        when(assignmentRepository.findByTaskId(200L)).thenReturn(assignments);

        // When
        List<TaskAssignment> result = taskService.getTaskAssignments(200L);

        // Then
        assertThat(result)
                .hasSize(1)
                .first()
                .extracting(TaskAssignment::getId)
                .isEqualTo(300L);
    }

    @Test
    void getAssignmentById_shouldReturnAssignment() {
        // Given
        when(assignmentRepository.findById(300L)).thenReturn(Optional.of(assignment));

        // When
        TaskAssignment result = taskService.getAssignmentById(300L);

        // Then
        assertEquals(300L, result.getId(), "Should return correct assignment");
    }

    @Test
    void getAssignmentsByStatus_shouldFilterByStatus() {
        // Given
        TaskAssignment completedAssignment = TaskAssignment.builder()
                .id(301L)
                .task(task)
                .user(participant)
                .status(AssignmentStatus.COMPLETED)
                .build();

        List<TaskAssignment> allAssignments = Arrays.asList(assignment, completedAssignment);
        when(assignmentRepository.findByTaskId(200L)).thenReturn(allAssignments);

        // When
        List<TaskAssignment> result = taskService.getAssignmentsByStatus(200L, AssignmentStatus.COMPLETED);

        // Then
        assertAll(
                () -> assertEquals(1, result.size(), "Should return only completed assignments"),
                () -> assertEquals(AssignmentStatus.COMPLETED, result.get(0).getStatus(), "Should be COMPLETED"),
                () -> assertEquals(301L, result.get(0).getId(), "Should be correct assignment")
        );
    }

    @Test
    void countCompletedAssignments_shouldReturnCorrectCount() {
        // Given
        TaskAssignment completed1 = TaskAssignment.builder()
                .id(301L).task(task).status(AssignmentStatus.COMPLETED).build();
        TaskAssignment completed2 = TaskAssignment.builder()
                .id(302L).task(task).status(AssignmentStatus.COMPLETED).build();

        List<TaskAssignment> allAssignments = Arrays.asList(assignment, completed1, completed2);
        when(assignmentRepository.findByTaskId(200L)).thenReturn(allAssignments);

        // When
        Long count = taskService.countCompletedAssignments(200L);

        // Then
        assertEquals(2L, count, "Should count 2 completed assignments");
    }

    @Test
    void canUserManageTask_shouldReturnTrueForOrganizer() {
        // Given
        when(taskRepository.findById(200L)).thenReturn(Optional.of(task));

        // When
        boolean result = taskService.canUserManageTask(200L, 1L);

        // Then
        assertTrue(result, "Organizer should be able to manage task");
    }

    @Test
    void canUserManageTask_shouldReturnFalseForNonOrganizer() {
        // Given
        when(taskRepository.findById(200L)).thenReturn(Optional.of(task));

        // When
        boolean result = taskService.canUserManageTask(200L, 2L);

        // Then
        assertFalse(result, "Non-organizer should not be able to manage task");
    }

    @Test
    void canUserManageTask_shouldReturnFalseWhenTaskNotFound() {
        // Given
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        boolean result = taskService.canUserManageTask(999L, 1L);

        // Then
        assertFalse(result, "Should return false when task not found");
    }

    @Test
    void canUserAccessAssignment_shouldReturnTrueForOwner() {
        // Given
        when(assignmentRepository.findById(300L)).thenReturn(Optional.of(assignment));

        // When
        boolean result = taskService.canUserAccessAssignment(300L, 2L);

        // Then
        assertTrue(result, "Owner should be able to access assignment");
    }

    @Test
    void canUserAccessAssignment_shouldReturnTrueForOrganizer() {
        // Given
        when(assignmentRepository.findById(300L)).thenReturn(Optional.of(assignment));

        // When
        boolean result = taskService.canUserAccessAssignment(300L, 1L);

        // Then
        assertTrue(result, "Organizer should be able to access assignment");
    }

    @Test
    void canUserAccessAssignment_shouldReturnFalseForUnauthorizedUser() {
        // Given
        when(assignmentRepository.findById(300L)).thenReturn(Optional.of(assignment));

        // When
        boolean result = taskService.canUserAccessAssignment(300L, 999L);

        // Then
        assertFalse(result, "Unauthorized user should not be able to access assignment");
    }



    @Test
    void getUserAssignments_shouldReturnUserAssignments() {
        // Given
        List<TaskAssignment> assignments = Arrays.asList(assignment);
        when(assignmentRepository.findByUserId(2L)).thenReturn(assignments);

        // When
        List<TaskAssignment> result = taskService.getUserAssignments(2L);

        // Then
        assertThat(result)
                .hasSize(1)
                .first()
                .extracting(TaskAssignment::getId)
                .isEqualTo(300L);
    }

    @Test
    void getUserCreatedTasks_shouldReturnTasksCreatedByUser() {
        // Given
        List<Task> tasks = Arrays.asList(task);
        when(taskRepository.findByCreatedById(1L)).thenReturn(tasks);

        // When
        List<Task> result = taskService.getUserCreatedTasks(1L);

        // Then
        assertThat(result)
                .hasSize(1)
                .first()
                .extracting(Task::getId)
                .isEqualTo(200L);
    }

    @Test
    void countTotalAssignments_shouldReturnCorrectCount() {
        // Given
        List<TaskAssignment> assignments = Arrays.asList(assignment, assignment, assignment);
        when(assignmentRepository.findByTaskId(200L)).thenReturn(assignments);

        // When
        Long count = taskService.countTotalAssignments(200L);

        // Then
        assertEquals(3L, count, "Should count total assignments");
    }

    // Note: Tests for file operations are simplified since they involve file system operations
    // In a real project, you might want to mock the file system or use a test configuration

    @Test
    void uploadFile_shouldThrowException_whenFileEmpty() {
        // Given
        MultipartFile emptyFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                new byte[0]
        );

        when(assignmentRepository.findById(300L)).thenReturn(Optional.of(assignment));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.uploadFile(300L, emptyFile, 2L));

        assertEquals("Plik jest pusty", exception.getMessage());
    }

    @Test
    void getAssignmentFiles_shouldReturnFiles() {
        // Given
        TaskFile taskFile = TaskFile.builder()
                .id(400L)
                .originalFilename("test.pdf")
                .assignment(assignment)
                .build();

        List<TaskFile> files = Arrays.asList(taskFile);
        when(assignmentRepository.findById(300L)).thenReturn(Optional.of(assignment));
        when(fileRepository.findByAssignmentId(300L)).thenReturn(files);

        // When
        List<TaskFile> result = taskService.getAssignmentFiles(300L, 2L);

        // Then
        assertThat(result)
                .hasSize(1)
                .first()
                .extracting(TaskFile::getId)
                .isEqualTo(400L);
    }

    @Test
    void deleteFile_shouldThrowException_whenFileNotFound() {
        // Given
        when(fileRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> taskService.deleteFile(999L, 2L));

        assertEquals("Plik nie został znaleziony", exception.getMessage());
    }
}

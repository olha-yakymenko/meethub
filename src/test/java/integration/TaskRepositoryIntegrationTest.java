//package com.meethub.domain.repository.jpa;
//
//import com.meethub.domain.model.entity.Task;
//import com.meethub.domain.model.enums.TaskStatus;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
//import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.jdbc.Sql;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//
//
//@JdbcTest
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
//@ActiveProfiles("postgres")
//
//class TaskRepositoryIntegrationTest {
//
//    @Autowired
//    private JdbcTemplate jdbcTemplate;
//
//    private TaskRepository taskRepository;
//    private LocalDateTime now;
//
//    @BeforeEach
//    void setUp() {
//        taskRepository = new TaskRepository(jdbcTemplate);
//        now = LocalDateTime.now();
//    }
//
////    @Test
////    void save_ShouldInsertAndRetrieveTask() {
////        // Given
////        Task newTask = Task.builder()
////                .title("New Test Task")
////                .description("New Test Description")
////                .status(TaskStatus.TODO)
////                .deadline(now.plusDays(10))
////                .createdAt(now)
////                .updatedAt(now)
////                .allowedFileTypes("pdf")
////                .maxFileSize(10485760L)
////                .maxFilesPerUser(3)
////                .allowSelfAssignment(true)
////                .build();
////        newTask.setMeetingId(1L); // Test Meeting ID
////        newTask.setCreatedById(2L); // test.organizer@example.com ID
////
////        // When
////        Task savedTask = taskRepository.save(newTask);
////        Optional<Task> retrievedTask = taskRepository.findById(savedTask.getId());
////
////        // Then
////        assertThat(retrievedTask).isPresent();
////        assertThat(retrievedTask.get().getTitle()).isEqualTo("New Test Task");
////        assertThat(retrievedTask.get().getDescription()).isEqualTo("New Test Description");
////        assertThat(retrievedTask.get().getStatus()).isEqualTo(TaskStatus.TODO);
////        assertThat(retrievedTask.get().getAllowedFileTypes()).isEqualTo("pdf");
////        assertThat(retrievedTask.get().getMaxFileSize()).isEqualTo(10485760L);
////        assertThat(retrievedTask.get().getMaxFilesPerUser()).isEqualTo(3);
////        assertThat(retrievedTask.get().getAllowSelfAssignment()).isTrue();
////    }
//
//    @Test
//    void findByMeetingId_ShouldReturnTasksForMeeting() {
//        // When
//        List<Task> tasks = taskRepository.findByMeetingId(1L); // Test Meeting ID
//
//        // Then
//        assertThat(tasks).hasSize(3); // Prepare slides, Send invitations, Setup room
//        assertThat(tasks).extracting(Task::getMeetingId).containsOnly(1L);
//        assertThat(tasks).extracting(Task::getTitle).containsExactlyInAnyOrder(
//                "Prepare slides", "Send invitations", "Setup room"
//        );
//    }
//
//    @Test
//    void findByCreatedById_ShouldReturnTasksCreatedByUser() {
//        // When - sprawdź zadania utworzone przez organizatora (ID = 2)
//        List<Task> tasks = taskRepository.findByCreatedById(2L);
//
//        // Then
//        assertThat(tasks).hasSize(2); // Prepare slides, Send invitations
//        assertThat(tasks).extracting(Task::getCreatedById).containsOnly(2L);
//        assertThat(tasks).extracting(Task::getTitle).containsExactlyInAnyOrder(
//                "Prepare slides", "Send invitations"
//        );
//    }
//
//    @Test
//    void findById_ShouldReturnTask_WhenExists() {
//        // When
//        Optional<Task> task = taskRepository.findById(1L);
//
//        // Then
//        assertThat(task).isPresent();
//        assertThat(task.get().getTitle()).isEqualTo("Prepare slides");
//        assertThat(task.get().getStatus()).isEqualTo(TaskStatus.TODO);
//    }
//
//    @Test
//    void findById_ShouldReturnEmpty_WhenNotExists() {
//        // When
//        Optional<Task> task = taskRepository.findById(999L);
//
//        // Then
//        assertThat(task).isEmpty();
//    }
//
//    @Test
//    void update_ShouldModifyTask() {
//        // Given
//        Optional<Task> existingTask = taskRepository.findById(1L);
//        assertThat(existingTask).isPresent();
//
//        Task taskToUpdate = existingTask.get();
//        taskToUpdate.setTitle("Updated Presentation Slides");
//        taskToUpdate.setDescription("Updated Description");
//        taskToUpdate.setStatus(TaskStatus.IN_PROGRESS);
//        taskToUpdate.setDeadline(now.plusDays(5));
//        taskToUpdate.setAllowedFileTypes("jpg,png");
//        taskToUpdate.setMaxFileSize(2097152L);
//        taskToUpdate.setMaxFilesPerUser(2);
//        taskToUpdate.setAllowSelfAssignment(false);
//        taskToUpdate.setUpdatedAt(now.plusHours(1));
//
//        // When
//        Task updatedTask = taskRepository.save(taskToUpdate);
//        Optional<Task> retrievedTask = taskRepository.findById(1L);
//
//        // Then
//        assertThat(retrievedTask).isPresent();
//        assertThat(retrievedTask.get().getTitle()).isEqualTo("Updated Presentation Slides");
//        assertThat(retrievedTask.get().getDescription()).isEqualTo("Updated Description");
//        assertThat(retrievedTask.get().getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
//        assertThat(retrievedTask.get().getAllowedFileTypes()).isEqualTo("jpg,png");
//        assertThat(retrievedTask.get().getMaxFileSize()).isEqualTo(2097152L);
//        assertThat(retrievedTask.get().getMaxFilesPerUser()).isEqualTo(2);
//        assertThat(retrievedTask.get().getAllowSelfAssignment()).isFalse();
//    }
//
//    @Test
//    void deleteById_ShouldRemoveTask() {
//        // Given
//        assertThat(taskRepository.existsById(1L)).isTrue();
//
//        // When
//        taskRepository.deleteById(1L);
//
//        // Then
//        assertThat(taskRepository.existsById(1L)).isFalse();
//        assertThat(taskRepository.findById(1L)).isEmpty();
//    }
//
//    @Test
//    void deleteById_ShouldThrowException_WhenTaskNotFound() {
//        // When & Then
//        RuntimeException exception = assertThrows(RuntimeException.class,
//                () -> taskRepository.deleteById(999L));
//        assertThat(exception.getMessage()).contains("Zadanie nie zostało znalezione: 999");
//    }
//
//    @Test
//    void existsById_ShouldReturnCorrectBoolean() {
//        // When & Then
//        assertThat(taskRepository.existsById(1L)).isTrue();
//        assertThat(taskRepository.existsById(2L)).isTrue();
//        assertThat(taskRepository.existsById(3L)).isTrue();
//        assertThat(taskRepository.existsById(999L)).isFalse();
//    }
//
//    @Test
//    void findByMeetingId_ShouldReturnEmptyList_WhenNoTasks() {
//        // When
//        List<Task> tasks = taskRepository.findByMeetingId(999L);
//
//        // Then
//        assertThat(tasks).isEmpty();
//    }
//
//    @Test
//    void findByCreatedById_ShouldReturnEmptyList_WhenNoTasks() {
//        // When
//        List<Task> tasks = taskRepository.findByCreatedById(999L);
//
//        // Then
//        assertThat(tasks).isEmpty();
//    }
//
////    @Test
////    void save_ShouldHandleNullValues() {
////        // Given
////        Task taskWithNulls = Task.builder()
////                .title("Task with Nulls")
////                .description(null)
////                .status(TaskStatus.TODO)
////                .deadline(null)
////                .createdAt(now)
////                .updatedAt(now)
////                .allowedFileTypes(null)
////                .maxFileSize(null)
////                .maxFilesPerUser(null)
////                .allowSelfAssignment(null)
////                .build();
////        taskWithNulls.setMeetingId(1L);
////        taskWithNulls.setCreatedById(2L);
////
////        // When
////        Task savedTask = taskRepository.save(taskWithNulls);
////        Optional<Task> retrievedTask = taskRepository.findById(savedTask.getId());
////
////        // Then
////        assertThat(retrievedTask).isPresent();
////        assertThat(retrievedTask.get().getDescription()).isNull();
////        assertThat(retrievedTask.get().getDeadline()).isNull();
////        assertThat(retrievedTask.get().getAllowedFileTypes()).isNull();
////        assertThat(retrievedTask.get().getMaxFileSize()).isNull();
////        assertThat(retrievedTask.get().getMaxFilesPerUser()).isNull();
////        assertThat(retrievedTask.get().getAllowSelfAssignment()).isNull();
////    }
//
//    @Test
//    void findByMeetingId_ShouldReturnTasksInCorrectOrder() {
//        // When
//        List<Task> tasks = taskRepository.findByMeetingId(1L);
//
//        // Then - should be ordered by created_at DESC
//        assertThat(tasks).isNotEmpty();
//        for (int i = 0; i < tasks.size() - 1; i++) {
//            assertThat(tasks.get(i).getCreatedAt())
//                    .isAfterOrEqualTo(tasks.get(i + 1).getCreatedAt());
//        }
//    }
//
//    @Test
//    void findByCreatedById_ShouldReturnTasksInCorrectOrder() {
//        // When
//        List<Task> tasks = taskRepository.findByCreatedById(2L); // Organizer
//
//        // Then - should be ordered by deadline ASC
//        assertThat(tasks).hasSize(2);
//        for (int i = 0; i < tasks.size() - 1; i++) {
//            LocalDateTime deadline1 = tasks.get(i).getDeadline();
//            LocalDateTime deadline2 = tasks.get(i + 1).getDeadline();
//            if (deadline1 != null && deadline2 != null) {
//                assertThat(deadline1).isBeforeOrEqualTo(deadline2);
//            }
//        }
//    }
//}
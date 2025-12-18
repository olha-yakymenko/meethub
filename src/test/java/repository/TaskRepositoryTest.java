package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.Task;
import com.meethub.domain.model.enums.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private TaskRepository taskRepository;

    private Task testTask;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        testTask = Task.builder()
                .id(1L)
                .title("Test Task")
                .description("Test Description")
                .status(TaskStatus.TODO)
                .deadline(now.plusDays(7))
                .createdAt(now)
                .updatedAt(now)
                .allowedFileTypes("pdf,docx")
                .maxFileSize(5242880L)
                .maxFilesPerUser(5)
                .allowSelfAssignment(true)
                .build();
        testTask.setMeetingId(100L);
        testTask.setCreatedById(200L);
    }

    @Test
    void findByMeetingId_ShouldReturnTasks() {
        Long meetingId = 100L;
        List<Task> expectedTasks = Collections.singletonList(testTask);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(meetingId))).thenReturn(expectedTasks);

        List<Task> result = taskRepository.findByMeetingId(meetingId);

        assertAll("Find by meeting id",
                () -> assertThat(result).isEqualTo(expectedTasks),
                () -> verify(jdbcTemplate).query(
                        eq("SELECT * FROM tasks WHERE meeting_id = ? ORDER BY created_at DESC"),
                        any(RowMapper.class),
                        eq(meetingId)
                )
        );
    }

    @Test
    void findByCreatedById_ShouldReturnTasks() {
        Long userId = 200L;
        List<Task> expectedTasks = Collections.singletonList(testTask);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(userId))).thenReturn(expectedTasks);

        List<Task> result = taskRepository.findByCreatedById(userId);

        assertAll("Find by createdBy id",
                () -> assertThat(result).isEqualTo(expectedTasks),
                () -> verify(jdbcTemplate).query(
                        eq("SELECT * FROM tasks WHERE created_by = ? ORDER BY deadline ASC"),
                        any(RowMapper.class),
                        eq(userId)
                )
        );
    }

    @Test
    void findById_ShouldReturnTask_WhenExists() {
        Long taskId = 1L;
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq(taskId))).thenReturn(testTask);

        Optional<Task> result = taskRepository.findById(taskId);

        assertAll("Find by id - exists",
                () -> assertThat(result).isPresent(),
                () -> assertThat(result.get()).isEqualTo(testTask),
                () -> verify(jdbcTemplate).queryForObject(
                        eq("SELECT * FROM tasks WHERE id = ?"),
                        any(RowMapper.class),
                        eq(taskId)
                )
        );
    }

    @Test
    void findById_ShouldReturnEmpty_WhenNotExists() {
        Long taskId = 999L;
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq(taskId)))
                .thenThrow(new org.springframework.dao.EmptyResultDataAccessException(1));

        Optional<Task> result = taskRepository.findById(taskId);

        assertAll("Find by id - not exists",
                () -> assertThat(result).isEmpty()
        );
    }

    @Test
    void save_ShouldUpdate_WhenExistingTask() {
        Task existingTask = testTask;
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> paramsCaptor = ArgumentCaptor.forClass(Object[].class);

        when(jdbcTemplate.update(sqlCaptor.capture(), paramsCaptor.capture())).thenReturn(1);

        existingTask.setTitle("Updated Task");
        existingTask.setDescription("Updated Description");
        existingTask.setStatus(TaskStatus.IN_PROGRESS);
        existingTask.setDeadline(now.plusDays(3));
        existingTask.setAllowedFileTypes("pdf");
        existingTask.setMaxFileSize(2097152L);
        existingTask.setMaxFilesPerUser(2);
        existingTask.setAllowSelfAssignment(false);
        existingTask.setUpdatedAt(now.plusHours(1));

        Task updatedTask = taskRepository.save(existingTask);
        String capturedSql = sqlCaptor.getValue();
        Object[] capturedParams = paramsCaptor.getValue();

        assertAll("Save update task",
                () -> assertThat(capturedSql).contains("UPDATE tasks SET"),
                () -> assertThat(capturedSql).contains("title = ?", "description = ?", "status = ?", "deadline = ?",
                        "updated_at = ?", "allowed_file_types = ?", "max_file_size = ?", "max_files_per_user = ?", "allow_self_assignment = ?", "WHERE id = ?"),
                () -> assertThat(capturedParams).hasSize(10),
                () -> assertThat(capturedParams[0]).isEqualTo("Updated Task"),
                () -> assertThat(capturedParams[1]).isEqualTo("Updated Description"),
                () -> assertThat(capturedParams[2]).isEqualTo("IN_PROGRESS"),
                () -> assertThat(updatedTask).isEqualTo(existingTask)
        );
    }

    @Test
    void save_ShouldThrowException_WhenUpdateFails() {
        Task existingTask = testTask;
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(0);

        assertAll("Save - update fails",
                () -> assertThatThrownBy(() -> taskRepository.save(existingTask))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("Zadanie nie zostało znalezione: 1")
        );
    }

    @Test
    void deleteById_ShouldDeleteTask() {
        Long taskId = 1L;
        when(jdbcTemplate.update(eq("DELETE FROM tasks WHERE id = ?"), eq(taskId))).thenReturn(1);

        taskRepository.deleteById(taskId);

        assertAll("Delete task",
                () -> verify(jdbcTemplate).update(eq("DELETE FROM tasks WHERE id = ?"), eq(taskId))
        );
    }

    @Test
    void deleteById_ShouldThrowException_WhenTaskNotFound() {
        Long taskId = 999L;
        when(jdbcTemplate.update(eq("DELETE FROM tasks WHERE id = ?"), eq(taskId))).thenReturn(0);

        assertAll("Delete task - not found",
                () -> assertThatThrownBy(() -> taskRepository.deleteById(taskId))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("Zadanie nie zostało znalezione: 999")
        );
    }

    @Test
    void existsById_ShouldReturnTrue_WhenTaskExists() {
        Long taskId = 1L;
        when(jdbcTemplate.queryForObject(eq("SELECT EXISTS(SELECT 1 FROM tasks WHERE id = ?)"), eq(Boolean.class), eq(taskId)))
                .thenReturn(true);

        boolean exists = taskRepository.existsById(taskId);

        assertAll("Exists - true",
                () -> assertThat(exists).isTrue()
        );
    }

    @Test
    void existsById_ShouldReturnFalse_WhenTaskNotExists() {
        Long taskId = 999L;
        when(jdbcTemplate.queryForObject(eq("SELECT EXISTS(SELECT 1 FROM tasks WHERE id = ?)"), eq(Boolean.class), eq(taskId)))
                .thenReturn(false);

        boolean exists = taskRepository.existsById(taskId);

        assertAll("Exists - false",
                () -> assertThat(exists).isFalse()
        );
    }

    @Test
    void taskRowMapper_ShouldMapResultSetCorrectly() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(1L);
        when(rs.getString("title")).thenReturn("Test Task");
        when(rs.getString("description")).thenReturn("Test Description");
        when(rs.getString("status")).thenReturn("TODO");
        when(rs.getTimestamp("deadline")).thenReturn(Timestamp.valueOf(now.plusDays(7)));
        when(rs.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(now));
        when(rs.getTimestamp("updated_at")).thenReturn(Timestamp.valueOf(now));
        when(rs.getString("allowed_file_types")).thenReturn("pdf,docx");
        when(rs.getLong("max_file_size")).thenReturn(5242880L);
        when(rs.getInt("max_files_per_user")).thenReturn(5);
        when(rs.getBoolean("allow_self_assignment")).thenReturn(true);
        when(rs.getLong("meeting_id")).thenReturn(100L);
        when(rs.getLong("created_by")).thenReturn(200L);

        Task task = taskRepository.getTaskRowMapper().mapRow(rs, 1);

        assertAll("Task RowMapper",
                () -> assertThat(task.getId()).isEqualTo(1L),
                () -> assertThat(task.getTitle()).isEqualTo("Test Task"),
                () -> assertThat(task.getDescription()).isEqualTo("Test Description"),
                () -> assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO),
                () -> assertThat(task.getDeadline()).isEqualTo(now.plusDays(7)),
                () -> assertThat(task.getCreatedAt()).isEqualTo(now),
                () -> assertThat(task.getUpdatedAt()).isEqualTo(now),
                () -> assertThat(task.getAllowedFileTypes()).isEqualTo("pdf,docx"),
                () -> assertThat(task.getMaxFileSize()).isEqualTo(5242880L),
                () -> assertThat(task.getMaxFilesPerUser()).isEqualTo(5),
                () -> assertThat(task.getAllowSelfAssignment()).isTrue(),
                () -> assertThat(task.getMeetingId()).isEqualTo(100L),
                () -> assertThat(task.getCreatedById()).isEqualTo(200L)
        );
    }

    // Helper method to access private RowMapper
    private RowMapper<Task> getTaskRowMapper() {
        try {
            java.lang.reflect.Field field = TaskRepository.class.getDeclaredField("taskRowMapper");
            field.setAccessible(true);
            return (RowMapper<Task>) field.get(taskRepository);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access taskRowMapper", e);
        }
    }
}

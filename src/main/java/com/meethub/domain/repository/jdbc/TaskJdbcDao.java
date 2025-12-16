package com.meethub.domain.repository.jdbc;

import com.meethub.domain.model.entity.Task;
import com.meethub.domain.model.enums.TaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TaskJdbcDao {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Task> taskRowMapper = new RowMapper<>() {
        @Override
        public Task mapRow(ResultSet rs, int rowNum) throws SQLException {
            Task task = new Task();
            task.setId(rs.getLong("id"));
            task.setTitle(rs.getString("title"));
            task.setDescription(rs.getString("description"));
            task.setStatus(TaskStatus.valueOf(rs.getString("status")));
            task.setDeadline(rs.getObject("deadline", LocalDateTime.class));
            task.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
            task.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
            task.setAllowedFileTypes(rs.getString("allowed_file_types"));
            task.setMaxFileSize(rs.getLong("max_file_size"));
            task.setMaxFilesPerUser(rs.getInt("max_files_per_user"));
            task.setAllowSelfAssignment(rs.getBoolean("allow_self_assignment"));

            // Ustawienie relacji (jeśli potrzebujesz pełnych obiektów, wymaga dodatkowych JOIN)
            // task.setMeetingId(rs.getLong("meeting_id"));
            // task.setCreatedById(rs.getLong("created_by_id"));

            return task;
        }
    };

    // SELECT z RowMapper
    public List<Task> findByMeetingId(Long meetingId) {
        String sql = """
            SELECT t.* FROM tasks t
            WHERE t.meeting_id = ?
            ORDER BY t.created_at DESC
            """;

        return jdbcTemplate.query(sql, taskRowMapper, meetingId);
    }

    public List<Task> findByCreatedById(Long userId) {
        String sql = """
            SELECT t.* FROM tasks t
            WHERE t.created_by_id = ?
            ORDER BY t.deadline ASC
            """;

        return jdbcTemplate.query(sql, taskRowMapper, userId);
    }

    public Optional<Task> findById(Long taskId) {
        String sql = "SELECT * FROM tasks WHERE id = ?";

        try {
            Task task = jdbcTemplate.queryForObject(sql, taskRowMapper, taskId);
            return Optional.ofNullable(task);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // INSERT z update()
    public Task save(Task task) {
        if (task.getId() == null) {
            return insert(task);
        } else {
            return update(task);
        }
    }

    private Task insert(Task task) {
        String sql = """
            INSERT INTO tasks (
                title, description, status, deadline, meeting_id,
                created_by_id, created_at, updated_at,
                allowed_file_types, max_file_size, max_files_per_user,
                allow_self_assignment
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        jdbcTemplate.update(sql,
                task.getTitle(),
                task.getDescription(),
                task.getStatus().name(),
                task.getDeadline(),
                task.getMeeting().getId(),
                task.getCreatedBy().getId(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                task.getAllowedFileTypes(),
                task.getMaxFileSize(),
                task.getMaxFilesPerUser(),
                task.getAllowSelfAssignment()
        );

        // Pobierz wygenerowane ID
        Long generatedId = jdbcTemplate.queryForObject(
                "SELECT LAST_INSERT_ID()", Long.class
        );
        task.setId(generatedId);

        return task;
    }

    private Task update(Task task) {
        String sql = """
            UPDATE tasks SET
                title = ?,
                description = ?,
                status = ?,
                deadline = ?,
                updated_at = ?,
                allowed_file_types = ?,
                max_file_size = ?,
                max_files_per_user = ?,
                allow_self_assignment = ?
            WHERE id = ?
            """;

        int updated = jdbcTemplate.update(sql,
                task.getTitle(),
                task.getDescription(),
                task.getStatus().name(),
                task.getDeadline(),
                LocalDateTime.now(),
                task.getAllowedFileTypes(),
                task.getMaxFileSize(),
                task.getMaxFilesPerUser(),
                task.getAllowSelfAssignment(),
                task.getId()
        );

        if (updated == 0) {
            throw new RuntimeException("Task not found with id: " + task.getId());
        }

        return task;
    }

    // DELETE z update()
    public void deleteById(Long taskId) {
        String sql = "DELETE FROM tasks WHERE id = ?";
        int deleted = jdbcTemplate.update(sql, taskId);

        if (deleted == 0) {
            throw new RuntimeException("Task not found with id: " + taskId);
        }
    }

    // Złożone zapytanie z JOIN
    public List<Task> findTasksWithMeetingDetails(Long userId) {
        String sql = """
            SELECT t.*, m.title as meeting_title, m.start_date
            FROM tasks t
            JOIN meetings m ON t.meeting_id = m.id
            WHERE t.created_by_id = ?
            OR EXISTS (
                SELECT 1 FROM task_assignments ta
                WHERE ta.task_id = t.id AND ta.user_id = ?
            )
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Task task = taskRowMapper.mapRow(rs, rowNum);
            // Dodatkowe dane z JOIN
            // task.setMeetingTitle(rs.getString("meeting_title"));
            return task;
        }, userId, userId);
    }

    // COUNT
    public Long countByMeetingId(Long meetingId) {
        String sql = "SELECT COUNT(*) FROM tasks WHERE meeting_id = ?";
        return jdbcTemplate.queryForObject(sql, Long.class, meetingId);
    }

    // EXISTS
    public boolean existsById(Long taskId) {
        String sql = "SELECT EXISTS(SELECT 1 FROM tasks WHERE id = ?)";
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, taskId));
    }
}
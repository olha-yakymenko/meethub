//// TaskRepository.java
//package com.meethub.domain.repository.jpa;
//
//import com.meethub.domain.model.entity.Task;
//import com.meethub.domain.model.enums.TaskStatus;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Optional;
//
//@Repository
//public interface TaskRepository extends JpaRepository<Task, Long> {
//
//    // Podstawowe zapytania
//    List<Task> findByMeetingId(Long meetingId);
//    List<Task> findByCreatedById(Long userId);
//
//}




// TaskRepository.java
package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.Task;
import com.meethub.domain.model.enums.TaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TaskRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Task> taskRowMapper = (rs, rowNum) -> {
        Task task = new Task();
        task.setId(rs.getLong("id"));
        task.setTitle(rs.getString("title"));
        task.setDescription(rs.getString("description"));
        task.setStatus(TaskStatus.valueOf(rs.getString("status")));
        task.setDeadline(rs.getTimestamp("deadline") != null ?
                rs.getTimestamp("deadline").toLocalDateTime() : null);
        task.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        task.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        task.setAllowedFileTypes(rs.getString("allowed_file_types"));
        task.setMaxFileSize(rs.getLong("max_file_size"));
        task.setMaxFilesPerUser(rs.getInt("max_files_per_user"));
        task.setAllowSelfAssignment(rs.getBoolean("allow_self_assignment"));

        // Ustawienie ID relacji - zgodnie z Twoim schematem
        task.setMeetingId(rs.getLong("meeting_id"));
        task.setCreatedById(rs.getLong("created_by")); // UWAGA: created_by, nie created_by_id!

        return task;
    };

    public RowMapper<Task> getTaskRowMapper() {
        return taskRowMapper;
    }

    // Podstawowe zapytania
    public List<Task> findByMeetingId(Long meetingId) {
        String sql = "SELECT * FROM tasks WHERE meeting_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, taskRowMapper, meetingId);
    }

    public List<Task> findByCreatedById(Long userId) {
        String sql = "SELECT * FROM tasks WHERE created_by = ? ORDER BY deadline ASC";
        return jdbcTemplate.query(sql, taskRowMapper, userId);
    }

    public Optional<Task> findById(Long taskId) {
        String sql = "SELECT * FROM tasks WHERE id = ?";
        try {
            Task task = jdbcTemplate.queryForObject(sql, taskRowMapper, taskId);
            return Optional.ofNullable(task);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Task save(Task task) {
        if (task.getId() == null) {
            return insert(task);
        } else {
            return update(task);
        }
    }

    // TaskRepository.java - prostsza wersja bez KeyHolder
    private Task insert(Task task) {
        String sql = """
        INSERT INTO tasks (
            title, description, status, deadline, meeting_id,
            created_by, created_at, updated_at,
            allowed_file_types, max_file_size, max_files_per_user,
            allow_self_assignment
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        Long generatedId = jdbcTemplate.queryForObject(
                sql + " RETURNING id", // Dla PostgreSQL/H2
                Long.class,
                task.getTitle(),
                task.getDescription(),
                task.getStatus().name(),
                task.getDeadline() != null ? Timestamp.valueOf(task.getDeadline()) : null,
                task.getMeetingId(),
                task.getCreatedById(),
                Timestamp.valueOf(task.getCreatedAt()),
                Timestamp.valueOf(task.getUpdatedAt()),
                task.getAllowedFileTypes(),
                task.getMaxFileSize(),
                task.getMaxFilesPerUser(),
                task.getAllowSelfAssignment()
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
                task.getDeadline() != null ? Timestamp.valueOf(task.getDeadline()) : null,
                Timestamp.valueOf(task.getUpdatedAt()),
                task.getAllowedFileTypes(),
                task.getMaxFileSize(),
                task.getMaxFilesPerUser(),
                task.getAllowSelfAssignment(),
                task.getId()
        );

        if (updated == 0) {
            throw new RuntimeException("Zadanie nie zostało znalezione: " + task.getId());
        }

        return task;
    }

    public void deleteById(Long taskId) {
        String sql = "DELETE FROM tasks WHERE id = ?";
        int deleted = jdbcTemplate.update(sql, taskId);

        if (deleted == 0) {
            throw new RuntimeException("Zadanie nie zostało znalezione: " + taskId);
        }
    }

    public boolean existsById(Long taskId) {
        String sql = "SELECT EXISTS(SELECT 1 FROM tasks WHERE id = ?)";
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, taskId));
    }
}
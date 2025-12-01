// TaskJdbcRepository.java
package com.meethub.domain.repository.jdbc;

import com.meethub.domain.model.response.TaskStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class TaskJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    // Zaawansowane zapytania SQL
    public List<TaskStatsResponse> getDetailedTaskStats(Long meetingId) {
        String sql = """
            SELECT 
                t.id as task_id,
                t.title as task_title,
                t.status as task_status,
                COUNT(ta.id) as total_assignments,
                SUM(CASE WHEN ta.status = 'COMPLETED' THEN 1 ELSE 0 END) as completed_assignments,
                COUNT(tf.id) as total_files,
                COALESCE(SUM(tf.file_size), 0) as total_file_size
            FROM tasks t
            LEFT JOIN task_assignments ta ON t.id = ta.task_id
            LEFT JOIN task_files tf ON ta.id = tf.assignment_id
            WHERE t.meeting_id = ?
            GROUP BY t.id, t.title, t.status
            ORDER BY t.created_at DESC
            """;

        return jdbcTemplate.query(sql, new Object[]{meetingId}, new RowMapper<TaskStatsResponse>() {
            @Override
            public TaskStatsResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
                return TaskStatsResponse.builder()
                        .taskId(rs.getLong("task_id"))
                        .taskTitle(rs.getString("task_title"))
                        .taskStatus(rs.getString("task_status"))
                        .totalAssignments(rs.getInt("total_assignments"))
                        .completedAssignments(rs.getInt("completed_assignments"))
                        .totalFiles(rs.getInt("total_files"))
                        .totalFileSize(rs.getLong("total_file_size"))
                        .completionRate(rs.getInt("total_assignments") > 0 ?
                                (rs.getInt("completed_assignments") * 100.0) / rs.getInt("total_assignments") : 0)
                        .build();
            }
        });
    }

    public void bulkUpdateTaskStatus(List<Long> taskIds, String status) {
        String sql = "UPDATE tasks SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";

        List<Object[]> batchArgs = taskIds.stream()
                .map(taskId -> new Object[]{status, taskId})
                .collect(java.util.stream.Collectors.toList());

        jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    public int deleteOrphanedFiles() {
        String sql = """
            DELETE FROM task_files 
            WHERE assignment_id IN (
                SELECT tf.assignment_id FROM task_files tf
                LEFT JOIN task_assignments ta ON tf.assignment_id = ta.id
                WHERE ta.id IS NULL
            )
            """;
        return jdbcTemplate.update(sql);
    }

    public List<String> getTaskParticipantsEmails(Long taskId) {
        String sql = """
            SELECT DISTINCT u.email 
            FROM users u
            INNER JOIN task_assignments ta ON u.id = ta.user_id
            WHERE ta.task_id = ?
            """;
        return jdbcTemplate.queryForList(sql, new Object[]{taskId}, String.class);
    }
}
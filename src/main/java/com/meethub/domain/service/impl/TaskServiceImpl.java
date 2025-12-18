// TaskServiceImpl.java
package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.*;
import com.meethub.domain.model.enums.AssignmentStatus;
import com.meethub.domain.model.enums.TaskStatus;
import com.meethub.domain.model.request.CreateTaskRequest;
import com.meethub.domain.model.request.UpdateTaskRequest;
import com.meethub.domain.model.response.*;
import com.meethub.domain.repository.jpa.*;
import com.meethub.domain.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final TaskAssignmentRepository assignmentRepository;
    private final TaskFileRepository fileRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    @Transactional
    public Task createTask(CreateTaskRequest request, Long meetingId, Long organizerId) {
        log.info("Creating task for meeting: {}, organizer: {}", meetingId, organizerId);

        // Sprawdź spotkanie i uprawnienia używając JDBC
        String meetingSql = "SELECT organizer_id FROM meetings WHERE id = ?";
        Long actualOrganizerId;
        try {
            actualOrganizerId = jdbcTemplate.queryForObject(meetingSql, Long.class, meetingId);
        } catch (EmptyResultDataAccessException e) {
            throw new RuntimeException("Spotkanie nie zostało znalezione");
        }

        if (!actualOrganizerId.equals(organizerId)) {
            throw new RuntimeException("Tylko organizator może wykonać tę akcję");
        }

        // Sprawdź użytkownika
        String userSql = "SELECT EXISTS(SELECT 1 FROM users WHERE id = ?)";
        Boolean userExists = jdbcTemplate.queryForObject(userSql, Boolean.class, organizerId);
        if (userExists == null || !userExists) {
            throw new RuntimeException("Użytkownik nie został znaleziony");
        }

        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(TaskStatus.TODO);
        task.setDeadline(request.getDeadline());
        task.setMeetingId(meetingId);
        task.setCreatedById(organizerId);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        task.setAllowedFileTypes(request.getAllowedFileTypesAsString());
        task.setMaxFileSize(request.getMaxFileSize() != null ? request.getMaxFileSize() : 10 * 1024 * 1024L);
        task.setMaxFilesPerUser(request.getMaxFilesPerUser() != null ? request.getMaxFilesPerUser() : 10);
        task.setAllowSelfAssignment(request.getAllowSelfAssignment());

        Task savedTask = taskRepository.save(task);
        log.info("Task created successfully: {}", savedTask.getId());
        return savedTask;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Task> getMeetingTasks(Long meetingId) {
        log.debug("Getting tasks for meeting: {}", meetingId);
        return taskRepository.findByMeetingId(meetingId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Task> getUserCreatedTasks(Long userId) {
        log.debug("Getting tasks created by user: {}", userId);
        return taskRepository.findByCreatedById(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Task getTaskById(Long taskId) {
        log.debug("Getting task by ID: {}", taskId);
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Zadanie nie zostało znalezione"));
    }

    @Override
    @Transactional
    public Task updateTaskWithRequest(Long taskId, UpdateTaskRequest request, Long userId) {
        log.info("Updating task: {}, user: {}", taskId, userId);

        Task task = getTaskById(taskId);

        // Sprawdź uprawnienia organizatora używając JDBC
        String permissionSql = """
            SELECT EXISTS(
                SELECT 1 FROM tasks t 
                JOIN meetings m ON t.meeting_id = m.id 
                WHERE t.id = ? AND m.organizer_id = ?
            )
            """;
        Boolean isOrganizer = jdbcTemplate.queryForObject(permissionSql, Boolean.class, taskId, userId);

        if (isOrganizer == null || !isOrganizer) {
            throw new RuntimeException("Tylko organizator może wykonać tę akcję");
        }

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setDeadline(request.getDeadline());
        task.setAllowSelfAssignment(request.getAllowSelfAssignment());
        task.setUpdatedAt(LocalDateTime.now());

        if (request.getAllowedFileTypes() != null) {
            task.setAllowedFileTypes(request.getAllowedFileTypesAsString());
        }

        Task updatedTask = taskRepository.save(task);
        log.info("Task updated successfully: {}", taskId);
        return updatedTask;
    }

    @Override
    @Transactional
    public void deleteTask(Long taskId, Long userId) {
        log.info("Deleting task: {}, user: {}", taskId, userId);

        Task task = getTaskById(taskId);

        // Sprawdź uprawnienia używając JDBC
        String permissionSql = """
            SELECT EXISTS(
                SELECT 1 FROM tasks t 
                JOIN meetings m ON t.meeting_id = m.id 
                WHERE t.id = ? AND m.organizer_id = ?
            )
            """;
        Boolean isOrganizer = jdbcTemplate.queryForObject(permissionSql, Boolean.class, taskId, userId);

        if (isOrganizer == null || !isOrganizer) {
            throw new RuntimeException("Tylko organizator może wykonać tę akcję");
        }

        // Usuń pliki fizycznie z dysku
        deleteTaskFilesFromDisk(task);

        // Usuń związane rekordy w odpowiedniej kolejności używając JDBC
        String deleteFilesSql = "DELETE FROM task_files WHERE assignment_id IN " +
                "(SELECT id FROM task_assignments WHERE task_id = ?)";
        jdbcTemplate.update(deleteFilesSql, taskId);

        String deleteAssignmentsSql = "DELETE FROM task_assignments WHERE task_id = ?";
        jdbcTemplate.update(deleteAssignmentsSql, taskId);

        // Usuń zadanie
        taskRepository.deleteById(taskId);

        log.info("Task deleted successfully: {}", taskId);
    }

    @Override
    @Transactional
    public TaskAssignment assignTask(Long taskId, Long userId, Long organizerId) {
        log.info("Assigning task: {} to user: {}, by organizer: {}", taskId, userId, organizerId);

        Task task = getTaskById(taskId);

        // Sprawdź uprawnienia organizatora używając JDBC
        String checkPermissionSql = """
            SELECT EXISTS(
                SELECT 1 FROM meetings m 
                WHERE m.id = ? AND m.organizer_id = ?
            )
            """;

        Boolean isOrganizer = jdbcTemplate.queryForObject(
                checkPermissionSql, Boolean.class, task.getMeetingId(), organizerId);

        if (isOrganizer == null || !isOrganizer) {
            throw new RuntimeException("Tylko organizator może wykonać tę akcję");
        }

        // Sprawdź czy użytkownik istnieje
        String userExistsSql = "SELECT EXISTS(SELECT 1 FROM users WHERE id = ?)";
        Boolean userExists = jdbcTemplate.queryForObject(userExistsSql, Boolean.class, userId);

        if (userExists == null || !userExists) {
            throw new RuntimeException("Użytkownik nie został znaleziony");
        }

        // Sprawdź czy użytkownik jest uczestnikiem spotkania
        String participantSql = """
            SELECT EXISTS(
                SELECT 1 FROM meeting_participants 
                WHERE meeting_id = ? AND user_id = ?
            )
            """;

        Boolean isParticipant = jdbcTemplate.queryForObject(
                participantSql, Boolean.class, task.getMeetingId(), userId);

        if (isParticipant == null || !isParticipant) {
            throw new RuntimeException("Użytkownik nie jest uczestnikiem spotkania");
        }

        // Sprawdź czy już nie jest przypisany
        String assignmentExistsSql = """
            SELECT EXISTS(
                SELECT 1 FROM task_assignments 
                WHERE task_id = ? AND user_id = ?
            )
            """;

        Boolean alreadyAssigned = jdbcTemplate.queryForObject(
                assignmentExistsSql, Boolean.class, taskId, userId);

        if (alreadyAssigned != null && alreadyAssigned) {
            throw new RuntimeException("Użytkownik jest już przypisany do tego zadania");
        }

        // Utwórz przypisanie używając JDBC
        String insertAssignmentSql = """
            INSERT INTO task_assignments (task_id, user_id, status, assigned_at)
            VALUES (?, ?, ?, ?)
            """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    insertAssignmentSql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, taskId);
            ps.setLong(2, userId);
            ps.setString(3, AssignmentStatus.ASSIGNED.name());
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            return ps;
        }, keyHolder);

        Long assignmentId = keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;

        // Pobierz utworzone przypisanie
        String getAssignmentSql = """
            SELECT ta.*, u.email as user_email, t.title as task_title
            FROM task_assignments ta
            JOIN users u ON ta.user_id = u.id
            JOIN tasks t ON ta.task_id = t.id
            WHERE ta.id = ?
            """;

        return jdbcTemplate.queryForObject(getAssignmentSql, (rs, rowNum) -> {
            TaskAssignment assignment = new TaskAssignment();
            assignment.setId(rs.getLong("id"));
            assignment.setTask(task);

            User user = new User();
            user.setId(userId);
            user.setEmail(rs.getString("user_email"));
            assignment.setUser(user);

            assignment.setStatus(AssignmentStatus.valueOf(rs.getString("status")));
            assignment.setAssignedAt(rs.getTimestamp("assigned_at").toLocalDateTime());
            return assignment;
        }, assignmentId);
    }

    @Override
    @Transactional
    public TaskAssignment assignTaskToCurrentUser(Long taskId, Long userId) {
        log.info("User {} self-assigning to task: {}", userId, taskId);

        Task task = getTaskById(taskId);

        // Sprawdź czy użytkownik jest uczestnikiem spotkania używając JDBC
        String participantSql = """
            SELECT EXISTS(
                SELECT 1 FROM meeting_participants 
                WHERE meeting_id = ? AND user_id = ?
            )
            """;

        Boolean isParticipant = jdbcTemplate.queryForObject(
                participantSql, Boolean.class, task.getMeetingId(), userId);

        if (isParticipant == null || !isParticipant) {
            throw new RuntimeException("Nie jesteś uczestnikiem tego spotkania");
        }

        // Sprawdź czy już nie jest przypisany
        String assignmentExistsSql = """
            SELECT EXISTS(
                SELECT 1 FROM task_assignments 
                WHERE task_id = ? AND user_id = ?
            )
            """;

        Boolean alreadyAssigned = jdbcTemplate.queryForObject(
                assignmentExistsSql, Boolean.class, taskId, userId);

        if (alreadyAssigned != null && alreadyAssigned) {
            throw new RuntimeException("Jesteś już przypisany do tego zadania");
        }

        // Sprawdź czy użytkownik istnieje
        String userExistsSql = "SELECT EXISTS(SELECT 1 FROM users WHERE id = ?)";
        Boolean userExists = jdbcTemplate.queryForObject(userExistsSql, Boolean.class, userId);

        if (userExists == null || !userExists) {
            throw new RuntimeException("Użytkownik nie został znaleziony");
        }

        // Utwórz przypisanie używając JDBC
        String insertAssignmentSql = """
            INSERT INTO task_assignments (task_id, user_id, status, assigned_at)
            VALUES (?, ?, ?, ?)
            """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    insertAssignmentSql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, taskId);
            ps.setLong(2, userId);
            ps.setString(3, AssignmentStatus.ASSIGNED.name());
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            return ps;
        }, keyHolder);

        Long assignmentId = keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;

        // Pobierz utworzone przypisanie
        String getAssignmentSql = """
            SELECT ta.*, u.email as user_email, t.title as task_title
            FROM task_assignments ta
            JOIN users u ON ta.user_id = u.id
            JOIN tasks t ON ta.task_id = t.id
            WHERE ta.id = ?
            """;

        return jdbcTemplate.queryForObject(getAssignmentSql, (rs, rowNum) -> {
            TaskAssignment assignment = new TaskAssignment();
            assignment.setId(rs.getLong("id"));
            assignment.setTask(task);

            User user = new User();
            user.setId(userId);
            user.setEmail(rs.getString("user_email"));
            assignment.setUser(user);

            assignment.setStatus(AssignmentStatus.valueOf(rs.getString("status")));
            assignment.setAssignedAt(rs.getTimestamp("assigned_at").toLocalDateTime());
            return assignment;
        }, assignmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskAssignment> getUserAssignments(Long userId) {
        log.debug("Getting assignments for user: {}", userId);

        String sql = """
            SELECT ta.*, t.title as task_title, t.description as task_description,
                   t.meeting_id, m.title as meeting_title, u.email as user_email
            FROM task_assignments ta
            JOIN tasks t ON ta.task_id = t.id
            JOIN meetings m ON t.meeting_id = m.id
            JOIN users u ON ta.user_id = u.id
            WHERE ta.user_id = ?
            ORDER BY ta.assigned_at DESC
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            TaskAssignment assignment = new TaskAssignment();
            assignment.setId(rs.getLong("id"));
            assignment.setStatus(AssignmentStatus.valueOf(rs.getString("status")));
            assignment.setAssignedAt(rs.getTimestamp("assigned_at").toLocalDateTime());
            assignment.setCompletedAt(rs.getTimestamp("completed_at") != null ?
                    rs.getTimestamp("completed_at").toLocalDateTime() : null);
            assignment.setComment(rs.getString("comment"));

            Task task = new Task();
            task.setId(rs.getLong("task_id"));
            task.setTitle(rs.getString("task_title"));
            task.setDescription(rs.getString("task_description"));
            task.setMeetingId(rs.getLong("meeting_id"));

            User user = new User();
            user.setId(userId);
            user.setEmail(rs.getString("user_email"));

            assignment.setTask(task);
            assignment.setUser(user);

            return assignment;
        }, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskAssignment> getTaskAssignments(Long taskId) {
        log.debug("Getting assignments for task: {}", taskId);

        String sql = """
            SELECT ta.*, u.email as user_email, u.first_name, u.last_name,
                   t.title as task_title
            FROM task_assignments ta
            JOIN users u ON ta.user_id = u.id
            JOIN tasks t ON ta.task_id = t.id
            WHERE ta.task_id = ?
            ORDER BY ta.assigned_at DESC
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            TaskAssignment assignment = new TaskAssignment();
            assignment.setId(rs.getLong("id"));
            assignment.setStatus(AssignmentStatus.valueOf(rs.getString("status")));
            assignment.setAssignedAt(rs.getTimestamp("assigned_at").toLocalDateTime());
            assignment.setCompletedAt(rs.getTimestamp("completed_at") != null ?
                    rs.getTimestamp("completed_at").toLocalDateTime() : null);
            assignment.setComment(rs.getString("comment"));

            User user = new User();
            user.setId(rs.getLong("user_id"));
            user.setEmail(rs.getString("user_email"));
            user.setFirstName(rs.getString("first_name"));
            user.setLastName(rs.getString("last_name"));

            assignment.setUser(user);
            assignment.setTask(Task.builder().id(taskId).title(rs.getString("task_title")).build());

            return assignment;
        }, taskId);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskAssignment getAssignmentById(Long assignmentId) {
        log.debug("Getting assignment by ID: {}", assignmentId);

        String sql = """
            SELECT ta.*, u.email as user_email, t.title as task_title,
                   t.meeting_id, m.organizer_id
            FROM task_assignments ta
            JOIN users u ON ta.user_id = u.id
            JOIN tasks t ON ta.task_id = t.id
            JOIN meetings m ON t.meeting_id = m.id
            WHERE ta.id = ?
            """;

        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                TaskAssignment assignment = new TaskAssignment();
                assignment.setId(rs.getLong("id"));
                assignment.setStatus(AssignmentStatus.valueOf(rs.getString("status")));
                assignment.setAssignedAt(rs.getTimestamp("assigned_at").toLocalDateTime());
                assignment.setCompletedAt(rs.getTimestamp("completed_at") != null ?
                        rs.getTimestamp("completed_at").toLocalDateTime() : null);
                assignment.setComment(rs.getString("comment"));

                User user = new User();
                user.setId(rs.getLong("user_id"));
                user.setEmail(rs.getString("user_email"));

                Task task = new Task();
                task.setId(rs.getLong("task_id"));
                task.setTitle(rs.getString("task_title"));
                task.setMeetingId(rs.getLong("meeting_id"));

                Meeting meeting = new Meeting();
                meeting.setId(rs.getLong("meeting_id"));
                meeting.setOrganizer(User.builder().id(rs.getLong("organizer_id")).build());

                assignment.setUser(user);
                assignment.setTask(task);

                return assignment;
            }, assignmentId);
        } catch (EmptyResultDataAccessException e) {
            throw new RuntimeException("Przypisanie nie zostało znalezione");
        }
    }

    @Override
    @Transactional
    public TaskAssignment updateAssignmentStatus(Long assignmentId, AssignmentStatus status, Long userId) {
        log.info("Updating assignment status: {}, status: {}, user: {}", assignmentId, status, userId);

        TaskAssignment assignment = getAssignmentById(assignmentId);
        validateAssignmentAccess(assignment, userId);

        String sql = "UPDATE task_assignments SET status = ?, completed_at = ? WHERE id = ?";

        Timestamp completedAt = status == AssignmentStatus.COMPLETED ?
                Timestamp.valueOf(LocalDateTime.now()) : null;

        int updated = jdbcTemplate.update(sql,
                status.name(),
                completedAt,
                assignmentId
        );

        if (updated == 0) {
            throw new RuntimeException("Nie udało się zaktualizować statusu przypisania");
        }

        assignment.setStatus(status);
        assignment.setCompletedAt(completedAt != null ? completedAt.toLocalDateTime() : null);

        log.info("Assignment status updated successfully: {}", assignmentId);
        return assignment;
    }

    @Override
    @Transactional
    public TaskAssignment updateAssignmentComment(Long assignmentId, String comment, Long userId) {
        log.info("Updating assignment comment: {}, user: {}", assignmentId, userId);

        TaskAssignment assignment = getAssignmentById(assignmentId);
        validateAssignmentAccess(assignment, userId);

        String sql = "UPDATE task_assignments SET comment = ? WHERE id = ?";
        int updated = jdbcTemplate.update(sql, comment, assignmentId);

        if (updated == 0) {
            throw new RuntimeException("Nie udało się zaktualizować komentarza przypisania");
        }

        assignment.setComment(comment);

        log.info("Assignment comment updated successfully: {}", assignmentId);
        return assignment;
    }

    @Override
    @Transactional
    public void removeAssignment(Long assignmentId, Long userId) {
        log.info("Removing assignment: {}, user: {}", assignmentId, userId);

        TaskAssignment assignment = getAssignmentById(assignmentId);

        // Sprawdź czy użytkownik jest organizatorem
        String checkOrganizerSql = """
            SELECT EXISTS(
                SELECT 1 FROM tasks t
                JOIN meetings m ON t.meeting_id = m.id
                WHERE t.id = ? AND m.organizer_id = ?
            )
            """;
        Boolean isOrganizer = jdbcTemplate.queryForObject(
                checkOrganizerSql, Boolean.class, assignment.getTask().getId(), userId);

        // Sprawdź czy użytkownik jest właścicielem przypisania
        boolean isOwner = assignment.getUser().getId().equals(userId);

        if ((isOrganizer == null || !isOrganizer) && !isOwner) {
            throw new RuntimeException("Brak uprawnień do usunięcia przypisania");
        }

        // Usuń pliki fizycznie z dysku
        deleteAssignmentFilesFromDisk(assignment);

        // Usuń przypisanie używając JDBC
        String deleteAssignmentSql = "DELETE FROM task_assignments WHERE id = ?";
        int deleted = jdbcTemplate.update(deleteAssignmentSql, assignmentId);

        if (deleted == 0) {
            throw new RuntimeException("Przypisanie nie zostało znalezione");
        }

        log.info("Assignment removed successfully: {}", assignmentId);
    }

    @Override
    @Transactional
    public TaskFile uploadFile(Long assignmentId, MultipartFile file, Long userId) {
        log.info("Uploading file for assignment: {}, user: {}", assignmentId, userId);

        TaskAssignment assignment = getAssignmentById(assignmentId);
        validateAssignmentAccess(assignment, userId);

        if (file.isEmpty()) {
            throw new RuntimeException("Plik jest pusty");
        }

        try {
            // Bezpieczna nazwa katalogu użytkownika
            String userEmail = assignment.getUser().getEmail();
            String safeUserDir = userEmail.replace("@", "_at_").replace(".", "_");
            String taskDir = "task_" + assignment.getTask().getId();

            Path uploadPath = Paths.get(uploadDir, "tasks", taskDir, safeUserDir);
            Files.createDirectories(uploadPath);

            // Unikalna nazwa pliku
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
            Path filePath = uploadPath.resolve(uniqueFilename);

            // Zapisz plik
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Zapisz w bazie używając JDBC
            String insertFileSql = """
                INSERT INTO task_files (
                    filename, original_filename, file_path, file_size,
                    content_type, assignment_id, uploaded_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        insertFileSql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, uniqueFilename);
                ps.setString(2, originalFilename);
                ps.setString(3, filePath.toString());
                ps.setLong(4, file.getSize());
                ps.setString(5, file.getContentType());
                ps.setLong(6, assignmentId);
                ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
                return ps;
            }, keyHolder);

            Long fileId = keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;

            TaskFile taskFile = new TaskFile();
            taskFile.setId(fileId);
            taskFile.setFilename(uniqueFilename);
            taskFile.setOriginalFilename(originalFilename);
            taskFile.setFilePath(filePath.toString());
            taskFile.setFileSize(file.getSize());
            taskFile.setContentType(file.getContentType());
            taskFile.setAssignment(assignment);
            taskFile.setUploadedAt(LocalDateTime.now());

            log.info("File uploaded successfully: {}", fileId);
            return taskFile;

        } catch (IOException e) {
            log.error("Error uploading file: {}", e.getMessage());
            throw new RuntimeException("Błąd podczas zapisywania pliku: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskFile> getAssignmentFiles(Long assignmentId, Long userId) {
        log.debug("Getting files for assignment: {}, user: {}", assignmentId, userId);

        TaskAssignment assignment = getAssignmentById(assignmentId);
        validateFileAccess(assignment, userId);

        String sql = "SELECT * FROM task_files WHERE assignment_id = ? ORDER BY uploaded_at DESC";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            TaskFile taskFile = new TaskFile();
            taskFile.setId(rs.getLong("id"));
            taskFile.setFilename(rs.getString("filename"));
            taskFile.setOriginalFilename(rs.getString("original_filename"));
            taskFile.setFilePath(rs.getString("file_path"));
            taskFile.setFileSize(rs.getLong("file_size"));
            taskFile.setContentType(rs.getString("content_type"));
            taskFile.setUploadedAt(rs.getTimestamp("uploaded_at").toLocalDateTime());

            // Ustaw tylko ID przypisania
            taskFile.setAssignment(new TaskAssignment());
            taskFile.getAssignment().setId(assignmentId);

            return taskFile;
        }, assignmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskAssignment> getAssignmentsByStatus(Long taskId, AssignmentStatus status) {
        log.debug("Getting assignments for task: {} with status: {}", taskId, status);

        String sql = """
            SELECT ta.*, u.email as user_email, u.first_name, u.last_name
            FROM task_assignments ta
            JOIN users u ON ta.user_id = u.id
            WHERE ta.task_id = ? AND ta.status = ?
            ORDER BY ta.assigned_at DESC
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            TaskAssignment assignment = new TaskAssignment();
            assignment.setId(rs.getLong("id"));
            assignment.setStatus(AssignmentStatus.valueOf(rs.getString("status")));
            assignment.setAssignedAt(rs.getTimestamp("assigned_at").toLocalDateTime());
            assignment.setCompletedAt(rs.getTimestamp("completed_at") != null ?
                    rs.getTimestamp("completed_at").toLocalDateTime() : null);
            assignment.setComment(rs.getString("comment"));

            User user = new User();
            user.setId(rs.getLong("user_id"));
            user.setEmail(rs.getString("user_email"));
            user.setFirstName(rs.getString("first_name"));
            user.setLastName(rs.getString("last_name"));

            assignment.setUser(user);
            assignment.setTask(Task.builder().id(taskId).build());

            return assignment;
        }, taskId, status.name());
    }

    @Override
    @Transactional(readOnly = true)
    public Long countCompletedAssignments(Long taskId) {
        String sql = "SELECT COUNT(*) FROM task_assignments WHERE task_id = ? AND status = 'COMPLETED'";
        return jdbcTemplate.queryForObject(sql, Long.class, taskId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countTotalAssignments(Long taskId) {
        String sql = "SELECT COUNT(*) FROM task_assignments WHERE task_id = ?";
        return jdbcTemplate.queryForObject(sql, Long.class, taskId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUserManageTask(Long taskId, Long userId) {
        String sql = """
            SELECT EXISTS(
                SELECT 1 FROM tasks t
                JOIN meetings m ON t.meeting_id = m.id
                WHERE t.id = ? AND m.organizer_id = ?
            )
            """;

        try {
            return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, taskId, userId));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUserAccessAssignment(Long assignmentId, Long userId) {
        String sql = """
            SELECT EXISTS(
                SELECT 1 FROM task_assignments ta
                JOIN tasks t ON ta.task_id = t.id
                JOIN meetings m ON t.meeting_id = m.id
                WHERE ta.id = ? AND (ta.user_id = ? OR m.organizer_id = ?)
            )
            """;

        try {
            return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                    sql, Boolean.class, assignmentId, userId, userId));
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public MeetingTasksResponse getMeetingTasksForUser(Long meetingId, Long userId) {
        // Pobierz spotkanie używając JDBC
        String meetingSql = """
            SELECT m.*, u.email as organizer_email
            FROM meetings m
            JOIN users u ON m.organizer_id = u.id
            WHERE m.id = ?
            """;

        Meeting meeting = jdbcTemplate.queryForObject(meetingSql, (rs, rowNum) -> {
            Meeting m = new Meeting();
            m.setId(rs.getLong("id"));
            m.setTitle(rs.getString("title"));
            m.setDescription(rs.getString("description"));
            m.setOrganizer(User.builder()
                    .id(rs.getLong("organizer_id"))
                    .email(rs.getString("organizer_email"))
                    .build());
            return m;
        }, meetingId);

        boolean isOrganizer = meeting.getOrganizer().getId().equals(userId);
        List<Task> tasks = getMeetingTasks(meetingId);

        return MeetingTasksResponse.builder()
                .meeting(meeting)
                .tasks(tasks)
                .isOrganizer(isOrganizer)
                .build();
    }

    @Transactional(readOnly = true)
    @Override
    public MeetingTaskFormResponse getTaskCreationFormData(Long meetingId, Long userId) {
        String meetingSql = """
            SELECT m.*, u.email as organizer_email
            FROM meetings m
            JOIN users u ON m.organizer_id = u.id
            WHERE m.id = ?
            """;

        Meeting meeting = jdbcTemplate.queryForObject(meetingSql, (rs, rowNum) -> {
            Meeting m = new Meeting();
            m.setId(rs.getLong("id"));
            m.setTitle(rs.getString("title"));
            m.setOrganizer(User.builder()
                    .id(rs.getLong("organizer_id"))
                    .email(rs.getString("organizer_email"))
                    .build());
            return m;
        }, meetingId);

        if (!meeting.getOrganizer().getId().equals(userId)) {
            throw new RuntimeException("Tylko organizator może tworzyć zadania");
        }

        return MeetingTaskFormResponse.builder()
                .meeting(meeting)
                .createTaskRequest(new CreateTaskRequest())
                .build();
    }

    @Transactional(readOnly = true)
    @Override
    public MeetingTaskDetailsResponse getTaskDetailsForUser(Long meetingId, Long taskId, Long userId) {
        Task task = getTaskById(taskId);

        String meetingSql = "SELECT * FROM meetings WHERE id = ?";
        Meeting meeting = jdbcTemplate.queryForObject(meetingSql, (rs, rowNum) -> {
            Meeting m = new Meeting();
            m.setId(rs.getLong("id"));
            m.setTitle(rs.getString("title"));
            m.setOrganizer(User.builder().id(rs.getLong("organizer_id")).build());
            return m;
        }, meetingId);

        boolean isOrganizer = meeting.getOrganizer().getId().equals(userId);

        // Sprawdź czy użytkownik jest przypisany do zadania
        String checkAssignmentSql = """
            SELECT EXISTS(
                SELECT 1 FROM task_assignments 
                WHERE task_id = ? AND user_id = ?
            )
            """;
        Boolean isAssigned = jdbcTemplate.queryForObject(
                checkAssignmentSql, Boolean.class, taskId, userId);

        boolean canAccess = isOrganizer || (isAssigned != null && isAssigned);

        if (!canAccess) {
            throw new RuntimeException("Brak uprawnień do tego zadania");
        }

        return MeetingTaskDetailsResponse.builder()
                .meeting(meeting)
                .task(task)
                .isOrganizer(isOrganizer)
                .userId(userId)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public MeetingTaskEditResponse getTaskForEditing(Long meetingId, Long taskId, Long userId) {
        Task task = getTaskById(taskId);

        String meetingSql = "SELECT * FROM meetings WHERE id = ?";
        Meeting meeting = jdbcTemplate.queryForObject(meetingSql, (rs, rowNum) -> {
            Meeting m = new Meeting();
            m.setId(rs.getLong("id"));
            m.setTitle(rs.getString("title"));
            m.setOrganizer(User.builder().id(rs.getLong("organizer_id")).build());
            return m;
        }, meetingId);

        if (!meeting.getOrganizer().getId().equals(userId)) {
            throw new RuntimeException("Tylko organizator może edytować zadania");
        }

        String formattedDeadline = task.getDeadline() != null ?
                task.getDeadline().toString().replace("T", " ") : null;

        return MeetingTaskEditResponse.builder()
                .meeting(meeting)
                .task(task)
                .formattedDeadline(formattedDeadline)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public MeetingTaskAssignmentsResponse getTaskAssignmentsForUser(Long meetingId, Long taskId, Long userId) {
        Task task = getTaskById(taskId);

        String meetingSql = "SELECT * FROM meetings WHERE id = ?";
        Meeting meeting = jdbcTemplate.queryForObject(meetingSql, (rs, rowNum) -> {
            Meeting m = new Meeting();
            m.setId(rs.getLong("id"));
            m.setTitle(rs.getString("title"));
            m.setOrganizer(User.builder().id(rs.getLong("organizer_id")).build());
            return m;
        }, meetingId);

        if (!meeting.getOrganizer().getId().equals(userId)) {
            throw new RuntimeException("Tylko organizator może przypisywać użytkowników");
        }

        // Pobierz przypisanych użytkowników
        String assignedUsersSql = """
            SELECT u.* FROM users u
            JOIN task_assignments ta ON u.id = ta.user_id
            WHERE ta.task_id = ?
            """;

        List<User> assignedUsers = jdbcTemplate.query(assignedUsersSql, (rs, rowNum) -> {
            User user = new User();
            user.setId(rs.getLong("id"));
            user.setEmail(rs.getString("email"));
            user.setFirstName(rs.getString("first_name"));
            user.setLastName(rs.getString("last_name"));
            return user;
        }, taskId);

        // Pobierz dostępnych użytkowników
        String availableUsersSql = """
            SELECT u.* FROM users u
            JOIN meeting_participants mp ON u.id = mp.user_id
            WHERE mp.meeting_id = ? AND u.id NOT IN (
                SELECT ta.user_id FROM task_assignments ta
                WHERE ta.task_id = ?
            )
            """;

        List<User> availableUsers = jdbcTemplate.query(availableUsersSql, (rs, rowNum) -> {
            User user = new User();
            user.setId(rs.getLong("id"));
            user.setEmail(rs.getString("email"));
            user.setFirstName(rs.getString("first_name"));
            user.setLastName(rs.getString("last_name"));
            return user;
        }, meetingId, taskId);

        List<TaskAssignment> assignments = getTaskAssignments(taskId);

        return MeetingTaskAssignmentsResponse.builder()
                .meeting(meeting)
                .task(task)
                .availableUsers(availableUsers)
                .assignedUsers(assignedUsers)
                .assignments(assignments)
                .build();
    }

    @Transactional(readOnly = true)
    @Override
    public TaskFile getFileById(Long fileId) {
        log.debug("Getting file by ID: {}", fileId);

        String sql = """
            SELECT tf.*, ta.task_id, ta.user_id, u.email as user_email
            FROM task_files tf
            JOIN task_assignments ta ON tf.assignment_id = ta.id
            JOIN users u ON ta.user_id = u.id
            WHERE tf.id = ?
            """;

        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                TaskFile taskFile = new TaskFile();
                taskFile.setId(rs.getLong("id"));
                taskFile.setFilename(rs.getString("filename"));
                taskFile.setOriginalFilename(rs.getString("original_filename"));
                taskFile.setFilePath(rs.getString("file_path"));
                taskFile.setFileSize(rs.getLong("file_size"));
                taskFile.setContentType(rs.getString("content_type"));
                taskFile.setUploadedAt(rs.getTimestamp("uploaded_at").toLocalDateTime());

                TaskAssignment assignment = new TaskAssignment();
                assignment.setId(rs.getLong("assignment_id"));

                Task task = new Task();
                task.setId(rs.getLong("task_id"));
                assignment.setTask(task);

                User user = new User();
                user.setId(rs.getLong("user_id"));
                user.setEmail(rs.getString("user_email"));
                assignment.setUser(user);

                taskFile.setAssignment(assignment);

                return taskFile;
            }, fileId);
        } catch (EmptyResultDataAccessException e) {
            throw new RuntimeException("Plik nie został znaleziony");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadFile(Long fileId, Long userId) {
        log.debug("Downloading file: {}, user: {}", fileId, userId);

        TaskFile taskFile = getFileById(fileId);
        validateFileAccess(taskFile, userId);

        try {
            Path filePath = Paths.get(taskFile.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                log.info("File downloaded successfully: {}", fileId);
                return resource;
            } else {
                throw new RuntimeException("Plik nie istnieje lub nie można go odczytać");
            }
        } catch (MalformedURLException e) {
            log.error("Error downloading file: {}", e.getMessage());
            throw new RuntimeException("Błąd podczas pobierania pliku: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    @Override
    public List<TaskFile> getTaskFiles(Long taskId, Long userId) {
        log.debug("Getting all files for task: {}, user: {}", taskId, userId);

        Task task = getTaskById(taskId);

        if (!canUserViewTaskFiles(taskId, userId)) {
            throw new RuntimeException("Brak uprawnień do przeglądania plików tego zadania");
        }

        // Pobierz wszystkie pliki związane z tym zadaniem
        String sql = """
            SELECT tf.* FROM task_files tf
            JOIN task_assignments ta ON tf.assignment_id = ta.id
            WHERE ta.task_id = ?
            UNION
            SELECT tf.* FROM task_files tf
            WHERE tf.task_id = ? AND tf.assignment_id IS NULL
            ORDER BY uploaded_at DESC
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            TaskFile taskFile = new TaskFile();
            taskFile.setId(rs.getLong("id"));
            taskFile.setFilename(rs.getString("filename"));
            taskFile.setOriginalFilename(rs.getString("original_filename"));
            taskFile.setFilePath(rs.getString("file_path"));
            taskFile.setFileSize(rs.getLong("file_size"));
            taskFile.setContentType(rs.getString("content_type"));
            taskFile.setUploadedAt(rs.getTimestamp("uploaded_at").toLocalDateTime());

            TaskAssignment assignment = new TaskAssignment();
            assignment.setId(rs.getLong("assignment_id"));
            assignment.setTask(task);
            taskFile.setAssignment(assignment);

            return taskFile;
        }, taskId, taskId);
    }

    @Override
    @Transactional
    public void deleteFile(Long fileId, Long userId) {
        log.info("Deleting file: {}, user: {}", fileId, userId);

        TaskFile taskFile = getFileById(fileId);

        // Sprawdź uprawnienia
        String checkPermissionsSql = """
            SELECT EXISTS(
                SELECT 1 FROM task_files tf
                JOIN task_assignments ta ON tf.assignment_id = ta.id
                JOIN tasks t ON ta.task_id = t.id
                JOIN meetings m ON t.meeting_id = m.id
                WHERE tf.id = ? AND (
                    tf.uploaded_by_id = ? OR 
                    m.organizer_id = ?
                )
            )
            """;

        Boolean hasPermission = jdbcTemplate.queryForObject(
                checkPermissionsSql, Boolean.class, fileId, userId, userId);

        if (hasPermission == null || !hasPermission) {
            throw new RuntimeException("Brak uprawnień do usunięcia tego pliku");
        }

        try {
            // Usuń plik fizycznie
            Path filePath = Paths.get(taskFile.getFilePath());
            Files.deleteIfExists(filePath);

            // Usuń z bazy
            String deleteSql = "DELETE FROM task_files WHERE id = ?";
            jdbcTemplate.update(deleteSql, fileId);

            log.info("File deleted successfully: {}", fileId);
        } catch (IOException e) {
            log.error("Error deleting file: {}", e.getMessage());
            throw new RuntimeException("Błąd podczas usuwania pliku: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskFile> getUserFilesForTask(Long taskId, Long userId) {
        log.debug("Getting user files for task: {}, user: {}", taskId, userId);

        Task task = getTaskById(taskId);

        // Sprawdź czy użytkownik ma dostęp do tego zadania
        if (!canUserViewTaskFiles(taskId, userId)) {
            throw new RuntimeException("Brak uprawnień do przeglądania plików tego zadania");
        }

        String sql = """
            SELECT tf.* FROM task_files tf
            JOIN task_assignments ta ON tf.assignment_id = ta.id
            WHERE ta.task_id = ? AND ta.user_id = ?
            ORDER BY tf.uploaded_at DESC
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            TaskFile taskFile = new TaskFile();
            taskFile.setId(rs.getLong("id"));
            taskFile.setFilename(rs.getString("filename"));
            taskFile.setOriginalFilename(rs.getString("original_filename"));
            taskFile.setFilePath(rs.getString("file_path"));
            taskFile.setFileSize(rs.getLong("file_size"));
            taskFile.setContentType(rs.getString("content_type"));
            taskFile.setUploadedAt(rs.getTimestamp("uploaded_at").toLocalDateTime());

            TaskAssignment assignment = new TaskAssignment();
            assignment.setId(rs.getLong("assignment_id"));
            assignment.setTask(task);

            User user = new User();
            user.setId(userId);
            assignment.setUser(user);

            taskFile.setAssignment(assignment);

            return taskFile;
        }, taskId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskFile> getAllTaskFilesForOrganizer(Long taskId, Long userId) {
        log.debug("Getting all files for task (organizer view): {}, user: {}", taskId, userId);

        Task task = getTaskById(taskId);

        String checkOrganizerSql = """
            SELECT EXISTS(
                SELECT 1 FROM tasks t
                JOIN meetings m ON t.meeting_id = m.id
                WHERE t.id = ? AND m.organizer_id = ?
            )
            """;

        Boolean isOrganizer = jdbcTemplate.queryForObject(
                checkOrganizerSql, Boolean.class, taskId, userId);

        if (isOrganizer == null || !isOrganizer) {
            throw new RuntimeException("Tylko organizator może przeglądać wszystkie pliki zadania");
        }

        String sql = """
            SELECT tf.*, ta.user_id, u.email as user_email
            FROM task_files tf
            LEFT JOIN task_assignments ta ON tf.assignment_id = ta.id
            LEFT JOIN users u ON ta.user_id = u.id
            WHERE tf.task_id = ? OR ta.task_id = ?
            ORDER BY tf.uploaded_at DESC
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            TaskFile taskFile = new TaskFile();
            taskFile.setId(rs.getLong("id"));
            taskFile.setFilename(rs.getString("filename"));
            taskFile.setOriginalFilename(rs.getString("original_filename"));
            taskFile.setFilePath(rs.getString("file_path"));
            taskFile.setFileSize(rs.getLong("file_size"));
            taskFile.setContentType(rs.getString("content_type"));
            taskFile.setUploadedAt(rs.getTimestamp("uploaded_at").toLocalDateTime());

            TaskAssignment assignment = new TaskAssignment();
            assignment.setId(rs.getLong("assignment_id"));
            assignment.setTask(task);

            if (rs.getLong("user_id") > 0) {
                User user = new User();
                user.setId(rs.getLong("user_id"));
                user.setEmail(rs.getString("user_email"));
                assignment.setUser(user);
            }

            taskFile.setAssignment(assignment);

            return taskFile;
        }, taskId, taskId);
    }

    @Transactional(readOnly = true)
    @Override
    public boolean canUserUploadToTask(Long taskId, Long userId) {
        String sql = """
            SELECT EXISTS(
                SELECT 1 FROM tasks t
                JOIN meetings m ON t.meeting_id = m.id
                WHERE t.id = ? AND (
                    m.organizer_id = ? OR
                    EXISTS(
                        SELECT 1 FROM task_assignments ta
                        WHERE ta.task_id = t.id AND ta.user_id = ?
                    )
                )
            )
            """;

        try {
            return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                    sql, Boolean.class, taskId, userId, userId));
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public boolean canUserViewTaskFiles(Long taskId, Long userId) {
        String sql = """
            SELECT EXISTS(
                SELECT 1 FROM tasks t
                JOIN meetings m ON t.meeting_id = m.id
                WHERE t.id = ? AND (
                    m.organizer_id = ? OR
                    EXISTS(
                        SELECT 1 FROM task_assignments ta
                        WHERE ta.task_id = t.id AND ta.user_id = ?
                    )
                )
            )
            """;

        try {
            return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                    sql, Boolean.class, taskId, userId, userId));
        } catch (Exception e) {
            return false;
        }
    }

    // ========== PRIVATE METHODS ==========

    private void validateAssignmentAccess(TaskAssignment assignment, Long userId) {
        validateAssignmentAccess(assignment, userId, true);
    }

    private boolean validateAssignmentAccess(TaskAssignment assignment, Long userId, boolean throwException) {
        boolean isOwner = assignment.getUser().getId().equals(userId);

        // Sprawdź czy użytkownik jest organizatorem
        String checkOrganizerSql = """
            SELECT EXISTS(
                SELECT 1 FROM tasks t
                JOIN meetings m ON t.meeting_id = m.id
                WHERE t.id = ? AND m.organizer_id = ?
            )
            """;

        Boolean isOrganizer = jdbcTemplate.queryForObject(
                checkOrganizerSql, Boolean.class, assignment.getTask().getId(), userId);

        if (!isOwner && (isOrganizer == null || !isOrganizer)) {
            if (throwException) {
                throw new RuntimeException("Brak uprawnień do tego przypisania");
            }
            return false;
        }
        return true;
    }

    private void validateFileAccess(TaskFile taskFile, Long userId) {
        validateFileAccess(taskFile.getAssignment(), userId);
    }

    private void validateFileAccess(TaskAssignment assignment, Long userId) {
        validateAssignmentAccess(assignment, userId);
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    private void deleteTaskFilesFromDisk(Task task) {
        try {
            String taskDir = "task_" + task.getId();
            Path taskPath = Paths.get(uploadDir, "tasks", taskDir);

            if (Files.exists(taskPath)) {
                Files.walk(taskPath)
                        .sorted((a, b) -> -a.compareTo(b))
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                log.warn("Could not delete file: {}", path);
                            }
                        });
                log.info("Task files deleted from disk: {}", task.getId());
            }
        } catch (IOException e) {
            log.error("Error deleting task files from disk: {}", e.getMessage());
        }
    }

    private void deleteAssignmentFilesFromDisk(TaskAssignment assignment) {
        try {
            String userEmail = assignment.getUser().getEmail();
            String safeUserDir = userEmail.replace("@", "_at_").replace(".", "_");
            String taskDir = "task_" + assignment.getTask().getId();

            Path userPath = Paths.get(uploadDir, "tasks", taskDir, safeUserDir);

            if (Files.exists(userPath)) {
                Files.walk(userPath)
                        .sorted((a, b) -> -a.compareTo(b))
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                log.warn("Could not delete file: {}", path);
                            }
                        });
                log.info("Assignment files deleted from disk: {}", assignment.getId());
            }
        } catch (IOException e) {
            log.error("Error deleting assignment files from disk: {}", e.getMessage());
        }
    }


    // Dodaj te metody do TaskServiceImpl.java

    @Transactional
    @Override
    public TaskFile uploadFileToAssignment(Long assignmentId, MultipartFile file, Long userId, String description) {
        log.info("Uploading file to assignment: {}, user: {}, filename: {}",
                assignmentId, userId, file.getOriginalFilename());

        TaskAssignment assignment = getAssignmentById(assignmentId);
        validateAssignmentAccess(assignment, userId);

        // Sprawdź czy użytkownik istnieje
        String userSql = "SELECT email FROM users WHERE id = ?";
        String userEmail = jdbcTemplate.queryForObject(userSql, String.class, userId);

        if (userEmail == null) {
            throw new RuntimeException("Użytkownik nie został znaleziony");
        }

        Task task = assignment.getTask();

        return saveFile(file, userId, userEmail, description, task, assignment);
    }

    @Transactional
    @Override
    public TaskFile uploadFileToTask(Long taskId, MultipartFile file, Long userId, String description) {
        log.info("Uploading file to task: {}, user: {}, filename: {}",
                taskId, userId, file.getOriginalFilename());

        Task task = getTaskById(taskId);

        // Sprawdź czy użytkownik jest organizatorem
        String checkOrganizerSql = """
        SELECT EXISTS(
            SELECT 1 FROM tasks t
            JOIN meetings m ON t.meeting_id = m.id
            WHERE t.id = ? AND m.organizer_id = ?
        )
        """;

        Boolean isOrganizer = jdbcTemplate.queryForObject(
                checkOrganizerSql, Boolean.class, taskId, userId);

        if (isOrganizer == null || !isOrganizer) {
            throw new RuntimeException("Tylko organizator może wrzucać pliki bezpośrednio do zadania");
        }

        // Pobierz email użytkownika
        String userSql = "SELECT email FROM users WHERE id = ?";
        String userEmail = jdbcTemplate.queryForObject(userSql, String.class, userId);

        if (userEmail == null) {
            throw new RuntimeException("Użytkownik nie został znaleziony");
        }

        return saveFile(file, userId, userEmail, description, task, null);
    }

    private TaskFile saveFile(MultipartFile file, Long userId, String userEmail, String description,
                              Task task, TaskAssignment assignment) {
        if (file.isEmpty()) {
            throw new RuntimeException("Plik jest pusty");
        }

        try {
            // Walidacja pliku względem ustawień zadania
            validateFileAgainstTaskSettings(file, task);

            // Bezpieczna nazwa katalogu użytkownika
            String safeUserDir = userEmail.replace("@", "_at_").replace(".", "_");
            String taskDir = "task_" + task.getId();

            Path uploadPath = Paths.get(uploadDir, "tasks", taskDir, safeUserDir);
            Files.createDirectories(uploadPath);

            // Unikalna nazwa pliku: timestamp_userId_randomUUID_originalName
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String baseName = originalFilename.contains(".") ?
                    originalFilename.substring(0, originalFilename.lastIndexOf('.')) : originalFilename;
            String timestamp = String.valueOf(System.currentTimeMillis());
            String randomId = UUID.randomUUID().toString().substring(0, 8);

            // Usuń niebezpieczne znaki z nazwy
            String safeBaseName = baseName.replaceAll("[^a-zA-Z0-9._-]", "_");
            String uniqueFilename = timestamp + "_" + userId + "_" + randomId + "_" + safeBaseName + fileExtension;

            Path filePath = uploadPath.resolve(uniqueFilename);

            // Zapisz plik
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Zapisz w bazie używając JDBC
            String insertFileSql = """
            INSERT INTO task_files (
                filename, original_filename, file_path, file_size,
                content_type, assignment_id, task_id, uploaded_by_id,
                uploaded_at, description
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        insertFileSql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, uniqueFilename);
                ps.setString(2, originalFilename);
                ps.setString(3, filePath.toString());
                ps.setLong(4, file.getSize());
                ps.setString(5, file.getContentType());
                ps.setObject(6, assignment != null ? assignment.getId() : null);
                ps.setObject(7, task != null ? task.getId() : null);
                ps.setLong(8, userId);
                ps.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
                ps.setString(10, description);
                return ps;
            }, keyHolder);

            Long fileId = keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;

            TaskFile taskFile = new TaskFile();
            taskFile.setId(fileId);
            taskFile.setFilename(uniqueFilename);
            taskFile.setOriginalFilename(originalFilename);
            taskFile.setFilePath(filePath.toString());
            taskFile.setFileSize(file.getSize());
            taskFile.setContentType(file.getContentType());
            taskFile.setUploadedAt(LocalDateTime.now());

            if (assignment != null) {
                taskFile.setAssignment(assignment);
            }

            if (task != null) {
                taskFile.setTask(task);
            }

            User uploadedBy = new User();
            uploadedBy.setId(userId);
            uploadedBy.setEmail(userEmail);
            taskFile.setUploadedBy(uploadedBy);

            log.info("File uploaded successfully: {}", fileId);
            return taskFile;

        } catch (IOException e) {
            log.error("Error uploading file: {}", e.getMessage());
            throw new RuntimeException("Błąd podczas zapisywania pliku: " + e.getMessage());
        }
    }

    private void validateFileAgainstTaskSettings(MultipartFile file, Task task) {
        // 1. Walidacja rozmiaru
        validateFileSize(file, task);

        // 2. Walidacja rozszerzenia (tylko jeśli zdefiniowano)
        if (task.getAllowedFileTypes() != null && !task.getAllowedFileTypes().trim().isEmpty()) {
            validateFileExtension(file, task.getAllowedFileTypes());
        }
    }

    private void validateFileSize(MultipartFile file, Task task) {
        // Konwertuj MB → bajty
        long maxSizeMB = task.getMaxFileSize() != null && task.getMaxFileSize() > 0
                ? task.getMaxFileSize()
                : 10;

        long maxSizeBytes = maxSizeMB * 1024L * 1024L;

        if (file.getSize() > maxSizeBytes) {
            double fileSizeMB = file.getSize() / (1024.0 * 1024.0);
            throw new RuntimeException(
                    String.format("Plik (%.2f MB) przekracza maksymalny rozmiar %d MB",
                            fileSizeMB, maxSizeMB)
            );
        }
    }

    private void validateFileExtension(MultipartFile file, String allowedTypes) {
        String filename = file.getOriginalFilename();

        if (filename == null || !filename.contains(".")) {
            throw new RuntimeException("Nie można określić typu pliku");
        }

        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();

        // Przygotuj listę dozwolonych rozszerzeń
        Set<String> allowedExtensions = Arrays.stream(allowedTypes.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .map(s -> s.startsWith(".") ? s.substring(1) : s)
                .collect(Collectors.toSet());

        if (!allowedExtensions.contains(extension)) {
            String allowedList = allowedExtensions.stream()
                    .map(ext -> "." + ext)
                    .collect(Collectors.joining(", "));

            throw new RuntimeException(
                    String.format("Plik .%s nie jest dozwolony. Dozwolone typy: %s",
                            extension, allowedList)
            );
        }
    }
}


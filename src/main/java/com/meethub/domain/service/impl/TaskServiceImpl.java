
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final TaskAssignmentRepository assignmentRepository;
    private final TaskFileRepository fileRepository;
    private final UserRepository userRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository participantRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    @Transactional
    public Task createTask(CreateTaskRequest request, Long meetingId, Long organizerId) {
        log.info("Creating task for meeting: {}, organizer: {}", meetingId, organizerId);

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Spotkanie nie zostało znalezione"));

        validateOrganizerPermissions(meeting, organizerId);

        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new RuntimeException("Użytkownik nie został znaleziony"));

        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(TaskStatus.TODO)
                .deadline(request.getDeadline())
                .meeting(meeting)
                .createdBy(organizer)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

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
        validateOrganizerPermissions(task.getMeeting(), userId);

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setDeadline(request.getDeadline());
        task.setAllowSelfAssignment(request.getAllowSelfAssignment());
//        task.setMaxFilesPerUser(request.getMaxFilesPerUser());
//        task.setMaxFileSize(request.getMaxFileSize());
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
        validateOrganizerPermissions(task.getMeeting(), userId);

        // Usuń pliki fizycznie z dysku
        deleteTaskFilesFromDisk(task);

        taskRepository.delete(task);
        log.info("Task deleted successfully: {}", taskId);
    }

    @Override
    @Transactional
    public TaskAssignment assignTask(Long taskId, Long userId, Long organizerId) {
        log.info("Assigning task: {} to user: {}, by organizer: {}", taskId, userId, organizerId);

        Task task = getTaskById(taskId);
        validateOrganizerPermissions(task.getMeeting(), organizerId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Użytkownik nie został znaleziony"));

        // Sprawdź czy użytkownik jest uczestnikiem spotkania
        if (!participantRepository.existsByMeetingIdAndUserId(task.getMeeting().getId(), userId)) {
            throw new RuntimeException("Użytkownik nie jest uczestnikiem spotkania");
        }

        // Sprawdź czy już nie jest przypisany
        assignmentRepository.findByTaskIdAndUserId(taskId, userId)
                .ifPresent(assignment -> {
                    throw new RuntimeException("Użytkownik jest już przypisany do tego zadania");
                });

        TaskAssignment assignment = TaskAssignment.builder()
                .task(task)
                .user(user)
                .status(AssignmentStatus.ASSIGNED)
                .assignedAt(LocalDateTime.now())
                .build();

        TaskAssignment savedAssignment = assignmentRepository.save(assignment);
        log.info("Task assigned successfully: {}", savedAssignment.getId());
        return savedAssignment;
    }

    @Override
    @Transactional
    public TaskAssignment assignTaskToCurrentUser(Long taskId, Long userId) {
        log.info("User {} self-assigning to task: {}", userId, taskId);

        Task task = getTaskById(taskId);

        // Sprawdź czy użytkownik jest uczestnikiem spotkania
        if (!participantRepository.existsByMeetingIdAndUserId(task.getMeeting().getId(), userId)) {
            throw new RuntimeException("Nie jesteś uczestnikiem tego spotkania");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Użytkownik nie został znaleziony"));

        // Sprawdź czy już nie jest przypisany
        assignmentRepository.findByTaskIdAndUserId(taskId, userId)
                .ifPresent(assignment -> {
                    throw new RuntimeException("Jesteś już przypisany do tego zadania");
                });

        TaskAssignment assignment = TaskAssignment.builder()
                .task(task)
                .user(user)
                .status(AssignmentStatus.ASSIGNED)
                .assignedAt(LocalDateTime.now())
                .build();

        TaskAssignment savedAssignment = assignmentRepository.save(assignment);
        log.info("Self-assignment successful: {}", savedAssignment.getId());
        return savedAssignment;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskAssignment> getUserAssignments(Long userId) {
        log.debug("Getting assignments for user: {}", userId);
        return assignmentRepository.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskAssignment> getTaskAssignments(Long taskId) {
        log.debug("Getting assignments for task: {}", taskId);
        return assignmentRepository.findByTaskId(taskId);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskAssignment getAssignmentById(Long assignmentId) {
        log.debug("Getting assignment by ID: {}", assignmentId);
        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Przypisanie nie zostało znalezione"));
    }

    @Override
    @Transactional
    public TaskAssignment updateAssignmentStatus(Long assignmentId, AssignmentStatus status, Long userId) {
        log.info("Updating assignment status: {}, status: {}, user: {}", assignmentId, status, userId);

        TaskAssignment assignment = getAssignmentById(assignmentId);
        validateAssignmentAccess(assignment, userId);

        assignment.setStatus(status);
        if (status == AssignmentStatus.COMPLETED) {
            assignment.setCompletedAt(LocalDateTime.now());
        } else {
            assignment.setCompletedAt(null);
        }

        TaskAssignment updatedAssignment = assignmentRepository.save(assignment);
        log.info("Assignment status updated successfully: {}", assignmentId);
        return updatedAssignment;
    }

    @Override
    @Transactional
    public TaskAssignment updateAssignmentComment(Long assignmentId, String comment, Long userId) {
        log.info("Updating assignment comment: {}, user: {}", assignmentId, userId);

        TaskAssignment assignment = getAssignmentById(assignmentId);
        validateAssignmentAccess(assignment, userId);

        assignment.setComment(comment);
        TaskAssignment updatedAssignment = assignmentRepository.save(assignment);
        log.info("Assignment comment updated successfully: {}", assignmentId);
        return updatedAssignment;
    }

    @Override
    @Transactional
    public void removeAssignment(Long assignmentId, Long userId) {
        log.info("Removing assignment: {}, user: {}", assignmentId, userId);

        TaskAssignment assignment = getAssignmentById(assignmentId);

        // Może usunąć organizator lub właściciel przypisania
        boolean isOrganizer = assignment.getTask().getMeeting().getOrganizer().getId().equals(userId);
        boolean isOwner = assignment.getUser().getId().equals(userId);

        if (!isOrganizer && !isOwner) {
            throw new RuntimeException("Brak uprawnień do usunięcia przypisania");
        }

        // Usuń pliki fizycznie z dysku
        deleteAssignmentFilesFromDisk(assignment);

        assignmentRepository.delete(assignment);
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

            TaskFile taskFile = TaskFile.builder()
                    .filename(uniqueFilename)
                    .originalFilename(originalFilename)
                    .filePath(filePath.toString())
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .assignment(assignment)
                    .uploadedAt(LocalDateTime.now())
                    .build();

            TaskFile savedFile = fileRepository.save(taskFile);
            log.info("File uploaded successfully: {}", savedFile.getId());
            return savedFile;

        } catch (IOException e) {
            log.error("Error uploading file: {}", e.getMessage());
            throw new RuntimeException("Błąd podczas zapisywania pliku: " + e.getMessage());
        }
    }

//    @Override
//    @Transactional(readOnly = true)
//    public Resource downloadFile(Long fileId, Long userId) {
//        log.debug("Downloading file: {}, user: {}", fileId, userId);
//
//        TaskFile taskFile = fileRepository.findById(fileId)
//                .orElseThrow(() -> new RuntimeException("Plik nie został znaleziony"));
//
//        validateFileAccess(taskFile, userId);
//
//        try {
//            Path filePath = Paths.get(taskFile.getFilePath());
//            Resource resource = new UrlResource(filePath.toUri());
//
//            if (resource.exists() && resource.isReadable()) {
//                log.info("File downloaded successfully: {}", fileId);
//                return resource;
//            } else {
//                throw new RuntimeException("Plik nie istnieje lub nie można go odczytać");
//            }
//        } catch (MalformedURLException e) {
//            log.error("Error downloading file: {}", e.getMessage());
//            throw new RuntimeException("Błąd podczas pobierania pliku: " + e.getMessage());
//        }
//    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskFile> getAssignmentFiles(Long assignmentId, Long userId) {
        log.debug("Getting files for assignment: {}, user: {}", assignmentId, userId);

        TaskAssignment assignment = getAssignmentById(assignmentId);
        validateFileAccess(assignment, userId);

        return fileRepository.findByAssignmentId(assignmentId);
    }

    @Override
    @Transactional
    public void deleteFile(Long fileId, Long userId) {
        log.info("Deleting file: {}, user: {}", fileId, userId);

        TaskFile taskFile = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("Plik nie został znaleziony"));

        validateFileAccess(taskFile, userId);

        try {
            // Usuń plik fizycznie
            Path filePath = Paths.get(taskFile.getFilePath());
            Files.deleteIfExists(filePath);

            // Usuń z bazy
            fileRepository.delete(taskFile);
            log.info("File deleted successfully: {}", fileId);
        } catch (IOException e) {
            log.error("Error deleting file: {}", e.getMessage());
            throw new RuntimeException("Błąd podczas usuwania pliku: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskAssignment> getAssignmentsByStatus(Long taskId, AssignmentStatus status) {
        log.debug("Getting assignments for task: {} with status: {}", taskId, status);
        return assignmentRepository.findByTaskId(taskId).stream()
                .filter(assignment -> assignment.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Long countCompletedAssignments(Long taskId) {
        return getAssignmentsByStatus(taskId, AssignmentStatus.COMPLETED).stream().count();
    }

    @Override
    @Transactional(readOnly = true)
    public Long countTotalAssignments(Long taskId) {
        return assignmentRepository.findByTaskId(taskId).stream().count();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUserManageTask(Long taskId, Long userId) {
        try {
            Task task = getTaskById(taskId);
            return task.getMeeting().getOrganizer().getId().equals(userId);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUserAccessAssignment(Long assignmentId, Long userId) {
        try {
            TaskAssignment assignment = getAssignmentById(assignmentId);
            return validateAssignmentAccess(assignment, userId, false);
        } catch (Exception e) {
            return false;
        }
    }

    // ========== PRIVATE METHODS ==========

    private void validateOrganizerPermissions(Meeting meeting, Long userId) {
        if (!meeting.getOrganizer().getId().equals(userId)) {
            throw new RuntimeException("Tylko organizator może wykonać tę akcję");
        }
    }

    private void validateAssignmentAccess(TaskAssignment assignment, Long userId) {
        validateAssignmentAccess(assignment, userId, true);
    }

    private boolean validateAssignmentAccess(TaskAssignment assignment, Long userId, boolean throwException) {
        boolean isOwner = assignment.getUser().getId().equals(userId);
        boolean isOrganizer = assignment.getTask().getMeeting().getOrganizer().getId().equals(userId);

        if (!isOwner && !isOrganizer) {
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
                        .sorted((a, b) -> -a.compareTo(b)) // usuń najpierw pliki potem katalogi
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

    @Transactional(readOnly = true)
    @Override
    public MeetingTasksResponse getMeetingTasksForUser(Long meetingId, Long userId) {
        // Pobierz spotkanie
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Spotkanie nie zostało znalezione"));

        // Sprawdź, czy użytkownik jest organizatorem
        boolean isOrganizer = meeting.getOrganizer().getId().equals(userId);

        // Pobierz zadania
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
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Spotkanie nie zostało znalezione"));

        if (!meeting.getOrganizer().getId().equals(userId)) {
            throw new RuntimeException("Tylko organizator może tworzyć zadania");
        }

        return MeetingTaskFormResponse.builder()
                .meeting(meeting)
                .createTaskRequest(new CreateTaskRequest())
                .build();
    }

    // TaskServiceImpl.java
    @Transactional(readOnly = true)
    @Override
    public MeetingTaskDetailsResponse getTaskDetailsForUser(Long meetingId, Long taskId, Long userId) {
        Task task = getTaskById(taskId);

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Spotkanie nie zostało znalezione"));

        boolean isOrganizer = meeting.getOrganizer().getId().equals(userId);
        boolean canAccess = isOrganizer || task.getAssignments().stream()
                .anyMatch(a -> a.getUser().getId().equals(userId));

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

    // TaskServiceImpl.java
    @Override
    @Transactional(readOnly = true)
    public MeetingTaskEditResponse getTaskForEditing(Long meetingId, Long taskId, Long userId) {
        Task task = getTaskById(taskId);

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Spotkanie nie zostało znalezione"));

        if (!meeting.getOrganizer().getId().equals(userId)) {
            throw new RuntimeException("Tylko organizator może edytować zadania");
        }

        String formattedDeadline = task.getDeadline() != null ? task.getDeadline().toString().replace("T", " ") : null;

        return MeetingTaskEditResponse.builder()
                .meeting(meeting)
                .task(task)
                .formattedDeadline(formattedDeadline)
                .build();
    }

    // TaskServiceImpl.java
    @Override
    @Transactional(readOnly = true)
    public MeetingTaskAssignmentsResponse getTaskAssignmentsForUser(Long meetingId, Long taskId, Long userId) {
        Task task = getTaskById(taskId);

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Spotkanie nie zostało znalezione"));

        if (!meeting.getOrganizer().getId().equals(userId)) {
            throw new RuntimeException("Tylko organizator może przypisywać użytkowników");
        }

        List<User> assignedUsers = assignmentRepository.findAssignedUsersByTaskId(taskId);

        List<User> availableUsers = participantRepository.findAvailableUsersForTask(meetingId, taskId);


        List<TaskAssignment> assignments = assignmentRepository.findByTaskId(taskId);

        return MeetingTaskAssignmentsResponse.builder()
                .meeting(meeting)
                .task(task)
                .availableUsers(availableUsers)
                .assignedUsers(assignedUsers)
                .assignments(assignments)
                .build();
    }


// TaskServiceImpl.java - dodaj te metody

    @Transactional(readOnly = true)
    @Override
    public TaskFile getFileById(Long fileId) {
        log.debug("Getting file by ID: {}", fileId);
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("Plik nie został znaleziony"));
    }
//
//    @Override
//    @Transactional
//    public TaskFile uploadFileToAssignment(Long assignmentId, MultipartFile file, Long userId, String description) {
//        log.info("Uploading file to assignment: {}, user: {}, filename: {}",
//                assignmentId, userId, file.getOriginalFilename());
//
//        TaskAssignment assignment = getAssignmentById(assignmentId);
//        validateAssignmentAccess(assignment, userId);
//
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new RuntimeException("Użytkownik nie został znaleziony"));
//
//        Task task = assignment.getTask();
//
//        return saveFile(file, user, description, task, assignment);
//    }

    @Transactional
    @Override
    public TaskFile uploadFileToTask(Long taskId, MultipartFile file, Long userId, String description) {
        log.info("Uploading file to task: {}, user: {}, filename: {}",
                taskId, userId, file.getOriginalFilename());

        Task task = getTaskById(taskId);
        Meeting meeting = task.getMeeting();

        // Tylko organizator może wrzucać pliki bezpośrednio do zadania
        if (!meeting.getOrganizer().getId().equals(userId)) {
            throw new RuntimeException("Tylko organizator może wrzucać pliki bezpośrednio do zadania");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Użytkownik nie został znaleziony"));

        return saveFile(file, user, description, task, null);
    }

    private TaskFile saveFile(MultipartFile file, User user, String description, Task task, TaskAssignment assignment) {
        if (file.isEmpty()) {
            throw new RuntimeException("Plik jest pusty");
        }

        try {
            // Bezpieczna nazwa katalogu użytkownika
            String userEmail = user.getEmail();
            String safeUserDir = userEmail.replace("@", "_at_").replace(".", "_");
            String taskDir = "task_" + task.getId();

            Path uploadPath = Paths.get(uploadDir, "tasks", taskDir, safeUserDir);
            Files.createDirectories(uploadPath);

            // Unikalna nazwa pliku: timestamp_userId_randomUUID_originalName
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String baseName = originalFilename.substring(0, originalFilename.lastIndexOf('.'));
            String timestamp = String.valueOf(System.currentTimeMillis());
            String randomId = UUID.randomUUID().toString().substring(0, 8);

            // Usuń niebezpieczne znaki z nazwy
            String safeBaseName = baseName.replaceAll("[^a-zA-Z0-9._-]", "_");
            String uniqueFilename = timestamp + "_" + user.getId() + "_" + randomId + "_" + safeBaseName + fileExtension;

            Path filePath = uploadPath.resolve(uniqueFilename);

            // Walidacja na podstawie ustawień zadania
//            validateFileAgainstTaskSettings(file, task);

            // Zapisz plik
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Utwórz rekord w bazie
            TaskFile taskFile = TaskFile.builder()
                    .filename(uniqueFilename)
                    .originalFilename(originalFilename)
                    .filePath(filePath.toString())
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .assignment(assignment)
                    .uploadedAt(LocalDateTime.now())
                    .build();

            TaskFile savedFile = fileRepository.save(taskFile);
            log.info("File uploaded successfully: {}", savedFile.getId());
            return savedFile;

        } catch (IOException e) {
            log.error("Error uploading file: {}", e.getMessage());
            throw new RuntimeException("Błąd podczas zapisywania pliku: " + e.getMessage());
        }
    }

//    private void validateFileAgainstTaskSettings(MultipartFile file, Task task) {
//        // Walidacja rozmiaru pliku
//        if (task.getMaxFileSize() != null && task.getMaxFileSize() > 0) {
//            long maxSizeBytes = task.getMaxFileSize();
//            if (file.getSize() > maxSizeBytes) {
//                throw new RuntimeException(
//                        String.format("Plik przekracza maksymalny rozmiar %d MB",
//                                task.getMaxFileSize() / (1024 * 1024))
//                );
//            }
//        }
//
//        // Walidacja typu pliku
//        if (task.getAllowedFileTypes() != null && !task.getAllowedFileTypes().isEmpty()) {
//            String contentType = file.getContentType();
//            String originalFilename = file.getOriginalFilename();
//
//            if (contentType == null) {
//                contentType = "application/octet-stream";
//            }
//
//            // Sprawdź czy typ jest dozwolony
//            String[] allowedTypes = task.getAllowedFileTypes().split(",");
//            boolean allowed = false;
//
//            for (String allowedType : allowedTypes) {
//                String trimmedType = allowedType.trim().toLowerCase();
//
//                // Sprawdź typ MIME
//                if (contentType.toLowerCase().contains(trimmedType)) {
//                    allowed = true;
//                    break;
//                }
//
//                // Sprawdź rozszerzenie
//                if (originalFilename != null &&
//                        originalFilename.toLowerCase().endsWith("." + trimmedType)) {
//                    allowed = true;
//                    break;
//                }
//            }
//
//            if (!allowed) {
//                throw new RuntimeException(
//                        String.format("Typ pliku '%s' nie jest dozwolony. Dozwolone typy: %s",
//                                contentType, task.getAllowedFileTypes())
//                );
//            }
//        }
//    }

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

//    @Override
//    @Transactional(readOnly = true)
//    public List<TaskFile> getTaskFiles(Long taskId, Long userId) {
//        log.debug("Getting all files for task: {}, user: {}", taskId, userId);
//
//        Task task = getTaskById(taskId);
//
//        if (!canUserViewTaskFiles(taskId, userId)) {
//            throw new RuntimeException("Brak uprawnień do przeglądania plików tego zadania");
//        }
//
//        // Pobierz wszystkie pliki związane z tym zadaniem
//        // (zarówno powiązane z assignment, jak i bezpośrednio z task)
//        List<TaskFile> files = new ArrayList<>();
//
//        // Pliki przypisań
//        List<TaskAssignment> assignments = assignmentRepository.findByTaskId(taskId);
//        for (TaskAssignment assignment : assignments) {
//            files.addAll(fileRepository.findByAssignmentId(assignment.getId()));
//        }
//
//        // Pliki bezpośrednio powiązane z zadaniem (dla organizatora)
//        files.addAll(fileRepository.findByTaskIdAndAssignmentIsNull(taskId));
//
//        return files;
//    }

//    @Transactional(readOnly = true)
//    @Override
//    public List<TaskFile> getAllTaskFilesForOrganizer(Long taskId, Long userId) {
//        log.debug("Getting all files for task (organizer view): {}, user: {}", taskId, userId);
//
//        Task task = getTaskById(taskId);
//        Meeting meeting = task.getMeeting();
//
//        if (!meeting.getOrganizer().getId().equals(userId)) {
//            throw new RuntimeException("Tylko organizator może przeglądać wszystkie pliki zadania");
//        }
//
//        return fileRepository.findByTaskId(taskId);
//    }

//    @Override
//    @Transactional
//    public void deleteFile(Long fileId, Long userId) {
//        log.info("Deleting file: {}, user: {}", fileId, userId);
//
//        TaskFile taskFile = getFileById(fileId);
//
//        // Sprawdź uprawnienia: organizator lub osoba która wrzuciła plik
//        Task task = taskFile.getAssignment() != null ?
//                taskFile.getAssignment().getTask() :
//                getTaskById(taskFile.getId()); // potrzebujesz relacji TaskFile -> Task
//
//        Meeting meeting = task.getMeeting();
//        boolean isOrganizer = meeting.getOrganizer().getId().equals(userId);
//        boolean isUploader = taskFile.getUploadedBy().getId().equals(userId);
//
//        if (!isOrganizer && !isUploader) {
//            throw new RuntimeException("Brak uprawnień do usunięcia tego pliku");
//        }
//
//        try {
//            // Usuń plik fizycznie
//            Path filePath = Paths.get(taskFile.getFilePath());
//            Files.deleteIfExists(filePath);
//
//            // Usuń z bazy
//            fileRepository.delete(taskFile);
//            log.info("File deleted successfully: {}", fileId);
//        } catch (IOException e) {
//            log.error("Error deleting file: {}", e.getMessage());
//            throw new RuntimeException("Błąd podczas usuwania pliku: " + e.getMessage());
//        }
//    }

    @Transactional(readOnly = true)
    @Override
    public boolean canUserUploadToTask(Long taskId, Long userId) {
        try {
            Task task = getTaskById(taskId);
            Meeting meeting = task.getMeeting();

            // Organizator może zawsze
            if (meeting.getOrganizer().getId().equals(userId)) {
                return true;
            }

            // Użytkownik przypisany do zadania może
            boolean isAssigned = assignmentRepository.findByTaskIdAndUserId(taskId, userId).isPresent();
            return isAssigned;

        } catch (Exception e) {
            return false;
        }
    }

//    @Transactional(readOnly = true)
//    @Override
//    public boolean canUserViewTaskFiles(Long taskId, Long userId) {
//        try {
//            Task task = getTaskById(taskId);
//            Meeting meeting = task.getMeeting();
//
//            // Organizator może zawsze
//            if (meeting.getOrganizer().getId().equals(userId)) {
//                return true;
//            }
//
//            // Użytkownik przypisany do zadania może
//            boolean isAssigned = assignmentRepository.findByTaskIdAndUserId(taskId, userId).isPresent();
//            return isAssigned;
//
//        } catch (Exception e) {
//            return false;
//        }
//    }

}









//package com.meethub.domain.service.impl;
//
//import com.meethub.domain.model.entity.*;
//import com.meethub.domain.model.enums.AssignmentStatus;
//import com.meethub.domain.model.enums.TaskStatus;
//import com.meethub.domain.model.request.CreateTaskRequest;
//import com.meethub.domain.model.request.UpdateTaskRequest;
//import com.meethub.domain.model.response.*;
//import com.meethub.domain.repository.jpa.*;
//import com.meethub.domain.service.TaskService;
//import jakarta.validation.Valid;
//import jakarta.validation.constraints.NotNull;
//import jakarta.validation.constraints.Positive;
//import jakarta.validation.constraints.Size;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.core.io.Resource;
//import org.springframework.core.io.UrlResource;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.multipart.MultipartFile;
//
//
//import java.io.IOException;
//import java.net.MalformedURLException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.StandardCopyOption;
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.UUID;
//import java.util.stream.Collectors;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class TaskServiceImpl implements TaskService {
//
//    private final TaskRepository taskRepository;
//    private final TaskAssignmentRepository assignmentRepository;
//    private final TaskFileRepository fileRepository;
//    private final UserRepository userRepository;
//    private final MeetingRepository meetingRepository;
//    private final MeetingParticipantRepository participantRepository;
//
//    @Value("${app.upload.dir:uploads}")
//    private String uploadDir;
//
//    @Override
//    @Transactional
//    public Task createTask(
//            @Valid @NotNull(message = "Żądanie utworzenia zadania nie może być puste")
//            CreateTaskRequest request,
//
//            @NotNull(message = "Identyfikator spotkania nie może być pusty")
//            @Positive(message = "Identyfikator spotkania musi być liczbą dodatnią")
//            Long meetingId,
//
//            @NotNull(message = "Identyfikator organizatora nie może być pusty")
//            @Positive(message = "Identyfikator organizatora musi być liczbą dodatnią")
//            Long organizerId) {
//
//        log.info("Creating task for meeting: {}, organizer: {}", meetingId, organizerId);
//
//        validateMeetingAndOrganizerIds(meetingId, organizerId);
//        validateCreateTaskRequest(request);
//
//        Meeting meeting = meetingRepository.findById(meetingId)
//                .orElseThrow(() -> new RuntimeException("Spotkanie nie zostało znalezione"));
//
//        validateOrganizerPermissions(meeting, organizerId);
//
//        User organizer = userRepository.findById(organizerId)
//                .orElseThrow(() -> new RuntimeException("Użytkownik nie został znaleziony"));
//
//        Task task = Task.builder()
//                .title(request.getTitle())
//                .description(request.getDescription())
//                .status(TaskStatus.TODO)
//                .deadline(request.getDeadline())
//                .meeting(meeting)
//                .createdBy(organizer)
//                .createdAt(LocalDateTime.now())
//                .updatedAt(LocalDateTime.now())
//                .build();
//
//        Task savedTask = taskRepository.save(task);
//        log.info("Task created successfully: {}", savedTask.getId());
//        return savedTask;
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<Task> getMeetingTasks(
//            @NotNull(message = "Identyfikator spotkania nie może być pusty")
//            @Positive(message = "Identyfikator spotkania musi być liczbą dodatnią")
//            Long meetingId) {
//
//        log.debug("Getting tasks for meeting: {}", meetingId);
//        validateMeetingId(meetingId);
//        return taskRepository.findByMeetingId(meetingId);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<Task> getUserCreatedTasks(
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//
//        log.debug("Getting tasks created by user: {}", userId);
//        validateUserId(userId);
//        return taskRepository.findByCreatedById(userId);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public Task getTaskById(
//            @NotNull(message = "Identyfikator zadania nie może być pusty")
//            @Positive(message = "Identyfikator zadania musi być liczbą dodatnią")
//            Long taskId) {
//
//        log.debug("Getting task by ID: {}", taskId);
//        validateTaskId(taskId);
//        return taskRepository.findById(taskId)
//                .orElseThrow(() -> new RuntimeException("Zadanie nie zostało znalezione"));
//    }
//
//    @Override
//    @Transactional
//    public Task updateTaskWithRequest(
//            @NotNull(message = "Identyfikator zadania nie może być pusty")
//            @Positive(message = "Identyfikator zadania musi być liczbą dodatnią")
//            Long taskId,
//
//            @Valid @NotNull(message = "Żądanie aktualizacji zadania nie może być puste")
//            UpdateTaskRequest request,
//
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//
//        log.info("Updating task: {}, user: {}", taskId, userId);
//
//        validateTaskAndUserIds(taskId, userId);
//        validateUpdateTaskRequest(request);
//
//        Task task = getTaskById(taskId);
//        validateOrganizerPermissions(task.getMeeting(), userId);
//
//        task.setTitle(request.getTitle());
//        task.setDescription(request.getDescription());
//        task.setDeadline(request.getDeadline());
//        task.setAllowSelfAssignment(request.getAllowSelfAssignment());
//        task.setUpdatedAt(LocalDateTime.now());
//
//        if (request.getAllowedFileTypes() != null) {
//            task.setAllowedFileTypes(request.getAllowedFileTypesAsString());
//        }
//
//        Task updatedTask = taskRepository.save(task);
//        log.info("Task updated successfully: {}", taskId);
//        return updatedTask;
//    }
//
//    @Override
//    @Transactional
//    public void deleteTask(
//            @NotNull(message = "Identyfikator zadania nie może być pusty")
//            @Positive(message = "Identyfikator zadania musi być liczbą dodatnią")
//            Long taskId,
//
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//
//        log.info("Deleting task: {}, user: {}", taskId, userId);
//
//        validateTaskAndUserIds(taskId, userId);
//
//        Task task = getTaskById(taskId);
//        validateOrganizerPermissions(task.getMeeting(), userId);
//
//        // Usuń pliki fizycznie z dysku
//        deleteTaskFilesFromDisk(task);
//
//        taskRepository.delete(task);
//        log.info("Task deleted successfully: {}", taskId);
//    }
//
//    @Override
//    @Transactional
//    public TaskAssignment assignTask(
//            @NotNull(message = "Identyfikator zadania nie może być pusty")
//            @Positive(message = "Identyfikator zadania musi być liczbą dodatnią")
//            Long taskId,
//
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId,
//
//            @NotNull(message = "Identyfikator organizatora nie może być pusty")
//            @Positive(message = "Identyfikator organizatora musi być liczbą dodatnią")
//            Long organizerId) {
//
//        log.info("Assigning task: {} to user: {}, by organizer: {}", taskId, userId, organizerId);
//
//        validateTaskUserAndOrganizerIds(taskId, userId, organizerId);
//
//        Task task = getTaskById(taskId);
//        validateOrganizerPermissions(task.getMeeting(), organizerId);
//
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new RuntimeException("Użytkownik nie został znaleziony"));
//
//        // Sprawdź czy użytkownik jest uczestnikiem spotkania
//        if (!participantRepository.existsByMeetingIdAndUserId(task.getMeeting().getId(), userId)) {
//            throw new RuntimeException("Użytkownik nie jest uczestnikiem spotkania");
//        }
//
//        // Sprawdź czy już nie jest przypisany
//        assignmentRepository.findByTaskIdAndUserId(taskId, userId)
//                .ifPresent(assignment -> {
//                    throw new RuntimeException("Użytkownik jest już przypisany do tego zadania");
//                });
//
//        TaskAssignment assignment = TaskAssignment.builder()
//                .task(task)
//                .user(user)
//                .status(AssignmentStatus.ASSIGNED)
//                .assignedAt(LocalDateTime.now())
//                .build();
//
//        TaskAssignment savedAssignment = assignmentRepository.save(assignment);
//        log.info("Task assigned successfully: {}", savedAssignment.getId());
//        return savedAssignment;
//    }
//
//    @Override
//    @Transactional
//    public TaskAssignment assignTaskToCurrentUser(
//            @NotNull(message = "Identyfikator zadania nie może być pusty")
//            @Positive(message = "Identyfikator zadania musi być liczbą dodatnią")
//            Long taskId,
//
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//
//        log.info("User {} self-assigning to task: {}", userId, taskId);
//
//        validateTaskAndUserIds(taskId, userId);
//
//        Task task = getTaskById(taskId);
//
//        // Sprawdź czy użytkownik jest uczestnikiem spotkania
//        if (!participantRepository.existsByMeetingIdAndUserId(task.getMeeting().getId(), userId)) {
//            throw new RuntimeException("Nie jesteś uczestnikiem tego spotkania");
//        }
//
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new RuntimeException("Użytkownik nie został znaleziony"));
//
//        // Sprawdź czy już nie jest przypisany
//        assignmentRepository.findByTaskIdAndUserId(taskId, userId)
//                .ifPresent(assignment -> {
//                    throw new RuntimeException("Jesteś już przypisany do tego zadania");
//                });
//
//        TaskAssignment assignment = TaskAssignment.builder()
//                .task(task)
//                .user(user)
//                .status(AssignmentStatus.ASSIGNED)
//                .assignedAt(LocalDateTime.now())
//                .build();
//
//        TaskAssignment savedAssignment = assignmentRepository.save(assignment);
//        log.info("Self-assignment successful: {}", savedAssignment.getId());
//        return savedAssignment;
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<TaskAssignment> getUserAssignments(
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//
//        log.debug("Getting assignments for user: {}", userId);
//        validateUserId(userId);
//        return assignmentRepository.findByUserId(userId);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<TaskAssignment> getTaskAssignments(
//            @NotNull(message = "Identyfikator zadania nie może być pusty")
//            @Positive(message = "Identyfikator zadania musi być liczbą dodatnią")
//            Long taskId) {
//
//        log.debug("Getting assignments for task: {}", taskId);
//        validateTaskId(taskId);
//        return assignmentRepository.findByTaskId(taskId);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public TaskAssignment getAssignmentById(
//            @NotNull(message = "Identyfikator przypisania nie może być pusty")
//            @Positive(message = "Identyfikator przypisania musi być liczbą dodatnią")
//            Long assignmentId) {
//
//        log.debug("Getting assignment by ID: {}", assignmentId);
//        validateAssignmentId(assignmentId);
//        return assignmentRepository.findById(assignmentId)
//                .orElseThrow(() -> new RuntimeException("Przypisanie nie zostało znalezione"));
//    }
//
//    @Override
//    @Transactional
//    public TaskAssignment updateAssignmentStatus(
//            @NotNull(message = "Identyfikator przypisania nie może być pusty")
//            @Positive(message = "Identyfikator przypisania musi być liczbą dodatnią")
//            Long assignmentId,
//
//            @NotNull(message = "Status przypisania nie może być pusty")
//            AssignmentStatus status,
//
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//
//        log.info("Updating assignment status: {}, status: {}, user: {}", assignmentId, status, userId);
//
//        validateAssignmentAndUserIds(assignmentId, userId);
//        validateAssignmentStatus(status);
//
//        TaskAssignment assignment = getAssignmentById(assignmentId);
//        validateAssignmentAccess(assignment, userId);
//
//        assignment.setStatus(status);
//        if (status == AssignmentStatus.COMPLETED) {
//            assignment.setCompletedAt(LocalDateTime.now());
//        } else {
//            assignment.setCompletedAt(null);
//        }
//
//        TaskAssignment updatedAssignment = assignmentRepository.save(assignment);
//        log.info("Assignment status updated successfully: {}", assignmentId);
//        return updatedAssignment;
//    }
//
//    @Override
//    @Transactional
//    public TaskAssignment updateAssignmentComment(
//            @NotNull(message = "Identyfikator przypisania nie może być pusty")
//            @Positive(message = "Identyfikator przypisania musi być liczbą dodatnią")
//            Long assignmentId,
//
//            @Size(max = 1000, message = "Komentarz nie może przekraczać 1000 znaków")
//            String comment,
//
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//
//        log.info("Updating assignment comment: {}, user: {}", assignmentId, userId);
//
//        validateAssignmentAndUserIds(assignmentId, userId);
//        validateComment(comment);
//
//        TaskAssignment assignment = getAssignmentById(assignmentId);
//        validateAssignmentAccess(assignment, userId);
//
//        assignment.setComment(comment);
//        TaskAssignment updatedAssignment = assignmentRepository.save(assignment);
//        log.info("Assignment comment updated successfully: {}", assignmentId);
//        return updatedAssignment;
//    }
//
//    @Override
//    @Transactional
//    public void removeAssignment(
//            @NotNull(message = "Identyfikator przypisania nie może być pusty")
//            @Positive(message = "Identyfikator przypisania musi być liczbą dodatnią")
//            Long assignmentId,
//
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//
//        log.info("Removing assignment: {}, user: {}", assignmentId, userId);
//
//        validateAssignmentAndUserIds(assignmentId, userId);
//
//        TaskAssignment assignment = getAssignmentById(assignmentId);
//
//        // Może usunąć organizator lub właściciel przypisania
//        boolean isOrganizer = assignment.getTask().getMeeting().getOrganizer().getId().equals(userId);
//        boolean isOwner = assignment.getUser().getId().equals(userId);
//
//        if (!isOrganizer && !isOwner) {
//            throw new RuntimeException("Brak uprawnień do usunięcia przypisania");
//        }
//
//        // Usuń pliki fizycznie z dysku
//        deleteAssignmentFilesFromDisk(assignment);
//
//        assignmentRepository.delete(assignment);
//        log.info("Assignment removed successfully: {}", assignmentId);
//    }
//
//    @Override
//    @Transactional
//    public TaskFile uploadFile(
//            @NotNull(message = "Identyfikator przypisania nie może być pusty")
//            @Positive(message = "Identyfikator przypisania musi być liczbą dodatnią")
//            Long assignmentId,
//
//            @NotNull(message = "Plik nie może być pusty")
//            MultipartFile file,
//
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//
//        log.info("Uploading file for assignment: {}, user: {}", assignmentId, userId);
//
//        validateAssignmentAndUserIds(assignmentId, userId);
//        validateFile(file);
//
//        TaskAssignment assignment = getAssignmentById(assignmentId);
//        validateAssignmentAccess(assignment, userId);
//
//        if (file.isEmpty()) {
//            throw new RuntimeException("Plik jest pusty");
//        }
//
//        try {
//            // Bezpieczna nazwa katalogu użytkownika
//            String userEmail = assignment.getUser().getEmail();
//            String safeUserDir = userEmail.replace("@", "_at_").replace(".", "_");
//            String taskDir = "task_" + assignment.getTask().getId();
//
//            Path uploadPath = Paths.get(uploadDir, "tasks", taskDir, safeUserDir);
//            Files.createDirectories(uploadPath);
//
//            // Unikalna nazwa pliku
//            String originalFilename = file.getOriginalFilename();
//            String fileExtension = getFileExtension(originalFilename);
//            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
//            Path filePath = uploadPath.resolve(uniqueFilename);
//
//            // Zapisz plik
//            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
//
//            TaskFile taskFile = TaskFile.builder()
//                    .filename(uniqueFilename)
//                    .originalFilename(originalFilename)
//                    .filePath(filePath.toString())
//                    .fileSize(file.getSize())
//                    .contentType(file.getContentType())
//                    .assignment(assignment)
//                    .uploadedAt(LocalDateTime.now())
//                    .build();
//
//            TaskFile savedFile = fileRepository.save(taskFile);
//            log.info("File uploaded successfully: {}", savedFile.getId());
//            return savedFile;
//
//        } catch (IOException e) {
//            log.error("Error uploading file: {}", e.getMessage());
//            throw new RuntimeException("Błąd podczas zapisywania pliku: " + e.getMessage());
//        }
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public Resource downloadFile(
//            @NotNull(message = "Identyfikator pliku nie może być pusty")
//            @Positive(message = "Identyfikator pliku musi być liczbą dodatnią")
//            Long fileId,
//
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//
//        log.debug("Downloading file: {}, user: {}", fileId, userId);
//
//        validateFileAndUserIds(fileId, userId);
//
//        TaskFile taskFile = fileRepository.findById(fileId)
//                .orElseThrow(() -> new RuntimeException("Plik nie został znaleziony"));
//
//        validateFileAccess(taskFile, userId);
//
//        try {
//            Path filePath = Paths.get(taskFile.getFilePath());
//            Resource resource = new UrlResource(filePath.toUri());
//
//            if (resource.exists() && resource.isReadable()) {
//                log.info("File downloaded successfully: {}", fileId);
//                return resource;
//            } else {
//                throw new RuntimeException("Plik nie istnieje lub nie można go odczytać");
//            }
//        } catch (MalformedURLException e) {
//            log.error("Error downloading file: {}", e.getMessage());
//            throw new RuntimeException("Błąd podczas pobierania pliku: " + e.getMessage());
//        }
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<TaskFile> getAssignmentFiles(
//            @NotNull(message = "Identyfikator przypisania nie może być pusty")
//            @Positive(message = "Identyfikator przypisania musi być liczbą dodatnią")
//            Long assignmentId,
//
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//
//        log.debug("Getting files for assignment: {}, user: {}", assignmentId, userId);
//
//        validateAssignmentAndUserIds(assignmentId, userId);
//
//        TaskAssignment assignment = getAssignmentById(assignmentId);
//        validateFileAccess(assignment, userId);
//
//        return fileRepository.findByAssignmentId(assignmentId);
//    }
//
//    @Override
//    @Transactional
//    public void deleteFile(
//            @NotNull(message = "Identyfikator pliku nie może być pusty")
//            @Positive(message = "Identyfikator pliku musi być liczbą dodatnią")
//            Long fileId,
//
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//
//        log.info("Deleting file: {}, user: {}", fileId, userId);
//
//        validateFileAndUserIds(fileId, userId);
//
//        TaskFile taskFile = fileRepository.findById(fileId)
//                .orElseThrow(() -> new RuntimeException("Plik nie został znaleziony"));
//
//        validateFileAccess(taskFile, userId);
//
//        try {
//            // Usuń plik fizycznie
//            Path filePath = Paths.get(taskFile.getFilePath());
//            Files.deleteIfExists(filePath);
//
//            // Usuń z bazy
//            fileRepository.delete(taskFile);
//            log.info("File deleted successfully: {}", fileId);
//        } catch (IOException e) {
//            log.error("Error deleting file: {}", e.getMessage());
//            throw new RuntimeException("Błąd podczas usuwania pliku: " + e.getMessage());
//        }
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<TaskAssignment> getAssignmentsByStatus(
//            @NotNull(message = "Identyfikator zadania nie może być pusty")
//            @Positive(message = "Identyfikator zadania musi być liczbą dodatnią")
//            Long taskId,
//
//            @NotNull(message = "Status przypisania nie może być pusty")
//            AssignmentStatus status) {
//
//        log.debug("Getting assignments for task: {} with status: {}", taskId, status);
//
//        validateTaskId(taskId);
//        validateAssignmentStatus(status);
//
//        return assignmentRepository.findByTaskId(taskId).stream()
//                .filter(assignment -> assignment.getStatus() == status)
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public Long countCompletedAssignments(
//            @NotNull(message = "Identyfikator zadania nie może być pusty")
//            @Positive(message = "Identyfikator zadania musi być liczbą dodatnią")
//            Long taskId) {
//
//        validateTaskId(taskId);
//        return getAssignmentsByStatus(taskId, AssignmentStatus.COMPLETED).stream().count();
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public Long countTotalAssignments(
//            @NotNull(message = "Identyfikator zadania nie może być pusty")
//            @Positive(message = "Identyfikator zadania musi być liczbą dodatnią")
//            Long taskId) {
//
//        validateTaskId(taskId);
//        return assignmentRepository.findByTaskId(taskId).stream().count();
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public boolean canUserManageTask(
//            @NotNull(message = "Identyfikator zadania nie może być pusty")
//            @Positive(message = "Identyfikator zadania musi być liczbą dodatnią")
//            Long taskId,
//
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//
//        try {
//            validateTaskAndUserIds(taskId, userId);
//            Task task = getTaskById(taskId);
//            return task.getMeeting().getOrganizer().getId().equals(userId);
//        } catch (Exception e) {
//            return false;
//        }
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public boolean canUserAccessAssignment(
//            @NotNull(message = "Identyfikator przypisania nie może być pusty")
//            @Positive(message = "Identyfikator przypisania musi być liczbą dodatnią")
//            Long assignmentId,
//
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//
//        try {
//            validateAssignmentAndUserIds(assignmentId, userId);
//            TaskAssignment assignment = getAssignmentById(assignmentId);
//            return validateAssignmentAccess(assignment, userId, false);
//        } catch (Exception e) {
//            return false;
//        }
//    }
//
//    @Transactional(readOnly = true)
//    @Override
//    public MeetingTasksResponse getMeetingTasksForUser(
//            @NotNull(message = "Identyfikator spotkania nie może być pusty")
//            @Positive(message = "Identyfikator spotkania musi być liczbą dodatnią")
//            Long meetingId,
//
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//
//        validateMeetingAndUserIds(meetingId, userId);
//
//        // Pobierz spotkanie
//        Meeting meeting = meetingRepository.findById(meetingId)
//                .orElseThrow(() -> new RuntimeException("Spotkanie nie zostało znalezione"));
//
//        // Sprawdź, czy użytkownik jest organizatorem
//        boolean isOrganizer = meeting.getOrganizer().getId().equals(userId);
//
//        // Pobierz zadania
//        List<Task> tasks = getMeetingTasks(meetingId);
//
//        return MeetingTasksResponse.builder()
//                .meeting(meeting)
//                .tasks(tasks)
//                .isOrganizer(isOrganizer)
//                .build();
//    }
//
//    @Transactional(readOnly = true)
//    @Override
//    public MeetingTaskFormResponse getTaskCreationFormData(
//            @NotNull(message = "Identyfikator spotkania nie może być pusty")
//            @Positive(message = "Identyfikator spotkania musi być liczbą dodatnią")
//            Long meetingId,
//
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//
//        validateMeetingAndUserIds(meetingId, userId);
//
//        Meeting meeting = meetingRepository.findById(meetingId)
//                .orElseThrow(() -> new RuntimeException("Spotkanie nie zostało znalezione"));
//
//        if (!meeting.getOrganizer().getId().equals(userId)) {
//            throw new RuntimeException("Tylko organizator może tworzyć zadania");
//        }
//
//        return MeetingTaskFormResponse.builder()
//                .meeting(meeting)
//                .createTaskRequest(new CreateTaskRequest())
//                .build();
//    }
//
//    @Transactional(readOnly = true)
//    @Override
//    public MeetingTaskDetailsResponse getTaskDetailsForUser(
//            @NotNull(message = "Identyfikator spotkania nie może być pusty")
//            @Positive(message = "Identyfikator spotkania musi być liczbą dodatnią")
//            Long meetingId,
//
//            @NotNull(message = "Identyfikator zadania nie może być pusty")
//            @Positive(message = "Identyfikator zadania musi być liczbą dodatnią")
//            Long taskId,
//
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//
//        validateMeetingTaskAndUserIds(meetingId, taskId, userId);
//
//        Task task = getTaskById(taskId);
//
//        Meeting meeting = meetingRepository.findById(meetingId)
//                .orElseThrow(() -> new RuntimeException("Spotkanie nie zostało znalezione"));
//
//        boolean isOrganizer = meeting.getOrganizer().getId().equals(userId);
//        boolean canAccess = isOrganizer || task.getAssignments().stream()
//                .anyMatch(a -> a.getUser().getId().equals(userId));
//
//        if (!canAccess) {
//            throw new RuntimeException("Brak uprawnień do tego zadania");
//        }
//
//        return MeetingTaskDetailsResponse.builder()
//                .meeting(meeting)
//                .task(task)
//                .isOrganizer(isOrganizer)
//                .userId(userId)
//                .build();
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public MeetingTaskEditResponse getTaskForEditing(
//            @NotNull(message = "Identyfikator spotkania nie może być pusty")
//            @Positive(message = "Identyfikator spotkania musi być liczbą dodatnią")
//            Long meetingId,
//
//            @NotNull(message = "Identyfikator zadania nie może być pusty")
//            @Positive(message = "Identyfikator zadania musi być liczbą dodatnią")
//            Long taskId,
//
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//
//        validateMeetingTaskAndUserIds(meetingId, taskId, userId);
//
//        Task task = getTaskById(taskId);
//
//        Meeting meeting = meetingRepository.findById(meetingId)
//                .orElseThrow(() -> new RuntimeException("Spotkanie nie zostało znalezione"));
//
//        if (!meeting.getOrganizer().getId().equals(userId)) {
//            throw new RuntimeException("Tylko organizator może edytować zadania");
//        }
//
//        String formattedDeadline = task.getDeadline() != null ? task.getDeadline().toString().replace("T", " ") : null;
//
//        return MeetingTaskEditResponse.builder()
//                .meeting(meeting)
//                .task(task)
//                .formattedDeadline(formattedDeadline)
//                .build();
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public MeetingTaskAssignmentsResponse getTaskAssignmentsForUser(
//            @NotNull(message = "Identyfikator spotkania nie może być pusty")
//            @Positive(message = "Identyfikator spotkania musi być liczbą dodatnią")
//            Long meetingId,
//
//            @NotNull(message = "Identyfikator zadania nie może być pusty")
//            @Positive(message = "Identyfikator zadania musi być liczbą dodatnią")
//            Long taskId,
//
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//
//        validateMeetingTaskAndUserIds(meetingId, taskId, userId);
//
//        Task task = getTaskById(taskId);
//
//        Meeting meeting = meetingRepository.findById(meetingId)
//                .orElseThrow(() -> new RuntimeException("Spotkanie nie zostało znalezione"));
//
//        if (!meeting.getOrganizer().getId().equals(userId)) {
//            throw new RuntimeException("Tylko organizator może przypisywać użytkowników");
//        }
//
//        List<User> assignedUsers = assignmentRepository.findAssignedUsersByTaskId(taskId);
//        List<User> availableUsers = participantRepository.findAvailableUsersForTask(meetingId, taskId);
//        List<TaskAssignment> assignments = assignmentRepository.findByTaskId(taskId);
//
//        return MeetingTaskAssignmentsResponse.builder()
//                .meeting(meeting)
//                .task(task)
//                .availableUsers(availableUsers)
//                .assignedUsers(assignedUsers)
//                .assignments(assignments)
//                .build();
//    }
//
//    // ========== METODY WALIDACYJNE ==========
//
//    /**
//     * Walidacja identyfikatora spotkania
//     */
//    private void validateMeetingId(
//            @NotNull(message = "Identyfikator spotkania nie może być pusty")
//            @Positive(message = "Identyfikator spotkania musi być liczbą dodatnią")
//            Long meetingId) {
//
//        if (meetingId == null) {
//            throw new IllegalArgumentException("Identyfikator spotkania nie może być pusty");
//        }
//
//        if (meetingId <= 0) {
//            throw new IllegalArgumentException("Identyfikator spotkania musi być liczbą dodatnią");
//        }
//    }
//
//    /**
//     * Walidacja identyfikatora użytkownika
//     */
//    private void validateUserId(
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//
//        if (userId == null) {
//            throw new IllegalArgumentException("Identyfikator użytkownika nie może być pusty");
//        }
//
//        if (userId <= 0) {
//            throw new IllegalArgumentException("Identyfikator użytkownika musi być liczbą dodatnią");
//        }
//    }
//
//    /**
//     * Walidacja identyfikatora zadania
//     */
//    private void validateTaskId(
//            @NotNull(message = "Identyfikator zadania nie może być pusty")
//            @Positive(message = "Identyfikator zadania musi być liczbą dodatnią")
//            Long taskId) {
//
//        if (taskId == null) {
//            throw new IllegalArgumentException("Identyfikator zadania nie może być pusty");
//        }
//
//        if (taskId <= 0) {
//            throw new IllegalArgumentException("Identyfikator zadania musi być liczbą dodatnią");
//        }
//    }
//
//    /**
//     * Walidacja identyfikatora przypisania
//     */
//    private void validateAssignmentId(
//            @NotNull(message = "Identyfikator przypisania nie może być pusty")
//            @Positive(message = "Identyfikator przypisania musi być liczbą dodatnią")
//            Long assignmentId) {
//
//        if (assignmentId == null) {
//            throw new IllegalArgumentException("Identyfikator przypisania nie może być pusty");
//        }
//
//        if (assignmentId <= 0) {
//            throw new IllegalArgumentException("Identyfikator przypisania musi być liczbą dodatnią");
//        }
//    }
//
//    /**
//     * Walidacja identyfikatora pliku
//     */
//    private void validateFileId(
//            @NotNull(message = "Identyfikator pliku nie może być pusty")
//            @Positive(message = "Identyfikator pliku musi być liczbą dodatnią")
//            Long fileId) {
//
//        if (fileId == null) {
//            throw new IllegalArgumentException("Identyfikator pliku nie może być pusty");
//        }
//
//        if (fileId <= 0) {
//            throw new IllegalArgumentException("Identyfikator pliku musi być liczbą dodatnią");
//        }
//    }
//
//    /**
//     * Walidacja identyfikatorów spotkania i organizatora
//     */
//    private void validateMeetingAndOrganizerIds(
//            @NotNull(message = "Identyfikator spotkania nie może być pusty")
//            @Positive(message = "Identyfikator spotkania musi być liczbą dodatnią")
//            Long meetingId,
//
//            @NotNull(message = "Identyfikator organizatora nie może być pusty")
//            @Positive(message = "Identyfikator organizatora musi być liczbą dodatnią")
//            Long organizerId) {
//
//        validateMeetingId(meetingId);
//        validateUserId(organizerId);
//    }
//
//    /**
//     * Walidacja identyfikatorów spotkania i użytkownika
//     */
//    private void validateMeetingAndUserIds(
//            @NotNull(message = "Identyfikator spotkania nie może być pusty")
//            @Positive(message = "Identyfikator spotkania musi być liczbą dodatnią")
//            Long meetingId,
//
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//
//        validateMeetingId(meetingId);
//        validateUserId(userId);
//    }
//
//    /**
//     * Walidacja identyfikatorów zadania i użytkownika
//     */
//    private void validateTaskAndUserIds(
//            @NotNull(message = "Identyfikator zadania nie może być pusty")
//            @Positive(message = "Identyfikator zadania musi być liczbą dodatnią")
//            Long taskId,
//
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//
//        validateTaskId(taskId);
//        validateUserId(userId);
//    }
//
//    /**
//     * Walidacja identyfikatorów spotkania, zadania i użytkownika
//     */
//    private void validateMeetingTaskAndUserIds(
//            @NotNull(message = "Identyfikator spotkania nie może być pusty")
//            @Positive(message = "Identyfikator spotkania musi być liczbą dodatnią")
//            Long meetingId,
//
//            @NotNull(message = "Identyfikator zadania nie może być pusty")
//            @Positive(message = "Identyfikator zadania musi być liczbą dodatnią")
//            Long taskId,
//
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//
//        validateMeetingId(meetingId);
//        validateTaskId(taskId);
//        validateUserId(userId);
//    }
//
//    /**
//     * Walidacja identyfikatorów zadania, użytkownika i organizatora
//     */
//    private void validateTaskUserAndOrganizerIds(
//            @NotNull(message = "Identyfikator zadania nie może być pusty")
//            @Positive(message = "Identyfikator zadania musi być liczbą dodatnią")
//            Long taskId,
//
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId,
//
//            @NotNull(message = "Identyfikator organizatora nie może być pusty")
//            @Positive(message = "Identyfikator organizatora musi być liczbą dodatnią")
//            Long organizerId) {
//
//        validateTaskId(taskId);
//        validateUserId(userId);
//        validateUserId(organizerId);
//    }
//
//    /**
//     * Walidacja identyfikatorów przypisania i użytkownika
//     */
//    private void validateAssignmentAndUserIds(
//            @NotNull(message = "Identyfikator przypisania nie może być pusty")
//            @Positive(message = "Identyfikator przypisania musi być liczbą dodatnią")
//            Long assignmentId,
//
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//
//        validateAssignmentId(assignmentId);
//        validateUserId(userId);
//    }
//
//    /**
//     * Walidacja identyfikatorów pliku i użytkownika
//     */
//    private void validateFileAndUserIds(
//            @NotNull(message = "Identyfikator pliku nie może być pusty")
//            @Positive(message = "Identyfikator pliku musi być liczbą dodatnią")
//            Long fileId,
//
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//
//        validateFileId(fileId);
//        validateUserId(userId);
//    }
//
//    /**
//     * Walidacja uprawnień organizatora
//     */
//    private void validateOrganizerPermissions(
//            @NotNull(message = "Spotkanie nie może być puste")
//            Meeting meeting,
//
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//
//        if (meeting == null) {
//            throw new IllegalArgumentException("Spotkanie nie może być puste");
//        }
//
//        if (!meeting.getOrganizer().getId().equals(userId)) {
//            throw new RuntimeException("Tylko organizator może wykonać tę akcję");
//        }
//    }
//
//    /**
//     * Walidacja dostępu do przypisania
//     */
//    private void validateAssignmentAccess(
//            @NotNull(message = "Przypisanie nie może być puste")
//            TaskAssignment assignment,
//
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//
//        validateAssignmentAccess(assignment, userId, true);
//    }
//
//    /**
//     * Walidacja dostępu do przypisania z opcją rzucania wyjątku
//     */
//    private boolean validateAssignmentAccess(
//            @NotNull(message = "Przypisanie nie może być puste")
//            TaskAssignment assignment,
//
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId,
//            boolean throwException) {
//
//        boolean isOwner = assignment.getUser().getId().equals(userId);
//        boolean isOrganizer = assignment.getTask().getMeeting().getOrganizer().getId().equals(userId);
//
//        if (!isOwner && !isOrganizer) {
//            if (throwException) {
//                throw new RuntimeException("Brak uprawnień do tego przypisania");
//            }
//            return false;
//        }
//        return true;
//    }
//
//    /**
//     * Walidacja dostępu do pliku
//     */
//    private void validateFileAccess(
//            @NotNull(message = "Plik zadania nie może być pusty")
//            TaskFile taskFile,
//
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//
//        validateFileAccess(taskFile.getAssignment(), userId);
//    }
//
//    /**
//     * Walidacja dostępu do pliku przez przypisanie
//     */
//    private void validateFileAccess(
//            @NotNull(message = "Przypisanie nie może być puste")
//            TaskAssignment assignment,
//
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Positive(message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//
//        validateAssignmentAccess(assignment, userId);
//    }
//
//    /**
//     * Walidacja żądania utworzenia zadania
//     */
//    private void validateCreateTaskRequest(
//            @Valid @NotNull(message = "Żądanie utworzenia zadania nie może być puste")
//            CreateTaskRequest request) {
//
//        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
//            throw new IllegalArgumentException("Tytuł zadania nie może być pusty");
//        }
//
//        if (request.getTitle().length() > 200) {
//            throw new IllegalArgumentException("Tytuł zadania nie może przekraczać 200 znaków");
//        }
//
//        if (request.getDescription() != null && request.getDescription().length() > 1000) {
//            throw new IllegalArgumentException("Opis zadania nie może przekraczać 1000 znaków");
//        }
//
//        if (request.getDeadline() != null && request.getDeadline().isBefore(LocalDateTime.now())) {
//            throw new IllegalArgumentException("Termin wykonania nie może być w przeszłości");
//        }
//    }
//
//    /**
//     * Walidacja żądania aktualizacji zadania
//     */
//    private void validateUpdateTaskRequest(
//            @Valid @NotNull(message = "Żądanie aktualizacji zadania nie może być puste")
//            UpdateTaskRequest request) {
//
//        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
//            throw new IllegalArgumentException("Tytuł zadania nie może być pusty");
//        }
//
//        if (request.getTitle().length() > 200) {
//            throw new IllegalArgumentException("Tytuł zadania nie może przekraczać 200 znaków");
//        }
//
//        if (request.getDescription() != null && request.getDescription().length() > 1000) {
//            throw new IllegalArgumentException("Opis zadania nie może przekraczać 1000 znaków");
//        }
//
//        if (request.getDeadline() != null && request.getDeadline().isBefore(LocalDateTime.now())) {
//            throw new IllegalArgumentException("Termin wykonania nie może być w przeszłości");
//        }
//    }
//
//    /**
//     * Walidacja statusu przypisania
//     */
//    private void validateAssignmentStatus(
//            @NotNull(message = "Status przypisania nie może być pusty")
//            AssignmentStatus status) {
//
//        if (status == null) {
//            throw new IllegalArgumentException("Status przypisania nie może być pusty");
//        }
//    }
//
//    /**
//     * Walidacja komentarza
//     */
//    private void validateComment(
//            @Size(max = 1000, message = "Komentarz nie może przekraczać 1000 znaków")
//            String comment) {
//
//        if (comment != null && comment.length() > 1000) {
//            throw new IllegalArgumentException("Komentarz nie może przekraczać 1000 znaków");
//        }
//    }
//
//    /**
//     * Walidacja pliku
//     */
//    private void validateFile(
//            @NotNull(message = "Plik nie może być pusty")
//            MultipartFile file) {
//
//        if (file == null) {
//            throw new IllegalArgumentException("Plik nie może być pusty");
//        }
//
//        if (file.isEmpty()) {
//            throw new IllegalArgumentException("Plik jest pusty");
//        }
//
//        if (file.getSize() > 10 * 1024 * 1024) { // 10MB
//            throw new IllegalArgumentException("Plik jest zbyt duży. Maksymalny rozmiar: 10MB");
//        }
//
//        String originalFilename = file.getOriginalFilename();
//        if (originalFilename == null || originalFilename.trim().isEmpty()) {
//            throw new IllegalArgumentException("Nazwa pliku nie może być pusta");
//        }
//
//        if (originalFilename.length() > 255) {
//            throw new IllegalArgumentException("Nazwa pliku jest zbyt długa");
//        }
//    }
//
//    /**
//     * Pobranie rozszerzenia pliku
//     */
//    private String getFileExtension(String filename) {
//        if (filename == null || !filename.contains(".")) {
//            return "";
//        }
//        return filename.substring(filename.lastIndexOf("."));
//    }
//
//    /**
//     * Usuwanie plików zadania z dysku
//     */
//    private void deleteTaskFilesFromDisk(
//            @NotNull(message = "Zadanie nie może być puste")
//            Task task) {
//
//        try {
//            String taskDir = "task_" + task.getId();
//            Path taskPath = Paths.get(uploadDir, "tasks", taskDir);
//
//            if (Files.exists(taskPath)) {
//                Files.walk(taskPath)
//                        .sorted((a, b) -> -a.compareTo(b))
//                        .forEach(path -> {
//                            try {
//                                Files.deleteIfExists(path);
//                            } catch (IOException e) {
//                                log.warn("Could not delete file: {}", path);
//                            }
//                        });
//                log.info("Task files deleted from disk: {}", task.getId());
//            }
//        } catch (IOException e) {
//            log.error("Error deleting task files from disk: {}", e.getMessage());
//        }
//    }
//
//    /**
//     * Usuwanie plików przypisania z dysku
//     */
//    private void deleteAssignmentFilesFromDisk(
//            @NotNull(message = "Przypisanie nie może być puste")
//            TaskAssignment assignment) {
//
//        try {
//            String userEmail = assignment.getUser().getEmail();
//            String safeUserDir = userEmail.replace("@", "_at_").replace(".", "_");
//            String taskDir = "task_" + assignment.getTask().getId();
//
//            Path userPath = Paths.get(uploadDir, "tasks", taskDir, safeUserDir);
//
//            if (Files.exists(userPath)) {
//                Files.walk(userPath)
//                        .sorted((a, b) -> -a.compareTo(b))
//                        .forEach(path -> {
//                            try {
//                                Files.deleteIfExists(path);
//                            } catch (IOException e) {
//                                log.warn("Could not delete file: {}", path);
//                            }
//                        });
//                log.info("Assignment files deleted from disk: {}", assignment.getId());
//            }
//        } catch (IOException e) {
//            log.error("Error deleting assignment files from disk: {}", e.getMessage());
//        }
//    }
//}







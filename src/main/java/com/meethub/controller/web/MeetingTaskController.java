package com.meethub.controller.web;

import com.meethub.domain.model.entity.*;
import com.meethub.domain.model.request.CreateTaskRequest;
import com.meethub.domain.model.request.UpdateTaskRequest;
import com.meethub.domain.model.response.*;
import com.meethub.domain.service.TaskService;
import com.meethub.security.CustomUserDetailsService.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Validated // DODANE - walidacja dla kontrolera webowego
@Slf4j
@Controller
@RequestMapping("/meetings/{meetingId}/tasks")
@RequiredArgsConstructor
@Tag(name = "Zadania Spotkania", description = "Strony web do zarządzania zadaniami w ramach spotkania")
public class MeetingTaskController {

    private final TaskService taskService;

    @GetMapping
    @Operation(summary = "Lista zadań spotkania", description = "Wyświetla wszystkie zadania przypisane do spotkania.")
    public String getMeetingTasks(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetails userDetails,
            Model model) {

        try {
            log.info("Wyświetlanie listy zadań dla spotkania ID={} przez użytkownika {}",
                    meetingId, userDetails.getId());

            MeetingTasksResponse response = taskService.getMeetingTasksForUser(meetingId, userDetails.getId());

            model.addAttribute("meeting", response.getMeeting());
            model.addAttribute("tasks", response.getTasks());
            model.addAttribute("isOrganizer", response.isOrganizer());
            model.addAttribute("userId", userDetails.getId());

            log.info("Wyświetlono {} zadań dla spotkania ID={}", response.getTasks().size(), meetingId);
            return "meetings/tasks/list";

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji przy wyświetlaniu listy zadań: {}", e.getMessage());
            model.addAttribute("error", "Nieprawidłowy identyfikator spotkania");
            return "redirect:/meetings";

        } catch (Exception e) {
            log.error("Błąd podczas wyświetlania listy zadań dla spotkania {}: {}", meetingId, e.getMessage(), e);
            model.addAttribute("error", e.getMessage());
            return "redirect:/meetings/" + meetingId;
        }
    }

    @GetMapping("/create")
    @Operation(summary = "Formularz tworzenia zadania", description = "Wyświetla formularz umożliwiający utworzenie nowego zadania. Tylko dla organizatora.")
    public String showCreateTaskForm(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetails userDetails,
            Model model) {

        try {
            log.info("Wyświetlanie formularza tworzenia zadania dla spotkania ID={} przez użytkownika {}",
                    meetingId, userDetails.getId());

            MeetingTaskFormResponse response = taskService.getTaskCreationFormData(meetingId, userDetails.getId());
            model.addAttribute("meeting", response.getMeeting());
            model.addAttribute("createTaskRequest", response.getCreateTaskRequest());

            return "meetings/tasks/create";

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji przy wyświetlaniu formularza tworzenia zadania: {}", e.getMessage());
            model.addAttribute("error", "Nieprawidłowy identyfikator spotkania");
            return "redirect:/meetings/" + meetingId + "/tasks";

        } catch (Exception e) {
            log.error("Błąd podczas wyświetlania formularza tworzenia zadania dla spotkania {}: {}",
                    meetingId, e.getMessage(), e);
            model.addAttribute("error", e.getMessage());
            return "redirect:/meetings/" + meetingId + "/tasks";
        }
    }

    @PostMapping("/create")
    @Operation(summary = "Utwórz zadanie", description = "Tworzy nowe zadanie w spotkaniu.")
    public String createTask(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @ModelAttribute @Valid CreateTaskRequest request,
            BindingResult result,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetails userDetails,

            RedirectAttributes redirectAttributes) {

        try {
            log.info("Tworzenie zadania dla spotkania ID={} przez użytkownika {}", meetingId, userDetails.getId());

            if (result.hasErrors()) {
                log.warn("Błędy walidacji formularza tworzenia zadania: {}", result.getAllErrors());
                redirectAttributes.addFlashAttribute("error", "Nieprawidłowe dane w formularzu");
                return "redirect:/meetings/" + meetingId + "/tasks/create";
            }

            Task task = taskService.createTask(request, meetingId, userDetails.getId());
            redirectAttributes.addFlashAttribute("success", "Zadanie zostało utworzone");
            log.info("Zadanie utworzone: ID={}, tytuł={}", task.getId(), task.getTitle());

            return "redirect:/meetings/" + meetingId + "/tasks/" + task.getId();

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji podczas tworzenia zadania: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Nieprawidłowe dane wejściowe");
            return "redirect:/meetings/" + meetingId + "/tasks/create";

        } catch (Exception e) {
            log.error("Błąd podczas tworzenia zadania dla spotkania {}: {}", meetingId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/meetings/" + meetingId + "/tasks/create";
        }
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "Szczegóły zadania", description = "Wyświetla szczegóły zadania oraz uprawnienia dostępu użytkownika.")
    public String getTaskDetails(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @PathVariable @NotNull(message = "Identyfikator zadania nie może być pusty")
            @Min(value = 1, message = "Identyfikator zadania musi być liczbą dodatnią")
            Long taskId,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetails userDetails,
            Model model) {

        try {
            log.info("Wyświetlanie szczegółów zadania ID={} ze spotkania ID={} przez użytkownika {}",
                    taskId, meetingId, userDetails.getId());

            MeetingTaskDetailsResponse response = taskService.getTaskDetailsForUser(meetingId, taskId, userDetails.getId());

            model.addAttribute("meeting", response.getMeeting());
            model.addAttribute("task", response.getTask());
            model.addAttribute("isOrganizer", response.isOrganizer());
            model.addAttribute("userId", response.getUserId());

            return "meetings/tasks/details";

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji przy wyświetlaniu szczegółów zadania: {}", e.getMessage());
            model.addAttribute("error", "Nieprawidłowy identyfikator");
            return "redirect:/meetings/" + meetingId + "/tasks";

        } catch (Exception e) {
            log.error("Błąd podczas wyświetlania szczegółów zadania {}: {}", taskId, e.getMessage(), e);
            model.addAttribute("error", e.getMessage());
            return "redirect:/meetings/" + meetingId + "/tasks";
        }
    }

    @GetMapping("/{taskId}/edit")
    @Operation(summary = "Formularz edycji zadania", description = "Wyświetla formularz edycji zadania. Tylko organizator może edytować.")
    public String showEditTaskForm(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @PathVariable @NotNull(message = "Identyfikator zadania nie może być pusty")
            @Min(value = 1, message = "Identyfikator zadania musi być liczbą dodatnią")
            Long taskId,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetails userDetails,
            Model model) {

        try {
            log.info("Wyświetlanie formularza edycji zadania ID={} ze spotkania ID={}", taskId, meetingId);

            MeetingTaskEditResponse response = taskService.getTaskForEditing(meetingId, taskId, userDetails.getId());

            model.addAttribute("meeting", response.getMeeting());
            model.addAttribute("task", response.getTask());
            model.addAttribute("formattedDeadline", response.getFormattedDeadline());

            return "meetings/tasks/edit";

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji przy wyświetlaniu formularza edycji zadania: {}", e.getMessage());
            model.addAttribute("error", "Nieprawidłowy identyfikator");
            return "redirect:/meetings/" + meetingId + "/tasks/" + taskId;

        } catch (Exception e) {
            log.error("Błąd podczas wyświetlania formularza edycji zadania {}: {}", taskId, e.getMessage(), e);
            model.addAttribute("error", e.getMessage());
            return "redirect:/meetings/" + meetingId + "/tasks/" + taskId;
        }
    }

    @PostMapping("/{taskId}/edit")
    @Operation(summary = "Aktualizuj zadanie", description = "Aktualizuje dane zadania na podstawie formularza edycji.")
    public String updateTask(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @PathVariable @NotNull(message = "Identyfikator zadania nie może być pusty")
            @Min(value = 1, message = "Identyfikator zadania musi być liczbą dodatnią")
            Long taskId,

            @ModelAttribute @Valid UpdateTaskRequest request,
            BindingResult result,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetails userDetails,

            RedirectAttributes redirectAttributes) {

        try {
            log.info("Aktualizacja zadania ID={} ze spotkania ID={}", taskId, meetingId);

            if (result.hasErrors()) {
                log.warn("Błędy walidacji formularza edycji zadania: {}", result.getAllErrors());
                redirectAttributes.addFlashAttribute("error", "Nieprawidłowe dane w formularzu");
                return "redirect:/meetings/" + meetingId + "/tasks/" + taskId + "/edit";
            }

            Task task = taskService.updateTaskWithRequest(taskId, request, userDetails.getId());
            redirectAttributes.addFlashAttribute("success", "Zadanie zostało zaktualizowane");
            log.info("Zadanie ID={} zaktualizowane pomyślnie", taskId);

            return "redirect:/meetings/" + meetingId + "/tasks/" + task.getId();

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji podczas aktualizacji zadania: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Nieprawidłowe dane wejściowe");
            return "redirect:/meetings/" + meetingId + "/tasks/" + taskId + "/edit";

        } catch (Exception e) {
            log.error("Błąd podczas aktualizacji zadania {}: {}", taskId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/meetings/" + meetingId + "/tasks/" + taskId + "/edit";
        }
    }

    @PostMapping("/{taskId}/delete")
    @Operation(summary = "Usuń zadanie", description = "Usuwa wskazane zadanie z spotkania.")
    public String deleteTask(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @PathVariable @NotNull(message = "Identyfikator zadania nie może być pusty")
            @Min(value = 1, message = "Identyfikator zadania musi być liczbą dodatnią")
            Long taskId,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            log.info("Usuwanie zadania ID={} ze spotkania ID={} przez użytkownika {}",
                    taskId, meetingId, userDetails.getId());

            taskService.deleteTask(taskId, userDetails.getId());
            redirectAttributes.addFlashAttribute("success", "Zadanie zostało usunięte");
            log.info("Zadanie ID={} usunięte pomyślnie", taskId);

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji podczas usuwania zadania: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Nieprawidłowy identyfikator zadania");

        } catch (Exception e) {
            log.error("Błąd usuwania zadania {}: {}", taskId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/meetings/" + meetingId + "/tasks";
    }

    @PostMapping("/{taskId}/assign-self")
    @Operation(summary = "Przypisz siebie do zadania", description = "Pozwala użytkownikowi przypisać siebie do zadania.")
    public String assignSelfToTask(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @PathVariable @NotNull(message = "Identyfikator zadania nie może być pusty")
            @Min(value = 1, message = "Identyfikator zadania musi być liczbą dodatnią")
            Long taskId,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            log.info("Przypisywanie siebie do zadania ID={} przez użytkownika {}", taskId, userDetails.getId());

            taskService.assignTaskToCurrentUser(taskId, userDetails.getId());
            redirectAttributes.addFlashAttribute("success", "Zostałeś przypisany do zadania");
            log.info("Użytkownik {} przypisany do zadania {}", userDetails.getId(), taskId);

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji podczas przypisywania siebie do zadania: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Nieprawidłowy identyfikator zadania");

        } catch (Exception e) {
            log.error("Błąd przypisywania siebie do zadania {}: {}", taskId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/meetings/" + meetingId + "/tasks/" + taskId;
    }

    @PostMapping("/{taskId}/assignment/{assignmentId}/comment")
    @Operation(summary = "Dodaj/aktualizuj komentarz do przypisania", description = "Pozwala użytkownikowi dodać komentarz do przypisanego zadania.")
    public String updateAssignmentComment(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @PathVariable @NotNull(message = "Identyfikator zadania nie może być pusty")
            @Min(value = 1, message = "Identyfikator zadania musi być liczbą dodatnią")
            Long taskId,

            @PathVariable @NotNull(message = "Identyfikator przypisania nie może być pusty")
            @Min(value = 1, message = "Identyfikator przypisania musi być liczbą dodatnią")
            Long assignmentId,

            @RequestParam @NotBlank(message = "Komentarz nie może być pusty")
            @Size(max = 1000, message = "Komentarz nie może przekraczać 1000 znaków")
            String comment,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            log.info("Aktualizacja komentarza przypisania ID={} przez użytkownika {}", assignmentId, userDetails.getId());

            taskService.updateAssignmentComment(assignmentId, comment, userDetails.getId());
            redirectAttributes.addFlashAttribute("success", "Komentarz został zapisany");
            log.info("Komentarz przypisania ID={} zaktualizowany", assignmentId);

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji podczas aktualizacji komentarza: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Nieprawidłowe dane wejściowe");

        } catch (Exception e) {
            log.error("Błąd aktualizacji komentarza przypisania {}: {}", assignmentId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/meetings/" + meetingId + "/tasks/" + taskId;
    }

    @GetMapping("/{taskId}/assign")
    @Operation(summary = "Formularz przypisywania użytkowników", description = "Pozwala organizatorowi przypisywać użytkowników do zadania.")
    public String showAssignUsersForm(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @PathVariable @NotNull(message = "Identyfikator zadania nie może być pusty")
            @Min(value = 1, message = "Identyfikator zadania musi być liczbą dodatnią")
            Long taskId,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetails userDetails,
            Model model) {

        try {
            log.info("Wyświetlanie formularza przypisywania użytkowników do zadania ID={}", taskId);

            MeetingTaskAssignmentsResponse response = taskService.getTaskAssignmentsForUser(meetingId, taskId, userDetails.getId());

            model.addAttribute("meeting", response.getMeeting());
            model.addAttribute("task", response.getTask());
            model.addAttribute("availableUsers", response.getAvailableUsers());
            model.addAttribute("assignedUsers", response.getAssignedUsers());
            model.addAttribute("assignments", response.getAssignments());

            return "meetings/tasks/assign";

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji przy wyświetlaniu formularza przypisywania: {}", e.getMessage());
            model.addAttribute("error", "Nieprawidłowy identyfikator");
            return "redirect:/meetings/" + meetingId + "/tasks/" + taskId;

        } catch (Exception e) {
            log.error("Błąd podczas wyświetlania formularza przypisywania użytkowników do zadania {}: {}",
                    taskId, e.getMessage(), e);
            model.addAttribute("error", e.getMessage());
            return "redirect:/meetings/" + meetingId + "/tasks/" + taskId;
        }
    }

    @PostMapping("/{taskId}/assign")
    @Operation(summary = "Przypisz użytkownika do zadania", description = "Przypisuje wybranego użytkownika do zadania przez organizatora.")
    public String assignUserToTask(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @PathVariable @NotNull(message = "Identyfikator zadania nie może być pusty")
            @Min(value = 1, message = "Identyfikator zadania musi być liczbą dodatnią")
            Long taskId,

            @RequestParam @NotNull(message = "Identyfikator użytkownika nie może być pusty")
            @Min(value = 1, message = "Identyfikator użytkownika musi być liczbą dodatnią")
            Long userId,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            log.info("Przypisywanie użytkownika {} do zadania {} przez organizatora {}",
                    userId, taskId, userDetails.getId());

            taskService.assignTask(taskId, userId, userDetails.getId());
            redirectAttributes.addFlashAttribute("success", "Użytkownik został przypisany do zadania");
            log.info("Użytkownik {} przypisany do zadania {}", userId, taskId);

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji podczas przypisywania użytkownika do zadania: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Nieprawidłowy identyfikator użytkownika");

        } catch (Exception e) {
            log.error("Błąd przypisywania użytkownika {} do zadania {}: {}", userId, taskId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/meetings/" + meetingId + "/tasks/" + taskId + "/assign";
    }

    @PostMapping("/{taskId}/assignment/{assignmentId}/remove")
    @Operation(summary = "Usuń przypisanie", description = "Usuwa przypisanie użytkownika z zadania.")
    public String removeAssignment(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @PathVariable @NotNull(message = "Identyfikator zadania nie może być pusty")
            @Min(value = 1, message = "Identyfikator zadania musi być liczbą dodatnią")
            Long taskId,

            @PathVariable @NotNull(message = "Identyfikator przypisania nie może być pusty")
            @Min(value = 1, message = "Identyfikator przypisania musi być liczbą dodatnią")
            Long assignmentId,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            log.info("Usuwanie przypisania ID={} przez użytkownika {}", assignmentId, userDetails.getId());

            taskService.removeAssignment(assignmentId, userDetails.getId());
            redirectAttributes.addFlashAttribute("success", "Przypisanie zostało usunięte");
            log.info("Przypisanie ID={} usunięte pomyślnie", assignmentId);

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji podczas usuwania przypisania: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Nieprawidłowy identyfikator przypisania");

        } catch (Exception e) {
            log.error("Błąd usuwania przypisania {}: {}", assignmentId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/meetings/" + meetingId + "/tasks/" + taskId + "/assign";
    }

    @PostMapping("/{taskId}/assignment/{assignmentId}/status")
    @Operation(summary = "Zmień status przypisania", description = "Aktualizuje status przypisania zadania dla użytkownika.")
    public String updateAssignmentStatus(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @PathVariable @NotNull(message = "Identyfikator zadania nie może być pusty")
            @Min(value = 1, message = "Identyfikator zadania musi być liczbą dodatnią")
            Long taskId,

            @PathVariable @NotNull(message = "Identyfikator przypisania nie może być pusty")
            @Min(value = 1, message = "Identyfikator przypisania musi być liczbą dodatnią")
            Long assignmentId,

            @RequestParam("status") @NotBlank(message = "Status nie może być pusty")
            String status,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            log.info("Zmiana statusu przypisania ID={} na '{}' przez użytkownika {}",
                    assignmentId, status, userDetails.getId());

            taskService.updateAssignmentStatus(assignmentId,
                    com.meethub.domain.model.enums.AssignmentStatus.valueOf(status),
                    userDetails.getId());
            redirectAttributes.addFlashAttribute("success", "Status zadania został zaktualizowany");
            log.info("Status przypisania ID={} zmieniony na '{}'", assignmentId, status);

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji podczas zmiany statusu przypisania: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Nieprawidłowy status");

        } catch (IllegalArgumentException e) {
            log.warn("Nieprawidłowy status '{}' dla przypisania ID={}", status, assignmentId);
            redirectAttributes.addFlashAttribute("error", "Nieprawidłowy status: " + status);

        } catch (Exception e) {
            log.error("Błąd zmiany statusu przypisania {}: {}", assignmentId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/meetings/" + meetingId + "/tasks/" + taskId;
    }
// MeetingTaskController.java - poprawione metody
//
//    @GetMapping("/{taskId}/files")
//    @Operation(summary = "Lista plików zadania", description = "Wyświetla wszystkie pliki przypisane do zadania.")
//    public String getTaskFiles(
//            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
//            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
//            Long meetingId,
//
//            @PathVariable @NotNull(message = "Identyfikator zadania nie może być pusty")
//            @Min(value = 1, message = "Identyfikator zadania musi być liczbą dodatnią")
//            Long taskId,
//
//            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
//            CustomUserDetails userDetails,
//            Model model) {
//
//        try {
//            log.info("Wyświetlanie plików dla zadania ID={} przez użytkownika {}", taskId, userDetails.getId());
//
//            // Używamy serwisu do pobrania zadania i spotkania
//            MeetingTaskDetailsResponse response = taskService.getTaskDetailsForUser(meetingId, taskId, userDetails.getId());
//
//            List<TaskFile> files = taskService.getTaskFiles(taskId, userDetails.getId());
//            boolean canUpload = taskService.canUserUploadToTask(taskId, userDetails.getId());
//
//            model.addAttribute("meeting", response.getMeeting());
//            model.addAttribute("task", response.getTask());
//            model.addAttribute("files", files);
//            model.addAttribute("isOrganizer", response.isOrganizer());
//            model.addAttribute("canUpload", canUpload);
//            model.addAttribute("userId", userDetails.getId());
//
//            return "meetings/tasks/files";
//
//        } catch (Exception e) {
//            log.error("Błąd podczas wyświetlania plików zadania {}: {}", taskId, e.getMessage(), e);
//            model.addAttribute("error", e.getMessage());
//            return "redirect:/meetings/" + meetingId + "/tasks/" + taskId;
//        }
//    }

//    @PostMapping("/{taskId}/files/upload")
//    @Operation(summary = "Prześlij plik do zadania", description = "Przesyła nowy plik do zadania.")
//    public String uploadTaskFile(
//            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
//            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
//            Long meetingId,
//
//            @PathVariable @NotNull(message = "Identyfikator zadania nie może być pusty")
//            @Min(value = 1, message = "Identyfikator zadania musi być liczbą dodatnią")
//            Long taskId,
//
//            @RequestParam("file") @NotNull(message = "Plik nie może być pusty")
//            MultipartFile file,
//
//            @RequestParam(value = "description", required = false, defaultValue = "")
//            @Size(max = 500, message = "Opis nie może przekraczać 500 znaków")
//            String description,
//
//            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
//            CustomUserDetails userDetails,
//            RedirectAttributes redirectAttributes) {
//
//        try {
//            log.info("Przesyłanie pliku do zadania ID={} przez użytkownika {}", taskId, userDetails.getId());
//
//            // Sprawdź czy użytkownik ma dostęp do zadania
//            MeetingTaskDetailsResponse response = taskService.getTaskDetailsForUser(meetingId, taskId, userDetails.getId());
//
//            TaskFile uploadedFile;
//
//            if (response.isOrganizer()) {
//                // Organizator wrzuca plik bezpośrednio do zadania
//                uploadedFile = taskService.uploadFileToTask(taskId, file, userDetails.getId(), description);
//            } else {
//                // Dla użytkownika - sprawdź czy jest przypisany do zadania
//                // Najpierw sprawdź czy może uploadować
//                if (!taskService.canUserUploadToTask(taskId, userDetails.getId())) {
//                    throw new RuntimeException("Nie masz uprawnień do przesyłania plików do tego zadania");
//                }
//
//                // Pobierz assignment dla tego użytkownika i zadania
//                List<TaskAssignment> userAssignments = taskService.getUserAssignments(userDetails.getId());
//                TaskAssignment userAssignment = userAssignments.stream()
//                        .filter(assignment -> assignment.getTask().getId().equals(taskId))
//                        .findFirst()
//                        .orElseThrow(() -> new RuntimeException("Nie jesteś przypisany do tego zadania"));
//
//                uploadedFile = taskService.uploadFileToAssignment(userAssignment.getId(), file, userDetails.getId(), description);
//            }
//
//            redirectAttributes.addFlashAttribute("success",
//                    String.format("Plik '%s' został przesłany", uploadedFile.getOriginalFilename()));
//
//        } catch (jakarta.validation.ConstraintViolationException e) {
//            log.warn("Błąd walidacji podczas przesyłania pliku: {}", e.getMessage());
//            redirectAttributes.addFlashAttribute("error", "Nieprawidłowe dane wejściowe");
//
//        } catch (Exception e) {
//            log.error("Błąd podczas przesyłania pliku do zadania {}: {}", taskId, e.getMessage(), e);
//            redirectAttributes.addFlashAttribute("error", e.getMessage());
//        }
//
//        return "redirect:/meetings/" + meetingId + "/tasks/" + taskId + "/files";
//    }

    @GetMapping("/{taskId}/files/{fileId}/download")
    @Operation(summary = "Pobierz plik", description = "Pobiera plik przypisany do zadania.")
    public ResponseEntity<Resource> downloadTaskFile(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @PathVariable @NotNull(message = "Identyfikator zadania nie może być pusty")
            @Min(value = 1, message = "Identyfikator zadania musi być liczbą dodatnią")
            Long taskId,

            @PathVariable @NotNull(message = "Identyfikator pliku nie może być pusty")
            @Min(value = 1, message = "Identyfikator pliku musi być liczbą dodatnią")
            Long fileId,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetails userDetails) {

        try {
            log.info("Pobieranie pliku ID={} z zadania ID={}", fileId, taskId);

            // Sprawdź czy użytkownik ma dostęp do zadania
            taskService.getTaskDetailsForUser(meetingId, taskId, userDetails.getId());

            Resource resource = taskService.downloadFile(fileId, userDetails.getId());
            TaskFile taskFile = taskService.getFileById(fileId);

            String contentType = taskFile.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + taskFile.getOriginalFilename() + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("Błąd podczas pobierania pliku {}: {}", fileId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping("/{taskId}/files/{fileId}/delete")
    @Operation(summary = "Usuń plik", description = "Usuwa plik z zadania.")
    public String deleteTaskFile(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @PathVariable @NotNull(message = "Identyfikator zadania nie może być pusty")
            @Min(value = 1, message = "Identyfikator zadania musi być liczbą dodatnią")
            Long taskId,

            @PathVariable @NotNull(message = "Identyfikator pliku nie może być pusty")
            @Min(value = 1, message = "Identyfikator pliku musi być liczbą dodatnią")
            Long fileId,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            log.info("Usuwanie pliku ID={} z zadania ID={}", fileId, taskId);

            // Sprawdź czy użytkownik ma dostęp do zadania
            taskService.getTaskDetailsForUser(meetingId, taskId, userDetails.getId());

            taskService.deleteFile(fileId, userDetails.getId());
            redirectAttributes.addFlashAttribute("success", "Plik został usunięty");

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji podczas usuwania pliku: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Nieprawidłowy identyfikator pliku");

        } catch (Exception e) {
            log.error("Błąd usuwania pliku {}: {}", fileId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/meetings/" + meetingId + "/tasks/" + taskId + "/files";
    }
}












//package com.meethub.controller.web;
//
//import com.meethub.domain.model.entity.*;
//import com.meethub.domain.model.request.CreateTaskRequest;
//import com.meethub.domain.model.request.UpdateTaskRequest;
//import com.meethub.domain.model.response.*;
//import com.meethub.domain.repository.jpa.MeetingParticipantRepository;
//import com.meethub.domain.repository.jpa.MeetingRepository;
//import com.meethub.domain.service.TaskService;
//import com.meethub.security.CustomUserDetailsService.CustomUserDetails;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.Parameter;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.servlet.mvc.support.RedirectAttributes;
//
//
//@Controller
//@RequestMapping("/meetings/{meetingId}/tasks")
//@RequiredArgsConstructor
//@Tag(name = "Zadania Spotkania", description = "Zarządzanie zadaniami w ramach spotkania")
//public class MeetingTaskController {
//
//    private final TaskService taskService;
//
////    @GetMapping
////    @Operation(summary = "Lista zadań spotkania", description = "Wyświetla wszystkie zadania przypisane do spotkania.")
////    public String getMeetingTasks(
////            @Parameter(description = "ID spotkania", required = true) @PathVariable Long meetingId,
////            @AuthenticationPrincipal CustomUserDetails userDetails,
////            Model model) {
////
////        try {
////            Meeting meeting = meetingRepository.findById(meetingId)
////                    .orElseThrow(() -> new RuntimeException("Spotkanie nie zostało znalezione"));
////
////            List<Task> tasks = taskService.getMeetingTasks(meetingId);
////            boolean isOrganizer = meeting.getOrganizer().getId().equals(userDetails.getId());
////
////            model.addAttribute("meeting", meeting);
////            model.addAttribute("tasks", tasks);
////            model.addAttribute("isOrganizer", isOrganizer);
////            model.addAttribute("userId", userDetails.getId());
////
////            return "meetings/tasks/list";
////        } catch (Exception e) {
////            model.addAttribute("error", e.getMessage());
////            return "redirect:/meetings/" + meetingId;
////        }
////    }
//
//
//    @GetMapping
//    @Operation(summary = "Lista zadań spotkania", description = "Wyświetla wszystkie zadania przypisane do spotkania.")
//    public String getMeetingTasks(@PathVariable Long meetingId,
//                                  @AuthenticationPrincipal CustomUserDetails userDetails,
//                                  Model model) {
//
//        if (userDetails == null) {
//            return "redirect:/login";
//        }
//
//        try {
//            MeetingTasksResponse response = taskService.getMeetingTasksForUser(meetingId, userDetails.getId());
//
//            model.addAttribute("meeting", response.getMeeting());
//            model.addAttribute("tasks", response.getTasks());
//            model.addAttribute("isOrganizer", response.isOrganizer());
//            model.addAttribute("userId", userDetails.getId());
//
//            return "meetings/tasks/list";
//        } catch (Exception e) {
//            model.addAttribute("error", e.getMessage());
//            return "redirect:/meetings/" + meetingId;
//        }
//    }
//
//
////    @GetMapping("/create")
////    @Operation(summary = "Formularz tworzenia zadania", description = "Wyświetla formularz umożliwiający utworzenie nowego zadania. Tylko dla organizatora.")
////    public String showCreateTaskForm(
////            @Parameter(description = "ID spotkania", required = true) @PathVariable Long meetingId,
////            @AuthenticationPrincipal CustomUserDetails userDetails,
////            Model model) {
////
////        try {
////            Meeting meeting = meetingRepository.findById(meetingId)
////                    .orElseThrow(() -> new RuntimeException("Spotkanie nie zostało znalezione"));
////
////            if (!meeting.getOrganizer().getId().equals(userDetails.getId())) {
////                throw new RuntimeException("Tylko organizator może tworzyć zadania");
////            }
////
////            model.addAttribute("meeting", meeting);
////            model.addAttribute("createTaskRequest", new CreateTaskRequest());
////
////            return "meetings/tasks/create";
////        } catch (Exception e) {
////            model.addAttribute("error", e.getMessage());
////            return "redirect:/meetings/" + meetingId + "/tasks";
////        }
////    }
//
//
//    @GetMapping("/create")
//    @Operation(summary = "Formularz tworzenia zadania", description = "Wyświetla formularz umożliwiający utworzenie nowego zadania. Tylko dla organizatora.")
//    public String showCreateTaskForm(
//            @PathVariable Long meetingId,
//            @AuthenticationPrincipal CustomUserDetails userDetails,
//            Model model) {
//
//        try {
//            MeetingTaskFormResponse response = taskService.getTaskCreationFormData(meetingId, userDetails.getId());
//            model.addAttribute("meeting", response.getMeeting());
//            model.addAttribute("createTaskRequest", response.getCreateTaskRequest());
//
//            return "meetings/tasks/create";
//        } catch (Exception e) {
//            model.addAttribute("error", e.getMessage());
//            return "redirect:/meetings/" + meetingId + "/tasks";
//        }
//    }
//
//
//    @PostMapping("/create")
//    @Operation(summary = "Utwórz zadanie", description = "Tworzy nowe zadanie w spotkaniu.")
//    public String createTask(
//            @Parameter(description = "ID spotkania", required = true) @PathVariable Long meetingId,
//            @ModelAttribute CreateTaskRequest request,
//            @AuthenticationPrincipal CustomUserDetails userDetails,
//            RedirectAttributes redirectAttributes) {
//
//        try {
//            Task task = taskService.createTask(request, meetingId, userDetails.getId());
//            redirectAttributes.addFlashAttribute("success", "Zadanie zostało utworzone");
//            return "redirect:/meetings/" + meetingId + "/tasks/" + task.getId();
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("error", e.getMessage());
//            return "redirect:/meetings/" + meetingId + "/tasks/create";
//        }
//    }
//
////    @GetMapping("/{taskId}")
////    @Operation(summary = "Szczegóły zadania", description = "Wyświetla szczegóły zadania oraz uprawnienia dostępu użytkownika.")
////    public String getTaskDetails(
////            @Parameter(description = "ID spotkania", required = true) @PathVariable Long meetingId,
////            @Parameter(description = "ID zadania", required = true) @PathVariable Long taskId,
////            @AuthenticationPrincipal CustomUserDetails userDetails,
////            Model model) {
////
////        try {
////            Task task = taskService.getTaskById(taskId);
////            Meeting meeting = meetingRepository.findById(meetingId)
////                    .orElseThrow(() -> new RuntimeException("Spotkanie nie zostało znalezione"));
////
////            boolean isOrganizer = meeting.getOrganizer().getId().equals(userDetails.getId());
////            boolean canAccess = isOrganizer || task.getAssignments().stream()
////                    .anyMatch(a -> a.getUser().getId().equals(userDetails.getId()));
////
////            if (!canAccess) {
////                throw new RuntimeException("Brak uprawnień do tego zadania");
////            }
////
////            model.addAttribute("meeting", meeting);
////            model.addAttribute("task", task);
////            model.addAttribute("isOrganizer", isOrganizer);
////            model.addAttribute("userId", userDetails.getId());
////
////            return "meetings/tasks/details";
////        } catch (Exception e) {
////            model.addAttribute("error", e.getMessage());
////            return "redirect:/meetings/" + meetingId + "/tasks";
////        }
////    }
//
//
//
//    @GetMapping("/{taskId}")
//    @Operation(summary = "Szczegóły zadania", description = "Wyświetla szczegóły zadania oraz uprawnienia dostępu użytkownika.")
//    public String getTaskDetails(
//            @PathVariable Long meetingId,
//            @PathVariable Long taskId,
//            @AuthenticationPrincipal CustomUserDetails userDetails,
//            Model model) {
//
//        try {
//            MeetingTaskDetailsResponse response = taskService.getTaskDetailsForUser(meetingId, taskId, userDetails.getId());
//
//            model.addAttribute("meeting", response.getMeeting());
//            model.addAttribute("task", response.getTask());
//            model.addAttribute("isOrganizer", response.isOrganizer());
//            model.addAttribute("userId", response.getUserId());
//
//            return "meetings/tasks/details";
//        } catch (Exception e) {
//            model.addAttribute("error", e.getMessage());
//            return "redirect:/meetings/" + meetingId + "/tasks";
//        }
//    }
//
//
////    @GetMapping("/{taskId}/edit")
////    @Operation(summary = "Formularz edycji zadania", description = "Wyświetla formularz edycji zadania. Tylko organizator może edytować.")
////    public String showEditTaskForm(
////            @PathVariable Long meetingId,
////            @PathVariable Long taskId,
////            @AuthenticationPrincipal CustomUserDetails userDetails,
////            Model model) {
////
////        try {
////            Task task = taskService.getTaskById(taskId);
////            Meeting meeting = meetingRepository.findById(meetingId)
////                    .orElseThrow(() -> new RuntimeException("Spotkanie nie zostało znalezione"));
////
////            if (!meeting.getOrganizer().getId().equals(userDetails.getId())) {
////                throw new RuntimeException("Tylko organizator może edytować zadania");
////            }
////
////            String formattedDeadline = task.getDeadline().toString().replace("T", " ");
////            model.addAttribute("meeting", meeting);
////            model.addAttribute("task", task);
////            model.addAttribute("formattedDeadline", formattedDeadline);
////
////            return "meetings/tasks/edit";
////        } catch (Exception e) {
////            model.addAttribute("error", e.getMessage());
////            return "redirect:/meetings/" + meetingId + "/tasks/" + taskId;
////        }
////    }
//
//
//    @GetMapping("/{taskId}/edit")
//    @Operation(summary = "Formularz edycji zadania", description = "Wyświetla formularz edycji zadania. Tylko organizator może edytować.")
//    public String showEditTaskForm(
//            @PathVariable Long meetingId,
//            @PathVariable Long taskId,
//            @AuthenticationPrincipal CustomUserDetails userDetails,
//            Model model) {
//
//        try {
//            MeetingTaskEditResponse response = taskService.getTaskForEditing(meetingId, taskId, userDetails.getId());
//
//            model.addAttribute("meeting", response.getMeeting());
//            model.addAttribute("task", response.getTask());
//            model.addAttribute("formattedDeadline", response.getFormattedDeadline());
//
//            return "meetings/tasks/edit";
//        } catch (Exception e) {
//            model.addAttribute("error", e.getMessage());
//            return "redirect:/meetings/" + meetingId + "/tasks/" + taskId;
//        }
//    }
//
//
//
//    @PostMapping("/{taskId}/edit")
//    @Operation(summary = "Aktualizuj zadanie", description = "Aktualizuje dane zadania na podstawie formularza edycji.")
//    public String updateTask(
//            @PathVariable Long meetingId,
//            @PathVariable Long taskId,
//            @ModelAttribute UpdateTaskRequest request,
//            @AuthenticationPrincipal CustomUserDetails userDetails,
//            RedirectAttributes redirectAttributes) {
//
//        try {
//            Task task = taskService.updateTaskWithRequest(taskId, request, userDetails.getId());
//            redirectAttributes.addFlashAttribute("success", "Zadanie zostało zaktualizowane");
//            return "redirect:/meetings/" + meetingId + "/tasks/" + task.getId();
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("error", e.getMessage());
//            return "redirect:/meetings/" + meetingId + "/tasks/" + taskId + "/edit";
//        }
//    }
//
//    @PostMapping("/{taskId}/delete")
//    @Operation(summary = "Usuń zadanie", description = "Usuwa wskazane zadanie z spotkania.")
//    public String deleteTask(
//            @PathVariable Long meetingId,
//            @PathVariable Long taskId,
//            @AuthenticationPrincipal CustomUserDetails userDetails,
//            RedirectAttributes redirectAttributes) {
//
//        try {
//            taskService.deleteTask(taskId, userDetails.getId());
//            redirectAttributes.addFlashAttribute("success", "Zadanie zostało usunięte");
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("error", e.getMessage());
//        }
//
//        return "redirect:/meetings/" + meetingId + "/tasks";
//    }
//
//    @PostMapping("/{taskId}/assign-self")
//    @Operation(summary = "Przypisz siebie do zadania", description = "Pozwala użytkownikowi przypisać siebie do zadania.")
//    public String assignSelfToTask(
//            @PathVariable Long meetingId,
//            @PathVariable Long taskId,
//            @AuthenticationPrincipal CustomUserDetails userDetails,
//            RedirectAttributes redirectAttributes) {
//
//        try {
//            taskService.assignTaskToCurrentUser(taskId, userDetails.getId());
//            redirectAttributes.addFlashAttribute("success", "Zostałeś przypisany do zadania");
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("error", e.getMessage());
//        }
//
//        return "redirect:/meetings/" + meetingId + "/tasks/" + taskId;
//    }
//
//    @PostMapping("/{taskId}/assignment/{assignmentId}/comment")
//    @Operation(summary = "Dodaj/aktualizuj komentarz do przypisania", description = "Pozwala użytkownikowi dodać komentarz do przypisanego zadania.")
//    public String updateAssignmentComment(
//            @PathVariable Long meetingId,
//            @PathVariable Long taskId,
//            @PathVariable Long assignmentId,
//            @RequestParam String comment,
//            @AuthenticationPrincipal CustomUserDetails userDetails,
//            RedirectAttributes redirectAttributes) {
//
//        try {
//            taskService.updateAssignmentComment(assignmentId, comment, userDetails.getId());
//            redirectAttributes.addFlashAttribute("success", "Komentarz został zapisany");
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("error", e.getMessage());
//        }
//
//        return "redirect:/meetings/" + meetingId + "/tasks/" + taskId;
//    }
//
////    @GetMapping("/{taskId}/assign")
////    @Operation(summary = "Formularz przypisywania użytkowników", description = "Pozwala organizatorowi przypisywać użytkowników do zadania.")
////    public String showAssignUsersForm(
////            @PathVariable Long meetingId,
////            @PathVariable Long taskId,
////            @AuthenticationPrincipal CustomUserDetails userDetails,
////            Model model) {
////
////        try {
////            Task task = taskService.getTaskById(taskId);
////            Meeting meeting = meetingRepository.findById(meetingId)
////                    .orElseThrow(() -> new RuntimeException("Spotkanie nie zostało znalezione"));
////
////            if (!meeting.getOrganizer().getId().equals(userDetails.getId())) {
////                throw new RuntimeException("Tylko organizator może przypisywać użytkowników");
////            }
////
////            List<MeetingParticipant> participants = participantRepository.findByMeetingId(meetingId);
////            List<User> confirmedUsers = participants.stream()
////                    .filter(p -> p.getStatus() == com.meethub.domain.model.enums.ParticipationStatus.CONFIRMED)
////                    .map(MeetingParticipant::getUser)
////                    .collect(Collectors.toList());
////
////            List<TaskAssignment> assignments = taskService.getTaskAssignments(taskId);
////            List<User> assignedUsers = assignments.stream()
////                    .map(TaskAssignment::getUser)
////                    .collect(Collectors.toList());
////
////            List<User> availableUsers = confirmedUsers.stream()
////                    .filter(user -> !assignedUsers.contains(user))
////                    .collect(Collectors.toList());
////
////            model.addAttribute("meeting", meeting);
////            model.addAttribute("task", task);
////            model.addAttribute("availableUsers", availableUsers);
////            model.addAttribute("assignedUsers", assignedUsers);
////            model.addAttribute("assignments", assignments);
////
////            return "meetings/tasks/assign";
////        } catch (Exception e) {
////            model.addAttribute("error", e.getMessage());
////            return "redirect:/meetings/" + meetingId + "/tasks/" + taskId;
////        }
////    }
//
//
//    @GetMapping("/{taskId}/assign")
//    @Operation(summary = "Formularz przypisywania użytkowników", description = "Pozwala organizatorowi przypisywać użytkowników do zadania.")
//    public String showAssignUsersForm(
//            @PathVariable Long meetingId,
//            @PathVariable Long taskId,
//            @AuthenticationPrincipal CustomUserDetails userDetails,
//            Model model) {
//
//        try {
//            MeetingTaskAssignmentsResponse response = taskService.getTaskAssignmentsForUser(meetingId, taskId, userDetails.getId());
//
//            model.addAttribute("meeting", response.getMeeting());
//            model.addAttribute("task", response.getTask());
//            model.addAttribute("availableUsers", response.getAvailableUsers());
//            model.addAttribute("assignedUsers", response.getAssignedUsers());
//            model.addAttribute("assignments", response.getAssignments());
//
//            return "meetings/tasks/assign";
//        } catch (Exception e) {
//            model.addAttribute("error", e.getMessage());
//            return "redirect:/meetings/" + meetingId + "/tasks/" + taskId;
//        }
//    }
//
//
//    @PostMapping("/{taskId}/assign")
//    @Operation(summary = "Przypisz użytkownika do zadania", description = "Przypisuje wybranego użytkownika do zadania przez organizatora.")
//    public String assignUserToTask(
//            @PathVariable Long meetingId,
//            @PathVariable Long taskId,
//            @RequestParam Long userId,
//            @AuthenticationPrincipal CustomUserDetails userDetails,
//            RedirectAttributes redirectAttributes) {
//
//        try {
//            taskService.assignTask(taskId, userId, userDetails.getId());
//            redirectAttributes.addFlashAttribute("success", "Użytkownik został przypisany do zadania");
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("error", e.getMessage());
//        }
//
//        return "redirect:/meetings/" + meetingId + "/tasks/" + taskId + "/assign";
//    }
//
//    @PostMapping("/{taskId}/assignment/{assignmentId}/remove")
//    @Operation(summary = "Usuń przypisanie", description = "Usuwa przypisanie użytkownika z zadania.")
//    public String removeAssignment(
//            @PathVariable Long meetingId,
//            @PathVariable Long taskId,
//            @PathVariable Long assignmentId,
//            @AuthenticationPrincipal CustomUserDetails userDetails,
//            RedirectAttributes redirectAttributes) {
//
//        try {
//            taskService.removeAssignment(assignmentId, userDetails.getId());
//            redirectAttributes.addFlashAttribute("success", "Przypisanie zostało usunięte");
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("error", e.getMessage());
//        }
//
//        return "redirect:/meetings/" + meetingId + "/tasks/" + taskId + "/assign";
//    }
//
//    @PostMapping("/{taskId}/assignment/{assignmentId}/status")
//    @Operation(summary = "Zmień status przypisania", description = "Aktualizuje status przypisania zadania dla użytkownika.")
//    public String updateAssignmentStatus(
//            @PathVariable Long meetingId,
//            @PathVariable Long taskId,
//            @PathVariable Long assignmentId,
//            @RequestParam("status") String status,
//            @AuthenticationPrincipal CustomUserDetails userDetails,
//            RedirectAttributes redirectAttributes) {
//
//        try {
//            taskService.updateAssignmentStatus(assignmentId,
//                    com.meethub.domain.model.enums.AssignmentStatus.valueOf(status),
//                    userDetails.getId());
//            redirectAttributes.addFlashAttribute("success", "Status zadania został zaktualizowany");
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("error", e.getMessage());
//        }
//
//        return "redirect:/meetings/" + meetingId + "/tasks/" + taskId;
//    }
//}

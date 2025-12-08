package com.meethub.controller.web;

import com.meethub.domain.model.entity.*;
import com.meethub.domain.model.request.CreateTaskRequest;
import com.meethub.domain.model.request.UpdateTaskRequest;
import com.meethub.domain.repository.jpa.MeetingParticipantRepository;
import com.meethub.domain.repository.jpa.MeetingRepository;
import com.meethub.domain.service.TaskService;
import com.meethub.security.CustomUserDetailsService.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/meetings/{meetingId}/tasks")
@RequiredArgsConstructor
@Tag(name = "Zadania Spotkania", description = "Zarządzanie zadaniami w ramach spotkania")
public class MeetingTaskController {

    private final TaskService taskService;
    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository participantRepository;

    @GetMapping
    @Operation(summary = "Lista zadań spotkania", description = "Wyświetla wszystkie zadania przypisane do spotkania.")
    public String getMeetingTasks(
            @Parameter(description = "ID spotkania", required = true) @PathVariable Long meetingId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        try {
            Meeting meeting = meetingRepository.findById(meetingId)
                    .orElseThrow(() -> new RuntimeException("Spotkanie nie zostało znalezione"));

            List<Task> tasks = taskService.getMeetingTasks(meetingId);
            boolean isOrganizer = meeting.getOrganizer().getId().equals(userDetails.getId());

            model.addAttribute("meeting", meeting);
            model.addAttribute("tasks", tasks);
            model.addAttribute("isOrganizer", isOrganizer);
            model.addAttribute("userId", userDetails.getId());

            return "meetings/tasks/list";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/meetings/" + meetingId;
        }
    }

    @GetMapping("/create")
    @Operation(summary = "Formularz tworzenia zadania", description = "Wyświetla formularz umożliwiający utworzenie nowego zadania. Tylko dla organizatora.")
    public String showCreateTaskForm(
            @Parameter(description = "ID spotkania", required = true) @PathVariable Long meetingId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        try {
            Meeting meeting = meetingRepository.findById(meetingId)
                    .orElseThrow(() -> new RuntimeException("Spotkanie nie zostało znalezione"));

            if (!meeting.getOrganizer().getId().equals(userDetails.getId())) {
                throw new RuntimeException("Tylko organizator może tworzyć zadania");
            }

            model.addAttribute("meeting", meeting);
            model.addAttribute("createTaskRequest", new CreateTaskRequest());

            return "meetings/tasks/create";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/meetings/" + meetingId + "/tasks";
        }
    }

    @PostMapping("/create")
    @Operation(summary = "Utwórz zadanie", description = "Tworzy nowe zadanie w spotkaniu.")
    public String createTask(
            @Parameter(description = "ID spotkania", required = true) @PathVariable Long meetingId,
            @ModelAttribute CreateTaskRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            Task task = taskService.createTask(request, meetingId, userDetails.getId());
            redirectAttributes.addFlashAttribute("success", "Zadanie zostało utworzone");
            return "redirect:/meetings/" + meetingId + "/tasks/" + task.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/meetings/" + meetingId + "/tasks/create";
        }
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "Szczegóły zadania", description = "Wyświetla szczegóły zadania oraz uprawnienia dostępu użytkownika.")
    public String getTaskDetails(
            @Parameter(description = "ID spotkania", required = true) @PathVariable Long meetingId,
            @Parameter(description = "ID zadania", required = true) @PathVariable Long taskId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        try {
            Task task = taskService.getTaskById(taskId);
            Meeting meeting = meetingRepository.findById(meetingId)
                    .orElseThrow(() -> new RuntimeException("Spotkanie nie zostało znalezione"));

            boolean isOrganizer = meeting.getOrganizer().getId().equals(userDetails.getId());
            boolean canAccess = isOrganizer || task.getAssignments().stream()
                    .anyMatch(a -> a.getUser().getId().equals(userDetails.getId()));

            if (!canAccess) {
                throw new RuntimeException("Brak uprawnień do tego zadania");
            }

            model.addAttribute("meeting", meeting);
            model.addAttribute("task", task);
            model.addAttribute("isOrganizer", isOrganizer);
            model.addAttribute("userId", userDetails.getId());

            return "meetings/tasks/details";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/meetings/" + meetingId + "/tasks";
        }
    }

    @GetMapping("/{taskId}/edit")
    @Operation(summary = "Formularz edycji zadania", description = "Wyświetla formularz edycji zadania. Tylko organizator może edytować.")
    public String showEditTaskForm(
            @PathVariable Long meetingId,
            @PathVariable Long taskId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        try {
            Task task = taskService.getTaskById(taskId);
            Meeting meeting = meetingRepository.findById(meetingId)
                    .orElseThrow(() -> new RuntimeException("Spotkanie nie zostało znalezione"));

            if (!meeting.getOrganizer().getId().equals(userDetails.getId())) {
                throw new RuntimeException("Tylko organizator może edytować zadania");
            }

            String formattedDeadline = task.getDeadline().toString().replace("T", " ");
            model.addAttribute("meeting", meeting);
            model.addAttribute("task", task);
            model.addAttribute("formattedDeadline", formattedDeadline);

            return "meetings/tasks/edit";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/meetings/" + meetingId + "/tasks/" + taskId;
        }
    }

    @PostMapping("/{taskId}/edit")
    @Operation(summary = "Aktualizuj zadanie", description = "Aktualizuje dane zadania na podstawie formularza edycji.")
    public String updateTask(
            @PathVariable Long meetingId,
            @PathVariable Long taskId,
            @ModelAttribute UpdateTaskRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            Task task = taskService.updateTaskWithRequest(taskId, request, userDetails.getId());
            redirectAttributes.addFlashAttribute("success", "Zadanie zostało zaktualizowane");
            return "redirect:/meetings/" + meetingId + "/tasks/" + task.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/meetings/" + meetingId + "/tasks/" + taskId + "/edit";
        }
    }

    @PostMapping("/{taskId}/delete")
    @Operation(summary = "Usuń zadanie", description = "Usuwa wskazane zadanie z spotkania.")
    public String deleteTask(
            @PathVariable Long meetingId,
            @PathVariable Long taskId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            taskService.deleteTask(taskId, userDetails.getId());
            redirectAttributes.addFlashAttribute("success", "Zadanie zostało usunięte");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/meetings/" + meetingId + "/tasks";
    }

    @PostMapping("/{taskId}/assign-self")
    @Operation(summary = "Przypisz siebie do zadania", description = "Pozwala użytkownikowi przypisać siebie do zadania.")
    public String assignSelfToTask(
            @PathVariable Long meetingId,
            @PathVariable Long taskId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            taskService.assignTaskToCurrentUser(taskId, userDetails.getId());
            redirectAttributes.addFlashAttribute("success", "Zostałeś przypisany do zadania");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/meetings/" + meetingId + "/tasks/" + taskId;
    }

    @PostMapping("/{taskId}/assignment/{assignmentId}/comment")
    @Operation(summary = "Dodaj/aktualizuj komentarz do przypisania", description = "Pozwala użytkownikowi dodać komentarz do przypisanego zadania.")
    public String updateAssignmentComment(
            @PathVariable Long meetingId,
            @PathVariable Long taskId,
            @PathVariable Long assignmentId,
            @RequestParam String comment,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            taskService.updateAssignmentComment(assignmentId, comment, userDetails.getId());
            redirectAttributes.addFlashAttribute("success", "Komentarz został zapisany");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/meetings/" + meetingId + "/tasks/" + taskId;
    }

    @GetMapping("/{taskId}/assign")
    @Operation(summary = "Formularz przypisywania użytkowników", description = "Pozwala organizatorowi przypisywać użytkowników do zadania.")
    public String showAssignUsersForm(
            @PathVariable Long meetingId,
            @PathVariable Long taskId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        try {
            Task task = taskService.getTaskById(taskId);
            Meeting meeting = meetingRepository.findById(meetingId)
                    .orElseThrow(() -> new RuntimeException("Spotkanie nie zostało znalezione"));

            if (!meeting.getOrganizer().getId().equals(userDetails.getId())) {
                throw new RuntimeException("Tylko organizator może przypisywać użytkowników");
            }

            List<MeetingParticipant> participants = participantRepository.findByMeetingId(meetingId);
            List<User> confirmedUsers = participants.stream()
                    .filter(p -> p.getStatus() == com.meethub.domain.model.enums.ParticipationStatus.CONFIRMED)
                    .map(MeetingParticipant::getUser)
                    .collect(Collectors.toList());

            List<TaskAssignment> assignments = taskService.getTaskAssignments(taskId);
            List<User> assignedUsers = assignments.stream()
                    .map(TaskAssignment::getUser)
                    .collect(Collectors.toList());

            List<User> availableUsers = confirmedUsers.stream()
                    .filter(user -> !assignedUsers.contains(user))
                    .collect(Collectors.toList());

            model.addAttribute("meeting", meeting);
            model.addAttribute("task", task);
            model.addAttribute("availableUsers", availableUsers);
            model.addAttribute("assignedUsers", assignedUsers);
            model.addAttribute("assignments", assignments);

            return "meetings/tasks/assign";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/meetings/" + meetingId + "/tasks/" + taskId;
        }
    }

    @PostMapping("/{taskId}/assign")
    @Operation(summary = "Przypisz użytkownika do zadania", description = "Przypisuje wybranego użytkownika do zadania przez organizatora.")
    public String assignUserToTask(
            @PathVariable Long meetingId,
            @PathVariable Long taskId,
            @RequestParam Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            taskService.assignTask(taskId, userId, userDetails.getId());
            redirectAttributes.addFlashAttribute("success", "Użytkownik został przypisany do zadania");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/meetings/" + meetingId + "/tasks/" + taskId + "/assign";
    }

    @PostMapping("/{taskId}/assignment/{assignmentId}/remove")
    @Operation(summary = "Usuń przypisanie", description = "Usuwa przypisanie użytkownika z zadania.")
    public String removeAssignment(
            @PathVariable Long meetingId,
            @PathVariable Long taskId,
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            taskService.removeAssignment(assignmentId, userDetails.getId());
            redirectAttributes.addFlashAttribute("success", "Przypisanie zostało usunięte");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/meetings/" + meetingId + "/tasks/" + taskId + "/assign";
    }

    @PostMapping("/{taskId}/assignment/{assignmentId}/status")
    @Operation(summary = "Zmień status przypisania", description = "Aktualizuje status przypisania zadania dla użytkownika.")
    public String updateAssignmentStatus(
            @PathVariable Long meetingId,
            @PathVariable Long taskId,
            @PathVariable Long assignmentId,
            @RequestParam("status") String status,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            taskService.updateAssignmentStatus(assignmentId,
                    com.meethub.domain.model.enums.AssignmentStatus.valueOf(status),
                    userDetails.getId());
            redirectAttributes.addFlashAttribute("success", "Status zadania został zaktualizowany");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/meetings/" + meetingId + "/tasks/" + taskId;
    }
}

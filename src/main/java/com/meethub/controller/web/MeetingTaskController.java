package com.meethub.controller.web;

// MeetingTaskController.java

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.Task;
import com.meethub.domain.model.request.CreateTaskRequest;
import com.meethub.domain.model.request.UpdateTaskRequest;
import com.meethub.domain.repository.jpa.MeetingRepository;
import com.meethub.domain.service.TaskService;
import com.meethub.security.CustomUserDetailsService.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/meetings/{meetingId}/tasks")
@RequiredArgsConstructor
public class MeetingTaskController {

    private final TaskService taskService;
    private final MeetingRepository meetingRepository;

    @GetMapping
    public String getMeetingTasks(@PathVariable Long meetingId,
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
    public String showCreateTaskForm(@PathVariable Long meetingId,
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
    public String createTask(@PathVariable Long meetingId,
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
    public String getTaskDetails(@PathVariable Long meetingId,
                                 @PathVariable Long taskId,
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
    public String showEditTaskForm(@PathVariable Long meetingId,
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

            UpdateTaskRequest updateRequest = UpdateTaskRequest.builder()
                    .title(task.getTitle())
                    .description(task.getDescription())
                    .deadline(task.getDeadline())
                    .allowSelfAssignment(task.getAllowSelfAssignment())
//                    .maxFilesPerUser(task.getMaxFilesPerUser())
//                    .maxFileSize(task.getMaxFileSize())
                    .build();

            model.addAttribute("meeting", meeting);
            model.addAttribute("task", task);
            model.addAttribute("updateTaskRequest", updateRequest);

            return "meetings/tasks/edit";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/meetings/" + meetingId + "/tasks/" + taskId;
        }
    }

    @PostMapping("/{taskId}/edit")
    public String updateTask(@PathVariable Long meetingId,
                             @PathVariable Long taskId,
                             @ModelAttribute UpdateTaskRequest request,
                             @AuthenticationPrincipal CustomUserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        try {
            // Użyj nowej nazwy metody
            Task task = taskService.updateTaskWithRequest(taskId, request, userDetails.getId());
            redirectAttributes.addFlashAttribute("success", "Zadanie zostało zaktualizowane");
            return "redirect:/meetings/" + meetingId + "/tasks/" + task.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/meetings/" + meetingId + "/tasks/" + taskId + "/edit";
        }
    }

    @PostMapping("/{taskId}/delete")
    public String deleteTask(@PathVariable Long meetingId,
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
    public String assignSelfToTask(@PathVariable Long meetingId,
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

    @PostMapping("/{taskId}/assignment/{assignmentId}/status")
    public String updateAssignmentStatus(@PathVariable Long meetingId,
                                         @PathVariable Long taskId,
                                         @PathVariable Long assignmentId,
                                         @RequestParam String status,
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

    @PostMapping("/{taskId}/assignment/{assignmentId}/comment")
    public String updateAssignmentComment(@PathVariable Long meetingId,
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
}
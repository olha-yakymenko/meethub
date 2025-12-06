package com.meethub.controller.web;

// MeetingTaskController.java

import com.meethub.domain.model.entity.*;
import com.meethub.domain.model.request.CreateTaskRequest;
import com.meethub.domain.model.request.UpdateTaskRequest;
import com.meethub.domain.repository.jpa.MeetingParticipantRepository;
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
import java.util.stream.Collectors;

@Controller
@RequestMapping("/meetings/{meetingId}/tasks")
@RequiredArgsConstructor
public class MeetingTaskController {

    private final TaskService taskService;
    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository participantRepository;

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

//    @GetMapping("/{taskId}")
//    public String getTaskDetails(@PathVariable Long meetingId,
//                                 @PathVariable Long taskId,
//                                 @AuthenticationPrincipal CustomUserDetails userDetails,
//                                 Model model) {
//        try {
//            Task task = taskService.getTaskById(taskId);
//            Meeting meeting = meetingRepository.findById(meetingId)
//                    .orElseThrow(() -> new RuntimeException("Spotkanie nie zostało znalezione"));
//
//            boolean isOrganizer = meeting.getOrganizer().getId().equals(userDetails.getId());
//            boolean canAccess = isOrganizer || task.getAssignments().stream()
//                    .anyMatch(a -> a.getUser().getId().equals(userDetails.getId()));
//
//            if (!canAccess) {
//                throw new RuntimeException("Brak uprawnień do tego zadania");
//            }
//
//            model.addAttribute("meeting", meeting);
//            model.addAttribute("task", task);
//            model.addAttribute("isOrganizer", isOrganizer);
//            model.addAttribute("userId", userDetails.getId());
//
//            return "meetings/tasks/details";
//        } catch (Exception e) {
//            model.addAttribute("error", e.getMessage());
//            return "redirect:/meetings/" + meetingId + "/tasks";
//        }
//    }



    @GetMapping("/{taskId}")
    public String getTaskDetails(@PathVariable Long meetingId,
                                 @PathVariable Long taskId,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 Model model) {
        try {
            System.out.println("DEBUG: Getting task details for taskId=" + taskId);
            Task task = taskService.getTaskById(taskId);
            System.out.println("DEBUG: Task found: " + task.getTitle());

            Meeting meeting = meetingRepository.findById(meetingId)
                    .orElseThrow(() -> new RuntimeException("Spotkanie nie zostało znalezione"));

            boolean isOrganizer = meeting.getOrganizer().getId().equals(userDetails.getId());
            System.out.println("DEBUG: Is organizer: " + isOrganizer);
            System.out.println("DEBUG: User ID: " + userDetails.getId());

            boolean canAccess = isOrganizer || task.getAssignments().stream()
                    .anyMatch(a -> a.getUser().getId().equals(userDetails.getId()));

            System.out.println("DEBUG: Can access: " + canAccess);

            if (!canAccess) {
                System.out.println("DEBUG: Access denied!");
                throw new RuntimeException("Brak uprawnień do tego zadania");
            }

            model.addAttribute("meeting", meeting);
            model.addAttribute("task", task);
            model.addAttribute("isOrganizer", isOrganizer);
            model.addAttribute("userId", userDetails.getId());

            System.out.println("DEBUG: All attributes added successfully");

            return "meetings/tasks/details";
        } catch (Exception e) {
            System.out.println("DEBUG: Error in getTaskDetails: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", e.getMessage());
            return "redirect:/meetings/" + meetingId + "/tasks";
        }
    }



//    @GetMapping("/{taskId}/edit")
//    public String showEditTaskForm(@PathVariable Long meetingId,
//                                   @PathVariable Long taskId,
//                                   @AuthenticationPrincipal CustomUserDetails userDetails,
//                                   Model model) {
//        try {
//            Task task = taskService.getTaskById(taskId);
//            Meeting meeting = meetingRepository.findById(meetingId)
//                    .orElseThrow(() -> new RuntimeException("Spotkanie nie zostało znalezione"));
//
//            if (!meeting.getOrganizer().getId().equals(userDetails.getId())) {
//                throw new RuntimeException("Tylko organizator może edytować zadania");
//            }
//
//            UpdateTaskRequest updateRequest = UpdateTaskRequest.builder()
//                    .title(task.getTitle())
//                    .description(task.getDescription())
//                    .deadline(task.getDeadline())
//                    .allowSelfAssignment(task.getAllowSelfAssignment())
////                    .maxFilesPerUser(task.getMaxFilesPerUser())
////                    .maxFileSize(task.getMaxFileSize())
//                    .build();
//
//            model.addAttribute("meeting", meeting);
//            model.addAttribute("task", task);
//            model.addAttribute("updateTaskRequest", updateRequest);
//
//            return "meetings/tasks/edit";
//        } catch (Exception e) {
//            model.addAttribute("error", e.getMessage());
//            return "redirect:/meetings/" + meetingId + "/tasks/" + taskId;
//        }
//    }

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

//    @PostMapping("/{taskId}/assignment/{assignmentId}/status")
//    public String updateAssignmentStatus(@PathVariable Long meetingId,
//                                         @PathVariable Long taskId,
//                                         @PathVariable Long assignmentId,
//                                         @RequestParam String status,
//                                         @AuthenticationPrincipal CustomUserDetails userDetails,
//                                         RedirectAttributes redirectAttributes) {
//        try {
//            taskService.updateAssignmentStatus(assignmentId,
//                    com.meethub.domain.model.enums.AssignmentStatus.valueOf(status),
//                    userDetails.getId());
//            redirectAttributes.addFlashAttribute("success", "Status zadania został zaktualizowany");
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("error", e.getMessage());
//        }
//        return "redirect:/meetings/" + meetingId + "/tasks/" + taskId;
//    }

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







    @GetMapping("/{taskId}/assign")
    public String showAssignUsersForm(@PathVariable Long meetingId,
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

            // Pobierz potwierdzonych uczestników spotkania
            List<MeetingParticipant> participants = participantRepository.findByMeetingId(meetingId);
            List<User> confirmedUsers = participants.stream()
                    .filter(p -> p.getStatus() == com.meethub.domain.model.enums.ParticipationStatus.CONFIRMED)
                    .map(MeetingParticipant::getUser)
                    .collect(Collectors.toList());

            // Pobierz już przypisanych użytkowników
            List<TaskAssignment> assignments = taskService.getTaskAssignments(taskId);
            List<User> assignedUsers = assignments.stream()
                    .map(TaskAssignment::getUser)
                    .collect(Collectors.toList());

            // Dostępni użytkownicy = potwierdzeni uczestnicy - już przypisani
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
    public String assignUserToTask(@PathVariable Long meetingId,
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
    public String removeAssignment(@PathVariable Long meetingId,
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

    // Poprawiona metoda do edycji statusu (dodaj parametr status)
    @PostMapping("/{taskId}/assignment/{assignmentId}/status")
    public String updateAssignmentStatus(@PathVariable Long meetingId,
                                         @PathVariable Long taskId,
                                         @PathVariable Long assignmentId,
                                         @RequestParam("status") String status, // Dodaj @RequestParam
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

    @GetMapping("/{taskId}/edit")
    public String showEditTaskForm(@PathVariable Long meetingId,
                                   @PathVariable Long taskId,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   Model model) {
        try {
            System.out.println("DEBUG: Loading edit form for task: " + taskId);
            Task task = taskService.getTaskById(taskId);
            System.out.println("DEBUG: Task title: " + task.getTitle());
            System.out.println("DEBUG: Task deadline: " + task.getDeadline());
            System.out.println("DEBUG: Task allowSelfAssignment: " + task.getAllowSelfAssignment());

            Meeting meeting = meetingRepository.findById(meetingId)
                    .orElseThrow(() -> new RuntimeException("Spotkanie nie zostało znalezione"));

            if (!meeting.getOrganizer().getId().equals(userDetails.getId())) {
                throw new RuntimeException("Tylko organizator może edytować zadania");
            }

            // Przekształć LocalDateTime na String w formacie wymaganym przez input datetime-local
            String formattedDeadline = task.getDeadline().toString().replace("T", " ");
            model.addAttribute("meeting", meeting);
            model.addAttribute("task", task);
            model.addAttribute("formattedDeadline", formattedDeadline); // Dodaj formatowaną datę

            System.out.println("DEBUG: Formatted deadline: " + formattedDeadline);
            System.out.println("DEBUG: Attributes added to model");

            return "meetings/tasks/edit";
        } catch (Exception e) {
            System.out.println("DEBUG: Error in edit form: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", e.getMessage());
            return "redirect:/meetings/" + meetingId + "/tasks/" + taskId;
        }
    }
}
package com.meethub.controller.api;

import com.meethub.domain.service.MeetingMarkService;
import com.meethub.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/meetings")
@RequiredArgsConstructor
public class MeetingMarkController {

    private final MeetingMarkService meetingMarkService;

    @PostMapping("/{meetingId}/important")
    @PreAuthorize("isAuthenticated()")
    public String markAsImportant(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
            @PathVariable Long meetingId,
            RedirectAttributes redirectAttributes) {

        Long currentUserId = userDetails.getId();
        log.info("User {} marking meeting {} as important", currentUserId, meetingId);

        try {
            meetingMarkService.markAsImportant(currentUserId, meetingId);
            redirectAttributes.addFlashAttribute("success", "Spotkanie oznaczone jako ważne");
        } catch (Exception e) {
            log.error("Error marking meeting as important", e);
            redirectAttributes.addFlashAttribute("error", "Nie udało się oznaczyć spotkania jako ważne: " + e.getMessage());
        }

        return "redirect:/meetings/" + meetingId;
    }

    @PostMapping("/{meetingId}/important/toggle")
    @PreAuthorize("isAuthenticated()")
    public String toggleImportant(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
            @PathVariable Long meetingId,
            RedirectAttributes redirectAttributes) {

        Long currentUserId = userDetails.getId();
        log.info("User {} toggling important status for meeting {}", currentUserId, meetingId);

        try {
            boolean newStatus = meetingMarkService.toggleImportant(currentUserId, meetingId);
            String message = newStatus ?
                    "Spotkanie oznaczone jako ważne" : "Spotkanie odznaczone z ważnych";
            redirectAttributes.addFlashAttribute("success", message);
        } catch (Exception e) {
            log.error("Error toggling important status", e);
            redirectAttributes.addFlashAttribute("error", "Nie udało się zmienić statusu: " + e.getMessage());
        }

        return "redirect:/meetings/" + meetingId;
    }

    @DeleteMapping("/{meetingId}/important")
    @PreAuthorize("isAuthenticated()")
    public String unmarkAsImportant(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
            @PathVariable Long meetingId,
            RedirectAttributes redirectAttributes) {

        Long currentUserId = userDetails.getId();
        log.info("User {} unmarking meeting {} as important", currentUserId, meetingId);

        try {
            meetingMarkService.unmarkAsImportant(currentUserId, meetingId);
            redirectAttributes.addFlashAttribute("success", "Spotkanie odznaczone z ważnych");
        } catch (Exception e) {
            log.error("Error unmarking meeting as important", e);
            redirectAttributes.addFlashAttribute("error", "Nie udało się odznaczyć spotkania: " + e.getMessage());
        }

        return "redirect:/meetings/" + meetingId;
    }

    @GetMapping("/{meetingId}/important/check")
    @PreAuthorize("isAuthenticated()")
    public String isMeetingImportant(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
            @PathVariable Long meetingId,
            Model model) {

        Long currentUserId = userDetails.getId();
        log.debug("Checking if meeting {} is important for user {}", meetingId, currentUserId);

        boolean isImportant = meetingMarkService.isMeetingImportantForUser(currentUserId, meetingId);
        model.addAttribute("isImportant", isImportant);

        // Możesz tu zwrócić fragment widoku zamiast pełnego przekierowania
        return "fragments :: important-status"; // przykładowy fragment
    }

    @GetMapping("/important")
    @PreAuthorize("isAuthenticated()")
    public String getImportantMeetings(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
            Model model) {

        Long currentUserId = userDetails.getId();
        log.debug("Fetching important meetings for user {}", currentUserId);

        // Możesz pobrać pełne obiekty spotkań zamiast tylko ID
        // List<Meeting> importantMeetings = meetingMarkService.getImportantMeetingsForUser(currentUserId);
        List<Long> importantMeetingIds = meetingMarkService.getImportantMeetingIdsForUser(currentUserId);

        model.addAttribute("importantMeetingIds", importantMeetingIds);
        model.addAttribute("count", importantMeetingIds.size());

        return "meetings/important"; // widok z listą ważnych spotkań
    }
}




//package com.meethub.controller.api;
//
//import com.meethub.domain.service.MeetingMarkService;
//import com.meethub.security.CustomUserDetailsService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.Parameter;
//import io.swagger.v3.oas.annotations.responses.ApiResponse;
//import io.swagger.v3.oas.annotations.responses.ApiResponses;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import jakarta.validation.Valid;
//import jakarta.validation.constraints.Positive;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@Slf4j
//@RestController
//@RequestMapping("/api/v1/meetings")
//@RequiredArgsConstructor
//@Tag(name = "Important Meetings", description = "API do zarządzania ważnymi spotkaniami")
//public class MeetingMarkController {
//
//    private final MeetingMarkService meetingMarkService;
//
//    // ================ ENDPOINTY DLA KONKRETNEGO SPOTKANIA ================
//
//    @Operation(
//            summary = "Oznacz spotkanie jako ważne",
//            description = "Dodaje spotkanie do listy ważnych spotkań użytkownika"
//    )
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "Spotkanie oznaczone jako ważne"),
//            @ApiResponse(responseCode = "401", description = "Nieautoryzowany dostęp"),
//            @ApiResponse(responseCode = "404", description = "Spotkanie nie istnieje")
//    })
//    @PostMapping("/{meetingId}/important")
//    @PreAuthorize("isAuthenticated()")
//    public ResponseEntity<Map<String, Object>> markAsImportant(
//            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails, // ZMIEŃ TUTAJ
//            @PathVariable @Valid @Positive Long meetingId) {
//
//        // Pobierz ID użytkownika z CustomUserDetails
//        Long currentUserId = userDetails.getId();
//
//        log.info("User {} marking meeting {} as important", currentUserId, meetingId);
//
//        meetingMarkService.markAsImportant(currentUserId, meetingId);
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("success", true);
//        response.put("message", "Meeting marked as important");
//        response.put("meetingId", meetingId);
//        response.put("userId", currentUserId);
//
//        return ResponseEntity.ok(response);
//    }
//
//    @Operation(
//            summary = "Odznacz spotkanie jako ważne",
//            description = "Usuwa spotkanie z listy ważnych spotkań użytkownika"
//    )
//    @DeleteMapping("/{meetingId}/important")
//    @PreAuthorize("isAuthenticated()")
//    public ResponseEntity<Map<String, Object>> unmarkAsImportant(
//            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails, // ZMIEŃ TUTAJ
//            @PathVariable @Valid @Positive Long meetingId) {
//
//        Long currentUserId = userDetails.getId();
//
//        log.info("User {} unmarking meeting {} as important", currentUserId, meetingId);
//
//        meetingMarkService.unmarkAsImportant(currentUserId, meetingId);
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("success", true);
//        response.put("message", "Meeting unmarked");
//        response.put("meetingId", meetingId);
//        response.put("userId", currentUserId);
//
//        return ResponseEntity.ok(response);
//    }
//
//    @Operation(
//            summary = "Sprawdź czy spotkanie jest ważne",
//            description = "Zwraca informację czy spotkanie jest oznaczone jako ważne dla zalogowanego użytkownika"
//    )
//    @GetMapping("/{meetingId}/important")
//    @PreAuthorize("isAuthenticated()")
//    public ResponseEntity<Map<String, Object>> isMeetingImportant(
//            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails, // ZMIEŃ TUTAJ
//            @PathVariable @Valid @Positive Long meetingId) {
//
//        Long currentUserId = userDetails.getId();
//
//        log.debug("Checking if meeting {} is important for user {}", meetingId, currentUserId);
//
//        boolean isImportant = meetingMarkService.isMeetingImportantForUser(currentUserId, meetingId);
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("isImportant", isImportant);
//        response.put("meetingId", meetingId);
//        response.put("userId", currentUserId);
//
//        return ResponseEntity.ok(response);
//    }
//
//    @Operation(
//            summary = "Przełącz status ważności spotkania",
//            description = "Jeśli spotkanie jest ważne - odznacza, jeśli nie jest - oznacza jako ważne"
//    )
//    @PostMapping("/{meetingId}/important/toggle")
//    @PreAuthorize("isAuthenticated()")
//    public ResponseEntity<Map<String, Object>> toggleImportant(
//            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails, // ZMIEŃ TUTAJ
//            @PathVariable @Valid @Positive Long meetingId) {
//
//        Long currentUserId = userDetails.getId();
//
//        log.info("User {} toggling important status for meeting {}", currentUserId, meetingId);
//
//        boolean newStatus = meetingMarkService.toggleImportant(currentUserId, meetingId);
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("success", true);
//        response.put("isImportant", newStatus);
//        response.put("message", newStatus ?
//                "Meeting marked as important" : "Meeting unmarked");
//        response.put("meetingId", meetingId);
//        response.put("userId", currentUserId);
//
//        return ResponseEntity.ok(response);
//    }
//
//    // ================ ENDPOINTY DLA LISTY SPOTKAŃ ================
//
//    @Operation(
//            summary = "Pobierz listę ważnych spotkań",
//            description = "Zwraca listę ID spotkań oznaczonych jako ważne dla zalogowanego użytkownika"
//    )
//    @GetMapping("/important")
//    @PreAuthorize("isAuthenticated()")
//    public ResponseEntity<Map<String, Object>> getImportantMeetings(
//            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) { // ZMIEŃ TUTAJ
//
//        Long currentUserId = userDetails.getId();
//
//        log.debug("Fetching important meetings for user {}", currentUserId);
//
//        List<Long> importantMeetingIds = meetingMarkService.getImportantMeetingIdsForUser(currentUserId);
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("importantMeetingIds", importantMeetingIds);
//        response.put("count", importantMeetingIds.size());
//        response.put("userId", currentUserId);
//
//        return ResponseEntity.ok(response);
//    }
//
//
//    @Operation(
//            summary = "Oznacz wiele spotkań jako ważne",
//            description = "Dodaje listę spotkań do ważnych spotkań użytkownika"
//    )
//    @PostMapping("/important/batch")
//    @PreAuthorize("isAuthenticated()")
//    public ResponseEntity<Map<String, Object>> batchMarkAsImportant(
//            @AuthenticationPrincipal Long currentUserId,
//            @RequestBody @Valid BatchImportantRequest request) {
//
//        log.info("User {} marking {} meetings as important", currentUserId, request.getMeetingIds().size());
//
//        for (Long meetingId : request.getMeetingIds()) {
//            try {
//                meetingMarkService.markAsImportant(currentUserId, meetingId);
//            } catch (Exception e) {
//                log.warn("Failed to mark meeting {} as important: {}", meetingId, e.getMessage());
//            }
//        }
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("success", true);
//        response.put("message", String.format("%d meetings processed", request.getMeetingIds().size()));
//        response.put("userId", currentUserId);
//
//        return ResponseEntity.ok(response);
//    }
//
//    @Operation(
//            summary = "Sprawdź które spotkania są ważne",
//            description = "Zwraca mapę spotkanie->status dla podanej listy spotkań"
//    )
//    @PostMapping("/important/check-batch")
//    @PreAuthorize("isAuthenticated()")
//    public ResponseEntity<Map<String, Object>> batchCheckImportant(
//            @AuthenticationPrincipal Long currentUserId,
//            @RequestBody @Valid BatchImportantRequest request) {
//
//        log.debug("Batch checking importance for {} meetings for user {}",
//                request.getMeetingIds().size(), currentUserId);
//
//        Map<Long, Boolean> importanceMap = new HashMap<>();
//        for (Long meetingId : request.getMeetingIds()) {
//            boolean isImportant = meetingMarkService.isMeetingImportantForUser(currentUserId, meetingId);
//            importanceMap.put(meetingId, isImportant);
//        }
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("importanceMap", importanceMap);
//        response.put("userId", currentUserId);
//
//        return ResponseEntity.ok(response);
//    }
//
//    // ================ DTO KLASY ================
//
//    public static class BatchImportantRequest {
//        @io.swagger.v3.oas.annotations.media.Schema(
//                description = "Lista ID spotkań",
//                example = "[1, 2, 3]",
//                required = true
//        )
//        private List<@Positive Long> meetingIds;
//
//        // Getters & Setters
//        public List<Long> getMeetingIds() {
//            return meetingIds;
//        }
//
//        public void setMeetingIds(List<Long> meetingIds) {
//            this.meetingIds = meetingIds;
//        }
//    }
//}
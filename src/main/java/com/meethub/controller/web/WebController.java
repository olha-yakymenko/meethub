//////package com.meethub.controller.web;
//////
//////import com.meethub.domain.model.request.UserRegistrationRequest;
//////import com.meethub.domain.model.response.DashboardStatsResponse;
//////import com.meethub.domain.model.response.MeetingResponse;
//////import com.meethub.domain.model.response.UserResponse;
//////import com.meethub.domain.service.AuthService;
//////import com.meethub.domain.service.DashboardService;
//////import com.meethub.domain.service.MeetingService;
//////import jakarta.validation.Valid;
//////import lombok.RequiredArgsConstructor;
//////import org.springframework.data.domain.Page;
//////import org.springframework.data.domain.PageRequest;
//////import org.springframework.data.domain.Pageable;
//////import org.springframework.security.core.annotation.AuthenticationPrincipal;
//////import org.springframework.stereotype.Controller;
//////import org.springframework.ui.Model;
//////import org.springframework.validation.BindingResult;
//////import org.springframework.web.bind.annotation.*;
//////import org.springframework.web.servlet.mvc.support.RedirectAttributes;
//////
//////import java.util.List;
//////
//////@Controller
//////@RequiredArgsConstructor
//////public class WebController {
//////
//////    private final MeetingService meetingService;
//////    private final AuthService authService;
//////    private final DashboardService dashboardService;
//////
//////    @GetMapping("/")
//////    public String home(Model model) {
//////        // Zawsze pokazuj publiczną stronę główną
//////        // Thymeleaf sam zadba o różne widoki dla zalogowanych/niezalogowanych
//////        List<MeetingResponse> upcomingMeetings = meetingService.getUpcomingPublicMeetings();
//////        model.addAttribute("upcomingMeetings", upcomingMeetings);
//////        model.addAttribute("totalMeetings", upcomingMeetings.size());
//////        return "index";
//////    }
//////
//////    @GetMapping("/meetings")
//////    public String meetings(
//////            @RequestParam(defaultValue = "0") int page,
//////            @RequestParam(defaultValue = "12") int size,
//////            Model model) {
//////
//////        Pageable pageable = PageRequest.of(page, size);
//////        Page<MeetingResponse> meetingsPage = meetingService.getUserMeetings(1L, pageable);
//////
//////        model.addAttribute("meetings", meetingsPage.getContent());
//////        model.addAttribute("currentPage", page);
//////        model.addAttribute("totalPages", meetingsPage.getTotalPages());
//////        model.addAttribute("totalItems", meetingsPage.getTotalElements());
//////
//////        return "meetings/list";
//////    }
//////
//////    @GetMapping("/meetings/{id}")
//////    public String meetingDetails(@PathVariable Long id, Model model) {
//////        MeetingResponse meeting = meetingService.getMeetingById(id);
//////        model.addAttribute("meeting", meeting);
//////        return "meetings/details.html";
//////    }
//////
//////    @GetMapping("/my-meetings")
//////    public String myMeetings(
//////            @AuthenticationPrincipal Long userId,
//////            @RequestParam(defaultValue = "0") int page,
//////            @RequestParam(defaultValue = "10") int size,
//////            Model model) {
//////
//////        Pageable pageable = PageRequest.of(page, size);
//////        Page<MeetingResponse> myMeetings = meetingService.getUserMeetings(userId, pageable);
//////
//////        model.addAttribute("meetings", myMeetings.getContent());
//////        model.addAttribute("currentPage", page);
//////        model.addAttribute("totalPages", myMeetings.getTotalPages());
//////        model.addAttribute("totalItems", myMeetings.getTotalElements());
//////
//////        return "meetings/my-meetings";
//////    }
//////
//////    @GetMapping("/login")
//////    public String login(
//////            @RequestParam(value = "error", required = false) String error,
//////            @RequestParam(value = "logout", required = false) String logout,
//////            Model model) {
//////
//////        if (error != null) {
//////            model.addAttribute("error", "Nieprawidłowy email lub hasło");
//////        }
//////        if (logout != null) {
//////            model.addAttribute("message", "Zostałeś pomyślnie wylogowany");
//////        }
//////
//////        return "auth/login";
//////    }
//////
//////    @GetMapping("/register")
//////    public String showRegistrationForm(Model model) {
//////        model.addAttribute("registrationRequest", new UserRegistrationRequest());
//////        return "auth/register";
//////    }
//////
//////    @PostMapping("/register")
//////    public String registerUser(
//////            @Valid @ModelAttribute("registrationRequest") UserRegistrationRequest request,
//////            BindingResult result,
//////            Model model,
//////            RedirectAttributes redirectAttributes) {
//////
//////        if (result.hasErrors()) {
//////            return "auth/register";
//////        }
//////
//////        if (!request.getPassword().equals(request.getConfirmPassword())) {
//////            model.addAttribute("error", "Hasła nie są identyczne");
//////            return "auth/register";
//////        }
//////
//////        try {
//////            UserResponse user = authService.register(request);
//////
//////            redirectAttributes.addFlashAttribute("message",
//////                    "Rejestracja zakończona sukcesem! Możesz się teraz zalogować.");
//////            return "redirect:/login";
//////
//////        } catch (Exception e) {
//////            model.addAttribute("error", "Błąd rejestracji: " + e.getMessage());
//////            return "auth/register";
//////        }
//////    }
//////
//////    @GetMapping("/profile")
//////    public String profile(@AuthenticationPrincipal Long userId, Model model) {
//////        model.addAttribute("userId", userId);
//////        return "user/profile";
//////    }
//////
//////    @GetMapping("/dashboard")
//////    public String dashboard(@AuthenticationPrincipal Long userId, Model model) {
//////        try {
//////            DashboardStatsResponse stats = dashboardService.getUserDashboardStats(userId);
//////            model.addAttribute("stats", stats);
//////
//////            Page<MeetingResponse> recentMeetings = meetingService.getUserMeetings(userId, PageRequest.of(0, 5));
//////            model.addAttribute("recentMeetings", recentMeetings.getContent());
//////
//////        } catch (Exception e) {
//////            DashboardStatsResponse fallbackStats = DashboardStatsResponse.builder()
//////                    .totalMeetings(0L)
//////                    .upcomingMeetings(0L)
//////                    .participantsCount(0L)
//////                    .organizedMeetings(0L)
//////                    .build();
//////            model.addAttribute("stats", fallbackStats);
//////            model.addAttribute("recentMeetings", List.of());
//////            model.addAttribute("warning", "Nie udało się załadować statystyk");
//////        }
//////
//////        return "dashboard";
//////    }
//////}
////
////
////
////
////
////
////
////
////
////
////
////
////package com.meethub.controller.web;
////
////import com.meethub.domain.model.entity.User;
////import com.meethub.domain.model.request.CreateMeetingRequest;
////import com.meethub.domain.model.request.UpdateMeetingRequest;
////import com.meethub.domain.model.request.UserRegistrationRequest;
////import com.meethub.domain.model.response.DashboardStatsResponse;
////import com.meethub.domain.model.response.MeetingResponse;
////import com.meethub.domain.model.response.NotificationResponse;
////import com.meethub.domain.model.response.UserResponse;
////import com.meethub.domain.repository.jpa.UserRepository;
////import com.meethub.domain.service.*;
////import com.meethub.security.CustomUserDetailsService.CustomUserDetails;
////import jakarta.validation.Valid;
////import lombok.RequiredArgsConstructor;
////import lombok.extern.slf4j.Slf4j;
////import org.springframework.data.domain.Page;
////import org.springframework.data.domain.PageRequest;
////import org.springframework.data.domain.Pageable;
////import org.springframework.http.HttpStatus;
////import org.springframework.http.ResponseEntity;
////import org.springframework.security.core.annotation.AuthenticationPrincipal;
////import org.springframework.stereotype.Controller;
////import org.springframework.ui.Model;
////import org.springframework.validation.BindingResult;
////import org.springframework.web.bind.annotation.*;
////import org.springframework.web.servlet.mvc.support.RedirectAttributes;
////
////import java.util.HashMap;
////import java.util.List;
////import java.util.Map;
////
////@Slf4j
////@Controller
////@RequiredArgsConstructor
////public class WebController {
////
////    private final MeetingService meetingService;
////    private final AuthService authService;
////    private final DashboardService dashboardService;
////    private final MeetingParticipantService participantService;
////    private final UserRepository userRepository;
////    private final UserService userService;
////    private final NotificationService notificationService;
////
////    // METODY POMOCNICZE - MUSZĄ BYĆ NA POCZĄTKU!
////    @ModelAttribute("currentUserId")
////    public Long getCurrentUserId(@AuthenticationPrincipal CustomUserDetails userDetails) {
////        return userDetails != null ? userDetails.getId() : null;
////    }
////
////    @ModelAttribute("currentUser")
////    public String getCurrentUserName(@AuthenticationPrincipal CustomUserDetails userDetails) {
////        return userDetails != null ? userDetails.getFirstName() + " " + userDetails.getLastName() : null;
////    }
////
////    @ModelAttribute("currentUserEntity")
////    public User getCurrentUserEntity(@AuthenticationPrincipal CustomUserDetails userDetails) {
////        if (userDetails == null) return null;
////        return userRepository.findById(userDetails.getId()).orElse(null);
////    }
////
////    @GetMapping("/")
////    public String home(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
////        if (userDetails != null) {
////            // Użytkownik zalogowany - pokaż dashboard
////            model.addAttribute("pageTitle", "Dashboard");
////            model.addAttribute("content", "dashboard");
////            return "layout";
////        } else {
////            // Użytkownik niezalogowany - pokaż stronę główną
////            List<MeetingResponse> upcomingMeetings = meetingService.getUpcomingPublicMeetings();
////            model.addAttribute("upcomingMeetings", upcomingMeetings);
////            model.addAttribute("totalMeetings", upcomingMeetings.size());
////            return "index";
////        }
////    }
////
////    @GetMapping("/meetings")
////    public String meetings(
////            @RequestParam(defaultValue = "0") int page,
////            @RequestParam(defaultValue = "12") int size,
////            @RequestParam(required = false) String search,
////            @RequestParam(required = false) String type,
////            @RequestParam(required = false) String status,
////            Model model) {
////
////        Pageable pageable = PageRequest.of(page, size);
////        Page<MeetingResponse> meetingsPage = meetingService.getFilteredMeetings(search, type, status, pageable);
////
////        model.addAttribute("meetings", meetingsPage.getContent());
////        model.addAttribute("currentPage", page);
////        model.addAttribute("totalPages", meetingsPage.getTotalPages());
////        model.addAttribute("totalItems", meetingsPage.getTotalElements());
////        model.addAttribute("search", search);
////        model.addAttribute("type", type);
////        model.addAttribute("status", status);
////
////        return "meetings/list";
////    }
////
////    @GetMapping("/meetings/{id}")
////    public String meetingDetails(@PathVariable Long id,
////                                 @AuthenticationPrincipal CustomUserDetails userDetails,
////                                 Model model) {
////        MeetingResponse meeting = meetingService.getMeetingById(id);
////        model.addAttribute("meeting", meeting);
////
////        Long userId = userDetails != null ? userDetails.getId() : null;
////        model.addAttribute("userId", userId);
////
////        if (userId != null) {
////            // Sprawdź czy użytkownik jest uczestnikiem
////            boolean isParticipant = participantService.isUserParticipant(userId, id);
////            model.addAttribute("isParticipant", isParticipant);
////        } else {
////            model.addAttribute("isParticipant", false);
////        }
////
////        // Pobierz listę uczestników
////        var participants = participantService.getMeetingParticipants(id);
////        model.addAttribute("participants", participants);
////
////        return "meetings/details.html";
////    }
////
////    @GetMapping("/meetings/create")
////    public String showCreateMeetingForm(Model model) {
////        model.addAttribute("createMeetingRequest", new CreateMeetingRequest());
////        return "meetings/create";
////    }
////
//////    @PostMapping("/meetings/create")
//////    public String createMeeting(
//////            @Valid @ModelAttribute("createMeetingRequest") CreateMeetingRequest request,
//////            BindingResult result,
//////            @AuthenticationPrincipal CustomUserDetails userDetails,
//////            Model model,
//////            RedirectAttributes redirectAttributes) {
//////
//////        log.info("=== MEETING CREATION START ===");
//////        log.info("UserDetails: {}", userDetails != null ? "Present" : "NULL");
//////
//////        if (userDetails == null) {
//////            log.error("User not authenticated");
//////            return "redirect:/login";
//////        }
//////
//////        Long userId = userDetails.getId();
//////        log.info("User ID from CustomUserDetails: {}", userId);
//////
//////        if (result.hasErrors()) {
//////            log.error("Validation errors: {}", result.getAllErrors());
//////            return "meetings/create";
//////        }
//////
//////        try {
//////            MeetingResponse meeting = meetingService.createMeeting(request, userId);
//////            redirectAttributes.addFlashAttribute("message",
//////                    "Spotkanie '" + meeting.getTitle() + "' zostało utworzone pomyślnie!");
//////            return "redirect:/meetings/" + meeting.getId();
//////
//////        } catch (Exception e) {
//////            log.error("Error creating meeting: {}", e.getMessage(), e);
//////            model.addAttribute("error", "Błąd podczas tworzenia spotkania: " + e.getMessage());
//////            return "meetings/create";
//////        }
//////    }
////
////
////    @PostMapping("/meetings/create")
////    public String createMeeting(
////            @Valid @ModelAttribute("createMeetingRequest") CreateMeetingRequest request,
////            BindingResult result,
////            @AuthenticationPrincipal CustomUserDetails userDetails,
////            Model model,
////            RedirectAttributes redirectAttributes) {
////
////        if (userDetails == null) {
////            return "redirect:/login";
////        }
////
////        Long userId = userDetails.getId();
////
////        if (result.hasErrors()) {
////            return "meetings/create";
////        }
////
////        try {
////            MeetingResponse meeting = meetingService.createMeeting(request, userId);
////            redirectAttributes.addFlashAttribute("message",
////                    "Spotkanie '" + meeting.getTitle() + "' zostało utworzone pomyślnie!");
////            return "redirect:/meetings/" + meeting.getId();
////
////        } catch (Exception e) {
////            model.addAttribute("error", "Błąd podczas tworzenia spotkania: " + e.getMessage());
////            return "meetings/create";
////        }
////    }
////
////    @GetMapping("/meetings/{id}/edit")
////    public String showEditMeetingForm(@PathVariable Long id,
////                                      @AuthenticationPrincipal CustomUserDetails userDetails,
////                                      Model model) {
////        if (userDetails == null) {
////            return "redirect:/login";
////        }
////
////        try {
////            MeetingResponse meeting = meetingService.getMeetingById(id);
////            Long userId = userDetails.getId();
////
////            // Sprawdź czy użytkownik jest organizatorem
////            if (!meeting.getOrganizer().getId().equals(userId)) {
////                return "redirect:/meetings/" + id + "?error=Nie masz uprawnień do edycji tego spotkania";
////            }
////
////            UpdateMeetingRequest updateRequest = UpdateMeetingRequest.builder()
////                    .title(meeting.getTitle())
////                    .description(meeting.getDescription())
////                    .agenda(meeting.getAgenda())
////                    .type(meeting.getType())
////                    .visibility(meeting.getVisibility())
////                    .startDate(meeting.getStartDate())
////                    .endDate(meeting.getEndDate())
////                    .maxParticipants(meeting.getMaxParticipants())
////                    .tags(meeting.getTags())
////                    .build();
////
////            model.addAttribute("updateMeetingRequest", updateRequest);
////            model.addAttribute("meetingId", id);
////            return "meetings/edit";
////
////        } catch (Exception e) {
////            return "redirect:/meetings?error=Spotkanie nie zostało znalezione";
////        }
////    }
////
////    @PostMapping("/meetings/{id}/edit")
////    public String updateMeeting(
////            @PathVariable Long id,
////            @Valid @ModelAttribute("updateMeetingRequest") UpdateMeetingRequest request,
////            BindingResult result,
////            @AuthenticationPrincipal CustomUserDetails userDetails,
////            Model model,
////            RedirectAttributes redirectAttributes) {
////
////        if (userDetails == null) {
////            return "redirect:/login";
////        }
////
////        if (result.hasErrors()) {
////            model.addAttribute("meetingId", id);
////            return "meetings/edit";
////        }
////
////        try {
////            MeetingResponse meeting = meetingService.updateMeeting(id, request, userDetails.getId());
////            redirectAttributes.addFlashAttribute("message",
////                    "Spotkanie '" + meeting.getTitle() + "' zostało zaktualizowane pomyślnie!");
////            return "redirect:/meetings/" + meeting.getId();
////
////        } catch (Exception e) {
////            model.addAttribute("error", "Błąd podczas aktualizacji spotkania: " + e.getMessage());
////            model.addAttribute("meetingId", id);
////            return "meetings/edit";
////        }
////    }
////
////    @PostMapping("/meetings/{id}/delete")
////    public String deleteMeeting(
////            @PathVariable Long id,
////            @AuthenticationPrincipal CustomUserDetails userDetails,
////            RedirectAttributes redirectAttributes) {
////
////        if (userDetails == null) {
////            return "redirect:/login";
////        }
////
////        try {
////            meetingService.deleteMeeting(id, userDetails.getId());
////            redirectAttributes.addFlashAttribute("message", "Spotkanie zostało usunięte pomyślnie!");
////            return "redirect:/meetings";
////
////        } catch (Exception e) {
////            redirectAttributes.addFlashAttribute("error", "Błąd podczas usuwania spotkania: " + e.getMessage());
////            return "redirect:/meetings/" + id;
////        }
////    }
////
////    @GetMapping("/meetings/{id}/duplicate")
////    public String duplicateMeeting(
////            @PathVariable Long id,
////            @AuthenticationPrincipal CustomUserDetails userDetails,
////            RedirectAttributes redirectAttributes) {
////
////        if (userDetails == null) {
////            return "redirect:/login";
////        }
////
////        try {
////            MeetingResponse duplicatedMeeting = meetingService.duplicateMeeting(id, userDetails.getId());
////            redirectAttributes.addFlashAttribute("message",
////                    "Spotkanie zostało skopiowane pomyślnie!");
////            return "redirect:/meetings/" + duplicatedMeeting.getId();
////
////        } catch (Exception e) {
////            redirectAttributes.addFlashAttribute("error", "Błąd podczas kopiowania spotkania: " + e.getMessage());
////            return "redirect:/meetings/" + id;
////        }
////    }
////
////    @PostMapping("/meetings/{id}/join")
////    public String joinMeeting(
////            @PathVariable Long id,
////            @AuthenticationPrincipal CustomUserDetails userDetails,
////            RedirectAttributes redirectAttributes) {
////
////        if (userDetails == null) {
////            return "redirect:/login";
////        }
////
////        try {
////            participantService.joinMeeting(userDetails.getId(), id);
////            redirectAttributes.addFlashAttribute("message", "Dołączyłeś do spotkania pomyślnie!");
////            return "redirect:/meetings/" + id;
////
////        } catch (Exception e) {
////            redirectAttributes.addFlashAttribute("error", "Błąd podczas dołączania do spotkania: " + e.getMessage());
////            return "redirect:/meetings/" + id;
////        }
////    }
////
////    @PostMapping("/meetings/{id}/leave")
////    public String leaveMeeting(
////            @PathVariable Long id,
////            @AuthenticationPrincipal CustomUserDetails userDetails,
////            RedirectAttributes redirectAttributes) {
////
////        if (userDetails == null) {
////            return "redirect:/login";
////        }
////
////        try {
////            participantService.leaveMeeting(userDetails.getId(), id);
////            redirectAttributes.addFlashAttribute("message", "Opuszczono spotkanie pomyślnie!");
////            return "redirect:/meetings/" + id;
////
////        } catch (Exception e) {
////            redirectAttributes.addFlashAttribute("error", "Błąd podczas opuszczania spotkania: " + e.getMessage());
////            return "redirect:/meetings/" + id;
////        }
////    }
////
////    @GetMapping("/my-meetings")
////    public String myMeetings(
////            @AuthenticationPrincipal CustomUserDetails userDetails,
////            @RequestParam(defaultValue = "0") int page,
////            @RequestParam(defaultValue = "10") int size,
////            Model model) {
////
////        if (userDetails == null) {
////            return "redirect:/login";
////        }
////
////        Pageable pageable = PageRequest.of(page, size);
////        Page<MeetingResponse> myMeetings = meetingService.getUserMeetings(userDetails.getId(), pageable);
////
////        model.addAttribute("meetings", myMeetings.getContent());
////        model.addAttribute("currentPage", page);
////        model.addAttribute("totalPages", myMeetings.getTotalPages());
////        model.addAttribute("totalItems", myMeetings.getTotalElements());
////
////        return "meetings/my-meetings";
////    }
////
//////    @GetMapping("/login")
//////    public String login(
//////            @RequestParam(value = "error", required = false) String error,
//////            @RequestParam(value = "logout", required = false) String logout,
//////            Model model) {
//////
//////        if (error != null) {
//////            model.addAttribute("error", "Nieprawidłowy email lub hasło");
//////        }
//////        if (logout != null) {
//////            model.addAttribute("message", "Zostałeś pomyślnie wylogowany");
//////        }
//////
//////        return "auth/login";
//////    }
////
////    @GetMapping("/login")
////    public String login(
////            @RequestParam(value = "error", required = false) String error,
////            @RequestParam(value = "logout", required = false) String logout,
////            Model model) {
////
////        if (error != null) {
////            model.addAttribute("error", "Nieprawidłowy email lub hasło");
////        }
////        if (logout != null) {
////            model.addAttribute("message", "Zostałeś pomyślnie wylogowany");
////        }
////
////        return "auth/login";
////    }
////
////    @GetMapping("/register")
////    public String showRegistrationForm(Model model) {
////        model.addAttribute("registrationRequest", new UserRegistrationRequest());
////        return "auth/register";
////    }
////
////    @PostMapping("/register")
////    public String registerUser(
////            @Valid @ModelAttribute("registrationRequest") UserRegistrationRequest request,
////            BindingResult result,
////            Model model,
////            RedirectAttributes redirectAttributes) {
////
////        if (result.hasErrors()) {
////            return "auth/register";
////        }
////
////        if (!request.getPassword().equals(request.getConfirmPassword())) {
////            model.addAttribute("error", "Hasła nie są identyczne");
////            return "auth/register";
////        }
////
////        try {
////            UserResponse user = authService.register(request);
////
////            redirectAttributes.addFlashAttribute("message",
////                    "Rejestracja zakończona sukcesem! Możesz się teraz zalogować.");
////            return "redirect:/login";
////
////        } catch (Exception e) {
////            model.addAttribute("error", "Błąd rejestracji: " + e.getMessage());
////            return "auth/register";
////        }
////    }
////
//////    @GetMapping("/profile")
//////    public String profile(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
//////        if (userDetails == null) {
//////            return "redirect:/login";
//////        }
//////
//////        model.addAttribute("userId", userDetails.getId());
//////        try {
//////            User user = userRepository.findById(userDetails.getId())
//////                    .orElseThrow(() -> new RuntimeException("User not found"));
//////            model.addAttribute("user", user);
//////        } catch (Exception e) {
//////            model.addAttribute("error", "Nie można załadować danych użytkownika");
//////        }
//////
//////        return "user/profile";
//////    }
////
////    @GetMapping("/dashboard")
////    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
////        if (userDetails == null) {
////            return "redirect:/login";
////        }
////
////        try {
////            DashboardStatsResponse stats = dashboardService.getUserDashboardStats(userDetails.getId());
////            model.addAttribute("stats", stats);
////
////            Page<MeetingResponse> recentMeetings = meetingService.getUserMeetings(userDetails.getId(), PageRequest.of(0, 5));
////            model.addAttribute("recentMeetings", recentMeetings.getContent());
////
////        } catch (Exception e) {
////            log.error("Error loading dashboard: {}", e.getMessage(), e);
////            DashboardStatsResponse fallbackStats = DashboardStatsResponse.builder()
////                    .totalMeetings(0L)
////                    .upcomingMeetings(0L)
////                    .participantsCount(0L)
////                    .organizedMeetings(0L)
////                    .build();
////            model.addAttribute("stats", fallbackStats);
////            model.addAttribute("recentMeetings", List.of());
////            model.addAttribute("warning", "Nie udało się załadować statystyk");
////        }
////
////        return "dashboard";
////    }
////
////    @GetMapping("/api/users/search")
////    @ResponseBody
////    public ResponseEntity<List<UserResponse>> searchUsers(
////            @RequestParam String query,
////            @AuthenticationPrincipal CustomUserDetails userDetails) {
////
////        if (userDetails == null) {
////            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
////        }
////
////        try {
////            List<UserResponse> users = userService.searchUsers(query);
////            return ResponseEntity.ok(users);
////        } catch (Exception e) {
////            log.error("Error searching users: {}", e.getMessage(), e);
////            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
////        }
////    }
////
////
//////    @GetMapping("/profile/notifications")
//////    public String profileNotifications(
//////            @AuthenticationPrincipal CustomUserDetails userDetails,
//////            @RequestParam(defaultValue = "0") int page,
//////            @RequestParam(defaultValue = "20") int size,
//////            Model model) {
//////
//////        if (userDetails == null) {
//////            return "redirect:/login";
//////        }
//////
//////        Pageable pageable = PageRequest.of(page, size);
//////        Page<NotificationResponse> notifications = notificationService.getUserNotifications(
//////                userDetails.getId(), pageable);
//////
//////        model.addAttribute("notifications", notifications.getContent());
//////        model.addAttribute("currentPage", page);
//////        model.addAttribute("totalPages", notifications.getTotalPages());
//////        model.addAttribute("totalItems", notifications.getTotalElements());
//////        model.addAttribute("unreadCount", notificationService.getUnreadCount(userDetails.getId()));
//////
//////        return "user/notifications";
//////    }
////
//////    @PostMapping("/profile/notifications/mark-all-read")
//////    public String markAllNotificationsRead(@AuthenticationPrincipal CustomUserDetails userDetails) {
//////        if (userDetails == null) {
//////            return "redirect:/login";
//////        }
//////
//////        notificationService.markAllAsRead(userDetails.getId());
//////        return "redirect:/profile/notifications";
//////    }
////
////
////
////
////    // =============================================
////// PARTICIPANT MANAGEMENT ENDPOINTS
////// =============================================
////
//////    @GetMapping("/meetings/{meetingId}/participants")
//////    public String getParticipants(@PathVariable Long meetingId,
//////                                  @AuthenticationPrincipal CustomUserDetails userDetails,
//////                                  Model model) {
//////        if (userDetails == null) {
//////            return "redirect:/login";
//////        }
//////
//////        try {
//////            MeetingResponse meeting = meetingService.getMeetingById(meetingId);
//////            var participants = participantService.getMeetingParticipants(meetingId);
//////
//////            // Statystyki
//////            long totalInvited = participants.stream()
//////                    .filter(p -> p.getStatus().name().equals("INVITED"))
//////                    .count();
//////            long totalConfirmed = participants.stream()
//////                    .filter(p -> p.getStatus().name().equals("CONFIRMED"))
//////                    .count();
//////            long waitlistCount = participants.stream()
//////                    .filter(p -> p.getStatus().name().equals("WAITING_LIST"))
//////                    .count();
//////
//////            model.addAttribute("meeting", meeting);
//////            model.addAttribute("participants", participants);
//////            model.addAttribute("meetingId", meetingId);
//////            model.addAttribute("isOrganizer", meeting.getOrganizer().getId().equals(userDetails.getId()));
//////
//////            // Statystyki - UŻYJ MAPY
//////            Map<String, Long> stats = new HashMap<>();
//////            stats.put("totalInvited", totalInvited);
//////            stats.put("totalConfirmed", totalConfirmed);
//////            stats.put("waitlistCount", waitlistCount);
//////
//////            model.addAttribute("stats", stats);
//////
//////            return "participants/list";
//////
//////        } catch (Exception e) {
//////            log.error("Error loading participants: {}", e.getMessage(), e);
//////            model.addAttribute("error", "Błąd podczas ładowania uczestników");
//////            return "redirect:/meetings/" + meetingId;
//////        }
//////    }
////
//////    @GetMapping("/meetings/{meetingId}/participants/invite")
//////    public String showInvitePage(@PathVariable Long meetingId,
//////                                 @AuthenticationPrincipal CustomUserDetails userDetails,
//////                                 Model model) {
//////        if (userDetails == null) {
//////            return "redirect:/login";
//////        }
//////
//////        try {
//////            MeetingResponse meeting = meetingService.getMeetingById(meetingId);
//////
//////            // Sprawdź czy użytkownik jest organizatorem
//////            if (!meeting.getOrganizer().getId().equals(userDetails.getId())) {
//////                return "redirect:/meetings/" + meetingId + "?error=Nie masz uprawnień do zapraszania uczestników";
//////            }
//////
//////            model.addAttribute("meeting", meeting);
//////            model.addAttribute("meetingId", meetingId);
//////            return "participants/invite";
//////
//////        } catch (Exception e) {
//////            log.error("Error loading invite page: {}", e.getMessage(), e);
//////            return "redirect:/meetings/" + meetingId + "?error=Spotkanie nie zostało znalezione";
//////        }
//////    }
//////
//////    @PostMapping("/meetings/{meetingId}/participants/invite")
//////    public String inviteParticipants(@PathVariable Long meetingId,
//////                                     @RequestParam List<Long> userIds,
//////                                     @RequestParam(defaultValue = "PARTICIPANT") String permissionLevel,
//////                                     @RequestParam(required = false) String message,
//////                                     @AuthenticationPrincipal CustomUserDetails userDetails,
//////                                     RedirectAttributes redirectAttributes) {
//////        if (userDetails == null) {
//////            return "redirect:/login";
//////        }
//////
//////        try {
//////            // Utwórz request dla każdego użytkownika
//////            for (Long userId : userIds) {
//////                participantService.inviteParticipant(meetingId, userId, userDetails.getId());
//////            }
//////
//////            redirectAttributes.addFlashAttribute("message",
//////                    "Wysłano " + userIds.size() + " zaproszeń");
//////            return "redirect:/meetings/" + meetingId + "/participants";
//////
//////        } catch (Exception e) {
//////            log.error("Error inviting participants: {}", e.getMessage(), e);
//////            redirectAttributes.addFlashAttribute("error",
//////                    "Błąd podczas zapraszania: " + e.getMessage());
//////            return "redirect:/meetings/" + meetingId + "/participants/invite";
//////        }
//////    }
//////
//////    @GetMapping("/meetings/{meetingId}/participants/{participantId}/update")
//////    public String showUpdateParticipantPage(@PathVariable Long meetingId,
//////                                            @PathVariable Long participantId,
//////                                            @AuthenticationPrincipal CustomUserDetails userDetails,
//////                                            Model model) {
//////        if (userDetails == null) {
//////            return "redirect:/login";
//////        }
//////
//////        try {
//////            MeetingResponse meeting = meetingService.getMeetingById(meetingId);
//////            var participants = participantService.getMeetingParticipants(meetingId);
//////            var participant = participants.stream()
//////                    .filter(p -> p.getId().equals(participantId))
//////                    .findFirst()
//////                    .orElseThrow(() -> new RuntimeException("Participant not found"));
//////
//////            // Sprawdź uprawnienia
//////            boolean isOrganizer = meeting.getOrganizer().getId().equals(userDetails.getId());
//////            boolean isSelf = participant.getUser().getId().equals(userDetails.getId());
//////
//////            if (!isOrganizer && !isSelf) {
//////                return "redirect:/meetings/" + meetingId + "/participants?error=Nie masz uprawnień do edycji tego uczestnika";
//////            }
//////
//////            model.addAttribute("meeting", meeting);
//////            model.addAttribute("participant", participant);
//////            model.addAttribute("meetingId", meetingId);
//////            model.addAttribute("participantId", participantId);
//////
//////            return "participants/update";
//////
//////        } catch (Exception e) {
//////            log.error("Error loading participant update page: {}", e.getMessage(), e);
//////            return "redirect:/meetings/" + meetingId + "/participants?error=Nie można załadować danych uczestnika";
//////        }
//////    }
//////
//////    @PostMapping("/meetings/{meetingId}/participants/{participantId}/update")
//////    public String updateParticipant(@PathVariable Long meetingId,
//////                                    @PathVariable Long participantId,
//////                                    @RequestParam String status,
//////                                    @RequestParam(required = false) String permissionLevel,
//////                                    @RequestParam(required = false) String comment,
//////                                    @AuthenticationPrincipal CustomUserDetails userDetails,
//////                                    RedirectAttributes redirectAttributes) {
//////        if (userDetails == null) {
//////            return "redirect:/login";
//////        }
//////
//////        try {
//////            participantService.updateParticipantStatus(meetingId, participantId,
//////                    com.meethub.domain.model.enums.ParticipationStatus.valueOf(status),
//////                    comment, userDetails.getId());
//////
//////            if (permissionLevel != null) {
//////                participantService.updateParticipantPermission(meetingId, participantId,
//////                        com.meethub.domain.model.enums.PermissionLevel.valueOf(permissionLevel),
//////                        userDetails.getId());
//////            }
//////
//////            redirectAttributes.addFlashAttribute("message", "Zaktualizowano uczestnika");
//////            return "redirect:/meetings/" + meetingId + "/participants";
//////
//////        } catch (Exception e) {
//////            log.error("Error updating participant: {}", e.getMessage(), e);
//////            redirectAttributes.addFlashAttribute("error", "Błąd podczas aktualizacji: " + e.getMessage());
//////            return "redirect:/meetings/" + meetingId + "/participants/" + participantId + "/update";
//////        }
//////    }
////
//////    @PostMapping("/meetings/{meetingId}/participants/{participantId}/remove")
//////    public String removeParticipant(@PathVariable Long meetingId,
//////                                    @PathVariable Long participantId,
//////                                    @AuthenticationPrincipal CustomUserDetails userDetails,
//////                                    RedirectAttributes redirectAttributes) {
//////        if (userDetails == null) {
//////            return "redirect:/login";
//////        }
//////
//////        try {
//////            participantService.removeParticipant(meetingId, participantId, userDetails.getId());
//////            redirectAttributes.addFlashAttribute("message", "Usunięto uczestnika");
//////            return "redirect:/meetings/" + meetingId + "/participants";
//////
//////        } catch (Exception e) {
//////            log.error("Error removing participant: {}", e.getMessage(), e);
//////            redirectAttributes.addFlashAttribute("error", "Błąd podczas usuwania: " + e.getMessage());
//////            return "redirect:/meetings/" + meetingId + "/participants";
//////        }
//////    }
////
//////    // Publiczne endpointy do potwierdzania przez token
//////    @GetMapping("/participants/confirm/{token}")
//////    public String confirmParticipation(@PathVariable String token,
//////                                       @RequestParam(required = false) String comment,
//////                                       Model model) {
//////        try {
//////            var participant = participantService.acceptInvitationByToken(token);
//////            model.addAttribute("success", "Potwierdzono udział w spotkaniu");
//////            model.addAttribute("participant", participant);
//////            model.addAttribute("meeting", participant.getMeeting());
//////            return "participants/confirmation-success";
//////        } catch (Exception e) {
//////            log.error("Error confirming participation: {}", e.getMessage(), e);
//////            model.addAttribute("error", "Błąd podczas potwierdzania: " + e.getMessage());
//////            return "participants/confirmation-error";
//////        }
//////    }
//////
//////    @GetMapping("/participants/decline/{token}")
//////    public String declineParticipation(@PathVariable String token,
//////                                       @RequestParam(required = false) String comment,
//////                                       Model model) {
//////        try {
//////            // Musisz dodać metodę declineInvitationByToken w serwisie
//////            // participantService.declineInvitationByToken(token, comment);
//////            model.addAttribute("success", "Odrzucono zaproszenie na spotkanie");
//////            return "participants/confirmation-success";
//////        } catch (Exception e) {
//////            log.error("Error declining participation: {}", e.getMessage(), e);
//////            model.addAttribute("error", "Błąd podczas odrzucania: " + e.getMessage());
//////            return "participants/confirmation-error";
//////        }
//////    }
////}
//
//
//
//
//
//
//
//
//
//
//
//package com.meethub.controller.web;
//
//import com.meethub.domain.model.request.UserRegistrationRequest;
//import com.meethub.domain.model.response.UserResponse;
//import com.meethub.domain.service.AuthService;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.validation.BindingResult;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.servlet.mvc.support.RedirectAttributes;
//
//import java.security.Principal;
//
//@Controller
//@RequiredArgsConstructor
//public class WebController {
//
//    private final AuthService authService;
//
//    @GetMapping("/")
//    public String home() {
//        return "index";
//    }
//
//    @GetMapping("/meetings")
//    public String meetings() {
//        return "meetings/list";
//    }
//
//    @GetMapping("/dashboard")
//    public String dashboard(Principal principal, Model model) {
//        if (principal == null) {
//            return "redirect:/login";
//        }
//        // Użytkownik jest zalogowany - Spring Security automatycznie sprawdza
//        model.addAttribute("username", principal.getName());
//        return "dashboard";
//    }
//
//    @GetMapping("/login")
//    public String login(
//            @RequestParam(value = "error", required = false) String error,
//            @RequestParam(value = "logout", required = false) String logout,
//            Model model) {
//
//        if (error != null) {
//            model.addAttribute("error", "Nieprawidłowy email lub hasło");
//        }
//        if (logout != null) {
//            model.addAttribute("message", "Zostałeś pomyślnie wylogowany");
//        }
//
//        return "auth/login";
//    }
//
//    @GetMapping("/register")
//    public String showRegistrationForm(Model model) {
//        model.addAttribute("registrationRequest", new UserRegistrationRequest());
//        return "auth/register";
//    }
//
//    @PostMapping("/register")
//    public String registerUser(
//            @Valid @ModelAttribute("registrationRequest") UserRegistrationRequest request,
//            BindingResult result,
//            Model model,
//            RedirectAttributes redirectAttributes) {
//
//        if (result.hasErrors()) {
//            return "auth/register";
//        }
//
//        if (!request.getPassword().equals(request.getConfirmPassword())) {
//            model.addAttribute("error", "Hasła nie są identyczne");
//            return "auth/register";
//        }
//
//        try {
//            UserResponse user = authService.register(request);
//            redirectAttributes.addFlashAttribute("message",
//                    "Rejestracja zakończona sukcesem! Możesz się teraz zalogować.");
//            return "redirect:/login";
//
//        } catch (Exception e) {
//            model.addAttribute("error", "Błąd rejestracji: " + e.getMessage());
//            return "auth/register";
//        }
//    }
//
//}






package com.meethub.controller.web;

import com.meethub.domain.model.entity.Feedback;
import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.MeetingParticipant;
import com.meethub.domain.model.entity.MeetingStatistics;
import com.meethub.domain.model.request.CreateMeetingRequest;
import com.meethub.domain.model.request.UserRegistrationRequest;
import com.meethub.domain.model.response.*;
import com.meethub.domain.service.*;
import com.meethub.security.CustomUserDetailsService.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final AuthService authService;
    private final MeetingService meetingService;
    private final MeetingAuthorizationService meetingAuthorizationService;
    private final UserService userService;
    private final MeetingParticipantService meetingParticipantService;
    private final MeetingVotingService meetingVotingService;

    private final FeedbackService feedbackService;
    private final MeetingResourceService resourceService;
    private final MeetingAnalyticsService meetingAnalyticsService; // DODAJ TEN SERWIS!


    @GetMapping("/")
    public String home(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails != null) {
            model.addAttribute("user", userDetails);
            return "dashboard";
        }

        // ✅ Dla niezalogowanych - pokaż publiczne spotkania na stronie głównej
        try {
            List<MeetingResponse> upcomingMeetings = meetingService.getUpcomingPublicMeetings();
            model.addAttribute("upcomingMeetings", upcomingMeetings);
            model.addAttribute("totalMeetings", upcomingMeetings.size());
        } catch (Exception e) {
            model.addAttribute("upcomingMeetings", Collections.emptyList());
            model.addAttribute("totalMeetings", 0);
        }

        return "index";
    }

//    @GetMapping("/meetings")
//    public String meetings(
//            @AuthenticationPrincipal CustomUserDetails userDetails,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "12") int size,
//            Model model) {
//
//        try {
//            Pageable pageable = PageRequest.of(page, size);
//            Page<MeetingResponse> meetingsPage;
//
//            if (userDetails != null) {
//                // ✅ Dla zalogowanych - użyj metody z filtrowaniem
//                meetingsPage = meetingService.getFilteredMeetings(null, null, null, pageable);
//            } else {
//                // ✅ Dla niezalogowanych - pobierz publiczne spotkania
//                List<MeetingResponse> publicMeetings = meetingService.getUpcomingPublicMeetings();
//
//                // Konwersja List na Page (uproszczone)
//                int start = (int) pageable.getOffset();
//                int end = Math.min((start + pageable.getPageSize()), publicMeetings.size());
//
//                if (start > publicMeetings.size()) {
//                    meetingsPage = Page.empty(pageable);
//                } else {
//                    meetingsPage = new org.springframework.data.domain.PageImpl<>(
//                            publicMeetings.subList(start, end),
//                            pageable,
//                            publicMeetings.size()
//                    );
//                }
//            }
//
//            model.addAttribute("meetings", meetingsPage.getContent());
//            model.addAttribute("currentPage", page);
//            model.addAttribute("totalPages", meetingsPage.getTotalPages());
//            model.addAttribute("totalItems", meetingsPage.getTotalElements());
//
//        } catch (Exception e) {
//            // W przypadku błędu, ustaw puste dane
//            model.addAttribute("meetings", Collections.emptyList());
//            model.addAttribute("currentPage", 0);
//            model.addAttribute("totalPages", 0);
//            model.addAttribute("totalItems", 0);
//            model.addAttribute("warning", "Nie udało się załadować listy spotkań");
//        }
//
//        if (userDetails != null) {
//            model.addAttribute("user", userDetails);
//        }
//
//        return "meetings/list";
//    }


    @GetMapping("/meetings")
    public String meetings(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            Model model) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<MeetingResponse> meetingsPage;

            if (userDetails != null) {
                // ✅ Przekaż parametry filtrowania do service
                meetingsPage = meetingService.getFilteredMeetings(search, type, status, pageable);
            } else {
                // Dla niezalogowanych - pobierz publiczne spotkania
                List<MeetingResponse> publicMeetings = meetingService.getUpcomingPublicMeetings();

                int start = (int) pageable.getOffset();
                int end = Math.min((start + pageable.getPageSize()), publicMeetings.size());

                if (start > publicMeetings.size()) {
                    meetingsPage = Page.empty(pageable);
                } else {
                    meetingsPage = new PageImpl<>(
                            publicMeetings.subList(start, end),
                            pageable,
                            publicMeetings.size()
                    );
                }
            }

            model.addAttribute("meetings", meetingsPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", meetingsPage.getTotalPages());
            model.addAttribute("totalItems", meetingsPage.getTotalElements());

            // ✅ DODAJ TE LINIJKI - przekaż parametry do modelu
            model.addAttribute("searchParam", search);
            model.addAttribute("typeParam", type);
            model.addAttribute("statusParam", status);

        } catch (Exception e) {
            model.addAttribute("meetings", Collections.emptyList());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalItems", 0);
            model.addAttribute("warning", "Nie udało się załadować listy spotkań");
        }

        if (userDetails != null) {
            model.addAttribute("user", userDetails);
            model.addAttribute("currentUserId", userDetails.getId()); // ✅ Dodaj to
        }

        return "meetings/list";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            // ✅ Pobierz spotkania użytkownika dla dashboard
            Page<MeetingResponse> userMeetings = meetingService.getUserMeetings(
                    userDetails.getId(),
                    PageRequest.of(0, 5)
            );
            model.addAttribute("recentMeetings", userMeetings.getContent());
        } catch (Exception e) {
            model.addAttribute("recentMeetings", Collections.emptyList());
        }

        model.addAttribute("user", userDetails);
        return "dashboard";
    }

    @GetMapping("/login")
    public String login(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model) {

        if (error != null) {
            model.addAttribute("error", "Nieprawidłowy email lub hasło");
        }
        if (logout != null) {
            model.addAttribute("message", "Zostałeś pomyślnie wylogowany");
        }

        return "auth/login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("registrationRequest", new UserRegistrationRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerUser(
            @Valid @ModelAttribute("registrationRequest") UserRegistrationRequest request,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "auth/register";
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            model.addAttribute("error", "Hasła nie są identyczne");
            return "auth/register";
        }

        try {
            UserResponse user = authService.register(request);
            redirectAttributes.addFlashAttribute("message",
                    "Rejestracja zakończona sukcesem! Możesz się teraz zalogować.");
            return "redirect:/login";

        } catch (Exception e) {
            model.addAttribute("error", "Błąd rejestracji: " + e.getMessage());
            return "auth/register";
        }
    }


//    @GetMapping("/meetings/{id}")
//    public String meetingDetails(
//            @PathVariable Long id,
//            @AuthenticationPrincipal CustomUserDetails userDetails,
//            Model model) {
//
//        try {
//            MeetingResponse meeting = meetingService.getMeetingById(id);
//            model.addAttribute("meeting", meeting);
//
//            if (userDetails != null) {
//                model.addAttribute("user", userDetails);
//                model.addAttribute("userId", userDetails.getId());
//
//                // ✅ DODAJ OBSŁUGĘ isParticipant
//                boolean isOrganizer = meeting.getOrganizer() != null &&
//                        meeting.getOrganizer().getId().equals(userDetails.getId());
//                model.addAttribute("isOrganizer", isOrganizer);
//
//                // ✅ USTAW isParticipant - tymczasowo na false
//                model.addAttribute("isParticipant", false);
//
//            } else {
//                // ✅ Dla niezalogowanych ustaw userId na null i isParticipant na false
//                model.addAttribute("userId", null);
//                model.addAttribute("isParticipant", false);
//            }
//
//            return "meetings/details";
//
//        } catch (Exception e) {
//
//            if (userDetails != null) {
//                model.addAttribute("user", userDetails);
//            }
//
//            model.addAttribute("error", "Spotkanie nie zostało znalezione");
//            return "redirect:/meetings";
//        }
//    }


//    @GetMapping("/meetings/{id}")
//    public String meetingDetails(
//            @PathVariable Long id,
//            @AuthenticationPrincipal CustomUserDetails userDetails,
//            Model model) {
//
//        try {
//            MeetingResponse meeting = meetingService.getMeetingById(id);
//            model.addAttribute("meeting", meeting);
//
//            Long userId = userDetails != null ? userDetails.getId() : null;
//
//            // ✅ DELEGACJA LOGIKI DO SERWISU
//            MeetingParticipationInfo participationInfo = meetingAuthorizationService.getUserMeetingPermissions(id, userId);
//
//            // ✅ PRZEKAŻ DANE DO TEMPLATE
//            model.addAttribute("isOrganizer", participationInfo.isOrganizer());
//            model.addAttribute("isParticipant", participationInfo.isParticipant());
//            model.addAttribute("isRelated", participationInfo.isRelated());
//            model.addAttribute("participantRole", participationInfo.getParticipantRole());
//            model.addAttribute("permissions", participationInfo.getPermissions());
//            model.addAttribute("canEdit", participationInfo.isCanEdit());
//            model.addAttribute("canDelete", participationInfo.isCanDelete());
//            model.addAttribute("canManageParticipants", participationInfo.isCanManageParticipants());
//            model.addAttribute("canJoin", participationInfo.isCanJoin());
//
//            if (userDetails != null) {
//                model.addAttribute("user", userDetails);
//                model.addAttribute("userId", userId);
//            } else {
//                model.addAttribute("userId", null);
//            }
//
//            return "meetings/details";
//
//        } catch (Exception e) {
//
//            if (userDetails != null) {
//                model.addAttribute("user", userDetails);
//            }
//
//            model.addAttribute("error", "Spotkanie nie zostało znalezione");
//            return "redirect:/meetings";
//        }
//    }

//    @GetMapping("/meetings/{id}")
//    public String meetingDetails(
//            @PathVariable Long id,
//            @AuthenticationPrincipal CustomUserDetails userDetails,
//            Model model) {
//
//        try {
//            MeetingResponse meeting = meetingService.getMeetingById(id);
//            model.addAttribute("meeting", meeting);
//
//            Long userId = userDetails != null ? userDetails.getId() : null;
//
//            // ✅ Pobierz POTWIERDZONYCH uczestników do wyświetlenia
//            List<ParticipantResponse> confirmedParticipants = meetingParticipantService.getConfirmedParticipants(id);
//
//            // ✅ Pobierz PEŁNE STATYSTYKI
//            Map<String, Long> participantStats = meetingParticipantService.getParticipantStatistics(id);
//
//            model.addAttribute("participants", confirmedParticipants);
//            model.addAttribute("participantStats", participantStats);
//
//            // ✅ Reszta logiki
//            MeetingParticipationInfo participationInfo = meetingAuthorizationService.getUserMeetingPermissions(id, userId);
//            model.addAttribute("isOrganizer", participationInfo.isOrganizer());
//            model.addAttribute("isParticipant", participationInfo.isParticipant());
//            model.addAttribute("isRelated", participationInfo.isRelated());
//            model.addAttribute("participantRole", participationInfo.getParticipantRole());
//            model.addAttribute("permissions", participationInfo.getPermissions());
//            model.addAttribute("canEdit", participationInfo.isCanEdit());
//            model.addAttribute("canDelete", participationInfo.isCanDelete());
//            model.addAttribute("canManageParticipants", participationInfo.isCanManageParticipants());
//            model.addAttribute("canJoin", participationInfo.isCanJoin());
//
//            if (userDetails != null) {
//                model.addAttribute("user", userDetails);
//                model.addAttribute("userId", userId);
//            }
//
//            return "meetings/details";
//
//        } catch (Exception e) {
//
//            if (userDetails != null) {
//                model.addAttribute("user", userDetails);
//            }
//            model.addAttribute("error", "Błąd podczas ładowania szczegółów spotkania");
//            return "redirect:/meetings";
//        }
//    }



//    @GetMapping("/meetings/{id}")
//    public String meetingDetails(
//            @PathVariable Long id,
//            @AuthenticationPrincipal CustomUserDetails userDetails,
//            Model model) {
//
//        try {
//            MeetingResponse meeting = meetingService.getMeetingById(id);
//            model.addAttribute("meeting", meeting);
//
//            Long userId = userDetails != null ? userDetails.getId() : null;
//
//            // ✅ Pobierz POTWIERDZONYCH uczestników do wyświetlenia
//            List<ParticipantResponse> confirmedParticipants = meetingParticipantService.getConfirmedParticipants(id);
//
//            // ✅ Pobierz PEŁNE STATYSTYKI
//            Map<String, Long> participantStats = meetingParticipantService.getParticipantStatistics(id);
//
//            model.addAttribute("participants", confirmedParticipants);
//            model.addAttribute("participantStats", participantStats);
//
//            // ✅ DODAJ: Pobierz głosowania
//            if (userId != null) {
//                List<VotingResponse> allVotings = meetingVotingService.getMeetingVotings(id, userId);
//
//                // Podziel na aktywne i zamknięte
//                List<VotingResponse> activeVotings = allVotings.stream()
//                        .filter(voting -> voting.getStatus().name().equals("ACTIVE"))
//                        .collect(Collectors.toList());
//
//                List<VotingResponse> closedVotings = allVotings.stream()
//                        .filter(voting -> voting.getStatus().name().equals("CLOSED"))
//                        .collect(Collectors.toList());
//
//                model.addAttribute("activeVotings", activeVotings);
//                model.addAttribute("closedVotings", closedVotings);
//            } else {
//                // Dla niezalogowanych - puste listy
//                model.addAttribute("activeVotings", Collections.emptyList());
//                model.addAttribute("closedVotings", Collections.emptyList());
//            }
//
//            // ✅ Reszta logiki
//            MeetingParticipationInfo participationInfo = meetingAuthorizationService.getUserMeetingPermissions(id, userId);
//            model.addAttribute("isOrganizer", participationInfo.isOrganizer());
//            model.addAttribute("isParticipant", participationInfo.isParticipant());
//            model.addAttribute("isRelated", participationInfo.isRelated());
//            model.addAttribute("participantRole", participationInfo.getParticipantRole());
//            model.addAttribute("permissions", participationInfo.getPermissions());
//            model.addAttribute("canEdit", participationInfo.isCanEdit());
//            model.addAttribute("canDelete", participationInfo.isCanDelete());
//            model.addAttribute("canManageParticipants", participationInfo.isCanManageParticipants());
//            model.addAttribute("canJoin", participationInfo.isCanJoin());
//
//            if (userDetails != null) {
//                model.addAttribute("user", userDetails);
//                model.addAttribute("userId", userId);
//            }
//
//            return "meetings/details";
//
//        } catch (Exception e) {
//            if (userDetails != null) {
//                model.addAttribute("user", userDetails);
//            }
//            model.addAttribute("error", "Błąd podczas ładowania szczegółów spotkania");
//            return "redirect:/meetings";
//        }
//    }
//



    @GetMapping("/meetings/{id}")
    public String meetingDetails(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        try {
            MeetingResponse meeting = meetingService.getMeetingById(id);
            model.addAttribute("meeting", meeting);

            Long userId = userDetails != null ? userDetails.getId() : null;

            // ✅ Pobierz POTWIERDZONYCH uczestników do wyświetlenia
            List<ParticipantResponse> confirmedParticipants = meetingParticipantService.getConfirmedParticipants(id);

            // ✅ Pobierz PEŁNE STATYSTYKI
            Map<String, Long> participantStats = meetingParticipantService.getParticipantStatistics(id);

            model.addAttribute("participants", confirmedParticipants);
            model.addAttribute("participantStats", participantStats);

            // ✅ DODAJ: Pobierz statystyki spotkania (ANALYTICS)
            if (userId != null) {
                // Sprawdź czy użytkownik jest organizatorem/adminem
                MeetingParticipationInfo participationInfo = meetingAuthorizationService.getUserMeetingPermissions(id, userId);
                boolean isOrganizer = participationInfo.isOrganizer();
                boolean isAdmin = userDetails.getAuthorities().stream()
                        .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

                // Tylko organizator/admin widzi statystyki
                if (isOrganizer || isAdmin) {
                    try {
                        // Pobierz istniejące statystyki
                        Optional<MeetingStatistics> statsOpt = meetingAnalyticsService.getMeetingStatistics(id);

                        if (statsOpt.isPresent()) {
                            // Jeśli istnieją, dodaj do modelu
                            model.addAttribute("meetingStatistics", statsOpt.get());
                        } else {
                            // Jeśli nie istnieją, ustaw null (HTML pokaże "Ładowanie...")
                            model.addAttribute("meetingStatistics", null);
                        }

                        // Dodaj flagi do modelu
                        model.addAttribute("isOrganizer", true);
                        model.addAttribute("isAdmin", isAdmin);

                    } catch (Exception e) {
                        model.addAttribute("meetingStatistics", null);
                    }
                } else {
                    // Nie-organizator nie widzi statystyk
                    model.addAttribute("meetingStatistics", null);
                    model.addAttribute("isOrganizer", false);
                    model.addAttribute("isAdmin", false);
                }
            } else {
                // Niezalogowany użytkownik
                model.addAttribute("meetingStatistics", null);
                model.addAttribute("isOrganizer", false);
                model.addAttribute("isAdmin", false);
            }

            // ✅ DODAJ: Pobierz głosowania
            if (userId != null) {
                try {
                    List<VotingResponse> allVotings = meetingVotingService.getMeetingVotings(id, userId);

                    // Podziel na aktywne i zamknięte
                    List<VotingResponse> activeVotings = allVotings.stream()
                            .filter(voting -> voting.getStatus().name().equals("ACTIVE"))
                            .collect(Collectors.toList());

                    List<VotingResponse> closedVotings = allVotings.stream()
                            .filter(voting -> voting.getStatus().name().equals("CLOSED"))
                            .collect(Collectors.toList());

                    model.addAttribute("activeVotings", activeVotings);
                    model.addAttribute("closedVotings", closedVotings);
                } catch (Exception e) {
                    // Obsłuż błąd pobierania głosowań
                    model.addAttribute("activeVotings", Collections.emptyList());
                    model.addAttribute("closedVotings", Collections.emptyList());
                }
            } else {
                // Dla niezalogowanych - puste listy
                model.addAttribute("activeVotings", Collections.emptyList());
                model.addAttribute("closedVotings", Collections.emptyList());
            }

            // ✅ Pobierz feedback użytkownika jeśli jest uczestnikiem - POPRAWIONE!
            if (userId != null) {
                try {
                    MeetingParticipationInfo participationInfo = meetingAuthorizationService.getUserMeetingPermissions(id, userId);

                    if (participationInfo.isParticipant()) {
                        // Dodaj osobny try-catch dla pobierania feedbacku
                        try {
                            Feedback userFeedback = feedbackService.getUserFeedback(id, userId);
                            if (userFeedback != null) {
                                model.addAttribute("userFeedback", userFeedback);
                            }
                        } catch (Exception e) {
                            // Jeśli nie ma feedbacku lub jest błąd, ustaw null
                            model.addAttribute("userFeedback", null);
                        }
                    }
                } catch (Exception e) {
                    // Błąd w pobieraniu permissions - kontynuuj bez feedbacku
                    model.addAttribute("userFeedback", null);
                }
            }

            // ✅ Pobierz zasoby spotkania
            if (userId != null) {
                try {
                    List<MeetingResourceResponse> resources = resourceService.getMeetingResources(id, userId);
                    model.addAttribute("resources", resources);
                    model.addAttribute("resourcesCount", resources.size());
                } catch (Exception e) {
                    model.addAttribute("resources", Collections.emptyList());
                    model.addAttribute("resourcesCount", 0);
                }
            } else {
                model.addAttribute("resources", Collections.emptyList());
                model.addAttribute("resourcesCount", 0);
            }

            // ✅ Reszta logiki (bez duplikacji isOrganizer)
            if (userId != null) {
                try {
                    MeetingParticipationInfo participationInfo = meetingAuthorizationService.getUserMeetingPermissions(id, userId);

                    // Nie nadpisuj isOrganizer jeśli już ustawione przez sekcję statystyk
                    if (!model.containsAttribute("isOrganizer")) {
                        model.addAttribute("isOrganizer", participationInfo.isOrganizer());
                    }

                    model.addAttribute("isParticipant", participationInfo.isParticipant());
                    model.addAttribute("isRelated", participationInfo.isRelated());
                    model.addAttribute("participantRole", participationInfo.getParticipantRole());
                    model.addAttribute("permissions", participationInfo.getPermissions());
                    model.addAttribute("canEdit", participationInfo.isCanEdit());
                    model.addAttribute("canDelete", participationInfo.isCanDelete());
                    model.addAttribute("canManageParticipants", participationInfo.isCanManageParticipants());
                    model.addAttribute("canJoin", participationInfo.isCanJoin());

                    model.addAttribute("user", userDetails);
                    model.addAttribute("userId", userId);
                } catch (Exception e) {
                    // Jeśli błąd w pobieraniu permissions, ustaw domyślne wartości
                    model.addAttribute("isOrganizer", false);
                    model.addAttribute("isParticipant", false);
                    model.addAttribute("isRelated", false);
                    model.addAttribute("canJoin", meeting.getVisibility().name().equals("PUBLIC"));
                }
            } else {
                // Dla niezalogowanych ustaw podstawowe flagi
                model.addAttribute("isOrganizer", false);
                model.addAttribute("isParticipant", false);
                model.addAttribute("isRelated", false);
                model.addAttribute("canJoin", meeting.getVisibility().name().equals("PUBLIC"));
            }

            return "meetings/details";

        } catch (Exception e) {
            // Loguj szczegółowy błąd
            System.err.println("ERROR in meetingDetails: " + e.getMessage());
            e.printStackTrace();

            if (userDetails != null) {
                model.addAttribute("user", userDetails);
            }
            model.addAttribute("error", "Błąd podczas ładowania szczegółów spotkania: " + e.getMessage());
            return "redirect:/meetings";
        }
    }



    @GetMapping("/meetings/create")
    public String showCreateMeetingForm(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", userDetails);
        // Dodaj pusty obiekt dla formularza
        model.addAttribute("createMeetingRequest", new com.meethub.domain.model.request.CreateMeetingRequest());
        return "meetings/create"; // nazwa twojego template
    }


    @PostMapping("/meetings/create")
    public String createMeeting(
            @Valid @ModelAttribute("createMeetingRequest") CreateMeetingRequest request,
            BindingResult result,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            model.addAttribute("user", userDetails);
            return "meetings/create";
        }

        try {
            MeetingResponse meeting = meetingService.createMeeting(request, userDetails.getId());
            redirectAttributes.addFlashAttribute("message",
                    "Spotkanie '" + meeting.getTitle() + "' zostało utworzone pomyślnie!");
            return "redirect:/meetings/" + meeting.getId();

        } catch (Exception e) {
            model.addAttribute("user", userDetails);
            model.addAttribute("error", "Błąd podczas tworzenia spotkania: " + e.getMessage());
            return "meetings/create";
        }
    }

    @GetMapping("/api/users/search")
    @ResponseBody
    public ResponseEntity<List<UserResponse>> searchUsers(
            @RequestParam String query,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<UserResponse> users = userService.searchUsers(query);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


}
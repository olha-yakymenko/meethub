//
//
//
//package com.meethub.controller.web;
//
//import com.meethub.domain.model.entity.*;
//import com.meethub.domain.model.enums.MeetingStatus;
//import com.meethub.domain.model.enums.UserRole;
//import com.meethub.domain.model.request.CreateMeetingRequest;
//import com.meethub.domain.model.request.UpdateMeetingRequest;
//import com.meethub.domain.model.request.UserRegistrationRequest;
//import com.meethub.domain.model.response.*;
//import com.meethub.domain.repository.jpa.CategoryRepository;
//import com.meethub.domain.service.*;
//import com.meethub.security.CustomUserDetailsService.CustomUserDetails;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.core.io.Resource;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.validation.BindingResult;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.servlet.mvc.support.RedirectAttributes;
//
//import lombok.extern.slf4j.Slf4j;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Slf4j
//@Controller
//@RequiredArgsConstructor
//public class WebController {
//
//    private final AuthService authService;
//    private final MeetingService meetingService;
//    private final MeetingAuthorizationService meetingAuthorizationService;
//    private final UserService userService;
//    private final MeetingParticipantService meetingParticipantService;
//    private final MeetingVotingService meetingVotingService;
//
//
//    private final FeedbackService feedbackService;
//    private final MeetingResourceService resourceService;
//    private final MeetingAnalyticsService meetingAnalyticsService; // DODAJ TEN SERWIS!
//
//    private final CategoryRepository categoryRepository;
//
//    @GetMapping("/")
//    public String home(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
//        if (userDetails != null) {
//            model.addAttribute("user", userDetails);
//            return "dashboard";
//        }
//
//        // ✅ Dla niezalogowanych - pokaż publiczne spotkania na stronie głównej
//        try {
//            List<MeetingResponse> upcomingMeetings = meetingService.getUpcomingPublicMeetings();
//            model.addAttribute("upcomingMeetings", upcomingMeetings);
//            model.addAttribute("totalMeetings", upcomingMeetings.size());
//        } catch (Exception e) {
//            model.addAttribute("upcomingMeetings", Collections.emptyList());
//            model.addAttribute("totalMeetings", 0);
//        }
//
//        return "index";
//    }
//
//
//    @GetMapping("/meetings")
//    public String meetings(
//            @AuthenticationPrincipal CustomUserDetails userDetails,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "12") int size,
//            @RequestParam(required = false) String search,
//            @RequestParam(required = false) String type,
//            @RequestParam(required = false) String status,
//            Model model) {
//
//        log.info("🔍 meetings() called - page: {}, size: {}, search: {}, type: {}, status: {}",
//                page, size, search, type, status);
//
//        try {
//            Pageable pageable = PageRequest.of(page, size);
//            Page<MeetingResponse> meetingsPage;
//
//            if (userDetails != null) {
//                // ✅ Przekaż parametry filtrowania do service
//                meetingsPage = meetingService.getFilteredMeetings(search, type, status, pageable);
//
//                // ✅ DODAJ KOMPLETNE DANE UCZESTNICTWA
//                Long userId = userDetails.getId();
//
//                for (MeetingResponse meeting : meetingsPage.getContent()) {
//                    try {
//                        // ✅ 1. Sprawdź czy użytkownik jest organizatorem
//                        boolean isOrganizer = meeting.getOrganizer() != null &&
//                                meeting.getOrganizer().getId().equals(userId);
//                        meeting.setUserIsOrganizer(isOrganizer);
//
//                        // ✅ 2. Sprawdź wszystkie statusy uczestnictwa
//                        boolean isConfirmed = meetingParticipantService.isConfirmedParticipant(meeting.getId(), userId);
//                        boolean isPending = meetingParticipantService.isPendingParticipant(meeting.getId(), userId);
//                        boolean isInvited = meetingParticipantService.isInvitedParticipant(meeting.getId(), userId);
//                        boolean isDeclined = meetingParticipantService.isDeclinedParticipant(meeting.getId(), userId);
//                        boolean isWaiting = meetingParticipantService.isWaitingListParticipant(meeting.getId(), userId);
//
//                        // ✅ 3. Sprawdź ogólnie czy jest uczestnikiem (jakikolwiek status)
//                        boolean isAnyParticipant = meetingParticipantService.isUserParticipant(meeting.getId(), userId);
//
//                        // ✅ 4. Sprawdź czy jest viewerem
//                        boolean isViewer = meetingParticipantService.isViewer(meeting.getId(), userId);
//
//                        // ✅ 5. Sprawdź czy jest bez związku
//                        boolean isUnrelated = meetingParticipantService.isUnrelatedUser(meeting.getId(), userId);
//
//                        // ✅ 6. Określ główną rolę użytkownika
//                        String userRole = determineUserRole(
//                                isOrganizer, isConfirmed, isPending, isInvited,
//                                isDeclined, isWaiting, isViewer, isUnrelated
//                        );
//
//                        // ✅ 7. Ustaw wszystkie pola statusu
//                        meeting.setUserIsOrganizer(isOrganizer);
//                        meeting.setUserIsConfirmed(isConfirmed);
//                        meeting.setUserIsPending(isPending);
//                        meeting.setUserIsInvited(isInvited);
//                        meeting.setUserIsDeclined(isDeclined);
//                        meeting.setUserIsWaiting(isWaiting);
//                        meeting.setUserIsParticipant(isAnyParticipant);
//                        meeting.setUserIsViewer(isViewer);
//                        meeting.setUserIsUnrelated(isUnrelated);
//                        meeting.setUserRole(userRole);
//
//                        // ✅ 8. Ustaw pola UI (canJoin, canLeave, etc.)
//                        meeting.setCanJoin(isViewer && !isOrganizer && !isAnyParticipant);
//                        meeting.setCanLeave(isConfirmed && !isOrganizer);
//                        meeting.setCanEdit(isOrganizer);
//                        meeting.setCanDelete(isOrganizer);
//
//                        // ✅ 9. Jeśli jest uczestnikiem, pobierz status uczestnictwa
//                        if (isAnyParticipant) {
//                            ParticipantResponse participant = meetingParticipantService.getParticipantInfo(userId, meeting.getId());
//                            if (participant != null) {
//                                meeting.setUserParticipationStatus(participant.getStatus());
//                            }
//                        }
//
//                    } catch (Exception e) {
//                        // W razie błędu ustaw domyślne wartości
//                        log.warn("Error checking participation for meeting {}: {}", meeting.getId(), e.getMessage());
//
//                        // Bezpieczne domyślne wartości
//                        meeting.setUserIsOrganizer(false);
//                        meeting.setUserIsConfirmed(false);
//                        meeting.setUserIsPending(false);
//                        meeting.setUserIsInvited(false);
//                        meeting.setUserIsDeclined(false);
//                        meeting.setUserIsWaiting(false);
//                        meeting.setUserIsParticipant(false);
//                        meeting.setUserIsViewer(true);
//                        meeting.setUserIsUnrelated(false);
//                        meeting.setUserRole("VIEWER");
//                        meeting.setCanJoin(false);
//                        meeting.setCanLeave(false);
//                        meeting.setCanEdit(false);
//                        meeting.setCanDelete(false);
//                    }
//                }
//
//            } else {
//                // Dla niezalogowanych - pobierz publiczne spotkania
//                List<MeetingResponse> publicMeetings = meetingService.getUpcomingPublicMeetings();
//
//                int start = (int) pageable.getOffset();
//                int end = Math.min((start + pageable.getPageSize()), publicMeetings.size());
//
//                if (start > publicMeetings.size()) {
//                    meetingsPage = Page.empty(pageable);
//                } else {
//                    List<MeetingResponse> pageContent = publicMeetings.subList(start, end);
//
//                    // Dla niezalogowanych ustaw wszystkie role na VIEWER
//                    for (MeetingResponse meeting : pageContent) {
//                        meeting.setUserRole("VIEWER");
//                        meeting.setUserIsViewer(true);
//                        meeting.setUserIsOrganizer(false);
//                        meeting.setUserIsConfirmed(false);
//                        meeting.setUserIsPending(false);
//                        meeting.setUserIsInvited(false);
//                        meeting.setUserIsDeclined(false);
//                        meeting.setUserIsWaiting(false);
//                        meeting.setUserIsParticipant(false);
//                        meeting.setUserIsUnrelated(false);
//                        meeting.setCanJoin(false); // Niezalogowany nie może dołączyć
//                        meeting.setCanLeave(false);
//                        meeting.setCanEdit(false);
//                        meeting.setCanDelete(false);
//                    }
//
//                    meetingsPage = new PageImpl<>(
//                            pageContent,
//                            pageable,
//                            publicMeetings.size()
//                    );
//                }
//            }
//
//            // ✅ Przekaż dane do modelu
//            model.addAttribute("meetings", meetingsPage.getContent());
//            model.addAttribute("currentPage", page);
//            model.addAttribute("totalPages", meetingsPage.getTotalPages());
//            model.addAttribute("totalItems", meetingsPage.getTotalElements());
//
//            // ✅ Przekaż parametry filtrowania do modelu
//            model.addAttribute("searchParam", search);
//            model.addAttribute("typeParam", type);
//            model.addAttribute("statusParam", status);
//
//            // ✅ Przekaż userId i userDetails dla template
//            if (userDetails != null) {
//                model.addAttribute("userId", userDetails.getId());
//                model.addAttribute("currentUserId", userDetails.getId());
//                model.addAttribute("user", userDetails);
//            } else {
//                model.addAttribute("userId", null);
//                model.addAttribute("currentUserId", null);
//            }
//
//        } catch (Exception e) {
//            log.error("Error loading meetings: {}", e.getMessage(), e);
//            model.addAttribute("meetings", Collections.emptyList());
//            model.addAttribute("currentPage", 0);
//            model.addAttribute("totalPages", 0);
//            model.addAttribute("totalItems", 0);
//            model.addAttribute("warning", "Nie udało się załadować listy spotkań");
//        }
//
//        return "meetings/list";
//    }
//
//    // ✅ Metoda pomocnicza do określania roli użytkownika
//    private String determineUserRole(boolean isOrganizer, boolean isConfirmed,
//                                     boolean isPending, boolean isInvited,
//                                     boolean isDeclined, boolean isWaiting,
//                                     boolean isViewer, boolean isUnrelated) {
//        if (isOrganizer) return "ORGANIZER";
//        if (isConfirmed) return "CONFIRMED_PARTICIPANT";
//        if (isPending) return "PENDING";
//        if (isInvited) return "INVITED";
//        if (isDeclined) return "DECLINED";
//        if (isWaiting) return "WAITING_LIST";
//        if (isViewer) return "VIEWER";
//        if (isUnrelated) return "UNRELATED";
//        return "VIEWER";
//    }
//
//
//
//
//    @GetMapping("/dashboard")
//    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
//        if (userDetails == null) {
//            return "redirect:/login";
//        }
//
//        try {
//            // ✅ Pobierz spotkania użytkownika dla dashboard
//            Page<MeetingResponse> userMeetings = meetingService.getUserMeetings(
//                    userDetails.getId(),
//                    PageRequest.of(0, 5)
//            );
//            model.addAttribute("recentMeetings", userMeetings.getContent());
//        } catch (Exception e) {
//            model.addAttribute("recentMeetings", Collections.emptyList());
//        }
//
//        model.addAttribute("user", userDetails);
//        return "dashboard";
//    }
//
//
//    @GetMapping("/meetings/{id}")
//    public String meetingDetails(
//            @PathVariable Long id,
//            @AuthenticationPrincipal CustomUserDetails userDetails,
//            Model model) {
//
//        log.info("=== START meetingDetails ===");
//        log.info("Meeting ID: {}", id);
//        log.info("User Details: {}", userDetails != null ? "Present" : "Null");
//
//        if (userDetails != null) {
//            log.info("User ID: {}", userDetails.getId());
//            log.info("User Authorities: {}", userDetails.getAuthorities());
//        }
//
//        try {
//            MeetingResponse meeting = meetingService.getMeetingById(id);
//            log.info("Meeting loaded: {}", meeting != null ? "Yes" : "No");
//            log.info("Meeting Title: {}", meeting != null ? meeting.getTitle() : "null");
//            log.info("Meeting Organizer ID: {}", meeting != null && meeting.getOrganizer() != null ?
//                    meeting.getOrganizer().getId() : "null");
//
//            model.addAttribute("meeting", meeting);
//            log.info("Attribute 'meeting' added to model: {}", meeting != null);
//
//            Long userId = userDetails != null ? userDetails.getId() : null;
//            log.info("User ID for model: {}", userId);
//
//            // ✅ DODAJ: Sprawdź czy użytkownik jest adminem
//            boolean isAdmin = false;
//            if (userDetails != null) {
//                isAdmin = userDetails.getAuthorities().stream()
//                        .anyMatch(auth -> {
//                            log.debug("Authority: {}", auth.getAuthority());
//                            return auth.getAuthority().equals("ROLE_ADMIN");
//                        });
//            }
//            log.info("Is Admin: {}", isAdmin);
//            model.addAttribute("isAdmin", isAdmin);
//
//            // ✅ POBIERZ JEDNORAZOWO PERMISSIONS
//            MeetingParticipationInfo participationInfo = null;
//            if (userId != null) {
//                try {
//                    participationInfo = meetingAuthorizationService.getUserMeetingPermissions(id, userId);
//                    log.info("Participation Info loaded: {}", participationInfo != null);
//                    if (participationInfo != null) {
//                        log.info("Is Organizer: {}", participationInfo.isOrganizer());
//                    }
//                } catch (Exception e) {
//                    log.error("Error loading participation info: {}", e.getMessage());
//                }
//            }
//
//            // ✅ PODSTAWOWE ATTRYBUTY
//            try {
//                List<ParticipantResponse> participants = meetingParticipantService.getConfirmedParticipants(id);
//                log.info("Confirmed participants count: {}", participants.size());
//                model.addAttribute("participants", participants);
//            } catch (Exception e) {
//                log.error("Error loading participants: {}", e.getMessage());
//                model.addAttribute("participants", Collections.emptyList());
//            }
//
//            try {
//                Map<String, Long> participantStats = meetingParticipantService.getParticipantStatistics(id);
//                log.info("Participant stats: {}", participantStats);
//                model.addAttribute("participantStats", participantStats);
//            } catch (Exception e) {
//                log.error("Error loading participant stats: {}", e.getMessage());
//                model.addAttribute("participantStats", new HashMap<>());
//            }
//
//            // ✅ DODAJ STATUSA UCZESTNICTWA
//            if (userId != null) {
//                try {
//                    boolean isPending = meetingParticipantService.isPendingParticipant(id, userId);
//                    boolean isDeclined = meetingParticipantService.isDeclinedParticipant(id, userId);
//                    boolean isInvited = meetingParticipantService.isInvitedParticipant(id, userId);
//                    boolean isConfirmed = meetingParticipantService.isConfirmedParticipant(id, userId);
//
//                    log.info("User status - Pending: {}, Declined: {}, Invited: {}, Confirmed: {}",
//                            isPending, isDeclined, isInvited, isConfirmed);
//
//                    model.addAttribute("isPending", isPending);
//                    model.addAttribute("isDeclined", isDeclined);
//                    model.addAttribute("isInvited", isInvited);
//                    model.addAttribute("isConfirmed", isConfirmed);
//                } catch (Exception e) {
//                    log.error("Error checking user status: {}", e.getMessage());
//                }
//            }
//
//            // ✅ PERMISSIONS UŻYTKOWNIKA
//            if (participationInfo != null) {
//                boolean isOrganizer = participationInfo.isOrganizer();
//                log.info("Setting isOrganizer to: {}", isOrganizer);
//
//                model.addAttribute("isOrganizer", isOrganizer);
//                model.addAttribute("isParticipant", participationInfo.isParticipant());
//                model.addAttribute("isRelated", participationInfo.isRelated());
//                model.addAttribute("participantRole", participationInfo.getParticipantRole());
//                model.addAttribute("canEdit", participationInfo.isCanEdit());
//                model.addAttribute("canDelete", participationInfo.isCanDelete());
//                model.addAttribute("canManageParticipants", participationInfo.isCanManageParticipants());
//                model.addAttribute("canJoin", participationInfo.isCanJoin());
//
//                // Log all permissions
//                log.info("Permissions - Organizer: {}, Participant: {}, CanEdit: {}, CanDelete: {}",
//                        isOrganizer, participationInfo.isParticipant(),
//                        participationInfo.isCanEdit(), participationInfo.isCanDelete());
//            } else {
//                // Niezalogowany użytkownik
//                boolean canJoin = meeting != null &&
//                        meeting.getVisibility() != null &&
//                        meeting.getVisibility().name().equals("PUBLIC");
//
//                log.info("Setting default values - isOrganizer: false, canJoin: {}", canJoin);
//
//                model.addAttribute("isOrganizer", false);
//                model.addAttribute("isParticipant", false);
//                model.addAttribute("isRelated", false);
//                model.addAttribute("canJoin", canJoin);
//            }
//
//            // ✅ STATYSTYKI (tylko dla organizatora/admina)
//            if (userId != null && participationInfo != null &&
//                    (participationInfo.isOrganizer() || isAdmin)) {
//
//                log.info("User is organizer or admin, loading statistics...");
//
//                try {
//                    Optional<MeetingStatistics> statsOpt = meetingAnalyticsService.getMeetingStatistics(id);
//                    boolean hasStats = statsOpt.isPresent();
//                    log.info("Statistics loaded: {}", hasStats);
//
//                    model.addAttribute("meetingStatistics", statsOpt.orElse(null));
//                } catch (Exception e) {
//                    log.error("Error loading statistics: {}", e.getMessage());
//                    model.addAttribute("meetingStatistics", null);
//                }
//            } else {
//                log.info("User is NOT organizer or admin, statistics will be null");
//                model.addAttribute("meetingStatistics", null);
//            }
//
//            // ✅ GŁOSOWANIA
//            if (userId != null) {
//                try {
//                    List<VotingResponse> allVotings = meetingVotingService.getMeetingVotings(id, userId);
//                    Map<Boolean, List<VotingResponse>> votings = allVotings.stream()
//                            .collect(Collectors.partitioningBy(v -> v.getStatus().name().equals("ACTIVE")));
//
//                    log.info("Votings loaded - Active: {}, Closed: {}",
//                            votings.get(true).size(), votings.get(false).size());
//
//                    model.addAttribute("activeVotings", votings.get(true));
//                    model.addAttribute("closedVotings", votings.get(false));
//                } catch (Exception e) {
//                    log.error("Error loading votings: {}", e.getMessage());
//                    model.addAttribute("activeVotings", Collections.emptyList());
//                    model.addAttribute("closedVotings", Collections.emptyList());
//                }
//            }
//
//            // ✅ FEEDBACK
//            if (userId != null && participationInfo != null && participationInfo.isParticipant()) {
//                try {
//                    Feedback userFeedback = feedbackService.getUserFeedback(id, userId);
//                    log.info("User feedback loaded: {}", userFeedback != null);
//                    model.addAttribute("userFeedback", userFeedback);
//                } catch (Exception e) {
//                    log.info("No feedback found for user: {}", e.getMessage());
//                    model.addAttribute("userFeedback", null);
//                }
//            }
//
//            // ✅ ZASOBY
//            if (userId != null) {
//                try {
//                    List<MeetingResourceResponse> resources = resourceService.getMeetingResources(id, userId);
//                    log.info("Resources loaded: {}", resources.size());
//                    model.addAttribute("resources", resources);
//                    model.addAttribute("resourcesCount", resources.size());
//                } catch (Exception e) {
//                    log.error("Error loading resources: {}", e.getMessage());
//                    model.addAttribute("resources", Collections.emptyList());
//                    model.addAttribute("resourcesCount", 0);
//                }
//            }
//
//            // ✅ UŻYTKOWNIK
//            if (userDetails != null) {
//                model.addAttribute("user", userDetails);
//                model.addAttribute("userId", userId);
//                log.info("Added user details to model");
//            }
//
//            // ✅ LOG ALL MODEL ATTRIBUTES
//            log.info("=== MODEL ATTRIBUTES ===");
//            Map<String, Object> modelMap = model.asMap();
//            for (Map.Entry<String, Object> entry : modelMap.entrySet()) {
//                log.info("  {} = {}", entry.getKey(),
//                        entry.getValue() != null ? entry.getValue().toString() : "null");
//            }
//            log.info("=== END MODEL ATTRIBUTES ===");
//
//            return "meetings/details";
//
//        } catch (Exception e) {
//            log.error("ERROR in meetingDetails: {}", e.getMessage(), e);
//
//            if (userDetails != null) {
//                model.addAttribute("user", userDetails);
//                model.addAttribute("userId", userDetails.getId());
//            }
//            model.addAttribute("error", "Błąd podczas ładowania szczegółów spotkania");
//            return "redirect:/meetings";
//        } finally {
//            log.info("=== END meetingDetails ===");
//        }
//    }
//
//
//
//
//
//
//
//    @GetMapping("/meetings/create")
//    public String showCreateMeetingForm(
//            @AuthenticationPrincipal CustomUserDetails userDetails,
//            Model model) {
//
//        if (userDetails == null) {
//            return "redirect:/login";
//        }
//
//        model.addAttribute("user", userDetails);
//        // Dodaj pusty obiekt dla formularza
//        model.addAttribute("createMeetingRequest", new com.meethub.domain.model.request.CreateMeetingRequest());
//        return "meetings/create"; // nazwa twojego template
//    }
//
//
//    @PostMapping("/meetings/create")
//    public String createMeeting(
//            @Valid @ModelAttribute("createMeetingRequest") CreateMeetingRequest request,
//            BindingResult result,
//            @AuthenticationPrincipal CustomUserDetails userDetails,
//            Model model,
//            RedirectAttributes redirectAttributes) {
//
//        if (userDetails == null) {
//            return "redirect:/login";
//        }
//
//        if (result.hasErrors()) {
//            model.addAttribute("user", userDetails);
//            return "meetings/create";
//        }
//
//        try {
//            MeetingResponse meeting = meetingService.createMeeting(request, userDetails.getId());
//            redirectAttributes.addFlashAttribute("message",
//                    "Spotkanie '" + meeting.getTitle() + "' zostało utworzone pomyślnie!");
//            return "redirect:/meetings/" + meeting.getId();
//
//        } catch (Exception e) {
//            model.addAttribute("user", userDetails);
//            model.addAttribute("error", "Błąd podczas tworzenia spotkania: " + e.getMessage());
//            return "meetings/create";
//        }
//    }
//
//    @GetMapping("/api/users/search")
//    @ResponseBody
//    public ResponseEntity<List<UserResponse>> searchUsers(
//            @RequestParam String query,
//            @AuthenticationPrincipal CustomUserDetails userDetails) {
//
//        if (userDetails == null) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
//        }
//
//        try {
//            List<UserResponse> users = userService.searchUsers(query);
//            return ResponseEntity.ok(users);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }
//
//
//
//
//        @GetMapping("/meetings/{id}/edit")
//        public String showEditMeetingForm(@PathVariable Long id,
//                                          @AuthenticationPrincipal CustomUserDetails userDetails,
//                                          Model model) {
//            if (userDetails == null) {
//                return "redirect:/login";
//            }
//
//            try {
//                MeetingResponse meeting = meetingService.getMeetingById(id);
//                Long userId = userDetails.getId();
//
//                // Sprawdź czy użytkownik jest organizatorem
//                if (!meeting.getOrganizer().getId().equals(userId)) {
//                    return "redirect:/meetings/" + id + "?error=Nie masz uprawnień do edycji tego spotkania";
//                }
//
//                UpdateMeetingRequest updateRequest = UpdateMeetingRequest.builder()
//                        .title(meeting.getTitle())
//                        .description(meeting.getDescription())
//                        .agenda(meeting.getAgenda())
//                        .type(meeting.getType())
//                        .visibility(meeting.getVisibility())
//                        .startDate(meeting.getStartDate())
//                        .endDate(meeting.getEndDate())
//                        .maxParticipants(meeting.getMaxParticipants())
//                        .tags(meeting.getTags())
//                        .build();
//
//                model.addAttribute("updateMeetingRequest", updateRequest);
//                model.addAttribute("meetingId", id);
//                return "meetings/edit";
//
//            } catch (Exception e) {
//                return "redirect:/meetings?error=Spotkanie nie zostało znalezione";
//            }
//        }
//
//        @PostMapping("/meetings/{id}/edit")
//        public String updateMeeting(
//                @PathVariable Long id,
//                @Valid @ModelAttribute("updateMeetingRequest") UpdateMeetingRequest request,
//                BindingResult result,
//                @AuthenticationPrincipal CustomUserDetails userDetails,
//                Model model,
//                RedirectAttributes redirectAttributes) {
//
//            if (userDetails == null) {
//                return "redirect:/login";
//            }
//
//            if (result.hasErrors()) {
//                model.addAttribute("meetingId", id);
//                return "meetings/edit";
//            }
//
//            try {
//                MeetingResponse meeting = meetingService.updateMeeting(id, request, userDetails.getId());
//                redirectAttributes.addFlashAttribute("message",
//                        "Spotkanie '" + meeting.getTitle() + "' zostało zaktualizowane pomyślnie!");
//                return "redirect:/meetings/" + meeting.getId();
//
//            } catch (Exception e) {
//                model.addAttribute("error", "Błąd podczas aktualizacji spotkania: " + e.getMessage());
//                model.addAttribute("meetingId", id);
//                return "meetings/edit";
//            }
//        }
//
//        @PostMapping("/meetings/{id}/delete")
//        public String deleteMeeting(
//                @PathVariable Long id,
//                @AuthenticationPrincipal CustomUserDetails userDetails,
//                RedirectAttributes redirectAttributes) {
//
//            if (userDetails == null) {
//                return "redirect:/login";
//            }
//
//            try {
//                meetingService.deleteMeeting(id, userDetails.getId());
//                redirectAttributes.addFlashAttribute("message", "Spotkanie zostało usunięte pomyślnie!");
//                return "redirect:/meetings";
//
//            } catch (Exception e) {
//                redirectAttributes.addFlashAttribute("error", "Błąd podczas usuwania spotkania: " + e.getMessage());
//                return "redirect:/meetings/" + id;
//            }
//        }
//
//        @GetMapping("/meetings/{id}/duplicate")
//        public String duplicateMeeting(
//                @PathVariable Long id,
//                @AuthenticationPrincipal CustomUserDetails userDetails,
//                RedirectAttributes redirectAttributes) {
//
//            if (userDetails == null) {
//                return "redirect:/login";
//            }
//
//            try {
//                MeetingResponse duplicatedMeeting = meetingService.duplicateMeeting(id, userDetails.getId());
//                redirectAttributes.addFlashAttribute("message",
//                        "Spotkanie zostało skopiowane pomyślnie!");
//                return "redirect:/meetings/" + duplicatedMeeting.getId();
//
//            } catch (Exception e) {
//                redirectAttributes.addFlashAttribute("error", "Błąd podczas kopiowania spotkania: " + e.getMessage());
//                return "redirect:/meetings/" + id;
//            }
//        }
//
//        @PostMapping("/meetings/{id}/join")
//        public String joinMeeting(
//                @PathVariable Long id,
//                @AuthenticationPrincipal CustomUserDetails userDetails,
//                RedirectAttributes redirectAttributes) {
//
//            if (userDetails == null) {
//                return "redirect:/login";
//            }
//
//            try {
//                meetingParticipantService.joinMeeting(userDetails.getId(), id);
//                redirectAttributes.addFlashAttribute("message", "Dołączyłeś do spotkania pomyślnie!");
//                return "redirect:/meetings/" + id;
//
//            } catch (Exception e) {
//                redirectAttributes.addFlashAttribute("error", "Błąd podczas dołączania do spotkania: " + e.getMessage());
//                return "redirect:/meetings/" + id;
//            }
//        }
//
//        @PostMapping("/meetings/{id}/leave")
//        public String leaveMeeting(
//                @PathVariable Long id,
//                @AuthenticationPrincipal CustomUserDetails userDetails,
//                RedirectAttributes redirectAttributes) {
//
//            if (userDetails == null) {
//                return "redirect:/login";
//            }
//
//            try {
//                meetingParticipantService.leaveMeeting(userDetails.getId(), id);
//                redirectAttributes.addFlashAttribute("message", "Opuszczono spotkanie pomyślnie!");
//                return "redirect:/meetings/" + id;
//
//            } catch (Exception e) {
//                redirectAttributes.addFlashAttribute("error", "Błąd podczas opuszczania spotkania: " + e.getMessage());
//                return "redirect:/meetings/" + id;
//            }
//        }
//
//
//
//
//    @GetMapping("/meetings/create-from-template/{templateId}")
//    public String createFromTemplate(@PathVariable Long templateId,
//                                     @AuthenticationPrincipal CustomUserDetails userDetails,
//                                     Model model) {
//        if (userDetails == null) return "redirect:/login";
//
//        MeetingResponse template = meetingService.getMeetingById(templateId);
//        model.addAttribute("template", template);
//        model.addAttribute("user", userDetails);
//        model.addAttribute("today", LocalDate.now());
//
//        return "meetings/create-from-template";
//    }
//
//    @PostMapping("/meetings/create-from-template/{templateId}")
//    public String createFromTemplatePost(@PathVariable Long templateId,
//                                         @RequestParam LocalDateTime startDate,
//                                         @AuthenticationPrincipal CustomUserDetails userDetails,
//                                         RedirectAttributes redirectAttributes) {
//
//        try {
//            MeetingResponse meeting = meetingService.createFromTemplate(templateId,
//                    userDetails.getId(),
//                    startDate);
//            redirectAttributes.addFlashAttribute("message",
//                    "Spotkanie utworzone z szablonu pomyślnie!");
//            return "redirect:/meetings/" + meeting.getId();
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("error", "Błąd: " + e.getMessage());
//            return "redirect:/meetings/templates";
//        }
//    }
//
//    // Lista szablonów
//    @GetMapping("/meetings/templates")
//    public String templates(@AuthenticationPrincipal CustomUserDetails userDetails,
//                            Model model) {
//        if (userDetails == null) return "redirect:/login";
//
//        List<MeetingResponse> templates = meetingService.getMeetingTemplates(userDetails.getId());
//        model.addAttribute("templates", templates);
//        model.addAttribute("user", userDetails);
//
//        return "meetings/templates";
//    }
//
//    // Historia zmian statusu
//    @GetMapping("/meetings/{id}/status-history")
//    public String statusHistory(@PathVariable Long id,
//                                @AuthenticationPrincipal CustomUserDetails userDetails,
//                                Model model) {
//        MeetingResponse meeting = meetingService.getMeetingById(id);
//        model.addAttribute("meeting", meeting);
//        model.addAttribute("user", userDetails);
//
//        return "meetings/status-history";
//    }
//
//    // Zmiana statusu
//    @PostMapping("/meetings/{id}/change-status")
//    public String changeStatus(@PathVariable Long id,
//                               @RequestParam String status,
//                               @RequestParam(required = false) String reason,
//                               @AuthenticationPrincipal CustomUserDetails userDetails,
//                               RedirectAttributes redirectAttributes) {
//
//        try {
//            // Musisz dodać metodę changeStatus w serwisie
//            UpdateMeetingRequest request = new UpdateMeetingRequest();
//            request.setStatus(MeetingStatus.valueOf(status));
//            request.setStatusChangeReason(reason);
//
//            meetingService.updateMeeting(id, request, userDetails.getId());
//
//            redirectAttributes.addFlashAttribute("message",
//                    "Status zmieniony na: " + status);
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("error", "Błąd: " + e.getMessage());
//        }
//
//        return "redirect:/meetings/" + id;
//    }
//
//    // Generuj następne wystąpienia
//    @PostMapping("/meetings/{id}/generate-occurrences")
//    public String generateOccurrences(@PathVariable Long id,
//                                      @RequestParam(defaultValue = "5") int count,
//                                      @AuthenticationPrincipal CustomUserDetails userDetails,
//                                      RedirectAttributes redirectAttributes) {
//
//        try {
//            List<MeetingResponse> occurrences = meetingService.generateNextRecurrence(id, count);
//            redirectAttributes.addFlashAttribute("message",
//                    "Wygenerowano " + occurrences.size() + " następnych wystąpień");
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("error", "Błąd: " + e.getMessage());
//        }
//
//        return "redirect:/meetings/" + id;
//    }
//
//    // ✅ DODAJ KATEGORIE DO MODELU DLA FORMULARZY
//    @ModelAttribute("categories")
//    public List<Category> getCategories(@AuthenticationPrincipal CustomUserDetails userDetails) {
//        if (userDetails == null) return Collections.emptyList();
//        return categoryRepository.findByCreatedById(userDetails.getId());
//    }
//
//
//}










package com.meethub.controller.web;

import com.meethub.domain.model.entity.Category;
import com.meethub.domain.model.entity.Feedback;
import com.meethub.domain.model.entity.MeetingStatistics;
import com.meethub.domain.model.enums.MeetingStatus;
import com.meethub.domain.model.enums.MeetingType;
import com.meethub.domain.model.enums.MeetingVisibility;
import com.meethub.domain.model.projection.LocationBasicInfo;
import com.meethub.domain.model.request.CreateMeetingRequest;
import com.meethub.domain.model.request.SearchCriteria;
import com.meethub.domain.model.request.UpdateMeetingRequest;
import com.meethub.domain.model.request.UserRegistrationRequest;
import com.meethub.domain.model.response.*;
import com.meethub.domain.repository.jpa.CategoryRepository;
import com.meethub.domain.repository.jpa.LocationRepository;
import com.meethub.domain.repository.jpa.UserRepository;
import com.meethub.domain.service.*;
import com.meethub.exception.BusinessException;
import com.meethub.security.CustomUserDetailsService.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
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
    private final MeetingAnalyticsService meetingAnalyticsService;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final LocationService locationService;

    @GetMapping("/")
    public String home(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails != null) {
            model.addAttribute("user", userDetails);
            return "dashboard";
        }

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

    @GetMapping("/meetings")
    public String meetings(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            Model model) {

        log.info("🔍 meetings() called - page: {}, size: {}, search: {}, type: {}, status: {}",
                page, size, search, type, status);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<MeetingResponse> meetingsPage;

            if (userDetails != null) {
                meetingsPage = meetingService.getFilteredMeetings(search, type, status, pageable);
                Long userId = userDetails.getId();

                // ✅ Wzbogać spotkania o informacje o użytkowniku
                List<MeetingResponse> enrichedMeetings = new ArrayList<>();
                for (MeetingResponse meeting : meetingsPage.getContent()) {
                    enrichedMeetings.add(enrichMeetingWithUserInfo(meeting, userId));
                }

                meetingsPage = new PageImpl<>(enrichedMeetings, pageable, meetingsPage.getTotalElements());
                model.addAttribute("userId", userId);
                model.addAttribute("currentUserId", userId);
                model.addAttribute("user", userDetails);
            } else {
                List<MeetingResponse> publicMeetings = meetingService.getUpcomingPublicMeetings();
                int start = (int) pageable.getOffset();
                int end = Math.min((start + pageable.getPageSize()), publicMeetings.size());

                if (start > publicMeetings.size()) {
                    meetingsPage = Page.empty(pageable);
                } else {
                    List<MeetingResponse> pageContent = publicMeetings.subList(start, end);

                    // ✅ Dla niezalogowanych ustaw podstawowe wartości
                    List<MeetingResponse> enrichedPageContent = pageContent.stream()
                            .map(meeting -> MeetingResponse.builder()
                                    .id(meeting.getId())
                                    .title(meeting.getTitle())
                                    .description(meeting.getDescription())
                                    .agenda(meeting.getAgenda())
                                    .type(meeting.getType())
                                    .status(meeting.getStatus())
                                    .visibility(meeting.getVisibility())
                                    .startDate(meeting.getStartDate())
                                    .endDate(meeting.getEndDate())
                                    .maxParticipants(meeting.getMaxParticipants())
                                    .organizer(meeting.getOrganizer())
                                    .location(meeting.getLocation())
                                    .tags(meeting.getTags())
                                    .createdAt(meeting.getCreatedAt())
                                    .updatedAt(meeting.getUpdatedAt())
                                    .confirmedParticipantsCount(meeting.getConfirmedParticipantsCount())
                                    .waitingListCount(meeting.getWaitingListCount())
                                    .availableSpots(meeting.getAvailableSpots())
                                    .userIsParticipant(false)
                                    .userIsOrganizer(false)
                                    .userIsConfirmed(false)
                                    .userIsPending(false)
                                    .userIsInvited(false)
                                    .userIsDeclined(false)
                                    .userIsWaiting(false)
                                    .userIsViewer(true)
                                    .userIsUnrelated(false)
                                    .userRole("VIEWER")
                                    .canJoin(false)
                                    .canLeave(false)
                                    .canEdit(false)
                                    .canDelete(false)
                                    .build())
                            .collect(Collectors.toList());

                    meetingsPage = new PageImpl<>(enrichedPageContent, pageable, publicMeetings.size());
                }

                model.addAttribute("userId", null);
                model.addAttribute("currentUserId", null);
            }

            model.addAttribute("meetings", meetingsPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", meetingsPage.getTotalPages());
            model.addAttribute("totalItems", meetingsPage.getTotalElements());
            model.addAttribute("searchParam", search);
            model.addAttribute("typeParam", type);
            model.addAttribute("statusParam", status);

        } catch (Exception e) {
            log.error("Error loading meetings: {}", e.getMessage(), e);
            model.addAttribute("meetings", Collections.emptyList());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalItems", 0);
            model.addAttribute("warning", "Nie udało się załadować listy spotkań");
        }

        return "meetings/list";
    }

    // ✅ Dodaj tę metodę pomocniczą w WebController:
    private MeetingResponse enrichMeetingWithUserInfo(MeetingResponse meeting, Long userId) {
        try {
            boolean isOrganizer = meeting.getOrganizer() != null &&
                    meeting.getOrganizer().getId().equals(userId);

            boolean isConfirmed = meetingParticipantService.isConfirmedParticipant(meeting.getId(), userId);
            boolean isPending = meetingParticipantService.isPendingParticipant(meeting.getId(), userId);
            boolean isInvited = meetingParticipantService.isInvitedParticipant(meeting.getId(), userId);
            boolean isDeclined = meetingParticipantService.isDeclinedParticipant(meeting.getId(), userId);
            boolean isWaiting = meetingParticipantService.isWaitingListParticipant(meeting.getId(), userId);
            boolean isParticipant = isConfirmed || isPending || isInvited || isWaiting;
            boolean isViewer = !isParticipant && !isOrganizer;
            boolean isUnrelated = false; // lub logika biznesowa

            String userRole = determineUserRole(isOrganizer, isConfirmed, isPending,
                    isInvited, isDeclined, isWaiting,
                    isViewer, isUnrelated);

            boolean canJoin = isViewer && !isOrganizer && meeting.getVisibility() != null &&
                    (meeting.getVisibility() == MeetingVisibility.PUBLIC ||
                            meeting.getVisibility() == MeetingVisibility.PRIVATE);
            boolean canLeave = isConfirmed && !isOrganizer;
            boolean canEdit = isOrganizer;
            boolean canDelete = isOrganizer;

            return MeetingResponse.builder()
                    .id(meeting.getId())
                    .title(meeting.getTitle())
                    .description(meeting.getDescription())
                    .agenda(meeting.getAgenda())
                    .type(meeting.getType())
                    .status(meeting.getStatus())
                    .visibility(meeting.getVisibility())
                    .startDate(meeting.getStartDate())
                    .endDate(meeting.getEndDate())
                    .maxParticipants(meeting.getMaxParticipants())
                    .organizer(meeting.getOrganizer())
                    .location(meeting.getLocation())
                    .tags(meeting.getTags())
                    .createdAt(meeting.getCreatedAt())
                    .updatedAt(meeting.getUpdatedAt())
                    .confirmedParticipantsCount(meeting.getConfirmedParticipantsCount())
                    .waitingListCount(meeting.getWaitingListCount())
                    .availableSpots(meeting.getAvailableSpots())
                    .userIsParticipant(isParticipant)
                    .userIsOrganizer(isOrganizer)
                    .userIsConfirmed(isConfirmed)
                    .userIsPending(isPending)
                    .userIsInvited(isInvited)
                    .userIsDeclined(isDeclined)
                    .userIsWaiting(isWaiting)
                    .userIsViewer(isViewer)
                    .userIsUnrelated(isUnrelated)
                    .userRole(userRole)
                    .canJoin(canJoin)
                    .canLeave(canLeave)
                    .canEdit(canEdit)
                    .canDelete(canDelete)
                    .build();

        } catch (Exception e) {
            log.error("Error enriching meeting {} for user {}: {}",
                    meeting.getId(), userId, e.getMessage());
            return meeting;
        }
    }

    private String determineUserRole(boolean isOrganizer, boolean isConfirmed,
                                     boolean isPending, boolean isInvited,
                                     boolean isDeclined, boolean isWaiting,
                                     boolean isViewer, boolean isUnrelated) {
        if (isOrganizer) return "ORGANIZER";
        if (isConfirmed) return "CONFIRMED_PARTICIPANT";
        if (isPending) return "PENDING";
        if (isInvited) return "INVITED";
        if (isDeclined) return "DECLINED";
        if (isWaiting) return "WAITING_LIST";
        if (isViewer) return "VIEWER";
        if (isUnrelated) return "UNRELATED";
        return "VIEWER";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
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

    @GetMapping("/meetings/{id}")
    public String meetingDetails(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        log.info("=== START meetingDetails ===");
        log.info("Meeting ID: {}", id);

        if (userDetails != null) {
            log.info("User ID: {}", userDetails.getId());
        }

        try {
            MeetingResponse meeting = meetingService.getMeetingById(id);
            model.addAttribute("meeting", meeting);

            Long userId = userDetails != null ? userDetails.getId() : null;

            boolean isAdmin = false;
            if (userDetails != null) {
                isAdmin = userDetails.getAuthorities().stream()
                        .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
            }
            model.addAttribute("isAdmin", isAdmin);

            MeetingParticipationInfo participationInfo = null;
            if (userId != null) {
                try {
                    participationInfo = meetingAuthorizationService.getUserMeetingPermissions(id, userId);
                } catch (Exception e) {
                    log.error("Error loading participation info: {}", e.getMessage());
                }
            }

            try {
                List<ParticipantResponse> participants = meetingParticipantService.getConfirmedParticipants(id);
                model.addAttribute("participants", participants);
            } catch (Exception e) {
                model.addAttribute("participants", Collections.emptyList());
            }

            try {
                Map<String, Long> participantStats = meetingParticipantService.getParticipantStatistics(id);
                model.addAttribute("participantStats", participantStats);
            } catch (Exception e) {
                model.addAttribute("participantStats", new HashMap<>());
            }

            if (userId != null) {
                try {
                    boolean isPending = meetingParticipantService.isPendingParticipant(id, userId);
                    boolean isDeclined = meetingParticipantService.isDeclinedParticipant(id, userId);
                    boolean isInvited = meetingParticipantService.isInvitedParticipant(id, userId);
                    boolean isConfirmed = meetingParticipantService.isConfirmedParticipant(id, userId);

                    model.addAttribute("isPending", isPending);
                    model.addAttribute("isDeclined", isDeclined);
                    model.addAttribute("isInvited", isInvited);
                    model.addAttribute("isConfirmed", isConfirmed);
                } catch (Exception e) {
                    log.error("Error checking user status: {}", e.getMessage());
                }
            }

            if (participationInfo != null) {
                boolean isOrganizer = participationInfo.isOrganizer();
                model.addAttribute("isOrganizer", isOrganizer);
                model.addAttribute("isParticipant", participationInfo.isParticipant());
                model.addAttribute("isRelated", participationInfo.isRelated());
                model.addAttribute("participantRole", participationInfo.getParticipantRole());
                model.addAttribute("canEdit", participationInfo.isCanEdit());
                model.addAttribute("canDelete", participationInfo.isCanDelete());
                model.addAttribute("canManageParticipants", participationInfo.isCanManageParticipants());
                model.addAttribute("canJoin", participationInfo.isCanJoin());
            } else {
                boolean canJoin = meeting != null &&
                        meeting.getVisibility() != null &&
                        meeting.getVisibility().name().equals("PUBLIC");
                model.addAttribute("isOrganizer", false);
                model.addAttribute("isParticipant", false);
                model.addAttribute("isRelated", false);
                model.addAttribute("canJoin", canJoin);
            }

            if (userId != null && participationInfo != null &&
                    (participationInfo.isOrganizer() || isAdmin)) {
                try {
                    Optional<MeetingStatistics> statsOpt = meetingAnalyticsService.getMeetingStatistics(id);
                    model.addAttribute("meetingStatistics", statsOpt.orElse(null));
                } catch (Exception e) {
                    model.addAttribute("meetingStatistics", null);
                }
            } else {
                model.addAttribute("meetingStatistics", null);
            }

            if (userId != null) {
                try {
                    List<VotingResponse> allVotings = meetingVotingService.getMeetingVotings(id, userId);
                    Map<Boolean, List<VotingResponse>> votings = allVotings.stream()
                            .collect(Collectors.partitioningBy(v -> v.getStatus().name().equals("ACTIVE")));

                    model.addAttribute("activeVotings", votings.get(true));
                    model.addAttribute("closedVotings", votings.get(false));
                } catch (Exception e) {
                    model.addAttribute("activeVotings", Collections.emptyList());
                    model.addAttribute("closedVotings", Collections.emptyList());
                }
            }

            if (userId != null && participationInfo != null && participationInfo.isParticipant()) {
                try {
                    Feedback userFeedback = feedbackService.getUserFeedback(id, userId);
                    model.addAttribute("userFeedback", userFeedback);
                } catch (Exception e) {
                    model.addAttribute("userFeedback", null);
                }
            }

            if (userId != null) {
                try {
                    List<MeetingResourceResponse> resources = resourceService.getMeetingResources(id, userId);
                    model.addAttribute("resources", resources);
                    model.addAttribute("resourcesCount", resources.size());
                } catch (Exception e) {
                    model.addAttribute("resources", Collections.emptyList());
                    model.addAttribute("resourcesCount", 0);
                }
            }

            if (userDetails != null) {
                model.addAttribute("user", userDetails);
                model.addAttribute("userId", userId);
            }

            return "meetings/details";

        } catch (Exception e) {
            log.error("ERROR in meetingDetails: {}", e.getMessage(), e);

            if (userDetails != null) {
                model.addAttribute("user", userDetails);
                model.addAttribute("userId", userDetails.getId());
            }
            model.addAttribute("error", "Błąd podczas ładowania szczegółów spotkania");
            return "redirect:/meetings";
        } finally {
            log.info("=== END meetingDetails ===");
        }
    }

//    @GetMapping("/meetings/create")
//    public String showCreateMeetingForm(
//            @AuthenticationPrincipal CustomUserDetails userDetails,
//            Model model) {
//
//        if (userDetails == null) {
//            return "redirect:/login";
//        }
//
//        model.addAttribute("user", userDetails);
//        model.addAttribute("createMeetingRequest", new CreateMeetingRequest());
//        return "meetings/create";
//    }

    @GetMapping("/meetings/create")
    public String showCreateMeetingForm(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        // ✅ Pobierz listę lokalizacji dla selecta
        List<LocationBasicInfo> locations = locationService.getLocationsForSelect();

        model.addAttribute("user", userDetails);
        model.addAttribute("createMeetingRequest", new CreateMeetingRequest());
        model.addAttribute("locations", locations); // ✅ Dodaj lokalizacje do modelu

        return "meetings/create";
    }

//    @PostMapping("/meetings/create")
//    public String createMeeting(
//            @Valid @ModelAttribute("createMeetingRequest") CreateMeetingRequest request,
//            BindingResult result,
//            @AuthenticationPrincipal CustomUserDetails userDetails,
//            Model model,
//            RedirectAttributes redirectAttributes) {
//
//        if (userDetails == null) {
//            return "redirect:/login";
//        }
//
//        if (result.hasErrors()) {
//            model.addAttribute("user", userDetails);
//            return "meetings/create";
//        }
//
//        try {
//            MeetingResponse meeting = meetingService.createMeeting(request, userDetails.getId());
//            redirectAttributes.addFlashAttribute("message",
//                    "Spotkanie '" + meeting.getTitle() + "' zostało utworzone pomyślnie!");
//            return "redirect:/meetings/" + meeting.getId();
//
//        } catch (Exception e) {
//            model.addAttribute("user", userDetails);
//            model.addAttribute("error", "Błąd podczas tworzenia spotkania: " + e.getMessage());
//            return "meetings/create";
//        }
//    }


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

        // ✅ WALIDACJA LOKALIZACJI W ZALEŻNOŚCI OD TYPU SPOTKANIA
        if (request.getType() != MeetingType.ONLINE && request.getLocationId() == null) {
            result.rejectValue("locationId", "NotNull",
                    "Lokalizacja jest wymagana dla spotkań osobiście lub hybrydowych");
        }

        // ✅ WALIDACJA: Sprawdź czy lokalizacja istnieje (jeśli wybrana)
        if (request.getLocationId() != null) {
            try {
                // Sprawdź czy lokalizacja istnieje w bazie
                boolean locationExists = locationRepository.existsById(request.getLocationId());
                if (!locationExists) {
                    result.rejectValue("locationId", "Invalid",
                            "Wybrana lokalizacja nie istnieje");
                }
            } catch (Exception e) {
                result.rejectValue("locationId", "Invalid",
                        "Błąd podczas weryfikacji lokalizacji");
            }
        }

        if (result.hasErrors()) {
            // ✅ Ponownie załaduj listę lokalizacji w przypadku błędów walidacji
            List<LocationBasicInfo> locations = locationService.getLocationsForSelect();
            model.addAttribute("locations", locations);
            model.addAttribute("user", userDetails);
            return "meetings/create";
        }

        try {
            MeetingResponse meeting = meetingService.createMeeting(request, userDetails.getId());
            redirectAttributes.addFlashAttribute("message",
                    "Spotkanie '" + meeting.getTitle() + "' zostało utworzone pomyślnie!");
            return "redirect:/meetings/" + meeting.getId();

        } catch (Exception e) {
            // ✅ Ponownie załaduj listę lokalizacji w przypadku błędu serwisu
            List<LocationBasicInfo> locations = locationService.getLocationsForSelect();
            model.addAttribute("locations", locations);
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

    @GetMapping("/meetings/{id}/edit")
    public String showEditMeetingForm(@PathVariable Long id,
                                      @AuthenticationPrincipal CustomUserDetails userDetails,
                                      Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            MeetingResponse meeting = meetingService.getMeetingById(id);
            Long userId = userDetails.getId();

            if (!meeting.getOrganizer().getId().equals(userId)) {
                return "redirect:/meetings/" + id + "?error=Nie masz uprawnień do edycji tego spotkania";
            }

            UpdateMeetingRequest updateRequest = UpdateMeetingRequest.builder()
                    .title(meeting.getTitle())
                    .description(meeting.getDescription())
                    .agenda(meeting.getAgenda())
                    .type(meeting.getType())
                    .visibility(meeting.getVisibility())
                    .startDate(meeting.getStartDate())
                    .endDate(meeting.getEndDate())
                    .maxParticipants(meeting.getMaxParticipants())
                    .tags(meeting.getTags())
                    .recurring(meeting.isRecurring())
                    .recurrencePattern(meeting.getRecurrencePattern())
                    .recurrenceEndDate(meeting.getRecurrenceEndDate())
                    .status(meeting.getStatus())
                    .build();

            model.addAttribute("updateMeetingRequest", updateRequest);
            model.addAttribute("meetingId", id);
            model.addAttribute("user", userDetails);
            return "meetings/edit";

        } catch (Exception e) {
            return "redirect:/meetings?error=Spotkanie nie zostało znalezione";
        }
    }

    @PostMapping("/meetings/{id}/edit")
    public String updateMeeting(
            @PathVariable Long id,
            @Valid @ModelAttribute("updateMeetingRequest") UpdateMeetingRequest request,
            BindingResult result,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            model.addAttribute("meetingId", id);
            model.addAttribute("user", userDetails);
            return "meetings/edit";
        }

        try {
            MeetingResponse meeting = meetingService.updateMeeting(id, request, userDetails.getId());
            redirectAttributes.addFlashAttribute("message",
                    "Spotkanie '" + meeting.getTitle() + "' zostało zaktualizowane pomyślnie!");
            return "redirect:/meetings/" + meeting.getId();

        } catch (Exception e) {
            model.addAttribute("error", "Błąd podczas aktualizacji spotkania: " + e.getMessage());
            model.addAttribute("meetingId", id);
            model.addAttribute("user", userDetails);
            return "meetings/edit";
        }
    }

    @PostMapping("/meetings/{id}/delete")
    public String deleteMeeting(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            meetingService.deleteMeeting(id, userDetails.getId());
            redirectAttributes.addFlashAttribute("message", "Spotkanie zostało usunięte pomyślnie!");
            return "redirect:/meetings";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Błąd podczas usuwania spotkania: " + e.getMessage());
            return "redirect:/meetings/" + id;
        }
    }

    @GetMapping("/meetings/{id}/duplicate")
    public String duplicateMeeting(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            MeetingResponse duplicatedMeeting = meetingService.duplicateMeeting(id, userDetails.getId());
            redirectAttributes.addFlashAttribute("message",
                    "Spotkanie zostało skopiowane pomyślnie!");
            return "redirect:/meetings/" + duplicatedMeeting.getId();

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Błąd podczas kopiowania spotkania: " + e.getMessage());
            return "redirect:/meetings/" + id;
        }
    }

    @PostMapping("/meetings/{id}/join")
    public String joinMeeting(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            meetingParticipantService.joinMeeting(userDetails.getId(), id);
            redirectAttributes.addFlashAttribute("message", "Dołączyłeś do spotkania pomyślnie!");
            return "redirect:/meetings/" + id;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Błąd podczas dołączania do spotkania: " + e.getMessage());
            return "redirect:/meetings/" + id;
        }
    }

    @PostMapping("/meetings/{id}/leave")
    public String leaveMeeting(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            meetingParticipantService.leaveMeeting(userDetails.getId(), id);
            redirectAttributes.addFlashAttribute("message", "Opuszczono spotkanie pomyślnie!");
            return "redirect:/meetings/" + id;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Błąd podczas opuszczania spotkania: " + e.getMessage());
            return "redirect:/meetings/" + id;
        }
    }

    // ✅ NOWE ENDPOINTY DLA SZABLONÓW

//    @GetMapping("/meetings/templates")
//    public String templates(@AuthenticationPrincipal CustomUserDetails userDetails,
//                            Model model) {
//        if (userDetails == null) return "redirect:/login";
//
//        List<MeetingResponse> templates = meetingService.getMeetingTemplates(userDetails.getId());
//        model.addAttribute("templates", templates);
//        model.addAttribute("user", userDetails);
//
//        return "meetings/templates";
//    }


    @GetMapping("/meetings/templates")
    public String templates(@AuthenticationPrincipal CustomUserDetails userDetails,
                            Model model) {
        if (userDetails == null) return "redirect:/login";

        log.info("🔍 Loading templates for user: {}", userDetails.getId());

        try {
            List<MeetingResponse> templates = meetingService.getMeetingTemplates(userDetails.getId());
            log.info("✅ Raw templates count: {}", templates.size());

            // Filtruj null wartości
            List<MeetingResponse> validTemplates = templates.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            log.info("📊 Valid templates count: {}", validTemplates.size());
            log.info("tempLOGIN {}", validTemplates);

            model.addAttribute("templates", validTemplates);
            model.addAttribute("user", userDetails);

            return "meetings/templates";

        } catch (Exception e) {
            log.error("❌ Error loading templates: {}", e.getMessage(), e);
            model.addAttribute("error", "Błąd podczas ładowania szablonów: " + e.getMessage());
            return "redirect:/meetings";
        }
    }

//    @GetMapping("/meetings/create-from-template/{templateId}")
//    public String createFromTemplate(@PathVariable Long templateId,
//                                     @AuthenticationPrincipal CustomUserDetails userDetails,
//                                     Model model) {
//        if (userDetails == null) return "redirect:/login";
//
//        MeetingResponse template = meetingService.getMeetingById(templateId);
//        model.addAttribute("template", template);
//        model.addAttribute("user", userDetails);
//        model.addAttribute("today", LocalDate.now());
//
//        return "meetings/create-from-template";
//    }

    @GetMapping("/meetings/create-from-template/{templateId}")
    public String createFromTemplate(@PathVariable Long templateId,
                                     @AuthenticationPrincipal CustomUserDetails userDetails,
                                     Model model) {
        if (userDetails == null) return "redirect:/login";

        try {
            // Pobierz szablon
            MeetingResponse template = meetingService.getMeetingById(templateId);

            // Sprawdź czy to szablon
            if (!template.isTemplate()) {
                throw new BusinessException("To nie jest szablon");
            }

            // ✅ STWÓRZ CreateMeetingRequest WYPEŁNIONY WARTOŚCIAMI Z SZABLONU
            CreateMeetingRequest request = CreateMeetingRequest.builder()
                    .title(template.getTitle() + " (z szablonu)")
                    .description(template.getDescription())
                    .agenda(template.getAgenda())
                    .type(template.getType())
                    .visibility(template.getVisibility())
                    .maxParticipants(template.getMaxParticipants())
                    .tags(template.getTags())
                    // Ustaw datę jak w szablonie (to samo miejsce w tygodniu)
                    .startDate(getNextOccurrenceDate(template))
                    .endDate(getNextOccurrenceEndDate(template))
                    .build();

            // ✅ PRZEKAŻ DO ISTNIEJĄCEGO FORMULARZA
            model.addAttribute("createMeetingRequest", request);
            model.addAttribute("user", userDetails);
            model.addAttribute("locations", locationService.getLocationsForSelect());
            model.addAttribute("fromTemplate", true); // Flaga że tworzymy z szablonu
            model.addAttribute("templateName", template.getTitle());
            model.addAttribute("templateId", templateId);

            return "meetings/create"; // ✅ Użyj istniejącego formularza create.html

        } catch (Exception e) {
            log.error("Error loading template {}: {}", templateId, e.getMessage());
            return "redirect:/meetings/templates?error=" + e.getMessage();
        }
    }

    // ✅ NOWA METODA: Znajdź następne wystąpienie tego samego dnia tygodnia/godziny
    private LocalDateTime getNextOccurrenceDate(MeetingResponse template) {
        if (template.getStartDate() == null) {
            // Jeśli szablon nie ma daty, ustaw domyślną: za tydzień 10:00
            return LocalDateTime.now().plusWeeks(1).withHour(10).withMinute(0).withSecond(0);
        }

        LocalDateTime templateDateTime = template.getStartDate();
        LocalDateTime now = LocalDateTime.now();

        // Znajdź następne wystąpienie tego samego dnia tygodnia i godziny
        LocalDateTime nextDate = templateDateTime;

        // Jeśli data szablonu jest w przeszłości, znajdź następny taki sam dzień
        while (nextDate.isBefore(now)) {
            nextDate = nextDate.plusWeeks(1); // Za tydzień
        }

        return nextDate;
    }

    // ✅ NOWA METODA: Oblicz datę zakończenia z zachowaniem czasu trwania
    private LocalDateTime getNextOccurrenceEndDate(MeetingResponse template) {
        LocalDateTime startDate = getNextOccurrenceDate(template);

        if (template.getStartDate() != null && template.getEndDate() != null) {
            // Zachowaj taki sam czas trwania jak w szablonie
            long durationMinutes = Duration.between(
                    template.getStartDate(),
                    template.getEndDate()
            ).toMinutes();
            return startDate.plusMinutes(durationMinutes);
        }

        // Domyślnie: startDate + 1 godzina
        return startDate.plusHours(1);
    }

    @PostMapping("/meetings/create-from-template/{templateId}")
    public String createFromTemplatePost(@PathVariable Long templateId,
                                         @RequestParam LocalDateTime startDate,
                                         @AuthenticationPrincipal CustomUserDetails userDetails,
                                         RedirectAttributes redirectAttributes) {

        try {
            MeetingResponse meeting = meetingService.createFromTemplate(templateId,
                    userDetails.getId(),
                    startDate);
            redirectAttributes.addFlashAttribute("message",
                    "Spotkanie utworzone z szablonu pomyślnie!");
            return "redirect:/meetings/" + meeting.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Błąd: " + e.getMessage());
            return "redirect:/meetings/templates";
        }
    }

    @PostMapping("/meetings/{id}/save-as-template")
    public String saveAsTemplate(@PathVariable Long id,
                                 @RequestParam String templateName,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        try {
            MeetingResponse template = meetingService.saveAsTemplate(id, templateName, userDetails.getId());
            redirectAttributes.addFlashAttribute("message",
                    "Spotkanie zapisane jako szablon: " + templateName);
            return "redirect:/meetings/templates";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Błąd: " + e.getMessage());
            return "redirect:/meetings/" + id;
        }
    }

    // ✅ NOWE ENDPOINTY DLA POWTARZAJĄCYCH SIĘ SPOTKAŃ

    @GetMapping("/meetings/{id}/recurrence")
    public String recurrenceDetails(@PathVariable Long id,
                                    @AuthenticationPrincipal CustomUserDetails userDetails,
                                    Model model) {
        MeetingResponse meeting = meetingService.getMeetingById(id);
        model.addAttribute("meeting", meeting);
        model.addAttribute("user", userDetails);

        try {
            List<MeetingResponse> series = meetingService.getRecurrenceSeries(id);
            model.addAttribute("series", series);
        } catch (Exception e) {
            model.addAttribute("series", Collections.emptyList());
        }

        return "meetings/recurrence-details";
    }

    @PostMapping("/meetings/{id}/generate-occurrences")
    public String generateOccurrences(@PathVariable Long id,
                                      @RequestParam(defaultValue = "5") int count,
                                      @AuthenticationPrincipal CustomUserDetails userDetails,
                                      RedirectAttributes redirectAttributes) {

        try {
            List<MeetingResponse> occurrences = meetingService.generateNextRecurrence(id, count);
            redirectAttributes.addFlashAttribute("message",
                    "Wygenerowano " + occurrences.size() + " następnych wystąpień");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Błąd: " + e.getMessage());
        }

        return "redirect:/meetings/" + id + "/recurrence";
    }

    @PostMapping("/meetings/{id}/add-exception")
    public String addRecurrenceException(@PathVariable Long id,
                                         @RequestParam String exceptionDate,
                                         @RequestParam(required = false) String reason,
                                         @AuthenticationPrincipal CustomUserDetails userDetails,
                                         RedirectAttributes redirectAttributes) {

        try {
            meetingService.addRecurrenceException(id, exceptionDate, reason);
            redirectAttributes.addFlashAttribute("message",
                    "Dodano wyjątek: " + exceptionDate);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Błąd: " + e.getMessage());
        }

        return "redirect:/meetings/" + id + "/recurrence";
    }

    // ✅ NOWE ENDPOINTY DLA KATEGORII

    @GetMapping("/categories")
    public String categories(@AuthenticationPrincipal CustomUserDetails userDetails,
                             Model model) {
        if (userDetails == null) return "redirect:/login";

        List<Category> categories = categoryRepository.findByCreatedById(userDetails.getId());
        model.addAttribute("categories", categories);
        model.addAttribute("user", userDetails);

        return "categories/list";
    }

    @GetMapping("/categories/create")
    public String showCreateCategoryForm(@AuthenticationPrincipal CustomUserDetails userDetails,
                                         Model model) {
        if (userDetails == null) return "redirect:/login";

        model.addAttribute("category", new Category());
        model.addAttribute("user", userDetails);

        return "categories/create";
    }

    @PostMapping("/categories/create")
    public String createCategory(@Valid @ModelAttribute("category") Category category,
                                 BindingResult result,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        if (userDetails == null) return "redirect:/login";

        if (result.hasErrors()) {
            return "categories/create";
        }

        try {
            category.setCreatedBy(userRepository.findById(userDetails.getId()).orElseThrow());
            categoryRepository.save(category);

            redirectAttributes.addFlashAttribute("message",
                    "Kategoria '" + category.getName() + "' została utworzona!");
            return "redirect:/categories";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Błąd: " + e.getMessage());
            return "redirect:/categories/create";
        }
    }

    @GetMapping("/categories/{id}/edit")
    public String showEditCategoryForm(@PathVariable Long id,
                                       @AuthenticationPrincipal CustomUserDetails userDetails,
                                       RedirectAttributes redirectAttributes,
                                       Model model) {
        if (userDetails == null) return "redirect:/login";

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kategoria nie znaleziona"));

        if (!category.getCreatedBy().getId().equals(userDetails.getId())) {
            redirectAttributes.addFlashAttribute("error", "Brak uprawnień do edycji tej kategorii");
            return "redirect:/categories";
        }

        model.addAttribute("category", category);
        model.addAttribute("user", userDetails);

        return "categories/edit";
    }

    @PostMapping("/categories/{id}/edit")
    public String updateCategory(@PathVariable Long id,
                                 @Valid @ModelAttribute("category") Category category,
                                 BindingResult result,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        if (userDetails == null) return "redirect:/login";

        if (result.hasErrors()) {
            return "categories/edit";
        }

        try {
            Category existingCategory = categoryRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Kategoria nie znaleziona"));

            if (!existingCategory.getCreatedBy().getId().equals(userDetails.getId())) {
                redirectAttributes.addFlashAttribute("error", "Brak uprawnień do edycji tej kategorii");
                return "redirect:/categories";
            }

            existingCategory.setName(category.getName());
            existingCategory.setDescription(category.getDescription());
            existingCategory.setColorCode(category.getColorCode());

            categoryRepository.save(existingCategory);

            redirectAttributes.addFlashAttribute("message",
                    "Kategoria '" + existingCategory.getName() + "' została zaktualizowana!");
            return "redirect:/categories";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Błąd: " + e.getMessage());
            return "redirect:/categories/" + id + "/edit";
        }
    }

    @PostMapping("/categories/{id}/delete")
    public String deleteCategory(@PathVariable Long id,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        if (userDetails == null) return "redirect:/login";

        try {
            Category category = categoryRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Kategoria nie znaleziona"));

            if (!category.getCreatedBy().getId().equals(userDetails.getId())) {
                redirectAttributes.addFlashAttribute("error", "Brak uprawnień do usunięcia tej kategorii");
                return "redirect:/categories";
            }

            categoryRepository.delete(category);

            redirectAttributes.addFlashAttribute("message",
                    "Kategoria '" + category.getName() + "' została usunięta!");
            return "redirect:/categories";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Błąd: " + e.getMessage());
            return "redirect:/categories";
        }
    }

    // ✅ ENDPOINTY DLA HISTORII STATUSÓW

    @GetMapping("/meetings/{id}/status-history")
    public String statusHistory(@PathVariable Long id,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                Model model) {
        MeetingResponse meeting = meetingService.getMeetingById(id);
        model.addAttribute("meeting", meeting);
        model.addAttribute("user", userDetails);

        return "meetings/status-history";
    }

    @PostMapping("/meetings/{id}/change-status")
    public String changeStatus(@PathVariable Long id,
                               @RequestParam String status,
                               @RequestParam(required = false) String reason,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               RedirectAttributes redirectAttributes) {

        try {
            UpdateMeetingRequest request = UpdateMeetingRequest.builder()
                    .status(MeetingStatus.valueOf(status))
                    .statusChangeReason(reason)
                    .build();

            meetingService.updateMeeting(id, request, userDetails.getId());

            redirectAttributes.addFlashAttribute("message",
                    "Status zmieniony na: " + status);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Błąd: " + e.getMessage());
        }

        return "redirect:/meetings/" + id;
    }

    // ✅ ZAAWANSOWANE FILTROWANIE

    @GetMapping("/meetings/advanced")
    public String advancedMeetings(@AuthenticationPrincipal CustomUserDetails userDetails,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "12") int size,
                                   @RequestParam(required = false) String search,
                                   @RequestParam(required = false) String type,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(required = false) List<Long> categoryIds,
                                   @RequestParam(required = false) List<String> tags,
                                   @RequestParam(required = false) Boolean recurring,
                                   @RequestParam(required = false) Boolean template,
                                   @RequestParam(required = false) String sortBy,
                                   @RequestParam(defaultValue = "desc") String sortOrder,
                                   Model model) {

        if (userDetails == null) return "redirect:/login";

        try {
            Pageable pageable = PageRequest.of(page, size,
                    Sort.by("asc".equalsIgnoreCase(sortOrder) ?
                                    Sort.Direction.ASC : Sort.Direction.DESC,
                            getSortField(sortBy)));

            // Użyj istniejącej metody lub rozszerz ją
            Page<MeetingResponse> meetingsPage = meetingService.getFilteredMeetings(
                    search, type, status, pageable);

            // Dodatkowe filtrowanie po stronie serwera dla nowych funkcji
            List<MeetingResponse> filteredMeetings = meetingsPage.getContent().stream()
                    .filter(meeting -> {
                        if (categoryIds != null && !categoryIds.isEmpty()) {
                            if (meeting.getCategories() == null) return false;
                            Set<Long> meetingCategoryIds = meeting.getCategories().stream()
                                    .map(CategoryResponse::getId)
                                    .collect(Collectors.toSet());
                            return meetingCategoryIds.stream().anyMatch(categoryIds::contains);
                        }
                        return true;
                    })
                    .filter(meeting -> {
                        if (tags != null && !tags.isEmpty()) {
                            if (meeting.getTags() == null) return false;
                            return meeting.getTags().stream().anyMatch(tags::contains);
                        }
                        return true;
                    })
                    .filter(meeting -> {
                        if (recurring != null) {
                            return meeting.isRecurring() == recurring;
                        }
                        return true;
                    })
                    .filter(meeting -> {
                        if (template != null) {
                            return meeting.isTemplate() == template;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());

            // Konwersja z powrotem na Page
            Page<MeetingResponse> finalPage = new PageImpl<>(
                    filteredMeetings,
                    pageable,
                    filteredMeetings.size()
            );

            model.addAttribute("meetings", finalPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", finalPage.getTotalPages());
            model.addAttribute("totalItems", finalPage.getTotalElements());
            model.addAttribute("user", userDetails);

            // Dodaj parametry do modelu dla formularza
            model.addAttribute("searchParam", search);
            model.addAttribute("typeParam", type);
            model.addAttribute("statusParam", status);
            model.addAttribute("categoryIds", categoryIds);
            model.addAttribute("tags", tags);
            model.addAttribute("recurring", recurring);
            model.addAttribute("template", template);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("sortOrder", sortOrder);

            // Dodaj listę kategorii użytkownika do wyboru
            List<Category> userCategories = categoryRepository.findByCreatedById(userDetails.getId());
            model.addAttribute("userCategories", userCategories);

        } catch (Exception e) {
            log.error("Error in advanced meetings search: {}", e.getMessage(), e);
            model.addAttribute("meetings", Collections.emptyList());
            model.addAttribute("error", "Błąd podczas wyszukiwania spotkań");
        }

        return "meetings/advanced-search";
    }

    private String getSortField(String sortBy) {
        if (sortBy == null || sortBy.isEmpty()) {
            return "startDate";
        }
        switch (sortBy) {
            case "title": return "title";
            case "created": return "createdAt";
            case "updated": return "updatedAt";
            case "participants": return "confirmedParticipantsCount";
            default: return "startDate";
        }
    }

    // ✅ ENDPOINT DLA SPOTKAŃ Z KATEGORIĄ

    @GetMapping("/meetings/category/{categoryId}")
    public String meetingsByCategory(@PathVariable Long categoryId,
                                     @AuthenticationPrincipal CustomUserDetails userDetails,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "12") int size,
                                     Model model) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<MeetingResponse> meetingsPage = meetingService.getMeetingsByCategory(categoryId, pageable);

            model.addAttribute("meetings", meetingsPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", meetingsPage.getTotalPages());
            model.addAttribute("totalItems", meetingsPage.getTotalElements());
            model.addAttribute("user", userDetails);

            Category category = categoryRepository.findById(categoryId).orElse(null);
            model.addAttribute("currentCategory", category);

        } catch (Exception e) {
            log.error("Error loading meetings by category: {}", e.getMessage(), e);
            model.addAttribute("meetings", Collections.emptyList());
            model.addAttribute("error", "Błąd podczas ładowania spotkań z kategorii");
        }

        return "meetings/by-category";
    }

    // ✅ ENDPOINT DLA SPOTKAŃ Z TAGIEM

    @GetMapping("/meetings/tag/{tag}")
    public String meetingsByTag(@PathVariable String tag,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "12") int size,
                                Model model) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<MeetingResponse> meetingsPage = meetingService.getMeetingsByTag(tag, pageable);

            model.addAttribute("meetings", meetingsPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", meetingsPage.getTotalPages());
            model.addAttribute("totalItems", meetingsPage.getTotalElements());
            model.addAttribute("user", userDetails);
            model.addAttribute("currentTag", tag);

        } catch (Exception e) {
            log.error("Error loading meetings by tag: {}", e.getMessage(), e);
            model.addAttribute("meetings", Collections.emptyList());
            model.addAttribute("error", "Błąd podczas ładowania spotkań z tagiem");
        }

        return "meetings/by-tag";
    }

    // ✅ ENDPOINT DLA POWTARZAJĄCYCH SIĘ SPOTKAŃ UŻYTKOWNIKA

    @GetMapping("/meetings/recurring")
    public String recurringMeetings(@AuthenticationPrincipal CustomUserDetails userDetails,
                                    Model model) {
        if (userDetails == null) return "redirect:/login";

        try {
            List<MeetingResponse> recurringMeetings = meetingService.getUpcomingRecurringMeetings(userDetails.getId());
            model.addAttribute("recurringMeetings", recurringMeetings);
            model.addAttribute("user", userDetails);
        } catch (Exception e) {
            log.error("Error loading recurring meetings: {}", e.getMessage(), e);
            model.addAttribute("recurringMeetings", Collections.emptyList());
            model.addAttribute("error", "Błąd podczas ładowania powtarzających się spotkań");
        }

        return "meetings/recurring";
    }

    // ✅ MODEL ATTRIBUTE DLA KATEGORII (dla formularzy)

    @ModelAttribute("userCategories")
    public List<CategoryResponse> getUserCategories(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) return Collections.emptyList();

        try {
            List<Category> categories = categoryRepository.findByCreatedById(userDetails.getId());
            return categories.stream()
                    .map(cat -> CategoryResponse.builder()
                            .id(cat.getId())
                            .name(cat.getName())
                            .colorCode(cat.getColorCode())
                            .description(cat.getDescription())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error loading user categories: {}", e.getMessage());
            return Collections.emptyList();
        }
    }



    @GetMapping("/meetings/search")
    public String searchMeetings(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String keywords,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) List<String> searchFields,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false, defaultValue = "0") Integer minParticipants,
            @RequestParam(required = false, defaultValue = "100") Integer maxParticipants,
            @RequestParam(required = false) String organizerName,
            @RequestParam(required = false) String myParticipation,
            @RequestParam(required = false) String visibility,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean recurringOnly,
            @RequestParam(required = false) Boolean templatesOnly,
            @RequestParam(required = false) Boolean hasAttachments,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Model model) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            // ✅ KONWERSJA TYPÓW
            MeetingType meetingType = null;
            if (type != null && !type.isEmpty()) {
                try {
                    meetingType = MeetingType.valueOf(type);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid meeting type provided: {}", type);
                }
            }

            MeetingVisibility meetingVisibility = null;
            if (visibility != null && !visibility.isEmpty()) {
                try {
                    meetingVisibility = MeetingVisibility.valueOf(visibility);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid visibility provided: {}", visibility);
                }
            }

            // ✅ Przygotuj kryteria wyszukiwania z poprawnymi typami
            SearchCriteria criteria = SearchCriteria.builder()
                    .keywords(keywords)
                    .tags(tags)
                    .searchFields(searchFields != null ? searchFields : Arrays.asList("TITLE", "DESCRIPTION"))
                    .dateFrom(dateFrom)
                    .dateTo(dateTo)
                    .type(meetingType)  // ✅ MeetingType zamiast String
                    .statuses(status)    // ✅ Uwaga: statuses zamiast status
                    .minParticipants(minParticipants)
                    .maxParticipants(maxParticipants)
                    .organizerName(organizerName)
                    .myParticipation(myParticipation)
                    .visibility(meetingVisibility)  // ✅ MeetingVisibility zamiast String
                    .sortBy(sortBy)
                    .categoryIds(categories)
                    .recurringOnly(Boolean.TRUE.equals(recurringOnly))
                    .templatesOnly(Boolean.TRUE.equals(templatesOnly))
                    .hasAttachments(Boolean.TRUE.equals(hasAttachments))
                    .currentUserId(userDetails.getId())  // ✅ Dodaj currentUserId
                    .userAuthenticated(true)
                    .includePublic(true)
                    .build();

            // ✅ Utwórz Pageable z sortowaniem
            Pageable pageable = createSortedPageable(page, size, sortBy);

            // ✅ Wykonaj wyszukiwanie
            Page<MeetingResponse> meetingsPage = meetingService.searchMeetings(criteria, pageable);

            // ✅ Wzbogać spotkania o informacje o użytkowniku
            List<MeetingResponse> enrichedMeetings = new ArrayList<>();
            for (MeetingResponse meeting : meetingsPage.getContent()) {
                enrichedMeetings.add(enrichMeetingWithUserInfo(meeting, userDetails.getId()));
            }

            Page<MeetingResponse> finalPage = new PageImpl<>(
                    enrichedMeetings,
                    pageable,
                    meetingsPage.getTotalElements()
            );

            // ✅ Dodaj do modelu
            model.addAttribute("meetings", finalPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", finalPage.getTotalPages());
            model.addAttribute("totalItems", finalPage.getTotalElements());
            model.addAttribute("pageSize", size);
            model.addAttribute("user", userDetails);
            model.addAttribute("searchCriteria", criteria);
            model.addAttribute("isSearchResults", true);
            model.addAttribute("resultsCount", finalPage.getTotalElements());

            // ✅ Przekaż parametry dla paginacji
            Map<String, String> searchParams = new LinkedHashMap<>(); // LinkedHashMap zachowuje kolejność
            if (keywords != null) searchParams.put("keywords", keywords);
            if (tags != null) searchParams.put("tags", tags);
            if (dateFrom != null) searchParams.put("dateFrom", dateFrom.toString());
            if (dateTo != null) searchParams.put("dateTo", dateTo.toString());
            if (type != null) searchParams.put("type", type);
            if (status != null && !status.isEmpty()) {
                searchParams.put("status", String.join(",", status));
            }
            if (organizerName != null) searchParams.put("organizerName", organizerName);
            if (myParticipation != null) searchParams.put("myParticipation", myParticipation);
            if (visibility != null) searchParams.put("visibility", visibility);
            if (sortBy != null) searchParams.put("sortBy", sortBy);
            if (categories != null && !categories.isEmpty()) {
                searchParams.put("categories", categories.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(",")));
            }
            if (recurringOnly != null) searchParams.put("recurringOnly", recurringOnly.toString());
            if (templatesOnly != null) searchParams.put("templatesOnly", templatesOnly.toString());
            if (hasAttachments != null) searchParams.put("hasAttachments", hasAttachments.toString());

            model.addAttribute("searchParams", searchParams);

            // ✅ Dodaj listę kategorii użytkownika do wyboru w formularzu
            List<Category> userCategories = categoryRepository.findByCreatedById(userDetails.getId());
            model.addAttribute("userCategories", userCategories);

            return "meetings/search-results";

        } catch (Exception e) {
            log.error("Error in advanced search: {}", e.getMessage(), e);
            model.addAttribute("user", userDetails);
            model.addAttribute("error", "Błąd podczas wyszukiwania: " + e.getMessage());
            model.addAttribute("meetings", Collections.emptyList());
            return "meetings/search-results";
        }
    }

    // ✅ Metoda pomocnicza do tworzenia Pageable z sortowaniem
    private Pageable createSortedPageable(int page, int size, String sortBy) {
        Sort sort;

        if (sortBy == null || sortBy.isEmpty()) {
            sort = Sort.by(Sort.Direction.DESC, "startDate");
        } else {
            switch (sortBy) {
                case "START_DATE_ASC":
                    sort = Sort.by(Sort.Direction.ASC, "startDate");
                    break;
                case "START_DATE_DESC":
                    sort = Sort.by(Sort.Direction.DESC, "startDate");
                    break;
                case "TITLE_ASC":
                    sort = Sort.by(Sort.Direction.ASC, "title");
                    break;
                case "TITLE_DESC":
                    sort = Sort.by(Sort.Direction.DESC, "title");
                    break;
                case "CREATED_AT_DESC":
                    sort = Sort.by(Sort.Direction.DESC, "createdAt");
                    break;
                case "PARTICIPANTS_DESC":
                    sort = Sort.by(Sort.Direction.DESC, "confirmedParticipantsCount");
                    break;
                default:
                    sort = Sort.by(Sort.Direction.DESC, "startDate");
            }
        }

        return PageRequest.of(page, size, sort);
    }

    // ✅ LOGIN I REGISTER ENDPOINTY (jeśli nie masz)
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

    // ✅ PROFILE ENDPOINT

//    @GetMapping("/profile")
//    public String profile(@AuthenticationPrincipal CustomUserDetails userDetails,
//                          Model model) {
//        if (userDetails == null) {
//            return "redirect:/login";
//        }
//
//        model.addAttribute("user", userDetails);
//        return "user/profile";
//    }
}
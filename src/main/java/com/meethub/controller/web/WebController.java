package com.meethub.controller.web;

import com.meethub.domain.model.entity.*;
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
import com.meethub.domain.repository.jpa.MeetingRepository;
import com.meethub.domain.repository.jpa.UserRepository;
import com.meethub.domain.repository.specification.MeetingSpecification;
import com.meethub.domain.service.*;
import com.meethub.domain.service.impl.AttendanceTokenServiceImpl;
import com.meethub.exception.BusinessException;
import com.meethub.security.CustomUserDetailsService.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.web.util.UriComponentsBuilder;

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

    private final LocationService locationService;

    private final MeetingMarkService meetingMarkService;
    private final AttendanceTokenService attendanceTokenService;

    @GetMapping("/")
    @Operation(summary = "Strona główna lub panel użytkownika",
            description = "Zwraca panel użytkownika dla zalogowanych użytkowników, publiczną stronę główną z nadchodzącymi spotkaniami dla gości")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Strona HTML (panel lub strona główna)"),
            @ApiResponse(responseCode = "302", description = "Przekierowanie do logowania jeśli wymagana autentykacja")
    })    public String home(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
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
    @Operation(summary = "Lista spotkań z filtrowaniem i paginacją",
            description = "Dla zalogowanych użytkowników: zwraca filtrowane spotkania z informacją o uczestnictwie. Dla gości: zwraca tylko publiczne spotkania. Obsługuje paginację, wyszukiwanie po tekście, typie i statusie.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Strona z listą spotkań"),
            @ApiResponse(responseCode = "302", description = "Przekierowanie do logowania jeśli wymagana autentykacja")
    })
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

    String determineUserRole(boolean isOrganizer, boolean isConfirmed,
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
    @Operation(summary = "Panel użytkownika",
            description = "Zwraca panel użytkownika z ostatnimi spotkaniami. Wymaga autentykacji.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Strona panelu użytkownika"),
            @ApiResponse(responseCode = "302", description = "Przekierowanie do logowania jeśli nie zalogowany")
    })
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
    @Operation(summary = "Szczegóły spotkania",
            description = "Zwraca szczegółowe informacje o spotkaniu wraz z informacjami o uczestnictwie, zasobach, głosowaniach i statystykach.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Strona ze szczegółami spotkania"),
            @ApiResponse(responseCode = "404", description = "Spotkanie nie znalezione"),
            @ApiResponse(responseCode = "302", description = "Przekierowanie do listy spotkań w przypadku błędu")
    })
    public String meetingDetails(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        log.info("=== START meetingDetails ===");
        log.info("Meeting ID: {}", id);
        if (userDetails != null) {
            log.info("User ID: {}, username: {}", userDetails.getId(), userDetails.getUsername());
        }

        try {
            // Pobranie szczegółów spotkania
            MeetingResponse meeting = meetingService.getMeetingById(id);
            log.info("Meeting fetched: {}", meeting);
            model.addAttribute("meeting", meeting);

            Long userId = userDetails != null ? userDetails.getId() : null;

            // ============= WAŻNE SPOTKANIA - DODANE =============
            boolean isImportant = false;
            if (userId != null) {
                try {
                    isImportant = meetingMarkService.isMeetingImportantForUser(userId, id);
                    log.info("Meeting importance for user {}: {}", userId, isImportant);
                } catch (Exception e) {
                    log.warn("Error checking meeting importance: {}", e.getMessage());
                }
            }
            model.addAttribute("isImportant", isImportant);
            // ============= KONIEC DODAWANIA =============

            // Sprawdzenie czy użytkownik jest adminem
            boolean isAdmin = userDetails != null && userDetails.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
            model.addAttribute("isAdmin", isAdmin);
            log.info("User isAdmin: {}", isAdmin);

            // Uczestnictwo użytkownika
            MeetingParticipationInfo participationInfo = null;
            if (userId != null) {
                try {
                    participationInfo = meetingAuthorizationService.getUserMeetingPermissions(id, userId);
                    log.info("Participation info: {}", participationInfo);
                } catch (Exception e) {
                    log.error("Error loading participation info: {}", e.getMessage(), e);
                }
            }

            // Potwierdzeni uczestnicy
            try {
                List<ParticipantResponse> participants = meetingParticipantService.getConfirmedParticipants(id);
                model.addAttribute("participants", participants);
                log.info("Confirmed participants: {}", participants);
            } catch (Exception e) {
                model.addAttribute("participants", Collections.emptyList());
                log.error("Error fetching confirmed participants: {}", e.getMessage(), e);
            }

            // Statystyki uczestników
            try {
                Map<String, Long> participantStats = meetingParticipantService.getParticipantStatistics(id);
                model.addAttribute("participantStats", participantStats);
                log.info("Participant stats: {}", participantStats);
            } catch (Exception e) {
                model.addAttribute("participantStats", new HashMap<>());
                log.error("Error fetching participant stats: {}", e.getMessage(), e);
            }

            // Status użytkownika
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

                    log.info("User status - Pending: {}, Declined: {}, Invited: {}, Confirmed: {}",
                            isPending, isDeclined, isInvited, isConfirmed);
                } catch (Exception e) {
                    log.error("Error checking user status: {}", e.getMessage(), e);
                }
            }

            // Uprawnienia użytkownika
            if (participationInfo != null) {
                model.addAttribute("isOrganizer", participationInfo.isOrganizer());
                model.addAttribute("isParticipant", participationInfo.isParticipant());
                model.addAttribute("isRelated", participationInfo.isRelated());
                model.addAttribute("participantRole", participationInfo.getParticipantRole());
                model.addAttribute("canEdit", participationInfo.isCanEdit());
                model.addAttribute("canDelete", participationInfo.isCanDelete());
                model.addAttribute("canManageParticipants", participationInfo.isCanManageParticipants());
                model.addAttribute("canJoin", participationInfo.isCanJoin());

                log.info("Participation flags added to model: {}", participationInfo);
            } else {
                boolean canJoin = meeting != null && meeting.getVisibility() != null &&
                        meeting.getVisibility().name().equals("PUBLIC");
                model.addAttribute("isOrganizer", false);
                model.addAttribute("isParticipant", false);
                model.addAttribute("isRelated", false);
                model.addAttribute("canJoin", canJoin);
                log.info("Default participation flags applied, canJoin: {}", canJoin);
            }

            // Statystyki spotkania dla organizatora/admina
            if (userId != null && participationInfo != null && (participationInfo.isOrganizer() || isAdmin)) {
                try {
                    Optional<MeetingStatistics> statsOpt = meetingAnalyticsService.getMeetingStatistics(id);
                    model.addAttribute("meetingStatistics", statsOpt.orElse(null));
                    log.info("Meeting statistics: {}", statsOpt.orElse(null));
                } catch (Exception e) {
                    model.addAttribute("meetingStatistics", null);
                    log.error("Error fetching meeting statistics: {}", e.getMessage(), e);
                }
            } else {
                model.addAttribute("meetingStatistics", null);
            }

            // Głosowania
            if (userId != null) {
                try {
                    List<VotingResponse> allVotings = meetingVotingService.getMeetingVotings(id, userId);
                    Map<Boolean, List<VotingResponse>> votings = allVotings.stream()
                            .collect(Collectors.partitioningBy(v -> v.getStatus().name().equals("ACTIVE")));

                    model.addAttribute("activeVotings", votings.get(true));
                    model.addAttribute("closedVotings", votings.get(false));

                    log.info("Voting info - Active: {}, Closed: {}", votings.get(true), votings.get(false));
                } catch (Exception e) {
                    model.addAttribute("activeVotings", Collections.emptyList());
                    model.addAttribute("closedVotings", Collections.emptyList());
                    log.error("Error fetching voting info: {}", e.getMessage(), e);
                }
            }

            // Feedback użytkownika
            if (userId != null && participationInfo != null && participationInfo.isParticipant()) {
                try {
                    Feedback userFeedback = feedbackService.getUserFeedback(id, userId);
                    model.addAttribute("userFeedback", userFeedback);
                    log.info("User feedback: {}", userFeedback);
                } catch (Exception e) {
                    model.addAttribute("userFeedback", null);
                    log.error("Error fetching user feedback: {}", e.getMessage(), e);
                }
            }

            // Zasoby spotkania
            if (userId != null) {
                try {
                    List<MeetingResourceResponse> resources = resourceService.getMeetingResources(id, userId);
                    model.addAttribute("resources", resources);
                    model.addAttribute("resourcesCount", resources.size());
                    log.info("Meeting resources count: {}, resources: {}", resources.size(), resources);
                } catch (Exception e) {
                    model.addAttribute("resources", Collections.emptyList());
                    model.addAttribute("resourcesCount", 0);
                    log.error("Error fetching meeting resources: {}", e.getMessage(), e);
                }
            }

            // ============= DODATKOWO: LISTA WAŻNYCH SPOTKAŃ =============
            if (userId != null) {
                try {
                    List<Long> importantMeetingIds = meetingMarkService.getImportantMeetingIdsForUser(userId);
                    model.addAttribute("importantMeetingIds", importantMeetingIds);
                    model.addAttribute("hasImportantMeetings", !importantMeetingIds.isEmpty());
                    log.info("User {} has {} important meetings", userId, importantMeetingIds.size());
                } catch (Exception e) {
                    model.addAttribute("importantMeetingIds", Collections.emptyList());
                    model.addAttribute("hasImportantMeetings", false);
                    log.warn("Error fetching important meetings list: {}", e.getMessage());
                }
            } else {
                model.addAttribute("importantMeetingIds", Collections.emptyList());
                model.addAttribute("hasImportantMeetings", false);
            }
            // ============= KONIEC DODAWANIA =============

            // Dane użytkownika w modelu
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


    @GetMapping("/meetings/create")
    @Operation(summary = "Formularz tworzenia spotkania",
            description = "Wyświetla formularz do tworzenia nowego spotkania. Wymaga autentykacji.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Formularz tworzenia spotkania"),
            @ApiResponse(responseCode = "302", description = "Przekierowanie do logowania jeśli nie zalogowany")
    })
    public String showCreateMeetingForm(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        List<LocationBasicInfo> locations = locationService.getLocationsForSelect();

        model.addAttribute("user", userDetails);
        model.addAttribute("createMeetingRequest", new CreateMeetingRequest());
        model.addAttribute("locations", locations); // ✅ Dodaj lokalizacje do modelu

        return "meetings/create";
    }

    @PostMapping("/meetings/create")
    @Operation(summary = "Utwórz nowe spotkanie",
            description = "Tworzy nowe spotkanie na podstawie danych z formularza. Wymaga autentykacji.")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Przekierowanie do szczegółów spotkania po utworzeniu"),
            @ApiResponse(responseCode = "400", description = "Błędy walidacji formularza"),
            @ApiResponse(responseCode = "302", description = "Przekierowanie do formularza w przypadku błędu")
    })
    public String createMeeting(
            @Valid @ModelAttribute("createMeetingRequest") CreateMeetingRequest request,
            BindingResult result,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        if (request.getType() != MeetingType.ONLINE && request.getLocationId() == null) {
            result.rejectValue("locationId", "NotNull",
                    "Lokalizacja jest wymagana dla spotkań osobiście lub hybrydowych");
        }

        if (request.getLocationId() != null) {
            try {
                locationService.validateLocationExists(request.getLocationId());
            } catch (IllegalArgumentException e) {
                result.rejectValue("locationId", "Invalid", e.getMessage());
            }
        }

        if (result.hasErrors()) {
            // Ponownie załaduj listę lokalizacji w przypadku błędów walidacji
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
            // Ponownie załaduj listę lokalizacji w przypadku błędu serwisu
            List<LocationBasicInfo> locations = locationService.getLocationsForSelect();
            model.addAttribute("locations", locations);
            model.addAttribute("user", userDetails);
            model.addAttribute("error", "Błąd podczas tworzenia spotkania: " + e.getMessage());
            return "meetings/create";
        }
    }

    @GetMapping("/api/users/search")
    @ResponseBody
    @Operation(summary = "Wyszukiwanie użytkowników",
            description = "Wyszukuje użytkowników na podstawie zapytania tekstowego. Wymaga autentykacji.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista użytkowników w formacie JSON"),
            @ApiResponse(responseCode = "401", description = "Nieautoryzowany dostęp"),
            @ApiResponse(responseCode = "500", description = "Błąd serwera")
    })
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
    @Operation(summary = "Formularz edycji spotkania",
            description = "Wyświetla formularz do edycji istniejącego spotkania. Tylko organizator może edytować.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Formularz edycji spotkania"),
            @ApiResponse(responseCode = "302", description = "Przekierowanie jeśli brak uprawnień"),
            @ApiResponse(responseCode = "404", description = "Spotkanie nie znalezione")
    })
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
            model.addAttribute("meetingStatuses", MeetingStatus.values());

            return "meetings/edit";

        } catch (Exception e) {
            return "redirect:/meetings?error=Spotkanie nie zostało znalezione";
        }
    }

    @PostMapping("/meetings/{id}/edit")
    @Operation(summary = "Aktualizuj spotkanie",
            description = "Aktualizuje istniejące spotkanie. Tylko organizator może aktualizować.")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Przekierowanie do szczegółów spotkania po aktualizacji"),
            @ApiResponse(responseCode = "400", description = "Błędy walidacji formularza"),
            @ApiResponse(responseCode = "403", description = "Brak uprawnień do edycji")
    })
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

        // Log wartości przekazanych do aktualizacji
        log.info("Updating meeting id={} by user id={}", id, userDetails.getId());
        log.info("UpdateMeetingRequest: title='{}', description='{}', type={}, visibility={}, startDate={}, endDate={}, status={}",
                request.getTitle(), request.getDescription(), request.getType(), request.getVisibility(),
                request.getStartDate(), request.getEndDate(), request.getStatus());

        // WALIDACJA LOKALIZACJI
        if (request.getType() != null && request.getType() != MeetingType.ONLINE &&
                request.getLocationId() == null) {
            result.rejectValue("locationId", "NotNull",
                    "Lokalizacja jest wymagana dla spotkań osobiście lub hybrydowych");
        }

        if (request.getLocationId() != null) {
            try {
                locationService.validateLocationExists(request.getLocationId());
            } catch (IllegalArgumentException e) {
                result.rejectValue("locationId", "Invalid", e.getMessage());
            }
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
    @Operation(summary = "Usuń spotkanie",
            description = "Usuwa istniejące spotkanie. Tylko organizator może usunąć.")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Przekierowanie do listy spotkań po usunięciu"),
            @ApiResponse(responseCode = "403", description = "Brak uprawnień do usunięcia"),
            @ApiResponse(responseCode = "404", description = "Spotkanie nie znalezione")
    })
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
    @Operation(summary = "Duplikuj spotkanie",
            description = "Tworzy kopię istniejącego spotkania. Wymaga autentykacji.")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Przekierowanie do nowo utworzonej kopii"),
            @ApiResponse(responseCode = "404", description = "Spotkanie nie znalezione")
    })
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
    @Operation(summary = "Dołącz do spotkania",
            description = "Użytkownik dołącza do spotkania. Tylko dla publicznych/private spotkań.")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Przekierowanie do szczegółów spotkania"),
            @ApiResponse(responseCode = "403", description = "Brak możliwości dołączenia"),
            @ApiResponse(responseCode = "409", description = "Brak wolnych miejsc")
    })
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
    @Operation(summary = "Opuść spotkanie",
            description = "Użytkownik opuszcza spotkanie. Tylko dla uczestników.")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Przekierowanie do szczegółów spotkania"),
            @ApiResponse(responseCode = "403", description = "Nie jesteś uczestnikiem")
    })
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

    @GetMapping("/meetings/templates")
    @Operation(summary = "Lista szablonów spotkań",
            description = "Wyświetla listę szablonów spotkań użytkownika. Wymaga autentykacji.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Strona z listą szablonów"),
            @ApiResponse(responseCode = "302", description = "Przekierowanie do logowania")
    })
    public String templates(@AuthenticationPrincipal CustomUserDetails userDetails,
                            Model model) {
        if (userDetails == null) return "redirect:/login";

        log.info(" Loading templates for user: {}", userDetails.getId());

        try {
            List<MeetingResponse> templates = meetingService.getMeetingTemplates(userDetails.getId());
            log.info(" Raw templates count: {}", templates.size());

            // Filtruj null wartości
            List<MeetingResponse> validTemplates = templates.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            log.info(" Valid templates count: {}", validTemplates.size());
            log.info("tempLOGIN {}", validTemplates);

            model.addAttribute("templates", validTemplates);
            model.addAttribute("user", userDetails);

            return "meetings/templates";

        } catch (Exception e) {
            log.error(" Error loading templates: {}", e.getMessage(), e);
            model.addAttribute("error", "Błąd podczas ładowania szablonów: " + e.getMessage());
            return "redirect:/meetings";
        }
    }

    @GetMapping("/meetings/create-from-template/{templateId}")
    @Operation(summary = "Tworzenie spotkania z szablonu",
            description = "Wyświetla formularz wypełniony danymi z szablonu. Wymaga autentykacji.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Formularz tworzenia spotkania z danymi szablonu"),
            @ApiResponse(responseCode = "404", description = "Szablon nie znaleziony"),
            @ApiResponse(responseCode = "400", description = "To nie jest szablon")
    })
    public String createFromTemplate(@PathVariable Long templateId,
                                     @AuthenticationPrincipal CustomUserDetails userDetails,
                                     Model model) {
        if (userDetails == null) return "redirect:/login";

        try {
            MeetingResponse template = meetingService.getMeetingById(templateId);

            if (!template.isTemplate()) {
                throw new BusinessException("To nie jest szablon");
            }

            // STWÓRZ CreateMeetingRequest WYPEŁNIONY WARTOŚCIAMI Z SZABLONU
            CreateMeetingRequest request = CreateMeetingRequest.builder()
                    .title(template.getTitle() + " (z szablonu)")
                    .description(template.getDescription())
                    .agenda(template.getAgenda())
                    .type(template.getType())
                    .visibility(template.getVisibility())
                    .maxParticipants(template.getMaxParticipants())
                    .tags(template.getTags())
                    .startDate(getNextOccurrenceDate(template))
                    .endDate(getNextOccurrenceEndDate(template))
                    .build();

            model.addAttribute("createMeetingRequest", request);
            model.addAttribute("user", userDetails);
            model.addAttribute("locations", locationService.getLocationsForSelect());
            model.addAttribute("fromTemplate", true);
            model.addAttribute("templateName", template.getTitle());
            model.addAttribute("templateId", templateId);

            return "meetings/create";

        } catch (Exception e) {
            log.error("Error loading template {}: {}", templateId, e.getMessage());
            return "redirect:/meetings/templates?error=" + e.getMessage();
        }
    }


    private LocalDateTime getNextOccurrenceDate(MeetingResponse template) {
        if (template.getStartDate() == null) {
            return LocalDateTime.now().plusWeeks(1).withHour(10).withMinute(0).withSecond(0);
        }

        LocalDateTime templateDateTime = template.getStartDate();
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime nextDate = templateDateTime;

        while (nextDate.isBefore(now)) {
            nextDate = nextDate.plusWeeks(1);
        }

        return nextDate;
    }

    private LocalDateTime getNextOccurrenceEndDate(MeetingResponse template) {
        LocalDateTime startDate = getNextOccurrenceDate(template);

        if (template.getStartDate() != null && template.getEndDate() != null) {
            long durationMinutes = Duration.between(
                    template.getStartDate(),
                    template.getEndDate()
            ).toMinutes();
            return startDate.plusMinutes(durationMinutes);
        }

        // Domyślnie: startDate + 1 godzina
        return startDate.plusHours(1);
    }


    @PostMapping("/meetings/{id}/attend")
    @Operation(summary = "Potwierdź obecność na spotkaniu",
            description = "Użytkownik potwierdza obecność na spotkaniu za pomocą tokenu")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Przekierowanie do szczegółów spotkania"),
            @ApiResponse(responseCode = "400", description = "Nieprawidłowy token"),
            @ApiResponse(responseCode = "404", description = "Spotkanie nie znalezione")
    })
    public String attendMeeting(
            @PathVariable Long id,
            @RequestParam @NotBlank(message = "Token jest wymagany") String token,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        Long userId = userDetails.getId();

        boolean tokenValid = attendanceTokenService.validateAndUseToken(token, id);
        if (!tokenValid) {
            redirectAttributes.addFlashAttribute("error", "Nieprawidłowy lub wygasły token");
            return "redirect:/meetings/" + id;
        }

        try {
            meetingParticipantService.markAsAttended(id, userId);
            redirectAttributes.addFlashAttribute("success", "Twoja obecność została odnotowana");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Błąd podczas oznaczania obecności: " + e.getMessage());
        }

        return "redirect:/meetings/" + id;
    }


    @GetMapping("/meetings/{id}/attend")
    @Operation(summary = "Potwierdź obecność na spotkaniu przez link",
            description = "Użytkownik potwierdza obecność na spotkaniu za pomocą tokenu bez logowania")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Przekierowanie do szczegółów spotkania"),
            @ApiResponse(responseCode = "400", description = "Nieprawidłowy token"),
            @ApiResponse(responseCode = "404", description = "Spotkanie nie znalezione")
    })
    public String attendMeetingViaLink(
            @PathVariable Long id,
            @RequestParam @NotBlank(message = "Token jest wymagany") String token,
            RedirectAttributes redirectAttributes) {

        Optional<Long> optionalUserId = attendanceTokenService.getUserIdFromToken(token, id);
        if (optionalUserId.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Nieprawidłowy lub wygasły token");
            return "redirect:/meetings/" + id;
        }

        Long userId = optionalUserId.get();

        boolean tokenValid = attendanceTokenService.validateAndUseToken(token, id);
        if (!tokenValid) {
            redirectAttributes.addFlashAttribute("error", "Nieprawidłowy lub wygasły token");
            return "redirect:/meetings/" + id;
        }

        try {
            meetingParticipantService.markAsAttended(id, userId);
            redirectAttributes.addFlashAttribute("success", "Twoja obecność została odnotowana");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Błąd podczas oznaczania obecności: " + e.getMessage());
        }

        return "redirect:/meetings/" + id;
    }




    @PostMapping("/meetings/create-from-template/{templateId}")
    @Operation(summary = "Utwórz spotkanie z szablonu",
            description = "Tworzy nowe spotkanie na podstawie szablonu. Wymaga autentykacji.")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Przekierowanie do szczegółów nowego spotkania"),
            @ApiResponse(responseCode = "400", description = "Błędy walidacji formularza"),
            @ApiResponse(responseCode = "404", description = "Szablon nie znaleziony")
    })
    public String createFromTemplatePost(
            @PathVariable Long templateId,
            @Valid @ModelAttribute("createMeetingRequest") CreateMeetingRequest request,
            BindingResult result,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (userDetails == null) return "redirect:/login";

        if (request.getType() != MeetingType.ONLINE && request.getLocationId() == null) {
            result.rejectValue("locationId", "NotNull",
                    "Lokalizacja jest wymagana dla spotkań osobiście lub hybrydowych");
        }

        if (request.getLocationId() != null) {
            try {
                locationService.validateLocationExists(request.getLocationId());
            } catch (IllegalArgumentException e) {
                result.rejectValue("locationId", "Invalid", e.getMessage());
            }
        }

        if (result.hasErrors()) {
            List<LocationBasicInfo> locations = locationService.getLocationsForSelect();
            model.addAttribute("locations", locations);
            model.addAttribute("user", userDetails);
            model.addAttribute("fromTemplate", true);
            model.addAttribute("templateId", templateId);
            return "meetings/create";
        }

        try {
            MeetingResponse meeting = meetingService.createFromTemplate(templateId,
                    userDetails.getId(),
                    request.getStartDate());
            redirectAttributes.addFlashAttribute("message",
                    "Spotkanie utworzone z szablonu pomyślnie!");
            return "redirect:/meetings/" + meeting.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Błąd: " + e.getMessage());
            return "redirect:/meetings/templates";
        }
    }

    @PostMapping("/meetings/{id}/save-as-template")
    @Operation(summary = "Zapisz spotkanie jako szablon",
            description = "Tworzy szablon na podstawie istniejącego spotkania. Wymaga autentykacji.")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Przekierowanie do listy szablonów"),
            @ApiResponse(responseCode = "400", description = "Nieprawidłowa nazwa szablonu"),
            @ApiResponse(responseCode = "404", description = "Spotkanie nie znalezione"),
            @ApiResponse(responseCode = "403", description = "Brak uprawnień")
    })
    public String saveAsTemplate(
            @PathVariable Long id,
            @RequestParam @NotBlank(message = "Nazwa szablonu jest wymagana")
            @Size(min = 3, max = 100, message = "Nazwa szablonu musi mieć od 3 do 100 znaków")
            String templateName,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        if (userDetails == null) {
            return "redirect:/login";
        }

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


    @GetMapping("/meetings/{id}/recurrence")
    @Operation(summary = "Szczegóły cyklu powtarzania",
            description = "Wyświetla szczegóły cyklu powtarzania dla spotkań cyklicznych.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Strona ze szczegółami cyklu"),
            @ApiResponse(responseCode = "404", description = "Spotkanie nie znalezione")
    })
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
    @Operation(summary = "Generuj następne wystąpienia",
            description = "Generuje kolejne wystąpienia dla spotkań cyklicznych.")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Przekierowanie z powrotem do szczegółów cyklu"),
            @ApiResponse(responseCode = "400", description = "Nieprawidłowa liczba wystąpień"),
            @ApiResponse(responseCode = "400", description = "Spotkanie nie jest cykliczne")
    })
    public String generateOccurrences(
            @PathVariable Long id,
            @RequestParam(defaultValue = "5")
            @Min(value = 1, message = "Liczba wystąpień musi być co najmniej 1")
            @Max(value = 50, message = "Liczba wystąpień nie może przekraczać 50")
            int count,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        if (userDetails == null) {
            return "redirect:/login";
        }

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
    @Operation(summary = "Dodaj wyjątek cyklu",
            description = "Dodaje wyjątek dla spotkania cyklicznego (np. odwołanie konkretnego terminu).")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Przekierowanie z powrotem do szczegółów cyklu"),
            @ApiResponse(responseCode = "400", description = "Nieprawidłowa data wyjątku")
    })
    public String addRecurrenceException(
            @PathVariable Long id,
            @RequestParam @NotBlank(message = "Data wyjątku jest wymagana") String exceptionDate,
            @RequestParam(required = false)
            @Size(max = 500, message = "Powód nie może przekraczać 500 znaków")
            String reason,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            meetingService.addRecurrenceException(id, exceptionDate, reason);
            redirectAttributes.addFlashAttribute("message",
                    "Dodano wyjątek: " + exceptionDate);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Błąd: " + e.getMessage());
        }

        return "redirect:/meetings/" + id + "/recurrence";
    }


    @GetMapping("/categories/create")
    @Operation(summary = "Formularz tworzenia kategorii",
            description = "Wyświetla formularz do tworzenia nowej kategorii. Wymaga autentykacji.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Formularz tworzenia kategorii"),
            @ApiResponse(responseCode = "302", description = "Przekierowanie do logowania")
    })
    public String showCreateCategoryForm(@AuthenticationPrincipal CustomUserDetails userDetails,
                                         Model model) {
        if (userDetails == null) return "redirect:/login";

        model.addAttribute("category", new Category());
        model.addAttribute("user", userDetails);

        return "categories/create";
    }

    @GetMapping("/meetings/{id}/status-history")
    @Operation(summary = "Historia zmian statusu",
            description = "Wyświetla historię zmian statusu dla spotkania.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Strona z historią zmian"),
            @ApiResponse(responseCode = "404", description = "Spotkanie nie znalezione")
    })
    public String statusHistory(@PathVariable Long id,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                Model model) {
        MeetingResponse meeting = meetingService.getMeetingById(id);
        model.addAttribute("meeting", meeting);
        model.addAttribute("user", userDetails);

        return "meetings/status-history";
    }

    @PostMapping("/meetings/{id}/change-status")
    @Operation(summary = "Zmień status spotkania",
            description = "Zmienia status spotkania. Tylko organizator może zmieniać status.")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Przekierowanie do szczegółów spotkania"),
            @ApiResponse(responseCode = "400", description = "Nieprawidłowy status"),
            @ApiResponse(responseCode = "403", description = "Brak uprawnień do zmiany statusu")
    })
    public String changeStatus(
            @PathVariable Long id,
            @RequestParam @NotBlank(message = "Status jest wymagany") String status,
            @RequestParam(required = false)
            @Size(max = 500, message = "Powód zmiany statusu nie może przekraczać 500 znaków")
            String reason,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        if (userDetails == null) {
            return "redirect:/login";
        }

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


    @GetMapping("/meetings/tag/{tag}")
    @Operation(summary = "Spotkania według tagu",
            description = "Wyświetla spotkania oznaczone określonym tagiem.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Strona z spotkaniami z tagiem"),
            @ApiResponse(responseCode = "404", description = "Brak spotkań z tym tagiem")
    })
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


    @GetMapping("/meetings/recurring")
    @Operation(summary = "Cykliczne spotkania",
            description = "Wyświetla nadchodzące cykliczne spotkania użytkownika.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Strona z cyklicznymi spotkaniami"),
            @ApiResponse(responseCode = "302", description = "Przekierowanie do logowania")
    })
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


    @GetMapping("/meetings/search")
    @Operation(summary = "Wyszukiwanie spotkań",
            description = "Zaawansowane wyszukiwanie spotkań z wieloma parametrami filtrowania, sortowaniem i paginacją.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Strona z wynikami wyszukiwania"),
            @ApiResponse(responseCode = "302", description = "Przekierowanie do logowania"),
            @ApiResponse(responseCode = "400", description = "Nieprawidłowe parametry wyszukiwania")
    })
    public String searchMeetings(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String keywords,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) List<String> searchFields,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(required = false, defaultValue = "0")
            @Min(value = 0, message = "Minimalna liczba uczestników nie może być ujemna")
            Integer minParticipants,
            @RequestParam(required = false, defaultValue = "100")
            @Min(value = 1, message = "Maksymalna liczba uczestników musi być co najmniej 1")
            Integer maxParticipants,
            @RequestParam(required = false) String organizerName,
            @RequestParam(required = false) String myParticipation,
            @RequestParam(required = false) String visibility,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(required = false) Boolean recurring,
            @RequestParam(required = false) Boolean recurringOnly,
            @RequestParam(required = false) Boolean template,
            @RequestParam(required = false) Boolean templatesOnly,
            @RequestParam(required = false) Boolean hasAttachments,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12")
            @Min(value = 1, message = "Rozmiar strony musi być co najmniej 1")
            @Max(value = 100, message = "Rozmiar strony nie może przekraczać 100")
            int size,
            Model model) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        log.info("🔍 ===== SEARCH PARAMETERS START =====");
        log.info("📥 Raw parameters received:");
        log.info("  search: {}", search);
        log.info("  keywords: {}", keywords);
        log.info("  status: {}", status);
        log.info("  statuses: {}", statuses);
        log.info("  type: {}", type);
        log.info("🔍 ===== SEARCH PARAMETERS END =====");

        try {
            String finalKeywords = search != null ? search : keywords;
            Boolean finalRecurring = recurring != null ? recurring : recurringOnly;
            Boolean finalTemplate = template != null ? template : templatesOnly;
            String finalSortBy = sortBy != null ? sortBy : "startDate";
            String finalSortOrder = sortOrder != null ? sortOrder : "desc";
            List<String> finalStatuses = statuses != null ? statuses :
                    (status != null ? List.of(status) : new ArrayList<>());

            MeetingType meetingType = null;
            if (type != null && !type.isBlank()) {
                try {
                    meetingType = MeetingType.valueOf(type.trim().toUpperCase());
                    log.info("MEtyp w kontrolerze: {}", meetingType);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid meeting type: {}", type);
                }
            }

            MeetingVisibility meetingVisibility = null;
            if (visibility != null && !visibility.isEmpty()) {
                try {
                    meetingVisibility = MeetingVisibility.valueOf(visibility);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid visibility: {}", visibility);
                }
            }

            SearchCriteria criteria = SearchCriteria.builder()
                    .keywords(finalKeywords)
                    .tags(tags != null ? String.join(",", tags) : null)
                    .searchFields(searchFields != null ? searchFields : List.of("TITLE", "DESCRIPTION"))
                    .dateFrom(dateFrom)
                    .dateTo(dateTo)
                    .type(meetingType)
                    .statuses(finalStatuses)
                    .minParticipants(minParticipants)
                    .maxParticipants(maxParticipants)
                    .organizerName(organizerName)
                    .myParticipation(myParticipation)
                    .visibility(meetingVisibility)
                    .recurringOnly(Boolean.TRUE.equals(finalRecurring))
                    .templatesOnly(Boolean.TRUE.equals(finalTemplate))
                    .hasAttachments(Boolean.TRUE.equals(hasAttachments))
                    .currentUserId(userDetails.getId())
                    .userAuthenticated(true)
                    .includePublic(true)
                    .build();

            Sort sort = Sort.by(
                    "asc".equalsIgnoreCase(finalSortOrder) ?
                            Sort.Direction.ASC : Sort.Direction.DESC,
                    getSortField(finalSortBy)
            );
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<MeetingResponse> meetingsPage = meetingService.searchMeetings(criteria, pageable);

            model.addAttribute("meetings", meetingsPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", meetingsPage.getTotalPages());
            model.addAttribute("totalItems", meetingsPage.getTotalElements());
            model.addAttribute("pageSize", size);
            model.addAttribute("user", userDetails);
            model.addAttribute("isSearchResults", true);
            model.addAttribute("resultsCount", meetingsPage.getTotalElements());

            model.addAttribute("search", finalKeywords);
            model.addAttribute("keywords", finalKeywords);
            model.addAttribute("tags", tags);
            model.addAttribute("dateFrom", dateFrom);
            model.addAttribute("dateTo", dateTo);
            model.addAttribute("type", type);
            model.addAttribute("status", status);
            model.addAttribute("statuses", finalStatuses);
            model.addAttribute("organizerName", organizerName);
            model.addAttribute("myParticipation", myParticipation);
            model.addAttribute("visibility", visibility);
            model.addAttribute("sortBy", finalSortBy);
            model.addAttribute("sortOrder", finalSortOrder);
            model.addAttribute("recurring", finalRecurring);
            model.addAttribute("template", finalTemplate);
            model.addAttribute("hasAttachments", hasAttachments);

            model.addAttribute("searchParams", buildSearchParams(
                    finalKeywords, tags, dateFrom, dateTo, type, finalStatuses,
                    minParticipants, maxParticipants, organizerName, myParticipation,
                    visibility, finalSortBy, finalSortOrder,
                    finalRecurring, finalTemplate, hasAttachments
            ));

            return "meetings/advanced-search";

        } catch (Exception e) {
            log.error("Error in search: {}", e.getMessage(), e);
            model.addAttribute("user", userDetails);
            model.addAttribute("error", "Błąd podczas wyszukiwania: " + e.getMessage());
            model.addAttribute("meetings", Collections.emptyList());
            return "meetings/advanced-search";
        }
    }

    private String getSortField(String sortBy) {
        if (sortBy == null || sortBy.isEmpty()) return "startDate";
        switch (sortBy.toLowerCase()) {
            case "title": return "title";
            case "created": return "createdAt";
            case "updated": return "updatedAt";
            case "date": return "startDate";
            default: return "startDate";
        }
    }

    private Map<String, String> buildSearchParams(
            String keywords, List<String> tags, LocalDate dateFrom, LocalDate dateTo,
            String type, List<String> statuses, Integer minParticipants, Integer maxParticipants,
            String organizerName, String myParticipation, String visibility,
            String sortBy, String sortOrder,
            Boolean recurring, Boolean template, Boolean hasAttachments) {

        Map<String, String> params = new LinkedHashMap<>();
        if (keywords != null) params.put("keywords", keywords);
        if (tags != null && !tags.isEmpty()) params.put("tags", String.join(",", tags));
        if (dateFrom != null) params.put("dateFrom", dateFrom.toString());
        if (dateTo != null) params.put("dateTo", dateTo.toString());
        if (type != null) params.put("type", type);
        if (statuses != null && !statuses.isEmpty()) params.put("status", String.join(",", statuses));
        if (organizerName != null) params.put("organizerName", organizerName);
        if (myParticipation != null) params.put("myParticipation", myParticipation);
        if (visibility != null) params.put("visibility", visibility);
        if (sortBy != null) params.put("sortBy", sortBy);
        if (sortOrder != null) params.put("sortOrder", sortOrder);
        if (recurring != null) params.put("recurring", recurring.toString());
        if (template != null) params.put("template", template.toString());
        if (hasAttachments != null) params.put("hasAttachments", hasAttachments.toString());

        return params;
    }
    @ModelAttribute("buildPaginationLink")
    public Function<Integer, String> buildPaginationLink(
            @RequestParam(required = false) String keywords,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) List<Long> categoryIds,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) Boolean recurring,
            @RequestParam(required = false) Boolean recurringOnly,
            @RequestParam(required = false) Boolean template,
            @RequestParam(required = false) Boolean templatesOnly,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(defaultValue = "12") int size) {

        return (page) -> {
            UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/meetings/search")
                    .queryParam("page", page)
                    .queryParam("size", size);

            if (keywords != null) builder.queryParam("keywords", keywords);
            if (search != null) builder.queryParam("keywords", search);
            if (type != null) builder.queryParam("type", type);
            if (status != null) builder.queryParam("status", status);
            if (tags != null && !tags.isEmpty()) {
                builder.queryParam("tags", String.join(",", tags));
            }
            if (recurring != null) builder.queryParam("recurring", recurring);
            if (recurringOnly != null) builder.queryParam("recurring", recurringOnly);
            if (template != null) builder.queryParam("template", template);
            if (templatesOnly != null) builder.queryParam("template", templatesOnly);
            if (sortBy != null) builder.queryParam("sortBy", sortBy);
            if (sortOrder != null) builder.queryParam("sortOrder", sortOrder);

            return builder.build().toUriString();
        };
    }


    @PostMapping("/{meetingId}/join-with-token")
    @Operation(summary = "Dołącz do spotkania z tokenem",
            description = "Dołącza użytkownika do spotkania za pomocą tokenu uczestnictwa.")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Przekierowanie do szczegółów spotkania"),
            @ApiResponse(responseCode = "400", description = "Nieprawidłowy token"),
            @ApiResponse(responseCode = "404", description = "Spotkanie lub uczestnik nie znaleziony")
    })
    public String joinMeetingWithToken(
            @PathVariable Long meetingId,
            @RequestParam @NotNull(message = "ID uczestnika jest wymagane") Long participantId,
            @RequestParam @NotBlank(message = "Token jest wymagany") String token,
            RedirectAttributes redirectAttributes
    ) {
        try {
            meetingParticipantService.confirmAttendance(participantId, token);
            redirectAttributes.addFlashAttribute("success", "Dołączono pomyślnie!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Nieprawidłowy token!");
        }

        return "redirect:/meetings/" + meetingId;
    }

}
















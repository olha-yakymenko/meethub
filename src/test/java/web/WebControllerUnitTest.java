package com.meethub.controller.web;

import com.meethub.domain.model.entity.Category;
import com.meethub.domain.model.entity.MeetingStatistics;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.MeetingStatus;
import com.meethub.domain.model.enums.MeetingType;
import com.meethub.domain.model.enums.MeetingVisibility;
import com.meethub.domain.model.enums.UserRole;
import com.meethub.domain.model.projection.LocationBasicInfo;
import com.meethub.domain.model.request.CreateMeetingRequest;
import com.meethub.domain.model.request.SearchCriteria;
import com.meethub.domain.model.request.UpdateMeetingRequest;
import com.meethub.domain.model.response.*;
import com.meethub.domain.service.*;
import com.meethub.exception.BusinessException;
import com.meethub.security.CustomUserDetailsService;
import com.meethub.security.CustomUserDetailsService.CustomUserDetails;
import org.h2.engine.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebControllerUnitTest {

    @Mock
    private MeetingService meetingService;

    @Mock
    private MeetingParticipantService meetingParticipantService;

    @Mock
    private MeetingAuthorizationService meetingAuthorizationService;

    @Mock
    private UserService userService;

    @Mock
    private LocationService locationService;

    @Mock
    private MeetingMarkService meetingMarkService;

    @Mock
    private MeetingVotingService meetingVotingService;

    @Mock
    private FeedbackService feedbackService;

    @Mock
    private MeetingResourceService resourceService;

    @Mock
    private MeetingAnalyticsService meetingAnalyticsService;

    @Mock
    private AttendanceTokenService attendanceTokenService;

    @Mock
    private Model model;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private WebController webController;

    private CustomUserDetails userDetails;
    private CustomUserDetails adminUserDetails;

    @BeforeEach
    void setUp() {
        // Regular user
        User regularUser = new User();
        regularUser.setId(1L);
        regularUser.setEmail("testuser@example.com");
        regularUser.setRole(UserRole.PARTICIPANT);
        regularUser.setPassword("dummyPassword");
        regularUser.setEnabled(true);

        userDetails = new CustomUserDetailsService.CustomUserDetails(regularUser);

        // Admin user
        User adminUser = new User();
        adminUser.setId(2L);
        adminUser.setEmail("admin@example.com");
        adminUser.setRole(UserRole.ORGANIZER);
        adminUser.setPassword("dummyPassword");
        adminUser.setEnabled(true);

        adminUserDetails = new CustomUserDetailsService.CustomUserDetails(adminUser);
    }




    // ==================== TESTS FOR HOME ENDPOINT ====================


    @Test
    void home_shouldReturnIndexWithPublicMeetings_whenUserNotAuthenticated() {
        // Given
        List<MeetingResponse> publicMeetings = Arrays.asList(
                MeetingResponse.builder()
                        .id(1L)
                        .title("Public Meeting 1")
                        .build(),
                MeetingResponse.builder()
                        .id(2L)
                        .title("Public Meeting 2")
                        .build()
        );
        when(meetingService.getUpcomingPublicMeetings()).thenReturn(publicMeetings);

        // When
        String viewName = webController.home(null, model);

        // Then
        assertEquals("index", viewName);
        verify(model).addAttribute("upcomingMeetings", publicMeetings);
        verify(model).addAttribute("totalMeetings", 2);
    }

    @Test
    void home_shouldHandleException_whenLoadingPublicMeetingsFails() {
        // Given
        when(meetingService.getUpcomingPublicMeetings()).thenThrow(new RuntimeException("Database error"));

        // When
        String viewName = webController.home(null, model);

        // Then
        assertEquals("index", viewName);
        verify(model).addAttribute("upcomingMeetings", Collections.emptyList());
        verify(model).addAttribute("totalMeetings", 0);
    }

    // ==================== TESTS FOR MEETINGS LIST ENDPOINT ====================

    @Test
    void meetings_shouldReturnFilteredMeetings_whenUserAuthenticated() {
        // Given
        Pageable pageable = PageRequest.of(0, 3);
        Page<MeetingResponse> meetingsPage = new PageImpl<>(Arrays.asList(
                createMockMeetingResponse(1L),
                createMockMeetingResponse(2L)
        ), pageable, 2);

        when(meetingService.getFilteredMeetings(anyString(), anyString(), anyString(), any(Pageable.class)))
                .thenReturn(meetingsPage);
        when(meetingParticipantService.isConfirmedParticipant(anyLong(), anyLong())).thenReturn(true);

        // When
        String viewName = webController.meetings(userDetails, 0, 3, "search", "WORKSHOP", "ACTIVE", model);

        // Then
        assertEquals("meetings/list", viewName);
        verify(model).addAttribute(eq("meetings"), anyList());
        verify(model).addAttribute(eq("userId"), eq(1L));
        verify(model).addAttribute(eq("currentUserId"), eq(1L));

    }

    @Test
    void meetings_shouldReturnPublicMeetings_whenUserNotAuthenticated() {
        // Given
        List<MeetingResponse> publicMeetings = Arrays.asList(
                createMockMeetingResponse(1L),
                createMockMeetingResponse(2L)
        );
        when(meetingService.getUpcomingPublicMeetings()).thenReturn(publicMeetings);

        // When
        String viewName = webController.meetings(null, 0, 3, null, null, null, model);

        // Then
        assertEquals("meetings/list", viewName);
        verify(model).addAttribute("userId", null);
        verify(model).addAttribute("currentUserId", null);
        verify(model, atLeastOnce()).addAttribute(eq("meetings"), anyList());
    }

    @Test
    void meetings_shouldHandleExceptionGracefully() {
        // Given
        when(meetingService.getFilteredMeetings(anyString(), anyString(), anyString(), any(Pageable.class)))
                .thenThrow(new RuntimeException("Service error"));

        // When
        String viewName = webController.meetings(userDetails, 0, 3, null, null, null, model);

        // Then
        assertEquals("meetings/list", viewName);
        verify(model).addAttribute("meetings", Collections.emptyList());
        verify(model).addAttribute("warning", "Nie udało się załadować listy spotkań");
    }

    // ==================== TESTS FOR MEETING DETAILS ENDPOINT ====================



    @Test
    void meetingDetails_shouldHandleMeetingNotFound() {
        // Given
        Long meetingId = 999L;
        when(meetingService.getMeetingById(meetingId))
                .thenThrow(new RuntimeException("Meeting not found"));

        // When
        String viewName = webController.meetingDetails(meetingId, userDetails, model);

        // Then
        assertEquals("redirect:/meetings", viewName);
        verify(model).addAttribute("error", "Błąd podczas ładowania szczegółów spotkania");
    }

//    @Test
//    void meetingDetails_shouldShowStatisticsForOrganizer() {
//        // Given
//        Long meetingId = 1L;
//        MeetingResponse meeting = createMockMeetingResponse(meetingId);
//        MeetingParticipationInfo participationInfo = createMockParticipationInfo(true, false);
//        MeetingStatistics statistics = MeetingStatistics.builder()
//                .totalParticipants(10)
//                .attendanceRate(BigDecimal.valueOf(0.8))
//                .build();
//
//        when(meetingService.getMeetingById(meetingId)).thenReturn(meeting);
//        when(meetingAuthorizationService.getUserMeetingPermissions(meetingId, 1L))
//                .thenReturn(participationInfo);
//        when(meetingAnalyticsService.getMeetingStatistics(meetingId))
//                .thenReturn(Optional.of(statistics));
//        when(meetingMarkService.isMeetingImportantForUser(1L, meetingId)).thenReturn(false);
//
//        // When
//        String viewName = webController.meetingDetails(meetingId, userDetails, model);
//
//        // Then
//        assertEquals("meetings/details", viewName);
//        verify(model).addAttribute("meetingStatistics", statistics);
//    }

    @Test
    void meetingDetails_shouldIncludeVotings() {
        // Given
        Long meetingId = 1L;
        MeetingResponse meeting = createMockMeetingResponse(meetingId);
        VotingResponse voting1 = VotingResponse.builder()
                .id(1L)
                .title("Voting 1")
                .status(com.meethub.domain.model.enums.VotingStatus.ACTIVE)
                .build();
        VotingResponse voting2 = VotingResponse.builder()
                .id(2L)
                .title("Voting 2")
                .status(com.meethub.domain.model.enums.VotingStatus.CLOSED)
                .build();

        when(meetingService.getMeetingById(meetingId)).thenReturn(meeting);
        when(meetingVotingService.getMeetingVotings(meetingId, 1L))
                .thenReturn(Arrays.asList(voting1, voting2));
        when(meetingMarkService.isMeetingImportantForUser(1L, meetingId)).thenReturn(false);

        // When
        String viewName = webController.meetingDetails(meetingId, userDetails, model);

        // Then
        assertEquals("meetings/details", viewName);
        verify(model).addAttribute("activeVotings", List.of(voting1));
        verify(model).addAttribute("closedVotings", List.of(voting2));
    }

    // ==================== TESTS FOR CREATE MEETING ENDPOINTS ====================
//
//    @Test
//    void showCreateMeetingForm_shouldReturnFormWithLocations() {
//        // Given
//        List<LocationBasicInfo> locations = Arrays.asList(
//                new LocationBasicInfo(1L, "Room A", "Building 1"),
//                new LocationBasicInfo(2L, "Room B", "Building 2")
//        );
//        when(locationService.getLocationsForSelect()).thenReturn(locations);
//
//        // When
//        String viewName = webController.showCreateMeetingForm(userDetails, model);
//
//        // Then
//        assertEquals("meetings/create", viewName);
//        verify(model).addAttribute("createMeetingRequest", any(CreateMeetingRequest.class));
//        verify(model).addAttribute("locations", locations);
//    }

    @Test
    void showCreateMeetingForm_shouldRedirectToLogin_whenUserNotAuthenticated() {
        // When
        String viewName = webController.showCreateMeetingForm(null, model);

        // Then
        assertEquals("redirect:/login", viewName);
    }

//    @Test
//    void createMeeting_shouldCreateMeeting_whenValidRequest() {
//        // Given
//        CreateMeetingRequest request = CreateMeetingRequest.builder()
//                .title("Test Meeting")
//                .description("Test Description")
//                .type(MeetingType.IN_PERSON)
//                .visibility(MeetingVisibility.PUBLIC)
//                .startDate(LocalDateTime.now().plusDays(1))
//                .endDate(LocalDateTime.now().plusDays(1).plusHours(2))
//                .locationId(1L)
//                .build();
//
//        MeetingResponse createdMeeting = MeetingResponse.builder()
//                .id(1L)
//                .title("Test Meeting")
//                .build();
//
//        when(locationService.validateLocationExists(1L)).thenReturn(true);
//        when(meetingService.createMeeting(request, 1L)).thenReturn(createdMeeting);
//
//        // When
//        String viewName = webController.createMeeting(request, bindingResult, userDetails, model, redirectAttributes);
//
//        // Then
//        assertEquals("redirect:/meetings/1", viewName);
//        verify(redirectAttributes).addFlashAttribute("message", "Spotkanie 'Test Meeting' zostało utworzone pomyślnie!");
//    }

//    @Test
//    void createMeeting_shouldReturnFormWithErrors_whenValidationFails() {
//        // Given
//        CreateMeetingRequest request = CreateMeetingRequest.builder()
//                .title("")  // Invalid - empty
//                .type(MeetingType.IN_PERSON)
//                .locationId(null)  // Invalid - required for IN_PERSON
//                .build();
//
//        when(bindingResult.hasErrors()).thenReturn(true);
//        FieldError fieldError = new FieldError("createMeetingRequest", "title", "Title is required");
//        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
//
//        List<LocationBasicInfo> locations = List.of(new LocationBasicInfo(1L, "Room A", "Building 1"));
//        when(locationService.getLocationsForSelect()).thenReturn(locations);
//
//        // When
//        String viewName = webController.createMeeting(request, bindingResult, userDetails, model, redirectAttributes);
//
//        // Then
//        assertEquals("meetings/create", viewName);
//        verify(model).addAttribute("locations", locations);
//    }

//    @Test
//    void createMeeting_shouldHandleServiceException() {
//        // Given
//        CreateMeetingRequest request = CreateMeetingRequest.builder()
//                .title("Test Meeting")
//                .type(MeetingType.ONLINE)
//                .startDate(LocalDateTime.now().plusDays(1))
//                .endDate(LocalDateTime.now().plusDays(1).plusHours(2))
//                .build();
//
//        when(meetingService.createMeeting(request, 1L))
//                .thenThrow(new RuntimeException("Service error"));
//
//        List<LocationBasicInfo> locations = List.of(new LocationBasicInfo(1L, "Room A", "Building 1"));
//        when(locationService.getLocationsForSelect()).thenReturn(locations);
//
//        // When
//        String viewName = webController.createMeeting(request, bindingResult, userDetails, model, redirectAttributes);
//
//        // Then
//        assertEquals("meetings/create", viewName);
//        verify(model).addAttribute("error", "Błąd podczas tworzenia spotkania: Service error");
//    }

    // ==================== TESTS FOR EDIT MEETING ENDPOINTS ====================

    @Test
    void showEditMeetingForm_shouldReturnForm_whenUserIsOrganizer() {
        // Given
        Long meetingId = 1L;
        MeetingResponse meeting = MeetingResponse.builder()
                .id(meetingId)
                .title("Existing Meeting")
                .organizer(UserResponse.builder().id(1L).build())
                .build();

        when(meetingService.getMeetingById(meetingId)).thenReturn(meeting);

        // When
        String viewName = webController.showEditMeetingForm(meetingId, userDetails, model);

        // Then
        assertEquals("meetings/edit", viewName);
        verify(model).addAttribute(eq("updateMeetingRequest"), any(UpdateMeetingRequest.class));
        verify(model).addAttribute(eq("meetingId"), eq(meetingId));

    }

    @Test
    void showEditMeetingForm_shouldRedirect_whenUserNotOrganizer() {
        // Given
        Long meetingId = 1L;
        MeetingResponse meeting = MeetingResponse.builder()
                .id(meetingId)
                .organizer(UserResponse.builder().id(999L).build())  // Different user
                .build();

        when(meetingService.getMeetingById(meetingId)).thenReturn(meeting);

        // When
        String viewName = webController.showEditMeetingForm(meetingId, userDetails, model);

        // Then
        assertEquals("redirect:/meetings/1?error=Nie masz uprawnień do edycji tego spotkania", viewName);
    }

    @Test
    void updateMeeting_shouldUpdateSuccessfully() {
        // Given
        Long meetingId = 1L;
        UpdateMeetingRequest request = UpdateMeetingRequest.builder()
                .title("Updated Title")
                .type(MeetingType.ONLINE)
                .build();

        MeetingResponse updatedMeeting = MeetingResponse.builder()
                .id(meetingId)
                .title("Updated Title")
                .build();

        when(meetingService.updateMeeting(meetingId, request, 1L)).thenReturn(updatedMeeting);

        // When
        String viewName = webController.updateMeeting(meetingId, request, bindingResult, userDetails, model, redirectAttributes);

        // Then
        assertEquals("redirect:/meetings/1", viewName);
        verify(redirectAttributes).addFlashAttribute("message", "Spotkanie 'Updated Title' zostało zaktualizowane pomyślnie!");
    }

    // ==================== TESTS FOR DELETE MEETING ====================

    @Test
    void deleteMeeting_shouldDeleteSuccessfully() {
        // Given
        Long meetingId = 1L;

        // When
        String viewName = webController.deleteMeeting(meetingId, userDetails, redirectAttributes);

        // Then
        assertEquals("redirect:/meetings", viewName);
        verify(meetingService).deleteMeeting(meetingId, 1L);
        verify(redirectAttributes).addFlashAttribute("message", "Spotkanie zostało usunięte pomyślnie!");
    }

    @Test
    void deleteMeeting_shouldHandleException() {
        // Given
        Long meetingId = 1L;
        doThrow(new RuntimeException("Cannot delete")).when(meetingService).deleteMeeting(meetingId, 1L);

        // When
        String viewName = webController.deleteMeeting(meetingId, userDetails, redirectAttributes);

        // Then
        assertEquals("redirect:/meetings/1", viewName);
        verify(redirectAttributes).addFlashAttribute("error", "Błąd podczas usuwania spotkania: Cannot delete");
    }

    // ==================== TESTS FOR JOIN/LEAVE MEETING ====================

    @Test
    void joinMeeting_shouldJoinSuccessfully() {
        // Given
        Long meetingId = 1L;

        // When
        String viewName = webController.joinMeeting(meetingId, userDetails, redirectAttributes);

        // Then
        assertEquals("redirect:/meetings/1", viewName);
        verify(meetingParticipantService).joinMeeting(1L, meetingId);
        verify(redirectAttributes).addFlashAttribute("message", "Dołączyłeś do spotkania pomyślnie!");
    }

    @Test
    void leaveMeeting_shouldLeaveSuccessfully() {
        // Given
        Long meetingId = 1L;

        // When
        String viewName = webController.leaveMeeting(meetingId, userDetails, redirectAttributes);

        // Then
        assertEquals("redirect:/meetings/1", viewName);
        verify(meetingParticipantService).leaveMeeting(1L, meetingId);
        verify(redirectAttributes).addFlashAttribute("message", "Opuszczono spotkanie pomyślnie!");
    }

    // ==================== TESTS FOR TEMPLATES ====================

    @Test
    void templates_shouldReturnTemplatesList() {
        // Given
        List<MeetingResponse> templates = Arrays.asList(
                MeetingResponse.builder().id(1L).title("Template 1").isTemplate(true).build(),
                MeetingResponse.builder().id(2L).title("Template 2").isTemplate(true).build()
        );
        when(meetingService.getMeetingTemplates(1L)).thenReturn(templates);

        // When
        String viewName = webController.templates(userDetails, model);

        // Then
        assertEquals("meetings/templates", viewName);
        verify(model).addAttribute("templates", templates);
    }

    @Test
    void createFromTemplate_shouldRedirectToCreateForm() {
        // Given
        Long templateId = 1L;
        MeetingResponse template = MeetingResponse.builder()
                .id(templateId)
                .title("Template")
                .isTemplate(true)
                .type(MeetingType.PHYSICAL)
                .visibility(MeetingVisibility.PUBLIC)
                .startDate(LocalDateTime.now().plusDays(7))
                .endDate(LocalDateTime.now().plusDays(7).plusHours(2))
                .build();

        when(meetingService.getMeetingById(templateId)).thenReturn(template);
        when(locationService.getLocationsForSelect()).thenReturn(Collections.emptyList());

        // When
        String viewName = webController.createFromTemplate(templateId, userDetails, model);

        // Then
        assertEquals("meetings/create", viewName);
        verify(model).addAttribute(eq("createMeetingRequest"), any(CreateMeetingRequest.class));
        verify(model).addAttribute(eq("fromTemplate"), eq(true));

    }

    // ==================== TESTS FOR ATTENDANCE TOKEN ====================

    @Test
    void attendMeeting_shouldValidateTokenSuccessfully() {
        // Given
        Long meetingId = 1L;
        String token = "valid-token";

        when(attendanceTokenService.validateAndUseToken(token, meetingId)).thenReturn(true);

        // When
        String viewName = webController.attendMeeting(meetingId, token, userDetails, redirectAttributes);

        // Then
        assertEquals("redirect:/meetings/1", viewName);
        verify(meetingParticipantService).markAsAttended(meetingId, 1L);
        verify(redirectAttributes).addFlashAttribute("success", "Twoja obecność została odnotowana");
    }

    @Test
    void attendMeeting_shouldHandleInvalidToken() {
        // Given
        Long meetingId = 1L;
        String token = "invalid-token";

        when(attendanceTokenService.validateAndUseToken(token, meetingId)).thenReturn(false);

        // When
        String viewName = webController.attendMeeting(meetingId, token, userDetails, redirectAttributes);

        // Then
        assertEquals("redirect:/meetings/1", viewName);
        verify(redirectAttributes).addFlashAttribute("error", "Nieprawidłowy lub wygasły token");
    }

    // ==================== TESTS FOR SEARCH ENDPOINT ====================

//    @Test
//    void searchMeetings_shouldPerformSearchWithParameters() {
//        // Given
//        SearchCriteria criteria = SearchCriteria.builder()
//                .keywords("test")
//                .currentUserId(1L)
//                .userAuthenticated(true)
//                .includePublic(true)
//                .build();
//
//        Page<MeetingResponse> searchResults = new PageImpl<>(Arrays.asList(
//                createMockMeetingResponse(1L),
//                createMockMeetingResponse(2L)
//        ));
//
//        when(meetingService.searchMeetings(any(SearchCriteria.class), any(Pageable.class)))
//                .thenReturn(searchResults);
//
//        // When
//        String viewName = webController.searchMeetings(
//                userDetails, "test", null, null, null, null,
//                null, null, null, null, null, null, null,
//                null, null, null, null, null, null, null,
////                0, 12, model
//        );
//
//        // Then
//        assertEquals("meetings/advanced-search", viewName);
//        verify(model).addAttribute("meetings", searchResults.getContent());
//        verify(model).addAttribute("isSearchResults", true);
//    }

    // ==================== TESTS FOR UTILITY METHODS ====================

    @Test
    void determineUserRole_shouldReturnCorrectRoles() {
        assertEquals("ORGANIZER", webController.determineUserRole(true, false, false, false, false, false, false, false));
        assertEquals("CONFIRMED_PARTICIPANT", webController.determineUserRole(false, true, false, false, false, false, false, false));
        assertEquals("PENDING", webController.determineUserRole(false, false, true, false, false, false, false, false));
        assertEquals("INVITED", webController.determineUserRole(false, false, false, true, false, false, false, false));
        assertEquals("DECLINED", webController.determineUserRole(false, false, false, false, true, false, false, false));
        assertEquals("WAITING_LIST", webController.determineUserRole(false, false, false, false, false, true, false, false));
        assertEquals("VIEWER", webController.determineUserRole(false, false, false, false, false, false, true, false));
        assertEquals("UNRELATED", webController.determineUserRole(false, false, false, false, false, false, false, true));
    }

    // ==================== HELPER METHODS ====================

    private MeetingResponse createMockMeetingResponse(Long id) {
        return MeetingResponse.builder()
                .id(id)
                .title("Test Meeting " + id)
                .description("Description " + id)
                .type(MeetingType.PHYSICAL)
                .visibility(MeetingVisibility.PUBLIC)
                .status(MeetingStatus.PLANNED)
                .startDate(LocalDateTime.now().plusDays(id))
                .endDate(LocalDateTime.now().plusDays(id).plusHours(2))
                .organizer(UserResponse.builder().id(1L).firstName("organizer").build())
                .maxParticipants(10)
                .confirmedParticipantsCount(5)
                .waitingListCount(2)
                .availableSpots(3)
                .build();
    }

    private MeetingParticipationInfo createMockParticipationInfo(boolean isOrganizer, boolean isParticipant) {
        return MeetingParticipationInfo.builder()
                .participantRole(isOrganizer ? "ORGANIZER" : "PARTICIPANT")
                .canEdit(isOrganizer)
                .canDelete(isOrganizer)
                .canManageParticipants(isOrganizer)
                .canJoin(!isOrganizer && !isParticipant)
                .build();
    }
}
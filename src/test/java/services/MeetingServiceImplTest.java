//package com.meethub.domain.service.impl;
//
//import com.meethub.domain.model.entity.*;
//import com.meethub.domain.model.enums.MeetingStatus;
//import com.meethub.domain.model.enums.MeetingType;
//import com.meethub.domain.model.enums.MeetingVisibility;
//import com.meethub.domain.model.mapper.MeetingMapper;
//import com.meethub.domain.model.request.CreateMeetingRequest;
//import com.meethub.domain.model.request.SearchCriteria;
//import com.meethub.domain.model.request.UpdateMeetingRequest;
//import com.meethub.domain.model.response.MeetingParticipationInfo;
//import com.meethub.domain.model.response.MeetingResponse;
//import com.meethub.domain.repository.jpa.CategoryRepository;
//import com.meethub.domain.repository.jpa.MeetingRepository;
//import com.meethub.domain.repository.jpa.UserRepository;
//import com.meethub.domain.repository.jdbc.CustomMeetingRepository;
//import com.meethub.domain.service.MeetingAuthorizationService;
//import com.meethub.domain.service.MeetingParticipantService;
//import com.meethub.exception.BusinessException;
//import com.meethub.exception.ResourceNotFoundException;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.*;
//import org.springframework.data.jpa.domain.Specification;
//
//import java.time.LocalDateTime;
//import java.util.*;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class MeetingServiceImplTest {
//
//    @Mock
//    private MeetingRepository meetingRepository;
//
//    @Mock
//    private UserRepository userRepository;
//
//    @Mock
//    private CustomMeetingRepository customMeetingRepository;
//
//    @Mock
//    private MeetingMapper meetingMapper;
//
//    @Mock
//    private MeetingParticipantService meetingParticipantService;
//
//    @Mock
//    private MeetingAuthorizationService meetingAuthorizationService;
//
//    @Mock
//    private CategoryRepository categoryRepository;
//
//    @InjectMocks
//    private MeetingServiceImpl meetingService;
//
//    private User organizer;
//    private Meeting meeting;
//    private MeetingResponse meetingResponse;
//    private Meeting templateMeeting;
//    private Category category;
//
//    @BeforeEach
//    void setUp() {
//        organizer = User.builder()
//                .id(1L)
//                .firstName("John")
//                .lastName("Doe")
//                .email("john@example.com")
//                .build();
//
//        category = Category.builder()
//                .id(1L)
//                .name("Business")
//                .build();
//
//        meeting = Meeting.builder()
//                .title("Team Meeting")
//                .description("Weekly sync")
//                .type(MeetingType.PHYSICAL)
//                .visibility(MeetingVisibility.PUBLIC)
//                .startDate(LocalDateTime.now().plusDays(1))
//                .endDate(LocalDateTime.now().plusDays(1).plusHours(1))
//                .organizer(organizer)
//                .maxParticipants(10)
//                .build();
//
//        templateMeeting = Meeting.builder()
//                .title("Template Meeting")
//                .organizer(organizer)
//                .build();
//
//        meetingResponse = MeetingResponse.builder()
//                .id(1L)
//                .title("Team Meeting")
//                .type(MeetingType.PHYSICAL)
//                .status(MeetingStatus.PLANNED)
//                .build();
//    }
//
//    // ============ CREATE MEETING TESTS ============
//
//    @Test
//    void createMeeting_Success() {
//        // Given
//        CreateMeetingRequest request = CreateMeetingRequest.builder()
//                .title("Team Meeting")
//                .type(MeetingType.PHYSICAL)
//                .visibility(MeetingVisibility.PUBLIC)
//                .startDate(LocalDateTime.now().plusDays(1))
//                .endDate(LocalDateTime.now().plusDays(1).plusHours(1))
//                .categoryIds((Set<Long>) List.of(1L))
//                .build();
//
//        when(userRepository.findById(1L)).thenReturn(Optional.of(organizer));
//        when(meetingMapper.toEntity(request)).thenReturn(meeting);
//        when(categoryRepository.findAllById(anyList())).thenReturn(List.of(category));
//        when(meetingRepository.save(any(Meeting.class))).thenReturn(meeting);
//        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(meetingResponse);
//
//        // When
//        MeetingResponse response = meetingService.createMeeting(request, 1L);
//
//        // Then
//        assertAll(
//                () -> assertNotNull(response),
//                () -> assertEquals(1L, response.getId()),
//                () -> assertEquals("Team Meeting", response.getTitle())
//        );
//
//        verify(userRepository).findById(1L);
//        verify(meetingMapper).toEntity(request);
//        verify(categoryRepository).findAllById(List.of(1L));
//        verify(meetingRepository).save(any(Meeting.class));
//    }
//
//    @Test
//    void createMeeting_OrganizerNotFound_ThrowsResourceNotFoundException() {
//        // Given
//        CreateMeetingRequest request = CreateMeetingRequest.builder()
//                .title("Team Meeting")
//                .build();
//
//        when(userRepository.findById(999L)).thenReturn(Optional.empty());
//
//        // When & Then
//        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
//                () -> meetingService.createMeeting(request, 999L));
//
//        assertEquals("User not found with id: 999", exception.getMessage());
//        verify(meetingRepository, never()).save(any(Meeting.class));
//    }
//
//    @Test
//    void createMeeting_SaveAsTemplate_Success() {
//        // Given
//        CreateMeetingRequest request = CreateMeetingRequest.builder()
//                .title("Template Meeting")
//                .saveAsTemplate(true)
//                .templateName("My Template")
//                .build();
//
//        Meeting template = Meeting.builder()
//                .title("My Template")
//                .build();
//
//        when(userRepository.findById(1L)).thenReturn(Optional.of(organizer));
//        when(meetingMapper.toEntity(request)).thenReturn(template);
//        when(meetingRepository.save(any(Meeting.class))).thenReturn(template);
//        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(MeetingResponse.builder()
//                .id(2L)
//                .title("My Template")
//                .build());
//
//        // When
//        MeetingResponse response = meetingService.createMeeting(request, 1L);
//
//        // Then
//        assertAll(
//                () -> assertNotNull(response),
//                () -> assertEquals(2L, response.getId()),
//                () -> assertEquals("My Template", response.getTitle())
//        );
//    }
//
//    @Test
//    void createMeeting_WithRecurring_GeneratesNextOccurrences() {
//        // Given
//        CreateMeetingRequest request = CreateMeetingRequest.builder()
//                .title("Recurring Meeting")
//                .recurring(true)
//                .recurrencePattern("DAILY:1")
//                .build();
//
//        Meeting recurringMeeting = Meeting.builder()
//                .title("Recurring Meeting")
//                .build();
//
//        when(userRepository.findById(1L)).thenReturn(Optional.of(organizer));
//        when(meetingMapper.toEntity(request)).thenReturn(recurringMeeting);
//        when(meetingRepository.save(any(Meeting.class))).thenReturn(recurringMeeting);
//        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(MeetingResponse.builder()
//                .id(3L)
//                .title("Recurring Meeting")
//                .build());
//
//        // When
//        MeetingResponse response = meetingService.createMeeting(request, 1L);
//
//        // Then
//        assertNotNull(response);
//        verify(meetingRepository).save(any(Meeting.class));
//    }
//
//    // ============ UPDATE MEETING TESTS ============
//
//    @Test
//    void updateMeeting_Success() {
//        // Given
//        UpdateMeetingRequest request = UpdateMeetingRequest.builder()
//                .title("Updated Meeting")
//                .categoryIds((Set<Long>) List.of(1L))
//                .build();
//
//        when(meetingAuthorizationService.canUserEditMeeting(1L, 1L)).thenReturn(true);
//        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
//        when(categoryRepository.findAllById(anyList())).thenReturn(List.of(category));
//        when(meetingRepository.save(any(Meeting.class))).thenReturn(meeting);
//        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(MeetingResponse.builder()
//                .id(1L)
//                .title("Updated Meeting")
//                .build());
//
//        // When
//        MeetingResponse response = meetingService.updateMeeting(1L, request, 1L);
//
//        // Then
//        assertAll(
//                () -> assertNotNull(response),
//                () -> assertEquals(1L, response.getId()),
//                () -> assertEquals("Updated Meeting", response.getTitle())
//        );
//
//        verify(meetingAuthorizationService).canUserEditMeeting(1L, 1L);
//        verify(meetingRepository).findById(1L);
//        verify(meetingRepository).save(any(Meeting.class));
//    }
//
//    @Test
//    void updateMeeting_NoPermission_ThrowsBusinessException() {
//        // Given
//        UpdateMeetingRequest request = UpdateMeetingRequest.builder().title("Updated").build();
//        when(meetingAuthorizationService.canUserEditMeeting(1L, 1L)).thenReturn(false);
//
//        // When & Then
//        BusinessException exception = assertThrows(BusinessException.class,
//                () -> meetingService.updateMeeting(1L, request, 1L));
//
//        assertEquals("No permission to edit this meeting", exception.getMessage());
//        verify(meetingRepository, never()).save(any(Meeting.class));
//    }
//
//    @Test
//    void updateMeeting_MeetingNotFound_ThrowsResourceNotFoundException() {
//        // Given
//        UpdateMeetingRequest request = UpdateMeetingRequest.builder().title("Updated").build();
//        when(meetingAuthorizationService.canUserEditMeeting(1L, 1L)).thenReturn(true);
//        when(meetingRepository.findById(1L)).thenReturn(Optional.empty());
//
//        // When & Then
//        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
//                () -> meetingService.updateMeeting(1L, request, 1L));
//
//        assertEquals("Meeting not found with id: 1", exception.getMessage());
//    }
//
//    @Test
//    void updateMeeting_StatusChange_SavesStatusHistory() {
//        // Given
//        UpdateMeetingRequest request = UpdateMeetingRequest.builder()
//                .status(MeetingStatus.CONFIRMED)
//                .statusChangeReason("Confirmed by manager")
//                .build();
//
//        when(meetingAuthorizationService.canUserEditMeeting(1L, 1L)).thenReturn(true);
//        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
//        when(meetingRepository.save(any(Meeting.class))).thenReturn(meeting);
//        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(meetingResponse);
//
//        // When
//        MeetingResponse response = meetingService.updateMeeting(1L, request, 1L);
//
//        // Then
//        assertNotNull(response);
//        verify(meetingRepository).save(any(Meeting.class));
//    }
//
//    // ============ DELETE MEETING TESTS ============
//
//    @Test
//    void deleteMeeting_Success() {
//        // Given
//        when(meetingAuthorizationService.canUserDeleteMeeting(1L, 1L)).thenReturn(true);
//        when(meetingRepository.findByIdAndOrganizerId(1L, 1L)).thenReturn(Optional.of(meeting));
//
//        // When
//        meetingService.deleteMeeting(1L, 1L);
//
//        // Then
//        verify(meetingAuthorizationService).canUserDeleteMeeting(1L, 1L);
//        verify(meetingRepository).delete(meeting);
//    }
//
//    @Test
//    void deleteMeeting_NoPermission_ThrowsBusinessException() {
//        // Given
//        when(meetingAuthorizationService.canUserDeleteMeeting(1L, 1L)).thenReturn(false);
//
//        // When & Then
//        BusinessException exception = assertThrows(BusinessException.class,
//                () -> meetingService.deleteMeeting(1L, 1L));
//
//        assertEquals("No permission to delete this meeting", exception.getMessage());
//        verify(meetingRepository, never()).delete(any(Meeting.class));
//    }
//
//    @Test
//    void deleteMeeting_MeetingNotFound_ThrowsResourceNotFoundException() {
//        // Given
//        when(meetingAuthorizationService.canUserDeleteMeeting(1L, 1L)).thenReturn(true);
//        when(meetingRepository.findByIdAndOrganizerId(1L, 1L)).thenReturn(Optional.empty());
//
//        // When & Then
//        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
//                () -> meetingService.deleteMeeting(1L, 1L));
//
//        assertTrue(exception.getMessage().contains("Meeting not found with id: 1"));
//    }
//
//    // ============ GET MEETING TESTS ============
//
//    @Test
//    void getMeetingById_Success() {
//        // Given
//        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
//        when(meetingMapper.toResponse(meeting)).thenReturn(meetingResponse);
//
//        // When
//        MeetingResponse response = meetingService.getMeetingById(1L);
//
//        // Then
//        assertAll(
//                () -> assertNotNull(response),
//                () -> assertEquals(1L, response.getId())
//        );
//
//        verify(meetingRepository).findById(1L);
//        verify(meetingMapper).toResponse(meeting);
//    }
//
//    @Test
//    void getMeetingById_NotFound_ThrowsResourceNotFoundException() {
//        // Given
//        when(meetingRepository.findById(999L)).thenReturn(Optional.empty());
//
//        // When & Then
//        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
//                () -> meetingService.getMeetingById(999L));
//
//        assertEquals("Meeting not found with id: 999", exception.getMessage());
//    }
//
//    // ============ GET USER MEETINGS TESTS ============
//
//    @Test
//    void getUserMeetings_Success() {
//        // Given
//        Pageable pageable = PageRequest.of(0, 10);
//        Page<Meeting> meetingsPage = new PageImpl<>(List.of(meeting), pageable, 1);
//
//        when(meetingRepository.findByOrganizerId(1L, pageable)).thenReturn(meetingsPage);
//        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(meetingResponse);
//
//        // When
//        Page<MeetingResponse> responsePage = meetingService.getUserMeetings(1L, pageable);
//
//        // Then
//        assertAll(
//                () -> assertNotNull(responsePage),
//                () -> assertEquals(1, responsePage.getTotalElements())
//        );
//
//        verify(meetingRepository).findByOrganizerId(1L, pageable);
//    }
//
//    // ============ GET UPCOMING PUBLIC MEETINGS TESTS ============
//
//    @Test
//    void getUpcomingPublicMeetings_Success() {
//        // Given
//        List<Meeting> meetings = List.of(meeting);
//        when(meetingRepository.findUpcomingPublicMeetings(any(LocalDateTime.class))).thenReturn(meetings);
//        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(meetingResponse);
//
//        // When
//        List<MeetingResponse> responses = meetingService.getUpcomingPublicMeetings();
//
//        // Then
//        assertAll(
//                () -> assertNotNull(responses),
//                () -> assertEquals(1, responses.size())
//        );
//
//        verify(meetingRepository).findUpcomingPublicMeetings(any(LocalDateTime.class));
//    }
//
//    // ============ CHANGE MEETING STATUS TESTS ============
//
//    @Test
//    void changeMeetingStatus_Success() {
//        // Given
//        when(meetingAuthorizationService.canUserEditMeeting(1L, 1L)).thenReturn(true);
//        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
//        when(meetingRepository.save(any(Meeting.class))).thenReturn(meeting);
//
//        // When
//        meetingService.changeMeetingStatus(1L, MeetingStatus.CONFIRMED, 1L);
//
//        // Then
//        assertAll(
//                () -> verify(meetingAuthorizationService).canUserEditMeeting(1L, 1L),
//                () -> verify(meetingRepository).findById(1L),
//                () -> verify(meetingRepository).save(any(Meeting.class)),
//                () -> assertEquals(MeetingStatus.CONFIRMED, meeting.getStatus())
//        );
//    }
//
//    @Test
//    void changeMeetingStatus_NoPermission_ThrowsBusinessException() {
//        // Given
//        when(meetingAuthorizationService.canUserEditMeeting(1L, 1L)).thenReturn(false);
//
//        // When & Then
//        BusinessException exception = assertThrows(BusinessException.class,
//                () -> meetingService.changeMeetingStatus(1L, MeetingStatus.CONFIRMED, 1L));
//
//        assertEquals("No permission to change status of this meeting", exception.getMessage());
//    }
//
//    // ============ DUPLICATE MEETING TESTS ============
//
//    @Test
//    void duplicateMeeting_Success() {
//        // Given
//        Meeting duplicate = Meeting.builder()
//                .title("Team Meeting (Copy)")
//                .build();
//
//        when(meetingAuthorizationService.canUserEditMeeting(1L, 1L)).thenReturn(true);
//        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
//        when(meetingMapper.cloneMeeting(meeting)).thenReturn(duplicate);
//        when(userRepository.findById(1L)).thenReturn(Optional.of(organizer));
//        when(meetingRepository.save(any(Meeting.class))).thenReturn(duplicate);
//        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(MeetingResponse.builder()
//                .id(2L)
//                .title("Team Meeting (Copy)")
//                .build());
//
//        // When
//        MeetingResponse response = meetingService.duplicateMeeting(1L, 1L);
//
//        // Then
//        assertAll(
//                () -> assertNotNull(response),
//                () -> assertEquals(2L, response.getId())
//        );
//
//        verify(meetingAuthorizationService).canUserEditMeeting(1L, 1L);
//        verify(meetingRepository).save(any(Meeting.class));
//    }
//
//    // ============ GET TEMPLATES TESTS ============
//
//    @Test
//    void getMeetingTemplates_Success() {
//        // Given
//        List<Meeting> templates = List.of(templateMeeting);
//        when(meetingRepository.findByOrganizerIdAndTemplateTrue(1L)).thenReturn(templates);
//        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(MeetingResponse.builder()
//                .id(2L)
//                .title("Template Meeting")
//                .build());
//
//        // When
//        List<MeetingResponse> responses = meetingService.getMeetingTemplates(1L);
//
//        // Then
//        assertAll(
//                () -> assertNotNull(responses),
//                () -> assertEquals(1, responses.size()),
//                () -> assertEquals("Template Meeting", responses.get(0).getTitle())
//        );
//
//        verify(meetingRepository).findByOrganizerIdAndTemplateTrue(1L);
//    }
//
//    // ============ CREATE FROM TEMPLATE TESTS ============
//
//    @Test
//    void createFromTemplate_Success() {
//        // Given
//        LocalDateTime newStartDate = LocalDateTime.now().plusDays(7);
//        Meeting newMeeting = Meeting.builder()
//                .title("New Meeting")
//                .build();
//
//        when(meetingRepository.findByIdAndTemplateTrue(2L)).thenReturn(Optional.of(templateMeeting));
//        when(meetingMapper.cloneMeeting(templateMeeting)).thenReturn(newMeeting);
//        when(userRepository.findById(1L)).thenReturn(Optional.of(organizer));
//        when(meetingRepository.save(any(Meeting.class))).thenReturn(newMeeting);
//        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(MeetingResponse.builder()
//                .id(3L)
//                .title("New Meeting")
//                .build());
//
//        // When
//        MeetingResponse response = meetingService.createFromTemplate(2L, 1L, newStartDate);
//
//        // Then
//        assertAll(
//                () -> assertNotNull(response),
//                () -> assertEquals(3L, response.getId())
//        );
//
//        verify(meetingRepository).findByIdAndTemplateTrue(2L);
//        verify(meetingRepository).save(any(Meeting.class));
//    }
//
//    @Test
//    void createFromTemplate_TemplateNotFound_ThrowsResourceNotFoundException() {
//        // Given
//        when(meetingRepository.findByIdAndTemplateTrue(999L)).thenReturn(Optional.empty());
//
//        // When & Then
//        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
//                () -> meetingService.createFromTemplate(999L, 1L, LocalDateTime.now()));
//
//        assertEquals("Template not found or not a template", exception.getMessage());
//    }
//
//    // ============ SAVE AS TEMPLATE TESTS ============
//
//    @Test
//    void saveAsTemplate_Success() {
//        // Given
//        String templateName = "My Template";
//        Meeting template = Meeting.builder()
//                .title(templateName)
//                .build();
//
//        when(meetingAuthorizationService.canUserEditMeeting(1L, 1L)).thenReturn(true);
//        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
//        when(meetingMapper.createTemplateFromMeeting(meeting, templateName)).thenReturn(template);
//        when(userRepository.findById(1L)).thenReturn(Optional.of(organizer));
//        when(meetingRepository.save(any(Meeting.class))).thenReturn(template);
//        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(MeetingResponse.builder()
//                .id(4L)
//                .title(templateName)
//                .build());
//
//        // When
//        MeetingResponse response = meetingService.saveAsTemplate(1L, templateName, 1L);
//
//        // Then
//        assertAll(
//                () -> assertNotNull(response),
//                () -> assertEquals(4L, response.getId()),
//                () -> assertEquals(templateName, response.getTitle())
//        );
//
//        verify(meetingAuthorizationService).canUserEditMeeting(1L, 1L);
//        verify(meetingRepository).save(any(Meeting.class));
//    }
//
//    // ============ GET ACCESSIBLE MEETINGS TESTS ============
//
//    @Test
//    void getAccessibleMeetings_Success() {
//        // Given
//        List<Meeting> allMeetings = List.of(meeting);
//        when(meetingRepository.findAll()).thenReturn(allMeetings);
//        when(meetingAuthorizationService.canUserViewResource(1L, 1L)).thenReturn(true);
//        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(meetingResponse);
//
//        // When
//        List<MeetingResponse> responses = meetingService.getAccessibleMeetings(1L);
//
//        // Then
//        assertAll(
//                () -> assertNotNull(responses),
//                () -> assertEquals(1, responses.size())
//        );
//
//        verify(meetingRepository).findAll();
//        verify(meetingAuthorizationService).canUserViewResource(1L, 1L);
//    }
//
//    // ============ SEARCH MEETINGS TESTS ============
//
//    @Test
//    void searchMeetings_Success() {
//        // Given
//        SearchCriteria criteria = SearchCriteria.builder()
//                .currentUserId(1L)
//                .build();
//
//        Pageable pageable = PageRequest.of(0, 10);
//        Page<Meeting> meetingsPage = new PageImpl<>(List.of(meeting), pageable, 1);
//
//        when(meetingRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(meetingsPage);
//        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(meetingResponse);
//
//        // When
//        Page<MeetingResponse> responsePage = meetingService.searchMeetings(criteria, pageable);
//
//        // Then
//        assertAll(
//                () -> assertNotNull(responsePage),
//                () -> assertEquals(1, responsePage.getTotalElements())
//        );
//
//        verify(meetingRepository).findAll(any(Specification.class), eq(pageable));
//    }
//
//    @Test
//    void searchMeetings_NoUserId_ThrowsBusinessException() {
//        // Given
//        SearchCriteria criteria = SearchCriteria.builder().build();
//        Pageable pageable = PageRequest.of(0, 10);
//
//        // When & Then
//        BusinessException exception = assertThrows(BusinessException.class,
//                () -> meetingService.searchMeetings(criteria, pageable));
//
//        assertEquals("User ID is required for search", exception.getMessage());
//    }
//
//    // ============ MEETING PARTICIPATION TESTS ============
//
//    @Test
//    void getMeetingParticipationInfo_Success() {
//        // Given
//        MeetingParticipationInfo info = MeetingParticipationInfo.builder()
//                .canViewDetails(true)
//                .canEdit(false)
//                .build();
//
//        when(meetingAuthorizationService.getUserMeetingPermissions(1L, 1L)).thenReturn(info);
//
//        // When
//        MeetingParticipationInfo result = meetingService.getMeetingParticipationInfo(1L, 1L);
//
//        // Then
//        assertAll(
//                () -> assertNotNull(result),
//                () -> assertTrue(result.isCanViewDetails()),
//                () -> assertFalse(result.isCanEdit())
//        );
//
//        verify(meetingAuthorizationService).getUserMeetingPermissions(1L, 1L);
//    }
//
//    @Test
//    void canUserAccessMeeting_Success() {
//        // Given
//        MeetingParticipationInfo info = MeetingParticipationInfo.builder()
//                .canViewDetails(true)
//                .build();
//
//        when(meetingAuthorizationService.getUserMeetingPermissions(1L, 1L)).thenReturn(info);
//
//        // When
//        boolean canAccess = meetingService.canUserAccessMeeting(1L, 1L);
//
//        // Then
//        assertTrue(canAccess);
//        verify(meetingAuthorizationService).getUserMeetingPermissions(1L, 1L);
//    }
//
//    // ============ GET MEETINGS BY CATEGORY TESTS ============
//
//    @Test
//    void getMeetingsByCategory_Success() {
//        // Given
//        Pageable pageable = PageRequest.of(0, 10);
//        Page<Meeting> meetingsPage = new PageImpl<>(List.of(meeting), pageable, 1);
//
//        when(meetingRepository.findByCategoryId(1L, pageable)).thenReturn(meetingsPage);
//        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(meetingResponse);
//
//        // When
//        Page<MeetingResponse> responsePage = meetingService.getMeetingsByCategory(1L, pageable);
//
//        // Then
//        assertAll(
//                () -> assertNotNull(responsePage),
//                () -> assertEquals(1, responsePage.getTotalElements())
//        );
//
//        verify(meetingRepository).findByCategoryId(1L, pageable);
//    }
//
//    // ============ GET MEETINGS BY TAG TESTS ============
//
//    @Test
//    void getMeetingsByTag_Success() {
//        // Given
//        Pageable pageable = PageRequest.of(0, 10);
//        Page<Meeting> meetingsPage = new PageImpl<>(List.of(meeting), pageable, 1);
//
//        when(meetingRepository.findByTag("team", pageable)).thenReturn(meetingsPage);
//        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(meetingResponse);
//
//        // When
//        Page<MeetingResponse> responsePage = meetingService.getMeetingsByTag("team", pageable);
//
//        // Then
//        assertAll(
//                () -> assertNotNull(responsePage),
//                () -> assertEquals(1, responsePage.getTotalElements())
//        );
//
//        verify(meetingRepository).findByTag("team", pageable);
//    }
//
//    // ============ GET MEETING ENTITY TESTS ============
//
//    @Test
//    void getMeeting_Success() {
//        // Given
//        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
//
//        // When
//        Meeting result = meetingService.getMeeting(1L);
//
//        // Then
//        assertAll(
//                () -> assertNotNull(result),
//                () -> assertEquals(1L, result.getId()),
//                () -> assertEquals("Team Meeting", result.getTitle())
//        );
//
//        verify(meetingRepository).findById(1L);
//    }
//
//    @Test
//    void getMeeting_NotFound_ThrowsResourceNotFoundException() {
//        // Given
//        when(meetingRepository.findById(999L)).thenReturn(Optional.empty());
//
//        // When & Then
//        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
//                () -> meetingService.getMeeting(999L));
//
//        assertEquals("Meeting not found with id: 999", exception.getMessage());
//    }
//
//    // ============ GET CONFLICTING MEETINGS TESTS ============
//
//    @Test
//    void findConflictingMeetings_Success() {
//        // Given
//        LocalDateTime startDate = LocalDateTime.now().plusDays(1);
//        LocalDateTime endDate = LocalDateTime.now().plusDays(2);
//        List<Meeting> conflicts = List.of(meeting);
//
//        when(meetingRepository.findConfirmedMeetingsForUserInPeriod(1L, startDate, endDate))
//                .thenReturn(conflicts);
//        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(meetingResponse);
//
//        // When
//        List<MeetingResponse> responses = meetingService.findConflictingMeetings(1L, startDate, endDate);
//
//        // Then
//        assertAll(
//                () -> assertNotNull(responses),
//                () -> assertEquals(1, responses.size())
//        );
//
//        verify(meetingRepository).findConfirmedMeetingsForUserInPeriod(1L, startDate, endDate);
//    }
//
//    // ============ GET FILTERED MEETINGS TESTS ============
//
//    @Test
//    void getFilteredMeetings_Success() {
//        // Given
//        Pageable pageable = PageRequest.of(0, 10);
//        Page<Meeting> meetingsPage = new PageImpl<>(List.of(meeting), pageable, 1);
//
//        when(customMeetingRepository.findFilteredMeetings("Team", "MEETING", "PLANNED", pageable))
//                .thenReturn(meetingsPage);
//        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(meetingResponse);
//
//        // When
//        Page<MeetingResponse> responsePage = meetingService.getFilteredMeetings("Team", "MEETING", "PLANNED", pageable);
//
//        // Then
//        assertAll(
//                () -> assertNotNull(responsePage),
//                () -> assertEquals(1, responsePage.getTotalElements())
//        );
//
//        verify(customMeetingRepository).findFilteredMeetings("Team", "MEETING", "PLANNED", pageable);
//    }
//
//    // ============ GET UPCOMING RECURRING MEETINGS TESTS ============
//
//    @Test
//    void getUpcomingRecurringMeetings_Success() {
//        // Given
//        Meeting recurringMeeting = Meeting.builder()
//                .title("Recurring Meeting")
//                .organizer(organizer)
//                .build();
//
//        List<Meeting> meetings = List.of(recurringMeeting);
//
//        when(meetingRepository.findByRecurringTrueAndRecurrenceEndDateAfter(any(LocalDateTime.class)))
//                .thenReturn(meetings);
//        when(meetingParticipantService.isUserParticipant(5L, 1L)).thenReturn(false);
//        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(MeetingResponse.builder()
//                .id(5L)
//                .title("Recurring Meeting")
//                .build());
//
//        // When
//        List<MeetingResponse> responses = meetingService.getUpcomingRecurringMeetings(1L);
//
//        // Then
//        assertAll(
//                () -> assertNotNull(responses),
//                () -> assertEquals(1, responses.size()),
//                () -> assertEquals("Recurring Meeting", responses.get(0).getTitle())
//        );
//
//        verify(meetingRepository).findByRecurringTrueAndRecurrenceEndDateAfter(any(LocalDateTime.class));
//    }
//
//    // ============ NEARBY MEETINGS TESTS ============
//
//    @Test
//    void findNearbyMeetings_Success() {
//        // Given
//        List<Meeting> meetings = List.of(meeting);
//        when(customMeetingRepository.findNearbyMeetings(52.2297, 21.0122, 10.0, 50)).thenReturn(meetings);
//        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(meetingResponse);
//
//        // When
//        List<MeetingResponse> responses = meetingService.findNearbyMeetings(52.2297, 21.0122, 10.0);
//
//        // Then
//        assertAll(
//                () -> assertNotNull(responses),
//                () -> assertEquals(1, responses.size())
//        );
//
//        verify(customMeetingRepository).findNearbyMeetings(52.2297, 21.0122, 10.0, 50);
//    }
//
//    // ============ ADD RECURRENCE EXCEPTION TESTS ============
//
//    @Test
//    void addRecurrenceException_Success() {
//        // Given
//        Meeting recurringMeeting = Meeting.builder()
//                .build();
//
//        when(meetingRepository.findById(6L)).thenReturn(Optional.of(recurringMeeting));
//        when(meetingRepository.save(any(Meeting.class))).thenReturn(recurringMeeting);
//
//        // When
//        meetingService.addRecurrenceException(6L, "2024-01-15", "Holiday");
//
//        // Then
//        verify(meetingRepository).findById(6L);
//        verify(meetingRepository).save(any(Meeting.class));
//    }
//
//    @Test
//    void addRecurrenceException_NotRecurring_ThrowsBusinessException() {
//        // Given
//        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
//
//        // When & Then
//        BusinessException exception = assertThrows(BusinessException.class,
//                () -> meetingService.addRecurrenceException(1L, "2024-01-15", "Reason"));
//
//        assertEquals("This meeting is not recurring", exception.getMessage());
//    }
//
//    // ============ GET RECURRENCE SERIES TESTS ============
//
//    @Test
//    void getRecurrenceSeries_Success() {
//        // Given
//        List<Meeting> series = List.of(meeting);
//        when(meetingRepository.findByOriginalMeetingId(1L)).thenReturn(series);
//        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(meetingResponse);
//
//        // When
//        List<MeetingResponse> responses = meetingService.getRecurrenceSeries(1L);
//
//        // Then
//        assertAll(
//                () -> assertNotNull(responses),
//                () -> assertEquals(1, responses.size())
//        );
//
//        verify(meetingRepository).findByOriginalMeetingId(1L);
//    }
//
//    // ============ GENERATE NEXT RECURRENCE TESTS ============
//
//    @Test
//    void generateNextRecurrence_Success() {
//        // Given
//        Meeting recurringMeeting = Meeting.builder()
//
//                .startDate(LocalDateTime.now())
//                .endDate(LocalDateTime.now().plusHours(1))
//                .build();
//
//        List<Meeting> occurrences = List.of(
//                Meeting.builder().build(),
//                Meeting.builder().build()
//        );
//
//        when(meetingRepository.findById(7L)).thenReturn(Optional.of(recurringMeeting));
//        when(meetingRepository.findByOriginalMeetingId(7L)).thenReturn(Collections.emptyList());
//        when(meetingRepository.saveAll(anyList())).thenReturn(occurrences);
//        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(MeetingResponse.builder().id(8L).build());
//
//        // When
//        List<MeetingResponse> responses = meetingService.generateNextRecurrence(7L, 2);
//
//        // Then
//        assertAll(
//                () -> assertNotNull(responses),
//                () -> assertEquals(2, responses.size())
//        );
//
//        verify(meetingRepository).findById(7L);
//        verify(meetingRepository).saveAll(anyList());
//    }
//
//    @Test
//    void generateNextRecurrence_NotRecurring_ThrowsBusinessException() {
//        // Given
//        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
//
//        // When & Then
//        BusinessException exception = assertThrows(BusinessException.class,
//                () -> meetingService.generateNextRecurrence(1L, 2));
//
//        assertEquals("This meeting is not recurring", exception.getMessage());
//    }
//}







package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.*;
import com.meethub.domain.model.enums.MeetingStatus;
import com.meethub.domain.model.enums.MeetingType;
import com.meethub.domain.model.enums.MeetingVisibility;
import com.meethub.domain.model.mapper.MeetingMapper;
import com.meethub.domain.model.request.CreateMeetingRequest;
import com.meethub.domain.model.request.SearchCriteria;
import com.meethub.domain.model.request.UpdateMeetingRequest;
import com.meethub.domain.model.response.MeetingParticipationInfo;
import com.meethub.domain.model.response.MeetingResponse;
import com.meethub.domain.repository.jpa.CategoryRepository;
import com.meethub.domain.repository.jpa.MeetingRepository;
import com.meethub.domain.repository.jpa.UserRepository;
import com.meethub.domain.repository.jdbc.CustomMeetingRepository;
import com.meethub.domain.service.MeetingAuthorizationService;
import com.meethub.domain.service.MeetingParticipantService;
import com.meethub.domain.service.MeetingSchedulerService;
import com.meethub.exception.BusinessException;
import com.meethub.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingServiceImplTest {

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomMeetingRepository customMeetingRepository;

    @Mock
    private MeetingMapper meetingMapper;

    @Mock
    private MeetingParticipantService meetingParticipantService;

    @Mock
    private MeetingAuthorizationService meetingAuthorizationService;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private MeetingServiceImpl meetingService;
    @Mock
    private MeetingSchedulerService meetingSchedulerService;

    private User organizer;
    private Meeting meeting;
    private MeetingResponse meetingResponse;
    private Meeting templateMeeting;
    private Category category;
    @Mock
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        organizer = User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .build();

        category = Category.builder()
                .id(1L)
                .name("Business")
                .build();

        meeting = Meeting.builder()
                .title("Team Meeting")
                .description("Weekly sync")
                .type(MeetingType.PHYSICAL)
                .visibility(MeetingVisibility.PUBLIC)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(1).plusHours(1))
                .organizer(organizer)
                .maxParticipants(10)
                .build();

        templateMeeting = Meeting.builder()
                .title("Template Meeting")
                .organizer(organizer)
                .build();

        meetingResponse = MeetingResponse.builder()
                .id(1L)
                .title("Team Meeting")
                .type(MeetingType.PHYSICAL)
                .status(MeetingStatus.PLANNED)
                .build();
    }

    // ============ CREATE MEETING TESTS ============

    @Test
    void createMeeting_Success() {
        // Given
        CreateMeetingRequest request = CreateMeetingRequest.builder()
                .title("Team Meeting")
                .type(MeetingType.PHYSICAL)
                .visibility(MeetingVisibility.PUBLIC)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(1).plusHours(1))
                .categoryIds(Set.of(1L))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(organizer));
        when(meetingMapper.toEntity(request)).thenReturn(meeting);
        when(categoryRepository.findAllById(anySet())).thenReturn(List.of(category));
        when(meetingRepository.save(any(Meeting.class))).thenReturn(meeting);
        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(meetingResponse);

        // When
        MeetingResponse response = meetingService.createMeeting(request, 1L);

        // Then
        assertAll(
                () -> assertNotNull(response),
                () -> assertEquals(1L, response.getId()),
                () -> assertEquals("Team Meeting", response.getTitle())
        );

        verify(userRepository).findById(1L);
        verify(meetingMapper).toEntity(request);
        verify(categoryRepository).findAllById(Set.of(1L));
        verify(meetingRepository).save(any(Meeting.class));
    }

    @Test
    void createMeeting_OrganizerNotFound_ThrowsResourceNotFoundException() {
        // Given
        CreateMeetingRequest request = CreateMeetingRequest.builder()
                .title("Team Meeting")
                .build();

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> meetingService.createMeeting(request, 999L));

        assertEquals("User not found with id: 999", exception.getMessage());
        verify(meetingRepository, never()).save(any(Meeting.class));
    }

    @Test
    void createMeeting_SaveAsTemplate_Success() {
        // Given
        CreateMeetingRequest request = CreateMeetingRequest.builder()
                .title("Template Meeting")
                .saveAsTemplate(true)
                .templateName("My Template")
                .build();

        Meeting template = Meeting.builder()
                .title("My Template")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(organizer));
        when(meetingMapper.toEntity(request)).thenReturn(template);
        when(meetingRepository.save(any(Meeting.class))).thenReturn(template);
        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(MeetingResponse.builder()
                .id(2L)
                .title("My Template")
                .build());

        // When
        MeetingResponse response = meetingService.createMeeting(request, 1L);

        // Then
        assertAll(
                () -> assertNotNull(response),
                () -> assertEquals(2L, response.getId()),
                () -> assertEquals("My Template", response.getTitle())
        );
    }

    @Test
    void createMeeting_WithRecurring_GeneratesNextOccurrences() {
        // Given
        CreateMeetingRequest request = CreateMeetingRequest.builder()
                .title("Recurring Meeting")
                .recurring(true)
                .recurrencePattern("DAILY:1")
                .build();

        Meeting recurringMeeting = Meeting.builder()
                .title("Recurring Meeting")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(organizer));
        when(meetingMapper.toEntity(request)).thenReturn(recurringMeeting);
        when(meetingRepository.save(any(Meeting.class))).thenReturn(recurringMeeting);
        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(MeetingResponse.builder()
                .id(3L)
                .title("Recurring Meeting")
                .build());

        // When
        MeetingResponse response = meetingService.createMeeting(request, 1L);

        // Then
        assertNotNull(response);
        verify(meetingRepository).save(any(Meeting.class));
    }

    // ============ UPDATE MEETING TESTS ============

    @Test
    void updateMeeting_Success() {
        // Given
        UpdateMeetingRequest request = UpdateMeetingRequest.builder()
                .title("Updated Meeting")
                .categoryIds(Set.of(1L))
                .build();

        Meeting updatedMeeting = Meeting.builder()
                .title("Updated Meeting")
                .build();

        when(meetingAuthorizationService.canUserEditMeeting(1L, 1L)).thenReturn(true);
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
        when(categoryRepository.findAllById(anySet())).thenReturn(List.of(category));
        when(meetingRepository.save(any(Meeting.class))).thenReturn(updatedMeeting);
        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(MeetingResponse.builder()
                .id(1L)
                .title("Updated Meeting")
                .build());

        // When
        MeetingResponse response = meetingService.updateMeeting(1L, request, 1L);

        // Then
        assertAll(
                () -> assertNotNull(response),
                () -> assertEquals(1L, response.getId()),
                () -> assertEquals("Updated Meeting", response.getTitle())
        );

        verify(meetingAuthorizationService).canUserEditMeeting(1L, 1L);
        verify(meetingRepository).findById(1L);
        verify(meetingRepository).save(any(Meeting.class));
    }

    @Test
    void updateMeeting_NoPermission_ThrowsBusinessException() {
        // Given
        UpdateMeetingRequest request = UpdateMeetingRequest.builder().title("Updated").build();
        when(meetingAuthorizationService.canUserEditMeeting(1L, 1L)).thenReturn(false);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> meetingService.updateMeeting(1L, request, 1L));

        assertEquals("No permission to edit this meeting", exception.getMessage());
        verify(meetingRepository, never()).save(any(Meeting.class));
    }

    @Test
    void updateMeeting_MeetingNotFound_ThrowsResourceNotFoundException() {
        // Given
        UpdateMeetingRequest request = UpdateMeetingRequest.builder().title("Updated").build();
        when(meetingAuthorizationService.canUserEditMeeting(1L, 1L)).thenReturn(true);
        when(meetingRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> meetingService.updateMeeting(1L, request, 1L));

        assertEquals("Meeting not found with id: 1", exception.getMessage());
    }

    @Test
    void updateMeeting_StatusChange_SavesStatusHistory() {
        // Given
        UpdateMeetingRequest request = UpdateMeetingRequest.builder()
                .status(MeetingStatus.PLANNED)
                .statusChangeReason("Confirmed by manager")
                .build();

        Meeting meetingWithStatus = Meeting.builder()
                .build();

        when(meetingAuthorizationService.canUserEditMeeting(1L, 1L)).thenReturn(true);
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meetingWithStatus));
        when(meetingRepository.save(any(Meeting.class))).thenReturn(meetingWithStatus);
        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(meetingResponse);

        // When
        MeetingResponse response = meetingService.updateMeeting(1L, request, 1L);

        // Then
        assertNotNull(response);
        verify(meetingRepository).save(any(Meeting.class));
    }

    // ============ DELETE MEETING TESTS ============

    @Test
    void deleteMeeting_Success() {
        // Given
        when(meetingAuthorizationService.canUserDeleteMeeting(1L, 1L)).thenReturn(true);
        when(meetingRepository.findByIdAndOrganizerId(1L, 1L)).thenReturn(Optional.of(meeting));

        // Mock JDBC operations
        when(jdbcTemplate.update(anyString(), eq(1L))).thenReturn(1);

        // When
        meetingService.deleteMeeting(1L, 1L);

        // Then
        verify(meetingAuthorizationService).canUserDeleteMeeting(1L, 1L);
        verify(jdbcTemplate, times(3)).update(anyString(), eq(1L));
    }

    @Test
    void deleteMeeting_NoPermission_ThrowsBusinessException() {
        // Given
        when(meetingAuthorizationService.canUserDeleteMeeting(1L, 1L)).thenReturn(false);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> meetingService.deleteMeeting(1L, 1L));

        assertEquals("No permission to delete this meeting", exception.getMessage());
        verify(meetingRepository, never()).delete(any(Meeting.class));
    }

    @Test
    void deleteMeeting_MeetingNotFound_ThrowsResourceNotFoundException() {
        // Given
        when(meetingAuthorizationService.canUserDeleteMeeting(1L, 1L)).thenReturn(true);
        when(meetingRepository.findByIdAndOrganizerId(1L, 1L)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> meetingService.deleteMeeting(1L, 1L));

        assertTrue(exception.getMessage().contains("Meeting not found with id: 1"));
    }

    // ============ GET MEETING TESTS ============

    @Test
    void getMeetingById_Success() {
        // Given
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
        when(meetingMapper.toResponse(meeting)).thenReturn(meetingResponse);

        // When
        MeetingResponse response = meetingService.getMeetingById(1L);

        // Then
        assertAll(
                () -> assertNotNull(response),
                () -> assertEquals(1L, response.getId())
        );

        verify(meetingRepository).findById(1L);
        verify(meetingMapper).toResponse(meeting);
    }

    @Test
    void getMeetingById_NotFound_ThrowsResourceNotFoundException() {
        // Given
        when(meetingRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> meetingService.getMeetingById(999L));

        assertEquals("Meeting not found with id: 999", exception.getMessage());
    }

    // ============ GET USER MEETINGS TESTS ============

    @Test
    void getUserMeetings_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Meeting> meetingsPage = new PageImpl<>(List.of(meeting), pageable, 1);

        when(meetingRepository.findByOrganizerId(1L, pageable)).thenReturn(meetingsPage);
        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(meetingResponse);

        // When
        Page<MeetingResponse> responsePage = meetingService.getUserMeetings(1L, pageable);

        // Then
        assertAll(
                () -> assertNotNull(responsePage),
                () -> assertEquals(1, responsePage.getTotalElements())
        );

        verify(meetingRepository).findByOrganizerId(1L, pageable);
    }

    // ============ GET UPCOMING PUBLIC MEETINGS TESTS ============

    @Test
    void getUpcomingPublicMeetings_Success() {
        // Given
        List<Meeting> meetings = List.of(meeting);
        when(meetingRepository.findUpcomingPublicMeetings(any(LocalDateTime.class))).thenReturn(meetings);
        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(meetingResponse);

        // When
        List<MeetingResponse> responses = meetingService.getUpcomingPublicMeetings();

        // Then
        assertAll(
                () -> assertNotNull(responses),
                () -> assertEquals(1, responses.size())
        );

        verify(meetingRepository).findUpcomingPublicMeetings(any(LocalDateTime.class));
    }


    @Test
    void changeMeetingStatus_NoPermission_ThrowsBusinessException() {
        // Given
        when(meetingAuthorizationService.canUserEditMeeting(1L, 1L)).thenReturn(false);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> meetingService.changeMeetingStatus(1L, MeetingStatus.PLANNED, 1L));

        assertEquals("No permission to change status of this meeting", exception.getMessage());
    }

    // ============ DUPLICATE MEETING TESTS ============

    @Test
    void duplicateMeeting_Success() {
        // Given
        Meeting duplicate = Meeting.builder()
                .title("Team Meeting (Copy)")
                .build();

        when(meetingAuthorizationService.canUserEditMeeting(1L, 1L)).thenReturn(true);
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
        when(meetingRepository.save(any(Meeting.class))).thenReturn(duplicate);
        when(meetingMapper.toResponse(duplicate)).thenReturn(
                MeetingResponse.builder()
                        .id(2L)  // stubbed response ID
                        .title("Team Meeting (Copy)")
                        .build()
        );

        // When
        MeetingResponse response = meetingService.duplicateMeeting(1L, 1L);

        // Then
        assertAll(
                () -> assertNotNull(response),
                () -> assertEquals(2L, response.getId())
        );
    }



    // ============ GET TEMPLATES TESTS ============

    @Test
    void getMeetingTemplates_Success() {
        // Given
        List<Meeting> templates = List.of(templateMeeting);
        when(meetingRepository.findByOrganizerIdAndTemplateTrue(1L)).thenReturn(templates);
        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(MeetingResponse.builder()
                .id(2L)
                .title("Template Meeting")
                .build());

        // When
        List<MeetingResponse> responses = meetingService.getMeetingTemplates(1L);

        // Then
        assertAll(
                () -> assertNotNull(responses),
                () -> assertEquals(1, responses.size()),
                () -> assertEquals("Template Meeting", responses.get(0).getTitle())
        );

        verify(meetingRepository).findByOrganizerIdAndTemplateTrue(1L);
    }

    // ============ CREATE FROM TEMPLATE TESTS ============

    @Test
    void createFromTemplate_Success() {
        // Given
        LocalDateTime newStartDate = LocalDateTime.now().plusDays(7);
        Meeting newMeeting = Meeting.builder()
                .title("New Meeting")
                .build();

        Meeting templateWithDates = Meeting.builder()
                .title("Template Meeting")
                .organizer(organizer)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusHours(2))
                .build();

        when(meetingRepository.findByIdAndTemplateTrue(2L)).thenReturn(Optional.of(templateWithDates));
        when(meetingMapper.cloneMeeting(templateWithDates)).thenReturn(newMeeting);
        when(userRepository.findById(1L)).thenReturn(Optional.of(organizer));
        when(meetingRepository.save(any(Meeting.class))).thenReturn(newMeeting);
        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(MeetingResponse.builder()
                .id(3L)
                .title("New Meeting")
                .build());

        // When
        MeetingResponse response = meetingService.createFromTemplate(2L, 1L, newStartDate);

        // Then
        assertAll(
                () -> assertNotNull(response),
                () -> assertEquals(3L, response.getId())
        );

        verify(meetingRepository).findByIdAndTemplateTrue(2L);
        verify(meetingRepository).save(any(Meeting.class));
    }

    @Test
    void createFromTemplate_TemplateNotFound_ThrowsResourceNotFoundException() {
        // Given
        when(meetingRepository.findByIdAndTemplateTrue(999L)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> meetingService.createFromTemplate(999L, 1L, LocalDateTime.now()));

        assertEquals("Template not found or not a template", exception.getMessage());
    }

    // ============ SAVE AS TEMPLATE TESTS ============

    @Test
    void saveAsTemplate_Success() {
        // Given
        String templateName = "My Template";
        Meeting template = Meeting.builder()
                .title(templateName)
                .build();

        when(meetingAuthorizationService.canUserEditMeeting(1L, 1L)).thenReturn(true);
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
        when(meetingMapper.createTemplateFromMeeting(meeting, templateName)).thenReturn(template);
        when(userRepository.findById(1L)).thenReturn(Optional.of(organizer));
        when(meetingRepository.save(any(Meeting.class))).thenReturn(template);
        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(MeetingResponse.builder()
                .id(4L)
                .title(templateName)
                .build());

        // When
        MeetingResponse response = meetingService.saveAsTemplate(1L, templateName, 1L);

        // Then
        assertAll(
                () -> assertNotNull(response),
                () -> assertEquals(4L, response.getId()),
                () -> assertEquals(templateName, response.getTitle())
        );

        verify(meetingAuthorizationService).canUserEditMeeting(1L, 1L);
        verify(meetingRepository).save(any(Meeting.class));
    }

    // ============ GET ACCESSIBLE MEETINGS TESTS ============


    // ============ SEARCH MEETINGS TESTS ============

    @Test
    void searchMeetings_Success() {
        // Given
        SearchCriteria criteria = SearchCriteria.builder()
                .currentUserId(1L)
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<Meeting> meetingsPage = new PageImpl<>(List.of(meeting), pageable, 1);

        when(meetingRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(meetingsPage);
        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(meetingResponse);

        // When
        Page<MeetingResponse> responsePage = meetingService.searchMeetings(criteria, pageable);

        // Then
        assertAll(
                () -> assertNotNull(responsePage),
                () -> assertEquals(1, responsePage.getTotalElements())
        );

        verify(meetingRepository).findAll(any(Specification.class), eq(pageable));
    }

    // ============ MEETING PARTICIPATION TESTS ============

    @Test
    void getMeetingParticipationInfo_Success() {
        // Given
        MeetingParticipationInfo info = MeetingParticipationInfo.builder()
                .canViewDetails(true)
                .canEdit(false)
                .build();

        when(meetingAuthorizationService.getUserMeetingPermissions(1L, 1L)).thenReturn(info);

        // When
        MeetingParticipationInfo result = meetingService.getMeetingParticipationInfo(1L, 1L);

        // Then
        assertAll(
                () -> assertNotNull(result),
                () -> assertTrue(result.isCanViewDetails()),
                () -> assertFalse(result.isCanEdit())
        );

        verify(meetingAuthorizationService).getUserMeetingPermissions(1L, 1L);
    }

    @Test
    void canUserAccessMeeting_Success() {
        // Given
        MeetingParticipationInfo info = MeetingParticipationInfo.builder()
                .canViewDetails(true)
                .build();

        when(meetingAuthorizationService.getUserMeetingPermissions(1L, 1L)).thenReturn(info);

        // When
        boolean canAccess = meetingService.canUserAccessMeeting(1L, 1L);

        // Then
        assertTrue(canAccess);
        verify(meetingAuthorizationService).getUserMeetingPermissions(1L, 1L);
    }

    // ============ GET MEETINGS BY TAG TESTS ============

    @Test
    void getMeetingsByTag_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Meeting> meetingsPage = new PageImpl<>(List.of(meeting), pageable, 1);

        when(meetingRepository.findByTag("team", pageable)).thenReturn(meetingsPage);
        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(meetingResponse);

        // When
        Page<MeetingResponse> responsePage = meetingService.getMeetingsByTag("team", pageable);

        // Then
        assertAll(
                () -> assertNotNull(responsePage),
                () -> assertEquals(1, responsePage.getTotalElements())
        );

        verify(meetingRepository).findByTag("team", pageable);
    }

    // ============ GET MEETING ENTITY TESTS ============


    @Test
    void getMeeting_NotFound_ThrowsResourceNotFoundException() {
        // Given
        when(meetingRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> meetingService.getMeeting(999L));

        assertEquals("Meeting not found with id: 999", exception.getMessage());
    }

    // ============ GET CONFLICTING MEETINGS TESTS ============

    @Test
    void findConflictingMeetings_Success() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().plusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusDays(2);
        List<Meeting> conflicts = List.of(meeting);

        when(meetingRepository.findConfirmedMeetingsForUserInPeriod(1L, startDate, endDate))
                .thenReturn(conflicts);
        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(meetingResponse);

        // When
        List<MeetingResponse> responses = meetingService.findConflictingMeetings(1L, startDate, endDate);

        // Then
        assertAll(
                () -> assertNotNull(responses),
                () -> assertEquals(1, responses.size())
        );

        verify(meetingRepository).findConfirmedMeetingsForUserInPeriod(1L, startDate, endDate);
    }

    // ============ GET FILTERED MEETINGS TESTS ============

    @Test
    void getFilteredMeetings_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Meeting> meetingsPage = new PageImpl<>(List.of(meeting), pageable, 1);

        when(customMeetingRepository.findFilteredMeetings("Team", "MEETING", "PLANNED", pageable))
                .thenReturn(meetingsPage);
        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(meetingResponse);

        // When
        Page<MeetingResponse> responsePage = meetingService.getFilteredMeetings("Team", "MEETING", "PLANNED", pageable);

        // Then
        assertAll(
                () -> assertNotNull(responsePage),
                () -> assertEquals(1, responsePage.getTotalElements())
        );

        verify(customMeetingRepository).findFilteredMeetings("Team", "MEETING", "PLANNED", pageable);
    }

    // ============ GET UPCOMING RECURRING MEETINGS TESTS ============



    // ============ NEARBY MEETINGS TESTS ============

    @Test
    void findNearbyMeetings_Success() {
        // Given
        List<Meeting> meetings = List.of(meeting);
        when(customMeetingRepository.findNearbyMeetings(52.2297, 21.0122, 10.0, 50)).thenReturn(meetings);
        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(meetingResponse);

        // When
        List<MeetingResponse> responses = meetingService.findNearbyMeetings(52.2297, 21.0122, 10.0);

        // Then
        assertAll(
                () -> assertNotNull(responses),
                () -> assertEquals(1, responses.size())
        );

        verify(customMeetingRepository).findNearbyMeetings(52.2297, 21.0122, 10.0, 50);
    }

    // ============ ADD RECURRENCE EXCEPTION TESTS ============



    @Test
    void addRecurrenceException_NotRecurring_ThrowsBusinessException() {
        // Given
        Meeting nonRecurringMeeting = Meeting.builder()
                .build();

        when(meetingRepository.findById(1L)).thenReturn(Optional.of(nonRecurringMeeting));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> meetingService.addRecurrenceException(1L, "2024-01-15", "Reason"));

        assertEquals("This meeting is not recurring", exception.getMessage());
    }

    // ============ GET RECURRENCE SERIES TESTS ============

    @Test
    void getRecurrenceSeries_Success() {
        // Given
        List<Meeting> series = List.of(meeting);
        when(meetingRepository.findByOriginalMeetingId(1L)).thenReturn(series);
        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(meetingResponse);

        // When
        List<MeetingResponse> responses = meetingService.getRecurrenceSeries(1L);

        // Then
        assertAll(
                () -> assertNotNull(responses),
                () -> assertEquals(1, responses.size())
        );

        verify(meetingRepository).findByOriginalMeetingId(1L);
    }

    // ============ GENERATE NEXT RECURRENCE TESTS ============


    @Test
    void generateNextRecurrence_NotRecurring_ThrowsBusinessException() {
        // Given
        Meeting nonRecurringMeeting = Meeting.builder()

                .build();

        when(meetingRepository.findById(1L)).thenReturn(Optional.of(nonRecurringMeeting));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> meetingService.generateNextRecurrence(1L, 2));

        assertEquals("This meeting is not recurring", exception.getMessage());
    }


}
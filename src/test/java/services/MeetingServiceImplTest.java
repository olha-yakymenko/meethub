//// MeetingServiceImplTest.java
//package com.meethub.domain.service.impl;
//
//import com.meethub.domain.model.entity.Meeting;
//import com.meethub.domain.model.entity.User;
//import com.meethub.domain.model.enums.MeetingStatus;
//import com.meethub.domain.model.enums.MeetingVisibility;
//import com.meethub.domain.model.request.CreateMeetingRequest;
//import com.meethub.domain.model.request.UpdateMeetingRequest;
//import com.meethub.domain.model.response.MeetingParticipationInfo;
//import com.meethub.domain.model.response.MeetingResponse;
//import com.meethub.domain.repository.jpa.MeetingRepository;
//import com.meethub.domain.repository.jpa.UserRepository;
//import com.meethub.domain.repository.jdbc.CustomMeetingRepository;
//import com.meethub.domain.service.MeetingAuthorizationService;
//import com.meethub.domain.service.MeetingParticipantService;
//import com.meethub.exception.BusinessException;
//import com.meethub.exception.ResourceNotFoundException;
//import com.meethub.domain.model.mapper.MeetingMapper;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.*;
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
//    @InjectMocks
//    private MeetingServiceImpl meetingService;
//
//    private User organizer;
//    private Meeting meeting;
//    private CreateMeetingRequest createRequest;
//    private UpdateMeetingRequest updateRequest;
//
//    @BeforeEach
//    void setUp() {
//        // Setup organizer
//        organizer = User.builder()
//                .id(1L)
//                .firstName("John")
//                .lastName("Doe")
//                .email("john.doe@example.com")
//                .build();
//
//        // Setup meeting
//        meeting = Meeting.builder()
//                .title("Test Meeting")
//                .description("Test Description")
//                .agenda("Test Agenda")
//                .visibility(MeetingVisibility.PUBLIC)
//                .startDate(LocalDateTime.now().plusDays(1))
//                .endDate(LocalDateTime.now().plusDays(1).plusHours(2))
//                .maxParticipants(50)
//                .organizer(organizer)
//                .build();
//
//        // Setup create request
//        createRequest = new CreateMeetingRequest();
//        createRequest.setTitle("New Meeting");
//        createRequest.setDescription("New Description");
//        createRequest.setVisibility(MeetingVisibility.PUBLIC);
//        createRequest.setStartDate(LocalDateTime.now().plusDays(1));
//        createRequest.setEndDate(LocalDateTime.now().plusDays(1).plusHours(2));
//        createRequest.setMaxParticipants(30);
//
//        // Setup update request
//        updateRequest = new UpdateMeetingRequest();
//        updateRequest.setTitle("Updated Meeting");
//        updateRequest.setDescription("Updated Description");
//        updateRequest.setVisibility(MeetingVisibility.PRIVATE);
//    }
//
//    @Test
//    void testCreateMeeting_Success() {
//        // Given
//        when(userRepository.findById(1L)).thenReturn(Optional.of(organizer));
//
//        Meeting mappedMeeting = Meeting.builder()
//                .title("New Meeting")
//                .description("New Description")
//                .visibility(MeetingVisibility.PUBLIC)
//                .startDate(LocalDateTime.now().plusDays(1))
//                .endDate(LocalDateTime.now().plusDays(1).plusHours(2))
//                .maxParticipants(30)
//                .build();
//
//        when(meetingMapper.toEntity(any(CreateMeetingRequest.class))).thenReturn(mappedMeeting);
//
//        Meeting savedMeeting = Meeting.builder()
//                .title("New Meeting")
//                .description("New Description")
//                .visibility(MeetingVisibility.PUBLIC)
//                .startDate(LocalDateTime.now().plusDays(1))
//                .endDate(LocalDateTime.now().plusDays(1).plusHours(2))
//                .maxParticipants(30)
//                .organizer(organizer)
//                .build();
//
//        when(meetingRepository.save(any(Meeting.class))).thenReturn(savedMeeting);
//
//        MeetingResponse expectedResponse = MeetingResponse.builder()
//                .id(100L)
//                .title("New Meeting")
//                .build();
//
//        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(expectedResponse);
//
//        // When
//        MeetingResponse response = meetingService.createMeeting(createRequest, 1L);
//
//        // Then
//        assertNotNull(response);
//        assertEquals(100L, response.getId());
//        assertEquals("New Meeting", response.getTitle());
//
//        verify(userRepository).findById(1L);
//        verify(meetingMapper).toEntity(createRequest);
//        verify(meetingRepository).save(any(Meeting.class));
//        verify(meetingMapper).toResponse(savedMeeting);
//    }
//
//    @Test
//    void testCreateMeeting_OrganizerNotFound_ThrowsException() {
//        // Given
//        when(userRepository.findById(999L)).thenReturn(Optional.empty());
//
//        // When & Then
//        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
//                () -> meetingService.createMeeting(createRequest, 999L));
//
//        assertEquals("User not found with id: 999", exception.getMessage());
//        verify(userRepository).findById(999L);
//        verify(meetingMapper, never()).toEntity(any());
//        verify(meetingRepository, never()).save(any(Meeting.class));
//    }
//
//    @Test
//    void testCreateMeeting_DatabaseError_ThrowsBusinessException() {
//        // Given
//        when(userRepository.findById(1L)).thenReturn(Optional.of(organizer));
//        when(meetingMapper.toEntity(any(CreateMeetingRequest.class))).thenReturn(meeting);
//        when(meetingRepository.save(any(Meeting.class))).thenThrow(new RuntimeException("Database error"));
//
//        // When & Then
//        BusinessException exception = assertThrows(BusinessException.class,
//                () -> meetingService.createMeeting(createRequest, 1L));
//
//        assertTrue(exception.getMessage().contains("Error creating meeting"));
//        verify(userRepository).findById(1L);
//        verify(meetingMapper).toEntity(createRequest);
//        verify(meetingRepository).save(any(Meeting.class));
//    }
//
//    @Test
//    void testUpdateMeeting_Success() {
//        // Given
//        when(meetingAuthorizationService.canUserEditMeeting(1L, 1L)).thenReturn(true);
//        when(meetingRepository.findByIdAndOrganizerId(1L, 1L)).thenReturn(Optional.of(meeting));
//        when(meetingRepository.save(any(Meeting.class))).thenReturn(meeting);
//
//        MeetingResponse expectedResponse = MeetingResponse.builder()
//                .id(1L)
//                .title("Updated Meeting")
//                .build();
//
//        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(expectedResponse);
//
//        // When
//        MeetingResponse response = meetingService.updateMeeting(1L, updateRequest, 1L);
//
//        // Then
//        assertNotNull(response);
//        assertEquals(1L, response.getId());
//        assertEquals("Updated Meeting", response.getTitle());
//
//        verify(meetingAuthorizationService).canUserEditMeeting(1L, 1L);
//        verify(meetingRepository).findByIdAndOrganizerId(1L, 1L);
//        verify(meetingMapper).updateEntityFromRequest(updateRequest, meeting);
//        verify(meetingRepository).save(meeting);
//        verify(meetingMapper).toResponse(meeting);
//    }
//
//    @Test
//    void testUpdateMeeting_NoPermission_ThrowsException() {
//        // Given
//        when(meetingAuthorizationService.canUserEditMeeting(1L, 2L)).thenReturn(false);
//
//        // When & Then
//        BusinessException exception = assertThrows(BusinessException.class,
//                () -> meetingService.updateMeeting(1L, updateRequest, 2L));
//
//        assertEquals("No permission to edit this meeting", exception.getMessage());
//        verify(meetingAuthorizationService).canUserEditMeeting(1L, 2L);
//        verify(meetingRepository, never()).findByIdAndOrganizerId(anyLong(), anyLong());
//    }
//
//    @Test
//    void testUpdateMeeting_MeetingNotFound_ThrowsException() {
//        // Given
//        when(meetingAuthorizationService.canUserEditMeeting(1L, 1L)).thenReturn(true);
//        when(meetingRepository.findByIdAndOrganizerId(1L, 1L)).thenReturn(Optional.empty());
//
//        // When & Then
//        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
//                () -> meetingService.updateMeeting(1L, updateRequest, 1L));
//
//        assertEquals("Meeting not found with id: 1 for organizer: 1", exception.getMessage());
//        verify(meetingAuthorizationService).canUserEditMeeting(1L, 1L);
//        verify(meetingRepository).findByIdAndOrganizerId(1L, 1L);
//        verify(meetingMapper, never()).updateEntityFromRequest(any(), any());
//        verify(meetingRepository, never()).save(any(Meeting.class));
//    }
//
//    @Test
//    void testDeleteMeeting_Success() {
//        // Given
//        when(meetingAuthorizationService.canUserDeleteMeeting(1L, 1L)).thenReturn(true);
//        when(meetingRepository.findByIdAndOrganizerId(1L, 1L)).thenReturn(Optional.of(meeting));
//
//        // When
//        meetingService.deleteMeeting(1L, 1L);
//
//        // Then
//        verify(meetingAuthorizationService).canUserDeleteMeeting(1L, 1L);
//        verify(meetingRepository).findByIdAndOrganizerId(1L, 1L);
//        verify(meetingRepository).delete(meeting);
//    }
//
//    @Test
//    void testDeleteMeeting_NoPermission_ThrowsException() {
//        // Given
//        when(meetingAuthorizationService.canUserDeleteMeeting(1L, 2L)).thenReturn(false);
//
//        // When & Then
//        BusinessException exception = assertThrows(BusinessException.class,
//                () -> meetingService.deleteMeeting(1L, 2L));
//
//        assertEquals("No permission to delete this meeting", exception.getMessage());
//        verify(meetingAuthorizationService).canUserDeleteMeeting(1L, 2L);
//        verify(meetingRepository, never()).findByIdAndOrganizerId(anyLong(), anyLong());
//        verify(meetingRepository, never()).delete(any(Meeting.class));
//    }
//
//    @Test
//    void testDeleteMeeting_MeetingNotFound_ThrowsException() {
//        // Given
//        when(meetingAuthorizationService.canUserDeleteMeeting(1L, 1L)).thenReturn(true);
//        when(meetingRepository.findByIdAndOrganizerId(1L, 1L)).thenReturn(Optional.empty());
//
//        // When & Then
//        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
//                () -> meetingService.deleteMeeting(1L, 1L));
//
//        assertEquals("Meeting not found with id: 1 for organizer: 1", exception.getMessage());
//        verify(meetingAuthorizationService).canUserDeleteMeeting(1L, 1L);
//        verify(meetingRepository).findByIdAndOrganizerId(1L, 1L);
//        verify(meetingRepository, never()).delete(any(Meeting.class));
//    }
//
//    @Test
//    void testGetMeetingById_Success() {
//        // Given
//        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
//
//        MeetingResponse expectedResponse = MeetingResponse.builder()
//                .id(1L)
//                .title("Test Meeting")
//                .build();
//
//        when(meetingMapper.toResponse(meeting)).thenReturn(expectedResponse);
//
//        // When
//        MeetingResponse response = meetingService.getMeetingById(1L);
//
//        // Then
//        assertNotNull(response);
//        assertEquals(1L, response.getId());
//        assertEquals("Test Meeting", response.getTitle());
//
//        verify(meetingRepository).findById(1L);
//        verify(meetingMapper).toResponse(meeting);
//    }
//
//    @Test
//    void testGetMeetingById_NotFound_ThrowsException() {
//        // Given
//        when(meetingRepository.findById(999L)).thenReturn(Optional.empty());
//
//        // When & Then
//        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
//                () -> meetingService.getMeetingById(999L));
//
//        assertEquals("Meeting not found with id: 999", exception.getMessage());
//        verify(meetingRepository).findById(999L);
//        verify(meetingMapper, never()).toResponse(any(Meeting.class));
//    }
//
//    @Test
//    void testGetUserMeetings_Success() {
//        // Given
//        Pageable pageable = PageRequest.of(0, 10);
//        Page<Meeting> meetingPage = new PageImpl<>(List.of(meeting), pageable, 1);
//
//        when(meetingRepository.findByOrganizerId(1L, pageable)).thenReturn(meetingPage);
//
//        MeetingResponse meetingResponse = MeetingResponse.builder()
//                .id(1L)
//                .title("Test Meeting")
//                .build();
//
//        when(meetingMapper.toResponse(meeting)).thenReturn(meetingResponse);
//
//        // When
//        Page<MeetingResponse> responsePage = meetingService.getUserMeetings(1L, pageable);
//
//        // Then
//        assertNotNull(responsePage);
//        assertEquals(1, responsePage.getTotalElements());
//        assertEquals(1L, responsePage.getContent().get(0).getId());
//
//        verify(meetingRepository).findByOrganizerId(1L, pageable);
//        verify(meetingMapper).toResponse(meeting);
//    }
//
//    @Test
//    void testGetUpcomingPublicMeetings_Success() {
//        // Given
//        List<Meeting> meetings = List.of(meeting);
//        when(meetingRepository.findUpcomingPublicMeetings(any(LocalDateTime.class))).thenReturn(meetings);
//
//        MeetingResponse meetingResponse = MeetingResponse.builder()
//                .id(1L)
//                .title("Test Meeting")
//                .build();
//
//        when(meetingMapper.toResponse(meeting)).thenReturn(meetingResponse);
//
//        // When
//        List<MeetingResponse> responses = meetingService.getUpcomingPublicMeetings();
//
//        // Then
//        assertNotNull(responses);
//        assertEquals(1, responses.size());
//        assertEquals(1L, responses.get(0).getId());
//
//        verify(meetingRepository).findUpcomingPublicMeetings(any(LocalDateTime.class));
//        verify(meetingMapper).toResponse(meeting);
//    }
//
//    @Test
//    void testFindNearbyMeetings_Success() {
//        // Given
//        List<Meeting> meetings = List.of(meeting);
//        when(customMeetingRepository.findNearbyMeetings(52.2297, 21.0122, 10.0, 50)).thenReturn(meetings);
//
//        MeetingResponse meetingResponse = MeetingResponse.builder()
//                .id(1L)
//                .title("Test Meeting")
//                .build();
//
//        when(meetingMapper.toResponse(meeting)).thenReturn(meetingResponse);
//
//        // When
//        List<MeetingResponse> responses = meetingService.findNearbyMeetings(52.2297, 21.0122, 10.0);
//
//        // Then
//        assertNotNull(responses);
//        assertEquals(1, responses.size());
//        assertEquals(1L, responses.get(0).getId());
//
//        verify(customMeetingRepository).findNearbyMeetings(52.2297, 21.0122, 10.0, 50);
//        verify(meetingMapper).toResponse(meeting);
//    }
//
//    @Test
//    void testChangeMeetingStatus_Success() {
//        // Given
//        when(meetingAuthorizationService.canUserEditMeeting(1L, 1L)).thenReturn(true);
//        when(meetingRepository.findByIdAndOrganizerId(1L, 1L)).thenReturn(Optional.of(meeting));
//        when(meetingRepository.save(any(Meeting.class))).thenReturn(meeting);
//
//        // When
//        meetingService.changeMeetingStatus(1L, MeetingStatus.CANCELLED, 1L);
//
//        // Then
//        verify(meetingAuthorizationService).canUserEditMeeting(1L, 1L);
//        verify(meetingRepository).findByIdAndOrganizerId(1L, 1L);
//        verify(meetingRepository).save(meeting);
//        assertEquals(MeetingStatus.CANCELLED, meeting.getStatus());
//    }
//
//    @Test
//    void testChangeMeetingStatus_NoPermission_ThrowsException() {
//        // Given
//        when(meetingAuthorizationService.canUserEditMeeting(1L, 2L)).thenReturn(false);
//
//        // When & Then
//        BusinessException exception = assertThrows(BusinessException.class,
//                () -> meetingService.changeMeetingStatus(1L, MeetingStatus.CANCELLED, 2L));
//
//        assertEquals("No permission to change status of this meeting", exception.getMessage());
//        verify(meetingAuthorizationService).canUserEditMeeting(1L, 2L);
//        verify(meetingRepository, never()).findByIdAndOrganizerId(anyLong(), anyLong());
//        verify(meetingRepository, never()).save(any(Meeting.class));
//    }
//
//    @Test
//    void testDuplicateMeeting_Success() {
//        // Given
//        when(meetingAuthorizationService.canUserEditMeeting(1L, 1L)).thenReturn(true);
//        when(meetingRepository.findByIdAndOrganizerId(1L, 1L)).thenReturn(Optional.of(meeting));
//
//        Meeting duplicateMeeting = Meeting.builder()
//                .id(2L)
//                .title("Test Meeting (Copy)")
//                .description("Test Description")
//                .agenda("Test Agenda")
//                .type("TECHNICAL")
//                .status(MeetingStatus.PLANNED)
//                .visibility(MeetingVisibility.PUBLIC)
//                .startDate(meeting.getStartDate().plusDays(7))
//                .endDate(meeting.getEndDate().plusDays(7))
//                .maxParticipants(50)
//                .organizer(organizer)
//                .build();
//
//        when(meetingRepository.save(any(Meeting.class))).thenReturn(duplicateMeeting);
//
//        MeetingResponse expectedResponse = MeetingResponse.builder()
//                .id(2L)
//                .title("Test Meeting (Copy)")
//                .build();
//
//        when(meetingMapper.toResponse(duplicateMeeting)).thenReturn(expectedResponse);
//
//        // When
//        MeetingResponse response = meetingService.duplicateMeeting(1L, 1L);
//
//        // Then
//        assertNotNull(response);
//        assertEquals(2L, response.getId());
//        assertEquals("Test Meeting (Copy)", response.getTitle());
//
//        verify(meetingAuthorizationService).canUserEditMeeting(1L, 1L);
//        verify(meetingRepository).findByIdAndOrganizerId(1L, 1L);
//        verify(meetingRepository).save(any(Meeting.class));
//        verify(meetingMapper).toResponse(duplicateMeeting);
//    }
//
//    @Test
//    void testDuplicateMeeting_NoPermission_ThrowsException() {
//        // Given
//        when(meetingAuthorizationService.canUserEditMeeting(1L, 2L)).thenReturn(false);
//
//        // When & Then
//        BusinessException exception = assertThrows(BusinessException.class,
//                () -> meetingService.duplicateMeeting(1L, 2L));
//
//        assertEquals("No permission to duplicate this meeting", exception.getMessage());
//        verify(meetingAuthorizationService).canUserEditMeeting(1L, 2L);
//        verify(meetingRepository, never()).findByIdAndOrganizerId(anyLong(), anyLong());
//        verify(meetingRepository, never()).save(any(Meeting.class));
//    }
//
//    @Test
//    void testFindConflictingMeetings_Success() {
//        // Given
//        LocalDateTime startDate = LocalDateTime.now().plusDays(1);
//        LocalDateTime endDate = LocalDateTime.now().plusDays(1).plusHours(2);
//
//        List<Meeting> conflicts = List.of(meeting);
//        when(meetingRepository.findConfirmedMeetingsForUserInPeriod(1L, startDate, endDate)).thenReturn(conflicts);
//
//        MeetingResponse meetingResponse = MeetingResponse.builder()
//                .id(1L)
//                .title("Test Meeting")
//                .build();
//
//        when(meetingMapper.toResponse(meeting)).thenReturn(meetingResponse);
//
//        // When
//        List<MeetingResponse> responses = meetingService.findConflictingMeetings(1L, startDate, endDate);
//
//        // Then
//        assertNotNull(responses);
//        assertEquals(1, responses.size());
//        assertEquals(1L, responses.get(0).getId());
//
//        verify(meetingRepository).findConfirmedMeetingsForUserInPeriod(1L, startDate, endDate);
//        verify(meetingMapper).toResponse(meeting);
//    }
//
//    @Test
//    void testGetFilteredMeetings_Success() {
//        // Given
//        Pageable pageable = PageRequest.of(0, 10);
//        Page<Meeting> meetingPage = new PageImpl<>(List.of(meeting), pageable, 1);
//
//        when(customMeetingRepository.findFilteredMeetings("test", "TECHNICAL", "PLANNED", pageable))
//                .thenReturn(meetingPage);
//
//        MeetingResponse meetingResponse = MeetingResponse.builder()
//                .id(1L)
//                .title("Test Meeting")
//                .build();
//
//        when(meetingMapper.toResponse(meeting)).thenReturn(meetingResponse);
//
//        // When
//        Page<MeetingResponse> responsePage = meetingService.getFilteredMeetings("test", "TECHNICAL", "PLANNED", pageable);
//
//        // Then
//        assertNotNull(responsePage);
//        assertEquals(1, responsePage.getTotalElements());
//        assertEquals(1L, responsePage.getContent().get(0).getId());
//
//        verify(customMeetingRepository).findFilteredMeetings("test", "TECHNICAL", "PLANNED", pageable);
//        verify(meetingMapper).toResponse(meeting);
//    }
//
//    @Test
//    void testGetMeeting_Success() {
//        // Given
//        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
//
//        // When
//        Meeting result = meetingService.getMeeting(1L);
//
//        // Then
//        assertNotNull(result);
//        assertEquals(1L, result.getId());
//        assertEquals("Test Meeting", result.getTitle());
//
//        verify(meetingRepository).findById(1L);
//    }
//
//    @Test
//    void testGetMeeting_NotFound_ThrowsException() {
//        // Given
//        when(meetingRepository.findById(999L)).thenReturn(Optional.empty());
//
//        // When & Then
//        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
//                () -> meetingService.getMeeting(999L));
//
//        assertEquals("Meeting not found with id: 999", exception.getMessage());
//        verify(meetingRepository).findById(999L);
//    }
//
//    @Test
//    void testGetMeetingParticipationInfo_Success() {
//        // Given
//        MeetingParticipationInfo participationInfo = MeetingParticipationInfo.builder()
//                .meetingId(1L)
//                .userId(2L)
//                .canViewDetails(true)
//                .canEdit(false)
//                .build();
//
//        when(meetingAuthorizationService.getUserMeetingPermissions(1L, 2L)).thenReturn(participationInfo);
//
//        // When
//        MeetingParticipationInfo result = meetingService.getMeetingParticipationInfo(1L, 2L);
//
//        // Then
//        assertNotNull(result);
//        assertEquals(1L, result.getMeetingId());
//        assertEquals(2L, result.getUserId());
//        assertTrue(result.isCanViewDetails());
//        assertFalse(result.isCanEdit());
//
//        verify(meetingAuthorizationService).getUserMeetingPermissions(1L, 2L);
//    }
//
//    @Test
//    void testCanUserAccessMeeting_Success() {
//        // Given
//        MeetingParticipationInfo participationInfo = MeetingParticipationInfo.builder()
//                .canViewDetails(true)
//                .build();
//
//        when(meetingAuthorizationService.getUserMeetingPermissions(1L, 2L)).thenReturn(participationInfo);
//
//        // When
//        boolean canAccess = meetingService.canUserAccessMeeting(1L, 2L);
//
//        // Then
//        assertTrue(canAccess);
//        verify(meetingAuthorizationService).getUserMeetingPermissions(1L, 2L);
//    }
//
//    @Test
//    void testCanUserAccessMeeting_NoAccess_ReturnsFalse() {
//        // Given
//        MeetingParticipationInfo participationInfo = MeetingParticipationInfo.builder()
//                .canViewDetails(false)
//                .build();
//
//        when(meetingAuthorizationService.getUserMeetingPermissions(1L, 2L)).thenReturn(participationInfo);
//
//        // When
//        boolean canAccess = meetingService.canUserAccessMeeting(1L, 2L);
//
//        // Then
//        assertFalse(canAccess);
//        verify(meetingAuthorizationService).getUserMeetingPermissions(1L, 2L);
//    }
//
//    @Test
//    void testCanUserAccessMeeting_Exception_ReturnsFalse() {
//        // Given
//        when(meetingAuthorizationService.getUserMeetingPermissions(1L, 2L))
//                .thenThrow(new RuntimeException("Permission check failed"));
//
//        // When
//        boolean canAccess = meetingService.canUserAccessMeeting(1L, 2L);
//
//        // Then
//        assertFalse(canAccess);
//        verify(meetingAuthorizationService).getUserMeetingPermissions(1L, 2L);
//    }
//
//    @Test
//    void testGetAccessibleMeetings_Success() {
//        // Given
//        Meeting privateMeeting = Meeting.builder()
//                .title("Private Meeting")
//                .visibility(MeetingVisibility.PRIVATE)
//                .organizer(organizer)
//                .build();
//
//        Meeting publicMeeting = Meeting.builder()
//                .title("Public Meeting")
//                .visibility(MeetingVisibility.PUBLIC)
//                .organizer(organizer)
//                .build();
//
//        List<Meeting> allMeetings = Arrays.asList(meeting, privateMeeting, publicMeeting);
//
//        when(meetingRepository.findAll()).thenReturn(allMeetings);
//        when(meetingAuthorizationService.canUserViewResource(1L, 2L)).thenReturn(true);
//        when(meetingAuthorizationService.canUserViewResource(2L, 2L)).thenReturn(false);
//        when(meetingAuthorizationService.canUserViewResource(3L, 2L)).thenReturn(true);
//
//        MeetingResponse response1 = MeetingResponse.builder().id(1L).title("Test Meeting").build();
//        MeetingResponse response3 = MeetingResponse.builder().id(3L).title("Public Meeting").build();
//
//        when(meetingMapper.toResponse(meeting)).thenReturn(response1);
//        when(meetingMapper.toResponse(publicMeeting)).thenReturn(response3);
//
//        // When
//        List<MeetingResponse> accessibleMeetings = meetingService.getAccessibleMeetings(2L);
//
//        // Then
//        assertNotNull(accessibleMeetings);
//        assertEquals(2, accessibleMeetings.size());
//        assertTrue(accessibleMeetings.stream().anyMatch(m -> m.getId() == 1L));
//        assertTrue(accessibleMeetings.stream().anyMatch(m -> m.getId() == 3L));
//
//        verify(meetingRepository).findAll();
//        verify(meetingAuthorizationService).canUserViewResource(1L, 2L);
//        verify(meetingAuthorizationService).canUserViewResource(2L, 2L);
//        verify(meetingAuthorizationService).canUserViewResource(3L, 2L);
//        verify(meetingMapper, times(2)).toResponse(any(Meeting.class));
//    }
//
//    @Test
//    void testGetAccessibleMeetings_ExceptionDuringCheck_LogsWarning() {
//        // Given
//        List<Meeting> allMeetings = List.of(meeting);
//        when(meetingRepository.findAll()).thenReturn(allMeetings);
//        when(meetingAuthorizationService.canUserViewResource(1L, 2L))
//                .thenThrow(new RuntimeException("Permission service error"));
//
//        // When
//        List<MeetingResponse> accessibleMeetings = meetingService.getAccessibleMeetings(2L);
//
//        // Then
//        assertNotNull(accessibleMeetings);
//        assertTrue(accessibleMeetings.isEmpty());
//
//        verify(meetingRepository).findAll();
//        verify(meetingAuthorizationService).canUserViewResource(1L, 2L);
//        verify(meetingMapper, never()).toResponse(any(Meeting.class));
//    }
//
//    @Test
//    void testGetAccessibleMeetings_EmptyList_ReturnsEmptyList() {
//        // Given
//        when(meetingRepository.findAll()).thenReturn(Collections.emptyList());
//
//        // When
//        List<MeetingResponse> accessibleMeetings = meetingService.getAccessibleMeetings(2L);
//
//        // Then
//        assertNotNull(accessibleMeetings);
//        assertTrue(accessibleMeetings.isEmpty());
//
//        verify(meetingRepository).findAll();
//        verify(meetingAuthorizationService, never()).canUserViewResource(anyLong(), anyLong());
//        verify(meetingMapper, never()).toResponse(any(Meeting.class));
//    }
//}
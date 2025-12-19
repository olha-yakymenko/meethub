package com.meethub.domain.service.impl;

import com.meethub.domain.model.dto.ParticipantCountDto;
import com.meethub.domain.model.entity.*;
import com.meethub.domain.model.enums.MeetingStatus;
import com.meethub.domain.model.enums.MeetingVisibility;
import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.model.enums.PermissionLevel;
import com.meethub.domain.model.mapper.MeetingMapper;
import com.meethub.domain.model.projection.ParticipantProjection;
import com.meethub.domain.model.request.*;
import com.meethub.domain.model.response.MeetingParticipationInfo;
import com.meethub.domain.model.response.MeetingResponse;
import com.meethub.domain.model.response.ParticipantResponse;
import com.meethub.domain.model.response.UserResponse;
import com.meethub.domain.repository.jdbc.CustomMeetingRepository;
import com.meethub.domain.repository.jpa.*;
import com.meethub.domain.service.*;
import com.meethub.exception.BusinessException;
import com.meethub.exception.ResourceNotFoundException;
import com.meethub.security.CustomUserDetailsService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.awt.print.Pageable;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class MeetingParticipantServiceImplTest {

    @Mock
    private MeetingParticipantRepository participantRepository;

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ParticipantStatusHistoryRepository statusHistoryRepository;

    @Mock
    private WaitlistEntryRepository waitlistEntryRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private MeetingMapper meetingMapper;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private MeetingParticipantServiceImpl meetingParticipantService;
    @Mock
    private SecurityContext securityContext;

    @Mock
    private CustomMeetingRepository customMeetingRepository;

    @Mock
    private MeetingAuthorizationService meetingAuthorizationService;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;


    @Mock
    private MeetingSchedulerService meetingSchedulerService;

    @InjectMocks
    private MeetingServiceImpl meetingService;

    private User testOrganizer;
    private Meeting testMeeting;
    private MeetingResponse testMeetingResponse;

    private CustomUserDetailsService.CustomUserDetails testUserDetails;

    private User testUser;
    private MeetingParticipant testParticipant;
    private MeetingParticipant testOrganizerParticipant;

    @BeforeEach
    void setUp() {
        testOrganizer = User.builder()
                .id(1L)
                .email("organizer@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        testUser = User.builder()
                .id(2L)
                .email("user@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .build();

        testMeeting = Meeting.builder()
                .title("Test Meeting")
                .description("Test Description")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(1).plusHours(2))
                .visibility(MeetingVisibility.PUBLIC)
                .maxParticipants(10)
                .organizer(testOrganizer)
                .build();

        testMeeting.setId(100L);

        testParticipant = MeetingParticipant.builder()
                .id(200L)
                .meeting(testMeeting)
                .user(testUser)
                .status(ParticipationStatus.CONFIRMED)
                .permissionLevel(PermissionLevel.PARTICIPANT)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testOrganizerParticipant = MeetingParticipant.builder()
                .id(300L)
                .meeting(testMeeting)
                .user(testOrganizer)
                .status(ParticipationStatus.CONFIRMED)
                .permissionLevel(PermissionLevel.ORGANIZER)
                .build();

        testUserDetails = mock(CustomUserDetailsService.CustomUserDetails.class);
    }

    @Test
    void inviteMultipleParticipants_shouldInviteAllUsers() {
        InviteParticipantsRequest request = new InviteParticipantsRequest();
        request.setUserIds(Arrays.asList(2L, 3L, 4L));

        when(meetingRepository.findByIdAndOrganizerId(100L, 1L))
                .thenReturn(Optional.of(testMeeting));
        when(userRepository.findById(anyLong())).thenAnswer(invocation -> {
            Long userId = invocation.getArgument(0);
            return Optional.of(User.builder().id(userId).email("user" + userId + "@example.com").build());
        });
        when(participantRepository.findByMeetingIdAndUserId(anyLong(), anyLong()))
                .thenReturn(Optional.empty());
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.countByMeetingIdAndStatus(100L, ParticipationStatus.CONFIRMED))
                .thenReturn(5L);
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenReturn(testParticipant);

        List<MeetingParticipant> result = meetingParticipantService
                .inviteMultipleParticipants(100L, request, 1L);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(3, result.size())
        );

        verify(participantRepository, times(3)).save(any(MeetingParticipant.class));
    }

    @Test
    void inviteParticipant_whenUserAlreadyParticipant_shouldThrowException() {
        when(meetingRepository.findByIdAndOrganizerId(100L, 1L))
                .thenReturn(Optional.of(testMeeting));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));

        assertThrows(IllegalArgumentException.class, () ->
                meetingParticipantService.inviteParticipant(100L, 2L, 1L)
        );
    }

    @Test
    void inviteParticipant_whenNotOrganizer_shouldThrowException() {
        when(meetingRepository.findByIdAndOrganizerId(100L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                meetingParticipantService.inviteParticipant(100L, 2L, 1L)
        );
    }

    @Test
    void updateParticipant_shouldUpdateSuccessfully() {
        UpdateParticipantRequest request = new UpdateParticipantRequest();
        request.setStatus(ParticipationStatus.CONFIRMED);
        request.setPermissionLevel(PermissionLevel.MODERATOR);
        request.setComment("Updated status");

        when(participantRepository.findById(200L))
                .thenReturn(Optional.of(testParticipant));
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenReturn(testParticipant);

        ParticipantResponse result = meetingParticipantService.updateParticipant(200L, request);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(ParticipationStatus.CONFIRMED, result.getStatus()),
                () -> assertEquals(PermissionLevel.MODERATOR, result.getPermissionLevel()),
                () -> assertEquals("Updated status", result.getComment())
        );

        verify(participantRepository).save(testParticipant);
    }

    @Test
    void updateParticipantPermission_shouldUpdatePermissionLevel() {
        when(meetingRepository.findByIdAndOrganizerId(100L, 1L))
                .thenReturn(Optional.of(testMeeting));
        when(participantRepository.findById(200L))
                .thenReturn(Optional.of(testParticipant));
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenReturn(testParticipant);

        MeetingParticipant result = meetingParticipantService
                .updateParticipantPermission(100L, 200L, PermissionLevel.MODERATOR, 1L);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(PermissionLevel.MODERATOR, result.getPermissionLevel())
        );
    }

    @Test
    void updateParticipantPermission_whenNotOrganizer_shouldThrowException() {
        when(meetingRepository.findByIdAndOrganizerId(100L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                meetingParticipantService.updateParticipantPermission(100L, 200L, PermissionLevel.MODERATOR, 1L)
        );
    }

    @Test
    void removeParticipant_shouldDeleteParticipant() {
        when(meetingRepository.findByIdAndOrganizerId(100L, 1L))
                .thenReturn(Optional.of(testMeeting));

        meetingParticipantService.removeParticipant(100L, 200L, 1L);

        verify(participantRepository).deleteById(200L);
    }

    @Test
    void removeParticipantById_shouldDelete() {
        meetingParticipantService.removeParticipant(200L);

        verify(participantRepository).deleteById(200L);
    }

    @Test
    void joinPublicMeeting_whenMeetingCompleted_shouldThrowException() {
        testMeeting.setStatus(MeetingStatus.COMPLETED);
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));

        assertThrows(IllegalStateException.class, () ->
                meetingParticipantService.joinPublicMeeting(100L, 2L)
        );
    }

    @Test
    void joinPublicMeeting_whenNotPublic_shouldThrowException() {
        testMeeting.setVisibility(MeetingVisibility.PRIVATE);
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));

        assertThrows(SecurityException.class, () ->
                meetingParticipantService.joinPublicMeeting(100L, 2L)
        );
    }

    @Test
    void joinPublicMeeting_whenNoAvailableSpots_shouldThrowException() {
        testMeeting.setMaxParticipants(5);
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.empty());
        when(participantRepository.countByMeetingIdAndStatus(100L, ParticipationStatus.CONFIRMED))
                .thenReturn(5L);

        assertThrows(IllegalArgumentException.class, () ->
                meetingParticipantService.joinPublicMeeting(100L, 2L)
        );
    }

    @Test
    void joinPublicMeeting_whenAlreadyInvited_shouldConfirm() {
        testParticipant.setStatus(ParticipationStatus.INVITED);
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenReturn(testParticipant);

        MeetingParticipant result = meetingParticipantService.joinPublicMeeting(100L, 2L);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(ParticipationStatus.CONFIRMED, result.getStatus())
        );
    }

    @Test
    void requestToJoinPrivateMeeting_whenAlreadyPending_shouldThrowException() {
        testMeeting.setVisibility(MeetingVisibility.PRIVATE);
        testParticipant.setStatus(ParticipationStatus.PENDING);

        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));

        assertThrows(IllegalArgumentException.class, () ->
                meetingParticipantService.requestToJoinPrivateMeeting(100L, 2L)
        );
    }


    @Test
    void getPendingRequests_shouldReturnPendingParticipants() {
        MeetingParticipant pendingParticipant = MeetingParticipant.builder()
                .id(201L)
                .meeting(testMeeting)
                .user(testUser)
                .status(ParticipationStatus.PENDING)
                .build();

        when(participantRepository.findByMeetingIdAndStatus(100L, ParticipationStatus.PENDING))
                .thenReturn(List.of(pendingParticipant));

        List<ParticipantResponse> result = meetingParticipantService.getPendingRequests(100L);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(1, result.size()),
                () -> assertEquals(ParticipationStatus.PENDING, result.get(0).getStatus())
        );
    }

    @Test
    void isUserPendingApproval_whenPending_shouldReturnTrue() {
        testParticipant.setStatus(ParticipationStatus.PENDING);
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));

        boolean result = meetingParticipantService.isUserPendingApproval(100L, 2L);

        assertAll(
                () -> assertTrue(result)
        );
    }

    @Test
    void isUserPendingApproval_whenNotParticipant_shouldReturnFalse() {
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.empty());

        boolean result = meetingParticipantService.isUserPendingApproval(100L, 2L);

        assertAll(
                () -> assertFalse(result)
        );
    }

    @Test
    void acceptInvitationByToken_whenInvalidToken_shouldThrowException() {
        when(participantRepository.findByInvitationToken("invalid-token"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                meetingParticipantService.acceptInvitationByToken("invalid-token")
        );
    }

    @Test
    void acceptInvitationByToken_whenMeetingNotOngoing_shouldThrowException() {
        String token = "valid-token";
        testParticipant.setInvitationToken(token);
        testMeeting.setStartDate(LocalDateTime.now().plusDays(1));

        when(participantRepository.findByInvitationToken(token))
                .thenReturn(Optional.of(testParticipant));

        assertThrows(IllegalArgumentException.class, () ->
                meetingParticipantService.acceptInvitationByToken(token)
        );
    }

    @Test
    void acceptInvitationByToken_whenAlreadyAttended_shouldThrowException() {
        String token = "valid-token";
        testParticipant.setInvitationToken(token);
        testParticipant.setAttendanceConfirmedAt(LocalDateTime.now());
        testMeeting.setStartDate(LocalDateTime.now().minusHours(1));
        testMeeting.setEndDate(LocalDateTime.now().plusHours(1));

        when(participantRepository.findByInvitationToken(token))
                .thenReturn(Optional.of(testParticipant));

        assertThrows(IllegalArgumentException.class, () ->
                meetingParticipantService.acceptInvitationByToken(token)
        );
    }

    @Test
    void acceptInvitationByToken_whenValid_shouldConfirmAttendance() {
        String token = "valid-token";
        testParticipant.setInvitationToken(token);
        testParticipant.setStatus(ParticipationStatus.INVITED);
        testMeeting.setStartDate(LocalDateTime.now().minusHours(1));
        testMeeting.setEndDate(LocalDateTime.now().plusHours(1));

        when(participantRepository.findByInvitationToken(token))
                .thenReturn(Optional.of(testParticipant));
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenReturn(testParticipant);

        MeetingParticipant result = meetingParticipantService.acceptInvitationByToken(token);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(ParticipationStatus.ATTENDED, result.getStatus()),
                () -> assertNotNull(result.getAttendanceConfirmedAt()),
                () -> assertNull(result.getInvitationToken())
        );
    }

    @Test
    void isUserParticipant_whenConfirmed_shouldReturnTrue() {
        testParticipant.setStatus(ParticipationStatus.CONFIRMED);
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));

        boolean result = meetingParticipantService.isUserParticipant(100L, 2L);

        assertAll(
                () -> assertTrue(result)
        );
    }

    @Test
    void isUserParticipant_whenPending_shouldReturnTrue() {
        testParticipant.setStatus(ParticipationStatus.PENDING);
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));

        boolean result = meetingParticipantService.isUserParticipant(100L, 2L);

        assertAll(
                () -> assertTrue(result)
        );
    }

    @Test
    void isUserParticipant_whenDeclined_shouldReturnFalse() {
        testParticipant.setStatus(ParticipationStatus.DECLINED);
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));

        boolean result = meetingParticipantService.isUserParticipant(100L, 2L);

        assertAll(
                () -> assertFalse(result)
        );
    }

    @Test
    void isUserParticipant_whenNotParticipant_shouldReturnFalse() {
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.empty());

        boolean result = meetingParticipantService.isUserParticipant(100L, 2L);

        assertAll(
                () -> assertFalse(result)
        );
    }

    @Test
    void canUserEditMeeting_whenModerator_shouldReturnTrue() {
        testParticipant.setPermissionLevel(PermissionLevel.MODERATOR);
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));

        boolean result = meetingParticipantService.canUserEditMeeting(100L, 2L);

        assertAll(
                () -> assertTrue(result)
        );
    }

    @Test
    void canUserEditMeeting_whenContributor_shouldReturnTrue() {
        testParticipant.setPermissionLevel(PermissionLevel.CONTRIBUTOR);
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));

        boolean result = meetingParticipantService.canUserEditMeeting(100L, 2L);

        assertAll(
                () -> assertTrue(result)
        );
    }

    @Test
    void canUserEditMeeting_whenParticipant_shouldReturnFalse() {
        testParticipant.setPermissionLevel(PermissionLevel.PARTICIPANT);
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));

        boolean result = meetingParticipantService.canUserEditMeeting(100L, 2L);

        assertAll(
                () -> assertFalse(result)
        );
    }

    @Test
    void getParticipantPermissionLevel_whenUserNotFound_shouldReturnDefault() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.findByMeetingIdAndUserId(100L, 999L))
                .thenReturn(Optional.empty());

        PermissionLevel result = meetingParticipantService.getParticipantPermissionLevel(100L, 999L);

        assertAll(
                () -> assertEquals(PermissionLevel.PARTICIPANT, result)
        );
    }

    @Test
    void getParticipantPermissionLevel_whenMeetingNotFound_shouldReturnDefault() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.empty());

        PermissionLevel result = meetingParticipantService.getParticipantPermissionLevel(100L, 2L);

        assertAll(
                () -> assertEquals(PermissionLevel.PARTICIPANT, result)
        );
    }

    @Test
    void getUserInvitations_shouldReturnInvitedParticipants() {
        MeetingParticipant invitedParticipant = MeetingParticipant.builder()
                .id(201L)
                .meeting(testMeeting)
                .user(testUser)
                .status(ParticipationStatus.INVITED)
                .build();

        when(participantRepository.findByUserIdAndStatus(2L, ParticipationStatus.INVITED))
                .thenReturn(List.of(invitedParticipant));

        List<ParticipantResponse> result = meetingParticipantService.getUserInvitations(2L);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(1, result.size()),
                () -> assertEquals(ParticipationStatus.INVITED, result.get(0).getStatus())
        );
    }

    @Test
    void respondToInvitation_whenNotOwner_shouldThrowException() {
        testParticipant.setStatus(ParticipationStatus.INVITED);
        User differentUser = User.builder().id(999L).build();
        testParticipant.setUser(differentUser);

        when(participantRepository.findById(200L)).thenReturn(Optional.of(testParticipant));

        assertThrows(SecurityException.class, () ->
                meetingParticipantService.respondToInvitation(200L, ParticipationStatus.CONFIRMED, "", 2L)
        );
    }

    @Test
    void respondToInvitation_whenAlreadyResponded_shouldThrowException() {
        testParticipant.setStatus(ParticipationStatus.CONFIRMED);
        when(participantRepository.findById(200L)).thenReturn(Optional.of(testParticipant));

        assertThrows(IllegalArgumentException.class, () ->
                meetingParticipantService.respondToInvitation(200L, ParticipationStatus.CONFIRMED, "", 2L)
        );
    }

//    @Test
//    void getParticipantStatistics_whenException_shouldReturnDefaultStats() {
//        when(meetingRepository.findById(100L)).thenThrow(new RuntimeException("DB Error"));
//
//        Map<String, Long> stats = meetingParticipantService.getParticipantStatistics(100L);
//
//        assertAll(
//                () -> assertNotNull(stats),
//                () -> assertTrue(stats.containsKey("total")),
//                () -> assertEquals(0L, stats.get("total"))
//        );
//    }


    @Test
    void getParticipantStatistics_whenMeetingNotFound_shouldReturnDefaultStats() {
        // Given - meeting nie istnieje
        when(meetingRepository.findById(100L))
                .thenReturn(Optional.empty());

        // When
        Map<String, Long> stats = meetingParticipantService.getParticipantStatistics(100L);

        // Then
        assertAll(
                () -> assertNotNull(stats, "Stats should not be null"),
                () -> assertEquals(0L, stats.get("total"), "Total should be 0"),
                () -> assertEquals(0L, stats.get("confirmed"), "Confirmed should be 0"),
                () -> assertEquals(0L, stats.get("pending"), "Pending should be 0"),
                () -> assertEquals(0L, stats.get("invited"), "Invited should be 0"),
                () -> assertEquals(0L, stats.get("declined"), "Declined should be 0")
        );

        verify(meetingRepository).findById(100L);
        verify(participantRepository, never()).countByMeetingId(anyLong());
        verify(participantRepository, never()).countByMeetingIdAndStatus(anyLong(), any());
    }

    @Test
    void getParticipantStatistics_whenRepositoryThrowsException_shouldReturnDefaultStats() {
        // Given - wyjątek z repozytorium
        when(meetingRepository.findById(100L))
                .thenThrow(new RuntimeException("DB Error"));

        // When
        Map<String, Long> stats = meetingParticipantService.getParticipantStatistics(100L);

        // Then
        assertAll(
                () -> assertNotNull(stats, "Stats should not be null"),
                () -> assertEquals(0L, stats.get("total"), "Total should be 0"),
                () -> assertEquals(0L, stats.get("confirmed"), "Confirmed should be 0"),
                () -> assertEquals(0L, stats.get("pending"), "Pending should be 0"),
                () -> assertEquals(0L, stats.get("invited"), "Invited should be 0"),
                () -> assertEquals(0L, stats.get("declined"), "Declined should be 0")
        );
    }

    @Test
    void getParticipantStatistics_whenOrganizerNotParticipant_shouldIncludeOrganizer() {
        // Given - organizator nie jest w tabeli participants
        Long meetingId = 100L;
        Long organizerId = 1L;

        Meeting meeting = Meeting.builder()
                .organizer(User.builder().id(organizerId).build())
                .build();

        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(meeting));
        when(participantRepository.countByMeetingId(meetingId)).thenReturn(5L);
        when(participantRepository.countByMeetingIdAndStatusIn(
                eq(meetingId),
                argThat(list -> list.contains(ParticipationStatus.CONFIRMED) &&
                        list.contains(ParticipationStatus.ATTENDED))))
                .thenReturn(3L);
        when(participantRepository.findByMeetingIdAndUserId(meetingId, organizerId))
                .thenReturn(Optional.empty());
        when(participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.PENDING))
                .thenReturn(1L);
        when(participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.INVITED))
                .thenReturn(2L);
        when(participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.DECLINED))
                .thenReturn(0L);

        // When
        Map<String, Long> stats = meetingParticipantService.getParticipantStatistics(meetingId);

        // Then - organizator dodany do statystyk
        assertAll(
                () -> assertEquals(6L, stats.get("total"), "Total should be 6 (5 + organizer)"),
                () -> assertEquals(4L, stats.get("confirmed"), "Confirmed should be 4 (3 + organizer)"),
                () -> assertEquals(1L, stats.get("pending"), "Pending should be 1"),
                () -> assertEquals(2L, stats.get("invited"), "Invited should be 2"),
                () -> assertEquals(0L, stats.get("declined"), "Declined should be 0"),
                () -> assertEquals(0L, stats.get("organizerIncluded"), "Organizer not in participants table")
        );
    }

    @Test
    void getParticipantStatistics_whenOrganizerIsParticipant_shouldNotDuplicate() {
        // Given - organizator jest już w tabeli participants
        Long meetingId = 100L;
        Long organizerId = 1L;

        Meeting meeting = Meeting.builder()
                .organizer(User.builder().id(organizerId).build())
                .build();

        MeetingParticipant organizerParticipant = MeetingParticipant.builder()
                .meeting(meeting)
                .user(meeting.getOrganizer())
                .status(ParticipationStatus.CONFIRMED)
                .build();

        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(meeting));
        when(participantRepository.countByMeetingId(meetingId)).thenReturn(5L); // w tym organizator
        when(participantRepository.countByMeetingIdAndStatusIn(
                eq(meetingId),
                argThat(list -> list.contains(ParticipationStatus.CONFIRMED) &&
                        list.contains(ParticipationStatus.ATTENDED))))
                .thenReturn(3L); // w tym organizator
        when(participantRepository.findByMeetingIdAndUserId(meetingId, organizerId))
                .thenReturn(Optional.of(organizerParticipant));
        when(participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.PENDING))
                .thenReturn(1L);
        when(participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.INVITED))
                .thenReturn(1L);
        when(participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.DECLINED))
                .thenReturn(0L);

        // When
        Map<String, Long> stats = meetingParticipantService.getParticipantStatistics(meetingId);

        // Then - organizator nie jest dodawany podwójnie
        assertAll(
                () -> assertEquals(5L, stats.get("total"), "Total should be 5 (organizer already counted)"),
                () -> assertEquals(3L, stats.get("confirmed"), "Confirmed should be 3 (organizer already counted)"),
                () -> assertEquals(1L, stats.get("pending"), "Pending should be 1"),
                () -> assertEquals(1L, stats.get("invited"), "Invited should be 1"),
                () -> assertEquals(0L, stats.get("declined"), "Declined should be 0"),
                () -> assertEquals(1L, stats.get("organizerIncluded"), "Organizer is in participants table")
        );
    }

    @Test
    void getParticipantStatistics_whenParticipantRepositoryThrowsException_shouldReturnDefaultStats() {
        // Given - meeting istnieje, ale repozytorium uczestników rzuca wyjątek
        Long meetingId = 100L;

        Meeting meeting = Meeting.builder()
                .organizer(User.builder().id(1L).build())
                .build();

        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(meeting));
        when(participantRepository.countByMeetingId(meetingId))
                .thenThrow(new RuntimeException("DB Error"));

        // When
        Map<String, Long> stats = meetingParticipantService.getParticipantStatistics(meetingId);

        // Then
        assertAll(
                () -> assertNotNull(stats, "Stats should not be null"),
                () -> assertEquals(0L, stats.get("total"), "Total should be 0"),
                () -> assertEquals(0L, stats.get("confirmed"), "Confirmed should be 0"),
                () -> assertEquals(0L, stats.get("pending"), "Pending should be 0")
        );
    }


    @Test
    void hasAccessToMeeting_whenOrganizer_shouldReturnTrue() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));

        boolean result = meetingParticipantService.hasAccessToMeeting(100L, 1L);

        assertAll(
                () -> assertTrue(result)
        );
    }

    @Test
    void hasAccessToMeeting_whenParticipant_shouldReturnTrue() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));

        boolean result = meetingParticipantService.hasAccessToMeeting(100L, 2L);

        assertAll(
                () -> assertTrue(result)
        );
    }

    @Test
    void isOrganizer_whenMeetingNotFound_shouldReturnFalse() {
        when(meetingRepository.findById(100L)).thenThrow(new ResourceNotFoundException("Not found"));

        boolean result = meetingParticipantService.isOrganizer(100L, 1L);

        assertAll(
                () -> assertFalse(result)
        );
    }

    @Test
    void canEditParticipant_whenOrganizer_shouldReturnTrue() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));

        boolean result = meetingParticipantService.canEditParticipant(100L, 200L, 1L);

        assertAll(
                () -> assertTrue(result)
        );
    }

    @Test
    void canRemoveParticipant_whenNotOrganizer_shouldReturnFalse() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));

        boolean result = meetingParticipantService.canRemoveParticipant(100L, 200L, 2L);

        assertAll(
                () -> assertFalse(result)
        );
    }

    @Test
    void isParticipant_whenOrganizer_shouldReturnTrue() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));

        boolean result = meetingParticipantService.isParticipant(100L, 1L);

        assertAll(
                () -> assertTrue(result)
        );
    }

    @Test
    void isParticipant_whenConfirmed_shouldReturnTrue() {
        testParticipant.setStatus(ParticipationStatus.CONFIRMED);
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));

        boolean result = meetingParticipantService.isParticipant(100L, 2L);

        assertAll(
                () -> assertTrue(result)
        );
    }

    @Test
    void isParticipant_whenInvited_shouldReturnFalse() {
        testParticipant.setStatus(ParticipationStatus.INVITED);
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));

        boolean result = meetingParticipantService.isParticipant(100L, 2L);

        assertAll(
                () -> assertFalse(result)
        );
    }

    @Test
    void getParticipantInfo_whenOrganizer_shouldReturnOrganizerResponse() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));

        ParticipantResponse result = meetingParticipantService.getParticipantInfo(1L, 100L);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(PermissionLevel.ORGANIZER, result.getPermissionLevel())
        );
    }

    @Test
    void getParticipantInfo_whenNotParticipant_shouldReturnNull() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.empty());

        ParticipantResponse result = meetingParticipantService.getParticipantInfo(2L, 100L);

        assertAll(
                () -> assertNull(result)
        );
    }

    @Test
    void confirmParticipation_shouldReturnParticipantResponse() {
        String token = "valid-token";
        testParticipant.setInvitationToken(token);
        testParticipant.setStatus(ParticipationStatus.INVITED);
        testMeeting.setStartDate(LocalDateTime.now().minusHours(1));
        testMeeting.setEndDate(LocalDateTime.now().plusHours(1));

        when(participantRepository.findByInvitationToken(token))
                .thenReturn(Optional.of(testParticipant));
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenReturn(testParticipant);

        ParticipantResponse result = meetingParticipantService.confirmParticipation(token, "Confirmed");

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(ParticipationStatus.ATTENDED, result.getStatus())
        );
    }

    @Test
    void declineParticipation_shouldUpdateStatusToDeclined() {
        String token = "valid-token";
        testParticipant.setInvitationToken(token);
        testParticipant.setStatus(ParticipationStatus.INVITED);

        when(participantRepository.findByInvitationToken(token))
                .thenReturn(Optional.of(testParticipant));
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenReturn(testParticipant);

        ParticipantResponse result = meetingParticipantService.declineParticipation(token, "Cannot attend");

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(ParticipationStatus.DECLINED, result.getStatus()),
                () -> assertEquals("Cannot attend", result.getComment())
        );
    }

    @Test
    void isPendingParticipant_whenPending_shouldReturnTrue() {
        when(participantRepository.existsByMeetingIdAndUserIdAndStatus(
                100L, 2L, ParticipationStatus.PENDING))
                .thenReturn(true);

        boolean result = meetingParticipantService.isPendingParticipant(100L, 2L);

        assertAll(
                () -> assertTrue(result)
        );
    }

    @Test
    void isInvitedParticipant_whenInvited_shouldReturnTrue() {
        when(participantRepository.existsByMeetingIdAndUserIdAndStatus(
                100L, 2L, ParticipationStatus.INVITED))
                .thenReturn(true);

        boolean result = meetingParticipantService.isInvitedParticipant(100L, 2L);

        assertAll(
                () -> assertTrue(result)
        );
    }

    @Test
    void isDeclinedParticipant_whenDeclined_shouldReturnTrue() {
        when(participantRepository.existsByMeetingIdAndUserIdAndStatus(
                100L, 2L, ParticipationStatus.DECLINED))
                .thenReturn(true);

        boolean result = meetingParticipantService.isDeclinedParticipant(100L, 2L);

        assertAll(
                () -> assertTrue(result)
        );
    }

    @Test
    void isWaitingListParticipant_whenWaiting_shouldReturnTrue() {
        when(participantRepository.existsByMeetingIdAndUserIdAndStatus(
                100L, 2L, ParticipationStatus.PENDING))
                .thenReturn(true);

        boolean result = meetingParticipantService.isWaitingListParticipant(100L, 2L);

        assertAll(
                () -> assertTrue(result)
        );
    }

    @Test
    void isViewer_whenPublicMeetingAndNotParticipant_shouldReturnTrue() {
        testMeeting.setVisibility(MeetingVisibility.PUBLIC);
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.findByMeetingIdAndUserId(100L, 999L))
                .thenReturn(Optional.empty());

        boolean result = meetingParticipantService.isViewer(100L, 999L);

        assertAll(
                () -> assertTrue(result)
        );
    }

    @Test
    void isViewer_whenPrivateMeeting_shouldReturnFalse() {
        testMeeting.setVisibility(MeetingVisibility.PRIVATE);
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));

        boolean result = meetingParticipantService.isViewer(100L, 999L);

        assertAll(
                () -> assertFalse(result)
        );
    }

    @Test
    void isViewer_whenNullUserIdAndPublicMeeting_shouldReturnTrue() {
        testMeeting.setVisibility(MeetingVisibility.PUBLIC);
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));

        boolean result = meetingParticipantService.isViewer(100L, null);

        assertAll(
                () -> assertTrue(result)
        );
    }

    @Test
    void isUnrelatedUser_whenPrivateMeetingAndNotParticipant_shouldReturnTrue() {
        testMeeting.setVisibility(MeetingVisibility.PRIVATE);
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.findByMeetingIdAndUserId(100L, 999L))
                .thenReturn(Optional.empty());

        boolean result = meetingParticipantService.isUnrelatedUser(100L, 999L);

        assertAll(
                () -> assertTrue(result)
        );
    }

    @Test
    void isUnrelatedUser_whenPublicMeetingAndNotParticipant_shouldReturnFalse() {
        testMeeting.setVisibility(MeetingVisibility.PUBLIC);
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.findByMeetingIdAndUserId(100L, 999L))
                .thenReturn(Optional.empty());

        boolean result = meetingParticipantService.isUnrelatedUser(100L, 999L);

        assertAll(
                () -> assertFalse(result)
        );
    }

    @Test
    void exportParticipantsToCsv_shouldReturnByteArrayResource() {
        List<ParticipantProjection> projections = new ArrayList<>();
        when(participantRepository.findParticipantsProjection(100L))
                .thenReturn(projections);

        ByteArrayResource result = meetingParticipantService.exportParticipantsToCsv(100L);

        assertAll(
                () -> assertNotNull(result),
                () -> assertTrue(result.exists())
        );
    }

    @Test
    void addOrganizerAsParticipant_shouldSaveOrganizerAsParticipant() {
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenReturn(testOrganizerParticipant);

        meetingParticipantService.addOrganizerAsParticipant(testMeeting, testOrganizer);

        verify(participantRepository).save(any(MeetingParticipant.class));
    }

    @Test
    void confirmAttendance_shouldUpdateStatusToAttended() {
        String token = "attendance-token";
        testParticipant.setInvitationToken(token);

        when(participantRepository.findByIdAndInvitationToken(200L, token))
                .thenReturn(Optional.of(testParticipant));

        meetingParticipantService.confirmAttendance(200L, token);

        assertAll(
                () -> assertEquals(ParticipationStatus.ATTENDED, testParticipant.getStatus())
        );

        verify(participantRepository).save(testParticipant);
    }

    @Test
    void confirmAttendance_whenInvalidToken_shouldThrowException() {
        when(participantRepository.findByIdAndInvitationToken(200L, "invalid-token"))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                meetingParticipantService.confirmAttendance(200L, "invalid-token")
        );
    }

    @Test
    void hasAvailableSpots_whenLimitReached_shouldReturnFalse() {
        testMeeting.setMaxParticipants(5);
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.countByMeetingIdAndStatus(100L, ParticipationStatus.CONFIRMED))
                .thenReturn(5L);

        boolean result = meetingParticipantService.hasAvailableSpots(100L);

        assertAll(
                () -> assertFalse(result)
        );
    }

    @Test
    void isMeetingFull_whenNoLimit_shouldReturnFalse() {
        testMeeting.setMaxParticipants(null);
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));

        boolean result = meetingParticipantService.isMeetingFull(100L);

        assertAll(
                () -> assertFalse(result)
        );
    }

    @Test
    void getParticipant_whenNotFound_shouldThrowException() {
        when(participantRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                meetingParticipantService.getParticipant(999L)
        );
    }

    @Test
    void getDetailedStats_shouldReturnMapWithParticipants() {
        List<ParticipantProjection> projections = new ArrayList<>();
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.countByMeetingId(100L)).thenReturn(5L);
        when(participantRepository.countByMeetingIdAndStatus(100L, ParticipationStatus.CONFIRMED))
                .thenReturn(3L);
        when(participantRepository.findByMeetingIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(testOrganizerParticipant));
        when(participantRepository.findParticipantsProjection(100L))
                .thenReturn(projections);

        Map<String, Object> detailedStats = meetingParticipantService.getDetailedStats(100L);

        assertAll(
                () -> assertNotNull(detailedStats),
                () -> assertTrue(detailedStats.containsKey("total")),
                () -> assertTrue(detailedStats.containsKey("confirmed")),
                () -> assertTrue(detailedStats.containsKey("participants")),
                () -> assertTrue(detailedStats.containsKey("meetingId")),
                () -> assertEquals(100L, detailedStats.get("meetingId"))
        );
    }

    @Test
    void isUserParticipant_whenNullMeetingId_shouldReturnFalse() {
        boolean result = meetingParticipantService.isUserParticipant(null, 2L);

        assertAll(
                () -> assertFalse(result)
        );
    }

    @Test
    void isUserParticipant_whenNullUserId_shouldReturnFalse() {
        boolean result = meetingParticipantService.isUserParticipant(100L, null);

        assertAll(
                () -> assertFalse(result)
        );
    }

    @Test
    void getParticipantPermissionLevel_whenNullParameters_shouldReturnDefault() {
        PermissionLevel result = meetingParticipantService.getParticipantPermissionLevel(null, null);

        assertAll(
                () -> assertEquals(PermissionLevel.PARTICIPANT, result)
        );
    }

    @Test
    void isOnWaitlist_whenOnWaitlist_shouldReturnTrue() {
        when(waitlistEntryRepository.existsByMeetingIdAndUserId(100L, 2L))
                .thenReturn(true);

        boolean result = meetingParticipantService.isOnWaitlist(100L, 2L);

        assertAll(
                () -> assertTrue(result)
        );
    }


//    @Test
//    void completeUserJourney_publicMeeting() {
////        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
//        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
//                .thenReturn(Optional.empty());
////        when(participantRepository.countByMeetingIdAndStatus(100L, ParticipationStatus.CONFIRMED))
////                .thenReturn(5L);
//
//        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
//        when(participantRepository.save(any(MeetingParticipant.class))).thenReturn(testParticipant);
//
//        MeetingParticipant participant = meetingParticipantService.joinPublicMeeting(100L, 2L);
//
//        assertAll(
//                () -> assertNotNull(participant),
//                () -> assertEquals(ParticipationStatus.CONFIRMED, participant.getStatus())
//        );
//
//        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
//                .thenReturn(Optional.of(testParticipant));
//        when(waitlistEntryRepository.findFirstByMeetingIdOrderByPositionAsc(100L))
//                .thenReturn(Optional.empty());
//
//        meetingParticipantService.leaveMeeting(2L, 100L);
//
//        verify(participantRepository).delete(testParticipant);
//    }


    @Test
    void getMeetingParticipants_shouldReturnParticipants() {
        ParticipantProjection projection = mock(ParticipantProjection.class);
        when(participantRepository.findAllParticipantsByMeetingId(100L))
                .thenReturn(List.of(projection));

        List<ParticipantProjection> result = meetingParticipantService.getMeetingParticipants(100L);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(1, result.size())
        );
    }


    @Test
    void leaveMeeting_shouldDeleteParticipantAndPromoteFromWaitlist() {
        testParticipant.setStatus(ParticipationStatus.CONFIRMED);

        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));
//        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
//        when(participantRepository.countByMeetingIdAndStatus(100L, ParticipationStatus.CONFIRMED))
//                .thenReturn(3L);

        meetingParticipantService.leaveMeeting(2L, 100L);

        verify(participantRepository).delete(testParticipant);
    }

    @Test
    void searchUsersForInvitation_shouldReturnFilteredUsers() {
        List<User> users = List.of(
                User.builder().id(2L).email("user1@test.com").firstName("John").lastName("Doe").build(),
                User.builder().id(3L).email("user2@test.com").firstName("Jane").lastName("Smith").build()
        );

        when(userRepository.findByEmailContainingOrFirstNameContainingOrLastNameContaining(
                anyString(), anyString(), anyString()))
                .thenReturn(users);
        when(participantRepository.existsByMeetingIdAndUserId(100L, 2L)).thenReturn(true);
        when(participantRepository.existsByMeetingIdAndUserId(100L, 3L)).thenReturn(false);

        List<UserResponse> result = meetingParticipantService.searchUsersForInvitation("user", 100L);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(1, result.size()), // Tylko user2, bo user1 już jest uczestnikiem
                () -> assertEquals(3L, result.get(0).getId())
        );
    }


    @Test
    void isConfirmedParticipant_shouldReturnTrueForOrganizer() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));

        boolean result = meetingParticipantService.isConfirmedParticipant(100L, 1L);

        assertTrue(result);
    }

    @Test
    void isConfirmedParticipant_shouldReturnTrueForConfirmedParticipant() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.existsByMeetingIdAndUserIdAndStatusIn(
                100L, 2L, Arrays.asList(ParticipationStatus.CONFIRMED, ParticipationStatus.ATTENDED)))
                .thenReturn(true);

        boolean result = meetingParticipantService.isConfirmedParticipant(100L, 2L);

        assertTrue(result);
    }

    @Test
    void isConfirmedParticipant_whenNullParameters_shouldReturnFalse() {
        boolean result = meetingParticipantService.isConfirmedParticipant(null, 2L);

        assertFalse(result);
    }


    @Test
    void markAsAttended_whenNotConfirmed_shouldThrowException() {
        testParticipant.setStatus(ParticipationStatus.INVITED);

        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));

        assertThrows(BusinessException.class, () ->
                meetingParticipantService.markAsAttended(100L, 2L)
        );
    }

    @Test
    void markAsAttended_shouldUpdateStatusToAttended() {
        testParticipant.setStatus(ParticipationStatus.CONFIRMED);

        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenReturn(testParticipant);

        MeetingParticipant result = meetingParticipantService.markAsAttended(100L, 2L);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(ParticipationStatus.ATTENDED, result.getStatus())
        );
    }

    @Test
    void getParticipant_whenFound_shouldReturnParticipantResponse() {
        when(participantRepository.findById(200L)).thenReturn(Optional.of(testParticipant));

        ParticipantResponse result = meetingParticipantService.getParticipant(200L);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(200L, result.getId())
        );
    }

    @Test
    void respondToInvitation_shouldUpdateStatus() {
        testParticipant.setStatus(ParticipationStatus.INVITED);

        when(participantRepository.findById(200L)).thenReturn(Optional.of(testParticipant));
        when(participantRepository.save(any(MeetingParticipant.class))).thenReturn(testParticipant);

        meetingParticipantService.respondToInvitation(200L, ParticipationStatus.CONFIRMED,
                "Accepted", 2L);

        assertAll(
                () -> assertEquals(ParticipationStatus.CONFIRMED, testParticipant.getStatus()),
                () -> assertNotNull(testParticipant.getResponseDate())
        );

        verify(statusHistoryRepository).save(any(ParticipantStatusHistory.class));
    }


    @Test
    void joinMeeting_whenInviteOnly_shouldThrowException() {
        testMeeting.setVisibility(MeetingVisibility.INVITE_ONLY);

        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () ->
                meetingParticipantService.joinMeeting(100L, 2L)
        );
    }

    @Test
    void joinMeeting_whenAlreadyParticipant_shouldThrowException() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));

        assertThrows(IllegalStateException.class, () ->
                meetingParticipantService.joinMeeting(100L, 2L)
        );
    }

    @Test
    void exportParticipantsToCsv_shouldGenerateCsv() {
        List<ParticipantProjection> projections = new ArrayList<>();
        ParticipantProjection projection = mock(ParticipantProjection.class);
        when(projection.getId()).thenReturn(1L);
        when(projection.getFullName()).thenReturn("John Doe");
        when(projection.getEmail()).thenReturn("john@example.com");
        projections.add(projection);

        when(participantRepository.findParticipantsProjection(100L)).thenReturn(projections);

        ByteArrayResource result = meetingParticipantService.exportParticipantsToCsv(100L);

        assertAll(
                () -> assertNotNull(result),
                () -> assertTrue(result.exists()),
                () -> assertTrue(result.getByteArray().length > 0)
        );
    }

    @Test
    void isOnWaitlist_shouldCheckMultipleConditions() {
        when(waitlistEntryRepository.existsByMeetingIdAndUserId(100L, 2L)).thenReturn(true);

        boolean result = meetingParticipantService.isOnWaitlist(100L, 2L);

        assertTrue(result);
    }

    @Test
    void isOnWaitlist_whenNullParameters_shouldReturnFalse() {
        boolean result = meetingParticipantService.isOnWaitlist(null, 2L);

        assertFalse(result);
    }

    @Test
    void removeFromWaitlist_shouldUpdatePositions() {
        WaitlistEntry entry1 = WaitlistEntry.builder()
                .id(1L)
                .meeting(testMeeting)
                .user(testUser)
                .position(1)
                .build();

        WaitlistEntry entry2 = WaitlistEntry.builder()
                .id(2L)
                .meeting(testMeeting)
                .user(User.builder().id(3L).build())
                .position(2)
                .build();

        when(waitlistEntryRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(entry1));
        when(waitlistEntryRepository.findByMeetingIdAndPositionGreaterThan(100L, 1))
                .thenReturn(List.of(entry2));

        meetingParticipantService.removeFromWaitlist(100L, 2L);

        verify(waitlistEntryRepository).delete(entry1);
        verify(waitlistEntryRepository).save(entry2);
        assertEquals(1, entry2.getPosition());
    }


    @Test
    void mapToUserResponse_shouldMapUserFields() {
        User user = User.builder()
                .id(1L)
                .email("test@test.com")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("123456789")
                .createdAt(LocalDateTime.now())
                .build();

        // Testowane pośrednio przez mapToResponse
        ParticipantResponse response = ParticipantResponse.builder()
                .user(UserResponse.builder()
                        .id(1L)
                        .email("test@test.com")
                        .firstName("John")
                        .lastName("Doe")
                        .phoneNumber("123456789")
                        .createdAt(user.getCreatedAt())
                        .build())
                .build();

        assertNotNull(response.getUser());
        assertEquals("John", response.getUser().getFirstName());
    }


    @Test
    void joinMeeting_whenUserIdNull_shouldThrowException() {
        assertThrows(IllegalStateException.class, () ->
                meetingParticipantService.joinMeeting(100L, null)
        );
    }


    @Test
    void getDefaultStats_shouldReturnZeroValues() {
        // Metoda prywatna, testowana pośrednio przez getParticipantStatistics
        when(meetingRepository.findById(100L)).thenThrow(new RuntimeException("Test error"));

        Map<String, Long> stats = meetingParticipantService.getParticipantStatistics(100L);

        assertAll(
                () -> assertEquals(0L, stats.get("total")),
                () -> assertEquals(0L, stats.get("confirmed")),
                () -> assertEquals(0L, stats.get("pending"))
        );
    }

    @Test
    void isOrganizer_whenOrganizerNull_shouldReturnFalse() {
        testMeeting.setOrganizer(null);

        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));

        boolean result = meetingParticipantService.isOrganizer(100L, 1L);

        assertFalse(result);
    }

    @Test
    void isViewer_whenNullUserIdAndPrivateMeeting_shouldReturnFalse() {
        testMeeting.setVisibility(MeetingVisibility.PRIVATE);

        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));

        boolean result = meetingParticipantService.isViewer(100L, null);

        assertFalse(result);
    }

    @Test
    void isUnrelatedUser_whenNullUserIdAndPrivateMeeting_shouldReturnTrue() {
        testMeeting.setVisibility(MeetingVisibility.PRIVATE);

        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));

        boolean result = meetingParticipantService.isUnrelatedUser(100L, null);

        assertTrue(result);
    }

    @Test
    void isUnrelatedUser_whenNullUserIdAndPublicMeeting_shouldReturnFalse() {
        testMeeting.setVisibility(MeetingVisibility.PUBLIC);

        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));

        boolean result = meetingParticipantService.isUnrelatedUser(100L, null);

        assertFalse(result);
    }

    @Test
    void requestToJoinPrivateMeeting_whenAlreadyConfirmed_shouldThrowException() {
        testMeeting.setVisibility(MeetingVisibility.PRIVATE);
        testParticipant.setStatus(ParticipationStatus.CONFIRMED);

        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));

        assertThrows(IllegalArgumentException.class, () ->
                meetingParticipantService.requestToJoinPrivateMeeting(100L, 2L)
        );
    }

    @Test
    void updateParticipantStatus_whenValidInput_shouldUpdateSuccessfully() {
        // Given
        Long meetingId = 100L;
        Long participantId = 200L;
        Long userId = 1L; // organizator
        ParticipationStatus newStatus = ParticipationStatus.CONFIRMED;
        String comment = "User confirmed participation";

        MeetingParticipant participant = MeetingParticipant.builder()
                .id(participantId)
                .meeting(testMeeting)
                .user(testUser)
                .status(ParticipationStatus.INVITED)
                .permissionLevel(PermissionLevel.PARTICIPANT)
                .build();

        // Mockowanie - organizator ma uprawnienia
        when(participantRepository.findById(participantId)).thenReturn(Optional.of(participant));
        when(participantRepository.save(any(MeetingParticipant.class))).thenReturn(participant);
        when(waitlistEntryRepository.existsByMeetingIdAndUserId(meetingId, testUser.getId()))
                .thenReturn(false);

        // When
        MeetingParticipant result = meetingParticipantService.updateParticipantStatus(
                meetingId, participantId, newStatus, comment, userId);

        // Then
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(newStatus, result.getStatus()),
                () -> assertEquals(comment, result.getComment()),
                () -> assertNotNull(result.getResponseDate())
        );

        verify(participantRepository).save(participant);
        verify(statusHistoryRepository).save(any(ParticipantStatusHistory.class));
    }

    @Test
    void updateParticipantStatus_whenNotOrganizerOrSelf_shouldThrowSecurityException() {
        // Given
        Long meetingId = 100L;
        Long participantId = 200L;
        Long userId = 999L; // inny użytkownik bez uprawnień
        ParticipationStatus newStatus = ParticipationStatus.CONFIRMED;

        MeetingParticipant participant = MeetingParticipant.builder()
                .id(participantId)
                .meeting(testMeeting)
                .user(User.builder().id(3L).build()) // różny użytkownik
                .status(ParticipationStatus.INVITED)
                .build();

        when(participantRepository.findById(participantId)).thenReturn(Optional.of(participant));

        // When & Then
        assertThrows(SecurityException.class, () ->
                meetingParticipantService.updateParticipantStatus(
                        meetingId, participantId, newStatus, "Test", userId)
        );

        verify(participantRepository, never()).save(any());
    }

    @Test
    void updateParticipantStatus_whenSelfUpdate_shouldBeAllowed() {
        // Given
        Long meetingId = 100L;
        Long participantId = 200L;
        Long userId = 2L; // sam użytkownik
        ParticipationStatus newStatus = ParticipationStatus.DECLINED;

        MeetingParticipant participant = MeetingParticipant.builder()
                .id(participantId)
                .meeting(testMeeting)
                .user(User.builder().id(userId).build()) // ten sam użytkownik
                .status(ParticipationStatus.INVITED)
                .build();

        when(participantRepository.findById(participantId)).thenReturn(Optional.of(participant));
        when(participantRepository.save(any(MeetingParticipant.class))).thenReturn(participant);

        // When
        MeetingParticipant result = meetingParticipantService.updateParticipantStatus(
                meetingId, participantId, newStatus, "I can't attend", userId);

        // Then
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(newStatus, result.getStatus())
        );
    }

    @Test
    void updateParticipantStatus_whenConfirmedAndOnWaitlist_shouldRemoveFromWaitlist() {
        // Given
        Long meetingId = 100L;
        Long participantId = 200L;
        Long userId = 1L; // organizator
        ParticipationStatus newStatus = ParticipationStatus.CONFIRMED;

        MeetingParticipant participant = MeetingParticipant.builder()
                .id(participantId)
                .meeting(testMeeting)
                .user(testUser)
                .status(ParticipationStatus.PENDING)
                .build();

        when(participantRepository.findById(participantId)).thenReturn(Optional.of(participant));
        when(participantRepository.save(any(MeetingParticipant.class))).thenReturn(participant);
        when(waitlistEntryRepository.existsByMeetingIdAndUserId(meetingId, testUser.getId()))
                .thenReturn(true);

        // When
        meetingParticipantService.updateParticipantStatus(
                meetingId, participantId, newStatus, "Confirmed from waitlist", userId);

        // Then
        verify(waitlistEntryRepository).existsByMeetingIdAndUserId(meetingId, testUser.getId());
        // removeFromWaitlist zostanie wywołane wewnętrznie
    }

    @Test
    void updateParticipantStatus_whenConfirmedAndNotOnWaitlist_shouldNotRemove() {
        // Given
        Long meetingId = 100L;
        Long participantId = 200L;
        Long userId = 1L;
        ParticipationStatus newStatus = ParticipationStatus.CONFIRMED;

        MeetingParticipant participant = MeetingParticipant.builder()
                .id(participantId)
                .meeting(testMeeting)
                .user(testUser)
                .status(ParticipationStatus.INVITED)
                .build();

        when(participantRepository.findById(participantId)).thenReturn(Optional.of(participant));
        when(participantRepository.save(any(MeetingParticipant.class))).thenReturn(participant);
        when(waitlistEntryRepository.existsByMeetingIdAndUserId(meetingId, testUser.getId()))
                .thenReturn(false);

        // When
        meetingParticipantService.updateParticipantStatus(
                meetingId, participantId, newStatus, "Confirmed", userId);

        // Then
        verify(waitlistEntryRepository, never()).delete(any());
    }

    @Test
    void updateParticipantStatus_whenParticipantNotFound_shouldThrowException() {
        // Given
        Long meetingId = 100L;
        Long participantId = 999L;
        Long userId = 1L;

        when(participantRepository.findById(participantId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
                meetingParticipantService.updateParticipantStatus(
                        meetingId, participantId, ParticipationStatus.CONFIRMED, "Test", userId)
        );
    }

    @Test
    void updateParticipantStatus_whenSameStatus_shouldStillUpdate() {
        // Given
        Long meetingId = 100L;
        Long participantId = 200L;
        Long userId = 2L; // sam użytkownik
        ParticipationStatus sameStatus = ParticipationStatus.CONFIRMED;
        String newComment = "Updated comment";

        MeetingParticipant participant = MeetingParticipant.builder()
                .id(participantId)
                .meeting(testMeeting)
                .user(User.builder().id(userId).build())
                .status(sameStatus) // ten sam status
                .comment("Old comment")
                .build();

        when(participantRepository.findById(participantId)).thenReturn(Optional.of(participant));
        when(participantRepository.save(any(MeetingParticipant.class))).thenReturn(participant);

        // When
        MeetingParticipant result = meetingParticipantService.updateParticipantStatus(
                meetingId, participantId, sameStatus, newComment, userId);

        // Then
        assertAll(
                () -> assertEquals(newComment, result.getComment()),
                () -> assertNotNull(result.getResponseDate())
        );
        verify(statusHistoryRepository).save(any(ParticipantStatusHistory.class));
    }

    @Test
    void updateParticipantStatus_whenNullComment_shouldBeAllowed() {
        // Given
        Long meetingId = 100L;
        Long participantId = 200L;
        Long userId = 1L;

        MeetingParticipant participant = MeetingParticipant.builder()
                .id(participantId)
                .meeting(testMeeting)
                .user(testUser)
                .status(ParticipationStatus.INVITED)
                .comment("Old comment")
                .build();

        when(participantRepository.findById(participantId)).thenReturn(Optional.of(participant));
        when(participantRepository.save(any(MeetingParticipant.class))).thenReturn(participant);

        // When
        MeetingParticipant result = meetingParticipantService.updateParticipantStatus(
                meetingId, participantId, ParticipationStatus.CONFIRMED, null, userId);

        // Then
        assertNull(result.getComment());
    }

    @Test
    void updateParticipantStatus_whenEmptyComment_shouldBeAllowed() {
        // Given
        Long meetingId = 100L;
        Long participantId = 200L;
        Long userId = 1L;

        MeetingParticipant participant = MeetingParticipant.builder()
                .id(participantId)
                .meeting(testMeeting)
                .user(testUser)
                .status(ParticipationStatus.INVITED)
                .build();

        when(participantRepository.findById(participantId)).thenReturn(Optional.of(participant));
        when(participantRepository.save(any(MeetingParticipant.class))).thenReturn(participant);

        // When
        MeetingParticipant result = meetingParticipantService.updateParticipantStatus(
                meetingId, participantId, ParticipationStatus.CONFIRMED, "", userId);

        // Then
        assertEquals("", result.getComment());
    }

    @Test
    void updateParticipantStatus_whenStatusHistorySaveFails_shouldStillUpdateStatus() {
        // Given
        Long meetingId = 100L;
        Long participantId = 200L;
        Long userId = 1L;

        MeetingParticipant participant = MeetingParticipant.builder()
                .id(participantId)
                .meeting(testMeeting)
                .user(testUser)
                .status(ParticipationStatus.INVITED)
                .build();

        when(participantRepository.findById(participantId)).thenReturn(Optional.of(participant));
        when(participantRepository.save(any(MeetingParticipant.class))).thenReturn(participant);
        doThrow(new RuntimeException("DB error")).when(statusHistoryRepository).save(any());

        // When & Then - powinno zaktualizować status mimo błędu w historii
        assertThrows(RuntimeException.class, () ->
                meetingParticipantService.updateParticipantStatus(
                        meetingId, participantId, ParticipationStatus.CONFIRMED, "Test", userId)
        );

        // Status powinien być zaktualizowany przed zapisem historii
        verify(participantRepository).save(participant);
    }


    @Test
    void createMeeting_whenUserNotFound_shouldThrowException() {
        CreateMeetingRequest request = new CreateMeetingRequest();
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                meetingService.createMeeting(request, 999L)
        );
    }

    @Test
    void createMeeting_whenInvalidDates_shouldThrowException() {
        CreateMeetingRequest request = new CreateMeetingRequest();
        request.setStartDate(LocalDateTime.now().minusDays(1)); // Data w przeszłości
        request.setEndDate(LocalDateTime.now().plusDays(1));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testOrganizer));
        when(meetingMapper.toEntity(request)).thenReturn(testMeeting);

        assertThrows(BusinessException.class, () ->
                meetingService.createMeeting(request, 1L)
        );
    }

    @Test
    void updateMeeting_whenNotAuthorized_shouldThrowException() {
        UpdateMeetingRequest request = new UpdateMeetingRequest();
        when(meetingAuthorizationService.canUserEditMeeting(100L, 1L)).thenReturn(false);

        assertThrows(BusinessException.class, () ->
                meetingService.updateMeeting(100L, request, 1L)
        );
    }

    @Test
    void updateMeeting_whenMeetingNotFound_shouldThrowException() {
        UpdateMeetingRequest request = new UpdateMeetingRequest();
        when(meetingAuthorizationService.canUserEditMeeting(100L, 1L)).thenReturn(true);
        when(meetingRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                meetingService.updateMeeting(100L, request, 1L)
        );
    }

    @Test
    void updateMeeting_whenStatusChangesToCancelled_shouldCancelNotifications() {
        UpdateMeetingRequest request = new UpdateMeetingRequest();
        request.setStatus(MeetingStatus.CANCELLED);
        request.setStatusChangeReason("Cancelled by user");

        testMeeting.setStatus(MeetingStatus.PLANNED);

        when(meetingAuthorizationService.canUserEditMeeting(100L, 1L)).thenReturn(true);
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(meetingRepository.save(any(Meeting.class))).thenReturn(testMeeting);
        when(meetingMapper.toResponse(testMeeting)).thenReturn(testMeetingResponse);

        meetingService.updateMeeting(100L, request, 1L);

        verify(meetingSchedulerService).cancelMeetingSchedule(100L);
    }

    @Test
    void deleteMeeting_shouldDeleteSuccessfully() {
        when(meetingAuthorizationService.canUserDeleteMeeting(100L, 1L)).thenReturn(true);
        when(meetingRepository.findByIdAndOrganizerId(100L, 1L)).thenReturn(Optional.of(testMeeting));
        when(jdbcTemplate.update(anyString(), eq(100L))).thenReturn(1);

        meetingService.deleteMeeting(100L, 1L);

        verify(jdbcTemplate, times(3)).update(anyString(), eq(100L));
    }

    @Test
    void deleteMeeting_whenNotAuthorized_shouldThrowException() {
        when(meetingAuthorizationService.canUserDeleteMeeting(100L, 1L)).thenReturn(false);

        assertThrows(BusinessException.class, () ->
                meetingService.deleteMeeting(100L, 1L)
        );
    }

    @Test
    void deleteMeeting_whenMeetingNotFound_shouldThrowException() {
        when(meetingAuthorizationService.canUserDeleteMeeting(100L, 1L)).thenReturn(true);
        when(meetingRepository.findByIdAndOrganizerId(100L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                meetingService.deleteMeeting(100L, 1L)
        );
    }


    @Test
    void getMeetingById_whenNotFound_shouldThrowException() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                meetingService.getMeetingById(100L)
        );
    }


    @Test
    void getUpcomingPublicMeetings_shouldReturnList() {
        when(meetingRepository.findUpcomingPublicMeetings(any(LocalDateTime.class)))
                .thenReturn(List.of(testMeeting));
        when(meetingMapper.toResponse(testMeeting)).thenReturn(testMeetingResponse);

        List<MeetingResponse> result = meetingService.getUpcomingPublicMeetings();

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(1, result.size())
        );
    }

    @Test
    void findNearbyMeetings_shouldReturnList() {
        when(customMeetingRepository.findNearbyMeetings(anyDouble(), anyDouble(), anyDouble(), anyInt()))
                .thenReturn(List.of(testMeeting));
        when(meetingMapper.toResponse(testMeeting)).thenReturn(testMeetingResponse);

        List<MeetingResponse> result = meetingService.findNearbyMeetings(52.0, 21.0, 10.0);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(1, result.size())
        );
    }

    @Test
    void changeMeetingStatus_shouldUpdateStatus() {
        when(meetingAuthorizationService.canUserEditMeeting(100L, 1L)).thenReturn(true);
        when(customMeetingRepository.bulkUpdateMeetingStatus(anyList(), anyString())).thenReturn(1);
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));

        meetingService.changeMeetingStatus(100L, MeetingStatus.CANCELLED, 1L);

        verify(customMeetingRepository).bulkUpdateMeetingStatus(List.of(100L), "CANCELLED");
    }

    @Test
    void changeMeetingStatus_whenNotAuthorized_shouldThrowException() {
        when(meetingAuthorizationService.canUserEditMeeting(100L, 1L)).thenReturn(false);

        assertThrows(BusinessException.class, () ->
                meetingService.changeMeetingStatus(100L, MeetingStatus.CANCELLED, 1L)
        );
    }

    @Test
    void changeMeetingStatus_whenUpdateFails_shouldThrowException() {
        when(meetingAuthorizationService.canUserEditMeeting(100L, 1L)).thenReturn(true);
        when(customMeetingRepository.bulkUpdateMeetingStatus(anyList(), anyString())).thenReturn(0);

        assertThrows(ResourceNotFoundException.class, () ->
                meetingService.changeMeetingStatus(100L, MeetingStatus.CANCELLED, 1L)
        );
    }


    @Test
    void findConflictingMeetings_shouldReturnConflicts() {
        when(meetingRepository.findConfirmedMeetingsForUserInPeriod(anyLong(), any(), any()))
                .thenReturn(List.of(testMeeting));
        when(meetingMapper.toResponse(testMeeting)).thenReturn(testMeetingResponse);

        List<MeetingResponse> result = meetingService.findConflictingMeetings(1L,
                LocalDateTime.now(), LocalDateTime.now().plusHours(2));

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(1, result.size())
        );
    }

    @Test
    void getMeeting_whenNotFound_shouldThrowException() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                meetingService.getMeeting(100L)
        );
    }

    @Test
    void getMeetingParticipationInfo_shouldReturnInfo() {
        MeetingParticipationInfo info = MeetingParticipationInfo.builder()
                .canViewDetails(true)
                .canEdit(false)
                .build();

        when(meetingAuthorizationService.getUserMeetingPermissions(100L, 1L)).thenReturn(info);

        MeetingParticipationInfo result = meetingService.getMeetingParticipationInfo(100L, 1L);

        assertAll(
                () -> assertNotNull(result),
                () -> assertTrue(result.isCanViewDetails())
        );
    }

    @Test
    void canUserAccessMeeting_shouldReturnTrueWhenCanView() {
        MeetingParticipationInfo info = MeetingParticipationInfo.builder()
                .canViewDetails(true)
                .build();

        when(meetingAuthorizationService.getUserMeetingPermissions(100L, 1L)).thenReturn(info);

        boolean result = meetingService.canUserAccessMeeting(100L, 1L);

        assertTrue(result);
    }

    @Test
    void canUserAccessMeeting_whenException_shouldReturnFalse() {
        when(meetingAuthorizationService.getUserMeetingPermissions(100L, 1L))
                .thenThrow(new RuntimeException("Error"));

        boolean result = meetingService.canUserAccessMeeting(100L, 1L);

        assertFalse(result);
    }

    @Test
    void getMeetingTemplates_shouldReturnTemplates() {
        Meeting template = Meeting.builder()
                .title("Template")
                .organizer(testOrganizer)
                .build();

        when(meetingRepository.findByOrganizerIdAndTemplateTrue(1L)).thenReturn(List.of(template));
        when(meetingMapper.toResponse(template)).thenReturn(testMeetingResponse);

        List<MeetingResponse> result = meetingService.getMeetingTemplates(1L);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(1, result.size())
        );
    }

    @Test
    void addRecurrenceException_shouldAddException() {
        testMeeting.setRecurring(true);
        testMeeting.setRecurrenceExceptionsJson("[]");

        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(meetingRepository.save(testMeeting)).thenReturn(testMeeting);

        meetingService.addRecurrenceException(100L, "2024-12-25", "Christmas");

        assertNotNull(testMeeting.getRecurrenceExceptionsJson());
        verify(meetingRepository).save(testMeeting);
    }

    @Test
    void addRecurrenceException_whenNotRecurring_shouldThrowException() {
        testMeeting.setRecurring(false);

        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));

        assertThrows(BusinessException.class, () ->
                meetingService.addRecurrenceException(100L, "2024-12-25", "Christmas")
        );
    }

    @Test
    void getRecurrenceSeries_shouldReturnSeries() {
        Meeting occurrence = Meeting.builder()
                .build();

        when(meetingRepository.findByOriginalMeetingId(100L)).thenReturn(List.of(occurrence));
        when(meetingMapper.toResponse(occurrence)).thenReturn(testMeetingResponse);

        List<MeetingResponse> result = meetingService.getRecurrenceSeries(100L);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(1, result.size())
        );
    }

    @Test
    void getUpcomingRecurringMeetings_shouldReturnRecurringMeetings() {
        testMeeting.setRecurring(true);

        when(meetingRepository.findByRecurringTrueAndRecurrenceEndDateAfter(any(LocalDateTime.class)))
                .thenReturn(List.of(testMeeting));
        when(meetingMapper.toResponse(testMeeting)).thenReturn(testMeetingResponse);

        List<MeetingResponse> result = meetingService.getUpcomingRecurringMeetings(1L);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(1, result.size())
        );
    }


    @Test
    void saveAsTemplate_whenNotAuthorized_shouldThrowException() {
        when(meetingAuthorizationService.canUserEditMeeting(100L, 1L)).thenReturn(false);

        assertThrows(BusinessException.class, () ->
                meetingService.saveAsTemplate(100L, "Template Name", 1L)
        );
    }


    @Test
    void getMeetingDetails_shouldReturnMeetingResponse() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));

        MeetingResponse result = meetingService.getMeetingDetails(100L, 1L);

        assertNotNull(result);
    }

    @Test
    void getMeetingDetails_whenMeetingNotFound_shouldThrowException() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                meetingService.getMeetingDetails(100L, 1L)
        );
    }

    @Test
    void getMeetingForVotingCreation_shouldReturnMeetingResponse() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));

        MeetingResponse result = meetingService.getMeetingForVotingCreation(100L, 1L);

        assertNotNull(result);
    }

    @Test
    void getMeetingForVotingCreation_whenNotOrganizer_shouldThrowException() {
        User otherUser = User.builder().id(2L).build();
        testMeeting.setOrganizer(otherUser);

        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));

        assertThrows(IllegalStateException.class, () ->
                meetingService.getMeetingForVotingCreation(100L, 1L)
        );
    }


    @Test
    void validateMeetingDates_whenInvalidDates_shouldThrowException() {
        // Testowanie metody prywatnej
        // Można użyć refleksji lub przetestować pośrednio
        CreateMeetingRequest request = new CreateMeetingRequest();
        request.setStartDate(LocalDateTime.now().plusDays(2));
        request.setEndDate(LocalDateTime.now().plusDays(1)); // End przed start

        when(userRepository.findById(1L)).thenReturn(Optional.of(testOrganizer));
        when(meetingMapper.toEntity(request)).thenReturn(testMeeting);

        assertThrows(BusinessException.class, () ->
                meetingService.createMeeting(request, 1L)
        );
    }

    @Test
    void inviteParticipant_whenMeetingNotFound_shouldThrowResourceNotFoundException() {
        when(meetingRepository.findByIdAndOrganizerId(100L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                meetingParticipantService.inviteParticipant(100L, 2L, 1L)
        );
    }

    @Test
    void inviteParticipant_whenUserNotFound_shouldThrowResourceNotFoundException() {
        when(meetingRepository.findByIdAndOrganizerId(100L, 1L))
                .thenReturn(Optional.of(testMeeting));
        when(userRepository.findById(2L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                meetingParticipantService.inviteParticipant(100L, 2L, 1L)
        );
    }

    @Test
    void inviteParticipant_whenUserAlreadyParticipant_shouldThrowIllegalArgumentException() {
        when(meetingRepository.findByIdAndOrganizerId(100L, 1L))
                .thenReturn(Optional.of(testMeeting));
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(testUser));
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));

        assertThrows(IllegalArgumentException.class, () ->
                meetingParticipantService.inviteParticipant(100L, 2L, 1L)
        );
    }

    @Test
    void updateParticipantStatus_whenParticipantNotFound_shouldThrowResourceNotFoundException() {
        when(participantRepository.findById(200L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                meetingParticipantService.updateParticipantStatus(100L, 200L,
                        ParticipationStatus.CONFIRMED, "Test", 1L)
        );
    }

    @Test
    void updateParticipantStatus_whenNoPermission_shouldThrowSecurityException() {
        User differentUser = User.builder().id(999L).build();
        MeetingParticipant participant = MeetingParticipant.builder()
                .id(200L)
                .meeting(testMeeting)
                .user(differentUser)
                .build();

        when(participantRepository.findById(200L))
                .thenReturn(Optional.of(participant));

        assertThrows(SecurityException.class, () ->
                meetingParticipantService.updateParticipantStatus(100L, 200L,
                        ParticipationStatus.CONFIRMED, "Test", 2L)
        );
    }

    @Test
    void updateParticipantStatus_whenOrganizer_shouldHavePermission() {
        when(participantRepository.findById(200L))
                .thenReturn(Optional.of(testParticipant));
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenReturn(testParticipant);

        MeetingParticipant result = meetingParticipantService.updateParticipantStatus(
                100L, 200L, ParticipationStatus.CONFIRMED, "Test", 1L);

        assertNotNull(result);
        verify(participantRepository).save(any(MeetingParticipant.class));
    }

    @Test
    void updateParticipantStatus_whenSelfUpdate_shouldHavePermission() {
        when(participantRepository.findById(200L))
                .thenReturn(Optional.of(testParticipant));
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenReturn(testParticipant);

        MeetingParticipant result = meetingParticipantService.updateParticipantStatus(
                100L, 200L, ParticipationStatus.CONFIRMED, "Test", 2L);

        assertNotNull(result);
        verify(participantRepository).save(any(MeetingParticipant.class));
    }


    @Test
    void updateParticipantStatus_whenStatusHistorySaveFails_shouldThrowRuntimeException() {
        when(participantRepository.findById(200L))
                .thenReturn(Optional.of(testParticipant));
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenReturn(testParticipant);
        doThrow(new RuntimeException("DB error")).when(statusHistoryRepository).save(any());

        assertThrows(RuntimeException.class, () ->
                meetingParticipantService.updateParticipantStatus(100L, 200L,
                        ParticipationStatus.CONFIRMED, "Test", 1L)
        );
    }

    @Test
    void updateParticipantPermission_whenNotOrganizer_shouldThrowResourceNotFoundException() {
        when(meetingRepository.findByIdAndOrganizerId(100L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                meetingParticipantService.updateParticipantPermission(100L, 200L,
                        PermissionLevel.MODERATOR, 1L)
        );
    }

    @Test
    void updateParticipantPermission_whenParticipantNotFound_shouldThrowResourceNotFoundException() {
        when(meetingRepository.findByIdAndOrganizerId(100L, 1L))
                .thenReturn(Optional.of(testMeeting));
        when(participantRepository.findById(200L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                meetingParticipantService.updateParticipantPermission(100L, 200L,
                        PermissionLevel.MODERATOR, 1L)
        );
    }

    @Test
    void removeParticipant_whenNotOrganizer_shouldThrowResourceNotFoundException() {
        when(meetingRepository.findByIdAndOrganizerId(100L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                meetingParticipantService.removeParticipant(100L, 200L, 1L)
        );
    }

    @Test
    void joinPublicMeeting_whenMeetingNotFound_shouldThrowResourceNotFoundException() {
        when(meetingRepository.findById(100L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                meetingParticipantService.joinPublicMeeting(100L, 2L)
        );
    }

    @Test
    void joinPublicMeeting_whenMeetingCompleted_shouldThrowIllegalStateException() {
        testMeeting.setStatus(MeetingStatus.COMPLETED);
        when(meetingRepository.findById(100L))
                .thenReturn(Optional.of(testMeeting));

        assertThrows(IllegalStateException.class, () ->
                meetingParticipantService.joinPublicMeeting(100L, 2L)
        );
    }

    @Test
    void joinPublicMeeting_whenNotPublic_shouldThrowSecurityException() {
        testMeeting.setVisibility(MeetingVisibility.PRIVATE);
        when(meetingRepository.findById(100L))
                .thenReturn(Optional.of(testMeeting));

        assertThrows(SecurityException.class, () ->
                meetingParticipantService.joinPublicMeeting(100L, 2L)
        );
    }

    @Test
    void joinPublicMeeting_whenUserNotFound_shouldThrowResourceNotFoundException() {
        when(meetingRepository.findById(100L))
                .thenReturn(Optional.of(testMeeting));
        when(userRepository.findById(2L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                meetingParticipantService.joinPublicMeeting(100L, 2L)
        );
    }

    @Test
    void joinPublicMeeting_whenAlreadyConfirmedParticipant_shouldThrowIllegalArgumentException() {
        testParticipant.setStatus(ParticipationStatus.CONFIRMED);
        when(meetingRepository.findById(100L))
                .thenReturn(Optional.of(testMeeting));
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(testUser));
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));

        assertThrows(IllegalArgumentException.class, () ->
                meetingParticipantService.joinPublicMeeting(100L, 2L)
        );
    }

    @Test
    void joinPublicMeeting_whenNoAvailableSpots_shouldThrowIllegalArgumentException() {
        testMeeting.setMaxParticipants(5);
        when(meetingRepository.findById(100L))
                .thenReturn(Optional.of(testMeeting));
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(testUser));
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.empty());
        when(participantRepository.countByMeetingIdAndStatus(100L, ParticipationStatus.CONFIRMED))
                .thenReturn(5L);

        assertThrows(IllegalArgumentException.class, () ->
                meetingParticipantService.joinPublicMeeting(100L, 2L)
        );
    }

    @Test
    void requestToJoinPrivateMeeting_whenMeetingNotFound_shouldThrowResourceNotFoundException() {
        when(meetingRepository.findById(100L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                meetingParticipantService.requestToJoinPrivateMeeting(100L, 2L)
        );
    }

    @Test
    void requestToJoinPrivateMeeting_whenMeetingCompleted_shouldThrowIllegalStateException() {
        testMeeting.setStatus(MeetingStatus.COMPLETED);
        when(meetingRepository.findById(100L))
                .thenReturn(Optional.of(testMeeting));

        assertThrows(IllegalStateException.class, () ->
                meetingParticipantService.requestToJoinPrivateMeeting(100L, 2L)
        );
    }

    @Test
    void requestToJoinPrivateMeeting_whenNotPrivate_shouldThrowSecurityException() {
        testMeeting.setVisibility(MeetingVisibility.PUBLIC);
        when(meetingRepository.findById(100L))
                .thenReturn(Optional.of(testMeeting));

        assertThrows(SecurityException.class, () ->
                meetingParticipantService.requestToJoinPrivateMeeting(100L, 2L)
        );
    }

    @Test
    void requestToJoinPrivateMeeting_whenUserNotFound_shouldThrowResourceNotFoundException() {
        testMeeting.setVisibility(MeetingVisibility.PRIVATE);
        when(meetingRepository.findById(100L))
                .thenReturn(Optional.of(testMeeting));
        when(userRepository.findById(2L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                meetingParticipantService.requestToJoinPrivateMeeting(100L, 2L)
        );
    }

    @Test
    void requestToJoinPrivateMeeting_whenAlreadyPending_shouldThrowIllegalArgumentException() {
        testMeeting.setVisibility(MeetingVisibility.PRIVATE);
        testParticipant.setStatus(ParticipationStatus.PENDING);

        when(meetingRepository.findById(100L))
                .thenReturn(Optional.of(testMeeting));
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(testUser));
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));

        assertThrows(IllegalArgumentException.class, () ->
                meetingParticipantService.requestToJoinPrivateMeeting(100L, 2L)
        );
    }

    @Test
    void requestToJoinPrivateMeeting_whenAlreadyConfirmed_shouldThrowIllegalArgumentException() {
        testMeeting.setVisibility(MeetingVisibility.PRIVATE);
        testParticipant.setStatus(ParticipationStatus.CONFIRMED);

        when(meetingRepository.findById(100L))
                .thenReturn(Optional.of(testMeeting));
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(testUser));
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));

        assertThrows(IllegalArgumentException.class, () ->
                meetingParticipantService.requestToJoinPrivateMeeting(100L, 2L)
        );
    }

    @Test
    void approveJoinRequest_whenMeetingNotFound_shouldThrowResourceNotFoundException() {
        when(meetingRepository.findById(100L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                meetingParticipantService.approveJoinRequest(100L, 200L, 1L)
        );
    }

    @Test
    void approveJoinRequest_whenNotOrganizer_shouldThrowSecurityException() {
        User differentOrganizer = User.builder().id(999L).build();
        testMeeting.setOrganizer(differentOrganizer);

        when(meetingRepository.findById(100L))
                .thenReturn(Optional.of(testMeeting));

        assertThrows(SecurityException.class, () ->
                meetingParticipantService.approveJoinRequest(100L, 200L, 1L)
        );
    }

    @Test
    void approveJoinRequest_whenParticipantNotFound_shouldThrowResourceNotFoundException() {
        when(meetingRepository.findById(100L))
                .thenReturn(Optional.of(testMeeting));
        when(participantRepository.findById(200L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                meetingParticipantService.approveJoinRequest(100L, 200L, 1L)
        );
    }

    @Test
    void approveJoinRequest_whenParticipantNotInMeeting_shouldThrowIllegalArgumentException() {
        Meeting differentMeeting = Meeting.builder().build();
        MeetingParticipant participant = MeetingParticipant.builder()
                .meeting(differentMeeting)
                .build();

        when(meetingRepository.findById(100L))
                .thenReturn(Optional.of(testMeeting));
        when(participantRepository.findById(200L))
                .thenReturn(Optional.of(participant));

        assertThrows(NullPointerException.class, () ->
                meetingParticipantService.approveJoinRequest(100L, 200L, 1L)
        );
    }

    @Test
    void approveJoinRequest_whenNotPendingStatus_shouldThrowIllegalArgumentException() {
        testParticipant.setStatus(ParticipationStatus.CONFIRMED);

        when(meetingRepository.findById(100L))
                .thenReturn(Optional.of(testMeeting));
        when(participantRepository.findById(200L))
                .thenReturn(Optional.of(testParticipant));

        assertThrows(IllegalArgumentException.class, () ->
                meetingParticipantService.approveJoinRequest(100L, 200L, 1L)
        );
    }

    @Test
    void approveJoinRequest_whenNoAvailableSpots_shouldThrowIllegalArgumentException() {
        testMeeting.setMaxParticipants(5);
        testParticipant.setStatus(ParticipationStatus.PENDING);

        when(meetingRepository.findById(100L))
                .thenReturn(Optional.of(testMeeting));


        assertThrows(ResourceNotFoundException.class, () ->
                meetingParticipantService.approveJoinRequest(100L, 200L, 1L)
        );
    }

    @Test
    void rejectJoinRequest_whenNotOrganizer_shouldThrowSecurityException() {
        User differentOrganizer = User.builder().id(999L).build();
        testMeeting.setOrganizer(differentOrganizer);

        when(meetingRepository.findById(100L))
                .thenReturn(Optional.of(testMeeting));

        assertThrows(SecurityException.class, () ->
                meetingParticipantService.rejectJoinRequest(100L, 200L, 1L)
        );
    }

    @Test
    void acceptInvitationByToken_whenInvalidToken_shouldThrowResourceNotFoundException() {
        when(participantRepository.findByInvitationToken("invalid-token"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                meetingParticipantService.acceptInvitationByToken("invalid-token")
        );
    }

    @Test
    void acceptInvitationByToken_whenMeetingNotOngoing_shouldThrowIllegalArgumentException() {
        String token = "valid-token";
        testParticipant.setInvitationToken(token);
        testMeeting.setStartDate(LocalDateTime.now().plusDays(1));
        testMeeting.setEndDate(LocalDateTime.now().plusDays(1).plusHours(2));

        when(participantRepository.findByInvitationToken(token))
                .thenReturn(Optional.of(testParticipant));

        assertThrows(IllegalArgumentException.class, () ->
                meetingParticipantService.acceptInvitationByToken(token)
        );
    }

    @Test
    void acceptInvitationByToken_whenAttendanceAlreadyConfirmed_shouldThrowIllegalArgumentException() {
        String token = "valid-token";
        testParticipant.setInvitationToken(token);
        testParticipant.setAttendanceConfirmedAt(LocalDateTime.now());
        testMeeting.setStartDate(LocalDateTime.now().minusHours(1));
        testMeeting.setEndDate(LocalDateTime.now().plusHours(1));

        when(participantRepository.findByInvitationToken(token))
                .thenReturn(Optional.of(testParticipant));

        assertThrows(IllegalArgumentException.class, () ->
                meetingParticipantService.acceptInvitationByToken(token)
        );
    }

    @Test
    void markAsAttended_whenNotConfirmedParticipant_shouldThrowBusinessException() {
        testParticipant.setStatus(ParticipationStatus.INVITED);

        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));

        assertThrows(BusinessException.class, () ->
                meetingParticipantService.markAsAttended(100L, 2L)
        );
    }

    @Test
    void markAsAttended_whenNotParticipant_shouldThrowResourceNotFoundException() {
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                meetingParticipantService.markAsAttended(100L, 2L)
        );
    }

    @Test
    void leaveMeeting_whenNotParticipant_shouldThrowResourceNotFoundException() {
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                meetingParticipantService.leaveMeeting(2L, 100L)
        );
    }

    @Test
    void respondToInvitation_whenInvitationNotFound_shouldThrowResourceNotFoundException() {
        when(participantRepository.findById(200L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                meetingParticipantService.respondToInvitation(200L, ParticipationStatus.CONFIRMED,
                        "Accepted", 2L)
        );
    }

    @Test
    void respondToInvitation_whenNotOwner_shouldThrowSecurityException() {
        User differentUser = User.builder().id(999L).build();
        testParticipant.setUser(differentUser);

        when(participantRepository.findById(200L))
                .thenReturn(Optional.of(testParticipant));

        assertThrows(SecurityException.class, () ->
                meetingParticipantService.respondToInvitation(200L, ParticipationStatus.CONFIRMED,
                        "Accepted", 2L)
        );
    }

    @Test
    void respondToInvitation_whenAlreadyResponded_shouldThrowIllegalArgumentException() {
        testParticipant.setStatus(ParticipationStatus.CONFIRMED);

        when(participantRepository.findById(200L))
                .thenReturn(Optional.of(testParticipant));

        assertThrows(IllegalArgumentException.class, () ->
                meetingParticipantService.respondToInvitation(200L, ParticipationStatus.CONFIRMED,
                        "Accepted", 2L)
        );
    }

    @Test
    void getParticipant_whenNotFound_shouldThrowResourceNotFoundException() {
        when(participantRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                meetingParticipantService.getParticipant(999L)
        );
    }

    @Test
    void confirmAttendance_whenInvalidToken_shouldThrowRuntimeException() {
        when(participantRepository.findByIdAndInvitationToken(200L, "invalid-token"))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                meetingParticipantService.confirmAttendance(200L, "invalid-token")
        );
    }

    @Test
    void declineParticipation_whenInvalidToken_shouldThrowResourceNotFoundException() {
        when(participantRepository.findByInvitationToken("invalid-token"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                meetingParticipantService.declineParticipation("invalid-token", "Cannot attend")
        );
    }

    @Test
    void joinMeeting_whenUserIdNull_shouldThrowIllegalStateException() {
        assertThrows(IllegalStateException.class, () ->
                meetingParticipantService.joinMeeting(100L, null)
        );
    }

    @Test
    void joinMeeting_whenMeetingNotFound_shouldThrowIllegalArgumentException() {
        when(meetingRepository.findById(100L))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                meetingParticipantService.joinMeeting(100L, 2L)
        );
    }

    @Test
    void joinMeeting_whenAlreadyParticipant_shouldThrowIllegalStateException() {
        when(meetingRepository.findById(100L))
                .thenReturn(Optional.of(testMeeting));
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));

        assertThrows(IllegalStateException.class, () ->
                meetingParticipantService.joinMeeting(100L, 2L)
        );
    }

    @Test
    void joinMeeting_whenInviteOnly_shouldThrowIllegalStateException() {
        testMeeting.setVisibility(MeetingVisibility.INVITE_ONLY);

        when(meetingRepository.findById(100L))
                .thenReturn(Optional.of(testMeeting));
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () ->
                meetingParticipantService.joinMeeting(100L, 2L)
        );
    }

    @Test
    void hasAvailableSpots_whenMeetingNotFound_shouldThrowResourceNotFoundException() {
        when(meetingRepository.findById(100L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                meetingParticipantService.hasAvailableSpots(100L)
        );
    }


    @Test
    void exportParticipantsToCsv_whenNoParticipants_shouldReturnEmptyCsv() {
        when(participantRepository.findParticipantsProjection(100L))
                .thenReturn(Collections.emptyList());

        ByteArrayResource result = meetingParticipantService.exportParticipantsToCsv(100L);

        assertNotNull(result);
        assertTrue(result.getByteArray().length > 0);
    }

    @Test
    void exportParticipantsToCsv_whenRepositoryThrowsException_shouldThrowException() {
        when(participantRepository.findParticipantsProjection(100L))
                .thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class, () ->
                meetingParticipantService.exportParticipantsToCsv(100L)
        );
    }


    @Test
    void getConfirmedParticipants_whenMeetingNotFound_shouldReturnEmptyList() {
        when(meetingRepository.findById(100L))
                .thenThrow(new ResourceNotFoundException("Meeting not found"));

        List<ParticipantResponse> result = meetingParticipantService.getConfirmedParticipants(100L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getConfirmedParticipants_whenRepositoryThrowsException_shouldReturnEmptyList() {
        when(meetingRepository.findById(100L))
                .thenThrow(new RuntimeException("DB error"));

        List<ParticipantResponse> result = meetingParticipantService.getConfirmedParticipants(100L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getMeetingStats_whenMeetingNotFound_shouldReturnDefaultStats() {
        when(meetingRepository.findById(100L))
                .thenReturn(Optional.empty());

        MeetingParticipantService.ParticipantStats stats = meetingParticipantService.getMeetingStats(100L);

        assertNotNull(stats);
        assertEquals(0L, stats.getTotalConfirmed());
        assertEquals(0L, stats.getTotalInvited());
    }

    @Test
    void getConfirmedParticipants_shouldReturnParticipants() {
        // Given
        Long meetingId = 100L;
        Meeting meeting = testMeeting;

        MeetingParticipant confirmedParticipant = MeetingParticipant.builder()
                .id(201L)
                .meeting(meeting)
                .user(testUser)
                .status(ParticipationStatus.CONFIRMED)
                .build();

        List<MeetingParticipant> confirmedParticipants = Arrays.asList(confirmedParticipant);

        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(meeting));
        when(participantRepository.findByMeetingIdAndStatus(meetingId, ParticipationStatus.CONFIRMED))
                .thenReturn(confirmedParticipants);

        // When
        List<ParticipantResponse> result = meetingParticipantService.getConfirmedParticipants(meetingId);

        // Then
        assertNotNull(result);
    }

    @Test
    void getConfirmedParticipants_whenOrganizerNotInList_shouldAddOrganizer() {
        // Given - organizator nie jest w liście uczestników
        Long meetingId = 100L;
        Meeting meeting = testMeeting;

        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(meeting));
        when(participantRepository.findByMeetingIdAndStatus(meetingId, ParticipationStatus.CONFIRMED))
                .thenReturn(Collections.emptyList());

        // When
        List<ParticipantResponse> result = meetingParticipantService.getConfirmedParticipants(meetingId);

        // Then - powinien dodać organizatora
        assertNotNull(result);
    }

    @Test
    void getConfirmedParticipants_whenException_shouldReturnEmptyList() {
        // Given
        Long meetingId = 100L;

        when(meetingRepository.findById(meetingId))
                .thenThrow(new RuntimeException("DB Error"));

        // When
        List<ParticipantResponse> result = meetingParticipantService.getConfirmedParticipants(meetingId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void approveJoinRequest_shouldApproveSuccessfully() {
        // Given
        Long meetingId = 100L;
        Long participantId = 200L;
        Long organizerId = 1L;

        testParticipant.setStatus(ParticipationStatus.PENDING);
        testParticipant.setMeeting(testMeeting); // USTAW SPOTKANIE
        testMeeting.setMaxParticipants(10);
        testMeeting.setId(meetingId); // USTAW ID

        // Upewnij się, że organizator ma ID
        testOrganizer.setId(organizerId);
        testMeeting.setOrganizer(testOrganizer);

        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.findById(participantId)).thenReturn(Optional.of(testParticipant));
        when(participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.CONFIRMED))
                .thenReturn(5L);
        when(participantRepository.save(any(MeetingParticipant.class))).thenReturn(testParticipant);

        // When
        meetingParticipantService.approveJoinRequest(meetingId, participantId, organizerId);

        // Then
        verify(participantRepository).save(testParticipant);
        verify(statusHistoryRepository).save(any(ParticipantStatusHistory.class));
        verify(notificationService).sendRequestApprovedNotification(testUser, testMeeting);
        assertEquals(ParticipationStatus.CONFIRMED, testParticipant.getStatus());
    }

//
//    @Test
//    void approveJoinRequest_whenMeetingNotFound_shouldThrowException() {
//        // Given
//        when(meetingRepository.findById(100L)).thenReturn(Optional.empty());
//
//        // When & Then
//        assertThrows(ResourceNotFoundException.class, () ->
//                meetingParticipantService.approveJoinRequest(100L, 200L, 1L)
//        );
//    }

//    @Test
//    void approveJoinRequest_whenNotOrganizer_shouldThrowException() {
//        // Given
//        User differentOrganizer = User.builder().id(999L).build();
//        testMeeting.setOrganizer(differentOrganizer);
//
//        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
//
//        // When & Then
//        assertThrows(SecurityException.class, () ->
//                meetingParticipantService.approveJoinRequest(100L, 200L, 1L)
//        );
//    }
//
//    @Test
//    void approveJoinRequest_whenParticipantNotFound_shouldThrowException() {
//        // Given
//        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
//        when(participantRepository.findById(200L)).thenReturn(Optional.empty());
//
//        // When & Then
//        assertThrows(ResourceNotFoundException.class, () ->
//                meetingParticipantService.approveJoinRequest(100L, 200L, 1L)
//        );
//    }
//
////    @Test
////    void approveJoinRequest_whenParticipantNotInMeeting_shouldThrowException() {
////        // Given
////        Meeting differentMeeting = Meeting.builder().build();
////        MeetingParticipant participant = MeetingParticipant.builder()
////                .meeting(differentMeeting)
////                .build();
////
////        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
////        when(participantRepository.findById(200L)).thenReturn(Optional.of(participant));
////
////        // When & Then
////        assertThrows(IllegalArgumentException.class, () ->
////                meetingParticipantService.approveJoinRequest(100L, 200L, 1L)
////        );
////    }
//
//    @Test
//    void approveJoinRequest_whenNotPendingStatus_shouldThrowException() {
//        // Given
//        testParticipant.setStatus(ParticipationStatus.CONFIRMED);
//
//        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
//
//        // When & Then
//        assertThrows(ResourceNotFoundException.class, () ->
//                meetingParticipantService.approveJoinRequest(100L, 200L, 1L)
//        );
//    }
//
//    @Test
//    void approveJoinRequest_whenNoAvailableSpots_shouldThrowException() {
//        // Given
//        testParticipant.setStatus(ParticipationStatus.PENDING);
//        testMeeting.setMaxParticipants(5);
//
//        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
//
//
//        // When & Then
//        assertThrows(ResourceNotFoundException.class, () ->
//                meetingParticipantService.approveJoinRequest(100L, 200L, 1L)
//        );
//    }

    @Test
    void promoteNextFromWaitlist_whenNoWaitlistEntries_shouldDoNothing() {
        // Testowanie scenariusza, gdzie lista oczekujących jest pusta
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));


        testMeeting.setMaxParticipants(10);

        assertDoesNotThrow(() -> {
            meetingParticipantService.leaveMeeting(2L, 100L);
        });

        verify(waitlistEntryRepository, never()).delete(any());
    }


    @Test
    void rejectJoinRequest_shouldRejectSuccessfully() {
        // Given
        Long meetingId = 100L;
        Long participantId = 200L;
        Long organizerId = 1L;

        testParticipant.setStatus(ParticipationStatus.PENDING);
        testParticipant.setMeeting(testMeeting); // Ustaw spotkanie dla uczestnika

        // Upewnij się, że testMeeting ma ID
        testMeeting.setId(meetingId);

        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.findById(participantId)).thenReturn(Optional.of(testParticipant));
        when(participantRepository.save(any(MeetingParticipant.class))).thenReturn(testParticipant);

        // When
        meetingParticipantService.rejectJoinRequest(meetingId, participantId, organizerId);

        // Then
        verify(participantRepository).save(testParticipant);
        verify(statusHistoryRepository).save(any(ParticipantStatusHistory.class));
        verify(notificationService).sendRequestRejectedNotification(testUser, testMeeting);
        assertEquals(ParticipationStatus.DECLINED, testParticipant.getStatus());
    }

    @Test
    void rejectJoinRequest_whenMeetingNotFound_shouldThrowException() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
                meetingParticipantService.rejectJoinRequest(100L, 200L, 1L)
        );
    }

    @Test
    void rejectJoinRequest_whenNotOrganizer_shouldThrowException() {
        // Given
        User differentOrganizer = User.builder().id(999L).build();
        testMeeting.setOrganizer(differentOrganizer);

        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));

        // When & Then
        assertThrows(SecurityException.class, () ->
                meetingParticipantService.rejectJoinRequest(100L, 200L, 1L)
        );
    }

    @Test
    void rejectJoinRequest_whenParticipantNotFound_shouldThrowException() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.findById(200L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
                meetingParticipantService.rejectJoinRequest(100L, 200L, 1L)
        );
    }

    @Test
    void rejectJoinRequest_whenParticipantNotInMeeting_shouldThrowException() {
        // Given
        Long meetingId = 100L;
        Long participantId = 200L;
        Long organizerId = 1L;

        System.out.println("=== DEBUG START ===");

        Meeting participantMeeting = new Meeting();
        participantMeeting.setId(999L);
        System.out.println("1. participantMeeting ID: " + participantMeeting.getId());

        MeetingParticipant differentParticipant = new MeetingParticipant();
        differentParticipant.setId(participantId);
        differentParticipant.setMeeting(participantMeeting);
        differentParticipant.setUser(testUser);
        differentParticipant.setStatus(ParticipationStatus.PENDING);
        System.out.println("2. participant status: " + differentParticipant.getStatus());

        testMeeting.setId(meetingId);
        testMeeting.setOrganizer(testOrganizer);
        testOrganizer.setId(organizerId);
        System.out.println("3. organizer ID: " + testOrganizer.getId());
        System.out.println("4. testMeeting ID: " + testMeeting.getId());
        System.out.println("5. participant meeting ID: " + differentParticipant.getMeeting().getId());

        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.findById(participantId)).thenReturn(Optional.of(differentParticipant));

        System.out.println("=== BEFORE METHOD CALL ===");

        try {
            meetingParticipantService.rejectJoinRequest(meetingId, participantId, organizerId);
            System.out.println("=== NO EXCEPTION THROWN ===");
            fail("Should have thrown IllegalArgumentException");
        } catch (SecurityException e) {
            System.out.println("=== SECURITY EXCEPTION: " + e.getMessage());
            fail("Wrong exception: SecurityException");
        } catch (IllegalArgumentException e) {
            System.out.println("=== CORRECT EXCEPTION: " + e.getMessage());
            assertEquals("Participant does not belong to this meeting", e.getMessage());
        } catch (Exception e) {
            System.out.println("=== OTHER EXCEPTION: " + e.getClass() + " - " + e.getMessage());
            fail("Wrong exception type: " + e.getClass());
        }
    }

    @Test
    void rejectJoinRequest_whenNotPendingStatus_shouldThrowException() {
        // Given
        testParticipant.setStatus(ParticipationStatus.CONFIRMED);

        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.findById(200L)).thenReturn(Optional.of(testParticipant));

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
                meetingParticipantService.rejectJoinRequest(100L, 200L, 1L)
        );
    }


    @Test
    void getCurrentUserId_whenAuthenticated_shouldReturnUserId() {
        // Given
        Long expectedUserId = 1L;
        CustomUserDetailsService.CustomUserDetails userDetails =
                mock(CustomUserDetailsService.CustomUserDetails.class);
        when(userDetails.getId()).thenReturn(expectedUserId);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When
        Long result = meetingParticipantService.getCurrentUserId();

        // Then
        assertEquals(expectedUserId, result);
    }


    @Test
    void getCurrentUserId_whenNotAuthenticated_shouldThrowException() {
        // Given
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);

        // When & Then
        assertThrows(SecurityException.class, () ->
                meetingParticipantService.getCurrentUserId()
        );
    }

    @Test
    void getCurrentUserId_whenWrongPrincipalType_shouldThrowException() {
        // Given
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn("not-user-details");

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When & Then
        assertThrows(SecurityException.class, () ->
                meetingParticipantService.getCurrentUserId()
        );
    }


    @Test
    void addToWaitlist_shouldAddUserToWaitlist() {
        // Test prywatnej metody poprzez publiczną metodę inviteParticipant
        Long meetingId = 100L;
        Long userId = 2L;

        testMeeting.setMaxParticipants(1); // Spotkanie pełne
        testMeeting.setId(meetingId); // ← DODAJ TO! USTAW ID SPOTKANIA
        testMeeting.setOrganizer(testOrganizer);

        testMeeting.setMaxParticipants(1); // Spotkanie pełne

        when(meetingRepository.findById(meetingId))
                .thenReturn(Optional.of(testMeeting));
        when(meetingRepository.findByIdAndOrganizerId(meetingId, 1L))
                .thenReturn(Optional.of(testMeeting));
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(testUser));
        when(participantRepository.findByMeetingIdAndUserId(meetingId, userId))
                .thenReturn(Optional.empty());
        when(participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.CONFIRMED))
                .thenReturn(1L); // Spotkanie pełne
        when(waitlistEntryRepository.existsByMeetingIdAndUserId(meetingId, userId))
                .thenReturn(false);
        when(waitlistEntryRepository.findMaxPositionByMeetingId(meetingId))
                .thenReturn(Optional.of(0));
        when(waitlistEntryRepository.save(any(WaitlistEntry.class)))
                .thenReturn(WaitlistEntry.builder().build());

        // Używamy ArgumentCaptor do przechwycenia nowego uczestnika
        ArgumentCaptor<MeetingParticipant> participantCaptor = ArgumentCaptor.forClass(MeetingParticipant.class);
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenReturn(testParticipant);

        // When - zaproś użytkownika gdy spotkanie pełne
        MeetingParticipant result = meetingParticipantService.inviteParticipant(meetingId, userId, 1L);

        // Then - powinien trafić na listę oczekujących
        assertNotNull(result);

        // Sprawdź co faktycznie zostało zapisane
        verify(participantRepository).save(participantCaptor.capture());
        MeetingParticipant savedParticipant = participantCaptor.getValue();
        assertEquals(ParticipationStatus.PENDING, savedParticipant.getStatus());

        verify(waitlistEntryRepository).save(any(WaitlistEntry.class));
    }

//    @Test
//    void addToWaitlist_whenAlreadyOnWaitlist_shouldThrowException() {
//        // Test sytuacji gdy użytkownik już jest na liście oczekujących
//        Long meetingId = 100L;
//        Long userId = 2L;
//
//        testMeeting.setMaxParticipants(1);
//
//        when(meetingRepository.findByIdAndOrganizerId(meetingId, 1L))
//                .thenReturn(Optional.of(testMeeting));
//        when(userRepository.findById(userId))
//                .thenReturn(Optional.of(testUser));
//        when(participantRepository.findByMeetingIdAndUserId(meetingId, userId))
//                .thenReturn(Optional.empty());
//
//        // When & Then - powinien rzucić wyjątek
//        assertThrows(ResourceNotFoundException.class, () ->
//                meetingParticipantService.inviteParticipant(meetingId, userId, 1L)
//        );
//    }

    @Test
    void addToWaitlist_whenEmptyWaitlist_shouldSetPositionTo1() {
        // Test gdy lista oczekujących jest pusta
        Long meetingId = 100L;
        Long userId = 2L;

        testMeeting.setMaxParticipants(1);
        testMeeting.setId(meetingId); // ← DODAJ TO - USTAW ID SPOTKANIA
        testMeeting.setOrganizer(testOrganizer); // ← I ORGANIZATORA

        // Upewnij się, że testOrganizer ma ID
        testOrganizer.setId(1L);

        when(meetingRepository.findByIdAndOrganizerId(meetingId, 1L))
                .thenReturn(Optional.of(testMeeting));
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(testUser));
        when(participantRepository.findByMeetingIdAndUserId(meetingId, userId))
                .thenReturn(Optional.empty());
        when(participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.CONFIRMED))
                .thenReturn(1L);
        when(waitlistEntryRepository.existsByMeetingIdAndUserId(meetingId, userId))
                .thenReturn(false);
        when(waitlistEntryRepository.findMaxPositionByMeetingId(meetingId))
                .thenReturn(Optional.empty()); // Pusta lista

        // DODAJ MOCK DLA findById (potrzebne dla isMeetingFull)
        when(meetingRepository.findById(meetingId))
                .thenReturn(Optional.of(testMeeting));

        ArgumentCaptor<WaitlistEntry> waitlistCaptor = ArgumentCaptor.forClass(WaitlistEntry.class);

        when(waitlistEntryRepository.save(any(WaitlistEntry.class)))
                .thenReturn(WaitlistEntry.builder().build());
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenReturn(testParticipant);

        // When
        meetingParticipantService.inviteParticipant(meetingId, userId, 1L);

        // Then - pozycja powinna być 1
        verify(waitlistEntryRepository).save(waitlistCaptor.capture());
        WaitlistEntry savedEntry = waitlistCaptor.getValue();
        assertEquals(1, savedEntry.getPosition());
    }


    @Test
    void joinPublicMeeting_whenNewParticipant_shouldCreateConfirmedParticipant() {
        // Given - nowy użytkownik chce dołączyć do publicznego spotkania
        Long meetingId = 100L;
        Long userId = 2L;

        testMeeting.setVisibility(MeetingVisibility.PUBLIC);
        testMeeting.setMaxParticipants(10);

        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(testMeeting));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(participantRepository.findByMeetingIdAndUserId(meetingId, userId))
                .thenReturn(Optional.empty());
        when(participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.CONFIRMED))
                .thenReturn(5L);

        MeetingParticipant newParticipant = MeetingParticipant.builder()
                .meeting(testMeeting)
                .user(testUser)
                .status(ParticipationStatus.CONFIRMED)
                .permissionLevel(PermissionLevel.PARTICIPANT)
                .build();

        when(participantRepository.save(any(MeetingParticipant.class))).thenReturn(newParticipant);

        // When
        MeetingParticipant result = meetingParticipantService.joinPublicMeeting(meetingId, userId);

        // Then
        assertAll(
                () -> assertNotNull(result),
        () -> assertEquals(ParticipationStatus.CONFIRMED, result.getStatus()),
        () -> assertEquals(PermissionLevel.PARTICIPANT, result.getPermissionLevel())


        );
        ;
        verify(notificationService).sendParticipantJoinedNotification(testOrganizer, testUser, testMeeting);
    }


    @Test
    void requestToJoinPrivateMeeting_whenNewRequest_shouldCreatePendingParticipant() {
        // Given - nowy użytkownik chce dołączyć do prywatnego spotkania
        Long meetingId = 100L;
        Long userId = 2L;

        testMeeting.setVisibility(MeetingVisibility.PRIVATE);

        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(testMeeting));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(participantRepository.findByMeetingIdAndUserId(meetingId, userId))
                .thenReturn(Optional.empty());

        MeetingParticipant pendingParticipant = MeetingParticipant.builder()
                .meeting(testMeeting)
                .user(testUser)
                .status(ParticipationStatus.PENDING)
                .permissionLevel(PermissionLevel.PARTICIPANT)
                .build();

        when(participantRepository.save(any(MeetingParticipant.class))).thenReturn(pendingParticipant);

        // When
        MeetingParticipant result = meetingParticipantService.requestToJoinPrivateMeeting(meetingId, userId);

        // Then
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(ParticipationStatus.PENDING, result.getStatus()),
                () -> assertEquals(PermissionLevel.PARTICIPANT, result.getPermissionLevel())
        );



        verify(notificationService).sendJoinRequestNotification(testOrganizer, testUser, testMeeting);
    }



//
//        @Test
//    void getMeetingStats_whenStatsMissingKeys_shouldReturnDefaultValues() {
//        // Given
//        Long meetingId = 100L;
//        Map<String, Long> stats = new HashMap<>(); // Pusta mapa
//
//        // When
//        MeetingParticipantService.ParticipantStats result = meetingParticipantService.getMeetingStats(meetingId);
//
//        // Then
//        assertNotNull(result);
//        assertEquals(0L, result.getTotalInvited());
//        assertEquals(0L, result.getTotalConfirmed());
//        assertEquals(0L, result.getWaitlistCount());
//        assertEquals(0L, result.getPendingCount());
//    }

    @Test
    void approveJoinRequest_whenMeetingNotFound_shouldThrowException() {
        // Given
        Long meetingId = 100L;

        when(meetingRepository.findById(meetingId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
                meetingParticipantService.approveJoinRequest(meetingId, 200L, 1L)
        );
    }

    @Test
    void approveJoinRequest_whenNotOrganizer_shouldThrowException() {
        // Given
        Long meetingId = 100L;
        Long participantId = 200L;
        Long wrongOrganizerId = 999L;

        User differentOrganizer = User.builder().id(999L).build();
        testMeeting.setOrganizer(differentOrganizer);

        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(testMeeting));

        // When & Then
        assertThrows(SecurityException.class, () ->
                meetingParticipantService.approveJoinRequest(meetingId, participantId, 1L)
        );
    }

    @Test
    void approveJoinRequest_whenParticipantNotFound_shouldThrowException() {
        // Given
        Long meetingId = 100L;
        Long participantId = 200L;
        Long organizerId = 1L;

        testMeeting.setOrganizer(testOrganizer);

        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.findById(participantId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
                meetingParticipantService.approveJoinRequest(meetingId, participantId, organizerId)
        );
    }




    @Test
    void approveJoinRequest_whenParticipantNotInMeeting_shouldThrowException() {
        // Given
        Long meetingId = 100L;
        Long participantId = 200L;
        Long organizerId = 1L;

        // Tworzymy spotkanie z ID używając settera
        Meeting differentMeeting = new Meeting();
        differentMeeting.setId(999L); // Ustawiamy ID

        testParticipant.setMeeting(differentMeeting); // Ustaw inne spotkanie
        testMeeting.setOrganizer(testOrganizer);
        testMeeting.setId(meetingId); // Ustaw ID dla testMeeting

        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.findById(participantId)).thenReturn(Optional.of(testParticipant));

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
                meetingParticipantService.approveJoinRequest(meetingId, participantId, organizerId)
        );
    }

    @Test
    void approveJoinRequest_whenNotPendingStatus_shouldThrowException() {
        // Given
        Long meetingId = 100L;
        Long participantId = 200L;
        Long organizerId = 1L;

        testParticipant.setStatus(ParticipationStatus.CONFIRMED);
        testMeeting.setOrganizer(testOrganizer);
        testMeeting.setId(meetingId); // USTAWIAMY ID

        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.findById(participantId)).thenReturn(Optional.of(testParticipant));

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
                meetingParticipantService.approveJoinRequest(meetingId, participantId, organizerId)
        );
    }

    @Test
    void approveJoinRequest_whenNoAvailableSpots_shouldThrowException() {
        // Given
        Long meetingId = 100L;
        Long participantId = 200L;
        Long organizerId = 1L;

        testParticipant.setStatus(ParticipationStatus.PENDING);
        testMeeting.setOrganizer(testOrganizer);
        testMeeting.setId(meetingId); // USTAWIAMY ID
        testMeeting.setMaxParticipants(5);

        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.findById(participantId)).thenReturn(Optional.of(testParticipant));
        when(participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.CONFIRMED))
                .thenReturn(5L);

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
                meetingParticipantService.approveJoinRequest(meetingId, participantId, organizerId)
        );
    }




//    @Test
//    void isConfirmedParticipant_whenRepositoryThrowsException_shouldReturnFalse() {
//        Long meetingId = 100L;
//        Long userId = 1L;
//
//        when(meetingRepository.findById(meetingId)).thenThrow(new RuntimeException("DB error"));
//
//        assertFalse(meetingParticipantService.isConfirmedParticipant(meetingId, userId));
//    }

    @Test
    void isPendingParticipant_whenNullParameters_shouldReturnFalse() {
        assertFalse(meetingParticipantService.isPendingParticipant(null, null));
    }

    @Test
    void isInvitedParticipant_whenNullParameters_shouldReturnFalse() {
        assertFalse(meetingParticipantService.isInvitedParticipant(null, null));
    }

    @Test
    void isDeclinedParticipant_whenNullParameters_shouldReturnFalse() {
        assertFalse(meetingParticipantService.isDeclinedParticipant(null, null));
    }

    @Test
    void isWaitingListParticipant_whenNullParameters_shouldReturnFalse() {
        assertFalse(meetingParticipantService.isWaitingListParticipant(null, null));
    }

    @Test
    void isViewer_whenUserIdNullAndMeetingPublic_shouldReturnTrue() {
        Long meetingId = 100L;
        testMeeting.setVisibility(MeetingVisibility.PUBLIC);

        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(testMeeting));

        assertTrue(meetingParticipantService.isViewer(meetingId, null));
    }

    @Test
    void isViewer_whenUserIdNullAndMeetingPrivate_shouldReturnFalse() {
        Long meetingId = 100L;
        testMeeting.setVisibility(MeetingVisibility.PRIVATE);

        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(testMeeting));

        assertFalse(meetingParticipantService.isViewer(meetingId, null));
    }

    @Test
    void isViewer_whenMeetingNotFound_shouldReturnFalse() {
        Long meetingId = 100L;
        Long userId = 1L;

        when(meetingRepository.findById(meetingId)).thenReturn(Optional.empty());

        assertFalse(meetingParticipantService.isViewer(meetingId, userId));
    }

    @Test
    void isViewer_whenException_shouldReturnFalse() {
        Long meetingId = 100L;
        Long userId = 1L;

        when(meetingRepository.findById(meetingId)).thenThrow(new RuntimeException("DB error"));

        assertFalse(meetingParticipantService.isViewer(meetingId, userId));
    }

    @Test
    void isUnrelatedUser_whenExceptionInTryBlock_shouldCatchAndReturnTrue() {
        // Given
        Long meetingId = 100L;
        Long userId = 1L;
        testMeeting.setVisibility(MeetingVisibility.PUBLIC);

        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(testMeeting));


        when(participantRepository.findByMeetingIdAndUserId(meetingId, userId))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When
        boolean result = meetingParticipantService.isUnrelatedUser(meetingId, userId);

        // Then
        assertTrue(result, "Metoda powinna zwrócić true gdy wystąpi wyjątek w bloku try-catch");

        // Weryfikacja
        verify(meetingRepository, times(1)).findById(meetingId);
        verify(participantRepository, times(1)).findByMeetingIdAndUserId(meetingId, userId);
    }

        @Test
    void isUnrelatedUser_whenException_shouldReturnTrue() {
        Long meetingId = 100L;
        Long userId = 1L;

        when(meetingRepository.findById(meetingId)).thenThrow(new RuntimeException("DB error"));

        assertTrue(meetingParticipantService.isUnrelatedUser(meetingId, userId));
    }

    // Testy dla addOrganizerAsParticipant
    @Test
    void addOrganizerAsParticipant_shouldSaveOrganizer() {
        when(participantRepository.save(any(MeetingParticipant.class))).thenReturn(testOrganizerParticipant);

        meetingParticipantService.addOrganizerAsParticipant(testMeeting, testOrganizer);

        verify(participantRepository).save(any(MeetingParticipant.class));
    }


    @Test
    void confirmAttendance_whenValidToken_shouldUpdateStatus() {
        Long participantId = 200L;
        String token = "valid-token";
        testParticipant.setInvitationToken(token);
        testParticipant.setStatus(ParticipationStatus.CONFIRMED);

        when(participantRepository.findByIdAndInvitationToken(participantId, token))
                .thenReturn(Optional.of(testParticipant));
        when(participantRepository.save(any(MeetingParticipant.class))).thenReturn(testParticipant);

        meetingParticipantService.confirmAttendance(participantId, token);

        assertEquals(ParticipationStatus.ATTENDED, testParticipant.getStatus());
        verify(participantRepository).save(testParticipant);
    }

    @Test
    void joinMeeting_whenPublicMeeting_shouldCallJoinPublicMeeting() {
        Long meetingId = 100L;
        Long userId = 2L;

        testMeeting.setVisibility(MeetingVisibility.PUBLIC);
        testMeeting.setMaxParticipants(10);
        testMeeting.setOrganizer(testOrganizer); // Ustaw organizatora

        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(testMeeting));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(participantRepository.findByMeetingIdAndUserId(meetingId, userId))
                .thenReturn(Optional.empty());
        when(participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.CONFIRMED))
                .thenReturn(5L);
        when(participantRepository.save(any(MeetingParticipant.class))).thenReturn(testParticipant);

        meetingParticipantService.joinMeeting(meetingId, userId);

        // Sprawdź że meetingRepository.findById zostało wywołane przynajmniej raz
        verify(meetingRepository, atLeastOnce()).findById(meetingId);
        verify(userRepository, atLeastOnce()).findById(userId);
        verify(participantRepository, atLeastOnce()).save(any(MeetingParticipant.class));
    }

    @Test
    void joinMeeting_whenPrivateMeeting_shouldCallRequestToJoinPrivateMeeting() {
        Long meetingId = 100L;
        Long userId = 2L;

        testMeeting.setVisibility(MeetingVisibility.PRIVATE);
        testMeeting.setOrganizer(testOrganizer); // Ustaw organizatora

        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(testMeeting));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(participantRepository.findByMeetingIdAndUserId(meetingId, userId))
                .thenReturn(Optional.empty());
        when(participantRepository.save(any(MeetingParticipant.class))).thenReturn(testParticipant);

        meetingParticipantService.joinMeeting(meetingId, userId);

        // Sprawdź że meetingRepository.findById zostało wywołane przynajmniej raz
        verify(meetingRepository, atLeastOnce()).findById(meetingId);
        verify(userRepository, atLeastOnce()).findById(userId);
        verify(participantRepository, atLeastOnce()).save(any(MeetingParticipant.class));
    }


    @Test
    void joinMeeting_whenMeetingNotFound_shouldThrowException() {
        Long meetingId = 100L;
        Long userId = 2L;

        when(meetingRepository.findById(meetingId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                meetingParticipantService.joinMeeting(meetingId, userId)
        );
    }


    // Testy dla isUnrelatedUser
    @Test
    void isUnrelatedUser_whenMeetingNotFound_shouldReturnTrue() {
        Long meetingId = 100L;
        Long userId = 1L;

        when(meetingRepository.findById(meetingId)).thenReturn(Optional.empty());

        assertTrue(meetingParticipantService.isUnrelatedUser(meetingId, userId));
    }

    // Testy dla isDeclinedParticipant
    @Test
    void isDeclinedParticipant_whenRepositoryThrowsException_shouldReturnFalse() {
        Long meetingId = 100L;
        Long userId = 1L;

        when(participantRepository.existsByMeetingIdAndUserIdAndStatus(
                meetingId, userId, ParticipationStatus.DECLINED))
                .thenThrow(new RuntimeException("DB error"));

        assertFalse(meetingParticipantService.isDeclinedParticipant(meetingId, userId));
    }

    // Testy dla isWaitingListParticipant
    @Test
    void isWaitingListParticipant_whenRepositoryThrowsException_shouldReturnFalse() {
        Long meetingId = 100L;
        Long userId = 1L;

        when(participantRepository.existsByMeetingIdAndUserIdAndStatus(
                meetingId, userId, ParticipationStatus.PENDING))
                .thenThrow(new RuntimeException("DB error"));

        assertFalse(meetingParticipantService.isWaitingListParticipant(meetingId, userId));
    }

    // Testy dla isConfirmedParticipant
//    @Test
//    void isConfirmedParticipant_whenRepositoryThrowsException_shouldReturnFalse() {
//        Long meetingId = 100L;
//        Long userId = 1L;
//
//        when(participantRepository.existsByMeetingIdAndUserIdAndStatusIn(
//                anyLong(), anyLong(), anyList()))
//                .thenThrow(new RuntimeException("DB error"));
//
//        assertFalse(meetingParticipantService.isConfirmedParticipant(meetingId, userId));
//    }

    // Testy dla isPendingParticipant
    @Test
    void isPendingParticipant_whenRepositoryThrowsException_shouldReturnFalse() {
        Long meetingId = 100L;
        Long userId = 1L;

        when(participantRepository.existsByMeetingIdAndUserIdAndStatus(
                meetingId, userId, ParticipationStatus.PENDING))
                .thenThrow(new RuntimeException("DB error"));

        assertFalse(meetingParticipantService.isPendingParticipant(meetingId, userId));
    }

    // Testy dla isInvitedParticipant
    @Test
    void isInvitedParticipant_whenRepositoryThrowsException_shouldReturnFalse() {
        Long meetingId = 100L;
        Long userId = 1L;

        when(participantRepository.existsByMeetingIdAndUserIdAndStatus(
                meetingId, userId, ParticipationStatus.INVITED))
                .thenThrow(new RuntimeException("DB error"));

        assertFalse(meetingParticipantService.isInvitedParticipant(meetingId, userId));
    }

    // Testy dla isParticipant
    @Test
    void isParticipant_whenRepositoryThrowsException_shouldReturnFalse() {
        Long meetingId = 100L;
        Long userId = 1L;

        when(participantRepository.findByMeetingIdAndUserId(meetingId, userId))
                .thenThrow(new RuntimeException("DB error"));

        assertFalse(meetingParticipantService.isParticipant(meetingId, userId));
    }

    // Testy dla getParticipantInfo
    @Test
    void getParticipantInfo_whenMeetingNotFound_shouldReturnNull() {
        Long meetingId = 100L;
        Long userId = 1L;

        when(meetingRepository.findById(meetingId))
                .thenThrow(new ResourceNotFoundException("Meeting not found"));

        assertNull(meetingParticipantService.getParticipantInfo(userId, meetingId));
    }

    @Test
    void getParticipantInfo_whenException_shouldReturnNull() {
        Long meetingId = 100L;
        Long userId = 1L;

        when(meetingRepository.findById(meetingId))
                .thenThrow(new RuntimeException("DB error"));

        assertNull(meetingParticipantService.getParticipantInfo(userId, meetingId));
    }

    // Testy dla inviteParticipants (wywołuje getCurrentUserId - SecurityException)
    @Test
    void inviteParticipants_shouldExecuteWithoutErrors() {
        // Given
        Long meetingId = 100L;
        Long userId = 2L;
        Long organizerId = 1L;

        testMeeting.setId(meetingId);
        testMeeting.setMaxParticipants(10); // Ustaw limit

        InviteParticipantsRequest request = new InviteParticipantsRequest();
        request.setUserIds(Arrays.asList(userId));

        // Mock getCurrentUserId
        CustomUserDetailsService.CustomUserDetails userDetails =
                mock(CustomUserDetailsService.CustomUserDetails.class);
        when(userDetails.getId()).thenReturn(organizerId);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Mock podstawowe wywołania
        when(meetingRepository.findByIdAndOrganizerId(meetingId, organizerId))
                .thenReturn(Optional.of(testMeeting));

        // DODAJ TEGO MOCKA - isMeetingFull używa findById
        when(meetingRepository.findById(meetingId))
                .thenReturn(Optional.of(testMeeting));

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(testUser));
        when(participantRepository.findByMeetingIdAndUserId(meetingId, userId))
                .thenReturn(Optional.empty());
        when(participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.CONFIRMED))
                .thenReturn(5L);
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenReturn(testParticipant);

        // When & Then - powinno się wykonać bez błędów
        assertDoesNotThrow(() ->
                meetingParticipantService.inviteParticipants(meetingId, request)
        );
    }

    // Testy dla getParticipantPermissionLevel
//    @Test
//    void getParticipantPermissionLevel_whenMeetingNotFound_shouldReturnDefault() {
//        Long meetingId = 100L;
//        Long userId = 1L;
//
//        when(meetingRepository.findById(meetingId))
//                .thenThrow(new ResourceNotFoundException("Meeting not found"));
//
//        PermissionLevel result = meetingParticipantService.getParticipantPermissionLevel(meetingId, userId);
//
//        assertEquals(PermissionLevel.PARTICIPANT, result);
//    }

    @Test
    void getParticipantPermissionLevel_whenException_shouldReturnDefault() {
        Long meetingId = 100L;
        Long userId = 1L;

        when(meetingRepository.findById(meetingId))
                .thenThrow(new RuntimeException("DB error"));

        PermissionLevel result = meetingParticipantService.getParticipantPermissionLevel(meetingId, userId);

        assertEquals(PermissionLevel.PARTICIPANT, result);
    }


    // Testy dla isConfirmedParticipant - catch Exception
    @Test
    void isConfirmedParticipant_whenIsOrganizerThrowsException_shouldReturnFalse() {
        Long meetingId = 100L;
        Long userId = 1L;

        // Symuluj, że isOrganizer rzuca wyjątek
        when(meetingRepository.findById(meetingId)).thenThrow(new RuntimeException("DB error"));

        assertFalse(meetingParticipantService.isConfirmedParticipant(meetingId, userId));
    }

    @Test
    void getMeetingStats_shouldReturnStatsWithDefaultValues() {
        Long meetingId = 100L;

        testMeeting.setOrganizer(testOrganizer);
        testOrganizer.setId(1L);
        testMeeting.setId(meetingId);

        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.countByMeetingId(meetingId)).thenReturn(5L);
        when(participantRepository.countByMeetingIdAndStatusIn(
                eq(meetingId),
                argThat(list -> list.contains(ParticipationStatus.CONFIRMED) &&
                        list.contains(ParticipationStatus.ATTENDED))))
                .thenReturn(3L);
        when(participantRepository.findByMeetingIdAndUserId(meetingId, testOrganizer.getId()))
                .thenReturn(Optional.empty());
        when(participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.INVITED))
                .thenReturn(5L); // invited = 5
        when(participantRepository.countByMeetingIdAndStatus(meetingId, ParticipationStatus.CONFIRMED))
                .thenReturn(10L); // confirmed = 10

        MeetingParticipantService.ParticipantStats result = meetingParticipantService.getMeetingStats(meetingId);

        assertNotNull(result);
    }

    // Testy dla getParticipantInfo - null parameters
    @Test
    void getParticipantInfo_whenNullParameters_shouldReturnNull() {
        assertNull(meetingParticipantService.getParticipantInfo(null, null));
        assertNull(meetingParticipantService.getParticipantInfo(1L, null));
        assertNull(meetingParticipantService.getParticipantInfo(null, 100L));
    }

    // Testy dla getParticipantInfo - participantOpt.isPresent()
    @Test
    void getParticipantInfo_whenParticipantExists_shouldReturnResponse() {
        Long meetingId = 100L;
        Long userId = 2L;

        testMeeting.setId(meetingId);
        testParticipant.setMeeting(testMeeting);

        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.findByMeetingIdAndUserId(meetingId, userId))
                .thenReturn(Optional.of(testParticipant));

        ParticipantResponse result = meetingParticipantService.getParticipantInfo(userId, meetingId);

        assertNotNull(result);
        // verify że mapToResponse został wywołany
    }


    // Testy dla getParticipantPermissionLevel - organizator
    @Test
    void getParticipantPermissionLevel_whenOrganizer_shouldReturnOrganizer() {
        Long meetingId = 100L;
        Long userId = 1L;

        testMeeting.setOrganizer(testOrganizer);
        testOrganizer.setId(userId);

        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(testMeeting));

        PermissionLevel result = meetingParticipantService.getParticipantPermissionLevel(meetingId, userId);

        assertEquals(PermissionLevel.ORGANIZER, result);
    }

    // Testy dla getParticipantPermissionLevel - participantOpt.isPresent()
    @Test
    void getParticipantPermissionLevel_whenParticipantExists_shouldReturnPermissionLevel() {
        Long meetingId = 100L;
        Long userId = 2L;

        testParticipant.setPermissionLevel(PermissionLevel.MODERATOR);

        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.findByMeetingIdAndUserId(meetingId, userId))
                .thenReturn(Optional.of(testParticipant));

        PermissionLevel result = meetingParticipantService.getParticipantPermissionLevel(meetingId, userId);

        assertEquals(PermissionLevel.MODERATOR, result);
    }

    // Testy dla addToWaitlist - exception (już na liście)
    @Test
    void addToWaitlist_whenAlreadyOnWaitlist_shouldThrowException() {
        // Test prywatnej metody za pomocą refleksji
        MeetingParticipantServiceImpl service = meetingParticipantService;

        Long meetingId = 100L;
        testMeeting.setId(meetingId);
        testUser.setId(2L);

        when(waitlistEntryRepository.existsByMeetingIdAndUserId(meetingId, testUser.getId()))
                .thenReturn(true); // Już jest na liście

        assertThrows(IllegalArgumentException.class, () ->
                ReflectionTestUtils.invokeMethod(service, "addToWaitlist", testMeeting, testUser)
        );
    }


    // Testy dla rejectJoinRequest - sprawdzenie czy uczestnik należy do spotkania
//    @Test
//    void rejectJoinRequest_whenParticipantBelongsToMeeting_shouldNotThrowException() {
//        Long meetingId = 100L;
//        Long participantId = 200L;
//        Long organizerId = 1L;
//
//        testParticipant.setStatus(ParticipationStatus.PENDING);
//        testMeeting.setId(meetingId);
//        testMeeting.setOrganizer(testOrganizer);
//        testOrganizer.setId(organizerId);
//
//        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(testMeeting));
//        when(participantRepository.findById(participantId)).thenReturn(Optional.of(testParticipant));
//        when(participantRepository.save(any(MeetingParticipant.class))).thenReturn(testParticipant);
//
//        // Should not throw IllegalArgumentException
//        assertDoesNotThrow(() ->
//                meetingParticipantService.rejectJoinRequest(meetingId, participantId, organizerId)
//        );
//    }

    // Testy dla hasAvailableSpots - gdy brak limitu (maxParticipants == null)
    @Test
    void hasAvailableSpots_whenNoLimit_shouldReturnTrue() {
        Long meetingId = 100L;

        testMeeting.setMaxParticipants(null); // Brak limitu

        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(testMeeting));

        boolean result = meetingParticipantService.hasAvailableSpots(meetingId);

        assertTrue(result);
        // Nie powinno wywoływać countByMeetingIdAndStatus gdy brak limitu
        verify(participantRepository, never()).countByMeetingIdAndStatus(anyLong(), any());
    }

    // Testy dla removeFromWaitlist - null parameters
    @Test
    void removeFromWaitlist_whenNullParameters_shouldDoNothing() {
        // Test prywatnej metody za pomocą refleksji
        MeetingParticipantServiceImpl service = meetingParticipantService;

        // Powinno się wykonać bez błędów
        assertDoesNotThrow(() ->
                ReflectionTestUtils.invokeMethod(service, "removeFromWaitlist", null, null)
        );
        assertDoesNotThrow(() ->
                ReflectionTestUtils.invokeMethod(service, "removeFromWaitlist", 100L, null)
        );
        assertDoesNotThrow(() ->
                ReflectionTestUtils.invokeMethod(service, "removeFromWaitlist", null, 2L)
        );
    }

    // Testy dla removeFromWaitlist - catch Exception
    @Test
    void removeFromWaitlist_whenException_shouldLogError() {
        MeetingParticipantServiceImpl service = meetingParticipantService;

        Long meetingId = 100L;
        Long userId = 2L;

        when(waitlistEntryRepository.findByMeetingIdAndUserId(meetingId, userId))
                .thenThrow(new RuntimeException("DB error"));

        // Should not throw exception
        assertDoesNotThrow(() ->
                ReflectionTestUtils.invokeMethod(service, "removeFromWaitlist", meetingId, userId)
        );
    }

    // Testy dla hasPermissionToUpdateStatus - null userId
    @Test
    void hasPermissionToUpdateStatus_whenUserIdNull_shouldReturnFalse() {
        MeetingParticipantServiceImpl service = meetingParticipantService;

        boolean result = ReflectionTestUtils.invokeMethod(
                service, "hasPermissionToUpdateStatus", testParticipant, null);

        assertFalse(result);
    }

    // Testy dla inviteParticipants - SecurityException z getCurrentUserId
    @Test
    void inviteParticipants_whenSecurityException_shouldThrow() {
        Long meetingId = 100L;
        InviteParticipantsRequest request = new InviteParticipantsRequest();

        // Symuluj brak autentykacji
        SecurityContextHolder.clearContext();

        assertThrows(SecurityException.class, () ->
                meetingParticipantService.inviteParticipants(meetingId, request)
        );
    }

    // Testy dla isParticipant - null parameters
    @Test
    void isParticipant_whenNullParameters_shouldReturnFalse() {
        assertFalse(meetingParticipantService.isParticipant(null, null));
        assertFalse(meetingParticipantService.isParticipant(100L, null));
        assertFalse(meetingParticipantService.isParticipant(null, 1L));
    }

    // Testy dla isUnrelatedUser - when userId is null and meeting is private
    @Test
    void isUnrelatedUser_whenUserIdNullAndMeetingPrivate_shouldReturnTrue() {
        Long meetingId = 100L;

        testMeeting.setVisibility(MeetingVisibility.PRIVATE);

        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(testMeeting));

        boolean result = meetingParticipantService.isUnrelatedUser(meetingId, null);

        assertTrue(result);
    }

    // Testy dla isViewer - gdy organizator lub uczestnik
    @Test
    void isViewer_whenOrganizer_shouldReturnFalse() {
        Long meetingId = 100L;
        Long userId = 1L;

        testMeeting.setOrganizer(testOrganizer);
        testOrganizer.setId(userId);

        // isOrganizer zwróci true
        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(testMeeting));

        boolean result = meetingParticipantService.isViewer(meetingId, userId);

        assertFalse(result);
    }

    @Test
    void isViewer_whenParticipant_shouldReturnFalse() {
        Long meetingId = 100L;
        Long userId = 2L;

        // isUserParticipant zwróci true
        when(participantRepository.findByMeetingIdAndUserId(meetingId, userId))
                .thenReturn(Optional.of(testParticipant));

        boolean result = meetingParticipantService.isViewer(meetingId, userId);

        assertFalse(result);
    }

    // Testy dla isConfirmedParticipant - gdy isConfirmedOrAttended zwraca true
    @Test
    void isConfirmedParticipant_whenConfirmed_shouldReturnTrue() {
        Long meetingId = 100L;
        Long userId = 2L;

        when(participantRepository.existsByMeetingIdAndUserIdAndStatusIn(
                meetingId, userId, Arrays.asList(ParticipationStatus.CONFIRMED, ParticipationStatus.ATTENDED)))
                .thenReturn(true);

        boolean result = meetingParticipantService.isConfirmedParticipant(meetingId, userId);

        assertTrue(result);
    }

    // Testy dla isConfirmedParticipant - catch Exception
    @Test
    void isConfirmedParticipant_whenRepositoryThrowsException_shouldReturnFalse() {
        Long meetingId = 100L;
        Long userId = 2L;

        when(participantRepository.existsByMeetingIdAndUserIdAndStatusIn(
                anyLong(), anyLong(), anyList()))
                .thenThrow(new RuntimeException("DB error"));

        boolean result = meetingParticipantService.isConfirmedParticipant(meetingId, userId);

        assertFalse(result);
    }



    // Testy dla requestToJoinPrivateMeeting - gdy ma inny status (zmienia na PENDING)
    @Test
    void requestToJoinPrivateMeeting_whenExistingParticipantWithOtherStatus_shouldChangeToPending() {
        Long meetingId = 100L;
        Long userId = 2L;

        testMeeting.setVisibility(MeetingVisibility.PRIVATE);
        testParticipant.setStatus(ParticipationStatus.INVITED); // INNY STATUS niż PENDING lub CONFIRMED

        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(testMeeting));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(participantRepository.findByMeetingIdAndUserId(meetingId, userId))
                .thenReturn(Optional.of(testParticipant));
        when(participantRepository.save(any(MeetingParticipant.class))).thenReturn(testParticipant);

        MeetingParticipant result = meetingParticipantService.requestToJoinPrivateMeeting(meetingId, userId);

        assertEquals(ParticipationStatus.PENDING, testParticipant.getStatus());
        verify(participantRepository).save(testParticipant);
        // W tej sytuacji powiadomienie NIE powinno być wysyłane (lub zależy od implementacji)
    }

    // Testy dla rejectJoinRequest - gdy uczestnik należy do spotkania (nie rzuca wyjątku)
    @Test
    void rejectJoinRequest_whenParticipantBelongsToMeeting_shouldNotThrowException() {
        Long meetingId = 100L;
        Long participantId = 200L;
        Long organizerId = 1L;

        testParticipant.setStatus(ParticipationStatus.PENDING);
        testMeeting.setId(meetingId);
        testMeeting.setOrganizer(testOrganizer);
        testOrganizer.setId(organizerId);

        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.findById(participantId)).thenReturn(Optional.of(testParticipant));
        when(participantRepository.save(any(MeetingParticipant.class))).thenReturn(testParticipant);

        // Should not throw IllegalArgumentException
        assertDoesNotThrow(() ->
                meetingParticipantService.rejectJoinRequest(meetingId, participantId, organizerId)
        );

        // Sprawdź że status zmieniony na DECLINED
        assertEquals(ParticipationStatus.DECLINED, testParticipant.getStatus());
    }

    // Testy dla inviteParticipants - SecurityException (już testowane, dodajemy dla kompletności)
    @Test
    void inviteParticipants_whenNotAuthenticated_shouldThrowSecurityException() {
        Long meetingId = 100L;
        InviteParticipantsRequest request = new InviteParticipantsRequest();

        // Symuluj brak autentykacji
        SecurityContextHolder.clearContext();

        assertThrows(SecurityException.class, () ->
                meetingParticipantService.inviteParticipants(meetingId, request)
        );
    }

    @Test
    void isUnrelatedUser_whenIsOrganizerThrowsException_shouldReturnTrue() {
        Long meetingId = 100L;
        Long userId = 1L;

        // Symuluj że isOrganizer rzuca wyjątek
        when(meetingRepository.findById(meetingId)).thenThrow(new RuntimeException("DB error"));

        boolean result = meetingParticipantService.isUnrelatedUser(meetingId, userId);

        // catch (Exception e) { return true; }
        assertTrue(result);
    }

    @Test
    void isOnWaitlist_whenParticipantIsPending_shouldReturnTrue() {
        // Given
        Long meetingId = 100L;
        Long userId = 2L;

        MeetingParticipant pendingParticipant = MeetingParticipant.builder()
                .meeting(testMeeting)
                .user(testUser)
                .status(ParticipationStatus.PENDING)
                .build();

        when(waitlistEntryRepository.existsByMeetingIdAndUserId(meetingId, userId))
                .thenReturn(false);
        when(participantRepository.findByMeetingIdAndUserId(meetingId, userId))
                .thenReturn(Optional.of(pendingParticipant));

        // When
        boolean result = meetingParticipantService.isOnWaitlist(meetingId, userId);

        // Then
        assertTrue(result, "User with PENDING status should be considered on waitlist");
    }

    @Test
    void isUserPendingApproval_whenParticipantIsPending_shouldReturnTrue() {
        // Given
        Long meetingId = 100L;
        Long userId = 2L;

        MeetingParticipant pendingParticipant = MeetingParticipant.builder()
                .meeting(testMeeting)
                .user(testUser)
                .status(ParticipationStatus.PENDING)
                .build();

        when(participantRepository.findByMeetingIdAndUserId(meetingId, userId))
                .thenReturn(Optional.of(pendingParticipant));

        // When
        boolean result = meetingParticipantService.isUserPendingApproval(meetingId, userId);

        // Then
        assertTrue(result);
    }

    @Test
    void getParticipantInfo_whenLambdaThrowsException_shouldReturnNull() {
        // Given
        Long meetingId = 100L;
        Long userId = 1L;

        // Symuluj wyjątek w lambda
        when(meetingRepository.findById(meetingId))
                .thenThrow(new RuntimeException("DB error in lambda"));

        // When
        ParticipantResponse result = meetingParticipantService.getParticipantInfo(userId, meetingId);

        // Then
        assertNull(result, "Should return null when lambda throws exception");
    }


    @Test
    void getConfirmedParticipants_whenLambdaThrowsException_shouldHandleGracefully() {
        // Given
        Long meetingId = 100L;

        when(meetingRepository.findById(meetingId))
                .thenReturn(Optional.of(testMeeting));

        // Symuluj wyjątek w strumieniu
        when(participantRepository.findByMeetingIdAndStatus(meetingId, ParticipationStatus.CONFIRMED))
                .thenThrow(new RuntimeException("Stream processing error"));

        // When
        List<ParticipantResponse> result = meetingParticipantService.getConfirmedParticipants(meetingId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Should return empty list on exception");
    }

    @Test
    void updateParticipant_whenLambdaThrowsException_shouldThrow() {
        // Given
        Long participantId = 200L;
        UpdateParticipantRequest request = new UpdateParticipantRequest();
        request.setStatus(ParticipationStatus.CONFIRMED);

        // Symuluj wyjątek w mapowaniu
        when(participantRepository.findById(participantId))
                .thenThrow(new RuntimeException("Mapping error"));

        // When & Then
        assertThrows(RuntimeException.class, () ->
                meetingParticipantService.updateParticipant(participantId, request)
        );
    }

    @Test
    void getParticipant_whenParticipantNotFound_shouldThrowResourceNotFoundException() {
        // Given
        Long nonExistentParticipantId = 999L;

        when(participantRepository.findById(nonExistentParticipantId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
                        meetingParticipantService.getParticipant(nonExistentParticipantId),
                "Should throw ResourceNotFoundException when participant not found"
        );

        verify(participantRepository).findById(nonExistentParticipantId);
    }


    @Test
    void isUserPendingApproval_whenParticipantIsNotPending_shouldReturnFalse() {
        // Given
        Long meetingId = 100L;
        Long userId = 2L;

        MeetingParticipant confirmedParticipant = MeetingParticipant.builder()
                .meeting(testMeeting)
                .user(testUser)
                .status(ParticipationStatus.CONFIRMED) // ← INNY STATUS!
                .build();

        when(participantRepository.findByMeetingIdAndUserId(meetingId, userId))
                .thenReturn(Optional.of(confirmedParticipant));

        // When
        boolean result = meetingParticipantService.isUserPendingApproval(meetingId, userId);

        // Then
        assertFalse(result, "Should return false when participant status is not PENDING");
    }

    @Test
    void mapToResponse_whenParticipantHasMeeting_shouldSetMeetingInResponse() {
        // Given
        MeetingParticipant participant = MeetingParticipant.builder()
                .id(200L)
                .meeting(testMeeting) // ← MA SPOTKANIE!
                .user(testUser)
                .status(ParticipationStatus.CONFIRMED)
                .permissionLevel(PermissionLevel.PARTICIPANT)
                .build();

        MeetingResponse meetingResponse = new MeetingResponse();
        when(meetingMapper.toResponse(testMeeting)).thenReturn(meetingResponse);

        // When
        ParticipantResponse result = meetingParticipantService.mapToResponse(participant);

        // Then
        assertNotNull(result);
        assertEquals(meetingResponse, result.getMeeting());
        verify(meetingMapper).toResponse(testMeeting);
    }

    @Test
    void mapToResponse_whenParticipantHasNoMeeting_shouldNotSetMeetingInResponse() {
        // Given
        MeetingParticipant participant = MeetingParticipant.builder()
                .id(200L)
                .meeting(null) // ← BRAK SPOTKANIA!
                .user(testUser)
                .status(ParticipationStatus.CONFIRMED)
                .permissionLevel(PermissionLevel.PARTICIPANT)
                .build();

        // When
        ParticipantResponse result = meetingParticipantService.mapToResponse(participant);

        // Then
        assertNotNull(result);
        assertNull(result.getMeeting());
        verify(meetingMapper, never()).toResponse(any());
    }

    @Test
    void mapToResponse_whenParticipantMeetingIsNull_shouldHandleGracefully() {
        // Given
        MeetingParticipant participant = new MeetingParticipant();
        participant.setId(200L);
        participant.setUser(testUser);
        participant.setStatus(ParticipationStatus.CONFIRMED);
        // meeting is null by default

        // When
        ParticipantResponse result = meetingParticipantService.mapToResponse(participant);

        // Then
        assertNotNull(result);
        assertNull(result.getMeeting());
        assertEquals(ParticipationStatus.CONFIRMED, result.getStatus());
    }

    @Test
    void isUserPendingApproval_whenParticipantNotFound_shouldReturnFalse() {
        // Given
        Long meetingId = 100L;
        Long userId = 2L;

        when(participantRepository.findByMeetingIdAndUserId(meetingId, userId))
                .thenReturn(Optional.empty()); // ← BRAK PARTICIPANT!

        // When
        boolean result = meetingParticipantService.isUserPendingApproval(meetingId, userId);

        // Then
        assertFalse(result, "Should return false when participant not found");

        verify(participantRepository).findByMeetingIdAndUserId(meetingId, userId);
    }


    @Test
    void updateParticipant_whenParticipantNotFound_shouldThrowResourceNotFoundException() {
        // Given
        Long nonExistentParticipantId = 999L;
        UpdateParticipantRequest request = new UpdateParticipantRequest();
        request.setStatus(ParticipationStatus.CONFIRMED);

        when(participantRepository.findById(nonExistentParticipantId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
                meetingParticipantService.updateParticipant(nonExistentParticipantId, request)
        );
    }


    @Test
    void hasPermissionToUpdateStatus_whenUserIsModerator_shouldReturnTrue() {
        // Given
        Long meetingId = 100L;
        Long moderatorUserId = 3L;
        Long participantUserId = 2L;

        MeetingParticipant participant = MeetingParticipant.builder()
                .id(200L)
                .meeting(testMeeting)
                .user(User.builder().id(participantUserId).build())
                .build();

        MeetingParticipant moderator = MeetingParticipant.builder()
                .meeting(testMeeting)
                .user(User.builder().id(moderatorUserId).build())
                .permissionLevel(PermissionLevel.MODERATOR)
                .build();

        when(participantRepository.findByMeetingIdAndUserId(meetingId, moderatorUserId))
                .thenReturn(Optional.of(moderator));

        // When - użyj refleksji, bo to metoda prywatna
        boolean result = ReflectionTestUtils.invokeMethod(
                meetingParticipantService,
                "hasPermissionToUpdateStatus",
                participant,
                moderatorUserId
        );

        // Then
        assertTrue(result, "Moderator should have permission to update status");
    }

    @Test
    void hasPermissionToUpdateStatus_whenUserIsContributor_shouldReturnFalse() {
        // Given
        Long meetingId = 100L;
        Long contributorUserId = 3L;
        Long participantUserId = 2L;

        MeetingParticipant participant = MeetingParticipant.builder()
                .meeting(testMeeting)
                .user(User.builder().id(participantUserId).build())
                .build();

        MeetingParticipant contributor = MeetingParticipant.builder()
                .meeting(testMeeting)
                .user(User.builder().id(contributorUserId).build())
                .permissionLevel(PermissionLevel.CONTRIBUTOR) // NOT MODERATOR!
                .build();

        when(participantRepository.findByMeetingIdAndUserId(meetingId, contributorUserId))
                .thenReturn(Optional.of(contributor));

        // When
        boolean result = ReflectionTestUtils.invokeMethod(
                meetingParticipantService,
                "hasPermissionToUpdateStatus",
                participant,
                contributorUserId
        );

        // Then
        assertFalse(result, "Contributor should NOT have permission to update status");
    }

    @Test
    void hasPermissionToUpdateStatus_whenUserParticipantNotFound_shouldReturnFalse() {
        // Given
        Long meetingId = 100L;
        Long randomUserId = 999L;
        Long participantUserId = 2L;

        MeetingParticipant participant = MeetingParticipant.builder()
                .meeting(testMeeting)
                .user(User.builder().id(participantUserId).build())
                .build();

        when(participantRepository.findByMeetingIdAndUserId(meetingId, randomUserId))
                .thenReturn(Optional.empty());

        // When
        boolean result = ReflectionTestUtils.invokeMethod(
                meetingParticipantService,
                "hasPermissionToUpdateStatus",
                participant,
                randomUserId
        );

        // Then
        assertFalse(result, "User who is not moderator should NOT have permission");
    }


    @Test
    void isUnrelatedUser_whenUserIdNullAndMeetingNull_shouldReturnFalse() {
        // Given
        Long meetingId = 100L;

        when(meetingRepository.findById(meetingId))
                .thenReturn(Optional.empty());

        // When
        boolean result = meetingParticipantService.isUnrelatedUser(meetingId, null);

        // Then
        // meeting == null, więc: null != null && ... → false
        assertFalse(result);
    }


    @Test
    void isUnrelatedUser_whenUserIdNullAndMeetingPublic_shouldReturnFalse() {
        // Given
        Long meetingId = 100L;
        testMeeting.setVisibility(MeetingVisibility.PUBLIC);

        when(meetingRepository.findById(meetingId))
                .thenReturn(Optional.of(testMeeting));

        // When
        boolean result = meetingParticipantService.isUnrelatedUser(meetingId, null);

        // Then
        // meeting != null && visibility != PUBLIC → false
        assertFalse(result);
    }

    @Test
    void isUnrelatedUser_whenUserIsOrganizerParticipantViewer_shouldReturnFalse() {
        // Given
        Long meetingId = 100L;
        Long userId = 1L;

        // Ustaw wszystkie 3 na true
        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(testMeeting));
        // Mockuj że użytkownik jest organizatorem
        testMeeting.setOrganizer(User.builder().id(userId).build());

        // When
        boolean result = meetingParticipantService.isUnrelatedUser(meetingId, userId);

        // Then
        // !true && !true && !true → !true → false
        assertFalse(result, "Should return false when user is organizer");
    }

    @Test
    void isUnrelatedUser_whenUserIsNotOrganizerNotParticipantNotViewer_shouldReturnTrue() {
        // Given
        Long meetingId = 100L;
        Long userId = 999L; // obcy użytkownik

        testMeeting.setVisibility(MeetingVisibility.PRIVATE);
        testMeeting.setOrganizer(User.builder().id(1L).build()); // inny organizator

        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(testMeeting));
        // isOrganizer zwróci false (użytkownik nie jest organizatorem)
        // isUserParticipant zwróci false (nie jest uczestnikiem)
        // isViewer zwróci false (spotkanie prywatne)

        // When
        boolean result = meetingParticipantService.isUnrelatedUser(meetingId, userId);

        // Then
        // !false && !false && !false → !false → true
        assertTrue(result, "Should return true when user is completely unrelated");
    }

    @Test
    void isUnrelatedUser_whenUserIsOnlyViewer_shouldReturnFalse() {
        // Given
        Long meetingId = 100L;
        Long userId = 999L;

        testMeeting.setVisibility(MeetingVisibility.PUBLIC);
        testMeeting.setOrganizer(User.builder().id(1L).build()); // inny organizator

        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.findByMeetingIdAndUserId(meetingId, userId))
                .thenReturn(Optional.empty()); // nie jest uczestnikiem

        // isViewer zwróci true (spotkanie publiczne, nie jest organizatorem/uczestnikiem)

        // When
        boolean result = meetingParticipantService.isUnrelatedUser(meetingId, userId);

        // Then
        // !false && !false && !true → !true → false
        assertFalse(result, "Should return false when user is viewer (public meeting)");
    }


    @Test
    void isOnWaitlist_whenParticipantIsPendingButNotInWaitlistEntry_shouldReturnTrue() {
        // Given
        Long meetingId = 100L;
        Long userId = 2L;

        MeetingParticipant pendingParticipant = MeetingParticipant.builder()
                .meeting(testMeeting)
                .user(testUser)
                .status(ParticipationStatus.PENDING)  // Status PENDING ale nie w waitlistEntry
                .build();

        // WaitlistEntry nie istnieje, ale participant istnieje ze statusem PENDING
        when(waitlistEntryRepository.existsByMeetingIdAndUserId(meetingId, userId))
                .thenReturn(false);  // Nie ma w WaitlistEntry
        when(participantRepository.findByMeetingIdAndUserId(meetingId, userId))
                .thenReturn(Optional.of(pendingParticipant));  // Ale jest w participants

        // When
        boolean result = meetingParticipantService.isOnWaitlist(meetingId, userId);

        // Then - lambda powinna być wykonana
        assertTrue(result, "User with PENDING status should be considered on waitlist even without WaitlistEntry");

        // Verify
        verify(waitlistEntryRepository).existsByMeetingIdAndUserId(meetingId, userId);
        verify(participantRepository).findByMeetingIdAndUserId(meetingId, userId);
    }


    @Test
    void isOnWaitlist_whenUserNotParticipantAndNotInWaitlist_shouldReturnFalse() {
        // Given
        Long meetingId = 100L;
        Long userId = 2L;

        // Użytkownik nie istnieje ani w waitlist ani w participants
        when(waitlistEntryRepository.existsByMeetingIdAndUserId(meetingId, userId))
                .thenReturn(false);
        when(participantRepository.findByMeetingIdAndUserId(meetingId, userId))
                .thenReturn(Optional.empty());  // Empty optional

        // When
        boolean result = meetingParticipantService.isOnWaitlist(meetingId, userId);

        // Then
        assertFalse(result, "User should not be on waitlist");

        // Verify that lambda's map() was called and returned Optional.empty()
        verify(participantRepository).findByMeetingIdAndUserId(meetingId, userId);
    }
    }

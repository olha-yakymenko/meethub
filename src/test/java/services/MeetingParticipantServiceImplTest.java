package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.*;
import com.meethub.domain.model.enums.MeetingStatus;
import com.meethub.domain.model.enums.MeetingVisibility;
import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.model.enums.PermissionLevel;
import com.meethub.domain.model.mapper.MeetingMapper;
import com.meethub.domain.model.projection.ParticipantProjection;
import com.meethub.domain.model.request.InviteParticipantsRequest;
import com.meethub.domain.model.request.UpdateParticipantRequest;
import com.meethub.domain.model.response.ParticipantResponse;
import com.meethub.domain.model.response.UserResponse;
import com.meethub.domain.repository.jpa.*;
import com.meethub.domain.service.EmailService;
import com.meethub.domain.service.MeetingParticipantService;
import com.meethub.domain.service.NotificationService;
import com.meethub.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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

    @InjectMocks
    private MeetingParticipantServiceImpl meetingParticipantService;

    private Meeting testMeeting;
    private User testUser;
    private User testOrganizer;
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
    void approveJoinRequest_whenNotOrganizer_shouldThrowException() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));

        assertThrows(SecurityException.class, () ->
                meetingParticipantService.approveJoinRequest(100L, 200L, 999L)
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

    @Test
    void getParticipantStatistics_whenException_shouldReturnDefaultStats() {
        when(meetingRepository.findById(100L)).thenThrow(new RuntimeException("DB Error"));

        Map<String, Long> stats = meetingParticipantService.getParticipantStatistics(100L);

        assertAll(
                () -> assertNotNull(stats),
                () -> assertTrue(stats.containsKey("total")),
                () -> assertEquals(0L, stats.get("total"))
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

    @Test
    void removeFromWaitlist_whenEntryNotFound_shouldDoNothing() {
        when(waitlistEntryRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.empty());

        meetingParticipantService.removeFromWaitlist(100L, 2L);

        verify(waitlistEntryRepository, never()).delete(any());
    }

    @Test
    void completeUserJourney_publicMeeting() {
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.empty());
        when(participantRepository.countByMeetingIdAndStatus(100L, ParticipationStatus.CONFIRMED))
                .thenReturn(5L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(participantRepository.save(any(MeetingParticipant.class))).thenReturn(testParticipant);

        MeetingParticipant participant = meetingParticipantService.joinPublicMeeting(100L, 2L);

        assertAll(
                () -> assertNotNull(participant),
                () -> assertEquals(ParticipationStatus.CONFIRMED, participant.getStatus())
        );

        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));
        when(waitlistEntryRepository.findFirstByMeetingIdOrderByPositionAsc(100L))
                .thenReturn(Optional.empty());

        meetingParticipantService.leaveMeeting(2L, 100L);

        verify(participantRepository).delete(testParticipant);
    }
}
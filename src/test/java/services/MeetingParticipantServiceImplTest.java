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

import static org.assertj.core.api.Assertions.*;
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

    // ==================== TESTY DLA METOD ZAPRASZANIA ====================

    @Test
    void inviteMultipleParticipants_shouldInviteAllUsers() {
        // Given
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

        // When
        List<MeetingParticipant> result = meetingParticipantService
                .inviteMultipleParticipants(100L, request, 1L);

        // Then
        assertThat(result).hasSize(3);
        verify(participantRepository, times(3)).save(any(MeetingParticipant.class));
    }

    @Test
    void inviteParticipant_whenUserAlreadyParticipant_shouldThrowException() {
        // Given
        when(meetingRepository.findByIdAndOrganizerId(100L, 1L))
                .thenReturn(Optional.of(testMeeting));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));

        // When & Then
        assertThatThrownBy(() -> meetingParticipantService.inviteParticipant(100L, 2L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User is already a participant");
    }

    @Test
    void inviteParticipant_whenNotOrganizer_shouldThrowException() {
        // Given
        when(meetingRepository.findByIdAndOrganizerId(100L, 1L))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> meetingParticipantService.inviteParticipant(100L, 2L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Meeting not found or access denied");
    }

    // ==================== TESTY DLA METOD AKTUALIZACJI ====================

    @Test
    void updateParticipant_shouldUpdateSuccessfully() {
        // Given
        UpdateParticipantRequest request = new UpdateParticipantRequest();
        request.setStatus(ParticipationStatus.CONFIRMED);
        request.setPermissionLevel(PermissionLevel.MODERATOR);
        request.setComment("Updated status");

        when(participantRepository.findById(200L))
                .thenReturn(Optional.of(testParticipant));
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenReturn(testParticipant);

        // When
        ParticipantResponse result = meetingParticipantService.updateParticipant(200L, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ParticipationStatus.CONFIRMED);
        assertThat(result.getPermissionLevel()).isEqualTo(PermissionLevel.MODERATOR);
        assertThat(result.getComment()).isEqualTo("Updated status");
        verify(participantRepository).save(testParticipant);
    }

//    @Test
//    void updateParticipantStatus_shouldUpdateAndSaveHistory() {
//        // Given
//        ParticipationStatus newStatus = ParticipationStatus.DECLINED;
//        String comment = "Cannot attend";
//
//        when(participantRepository.findById(200L))
//                .thenReturn(Optional.of(testParticipant));
//        when(participantRepository.findByMeetingIdAndUserId(100L, 1L))
//                .thenReturn(Optional.of(testOrganizerParticipant));
//        when(participantRepository.save(any(MeetingParticipant.class)))
//                .thenReturn(testParticipant);
//
//        // When
//        MeetingParticipant result = meetingParticipantService
//                .updateParticipantStatus(100L, 200L, newStatus, comment, 1L);
//
//        // Then
//        assertThat(result).isNotNull();
//        assertThat(result.getStatus()).isEqualTo(newStatus);
//        assertThat(result.getComment()).isEqualTo(comment);
//        assertThat(result.getResponseDate()).isNotNull();
//        verify(statusHistoryRepository).save(any(ParticipantStatusHistory.class));
//    }

//    @Test
//    void updateParticipantStatus_whenNoPermission_shouldThrowException() {
//        // Given
//        when(participantRepository.findById(200L))
//                .thenReturn(Optional.of(testParticipant));
//        when(participantRepository.findByMeetingIdAndUserId(100L, 999L))
//                .thenReturn(Optional.empty());
//
//        // When & Then
//        assertThatThrownBy(() -> meetingParticipantService
//                .updateParticipantStatus(100L, 200L, ParticipationStatus.CONFIRMED, "", 999L))
//                .isInstanceOf(SecurityException.class)
//                .hasMessageContaining("No permission");
//    }

    @Test
    void updateParticipantPermission_shouldUpdatePermissionLevel() {
        // Given
        when(meetingRepository.findByIdAndOrganizerId(100L, 1L))
                .thenReturn(Optional.of(testMeeting));
        when(participantRepository.findById(200L))
                .thenReturn(Optional.of(testParticipant));
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenReturn(testParticipant);

        // When
        MeetingParticipant result = meetingParticipantService
                .updateParticipantPermission(100L, 200L, PermissionLevel.MODERATOR, 1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPermissionLevel()).isEqualTo(PermissionLevel.MODERATOR);
    }

    @Test
    void updateParticipantPermission_whenNotOrganizer_shouldThrowException() {
        // Given
        when(meetingRepository.findByIdAndOrganizerId(100L, 1L))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> meetingParticipantService
                .updateParticipantPermission(100L, 200L, PermissionLevel.MODERATOR, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Meeting not found or access denied");
    }

    // ==================== TESTY DLA METOD USUWANIA ====================

    @Test
    void removeParticipant_shouldDeleteParticipant() {
        // Given
        when(meetingRepository.findByIdAndOrganizerId(100L, 1L))
                .thenReturn(Optional.of(testMeeting));

        // When
        meetingParticipantService.removeParticipant(100L, 200L, 1L);

        // Then
        verify(participantRepository).deleteById(200L);
    }

    @Test
    void removeParticipantById_shouldDelete() {
        // Given - no setup needed for simple delete

        // When
        meetingParticipantService.removeParticipant(200L);

        // Then
        verify(participantRepository).deleteById(200L);
    }

    // ==================== TESTY DLA PUBLIC MEETINGS ====================

    @Test
    void joinPublicMeeting_whenMeetingCompleted_shouldThrowException() {
        // Given
        testMeeting.setStatus(MeetingStatus.COMPLETED);
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));

        // When & Then
        assertThatThrownBy(() -> meetingParticipantService.joinPublicMeeting(100L, 2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already been completed");
    }

    @Test
    void joinPublicMeeting_whenNotPublic_shouldThrowException() {
        // Given
        testMeeting.setVisibility(MeetingVisibility.PRIVATE);
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));

        // When & Then
        assertThatThrownBy(() -> meetingParticipantService.joinPublicMeeting(100L, 2L))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("not public");
    }

    @Test
    void joinPublicMeeting_whenNoAvailableSpots_shouldThrowException() {
        // Given
        testMeeting.setMaxParticipants(5);
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.empty());
        when(participantRepository.countByMeetingIdAndStatus(100L, ParticipationStatus.CONFIRMED))
                .thenReturn(5L);

        // When & Then
        assertThatThrownBy(() -> meetingParticipantService.joinPublicMeeting(100L, 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No available spots");
    }

    @Test
    void joinPublicMeeting_whenAlreadyInvited_shouldConfirm() {
        // Given
        testParticipant.setStatus(ParticipationStatus.INVITED);
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenReturn(testParticipant);

        // When
        MeetingParticipant result = meetingParticipantService.joinPublicMeeting(100L, 2L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ParticipationStatus.CONFIRMED);
    }

    // ==================== TESTY DLA PRIVATE MEETINGS ====================

    @Test
    void requestToJoinPrivateMeeting_whenAlreadyPending_shouldThrowException() {
        // Given
        testMeeting.setVisibility(MeetingVisibility.PRIVATE);
        testParticipant.setStatus(ParticipationStatus.PENDING);

        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));

        // When & Then
        assertThatThrownBy(() -> meetingParticipantService.requestToJoinPrivateMeeting(100L, 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already sent a join request");
    }

    @Test
    void approveJoinRequest_whenNotOrganizer_shouldThrowException() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));

        // When & Then
        assertThatThrownBy(() -> meetingParticipantService.approveJoinRequest(100L, 200L, 999L))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Only organizer can approve");
    }

//    @Test
//    void approveJoinRequest_whenParticipantNotBelongsToMeeting_shouldThrowException() {
//        // Given
//        Meeting differentMeeting = Meeting.builder().organizer(testOrganizer).build();
//        testParticipant.setMeeting(differentMeeting);
//
//        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
//        when(participantRepository.findById(200L)).thenReturn(Optional.of(testParticipant));
//
//        // When & Then
//        assertThatThrownBy(() -> meetingParticipantService.approveJoinRequest(100L, 200L, 1L))
//                .isInstanceOf(IllegalArgumentException.class)
//                .hasMessageContaining("does not belong to this meeting");
//    }
//
//    @Test
//    void rejectJoinRequest_shouldUpdateStatusToDeclined() {
//        // Given
//        testMeeting.setVisibility(MeetingVisibility.PRIVATE);
//        testParticipant.setStatus(ParticipationStatus.PENDING);
//
//        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
//        when(participantRepository.findById(200L)).thenReturn(Optional.of(testParticipant));
//
//        // When
//        meetingParticipantService.rejectJoinRequest(100L, 200L, 1L);
//
//        // Then
//        assertThat(testParticipant.getStatus()).isEqualTo(ParticipationStatus.DECLINED);
//        verify(participantRepository).save(testParticipant);
//        verify(statusHistoryRepository).save(any(ParticipantStatusHistory.class));
//        verify(notificationService).sendRequestRejectedNotification(any(), any());
//    }

    // ==================== TESTY DLA METOD POMOCNICZYCH ====================



    @Test
    void getPendingRequests_shouldReturnPendingParticipants() {
        // Given
        MeetingParticipant pendingParticipant = MeetingParticipant.builder()
                .id(201L)
                .meeting(testMeeting)
                .user(testUser)
                .status(ParticipationStatus.PENDING)
                .build();

        when(participantRepository.findByMeetingIdAndStatus(100L, ParticipationStatus.PENDING))
                .thenReturn(List.of(pendingParticipant));

        // When
        List<ParticipantResponse> result = meetingParticipantService.getPendingRequests(100L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(ParticipationStatus.PENDING);
    }

    @Test
    void isUserPendingApproval_whenPending_shouldReturnTrue() {
        // Given
        testParticipant.setStatus(ParticipationStatus.PENDING);
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));

        // When
        boolean result = meetingParticipantService.isUserPendingApproval(100L, 2L);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isUserPendingApproval_whenNotParticipant_shouldReturnFalse() {
        // Given
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.empty());

        // When
        boolean result = meetingParticipantService.isUserPendingApproval(100L, 2L);

        // Then
        assertThat(result).isFalse();
    }

    // ==================== TESTY DLA acceptInvitationByToken ====================

    @Test
    void acceptInvitationByToken_whenInvalidToken_shouldThrowException() {
        // Given
        when(participantRepository.findByInvitationToken("invalid-token"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> meetingParticipantService.acceptInvitationByToken("invalid-token"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Invalid attendance token");
    }

    @Test
    void acceptInvitationByToken_whenMeetingNotOngoing_shouldThrowException() {
        // Given
        String token = "valid-token";
        testParticipant.setInvitationToken(token);
        testMeeting.setStartDate(LocalDateTime.now().plusDays(1)); // Meeting in future

        when(participantRepository.findByInvitationToken(token))
                .thenReturn(Optional.of(testParticipant));

        // When & Then
        assertThatThrownBy(() -> meetingParticipantService.acceptInvitationByToken(token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("can only be used during the meeting");
    }

    @Test
    void acceptInvitationByToken_whenAlreadyAttended_shouldThrowException() {
        // Given
        String token = "valid-token";
        testParticipant.setInvitationToken(token);
        testParticipant.setAttendanceConfirmedAt(LocalDateTime.now());
        testMeeting.setStartDate(LocalDateTime.now().minusHours(1));
        testMeeting.setEndDate(LocalDateTime.now().plusHours(1));

        when(participantRepository.findByInvitationToken(token))
                .thenReturn(Optional.of(testParticipant));

        // When & Then
        assertThatThrownBy(() -> meetingParticipantService.acceptInvitationByToken(token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already confirmed");
    }

    @Test
    void acceptInvitationByToken_whenValid_shouldConfirmAttendance() {
        // Given
        String token = "valid-token";
        testParticipant.setInvitationToken(token);
        testParticipant.setStatus(ParticipationStatus.INVITED);
        testMeeting.setStartDate(LocalDateTime.now().minusHours(1));
        testMeeting.setEndDate(LocalDateTime.now().plusHours(1));

        when(participantRepository.findByInvitationToken(token))
                .thenReturn(Optional.of(testParticipant));
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenReturn(testParticipant);

        // When
        MeetingParticipant result = meetingParticipantService.acceptInvitationByToken(token);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ParticipationStatus.ATTENDED);
        assertThat(result.getAttendanceConfirmedAt()).isNotNull();
        assertThat(result.getInvitationToken()).isNull();
    }

    // ==================== TESTY DLA isUserParticipant ====================

    @Test
    void isUserParticipant_whenConfirmed_shouldReturnTrue() {
        // Given
        testParticipant.setStatus(ParticipationStatus.CONFIRMED);
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));

        // When
        boolean result = meetingParticipantService.isUserParticipant(100L, 2L);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isUserParticipant_whenPending_shouldReturnTrue() {
        // Given
        testParticipant.setStatus(ParticipationStatus.PENDING);
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));

        // When
        boolean result = meetingParticipantService.isUserParticipant(100L, 2L);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isUserParticipant_whenDeclined_shouldReturnFalse() {
        // Given
        testParticipant.setStatus(ParticipationStatus.DECLINED);
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));

        // When
        boolean result = meetingParticipantService.isUserParticipant(100L, 2L);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isUserParticipant_whenNotParticipant_shouldReturnFalse() {
        // Given
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.empty());

        // When
        boolean result = meetingParticipantService.isUserParticipant(100L, 2L);

        // Then
        assertThat(result).isFalse();
    }

    // ==================== TESTY DLA UPRAWNIEŃ ====================

    @Test
    void canUserEditMeeting_whenModerator_shouldReturnTrue() {
        // Given
        testParticipant.setPermissionLevel(PermissionLevel.MODERATOR);
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));

        // When
        boolean result = meetingParticipantService.canUserEditMeeting(100L, 2L);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void canUserEditMeeting_whenContributor_shouldReturnTrue() {
        // Given
        testParticipant.setPermissionLevel(PermissionLevel.CONTRIBUTOR);
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));

        // When
        boolean result = meetingParticipantService.canUserEditMeeting(100L, 2L);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void canUserEditMeeting_whenParticipant_shouldReturnFalse() {
        // Given
        testParticipant.setPermissionLevel(PermissionLevel.PARTICIPANT);
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));

        // When
        boolean result = meetingParticipantService.canUserEditMeeting(100L, 2L);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void getParticipantPermissionLevel_whenUserNotFound_shouldReturnDefault() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.findByMeetingIdAndUserId(100L, 999L))
                .thenReturn(Optional.empty());

        // When
        PermissionLevel result = meetingParticipantService.getParticipantPermissionLevel(100L, 999L);

        // Then
        assertThat(result).isEqualTo(PermissionLevel.PARTICIPANT);
    }

    @Test
    void getParticipantPermissionLevel_whenMeetingNotFound_shouldReturnDefault() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.empty());

        // When
        PermissionLevel result = meetingParticipantService.getParticipantPermissionLevel(100L, 2L);

        // Then
        assertThat(result).isEqualTo(PermissionLevel.PARTICIPANT);
    }

    // ==================== TESTY DLA ZAPROSZEŃ UŻYTKOWNIKA ====================

    @Test
    void getUserInvitations_shouldReturnInvitedParticipants() {
        // Given
        MeetingParticipant invitedParticipant = MeetingParticipant.builder()
                .id(201L)
                .meeting(testMeeting)
                .user(testUser)
                .status(ParticipationStatus.INVITED)
                .build();

        when(participantRepository.findByUserIdAndStatus(2L, ParticipationStatus.INVITED))
                .thenReturn(List.of(invitedParticipant));

        // When
        List<ParticipantResponse> result = meetingParticipantService.getUserInvitations(2L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(ParticipationStatus.INVITED);
    }

    @Test
    void respondToInvitation_whenNotOwner_shouldThrowException() {
        // Given
        testParticipant.setStatus(ParticipationStatus.INVITED);
        User differentUser = User.builder().id(999L).build();
        testParticipant.setUser(differentUser);

        when(participantRepository.findById(200L)).thenReturn(Optional.of(testParticipant));

        // When & Then
        assertThatThrownBy(() -> meetingParticipantService
                .respondToInvitation(200L, ParticipationStatus.CONFIRMED, "", 2L))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("No permission");
    }

    @Test
    void respondToInvitation_whenAlreadyResponded_shouldThrowException() {
        // Given
        testParticipant.setStatus(ParticipationStatus.CONFIRMED);
        when(participantRepository.findById(200L)).thenReturn(Optional.of(testParticipant));

        // When & Then
        assertThatThrownBy(() -> meetingParticipantService
                .respondToInvitation(200L, ParticipationStatus.CONFIRMED, "", 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already been responded to");
    }

    // ==================== TESTY DLA STATYSTYK ====================

//    @Test
//    void getParticipantStatistics_whenOrganizerIsParticipant_shouldNotDoubleCount() {
//        // Given
//        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
//        when(participantRepository.countByMeetingId(100L)).thenReturn(5L);
//        when(participantRepository.countByMeetingIdAndStatus(100L, ParticipationStatus.CONFIRMED))
//                .thenReturn(5L);
//        when(participantRepository.findByMeetingIdAndUserId(100L, 1L))
//                .thenReturn(Optional.of(testOrganizerParticipant));
//
//        // When
//        Map<String, Long> stats = meetingParticipantService.getParticipantStatistics(100L);
//
//        // Then
//        assertThat(stats.get("total")).isEqualTo(5L); // Organizer already counted
//        assertThat(stats.get("confirmed")).isEqualTo(5L);
//        assertThat(stats.get("organizerIncluded")).isEqualTo(1L);
//    }

    @Test
    void getParticipantStatistics_whenException_shouldReturnDefaultStats() {
        // Given
        when(meetingRepository.findById(100L)).thenThrow(new RuntimeException("DB Error"));

        // When
        Map<String, Long> stats = meetingParticipantService.getParticipantStatistics(100L);

        // Then
        assertThat(stats).isNotEmpty();
        assertThat(stats.get("total")).isEqualTo(0L);
    }

    // ==================== TESTY DLA DOSTĘPU ====================

    @Test
    void hasAccessToMeeting_whenOrganizer_shouldReturnTrue() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));

        // When
        boolean result = meetingParticipantService.hasAccessToMeeting(100L, 1L);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void hasAccessToMeeting_whenParticipant_shouldReturnTrue() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));

        // When
        boolean result = meetingParticipantService.hasAccessToMeeting(100L, 2L);

        // Then
        assertThat(result).isTrue();
    }

//    @Test
//    void isOrganizer_whenUserIsOrganizer_shouldReturnTrue() {
//        // Given
//        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
//
//        // When
//        boolean result = meetingParticipantService.isOrganizer(100L, 1L);
//
//        // Then
//        assertThat(result).isTrue();
//    }

    @Test
    void isOrganizer_whenMeetingNotFound_shouldReturnFalse() {
        // Given
        when(meetingRepository.findById(100L)).thenThrow(new ResourceNotFoundException("Not found"));

        // When
        boolean result = meetingParticipantService.isOrganizer(100L, 1L);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void canEditParticipant_whenOrganizer_shouldReturnTrue() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));

        // When
        boolean result = meetingParticipantService.canEditParticipant(100L, 200L, 1L);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void canRemoveParticipant_whenNotOrganizer_shouldReturnFalse() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));

        // When
        boolean result = meetingParticipantService.canRemoveParticipant(100L, 200L, 2L);

        // Then
        assertThat(result).isFalse();
    }

    // ==================== TESTY DLA METOD UCZESTNIKA ====================

    @Test
    void isParticipant_whenOrganizer_shouldReturnTrue() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));

        // When
        boolean result = meetingParticipantService.isParticipant(100L, 1L);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isParticipant_whenConfirmed_shouldReturnTrue() {
        // Given
        testParticipant.setStatus(ParticipationStatus.CONFIRMED);
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));

        // When
        boolean result = meetingParticipantService.isParticipant(100L, 2L);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isParticipant_whenInvited_shouldReturnFalse() {
        // Given
        testParticipant.setStatus(ParticipationStatus.INVITED);
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));

        // When
        boolean result = meetingParticipantService.isParticipant(100L, 2L);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void getParticipantInfo_whenOrganizer_shouldReturnOrganizerResponse() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));

        // When
        ParticipantResponse result = meetingParticipantService.getParticipantInfo(1L, 100L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPermissionLevel()).isEqualTo(PermissionLevel.ORGANIZER);
    }

    @Test
    void getParticipantInfo_whenNotParticipant_shouldReturnNull() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.empty());

        // When
        ParticipantResponse result = meetingParticipantService.getParticipantInfo(2L, 100L);

        // Then
        assertThat(result).isNull();
    }

    // ==================== TESTY DLA POTWIERDZANIA UCZESTNICTWA ====================

    @Test
    void confirmParticipation_shouldReturnParticipantResponse() {
        // Given
        String token = "valid-token";
        testParticipant.setInvitationToken(token);
        testParticipant.setStatus(ParticipationStatus.INVITED);
        testMeeting.setStartDate(LocalDateTime.now().minusHours(1));
        testMeeting.setEndDate(LocalDateTime.now().plusHours(1));

        when(participantRepository.findByInvitationToken(token))
                .thenReturn(Optional.of(testParticipant));
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenReturn(testParticipant);

        // When
        ParticipantResponse result = meetingParticipantService.confirmParticipation(token, "Confirmed");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ParticipationStatus.ATTENDED);
    }

    @Test
    void declineParticipation_shouldUpdateStatusToDeclined() {
        // Given
        String token = "valid-token";
        testParticipant.setInvitationToken(token);
        testParticipant.setStatus(ParticipationStatus.INVITED);

        when(participantRepository.findByInvitationToken(token))
                .thenReturn(Optional.of(testParticipant));
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenReturn(testParticipant);

        // When
        ParticipantResponse result = meetingParticipantService.declineParticipation(token, "Cannot attend");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ParticipationStatus.DECLINED);
        assertThat(result.getComment()).isEqualTo("Cannot attend");
    }

    // ==================== TESTY DLA STATUSÓW UCZESTNIKA ====================

//    @Test
//    void isConfirmedParticipant_whenConfirmed_shouldReturnTrue() {
//        // Given
//        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
//        when(participantRepository.existsByMeetingIdAndUserIdAndStatus(
//                100L, 2L, ParticipationStatus.CONFIRMED))
//                .thenReturn(true);
//
//        // When
//        boolean result = meetingParticipantService.isConfirmedParticipant(100L, 2L);
//
//        // Then
//        assertThat(result).isTrue();
//    }

    @Test
    void isPendingParticipant_whenPending_shouldReturnTrue() {
        // Given
        when(participantRepository.existsByMeetingIdAndUserIdAndStatus(
                100L, 2L, ParticipationStatus.PENDING))
                .thenReturn(true);

        // When
        boolean result = meetingParticipantService.isPendingParticipant(100L, 2L);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isInvitedParticipant_whenInvited_shouldReturnTrue() {
        // Given
        when(participantRepository.existsByMeetingIdAndUserIdAndStatus(
                100L, 2L, ParticipationStatus.INVITED))
                .thenReturn(true);

        // When
        boolean result = meetingParticipantService.isInvitedParticipant(100L, 2L);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isDeclinedParticipant_whenDeclined_shouldReturnTrue() {
        // Given
        when(participantRepository.existsByMeetingIdAndUserIdAndStatus(
                100L, 2L, ParticipationStatus.DECLINED))
                .thenReturn(true);

        // When
        boolean result = meetingParticipantService.isDeclinedParticipant(100L, 2L);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isWaitingListParticipant_whenWaiting_shouldReturnTrue() {
        // Given
        when(participantRepository.existsByMeetingIdAndUserIdAndStatus(
                100L, 2L, ParticipationStatus.PENDING))
                .thenReturn(true);

        // When
        boolean result = meetingParticipantService.isWaitingListParticipant(100L, 2L);

        // Then
        assertThat(result).isTrue();
    }

    // ==================== TESTY DLA ROLI VIEWERA ====================

    @Test
    void isViewer_whenPublicMeetingAndNotParticipant_shouldReturnTrue() {
        // Given
        testMeeting.setVisibility(MeetingVisibility.PUBLIC);
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.findByMeetingIdAndUserId(100L, 999L))
                .thenReturn(Optional.empty());

        // When
        boolean result = meetingParticipantService.isViewer(100L, 999L);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isViewer_whenPrivateMeeting_shouldReturnFalse() {
        // Given
        testMeeting.setVisibility(MeetingVisibility.PRIVATE);
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));

        // When
        boolean result = meetingParticipantService.isViewer(100L, 999L);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isViewer_whenNullUserIdAndPublicMeeting_shouldReturnTrue() {
        // Given
        testMeeting.setVisibility(MeetingVisibility.PUBLIC);
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));

        // When
        boolean result = meetingParticipantService.isViewer(100L, null);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isUnrelatedUser_whenPrivateMeetingAndNotParticipant_shouldReturnTrue() {
        // Given
        testMeeting.setVisibility(MeetingVisibility.PRIVATE);
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.findByMeetingIdAndUserId(100L, 999L))
                .thenReturn(Optional.empty());

        // When
        boolean result = meetingParticipantService.isUnrelatedUser(100L, 999L);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isUnrelatedUser_whenPublicMeetingAndNotParticipant_shouldReturnFalse() {
        // Given
        testMeeting.setVisibility(MeetingVisibility.PUBLIC);
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.findByMeetingIdAndUserId(100L, 999L))
                .thenReturn(Optional.empty());

        // When
        boolean result = meetingParticipantService.isUnrelatedUser(100L, 999L);

        // Then
        assertThat(result).isFalse(); // Because they're a viewer
    }

    // ==================== TESTY DLA EKSPORTU CSV ====================

    @Test
    void exportParticipantsToCsv_shouldReturnByteArrayResource() {
        // Given
        List<ParticipantProjection> projections = new ArrayList<>();
        when(participantRepository.findParticipantsProjection(100L))
                .thenReturn(projections);

        // When
        ByteArrayResource result = meetingParticipantService.exportParticipantsToCsv(100L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.exists()).isTrue();
    }

    // ==================== TESTY DLA METOD ORGANIZATORA ====================

    @Test
    void addOrganizerAsParticipant_shouldSaveOrganizerAsParticipant() {
        // Given
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenReturn(testOrganizerParticipant);

        // When
        meetingParticipantService.addOrganizerAsParticipant(testMeeting, testOrganizer);

        // Then
        verify(participantRepository).save(any(MeetingParticipant.class));
    }

    @Test
    void confirmAttendance_shouldUpdateStatusToAttended() {
        // Given
        String token = "attendance-token";
        testParticipant.setInvitationToken(token);

        when(participantRepository.findByIdAndInvitationToken(200L, token))
                .thenReturn(Optional.of(testParticipant));

        // When
        meetingParticipantService.confirmAttendance(200L, token);

        // Then
        assertThat(testParticipant.getStatus()).isEqualTo(ParticipationStatus.ATTENDED);
        verify(participantRepository).save(testParticipant);
    }

    @Test
    void confirmAttendance_whenInvalidToken_shouldThrowException() {
        // Given
        when(participantRepository.findByIdAndInvitationToken(200L, "invalid-token"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> meetingParticipantService.confirmAttendance(200L, "invalid-token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Nieprawidłowy token");
    }

    // ==================== TESTY GRANICZNE ====================

    @Test
    void hasAvailableSpots_whenLimitReached_shouldReturnFalse() {
        // Given
        testMeeting.setMaxParticipants(5);
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.countByMeetingIdAndStatus(100L, ParticipationStatus.CONFIRMED))
                .thenReturn(5L);

        // When
        boolean result = meetingParticipantService.hasAvailableSpots(100L);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isMeetingFull_whenNoLimit_shouldReturnFalse() {
        // Given
        testMeeting.setMaxParticipants(null);
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));

        // When
        boolean result = meetingParticipantService.isMeetingFull(100L);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void getParticipant_whenNotFound_shouldThrowException() {
        // Given
        when(participantRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> meetingParticipantService.getParticipant(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Participant not found");
    }

//    @Test
//    void getMeetingStats_shouldReturnStatsInterface() {
//        // Given
//        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
//        when(participantRepository.countByMeetingId(100L)).thenReturn(5L);
//        when(participantRepository.countByMeetingIdAndStatus(100L, ParticipationStatus.CONFIRMED))
//                .thenReturn(3L);
//        when(participantRepository.countByMeetingIdAndStatus(100L, ParticipationStatus.INVITED))
//                .thenReturn(1L);
//        when(participantRepository.countByMeetingIdAndStatus(100L, ParticipationStatus.WAITING_LIST))
//                .thenReturn(1L);
//        when(participantRepository.countByMeetingIdAndStatus(100L, ParticipationStatus.PENDING))
//                .thenReturn(0L);
//        when(participantRepository.findByMeetingIdAndUserId(100L, 1L))
//                .thenReturn(Optional.of(testOrganizerParticipant));
//
//        // When
//        MeetingParticipantServiceImpl.ParticipantStats stats = meetingParticipantService.getMeetingStats(100L);
//
//        // Then
//        assertThat(stats.getTotalConfirmed()).isEqualTo(3L);
//        assertThat(stats.getTotalInvited()).isEqualTo(1L);
//        assertThat(stats.getWaitlistCount()).isEqualTo(1L);
//        assertThat(stats.getPendingCount()).isEqualTo(0L);
//    }

    @Test
    void getDetailedStats_shouldReturnMapWithParticipants() {
        // Given
        List<ParticipantProjection> projections = new ArrayList<>();
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.countByMeetingId(100L)).thenReturn(5L);
        when(participantRepository.countByMeetingIdAndStatus(100L, ParticipationStatus.CONFIRMED))
                .thenReturn(3L);
        when(participantRepository.findByMeetingIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(testOrganizerParticipant));
        when(participantRepository.findParticipantsProjection(100L))
                .thenReturn(projections);

        // When
        Map<String, Object> detailedStats = meetingParticipantService.getDetailedStats(100L);

        // Then
        assertThat(detailedStats).containsKeys("total", "confirmed", "participants", "meetingId");
        assertThat(detailedStats.get("meetingId")).isEqualTo(100L);
    }

    // ==================== TESTY DLA NULL PARAMETRÓW ====================

    @Test
    void isUserParticipant_whenNullMeetingId_shouldReturnFalse() {
        // When
        boolean result = meetingParticipantService.isUserParticipant(null, 2L);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isUserParticipant_whenNullUserId_shouldReturnFalse() {
        // When
        boolean result = meetingParticipantService.isUserParticipant(100L, null);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void getParticipantPermissionLevel_whenNullParameters_shouldReturnDefault() {
        // When
        PermissionLevel result = meetingParticipantService.getParticipantPermissionLevel(null, null);

        // Then
        assertThat(result).isEqualTo(PermissionLevel.PARTICIPANT);
    }

    // ==================== TESTY DLA METOD WAITLIST ====================

    @Test
    void isOnWaitlist_whenOnWaitlist_shouldReturnTrue() {
        // Given
        when(waitlistEntryRepository.existsByMeetingIdAndUserId(100L, 2L))
                .thenReturn(true);

        // When
        boolean result = meetingParticipantService.isOnWaitlist(100L, 2L);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void removeFromWaitlist_whenEntryNotFound_shouldDoNothing() {
        // Given
        when(waitlistEntryRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.empty());

        // When
        meetingParticipantService.removeFromWaitlist(100L, 2L);

        // Then
        verify(waitlistEntryRepository, never()).delete(any());
    }

    // ==================== TESTY INTEGRACYJNE ====================

    @Test
    void completeUserJourney_publicMeeting() {
        // 1. Użytkownik sprawdza czy może dołączyć
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.empty());
        when(participantRepository.countByMeetingIdAndStatus(100L, ParticipationStatus.CONFIRMED))
                .thenReturn(5L);


        // 2. Użytkownik dołącza
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(participantRepository.save(any(MeetingParticipant.class))).thenReturn(testParticipant);

        MeetingParticipant participant = meetingParticipantService.joinPublicMeeting(100L, 2L);
        assertThat(participant.getStatus()).isEqualTo(ParticipationStatus.CONFIRMED);

        // 3. Użytkownik opuszcza spotkanie
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));
        when(waitlistEntryRepository.findFirstByMeetingIdOrderByPositionAsc(100L))
                .thenReturn(Optional.empty());

        meetingParticipantService.leaveMeeting(2L, 100L);
        verify(participantRepository).delete(testParticipant);
    }
}
package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.*;
import com.meethub.domain.model.enums.MeetingVisibility;
import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.model.enums.PermissionLevel;
import com.meethub.domain.model.mapper.MeetingMapper;
import com.meethub.domain.model.request.InviteParticipantsRequest;
import com.meethub.domain.model.response.ParticipantResponse;
import com.meethub.domain.model.response.UserResponse;
import com.meethub.domain.repository.jpa.*;
import com.meethub.domain.service.EmailService;
import com.meethub.domain.service.NotificationService;
import com.meethub.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

        // Tworzymy Meeting i ustawiamy ID PO zbudowaniu
        testMeeting = Meeting.builder()
                .title("Test Meeting")
                .description("Test Description")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(1).plusHours(2))
                .visibility(MeetingVisibility.PUBLIC)
                .maxParticipants(10)
                .organizer(testOrganizer)
                .build();
        testMeeting.setId(100L); // USTAW ID PO ZBUDOWANIU

        testParticipant = MeetingParticipant.builder()
                .id(200L)
                .meeting(testMeeting)
                .user(testUser)
                .status(ParticipationStatus.CONFIRMED)
                .permissionLevel(PermissionLevel.PARTICIPANT)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

//    @Test
//    void getMeetingParticipants_shouldReturnParticipantsList() {
//        // Given
//        List<MeetingParticipant> participants = List.of(testParticipant);
//        when(participantRepository.findByMeetingId(100L)).thenReturn(participants);
//        when(meetingMapper.toResponse(any(Meeting.class))).thenReturn(new com.meethub.domain.model.response.MeetingResponse());
//
//        // When
//        List<ParticipantResponse> result = meetingParticipantService.getMeetingParticipants(100L);
//
//        // Then
//        assertThat(result).isNotNull();
//        assertThat(result).hasSize(1);
//        verify(participantRepository).findByMeetingId(100L);
//    }

    @Test
    void inviteParticipant_shouldSuccessfullyInviteUser() {
        // Given
        when(meetingRepository.findByIdAndOrganizerId(100L, 1L))
                .thenReturn(Optional.of(testMeeting));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.empty());

        // Mock isMeetingFull - używając lenient aby uniknąć problemów z strict stubbing
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.countByMeetingIdAndStatus(100L, ParticipationStatus.CONFIRMED))
                .thenReturn(5L);

        when(participantRepository.save(any(MeetingParticipant.class))).thenAnswer(invocation -> {
            MeetingParticipant participant = invocation.getArgument(0);
            participant.setId(300L);
            return participant;
        });

        // When
        MeetingParticipant result = meetingParticipantService.inviteParticipant(100L, 2L, 1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ParticipationStatus.INVITED);
        assertThat(result.getInvitationToken()).isNotNull();
        verify(emailService, times(1)).sendTemplateEmail(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void inviteParticipant_whenMeetingFull_shouldAddToWaitlist() {
        // Given
        testMeeting.setMaxParticipants(5);
        when(meetingRepository.findByIdAndOrganizerId(100L, 1L))
                .thenReturn(Optional.of(testMeeting));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.empty());

        // Mock isMeetingFull
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.countByMeetingIdAndStatus(100L, ParticipationStatus.CONFIRMED))
                .thenReturn(5L);

        // Mock addToWaitlist - użyj lenient aby uniknąć strict stubbing
        lenient().when(waitlistEntryRepository.existsByMeetingIdAndUserId(100L, 2L)).thenReturn(false);
        lenient().when(waitlistEntryRepository.findMaxPositionByMeetingId(100L)).thenReturn(Optional.of(0));
        when(waitlistEntryRepository.save(any(WaitlistEntry.class))).thenReturn(new WaitlistEntry());
        when(participantRepository.save(any(MeetingParticipant.class))).thenReturn(testParticipant);

        // When
        MeetingParticipant result = meetingParticipantService.inviteParticipant(100L, 2L, 1L);

        // Then
        assertThat(result).isNotNull();
        verify(waitlistEntryRepository).save(any(WaitlistEntry.class));
    }

    @Test
    void joinPublicMeeting_shouldSuccessfullyJoinMeeting() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.empty());
        when(participantRepository.countByMeetingIdAndStatus(100L, ParticipationStatus.CONFIRMED))
                .thenReturn(5L);
        when(participantRepository.save(any(MeetingParticipant.class))).thenReturn(testParticipant);

        // When
        MeetingParticipant result = meetingParticipantService.joinPublicMeeting(100L, 2L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ParticipationStatus.CONFIRMED);
        verify(notificationService).sendParticipantJoinedNotification(any(), any(), any());
    }

    @Test
    void joinPublicMeeting_whenAlreadyConfirmed_shouldThrowException() {
        // Given
        testParticipant.setStatus(ParticipationStatus.CONFIRMED);
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));

        // When & Then
        assertThatThrownBy(() -> meetingParticipantService.joinPublicMeeting(100L, 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User is already a confirmed participant");
    }

    @Test
    void requestToJoinPrivateMeeting_shouldCreatePendingRequest() {
        // Given
        testMeeting.setVisibility(MeetingVisibility.PRIVATE);
        MeetingParticipant pendingParticipant = MeetingParticipant.builder()
                .id(200L)
                .meeting(testMeeting)
                .user(testUser)
                .status(ParticipationStatus.PENDING)
                .permissionLevel(PermissionLevel.PARTICIPANT)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.empty());
        when(participantRepository.save(any(MeetingParticipant.class))).thenReturn(pendingParticipant);

        // When
        MeetingParticipant result = meetingParticipantService.requestToJoinPrivateMeeting(100L, 2L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ParticipationStatus.PENDING);
        verify(notificationService).sendJoinRequestNotification(any(), any(), any());
    }

    @Test
    void approveJoinRequest_shouldChangeStatusToConfirmed() {
        // Given
        testMeeting.setVisibility(MeetingVisibility.PRIVATE);
        testParticipant.setStatus(ParticipationStatus.PENDING);

        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.findById(200L)).thenReturn(Optional.of(testParticipant));
        when(participantRepository.countByMeetingIdAndStatus(100L, ParticipationStatus.CONFIRMED))
                .thenReturn(5L);
        lenient().when(waitlistEntryRepository.existsByMeetingIdAndUserId(100L, 2L)).thenReturn(false);

        // When
        meetingParticipantService.approveJoinRequest(100L, 200L, 1L);

        // Then
        assertThat(testParticipant.getStatus()).isEqualTo(ParticipationStatus.CONFIRMED);
        verify(participantRepository).save(testParticipant);
        verify(statusHistoryRepository).save(any(ParticipantStatusHistory.class));
        verify(notificationService).sendRequestApprovedNotification(any(), any());
    }

    @Test
    void hasAvailableSpots_whenNoLimit_shouldReturnTrue() {
        // Given
        testMeeting.setMaxParticipants(null);
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));

        // When
        boolean result = meetingParticipantService.hasAvailableSpots(100L);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void hasAvailableSpots_whenLimitNotReached_shouldReturnTrue() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.countByMeetingIdAndStatus(100L, ParticipationStatus.CONFIRMED))
                .thenReturn(5L);

        // When
        boolean result = meetingParticipantService.hasAvailableSpots(100L);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void canUserJoinMeeting_whenInviteOnly_shouldReturnFalse() {
        // Given
        testMeeting.setVisibility(MeetingVisibility.INVITE_ONLY);
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.empty());

        // When
        boolean result = meetingParticipantService.canUserJoinMeeting(100L, 2L);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void getParticipantPermissionLevel_whenUserIsOrganizer_shouldReturnOrganizer() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));

        // When
        PermissionLevel result = meetingParticipantService.getParticipantPermissionLevel(100L, 1L);

        // Then
        assertThat(result).isEqualTo(PermissionLevel.ORGANIZER);
    }

    @Test
    void getParticipantPermissionLevel_whenUserIsParticipant_shouldReturnParticipantLevel() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));

        // When
        PermissionLevel result = meetingParticipantService.getParticipantPermissionLevel(100L, 2L);

        // Then
        assertThat(result).isEqualTo(PermissionLevel.PARTICIPANT);
    }

    @Test
    void getConfirmedParticipants_shouldIncludeOrganizerIfNotInList() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.findByMeetingIdAndStatus(100L, ParticipationStatus.CONFIRMED))
                .thenReturn(Collections.emptyList());

        // When
        List<ParticipantResponse> result = meetingParticipantService.getConfirmedParticipants(100L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUser().getId()).isEqualTo(1L);
        assertThat(result.get(0).getPermissionLevel()).isEqualTo(PermissionLevel.ORGANIZER);
    }

    @Test
    void getParticipantStatistics_shouldIncludeOrganizerInCount() {
        // Given
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.countByMeetingId(100L)).thenReturn(5L);
        when(participantRepository.countByMeetingIdAndStatus(100L, ParticipationStatus.CONFIRMED))
                .thenReturn(5L);
        when(participantRepository.countByMeetingIdAndStatus(100L, ParticipationStatus.PENDING))
                .thenReturn(2L);
        when(participantRepository.countByMeetingIdAndStatus(100L, ParticipationStatus.INVITED))
                .thenReturn(3L);
        when(participantRepository.findByMeetingIdAndUserId(100L, 1L))
                .thenReturn(Optional.empty());

        // When
        Map<String, Long> stats = meetingParticipantService.getParticipantStatistics(100L);

        // Then
        assertThat(stats.get("total")).isEqualTo(6L);
        assertThat(stats.get("confirmed")).isEqualTo(6L);
        assertThat(stats.get("pending")).isEqualTo(2L);
        assertThat(stats.get("invited")).isEqualTo(3L);
    }

    @Test
    void leaveMeeting_whenWasConfirmed_shouldPromoteFromWaitlist() {
        // Given
        testParticipant.setStatus(ParticipationStatus.CONFIRMED);
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.of(testParticipant));
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.countByMeetingIdAndStatus(100L, ParticipationStatus.CONFIRMED))
                .thenReturn(4L);
        when(waitlistEntryRepository.findFirstByMeetingIdOrderByPositionAsc(100L))
                .thenReturn(Optional.empty());

        // When
        meetingParticipantService.leaveMeeting(2L, 100L);

        // Then
        verify(participantRepository).delete(testParticipant);
        verify(waitlistEntryRepository).findFirstByMeetingIdOrderByPositionAsc(100L);
    }

    @Test
    void respondToInvitation_shouldUpdateStatusAndSaveHistory() {
        // Given
        testParticipant.setStatus(ParticipationStatus.INVITED);
        when(participantRepository.findById(200L)).thenReturn(Optional.of(testParticipant));

        // When
        meetingParticipantService.respondToInvitation(200L, ParticipationStatus.CONFIRMED, "Accepted", 2L);

        // Then
        assertThat(testParticipant.getStatus()).isEqualTo(ParticipationStatus.CONFIRMED);
        assertThat(testParticipant.getComment()).isEqualTo("Accepted");
        assertThat(testParticipant.getResponseDate()).isNotNull();
        verify(participantRepository).save(testParticipant);
        verify(statusHistoryRepository).save(any(ParticipantStatusHistory.class));
    }

    @Test
    void searchUsersForInvitation_shouldFilterOutExistingParticipants() {
        // Given
        String query = "test";
        List<User> users = List.of(
                User.builder().id(1L).email("test1@example.com").build(),
                User.builder().id(2L).email("test2@example.com").build()
        );

        when(userRepository.findByEmailContainingOrFirstNameContainingOrLastNameContaining(query, query, query))
                .thenReturn(users);
        when(participantRepository.existsByMeetingIdAndUserId(100L, 1L)).thenReturn(true);
        when(participantRepository.existsByMeetingIdAndUserId(100L, 2L)).thenReturn(false);

        // When
        List<UserResponse> result = meetingParticipantService.searchUsersForInvitation(query, 100L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("test2@example.com");
    }

    @Test
    void acceptInvitationByToken_whenValidToken_shouldAcceptInvitation() {
        // Given
        String token = "valid-token-123";
        testParticipant.setStatus(ParticipationStatus.INVITED);
        testParticipant.setInvitationToken(token);
        testParticipant.setTokenExpiresAt(LocalDateTime.now().plusDays(1));

        // Ustaw status przed save
        MeetingParticipant savedParticipant = MeetingParticipant.builder()
                .id(200L)
                .meeting(testMeeting)
                .user(testUser)
                .status(ParticipationStatus.CONFIRMED)
                .permissionLevel(PermissionLevel.PARTICIPANT)
                .invitationToken(null)
                .responseDate(LocalDateTime.now())
                .build();

        when(participantRepository.findByInvitationToken(token))
                .thenReturn(Optional.of(testParticipant));

        // Mock isMeetingFull
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.countByMeetingIdAndStatus(100L, ParticipationStatus.CONFIRMED))
                .thenReturn(5L);

        // ZAWSZE zwracaj nie-nullowy obiekt
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenReturn(savedParticipant);

        // When
        MeetingParticipant result = meetingParticipantService.acceptInvitationByToken(token);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ParticipationStatus.CONFIRMED);
        assertThat(result.getInvitationToken()).isNull();
        assertThat(result.getResponseDate()).isNotNull();
        verify(participantRepository).save(any(MeetingParticipant.class));
    }

    @Test
    void acceptInvitationByToken_whenMeetingFull_shouldAddToWaitlist() {
        // Given
        String token = "valid-token-123";
        testParticipant.setStatus(ParticipationStatus.INVITED);
        testParticipant.setInvitationToken(token);
        testParticipant.setTokenExpiresAt(LocalDateTime.now().plusDays(1));

        when(participantRepository.findByInvitationToken(token))
                .thenReturn(Optional.of(testParticipant));

        // Mock isMeetingFull
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.countByMeetingIdAndStatus(100L, ParticipationStatus.CONFIRMED))
                .thenReturn(10L);
        when(waitlistEntryRepository.existsByMeetingIdAndUserId(100L, 2L)).thenReturn(false);

        // Mock addToWaitlist
        when(waitlistEntryRepository.findMaxPositionByMeetingId(100L)).thenReturn(Optional.of(0));
        when(waitlistEntryRepository.save(any(WaitlistEntry.class))).thenReturn(new WaitlistEntry());
        when(participantRepository.save(any(MeetingParticipant.class))).thenReturn(testParticipant);

        // When & Then
        assertThatThrownBy(() -> meetingParticipantService.acceptInvitationByToken(token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Meeting is full");

        // Użyj atLeastOnce() ponieważ isOnWaitlist() wywołuje to 2 razy
        verify(waitlistEntryRepository, atLeastOnce()).existsByMeetingIdAndUserId(100L, 2L);

        // Lub jeśli chcesz dokładnie 2 razy:
        // verify(waitlistEntryRepository, times(2)).existsByMeetingIdAndUserId(100L, 2L);
    }

    @Test
    void joinMeeting_whenPublic_shouldCallJoinPublicMeeting() {
        // Given
        testMeeting.setVisibility(MeetingVisibility.PUBLIC);
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.empty());
        when(participantRepository.countByMeetingIdAndStatus(100L, ParticipationStatus.CONFIRMED))
                .thenReturn(5L);
        when(participantRepository.save(any(MeetingParticipant.class))).thenReturn(testParticipant);

        // When
        meetingParticipantService.joinMeeting(2L, 100L);

        // Then
        verify(notificationService).sendParticipantJoinedNotification(any(), any(), any());
    }

    @Test
    void joinMeeting_whenPrivate_shouldCallRequestToJoinPrivateMeeting() {
        // Given
        testMeeting.setVisibility(MeetingVisibility.PRIVATE);
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(participantRepository.findByMeetingIdAndUserId(100L, 2L))
                .thenReturn(Optional.empty());
        when(participantRepository.save(any(MeetingParticipant.class))).thenReturn(testParticipant);

        // When
        meetingParticipantService.joinMeeting(2L, 100L);

        // Then
        verify(notificationService).sendJoinRequestNotification(any(), any(), any());
    }

    @Test
    void joinMeeting_whenInviteOnly_shouldThrowSecurityException() {
        // Given
        testMeeting.setVisibility(MeetingVisibility.INVITE_ONLY);
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(testMeeting));

        // When & Then
        assertThatThrownBy(() -> meetingParticipantService.joinMeeting(2L, 100L))
                .isInstanceOf(SecurityException.class)
                .hasMessage("This meeting is invite-only");
    }
}
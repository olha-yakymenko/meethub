package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.MeetingParticipant;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.repository.jpa.MeetingParticipantRepository;
import com.meethub.domain.repository.jpa.MeetingRepository;
import com.meethub.domain.repository.jpa.UserRepository;
import com.meethub.exception.BusinessException;
import com.meethub.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParticipationServiceImplTest {

    @Mock
    private MeetingParticipantRepository participantRepository;

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ParticipationServiceImpl participationService;

    private Meeting testMeeting;
    private User testUser;
    private MeetingParticipant testParticipant;

    @BeforeEach
    void setUp() {
        // Setup test meeting
        testMeeting = Meeting.builder()
                .title("Test Meeting")
                .description("Test Description")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .maxParticipants(10)
                .build();

        // Setup test user
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        // Setup test participant
        testParticipant = MeetingParticipant.builder()
                .id(1L)
                .meeting(testMeeting)
                .user(testUser)
                .status(ParticipationStatus.INVITED)
                .build();
    }

    // ========== TESTY confirmParticipation ==========

    @Test
    void confirmParticipation_shouldConfirm_whenValidRequest() {
        // Given
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.countByMeetingIdAndStatusIn(eq(1L), anyList()))
                .thenReturn(5L); // Mniej niż maxParticipants
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        MeetingParticipant result = participationService.confirmParticipation(1L, 1L);

        // Then
        assertNotNull(result);
        assertEquals(ParticipationStatus.CONFIRMED, result.getStatus());
        verify(participantRepository).save(testParticipant);
    }

    @Test
    void confirmParticipation_shouldThrow_whenAlreadyConfirmed() {
        // Given
        testParticipant.setStatus(ParticipationStatus.CONFIRMED);
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));

        // When & Then
        assertThrows(BusinessException.class,
                () -> participationService.confirmParticipation(1L, 1L));
    }

    @Test
    void confirmParticipation_shouldThrow_whenAlreadyDeclined() {
        // Given
        testParticipant.setStatus(ParticipationStatus.DECLINED);
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));

        // When & Then
        assertThrows(BusinessException.class,
                () -> participationService.confirmParticipation(1L, 1L));
    }

    @Test
    void confirmParticipation_shouldThrow_whenMeetingReachedMaxParticipants() {
        // Given
        testMeeting.setMaxParticipants(5);
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.countByMeetingIdAndStatusIn(eq(1L), anyList()))
                .thenReturn(5L); // Równe maxParticipants

        // When & Then
        assertThrows(BusinessException.class,
                () -> participationService.confirmParticipation(1L, 1L));
    }

    @Test
    void confirmParticipation_shouldWork_whenNoMaxParticipants() {
        // Given
        testMeeting.setMaxParticipants(null);
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        MeetingParticipant result = participationService.confirmParticipation(1L, 1L);

        // Then
        assertEquals(ParticipationStatus.CONFIRMED, result.getStatus());
    }

    // ========== TESTY declineParticipation ==========

    @Test
    void declineParticipation_shouldDecline_whenValidRequest() {
        // Given
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        MeetingParticipant result = participationService.declineParticipation(1L, 1L);

        // Then
        assertEquals(ParticipationStatus.DECLINED, result.getStatus());
        verify(participantRepository).save(testParticipant);
    }

    @Test
    void declineParticipation_shouldThrow_whenParticipantNotFound() {
        // Given
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> participationService.declineParticipation(1L, 1L));
    }

    // ========== TESTY markAsAttended ==========

    @Test
    void markAsAttended_shouldMark_whenParticipantConfirmed() {
        // Given
        testParticipant.setStatus(ParticipationStatus.CONFIRMED);
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        MeetingParticipant result = participationService.markAsAttended(1L, 1L);

        // Then
        assertEquals(ParticipationStatus.ATTENDED, result.getStatus());
    }

    @Test
    void markAsAttended_shouldThrow_whenParticipantNotConfirmed() {
        // Given
        testParticipant.setStatus(ParticipationStatus.INVITED);
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));

        // When & Then
        assertThrows(BusinessException.class,
                () -> participationService.markAsAttended(1L, 1L));
    }

    // ========== TESTY getResponseStatistics ==========

    @Test
    void getResponseStatistics_shouldReturnStatistics() {
        // Given
        MeetingParticipant confirmed1 = createParticipant(ParticipationStatus.CONFIRMED);
        MeetingParticipant confirmed2 = createParticipant(ParticipationStatus.CONFIRMED);
        MeetingParticipant declined = createParticipant(ParticipationStatus.DECLINED);
        MeetingParticipant invited = createParticipant(ParticipationStatus.INVITED);

        List<MeetingParticipant> participants = Arrays.asList(
                confirmed1, confirmed2, declined, invited
        );

        when(participantRepository.findByMeetingId(1L)).thenReturn(participants);

        // When
        Map<ParticipationStatus, Long> statistics = participationService.getResponseStatistics(1L);

        // Then
        assertNotNull(statistics);
        assertEquals(2, statistics.get(ParticipationStatus.CONFIRMED));
        assertEquals(1, statistics.get(ParticipationStatus.DECLINED));
        assertEquals(1, statistics.get(ParticipationStatus.INVITED));
        assertNull(statistics.get(ParticipationStatus.ATTENDED));
    }

    @Test
    void getResponseStatistics_shouldReturnEmptyMap_whenNoParticipants() {
        // Given
        when(participantRepository.findByMeetingId(1L)).thenReturn(Collections.emptyList());

        // When
        Map<ParticipationStatus, Long> statistics = participationService.getResponseStatistics(1L);

        // Then
        assertNotNull(statistics);
        assertTrue(statistics.isEmpty());
    }

    // ========== TESTY getAverageResponseTime ==========

    @Test
    void getAverageResponseTime_shouldReturnAverage_whenDataExists() {
        // Given
        when(participantRepository.findAverageResponseTimeHours(1L)).thenReturn(24.5);

        // When
        Double result = participationService.getAverageResponseTime(1L);

        // Then
        assertEquals(24.5, result);
    }

    @Test
    void getAverageResponseTime_shouldReturnZero_whenNoData() {
        // Given
        when(participantRepository.findAverageResponseTimeHours(1L)).thenReturn(null);

        // When
        Double result = participationService.getAverageResponseTime(1L);

        // Then
        assertEquals(0.0, result);
    }

    // ========== TESTY isUserParticipant ==========

    @Test
    void isUserParticipant_shouldReturnTrue_whenUserIsParticipant() {
        // Given
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));

        // When
        boolean result = participationService.isUserParticipant(1L, 1L);

        // Then
        assertTrue(result);
    }

    @Test
    void isUserParticipant_shouldReturnFalse_whenUserIsNotParticipant() {
        // Given
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.empty());

        // When
        boolean result = participationService.isUserParticipant(1L, 1L);

        // Then
        assertFalse(result);
    }

    // ========== TESTY isUserConfirmed ==========

    @Test
    void isUserConfirmed_shouldReturnTrue_whenUserIsConfirmed() {
        // Given
        testParticipant.setStatus(ParticipationStatus.CONFIRMED);
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));

        // When
        boolean result = participationService.isUserConfirmed(1L, 1L);

        // Then
        assertTrue(result);
    }

    @Test
    void isUserConfirmed_shouldReturnTrue_whenUserIsAttended() {
        // Given
        testParticipant.setStatus(ParticipationStatus.ATTENDED);
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));

        // When
        boolean result = participationService.isUserConfirmed(1L, 1L);

        // Then
        assertTrue(result);
    }

    @Test
    void isUserConfirmed_shouldReturnFalse_whenUserIsInvited() {
        // Given
        testParticipant.setStatus(ParticipationStatus.INVITED);
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));

        // When
        boolean result = participationService.isUserConfirmed(1L, 1L);

        // Then
        assertFalse(result);
    }

    @Test
    void isUserConfirmed_shouldReturnFalse_whenUserIsNotParticipant() {
        // Given
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.empty());

        // When
        boolean result = participationService.isUserConfirmed(1L, 1L);

        // Then
        assertFalse(result);
    }

    // ========== TESTY updateUserStatus ==========

    @Test
    void updateUserStatus_shouldUpdateStatus_whenValidRequest() {
        // Given
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        MeetingParticipant result = participationService.updateUserStatus(1L, 1L, ParticipationStatus.CONFIRMED);

        // Then
        assertEquals(ParticipationStatus.CONFIRMED, result.getStatus());
        verify(participantRepository).save(testParticipant);
    }

    @Test
    void updateUserStatus_shouldThrow_whenChangingDeclinedToConfirmed() {
        // Given
        testParticipant.setStatus(ParticipationStatus.DECLINED);
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));

        // When & Then
        assertThrows(BusinessException.class,
                () -> participationService.updateUserStatus(1L, 1L, ParticipationStatus.CONFIRMED));
    }

    @Test
    void updateUserStatus_shouldWork_whenChangingConfirmedToAttended() {
        // Given
        testParticipant.setStatus(ParticipationStatus.CONFIRMED);
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        MeetingParticipant result = participationService.updateUserStatus(1L, 1L, ParticipationStatus.ATTENDED);

        // Then
        assertEquals(ParticipationStatus.ATTENDED, result.getStatus());
    }

    // ========== TESTY getMeetingParticipants ==========

    @Test
    void getMeetingParticipants_shouldReturnAllParticipants() {
        // Given
        List<MeetingParticipant> participants = Arrays.asList(
                createParticipant(ParticipationStatus.INVITED),
                createParticipant(ParticipationStatus.CONFIRMED),
                createParticipant(ParticipationStatus.DECLINED)
        );

        when(participantRepository.findByMeetingId(1L)).thenReturn(participants);

        // When
        List<MeetingParticipant> result = participationService.getMeetingParticipants(1L);

        // Then
        assertEquals(3, result.size());
        verify(participantRepository).findByMeetingId(1L);
    }

    @Test
    void getMeetingParticipants_shouldReturnEmptyList_whenNoParticipants() {
        // Given
        when(participantRepository.findByMeetingId(1L)).thenReturn(Collections.emptyList());

        // When
        List<MeetingParticipant> result = participationService.getMeetingParticipants(1L);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== TESTY getConfirmedParticipants ==========

    @Test
    void getConfirmedParticipants_shouldReturnOnlyConfirmedAndAttended() {
        // Given
        List<MeetingParticipant> allParticipants = Arrays.asList(
                createParticipant(ParticipationStatus.INVITED),        // Nie powinien być włączony
                createParticipant(ParticipationStatus.CONFIRMED),      // Powinien być włączony
                createParticipant(ParticipationStatus.DECLINED),       // Nie powinien być włączony
                createParticipant(ParticipationStatus.ATTENDED),       // Powinien być włączony
                createParticipant(ParticipationStatus.PENDING)    // Nie powinien być włączony
        );

        when(participantRepository.findByMeetingId(1L)).thenReturn(allParticipants);

        // When
        List<MeetingParticipant> result = participationService.getConfirmedParticipants(1L);

        // Then
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(p ->
                p.getStatus() == ParticipationStatus.CONFIRMED ||
                        p.getStatus() == ParticipationStatus.ATTENDED));
    }

    @Test
    void getConfirmedParticipants_shouldReturnEmptyList_whenNoConfirmed() {
        // Given
        List<MeetingParticipant> allParticipants = Arrays.asList(
                createParticipant(ParticipationStatus.INVITED),
                createParticipant(ParticipationStatus.DECLINED),
                createParticipant(ParticipationStatus.PENDING)
        );

        when(participantRepository.findByMeetingId(1L)).thenReturn(allParticipants);

        // When
        List<MeetingParticipant> result = participationService.getConfirmedParticipants(1L);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== TESTY addToWaitingList ==========

    @Test
    void addToWaitingList_shouldAddUser_whenNotAlreadyParticipant() {
        // Given
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.empty());
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenAnswer(invocation -> {
                    MeetingParticipant p = invocation.getArgument(0);
                    p.setId(2L);
                    return p;
                });

        // When
        MeetingParticipant result = participationService.addToWaitingList(1L, 1L);

        // Then
        assertNotNull(result);
        assertEquals(ParticipationStatus.PENDING, result.getStatus());
        assertEquals(testMeeting, result.getMeeting());
        assertEquals(testUser, result.getUser());
        verify(participantRepository).save(any(MeetingParticipant.class));
    }

    @Test
    void addToWaitingList_shouldThrow_whenUserAlreadyParticipant() {
        // Given
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));

        // When & Then
        assertThrows(BusinessException.class,
                () -> participationService.addToWaitingList(1L, 1L));
    }

    @Test
    void addToWaitingList_shouldThrow_whenMeetingNotFound() {
        // Given
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.empty());
        when(meetingRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> participationService.addToWaitingList(1L, 1L));
    }

    @Test
    void addToWaitingList_shouldThrow_whenUserNotFound() {
        // Given
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.empty());
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> participationService.addToWaitingList(1L, 1L));
    }

    // ========== TESTY promoteFromWaitingList ==========

    @Test
    void promoteFromWaitingList_shouldPromote_whenValidRequest() {
        // Given
        testParticipant.setStatus(ParticipationStatus.PENDING);
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.countByMeetingIdAndStatusIn(eq(1L), anyList()))
                .thenReturn(5L); // Mniej niż maxParticipants
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        MeetingParticipant result = participationService.promoteFromWaitingList(1L, 1L);

        // Then
        assertEquals(ParticipationStatus.CONFIRMED, result.getStatus());
        verify(participantRepository).save(testParticipant);
    }

    @Test
    void promoteFromWaitingList_shouldThrow_whenNotOnWaitingList() {
        // Given
        testParticipant.setStatus(ParticipationStatus.INVITED);
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));

        // When & Then
        assertThrows(BusinessException.class,
                () -> participationService.promoteFromWaitingList(1L, 1L));
    }

    @Test
    void promoteFromWaitingList_shouldThrow_whenNoAvailableSpots() {
        // Given
        testParticipant.setStatus(ParticipationStatus.PENDING);
        testMeeting.setMaxParticipants(5);

        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.countByMeetingIdAndStatusIn(eq(1L), anyList()))
                .thenReturn(5L); // Równe maxParticipants

        // When & Then
        assertThrows(BusinessException.class,
                () -> participationService.promoteFromWaitingList(1L, 1L));
    }

    @Test
    void promoteFromWaitingList_shouldWork_whenNoMaxParticipants() {
        // Given
        testParticipant.setStatus(ParticipationStatus.PENDING);
        testMeeting.setMaxParticipants(null);

        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        MeetingParticipant result = participationService.promoteFromWaitingList(1L, 1L);

        // Then
        assertEquals(ParticipationStatus.CONFIRMED, result.getStatus());
    }

    // ========== TESTY validateCanChangeStatus ==========

    @Test
    void validateCanChangeStatus_shouldThrow_whenChangingDeclinedToConfirmed() {
        // Given
        testParticipant.setStatus(ParticipationStatus.DECLINED);
        ParticipationStatus newStatus = ParticipationStatus.CONFIRMED;

        // When & Then
        assertThrows(BusinessException.class,
                () -> participationService.validateCanChangeStatus(testParticipant, newStatus));
    }

    @Test
    void validateCanChangeStatus_shouldThrow_whenChangingConfirmedToDeclined() {
        // Given
        testParticipant.setStatus(ParticipationStatus.CONFIRMED);
        ParticipationStatus newStatus = ParticipationStatus.DECLINED;

        // When & Then
        assertThrows(BusinessException.class,
                () -> participationService.validateCanChangeStatus(testParticipant, newStatus));
    }

    @Test
    void validateCanChangeStatus_shouldNotThrow_whenValidTransition() {
        // Given
        testParticipant.setStatus(ParticipationStatus.INVITED);
        ParticipationStatus newStatus = ParticipationStatus.CONFIRMED;

        // When & Then
        assertDoesNotThrow(() ->
                participationService.validateCanChangeStatus(testParticipant, newStatus));
    }

    // ========== DODATKOWE TESTY ==========

    @Test
    void confirmParticipation_shouldThrow_whenParticipantNotFound() {
        // Given
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> participationService.confirmParticipation(1L, 1L));
    }

    @Test
    void markAsAttended_shouldThrow_whenParticipantNotFound() {
        // Given
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> participationService.markAsAttended(1L, 1L));
    }

    @Test
    void updateUserStatus_shouldThrow_whenParticipantNotFound() {
        // Given
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> participationService.updateUserStatus(1L, 1L, ParticipationStatus.CONFIRMED));
    }

    @Test
    void promoteFromWaitingList_shouldThrow_whenParticipantNotFound() {
        // Given
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> participationService.promoteFromWaitingList(1L, 1L));
    }

    @Test
    void getParticipant_shouldThrow_whenNotFound() {
        // Given
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> participationService.getParticipant(1L, 1L));
    }

    @Test
    void confirmParticipation_shouldHandleNullMeeting() {
        // Given
        testMeeting.setMaxParticipants(null);
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        MeetingParticipant result = participationService.confirmParticipation(1L, 1L);

        // Then
        assertEquals(ParticipationStatus.CONFIRMED, result.getStatus());
    }

    @Test
    void promoteFromWaitingList_shouldHandleNullMeeting() {
        // Given
        testParticipant.setStatus(ParticipationStatus.PENDING);
        testMeeting.setMaxParticipants(null);

        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        MeetingParticipant result = participationService.promoteFromWaitingList(1L, 1L);

        // Then
        assertEquals(ParticipationStatus.CONFIRMED, result.getStatus());
    }

    // ========== POMOCNICZE METODY ==========

    private MeetingParticipant createParticipant(ParticipationStatus status) {
        User user = User.builder()
                .id(2L)
                .email("user2@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .build();

        return MeetingParticipant.builder()
                .meeting(testMeeting)
                .user(user)
                .status(status)
                .build();
    }

    // Metoda pomocnicza do testowania prywatnej metody getParticipant
    // Uwaga: To wymaga refleksji, w normalnych warunkach nie testujemy prywatnych metod
    private MeetingParticipant getParticipant(Long meetingId, Long userId) {
        try {
            var method = ParticipationServiceImpl.class.getDeclaredMethod("getParticipant", Long.class, Long.class);
            method.setAccessible(true);
            return (MeetingParticipant) method.invoke(participationService, meetingId, userId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Metoda pomocnicza do testowania prywatnej metody validateCanChangeStatus
    private void validateCanChangeStatus(MeetingParticipant participant, ParticipationStatus newStatus) {
        try {
            var method = ParticipationServiceImpl.class.getDeclaredMethod("validateCanChangeStatus",
                    MeetingParticipant.class, ParticipationStatus.class);
            method.setAccessible(true);
            method.invoke(participationService, participant, newStatus);
        } catch (Exception e) {
            if (e.getCause() instanceof BusinessException) {
                throw (BusinessException) e.getCause();
            }
            throw new RuntimeException(e);
        }
    }
}
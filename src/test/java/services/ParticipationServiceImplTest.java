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
        testMeeting = Meeting.builder()
                .title("Test Meeting")
                .description("Test Description")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .maxParticipants(10)
                .build();

        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        testParticipant = MeetingParticipant.builder()
                .id(1L)
                .meeting(testMeeting)
                .user(testUser)
                .status(ParticipationStatus.INVITED)
                .build();
    }

    @Test
    void confirmParticipation_shouldConfirm_whenValidRequest() {
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.countByMeetingIdAndStatusIn(eq(1L), anyList()))
                .thenReturn(5L);
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MeetingParticipant result = participationService.confirmParticipation(1L, 1L);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(ParticipationStatus.CONFIRMED, result.getStatus())
        );

        verify(participantRepository).save(testParticipant);
    }

    @Test
    void confirmParticipation_shouldThrow_whenAlreadyConfirmed() {
        testParticipant.setStatus(ParticipationStatus.CONFIRMED);
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));

        assertThrows(BusinessException.class,
                () -> participationService.confirmParticipation(1L, 1L));
    }

    @Test
    void confirmParticipation_shouldThrow_whenAlreadyDeclined() {
        testParticipant.setStatus(ParticipationStatus.DECLINED);
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));

        assertThrows(BusinessException.class,
                () -> participationService.confirmParticipation(1L, 1L));
    }

    @Test
    void confirmParticipation_shouldThrow_whenMeetingReachedMaxParticipants() {
        testMeeting.setMaxParticipants(5);
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.countByMeetingIdAndStatusIn(eq(1L), anyList()))
                .thenReturn(5L);

        assertThrows(BusinessException.class,
                () -> participationService.confirmParticipation(1L, 1L));
    }

    @Test
    void confirmParticipation_shouldWork_whenNoMaxParticipants() {
        testMeeting.setMaxParticipants(null);
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MeetingParticipant result = participationService.confirmParticipation(1L, 1L);

        assertAll(
                () -> assertEquals(ParticipationStatus.CONFIRMED, result.getStatus())
        );
    }

    @Test
    void declineParticipation_shouldDecline_whenValidRequest() {
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MeetingParticipant result = participationService.declineParticipation(1L, 1L);

        assertAll(
                () -> assertEquals(ParticipationStatus.DECLINED, result.getStatus())
        );

        verify(participantRepository).save(testParticipant);
    }

    @Test
    void declineParticipation_shouldThrow_whenParticipantNotFound() {
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> participationService.declineParticipation(1L, 1L));
    }

    @Test
    void markAsAttended_shouldMark_whenParticipantConfirmed() {
        testParticipant.setStatus(ParticipationStatus.CONFIRMED);
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MeetingParticipant result = participationService.markAsAttended(1L, 1L);

        assertAll(
                () -> assertEquals(ParticipationStatus.ATTENDED, result.getStatus())
        );
    }

    @Test
    void markAsAttended_shouldThrow_whenParticipantNotConfirmed() {
        testParticipant.setStatus(ParticipationStatus.INVITED);
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));

        assertThrows(BusinessException.class,
                () -> participationService.markAsAttended(1L, 1L));
    }

    @Test
    void getResponseStatistics_shouldReturnStatistics() {
        MeetingParticipant confirmed1 = createParticipant(ParticipationStatus.CONFIRMED);
        MeetingParticipant confirmed2 = createParticipant(ParticipationStatus.CONFIRMED);
        MeetingParticipant declined = createParticipant(ParticipationStatus.DECLINED);
        MeetingParticipant invited = createParticipant(ParticipationStatus.INVITED);

        List<MeetingParticipant> participants = Arrays.asList(
                confirmed1, confirmed2, declined, invited
        );

        when(participantRepository.findByMeetingId(1L)).thenReturn(participants);

        Map<ParticipationStatus, Long> statistics = participationService.getResponseStatistics(1L);

        assertAll(
                () -> assertNotNull(statistics),
                () -> assertEquals(2, statistics.get(ParticipationStatus.CONFIRMED)),
                () -> assertEquals(1, statistics.get(ParticipationStatus.DECLINED)),
                () -> assertEquals(1, statistics.get(ParticipationStatus.INVITED)),
                () -> assertNull(statistics.get(ParticipationStatus.ATTENDED))
        );
    }

    @Test
    void getResponseStatistics_shouldReturnEmptyMap_whenNoParticipants() {
        when(participantRepository.findByMeetingId(1L)).thenReturn(Collections.emptyList());

        Map<ParticipationStatus, Long> statistics = participationService.getResponseStatistics(1L);

        assertAll(
                () -> assertNotNull(statistics),
                () -> assertTrue(statistics.isEmpty())
        );
    }

    @Test
    void getAverageResponseTime_shouldReturnAverage_whenDataExists() {
        when(participantRepository.findAverageResponseTimeHours(1L)).thenReturn(24.5);

        Double result = participationService.getAverageResponseTime(1L);

        assertAll(
                () -> assertEquals(24.5, result)
        );
    }

    @Test
    void getAverageResponseTime_shouldReturnZero_whenNoData() {
        when(participantRepository.findAverageResponseTimeHours(1L)).thenReturn(null);

        Double result = participationService.getAverageResponseTime(1L);

        assertAll(
                () -> assertEquals(0.0, result)
        );
    }

    @Test
    void isUserParticipant_shouldReturnTrue_whenUserIsParticipant() {
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));

        boolean result = participationService.isUserParticipant(1L, 1L);

        assertAll(
                () -> assertTrue(result)
        );
    }

    @Test
    void isUserParticipant_shouldReturnFalse_whenUserIsNotParticipant() {
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.empty());

        boolean result = participationService.isUserParticipant(1L, 1L);

        assertAll(
                () -> assertFalse(result)
        );
    }

    @Test
    void isUserConfirmed_shouldReturnTrue_whenUserIsConfirmed() {
        testParticipant.setStatus(ParticipationStatus.CONFIRMED);
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));

        boolean result = participationService.isUserConfirmed(1L, 1L);

        assertAll(
                () -> assertTrue(result)
        );
    }

    @Test
    void isUserConfirmed_shouldReturnTrue_whenUserIsAttended() {
        testParticipant.setStatus(ParticipationStatus.ATTENDED);
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));

        boolean result = participationService.isUserConfirmed(1L, 1L);

        assertAll(
                () -> assertTrue(result)
        );
    }

    @Test
    void isUserConfirmed_shouldReturnFalse_whenUserIsInvited() {
        testParticipant.setStatus(ParticipationStatus.INVITED);
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));

        boolean result = participationService.isUserConfirmed(1L, 1L);

        assertAll(
                () -> assertFalse(result)
        );
    }

    @Test
    void isUserConfirmed_shouldReturnFalse_whenUserIsNotParticipant() {
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.empty());

        boolean result = participationService.isUserConfirmed(1L, 1L);

        assertAll(
                () -> assertFalse(result)
        );
    }

    @Test
    void updateUserStatus_shouldUpdateStatus_whenValidRequest() {
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MeetingParticipant result = participationService.updateUserStatus(1L, 1L, ParticipationStatus.CONFIRMED);

        assertAll(
                () -> assertEquals(ParticipationStatus.CONFIRMED, result.getStatus())
        );

        verify(participantRepository).save(testParticipant);
    }

    @Test
    void updateUserStatus_shouldThrow_whenChangingDeclinedToConfirmed() {
        testParticipant.setStatus(ParticipationStatus.DECLINED);
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));

        assertThrows(BusinessException.class,
                () -> participationService.updateUserStatus(1L, 1L, ParticipationStatus.CONFIRMED));
    }

    @Test
    void updateUserStatus_shouldWork_whenChangingConfirmedToAttended() {
        testParticipant.setStatus(ParticipationStatus.CONFIRMED);
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MeetingParticipant result = participationService.updateUserStatus(1L, 1L, ParticipationStatus.ATTENDED);

        assertAll(
                () -> assertEquals(ParticipationStatus.ATTENDED, result.getStatus())
        );
    }

    @Test
    void getMeetingParticipants_shouldReturnAllParticipants() {
        List<MeetingParticipant> participants = Arrays.asList(
                createParticipant(ParticipationStatus.INVITED),
                createParticipant(ParticipationStatus.CONFIRMED),
                createParticipant(ParticipationStatus.DECLINED)
        );

        when(participantRepository.findByMeetingId(1L)).thenReturn(participants);

        List<MeetingParticipant> result = participationService.getMeetingParticipants(1L);

        assertAll(
                () -> assertEquals(3, result.size())
        );

        verify(participantRepository).findByMeetingId(1L);
    }

    @Test
    void getMeetingParticipants_shouldReturnEmptyList_whenNoParticipants() {
        when(participantRepository.findByMeetingId(1L)).thenReturn(Collections.emptyList());

        List<MeetingParticipant> result = participationService.getMeetingParticipants(1L);

        assertAll(
                () -> assertNotNull(result),
                () -> assertTrue(result.isEmpty())
        );
    }

    @Test
    void getConfirmedParticipants_shouldReturnOnlyConfirmedAndAttended() {
        List<MeetingParticipant> allParticipants = Arrays.asList(
                createParticipant(ParticipationStatus.INVITED),
                createParticipant(ParticipationStatus.CONFIRMED),
                createParticipant(ParticipationStatus.DECLINED),
                createParticipant(ParticipationStatus.ATTENDED),
                createParticipant(ParticipationStatus.PENDING)
        );

        when(participantRepository.findByMeetingId(1L)).thenReturn(allParticipants);

        List<MeetingParticipant> result = participationService.getConfirmedParticipants(1L);

        assertAll(
                () -> assertEquals(2, result.size()),
                () -> assertTrue(result.stream().allMatch(p ->
                        p.getStatus() == ParticipationStatus.CONFIRMED ||
                                p.getStatus() == ParticipationStatus.ATTENDED))
        );
    }

    @Test
    void getConfirmedParticipants_shouldReturnEmptyList_whenNoConfirmed() {
        List<MeetingParticipant> allParticipants = Arrays.asList(
                createParticipant(ParticipationStatus.INVITED),
                createParticipant(ParticipationStatus.DECLINED),
                createParticipant(ParticipationStatus.PENDING)
        );

        when(participantRepository.findByMeetingId(1L)).thenReturn(allParticipants);

        List<MeetingParticipant> result = participationService.getConfirmedParticipants(1L);

        assertAll(
                () -> assertNotNull(result),
                () -> assertTrue(result.isEmpty())
        );
    }

    @Test
    void addToWaitingList_shouldAddUser_whenNotAlreadyParticipant() {
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

        MeetingParticipant result = participationService.addToWaitingList(1L, 1L);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(ParticipationStatus.PENDING, result.getStatus()),
                () -> assertEquals(testMeeting, result.getMeeting()),
                () -> assertEquals(testUser, result.getUser())
        );

        verify(participantRepository).save(any(MeetingParticipant.class));
    }

    @Test
    void addToWaitingList_shouldThrow_whenUserAlreadyParticipant() {
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));

        assertThrows(BusinessException.class,
                () -> participationService.addToWaitingList(1L, 1L));
    }

    @Test
    void addToWaitingList_shouldThrow_whenMeetingNotFound() {
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.empty());
        when(meetingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> participationService.addToWaitingList(1L, 1L));
    }

    @Test
    void addToWaitingList_shouldThrow_whenUserNotFound() {
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.empty());
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> participationService.addToWaitingList(1L, 1L));
    }

    @Test
    void promoteFromWaitingList_shouldPromote_whenValidRequest() {
        testParticipant.setStatus(ParticipationStatus.PENDING);
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.countByMeetingIdAndStatusIn(eq(1L), anyList()))
                .thenReturn(5L);
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MeetingParticipant result = participationService.promoteFromWaitingList(1L, 1L);

        assertAll(
                () -> assertEquals(ParticipationStatus.CONFIRMED, result.getStatus())
        );

        verify(participantRepository).save(testParticipant);
    }

    @Test
    void promoteFromWaitingList_shouldThrow_whenNotOnWaitingList() {
        testParticipant.setStatus(ParticipationStatus.INVITED);
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));

        assertThrows(BusinessException.class,
                () -> participationService.promoteFromWaitingList(1L, 1L));
    }

    @Test
    void promoteFromWaitingList_shouldThrow_whenNoAvailableSpots() {
        testParticipant.setStatus(ParticipationStatus.PENDING);
        testMeeting.setMaxParticipants(5);

        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.countByMeetingIdAndStatusIn(eq(1L), anyList()))
                .thenReturn(5L);

        assertThrows(BusinessException.class,
                () -> participationService.promoteFromWaitingList(1L, 1L));
    }

    @Test
    void promoteFromWaitingList_shouldWork_whenNoMaxParticipants() {
        testParticipant.setStatus(ParticipationStatus.PENDING);
        testMeeting.setMaxParticipants(null);

        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MeetingParticipant result = participationService.promoteFromWaitingList(1L, 1L);

        assertAll(
                () -> assertEquals(ParticipationStatus.CONFIRMED, result.getStatus())
        );
    }


    @Test
    void confirmParticipation_shouldThrow_whenParticipantNotFound() {
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> participationService.confirmParticipation(1L, 1L));
    }

    @Test
    void markAsAttended_shouldThrow_whenParticipantNotFound() {
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> participationService.markAsAttended(1L, 1L));
    }

    @Test
    void updateUserStatus_shouldThrow_whenParticipantNotFound() {
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> participationService.updateUserStatus(1L, 1L, ParticipationStatus.CONFIRMED));
    }

    @Test
    void promoteFromWaitingList_shouldThrow_whenParticipantNotFound() {
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> participationService.promoteFromWaitingList(1L, 1L));
    }

    @Test
    void getParticipant_shouldThrow_whenNotFound() {
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> participationService.getParticipant(1L, 1L));
    }

    @Test
    void confirmParticipation_shouldHandleNullMeeting() {
        testMeeting.setMaxParticipants(null);
        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MeetingParticipant result = participationService.confirmParticipation(1L, 1L);

        assertAll(
                () -> assertEquals(ParticipationStatus.CONFIRMED, result.getStatus())
        );
    }

    @Test
    void promoteFromWaitingList_shouldHandleNullMeeting() {
        testParticipant.setStatus(ParticipationStatus.PENDING);
        testMeeting.setMaxParticipants(null);

        when(participantRepository.findByMeetingIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testParticipant));
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(testMeeting));
        when(participantRepository.save(any(MeetingParticipant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MeetingParticipant result = participationService.promoteFromWaitingList(1L, 1L);

        assertAll(
                () -> assertEquals(ParticipationStatus.CONFIRMED, result.getStatus())
        );
    }

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

    private MeetingParticipant getParticipant(Long meetingId, Long userId) {
        try {
            var method = ParticipationServiceImpl.class.getDeclaredMethod("getParticipant", Long.class, Long.class);
            method.setAccessible(true);
            return (MeetingParticipant) method.invoke(participationService, meetingId, userId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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
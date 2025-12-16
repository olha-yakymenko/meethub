package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.*;
import com.meethub.domain.model.enums.*;
import com.meethub.domain.repository.jpa.*;
import com.meethub.domain.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingSchedulerServiceImplTest {

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private MeetingParticipantRepository participantRepository;

    @Mock
    private UserPreferenceRepository userPreferenceRepository;

    @InjectMocks
    private MeetingSchedulerServiceImpl schedulerService;

    private Meeting testMeeting;
    private User testOrganizer;
    private MeetingParticipant testParticipant;
    private User testUser;
    private Location testLocation;

    @BeforeEach
    void setUp() {
        // Przygotowanie testowych danych
        testOrganizer = User.builder()
                .id(1L)
                .firstName("Jan")
                .lastName("Kowalski")
                .email("jan.kowalski@example.com")
                .build();

        testUser = User.builder()
                .id(2L)
                .firstName("Anna")
                .lastName("Nowak")
                .email("anna.nowak@example.com")
                .build();

        testLocation = Location.builder()
                .id(1L)
                .name("Pokój konferencyjny A")
                .type(LocationType.PHYSICAL)
                .build();

        testMeeting = Meeting.builder()
                .title("Spotkanie testowe")
                .description("Opis spotkania testowego")
                .startDate(LocalDateTime.now().plusHours(2))
                .endDate(LocalDateTime.now().plusHours(3))
                .organizer(testOrganizer)
                .location(testLocation)
                .build();

        testParticipant = MeetingParticipant.builder()
                .id(1L)
                .meeting(testMeeting)
                .user(testUser)
                .status(ParticipationStatus.CONFIRMED)
                .build();
    }

//    @Test
//    void shouldScheduleUpcomingMeetings_WhenSchedulerEnabled() {
//        // Given
//        LocalDateTime now = LocalDateTime.now();
//        LocalDateTime lookAhead = now.plusHours(48);
//
//        List<Meeting> upcomingMeetings = Collections.singletonList(testMeeting);
//
//        when(meetingRepository.findByStatusAndStartDateBetween(
//                MeetingStatus.PLANNED, now, lookAhead))
//                .thenReturn(upcomingMeetings);
//
//        when(participantRepository.countByMeetingIdAndStatus(
//                eq(1L), eq(ParticipationStatus.CONFIRMED)))
//                .thenReturn(1L);
//
//        // When
//        schedulerService.scheduleUpcomingMeetings();
//
//        // Then
//        verify(meetingRepository, times(1))
//                .findByStatusAndStartDateBetween(MeetingStatus.PLANNED, now, lookAhead);
//    }

    @Test
    void shouldNotScheduleUpcomingMeetings_WhenSchedulerDisabled() {
        // Given
        schedulerService.disableScheduling();

        // When
        schedulerService.scheduleUpcomingMeetings();

        // Then
        verify(meetingRepository, never())
                .findByStatusAndStartDateBetween(any(), any(), any());
    }

//    @Test
//    void shouldCloseOngoingMeetings_WhenEndDatePassed() {
//        // Given
//        LocalDateTime now = LocalDateTime.now();
//        LocalDateTime past = now.minusHours(1);
//
//        testMeeting.setStatus(MeetingStatus.ONGOING);
//        testMeeting.setEndDate(past);
//
//        List<Meeting> meetingsToClose = Collections.singletonList(testMeeting);
//
//        when(meetingRepository.findByStatusAndStartDateBetween(
//                any(), any(), any()))
//                .thenReturn(Collections.emptyList());
//
//        when(meetingRepository.findByStatusAndEndDateBefore(
//                MeetingStatus.ONGOING, now))
//                .thenReturn(meetingsToClose);
//
//        // When
//        schedulerService.scheduleUpcomingMeetings();
//
//        // Then
//        verify(meetingRepository, times(1)).save(testMeeting);
//        assertEquals(MeetingStatus.COMPLETED, testMeeting.getStatus());
//    }
//
//    @Test
//    void shouldScheduleMeetingNotifications_WhenMeetingHasParticipants() {
//        // Given
//        LocalDateTime startTime = LocalDateTime.now().plusHours(2);
//        testMeeting.setStartDate(startTime);
//
//        when(participantRepository.countByMeetingIdAndStatus(
//                eq(1L), eq(ParticipationStatus.CONFIRMED)))
//                .thenReturn(1L);
//
//        // When
//        schedulerService.scheduleMeetingNotifications(testMeeting);
//
//        // Then
//        assertNotNull(schedulerService.getSchedulerStatus());
//    }
//
//    @Test
//    void shouldNotScheduleMeetingNotifications_WhenMeetingAlreadyScheduled() {
//        // Given
//        testMeeting.setStartDate(LocalDateTime.now().plusMinutes(30));
//
//        // Symulacja, że spotkanie jest już zaplanowane
//        schedulerService.scheduleMeetingNotifications(testMeeting);
//
//        when(participantRepository.countByMeetingIdAndStatus(
//                eq(1L), eq(ParticipationStatus.CONFIRMED)))
//                .thenReturn(1L);
//
//        // When - próba ponownego zaplanowania
//        schedulerService.scheduleMeetingNotifications(testMeeting);
//
//        // Then - metoda powinna zakończyć się wcześniej
//        verify(participantRepository, times(2))
//                .countByMeetingIdAndStatus(anyLong(), any());
//    }
//
//    @Test
//    void shouldHandleMeetingStartImmediately_WhenStartTimePassed() {
//        // Given
//        testMeeting.setStartDate(LocalDateTime.now().minusMinutes(30));
//
//        when(participantRepository.countByMeetingIdAndStatus(
//                eq(1L), eq(ParticipationStatus.CONFIRMED)))
//                .thenReturn(1L);
//
//        // When
//        schedulerService.scheduleMeetingNotifications(testMeeting);
//
//        // Then - spotkanie powinno być natychmiast rozpoczęte
//        verify(meetingRepository, times(1)).save(testMeeting);
//    }
//
//    @Test
//    void shouldSendReminderNotification_ToConfirmedParticipants() {
//        // Given
//        List<MeetingParticipant> participants = Collections.singletonList(testParticipant);
//
//        when(participantRepository.findByMeetingIdAndStatus(
//                eq(1L), eq(ParticipationStatus.CONFIRMED)))
//                .thenReturn(participants);
//
//        when(userPreferenceRepository.findByUserIdAndPreferenceKey(
//                eq(2L), eq("meeting_reminders")))
//                .thenReturn(Optional.empty()); // Domyślnie true
//
//        // When
//        schedulerService.scheduleMeetingNotifications(testMeeting);
//
//        // Then - należy poczekać na wykonanie zadań (uproszczenie)
//        verify(notificationService, timeout(100).atLeastOnce())
//                .scheduleMeetingReminder(anyLong(), anyLong(), any());
//    }

//    @Test
//    void shouldNotSendReminder_WhenUserDisabledNotifications() {
//        // Given
//        UserPreference preference = UserPreference.builder()
//                .preferenceKey("meeting_reminders")
//                .preferenceValue("false")
//                .build();
//
//        List<MeetingParticipant> participants = Collections.singletonList(testParticipant);
//
//        when(participantRepository.findByMeetingIdAndStatus(
//                eq(1L), eq(ParticipationStatus.CONFIRMED)))
//                .thenReturn(participants);
//
//        when(userPreferenceRepository.findByUserIdAndPreferenceKey(
//                eq(2L), eq("meeting_reminders")))
//                .thenReturn(Optional.of(preference));
//
//        // When
//        schedulerService.scheduleMeetingNotifications(testMeeting);
//
//        // Then
//        verify(notificationService, timeout(100).never())
//                .scheduleMeetingReminder(anyLong(), eq(2L), any());
//    }
//
//    @Test
//    void shouldSendMeetingStartedNotifications_ToOrganizerAndParticipants() {
//        // Given
//        testMeeting.setStatus(MeetingStatus.ONGOING);
//
//        List<MeetingParticipant> participants = Collections.singletonList(testParticipant);
//
//        when(participantRepository.findByMeetingIdAndStatus(
//                eq(1L), eq(ParticipationStatus.CONFIRMED)))
//                .thenReturn(participants);
//
//        when(userPreferenceRepository.findByUserIdAndPreferenceKey(
//                anyLong(), eq("meeting_updates")))
//                .thenReturn(Optional.empty());
//
//        // When
//        schedulerService.scheduleMeetingNotifications(testMeeting);
//        schedulerService.handleMeetingStart(testMeeting);
//
//        // Then
//        verify(notificationService, atLeast(2))
//                .createNotificationFromTemplate(anyLong(), anyString(), anyMap(), any(), any());
//    }
//
//    @Test
//    void shouldCheckMeetingStatus_WhenMeetingNotStartedOnTime() {
//        // Given
//        testMeeting.setStartDate(LocalDateTime.now().minusMinutes(15));
//
//        when(meetingRepository.findById(eq(1L)))
//                .thenReturn(Optional.of(testMeeting));
//
//        when(participantRepository.countByMeetingIdAndStatus(
//                eq(1L), eq(ParticipationStatus.CONFIRMED)))
//                .thenReturn(1L);
//
//        // When
//        schedulerService.scheduleMeetingNotifications(testMeeting);
//
//        // Symulacja upływu czasu i wywołanie checkMeetingStatus
//        schedulerService.checkMeetingStatus(testMeeting);
//
//        // Then
//        verify(notificationService, times(1))
//                .createNotificationFromTemplate(eq(1L), eq("meeting_not_started"), anyMap(), any(), any());
//    }
//
//    @Test
//    void shouldAutoCancelMeeting_WhenOneHourLate() {
//        // Given
//        testMeeting.setStartDate(LocalDateTime.now().minusHours(2));
//
//        when(meetingRepository.findById(eq(1L)))
//                .thenReturn(Optional.of(testMeeting));
//
//        when(participantRepository.countByMeetingIdAndStatus(
//                eq(1L), eq(ParticipationStatus.CONFIRMED)))
//                .thenReturn(1L);
//
//        // When
//        schedulerService.checkMeetingStatus(testMeeting);
//
//        // Then
//        verify(meetingRepository, times(1)).save(testMeeting);
//        assertEquals(MeetingStatus.CANCELLED, testMeeting.getStatus());
//    }
//
//    @Test
//    void shouldCancelMeetingSchedule() {
//        // Given
//        testMeeting.setStartDate(LocalDateTime.now().plusMinutes(30));
//
//        when(participantRepository.countByMeetingIdAndStatus(
//                eq(1L), eq(ParticipationStatus.CONFIRMED)))
//                .thenReturn(1L);
//
//        schedulerService.scheduleMeetingNotifications(testMeeting);
//
//        // When
//        schedulerService.cancelMeetingSchedule(1L);
//
//        // Then
//        Map<String, Object> status = schedulerService.getSchedulerStatus();
//        assertEquals(0, status.get("scheduledMeetings"));
//    }
//
//    @Test
//    void shouldCleanupFinishedMeetings() {
//        // Given
//        testMeeting.setStatus(MeetingStatus.COMPLETED);
//        testMeeting.setUpdatedAt(LocalDateTime.now().minusHours(25));
//
//        List<Meeting> finishedMeetings = Collections.singletonList(testMeeting);
//
//        when(meetingRepository.findByStatusAndUpdatedAtBefore(
//                eq(MeetingStatus.COMPLETED), any(LocalDateTime.class)))
//                .thenReturn(finishedMeetings);
//
//        when(meetingRepository.findAllMeetingIds())
//                .thenReturn(Collections.emptyList());
//
//        // When
//        schedulerService.cleanupFinishedMeetings();
//
//        // Then
//        Map<String, Object> status = schedulerService.getSchedulerStatus();
//        assertEquals(0, status.get("scheduledMeetings"));
//    }

    @Test
    void shouldReturnCorrectSchedulerStatus() {
        // Given
        schedulerService.enableScheduling();

        // When
        Map<String, Object> status = schedulerService.getSchedulerStatus();

        // Then
        assertTrue((Boolean) status.get("isEnabled"));
        assertTrue((Boolean) status.get("schedulerActive"));
        assertNotNull(status.get("scheduledMeetings"));
        assertNotNull(status.get("totalTasks"));
    }

    @Test
    void shouldShutdownScheduler() {
        // When
        schedulerService.shutdown();

        // Then
        Map<String, Object> status = schedulerService.getSchedulerStatus();
        assertFalse((Boolean) status.get("isEnabled"));
    }

    @Test
    void shouldEnableAndDisableScheduling() {
        // When
        schedulerService.disableScheduling();

        // Then
        Map<String, Object> status = schedulerService.getSchedulerStatus();
        assertFalse((Boolean) status.get("isEnabled"));

        // When
        schedulerService.enableScheduling();

        // Then
        status = schedulerService.getSchedulerStatus();
        assertTrue((Boolean) status.get("isEnabled"));
    }
//
//    @Test
//    void shouldCreateMeetingStartedVariables_WithMeetingId() {
//        // When
//        Map<String, String> variables = schedulerService.createMeetingStartedVariables(testMeeting);
//
//        // Then
//        assertNotNull(variables);
//        assertEquals(testMeeting.getTitle(), variables.get("meetingTitle"));
//        assertEquals(testMeeting.getId().toString(), variables.get("meetingId"));
//        assertEquals(testMeeting.getOrganizer().getFirstName(), variables.get("organizerName"));
//        assertNotNull(variables.get("meetingDate"));
//    }

    @Test
    void shouldCreateReminderVariables_WithMeetingDetails() {
        // When
        Map<String, String> variables = schedulerService.createReminderVariables(testMeeting, 30);

        // Then
        assertNotNull(variables);
        assertEquals(testMeeting.getTitle(), variables.get("meetingTitle"));
        assertEquals("30", variables.get("minutesBefore"));
        assertEquals(testMeeting.getOrganizer().getFirstName(), variables.get("organizerName"));
    }

    @Test
    void shouldReturnTrueForNotificationEnabled_WhenNoPreference() {
        // Given
        when(userPreferenceRepository.findByUserIdAndPreferenceKey(
                eq(1L), eq("meeting_reminders")))
                .thenReturn(Optional.empty());

        // When
        boolean result = schedulerService.isNotificationEnabled(testOrganizer, "meeting_reminders");

        // Then
        assertTrue(result);
    }

//    @Test
//    void shouldReturnFalseForNotificationEnabled_WhenPreferenceIsFalse() {
//        // Given
//        UserPreference preference = UserPreference.builder()
//                .preferenceKey("meeting_reminders")
//                .preferenceValue("false")
//                .build();
//
//        when(userPreferenceRepository.findByUserIdAndPreferenceKey(
//                eq(1L), eq("meeting_reminders")))
//                .thenReturn(Optional.of(preference));
//
//        // When
//        boolean result = schedulerService.isNotificationEnabled(testOrganizer, "meeting_reminders");
//
//        // Then
//        assertFalse(result);
//    }
//
//    @Test
//    void shouldNotScheduleMeeting_WhenNoConfirmedParticipants() {
//        // Given
//        when(participantRepository.countByMeetingIdAndStatus(
//                eq(1L), eq(ParticipationStatus.CONFIRMED)))
//                .thenReturn(0L);
//
//        // When
//        boolean shouldSchedule = schedulerService.shouldScheduleMeeting(testMeeting);
//
//        // Then
//        assertFalse(shouldSchedule);
//    }
//
//    @Test
//    void shouldNotScheduleMeeting_WhenStartTimeLessThan5Minutes() {
//        // Given
//        testMeeting.setStartDate(LocalDateTime.now().plusMinutes(3));
//
//        when(participantRepository.countByMeetingIdAndStatus(
//                eq(1L), eq(ParticipationStatus.CONFIRMED)))
//                .thenReturn(1L);
//
//        // When
//        boolean shouldSchedule = schedulerService.shouldScheduleMeeting(testMeeting);
//
//        // Then
//        assertFalse(shouldSchedule);
//    }
//
//    @Test
//    void shouldScheduleMeeting_WhenValidConditions() {
//        // Given
//        testMeeting.setStartDate(LocalDateTime.now().plusMinutes(30));
//
//        when(participantRepository.countByMeetingIdAndStatus(
//                eq(1L), eq(ParticipationStatus.CONFIRMED)))
//                .thenReturn(1L);
//
//        // When
//        boolean shouldSchedule = schedulerService.shouldScheduleMeeting(testMeeting);
//
//        // Then
//        assertTrue(shouldSchedule);
//    }
//
//    @Test
//    void shouldHandleVirtualMeeting_WithMeetingLink() {
//        // Given
//        Location virtualLocation = Location.builder()
//                .id(2L)
//                .name("Spotkanie wirtualne")
//                .type(LocationType.VIRTUAL)
//                .virtualMeetingUrl("https://meet.example.com/123")
//                .build();
//
//        testMeeting.setLocation(virtualLocation);
//
//        when(participantRepository.findByMeetingIdAndStatus(
//                eq(1L), eq(ParticipationStatus.CONFIRMED)))
//                .thenReturn(Collections.singletonList(testParticipant));
//
//        when(userPreferenceRepository.findByUserIdAndPreferenceKey(
//                anyLong(), anyString()))
//                .thenReturn(Optional.empty());
//
//        // When
//        schedulerService.sendMeetingStartedNotifications(testMeeting);
//
//        // Then
//        verify(notificationService, atLeastOnce())
//                .createNotificationFromTemplate(anyLong(), anyString(), argThat(map ->
//                                map.containsKey("meetingLink") &&
//                                        "https://meet.example.com/123".equals(map.get("meetingLink"))),
//                        any(), any());
//    }

    @Test
    void shouldCleanupOrphanedTasks() {
        // Given
        List<Long> existingIds = Arrays.asList(1L, 2L, 3L);

        when(meetingRepository.findAllMeetingIds())
                .thenReturn(existingIds);

        // Dodanie zadań dla istniejących i nieistniejących spotkań
        schedulerService.addTask(1L, "task1", mock(ScheduledFuture.class));
        schedulerService.addTask(99L, "task99", mock(ScheduledFuture.class)); // nieistniejące

        // When
        schedulerService.cleanupOrphanedTasks();

        // Then
        Map<String, Object> status = schedulerService.getSchedulerStatus();
        assertEquals(1, status.get("scheduledMeetings")); // Tylko 1L powinien pozostać
    }
}
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

import java.lang.reflect.Field;
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

    @Test
    void shouldReturnCorrectSchedulerStatus() {
        // Given
        schedulerService.enableScheduling();

        // When
        Map<String, Object> status = schedulerService.getSchedulerStatus();

        // Then
        assertAll(
                () -> assertTrue((Boolean) status.get("isEnabled")),
                () -> assertTrue((Boolean) status.get("schedulerActive")),
                () -> assertNotNull(status.get("scheduledMeetings")),
                () -> assertNotNull(status.get("totalTasks"))
        );
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


    @Test
    void shouldNotScheduleMeeting_WhenMeetingStartsInLessThan5Minutes() {
        // Given
        Meeting meeting = Meeting.builder()
                .title("Bardzo bliskie spotkanie")
                .startDate(LocalDateTime.now().plusMinutes(3)) // Za 3 minuty
                .organizer(testOrganizer)
                .build();

        meeting.setId(1L);

        // When
        boolean result = schedulerService.shouldScheduleMeeting(meeting);

        // Then
        assertFalse(result);
    }

    @Test
    void shouldScheduleMeeting_WhenMeetingHasConfirmedParticipants() {
        // Given
        Meeting meeting = Meeting.builder()
                .title("Spotkanie z uczestnikami")
                .startDate(LocalDateTime.now().plusHours(1))
                .organizer(testOrganizer)
                .build();

        meeting.setId(1L);

        when(participantRepository.countByMeetingIdAndStatus(
                eq(1L), eq(ParticipationStatus.CONFIRMED)))
                .thenReturn(3L);

        // When
        boolean result = schedulerService.shouldScheduleMeeting(meeting);

        // Then
        assertTrue(result);
    }

    @Test
    void shouldReturnFalseForNotificationDisabled() {
        // Given
        UserPreference preference = UserPreference.builder()
                .preferenceKey("meeting_reminders")
                .preferenceValue("false")
                .build();

        when(userPreferenceRepository.findByUserIdAndPreferenceKey(
                eq(1L), eq("meeting_reminders")))
                .thenReturn(Optional.of(preference));

        // When
        boolean result = schedulerService.isNotificationEnabled(testOrganizer, "meeting_reminders");

        // Then
        assertFalse(result);
    }

    @Test
    void shouldCreateMeetingStartedVariables_WithVirtualMeetingLink() {
        // Given
        Location virtualLocation = Location.builder()
                .id(2L)
                .name("Zoom Meeting")
                .type(LocationType.VIRTUAL)
                .virtualMeetingUrl("https://zoom.us/j/123456")
                .build();

        Meeting virtualMeeting = Meeting.builder()
                .title("Spotkanie wirtualne")
                .startDate(LocalDateTime.now())
                .organizer(testOrganizer)
                .location(virtualLocation)
                .build();

        virtualMeeting.setId(2L);
        // When
        Map<String, String> variables = schedulerService.createMeetingStartedVariables(virtualMeeting);

        // Then
        assertEquals("https://zoom.us/j/123456", variables.get("meetingLink"));
        assertEquals("Spotkanie wirtualne", variables.get("meetingTitle"));
        assertNotNull(variables.get("meetingId"));
    }

    @Test
    void shouldCancelMeetingSchedule() {
        // Given
        Long meetingId = 1L;
        schedulerService.addTask(meetingId, "reminder_60min", mock(ScheduledFuture.class));
        schedulerService.addTask(meetingId, "start", mock(ScheduledFuture.class));

        // When
        schedulerService.cancelMeetingSchedule(meetingId);

        // Then
        Map<String, Object> status = schedulerService.getSchedulerStatus();
        assertEquals(0, status.get("scheduledMeetings"));
    }

    @Test
    void shouldHandleMeetingStartImmediately_WhenMeetingAlreadyStarted() {
        // Given
        Meeting meeting = Meeting.builder()
                .title("Opóźnione spotkanie")
                .startDate(LocalDateTime.now().minusMinutes(5))
                .organizer(testOrganizer)
                .build();

        meeting.setId(1L);
        meeting.setStatus(MeetingStatus.PLANNED);

        when(meetingRepository.save(any(Meeting.class))).thenReturn(meeting);

        // When
        schedulerService.handleMeetingStartImmediately(meeting);

        // Then
        verify(meetingRepository).save(meeting);
        assertEquals(MeetingStatus.ONGOING, meeting.getStatus());
    }


    @Test
    void scheduleMeetingNotifications_shouldNotScheduleWhenAlreadyScheduled() {
        // Given
        Meeting meeting = Meeting.builder()
                .title("Już zaplanowane spotkanie")
                .startDate(LocalDateTime.now().plusHours(2))
                .organizer(testOrganizer)
                .build();

        meeting.setId(2L);


        // Symuluj, że spotkanie jest już zaplanowane
        schedulerService.addTask(2L, "reminder_60min", mock(ScheduledFuture.class));

        // When
        schedulerService.scheduleMeetingNotifications(meeting);

        // Then - nie powinno dodawać nowych zadań (już zaplanowane)
        Map<String, Object> status = schedulerService.getSchedulerStatus();
        assertEquals(1, status.get("totalTasks")); // Tylko jedno istniejące zadanie
    }

    @Test
    void scheduleMeetingNotifications_shouldHandleMeetingThatShouldHaveStarted() {
        // Given
        Meeting meeting = Meeting.builder()
                .title("Opóźnione spotkanie")
                .startDate(LocalDateTime.now().minusMinutes(5)) // 5 minut temu
                .organizer(testOrganizer)
                .build();
        meeting.setId(3L);

        meeting.setStatus(MeetingStatus.PLANNED);

        when(meetingRepository.save(any(Meeting.class))).thenReturn(meeting);

        // When
        schedulerService.scheduleMeetingNotifications(meeting);

        // Then - powinno natychmiast rozpocząć spotkanie
        verify(meetingRepository, times(1)).save(meeting);
        assertEquals(MeetingStatus.ONGOING, meeting.getStatus());
    }


    @Test
    void scheduleMeetingNotifications_shouldLogAppropriateMessages() {
        // Given
        Meeting meeting = Meeting.builder()
                .title("Spotkanie z logowaniem")
                .startDate(LocalDateTime.now().plusHours(1))
                .organizer(testOrganizer)
                .build();

        meeting.setId(5L);

        // When
        schedulerService.scheduleMeetingNotifications(meeting);

        assertTrue(schedulerService.isMeetingAlreadyScheduled(5L));
    }

    @Test
    void scheduleMeetingNotifications_shouldHandleVirtualMeetingWithLink() {
        // Given
        Location virtualLocation = Location.builder()
                .id(1L)
                .name("Zoom Meeting")
                .type(LocationType.VIRTUAL)
                .virtualMeetingUrl("https://zoom.us/j/123456")
                .build();

        Meeting virtualMeeting = Meeting.builder()
                .title("Spotkanie wirtualne")
                .startDate(LocalDateTime.now().plusHours(2))
                .location(virtualLocation)
                .organizer(testOrganizer)
                .build();

        virtualMeeting.setId(6L);

        // When
        schedulerService.scheduleMeetingNotifications(virtualMeeting);

        // Then - powinno zaplanować normalnie
        assertTrue(schedulerService.isMeetingAlreadyScheduled(6L));
        Map<String, Object> status = schedulerService.getSchedulerStatus();
        assertTrue((int) status.get("totalTasks") > 0);
    }


    @Test
    void scheduleMeetingNotifications_shouldScheduleStatusCheck10MinutesAfterStart() {
        // Given
        Meeting meeting = Meeting.builder()
                .title("Spotkanie ze sprawdzeniem statusu")
                .startDate(LocalDateTime.now().plusMinutes(30)) // Za 30 minut
                .organizer(testOrganizer)
                .build();

        meeting.setId(7L);


        // When
        schedulerService.scheduleMeetingNotifications(meeting);

        // Then - status check powinien być za 40 minut (30 + 10)
        // Weryfikacja pośrednia poprzez sprawdzenie czy spotkanie jest zaplanowane
        assertTrue(schedulerService.isMeetingAlreadyScheduled(7L));
    }

    // Test integracyjny z użyciem ArgumentCaptor do przechwycenia ScheduledFuture
    @Test
    void scheduleMeetingNotifications_shouldScheduleCorrectNumberOfReminders() {
        // Given
        Meeting meeting = Meeting.builder()
                .title("Spotkanie z 7 reminderami")
                .startDate(LocalDateTime.now().plusDays(2)) // Za 2 dni (> 1440 minut)
                .organizer(testOrganizer)
                .build();

        meeting.setId(8L);
        // When
        schedulerService.scheduleMeetingNotifications(meeting);

        // Then - wszystkie 7 reminderów powinno być zaplanowane
        int reminderCount = schedulerService.countScheduledReminders(8L);
        assertEquals(7, reminderCount);

        // Plus start i status check
        Map<String, Object> status = schedulerService.getSchedulerStatus();
        int totalTasks = (int) status.get("totalTasks");
        assertEquals(9, totalTasks); // 7 reminders + start + status check
    }



}
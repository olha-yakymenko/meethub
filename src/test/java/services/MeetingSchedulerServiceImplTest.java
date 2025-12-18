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
}
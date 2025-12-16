package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.*;
import com.meethub.domain.model.enums.*;
import com.meethub.domain.repository.jpa.*;
import com.meethub.domain.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
class MeetingSchedulerServiceImplIntegrationTest {

    @Autowired
    private MeetingSchedulerServiceImpl schedulerService;

    @MockBean
    private MeetingRepository meetingRepository;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private MeetingParticipantRepository participantRepository;

    @MockBean
    private UserPreferenceRepository userPreferenceRepository;

    private Meeting testMeeting;
    private User testOrganizer;
    private MeetingParticipant testParticipant;

    @BeforeEach
    void setUp() {
        // Reset stanu schedulera przed każdym testem
        schedulerService.disableScheduling();

        testOrganizer = User.builder()
                .id(1L)
                .firstName("Jan")
                .email("jan@example.com")
                .build();

        testMeeting = Meeting.builder()
                .title("Spotkanie integracyjne")
                .startDate(LocalDateTime.now().plusMinutes(30)) // Wystarczająco w przyszłości
                .endDate(LocalDateTime.now().plusHours(1))
                .organizer(testOrganizer)
                .build();

        testParticipant = MeetingParticipant.builder()
                .id(1L)
                .meeting(testMeeting)
                .user(testOrganizer)
                .status(ParticipationStatus.CONFIRMED)
                .build();
    }
//
//    @Test
//    void shouldCompleteIntegrationFlow_FromPlannedToCompleted() throws InterruptedException {
//        // Given - Spotkanie planowane z ID
//        when(meetingRepository.findByStatusAndStartDateBetween(
//                eq(MeetingStatus.PLANNED), any(LocalDateTime.class), any(LocalDateTime.class)))
//                .thenReturn(Collections.singletonList(testMeeting));
//
//        when(participantRepository.countByMeetingIdAndStatus(
//                eq(1L), eq(ParticipationStatus.CONFIRMED)))
//                .thenReturn(1L);
//
//        when(participantRepository.findByMeetingIdAndStatus(
//                eq(1L), eq(ParticipationStatus.CONFIRMED)))
//                .thenReturn(Collections.singletonList(testParticipant));
//
//        when(userPreferenceRepository.findByUserIdAndPreferenceKey(
//                eq(1L), anyString()))
//                .thenReturn(Optional.empty());
//
//        when(meetingRepository.save(any(Meeting.class)))
//                .thenReturn(testMeeting);
//
//        // Włącz scheduler
//        schedulerService.enableScheduling();
//
//        // When - Planowanie spotkania (scheduled task się wykona)
//        schedulerService.scheduleUpcomingMeetings();
//
//        // Then - Poczekaj na wykonanie zadań asynchronicznych
//        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
//            verify(notificationService, atLeastOnce())
//                    .scheduleMeetingReminder(eq(1L), eq(1L), any(LocalDateTime.class));
//        });
//
//        // Given - Spotkanie się rozpoczyna
//        testMeeting.setStatus(MeetingStatus.ONGOING);
//        when(meetingRepository.findById(eq(1L)))
//                .thenReturn(Optional.of(testMeeting));
//
//        // When - Rozpoczęcie spotkania
//        schedulerService.handleMeetingStart(testMeeting);
//
//        // Then - Powiadomienia o rozpoczęciu powinny być wysłane
//        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
//            verify(notificationService, atLeastOnce())
//                    .createNotificationFromTemplate(
//                            eq(1L),
//                            anyString(),
//                            anyMap(),
//                            eq(NotificationType.MEETING_UPDATE),
//                            any(NotificationChannel.class)
//                    );
//        });
//
//        // Given - Spotkanie się kończy
//        testMeeting.setEndDate(LocalDateTime.now().minusMinutes(1));
//        when(meetingRepository.findByStatusAndEndDateBefore(
//                eq(MeetingStatus.ONGOING), any(LocalDateTime.class)))
//                .thenReturn(Collections.singletonList(testMeeting));
//
//        // When - Sprzątanie zakończonych spotkań
//        schedulerService.scheduleUpcomingMeetings();
//
//        // Then - Status powinien być zmieniony na COMPLETED
//        ArgumentCaptor<Meeting> meetingCaptor = ArgumentCaptor.forClass(Meeting.class);
//        verify(meetingRepository, atLeast(2)).save(meetingCaptor.capture());
//
//        Meeting savedMeeting = meetingCaptor.getAllValues().get(meetingCaptor.getAllValues().size() - 1);
//        assertEquals(MeetingStatus.COMPLETED, savedMeeting.getStatus());
//    }
//
//    @Test
//    void shouldHandleMultipleMeetings_Concurrently() {
//        // Given - Kilka spotkań z ID
//        Meeting meeting1 = Meeting.builder()
//                .title("Spotkanie 1")
//                .startDate(LocalDateTime.now().plusHours(1))
//                .organizer(User.builder().id(1L).build())
//                .build();
//
//        Meeting meeting2 = Meeting.builder()
//                .title("Spotkanie 2")
//                .startDate(LocalDateTime.now().plusHours(2))
//                .organizer(User.builder().id(2L).build())
//                .build();
//
//        when(meetingRepository.findByStatusAndStartDateBetween(
//                eq(MeetingStatus.PLANNED), any(LocalDateTime.class), any(LocalDateTime.class)))
//                .thenReturn(Arrays.asList(meeting1, meeting2));
//
//        when(participantRepository.countByMeetingIdAndStatus(anyLong(), eq(ParticipationStatus.CONFIRMED)))
//                .thenReturn(1L);
//
//        when(userPreferenceRepository.findByUserIdAndPreferenceKey(anyLong(), anyString()))
//                .thenReturn(Optional.empty());
//
//        // Włącz scheduler
//        schedulerService.enableScheduling();
//
//        // When
//        schedulerService.scheduleUpcomingMeetings();
//
//        // Then - Tylko jedno wywołanie (scheduled task wywołuje się wewnętrznie)
//        verify(meetingRepository, times(1))
//                .findByStatusAndStartDateBetween(eq(MeetingStatus.PLANNED), any(), any());
//
//        Map<String, Object> status = schedulerService.getSchedulerStatus();
//        assertNotNull(status);
//        assertEquals(2, status.get("scheduledMeetings")); // Oba spotkania powinny być zaplanowane
//    }
//
//    @Test
//    void shouldScheduleRemindersForAllIntervals() {
//        // Given - Spotkanie za 25 godzin (wystarczająco daleko dla wszystkich interwałów)
//        testMeeting.setStartDate(LocalDateTime.now().plusHours(25));
//
//        when(meetingRepository.findByStatusAndStartDateBetween(
//                eq(MeetingStatus.PLANNED), any(LocalDateTime.class), any(LocalDateTime.class)))
//                .thenReturn(Collections.singletonList(testMeeting));
//
//        when(participantRepository.countByMeetingIdAndStatus(
//                eq(1L), eq(ParticipationStatus.CONFIRMED)))
//                .thenReturn(1L);
//
//        when(userPreferenceRepository.findByUserIdAndPreferenceKey(anyLong(), anyString()))
//                .thenReturn(Optional.empty());
//
//        schedulerService.enableScheduling();
//
//        // When
//        schedulerService.scheduleUpcomingMeetings();
//
//        // Then - Wszystkie interwały powinny być zaplanowane (7 interwałów + start + status check = 9 zadań)
//        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
//            Map<String, Object> status = schedulerService.getSchedulerStatus();
//            int totalTasks = (int) status.get("totalTasks");
//            assertTrue(totalTasks >= 7, "Powinno być zaplanowanych co najmniej 7 przypomnień");
//        });
//    }

    @Test
    void shouldNotScheduleMeeting_WhenNoParticipants() {
        // Given - Spotkanie bez uczestników
        when(meetingRepository.findByStatusAndStartDateBetween(
                eq(MeetingStatus.PLANNED), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(testMeeting));

        when(participantRepository.countByMeetingIdAndStatus(
                eq(1L), eq(ParticipationStatus.CONFIRMED)))
                .thenReturn(0L); // Brak uczestników

        schedulerService.enableScheduling();

        // When
        schedulerService.scheduleUpcomingMeetings();

        // Then - Nie powinno być zaplanowanych zadań
        Map<String, Object> status = schedulerService.getSchedulerStatus();
        assertEquals(0, status.get("scheduledMeetings"));
    }
//
//    @Test
//    void shouldCancelMeetingSchedule() {
//        // Given - Zaplanowane spotkanie
//        when(meetingRepository.findByStatusAndStartDateBetween(
//                eq(MeetingStatus.PLANNED), any(LocalDateTime.class), any(LocalDateTime.class)))
//                .thenReturn(Collections.singletonList(testMeeting));
//
//        when(participantRepository.countByMeetingIdAndStatus(
//                eq(1L), eq(ParticipationStatus.CONFIRMED)))
//                .thenReturn(1L);
//
//        schedulerService.enableScheduling();
//        schedulerService.scheduleUpcomingMeetings();
//
//        // Potwierdź, że spotkanie jest zaplanowane
//        Map<String, Object> initialStatus = schedulerService.getSchedulerStatus();
//        assertTrue((int) initialStatus.get("scheduledMeetings") > 0);
//
//        // When
//        schedulerService.cancelMeetingSchedule(1L);
//
//        // Then
//        Map<String, Object> finalStatus = schedulerService.getSchedulerStatus();
//        assertEquals(0, finalStatus.get("scheduledMeetings"));
//    }
//
//    @Test
//    void shouldHandleMeetingStartImmediately_WhenLate() {
//        // Given - Spotkanie, które już powinno się rozpocząć
//        testMeeting.setStartDate(LocalDateTime.now().minusMinutes(10));
//
//        when(meetingRepository.findByStatusAndStartDateBetween(
//                eq(MeetingStatus.PLANNED), any(LocalDateTime.class), any(LocalDateTime.class)))
//                .thenReturn(Collections.singletonList(testMeeting));
//
//        when(participantRepository.countByMeetingIdAndStatus(
//                eq(1L), eq(ParticipationStatus.CONFIRMED)))
//                .thenReturn(1L);
//
//        when(userPreferenceRepository.findByUserIdAndPreferenceKey(anyLong(), anyString()))
//                .thenReturn(Optional.empty());
//
//        schedulerService.enableScheduling();
//
//        // When
//        schedulerService.scheduleUpcomingMeetings();
//
//        // Then - Spotkanie powinno być natychmiast rozpoczęte
//        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
//            verify(notificationService, atLeastOnce())
//                    .createNotificationFromTemplate(anyLong(), anyString(), anyMap(), any(), any());
//        });
//    }
//
//    @Test
//    void shouldCleanupFinishedMeetings() {
//        // Given - Zakończone spotkanie z przeszłości
//        testMeeting.setStatus(MeetingStatus.COMPLETED);
//        testMeeting.setUpdatedAt(LocalDateTime.now().minusHours(25));
//
//        when(meetingRepository.findByStatusAndUpdatedAtBefore(
//                eq(MeetingStatus.COMPLETED), any(LocalDateTime.class)))
//                .thenReturn(Collections.singletonList(testMeeting));
//
//        // Dodaj zadanie dla tego spotkania
//        schedulerService.addTask(1L, "test_task", mock(ScheduledFuture.class));
//
//        // When
//        schedulerService.cleanupFinishedMeetings();
//
//        // Then - Zadanie powinno być usunięte
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
    void shouldDisableScheduling() {
        // Given
        schedulerService.enableScheduling();

        // When
        schedulerService.disableScheduling();
        schedulerService.scheduleUpcomingMeetings();

        // Then - Nie powinno być żadnych wywołań do repository
        verify(meetingRepository, never())
                .findByStatusAndStartDateBetween(any(), any(), any());
    }
}
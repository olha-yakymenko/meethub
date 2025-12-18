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
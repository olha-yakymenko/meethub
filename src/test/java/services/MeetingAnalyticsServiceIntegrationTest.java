package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.*;
import com.meethub.domain.model.enums.*;
import com.meethub.domain.repository.jpa.*;
import com.meethub.domain.service.MeetingAnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=true",
        "spring.jpa.properties.hibernate.format_sql=true"
})
class MeetingAnalyticsServiceIntegrationTest {

    @Autowired
    private MeetingAnalyticsService analyticsService;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private MeetingParticipantRepository participantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MeetingStatisticsRepository statisticsRepository;

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskAssignmentRepository assignmentRepository;

    private User organizer;
    private User participant;
    private Meeting meeting;

    @BeforeEach
    void setUp() {
        // Create test users
        organizer = new User();
        organizer.setFirstName("Organizer");
        organizer.setLastName("Test");
        organizer.setEmail("organizer@test.com");
        organizer = userRepository.save(organizer);

        participant = new User();
        participant.setFirstName("Participant");
        participant.setLastName("Test");
        participant.setEmail("participant@test.com");
        participant = userRepository.save(participant);

        // Create meeting
        meeting = new Meeting();
        meeting.setTitle("Integration Test Meeting");
        meeting.setDescription("Test meeting for analytics");
        meeting.setStartDate(LocalDateTime.now().minusDays(2));
        meeting.setEndDate(LocalDateTime.now().minusDays(1));
        meeting.setOrganizer(organizer);
        meeting.setStatus(MeetingStatus.COMPLETED);
        meeting.setVisibility(MeetingVisibility.PUBLIC);
        meeting = meetingRepository.save(meeting);

        // Add participant
        MeetingParticipant meetingParticipant = new MeetingParticipant();
        meetingParticipant.setMeeting(meeting);
        meetingParticipant.setUser(participant);
        meetingParticipant.setStatus(ParticipationStatus.ATTENDED);
        participantRepository.save(meetingParticipant);

        // Add feedback
        Feedback feedback = new Feedback();
        feedback.setMeeting(meeting);
        feedback.setUser(participant);
        feedback.setRating(5);
        feedback.setComment("Great meeting!");
        feedback.setCreatedAt(LocalDateTime.now());
        feedbackRepository.save(feedback);
    }

    @Test
    void generateMeetingStatistics_shouldCreateAndPersistStatistics() {
        // When
        MeetingStatistics stats = analyticsService.generateMeetingStatistics(meeting.getId());

        // Then
        assertNotNull(stats);
        assertEquals(meeting.getId(), stats.getMeeting().getId());
        assertEquals(1, stats.getTotalParticipants());
        assertEquals(1, stats.getAttendedParticipants());
        assertThat(stats.getAttendanceRate()).isEqualByComparingTo("100.00");
        assertTrue(stats.getAvgFeedbackRating().compareTo(BigDecimal.ZERO) > 0);
        assertNotNull(stats.getGeneratedAt());
    }

    @Test
    void getMeetingStatistics_shouldReturnStatistics_whenGenerated() {
        // Given
        analyticsService.generateMeetingStatistics(meeting.getId());

        // When
        Optional<MeetingStatistics> stats = analyticsService.getMeetingStatistics(meeting.getId());

        // Then
        assertTrue(stats.isPresent());
        assertEquals(meeting.getId(), stats.get().getMeeting().getId());
    }

    @Test
    void getMeetingStatistics_shouldReturnEmpty_whenNotGenerated() {
        // When
        Optional<MeetingStatistics> stats = analyticsService.getMeetingStatistics(999L);

        // Then
        assertFalse(stats.isPresent());
    }

    @Test
    void deleteMeetingStatistics_shouldRemoveStatistics() {
        // Given
        analyticsService.generateMeetingStatistics(meeting.getId());

        // When
        analyticsService.deleteMeetingStatistics(meeting.getId());

        // Then
        assertFalse(analyticsService.getMeetingStatistics(meeting.getId()).isPresent());
    }

    @Test
    void getStatisticsOverview_shouldReturnCompleteOverview() {
        // Given
        analyticsService.generateMeetingStatistics(meeting.getId());

        // When
        Map<String, Object> overview = analyticsService.getStatisticsOverview(meeting.getId());

        // Then
        assertNotNull(overview);
        assertThat(overview).containsKeys(
                "meetingId", "meetingTitle", "attendanceRate",
                "engagementScore", "feedbackCount", "generatedAt", "grade"
        );
        assertEquals(meeting.getId(), overview.get("meetingId"));
        assertEquals("Integration Test Meeting", overview.get("meetingTitle"));
    }

    @Test
    void refreshAllStatistics_shouldProcessCompletedMeetings() {
        // When
        analyticsService.refreshAllStatistics();

        // Then
        Optional<MeetingStatistics> stats = statisticsRepository.findByMeetingId(meeting.getId());
        assertTrue(stats.isPresent());
    }

    @Test
    void refreshAllStatistics_shouldSkipNonCompletedMeetings() {
        // Given - create a non-completed meeting
        Meeting plannedMeeting = new Meeting();
        plannedMeeting.setTitle("Planned Meeting");
        plannedMeeting.setDescription("Not completed yet");
        plannedMeeting.setStartDate(LocalDateTime.now().plusDays(1));
        plannedMeeting.setEndDate(LocalDateTime.now().plusDays(2));
        plannedMeeting.setOrganizer(organizer);
        plannedMeeting.setStatus(MeetingStatus.PLANNED); // Different status
        plannedMeeting.setVisibility(MeetingVisibility.PUBLIC);
        meetingRepository.save(plannedMeeting);

        // When
        analyticsService.refreshAllStatistics();

        // Then - only completed meeting should have statistics
        Optional<MeetingStatistics> plannedStats = statisticsRepository.findByMeetingId(plannedMeeting.getId());
        assertFalse(plannedStats.isPresent());
    }

    @Test
    void getRecentStatistics_shouldReturnLimitedResults() {
        // Given - create multiple meetings with statistics
        for (int i = 1; i <= 5; i++) {
            Meeting m = new Meeting();
            m.setTitle("Meeting " + i);
            m.setStartDate(LocalDateTime.now().minusDays(i + 2));
            m.setEndDate(LocalDateTime.now().minusDays(i + 1));
            m.setOrganizer(organizer);
            m.setStatus(MeetingStatus.COMPLETED);
            m.setVisibility(MeetingVisibility.PUBLIC);
            m = meetingRepository.save(m);

            analyticsService.generateMeetingStatistics(m.getId());
        }

        // When
        List<MeetingStatistics> recent = analyticsService.getRecentStatistics(3);

        // Then
        assertThat(recent).hasSize(3);
    }

    @Test
    void calculateEngagementScore_withTasks_shouldIncludeTaskCompletion() {
        // Given - create a task with assignment
        Task task = new Task();
        task.setTitle("Test Task");
        task.setDescription("Test task description");
        task.setMeeting(meeting);
        task = taskRepository.save(task);

        TaskAssignment assignment = new TaskAssignment();
        assignment.setTask(task);
        assignment.setUser(participant);
        assignment.setStatus(AssignmentStatus.COMPLETED);
        assignment.setAssignedAt(LocalDateTime.now());
        assignmentRepository.save(assignment);

        // When
        MeetingStatistics stats = analyticsService.generateMeetingStatistics(meeting.getId());

        // Then
        assertNotNull(stats.getEngagementScore());
        assertTrue(stats.getEngagementScore().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void generateMeetingStatistics_shouldHandleMeetingWithoutParticipants() {
        // Given - meeting without participants
        Meeting emptyMeeting = new Meeting();
        emptyMeeting.setTitle("Empty Meeting");
        emptyMeeting.setStartDate(LocalDateTime.now().minusDays(1));
        emptyMeeting.setEndDate(LocalDateTime.now().minusHours(12));
        emptyMeeting.setOrganizer(organizer);
        emptyMeeting.setStatus(MeetingStatus.COMPLETED);
        emptyMeeting.setVisibility(MeetingVisibility.PUBLIC);
        emptyMeeting = meetingRepository.save(emptyMeeting);

        // When
        MeetingStatistics stats = analyticsService.generateMeetingStatistics(emptyMeeting.getId());

        // Then
        assertNotNull(stats);
        assertEquals(0, stats.getTotalParticipants());
        assertThat(stats.getAttendanceRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(stats.getEngagementScore()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void generateMeetingStatistics_shouldUpdateExistingStatistics() {
        // Given - generate statistics first time
        MeetingStatistics firstStats = analyticsService.generateMeetingStatistics(meeting.getId());

        // Add another participant
        User anotherParticipant = new User();
        anotherParticipant.setFirstName("Another");
        anotherParticipant.setLastName("Participant");
        anotherParticipant.setEmail("another@test.com");
        anotherParticipant = userRepository.save(anotherParticipant);

        MeetingParticipant newParticipant = new MeetingParticipant();
        newParticipant.setMeeting(meeting);
        newParticipant.setUser(anotherParticipant);
        newParticipant.setStatus(ParticipationStatus.CONFIRMED);
        participantRepository.save(newParticipant);

        // When - generate statistics again (should update)
        MeetingStatistics secondStats = analyticsService.generateMeetingStatistics(meeting.getId());

        // Then
        assertEquals(firstStats.getId(), secondStats.getId());
        assertEquals(2, secondStats.getTotalParticipants());
    }
}
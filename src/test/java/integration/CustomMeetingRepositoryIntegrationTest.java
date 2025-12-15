package com.meethub.repository.jdbc;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.enums.MeetingStatus;
import com.meethub.domain.model.enums.MeetingType;
import com.meethub.domain.model.enums.MeetingVisibility;
import com.meethub.domain.model.response.StatisticsResponse;
import com.meethub.domain.repository.jdbc.CustomMeetingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@ActiveProfiles("postgres")
@JdbcTest(includeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = CustomMeetingRepository.class
))
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CustomMeetingRepositoryIntegrationTest {

    @Autowired
    private CustomMeetingRepository customMeetingRepository;

    @BeforeEach
    void setUp() {
        // H2 automatycznie ładuje schema.sql i data.sql
    }

    @Test
    void testFindFilteredMeetings_WithNoFilters_ReturnsAllMeetings() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        var result = customMeetingRepository.findFilteredMeetings(null, null, null, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isNotEmpty();

        Meeting firstMeeting = result.getContent().get(0);
        assertThat(firstMeeting.getId()).isNotNull();
        assertThat(firstMeeting.getTitle()).isEqualTo("Test Meeting");
        assertThat(firstMeeting.getDescription()).isEqualTo("Test Description");
        assertThat(firstMeeting.getType()).isEqualTo(MeetingType.PHYSICAL);
        assertThat(firstMeeting.getStatus()).isEqualTo(MeetingStatus.PLANNED);
        assertThat(firstMeeting.getVisibility()).isEqualTo(MeetingVisibility.PUBLIC);

        // Sprawdź organizatora
        assertThat(firstMeeting.getOrganizer()).isNotNull();
        assertThat(firstMeeting.getOrganizer().getEmail()).isEqualTo("test.organizer@example.com");
        assertThat(firstMeeting.getOrganizer().getFirstName()).isEqualTo("Organizer");
        assertThat(firstMeeting.getOrganizer().getLastName()).isEqualTo("Test");
    }

    @Test
    void testFindFilteredMeetings_WithSearchFilter_ReturnsMatchingMeetings() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        var result = customMeetingRepository.findFilteredMeetings("Test", null, null, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).contains("Test");
    }

    @Test
    void testFindFilteredMeetings_WithStatusFilter_ReturnsFilteredMeetings() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        var result = customMeetingRepository.findFilteredMeetings(null, null, "PLANNED", pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(MeetingStatus.PLANNED);
    }

    @Test
    void testFindNearbyMeetings_ReturnsPublicPlannedMeetings() {
        // When
        List<Meeting> meetings = customMeetingRepository.findNearbyMeetings(52.0, 21.0, 10.0, 10);

        // Then
        assertThat(meetings).isNotEmpty();

        Meeting meeting = meetings.get(0);
        assertThat(meeting.getVisibility()).isEqualTo(MeetingVisibility.PUBLIC);
        assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.PLANNED); // W data.sql są PLANNED
        assertThat(meeting.getStartDate()).isAfter(LocalDateTime.now());
    }

    @Test
    void testGetMeetingStatistics_ReturnsStatisticsForOrganizer() {
        // Given
        Long organizerId = 2L; // ID organizatora z data.sql

        // When
        List<StatisticsResponse> statistics = customMeetingRepository.getMeetingStatistics(organizerId);

        // Then
        assertThat(statistics).isNotEmpty();

        StatisticsResponse stats = statistics.get(0);
        assertThat(stats.getTotalMeetings()).isGreaterThan(0);
        assertThat(stats.getCompletedMeetings()).isNotNull();
        assertThat(stats.getCancelledMeetings()).isNotNull();
        assertThat(stats.getAverageDuration()).isNotNull();
        assertThat(stats.getTotalParticipants()).isNotNull();
    }

    @Test
    void testBulkUpdateMeetingStatus_SuccessfullyUpdatesMeetings() {
        // Given
        List<Long> meetingIds = List.of(1L); // ID spotkania z data.sql
        String newStatus = "CANCELLED";

        // When
        int updated = customMeetingRepository.bulkUpdateMeetingStatus(meetingIds, newStatus);

        // Then
        assertThat(updated).isEqualTo(1);

        // Sprawdź czy status się zmienił
        Pageable pageable = PageRequest.of(0, 10);
        var result = customMeetingRepository.findFilteredMeetings(null, null, "CANCELLED", pageable);
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void testGetMeetingParticipantsCount_ReturnsCorrectCount() {
        // Given
        Long meetingId = 1L;

        // When
        int count = customMeetingRepository.getMeetingParticipantsCount(meetingId);

        // Then
        assertThat(count).isGreaterThanOrEqualTo(0);
    }
}
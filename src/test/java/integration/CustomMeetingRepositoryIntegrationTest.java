package com.meethub.repository.jdbc;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.enums.MeetingStatus;
import com.meethub.domain.model.enums.MeetingType;
import com.meethub.domain.model.enums.MeetingVisibility;
import com.meethub.domain.model.response.StatisticsResponse;
import com.meethub.domain.repository.jdbc.CustomMeetingRepository;
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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Test
    void testFindFilteredMeetings_WithNoFilters_ReturnsAllMeetings() {
        Pageable pageable = PageRequest.of(0, 10);

        var result = customMeetingRepository.findFilteredMeetings(null, null, null, pageable);

        assertAll("Verify all properties of the first meeting",
                () -> assertNotNull(result),
                () -> assertThat(result.getContent()).isNotEmpty(),
                () -> {
                    Meeting firstMeeting = result.getContent().get(0);
                    assertAll("First meeting fields",
                            () -> assertNotNull(firstMeeting.getId()),
                            () -> assertThat(firstMeeting.getTitle()).isEqualTo("Test Meeting"),
                            () -> assertThat(firstMeeting.getDescription()).isEqualTo("Test Description"),
                            () -> assertThat(firstMeeting.getType()).isEqualTo(MeetingType.PHYSICAL),
                            () -> assertThat(firstMeeting.getStatus()).isEqualTo(MeetingStatus.PLANNED),
                            () -> assertThat(firstMeeting.getVisibility()).isEqualTo(MeetingVisibility.PUBLIC)
                    );
                    assertAll("Organizer fields",
                            () -> assertNotNull(firstMeeting.getOrganizer()),
                            () -> assertThat(firstMeeting.getOrganizer().getEmail()).isEqualTo("test.organizer@example.com"),
                            () -> assertThat(firstMeeting.getOrganizer().getFirstName()).isEqualTo("Organizer"),
                            () -> assertThat(firstMeeting.getOrganizer().getLastName()).isEqualTo("Test")
                    );
                }
        );
    }

    @Test
    void testFindFilteredMeetings_WithSearchFilter_ReturnsMatchingMeetings() {
        Pageable pageable = PageRequest.of(0, 10);

        var result = customMeetingRepository.findFilteredMeetings("Test", null, null, pageable);

        assertAll("Search filter results",
                () -> assertNotNull(result),
                () -> assertThat(result.getContent()).hasSize(1),
                () -> assertThat(result.getContent().get(0).getTitle()).contains("Test")
        );
    }

    @Test
    void testFindFilteredMeetings_WithStatusFilter_ReturnsFilteredMeetings() {
        Pageable pageable = PageRequest.of(0, 10);

        var result = customMeetingRepository.findFilteredMeetings(null, null, "PLANNED", pageable);

        assertAll("Status filter results",
                () -> assertNotNull(result),
                () -> assertThat(result.getContent()).hasSize(1),
                () -> assertThat(result.getContent().get(0).getStatus()).isEqualTo(MeetingStatus.PLANNED)
        );
    }

    @Test
    void testFindNearbyMeetings_ReturnsPublicPlannedMeetings() {
        List<Meeting> meetings = customMeetingRepository.findNearbyMeetings(52.0, 21.0, 10.0, 10);

        assertAll("Nearby meetings",
                () -> assertThat(meetings).isNotEmpty(),
                () -> {
                    Meeting meeting = meetings.get(0);
                    assertAll("First nearby meeting fields",
                            () -> assertThat(meeting.getVisibility()).isEqualTo(MeetingVisibility.PUBLIC),
                            () -> assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.PLANNED),
                            () -> assertThat(meeting.getStartDate()).isAfter(LocalDateTime.now())
                    );
                }
        );
    }

    @Test
    void testGetMeetingStatistics_ReturnsStatisticsForOrganizer() {
        Long organizerId = 2L;

        List<StatisticsResponse> statistics = customMeetingRepository.getMeetingStatistics(organizerId);

        assertAll("Meeting statistics",
                () -> assertThat(statistics).isNotEmpty(),
                () -> {
                    StatisticsResponse stats = statistics.get(0);
                    assertAll("Statistics fields",
                            () -> assertThat(stats.getTotalMeetings()).isGreaterThan(0),
                            () -> assertNotNull(stats.getCompletedMeetings()),
                            () -> assertNotNull(stats.getCancelledMeetings()),
                            () -> assertNotNull(stats.getAverageDuration()),
                            () -> assertNotNull(stats.getTotalParticipants())
                    );
                }
        );
    }

    @Test
    void testBulkUpdateMeetingStatus_SuccessfullyUpdatesMeetings() {
        List<Long> meetingIds = List.of(1L);
        String newStatus = "CANCELLED";

        int updated = customMeetingRepository.bulkUpdateMeetingStatus(meetingIds, newStatus);

        Pageable pageable = PageRequest.of(0, 10);
        var result = customMeetingRepository.findFilteredMeetings(null, null, "CANCELLED", pageable);

        assertAll("Bulk update status",
                () -> assertThat(updated).isEqualTo(1),
                () -> assertThat(result.getContent()).hasSize(1)
        );
    }

    @Test
    void testGetMeetingParticipantsCount_ReturnsCorrectCount() {
        Long meetingId = 1L;

        int count = customMeetingRepository.getMeetingParticipantsCount(meetingId);

        assertAll("Participants count",
                () -> assertTrue(count >= 0)
        );
    }
}

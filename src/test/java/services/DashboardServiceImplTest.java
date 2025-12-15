package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.MeetingParticipant;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.MeetingStatus;
import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.model.response.DashboardStatsResponse;
import com.meethub.domain.repository.jpa.MeetingRepository;
import com.meethub.domain.repository.jpa.MeetingParticipantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardServiceImpl Tests")
class DashboardServiceImplTest {

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private MeetingParticipantRepository participantRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private User testUser;
    private User otherUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        otherUser = User.builder()
                .id(2L)
                .email("other@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .build();
    }

    @Nested
    @DisplayName("When user has no meetings")
    class NoMeetingsTests {

        @Test
        @DisplayName("Should return zero for all statistics")
        void shouldReturnZeroForAllStatistics() {
            // Given
            when(meetingRepository.findByOrganizerId(1L)).thenReturn(List.of());
            when(participantRepository.findByUserId(1L)).thenReturn(List.of());
            when(participantRepository.countByMeetingOrganizerId(1L)).thenReturn(0L);

            // When
            DashboardStatsResponse result = dashboardService.getUserDashboardStats(1L);

            // Then
            assertAll(
                    () -> assertThat(result.getTotalMeetings()).isZero(),
                    () -> assertThat(result.getUpcomingMeetings()).isZero(),
                    () -> assertThat(result.getParticipantsCount()).isZero(),
                    () -> assertThat(result.getOrganizedMeetings()).isZero(),
                    () -> assertThat(result.getMeetingsToday()).isZero(),
                    () -> assertThat(result.getMeetingsThisWeek()).isZero(),
                    () -> assertThat(result.getMeetingsThisMonth()).isZero()
            );
        }
    }

    @Nested
    @DisplayName("Organized meetings statistics")
    class OrganizedMeetingsTests {

        @Test
        @DisplayName("Should count organized meetings correctly")
        void shouldCountOrganizedMeetings() {
            // Given
            Meeting meeting1 = createMeeting(1L, "Future Meeting", testUser,
                    LocalDateTime.now().plusDays(1), MeetingStatus.PLANNED);

            Meeting meeting2 = createMeeting(2L, "Past Meeting", testUser,
                    LocalDateTime.now().minusDays(1), MeetingStatus.COMPLETED);

            when(meetingRepository.findByOrganizerId(1L)).thenReturn(Arrays.asList(meeting1, meeting2));
            when(participantRepository.findByUserId(1L)).thenReturn(List.of());
            when(participantRepository.countByMeetingOrganizerId(1L)).thenReturn(5L);

            // When
            DashboardStatsResponse result = dashboardService.getUserDashboardStats(1L);

            // Then
            assertThat(result.getOrganizedMeetings()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should count participants in organized meetings")
        void shouldCountParticipantsInOrganizedMeetings() {
            // Given
            Meeting meeting = createMeeting(1L, "Test Meeting", testUser,
                    LocalDateTime.now().plusDays(1), MeetingStatus.PLANNED);

            when(meetingRepository.findByOrganizerId(1L)).thenReturn(List.of(meeting));
            when(participantRepository.findByUserId(1L)).thenReturn(List.of());
            when(participantRepository.countByMeetingOrganizerId(1L)).thenReturn(3L);

            // When
            DashboardStatsResponse result = dashboardService.getUserDashboardStats(1L);

            // Then
            assertThat(result.getParticipantsCount()).isEqualTo(3L);
        }
    }

    @Nested
    @DisplayName("Participant meetings statistics")
    class ParticipantMeetingsTests {

        @Test
        @DisplayName("Should count participant meetings correctly")
        void shouldCountParticipantMeetings() {
            // Given
            Meeting otherUserMeeting = createMeeting(3L, "Other User Meeting", otherUser,
                    LocalDateTime.now().plusDays(2), MeetingStatus.PLANNED);

            MeetingParticipant participant = MeetingParticipant.builder()
                    .id(1L)
                    .user(testUser)
                    .meeting(otherUserMeeting)
                    .status(ParticipationStatus.CONFIRMED)
                    .build();

            when(meetingRepository.findByOrganizerId(1L)).thenReturn(List.of());
            when(participantRepository.findByUserId(1L)).thenReturn(List.of(participant));
            when(participantRepository.countByMeetingOrganizerId(1L)).thenReturn(0L);

            // When
            DashboardStatsResponse result = dashboardService.getUserDashboardStats(1L);

            // Then
            assertThat(result.getTotalMeetings()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should filter out own meetings from participants")
        void shouldFilterOutOwnMeetingsFromParticipants() {
            // Given
            Meeting ownMeeting = createMeeting(1L, "Own Meeting", testUser,
                    LocalDateTime.now().plusDays(1), MeetingStatus.PLANNED);

            Meeting otherUserMeeting = createMeeting(3L, "Other User Meeting", otherUser,
                    LocalDateTime.now().plusDays(2), MeetingStatus.PLANNED);

            MeetingParticipant participant1 = MeetingParticipant.builder()
                    .id(1L)
                    .user(testUser)
                    .meeting(otherUserMeeting)
                    .status(ParticipationStatus.CONFIRMED)
                    .build();

            MeetingParticipant participant2 = MeetingParticipant.builder()
                    .id(2L)
                    .user(testUser)
                    .meeting(ownMeeting) // własne spotkanie
                    .status(ParticipationStatus.CONFIRMED)
                    .build();

            when(meetingRepository.findByOrganizerId(1L)).thenReturn(List.of(ownMeeting));
            when(participantRepository.findByUserId(1L)).thenReturn(Arrays.asList(participant1, participant2));
            when(participantRepository.countByMeetingOrganizerId(1L)).thenReturn(3L);

            // When
            DashboardStatsResponse result = dashboardService.getUserDashboardStats(1L);

            // Then
            assertThat(result.getTotalMeetings()).isEqualTo(2); // 1 zorganizowane + 1 uczestnictwo
        }

        @Test
        @DisplayName("Should filter only confirmed participants")
        void shouldFilterOnlyConfirmedParticipants() {
            // Given
            Meeting otherUserMeeting = createMeeting(3L, "Other User Meeting", otherUser,
                    LocalDateTime.now().plusDays(2), MeetingStatus.PLANNED);

            MeetingParticipant pendingParticipant = MeetingParticipant.builder()
                    .id(3L)
                    .user(testUser)
                    .meeting(otherUserMeeting)
                    .status(ParticipationStatus.PENDING) // niepotwierdzone
                    .build();

            when(meetingRepository.findByOrganizerId(1L)).thenReturn(List.of());
            when(participantRepository.findByUserId(1L)).thenReturn(List.of(pendingParticipant));
            when(participantRepository.countByMeetingOrganizerId(1L)).thenReturn(0L);

            // When
            DashboardStatsResponse result = dashboardService.getUserDashboardStats(1L);

            // Then
            assertThat(result.getTotalMeetings()).isZero();
        }
    }

    @Nested
    @DisplayName("Upcoming meetings statistics")
    class UpcomingMeetingsTests {

        @Test
        @DisplayName("Should count upcoming meetings correctly")
        void shouldCountUpcomingMeetings() {
            // Given
            Meeting futureMeeting = createMeeting(1L, "Future Meeting", testUser,
                    LocalDateTime.now().plusDays(1), MeetingStatus.PLANNED);

            Meeting pastMeeting = createMeeting(2L, "Past Meeting", testUser,
                    LocalDateTime.now().minusDays(1), MeetingStatus.COMPLETED);

            Meeting otherUserMeeting = createMeeting(3L, "Other User Meeting", otherUser,
                    LocalDateTime.now().plusDays(2), MeetingStatus.PLANNED);

            MeetingParticipant participant = MeetingParticipant.builder()
                    .id(1L)
                    .user(testUser)
                    .meeting(otherUserMeeting)
                    .status(ParticipationStatus.CONFIRMED)
                    .build();

            when(meetingRepository.findByOrganizerId(1L)).thenReturn(Arrays.asList(futureMeeting, pastMeeting));
            when(participantRepository.findByUserId(1L)).thenReturn(List.of(participant));
            when(participantRepository.countByMeetingOrganizerId(1L)).thenReturn(4L);

            // When
            DashboardStatsResponse result = dashboardService.getUserDashboardStats(1L);

            // Then
            assertThat(result.getUpcomingMeetings()).isEqualTo(2); // futureMeeting + otherUserMeeting
        }

        @Test
        @DisplayName("Should handle null dates in upcoming count")
        void shouldHandleNullDatesInUpcomingCount() {
            // Given
            Meeting meetingWithoutDate = Meeting.builder()
                    .title("No Date Meeting")
                    .organizer(testUser)
                    .startDate(null)
                    .endDate(null)
                    .build();

            when(meetingRepository.findByOrganizerId(1L)).thenReturn(List.of(meetingWithoutDate));
            when(participantRepository.findByUserId(1L)).thenReturn(List.of());
            when(participantRepository.countByMeetingOrganizerId(1L)).thenReturn(0L);

            // When
            DashboardStatsResponse result = dashboardService.getUserDashboardStats(1L);

            // Then
            assertThat(result.getUpcomingMeetings()).isZero();
        }
    }

    @Nested
    @DisplayName("Time period statistics")
    class TimePeriodTests {

        @Test
        @DisplayName("Should count meetings today correctly")
        void shouldCountMeetingsToday() {
            // Given
            Meeting meetingToday = createMeeting(5L, "Today's Meeting", testUser,
                    LocalDateTime.now().minusHours(2), LocalDateTime.now().plusHours(1),
                    MeetingStatus.ONGOING);

            when(meetingRepository.findByOrganizerId(1L)).thenReturn(List.of(meetingToday));
            when(participantRepository.findByUserId(1L)).thenReturn(List.of());
            when(participantRepository.countByMeetingOrganizerId(1L)).thenReturn(0L);

            // When
            DashboardStatsResponse result = dashboardService.getUserDashboardStats(1L);

            // Then
            assertThat(result.getMeetingsToday()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should count meetings this week correctly")
        void shouldCountMeetingsThisWeek() {
            // Given
            Meeting meetingThisWeek = createMeeting(6L, "This Week Meeting", testUser,
                    LocalDateTime.now().minusDays(3), LocalDateTime.now().minusDays(3).plusHours(1),
                    MeetingStatus.COMPLETED);

            when(meetingRepository.findByOrganizerId(1L)).thenReturn(List.of(meetingThisWeek));
            when(participantRepository.findByUserId(1L)).thenReturn(List.of());
            when(participantRepository.countByMeetingOrganizerId(1L)).thenReturn(0L);

            // When
            DashboardStatsResponse result = dashboardService.getUserDashboardStats(1L);

            // Then
            assertThat(result.getMeetingsThisWeek()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should count meetings this month correctly")
        void shouldCountMeetingsThisMonth() {
            // Given
            Meeting meetingThisMonth = createMeeting(7L, "This Month Meeting", testUser,
                    LocalDateTime.now().minusDays(15), LocalDateTime.now().minusDays(15).plusHours(1),
                    MeetingStatus.COMPLETED);

            when(meetingRepository.findByOrganizerId(1L)).thenReturn(List.of(meetingThisMonth));
            when(participantRepository.findByUserId(1L)).thenReturn(List.of());
            when(participantRepository.countByMeetingOrganizerId(1L)).thenReturn(0L);

            // When
            DashboardStatsResponse result = dashboardService.getUserDashboardStats(1L);

            // Then
            assertThat(result.getMeetingsThisMonth()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should handle meetings spanning period boundaries")
        void shouldHandleMeetingsSpanningPeriodBoundaries() {
            // Given
            Meeting spanningMeeting = createMeeting(8L, "Spanning Meeting", testUser,
                    LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                    MeetingStatus.ONGOING);

            when(meetingRepository.findByOrganizerId(1L)).thenReturn(List.of(spanningMeeting));
            when(participantRepository.findByUserId(1L)).thenReturn(List.of());
            when(participantRepository.countByMeetingOrganizerId(1L)).thenReturn(0L);

            // When
            DashboardStatsResponse result = dashboardService.getUserDashboardStats(1L);

            // Then
            assertAll(
                    () -> assertThat(result.getMeetingsToday()).isEqualTo(1),
                    () -> assertThat(result.getMeetingsThisWeek()).isEqualTo(1),
                    () -> assertThat(result.getMeetingsThisMonth()).isEqualTo(1)
            );
        }
    }


    @Test
    @DisplayName("Should combine organized and participant meetings in total count")
    void shouldCombineOrganizedAndParticipantMeetings() {
        // Given
        Meeting meeting1 = createMeeting(1L, "Meeting 1", testUser,
                LocalDateTime.now().plusDays(1), MeetingStatus.PLANNED);

        Meeting meeting2 = createMeeting(2L, "Meeting 2", testUser,
                LocalDateTime.now().minusDays(1), MeetingStatus.COMPLETED);

        Meeting otherUserMeeting = createMeeting(3L, "Other User Meeting", otherUser,
                LocalDateTime.now().plusDays(2), MeetingStatus.PLANNED);

        MeetingParticipant participant = MeetingParticipant.builder()
                .id(1L)
                .user(testUser)
                .meeting(otherUserMeeting)
                .status(ParticipationStatus.CONFIRMED)
                .build();

        when(meetingRepository.findByOrganizerId(1L)).thenReturn(Arrays.asList(meeting1, meeting2));
        when(participantRepository.findByUserId(1L)).thenReturn(List.of(participant));
        when(participantRepository.countByMeetingOrganizerId(1L)).thenReturn(3L);

        // When
        DashboardStatsResponse result = dashboardService.getUserDashboardStats(1L);

        // Then
        assertThat(result.getTotalMeetings()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should calculate all time periods correctly for multiple meetings")
    void shouldCalculateAllTimePeriodsCorrectly() {
        // Given
        Meeting todayMeeting = createMeeting(12L, "Today", testUser,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1),
                MeetingStatus.ONGOING);

        Meeting weekMeeting = createMeeting(13L, "This Week", testUser,
                LocalDateTime.now().minusDays(4), LocalDateTime.now().minusDays(4).plusHours(2),
                MeetingStatus.COMPLETED);

        Meeting monthMeeting = createMeeting(14L, "This Month", testUser,
                LocalDateTime.now().minusDays(20), LocalDateTime.now().minusDays(20).plusHours(1),
                MeetingStatus.COMPLETED);

        List<Meeting> allMeetings = Arrays.asList(todayMeeting, weekMeeting, monthMeeting);

        when(meetingRepository.findByOrganizerId(1L)).thenReturn(allMeetings);
        when(participantRepository.findByUserId(1L)).thenReturn(List.of());
        when(participantRepository.countByMeetingOrganizerId(1L)).thenReturn(6L);

        // When
        DashboardStatsResponse result = dashboardService.getUserDashboardStats(1L);

        // Then
        assertAll(
                () -> assertThat(result.getMeetingsToday()).isEqualTo(1),
                () -> assertThat(result.getMeetingsThisWeek()).isEqualTo(2),
                () -> assertThat(result.getMeetingsThisMonth()).isEqualTo(3)
        );
    }

    // Helper methods
    private Meeting createMeeting(Long id, String title, User organizer,
                                  LocalDateTime startDate, MeetingStatus status) {
        return createMeeting(id, title, organizer, startDate, startDate.plusHours(2), status);
    }

    private Meeting createMeeting(Long id, String title, User organizer,
                                  LocalDateTime startDate, LocalDateTime endDate, MeetingStatus status) {
        return Meeting.builder()
                .title(title)
                .organizer(organizer)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }
}
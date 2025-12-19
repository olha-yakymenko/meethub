package com.meethub.domain.model.entity;

import com.meethub.domain.model.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingTest {

    private Meeting meeting;
    private User organizer;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @BeforeEach
    void setUp() {
        organizer = mock(User.class);
        startDate = LocalDateTime.now().plusDays(1);
        endDate = startDate.plusHours(2);

        meeting = Meeting.builder()
                .title("Test Meeting")
                .description("Test Description")
                .type(MeetingType.PHYSICAL)
                .visibility(MeetingVisibility.PUBLIC)
                .startDate(startDate)
                .endDate(endDate)
                .organizer(organizer)
                .maxParticipants(10)
                .build();
    }

    @Test
    void shouldCreateMeetingWithBuilder() {
        // Then
        assertAll(
                () -> assertThat(meeting.getTitle()).isEqualTo("Test Meeting"),
                () -> assertThat(meeting.getDescription()).isEqualTo("Test Description"),
                () -> assertThat(meeting.getType()).isEqualTo(MeetingType.PHYSICAL),
                () -> assertThat(meeting.getVisibility()).isEqualTo(MeetingVisibility.PUBLIC),
                () -> assertThat(meeting.getStartDate()).isEqualTo(startDate),
                () -> assertThat(meeting.getEndDate()).isEqualTo(endDate),
                () -> assertThat(meeting.getOrganizer()).isEqualTo(organizer),
                () -> assertThat(meeting.getMaxParticipants()).isEqualTo(10),
                () -> assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.PLANNED)
        );
    }

    @Test
    void shouldAddAndRemoveParticipants() {
        // Given
        com.meethub.domain.model.entity.MeetingParticipant participant = mock(com.meethub.domain.model.entity.MeetingParticipant.class);

        // When
        meeting.addParticipant(participant);

        // Then
        assertAll(
                () -> assertThat(meeting.getParticipants()).contains(participant)
        );

        // When
        meeting.removeParticipant(participant);

        // Then
        assertAll(
                () -> assertThat(meeting.getParticipants()).doesNotContain(participant)
        );
    }

    @Test
    void shouldAddAndRemoveTags() {
        // When
        meeting.addTag("important");
        meeting.addTag("urgent");

        // Then
        assertAll(
                () -> assertThat(meeting.getTags()).contains("important", "urgent")
        );

        // When
        meeting.removeTag("important");

        // Then
        assertAll(
                () -> assertThat(meeting.getTags()).doesNotContain("important"),
                () -> assertThat(meeting.getTags()).contains("urgent")
        );
    }

    @Test
    void shouldDetermineMeetingState() {
        // Given - upcoming meeting
        Meeting upcoming = createMeetingAt(LocalDateTime.now().plusHours(2), 3);

        // Given - ongoing meeting
        Meeting ongoing = createMeetingAt(LocalDateTime.now().minusMinutes(30), 1);

        // Given - past meeting
        Meeting past = createMeetingAt(LocalDateTime.now().minusDays(1), 2);

        // Then
        assertAll(
                () -> assertThat(upcoming.isUpcoming()).isTrue(),
                () -> assertThat(upcoming.isOngoing()).isFalse(),
                () -> assertThat(upcoming.isPast()).isFalse(),

                () -> assertThat(ongoing.isOngoing()).isTrue(),
                () -> assertThat(ongoing.isUpcoming()).isFalse(),
                () -> assertThat(ongoing.isPast()).isFalse(),

                () -> assertThat(past.isPast()).isTrue(),
                () -> assertThat(past.isUpcoming()).isFalse(),
                () -> assertThat(past.isOngoing()).isFalse()
        );
    }

    @Test
    void shouldCheckAvailableSpots() {
        // Given
        Meeting meetingWithLimit = Meeting.builder()
                .maxParticipants(2)
                .build();

        // When & Then - empty participants
        assertThat(meetingWithLimit.hasAvailableSpots()).isTrue();

        // When - add one confirmed participant
        com.meethub.domain.model.entity.MeetingParticipant participant1 = mock(com.meethub.domain.model.entity.MeetingParticipant.class);
        when(participant1.getStatus()).thenReturn(ParticipationStatus.CONFIRMED);
        meetingWithLimit.addParticipant(participant1);

        // Then
        assertAll(
                () -> assertThat(meetingWithLimit.hasAvailableSpots()).isTrue()
        );

        // When - add second confirmed participant
        com.meethub.domain.model.entity.MeetingParticipant participant2 = mock(com.meethub.domain.model.entity.MeetingParticipant.class);
        when(participant2.getStatus()).thenReturn(ParticipationStatus.CONFIRMED);
        meetingWithLimit.addParticipant(participant2);

        // Then
        assertAll(
                () -> assertThat(meetingWithLimit.hasAvailableSpots()).isFalse()
        );
    }

    @Test
    void shouldCountConfirmedParticipants() {
        // Given
        Meeting meeting = new Meeting();

        // When & Then - no participants
        assertThat(meeting.getConfirmedParticipantsCount()).isEqualTo(0);

        // When - add mixed participants
        com.meethub.domain.model.entity.MeetingParticipant confirmed1 = mock(com.meethub.domain.model.entity.MeetingParticipant.class);
        when(confirmed1.getStatus()).thenReturn(ParticipationStatus.CONFIRMED);

        com.meethub.domain.model.entity.MeetingParticipant pending = mock(com.meethub.domain.model.entity.MeetingParticipant.class);
        when(pending.getStatus()).thenReturn(ParticipationStatus.PENDING);

        com.meethub.domain.model.entity.MeetingParticipant confirmed2 = mock(com.meethub.domain.model.entity.MeetingParticipant.class);
        when(confirmed2.getStatus()).thenReturn(ParticipationStatus.CONFIRMED);

        meeting.addParticipant(confirmed1);
        meeting.addParticipant(pending);
        meeting.addParticipant(confirmed2);

        // Then
        assertAll(
                () -> assertThat(meeting.getConfirmedParticipantsCount()).isEqualTo(2)
        );
    }

    @Test
    void shouldCheckIfStartingSoon() {
        // Given
        Meeting meeting = new Meeting();
        LocalDateTime futureStart = LocalDateTime.now().plusMinutes(15);
        meeting.setStartDate(futureStart);

        // When & Then
        assertAll(
                () -> assertThat(meeting.isStartingSoon(30)).isTrue(),
                () -> assertThat(meeting.isStartingSoon(10)).isFalse()
        );
    }

    @Test
    void shouldCheckIfVirtual() {
        // Given
        Location virtualLocation = Location.builder()
                .type(LocationType.VIRTUAL)
                .virtualMeetingUrl("https://meet.google.com/abc")
                .build();

        Meeting meeting = new Meeting();
        meeting.setLocation(virtualLocation);

        // When & Then
        assertAll(
                () -> assertThat(meeting.isVirtual()).isTrue(),
                () -> assertThat(meeting.getVirtualMeetingUrl()).isEqualTo("https://meet.google.com/abc")
        );
    }

    @Test
    void shouldCheckRecurringMeeting() {
        // Given
        Meeting meeting = new Meeting();

        // When
        meeting.setRecurring(true);
        meeting.setRecurrencePattern("WEEKLY:2");
        meeting.setRecurrenceEndDate(LocalDateTime.now().plusMonths(3));

        // Then
        assertAll(
                () -> assertThat(meeting.isRecurring()).isTrue(),
                () -> assertThat(meeting.getRecurrencePattern()).isEqualTo("WEEKLY:2"),
                () -> assertThat(meeting.getRecurrenceEndDate()).isAfter(LocalDateTime.now())
        );
    }

    @Test
    void shouldSetMeetingAsTemplate() {
        // Given
        Meeting meeting = new Meeting();

        // When
        meeting.setTemplate(true);
        meeting.setOriginalMeetingId(123L);

        // Then
        assertAll(
                () -> assertThat(meeting.isTemplate()).isTrue(),
                () -> assertThat(meeting.getOriginalMeetingId()).isEqualTo(123L)
        );
    }

    @Test
    void shouldAddCategories() {
        // Given
        Meeting meeting = new Meeting();
        Category category1 = new Category();

        Set<Category> categories = new HashSet<>();
        categories.add(category1);

        // When
        meeting.setCategories(categories);

        // Then
        assertAll(
                () -> assertThat(meeting.getCategories()).hasSize(1),
                () -> assertThat(meeting.getCategories()).contains(category1)
        );
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        // Given
        Meeting meeting1 = new Meeting();
        meeting1.setId(1L);
        meeting1.setTitle("Meeting 1");

        Meeting meeting2 = new Meeting();
        meeting2.setId(1L);
        meeting2.setTitle("Meeting 1");

        Meeting meeting3 = new Meeting();
        meeting3.setId(2L);
        meeting3.setTitle("Meeting 2");

        // Then
        assertAll(
                () -> assertThat(meeting1).isEqualTo(meeting2),
                () -> assertThat(meeting1).isNotEqualTo(meeting3),
                () -> assertThat(meeting1.hashCode()).isEqualTo(meeting2.hashCode()),
                () -> assertThat(meeting1.hashCode()).isNotEqualTo(meeting3.hashCode())
        );
    }

    private Meeting createMeetingAt(LocalDateTime start, int durationHours) {
        return Meeting.builder()
                .title("Test")
                .type(MeetingType.PHYSICAL)
                .visibility(MeetingVisibility.PUBLIC)
                .startDate(start)
                .endDate(start.plusHours(durationHours))
                .organizer(mock(User.class))
                .build();
    }

    // Mock MeetingParticipantStatus enum since it's not provided
    private enum MeetingParticipantStatus {
        CONFIRMED, PENDING, DECLINED
    }

    // Mock MeetingParticipant class
    private static class MeetingParticipant {
        private MeetingParticipantStatus status;

        public MeetingParticipantStatus getStatus() {
            return status;
        }

        public void setStatus(MeetingParticipantStatus status) {
            this.status = status;
        }
    }
}
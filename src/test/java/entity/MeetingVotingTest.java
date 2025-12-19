package com.meethub.domain.model.entity;

import com.meethub.domain.model.enums.VotingStatus;
import com.meethub.domain.model.enums.VotingType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class MeetingVotingTest {

    @Test
    void shouldCreateMeetingVotingWithBuilder() {
        // Given
        Meeting meeting = mock(Meeting.class);
        LocalDateTime deadline = LocalDateTime.now().plusDays(1);

        // When
        MeetingVoting voting = MeetingVoting.builder()
                .meeting(meeting)
                .title("Best Meeting Time")
                .description("Vote for the best meeting time")
                .status(VotingStatus.ACTIVE)
                .type(VotingType.SINGLE_CHOICE)
                .maxChoices(1)
                .allowSuggestions(true)
                .deadlineDate(deadline)
                .autoClose(true)
                .build();

        // Then
        assertAll(
                () -> assertThat(voting.getMeeting()).isEqualTo(meeting),
                () -> assertThat(voting.getTitle()).isEqualTo("Best Meeting Time"),
                () -> assertThat(voting.getDescription()).isEqualTo("Vote for the best meeting time"),
                () -> assertThat(voting.getStatus()).isEqualTo(VotingStatus.ACTIVE),
                () -> assertThat(voting.getType()).isEqualTo(VotingType.SINGLE_CHOICE),
                () -> assertThat(voting.getMaxChoices()).isEqualTo(1),
                () -> assertThat(voting.getAllowSuggestions()).isTrue(),
                () -> assertThat(voting.getDeadlineDate()).isEqualTo(deadline),
                () -> assertThat(voting.getAutoClose()).isTrue(),
                () -> assertThat(voting.getOptions()).isEmpty(),
                () -> assertThat(voting.getVotes()).isEmpty()
        );
    }

    @Test
    void shouldSetTimestampsOnCreate() {
        // Given
        MeetingVoting voting = new MeetingVoting();
        voting.setMeeting(mock(Meeting.class));
        voting.setTitle("Test Voting");
        voting.setStatus(VotingStatus.PENDING);
        voting.setType(VotingType.MULTIPLE_CHOICE);

        // When
        voting.onCreate();

        // Then
        assertAll(
                () -> assertThat(voting.getCreatedAt()).isNotNull(),
                () -> assertThat(voting.getUpdatedAt()).isNotNull()
        );
    }

    @Test
    void shouldUpdateTimestampOnUpdate() {
        // Given
        MeetingVoting voting = new MeetingVoting();
        voting.onCreate();
        LocalDateTime initialUpdateTime = voting.getUpdatedAt();

        // When
        voting.onUpdate();

        // Then
        assertAll(
                () -> assertThat(voting.getUpdatedAt()).isAfter(initialUpdateTime)
        );
    }

    @Test
    void shouldAddOptions() {
        // Given
        MeetingVoting voting = MeetingVoting.builder().build();
        VotingOption option1 = mock(VotingOption.class);
        VotingOption option2 = mock(VotingOption.class);

        // When
        voting.getOptions().add(option1);
        voting.getOptions().add(option2);

        // Then
        assertAll(
                () -> assertThat(voting.getOptions()).hasSize(2),
                () -> assertThat(voting.getOptions()).contains(option1, option2)
        );
    }

    @Test
    void shouldAddVotes() {
        // Given
        MeetingVoting voting = MeetingVoting.builder().build();
        Vote vote1 = mock(Vote.class);
        Vote vote2 = mock(Vote.class);

        // When
        voting.getVotes().add(vote1);
        voting.getVotes().add(vote2);

        // Then
        assertAll(
                () -> assertThat(voting.getVotes()).hasSize(2),
                () -> assertThat(voting.getVotes()).contains(vote1, vote2)
        );
    }

    @Test
    void shouldUpdateAllFields() {
        // Given
        MeetingVoting voting = new MeetingVoting();
        Meeting newMeeting = mock(Meeting.class);
        LocalDateTime newDeadline = LocalDateTime.now().plusDays(2);

        // When
        voting.setId(1L);
        voting.setMeeting(newMeeting);
        voting.setTitle("Updated Title");
        voting.setDescription("Updated Description");
        voting.setStatus(VotingStatus.CLOSED);
        voting.setType(VotingType.PREFERENCE);
        voting.setMaxChoices(3);
        voting.setAllowSuggestions(false);
        voting.setDeadlineDate(newDeadline);
        voting.setAutoClose(false);

        // Then
        assertAll(
                () -> assertThat(voting.getId()).isEqualTo(1L),
                () -> assertThat(voting.getMeeting()).isEqualTo(newMeeting),
                () -> assertThat(voting.getTitle()).isEqualTo("Updated Title"),
                () -> assertThat(voting.getDescription()).isEqualTo("Updated Description"),
                () -> assertThat(voting.getStatus()).isEqualTo(VotingStatus.CLOSED),
                () -> assertThat(voting.getType()).isEqualTo(VotingType.PREFERENCE),
                () -> assertThat(voting.getMaxChoices()).isEqualTo(3),
                () -> assertThat(voting.getAllowSuggestions()).isFalse(),
                () -> assertThat(voting.getDeadlineDate()).isEqualTo(newDeadline),
                () -> assertThat(voting.getAutoClose()).isFalse()
        );
    }

    @Test
    void shouldHandleNullValues() {
        // Given
        MeetingVoting voting = MeetingVoting.builder()
                .meeting(mock(Meeting.class))
                .title("Test")
                .status(VotingStatus.PENDING)
                .type(VotingType.SINGLE_CHOICE)
                .build();

        // Then
        assertAll(
                () -> assertThat(voting.getMaxChoices()).isNull(),
                () -> assertThat(voting.getAllowSuggestions()).isNull(),
                () -> assertThat(voting.getDeadlineDate()).isNull(),
                () -> assertThat(voting.getAutoClose()).isNull(),
                () -> assertThat(voting.getOptions()).isNotNull(),
                () -> assertThat(voting.getVotes()).isNotNull()
        );
    }
}
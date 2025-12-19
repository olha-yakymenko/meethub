package com.meethub.domain.model.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class VoteTest {

    @Test
    void shouldCreateVoteWithBuilder() {
        // Given
        MeetingVoting voting = mock(MeetingVoting.class);
        VotingOption option = mock(VotingOption.class);
        User user = mock(User.class);
        LocalDateTime votedAt = LocalDateTime.now();

        // When
        Vote vote = Vote.builder()
                .voting(voting)
                .option(option)
                .user(user)
                .voteWeight(1)
                .preferenceOrder(1)
                .votedAt(votedAt)
                .build();

        // Then
        assertAll(
                () -> assertThat(vote.getVoting()).isEqualTo(voting),
                () -> assertThat(vote.getOption()).isEqualTo(option),
                () -> assertThat(vote.getUser()).isEqualTo(user),
                () -> assertThat(vote.getVoteWeight()).isEqualTo(1),
                () -> assertThat(vote.getPreferenceOrder()).isEqualTo(1),
                () -> assertThat(vote.getVotedAt()).isEqualTo(votedAt)
        );
    }

    @Test
    void shouldSetVotedAtOnCreate() {
        // Given
        Vote vote = new Vote();
        vote.setVoting(mock(MeetingVoting.class));
        vote.setOption(mock(VotingOption.class));
        vote.setUser(mock(User.class));

        // When
        vote.onCreate();

        // Then
        assertAll(
                () -> assertThat(vote.getVotedAt()).isNotNull(),
                () -> assertThat(vote.getVotedAt()).isBeforeOrEqualTo(LocalDateTime.now())
        );
    }

    @Test
    void shouldUpdateAllFields() {
        // Given
        Vote vote = new Vote();
        MeetingVoting newVoting = mock(MeetingVoting.class);
        VotingOption newOption = mock(VotingOption.class);
        User newUser = mock(User.class);
        LocalDateTime newVotedAt = LocalDateTime.now().minusHours(1);

        // When
        vote.setId(1L);
        vote.setVoting(newVoting);
        vote.setOption(newOption);
        vote.setUser(newUser);
        vote.setVoteWeight(2);
        vote.setPreferenceOrder(3);
        vote.setVotedAt(newVotedAt);

        // Then
        assertAll(
                () -> assertThat(vote.getId()).isEqualTo(1L),
                () -> assertThat(vote.getVoting()).isEqualTo(newVoting),
                () -> assertThat(vote.getOption()).isEqualTo(newOption),
                () -> assertThat(vote.getUser()).isEqualTo(newUser),
                () -> assertThat(vote.getVoteWeight()).isEqualTo(2),
                () -> assertThat(vote.getPreferenceOrder()).isEqualTo(3),
                () -> assertThat(vote.getVotedAt()).isEqualTo(newVotedAt)
        );
    }

    @Test
    void shouldHandleNullValues() {
        // When
        Vote vote = Vote.builder()
                .voting(mock(MeetingVoting.class))
                .option(mock(VotingOption.class))
                .user(mock(User.class))
                .build();

        // Then
        assertAll(
                () -> assertThat(vote.getVoteWeight()).isNull(),
                () -> assertThat(vote.getPreferenceOrder()).isNull(),
                () -> assertThat(vote.getVotedAt()).isNull()
        );
    }

    @Test
    void shouldHandleDifferentVoteWeights() {
        // Given
        Vote singleVote = Vote.builder()
                .voteWeight(1)
                .build();

        Vote weightedVote = Vote.builder()
                .voteWeight(2)
                .build();

        Vote multipleVotes = Vote.builder()
                .voteWeight(5)
                .build();

        // Then
        assertAll(
                () -> assertThat(singleVote.getVoteWeight()).isEqualTo(1),
                () -> assertThat(weightedVote.getVoteWeight()).isEqualTo(2),
                () -> assertThat(multipleVotes.getVoteWeight()).isEqualTo(5)
        );
    }

    @Test
    void shouldHandlePreferenceOrder() {
        // Given
        Vote firstChoice = Vote.builder()
                .preferenceOrder(1)
                .build();

        Vote secondChoice = Vote.builder()
                .preferenceOrder(2)
                .build();

        Vote thirdChoice = Vote.builder()
                .preferenceOrder(3)
                .build();

        // Then
        assertAll(
                () -> assertThat(firstChoice.getPreferenceOrder()).isEqualTo(1),
                () -> assertThat(secondChoice.getPreferenceOrder()).isEqualTo(2),
                () -> assertThat(thirdChoice.getPreferenceOrder()).isEqualTo(3)
        );
    }

    @Test
    void shouldAutomaticallySetVotedAtOnPersist() {
        // Given
        Vote vote = new Vote();
        vote.setVoting(mock(MeetingVoting.class));
        vote.setOption(mock(VotingOption.class));
        vote.setUser(mock(User.class));

        // Simulate @PrePersist
        vote.onCreate();

        // Then
        assertAll(
                () -> assertThat(vote.getVotedAt()).isNotNull(),
                () -> assertThat(vote.getVotedAt()).isBeforeOrEqualTo(LocalDateTime.now())
        );
    }
}
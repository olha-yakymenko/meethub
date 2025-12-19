package com.meethub.domain.model.entity;

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
class VotingOptionTest {

    @Test
    void shouldCreateVotingOptionWithBuilder() {
        // Given
        MeetingVoting voting = mock(MeetingVoting.class);
        LocalDateTime optionDate = LocalDateTime.now().plusDays(1);

        // When
        VotingOption option = VotingOption.builder()
                .voting(voting)
                .optionDate(optionDate)
                .durationMinutes(60)
                .isSuggested(false)
                .suggestedBy(null)
                .build();

        // Then
        assertAll(
                () -> assertThat(option.getVoting()).isEqualTo(voting),
                () -> assertThat(option.getOptionDate()).isEqualTo(optionDate),
                () -> assertThat(option.getDurationMinutes()).isEqualTo(60),
                () -> assertThat(option.getIsSuggested()).isFalse(),
                () -> assertThat(option.getSuggestedBy()).isNull(),
                () -> assertThat(option.getVotes()).isEmpty()
        );
    }

    @Test
    void shouldCreateSuggestedOption() {
        // Given
        MeetingVoting voting = mock(MeetingVoting.class);
        LocalDateTime optionDate = LocalDateTime.now().plusDays(2);
        Long suggestedByUserId = 123L;

        // When
        VotingOption option = VotingOption.builder()
                .voting(voting)
                .optionDate(optionDate)
                .durationMinutes(90)
                .isSuggested(true)
                .suggestedBy(suggestedByUserId)
                .build();

        // Then
        assertAll(
                () -> assertThat(option.getVoting()).isEqualTo(voting),
                () -> assertThat(option.getOptionDate()).isEqualTo(optionDate),
                () -> assertThat(option.getDurationMinutes()).isEqualTo(90),
                () -> assertThat(option.getIsSuggested()).isTrue(),
                () -> assertThat(option.getSuggestedBy()).isEqualTo(suggestedByUserId)
        );
    }

    @Test
    void shouldUpdateAllFields() {
        // Given
        VotingOption option = new VotingOption();
        MeetingVoting newVoting = mock(MeetingVoting.class);
        LocalDateTime newOptionDate = LocalDateTime.now().plusDays(3);

        // When
        option.setId(1L);
        option.setVoting(newVoting);
        option.setOptionDate(newOptionDate);
        option.setDurationMinutes(120);
        option.setIsSuggested(true);
        option.setSuggestedBy(456L);

        // Then
        assertAll(
                () -> assertThat(option.getId()).isEqualTo(1L),
                () -> assertThat(option.getVoting()).isEqualTo(newVoting),
                () -> assertThat(option.getOptionDate()).isEqualTo(newOptionDate),
                () -> assertThat(option.getDurationMinutes()).isEqualTo(120),
                () -> assertThat(option.getIsSuggested()).isTrue(),
                () -> assertThat(option.getSuggestedBy()).isEqualTo(456L)
        );
    }

    @Test
    void shouldAddVotes() {
        // Given
        VotingOption option = new VotingOption();
        Vote vote1 = mock(Vote.class);
        Vote vote2 = mock(Vote.class);

        // When
        option.getVotes().add(vote1);
        option.getVotes().add(vote2);

        // Then
        assertAll(
                () -> assertThat(option.getVotes()).hasSize(2),
                () -> assertThat(option.getVotes()).contains(vote1, vote2)
        );
    }

    @Test
    void shouldHandleNullValues() {
        // When
        VotingOption option = VotingOption.builder()
                .voting(mock(MeetingVoting.class))
                .optionDate(LocalDateTime.now())
                .build();

        // Then
        assertAll(
                () -> assertThat(option.getDurationMinutes()).isNull(),
                () -> assertThat(option.getIsSuggested()).isNull(),
                () -> assertThat(option.getSuggestedBy()).isNull(),
                () -> assertThat(option.getVotes()).isNotNull(),
                () -> assertThat(option.getVotes()).isEmpty()
        );
    }

    @Test
    void shouldInitializeVotesListByDefault() {
        // When
        VotingOption option1 = new VotingOption();
        VotingOption option2 = VotingOption.builder().build();

        // Then
        assertAll(
                () -> assertThat(option1.getVotes()).isNotNull(),
                () -> assertThat(option1.getVotes()).isEmpty(),
                () -> assertThat(option2.getVotes()).isNotNull(),
                () -> assertThat(option2.getVotes()).isEmpty()
        );
    }

    @Test
    void shouldHandleDifferentDurations() {
        // Given
        VotingOption shortOption = VotingOption.builder()
                .durationMinutes(30)
                .build();

        VotingOption longOption = VotingOption.builder()
                .durationMinutes(180)
                .build();

        VotingOption allDayOption = VotingOption.builder()
                .durationMinutes(1440) // 24 hours
                .build();

        // Then
        assertAll(
                () -> assertThat(shortOption.getDurationMinutes()).isEqualTo(30),
                () -> assertThat(longOption.getDurationMinutes()).isEqualTo(180),
                () -> assertThat(allDayOption.getDurationMinutes()).isEqualTo(1440)
        );
    }
}
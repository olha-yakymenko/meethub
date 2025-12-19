package com.meethub.domain.model.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class FeedbackTest {

    @Test
    void shouldCreateFeedbackWithBuilder() {
        // Given
        Meeting meeting = mock(Meeting.class);
        User user = mock(User.class);
        LocalDateTime now = LocalDateTime.now();

        // When
        Feedback feedback = Feedback.builder()
                .meeting(meeting)
                .user(user)
                .rating(5)
                .comment("Excellent meeting!")
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Then
        assertAll(
                () -> assertThat(feedback.getMeeting()).isEqualTo(meeting),
                () -> assertThat(feedback.getUser()).isEqualTo(user),
                () -> assertThat(feedback.getRating()).isEqualTo(5),
                () -> assertThat(feedback.getComment()).isEqualTo("Excellent meeting!"),
                () -> assertThat(feedback.getCreatedAt()).isEqualTo(now),
                () -> assertThat(feedback.getUpdatedAt()).isEqualTo(now)
        );
    }

    @Test
    void shouldSetTimestampsOnCreate() {
        // Given
        Feedback feedback = new Feedback();
        feedback.setMeeting(mock(Meeting.class));
        feedback.setUser(mock(User.class));
        feedback.setRating(4);

        // When
        feedback.onCreate();

        // Then
        assertAll(
                () -> assertThat(feedback.getCreatedAt()).isNotNull(),
                () -> assertThat(feedback.getUpdatedAt()).isNotNull()
                );
    }


    @Test
    void shouldUpdateFeedbackFields() {
        // Given
        Feedback feedback = new Feedback();

        Meeting meeting = mock(Meeting.class);
        User user = mock(User.class);
        LocalDateTime now = LocalDateTime.now();

        // When
        feedback.setId(1L);
        feedback.setMeeting(meeting);
        feedback.setUser(user);
        feedback.setRating(3);
        feedback.setComment("Average meeting");
        feedback.setCreatedAt(now);
        feedback.setUpdatedAt(now);

        // Then
        assertAll(
                () -> assertThat(feedback.getId()).isEqualTo(1L),
                () -> assertThat(feedback.getMeeting()).isEqualTo(meeting),
                () -> assertThat(feedback.getUser()).isEqualTo(user),
                () -> assertThat(feedback.getRating()).isEqualTo(3),
                () -> assertThat(feedback.getComment()).isEqualTo("Average meeting"),
                () -> assertThat(feedback.getCreatedAt()).isEqualTo(now),
                () -> assertThat(feedback.getUpdatedAt()).isEqualTo(now)
        );
    }
}
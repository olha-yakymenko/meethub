package com.meethub.domain.model.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class MeetingMarkTest {

    @Test
    void shouldCreateMeetingMarkWithDefaultValues() {
        // Given
        User user = mock(User.class);
        Meeting meeting = mock(Meeting.class);

        // When
        MeetingMark mark = new MeetingMark(user, meeting);

        // Then
        assertAll(
                () -> assertThat(mark.getUser()).isEqualTo(user),
                () -> assertThat(mark.getMeeting()).isEqualTo(meeting),
                () -> assertThat(mark.getIsImportant()).isTrue(),
                () -> assertThat(mark.getMarkedAt()).isNotNull()
        );
    }

    @Test
    void shouldUpdateMeetingMarkFields() {
        // Given
        MeetingMark mark = new MeetingMark();
        User user = mock(User.class);
        Meeting meeting = mock(Meeting.class);
        LocalDateTime markedAt = LocalDateTime.now();

        // When
        mark.setUser(user);
        mark.setMeeting(meeting);
        mark.setIsImportant(false);
        mark.setMarkedAt(markedAt);

        // Then
        assertAll(
                () -> assertThat(mark.getUser()).isEqualTo(user),
                () -> assertThat(mark.getMeeting()).isEqualTo(meeting),
                () -> assertThat(mark.getIsImportant()).isFalse(),
                () -> assertThat(mark.getMarkedAt()).isEqualTo(markedAt)
        );
    }

    @Test
    void shouldCreateMeetingMarkWithConstructor() {
        // Given
        User user = mock(User.class);
        Meeting meeting = mock(Meeting.class);

        // When
        MeetingMark mark = new MeetingMark(user, meeting);

        // Then
        assertAll(
                () -> assertThat(mark.getUser()).isEqualTo(user),
                () -> assertThat(mark.getMeeting()).isEqualTo(meeting),
                () -> assertThat(mark.getIsImportant()).isTrue()
        );
    }
}
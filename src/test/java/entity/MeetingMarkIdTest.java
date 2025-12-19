package com.meethub.domain.model.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@ExtendWith(MockitoExtension.class)
class MeetingMarkIdTest {

    @Test
    void shouldCreateMeetingMarkId() {
        // When
        MeetingMarkId id = new MeetingMarkId(1L, 2L);

        // Then
        assertAll(
                () -> assertThat(id.getUser()).isEqualTo(1L),
                () -> assertThat(id.getMeeting()).isEqualTo(2L)
        );
    }

    @Test
    void shouldUpdateFields() {
        // Given
        MeetingMarkId id = new MeetingMarkId();

        // When
        id.setUser(3L);
        id.setMeeting(4L);

        // Then
        assertAll(
                () -> assertThat(id.getUser()).isEqualTo(3L),
                () -> assertThat(id.getMeeting()).isEqualTo(4L)
        );
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        // Given
        MeetingMarkId id1 = new MeetingMarkId(1L, 2L);
        MeetingMarkId id2 = new MeetingMarkId(1L, 2L);
        MeetingMarkId id3 = new MeetingMarkId(3L, 4L);

        // Then
        assertAll(
                () -> assertThat(id1).isEqualTo(id2),
                () -> assertThat(id1).isNotEqualTo(id3),
                () -> assertThat(id1.hashCode()).isEqualTo(id2.hashCode()),
                () -> assertThat(id1.hashCode()).isNotEqualTo(id3.hashCode())
        );
    }

    @Test
    void shouldEqualItself() {
        // Given
        MeetingMarkId id = new MeetingMarkId(1L, 2L);

        // Then
        assertAll(
                () -> assertThat(id).isEqualTo(id)
        );
    }

    @Test
    void shouldNotEqualNullOrDifferentClass() {
        // Given
        MeetingMarkId id = new MeetingMarkId(1L, 2L);

        // Then
        assertAll(
                () -> assertThat(id).isNotEqualTo(null),
                () -> assertThat(id).isNotEqualTo("not a MeetingMarkId")
        );
    }
}
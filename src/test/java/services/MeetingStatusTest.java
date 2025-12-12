package com.meethub.domain.model.enums;

import com.meethub.domain.model.entity.Meeting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class MeetingStatusTest {

    @Test
    @DisplayName("MeetingStatus powinien mieć 6 wartości")
    void meetingStatus_ShouldHaveSixValues() {
        // When & Then
        assertThat(MeetingStatus.values()).hasSize(4);
    }

    @Test
    @DisplayName("MeetingStatus powinien zawierać wszystkie oczekiwane wartości")
    void meetingStatus_ShouldContainAllExpectedValues() {
        // When & Then
        assertThat(MeetingStatus.values())
                .containsExactly(
                        MeetingStatus.PLANNED,
                        MeetingStatus.CANCELLED,
                        MeetingStatus.COMPLETED,
                        MeetingStatus.ONGOING
                );
    }

    @ParameterizedTest
    @EnumSource(MeetingStatus.class)
    @DisplayName("Każdy MeetingStatus powinien mieć display name")
    void eachMeetingStatus_ShouldHaveDisplayName(MeetingStatus status) {
        // When & Then
        assertThat(status.getDisplayName()).isNotBlank();
    }

    @ParameterizedTest
    @CsvSource({
            "PLANNED, Planned",
            "CANCELLED, Cancelled",
            "COMPLETED, Completed",
            "ONGOING, Ongoing"
    })
    @DisplayName("MeetingStatus powinien mieć poprawne display name dla każdej wartości")
    void meetingStatus_ShouldHaveCorrectDisplayName(String enumName, String expectedDisplayName) {
        // Given
        MeetingStatus status = MeetingStatus.valueOf(enumName);

        // When & Then
        assertThat(status.getDisplayName()).isEqualTo(expectedDisplayName);
    }

    @Test
    @DisplayName("MeetingStatus.PLANNED powinien być domyślną wartością")
    void planned_ShouldBeDefaultValue() {
        // Given
        Meeting meeting = new Meeting();

        // When & Then
        assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.PLANNED);
    }
}
package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.ParticipantStatusHistory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@DataJpaTest
@ActiveProfiles("postgres")
class ParticipantStatusHistoryRepositoryTest {

    @Autowired
    private ParticipantStatusHistoryRepository participantStatusHistoryRepository;

    @Test
    @DisplayName("Should find status history by meeting id")
    void testFindByMeetingId() {
        Long meetingId = 1L;

        List<ParticipantStatusHistory> historyList = participantStatusHistoryRepository.findByMeetingId(meetingId);

        assertAll("Participant status history for meeting 1",
                () -> assertThat(historyList).isNotEmpty(),
                () -> assertThat(historyList.get(0).getParticipant().getMeeting().getId()).isEqualTo(meetingId),
                () -> assertThat(historyList).isSortedAccordingTo(
                        (h1, h2) -> h2.getChangedAt().compareTo(h1.getChangedAt())
                )
        );
    }

    @Test
    @DisplayName("Should return empty list for non-existing meeting id")
    void testFindByMeetingIdEmpty() {
        Long meetingId = 999L;

        List<ParticipantStatusHistory> historyList = participantStatusHistoryRepository.findByMeetingId(meetingId);

        assertAll("Participant status history for non-existing meeting",
                () -> assertThat(historyList).isEmpty()
        );
    }
}

package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.ParticipantStatusHistory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ParticipantStatusHistoryRepositoryTest {

    @Autowired
    private ParticipantStatusHistoryRepository participantStatusHistoryRepository;

    @Test
    @DisplayName("Should find status history by meeting id")
    void testFindByMeetingId() {
        // Używamy istniejącego meetingId z danych testowych
        Long meetingId = 1L;

        List<ParticipantStatusHistory> historyList = participantStatusHistoryRepository.findByMeetingId(meetingId);

        // Asercje
        assertThat(historyList).isNotEmpty(); // lista nie może być pusta
        assertThat(historyList.get(0).getParticipant().getMeeting().getId()).isEqualTo(meetingId);
        assertThat(historyList).isSortedAccordingTo(
                (h1, h2) -> h2.getChangedAt().compareTo(h1.getChangedAt()) // od najnowszych do najstarszych
        );
    }



    @Test
    @DisplayName("Should return empty list for non-existing meeting id")
    void testFindByMeetingIdEmpty() {
        Long meetingId = 999L;

        List<ParticipantStatusHistory> historyList = participantStatusHistoryRepository.findByMeetingId(meetingId);

        assertThat(historyList).isEmpty();
    }
}

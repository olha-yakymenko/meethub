package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.MeetingStatistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@DataJpaTest
@ActiveProfiles("postgres")
class MeetingStatisticsRepositoryTest {

    @Autowired
    private MeetingStatisticsRepository statisticsRepository;

    /**
     * data.sql NIE zawiera wpisów w meeting_statistics
     * → repozytorium powinno zwrócić Optional.empty()
     */
    @Test
    void findByMeetingId_shouldReturnEmpty_whenStatisticsDoNotExist() {
        Optional<MeetingStatistics> result =
                statisticsRepository.findByMeetingId(1L);

        assertThat(result).isEmpty(); // ✅ 1 asercja
    }

    /**
     * Brak statystyk → brak wyników dla organizatora
     */
    @Test
    void findByOrganizerId_shouldReturnEmptyList_whenNoStatisticsExist() {
        List<MeetingStatistics> result =
                statisticsRepository.findByOrganizerId(2L); // organizer z data.sql

        assertThat(result).isEmpty(); // ✅ 1 asercja
    }

    /**
     * Usunięcie statystyk dla spotkania,
     * które ich nie ma → brak wyjątku + nadal brak danych
     */
    @Test
    void deleteByMeetingId_shouldNotFail_whenStatisticsDoNotExist() {

        statisticsRepository.deleteByMeetingId(1L);

        Optional<MeetingStatistics> result =
                statisticsRepository.findByMeetingId(1L);

        assertAll(
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result).isEmpty()
        );
    }
}

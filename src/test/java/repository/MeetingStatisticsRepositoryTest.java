package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.dto.OrganizerReportStats;
import com.meethub.domain.model.entity.MeetingStatistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@DataJpaTest
@ActiveProfiles("postgres")
class MeetingStatisticsRepositoryTest {

    @Autowired
    private MeetingStatisticsRepository statisticsRepository;

    @Test
    void findByMeetingId_shouldReturnEmpty_whenStatisticsDoNotExist() {
        Optional<MeetingStatistics> result =
                statisticsRepository.findByMeetingId(1L);

        assertThat(result).isEmpty();
    }


    @Test
    void findByOrganizerId_shouldReturnEmptyList_whenNoStatisticsExist() {
        List<MeetingStatistics> result =
                statisticsRepository.findByOrganizerId(2L);

        assertThat(result).isEmpty();
    }

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


    @Test
    void getOrganizerReportStatsByDateRange_shouldReturnCompleteStats() {

        LocalDateTime dateFrom = LocalDateTime.now().minusDays(30);
        LocalDateTime dateTo = LocalDateTime.now();

        Long organizerId = 2L;

        // When
        OrganizerReportStats result = statisticsRepository.getOrganizerReportStatsByDateRange(
                organizerId, dateFrom, dateTo
        );

        // Then
        assertAll(
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result.getTotalMeetings()).isEqualTo(0L), // No statistics in test data
                () -> assertThat(result.getAverageAttendanceRate()).isNotNull(),
                () -> assertThat(result.getTotalParticipants()).isNotNull(),
                () -> assertThat(result.getTotalAttended()).isNotNull()
        );
    }
}
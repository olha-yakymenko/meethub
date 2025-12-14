package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.NotificationSchedule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class NotificationScheduleRepositoryTest {

    @Autowired
    private NotificationScheduleRepository notificationScheduleRepository;

    @Test
    @DisplayName("Should find notification schedules by user id")
    void testFindByUserId() {
        List<NotificationSchedule> schedules = notificationScheduleRepository.findByUserId(1L);

        assertThat(schedules).isNotEmpty();
        assertThat(schedules).hasSize(2);
//        assertThat(schedules.get(0).getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should return empty list for user with no schedules")
    void testFindByUserIdEmpty() {
        List<NotificationSchedule> schedules = notificationScheduleRepository.findByUserId(999L);

        assertThat(schedules).isEmpty();
    }
}

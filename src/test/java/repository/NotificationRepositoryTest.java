package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.Notification;
import com.meethub.domain.model.enums.NotificationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    @DisplayName("Should find notifications by user id")
    void testFindByUserIdOrderByCreatedAtDesc() {
        Page<Notification> page = notificationRepository.findByUserIdOrderByCreatedAtDesc(1L, PageRequest.of(0, 10));
        assertThat(page).isNotEmpty();
        assertThat(page.getContent().get(0).getUser().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should count notifications by user id and status")
    void testCountByUserIdAndStatus() {
        Long count = notificationRepository.countByUserIdAndStatus(1L, NotificationStatus.PENDING);
        assertThat(count).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Should find in-app messages by user id")
    void testFindInAppMessagesByUserId() {
        List<String> messages = notificationRepository.findInAppMessagesByUserId(1L);
        assertThat(messages).isNotEmpty();
        assertThat(messages.get(0)).contains("message");
    }

    @Test
    @DisplayName("Should find notification by id and user id")
    void testFindByIdAndUserId() {
        Optional<Notification> notification = notificationRepository.findByIdAndUserId(1L, 1L);
        assertThat(notification).isPresent();
        assertThat(notification.get().getUser().getId()).isEqualTo(1L);
    }
}

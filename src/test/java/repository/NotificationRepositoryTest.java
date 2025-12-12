//package repository;
//
//import com.meethub.domain.model.entity.Notification;
//import com.meethub.domain.model.entity.User;
//import com.meethub.domain.model.enums.NotificationStatus;
//import com.meethub.domain.repository.jpa.NotificationRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
//import org.springframework.context.annotation.Import;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.test.context.ActiveProfiles;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Optional;
//
//import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
//
//@DataJpaTest
//@ActiveProfiles("test")
//class NotificationRepositoryTest {
//
//    @Autowired
//    private NotificationRepository notificationRepository;
//
//    @Autowired
//    private TestEntityManager entityManager;
//
//    private User testUser;
//
//    @BeforeEach
//    void setUp() {
//        // Tworzenie testowego użytkownika
//        testUser = User.builder()
//                .email("test@example.com")
//                .firstName("John")
//                .lastName("Doe")
//                .build();
//        entityManager.persist(testUser);
//
//        // Tworzenie testowych powiadomień
//        Notification notification1 = Notification.builder()
//                .user(testUser)
//                .title("Test Notification 1")
//                .message("Test message 1")
//                .status(NotificationStatus.PENDING)
//                .scheduledFor(LocalDateTime.now().plusHours(1))
//                .build();
//
//        Notification notification2 = Notification.builder()
//                .user(testUser)
//                .title("Test Notification 2")
//                .message("Test message 2")
//                .status(NotificationStatus.SENT)
//                .readAt(LocalDateTime.now())
//                .build();
//
//        entityManager.persist(notification1);
//        entityManager.persist(notification2);
//        entityManager.flush();
//    }
//
//    @Test
//    void testCreateNotification() {
//        // Given
//        Notification newNotification = Notification.builder()
//                .user(testUser)
//                .title("New Notification")
//                .message("New message")
//                .status(NotificationStatus.PENDING)
//                .build();
//
//        // When
//        Notification saved = notificationRepository.save(newNotification);
//
//        // Then
//        assertThat(saved.getId()).isNotNull();
//        assertThat(saved.getTitle()).isEqualTo("New Notification");
//        assertThat(notificationRepository.count()).isEqualTo(3L);
//    }
//
//    @Test
//    void testReadNotification() {
//        // Given
//        Notification notification = notificationRepository.findAll().get(0);
//
//        // When
//        Notification found = notificationRepository.findById(notification.getId())
//                .orElseThrow();
//
//        // Then
//        assertThat(found).isNotNull();
//        assertThat(found.getTitle()).isEqualTo("Test Notification 1");
//        assertThat(found.getStatus()).isEqualTo(NotificationStatus.PENDING);
//    }
//
//    @Test
//    void testUpdateNotification() {
//        // Given
//        Notification notification = notificationRepository.findAll().get(0);
//        notification.setStatus(NotificationStatus.SENT);
//        notification.setReadAt(LocalDateTime.now());
//
//        // When
//        Notification updated = notificationRepository.save(notification);
//
//        // Then
//        assertThat(updated.getStatus()).isEqualTo(NotificationStatus.SENT);
//        assertThat(updated.getReadAt()).isNotNull();
//    }
//
//    @Test
//    void testDeleteNotification() {
//        // Given
//        Notification notification = notificationRepository.findAll().get(0);
//
//        // When
//        notificationRepository.delete(notification);
//
//        // Then
//        assertThat(notificationRepository.count()).isEqualTo(1L);
//        assertThat(notificationRepository.findById(notification.getId()))
//                .isEmpty();
//    }
//
//    @Test
//    void testFindByUserIdAndStatus() {
//        // When
//        List<Notification> pendingNotifications = notificationRepository
//                .findByUserIdAndStatus(testUser.getId(), NotificationStatus.PENDING);
//
//        // Then
//        assertThat(pendingNotifications).hasSize(1);
//        assertThat(pendingNotifications.get(0).getStatus())
//                .isEqualTo(NotificationStatus.PENDING);
//    }
//
//    @Test
//    void testFindByIdAndUserId() {
//        // Given
//        Notification notification = notificationRepository.findAll().get(0);
//
//        // When
//        Optional<Notification> found = notificationRepository
//                .findByIdAndUserId(notification.getId(), testUser.getId());
//
//        // Then
//        assertThat(found).isPresent();
//        assertThat(found.get().getId()).isEqualTo(notification.getId());
//    }
//
//    @Test
//    void testCountByUserIdAndStatus() {
//        // When
//        Long pendingCount = notificationRepository
//                .countByUserIdAndStatus(testUser.getId(), NotificationStatus.PENDING);
//
//        Long sentCount = notificationRepository
//                .countByUserIdAndStatus(testUser.getId(), NotificationStatus.SENT);
//
//        // Then
//        assertThat(pendingCount).isEqualTo(1L);
//        assertThat(sentCount).isEqualTo(1L);
//    }
//
//    @Test
//    void testFindByUserIdOrderByCreatedAtDesc() {
//        // When
//        Page<Notification> page = notificationRepository
//                .findByUserIdOrderByCreatedAtDesc(
//                        testUser.getId(),
//                        PageRequest.of(0, 10)
//                );
//
//        // Then
//        assertThat(page.getContent()).hasSize(2);
//        // Sprawdź sortowanie (najnowszy pierwszy)
//        List<Notification> notifications = page.getContent();
//        assertThat(notifications.get(0).getCreatedAt())
//                .isAfterOrEqualTo(notifications.get(1).getCreatedAt());
//    }
//
//    @Test
//    void testFindPendingNotificationsCustomQuery() {
//        // Given - tworzymy przeszłe powiadomienie PENDING
//        Notification pastNotification = Notification.builder()
//                .user(testUser)
//                .title("Past Notification")
//                .message("Should be returned")
//                .status(NotificationStatus.PENDING)
//                .scheduledFor(LocalDateTime.now().minusHours(1))
//                .build();
//        notificationRepository.save(pastNotification);
//
//        // When
//        List<Notification> pending = notificationRepository.findPendingNotifications();
//
//        // Then
//        assertThat(pending).hasSize(1);
//        assertThat(pending.get(0).getTitle()).isEqualTo("Past Notification");
//    }
//
//    @Test
//    void testCountUnreadByUserIdCustomQuery() {
//        // Given - przeczytaj jedno powiadomienie
//        Notification notification = notificationRepository
//                .findByUserIdAndStatus(testUser.getId(), NotificationStatus.PENDING)
//                .get(0);
//        notification.setReadAt(LocalDateTime.now());
//        notificationRepository.save(notification);
//
//        // When
//        Long unreadCount = notificationRepository.countUnreadByUserId(testUser.getId());
//
//        // Then
//        assertThat(unreadCount).isEqualTo(0L);
//    }
//
//    @Test
//    void testFindByStatusAndScheduledForBeforeNativeQuery() {
//        // Given
//        LocalDateTime now = LocalDateTime.now();
//
//        // Dodajemy kolejne przeszłe powiadomienie
//        Notification anotherPast = Notification.builder()
//                .user(testUser)
//                .title("Another Past")
//                .message("Another past notification")
//                .status(NotificationStatus.PENDING)
//                .scheduledFor(now.minusMinutes(30))
//                .build();
//        notificationRepository.save(anotherPast);
//
//        // When
//        List<Notification> found = notificationRepository
//                .findByStatusAndScheduledForBefore(
//                        NotificationStatus.PENDING,
//                        now
//                );
//
//        // Then
//        assertThat(found).hasSize(1);
//        assertThat(found.get(0).getTitle()).isEqualTo("Another Past");
//    }
//
//    @Test
//    void testCustomQueryWithParameters() {
//        // Given
//        LocalDateTime now = LocalDateTime.now();
//        Notification notification = Notification.builder()
//                .user(testUser)
//                .title("Test Param Query")
//                .message("Testing parameters")
//                .status(NotificationStatus.PENDING)
//                .scheduledFor(now.minusHours(2))
//                .build();
//        notificationRepository.save(notification);
//
//        // When - bezpośrednie wywołanie metody repository
//        List<Notification> result = notificationRepository
//                .findByStatusAndScheduledForBefore(NotificationStatus.PENDING, now);
//
//        // Then
//        assertThat(result).isNotEmpty();
//        assertThat(result)
//                .allMatch(n -> n.getStatus() == NotificationStatus.PENDING);
//        assertThat(result)
//                .allMatch(n -> n.getScheduledFor().isBefore(now));
//    }
//
//    @Test
//    void testNotificationRelationships() {
//        // Given
//        Notification notification = notificationRepository.findAll().get(0);
//
//        // When
//        Notification withUser = notificationRepository.findById(notification.getId())
//                .orElseThrow();
//
//        // Then
//        assertThat(withUser.getUser()).isNotNull();
//        assertThat(withUser.getUser().getEmail()).isEqualTo("test@example.com");
//    }
//
//    @Test
//    void testFindAllNotifications() {
//        // When
//        List<Notification> all = notificationRepository.findAll();
//
//        // Then
//        assertThat(all).hasSize(2);
//    }
//
//    @Test
//    void testDeleteAllNotifications() {
//        // When
//        notificationRepository.deleteAll();
//
//        // Then
//        assertThat(notificationRepository.count()).isEqualTo(0L);
//    }
//}
package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.Notification;
import com.meethub.domain.model.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Podstawowe metody
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    List<Notification> findByUserIdAndStatus(Long userId, NotificationStatus status);
    Optional<Notification> findByIdAndUserId(Long id, Long userId);
    Long countByUserIdAndStatus(Long userId, NotificationStatus status);
    Long countByUserId(Long userId);

    // POPRAWIONE METODY - DODAJ BRAKUJĄCE @Param
    @Query("SELECT n FROM Notification n WHERE n.status = :status AND n.scheduledFor <= :now")
    List<Notification> findByStatusAndScheduledForBefore(
            @Param("status") NotificationStatus status,
            @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.readAt IS NULL")
    Long countUnreadByUserId(@Param("userId") Long userId);

    // Domyślna implementacja dla uproszczenia
    default List<Notification> findPendingNotifications() {
        return findByStatusAndScheduledForBefore(NotificationStatus.PENDING, LocalDateTime.now());
    }
}






//package com.meethub.domain.repository.jpa;
//
//import com.meethub.domain.model.entity.Notification;
//import com.meethub.domain.model.enums.NotificationStatus;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.stereotype.Repository;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Optional;
//
//@Repository
//public interface NotificationRepository extends JpaRepository<Notification, Long> {
//
//    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
//    List<Notification> findByUserIdAndStatus(Long userId, NotificationStatus status);
//    Optional<Notification> findByIdAndUserId(Long id, Long userId);
//    Long countByUserIdAndStatus(Long userId, NotificationStatus status);
//    Long countByUserId(Long userId);
//
//    @Query("SELECT n FROM Notification n WHERE n.status = 'PENDING' AND n.scheduledFor <= :now")
//    List<Notification> findByStatusAndScheduledForBefore(NotificationStatus status, LocalDateTime now);
//
//    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.readAt IS NULL")
//    Long countUnreadByUserId(Long userId);
//}
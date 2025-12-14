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

    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    List<Notification> findByUserIdAndStatus(Long userId, NotificationStatus status);
    Optional<Notification> findByIdAndUserId(Long id, Long userId);
    Long countByUserIdAndStatus(Long userId, NotificationStatus status);
    Long countByUserId(Long userId);

    @Query("SELECT n.message FROM Notification n " +
            "WHERE n.user.id = :userId AND n.channel = 'IN_APP' " +
            "ORDER BY n.createdAt DESC")
    List<String> findInAppMessagesByUserId(@Param("userId") Long userId);

    @Query("SELECT n.message FROM Notification n " +
            "WHERE n.user.id = :userId AND n.channel = 'IN_APP' " +
            "ORDER BY n.createdAt DESC")
    List<String> findInAppMessagesByUserId(@Param("userId") Long userId, Pageable pageable);

}



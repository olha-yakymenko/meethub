package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.NotificationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationScheduleRepository extends JpaRepository<NotificationSchedule, Long> {
    List<NotificationSchedule> findByUserId(Long userId);
}
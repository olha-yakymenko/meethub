package com.meethub.domain.service;

import com.meethub.domain.model.entity.Meeting;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

@Validated
public interface MeetingSchedulerService {

    /**
     * Harmonogram sprawdzający nadchodzące spotkania co minutę
     */
    @Scheduled(fixedRate = 60000) // co 1 minutę
    @Transactional
    void scheduleUpcomingMeetings();

    /**
     * Zaplanowanie powiadomień dla konkretnego spotkania
     */
    void scheduleMeetingNotifications(
            @NotNull @Valid Meeting meeting
    );

    /**
     * Anulowanie harmonogramu spotkania
     */
    void cancelMeetingSchedule(
            @NotNull @Positive Long meetingId
    );

    /**
     * Czyszczenie zakończonych spotkań co godzinę
     */
    @Scheduled(fixedRate = 3600000) // co godzinę
    void cleanupFinishedMeetings();

    /**
     * Zatrzymanie harmonogramu
     */
    void shutdown();

    /**
     * Włączenie harmonogramu
     */
    void enableScheduling();

    /**
     * Wyłączenie harmonogramu
     */
    void disableScheduling();

    /**
     * Pobranie statusu harmonogramu
     */
    Map<String, Object> getSchedulerStatus();
}



//package com.meethub.domain.service;
//
//import com.meethub.domain.model.entity.Meeting;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.Map;
//
//public interface MeetingSchedulerService {
//    @Scheduled(fixedRate = 60000) // co 1 minutę
//    @Transactional
//    public abstract void scheduleUpcomingMeetings();
//
//    public abstract void scheduleMeetingNotifications(Meeting meeting);
//
//    public abstract void cancelMeetingSchedule(Long meetingId);
//
//    @Scheduled(fixedRate = 3600000) // co godzinę
//    public abstract void cleanupFinishedMeetings();
//
//    public abstract void shutdown();
//
//    public abstract void enableScheduling();
//
//    public abstract void disableScheduling();
//
//    public abstract Map<String, Object> getSchedulerStatus();
//}

package com.meethub.domain.service;

import com.meethub.domain.model.entity.Meeting;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

public interface MeetingSchedulerService {
    @Scheduled(fixedRate = 60000) // co 1 minutę
    @Transactional
    public abstract void scheduleUpcomingMeetings();

    public abstract void scheduleMeetingNotifications(Meeting meeting);

    public abstract void cancelMeetingSchedule(Long meetingId);

    @Scheduled(fixedRate = 3600000) // co godzinę
    public abstract void cleanupFinishedMeetings();

    public abstract void shutdown();

    public abstract void enableScheduling();

    public abstract void disableScheduling();

    public abstract Map<String, Object> getSchedulerStatus();
}

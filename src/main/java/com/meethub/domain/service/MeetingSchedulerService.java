// MeetingSchedulerService.java
package com.meethub.domain.service;

import com.meethub.domain.model.entity.Meeting;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.Map;


public interface MeetingSchedulerService {

    void scheduleUpcomingMeetings();
    void scheduleMeetingNotifications(@NotNull @Valid Meeting meeting);
    void cancelMeetingSchedule(@NotNull Long meetingId);
    void shutdown();
    void enableScheduling();
    void disableScheduling();
    Map<String, Object> getSchedulerStatus();
}
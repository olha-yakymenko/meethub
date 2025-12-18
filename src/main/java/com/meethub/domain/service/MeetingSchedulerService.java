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


    @Scheduled(fixedRate = 60000)
    @Transactional
    void scheduleUpcomingMeetings();


    void scheduleMeetingNotifications(
            @NotNull @Valid Meeting meeting
    );


    void cancelMeetingSchedule(
            @NotNull @Positive Long meetingId
    );



    void shutdown();


    void enableScheduling();


    void disableScheduling();


    Map<String, Object> getSchedulerStatus();
}



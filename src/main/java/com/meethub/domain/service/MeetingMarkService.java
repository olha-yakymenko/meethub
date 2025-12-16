package com.meethub.domain.service;

import jakarta.validation.constraints.Positive;
import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
public interface MeetingMarkService {

    @Transactional
    void markAsImportant(
            @NotNull @Positive Long userId,
            @NotNull @Positive Long meetingId
    );

    @Transactional
    void unmarkAsImportant(
            @NotNull @Positive Long userId,
            @NotNull @Positive Long meetingId
    );

    boolean isMeetingImportantForUser(
            @NotNull @Positive Long userId,
            @NotNull @Positive Long meetingId
    );

    @Transactional
    boolean toggleImportant(
            @NotNull @Positive Long userId,
            @NotNull @Positive Long meetingId
    );

    List<Long> getImportantMeetingIdsForUser(Long userId);
}
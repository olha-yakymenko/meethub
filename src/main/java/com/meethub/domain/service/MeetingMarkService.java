package com.meethub.domain.service;

import java.util.List;

public interface MeetingMarkService {

    void markAsImportant(Long userId, Long meetingId);

    void unmarkAsImportant(Long userId, Long meetingId);

    boolean isMeetingImportantForUser(Long userId, Long meetingId);

    boolean toggleImportant(Long userId, Long meetingId);

    List<Long> getImportantMeetingIdsForUser(Long userId);
}
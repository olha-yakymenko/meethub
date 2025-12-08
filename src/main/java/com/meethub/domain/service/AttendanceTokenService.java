package com.meethub.domain.service;

import com.meethub.domain.model.entity.AttendanceToken;
import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.User;

import java.util.Optional;

public interface AttendanceTokenService {
    AttendanceToken createToken(User user, Meeting meeting);

    Optional<AttendanceToken> getTokenForUserAndMeeting(Long userId, Long meetingId);
}

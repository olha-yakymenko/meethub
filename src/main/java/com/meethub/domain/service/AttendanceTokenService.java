package com.meethub.domain.service;

import com.meethub.domain.model.entity.AttendanceToken;
import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;

public interface AttendanceTokenService {

    AttendanceToken createToken(
            @NotNull User user,
            @NotNull Meeting meeting
    );

    boolean validateAndUseToken(
            @NotNull String token,
            @NotNull Long meetingId
    );

    Optional<AttendanceToken> getTokenForUserAndMeeting(
            @NotNull Long userId,
            @NotNull Long meetingId
    );

    Optional<Long> getUserIdFromToken(String token, Long meetingId);
}
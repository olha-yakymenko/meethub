//package com.meethub.domain.service;
//
//import com.meethub.domain.model.entity.AttendanceToken;
//import com.meethub.domain.model.entity.Meeting;
//import com.meethub.domain.model.entity.User;
//
//import java.util.Optional;
//
//public interface AttendanceTokenService {
//    AttendanceToken createToken(User user, Meeting meeting);
//
//    boolean validateAndUseToken(String token, Long meetingId);
//
//    Optional<AttendanceToken> getTokenForUserAndMeeting(Long userId, Long meetingId);
//}



package com.meethub.domain.service;

import com.meethub.domain.model.entity.AttendanceToken;
import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.User;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;

@Validated
public interface AttendanceTokenService {

    AttendanceToken createToken(
            @NotNull User user,
            @NotNull Meeting meeting
    );

    boolean validateAndUseToken(
            @NotBlank String token,
            @NotNull @Min(1) Long meetingId
    );

    Optional<AttendanceToken> getTokenForUserAndMeeting(
            @NotNull @Min(1) Long userId,
            @NotNull @Min(1) Long meetingId
    );
}

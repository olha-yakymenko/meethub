// ParticipationService.java
package com.meethub.domain.service;

import com.meethub.domain.model.entity.MeetingParticipant;
import com.meethub.domain.model.enums.ParticipationStatus;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;


public interface ParticipationService {

    MeetingParticipant confirmParticipation(@NotNull Long meetingId, @NotNull Long userId);
    MeetingParticipant declineParticipation(@NotNull Long meetingId, @NotNull Long userId);
    MeetingParticipant markAsAttended(@NotNull Long meetingId, @NotNull Long userId);

    Map<ParticipationStatus, Long> getResponseStatistics(@NotNull Long meetingId);
    Double getAverageResponseTime(@NotNull Long meetingId);

    boolean isUserParticipant(@NotNull Long meetingId, @NotNull Long userId);
    boolean isUserConfirmed(@NotNull Long meetingId, @NotNull Long userId);

    MeetingParticipant updateUserStatus(
            @NotNull Long meetingId,
            @NotNull Long userId,
            @NotNull ParticipationStatus status
    );

    List<MeetingParticipant> getMeetingParticipants(@NotNull Long meetingId);
    List<MeetingParticipant> getConfirmedParticipants(@NotNull Long meetingId);

    MeetingParticipant addToWaitingList(@NotNull Long meetingId, @NotNull Long userId);
    MeetingParticipant promoteFromWaitingList(@NotNull Long meetingId, @NotNull Long userId);
}
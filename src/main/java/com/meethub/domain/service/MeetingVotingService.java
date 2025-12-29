// MeetingVotingService.java
package com.meethub.domain.service;

import com.meethub.domain.model.entity.MeetingVoting;
import com.meethub.domain.model.request.*;
import com.meethub.domain.model.response.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;


public interface MeetingVotingService {

    VotingResponse createVoting(
            @NotNull Long meetingId,
            @Valid @NotNull CreateVotingRequest request,
            @NotNull Long organizerId
    );

    List<VotingResponse> getMeetingVotings(@NotNull Long meetingId, @NotNull Long userId);
    VotingResponse getVotingDetails(@NotNull Long votingId, @NotNull Long userId);

    VoteResponse submitVote(
            @NotNull Long votingId,
            @Valid @NotNull VoteRequest request,
            @NotNull Long userId
    );

    VotingOptionResponse suggestOption(
            @NotNull Long votingId,
            @Valid @NotNull VotingOptionRequest request,
            @NotNull Long userId
    );

    VotingResponse closeVoting(@NotNull Long votingId, @NotNull Long organizerId);
    WinningOptionResponse findOptimalTime(@NotNull Long votingId);
    boolean hasActiveVoting(@NotNull Long meetingId);
    List<VotingResponse> getExpiredVotings();
    void closeExpiredVotingIfNeeded(@NotNull Long votingId);

    MeetingVoting getVotingEntity(@NotNull Long votingId);
    VotingResponse getVotingDetailsForUser(@NotNull Long votingId, @NotNull Long userId);
    void validateUserCanVote(@NotNull Long meetingId, @NotNull Long votingId, @NotNull Long userId);
}
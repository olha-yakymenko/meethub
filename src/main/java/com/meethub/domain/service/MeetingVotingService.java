package com.meethub.domain.service;

import com.meethub.domain.model.entity.MeetingVoting;
import com.meethub.domain.model.request.*;
import com.meethub.domain.model.response.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
public interface MeetingVotingService {


    VotingResponse createVoting(
            @NotNull @Positive Long meetingId,
            @NotNull @Valid CreateVotingRequest request,
            @NotNull @Positive Long organizerId
    );


    List<VotingResponse> getMeetingVotings(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long userId
    );


    VotingResponse getVotingDetails(
            @NotNull @Positive Long votingId,
            @NotNull @Positive Long userId
    );


    VoteResponse submitVote(
            @NotNull @Positive Long votingId,
            @NotNull @Valid VoteRequest request,
            @NotNull @Positive Long userId
    );


    VotingOptionResponse suggestOption(
            @NotNull @Positive Long votingId,
            @NotNull @Valid VotingOptionRequest request,
            @NotNull @Positive Long userId
    );


    VotingResponse closeVoting(
            @NotNull @Positive Long votingId,
            @NotNull @Positive Long organizerId
    );


    WinningOptionResponse findOptimalTime(@NotNull @Positive Long votingId);


    boolean hasActiveVoting(@NotNull @Positive Long meetingId);


    List<VotingResponse> getExpiredVotings();

    void closeExpiredVotingIfNeeded(@NotNull @Positive Long votingId);

    @Transactional(readOnly = true)
    MeetingVoting getVotingEntity(@NotNull @Positive Long votingId);

    VotingResponse getVotingDetailsForUser(
            @NotNull @Positive Long votingId,
            @NotNull @Positive Long userId
    );

    @Transactional(readOnly = true)
    void validateUserCanVote(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long votingId,
            @NotNull @Positive Long userId
    );
}



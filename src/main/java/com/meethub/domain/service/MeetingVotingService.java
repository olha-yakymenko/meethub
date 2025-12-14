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

    /**
     * Tworzy nowe głosowanie dla spotkania
     */
    VotingResponse createVoting(
            @NotNull @Positive Long meetingId,
            @NotNull @Valid CreateVotingRequest request,
            @NotNull @Positive Long organizerId
    );

    /**
     * Pobiera wszystkie głosowania dla spotkania
     */
    List<VotingResponse> getMeetingVotings(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long userId
    );

    /**
     * Pobiera szczegóły konkretnego głosowania
     */
    VotingResponse getVotingDetails(
            @NotNull @Positive Long votingId,
            @NotNull @Positive Long userId
    );

    /**
     * Składa głos w głosowaniu
     */
    VoteResponse submitVote(
            @NotNull @Positive Long votingId,
            @NotNull @Valid VoteRequest request,
            @NotNull @Positive Long userId
    );

    /**
     * Sugeruje nową opcję w głosowaniu
     */
    VotingOptionResponse suggestOption(
            @NotNull @Positive Long votingId,
            @NotNull @Valid VotingOptionRequest request,
            @NotNull @Positive Long userId
    );

    /**
     * Zamyka głosowanie i wybiera zwycięski termin
     */
    VotingResponse closeVoting(
            @NotNull @Positive Long votingId,
            @NotNull @Positive Long organizerId
    );

    /**
     * Znajduje optymalny termin na podstawie głosów
     */
    WinningOptionResponse findOptimalTime(@NotNull @Positive Long votingId);

    /**
     * Sprawdza czy użytkownik ma aktywne głosowanie w spotkaniu
     */
    boolean hasActiveVoting(@NotNull @Positive Long meetingId);

    /**
     * Pobiera głosowania wymagające zamknięcia (przekroczony deadline)
     */
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









//// MeetingVotingService.java
//package com.meethub.domain.service;
//
//import com.meethub.domain.model.entity.MeetingVoting;
//import com.meethub.domain.model.request.*;
//import com.meethub.domain.model.response.*;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//
//public interface MeetingVotingService {
//
//    /**
//     * Tworzy nowe głosowanie dla spotkania
//     */
//    VotingResponse createVoting(Long meetingId, CreateVotingRequest request, Long organizerId);
//
//    /**
//     * Pobiera wszystkie głosowania dla spotkania
//     */
//    List<VotingResponse> getMeetingVotings(Long meetingId, Long userId);
//
//
//    /**
//     * Pobiera szczegóły konkretnego głosowania
//     */
//    VotingResponse getVotingDetails(Long votingId, Long userId);
//
//    /**
//     * Składa głos w głosowaniu
//     */
//    VoteResponse submitVote(Long votingId, VoteRequest request, Long userId);
//
//    /**
//     * Sugeruje nową opcję w głosowaniu
//     */
//    VotingOptionResponse suggestOption(Long votingId, VotingOptionRequest request, Long userId);
//
//    /**
//     * Zamyka głosowanie i wybiera zwycięski termin
//     */
//    VotingResponse closeVoting(Long votingId, Long organizerId);
//
//    /**
//     * Znajduje optymalny termin na podstawie głosów
//     */
//    WinningOptionResponse findOptimalTime(Long votingId);
//
//    /**
//     * Sprawdza czy użytkownik ma aktywne głosowanie w spotkaniu
//     */
//    boolean hasActiveVoting(Long meetingId);
//
//    /**
//     * Pobiera głosowania wymagające zamknięcia (przekroczony deadline)
//     */
//    List<VotingResponse> getExpiredVotings();
//
//    void closeExpiredVotingIfNeeded(Long votingId);
//
//    @Transactional(readOnly = true)
//    MeetingVoting getVotingEntity(Long votingId);
//
//    VotingResponse getVotingDetailsForUser(Long votingId, Long userId);
//
//    @Transactional(readOnly = true)
//    void validateUserCanVote(Long meetingId, Long votingId, Long userId);
//}
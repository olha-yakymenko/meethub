// MeetingVotingService.java
package com.meethub.domain.service;

import com.meethub.domain.model.entity.MeetingVoting;
import com.meethub.domain.model.request.*;
import com.meethub.domain.model.response.*;

import java.util.List;

public interface MeetingVotingService {

    /**
     * Tworzy nowe głosowanie dla spotkania
     */
    VotingResponse createVoting(Long meetingId, CreateVotingRequest request, Long organizerId);

    /**
     * Pobiera wszystkie głosowania dla spotkania
     */
    List<VotingResponse> getMeetingVotings(Long meetingId, Long userId);


    /**
     * Pobiera szczegóły konkretnego głosowania
     */
    VotingResponse getVotingDetails(Long votingId, Long userId);

    /**
     * Składa głos w głosowaniu
     */
    VoteResponse submitVote(Long votingId, VoteRequest request, Long userId);

    /**
     * Sugeruje nową opcję w głosowaniu
     */
    VotingOptionResponse suggestOption(Long votingId, VotingOptionRequest request, Long userId);

    /**
     * Zamyka głosowanie i wybiera zwycięski termin
     */
    VotingResponse closeVoting(Long votingId, Long organizerId);

    /**
     * Znajduje optymalny termin na podstawie głosów
     */
    WinningOptionResponse findOptimalTime(Long votingId);

    /**
     * Sprawdza czy użytkownik ma aktywne głosowanie w spotkaniu
     */
    boolean hasActiveVoting(Long meetingId);

    /**
     * Pobiera głosowania wymagające zamknięcia (przekroczony deadline)
     */
    List<VotingResponse> getExpiredVotings();

    void closeExpiredVotingIfNeeded(Long votingId);

}
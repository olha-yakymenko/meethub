package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {

    // ✅ ISTNIEJĄCE METODY
    List<Vote> findByVotingIdAndUserId(Long votingId, Long userId);

    @Query("SELECT COUNT(DISTINCT v.user.id) FROM Vote v WHERE v.voting.id = :votingId")
    long countDistinctVotersByVotingId(@Param("votingId") Long votingId);

    @Query("SELECT COUNT(v) FROM Vote v WHERE v.option.id = :optionId")
    long countByOptionId(@Param("optionId") Long optionId);

    @Query("SELECT v.option.id, COUNT(v) FROM Vote v WHERE v.voting.id = :votingId GROUP BY v.option.id")
    List<Object[]> countVotesByOption(@Param("votingId") Long votingId);

    boolean existsByVotingIdAndUserId(Long votingId, Long userId);

    @Query("SELECT v FROM Vote v WHERE v.voting.id = :votingId AND v.user.id = :userId")
    List<Vote> findUserVotes(@Param("votingId") Long votingId, @Param("userId") Long userId);

    void deleteByVotingIdAndUserId(Long votingId, Long userId);

    // ✅ NOWE METODY - DODANE

    /**
     * Sprawdza czy użytkownik głosował na konkretną opcję
     */
    @Query("SELECT COUNT(v) > 0 FROM Vote v WHERE v.voting.id = :votingId AND v.user.id = :userId AND v.option.id = :optionId")
    boolean existsByVotingIdAndUserIdAndOptionId(@Param("votingId") Long votingId,
                                                 @Param("userId") Long userId,
                                                 @Param("optionId") Long optionId);

    /**
     * Pobiera wszystkie głosy dla danego głosowania
     */
    List<Vote> findByVotingId(Long votingId);

    /**
     * Pobiera wszystkie głosy dla danego użytkownika
     */
    List<Vote> findByUserId(Long userId);

    /**
     * Pobiera głosy dla danej opcji głosowania
     */
    List<Vote> findByOptionId(Long optionId);

    /**
     * Liczy całkowitą liczbę głosów w głosowaniu
     */
    @Query("SELECT COUNT(v) FROM Vote v WHERE v.voting.id = :votingId")
    long countByVotingId(@Param("votingId") Long votingId);

    /**
     * Pobiera głosy z określoną wagą (dla zaawansowanych algorytmów)
     */
    @Query("SELECT v FROM Vote v WHERE v.voting.id = :votingId AND v.voteWeight > :minWeight")
    List<Vote> findByVotingIdAndVoteWeightGreaterThan(@Param("votingId") Long votingId,
                                                      @Param("minWeight") Integer minWeight);

    /**
     * Pobiera głosy z określonym porządkiem preferencji
     */
    @Query("SELECT v FROM Vote v WHERE v.voting.id = :votingId AND v.preferenceOrder IS NOT NULL ORDER BY v.preferenceOrder ASC")
    List<Vote> findVotesWithPreferencesByVotingId(@Param("votingId") Long votingId);

    /**
     * Pobiera unikalnych głosujących dla głosowania
     */
    @Query("SELECT DISTINCT v.user.id FROM Vote v WHERE v.voting.id = :votingId")
    List<Long> findDistinctVoterIdsByVotingId(@Param("votingId") Long votingId);

    /**
     * Pobiera głosy z określonym statusem preferencji
     */
    @Query("SELECT v FROM Vote v WHERE v.voting.id = :votingId AND v.preferenceOrder = :preferenceOrder")
    List<Vote> findByVotingIdAndPreferenceOrder(@Param("votingId") Long votingId,
                                                @Param("preferenceOrder") Integer preferenceOrder);

    /**
     * Usuwa wszystkie głosy dla danej opcji
     */
    @Modifying
    @Query("DELETE FROM Vote v WHERE v.option.id = :optionId")
    void deleteByOptionId(@Param("optionId") Long optionId);

    /**
     * Usuwa wszystkie głosy dla danego głosowania
     */
    @Modifying
    @Query("DELETE FROM Vote v WHERE v.voting.id = :votingId")
    void deleteByVotingId(@Param("votingId") Long votingId);

    /**
     * Pobiera głosy z określoną wagą głosu
     */
    @Query("SELECT v FROM Vote v WHERE v.voteWeight = :voteWeight")
    List<Vote> findByVoteWeight(@Param("voteWeight") Integer voteWeight);

    /**
     * Sprawdza czy istnieją jakieś głosy dla głosowania
     */
    boolean existsByVotingId(Long votingId);

    /**
     * Sprawdza czy istnieją jakieś głosy dla opcji
     */
    boolean existsByOptionId(Long optionId);

    /**
     * Pobiera głosy w określonym przedziale czasowym
     */
    @Query("SELECT v FROM Vote v WHERE v.votedAt BETWEEN :startDate AND :endDate")
    List<Vote> findVotesBetweenDates(@Param("startDate") java.time.LocalDateTime startDate,
                                     @Param("endDate") java.time.LocalDateTime endDate);

    /**
     * Pobiera ostatnie głosy użytkownika
     */
    @Query("SELECT v FROM Vote v WHERE v.user.id = :userId ORDER BY v.votedAt DESC LIMIT :limit")
    List<Vote> findRecentVotesByUserId(@Param("userId") Long userId,
                                       @Param("limit") int limit);

    /**
     * Pobiera statystyki głosów użytkownika
     */
    @Query("SELECT COUNT(v), COALESCE(SUM(v.voteWeight), 0) FROM Vote v WHERE v.user.id = :userId")
    Object[] getUserVoteStatistics(@Param("userId") Long userId);

    /**
     * Pobiera głosy z preferencjami dla algorytmów rankingowych
     */
    @Query("SELECT v FROM Vote v WHERE v.voting.id = :votingId AND v.preferenceOrder IS NOT NULL ORDER BY v.user.id, v.preferenceOrder")
    List<Vote> findRankedVotesByVotingId(@Param("votingId") Long votingId);

    /**
     * Pobiera unikalne pary użytkownik-opcja (dla sprawdzania duplikatów)
     */
    @Query("SELECT DISTINCT v.user.id, v.option.id FROM Vote v WHERE v.voting.id = :votingId")
    List<Object[]> findDistinctUserOptionPairsByVotingId(@Param("votingId") Long votingId);

    /**
     * Pobiera głosy z najwyższą wagą dla głosowania
     */
    @Query("SELECT v FROM Vote v WHERE v.voting.id = :votingId ORDER BY v.voteWeight DESC, v.votedAt DESC")
    List<Vote> findVotesByVotingIdOrderByWeightDesc(@Param("votingId") Long votingId);

    /**
     * Pobiera liczbę głosów według wagi
     */
    @Query("SELECT v.voteWeight, COUNT(v) FROM Vote v WHERE v.voting.id = :votingId GROUP BY v.voteWeight")
    List<Object[]> countVotesByWeightGroup(@Param("votingId") Long votingId);
}
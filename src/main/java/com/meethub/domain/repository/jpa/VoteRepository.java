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

    @Query("SELECT COUNT(DISTINCT v.user.id) FROM Vote v WHERE v.voting.id = :votingId")
    long countDistinctVotersByVotingId(@Param("votingId") Long votingId);

    @Query("SELECT COUNT(v) FROM Vote v WHERE v.option.id = :optionId")
    long countByOptionId(@Param("optionId") Long optionId);

    @Query("SELECT v.option.id, COUNT(v) FROM Vote v WHERE v.voting.id = :votingId GROUP BY v.option.id")
    List<Object[]> countVotesByOption(@Param("votingId") Long votingId);

    @Query("SELECT v FROM Vote v WHERE v.voting.id = :votingId AND v.user.id = :userId")
    List<Vote> findUserVotes(@Param("votingId") Long votingId, @Param("userId") Long userId);

    void deleteByVotingIdAndUserId(Long votingId, Long userId);

    @Query("SELECT COUNT(v) > 0 FROM Vote v WHERE v.voting.id = :votingId AND v.user.id = :userId AND v.option.id = :optionId")
    boolean existsByVotingIdAndUserIdAndOptionId(@Param("votingId") Long votingId,
                                                 @Param("userId") Long userId,
                                                 @Param("optionId") Long optionId);

    List<Vote> findByVotingId(Long votingId);

    List<Vote> findByUserId(Long userId);

    @Query("SELECT COUNT(v) FROM Vote v WHERE v.voting.id = :votingId")
    long countByVotingId(@Param("votingId") Long votingId);

}
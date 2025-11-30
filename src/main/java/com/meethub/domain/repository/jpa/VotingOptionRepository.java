package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.VotingOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VotingOptionRepository extends JpaRepository<VotingOption, Long> {

    List<VotingOption> findByVotingId(Long votingId);

    @Query("SELECT vo FROM VotingOption vo WHERE vo.voting.id = :votingId ORDER BY vo.optionDate ASC")
    List<VotingOption> findByVotingIdOrderByDate(@Param("votingId") Long votingId);

    @Query("SELECT COUNT(vo) FROM VotingOption vo WHERE vo.voting.id = :votingId")
    long countByVotingId(@Param("votingId") Long votingId);
}
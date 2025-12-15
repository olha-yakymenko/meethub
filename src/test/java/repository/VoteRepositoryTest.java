package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.Vote;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@DataJpaTest
@ActiveProfiles("postgres")
class VoteRepositoryTest {

    @Autowired
    private VoteRepository voteRepository;

    @Test
    @DisplayName("Should count distinct voters by voting ID")
    void testCountDistinctVotersByVotingId() {
        long count = voteRepository.countDistinctVotersByVotingId(1L);
        assertThat(count).isEqualTo(2);
    }


    @Test
    @DisplayName("Should count votes grouped by option")
    void testCountVotesByOption() {
        List<Object[]> results = voteRepository.countVotesByOption(1L);

        assertAll("Votes by option",
                () -> assertThat(results).isNotEmpty(),
                () -> assertThat(results.get(0)[0]).isNotNull(),
                () -> assertThat((Long) results.get(0)[1]).isPositive()
        );
    }

    @Test
    @DisplayName("Should find user votes by voting and user ID")
    void testFindUserVotes() {
        List<Vote> votes = voteRepository.findUserVotes(1L, 3L);

        assertAll("User votes",
                () -> assertThat(votes).hasSize(1),
                () -> assertThat(votes.get(0).getUser().getId()).isEqualTo(3L),
                () -> assertThat(votes.get(0).getVoting().getId()).isEqualTo(1L)
        );
    }

    @Test
    @DisplayName("Should check if vote exists")
    void testExistsByVotingIdAndUserIdAndOptionId() {
        assertAll("Vote existence",
                () -> assertThat(voteRepository.existsByVotingIdAndUserIdAndOptionId(1L, 3L, 1L)).isTrue(),
                () -> assertThat(voteRepository.existsByVotingIdAndUserIdAndOptionId(1L, 3L, 2L)).isFalse()
        );
    }

    @Test
    @DisplayName("Should count total votes for voting")
    void testCountByVotingId() {
        long count = voteRepository.countByVotingId(1L);
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should find votes by user ID")
    void testFindByUserId() {
        List<Vote> votes = voteRepository.findByUserId(3L);
        assertThat(votes).allSatisfy(v -> assertThat(v.getUser().getId()).isEqualTo(3L));
    }

    @Test
    @DisplayName("Should find votes by voting ID")
    void testFindByVotingId() {
        List<Vote> votes = voteRepository.findByVotingId(1L);
        assertThat(votes).allSatisfy(v -> assertThat(v.getVoting().getId()).isEqualTo(1L));
    }
}

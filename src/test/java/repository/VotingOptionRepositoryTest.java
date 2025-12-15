package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.VotingOption;
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
class VotingOptionRepositoryTest {

    @Autowired
    private VotingOptionRepository votingOptionRepository;

    @Test
    @DisplayName("Should find all voting options by voting ID")
    void testFindByVotingId() {
        List<VotingOption> options = votingOptionRepository.findByVotingId(1L);

        assertAll("Voting options",
                () -> assertThat(options).isNotEmpty(),
                () -> assertThat(options).allSatisfy(o -> assertThat(o.getVoting().getId()).isEqualTo(1L))
        );
    }

    @Test
    @DisplayName("Should count voting options by voting ID")
    void testCountByVotingId() {
        long count = votingOptionRepository.countByVotingId(1L);
        assertThat(count).isEqualTo(2);

        long expiredCount = votingOptionRepository.countByVotingId(2L);
        assertThat(expiredCount).isEqualTo(0); // jeśli dla "Test Voting 2 Expired" nie dodano opcji w data.sql
    }
}

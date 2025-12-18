package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.MeetingVoting;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.UserRole;
import com.meethub.domain.model.request.UserRegistrationRequest;
import com.meethub.domain.service.impl.AuthServiceImpl;
import com.meethub.security.CustomUserDetailsService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

@DataJpaTest
@ActiveProfiles("postgres")
class MeetingVotingRepositoryTest {

    @Autowired
    private MeetingVotingRepository meetingVotingRepository;

    @Test
    void testFindByMeetingId() {
        Long meetingId = 1L;
        List<MeetingVoting> votings = meetingVotingRepository.findByMeetingId(meetingId);

        assertAll("Votings by meeting ID",
                () -> assertThat(votings).hasSize(2)
        );
    }

    @Test
    void testFindExpiredVotings() {
        List<MeetingVoting> expired = meetingVotingRepository.findExpiredVotings(LocalDateTime.now());

        assertAll("Expired votings",
                () -> assertThat(expired).hasSize(1),
                () -> assertThat(expired.get(0).getTitle()).isEqualTo("Test Voting 2 Expired")
        );
    }

    @Test
    void testHasActiveVoting() {
        Long meetingId = 1L;
        boolean hasActive = meetingVotingRepository.hasActiveVoting(meetingId);

        assertAll("Check if meeting has active voting",
                () -> assertThat(hasActive).isTrue()
        );
    }

}

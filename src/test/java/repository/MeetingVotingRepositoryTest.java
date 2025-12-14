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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DataJpaTest
class MeetingVotingRepositoryTest {

    @Autowired
    private MeetingVotingRepository meetingVotingRepository;

    @Test
    void testFindByMeetingId() {
        Long meetingId = 1L; // Zakładamy, że Test Meeting ma ID=1 w data.sql
        List<MeetingVoting> votings = meetingVotingRepository.findByMeetingId(meetingId);
        assertThat(votings).hasSize(2); // Test Voting 1 + Test Voting 2 Expired
    }

    @Test
    void testFindExpiredVotings() {
        List<MeetingVoting> expired = meetingVotingRepository.findExpiredVotings(LocalDateTime.now());
        assertThat(expired).hasSize(1); // Tylko Test Voting 2 Expired
        assertThat(expired.get(0).getTitle()).isEqualTo("Test Voting 2 Expired");
    }

    @Test
    void testHasActiveVoting() {
        Long meetingId = 1L; // Test Meeting
        boolean hasActive = meetingVotingRepository.hasActiveVoting(meetingId);
        assertThat(hasActive).isTrue(); // Test Voting 1 jest aktywne
    }

    @Disabled
    @SpringBootTest
    //@ActiveProfiles("test")
    @Transactional
    static
    class AuthServiceIntegrationTest {

        @Autowired
        private AuthServiceImpl authService;

        @Autowired
        private UserRepository userRepository;

        @MockBean
        private PasswordEncoder passwordEncoder;

        @MockBean
        private CustomUserDetailsService userDetailsService;

        @Test
        void register_ShouldSaveUserToDatabase() {
            // Given
            UserRegistrationRequest request = UserRegistrationRequest.builder()
                    .email("integration@test.com")
                    .password("password123")
                    .confirmPassword("password123")
                    .firstName("Integration")
                    .lastName("Test")
                    .phoneNumber("987654321")
                    .build();

            when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

            // When
            var result = authService.register(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("integration@test.com");

            // Verify in database
            User savedUser = userRepository.findByEmail("integration@test.com").orElse(null);
            assertThat(savedUser).isNotNull();
            assertThat(savedUser.getFirstName()).isEqualTo("Integration");
            assertThat(savedUser.getRole()).isEqualTo(UserRole.PARTICIPANT);
        }
    }
}

package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.MeetingParticipant;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.MeetingType;
import com.meethub.domain.model.enums.MeetingVisibility;
import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.model.enums.PermissionLevel;
import com.meethub.domain.model.projection.ParticipantProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@DataJpaTest
class MeetingParticipantRepositoryTest {

    @Autowired
    private MeetingParticipantRepository repository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    private User user;
    private Meeting meeting;
    private MeetingParticipant participant;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .password("pass")
                .build();
        userRepository.save(user);

        meeting = Meeting.builder()
                .title("Test Meeting")
                .type(MeetingType.PHYSICAL)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(1).plusHours(2))
                .organizer(user)
                .visibility(MeetingVisibility.PUBLIC)
                .build();
        meetingRepository.save(meeting);

        participant = MeetingParticipant.builder()
                .meeting(meeting)
                .user(user)
                .status(ParticipationStatus.CONFIRMED)
                .permissionLevel(PermissionLevel.PARTICIPANT)
                .invitationToken("TOKEN123")
                .build();
        repository.save(participant);
    }

    @Test
    @DisplayName("should find participant by invitation token")
    void shouldFindByInvitationToken() {
        Optional<MeetingParticipant> found = repository.findByInvitationToken("TOKEN123");

        assertThat(found).isPresent();
    }

    @Test
    @DisplayName("should check if participant exists by meeting and user")
    void shouldExistByMeetingIdAndUserId() {
        boolean exists = repository.existsByMeetingIdAndUserId(meeting.getId(), user.getId());

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("should count participants by meeting and status")
    void shouldCountByMeetingIdAndStatus() {
        long count = repository.countByMeetingIdAndStatus(meeting.getId(), ParticipationStatus.CONFIRMED);

        assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("should return participant projections")
    void shouldFindParticipantsProjection() {
        List<ParticipantProjection> projections = repository.findParticipantsProjection(meeting.getId());

        assertAll(
                () -> assertThat(projections).hasSize(1),
                () -> assertThat(projections.get(0).getEmail()).isEqualTo("john@example.com"),
                () -> assertThat(projections.get(0).getStatus()).isEqualTo("CONFIRMED")
        );
    }
}

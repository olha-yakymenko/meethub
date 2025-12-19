package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.dto.ParticipantCountDto;
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
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@DataJpaTest
@ActiveProfiles("postgres")
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



    @Test
    @DisplayName("should return empty list for non-existent meeting")
    void shouldReturnEmptyListForNonExistentMeeting() {
        List<ParticipantProjection> projections = repository.findParticipantsProjection(-1L);

        assertAll(
                () -> assertThat(projections).isEmpty(),
                () -> assertThat(projections).isNotNull()
        );
    }


    // Test dla interfejsu ParticipantProjection (unit test)
    @Test
    @DisplayName("ParticipantProjection default method should work correctly")
    void participantProjectionDefaultMethodTest() {
        // Tworzenie mocka interfejsu
        ParticipantProjection projection = new ParticipantProjection() {
            @Override
            public Long getId() { return 1L; }

            @Override
            public String getUsername() { return "johndoe"; }

            @Override
            public String getEmail() { return "john@example.com"; }

            @Override
            public String getStatus() { return "CONFIRMED"; }

            @Override
            public String getFirstName() { return "John"; }

            @Override
            public String getLastName() { return "Doe"; }
        };

        assertAll(
                () -> assertThat(projection.getId()).isEqualTo(1L),
                () -> assertThat(projection.getEmail()).isEqualTo("john@example.com"),
                () -> assertThat(projection.getStatus()).isEqualTo("CONFIRMED"),
                () -> assertThat(projection.getUsername()).isEqualTo("johndoe"),
                () -> assertThat(projection.getFirstName()).isEqualTo("John"),
                () -> assertThat(projection.getLastName()).isEqualTo("Doe"),
                () -> assertThat(projection.getFullName()).isEqualTo("John Doe"),
                () -> assertThat(projection.getFullName())
                        .contains("John")
                        .contains("Doe")
                        .contains(" ")
        );
    }

    @Test
    @DisplayName("ParticipantProjection should handle null names in default method")
    void participantProjectionShouldHandleNullNames() {
        ParticipantProjection projectionWithNullNames = new ParticipantProjection() {
            @Override
            public Long getId() { return 2L; }

            @Override
            public String getUsername() { return "noname"; }

            @Override
            public String getEmail() { return "noname@example.com"; }

            @Override
            public String getStatus() { return "PENDING"; }

            @Override
            public String getFirstName() { return null; }

            @Override
            public String getLastName() { return null; }
        };

        assertAll(
                () -> assertThat(projectionWithNullNames.getFullName()).isEqualTo("null null"),
                () -> assertThat(projectionWithNullNames.getFirstName()).isNull(),
                () -> assertThat(projectionWithNullNames.getLastName()).isNull()
        );
    }

    @Test
    @DisplayName("ParticipantProjection should handle partial names")
    void participantProjectionShouldHandlePartialNames() {
        ParticipantProjection projectionWithPartialName = new ParticipantProjection() {
            @Override
            public Long getId() { return 3L; }

            @Override
            public String getUsername() { return "firstonly"; }

            @Override
            public String getEmail() { return "first@example.com"; }

            @Override
            public String getStatus() { return "ACCEPTED"; }

            @Override
            public String getFirstName() { return "FirstOnly"; }

            @Override
            public String getLastName() { return null; }
        };

        ParticipantProjection projectionWithLastNameOnly = new ParticipantProjection() {
            @Override
            public Long getId() { return 4L; }

            @Override
            public String getUsername() { return "lastonly"; }

            @Override
            public String getEmail() { return "last@example.com"; }

            @Override
            public String getStatus() { return "DECLINED"; }

            @Override
            public String getFirstName() { return null; }

            @Override
            public String getLastName() { return "LastOnly"; }
        };

        assertAll(
                () -> assertThat(projectionWithPartialName.getFullName()).isEqualTo("FirstOnly null"),
                () -> assertThat(projectionWithLastNameOnly.getFullName()).isEqualTo("null LastOnly")
        );
    }

    @Test
    @DisplayName("should find participants by meeting ID")
    void shouldFindByMeetingId() {
        List<MeetingParticipant> participants = repository.findByMeetingId(meeting.getId());

        assertThat(participants).hasSize(1);
        assertThat(participants).extracting(MeetingParticipant::getStatus)
                .containsExactlyInAnyOrder(ParticipationStatus.CONFIRMED);
    }

    @Test
    @DisplayName("should find participants by user ID")
    void shouldFindByUserId() {
        List<MeetingParticipant> participants = repository.findByUserId(user.getId());

        assertThat(participants).hasSize(1);
        assertThat(participants).extracting(MeetingParticipant::getMeeting)
                .extracting(Meeting::getTitle)
                .containsExactlyInAnyOrder("Test Meeting");
    }

    @Test
    @DisplayName("should find participants by meeting ID and status")
    void shouldFindByMeetingIdAndStatus() {
        List<MeetingParticipant> participants = repository.findByMeetingIdAndStatus(meeting.getId(), ParticipationStatus.CONFIRMED);

        assertThat(participants).hasSize(1);
        assertThat(participants.get(0).getUser().getEmail()).isEqualTo("john@example.com");
    }


    @Test
    @DisplayName("should count participants by meeting organizer ID")
    void shouldCountByMeetingOrganizerId() {
        long count = repository.countByMeetingOrganizerId(user.getId());

        assertThat(count).isEqualTo(1L); // user jest organizatorem meeting, ale nie meeting2
    }

    @Test
    @DisplayName("should find participant by meeting ID and user ID")
    void shouldFindByMeetingIdAndUserId() {
        Optional<MeetingParticipant> found = repository.findByMeetingIdAndUserId(meeting.getId(), user.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(ParticipationStatus.CONFIRMED);
    }

    @Test
    @DisplayName("should find participants by user ID and status")
    void shouldFindByUserIdAndStatus() {
        List<MeetingParticipant> participants = repository.findByUserIdAndStatus(user.getId(), ParticipationStatus.CONFIRMED);

        assertThat(participants).hasSize(1);
        assertThat(participants.get(0).getMeeting().getTitle()).isEqualTo("Test Meeting");
    }

    @Test
    @DisplayName("should count participants by meeting ID")
    void shouldCountByMeetingId() {
        long count = repository.countByMeetingId(meeting.getId());

        assertThat(count).isEqualTo(1L);
    }


    @Test
    @DisplayName("should count participants by meeting ID and status in list")
    void shouldCountByMeetingIdAndStatusIn() {
        List<ParticipationStatus> statuses = Arrays.asList(ParticipationStatus.CONFIRMED, ParticipationStatus.ATTENDED);
        long count = repository.countByMeetingIdAndStatusIn(meeting.getId(), statuses);

        assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("should check if participant exists by meeting ID, user ID and status")
    void shouldExistByMeetingIdAndUserIdAndStatus() {
        boolean exists = repository.existsByMeetingIdAndUserIdAndStatus(
                meeting.getId(),
                user.getId(),
                ParticipationStatus.CONFIRMED
        );

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("should check if participant exists by meeting ID, user ID and status in list")
    void shouldExistByMeetingIdAndUserIdAndStatusIn() {
        List<ParticipationStatus> statuses = Arrays.asList(ParticipationStatus.CONFIRMED, ParticipationStatus.PENDING);
        boolean exists = repository.existsByMeetingIdAndUserIdAndStatusIn(
                meeting.getId(),
                user.getId(),
                statuses
        );

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("should find all participants by meeting ID with ordering")
    void shouldFindAllParticipantsByMeetingId() {
        List<ParticipantProjection> projections = repository.findAllParticipantsByMeetingId(meeting.getId());

        assertAll(
                () -> assertThat(projections).hasSize(1),
                () -> assertThat(projections.get(0).getStatus()).isEqualTo("CONFIRMED"),
                () -> assertThat(projections.get(0).getEmail()).isEqualTo("john@example.com")
        );
    }

    @Test
    @DisplayName("should get participant counts DTO")
    void shouldGetParticipantCounts() {
        ParticipantCountDto counts = repository.getParticipantCounts(meeting.getId());

        assertAll(
                () -> assertThat(counts.getTotal()).isEqualTo(1L),
                () -> assertThat(counts.getAttended()).isEqualTo(0L),
                () -> assertThat(counts.getDeclined()).isEqualTo(0L),
                () -> assertThat(counts.getCancelled()).isEqualTo(0L),
                () -> assertThat(counts.getInvited()).isEqualTo(0L)
        );
    }

    @Test
    @DisplayName("should find participant by ID and invitation token")
    void shouldFindByIdAndInvitationToken() {
        Optional<MeetingParticipant> found = repository.findByIdAndInvitationToken(
                participant.getId(),
                "TOKEN123"
        );

        assertThat(found).isPresent();
        assertThat(found.get().getUser().getEmail()).isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("should return empty when participant not found by ID and wrong token")
    void shouldReturnEmptyWhenNotFoundByIdAndWrongToken() {
        Optional<MeetingParticipant> found = repository.findByIdAndInvitationToken(
                participant.getId(),
                "WRONG_TOKEN"
        );

        assertThat(found).isEmpty();
    }

}

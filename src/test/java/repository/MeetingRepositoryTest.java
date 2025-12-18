package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.MeetingType;
import com.meethub.domain.model.enums.MeetingVisibility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("postgres")
class MeetingRepositoryTest {

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testFindUpcomingPublicMeetings() {
        List<Meeting> meetings = meetingRepository.findUpcomingPublicMeetings(LocalDateTime.now());

        assertAll("Upcoming public meetings",
                () -> assertThat(meetings).isNotEmpty(),
                () -> assertThat(meetings).allMatch(m -> m.getVisibility() == MeetingVisibility.PUBLIC)
        );
    }

    @Test
    void testFindByTitle() {
        Optional<Meeting> meeting = meetingRepository.findByTitle("Test Meeting");

        assertAll("Find by title",
                () -> assertThat(meeting).isPresent(),
                () -> meeting.ifPresent(m -> assertThat(m.getType()).isEqualTo(MeetingType.PHYSICAL))
        );
    }

    @Test
    void testCountUpcomingMeetingsByUserId() {
        User user = userRepository.findByEmail("test.user@example.com").orElseThrow();
        Long count = meetingRepository.countUpcomingMeetingsByUserId(user.getId());

        assertAll("Count upcoming meetings by user",
                () -> assertNotNull(count),
                () -> assertThat(count).isEqualTo(1)
        );
    }

    @Test
    void testFindByIdAndOrganizerId() {
        User organizer = userRepository.findByEmail("test.organizer@example.com").orElseThrow();
        Meeting meeting = meetingRepository.findByTitle("Test Meeting").orElseThrow();
        Optional<Meeting> found = meetingRepository.findByIdAndOrganizerId(meeting.getId(), organizer.getId());

        assertAll("Find by ID and organizer ID",
                () -> assertThat(found).isPresent(),
                () -> found.ifPresent(m -> assertThat(m.getTitle()).isEqualTo("Test Meeting"))
        );
    }

    @Test
    void testFindAllMeetingIds() {
        List<Long> ids = meetingRepository.findAllMeetingIds();

        assertAll("Find all meeting IDs",
                () -> assertThat(ids).isNotEmpty(),
                () -> assertThat(ids).hasSize(1)
        );
    }

    @Test
    void testIsUserOrganizer() {
        User organizer = userRepository.findByEmail("test.organizer@example.com").orElseThrow();
        Meeting meeting = meetingRepository.findByTitle("Test Meeting").orElseThrow();
        boolean isOrganizer = meetingRepository.isUserOrganizer(meeting.getId(), organizer.getId());

        assertAll("Check if user is organizer",
                () -> assertTrue(isOrganizer)
        );
    }
}

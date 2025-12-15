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
        assertThat(meetings).isNotEmpty();
        // Wszystkie powinny mieć PUBLIC visibility
        assertThat(meetings).allMatch(m -> m.getVisibility() == MeetingVisibility.PUBLIC);
    }

    @Test
    void testFindByTitle() {
        Optional<Meeting> meeting = meetingRepository.findByTitle("Test Meeting");
        assertThat(meeting).isPresent();
        assertThat(meeting.get().getType()).isEqualTo(MeetingType.PHYSICAL);
    }

    @Test
    void testCountUpcomingMeetingsByUserId() {
        User user = userRepository.findByEmail("test.user@example.com").orElseThrow();
        Long count = meetingRepository.countUpcomingMeetingsByUserId(user.getId());
        assertThat(count).isEqualTo(1); // bo w data.sql tylko 1 meeting dla tego usera jako participant
    }

    @Test
    void testFindByIdAndOrganizerId() {
        User organizer = userRepository.findByEmail("test.organizer@example.com").orElseThrow();
        Meeting meeting = meetingRepository.findByTitle("Test Meeting").orElseThrow();
        Optional<Meeting> found = meetingRepository.findByIdAndOrganizerId(meeting.getId(), organizer.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Test Meeting");
    }

    @Test
    void testFindAllMeetingIds() {
        List<Long> ids = meetingRepository.findAllMeetingIds();
        assertThat(ids).isNotEmpty();
        assertThat(ids).hasSize(1); // tylko jedno spotkanie w data.sql
    }

    @Test
    void testIsUserOrganizer() {
        User organizer = userRepository.findByEmail("test.organizer@example.com").orElseThrow();
        Meeting meeting = meetingRepository.findByTitle("Test Meeting").orElseThrow();
        boolean isOrganizer = meetingRepository.isUserOrganizer(meeting.getId(), organizer.getId());
        assertThat(isOrganizer).isTrue();
    }
}

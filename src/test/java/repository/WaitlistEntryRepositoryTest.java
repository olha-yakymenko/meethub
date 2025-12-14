package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.WaitlistEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@DataJpaTest
class WaitlistEntryRepositoryTest {

    @Autowired
    private WaitlistEntryRepository waitlistEntryRepository;

    @Test
    @DisplayName("Should find max position by meeting ID")
    void testFindMaxPositionByMeetingId() {
        Optional<Integer> maxPosition = waitlistEntryRepository.findMaxPositionByMeetingId(1L);
        assertThat(maxPosition).isPresent();
    }

    @Test
    @DisplayName("Should check if waitlist entry exists for meeting and user")
    void testExistsByMeetingIdAndUserId() {
        boolean exists = waitlistEntryRepository.existsByMeetingIdAndUserId(1L, 3L);
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should find first waitlist entry by meeting ID ordered by position")
    void testFindFirstByMeetingIdOrderByPositionAsc() {
        Optional<WaitlistEntry> firstEntry = waitlistEntryRepository.findFirstByMeetingIdOrderByPositionAsc(1L);
        assertThat(firstEntry).isPresent();
    }

    @Test
    @DisplayName("Should find waitlist entries for user")
    void testFindByUserId() {
        List<WaitlistEntry> entries = waitlistEntryRepository.findByUserId(3L);
        assertThat(entries).isNotEmpty();
    }

    @Test
    @DisplayName("Should find waitlist entries by meeting ID and position greater than given")
    void testFindByMeetingIdAndPositionGreaterThan() {
        List<WaitlistEntry> entries = waitlistEntryRepository.findByMeetingIdAndPositionGreaterThan(1L, 0);
        assertThat(entries).isNotEmpty();
    }

    @Test
    @DisplayName("Should find waitlist entry by meeting ID and user ID")
    void testFindByMeetingIdAndUserId() {
        Optional<WaitlistEntry> entry = waitlistEntryRepository.findByMeetingIdAndUserId(1L, 3L);
        assertThat(entry).isPresent();
    }
}

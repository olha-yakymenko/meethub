package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.AttendanceToken;
import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.AttendanceTokenStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("postgres")
//@Sql(scripts = "/data.sql")
class AttendanceTokenRepositoryTest {

    @Autowired
    private AttendanceTokenRepository repository;
    @Autowired
    private MeetingRepository meetingRepository;
    @Autowired
    private UserRepository userRepository;

    /* ---------- CREATE ---------- */

    @Test
    @DisplayName("should save attendance token")
    void shouldSaveAttendanceToken() {
        Meeting meeting = meetingRepository.findByTitle("Test Meeting")
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        AttendanceToken token = AttendanceToken.builder()
                .token("NEW_TOKEN")
                .status(AttendanceTokenStatus.ACTIVE)
                .user(User.builder().id(1L).build())
                .meeting(meeting) // <- używamy istniejącego spotkania
                .expiresAt(LocalDateTime.now().plusHours(24)) // <- ważne, inaczej NULL
                .createdAt(LocalDateTime.now()) // jeśli pole też nie może być NULL
                .build();

        AttendanceToken saved = repository.save(token);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getToken()).isEqualTo("NEW_TOKEN");
    }

    /* ---------- READ ---------- */

    @Test
    void shouldFindByTokenAndMeetingId() {
        Optional<AttendanceToken> result =
                repository.findByTokenAndMeetingId("TOKEN_ACTIVE_123", 1L);

        assertThat(result).isPresent();
    }

    @Test
    void shouldReturnEmptyWhenTokenNotExists() {
        Optional<AttendanceToken> result =
                repository.findByTokenAndMeetingId("INVALID", 1L);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldFindByUserIdAndMeetingId() {
        Optional<AttendanceToken> result =
                repository.findByUserIdAndMeetingId(1L, 1L);

        assertThat(result).isPresent();
    }

    /* ---------- CUSTOM QUERY ---------- */

    @Test
    void shouldFindActiveByUserAndMeeting() {
        // Pobieramy istniejącego użytkownika i spotkanie z bazy testowej
        User user = userRepository.findByEmail("test.user@example.com")
                .orElseThrow(() -> new RuntimeException("User not found"));
        Meeting meeting = meetingRepository.findByTitle("Test Meeting")
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        // Wyszukujemy aktywny token dla użytkownika i spotkania
        Optional<AttendanceToken> result =
                repository.findActiveByUserAndMeeting(user.getId(), meeting.getId());

        // Assercje
        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(AttendanceTokenStatus.ACTIVE);
    }


    @Test
    void shouldNotReturnInactiveToken() {
        Optional<AttendanceToken> result =
                repository.findActiveByUserAndMeeting(2L, 1L);

        assertThat(result).isEmpty();
    }

    /* ---------- UPDATE ---------- */

    @Test
    void shouldUpdateTokenStatus() {
        AttendanceToken token =
                repository.findById(1L).orElseThrow();

        token.setStatus(AttendanceTokenStatus.USED);
        repository.save(token);

        AttendanceToken updated =
                repository.findById(1L).orElseThrow();

        assertThat(updated.getStatus())
                .isEqualTo(AttendanceTokenStatus.USED);
    }

    /* ---------- DELETE ---------- */

    @Test
    void shouldDeleteToken() {
        repository.deleteById(1L);

        Optional<AttendanceToken> result =
                repository.findById(1L);

        assertThat(result).isEmpty();
    }

    /* ---------- EXISTS ---------- */

    @Test
    void shouldCheckIfTokenExists() {
        boolean exists = repository.existsById(1L);
        assertThat(exists).isTrue();
    }

    @Test
    void shouldReturnFalseWhenTokenDoesNotExist() {
        boolean exists = repository.existsById(999L);
        assertThat(exists).isFalse();
    }
}

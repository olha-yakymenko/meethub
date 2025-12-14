package com.meethub.domain.repository.jpa;

import com.meethub.domain.model.entity.Feedback;
import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.MeetingStatus;
import com.meethub.domain.model.enums.MeetingType;
import com.meethub.domain.model.enums.MeetingVisibility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class FeedbackRepositoryTest {

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private UserRepository userRepository;

    private Meeting meeting;
    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("user@example.com")
                .firstName("John")
                .lastName("Doe")
                .password("pass")
                .build();
        userRepository.save(user);

        // przypisujemy do pola klasy, nie tworzymy nowej zmiennej lokalnej
        meeting = Meeting.builder()
                .title("Test Meeting")
                .description("Test Description")
                .type(MeetingType.PHYSICAL) // <- obowiązkowe
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(1).plusHours(2))
                .organizer(user) // jeśli wymagane
                .visibility(MeetingVisibility.PUBLIC)
                .build();
        meetingRepository.save(meeting);

        // przykładowy feedback
        Feedback feedback = Feedback.builder()
                .meeting(meeting)
                .user(user)
                .rating(4)
                .comment("Great meeting!")
                .build();
        feedbackRepository.save(feedback);
    }


    @Test
    @DisplayName("should save feedback")
    void shouldSaveFeedback() {
        User newUser = User.builder()
                .email("newuser@example.com")
                .firstName("Jane")
                .lastName("Doe")
                .password("pass")
                .build();
        userRepository.save(newUser);

        Feedback feedback = Feedback.builder()
                .meeting(meeting)  // używamy istniejącego spotkania
                .user(newUser)     // nowy użytkownik
                .rating(5)
                .comment("Excellent")
                .build();

        Feedback saved = feedbackRepository.save(feedback);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getRating()).isEqualTo(5);
        assertThat(saved.getComment()).isEqualTo("Excellent");
    }


    @Test
    @DisplayName("should find feedback by meeting id")
    void shouldFindByMeetingId() {
        List<Feedback> feedbacks = feedbackRepository.findByMeetingId(meeting.getId());

        assertThat(feedbacks).isNotEmpty();
        assertThat(feedbacks.get(0).getMeeting().getId()).isEqualTo(meeting.getId());
    }

    @Test
    @DisplayName("should find feedback by user id")
    void shouldFindByUserId() {
        List<Feedback> feedbacks = feedbackRepository.findByUserId(user.getId());

        assertThat(feedbacks).isNotEmpty();
        assertThat(feedbacks.get(0).getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("should find feedback by meeting id and user id")
    void shouldFindByMeetingIdAndUserId() {
        Optional<Feedback> feedback = feedbackRepository.findByMeetingIdAndUserId(meeting.getId(), user.getId());

        assertThat(feedback).isPresent();
        assertThat(feedback.get().getMeeting().getId()).isEqualTo(meeting.getId());
        assertThat(feedback.get().getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("should calculate average rating")
    void shouldFindAverageRatingByMeetingId() {
        Double average = feedbackRepository.findAverageRatingByMeetingId(meeting.getId());

        assertThat(average).isEqualTo(4.0);
    }

    @Test
    @DisplayName("should count feedback by meeting id")
    void shouldCountByMeetingId() {
        Long count = feedbackRepository.countByMeetingId(meeting.getId());

        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("should delete feedback")
    void shouldDeleteFeedback() {
        Feedback feedback = feedbackRepository.findByMeetingIdAndUserId(meeting.getId(), user.getId()).orElseThrow();
        feedbackRepository.delete(feedback);

        Optional<Feedback> deleted = feedbackRepository.findById(feedback.getId());
        assertThat(deleted).isEmpty();
    }
}

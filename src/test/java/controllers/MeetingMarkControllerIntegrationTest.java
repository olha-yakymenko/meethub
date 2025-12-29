package com.meethub.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.*;
import com.meethub.domain.repository.jpa.MeetingRepository;
import com.meethub.domain.repository.jpa.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import security.WithCustomUser;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
@Slf4j
class MeetingMarkControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MeetingRepository meetingRepository;

    private User testUser;
    private Meeting testMeeting;

    @BeforeEach
    void setUp() {
        meetingRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .firstName("Jan")
                .lastName("Kowalski")
                .email("organizer@example.com")
                .password(passwordEncoder.encode("password123"))
                .role(UserRole.PARTICIPANT)
                .enabled(true)
                .build();
        testUser = userRepository.save(testUser);

        // Tworzymy spotkanie z tym użytkownikiem jako organizatorem
        testMeeting = Meeting.builder()
                .title("Testowe Spotkanie")
                .description("Spotkanie testowe dla zasobów")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(1).plusHours(2))
                .organizer(testUser)  // Używamy testUser jako organizatora
                .type(MeetingType.ONLINE)
                .visibility(MeetingVisibility.PUBLIC)
                .maxParticipants(20)
                .build();
        testMeeting = meetingRepository.save(testMeeting);

        log.info("Setup: Meeting ID = {}", testMeeting.getId());
        log.info("Setup: User ID = {}", testUser.getId());
    }

    @Test
    @WithCustomUser(id = 1L, email = "user@example.com")
    void toggleImportant_ShouldToggleImportantStatus() throws Exception {
        mockMvc.perform(post("/meetings/{meetingId}/important/toggle", testMeeting.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/meetings/" + testMeeting.getId()))
                .andExpect(flash().attribute("success", "Spotkanie oznaczone jako ważne"));
    }

    @Test
    @WithCustomUser(id = 1L, email = "test@example.com")
    void unmarkAsImportant_ShouldUnmarkMeeting() throws Exception {
        // Najpierw oznacz jako ważne
        mockMvc.perform(post("/meetings/{meetingId}/important", testMeeting.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":" + testUser.getId() + "}"));

        // Potem odznacz
        mockMvc.perform(delete("/meetings/{meetingId}/important", testMeeting.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/meetings/" + testMeeting.getId()))
                .andExpect(flash().attribute("success", "Spotkanie odznaczone z ważnych"));
    }




    @Test
    void markAsImportant_Unauthenticated_ShouldRedirect() throws Exception {
        mockMvc.perform(post("/meetings/{meetingId}/important", testMeeting.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1}"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void getMeeting_ShouldReturnMeetingDetails() throws Exception {
        mockMvc.perform(get("/api/v1/meetings/{meetingId}", testMeeting.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.title", is("Testowe Spotkanie")));
    }

}
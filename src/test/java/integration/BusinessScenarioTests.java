//package com.meethub.controller.business;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.meethub.domain.model.request.CreateMeetingRequest;
//import com.meethub.domain.model.request.InviteParticipantsRequest;
//import com.meethub.domain.model.request.SubmitFeedbackRequest;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//import java.util.Arrays;
//
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//@ActiveProfiles("test")
//@Transactional
//class BusinessScenarioTests {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    /**
//     * Scenariusz 1: Organizator tworzy spotkanie, zaprasza uczestników,
//     * uczestnicy akceptują, spotkanie się odbywa, uczestnicy wystawiają opinie
//     */
//    @Test
//    @WithMockUser(username = "organizer", roles = {"USER"})
//    void scenario1_CompleteMeetingLifecycle() throws Exception {
//        // 1. Tworzenie spotkania
//        String meetingId = createPublicMeeting();
//
//        // 2. Zapraszanie uczestników
//        inviteParticipants(meetingId);
//
//        // 3. Pobieranie listy uczestników (weryfikacja)
//

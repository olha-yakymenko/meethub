////package com.meethub.controller.api;
////
////import org.springframework.http.HttpStatus;
////import org.springframework.http.ResponseEntity;
////import org.springframework.web.bind.annotation.*;
////
////import java.util.HashMap;
////import java.util.Map;
////
////@RestController
////@RequestMapping("/api/test")
////public class TestController {
////
////    @GetMapping("/health")
////    public Map<String, String> health() {
////        Map<String, String> response = new HashMap<>();
////        response.put("status", "OK");
////        response.put("message", "MeetHub API is running!");
////        response.put("timestamp", java.time.LocalDateTime.now().toString());
////        return response;
////    }
////
////    @GetMapping("/all-methods")
////    public Map<String, String> testAllMethods() {
////        Map<String, String> response = new HashMap<>();
////        response.put("method", "GET");
////        response.put("status", "OK");
////        return response;
////    }
////
////    @PostMapping("/all-methods")
////    public Map<String, String> testAllMethodsPost(@RequestBody Map<String, String> request) {
////        Map<String, String> response = new HashMap<>();
////        response.put("method", "POST");
////        response.put("status", "OK");
////        response.put("received", request.get("test"));
////        return response;
////    }
////
////    @PutMapping("/all-methods")
////    public Map<String, String> testAllMethodsPut(@RequestBody Map<String, String> request) {
////        Map<String, String> response = new HashMap<>();
////        response.put("method", "PUT");
////        response.put("status", "OK");
////        response.put("received", request.get("test"));
////        return response;
////    }
////
////    @DeleteMapping("/all-methods")
////    public ResponseEntity<Map<String, String>> testAllMethodsDelete() {
////        Map<String, String> response = new HashMap<>();
////        response.put("method", "DELETE");
////        response.put("status", "OK");
////        return ResponseEntity.ok(response);
////    }
////
////    @PatchMapping("/all-methods")
////    public Map<String, String> testAllMethodsPatch(@RequestBody Map<String, String> request) {
////        Map<String, String> response = new HashMap<>();
////        response.put("method", "PATCH");
////        response.put("status", "OK");
////        response.put("received", request.get("test"));
////        return response;
////    }
////
////    @GetMapping("/error-test")
////    public ResponseEntity<Map<String, String>> errorTest() {
////        Map<String, String> response = new HashMap<>();
////        response.put("status", "ERROR");
////        response.put("message", "This is a test error");
////        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
////    }
////
////    @GetMapping("/auth-test")
////    public Map<String, String> authTest() {
////        Map<String, String> response = new HashMap<>();
////        response.put("endpoint", "Authentication Test");
////        response.put("status", "OK - No auth required");
////        return response;
////    }
////}
//
//
//import com.meethub.domain.model.entity.User;
//import com.meethub.domain.model.enums.UserRole;
//import com.meethub.domain.model.request.CreateMeetingRequest;
//import com.meethub.domain.model.response.MeetingResponse;
//import com.meethub.domain.repository.jpa.NotificationRepository;
//import com.meethub.domain.repository.jpa.UserRepository;
//import com.meethub.domain.service.MeetingSchedulerService;
//import com.meethub.domain.service.MeetingService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.annotation.Profile;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.time.LocalDateTime;
//import java.util.Map;
//
//// TestController.java (tylko dla developmentu)
//@RestController
//@RequestMapping("/api/test")
//@Profile("dev")
//@RequiredArgsConstructor
//public class TestController {
//
//    private final MeetingService meetingService;
//    private final UserRepository userRepository;
//    private final MeetingSchedulerService schedulerService;
//    private final NotificationRepository notificationRepository;
//
//    @PostMapping("/create-test-meeting")
//    public ResponseEntity<?> createTestMeeting() {
//        // 1. Znajdź lub utwórz testowego użytkownika
//        User organizer = userRepository.findByEmail("test@example.com")
//                .orElseGet(() -> userRepository.save(User.builder()
//                        .email("test@example.com")
//                        .firstName("Test")
//                        .lastName("User")
//                        .password("password")
//                        .role(UserRole.ORGANIZER)
//                        .build()));
//
//        // 2. Utwórz spotkanie za 30 minut
//        CreateMeetingRequest request = new CreateMeetingRequest();
//        request.setTitle("Test Meeting - Reminder Check");
//        request.setDescription("This is a test meeting to check reminders");
//        request.setStartDate(LocalDateTime.now().plusMinutes(30));
//        request.setEndDate(LocalDateTime.now().plusMinutes(90));
//        request.setMaxParticipants(5);
//
//        // 3. Zapisz spotkanie
//        MeetingResponse meeting = meetingService.createMeeting(request, organizer.getId());
//
//        // 4. Dodaj uczestnika
//        User participant = userRepository.findByEmail("participant@example.com")
//                .orElseGet(() -> userRepository.save(User.builder()
//                        .email("participant@example.com")
//                        .firstName("Participant")
//                        .lastName("Test")
//                        .password("password")
//                        .role(UserRole.PARTICIPANT)
//                        .build()));
//
//        meetingService.addParticipant(meeting.getId(), participant.getId(), organizer.getId());
//
//        return ResponseEntity.ok(Map.of(
//                "message", "Test meeting created",
//                "meetingId", meeting.getId(),
//                "startsInMinutes", 30,
//                "remindersWillBeAt", "25, 20, 15, 10, 5 minutes before start"
//        ));
//    }
//}
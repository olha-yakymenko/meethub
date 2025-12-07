//// MeetingNotificationTestController.java
//package com.meethub.web.controllers;
//
//import com.meethub.domain.model.entity.*;
//import com.meethub.domain.model.enums.*;
//import com.meethub.domain.model.request.CreateMeetingRequest;
//import com.meethub.domain.service.*;
//import com.meethub.domain.repository.jpa.*;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.annotation.Profile;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.time.LocalDateTime;
//import java.util.*;
//
//@RestController
//@RequestMapping("/api/test/notifications")
//@Profile({"dev", "test"})
//@RequiredArgsConstructor
//@Slf4j
//public class MeetingNotificationTestController {
//
//    private final MeetingService meetingService;
//    private final MeetingParticipantService participantService;
//    private final MeetingSchedulerService schedulerService;
//    private final NotificationService notificationService;
//    private final UserRepository userRepository;
//    private final MeetingRepository meetingRepository;
//    private final NotificationRepository notificationRepository;
//    private final UserPreferenceRepository userPreferenceRepository;
//
//    /**
//     * 1. UTWÓRZ KOMPLETNY TEST: Spotkanie + Uczestnik + Sprawdź powiadomienia
//     */
//    @PostMapping("/full-test")
//    public ResponseEntity<?> runFullNotificationTest() {
//        log.info("🧪 Uruchamiam kompletny test systemu powiadomień...");
//
//        Map<String, Object> results = new HashMap<>();
//
//        try {
//            // A. Znajdź lub utwórz testowych użytkowników
//            User organizer = getOrCreateTestUser("organizer.test@meethub.com", "Test", "Organizer");
//            User participant = getOrCreateTestUser("participant.test@meethub.com", "Test", "Participant");
//
//            // B. Upewnij się, że użytkownicy mają włączone powiadomienia
//            enableAllNotifications(organizer);
//            enableAllNotifications(participant);
//
//            // C. Utwórz spotkanie za 15 minut (żeby szybko zobaczyć przypomnienia)
//            CreateMeetingRequest request = new CreateMeetingRequest();
//            request.setTitle("🧪 TEST: System Powiadomień");
//            request.setDescription("Spotkanie testowe do weryfikacji systemu powiadomień");
//            request.setStartDate(LocalDateTime.now().plusMinutes(15));
//            request.setEndDate(LocalDateTime.now().plusMinutes(75));
//            request.setVisibility(MeetingVisibility.PUBLIC);
//            request.setMaxParticipants(10);
//
//            var meetingResponse = meetingService.createMeeting(request, organizer.getId());
//            Meeting meeting = meetingRepository.findById(meetingResponse.getId())
//                    .orElseThrow(() -> new RuntimeException("Meeting not found"));
//
//            results.put("meetingCreated", Map.of(
//                    "id", meeting.getId(),
//                    "title", meeting.getTitle(),
//                    "startTime", meeting.getStartDate(),
//                    "organizer", organizer.getEmail()
//            ));
//
//            // D. Dodaj uczestnika
//            var participantResponse = participantService.inviteParticipant(
//                    meeting.getId(), participant.getId(), organizer.getId());
//
//            // E. Potwierdź udział uczestnika
//            participantService.updateParticipantStatus(
//                    meeting.getId(),
//                    participantResponse.getId(),
//                    ParticipationStatus.CONFIRMED,
//                    "Test acceptance",
//                    participant.getId()
//            );
//
//            results.put("participantAdded", Map.of(
//                    "userId", participant.getId(),
//                    "email", participant.getEmail(),
//                    "status", "CONFIRMED"
//            ));
//
//            // F. Sprawdź stan scheduler'a
//            var schedulerStatus = schedulerService.getSchedulerStatus();
//            results.put("schedulerStatus", schedulerStatus);
//
//            // G. Sprawdź zaplanowane powiadomienia w bazie
//            List<Notification> scheduledNotifications = notificationRepository
//                    .findByStatusAndScheduledForAfter(
//                            NotificationStatus.PENDING,
//                            LocalDateTime.now().minusHours(1)
//                    );
//
//            results.put("scheduledNotifications", scheduledNotifications.stream()
//                    .map(n -> Map.of(
//                            "id", n.getId(),
//                            "type", n.getType(),
//                            "channel", n.getChannel(),
//                            "scheduledFor", n.getScheduledFor(),
//                            "user", n.getUser().getEmail()
//                    ))
//                    .toList());
//
//            // H. Sprawdź powiadomienia IN_APP już utworzone
//            List<Notification> inAppNotifications = notificationRepository
//                    .findByUserIdAndChannelOrderByCreatedAtDesc(
//                            organizer.getId(),
//                            NotificationChannel.IN_APP
//                    );
//
//            results.put("inAppNotifications", inAppNotifications.size());
//
//            // I. Symuluj upływ czasu - sprawdź czy scheduler wyśle przypomnienia
//            scheduleManualCheck(meeting.getId(), 10); // Za 10 minut
//
//            results.put("testStatus", "SUCCESS");
//            results.put("message", """
//                ✅ Test został uruchomiony pomyślnie!
//
//                Co się stało:
//                1. Utworzono spotkanie na 15 minut od teraz
//                2. Dodano uczestnika i potwierdzono jego udział
//                3. Scheduler zaplanował powiadomienia
//                4. Utworzono powiadomienia IN_APP
//
//                Co sprawdzić:
//                1. Logi aplikacji - szukaj '🔔' i '📧'
//                2. Bazę danych - tabela notifications
//                3. Email testowy (jeśli skonfigurowany)
//                """);
//
//        } catch (Exception e) {
//            results.put("testStatus", "FAILED");
//            results.put("error", e.getMessage());
//            results.put("stackTrace", Arrays.toString(e.getStackTrace()));
//            log.error("❌ Test zakończony błędem", e);
//        }
//
//        return ResponseEntity.ok(results);
//    }
//
//    /**
//     * 2. SPRAWDŹ STAN SYSTEMU W CZASIE RZECZYWISTYM
//     */
//    @GetMapping("/system-status")
//    public ResponseEntity<?> getSystemStatus() {
//        Map<String, Object> status = new HashMap<>();
//
//        // Sprawdź scheduler
//        status.put("scheduler", schedulerService.getSchedulerStatus());
//
//        // Sprawdź spotkania zaplanowane na najbliższe 24h
//        LocalDateTime now = LocalDateTime.now();
//        LocalDateTime tomorrow = now.plusHours(24);
//
//        List<Meeting> upcomingMeetings = meetingRepository
//                .findByStatusAndStartDateBetween(MeetingStatus.PLANNED, now, tomorrow);
//
//        status.put("upcomingMeetings", upcomingMeetings.stream()
//                .map(m -> Map.of(
//                        "id", m.getId(),
//                        "title", m.getTitle(),
//                        "startTime", m.getStartDate(),
//                        "minutesUntilStart", java.time.Duration.between(now, m.getStartDate()).toMinutes()
//                ))
//                .toList());
//
//        // Sprawdź zaplanowane powiadomienia
//        long pendingNotifications = notificationRepository.countByStatus(NotificationStatus.PENDING);
//        status.put("pendingNotifications", pendingNotifications);
//
//        // Sprawdź ostatnie wysłane powiadomienia
//        List<Notification> recentNotifications = notificationRepository
//                .findTop10ByOrderByCreatedAtDesc();
//
//        status.put("recentNotifications", recentNotifications.stream()
//                .map(n -> Map.of(
//                        "id", n.getId(),
//                        "type", n.getType(),
//                        "channel", n.getChannel(),
//                        "status", n.getStatus(),
//                        "createdAt", n.getCreatedAt(),
//                        "user", n.getUser().getEmail()
//                ))
//                .toList());
//
//        return ResponseEntity.ok(status);
//    }
//
//    /**
//     * 3. SYMULACJA UPŁYWU CZASU - wyzwala natychmiastowe sprawdzenie
//     */
//    @PostMapping("/trigger-check/{meetingId}/{minutesBefore}")
//    public ResponseEntity<?> triggerReminderCheck(
//            @PathVariable Long meetingId,
//            @PathVariable int minutesBefore) {
//
//        Meeting meeting = meetingRepository.findById(meetingId)
//                .orElseThrow(() -> new RuntimeException("Meeting not found"));
//
//        log.info("⏰ Ręczne wywołanie przypomnienia dla spotkania {} ({} minut przed)",
//                meetingId, minutesBefore);
//
//        // Utwórz testowe powiadomienie
//        Map<String, String> variables = new HashMap<>();
//        variables.put("meetingTitle", meeting.getTitle());
//        variables.put("minutesBefore", String.valueOf(minutesBefore));
//        variables.put("meetingTime", meeting.getStartDate().toString());
//        variables.put("organizerName", meeting.getOrganizer().getFirstName());
//
//        // Wyślij testowe powiadomienie do organizatora
//        notificationService.createNotificationFromTemplate(
//                meeting.getOrganizer().getId(),
//                "meeting_reminder",
//                variables,
//                NotificationType.MEETING_REMINDER,
//                NotificationChannel.IN_APP
//        );
//
//        return ResponseEntity.ok(Map.of(
//                "message", "Wywołano testowe przypomnienie",
//                "meetingId", meetingId,
//                "minutesBefore", minutesBefore,
//                "notificationType", "MEETING_REMINDER"
//        ));
//    }
//
//    /**
//     * 4. SPRAWDŹ WSZYSTKIE ZAPLANOWANE POWIADOMIENIA DLA SPOTKANIA
//     */
//    @GetMapping("/meeting/{meetingId}/scheduled-notifications")
//    public ResponseEntity<?> getMeetingScheduledNotifications(@PathVariable Long meetingId) {
//        Meeting meeting = meetingRepository.findById(meetingId)
//                .orElseThrow(() -> new RuntimeException("Meeting not found"));
//
//        // Znajdź powiadomienia związane z tym spotkaniem
//        List<Notification> notifications = notificationRepository
//                .findByReferenceIdAndReferenceType(meetingId, "MEETING");
//
//        return ResponseEntity.ok(notifications.stream()
//                .map(n -> Map.of(
//                        "id", n.getId(),
//                        "type", n.getType(),
//                        "channel", n.getChannel(),
//                        "status", n.getStatus(),
//                        "scheduledFor", n.getScheduledFor(),
//                        "createdAt", n.getCreatedAt(),
//                        "user", Map.of(
//                                "id", n.getUser().getId(),
//                                "email", n.getUser().getEmail()
//                        )
//                ))
//                .toList());
//    }
//
//    /**
//     * 5. WYŚLIJ TESTOWEGO EMAILA (sprawdź konfigurację)
//     */
//    @PostMapping("/send-test-email")
//    public ResponseEntity<?> sendTestEmail() {
//        User testUser = getOrCreateTestUser("test.email@meethub.com", "Test", "Email");
//
//        Map<String, String> variables = new HashMap<>();
//        variables.put("userName", testUser.getFirstName());
//        variables.put("testTime", LocalDateTime.now().toString());
//
//        Notification notification = notificationService.createNotificationFromTemplate(
//                testUser.getId(),
//                "test_email",
//                variables,
//                NotificationType.SYSTEM,
//                NotificationChannel.EMAIL
//        );
//
//        return ResponseEntity.ok(Map.of(
//                "message", "Wysłano testowego emaila",
//                "to", testUser.getEmail(),
//                "notificationId", notification != null ? notification.getId() : "null"
//        ));
//    }
//
//    // ========== METODY POMOCNICZE ==========
//
//    private User getOrCreateTestUser(String email, String firstName, String lastName) {
//        return userRepository.findByEmail(email)
//                .orElseGet(() -> userRepository.save(User.builder()
//                        .email(email)
//                        .firstName(firstName)
//                        .lastName(lastName)
//                        .password("test123")
//                        .role(UserRole.PARTICIPANT)
//                        .emailNotificationsEnabled(true)
//                        .pushNotificationsEnabled(true)
//                        .smsNotificationsEnabled(false)
//                        .digestEnabled(true)
//                        .timezone("Europe/Warsaw")
//                        .language("pl")
//                        .enabled(true)
//                        .build()));
//    }
//
//    private void enableAllNotifications(User user) {
//        user.setEmailNotificationsEnabled(true);
//        user.setPushNotificationsEnabled(true);
//        user.setEnabledNotificationChannels(Set.of(
//                NotificationChannel.EMAIL,
//                NotificationChannel.IN_APP,
//                NotificationChannel.PUSH
//        ));
//        userRepository.save(user);
//
//        // Ustaw preferencje dla wszystkich typów powiadomień
//        String[] preferenceKeys = {
//                "meeting_invitations", "meeting_reminders",
//                "meeting_updates", "task_assignments", "security_alerts"
//        };
//
//        for (String key : preferenceKeys) {
//            userPreferenceRepository.findByUserIdAndPreferenceKey(user.getId(), key)
//                    .ifPresentOrElse(
//                            pref -> {
//                                pref.setPreferenceValue("true");
//                                userPreferenceRepository.save(pref);
//                            },
//                            () -> {
//                                UserPreference newPref = new UserPreference();
//                                newPref.setUser(user);
//                                newPref.setPreferenceKey(key);
//                                newPref.setPreferenceValue("true");
//                                userPreferenceRepository.save(newPref);
//                            }
//                    );
//        }
//    }
//
//    private void scheduleManualCheck(Long meetingId, int minutesFromNow) {
//        // Możesz dodać logikę planowania ręcznego sprawdzenia
//        log.info("⏰ Zaplanowano ręczne sprawdzenie spotkania {} za {} minut",
//                meetingId, minutesFromNow);
//    }
//}
package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.*;
import com.meethub.domain.model.enums.*;
import com.meethub.domain.repository.jpa.*;
import com.meethub.domain.service.MeetingSchedulerService;
import com.meethub.domain.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingSchedulerServiceImpl implements MeetingSchedulerService {
    private final MeetingRepository meetingRepository;
    private final NotificationService notificationService;
    private final MeetingParticipantRepository participantRepository;
    private final UserPreferenceRepository userPreferenceRepository;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
    private final Map<Long, Set<String>> scheduledTasks = new ConcurrentHashMap<>();
    private final AtomicBoolean isSchedulingEnabled = new AtomicBoolean(true);

    // Konfiguracja interwałów przypomnień (w minutach przed rozpoczęciem)
    private static final List<Integer> REMINDER_INTERVALS = Arrays.asList(1440, 720, 360, 60, 30, 15, 5); // 24h, 12h, 6h, 1h, 30min, 15min, 5min

    @Scheduled(fixedRate = 60000)
    @Transactional
    @Override
    public void scheduleUpcomingMeetings() {
        if (!isSchedulingEnabled.get()) {
            log.debug("Scheduler jest wyłączony");
            return;
        }

        log.debug("🔍 Sprawdzam nadchodzące spotkania do zaplanowania...");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lookAhead = now.plusHours(48); // Sprawdzaj spotkania w ciągu 48h

        try {
            // Znajdź zaplanowane spotkania które jeszcze się nie rozpoczęły
            List<Meeting> upcomingMeetings = meetingRepository.findByStatusAndStartDateBetween(
                    MeetingStatus.PLANNED, now, lookAhead);

            for (Meeting meeting : upcomingMeetings) {
                if (shouldScheduleMeeting(meeting)) {
                    scheduleMeetingNotifications(meeting);
                }
            }

            log.info("✅ Scheduler: sprawdzono {} spotkań, zaplanowano powiadomienia",
                    upcomingMeetings.size());

        } catch (Exception e) {
            log.error("❌ Błąd podczas planowania spotkań: {}", e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void scheduleMeetingNotifications(Meeting meeting) {
        Long meetingId = meeting.getId();

        // Sprawdź czy spotkanie jest już zaplanowane
        if (isMeetingAlreadyScheduled(meetingId)) {
            log.debug("Spotkanie {} już ma zaplanowane powiadomienia", meetingId);
            return;
        }

        LocalDateTime startTime = meeting.getStartDate();
        LocalDateTime now = LocalDateTime.now();

        long minutesUntilStart = ChronoUnit.MINUTES.between(now, startTime);

        if (minutesUntilStart <= 0) {
            log.debug("Spotkanie {} już powinno się rozpocząć, zmieniam status", meetingId);
            handleMeetingStartImmediately(meeting);
            return;
        }

        log.info("📅 Planowanie powiadomień dla spotkania {}: '{}' (za {} minut)",
                meetingId, meeting.getTitle(), minutesUntilStart);

        // Planuj przypomnienia
        for (int minutesBefore : REMINDER_INTERVALS) {
            if (minutesUntilStart > minutesBefore) {
                long delayMinutes = minutesUntilStart - minutesBefore;
                scheduleReminder(meeting, minutesBefore, delayMinutes);
            }
        }

        // Planuj powiadomienie o rozpoczęciu
        scheduleMeetingStart(meeting, minutesUntilStart);

        // Planuj sprawdzenie czy spotkanie się rozpoczęło (10 minut po planowanym rozpoczęciu)
        scheduleStatusCheck(meeting, minutesUntilStart + 10);

        // Zaznacz spotkanie jako zaplanowane
        markMeetingAsScheduled(meetingId);

        log.info("✅ Zaplanowano wszystkie powiadomienia dla spotkania {} ({} przypomnień)",
                meetingId, countScheduledReminders(meetingId));
    }

    private void scheduleReminder(Meeting meeting, int minutesBefore, long delayMinutes) {
        Long meetingId = meeting.getId();
        String taskKey = "reminder_" + minutesBefore + "min";

        if (delayMinutes <= 0) {
            log.debug("Pominięto przypomnienie dla spotkania {}: opóźnienie {}min <= 0",
                    meetingId, delayMinutes);
            return;
        }

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                log.debug("Wykonuję przypomnienie dla spotkania {} (za {} min)",
                        meetingId, minutesBefore);
                sendReminderNotification(meeting, minutesBefore);
                removeTask(meetingId, taskKey);
            } catch (Exception e) {
                log.error("❌ Błąd podczas wysyłania przypomnienia dla spotkania {}: {}",
                        meetingId, e.getMessage(), e);
            }
        }, delayMinutes, TimeUnit.MINUTES);

        addTask(meetingId, taskKey, future);
        log.debug("🔔 Zaplanowano przypomnienie za {}min (wykonanie za {}min) dla spotkania {}",
                minutesBefore, delayMinutes, meetingId);
    }

    private void scheduleMeetingStart(Meeting meeting, long delayMinutes) {
        Long meetingId = meeting.getId();
        String taskKey = "start";

        if (delayMinutes <= 0) {
            log.debug("Spotkanie {} już się rozpoczęło, zmieniam status natychmiast", meetingId);
            handleMeetingStartImmediately(meeting);
            return;
        }

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                log.debug("Rozpoczynam spotkanie {}", meetingId);
                handleMeetingStart(meeting);
                removeTask(meetingId, taskKey);
            } catch (Exception e) {
                log.error("❌ Błąd podczas rozpoczynania spotkania {}: {}",
                        meetingId, e.getMessage(), e);
            }
        }, delayMinutes, TimeUnit.MINUTES);

        addTask(meetingId, taskKey, future);
        log.debug("🎯 Zaplanowano rozpoczęcie spotkania {} za {} minut", meetingId, delayMinutes);
    }

    private void scheduleStatusCheck(Meeting meeting, long delayMinutes) {
        Long meetingId = meeting.getId();
        String taskKey = "status_check";

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                log.debug("Sprawdzam status spotkania {}", meetingId);
                checkMeetingStatus(meeting);
                removeTask(meetingId, taskKey);
            } catch (Exception e) {
                log.error("❌ Błąd podczas sprawdzania statusu spotkania {}: {}",
                        meetingId, e.getMessage(), e);
            }
        }, delayMinutes, TimeUnit.MINUTES);

        addTask(meetingId, taskKey, future);
        log.debug("🔍 Zaplanowano sprawdzenie statusu spotkania {} za {} minut",
                meetingId, delayMinutes);
    }

    @Transactional
    protected void sendReminderNotification(Meeting meeting, int minutesBefore) {
        log.info("🔔 Wysyłam przypomnienie o spotkaniu {}: '{}' (za {} minut)",
                meeting.getId(), meeting.getTitle(), minutesBefore);

        LocalDateTime reminderTime = LocalDateTime.now().plusMinutes(minutesBefore);

        // 1. Powiadom organizatora
        if (isNotificationEnabled(meeting.getOrganizer(), "meeting_reminders")) {
            try {
                notificationService.scheduleMeetingReminder(
                        meeting.getId(),
                        meeting.getOrganizer().getId(),
                        reminderTime
                );
                log.debug("✅ Zaplanowano przypomnienie dla organizatora {}",
                        meeting.getOrganizer().getId());
            } catch (Exception e) {
                log.error("❌ Błąd planowania przypomnienia dla organizatora: {}",
                        e.getMessage());
            }
        }

        // 2. Powiadom uczestników
        List<MeetingParticipant> confirmedParticipants = participantRepository
                .findByMeetingIdAndStatus(meeting.getId(), ParticipationStatus.CONFIRMED);

        int notifiedCount = 0;
        for (MeetingParticipant participant : confirmedParticipants) {
            User user = participant.getUser();

            if (isNotificationEnabled(user, "meeting_reminders")) {
                try {
                    notificationService.scheduleMeetingReminder(
                            meeting.getId(),
                            user.getId(),
                            reminderTime
                    );
                    notifiedCount++;
                    log.debug("✅ Zaplanowano przypomnienie dla użytkownika {}", user.getId());
                } catch (Exception e) {
                    log.error("❌ Błąd planowania przypomnienia dla użytkownika {}: {}",
                            user.getId(), e.getMessage());
                }
            }
        }

        log.info("📨 Zaplanowano przypomnienia dla spotkania {}: {} uczestników + organizator",
                meeting.getId(), notifiedCount);
    }


    @Transactional
    protected void handleMeetingStart(Meeting meeting) {
        log.info("🎉 Rozpoczynam spotkanie {}: '{}'",
                meeting.getId(), meeting.getTitle());

        // 1. Zmień status spotkania w transakcji
        meeting.setStatus(MeetingStatus.ONGOING);
//        meeting.setActualStartDate(LocalDateTime.now());
        meeting.setUpdatedAt(LocalDateTime.now());
        meetingRepository.save(meeting);

        // 2. Wyślij powiadomienia o rozpoczęciu
        sendMeetingStartedNotifications(meeting);

        // 3. Wyczyść zaplanowane zadania dla tego spotkania
        cleanupScheduledTasks(meeting.getId());
    }

    @Transactional
    protected void handleMeetingStartImmediately(Meeting meeting) {
        log.info("⏱️ Natychmiastowe rozpoczęcie spotkania {}: '{}'",
                meeting.getId(), meeting.getTitle());

        meeting.setStatus(MeetingStatus.ONGOING);
//        meeting.setActualStartDate(LocalDateTime.now());
        meeting.setUpdatedAt(LocalDateTime.now());
        meetingRepository.save(meeting);

        sendMeetingStartedNotifications(meeting);
        cleanupScheduledTasks(meeting.getId());
    }

//    @Transactional
//    protected void sendMeetingStartedNotifications(Meeting meeting) {
//        log.info("📨 Wysyłam powiadomienia o rozpoczęciu spotkania {}", meeting.getId());
//
//        int notifiedCount = 0;
//
//        // ========== 1. ZAWSZE POWIADOM ORGANIZATORA ==========
//        try {
//            Map<String, String> organizerVars = createMeetingStartedVariables(meeting);
//            organizerVars.put("userName", meeting.getOrganizer().getFirstName());
//
//            // EMAIL (jeśli ma włączone)
////            if (isNotificationEnabled(meeting.getOrganizer(), "meeting_updates")) {
//                notificationService.createNotificationFromTemplate(
//                        meeting.getOrganizer().getId(),
//                        "meeting_started",
//                        organizerVars,
//                        NotificationType.MEETING_UPDATE,
//                        NotificationChannel.EMAIL
//                );
//
//            notificationService.createNotificationFromTemplate(
//                    meeting.getOrganizer().getId(),
//                    "meeting_started",
//                    organizerVars,
//                    NotificationType.MEETING_UPDATE,
//                    NotificationChannel.IN_APP
//            );
//                log.debug("✅ Wysłano EMAIL o rozpoczęciu do organizatora {}",
//                        meeting.getOrganizer().getId());
////            }
//
//            notifiedCount++;
//            log.info("✅ Wysłano powiadomienia IN_APP o rozpoczęciu do organizatora {}",
//                    meeting.getOrganizer().getId());
//
//        } catch (Exception e) {
//            log.error("❌ Błąd wysyłania powiadomień do organizatora: {}", e.getMessage());
//        }
//
//        // ========== 2. POWIADOM UCZESTNIKÓW (jeśli istnieją) ==========
//        List<MeetingParticipant> confirmedParticipants = participantRepository
//                .findByMeetingIdAndStatus(meeting.getId(), ParticipationStatus.CONFIRMED);
//
//        if (confirmedParticipants.isEmpty()) {
//            log.info("ℹ️ Spotkanie {} nie ma potwierdzonych uczestników", meeting.getId());
//        } else {
//            for (MeetingParticipant participant : confirmedParticipants) {
//                User user = participant.getUser();
//
//                if (isNotificationEnabled(user, "meeting_updates")) {
//                    try {
//                        Map<String, String> participantVars = createMeetingStartedVariables(meeting);
//                        participantVars.put("userName", user.getFirstName());
//
//                        // Jeśli spotkanie jest wirtualne, dodaj link
//                        if (meeting.getLocation() != null &&
//                                meeting.getLocation().getType() == LocationType.VIRTUAL &&
//                                meeting.getLocation().getVirtualMeetingUrl() != null) {
//                            participantVars.put("meetingLink", meeting.getLocation().getVirtualMeetingUrl());
//                        }
//
//                        // EMAIL
//                        notificationService.createNotificationFromTemplate(
//                                user.getId(),
//                                "meeting_started_participant",
//                                participantVars,
//                                NotificationType.MEETING_UPDATE,
//                                NotificationChannel.EMAIL
//                        );
//
//                        // IN_APP
//                        notificationService.createNotificationFromTemplate(
//                                user.getId(),
//                                "meeting_started_participant",
//                                participantVars,
//                                NotificationType.MEETING_UPDATE,
//                                NotificationChannel.IN_APP
//                        );
//
//                        notifiedCount++;
//                        log.debug("✅ Wysłano powiadomienia o rozpoczęciu do użytkownika {}",
//                                user.getId());
//
//                    } catch (Exception e) {
//                        log.error("❌ Błąd wysyłania powiadomień do użytkownika {}: {}",
//                                user.getId(), e.getMessage());
//                    }
//                }
//            }
//        }
//
//        log.info("📨 Wysłano powiadomienia o rozpoczęciu dla {} osób (w tym organizator) spotkania {}",
//                notifiedCount, meeting.getId());
//    }


@Transactional
protected void sendMeetingStartedNotifications(Meeting meeting) {
    log.info("📨 Wysyłam powiadomienia o rozpoczęciu spotkania {}", meeting.getId());

    int notifiedCount = 0;

    // ========== 1. ZAWSZE POWIADOM ORGANIZATORA ==========
    try {
        Map<String, String> organizerVars = createMeetingStartedVariables(meeting);
        organizerVars.put("userName", meeting.getOrganizer().getFirstName());

        // DODAJ meetingId DLA ORGANIZATORA
        organizerVars.put("meetingId", meeting.getId().toString()); // ← KLUCZOWE!

        // EMAIL (jeśli ma włączone)
        notificationService.createNotificationFromTemplate(
                meeting.getOrganizer().getId(),
                "meeting_started",
                organizerVars, // ← TERAZ ZAWIERA meetingId
                NotificationType.MEETING_UPDATE,
                NotificationChannel.EMAIL
        );

        notificationService.createNotificationFromTemplate(
                meeting.getOrganizer().getId(),
                "meeting_started",
                organizerVars, // ← TERAZ ZAWIERA meetingId
                NotificationType.MEETING_UPDATE,
                NotificationChannel.IN_APP
        );

        notifiedCount++;
        log.info("✅ Wysłano powiadomienia do organizatora {} (meetingId: {})",
                meeting.getOrganizer().getId(), meeting.getId());

    } catch (Exception e) {
        log.error("❌ Błąd wysyłania powiadomień do organizatora: {}", e.getMessage());
    }

    // ========== 2. POWIADOM UCZESTNIKÓW (jeśli istnieją) ==========
    List<MeetingParticipant> confirmedParticipants = participantRepository
            .findByMeetingIdAndStatus(meeting.getId(), ParticipationStatus.CONFIRMED);

    if (confirmedParticipants.isEmpty()) {
        log.info("ℹ️ Spotkanie {} nie ma potwierdzonych uczestników", meeting.getId());
    } else {
        for (MeetingParticipant participant : confirmedParticipants) {
            User user = participant.getUser();

            if (isNotificationEnabled(user, "meeting_updates")) {
                try {
                    Map<String, String> participantVars = createMeetingStartedVariables(meeting);
                    participantVars.put("userName", user.getFirstName());

                    // DODAJ meetingId DLA UCZESTNIKA
                    participantVars.put("meetingId", meeting.getId().toString()); // ← KLUCZOWE!

                    // Jeśli spotkanie jest wirtualne, dodaj link
                    if (meeting.getLocation() != null &&
                            meeting.getLocation().getType() == LocationType.VIRTUAL &&
                            meeting.getLocation().getVirtualMeetingUrl() != null) {
                        participantVars.put("meetingLink", meeting.getLocation().getVirtualMeetingUrl());
                    }

                    // EMAIL
                    notificationService.createNotificationFromTemplate(
                            user.getId(),
                            "meeting_started_participant",
                            participantVars, // ← TERAZ ZAWIERA meetingId
                            NotificationType.MEETING_UPDATE,
                            NotificationChannel.EMAIL
                    );

                    // IN_APP
                    notificationService.createNotificationFromTemplate(
                            user.getId(),
                            "meeting_started_participant",
                            participantVars, // ← TERAZ ZAWIERA meetingId
                            NotificationType.MEETING_UPDATE,
                            NotificationChannel.IN_APP
                    );

                    notifiedCount++;
                    log.debug("✅ Wysłano powiadomienia o rozpoczęciu do użytkownika {} (meetingId: {})",
                            user.getId(), meeting.getId());

                } catch (Exception e) {
                    log.error("❌ Błąd wysyłania powiadomień do użytkownika {}: {}",
                            user.getId(), e.getMessage());
                }
            }
        }
    }

    log.info("📨 Wysłano powiadomienia o rozpoczęciu dla {} osób spotkania {}",
            notifiedCount, meeting.getId());
}

    @Transactional
    protected void checkMeetingStatus(Meeting meeting) {
        log.debug("🔍 Sprawdzam status spotkania {}", meeting.getId());

        // Pobierz świeże dane z bazy
        Meeting freshMeeting = meetingRepository.findById(meeting.getId())
                .orElse(null);

        if (freshMeeting == null) {
            log.warn("Spotkanie {} nie istnieje w bazie", meeting.getId());
            return;
        }

        // Jeśli spotkanie jest wciąż PLANOWANE mimo upływu czasu
        if (freshMeeting.getStatus() == MeetingStatus.PLANNED) {
            LocalDateTime now = LocalDateTime.now();
            long minutesLate = ChronoUnit.MINUTES.between(
                    freshMeeting.getStartDate(), now);

            if (minutesLate >= 10) { // 10 minut po czasie
                log.warn("⚠️ Spotkanie {} nie rozpoczęło się pomimo upływu {} minut: '{}'",
                        freshMeeting.getId(), minutesLate, freshMeeting.getTitle());

                sendMeetingNotStartedAlert(freshMeeting);

                // Automatycznie oznacz jako anulowane jeśli bardzo późno
                if (minutesLate >= 60) { // 1 godzina opóźnienia
                    log.warn("⚠️ Automatyczne anulowanie spotkania {} z powodu dużego opóźnienia",
                            freshMeeting.getId());
                    freshMeeting.setStatus(MeetingStatus.CANCELLED);
                    freshMeeting.setUpdatedAt(now);
                    meetingRepository.save(freshMeeting);
                }
            }
        }
    }

    @Transactional
    protected void sendMeetingNotStartedAlert(Meeting meeting) {
        log.warn("🚨 Wysyłam alert o nie rozpoczętym spotkaniu {}", meeting.getId());

        Map<String, String> vars = new HashMap<>();
        vars.put("meetingTitle", meeting.getTitle());
        vars.put("scheduledTime", meeting.getStartDate().toString());
        vars.put("organizerName", meeting.getOrganizer().getFirstName());
        vars.put("minutesLate", String.valueOf(
                ChronoUnit.MINUTES.between(meeting.getStartDate(), LocalDateTime.now())));

        // Tylko do organizatora
        notificationService.createNotificationFromTemplate(
                meeting.getOrganizer().getId(),
                "meeting_not_started",
                vars,
                NotificationType.SECURITY_ALERT,
                NotificationChannel.EMAIL
        );

        notificationService.createNotificationFromTemplate(
                meeting.getOrganizer().getId(),
                "meeting_not_started",
                vars,
                NotificationType.SECURITY_ALERT,
                NotificationChannel.IN_APP
        );
    }

    @Override
    public void cancelMeetingSchedule(Long meetingId) {
        log.info("❌ Anulowanie harmonogramu dla spotkania {}", meetingId);
        cleanupScheduledTasks(meetingId);
    }

    @Scheduled(fixedRate = 3600000) // co godzinę
    @Override
    @Transactional
    public void cleanupFinishedMeetings() {
        log.debug("🧹 Sprzątanie zakończonych spotkań...");

        // Znajdź spotkania które zakończyły się ponad 24h temu
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        List<Meeting> finishedMeetings = meetingRepository
                .findByStatusAndUpdatedAtBefore(MeetingStatus.COMPLETED, cutoff);

        int cleanedCount = 0;
        for (Meeting meeting : finishedMeetings) {
            if (cleanupScheduledTasks(meeting.getId())) {
                cleanedCount++;
            }
        }

        log.info("🧹 Wyczyczono harmonogramy {} zakończonych spotkań", cleanedCount);

        // Dodatkowo posprzątaj stare wpisy w scheduledTasks (spotkania które już nie istnieją)
        cleanupOrphanedTasks();
    }

    // ========== METODY POMOCNICZE ==========

    private boolean isNotificationEnabled(User user, String preferenceKey) {
        return userPreferenceRepository.findByUserIdAndPreferenceKey(user.getId(), preferenceKey)
                .map(pref -> "true".equalsIgnoreCase(pref.getPreferenceValue()))
                .orElse(true); // Domyślnie true jeśli nie ma preferencji
    }

    private boolean shouldScheduleMeeting(Meeting meeting) {
        // Sprawdź czy spotkanie ma wystarczającą ilość czasu przed rozpoczęciem
        LocalDateTime now = LocalDateTime.now();
        long minutesUntilStart = ChronoUnit.MINUTES.between(now, meeting.getStartDate());

        // Jeśli spotkanie zaczyna się za mniej niż 5 minut, nie planuj
        if (minutesUntilStart < 5) {
            return false;
        }

        // Sprawdź czy spotkanie ma uczestników
        long participantCount = participantRepository.countByMeetingIdAndStatus(
                meeting.getId(), ParticipationStatus.CONFIRMED);

        return participantCount > 0;
    }

    private boolean isMeetingAlreadyScheduled(Long meetingId) {
        return scheduledTasks.containsKey(meetingId) &&
                !scheduledTasks.get(meetingId).isEmpty();
    }

    private void markMeetingAsScheduled(Long meetingId) {
        scheduledTasks.putIfAbsent(meetingId, ConcurrentHashMap.newKeySet());
    }

    private int countScheduledReminders(Long meetingId) {
        if (!scheduledTasks.containsKey(meetingId)) {
            return 0;
        }

        return (int) scheduledTasks.get(meetingId).stream()
                .filter(key -> key.startsWith("reminder_"))
                .count();
    }

    private void addTask(Long meetingId, String taskKey, ScheduledFuture<?> future) {
        scheduledTasks.computeIfAbsent(meetingId, k -> ConcurrentHashMap.newKeySet())
                .add(taskKey);
    }

    private void removeTask(Long meetingId, String taskKey) {
        if (scheduledTasks.containsKey(meetingId)) {
            scheduledTasks.get(meetingId).remove(taskKey);
        }
    }

    private boolean cleanupScheduledTasks(Long meetingId) {
        if (!scheduledTasks.containsKey(meetingId)) {
            return false;
        }

        Set<String> tasks = scheduledTasks.remove(meetingId);
        log.debug("🧹 Usunięto {} zadań dla spotkania {}", tasks.size(), meetingId);
        return !tasks.isEmpty();
    }

    private void cleanupOrphanedTasks() {
        // Usuń zadania dla spotkań które już nie istnieją
        List<Long> existingMeetingIds = meetingRepository.findAllMeetingIds();

        scheduledTasks.keySet().removeIf(meetingId ->
                !existingMeetingIds.contains(meetingId));
    }

    private Map<String, String> createReminderVariables(Meeting meeting, int minutesBefore) {
        Map<String, String> vars = new HashMap<>();
        vars.put("meetingTitle", meeting.getTitle());
        vars.put("meetingTime", meeting.getStartDate().toString());
        vars.put("minutesBefore", String.valueOf(minutesBefore));
        vars.put("organizerName", meeting.getOrganizer().getFirstName());

        if (meeting.getLocation() != null) {
            vars.put("location", meeting.getLocation().getName());
            if (meeting.getLocation().getType() == LocationType.VIRTUAL &&
                    meeting.getLocation().getVirtualMeetingUrl() != null) {
                vars.put("meetingLink", meeting.getLocation().getVirtualMeetingUrl());
            }
        }

        if (meeting.getDescription() != null) {
            vars.put("meetingDescription", meeting.getDescription());
        }

        return vars;
    }

//    private Map<String, String> createMeetingStartedVariables(Meeting meeting) {
//        Map<String, String> vars = new HashMap<>();
//        vars.put("meetingTitle", meeting.getTitle());
//        vars.put("meetingTime", meeting.getStartDate().toString());
//        vars.put("organizerName", meeting.getOrganizer().getFirstName());
//
//        if (meeting.getLocation() != null) {
//            vars.put("location", meeting.getLocation().getName());
//            if (meeting.getLocation().getType() == LocationType.VIRTUAL &&
//                    meeting.getLocation().getVirtualMeetingUrl() != null) {
//                vars.put("meetingLink", meeting.getLocation().getVirtualMeetingUrl());
//            }
//        }
//
//        if (meeting.getDescription() != null) {
//            vars.put("meetingDescription", meeting.getDescription());
//        }
//
//        return vars;
//    }


    private Map<String, String> createMeetingStartedVariables(Meeting meeting) {
        Map<String, String> vars = new HashMap<>();
        vars.put("meetingTitle", meeting.getTitle());
        vars.put("meetingTime", meeting.getStartDate().toString());
        vars.put("organizerName", meeting.getOrganizer().getFirstName());

        // DODAJ meetingId DO PODSTAWOWYCH ZMIENNYCH
        vars.put("meetingId", meeting.getId().toString()); // ← DODAJ TUTAJ

        // Dodaj sformatowaną datę dla lepszej czytelności
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        vars.put("meetingDate", meeting.getStartDate().format(formatter));

        if (meeting.getLocation() != null) {
            vars.put("location", meeting.getLocation().getName());
            if (meeting.getLocation().getType() == LocationType.VIRTUAL &&
                    meeting.getLocation().getVirtualMeetingUrl() != null) {
                vars.put("meetingLink", meeting.getLocation().getVirtualMeetingUrl());
            }
        }

        if (meeting.getDescription() != null) {
            vars.put("meetingDescription", meeting.getDescription());
        }

        return vars;
    }

    @Override
    public void shutdown() {
        log.info("🛑 Zatrzymuję scheduler spotkań...");
        isSchedulingEnabled.set(false);

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        scheduledTasks.clear();
        log.info("✅ Scheduler został zatrzymiony");
    }

    @Override
    public void enableScheduling() {
        isSchedulingEnabled.set(true);
        log.info("✅ Włączono scheduler spotkań");
    }

    @Override
    public void disableScheduling() {
        isSchedulingEnabled.set(false);
        log.info("⏸️ Wyłączono scheduler spotkań");
    }

    @Override
    public Map<String, Object> getSchedulerStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("isEnabled", isSchedulingEnabled.get());
        status.put("scheduledMeetings", scheduledTasks.size());
        status.put("totalTasks", scheduledTasks.values().stream()
                .mapToInt(Set::size)
                .sum());
        status.put("schedulerActive", !scheduler.isShutdown());
        return status;
    }
}
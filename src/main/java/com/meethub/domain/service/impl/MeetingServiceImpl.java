




package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.*;
import com.meethub.domain.model.enums.MeetingStatus;
import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.model.request.CreateMeetingRequest;
import com.meethub.domain.model.request.SearchCriteria;
import com.meethub.domain.model.request.UpdateMeetingRequest;
import com.meethub.domain.model.response.MeetingParticipationInfo;
import com.meethub.domain.model.response.MeetingResponse;
import com.meethub.domain.repository.jpa.CategoryRepository;
import com.meethub.domain.repository.jpa.MeetingRepository;
import com.meethub.domain.repository.jpa.UserRepository;
import com.meethub.domain.repository.jdbc.CustomMeetingRepository;
import com.meethub.domain.repository.specification.MeetingSpecification;
import com.meethub.domain.service.MeetingAuthorizationService;
import com.meethub.domain.service.MeetingParticipantService;
import com.meethub.domain.service.MeetingSchedulerService;
import com.meethub.domain.service.MeetingService;
import com.meethub.exception.BusinessException;
import com.meethub.exception.ResourceNotFoundException;
import com.meethub.domain.model.mapper.MeetingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingServiceImpl implements MeetingService {

    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;
    private final CustomMeetingRepository customMeetingRepository;
    private final MeetingMapper meetingMapper;
    private final MeetingParticipantService meetingParticipantService;
    private final MeetingAuthorizationService meetingAuthorizationService;
    private final CategoryRepository categoryRepository;

    private final MeetingSchedulerService meetingSchedulerService;

    @Override
    @Transactional
    public MeetingResponse createMeeting(CreateMeetingRequest request, Long organizerId) {
        log.info("=== START CREATING MEETING ===");
        log.info("Organizer ID: {}, Title: {}, Recurring: {}",
                organizerId, request.getTitle(), request.isRecurring());

        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + organizerId));

        log.info("Found organizer: {} {} (ID: {})",
                organizer.getFirstName(), organizer.getLastName(), organizer.getId());

        Meeting meeting = meetingMapper.toEntity(request);

        // ✅ USTAW ORGANIZATORA I STATUS
        meeting.setOrganizer(organizer);
        meeting.setStatus(MeetingStatus.PLANNED);

        validateMeetingDates(meeting.getStartDate(), meeting.getEndDate());


        // ✅ OBSŁUGA KATEGORII
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            Set<Category> categories = new HashSet<>(categoryRepository.findAllById(request.getCategoryIds()));
            meeting.setCategories(categories);
            log.info("Added {} categories to meeting", categories.size());
        }

        // ✅ OBSŁUGA SZABLONU
        if (request.isSaveAsTemplate()) {
            meeting.setTemplate(true);
            meeting.setTitle(request.getTemplateName() != null ?
                    request.getTemplateName() : meeting.getTitle() + " (Szablon)");
            log.info("Meeting saved as template: {}", meeting.getTitle());
        }

        log.info("Before save - Title: {}, Recurring: {}, Pattern: {}",
                meeting.getTitle(), meeting.isRecurring(), meeting.getRecurrencePattern());

        try {
            Meeting savedMeeting = meetingRepository.save(meeting);
            meetingParticipantService.addOrganizerAsParticipant(savedMeeting, organizer);


            // ✅ ZAPISZ POCZĄTKOWY STATUS W HISTORII
            saveStatusChange(savedMeeting, null, savedMeeting.getStatus().name(),
                    organizerId, "Utworzono spotkanie");

            log.info("=== MEETING CREATED SUCCESSFULLY ===");
            log.info("Meeting ID: {}, Title: {}, Recurring: {}",
                    savedMeeting.getId(), savedMeeting.getTitle(), savedMeeting.isRecurring());

            // ✅ GENERUJ NASTĘPNE WYSTĄPIENIA DLA POWTARZAJĄCYCH SIĘ SPOTKAŃ
            if (savedMeeting.isRecurring() && savedMeeting.getRecurrencePattern() != null) {
                generateNextRecurrence(savedMeeting.getId(), 3); // Generuj 3 następne
            }

            meetingSchedulerService.scheduleMeetingNotifications(savedMeeting);

            log.info("✅ Utworzono spotkanie {} i zaplanowano powiadomienia", savedMeeting.getId());

            return meetingMapper.toResponse(savedMeeting);
        } catch (Exception e) {
            log.error("=== ERROR CREATING MEETING ===");
            log.error("Error: {}", e.getMessage(), e);
            throw new BusinessException("Error creating meeting: " + e.getMessage());
        }
    }

//    @Override
//    @Transactional
//    public MeetingResponse updateMeeting(Long meetingId, UpdateMeetingRequest request, Long userId) {
//        log.info("Updating meeting {} by user {}", meetingId, userId);
//
//        // ✅ SPRAWDŹ UPRAWNIENIA
//        if (!meetingAuthorizationService.canUserEditMeeting(meetingId, userId)) {
//            throw new BusinessException("No permission to edit this meeting");
//        }
//
//        Meeting meeting = meetingRepository.findById(meetingId)
//                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found with id: " + meetingId));
//
//        // ✅ ZAPISZ ZMIANĘ STATUSU W HISTORII
//        if (request.getStatus() != null && !request.getStatus().equals(meeting.getStatus())) {
//            saveStatusChange(meeting, meeting.getStatus().name(),
//                    request.getStatus().name(), userId, request.getStatusChangeReason());
//        }
//
//        if (request.getStartDate() != null &&
//                !request.getStartDate().equals(meeting.getStartDate())) {
//
//            // Anuluj stare powiadomienia
//            meetingSchedulerService.cancelMeetingSchedule(meetingId);
//
//            // Zaplanuj nowe
//            meetingSchedulerService.scheduleMeetingNotifications(updatedMeeting);
//
//            log.info("🔄 Przeplanowano powiadomienia dla spotkania {} po zmianie daty",
//                    meetingId);
//        }
//
//
//        // ✅ AKTUALIZUJ KATEGORIE
//        if (request.getCategoryIds() != null) {
//            Set<Category> categories = new HashSet<>(categoryRepository.findAllById(request.getCategoryIds()));
//            meeting.setCategories(categories);
//            log.info("Updated categories for meeting {}: {} categories", meetingId, categories.size());
//        }
//
//        // ✅ AKTUALIZUJ POZOSTAŁE POLA
//        meetingMapper.updateEntityFromRequest(request, meeting);
//
//        Meeting updatedMeeting = meetingRepository.save(meeting);
//        log.info("Meeting {} updated by user {}", meetingId, userId);
//
//        return meetingMapper.toResponse(updatedMeeting);
//    }


    @Override
    @Transactional
    public MeetingResponse updateMeeting(Long meetingId, UpdateMeetingRequest request, Long userId) {
        log.info("Updating meeting {} by user {}", meetingId, userId);

        // 1. SPRAWDŹ UPRAWNIENIA
        if (!meetingAuthorizationService.canUserEditMeeting(meetingId, userId)) {
            throw new BusinessException("No permission to edit this meeting");
        }

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found with id: " + meetingId));

        // 2. ZAPISZ STARE DANE DO PORÓWNANIA
        MeetingStatus oldStatus = meeting.getStatus();
        LocalDateTime oldStartDate = meeting.getStartDate();
        LocalDateTime oldEndDate = meeting.getEndDate();
        String oldTitle = meeting.getTitle();
        Location oldLocation = meeting.getLocation() != null ?
                new Location(meeting.getLocation()) : null; // głęboka kopia jeśli potrzeba

        // 3. ZAPISZ ZMIANĘ STATUSU W HISTORII
        if (request.getStatus() != null && !request.getStatus().equals(oldStatus)) {
            saveStatusChange(meeting, oldStatus.name(),
                    request.getStatus().name(), userId, request.getStatusChangeReason());

            // Jeśli zmieniasz status na CANCELLED - anuluj powiadomienia
            if (request.getStatus() == MeetingStatus.CANCELLED) {
                meetingSchedulerService.cancelMeetingSchedule(meetingId);
                log.info("❌ Anulowano powiadomienia dla spotkania {} (status zmieniony na CANCELLED)",
                        meetingId);
            }
            // Jeśli zmieniasz status z CANCELLED na PLANNED - zaplanuj powiadomienia
            else if (oldStatus == MeetingStatus.CANCELLED && request.getStatus() == MeetingStatus.PLANNED) {
                meetingSchedulerService.scheduleMeetingNotifications(meeting);
                log.info("🔄 Zaplanowano powiadomienia dla spotkania {} (status zmieniony z CANCELLED na PLANNED)",
                        meetingId);
            }
        }

        // 4. AKTUALIZUJ KATEGORIE (jeśli podano)
        if (request.getCategoryIds() != null) {
            Set<Category> categories = new HashSet<>(categoryRepository.findAllById(request.getCategoryIds()));
            meeting.setCategories(categories);
            log.info("Updated categories for meeting {}: {} categories", meetingId, categories.size());
        }

        // 5. AKTUALIZUJ POZOSTAŁE POLA
        meetingMapper.updateEntityFromRequest(request, meeting);

        // 6. ZAPISZ ZMIANY
        Meeting updatedMeeting = meetingRepository.save(meeting);
        log.info("Meeting {} updated by user {}", meetingId, userId);

        // 7. SPRAWDŹ CZY ZMIENIŁA SIĘ DATA I PRZEPLANUJ POWIADOMIENIA
        handleDateChanges(meetingId, updatedMeeting, oldStartDate, oldEndDate, userId);

        // 8. WYŚLIJ POWIADOMIENIA O ZMIANACH (opcjonalnie)
        if (shouldNotifyParticipantsAboutUpdate(request, oldTitle, oldStartDate, oldLocation)) {
//            sendMeetingUpdateNotifications(updatedMeeting, userId, getChangesSummary(request, oldTitle, oldStartDate, oldEndDate, oldLocation));
        }

        return meetingMapper.toResponse(updatedMeeting);
    }

    /**
     * Obsługa zmian daty spotkania
     */
    private void handleDateChanges(Long meetingId, Meeting updatedMeeting,
                                   LocalDateTime oldStartDate, LocalDateTime oldEndDate,
                                   Long userId) {
        boolean dateChanged = false;

        // Sprawdź czy zmieniła się data rozpoczęcia
        if (oldStartDate != null && updatedMeeting.getStartDate() != null &&
                !oldStartDate.equals(updatedMeeting.getStartDate())) {
            dateChanged = true;
            log.info("📅 Zmieniono datę rozpoczęcia spotkania {} z {} na {}",
                    meetingId, oldStartDate, updatedMeeting.getStartDate());
        }

        // Sprawdź czy zmieniła się data zakończenia
        if (oldEndDate != null && updatedMeeting.getEndDate() != null &&
                !oldEndDate.equals(updatedMeeting.getEndDate())) {
            dateChanged = true;
            log.info("📅 Zmieniono datę zakończenia spotkania {} z {} na {}",
                    meetingId, oldEndDate, updatedMeeting.getEndDate());
        }

        // Jeśli zmieniono datę - przepłań powiadomienia
        if (dateChanged && updatedMeeting.getStatus() == MeetingStatus.PLANNED) {
            // 1. Anuluj stare powiadomienia
            meetingSchedulerService.cancelMeetingSchedule(meetingId);

            // 2. Zaplanuj nowe
            meetingSchedulerService.scheduleMeetingNotifications(updatedMeeting);

            log.info("🔄 Przeplanowano powiadomienia dla spotkania {} po zmianie daty", meetingId);

            // 3. Zaloguj zmianę
        }
    }


    private String formatDateChange(LocalDateTime startDate, LocalDateTime endDate) {
        return "Start: " + (startDate != null ? startDate.toString() : "null") +
                ", End: " + (endDate != null ? endDate.toString() : "null");
    }

    /**
     * Sprawdza czy wysłać powiadomienia o zmianach
     */
    private boolean shouldNotifyParticipantsAboutUpdate(UpdateMeetingRequest request,
                                                        String oldTitle,
                                                        LocalDateTime oldStartDate,
                                                        Location oldLocation) {
        // Wysyłaj powiadomienia tylko o ważnych zmianach
        if (request.getTitle() != null && !request.getTitle().equals(oldTitle)) {
            return true;
        }
        if (request.getStartDate() != null && !request.getStartDate().equals(oldStartDate)) {
            return true;
        }
        if (request.getLocationId() != null && oldLocation != null &&
                !request.getLocationId().equals(oldLocation.getId())) {
            return true;
        }
        return false;
    }


    @Override
    @Transactional
    public void deleteMeeting(Long meetingId, Long organizerId) {
        // ✅ SPRAWDŹ UPRAWNIENIA
        if (!meetingAuthorizationService.canUserDeleteMeeting(meetingId, organizerId)) {
            throw new BusinessException("No permission to delete this meeting");
        }

        Meeting meeting = meetingRepository.findByIdAndOrganizerId(meetingId, organizerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Meeting not found with id: " + meetingId + " for organizer: " + organizerId));

        meetingRepository.delete(meeting);
        log.info("Meeting {} deleted by organizer {}", meetingId, organizerId);
    }

    @Override
    @Transactional(readOnly = true)
    public MeetingResponse getMeetingById(Long meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found with id: " + meetingId));

        return meetingMapper.toResponse(meeting);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MeetingResponse> getUserMeetings(Long userId, Pageable pageable) {
        Page<Meeting> meetings = meetingRepository.findByOrganizerId(userId, pageable);
        return meetings.map(meetingMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeetingResponse> getUpcomingPublicMeetings() {
        List<Meeting> meetings = meetingRepository.findUpcomingPublicMeetings(LocalDateTime.now());
        return meetings.stream()
                .map(meetingMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeetingResponse> findNearbyMeetings(double latitude, double longitude, double radius) {
        List<Meeting> meetings = customMeetingRepository.findNearbyMeetings(latitude, longitude, radius, 50);
        return meetings.stream()
                .map(meetingMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void changeMeetingStatus(Long meetingId, MeetingStatus status, Long organizerId) {
        // ✅ SPRAWDŹ UPRAWNIENIA
        if (!meetingAuthorizationService.canUserEditMeeting(meetingId, organizerId)) {
            throw new BusinessException("No permission to change status of this meeting");
        }

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found with id: " + meetingId));

        String oldStatus = meeting.getStatus().name();
        meeting.setStatus(status);
        meetingRepository.save(meeting);

        // ✅ ZAPISZ W HISTORII
        saveStatusChange(meeting, oldStatus, status.name(), organizerId, "Zmiana statusu");

        log.info("Meeting {} status changed from {} to {} by {}",
                meetingId, oldStatus, status, organizerId);
    }

    @Override
    @Transactional
    public MeetingResponse duplicateMeeting(Long meetingId, Long organizerId) {
        log.info("Duplicating meeting {} by user {}", meetingId, organizerId);

        // ✅ SPRAWDŹ UPRAWNIENIA
        if (!meetingAuthorizationService.canUserEditMeeting(meetingId, organizerId)) {
            throw new BusinessException("No permission to duplicate this meeting");
        }

        Meeting original = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Meeting not found with id: " + meetingId));

        // ✅ UŻYJ MAPPERA DO KOPIOWANIA
        Meeting duplicate = meetingMapper.cloneMeeting(original);

        // ✅ USTAW NOWEGO ORGANIZATORA
        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + organizerId));
        duplicate.setOrganizer(organizer);

        // ✅ USTAW DATĘ NA ZA TYDZIEŃ
        duplicate.setStartDate(original.getStartDate().plusDays(7));
        duplicate.setEndDate(original.getEndDate().plusDays(7));

        // ✅ ZACHOWAJ KATEGORIE Z ORYGINAŁU
        if (original.getCategories() != null) {
            duplicate.setCategories(new HashSet<>(original.getCategories()));
        }

        // ✅ USTAW REFERENCJĘ DO ORYGINAŁU
        duplicate.setOriginalMeetingId(original.getId());

        Meeting savedDuplicate = meetingRepository.save(duplicate);

        // ✅ ZAPISZ W HISTORII
        saveStatusChange(savedDuplicate, null, savedDuplicate.getStatus().name(),
                organizerId, "Utworzono jako kopia spotkania #" + original.getId());

        log.info("Meeting duplicated from {} to new id: {}", meetingId, savedDuplicate.getId());

        return meetingMapper.toResponse(savedDuplicate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeetingResponse> findConflictingMeetings(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        List<Meeting> conflicts = meetingRepository.findConfirmedMeetingsForUserInPeriod(userId, startDate, endDate);
        return conflicts.stream()
                .map(meetingMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MeetingResponse> getFilteredMeetings(String search, String type, String status, Pageable pageable) {
        Page<Meeting> meetings = customMeetingRepository.findFilteredMeetings(search, type, status, pageable);
        return meetings.map(meetingMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Meeting getMeeting(Long meetingId) {
        log.debug("Getting meeting by ID: {}", meetingId);

        return meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found with id: " + meetingId));
    }

    @Override
    @Transactional(readOnly = true)
    public MeetingParticipationInfo getMeetingParticipationInfo(Long meetingId, Long userId) {
        return meetingAuthorizationService.getUserMeetingPermissions(meetingId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUserAccessMeeting(Long meetingId, Long userId) {
        try {
            MeetingParticipationInfo info = meetingAuthorizationService.getUserMeetingPermissions(meetingId, userId);
            return info.isCanViewDetails();
        } catch (Exception e) {
            log.warn("Error checking meeting access for user {} to meeting {}: {}", userId, meetingId, e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeetingResponse> getAccessibleMeetings(Long userId) {
        List<Meeting> allMeetings = meetingRepository.findAll();

        return allMeetings.stream()
                .filter(meeting -> {
                    try {
                        return meetingAuthorizationService.canUserViewResource(meeting.getId(), userId);
                    } catch (Exception e) {
                        log.warn("Error checking access to meeting {} for user {}: {}", meeting.getId(), userId, e.getMessage());
                        return false;
                    }
                })
                .map(meetingMapper::toResponse)
                .toList();
    }

    // ✅ NOWE METODY DLA DODATKOWYCH FUNKCJI

    @Transactional(readOnly = true)
    @Override
    public List<MeetingResponse> getMeetingTemplates(Long userId) {
        log.info("Getting templates for user {}", userId);
        List<Meeting> templates = meetingRepository.findByOrganizerIdAndTemplateTrue(userId);
        return templates.stream()
                .map(meetingMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public MeetingResponse createFromTemplate(Long templateId, Long organizerId, LocalDateTime newStartDate) {
        log.info("Creating meeting from template {} by user {} with start date {}",
                templateId, organizerId, newStartDate);

        Meeting template = meetingRepository.findByIdAndTemplateTrue(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found or not a template"));

        // ✅ STWÓRZ NOWE SPOTKANIE NA PODSTAWIE SZABLONU
        Meeting meeting = meetingMapper.cloneMeeting(template);

        // ✅ USTAW ORGANIZATORA
        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        meeting.setOrganizer(organizer);

        // ✅ USTAW DATY
        meeting.setStartDate(newStartDate);

        // OBLICZ ENDDATE NA PODSTAWIE DŁUGOŚCI TRWANIA SZABLONU
        long durationMinutes = Duration.between(template.getStartDate(), template.getEndDate()).toMinutes();
        meeting.setEndDate(newStartDate.plusMinutes(durationMinutes));

        validateMeetingDates(meeting.getStartDate(), meeting.getEndDate());

        meeting.setTemplate(false);
        meeting.setOriginalMeetingId(templateId);

        Meeting savedMeeting = meetingRepository.save(meeting);

        saveStatusChange(savedMeeting, null, savedMeeting.getStatus().name(),
                organizerId, "Utworzono z szablonu #" + templateId);

        log.info("Created meeting {} from template {}", savedMeeting.getId(), templateId);

        return meetingMapper.toResponse(savedMeeting);
    }

    @Transactional
    @Override
    public List<MeetingResponse> generateNextRecurrence(Long meetingId, int count) {
        log.info("Generating {} next occurrences for meeting {}", count, meetingId);

        Meeting template = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        if (!template.isRecurring() || template.getRecurrencePattern() == null) {
            throw new BusinessException("This meeting is not recurring");
        }

        List<Meeting> occurrences = new ArrayList<>();
        LocalDateTime nextDate = getLastOccurrenceDate(template);

        for (int i = 0; i < count; i++) {
            nextDate = calculateNextDate(nextDate, template.getRecurrencePattern());

            // ✅ SPRAWDŹ WYJĄTKI
            if (isExceptionDate(nextDate, template.getRecurrenceExceptionsJson())) {
                log.info("Skipping exception date: {}", nextDate);
                continue;
            }

            Meeting occurrence = createOccurrenceFromTemplate(template, nextDate);
            occurrences.add(occurrence);
        }

        if (!occurrences.isEmpty()) {
            List<Meeting> savedOccurrences = meetingRepository.saveAll(occurrences);
            log.info("Generated {} occurrences for meeting {}", savedOccurrences.size(), meetingId);

            return savedOccurrences.stream()
                    .map(meetingMapper::toResponse)
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    @Transactional
    @Override
    public void addRecurrenceException(Long meetingId, String exceptionDate, String reason) {
        log.info("Adding recurrence exception {} for meeting {}", exceptionDate, meetingId);

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        if (!meeting.isRecurring()) {
            throw new BusinessException("This meeting is not recurring");
        }

        // ✅ PARSOWANIE I DODAWANIE WYJĄTKU DO JSON
        List<String> exceptions = parseRecurrenceExceptions(meeting.getRecurrenceExceptionsJson());
        exceptions.add(exceptionDate);

        try {
            String newExceptionsJson = new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(exceptions);
            meeting.setRecurrenceExceptionsJson(newExceptionsJson);
            meetingRepository.save(meeting);

            log.info("Added exception {} for meeting {}", exceptionDate, meetingId);
        } catch (Exception e) {
            throw new BusinessException("Failed to add recurrence exception: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    @Override
    public List<MeetingResponse> getRecurrenceSeries(Long originalMeetingId) {
        log.info("Getting recurrence series for original meeting {}", originalMeetingId);

        List<Meeting> series = meetingRepository.findByOriginalMeetingId(originalMeetingId);
        return series.stream()
                .map(meetingMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ✅ METODY POMOCNICZE

    private void saveStatusChange(Meeting meeting, String oldStatus, String newStatus,
                                  Long userId, String reason) {
        try {
            com.meethub.domain.model.entity.MeetingStatusChange statusChange =
                    com.meethub.domain.model.entity.MeetingStatusChange.builder()
                            .meeting(meeting)
                            .oldStatus(oldStatus)
                            .newStatus(newStatus)
                            .changedByUserId(userId)
                            .reason(reason)
                            .changedAt(LocalDateTime.now())
                            .build();

            if (meeting.getStatusChanges() == null) {
                meeting.setStatusChanges(new ArrayList<>());
            }
            meeting.getStatusChanges().add(statusChange);

            log.debug("Saved status change: {} -> {} for meeting {}",
                    oldStatus, newStatus, meeting.getId());
        } catch (Exception e) {
            log.error("Failed to save status change for meeting {}: {}", meeting.getId(), e.getMessage());
        }
    }

    private LocalDateTime calculateNextDate(LocalDateTime currentDate, String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return currentDate.plusDays(1);
        }

        String[] parts = pattern.split(":");
        String frequency = parts[0];

        switch (frequency) {
            case "DAILY":
                int days = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                return currentDate.plusDays(days);

            case "WEEKLY":
                int weeks = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                return currentDate.plusWeeks(weeks);

            case "MONTHLY":
                int months = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                return currentDate.plusMonths(months);

            case "YEARLY":
                int years = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                return currentDate.plusYears(years);

            default:
                return currentDate.plusDays(1);
        }
    }

    private LocalDateTime getLastOccurrenceDate(Meeting meeting) {
        // Szukaj ostatniego wystąpienia w serii
        List<Meeting> series = meetingRepository.findByOriginalMeetingId(meeting.getId());
        if (series.isEmpty()) {
            return meeting.getStartDate();
        }

        return series.stream()
                .max(Comparator.comparing(Meeting::getStartDate))
                .map(Meeting::getStartDate)
                .orElse(meeting.getStartDate());
    }

    private boolean isExceptionDate(LocalDateTime date, String exceptionsJson) {
        if (exceptionsJson == null || exceptionsJson.isEmpty()) {
            return false;
        }

        List<String> exceptions = parseRecurrenceExceptions(exceptionsJson);
        String dateStr = date.toLocalDate().toString();
        return exceptions.contains(dateStr);
    }

    private List<String> parseRecurrenceExceptions(String json) {
        if (json == null || json.isEmpty() || json.equals("[]")) {
            return new ArrayList<>();
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
                    });
        } catch (Exception e) {
            log.warn("Failed to parse recurrence exceptions JSON: {}", json, e);
            return new ArrayList<>();
        }
    }

    private Meeting createOccurrenceFromTemplate(Meeting template, LocalDateTime startDate) {
        Meeting occurrence = meetingMapper.cloneMeeting(template);

        occurrence.setStartDate(startDate);

        // OBLICZ ENDDATE NA PODSTAWIE DŁUGOŚCI TRWANIA
        long durationMinutes = Duration.between(template.getStartDate(), template.getEndDate()).toMinutes();
        occurrence.setEndDate(startDate.plusMinutes(durationMinutes));

        occurrence.setOriginalMeetingId(template.getId());
        occurrence.setTemplate(false);

        return occurrence;
    }
//
//    @Transactional(readOnly = true)
//    @Override
//    public Page<MeetingResponse> getMeetingsByCategory(Long categoryId, Pageable pageable) {
//        log.info("Getting meetings by category {}", categoryId);
//
//        Page<Meeting> meetings = meetingRepository.findByCategoryId(categoryId, pageable);
//        return meetings.map(meetingMapper::toResponse);
//    }

    @Transactional(readOnly = true)
    @Override
    public Page<MeetingResponse> getMeetingsByTag(String tag, Pageable pageable) {
        log.info("Getting meetings by tag {}", tag);

        Page<Meeting> meetings = meetingRepository.findByTag(tag, pageable);
        return meetings.map(meetingMapper::toResponse);
    }

    @Transactional(readOnly = true)
    @Override
    public List<MeetingResponse> getUpcomingRecurringMeetings(Long userId) {
        log.info("Getting upcoming recurring meetings for user {}", userId);

        List<Meeting> meetings = meetingRepository.findByRecurringTrueAndRecurrenceEndDateAfter(LocalDateTime.now());

        return meetings.stream()
                .filter(meeting -> meeting.getOrganizer().getId().equals(userId) ||
                        meetingParticipantService.isUserParticipant(meeting.getId(), userId))
                .map(meetingMapper::toResponse)
                .collect(Collectors.toList());
    }


    @Override
    @Transactional
    public MeetingResponse saveAsTemplate(Long meetingId, String templateName, Long userId) {
        log.info("Saving meeting {} as template by user {}", meetingId, userId);

        if (!meetingAuthorizationService.canUserEditMeeting(meetingId, userId)) {
            throw new BusinessException("No permission to save this meeting as template");
        }

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));

        Meeting template = meetingMapper.createTemplateFromMeeting(meeting, templateName);
        template.setOrganizer(userRepository.findById(userId).orElseThrow());

        Meeting savedTemplate = meetingRepository.save(template);
        log.info("Saved meeting {} as template {}", meetingId, savedTemplate.getId());

        return meetingMapper.toResponse(savedTemplate);
    }

//
//    @Override
//    public Page<MeetingResponse> getAdvancedMeetings(
//            CustomUserDetailsService.CustomUserDetails userDetails,
//            int page,
//            int size,
//            String search,
//            String type,
//            String status,
//            List<Long> categoryIds,
//            List<String> tags,
//            Boolean recurring,
//            Boolean template,
//            String sortBy,
//            String sortOrder
//    ) {
//
//        // 1. Tworzymy SearchCriteria
//        SearchCriteria criteria = new SearchCriteria();
//        criteria.setKeywords(search);
//        criteria.setType(MeetingType.fromDisplayName(type));
//        criteria.setStatuses(status != null ? List.of(String.valueOf(MeetingStatus.fromDisplayName(status))) : null);
//        criteria.setCategoryIds(categoryIds);
//        criteria.setTags(tags != null ? String.join(",", tags) : null);
//
//
//        // 2. Tworzymy Specification
//        Specification<Meeting> spec = new MeetingSpecification(criteria, userDetails.getId());
//
//        // 3. Pageable z sortowaniem w SQL
//        Pageable pageable = PageRequest.of(
//                page,
//                size,
//                Sort.by("asc".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC,
//                        getSortField(sortBy))
//        );
//
//        // 4. Pobieramy stronę z repozytorium
//        Page<Meeting> meetingsPage = meetingRepository.findAll(spec, pageable);
//
//        // 5. Mapujemy na DTO
//        return meetingsPage.map(meeting -> MeetingResponse.fromEntity(meeting, userDetails.getId()));
//    }
//
//    private String getSortField(String sortBy) {
//        if (sortBy == null || sortBy.isEmpty()) return "startDate";
//        switch (sortBy) {
//            case "title": return "title";
//            case "created": return "createdAt";
//            case "updated": return "updatedAt";
//            case "participants": return "confirmedParticipantsCount";
//            default: return "startDate";
//        }
//    }
//
//
//    @Override
//    public Page<MeetingResponse> getAdvancedMeetings(
//            CustomUserDetailsService.CustomUserDetails userDetails,
//            int page,
//            int size,
//            String search,
//            String type,
//            String status,
//            List<Long> categoryIds,
//            List<String> tags,
//            Boolean recurring,
//            Boolean template,
//            String sortBy,
//            String sortOrder) {
//
//        SearchCriteria criteria = SearchCriteria.builder()
//                .keywords(search)
//                .type(MeetingType.fromDisplayName(type))
//                .statuses(status != null ?
//                        List.of(String.valueOf(MeetingStatus.fromDisplayName(status))) :
//                        new ArrayList<>())
//                .categoryIds(categoryIds != null ? categoryIds : new ArrayList<>())
//                .tags(tags != null ? String.join(",", tags) : null)
//                .recurringOnly(recurring)
//                .templatesOnly(template)
//                .currentUserId(userDetails.getId())
//                .build();
//
//        Sort sort = Sort.by(
//                "asc".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC,
//                getSortField(sortBy)
//        );
//
//        Pageable pageable = PageRequest.of(page, size, sort);
//
//        return searchMeetings(criteria, pageable);
//    }
//
//    private String getSortField(String sortBy) {
//        if (sortBy == null || sortBy.isEmpty()) return "startDate";
//        switch (sortBy) {
//            case "title":
//                return "title";
//            case "created":
//                return "createdAt";
//            case "updated":
//                return "updatedAt";
//            default:
//                return "startDate";
//        }
//    }


//    @Override
//    @Transactional(readOnly = true)
//    public Page<MeetingResponse> searchMeetings(SearchCriteria criteria, Pageable pageable) {
//        log.info("🔍 Starting advanced search with criteria: {}", criteria);
//
//        try {
//            // ✅ Sprawdź czy userId jest w criteria
//            if (criteria.getCurrentUserId() == null) {
//                log.warn("User ID not set in search criteria");
//                throw new BusinessException("User ID is required for search");
//            }
//
//            // ✅ Utwórz specyfikację
//            Specification<Meeting> spec = new MeetingSpecification(criteria, criteria.getCurrentUserId());
//
//            // ✅ Wykonaj zapytanie
//            Page<Meeting> meetingsPage = meetingRepository.findAll(spec, pageable);
//
//            log.info("✅ Found {} meetings for user {}", meetingsPage.getTotalElements(), criteria.getCurrentUserId());
//
//            // ✅ Mapuj na responsy
//            return meetingsPage.map(meetingMapper::toResponse);
//
//        } catch (Exception e) {
//            log.error("❌ Error in advanced search: {}", e.getMessage(), e);
//            throw new BusinessException("Error during search: " + e.getMessage());
//        }
//    }




    @Override
    @Transactional(readOnly = true)
    public Page<MeetingResponse> searchMeetings(SearchCriteria criteria, Pageable pageable) {
        log.info("🔍 Dynamic search with criteria: {}", criteria);

        try {
            if (criteria.getCurrentUserId() == null) {
                throw new BusinessException("User ID is required for search");
            }

            Specification<Meeting> spec = buildDynamicSpecification(criteria);
            Page<Meeting> meetingsPage = meetingRepository.findAll(spec, pageable);
            log.info("✅ Found {} meetings", meetingsPage.getTotalElements());

            return meetingsPage.map(meetingMapper::toResponse);

        } catch (Exception e) {
            log.error("❌ Search error", e);
            throw new BusinessException("Search failed: " + e.getMessage());
        }
    }

    private Specification<Meeting> buildDynamicSpecification(SearchCriteria criteria) {
        Specification<Meeting> spec = Specification.where(null);

        log.info("CRITERIA TYPE = {}", criteria.getType());


        spec = spec.and((root, query, cb) -> {
            query.distinct(true);
            return null;
        });

        spec = spec.and(MeetingSpecification.accessibleToUser(criteria.getCurrentUserId()));

        if (criteria.hasKeywords()) {
            spec = spec.and(MeetingSpecification.hasKeywords(
                    criteria.getKeywords(),
                    criteria.getSearchFields()
            ));
        }

        if (criteria.hasTags()) {
            spec = spec.and((root, query, cb) ->
                    MeetingSpecification.buildTagsPredicate(root, cb, criteria.getTags())
            );
        }

        if (criteria.hasDateRange()) {
            spec = spec.and((root, query, cb) ->
                    MeetingSpecification.buildDateRangePredicate(root, cb,
                            criteria.getDateFrom(), criteria.getDateTo())
            );
        }

        if (criteria.hasType()) {
            spec = spec.and(MeetingSpecification.byType(criteria.getType()));
        }

        if (criteria.hasStatuses()) {
            spec = spec.and(MeetingSpecification.byStatuses(criteria.getStatuses()));
        }

        if (criteria.getMinParticipants() != null && criteria.getMinParticipants() > 0) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("maxParticipants"), criteria.getMinParticipants())
            );
        }

        if (criteria.getMaxParticipants() != null && criteria.getMaxParticipants() < 100) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("maxParticipants"), criteria.getMaxParticipants())
            );
        }

        if (criteria.hasOrganizerFilter()) {
            spec = spec.and((root, query, cb) ->
                    MeetingSpecification.buildOrganizerPredicate(root, query, cb, criteria.getOrganizerName())
            );
        }

//        if (criteria.hasCategoryFilter()) {
//            spec = spec.and(MeetingSpecification.byCategories(criteria.getCategoryIds()));
//        }

        if (criteria.hasRecurringFilter()) {
            spec = spec.and(MeetingSpecification.isRecurring());
        }

        if (criteria.hasTemplatesFilter()) {
            spec = spec.and(MeetingSpecification.isTemplate());
        }

        if (criteria.hasAttachmentsFilter()) {
            spec = spec.and(MeetingSpecification.hasAttachments());
        }

        if (criteria.hasMyParticipationFilter()) {
            spec = handleMyParticipation(spec, criteria);
        }

        if (criteria.hasVisibilityFilter()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("visibility"), criteria.getVisibility())
            );
        }

        return spec;
    }

    private Specification<Meeting> handleMyParticipation(Specification<Meeting> spec, SearchCriteria criteria) {
        Long userId = criteria.getCurrentUserId();
        String participation = criteria.getMyParticipation();

        switch (participation) {
            case "ORGANIZER":
                return spec.and((root, query, cb) ->
                        cb.equal(root.get("organizer").get("id"), userId)
                );

//            case "NOT_PARTICIPATING":
//                return spec.and((root, query, cb) ->
//                        MeetingSpecification.buildNotParticipatingPredicate(root, query, cb, userId)
//                );
//
            case "CONFIRMED":
                return spec.and((root, query, cb) ->
                        MeetingSpecification.buildParticipationStatusPredicate(root, query, cb, userId,
                                ParticipationStatus.CONFIRMED)
                );

            case "PENDING":
                return spec.and((root, query, cb) ->
                        MeetingSpecification.buildParticipationStatusPredicate(root, query, cb, userId,
                                ParticipationStatus.PENDING)
                );

            case "INVITED":
                return spec.and((root, query, cb) ->
                        MeetingSpecification.buildParticipationStatusPredicate(root, query, cb, userId,
                                ParticipationStatus.INVITED)
                );

            case "DECLINED":
                return spec.and((root, query, cb) ->
                        MeetingSpecification.buildParticipationStatusPredicate(root, query, cb, userId,
                                ParticipationStatus.DECLINED)
                );

            case "WAITING_LIST":
                return spec.and((root, query, cb) ->
                        MeetingSpecification.buildParticipationStatusPredicate(root, query, cb, userId,
                                ParticipationStatus.WAITING_LIST)
                );

            case "ATTENDED":
                return spec.and((root, query, cb) ->
                        MeetingSpecification.buildParticipationStatusPredicate(root, query, cb, userId,
                                ParticipationStatus.ATTENDED)
                );

            default:
                return spec;
        }
    }


    private void validateMeetingDates(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate != null && startDate.isBefore(LocalDateTime.now())) {
            throw new BusinessException("Nie można utworzyć spotkania w przeszłości.");
        }
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessException("Data zakończenia nie może być wcześniejsza niż data rozpoczęcia.");
        }
    }

    @Override
    public MeetingResponse getMeetingDetails(Long meetingId, Long userId) {

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("Spotkanie nie zostało znalezione"));

        return MeetingResponse.fromEntity(meeting, userId);
    }

    @Override
    public MeetingResponse getMeetingForVotingCreation(Long meetingId, Long userId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("Spotkanie nie zostało znalezione"));

        if (!meeting.getOrganizer().getId().equals(userId)) {
            throw new IllegalStateException("Tylko organizator może tworzyć głosowania");
        }

        return MeetingResponse.fromEntity(meeting, userId);
    }
}







//
//package com.meethub.domain.service.impl;
//
//import com.meethub.domain.model.entity.*;
//import com.meethub.domain.model.enums.MeetingStatus;
//import com.meethub.domain.model.enums.ParticipationStatus;
//import com.meethub.domain.model.request.CreateMeetingRequest;
//import com.meethub.domain.model.request.SearchCriteria;
//import com.meethub.domain.model.request.UpdateMeetingRequest;
//import com.meethub.domain.model.response.MeetingParticipationInfo;
//import com.meethub.domain.model.response.MeetingResponse;
//import com.meethub.domain.repository.jpa.CategoryRepository;
//import com.meethub.domain.repository.jpa.MeetingRepository;
//import com.meethub.domain.repository.jpa.UserRepository;
//import com.meethub.domain.repository.jdbc.CustomMeetingRepository;
//import com.meethub.domain.repository.specification.MeetingSpecification;
//import com.meethub.domain.service.MeetingAuthorizationService;
//import com.meethub.domain.service.MeetingParticipantService;
//import com.meethub.domain.service.MeetingSchedulerService;
//import com.meethub.domain.service.MeetingService;
//import com.meethub.exception.BusinessException;
//import com.meethub.exception.ResourceNotFoundException;
//import com.meethub.domain.model.mapper.MeetingMapper;
//import jakarta.validation.Valid;
//import jakarta.validation.constraints.*;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.domain.*;
//import org.springframework.data.jpa.domain.Specification;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.validation.annotation.Validated;
//
//import java.time.Duration;
//import java.time.LocalDateTime;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Validated
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class MeetingServiceImpl implements MeetingService {
//
//    private final MeetingRepository meetingRepository;
//    private final UserRepository userRepository;
//    private final CustomMeetingRepository customMeetingRepository;
//    private final MeetingMapper meetingMapper;
//    private final MeetingParticipantService meetingParticipantService;
//    private final MeetingAuthorizationService meetingAuthorizationService;
//    private final CategoryRepository categoryRepository;
//    private final MeetingSchedulerService meetingSchedulerService;
//
//    @Override
//    @Transactional
//    public MeetingResponse createMeeting(
//            @NotNull(message = "Żądanie utworzenia spotkania nie może być puste")
//            @Valid CreateMeetingRequest request,
//            @NotNull(message = "Identyfikator organizatora nie może być pusty")
//            @Min(value = 1, message = "Identyfikator organizatora musi być liczbą dodatnią")
//            Long organizerId) {
//        log.info("=== ROZPOCZĘCIE TWORZENIA SPOTKANIA ===");
//        log.info("Organizator ID: {}, Tytuł: {}, Cykliczne: {}",
//                organizerId, request.getTitle(), request.isRecurring());
//
//        User organizer = userRepository.findById(organizerId)
//                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono użytkownika z id: " + organizerId));
//
//        log.info("Znaleziono organizatora: {} {} (ID: {})",
//                organizer.getFirstName(), organizer.getLastName(), organizer.getId());
//
//        Meeting meeting = meetingMapper.toEntity(request);
//
//        // ✅ USTAW ORGANIZATORA I STATUS
//        meeting.setOrganizer(organizer);
//        meeting.setStatus(MeetingStatus.PLANNED);
//
//        validateMeetingDates(meeting.getStartDate(), meeting.getEndDate());
//
//        // ✅ OBSŁUGA KATEGORII
//        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
//            Set<Category> categories = new HashSet<>(categoryRepository.findAllById(request.getCategoryIds()));
//            meeting.setCategories(categories);
//            log.info("Dodano {} kategorii do spotkania", categories.size());
//        }
//
//        // ✅ OBSŁUGA SZABLONU
//        if (request.isSaveAsTemplate()) {
//            meeting.setTemplate(true);
//            meeting.setTitle(request.getTemplateName() != null ?
//                    request.getTemplateName() : meeting.getTitle() + " (Szablon)");
//            log.info("Spotkanie zapisane jako szablon: {}", meeting.getTitle());
//        }
//
//        log.info("Przed zapisem - Tytuł: {}, Cykliczne: {}, Wzorzec: {}",
//                meeting.getTitle(), meeting.isRecurring(), meeting.getRecurrencePattern());
//
//        try {
//            Meeting savedMeeting = meetingRepository.save(meeting);
//            meetingParticipantService.addOrganizerAsParticipant(savedMeeting, organizer);
//
//            // ✅ ZAPISZ POCZĄTKOWY STATUS W HISTORII
//            saveStatusChange(savedMeeting, null, savedMeeting.getStatus().name(),
//                    organizerId, "Utworzono spotkanie");
//
//            log.info("=== SPOTKANIE UTWORZONE POMYŚLNIE ===");
//            log.info("Spotkanie ID: {}, Tytuł: {}, Cykliczne: {}",
//                    savedMeeting.getId(), savedMeeting.getTitle(), savedMeeting.isRecurring());
//
//            // ✅ GENERUJ NASTĘPNE WYSTĄPIENIA DLA POWTARZAJĄCYCH SIĘ SPOTKAŃ
//            if (savedMeeting.isRecurring() && savedMeeting.getRecurrencePattern() != null) {
//                generateNextRecurrence(savedMeeting.getId(), 3); // Generuj 3 następne
//            }
//
//            meetingSchedulerService.scheduleMeetingNotifications(savedMeeting);
//
//            log.info("✅ Utworzono spotkanie {} i zaplanowano powiadomienia", savedMeeting.getId());
//
//            return meetingMapper.toResponse(savedMeeting);
//        } catch (Exception e) {
//            log.error("=== BŁĄD PODCZAS TWORZENIA SPOTKANIA ===");
//            log.error("Błąd: {}", e.getMessage(), e);
//            throw new BusinessException("Błąd podczas tworzenia spotkania: " + e.getMessage());
//        }
//    }
//
//    @Override
//    @Transactional
//    public MeetingResponse updateMeeting(
//            @NotNull(message = "Identyfikator spotkania nie może być pusty")
//            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
//            Long meetingId,
//            @NotNull(message = "Żądanie aktualizacji nie może być puste")
//            @Valid UpdateMeetingRequest request,
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Min(value = 1, message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//        log.info("Aktualizacja spotkania {} przez użytkownika {}", meetingId, userId);
//
//        // 1. SPRAWDŹ UPRAWNIENIA
//        if (!meetingAuthorizationService.canUserEditMeeting(meetingId, userId)) {
//            throw new BusinessException("Brak uprawnień do edycji tego spotkania");
//        }
//
//        Meeting meeting = meetingRepository.findById(meetingId)
//                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono spotkania z id: " + meetingId));
//
//        // 2. ZAPISZ STARE DANE DO PORÓWNANIA
//        MeetingStatus oldStatus = meeting.getStatus();
//        LocalDateTime oldStartDate = meeting.getStartDate();
//        LocalDateTime oldEndDate = meeting.getEndDate();
//        String oldTitle = meeting.getTitle();
//        Location oldLocation = meeting.getLocation() != null ?
//                new Location(meeting.getLocation()) : null;
//
//        // 3. ZAPISZ ZMIANĘ STATUSU W HISTORII
//        if (request.getStatus() != null && !request.getStatus().equals(oldStatus)) {
//            saveStatusChange(meeting, oldStatus.name(),
//                    request.getStatus().name(), userId, request.getStatusChangeReason());
//
//            // Jeśli zmieniasz status na CANCELLED - anuluj powiadomienia
//            if (request.getStatus() == MeetingStatus.CANCELLED) {
//                meetingSchedulerService.cancelMeetingSchedule(meetingId);
//                log.info("❌ Anulowano powiadomienia dla spotkania {} (status zmieniony na ANULOWANY)",
//                        meetingId);
//            }
//            // Jeśli zmieniasz status z CANCELLED na PLANNED - zaplanuj powiadomienia
//            else if (oldStatus == MeetingStatus.CANCELLED && request.getStatus() == MeetingStatus.PLANNED) {
//                meetingSchedulerService.scheduleMeetingNotifications(meeting);
//                log.info("🔄 Zaplanowano powiadomienia dla spotkania {} (status zmieniony z ANULOWANY na ZAPLANOWANY)",
//                        meetingId);
//            }
//        }
//
//        // 4. AKTUALIZUJ KATEGORIE (jeśli podano)
//        if (request.getCategoryIds() != null) {
//            Set<Category> categories = new HashSet<>(categoryRepository.findAllById(request.getCategoryIds()));
//            meeting.setCategories(categories);
//            log.info("Zaktualizowano kategorie dla spotkania {}: {} kategorii", meetingId, categories.size());
//        }
//
//        // 5. AKTUALIZUJ POZOSTAŁE POLA
//        meetingMapper.updateEntityFromRequest(request, meeting);
//
//        // 6. ZAPISZ ZMIANY
//        Meeting updatedMeeting = meetingRepository.save(meeting);
//        log.info("Spotkanie {} zaktualizowane przez użytkownika {}", meetingId, userId);
//
//        // 7. SPRAWDŹ CZY ZMIENIŁA SIĘ DATA I PRZEPLANUJ POWIADOMIENIA
//        handleDateChanges(meetingId, updatedMeeting, oldStartDate, oldEndDate, userId);
//
//        return meetingMapper.toResponse(updatedMeeting);
//    }
//
//    private void handleDateChanges(Long meetingId, Meeting updatedMeeting,
//                                   LocalDateTime oldStartDate, LocalDateTime oldEndDate,
//                                   Long userId) {
//        boolean dateChanged = false;
//
//        // Sprawdź czy zmieniła się data rozpoczęcia
//        if (oldStartDate != null && updatedMeeting.getStartDate() != null &&
//                !oldStartDate.equals(updatedMeeting.getStartDate())) {
//            dateChanged = true;
//            log.info("📅 Zmieniono datę rozpoczęcia spotkania {} z {} na {}",
//                    meetingId, oldStartDate, updatedMeeting.getStartDate());
//        }
//
//        // Sprawdź czy zmieniła się data zakończenia
//        if (oldEndDate != null && updatedMeeting.getEndDate() != null &&
//                !oldEndDate.equals(updatedMeeting.getEndDate())) {
//            dateChanged = true;
//            log.info("📅 Zmieniono datę zakończenia spotkania {} z {} na {}",
//                    meetingId, oldEndDate, updatedMeeting.getEndDate());
//        }
//
//        // Jeśli zmieniono datę - przepłań powiadomienia
//        if (dateChanged && updatedMeeting.getStatus() == MeetingStatus.PLANNED) {
//            // 1. Anuluj stare powiadomienia
//            meetingSchedulerService.cancelMeetingSchedule(meetingId);
//
//            // 2. Zaplanuj nowe
//            meetingSchedulerService.scheduleMeetingNotifications(updatedMeeting);
//
//            log.info("🔄 Przeplanowano powiadomienia dla spotkania {} po zmianie daty", meetingId);
//        }
//    }
//
//    @Override
//    @Transactional
//    public void deleteMeeting(
//            @NotNull(message = "Identyfikator spotkania nie może być pusty")
//            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
//            Long meetingId,
//            @NotNull(message = "Identyfikator organizatora nie może być pusty")
//            @Min(value = 1, message = "Identyfikator organizatora musi być liczbą dodatnią")
//            Long organizerId) {
//        // ✅ SPRAWDŹ UPRAWNIENIA
//        if (!meetingAuthorizationService.canUserDeleteMeeting(meetingId, organizerId)) {
//            throw new BusinessException("Brak uprawnień do usunięcia tego spotkania");
//        }
//
//        Meeting meeting = meetingRepository.findByIdAndOrganizerId(meetingId, organizerId)
//                .orElseThrow(() -> new ResourceNotFoundException(
//                        "Nie znaleziono spotkania z id: " + meetingId + " dla organizatora: " + organizerId));
//
//        meetingRepository.delete(meeting);
//        log.info("Spotkanie {} usunięte przez organizatora {}", meetingId, organizerId);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public MeetingResponse getMeetingById(
//            @NotNull(message = "Identyfikator spotkania nie może być pusty")
//            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
//            Long meetingId) {
//        Meeting meeting = meetingRepository.findById(meetingId)
//                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono spotkania z id: " + meetingId));
//
//        return meetingMapper.toResponse(meeting);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public Page<MeetingResponse> getUserMeetings(
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Min(value = 1, message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId,
//            @NotNull(message = "Stronicowanie nie może być puste")
//            Pageable pageable) {
//        Page<Meeting> meetings = meetingRepository.findByOrganizerId(userId, pageable);
//        return meetings.map(meetingMapper::toResponse);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<MeetingResponse> getUpcomingPublicMeetings() {
//        List<Meeting> meetings = meetingRepository.findUpcomingPublicMeetings(LocalDateTime.now());
//        return meetings.stream()
//                .map(meetingMapper::toResponse)
//                .toList();
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<MeetingResponse> findNearbyMeetings(
//            @NotNull(message = "Szerokość geograficzna nie może być pusta")
//            @DecimalMin(value = "-90.0", message = "Szerokość geograficzna musi być między -90.0 a 90.0")
//            @DecimalMax(value = "90.0", message = "Szerokość geograficzna musi być między -90.0 a 90.0")
//            double latitude,
//            @NotNull(message = "Długość geograficzna nie może być pusta")
//            @DecimalMin(value = "-180.0", message = "Długość geograficzna musi być między -180.0 a 180.0")
//            @DecimalMax(value = "180.0", message = "Długość geograficzna musi być między -180.0 a 180.0")
//            double longitude,
//            @NotNull(message = "Promień nie może być pusty")
//            @Min(value = 1, message = "Promień musi być liczbą dodatnią")
//            double radius) {
//        List<Meeting> meetings = customMeetingRepository.findNearbyMeetings(latitude, longitude, radius, 50);
//        return meetings.stream()
//                .map(meetingMapper::toResponse)
//                .toList();
//    }
//
//    @Override
//    @Transactional
//    public void changeMeetingStatus(
//            @NotNull(message = "Identyfikator spotkania nie może być pusty")
//            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
//            Long meetingId,
//            @NotNull(message = "Status nie może być pusty")
//            MeetingStatus status,
//            @NotNull(message = "Identyfikator organizatora nie może być pusty")
//            @Min(value = 1, message = "Identyfikator organizatora musi być liczbą dodatnią")
//            Long organizerId) {
//        // ✅ SPRAWDŹ UPRAWNIENIA
//        if (!meetingAuthorizationService.canUserEditMeeting(meetingId, organizerId)) {
//            throw new BusinessException("Brak uprawnień do zmiany statusu tego spotkania");
//        }
//
//        Meeting meeting = meetingRepository.findById(meetingId)
//                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono spotkania z id: " + meetingId));
//
//        String oldStatus = meeting.getStatus().name();
//        meeting.setStatus(status);
//        meetingRepository.save(meeting);
//
//        // ✅ ZAPISZ W HISTORII
//        saveStatusChange(meeting, oldStatus, status.name(), organizerId, "Zmiana statusu");
//
//        log.info("Status spotkania {} zmieniony z {} na {} przez {}",
//                meetingId, oldStatus, status, organizerId);
//    }
//
//    @Override
//    @Transactional
//    public MeetingResponse duplicateMeeting(
//            @NotNull(message = "Identyfikator spotkania nie może być pusty")
//            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
//            Long meetingId,
//            @NotNull(message = "Identyfikator organizatora nie może być pusty")
//            @Min(value = 1, message = "Identyfikator organizatora musi być liczbą dodatnią")
//            Long organizerId) {
//        log.info("Duplikowanie spotkania {} przez użytkownika {}", meetingId, organizerId);
//
//        // ✅ SPRAWDŹ UPRAWNIENIA
//        if (!meetingAuthorizationService.canUserEditMeeting(meetingId, organizerId)) {
//            throw new BusinessException("Brak uprawnień do duplikowania tego spotkania");
//        }
//
//        Meeting original = meetingRepository.findById(meetingId)
//                .orElseThrow(() -> new ResourceNotFoundException(
//                        "Nie znaleziono spotkania z id: " + meetingId));
//
//        // ✅ UŻYJ MAPPERA DO KOPIOWANIA
//        Meeting duplicate = meetingMapper.cloneMeeting(original);
//
//        // ✅ USTAW NOWEGO ORGANIZATORA
//        User organizer = userRepository.findById(organizerId)
//                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono użytkownika z id: " + organizerId));
//        duplicate.setOrganizer(organizer);
//
//        // ✅ USTAW DATĘ NA ZA TYDZIEŃ
//        duplicate.setStartDate(original.getStartDate().plusDays(7));
//        duplicate.setEndDate(original.getEndDate().plusDays(7));
//
//        // ✅ ZACHOWAJ KATEGORIE Z ORYGINAŁU
//        if (original.getCategories() != null) {
//            duplicate.setCategories(new HashSet<>(original.getCategories()));
//        }
//
//        // ✅ USTAW REFERENCJĘ DO ORYGINAŁU
//        duplicate.setOriginalMeetingId(original.getId());
//
//        Meeting savedDuplicate = meetingRepository.save(duplicate);
//
//        // ✅ ZAPISZ W HISTORII
//        saveStatusChange(savedDuplicate, null, savedDuplicate.getStatus().name(),
//                organizerId, "Utworzono jako kopia spotkania #" + original.getId());
//
//        log.info("Spotkanie zduplikowane z {} do nowego id: {}", meetingId, savedDuplicate.getId());
//
//        return meetingMapper.toResponse(savedDuplicate);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<MeetingResponse> findConflictingMeetings(
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Min(value = 1, message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId,
//            @NotNull(message = "Data rozpoczęcia nie może być pusta")
//            LocalDateTime startDate,
//            @NotNull(message = "Data zakończenia nie może być pusta")
//            @Future(message = "Data zakończenia musi być w przyszłości")
//            LocalDateTime endDate) {
//        List<Meeting> conflicts = meetingRepository.findConfirmedMeetingsForUserInPeriod(userId, startDate, endDate);
//        return conflicts.stream()
//                .map(meetingMapper::toResponse)
//                .toList();
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public Page<MeetingResponse> getFilteredMeetings(
//            String search,
//            String type,
//            String status,
//            @NotNull(message = "Stronicowanie nie może być puste")
//            Pageable pageable) {
//        Page<Meeting> meetings = customMeetingRepository.findFilteredMeetings(search, type, status, pageable);
//        return meetings.map(meetingMapper::toResponse);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public Meeting getMeeting(
//            @NotNull(message = "Identyfikator spotkania nie może być pusty")
//            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
//            Long meetingId) {
//        log.debug("Pobieranie spotkania po ID: {}", meetingId);
//
//        return meetingRepository.findById(meetingId)
//                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono spotkania z id: " + meetingId));
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public MeetingParticipationInfo getMeetingParticipationInfo(
//            @NotNull(message = "Identyfikator spotkania nie może być pusty")
//            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
//            Long meetingId,
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Min(value = 1, message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//        return meetingAuthorizationService.getUserMeetingPermissions(meetingId, userId);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public boolean canUserAccessMeeting(
//            @NotNull(message = "Identyfikator spotkania nie może być pusty")
//            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
//            Long meetingId,
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Min(value = 1, message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//        try {
//            MeetingParticipationInfo info = meetingAuthorizationService.getUserMeetingPermissions(meetingId, userId);
//            return info.isCanViewDetails();
//        } catch (Exception e) {
//            log.warn("Błąd sprawdzania dostępu do spotkania {} dla użytkownika {}: {}", userId, meetingId, e.getMessage());
//            return false;
//        }
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<MeetingResponse> getAccessibleMeetings(
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Min(value = 1, message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//        List<Meeting> allMeetings = meetingRepository.findAll();
//
//        return allMeetings.stream()
//                .filter(meeting -> {
//                    try {
//                        return meetingAuthorizationService.canUserViewResource(meeting.getId(), userId);
//                    } catch (Exception e) {
//                        log.warn("Błąd sprawdzania dostępu do spotkania {} dla użytkownika {}: {}", meeting.getId(), userId, e.getMessage());
//                        return false;
//                    }
//                })
//                .map(meetingMapper::toResponse)
//                .toList();
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<MeetingResponse> getMeetingTemplates(
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Min(value = 1, message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//        log.info("Pobieranie szablonów dla użytkownika {}", userId);
//        List<Meeting> templates = meetingRepository.findByOrganizerIdAndTemplateTrue(userId);
//        return templates.stream()
//                .map(meetingMapper::toResponse)
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    @Transactional
//    public MeetingResponse createFromTemplate(
//            @NotNull(message = "Identyfikator szablonu nie może być pusty")
//            @Min(value = 1, message = "Identyfikator szablonu musi być liczbą dodatnią")
//            Long templateId,
//            @NotNull(message = "Identyfikator organizatora nie może być pusty")
//            @Min(value = 1, message = "Identyfikator organizatora musi być liczbą dodatnią")
//            Long organizerId,
//            @NotNull(message = "Nowa data rozpoczęcia nie może być pusta")
//            @Future(message = "Nowa data rozpoczęcia musi być w przyszłości")
//            LocalDateTime newStartDate) {
//        log.info("Tworzenie spotkania z szablonu {} przez użytkownika {} z datą rozpoczęcia {}",
//                templateId, organizerId, newStartDate);
//
//        Meeting template = meetingRepository.findByIdAndTemplateTrue(templateId)
//                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono szablonu lub nie jest to szablon"));
//
//        // ✅ STWÓRZ NOWE SPOTKANIE NA PODSTAWIE SZABLONU
//        Meeting meeting = meetingMapper.cloneMeeting(template);
//
//        // ✅ USTAW ORGANIZATORA
//        User organizer = userRepository.findById(organizerId)
//                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono użytkownika"));
//        meeting.setOrganizer(organizer);
//
//        // ✅ USTAW DATY
//        meeting.setStartDate(newStartDate);
//
//        // OBLICZ ENDDATE NA PODSTAWIE DŁUGOŚCI TRWANIA SZABLONU
//        long durationMinutes = Duration.between(template.getStartDate(), template.getEndDate()).toMinutes();
//        meeting.setEndDate(newStartDate.plusMinutes(durationMinutes));
//
//        validateMeetingDates(meeting.getStartDate(), meeting.getEndDate());
//
//        meeting.setTemplate(false);
//        meeting.setOriginalMeetingId(templateId);
//
//        Meeting savedMeeting = meetingRepository.save(meeting);
//
//        saveStatusChange(savedMeeting, null, savedMeeting.getStatus().name(),
//                organizerId, "Utworzono z szablonu #" + templateId);
//
//        log.info("Utworzono spotkanie {} z szablonu {}", savedMeeting.getId(), templateId);
//
//        return meetingMapper.toResponse(savedMeeting);
//    }
//
//    @Override
//    @Transactional
//    public List<MeetingResponse> generateNextRecurrence(
//            @NotNull(message = "Identyfikator spotkania nie może być pusty")
//            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
//            Long meetingId,
//            @Min(value = 1, message = "Liczba wystąpień musi być co najmniej 1")
//            @Max(value = 100, message = "Maksymalna liczba wystąpień to 100")
//            int count) {
//        log.info("Generowanie {} następnych wystąpień dla spotkania {}", count, meetingId);
//
//        Meeting template = meetingRepository.findById(meetingId)
//                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono spotkania"));
//
//        if (!template.isRecurring() || template.getRecurrencePattern() == null) {
//            throw new BusinessException("To spotkanie nie jest cykliczne");
//        }
//
//        List<Meeting> occurrences = new ArrayList<>();
//        LocalDateTime nextDate = getLastOccurrenceDate(template);
//
//        for (int i = 0; i < count; i++) {
//            nextDate = calculateNextDate(nextDate, template.getRecurrencePattern());
//
//            // ✅ SPRAWDŹ WYJĄTKI
//            if (isExceptionDate(nextDate, template.getRecurrenceExceptionsJson())) {
//                log.info("Pomijanie daty wyjątku: {}", nextDate);
//                continue;
//            }
//
//            Meeting occurrence = createOccurrenceFromTemplate(template, nextDate);
//            occurrences.add(occurrence);
//        }
//
//        if (!occurrences.isEmpty()) {
//            List<Meeting> savedOccurrences = meetingRepository.saveAll(occurrences);
//            log.info("Wygenerowano {} wystąpień dla spotkania {}", savedOccurrences.size(), meetingId);
//
//            return savedOccurrences.stream()
//                    .map(meetingMapper::toResponse)
//                    .collect(Collectors.toList());
//        }
//
//        return Collections.emptyList();
//    }
//
//    @Override
//    @Transactional
//    public void addRecurrenceException(
//            @NotNull(message = "Identyfikator spotkania nie może być pusty")
//            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
//            Long meetingId,
//            @NotBlank(message = "Data wyjątku nie może być pusta")
//            String exceptionDate,
//            String reason) {
//        log.info("Dodawanie wyjątku cykliczności {} dla spotkania {}", exceptionDate, meetingId);
//
//        Meeting meeting = meetingRepository.findById(meetingId)
//                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono spotkania"));
//
//        if (!meeting.isRecurring()) {
//            throw new BusinessException("To spotkanie nie jest cykliczne");
//        }
//
//        // ✅ PARSOWANIE I DODAWANIE WYJĄTKU DO JSON
//        List<String> exceptions = parseRecurrenceExceptions(meeting.getRecurrenceExceptionsJson());
//        exceptions.add(exceptionDate);
//
//        try {
//            String newExceptionsJson = new com.fasterxml.jackson.databind.ObjectMapper()
//                    .writeValueAsString(exceptions);
//            meeting.setRecurrenceExceptionsJson(newExceptionsJson);
//            meetingRepository.save(meeting);
//
//            log.info("Dodano wyjątek {} dla spotkania {}", exceptionDate, meetingId);
//        } catch (Exception e) {
//            log.error("Błąd podczas dodawania wyjątku cykliczności dla spotkania {}: {}", meetingId, e.getMessage());
//            throw new BusinessException("Błąd przetwarzania wyjątku cykliczności: " + e.getMessage());
//        }
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<MeetingResponse> getRecurrenceSeries(
//            @NotNull(message = "Identyfikator oryginalnego spotkania nie może być pusty")
//            @Min(value = 1, message = "Identyfikator oryginalnego spotkania musi być liczbą dodatnią")
//            Long originalMeetingId) {
//        log.info("Getting recurrence series for original meeting {}", originalMeetingId);
//
//        List<Meeting> series = meetingRepository.findByOriginalMeetingId(originalMeetingId);
//        return series.stream()
//                .map(meetingMapper::toResponse)
//                .collect(Collectors.toList());
//    }
//
//
//
//
//    @Override
//    @Transactional(readOnly = true)
//    public Page<MeetingResponse> getMeetingsByTag(
//            @NotBlank(message = "Tag nie może być pusty")
//            String tag,
//            @NotNull(message = "Stronicowanie nie może być puste")
//            Pageable pageable) {
//        log.info("Pobieranie spotkań z tagiem {}", tag);
//
//        Page<Meeting> meetings = meetingRepository.findByTag(tag, pageable);
//        return meetings.map(meetingMapper::toResponse);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<MeetingResponse> getUpcomingRecurringMeetings(
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Min(value = 1, message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//        log.info("Pobieranie nadchodzących spotkań cyklicznych dla użytkownika {}", userId);
//
//        List<Meeting> meetings = meetingRepository.findUpcomingRecurringMeetingsForUser(userId, LocalDateTime.now());
//        return meetings.stream()
//                .map(meetingMapper::toResponse)
//                .toList();
//    }
//
//    @Override
//    @Transactional
//    public MeetingResponse saveAsTemplate(
//            @NotNull(message = "Identyfikator spotkania nie może być pusty")
//            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
//            Long meetingId,
//            @NotBlank(message = "Nazwa szablonu nie może być pusta")
//            String templateName,
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Min(value = 1, message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//        log.info("Zapisywanie spotkania {} jako szablon o nazwie '{}' przez użytkownika {}",
//                meetingId, templateName, userId);
//
//        if (!meetingAuthorizationService.canUserEditMeeting(meetingId, userId)) {
//            throw new BusinessException("Brak uprawnień do zapisania tego spotkania jako szablon");
//        }
//
//        Meeting meeting = meetingRepository.findById(meetingId)
//                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono spotkania"));
//
//        Meeting template = meetingMapper.cloneMeeting(meeting);
//        template.setId(null); // Nowa encja
//        template.setTemplate(true);
//        template.setTitle(templateName);
//        template.setOrganizer(meeting.getOrganizer()); // Oryginalny organizator
//
//        Meeting savedTemplate = meetingRepository.save(template);
//        log.info("Spotkanie {} zapisane jako szablon {}", meetingId, savedTemplate.getId());
//
//        return meetingMapper.toResponse(savedTemplate);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public Page<MeetingResponse> searchMeetings(
//            @NotNull(message = "Kryteria wyszukiwania nie mogą być puste")
//            @Valid SearchCriteria criteria,
//            @NotNull(message = "Stronicowanie nie może być puste")
//            Pageable pageable) {
//        log.info("Wyszukiwanie spotkań z kryteriami: {}", criteria);
//
//        // Dynamiczne budowanie Specification
//        Specification<Meeting> spec = MeetingSpecification.buildSpecification(criteria);
//        Page<Meeting> meetings = meetingRepository.findAll(spec, pageable);
//
//        return meetings.map(meetingMapper::toResponse);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public MeetingResponse getMeetingDetails(
//            @NotNull(message = "Identyfikator spotkania nie może być pusty")
//            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
//            Long meetingId,
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Min(value = 1, message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//        log.info("Pobieranie szczegółów spotkania {} dla użytkownika {}", meetingId, userId);
//
//        if (!canUserAccessMeeting(meetingId, userId)) {
//            throw new BusinessException("Użytkownik nie ma dostępu do szczegółów spotkania");
//        }
//
//        Meeting meeting = getMeeting(meetingId);
//        return meetingMapper.toResponse(meeting);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public MeetingResponse getMeetingForVotingCreation(
//            @NotNull(message = "Identyfikator spotkania nie może być pusty")
//            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
//            Long meetingId,
//            @NotNull(message = "Identyfikator użytkownika nie może być pusty")
//            @Min(value = 1, message = "Identyfikator użytkownika musi być liczbą dodatnią")
//            Long userId) {
//        log.info("Sprawdzanie uprawnień do tworzenia głosowania dla spotkania {} przez użytkownika {}", meetingId, userId);
//
//        if (!meetingRepository.isUserOrganizer(meetingId, userId)) {
//            throw new BusinessException("Użytkownik nie ma uprawnień do tworzenia głosowania dla tego spotkania");
//        }
//
//        Meeting meeting = getMeeting(meetingId);
//        return meetingMapper.toResponse(meeting);
//    }
//
//    // --- Metody prywatne (pomocnicze) ---
//
//    private void validateMeetingDates(LocalDateTime startDate, LocalDateTime endDate) {
//        if (startDate == null || endDate == null) {
//            throw new BusinessException("Data rozpoczęcia i zakończenia muszą być podane");
//        }
//
//        if (endDate.isBefore(startDate)) {
//            throw new BusinessException("Data zakończenia nie może być wcześniejsza niż data rozpoczęcia");
//        }
//    }
//
//    /**
//     * Zapisuje zmianę statusu spotkania w historii.
//     */
//    private void saveStatusChange(Meeting meeting, String oldStatus, String newStatus,
//                                  Long userId, String reason) {
//        log.debug("Zalogowano zmianę statusu: Spotkanie {} z {} na {} przez użytkownika {} (Powód: {})",
//                meeting.getId(), oldStatus, newStatus, userId, reason);
//    }
//
//    /**
//     * Oblicza kolejną datę spotkania na podstawie wzorca cykliczności.
//     */
//    private LocalDateTime calculateNextDate(LocalDateTime currentLastDate, String recurrencePattern) {
//        if ("WEEKLY".equalsIgnoreCase(recurrencePattern)) {
//            return currentLastDate.plusWeeks(1);
//        }
//        return currentLastDate.plusDays(1);
//    }
//
//    /**
//     * Tworzy wystąpienie spotkania z szablonu.
//     */
//    private Meeting createOccurrenceFromTemplate(Meeting template, LocalDateTime newStartDate) {
//        Meeting occurrence = meetingMapper.cloneMeeting(template);
//        occurrence.setId(null);
//        occurrence.setTemplate(false);
//        occurrence.setRecurring(false);
//        occurrence.setOriginalMeetingId(template.getId());
//
//        long durationMinutes = Duration.between(template.getStartDate(), template.getEndDate()).toMinutes();
//        occurrence.setStartDate(newStartDate);
//        occurrence.setEndDate(newStartDate.plusMinutes(durationMinutes));
//
//        occurrence.setRecurrenceExceptionsJson(null);
//        occurrence.setParticipants(new HashSet<>());
//        occurrence.setStatus(MeetingStatus.PLANNED);
//
//        return occurrence;
//    }
//
//    /**
//     * Znajduje datę ostatniego wystąpienia w serii lub datę oryginalnego spotkania.
//     */
//    private LocalDateTime getLastOccurrenceDate(Meeting original) {
//        Optional<Meeting> lastOccurrence = meetingRepository.findLastOccurrenceByOriginalMeetingId(original.getId());
//
//        return lastOccurrence
//                .map(Meeting::getStartDate)
//                .orElse(original.getStartDate());
//    }
//
//    /**
//     * Parsuje JSON z wyjątkami cykliczności do listy Stringów dat.
//     */
//    private List<String> parseRecurrenceExceptions(String exceptionsJson) {
//        if (exceptionsJson == null || exceptionsJson.trim().isEmpty()) {
//            return new ArrayList<>();
//        }
//        try {
//            return new com.fasterxml.jackson.databind.ObjectMapper()
//                    .readValue(exceptionsJson,
//                            new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
//        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
//            log.error("Błąd parsowania JSON wyjątków cykliczności: {}", exceptionsJson, e);
//            return new ArrayList<>();
//        }
//    }
//
//    /**
//     * Sprawdza, czy dana data jest wyjątkiem cykliczności.
//     */
//    private boolean isExceptionDate(LocalDateTime date, String exceptionsJson) {
//        List<String> exceptions = parseRecurrenceExceptions(exceptionsJson);
//        return exceptions.contains(date.toString());
//    }
//}
//
//
//

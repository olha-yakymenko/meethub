




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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
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

    private final JdbcTemplate jdbcTemplate;
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

        meeting.setOrganizer(organizer);
        meeting.setStatus(MeetingStatus.PLANNED);

        validateMeetingDates(meeting.getStartDate(), meeting.getEndDate());


        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            Set<Category> categories = new HashSet<>(categoryRepository.findAllById(request.getCategoryIds()));
            meeting.setCategories(categories);
            log.info("Added {} categories to meeting", categories.size());
        }

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


            saveStatusChange(savedMeeting, null, savedMeeting.getStatus().name(),
                    organizerId, "Utworzono spotkanie");

            log.info("=== MEETING CREATED SUCCESSFULLY ===");
            log.info("Meeting ID: {}, Title: {}, Recurring: {}",
                    savedMeeting.getId(), savedMeeting.getTitle(), savedMeeting.isRecurring());

            if (savedMeeting.isRecurring() && savedMeeting.getRecurrencePattern() != null) {
                generateNextRecurrence(savedMeeting.getId(), 3); // Generuj 3 następne
            }

            meetingSchedulerService.scheduleMeetingNotifications(savedMeeting);

            log.info(" Utworzono spotkanie {} i zaplanowano powiadomienia", savedMeeting.getId());

            return meetingMapper.toResponse(savedMeeting);
        } catch (Exception e) {
            log.error("=== ERROR CREATING MEETING ===");
            log.error("Error: {}", e.getMessage(), e);
            throw new BusinessException("Error creating meeting: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public MeetingResponse updateMeeting(Long meetingId, UpdateMeetingRequest request, Long userId) {
        log.info("=== START updateMeeting ===");
        log.info("Meeting ID: {}, User ID: {}", meetingId, userId);
        log.info("Request: title={}, status={}, statusChangeReason={}",
                request.getTitle(), request.getStatus(), request.getStatusChangeReason());

        if (!meetingAuthorizationService.canUserEditMeeting(meetingId, userId)) {
            throw new BusinessException("No permission to edit this meeting");
        }

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found with id: " + meetingId));

        log.info("Found meeting: id={}, title={}, status={}",
                meeting.getId(), meeting.getTitle(), meeting.getStatus());

        MeetingStatus oldStatus = meeting.getStatus();
        LocalDateTime oldStartDate = meeting.getStartDate();
        LocalDateTime oldEndDate = meeting.getEndDate();
        String oldTitle = meeting.getTitle();

        log.info("Old data: status={}, title={}", oldStatus, oldTitle);

        // 3. ZAPISZ ZMIANĘ STATUSU W HISTORII
        if (request.getStatus() != null && !request.getStatus().equals(oldStatus)) {
            log.info("Status change detected: {} -> {}", oldStatus, request.getStatus());
            saveStatusChange(meeting, oldStatus.name(),
                    request.getStatus().name(), userId, request.getStatusChangeReason());

            // Jeśli zmieniasz status na CANCELLED - anuluj powiadomienia
            if (request.getStatus() == MeetingStatus.CANCELLED) {
                meetingSchedulerService.cancelMeetingSchedule(meetingId);
                log.info("Anulowano powiadomienia dla spotkania {} (status zmieniony na CANCELLED)",
                        meetingId);
            }
            // Jeśli zmieniasz status z CANCELLED na PLANNED - zaplanuj powiadomienia
            else if (oldStatus == MeetingStatus.CANCELLED && request.getStatus() == MeetingStatus.PLANNED) {
                meetingSchedulerService.scheduleMeetingNotifications(meeting);
                log.info(" Zaplanowano powiadomienia dla spotkania {} (status zmieniony z CANCELLED na PLANNED)",
                        meetingId);
            }
        }

        if (request.getCategoryIds() != null) {
            Set<Category> categories = new HashSet<>(categoryRepository.findAllById(request.getCategoryIds()));
            meeting.setCategories(categories);
            log.info("Updated categories for meeting {}: {} categories", meetingId, categories.size());
        }

        log.info("BEFORE meetingMapper.updateEntityFromRequest - Title: {}", meeting.getTitle());
        log.info("Calling meetingMapper.updateEntityFromRequest...");
        meetingMapper.updateEntityFromRequest(request, meeting);
        log.info("AFTER meetingMapper.updateEntityFromRequest - Title: {}", meeting.getTitle());
        log.info("Meeting fields after update: title={}, status={}",
                meeting.getTitle(), meeting.getStatus());

        log.info("Saving meeting to repository...");
        Meeting updatedMeeting = meetingRepository.save(meeting);
        log.info("Meeting saved: id={}, title={}, status={}",
                updatedMeeting.getId(), updatedMeeting.getTitle(), updatedMeeting.getStatus());

        handleDateChanges(meetingId, updatedMeeting, oldStartDate, oldEndDate, userId);

        log.info("Mapping to response...");
        MeetingResponse response = meetingMapper.toResponse(updatedMeeting);

        return response;
    }


    private void handleDateChanges(Long meetingId, Meeting updatedMeeting,
                                   LocalDateTime oldStartDate, LocalDateTime oldEndDate,
                                   Long userId) {
        boolean dateChanged = false;

        if (oldStartDate != null && updatedMeeting.getStartDate() != null &&
                !oldStartDate.equals(updatedMeeting.getStartDate())) {
            dateChanged = true;
            log.info("Zmieniono datę rozpoczęcia spotkania {} z {} na {}",
                    meetingId, oldStartDate, updatedMeeting.getStartDate());
        }

        if (oldEndDate != null && updatedMeeting.getEndDate() != null &&
                !oldEndDate.equals(updatedMeeting.getEndDate())) {
            dateChanged = true;
            log.info(" Zmieniono datę zakończenia spotkania {} z {} na {}",
                    meetingId, oldEndDate, updatedMeeting.getEndDate());
        }

        if (dateChanged && updatedMeeting.getStatus() == MeetingStatus.PLANNED) {
            meetingSchedulerService.cancelMeetingSchedule(meetingId);

            meetingSchedulerService.scheduleMeetingNotifications(updatedMeeting);

        }
    }


    @Override
    @Transactional
    public void deleteMeeting(Long meetingId, Long organizerId) {
        if (!meetingAuthorizationService.canUserDeleteMeeting(meetingId, organizerId)) {
            throw new BusinessException("No permission to delete this meeting");
        }

        Meeting meeting = meetingRepository.findByIdAndOrganizerId(meetingId, organizerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Meeting not found with id: " + meetingId + " for organizer: " + organizerId));

        String deleteParticipantsSql = "DELETE FROM meeting_participants WHERE meeting_id = ?";
        jdbcTemplate.update(deleteParticipantsSql, meetingId);

        String deleteCategoriesSql = "DELETE FROM meeting_categories WHERE meeting_id = ?";
        jdbcTemplate.update(deleteCategoriesSql, meetingId);

        String deleteMeetingSql = "DELETE FROM meetings WHERE id = ?";
        int deleted = jdbcTemplate.update(deleteMeetingSql, meetingId);

        if (deleted == 0) {
            throw new BusinessException("Failed to delete meeting");
        }

        log.info("Meeting {} deleted by organizer {} (via JDBC)", meetingId, organizerId);
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
        if (!meetingAuthorizationService.canUserEditMeeting(meetingId, organizerId)) {
            throw new BusinessException("No permission to change status of this meeting");
        }

        List<Long> meetingIds = List.of(meetingId);
        int updated = customMeetingRepository.bulkUpdateMeetingStatus(meetingIds, status.name());

        if (updated == 0) {
            throw new ResourceNotFoundException("Meeting not found with id: " + meetingId);
        }

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found with id: " + meetingId));

        String oldStatus = meeting.getStatus().name();
        saveStatusChange(meeting, oldStatus, status.name(), organizerId, "Status change via JDBC");

        log.info("Meeting {} status changed from {} to {} by {} (via JDBC)",
                meetingId, oldStatus, status, organizerId);
    }

    @Override
    @Transactional
    public MeetingResponse duplicateMeeting(Long meetingId, Long organizerId) {
        log.info("Changing status of meeting {} to PLANNED by user {}", meetingId, organizerId);

        if (!meetingAuthorizationService.canUserEditMeeting(meetingId, organizerId)) {
            throw new BusinessException("No permission to change status of this meeting");
        }

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Meeting not found with id: " + meetingId));

        String oldStatus = meeting.getStatus().name();

        meeting.setStatus(MeetingStatus.PLANNED);

        Meeting updatedMeeting = meetingRepository.save(meeting);

        saveStatusChange(updatedMeeting, oldStatus, updatedMeeting.getStatus().name(),
                organizerId, "Status zmieniony na PLANNED");

        log.info("Meeting {} status changed from {} to {}", meetingId, oldStatus, updatedMeeting.getStatus().name());

        return meetingMapper.toResponse(updatedMeeting);
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

        Meeting meeting = meetingMapper.cloneMeeting(template);

        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        meeting.setOrganizer(organizer);

        meeting.setStartDate(newStartDate);

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

    @Override
    @Transactional(readOnly = true)
    public Page<MeetingResponse> searchMeetings(SearchCriteria criteria, Pageable pageable) {
        log.info(" Dynamic search with criteria: {}", criteria);

        try {
            if (criteria.getCurrentUserId() == null) {
                throw new BusinessException("User ID is required for search");
            }

            Specification<Meeting> spec = buildDynamicSpecification(criteria);
            Page<Meeting> meetingsPage = meetingRepository.findAll(spec, pageable);
            log.info(" Found {} meetings", meetingsPage.getTotalElements());

            return meetingsPage.map(meetingMapper::toResponse);

        } catch (Exception e) {
            log.error(" Search error", e);
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




//package com.meethub.domain.service.impl;
//
//import com.meethub.domain.model.entity.Meeting;
//import com.meethub.domain.model.entity.User;
//import com.meethub.domain.model.enums.MeetingStatus;
//import com.meethub.domain.model.enums.MeetingVisibility;
//import com.meethub.domain.model.request.CreateMeetingRequest;
//import com.meethub.domain.model.request.UpdateMeetingRequest;
//import com.meethub.domain.model.response.MeetingParticipationInfo;
//import com.meethub.domain.model.response.MeetingResponse;
//import com.meethub.domain.repository.jpa.CategoryRepository;
//import com.meethub.domain.repository.jpa.MeetingRepository;
//import com.meethub.domain.repository.jpa.UserRepository;
//import com.meethub.domain.repository.jdbc.CustomMeetingRepository;
//import com.meethub.domain.service.MeetingAuthorizationService;
//import com.meethub.domain.service.MeetingParticipantService;
//import com.meethub.domain.service.MeetingService;
//import com.meethub.exception.BusinessException;
//import com.meethub.exception.ResourceNotFoundException;
//import com.meethub.domain.model.mapper.MeetingMapper;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.HashSet;
//import java.util.List;
//
//
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
//    private final MeetingAuthorizationService meetingAuthorizationService; // ✅ DODAJ TĘ ZALEŻNOŚĆ
//    private final CategoryRepository categoryRepository;
//
//    @Override
//    @Transactional
//    public MeetingResponse createMeeting(CreateMeetingRequest request, Long organizerId) {
//        log.info("=== START CREATING MEETING ===");
//        log.info("Organizer ID: {}", organizerId);
//        log.info("Request data - Title: {}, Type: {}, Visibility: {}",
//                request.getTitle(), request.getType(), request.getVisibility());
//
//        User organizer = userRepository.findById(organizerId)
//                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + organizerId));
//
//        log.info("Found organizer: {} {} (ID: {})",
//                organizer.getFirstName(), organizer.getLastName(), organizer.getId());
//
//        Meeting meeting = meetingMapper.toEntity(request);
//        log.info("After mapping - Meeting organizer: {}", meeting.getOrganizer());
//
//        // USTAW ORGANIZATORA
//        meeting.setOrganizer(organizer);
//        meeting.setStatus(MeetingStatus.PLANNED);
//
//        log.info("Before save - Meeting organizer ID: {}, Title: {}",
//                meeting.getOrganizer() != null ? meeting.getOrganizer().getId() : "NULL",
//                meeting.getTitle());
//
//        try {
//            Meeting savedMeeting = meetingRepository.save(meeting);
//            log.info("=== MEETING CREATED SUCCESSFULLY ===");
//            log.info("Meeting ID: {}, Organizer ID: {}", savedMeeting.getId(), savedMeeting.getOrganizer().getId());
//
//            return meetingMapper.toResponse(savedMeeting);
//        } catch (Exception e) {
//            log.error("=== ERROR CREATING MEETING ===");
//            log.error("Error: {}", e.getMessage(), e);
//            throw new BusinessException("Error creating meeting: " + e.getMessage());
//        }
//    }
//
//    @Override
//    @Transactional
//    public MeetingResponse updateMeeting(Long meetingId, UpdateMeetingRequest request, Long organizerId) {
//        // ✅ SPRAWDŹ UPRAWNIENIA PRZED AKTUALIZACJĄ
//        if (!meetingAuthorizationService.canUserEditMeeting(meetingId, organizerId)) {
//            throw new BusinessException("No permission to edit this meeting");
//        }
//
//        Meeting meeting = meetingRepository.findByIdAndOrganizerId(meetingId, organizerId)
//                .orElseThrow(() -> new ResourceNotFoundException(
//                        "Meeting not found with id: " + meetingId + " for organizer: " + organizerId));
//
//        meetingMapper.updateEntityFromRequest(request, meeting);
//        Meeting updatedMeeting = meetingRepository.save(meeting);
//
//        log.info("Meeting updated with id: {} by organizer: {}", meetingId, organizerId);
//        return meetingMapper.toResponse(updatedMeeting);
//    }
//
//    @Override
//    @Transactional
//    public void deleteMeeting(Long meetingId, Long organizerId) {
//        // ✅ SPRAWDŹ UPRAWNIENIA PRZED USUNIĘCIEM
//        if (!meetingAuthorizationService.canUserDeleteMeeting(meetingId, organizerId)) {
//            throw new BusinessException("No permission to delete this meeting");
//        }
//
//        Meeting meeting = meetingRepository.findByIdAndOrganizerId(meetingId, organizerId)
//                .orElseThrow(() -> new ResourceNotFoundException(
//                        "Meeting not found with id: " + meetingId + " for organizer: " + organizerId));
//
//        meetingRepository.delete(meeting);
//        log.info("Meeting deleted with id: {} by organizer: {}", meetingId, organizerId);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public MeetingResponse getMeetingById(Long meetingId) {
//        Meeting meeting = meetingRepository.findById(meetingId)
//                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found with id: " + meetingId));
//
//        return meetingMapper.toResponse(meeting);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public Page<MeetingResponse> getUserMeetings(Long userId, Pageable pageable) {
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
//    public List<MeetingResponse> findNearbyMeetings(double latitude, double longitude, double radius) {
//        List<Meeting> meetings = customMeetingRepository.findNearbyMeetings(latitude, longitude, radius, 50);
//        return meetings.stream()
//                .map(meetingMapper::toResponse)
//                .toList();
//    }
//
//    @Override
//    @Transactional
//    public void changeMeetingStatus(Long meetingId, MeetingStatus status, Long organizerId) {
//        // ✅ SPRAWDŹ UPRAWNIENIA PRZED ZMIANĄ STATUSU
//        if (!meetingAuthorizationService.canUserEditMeeting(meetingId, organizerId)) {
//            throw new BusinessException("No permission to change status of this meeting");
//        }
//
//        Meeting meeting = meetingRepository.findByIdAndOrganizerId(meetingId, organizerId)
//                .orElseThrow(() -> new ResourceNotFoundException(
//                        "Meeting not found with id: " + meetingId + " for organizer: " + organizerId));
//
//        meeting.setStatus(status);
//        meetingRepository.save(meeting);
//        log.info("Meeting status changed to {} for meeting id: {}", status, meetingId);
//    }
//
//    @Override
//    @Transactional
//    public MeetingResponse duplicateMeeting(Long meetingId, Long organizerId) {
//        // ✅ SPRAWDŹ UPRAWNIENIA PRZED DUPLIKACJĄ
//        if (!meetingAuthorizationService.canUserEditMeeting(meetingId, organizerId)) {
//            throw new BusinessException("No permission to duplicate this meeting");
//        }
//
//        Meeting original = meetingRepository.findByIdAndOrganizerId(meetingId, organizerId)
//                .orElseThrow(() -> new ResourceNotFoundException(
//                        "Meeting not found with id: " + meetingId + " for organizer: " + organizerId));
//
//        Meeting duplicate = new Meeting();
//        // Copy all fields except ID and timestamps
//        duplicate.setTitle(original.getTitle() + " (Copy)");
//        duplicate.setDescription(original.getDescription());
//        duplicate.setAgenda(original.getAgenda());
//        duplicate.setType(original.getType());
//        duplicate.setStatus(MeetingStatus.PLANNED);
//        duplicate.setVisibility(original.getVisibility());
//        duplicate.setStartDate(original.getStartDate().plusDays(7)); // Default to one week later
//        duplicate.setEndDate(original.getEndDate().plusDays(7));
//        duplicate.setMaxParticipants(original.getMaxParticipants());
//        duplicate.setOrganizer(original.getOrganizer());
//        duplicate.setLocation(original.getLocation());
//        duplicate.setTags(new HashSet<>(original.getTags()));
//
//        Meeting savedDuplicate = meetingRepository.save(duplicate);
//        log.info("Meeting duplicated from id: {} to new id: {}", meetingId, savedDuplicate.getId());
//
//        return meetingMapper.toResponse(savedDuplicate);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<MeetingResponse> findConflictingMeetings(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
//        List<Meeting> conflicts = meetingRepository.findConfirmedMeetingsForUserInPeriod(userId, startDate, endDate);
//        return conflicts.stream()
//                .map(meetingMapper::toResponse)
//                .toList();
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public Page<MeetingResponse> getFilteredMeetings(String search, String type, String status, Pageable pageable) {
//        Page<Meeting> meetings = customMeetingRepository.findFilteredMeetings(search, type, status, pageable);
//        return meetings.map(meetingMapper::toResponse);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public Meeting getMeeting(Long meetingId) {
//        log.debug("Getting meeting by ID: {}", meetingId);
//
//        return meetingRepository.findById(meetingId)
//                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found with id: " + meetingId));
//    }
//
//    // ✅ DODAJ NOWE METODY DLA LEPSZEJ INTEGRACJI Z ZASOBAMI
//
//    @Override
//    @Transactional(readOnly = true)
//    public MeetingParticipationInfo getMeetingParticipationInfo(Long meetingId, Long userId) {
//        return meetingAuthorizationService.getUserMeetingPermissions(meetingId, userId);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public boolean canUserAccessMeeting(Long meetingId, Long userId) {
//        try {
//            MeetingParticipationInfo info = meetingAuthorizationService.getUserMeetingPermissions(meetingId, userId);
//            return info.isCanViewDetails();
//        } catch (Exception e) {
//            log.warn("Error checking meeting access for user {} to meeting {}: {}", userId, meetingId, e.getMessage());
//            return false;
//        }
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<MeetingResponse> getAccessibleMeetings(Long userId) {
//        // ✅ ZWRÓĆ WSZYSTKIE SPOTKANIA DO KTÓRYCH UŻYTKOWNIK MA DOSTĘP
//        List<Meeting> allMeetings = meetingRepository.findAll();
//
//        return allMeetings.stream()
//                .filter(meeting -> {
//                    try {
//                        return meetingAuthorizationService.canUserViewResource(meeting.getId(), userId);
//                    } catch (Exception e) {
//                        log.warn("Error checking access to meeting {} for user {}: {}", meeting.getId(), userId, e.getMessage());
//                        return false;
//                    }
//                })
//                .map(meetingMapper::toResponse)
//                .toList();
//    }
//
//
//}
















package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.*;
import com.meethub.domain.model.enums.MeetingStatus;
import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.model.enums.PermissionLevel;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

        // ✅ NIE JEST JUŻ SZABLONEM
        meeting.setTemplate(false);
        meeting.setOriginalMeetingId(templateId);

        Meeting savedMeeting = meetingRepository.save(meeting);

        // ✅ ZAPISZ W HISTORII
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
                    .readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
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
    public Page<MeetingResponse> getMeetingsByCategory(Long categoryId, Pageable pageable) {
        log.info("Getting meetings by category {}", categoryId);

        Page<Meeting> meetings = meetingRepository.findByCategoryId(categoryId, pageable);
        return meetings.map(meetingMapper::toResponse);
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
        log.info("🔍 Starting advanced search with criteria: {}", criteria);

        try {
            // ✅ Sprawdź czy userId jest w criteria
            if (criteria.getCurrentUserId() == null) {
                log.warn("User ID not set in search criteria");
                throw new BusinessException("User ID is required for search");
            }

            // ✅ Utwórz specyfikację
            Specification<Meeting> spec = new MeetingSpecification(criteria, criteria.getCurrentUserId());

            // ✅ Wykonaj zapytanie
            Page<Meeting> meetingsPage = meetingRepository.findAll(spec, pageable);

            log.info("✅ Found {} meetings for user {}", meetingsPage.getTotalElements(), criteria.getCurrentUserId());

            // ✅ Mapuj na responsy
            return meetingsPage.map(meetingMapper::toResponse);

        } catch (Exception e) {
            log.error("❌ Error in advanced search: {}", e.getMessage(), e);
            throw new BusinessException("Error during search: " + e.getMessage());
        }
    }

    private Pageable createSortedPageable(Pageable pageable, String sortBy) {
        if (sortBy == null || sortBy.isEmpty()) {
            return pageable;
        }

        Sort sort;
        switch (sortBy) {
            case "START_DATE_ASC":
                sort = Sort.by(Sort.Direction.ASC, "startDate");
                break;
            case "START_DATE_DESC":
                sort = Sort.by(Sort.Direction.DESC, "startDate");
                break;
            case "TITLE_ASC":
                sort = Sort.by(Sort.Direction.ASC, "title");
                break;
            case "TITLE_DESC":
                sort = Sort.by(Sort.Direction.DESC, "title");
                break;
            case "CREATED_AT_DESC":
                sort = Sort.by(Sort.Direction.DESC, "createdAt");
                break;
            case "PARTICIPANTS_DESC":
                sort = Sort.by(Sort.Direction.DESC, "confirmedParticipantsCount");
                break;
            default:
                sort = Sort.by(Sort.Direction.DESC, "startDate");
        }

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }
}

package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.MeetingStatus;
import com.meethub.domain.model.enums.MeetingVisibility;
import com.meethub.domain.model.request.CreateMeetingRequest;
import com.meethub.domain.model.request.UpdateMeetingRequest;
import com.meethub.domain.model.response.MeetingParticipationInfo;
import com.meethub.domain.model.response.MeetingResponse;
import com.meethub.domain.repository.jpa.MeetingRepository;
import com.meethub.domain.repository.jpa.UserRepository;
import com.meethub.domain.repository.jdbc.CustomMeetingRepository;
import com.meethub.domain.service.MeetingAuthorizationService;
import com.meethub.domain.service.MeetingParticipantService;
import com.meethub.domain.service.MeetingService;
import com.meethub.exception.BusinessException;
import com.meethub.exception.ResourceNotFoundException;
import com.meethub.domain.model.mapper.MeetingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingServiceImpl implements MeetingService {

    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;
    private final CustomMeetingRepository customMeetingRepository;
    private final MeetingMapper meetingMapper;
    private final MeetingParticipantService meetingParticipantService;
    private final MeetingAuthorizationService meetingAuthorizationService; // ✅ DODAJ TĘ ZALEŻNOŚĆ

    @Override
    @Transactional
    public MeetingResponse createMeeting(CreateMeetingRequest request, Long organizerId) {
        log.info("=== START CREATING MEETING ===");
        log.info("Organizer ID: {}", organizerId);
        log.info("Request data - Title: {}, Type: {}, Visibility: {}",
                request.getTitle(), request.getType(), request.getVisibility());

        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + organizerId));

        log.info("Found organizer: {} {} (ID: {})",
                organizer.getFirstName(), organizer.getLastName(), organizer.getId());

        Meeting meeting = meetingMapper.toEntity(request);
        log.info("After mapping - Meeting organizer: {}", meeting.getOrganizer());

        // USTAW ORGANIZATORA
        meeting.setOrganizer(organizer);
        meeting.setStatus(MeetingStatus.PLANNED);

        log.info("Before save - Meeting organizer ID: {}, Title: {}",
                meeting.getOrganizer() != null ? meeting.getOrganizer().getId() : "NULL",
                meeting.getTitle());

        try {
            Meeting savedMeeting = meetingRepository.save(meeting);
            log.info("=== MEETING CREATED SUCCESSFULLY ===");
            log.info("Meeting ID: {}, Organizer ID: {}", savedMeeting.getId(), savedMeeting.getOrganizer().getId());

            return meetingMapper.toResponse(savedMeeting);
        } catch (Exception e) {
            log.error("=== ERROR CREATING MEETING ===");
            log.error("Error: {}", e.getMessage(), e);
            throw new BusinessException("Error creating meeting: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public MeetingResponse updateMeeting(Long meetingId, UpdateMeetingRequest request, Long organizerId) {
        // ✅ SPRAWDŹ UPRAWNIENIA PRZED AKTUALIZACJĄ
        if (!meetingAuthorizationService.canUserEditMeeting(meetingId, organizerId)) {
            throw new BusinessException("No permission to edit this meeting");
        }

        Meeting meeting = meetingRepository.findByIdAndOrganizerId(meetingId, organizerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Meeting not found with id: " + meetingId + " for organizer: " + organizerId));

        meetingMapper.updateEntityFromRequest(request, meeting);
        Meeting updatedMeeting = meetingRepository.save(meeting);

        log.info("Meeting updated with id: {} by organizer: {}", meetingId, organizerId);
        return meetingMapper.toResponse(updatedMeeting);
    }

    @Override
    @Transactional
    public void deleteMeeting(Long meetingId, Long organizerId) {
        // ✅ SPRAWDŹ UPRAWNIENIA PRZED USUNIĘCIEM
        if (!meetingAuthorizationService.canUserDeleteMeeting(meetingId, organizerId)) {
            throw new BusinessException("No permission to delete this meeting");
        }

        Meeting meeting = meetingRepository.findByIdAndOrganizerId(meetingId, organizerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Meeting not found with id: " + meetingId + " for organizer: " + organizerId));

        meetingRepository.delete(meeting);
        log.info("Meeting deleted with id: {} by organizer: {}", meetingId, organizerId);
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
        // ✅ SPRAWDŹ UPRAWNIENIA PRZED ZMIANĄ STATUSU
        if (!meetingAuthorizationService.canUserEditMeeting(meetingId, organizerId)) {
            throw new BusinessException("No permission to change status of this meeting");
        }

        Meeting meeting = meetingRepository.findByIdAndOrganizerId(meetingId, organizerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Meeting not found with id: " + meetingId + " for organizer: " + organizerId));

        meeting.setStatus(status);
        meetingRepository.save(meeting);
        log.info("Meeting status changed to {} for meeting id: {}", status, meetingId);
    }

    @Override
    @Transactional
    public MeetingResponse duplicateMeeting(Long meetingId, Long organizerId) {
        // ✅ SPRAWDŹ UPRAWNIENIA PRZED DUPLIKACJĄ
        if (!meetingAuthorizationService.canUserEditMeeting(meetingId, organizerId)) {
            throw new BusinessException("No permission to duplicate this meeting");
        }

        Meeting original = meetingRepository.findByIdAndOrganizerId(meetingId, organizerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Meeting not found with id: " + meetingId + " for organizer: " + organizerId));

        Meeting duplicate = new Meeting();
        // Copy all fields except ID and timestamps
        duplicate.setTitle(original.getTitle() + " (Copy)");
        duplicate.setDescription(original.getDescription());
        duplicate.setAgenda(original.getAgenda());
        duplicate.setType(original.getType());
        duplicate.setStatus(MeetingStatus.PLANNED);
        duplicate.setVisibility(original.getVisibility());
        duplicate.setStartDate(original.getStartDate().plusDays(7)); // Default to one week later
        duplicate.setEndDate(original.getEndDate().plusDays(7));
        duplicate.setMaxParticipants(original.getMaxParticipants());
        duplicate.setOrganizer(original.getOrganizer());
        duplicate.setLocation(original.getLocation());
        duplicate.setTags(new HashSet<>(original.getTags()));

        Meeting savedDuplicate = meetingRepository.save(duplicate);
        log.info("Meeting duplicated from id: {} to new id: {}", meetingId, savedDuplicate.getId());

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

    // ✅ DODAJ NOWE METODY DLA LEPSZEJ INTEGRACJI Z ZASOBAMI

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
        // ✅ ZWRÓĆ WSZYSTKIE SPOTKANIA DO KTÓRYCH UŻYTKOWNIK MA DOSTĘP
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
}
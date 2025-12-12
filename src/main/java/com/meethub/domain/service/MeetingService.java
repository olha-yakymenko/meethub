package com.meethub.domain.service;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.enums.MeetingStatus;
import com.meethub.domain.model.request.CreateMeetingRequest;
import com.meethub.domain.model.request.SearchCriteria;
import com.meethub.domain.model.request.UpdateMeetingRequest;
import com.meethub.domain.model.response.MeetingParticipationInfo;
import com.meethub.domain.model.response.MeetingResponse;
import com.meethub.domain.model.response.ParticipantResponse;
import com.meethub.security.CustomUserDetailsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface MeetingService {

    MeetingResponse createMeeting(CreateMeetingRequest request, Long organizerId);

    MeetingResponse updateMeeting(Long meetingId, UpdateMeetingRequest request, Long organizerId);

    void deleteMeeting(Long meetingId, Long organizerId);

    MeetingResponse getMeetingById(Long meetingId);

    Page<MeetingResponse> getUserMeetings(Long userId, Pageable pageable);

    List<MeetingResponse> getUpcomingPublicMeetings();

    List<MeetingResponse> findNearbyMeetings(double latitude, double longitude, double radius);

    void changeMeetingStatus(Long meetingId, MeetingStatus status, Long organizerId);

    MeetingResponse duplicateMeeting(Long meetingId, Long organizerId);

    List<MeetingResponse> findConflictingMeetings(Long userId, LocalDateTime startDate, LocalDateTime endDate);

    Page<MeetingResponse> getFilteredMeetings(String search, String type, String status, Pageable pageable);

    Meeting getMeeting(Long meetingId);

    MeetingParticipationInfo getMeetingParticipationInfo(Long meetingId, Long userId);
    boolean canUserAccessMeeting(Long meetingId, Long userId);
    List<MeetingResponse> getAccessibleMeetings(Long userId);

    @Transactional(readOnly = true)
    List<MeetingResponse> getMeetingTemplates(Long userId);

    @Transactional
    MeetingResponse createFromTemplate(Long templateId, Long organizerId, LocalDateTime newStartDate);

    @Transactional
    List<MeetingResponse> generateNextRecurrence(Long meetingId, int count);

    @Transactional
    void addRecurrenceException(Long meetingId, String exceptionDate, String reason);

    @Transactional(readOnly = true)
    List<MeetingResponse> getRecurrenceSeries(Long originalMeetingId);

    @Transactional(readOnly = true)
    Page<MeetingResponse> getMeetingsByCategory(Long categoryId, Pageable pageable);

    @Transactional(readOnly = true)
    Page<MeetingResponse> getMeetingsByTag(String tag, Pageable pageable);

    @Transactional(readOnly = true)
    List<MeetingResponse> getUpcomingRecurringMeetings(Long userId);

//    Page<MeetingResponse> getAdvancedMeetings(
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
//    );

    MeetingResponse saveAsTemplate(Long meetingId, String templateName, Long userId);

//    @Transactional(readOnly = true)
//    Page<MeetingResponse> searchMeetings(SearchCriteria criteria,  Pageable pageable);

   Page<MeetingResponse> searchMeetings(SearchCriteria criteria, Pageable pageable);

    MeetingResponse getMeetingDetails(Long meetingId, Long userId);

    MeetingResponse getMeetingForVotingCreation(Long meetingId, Long userId);

//    List<ParticipantResponse> getConfirmedParticipants(Long meetingId);
}
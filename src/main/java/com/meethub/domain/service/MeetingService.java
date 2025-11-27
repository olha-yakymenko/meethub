package com.meethub.domain.service;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.enums.MeetingStatus;
import com.meethub.domain.model.request.CreateMeetingRequest;
import com.meethub.domain.model.request.UpdateMeetingRequest;
import com.meethub.domain.model.response.MeetingResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

}
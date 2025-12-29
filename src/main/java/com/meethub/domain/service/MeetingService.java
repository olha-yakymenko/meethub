// MeetingService.java
package com.meethub.domain.service;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.enums.MeetingStatus;
import com.meethub.domain.model.request.CreateMeetingRequest;
import com.meethub.domain.model.request.SearchCriteria;
import com.meethub.domain.model.request.UpdateMeetingRequest;
import com.meethub.domain.model.response.MeetingParticipationInfo;
import com.meethub.domain.model.response.MeetingResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;


public interface MeetingService {

    MeetingResponse createMeeting(
            @Valid @NotNull CreateMeetingRequest request,
            @NotNull Long organizerId
    );

    MeetingResponse updateMeeting(
            @NotNull Long meetingId,
            @Valid @NotNull UpdateMeetingRequest request,
            @NotNull Long organizerId
    );

    void deleteMeeting(@NotNull Long meetingId, @NotNull Long organizerId);
    MeetingResponse getMeetingById(@NotNull Long meetingId);

    Page<MeetingResponse> getUserMeetings(@NotNull Long userId, Pageable pageable);
    List<MeetingResponse> getUpcomingPublicMeetings();
    List<MeetingResponse> findNearbyMeetings(double latitude, double longitude, double radius);

    void changeMeetingStatus(
            @NotNull Long meetingId,
            @NotNull MeetingStatus status,
            @NotNull Long organizerId
    );

    MeetingResponse duplicateMeeting(@NotNull Long meetingId, @NotNull Long organizerId);

    List<MeetingResponse> findConflictingMeetings(
            @NotNull Long userId,
            @NotNull LocalDateTime startDate,
            @NotNull LocalDateTime endDate
    );

    Page<MeetingResponse> getFilteredMeetings(String search, String type, String status, Pageable pageable);
    Meeting getMeeting(@NotNull Long meetingId);

    MeetingParticipationInfo getMeetingParticipationInfo(
            @NotNull Long meetingId,
            @NotNull Long userId
    );

    boolean canUserAccessMeeting(@NotNull Long meetingId, @NotNull Long userId);
    List<MeetingResponse> getAccessibleMeetings(@NotNull Long userId);
    List<MeetingResponse> getMeetingTemplates(@NotNull Long userId);

    MeetingResponse createFromTemplate(
            @NotNull Long templateId,
            @NotNull Long organizerId,
            @NotNull LocalDateTime newStartDate
    );

    List<MeetingResponse> generateNextRecurrence(@NotNull Long meetingId, int count);
    void addRecurrenceException(@NotNull Long meetingId, String exceptionDate, String reason);
    List<MeetingResponse> getRecurrenceSeries(@NotNull Long originalMeetingId);

    Page<MeetingResponse> getMeetingsByTag(String tag, Pageable pageable);
    List<MeetingResponse> getUpcomingRecurringMeetings(@NotNull Long userId);

    MeetingResponse saveAsTemplate(
            @NotNull Long meetingId,
            String templateName,
            @NotNull Long userId
    );

    Page<MeetingResponse> searchMeetings(
            @Valid @NotNull SearchCriteria criteria,
            Pageable pageable
    );

    MeetingResponse getMeetingDetails(@NotNull Long meetingId, @NotNull Long userId);
    MeetingResponse getMeetingForVotingCreation(@NotNull Long meetingId, @NotNull Long userId);
}
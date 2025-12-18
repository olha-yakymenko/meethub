package com.meethub.domain.service;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.enums.MeetingStatus;
import com.meethub.domain.model.request.CreateMeetingRequest;
import com.meethub.domain.model.request.SearchCriteria;
import com.meethub.domain.model.request.UpdateMeetingRequest;
import com.meethub.domain.model.response.MeetingParticipationInfo;
import com.meethub.domain.model.response.MeetingResponse;
import com.meethub.domain.model.response.ParticipantResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

@Validated
public interface MeetingService {

    MeetingResponse createMeeting(
            @NotNull @Valid CreateMeetingRequest request,
            @NotNull @Positive Long organizerId
    );

    MeetingResponse updateMeeting(
            @NotNull @Positive Long meetingId,
            @NotNull @Valid UpdateMeetingRequest request,
            @NotNull @Positive Long organizerId
    );

    void deleteMeeting(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long organizerId
    );

    MeetingResponse getMeetingById(@NotNull @Positive Long meetingId);

    Page<MeetingResponse> getUserMeetings(
            @NotNull @Positive Long userId,
            @NotNull Pageable pageable
    );

    List<MeetingResponse> getUpcomingPublicMeetings();

    List<MeetingResponse> findNearbyMeetings(
            double latitude,
            double longitude,
            double radius
    );

    void changeMeetingStatus(
            @NotNull @Positive Long meetingId,
            @NotNull MeetingStatus status,
            @NotNull @Positive Long organizerId
    );

    MeetingResponse duplicateMeeting(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long organizerId
    );

    List<MeetingResponse> findConflictingMeetings(
            @NotNull @Positive Long userId,
            @NotNull LocalDateTime startDate,
            @NotNull LocalDateTime endDate
    );

    Page<MeetingResponse> getFilteredMeetings(
            String search,
            String type,
            String status,
            @NotNull Pageable pageable
    );

    Meeting getMeeting(@NotNull @Positive Long meetingId);

    MeetingParticipationInfo getMeetingParticipationInfo(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long userId
    );

    boolean canUserAccessMeeting(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long userId
    );

    List<MeetingResponse> getAccessibleMeetings(@NotNull @Positive Long userId);

    @Transactional(readOnly = true)
    List<MeetingResponse> getMeetingTemplates(@NotNull @Positive Long userId);

    @Transactional
    MeetingResponse createFromTemplate(
            @NotNull @Positive Long templateId,
            @NotNull @Positive Long organizerId,
            @NotNull LocalDateTime newStartDate
    );

    @Transactional
    List<MeetingResponse> generateNextRecurrence(
            @NotNull @Positive Long meetingId,
            int count
    );

    @Transactional
    void addRecurrenceException(
            @NotNull @Positive Long meetingId,
            @NotNull @Size(min = 1, max = 50) String exceptionDate,
            String reason
    );

    @Transactional(readOnly = true)
    List<MeetingResponse> getRecurrenceSeries(@NotNull @Positive Long originalMeetingId);

    @Transactional(readOnly = true)
    Page<MeetingResponse> getMeetingsByTag(
            @NotNull @Size(min = 1, max = 100) String tag,
            @NotNull Pageable pageable
    );

    @Transactional(readOnly = true)
    List<MeetingResponse> getUpcomingRecurringMeetings(@NotNull @Positive Long userId);

    MeetingResponse saveAsTemplate(
            @NotNull @Positive Long meetingId,
            @NotNull @Size(min = 1, max = 200) String templateName,
            @NotNull @Positive Long userId
    );

    Page<MeetingResponse> searchMeetings(
            @NotNull @Valid SearchCriteria criteria,
            @NotNull Pageable pageable
    );

    MeetingResponse getMeetingDetails(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long userId
    );

    MeetingResponse getMeetingForVotingCreation(
            @NotNull @Positive Long meetingId,
            @NotNull @Positive Long userId
    );

}





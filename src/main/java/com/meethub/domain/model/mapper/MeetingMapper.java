package com.meethub.domain.model.mapper;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.MeetingType;
import com.meethub.domain.model.enums.MeetingVisibility;
import com.meethub.domain.model.request.CreateMeetingRequest;
import com.meethub.domain.model.request.UpdateMeetingRequest;
import com.meethub.domain.model.response.MeetingResponse;
import com.meethub.domain.model.response.UserResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
@Slf4j

@Component
public class MeetingMapper {

    public Meeting toEntity(CreateMeetingRequest request) {
        if (request == null) {
            return null;
        }

        log.info("Mapping CreateMeetingRequest to Entity - Type: {}, Visibility: {}",
                request.getType(), request.getVisibility());

        Meeting meeting = new Meeting();
        meeting.setTitle(request.getTitle());
        meeting.setDescription(request.getDescription());
        meeting.setAgenda(request.getAgenda());

        // BEZ KONWERSJI - to już są enumy!
        meeting.setType(request.getType());
        meeting.setVisibility(request.getVisibility());

        meeting.setStartDate(request.getStartDate());
        meeting.setEndDate(request.getEndDate());
        meeting.setMaxParticipants(request.getMaxParticipants());

        if (request.getTags() != null) {
            meeting.setTags(new HashSet<>(request.getTags()));
        }

        log.info("Mapped meeting - Title: {}, Type: {}, Visibility: {}",
                meeting.getTitle(), meeting.getType(), meeting.getVisibility());
        return meeting;
    }

    public MeetingResponse toResponse(Meeting meeting) {
        if (meeting == null) {
            return null;
        }

        MeetingResponse response = new MeetingResponse();
        response.setId(meeting.getId());
        response.setTitle(meeting.getTitle());
        response.setDescription(meeting.getDescription());
        response.setAgenda(meeting.getAgenda());
        response.setType(meeting.getType());
        response.setStatus(meeting.getStatus());
        response.setVisibility(meeting.getVisibility());
        response.setStartDate(meeting.getStartDate());
        response.setEndDate(meeting.getEndDate());
        response.setMaxParticipants(meeting.getMaxParticipants());
        response.setTags(meeting.getTags());
        response.setCreatedAt(meeting.getCreatedAt());
        response.setUpdatedAt(meeting.getUpdatedAt());

        if (meeting.getOrganizer() != null) {
            response.setOrganizer(toUserResponse(meeting.getOrganizer()));
        }

        return response;
    }

    public void updateEntityFromRequest(UpdateMeetingRequest request, Meeting meeting) {
        if (request == null || meeting == null) {
            return;
        }

        if (request.getTitle() != null) {
            meeting.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            meeting.setDescription(request.getDescription());
        }
        if (request.getAgenda() != null) {
            meeting.setAgenda(request.getAgenda());
        }
        if (request.getType() != null) {
            // USUŃ KONWERSJĘ - to już jest enum!
            meeting.setType(request.getType());
        }
        if (request.getVisibility() != null) {
            // USUŃ KONWERSJĘ - to już jest enum!
            meeting.setVisibility(request.getVisibility());
        }
        if (request.getStartDate() != null) {
            meeting.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            meeting.setEndDate(request.getEndDate());
        }
        if (request.getMaxParticipants() != null) {
            meeting.setMaxParticipants(request.getMaxParticipants());
        }
        if (request.getTags() != null) {
            meeting.setTags(new HashSet<>(request.getTags()));
        }
    }

    public UserResponse toUserResponse(User user) {
        if (user == null) {
            return null;
        }

        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setRole(user.getRole());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setCreatedAt(user.getCreatedAt());

        return response;
    }
}
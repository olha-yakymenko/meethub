package com.meethub.domain.model.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meethub.domain.model.entity.Category;
import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.MeetingStatusChange;
import com.meethub.domain.model.entity.User;

import com.meethub.domain.model.request.CreateMeetingRequest;
import com.meethub.domain.model.request.UpdateMeetingRequest;
import com.meethub.domain.model.response.CategoryResponse;
import com.meethub.domain.model.response.MeetingResponse;
import com.meethub.domain.model.response.StatusChangeResponse;
import com.meethub.domain.model.response.UserResponse;
import com.meethub.domain.repository.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingMapper {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

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

        meeting.setRecurring(request.isRecurring());
        meeting.setRecurrencePattern(request.getRecurrencePattern());
        meeting.setRecurrenceEndDate(request.getRecurrenceEndDate());
        meeting.setRecurrenceExceptionsJson(request.getRecurrenceExceptionsJson());

        meeting.setTemplate(request.isSaveAsTemplate());
        if (request.isSaveAsTemplate() && request.getTemplateName() != null) {
            meeting.setTitle(request.getTemplateName());
        }

        log.info("Mapped meeting - Title: {}, Type: {}, Visibility: {}, Recurring: {}",
                meeting.getTitle(), meeting.getType(), meeting.getVisibility(), meeting.isRecurring());
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

        response.setRecurring(meeting.isRecurring());
        response.setRecurrencePattern(meeting.getRecurrencePattern());
        response.setRecurrenceEndDate(meeting.getRecurrenceEndDate());
        response.setRecurrenceExceptions(parseRecurrenceExceptions(meeting.getRecurrenceExceptionsJson()));

        response.setCategories(mapCategories(meeting.getCategories()));

        response.setTemplate(meeting.isTemplate());
        response.setOriginalMeetingId(meeting.getOriginalMeetingId());

        response.setStatusHistory(mapStatusChanges(meeting.getStatusChanges()));

        if (meeting.getOrganizer() != null) {
            response.setOrganizer(toUserResponse(meeting.getOrganizer()));
        }

        return response;
    }


    public void updateEntityFromRequest(UpdateMeetingRequest request, Meeting meeting) {
        if (request == null || meeting == null) {
            log.warn("Cannot update - request or meeting is null");
            return;
        }

        log.info("updateEntityFromRequest START - Meeting title before: {}", meeting.getTitle());
        log.info("Request fields - title: {}, status: {}",
                request.getTitle(), request.getStatus());

        if (request.getTitle() != null) {
            log.info("Updating title from '{}' to '{}'", meeting.getTitle(), request.getTitle());
            meeting.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            meeting.setDescription(request.getDescription());
        }
        if (request.getAgenda() != null) {
            meeting.setAgenda(request.getAgenda());
        }
        if (request.getType() != null) {
            meeting.setType(request.getType());
        }
        if (request.getVisibility() != null) {
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

        // ✅ NOWE POLA: Powtarzanie
        meeting.setRecurring(request.isRecurring());
        if (request.getRecurrencePattern() != null) {
            meeting.setRecurrencePattern(request.getRecurrencePattern());
        }
        if (request.getRecurrenceEndDate() != null) {
            meeting.setRecurrenceEndDate(request.getRecurrenceEndDate());
        }
        if (request.getRecurrenceExceptionsJson() != null) {
            meeting.setRecurrenceExceptionsJson(request.getRecurrenceExceptionsJson());
        }

        if (request.getStatus() != null && !request.getStatus().equals(meeting.getStatus())) {
            log.info("Status change in mapper: {} -> {}", meeting.getStatus(), request.getStatus());
            // Zapisz zmianę statusu w historii
            MeetingStatusChange statusChange = MeetingStatusChange.builder()
                    .meeting(meeting)
                    .oldStatus(meeting.getStatus().name())
                    .newStatus(request.getStatus().name())
                    .reason(request.getStatusChangeReason())
                    .changedAt(LocalDateTime.now())
                    .build();

            if (meeting.getStatusChanges() == null) {
                meeting.setStatusChanges(new ArrayList<>());
            }
            meeting.getStatusChanges().add(statusChange);

            meeting.setStatus(request.getStatus());
        }

        log.info("updateEntityFromRequest END - Meeting title after: {}", meeting.getTitle());
        log.info("Meeting status after: {}", meeting.getStatus());
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


    public List<String> parseRecurrenceExceptions(String json) {
        if (json == null || json.isEmpty() || json.equals("[]")) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse recurrence exceptions JSON: {}", json, e);
            return new ArrayList<>();
        }
    }

    public Set<CategoryResponse> mapCategories(Set<Category> categories) {
        if (categories == null) {
            return new HashSet<>();
        }

        return categories.stream()
                .map(cat -> CategoryResponse.builder()
                        .id(cat.getId())
                        .name(cat.getName())
                        .colorCode(cat.getColorCode())
                        .description(cat.getDescription())
                        .build())
                .collect(Collectors.toSet());
    }

    public List<StatusChangeResponse> mapStatusChanges(List<MeetingStatusChange> changes) {
        if (changes == null) {
            return new ArrayList<>();
        }

        return changes.stream()
                .map(change -> {
                    String changedByName = getUserNameById(change.getChangedByUserId());
                    return StatusChangeResponse.builder()
                            .oldStatus(change.getOldStatus())
                            .newStatus(change.getNewStatus())
                            .changedAt(change.getChangedAt())
                            .changedByName(changedByName)
                            .reason(change.getReason())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private String getUserNameById(Long userId) {
        if (userId == null) {
            return "System";
        }
        try {
            return userRepository.findById(userId)
                    .map(user -> user.getFirstName() + " " + user.getLastName())
                    .orElse("Unknown User");
        } catch (Exception e) {
            log.warn("Failed to get user name for ID: {}", userId, e);
            return "Unknown";
        }
    }


    public Meeting cloneMeeting(Meeting original) {
        if (original == null) {
            return null;
        }

        Meeting clone = new Meeting();

        clone.setTitle(original.getTitle() + " (Kopia)");
        clone.setDescription(original.getDescription());
        clone.setAgenda(original.getAgenda());
        clone.setType(original.getType());
        clone.setStatus(original.getStatus());
        clone.setVisibility(original.getVisibility());

        clone.setStartDate(original.getStartDate());
        clone.setEndDate(original.getEndDate());
        clone.setMaxParticipants(original.getMaxParticipants());

        clone.setRecurring(original.isRecurring());
        clone.setRecurrencePattern(original.getRecurrencePattern());
        clone.setRecurrenceEndDate(original.getRecurrenceEndDate());
        clone.setRecurrenceExceptionsJson(original.getRecurrenceExceptionsJson());

        if (original.getTags() != null) {
            clone.setTags(new HashSet<>(original.getTags()));
        }

        clone.setOriginalMeetingId(original.getId());

        clone.setTemplate(false);

        clone.setStatusChanges(new ArrayList<>());

        return clone;
    }

    public Meeting createTemplateFromMeeting(Meeting original, String templateName) {
        if (original == null) {
            return null;
        }

        original.setTemplate(true);

        if (templateName != null && !templateName.isBlank()) {
            original.setTitle(templateName);
        }

        return original;
    }

}
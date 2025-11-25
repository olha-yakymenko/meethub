package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.MeetingType;
import com.meethub.domain.model.enums.MeetingVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class CreateMeetingRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String description;
    private String agenda;

    @NotNull(message = "Meeting type is required")
    private MeetingType type;

    @NotNull(message = "Visibility is required")
    private MeetingVisibility visibility;

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    private LocalDateTime endDate;

    private Integer maxParticipants;
    private Long locationId;
    private Set<String> tags;
}
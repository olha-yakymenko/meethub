package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.MeetingType;
import com.meethub.domain.model.enums.MeetingVisibility;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder

public class UpdateMeetingRequest {
    private String title;
    private String description;
    private String agenda;
    private MeetingType type;
    private MeetingVisibility visibility;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer maxParticipants;
    private Long locationId;
    private Set<String> tags;
}
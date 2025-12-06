package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.MeetingStatus;
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

    // ✅ DODANE POLA DLA NOWYCH FUNKCJI
    private boolean recurring;
    private String recurrencePattern;
    private LocalDateTime recurrenceEndDate;
    private String recurrenceExceptionsJson; // JSON string

    private Set<Long> categoryIds;
    private MeetingStatus status;
    private String statusChangeReason;

    // ✅ BUILDER Z NOWYMI POLAMI
    public static class UpdateMeetingRequestBuilder {
        // Umożliwia używanie buildera z nowymi polami
    }
}
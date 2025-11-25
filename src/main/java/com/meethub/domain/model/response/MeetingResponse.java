package com.meethub.domain.model.response;

import com.meethub.domain.model.enums.MeetingStatus;
import com.meethub.domain.model.enums.MeetingType;
import com.meethub.domain.model.enums.MeetingVisibility;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class MeetingResponse {
    private Long id;
    private String title;
    private String description;
    private String agenda;
    private MeetingType type;
    private MeetingStatus status;
    private MeetingVisibility visibility;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer maxParticipants;
    private UserResponse organizer;
    private LocationResponse location;
    private Set<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Integer confirmedParticipantsCount;
    private Integer waitingListCount;
    private Integer availableSpots;

    // Metoda pomocnicza
    public boolean hasAvailableSpots() {
        return availableSpots == null || availableSpots > 0;
    }
}
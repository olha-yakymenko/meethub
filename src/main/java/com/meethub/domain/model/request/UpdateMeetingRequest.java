//package com.meethub.domain.model.request;
//
//import com.meethub.domain.model.enums.MeetingStatus;
//import com.meethub.domain.model.enums.MeetingType;
//import com.meethub.domain.model.enums.MeetingVisibility;
//import lombok.Builder;
//import lombok.Data;
//
//import java.time.LocalDateTime;
//import java.util.Set;
//
//@Data
//@Builder
//public class UpdateMeetingRequest {
//    private String title;
//    private String description;
//    private String agenda;
//    private MeetingType type;
//    private MeetingVisibility visibility;
//    private LocalDateTime startDate;
//    private LocalDateTime endDate;
//    private Integer maxParticipants;
//    private Long locationId;
//    private Set<String> tags;
//
//    private boolean recurring;
//    private String recurrencePattern;
//    private LocalDateTime recurrenceEndDate;
//    private String recurrenceExceptionsJson; // JSON string
//
//    private Set<Long> categoryIds;
//    private MeetingStatus status;
//    private String statusChangeReason;
//
//    public static class UpdateMeetingRequestBuilder {
//        // Umożliwia używanie buildera z nowymi polami
//    }
//}



package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.MeetingStatus;
import com.meethub.domain.model.enums.MeetingType;
import com.meethub.domain.model.enums.MeetingVisibility;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateMeetingRequest {

    @Size(min = 3, max = 200, message = "Title must be 3-200 characters")
    private String title;

    @Size(max = 5000, message = "Description cannot exceed 5000 characters")
    private String description;

    @Size(max = 5000, message = "Agenda cannot exceed 5000 characters")
    private String agenda;

    private MeetingType type;
    private MeetingVisibility visibility;

    @Future(message = "Start date must be in the future")
    private LocalDateTime startDate;

    @Future(message = "End date must be in the future")
    private LocalDateTime endDate;

    @Min(value = 1, message = "Maximum participants must be at least 1")
    @Max(value = 1000, message = "Maximum participants cannot exceed 1000")
    private Integer maxParticipants;

    private Long locationId;

    @Size(max = 20, message = "Cannot have more than 20 tags")
    private Set<String> tags;

    private boolean recurring;

    @Pattern(regexp = "^(DAILY|WEEKLY|MONTHLY|YEARLY)(:\\d+)?(:\\d+)?$",
            message = "Invalid recurrence pattern")
    private String recurrencePattern;

    @Future(message = "Recurrence end date must be in the future")
    private LocalDateTime recurrenceEndDate;

    private String recurrenceExceptionsJson;
    private Set<Long> categoryIds;
    private MeetingStatus status;

    @Size(max = 500, message = "Status change reason cannot exceed 500 characters")
    private String statusChangeReason;

    // Walidacja biznesowa
    @AssertTrue(message = "End date must be after start date")
    public boolean isEndDateValid() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return endDate.isAfter(startDate);
    }
}
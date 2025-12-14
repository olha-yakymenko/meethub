//package com.meethub.domain.model.request;
//
//import com.meethub.domain.model.enums.MeetingType;
//import com.meethub.domain.model.enums.MeetingVisibility;
//import jakarta.validation.constraints.NotBlank;
//import jakarta.validation.constraints.NotNull;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Set;
//
//@Builder
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//public class CreateMeetingRequest {
//    @NotBlank(message = "Title is required")
//    private String title;
//
//    private String description;
//    private String agenda;
//
//    @NotNull(message = "Meeting type is required")
//    private MeetingType type;
//
//    @NotNull(message = "Visibility is required")
//    private MeetingVisibility visibility;
//
//    @NotNull(message = "Start date is required")
//    private LocalDateTime startDate;
//
//    @NotNull(message = "End date is required")
//    private LocalDateTime endDate;
//
//    private Integer maxParticipants;
//    private Long locationId;
//    private Set<String> tags;
//
//    // ✅ DODANE POLA DLA NOWYCH FUNKCJI
//    private boolean recurring = false;
//    private String recurrencePattern; // Format: "DAILY:1", "WEEKLY:2", "MONTHLY:1:15", "YEARLY:1"
//    private LocalDateTime recurrenceEndDate;
//    private List<String> recurrenceExceptions; // Daty jako string "2024-01-15"
//
//    private Set<Long> categoryIds;
//    private boolean saveAsTemplate = false;
//    private String templateName;
//
//    // ✅ METODA POMOCNICZA
//    public String getRecurrenceExceptionsJson() {
//        if (recurrenceExceptions == null || recurrenceExceptions.isEmpty()) {
//            return "[]";
//        }
//        return "[\"" + String.join("\",\"", recurrenceExceptions) + "\"]";
//    }
//}






package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.MeetingType;
import com.meethub.domain.model.enums.MeetingVisibility;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateMeetingRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 200, message = "Title must be 3-200 characters")
    private String title;

    @Size(max = 5000, message = "Description cannot exceed 5000 characters")
    private String description;

    @Size(max = 5000, message = "Agenda cannot exceed 5000 characters")
    private String agenda;

    @NotNull(message = "Meeting type is required")
    private MeetingType type;

    @NotNull(message = "Visibility is required")
    private MeetingVisibility visibility;

    @NotNull(message = "Start date is required")
    @Future(message = "Start date must be in the future")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    @Future(message = "End date must be in the future")
    private LocalDateTime endDate;

    @Min(value = 1, message = "Maximum participants must be at least 1")
    @Max(value = 1000, message = "Maximum participants cannot exceed 1000")
    private Integer maxParticipants;

    private Long locationId;

    @Size(max = 20, message = "Cannot have more than 20 tags")
    private Set<String> tags;

    private boolean recurring = false;

    @Pattern(regexp = "^(DAILY|WEEKLY|MONTHLY|YEARLY)(:\\d+)?(:\\d+)?$",
            message = "Invalid recurrence pattern. Use: DAILY, WEEKLY:2, MONTHLY:1:15")
    private String recurrencePattern;

    @Future(message = "Recurrence end date must be in the future")
    private LocalDateTime recurrenceEndDate;

    @Size(max = 100, message = "Cannot have more than 100 recurrence exceptions")
    private List<String> recurrenceExceptions;

    @Size(max = 10, message = "Cannot assign more than 10 categories")
    private Set<Long> categoryIds;

    private boolean saveAsTemplate = false;

    @Size(max = 100, message = "Template name cannot exceed 100 characters")
    private String templateName;

    // Walidacje biznesowe
    @AssertTrue(message = "End date must be after start date")
    public boolean isEndDateAfterStartDate() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return endDate.isAfter(startDate);
    }

    @AssertTrue(message = "Meeting cannot exceed 24 hours")
    public boolean isDurationValid() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return java.time.Duration.between(startDate, endDate).toHours() <= 24;
    }

    @AssertTrue(message = "Recurrence pattern is required for recurring meetings")
    public boolean isRecurrenceValid() {
        if (recurring) {
            return recurrencePattern != null && !recurrencePattern.isBlank();
        }
        return true;
    }

    public String getRecurrenceExceptionsJson() {
        if (recurrenceExceptions == null || recurrenceExceptions.isEmpty()) {
            return "[]";
        }
        return "[\"" + String.join("\",\"", recurrenceExceptions) + "\"]";
    }
}
package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.MeetingType;
import com.meethub.domain.model.enums.MeetingVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    // ✅ DODANE POLA DLA NOWYCH FUNKCJI
    private boolean recurring = false;
    private String recurrencePattern; // Format: "DAILY:1", "WEEKLY:2", "MONTHLY:1:15", "YEARLY:1"
    private LocalDateTime recurrenceEndDate;
    private List<String> recurrenceExceptions; // Daty jako string "2024-01-15"

    private Set<Long> categoryIds;
    private boolean saveAsTemplate = false;
    private String templateName;

    // ✅ METODA POMOCNICZA
    public String getRecurrenceExceptionsJson() {
        if (recurrenceExceptions == null || recurrenceExceptions.isEmpty()) {
            return "[]";
        }
        return "[\"" + String.join("\",\"", recurrenceExceptions) + "\"]";
    }
}
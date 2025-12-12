package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.MeetingType;
import com.meethub.domain.model.enums.MeetingVisibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchCriteria {

    // Tekst i tagi
    private String keywords;
    private String tags;
    private List<String> searchFields;

    // Daty
    private LocalDate dateFrom;
    private LocalDate dateTo;

    // Typ i status
    private MeetingType type;

    @Builder.Default
    private List<String> statuses = new ArrayList<>();  // ✅ Zmiana z 'status' na 'statuses'

    // Uczestnicy
    @Builder.Default
    private Integer minParticipants = 0;

    @Builder.Default
    private Integer maxParticipants = 100;

    private String organizerName;
    private String myParticipation;

    // Zaawansowane
    private MeetingVisibility visibility;
//    private String sortBy;

    @Builder.Default
    private List<Long> categoryIds = new ArrayList<>();

    private Boolean recurringOnly;
    private Boolean templatesOnly;
    private Boolean hasAttachments;

    // Dodatkowe
    private Long currentUserId;
    private Boolean userAuthenticated;

    @Builder.Default
    private boolean includePublic = true;

    // ✅ Metody pomocnicze

    public boolean hasKeywords() {
        return keywords != null && !keywords.trim().isEmpty();
    }

    public boolean hasTags() {
        return tags != null && !tags.trim().isEmpty();
    }

    public boolean hasDateRange() {
        return dateFrom != null || dateTo != null;
    }

    public boolean hasType() {
        return type != null;
    }

    public boolean hasStatuses() {
        return statuses != null && !statuses.isEmpty();
    }

    public boolean hasParticipantsFilter() {
        return (minParticipants != null && minParticipants > 0) ||
                (maxParticipants != null && maxParticipants < 100);
    }

    public boolean hasOrganizerFilter() {
        return organizerName != null && !organizerName.trim().isEmpty();
    }

    public boolean hasMyParticipationFilter() {
        return myParticipation != null && !myParticipation.trim().isEmpty();
    }

    public boolean hasVisibilityFilter() {
        return visibility != null;
    }

    public boolean hasCategoryFilter() {
        return categoryIds != null && !categoryIds.isEmpty();
    }

    public boolean hasRecurringFilter() {
        return Boolean.TRUE.equals(recurringOnly);
    }

    public boolean hasTemplatesFilter() {
        return Boolean.TRUE.equals(templatesOnly);
    }

    public boolean hasAttachmentsFilter() {
        return Boolean.TRUE.equals(hasAttachments);
    }

    public List<String> getTagsList() {
        if (!hasTags()) {
            return List.of();
        }
        return List.of(tags.split(",")).stream()
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .toList();
    }

    public boolean isEmpty() {
        return !hasKeywords() && !hasTags() && !hasDateRange() && !hasType() &&
                !hasStatuses() && !hasParticipantsFilter() && !hasOrganizerFilter() &&
                !hasMyParticipationFilter() && !hasVisibilityFilter() && !hasCategoryFilter() &&
                !hasRecurringFilter() && !hasTemplatesFilter() && !hasAttachmentsFilter();
    }

    public void setType(String type) {
        if (type == null || type.isBlank()) {
            this.type = null;
        } else {
            this.type = MeetingType.valueOf(type.trim().toUpperCase());
        }
    }


}
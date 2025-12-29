package com.meethub.domain.model.request;

import com.meethub.domain.model.enums.MeetingType;
import com.meethub.domain.model.enums.MeetingVisibility;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class AdvancedSearchMeetingsRequest {

    // Wyszukiwanie tekstowe
    @Size(max = 100, message = "Wyszukiwanie nie może przekraczać 100 znaków")
    private String search;

    @Size(max = 100, message = "Słowa kluczowe nie mogą przekraczać 100 znaków")
    private String keywords;

    // Filtry podstawowe
    private List<String> tags = new ArrayList<>();
    private List<String> searchFields = new ArrayList<>();

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateTo;

    private String type;
    private String status;
    private List<String> statuses = new ArrayList<>();

    // Uczestnicy
    @Min(value = 0, message = "Minimalna liczba uczestników nie może być ujemna")
    private Integer minParticipants = 0;

    @Min(value = 1, message = "Maksymalna liczba uczestników musi być co najmniej 1")
    @Max(value = 100, message = "Maksymalna liczba uczestników nie może przekraczać 100")
    private Integer maxParticipants = 100;

    // Organizator
    @Size(max = 100, message = "Nazwa organizatora nie może przekraczać 100 znaków")
    private String organizerName;

    private String myParticipation;
    private String visibility;

    // Sortowanie
    @Size(max = 50, message = "Pole sortowania nie może przekraczać 50 znaków")
    private String sortBy = "startDate";

    @Pattern(regexp = "asc|desc", message = "Kolejność sortowania musi być 'asc' lub 'desc'")
    private String sortOrder = "desc";

    // Dodatkowe filtry
    private Boolean recurring;
    private Boolean recurringOnly;
    private Boolean template;
    private Boolean templatesOnly;
    private Boolean hasAttachments;

    // Paginacja
    @Min(value = 0, message = "Numer strony musi być nieujemny")
    private Integer page = 0;

    @Min(value = 1, message = "Rozmiar strony musi być co najmniej 1")
    @Max(value = 100, message = "Rozmiar strony nie może przekraczać 100")
    private Integer size = 12;


    public String getFinalKeywords() {
        return search != null ? search : keywords;
    }

    public Boolean getFinalRecurring() {
        return recurring != null ? recurring : recurringOnly;
    }

    public Boolean getFinalTemplate() {
        return template != null ? template : templatesOnly;
    }

    public List<String> getFinalStatuses() {
        if (statuses != null && !statuses.isEmpty()) {
            return statuses;
        }
        if (status != null && !status.isEmpty()) {
            return List.of(status);
        }
        return new ArrayList<>();
    }

    public List<String> getFinalSearchFields() {
        if (searchFields != null && !searchFields.isEmpty()) {
            return searchFields;
        }
        return List.of("TITLE", "DESCRIPTION");
    }

    public MeetingType getMeetingType() {
        if (type != null && !type.isBlank()) {
            try {
                return MeetingType.valueOf(type.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    public MeetingVisibility getMeetingVisibility() {
        if (visibility != null && !visibility.isEmpty()) {
            try {
                return MeetingVisibility.valueOf(visibility);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    public String getTagsAsString() {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        return String.join(",", tags);
    }
}
// WinningOptionResponse.java
package com.meethub.domain.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WinningOptionResponse {
    private Long optionId;
    private LocalDateTime optionDate;
    private Integer durationMinutes;
    private Integer voteCount;
    private Double percentage;
    private String algorithmUsed;
    private Boolean isTie;
    private Integer totalVoters;

    // Dodatkowe informacje dla organizatora
    private Integer totalParticipants;
    private Double participationRate;
    private String confidenceLevel;

    // Informacje o opcji
    private String optionDisplayText;
    private Boolean wasSuggested;
    private String suggestedBy;

    // Statystyki szczegółowe
    private Integer firstChoiceVotes;
    private Integer secondChoiceVotes;
    private Integer thirdChoiceVotes;

    // Metody pomocnicze
    public String getFormattedPercentage() {
        return percentage != null ? String.format("%.1f%%", percentage) : "0%";
    }

    public String getFormattedDateTime() {
        return optionDate != null ?
                optionDate.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) :
                "Nie określono";
    }

    public String getFormattedDuration() {
        if (durationMinutes == null) return "Nie określono";
        if (durationMinutes < 60) {
            return durationMinutes + " min";
        } else {
            int hours = durationMinutes / 60;
            int minutes = durationMinutes % 60;
            return minutes > 0 ? hours + "h " + minutes + "min" : hours + "h";
        }
    }

    public String getConfidenceBadge() {
        if (percentage == null) return "secondary";
        if (percentage >= 70) return "success";
        if (percentage >= 50) return "warning";
        return "danger";
    }
}
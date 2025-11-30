// VotingOptionResponse.java
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
public class VotingOptionResponse {
    private Long id;
    private LocalDateTime optionDate;
    private Integer durationMinutes;
    private Boolean isSuggested;
    private Long suggestedBy;
    private String suggestedByName;

    // Statystyki głosów
    private Integer voteCount;
    private Double percentage;
    private Boolean userVotedFor;
    private Integer preferenceScore;

    // Custom builder
    public static VotingOptionResponseBuilder builder() {
        return new VotingOptionResponseBuilder();
    }

    public static class VotingOptionResponseBuilder {
        private Long id;
        private LocalDateTime optionDate;
        private Integer durationMinutes;
        private Boolean isSuggested;
        private Long suggestedBy;
        private String suggestedByName;
        private Integer voteCount;
        private Double percentage;
        private Boolean userVotedFor;
        private Integer preferenceScore;

        public VotingOptionResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public VotingOptionResponseBuilder optionDate(LocalDateTime optionDate) {
            this.optionDate = optionDate;
            return this;
        }

        public VotingOptionResponseBuilder durationMinutes(Integer durationMinutes) {
            this.durationMinutes = durationMinutes;
            return this;
        }

        public VotingOptionResponseBuilder isSuggested(Boolean isSuggested) {
            this.isSuggested = isSuggested;
            return this;
        }

        public VotingOptionResponseBuilder suggestedBy(Long suggestedBy) {
            this.suggestedBy = suggestedBy;
            return this;
        }

        public VotingOptionResponseBuilder suggestedByName(String suggestedByName) {
            this.suggestedByName = suggestedByName;
            return this;
        }

        public VotingOptionResponseBuilder voteCount(Integer voteCount) {
            this.voteCount = voteCount;
            return this;
        }

        public VotingOptionResponseBuilder percentage(Double percentage) {
            this.percentage = percentage;
            return this;
        }

        public VotingOptionResponseBuilder userVotedFor(Boolean userVotedFor) {
            this.userVotedFor = userVotedFor;
            return this;
        }

        public VotingOptionResponseBuilder preferenceScore(Integer preferenceScore) {
            this.preferenceScore = preferenceScore;
            return this;
        }

        public VotingOptionResponse build() {
            VotingOptionResponse response = new VotingOptionResponse();
            response.setId(this.id);
            response.setOptionDate(this.optionDate);
            response.setDurationMinutes(this.durationMinutes);
            response.setIsSuggested(this.isSuggested);
            response.setSuggestedBy(this.suggestedBy);
            response.setSuggestedByName(this.suggestedByName);
            response.setVoteCount(this.voteCount);
            response.setPercentage(this.percentage);
            response.setUserVotedFor(this.userVotedFor);
            response.setPreferenceScore(this.preferenceScore);
            return response;
        }
    }

    // Metody pomocnicze
    public String getFormattedDateTime() {
        if (optionDate == null) return "Nie określono";
        return optionDate.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }

    public String getFormattedDate() {
        if (optionDate == null) return "Nie określono";
        return optionDate.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

    public String getFormattedTime() {
        if (optionDate == null) return "Nie określono";
        return optionDate.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
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

    public Double getPercentage() {
        if (percentage != null) return percentage;
        return 0.0;
    }

    public String getPercentageFormatted() {
        return String.format("%.1f%%", getPercentage());
    }
}
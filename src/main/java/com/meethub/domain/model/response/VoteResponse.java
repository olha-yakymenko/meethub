// VoteResponse.java
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
public class VoteResponse {
    private Long id;
    private Long optionId;
    private LocalDateTime votedAt;
    private Integer preferenceOrder;
    private Integer voteWeight;

    // Informacje o opcji
    private LocalDateTime optionDate;
    private Integer durationMinutes;

    // Status
    private Boolean success;
    private String message;

    // Custom builder
    public static VoteResponseBuilder builder() {
        return new VoteResponseBuilder();
    }

    public static class VoteResponseBuilder {
        private Long id;
        private Long optionId;
        private LocalDateTime votedAt;
        private Integer preferenceOrder;
        private Integer voteWeight;
        private LocalDateTime optionDate;
        private Integer durationMinutes;
        private Boolean success;
        private String message;

        public VoteResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public VoteResponseBuilder optionId(Long optionId) {
            this.optionId = optionId;
            return this;
        }

        public VoteResponseBuilder votedAt(LocalDateTime votedAt) {
            this.votedAt = votedAt;
            return this;
        }

        public VoteResponseBuilder preferenceOrder(Integer preferenceOrder) {
            this.preferenceOrder = preferenceOrder;
            return this;
        }

        public VoteResponseBuilder voteWeight(Integer voteWeight) {
            this.voteWeight = voteWeight;
            return this;
        }

        public VoteResponseBuilder optionDate(LocalDateTime optionDate) {
            this.optionDate = optionDate;
            return this;
        }

        public VoteResponseBuilder durationMinutes(Integer durationMinutes) {
            this.durationMinutes = durationMinutes;
            return this;
        }

        public VoteResponseBuilder success(Boolean success) {
            this.success = success;
            return this;
        }

        public VoteResponseBuilder message(String message) {
            this.message = message;
            return this;
        }

        public VoteResponse build() {
            VoteResponse response = new VoteResponse();
            response.setId(this.id);
            response.setOptionId(this.optionId);
            response.setVotedAt(this.votedAt);
            response.setPreferenceOrder(this.preferenceOrder);
            response.setVoteWeight(this.voteWeight);
            response.setOptionDate(this.optionDate);
            response.setDurationMinutes(this.durationMinutes);
            response.setSuccess(this.success);
            response.setMessage(this.message);
            return response;
        }
    }

    // Metody pomocnicze
    public String getFormattedVotedAt() {
        return votedAt != null ?
                votedAt.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) :
                "Nie określono";
    }

    public boolean isSuccessful() {
        return Boolean.TRUE.equals(success);
    }
}
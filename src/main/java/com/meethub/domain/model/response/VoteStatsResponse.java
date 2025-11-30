// VoteStatsResponse.java
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
public class VoteStatsResponse {
    private Integer totalVotes;
    private Integer totalVoters;
    private Integer totalOptions;
    private LocalDateTime votingEndsIn;
    private Boolean isClosed;
    private WinningOptionResponse winningOption;

    // Szczegółowe statystyki
    private Double averageVotesPerUser;
    private Integer usersWithNoVote;
    private Double participationRate;

    // Informacje o czasie
    private String timeRemaining;
    private Boolean isDeadlinePassed;

    // Custom builder z metodami pomocniczymi
    public static class VoteStatsResponseBuilder {
        private Double averageVotesPerUser;
        private Integer usersWithNoVote;
        private Double participationRate;
        private String timeRemaining;
        private Boolean isDeadlinePassed;

        public VoteStatsResponseBuilder calculateDerivedStats(Integer totalParticipants) {
            // Oblicz średnią liczbę głosów na użytkownika
            if (this.totalVotes != null && this.totalVoters != null && this.totalVoters > 0) {
                this.averageVotesPerUser = (double) this.totalVotes / this.totalVoters;
            }

            // Oblicz użytkowników bez głosu
            if (totalParticipants != null && this.totalVoters != null) {
                this.usersWithNoVote = totalParticipants - this.totalVoters;
            }

            // Oblicz frekwencję
            if (totalParticipants != null && totalParticipants > 0 && this.totalVoters != null) {
                this.participationRate = (this.totalVoters * 100.0) / totalParticipants;
            }

            // Oblicz pozostały czas
            if (this.votingEndsIn != null) {
                LocalDateTime now = LocalDateTime.now();
                this.isDeadlinePassed = now.isAfter(this.votingEndsIn);

                if (!this.isDeadlinePassed) {
                    java.time.Duration duration = java.time.Duration.between(now, this.votingEndsIn);
                    this.timeRemaining = formatDuration(duration);
                } else {
                    this.timeRemaining = "Czas minął";
                }
            }

            return this;
        }

        public VoteStatsResponseBuilder withWinningOption(WinningOptionResponse winningOption) {
            this.winningOption = winningOption;
            return this;
        }

        public VoteStatsResponseBuilder withWinningOptionFromVotes(Integer totalVotes, VotingOptionResponse winningOption) {
            if (winningOption != null && totalVotes != null && totalVotes > 0) {
                double percentage = (winningOption.getVoteCount() * 100.0) / totalVotes;
                this.winningOption = WinningOptionResponse.builder()
                        .optionId(winningOption.getId())
                        .optionDate(winningOption.getOptionDate())
                        .durationMinutes(winningOption.getDurationMinutes())
                        .voteCount(winningOption.getVoteCount())
                        .percentage(percentage)
                        .totalVoters(this.totalVoters)
                        .build();
            }
            return this;
        }

        private String formatDuration(java.time.Duration duration) {
            long days = duration.toDays();
            long hours = duration.toHours() % 24;
            long minutes = duration.toMinutes() % 60;

            if (days > 0) return days + " dni " + hours + " godz.";
            if (hours > 0) return hours + " godz. " + minutes + " min";
            if (minutes > 0) return minutes + " minut";
            return "Mniej niż minuta";
        }
    }

    // Metody pomocnicze
    public String getParticipationRateFormatted() {
        return participationRate != null ? String.format("%.1f%%", participationRate) : "0%";
    }

    public String getAverageVotesFormatted() {
        return averageVotesPerUser != null ? String.format("%.1f", averageVotesPerUser) : "0";
    }

    public boolean hasWinner() {
        return winningOption != null && winningOption.getVoteCount() != null && winningOption.getVoteCount() > 0;
    }

    public String getWinnerConfidence() {
        if (winningOption == null || winningOption.getPercentage() == null) return "BRAK";

        double percentage = winningOption.getPercentage();
        if (percentage >= 70) return "WYSOKA";
        if (percentage >= 50) return "ŚREDNIA";
        if (percentage >= 30) return "NISKA";
        return "BARDZO NISKA";
    }

    public String getVotingStatus() {
        if (Boolean.TRUE.equals(isClosed)) return "ZAMKNIĘTE";
        if (Boolean.TRUE.equals(isDeadlinePassed)) return "CZAS MINĄŁ";
        if (timeRemaining != null) return "AKTYWNE (" + timeRemaining + ")";
        return "AKTYWNE";
    }

    public boolean isHighParticipation() {
        return participationRate != null && participationRate >= 70;
    }

    public boolean isMediumParticipation() {
        return participationRate != null && participationRate >= 50;
    }

    public boolean isLowParticipation() {
        return participationRate != null && participationRate < 50;
    }
}
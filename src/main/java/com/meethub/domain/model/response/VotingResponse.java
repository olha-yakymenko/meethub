//// VotingResponse.java
//package com.meethub.domain.model.response;
//
//import com.meethub.domain.model.enums.VotingStatus;
//import com.meethub.domain.model.enums.VotingType;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//
//@Data
//@Builder
//@NoArgsConstructor
//@AllArgsConstructor
//public class VotingResponse {
//    private Long id;
//    private String title;
//    private String description;
//    private VotingStatus status;
//    private VotingType type;
//    private Integer maxChoices;
//    private Boolean allowSuggestions;
//    private LocalDateTime deadlineDate;
//    private Boolean autoClose;
//    private LocalDateTime createdAt;
//    private LocalDateTime updatedAt;
//
//    // Relacje
//    @Builder.Default
//    private List<VotingOptionResponse> options = new ArrayList<>();
//    private VoteStatsResponse stats;
//    private UserResponse createdBy;
//
//    // Informacje o użytkowniku
//    private Boolean hasVoted;
//    @Builder.Default
//    private List<VoteResponse> userVotes = new ArrayList<>();
//    private Boolean canVote;
//    private Boolean canManage;
//
//    // Custom builder z metodami pomocniczymi
//    public static class VotingResponseBuilder {
//        private List<VotingOptionResponse> options = new ArrayList<>();
//        private List<VoteResponse> userVotes = new ArrayList<>();
//        private Boolean canVote;
//        private Boolean canManage;
//
//        public VotingResponseBuilder option(VotingOptionResponse option) {
//            if (this.options == null) {
//                this.options = new ArrayList<>();
//            }
//            this.options.add(option);
//            return this;
//        }
//
//        public VotingResponseBuilder options(List<VotingOptionResponse> options) {
//            this.options = options != null ? new ArrayList<>(options) : new ArrayList<>();
//            return this;
//        }
//
//        public VotingResponseBuilder userVote(VoteResponse vote) {
//            if (this.userVotes == null) {
//                this.userVotes = new ArrayList<>();
//            }
//            this.userVotes.add(vote);
//            return this;
//        }
//
//        public VotingResponseBuilder userVotes(List<VoteResponse> userVotes) {
//            this.userVotes = userVotes != null ? new ArrayList<>(userVotes) : new ArrayList<>();
//            this.hasVoted = userVotes != null && !userVotes.isEmpty();
//            return this;
//        }
//
//        public VotingResponseBuilder calculateStats() {
//            if (this.options != null && !this.options.isEmpty()) {
//                int totalVotes = this.options.stream()
//                        .mapToInt(opt -> opt.getVoteCount() != null ? opt.getVoteCount() : 0)
//                        .sum();
//
//                int totalVoters = this.options.stream()
//                        .mapToInt(opt -> opt.getVoteCount() != null ? opt.getVoteCount() : 0)
//                        .sum(); // Uproszczone - w rzeczywistości trzeba by liczyć unikalnych głosujących
//
//                // Oblicz procenty dla każdej opcji
//                for (VotingOptionResponse option : this.options) {
//                    if (option.getVoteCount() != null && totalVotes > 0) {
//                        double percentage = (option.getVoteCount() * 100.0) / totalVotes;
//                        // Ustaw percentage bezpośrednio w obiekcie
//                        try {
//                            option.getClass().getMethod("setPercentage", Double.class).invoke(option, percentage);
//                        } catch (Exception e) {
//                            // Fallback - percentage zostanie obliczone w getterze
//                        }
//                    }
//                }
//
//                // Znajdź zwycięską opcję
//                VotingOptionResponse winningOption = this.options.stream()
//                        .filter(opt -> opt.getVoteCount() != null)
//                        .max((a, b) -> {
//                            int voteCompare = Integer.compare(b.getVoteCount(), a.getVoteCount());
//                            if (voteCompare != 0) return voteCompare;
//                            double aPercent = a.getPercentage() != null ? a.getPercentage() : 0;
//                            double bPercent = b.getPercentage() != null ? b.getPercentage() : 0;
//                            return Double.compare(bPercent, aPercent);
//                        })
//                        .orElse(null);
//
//                this.stats = VoteStatsResponse.builder()
//                        .totalVotes(totalVotes)
//                        .totalVoters(totalVoters)
//                        .totalOptions(this.options.size())
//                        .votingEndsIn(this.deadlineDate)
//                        .isClosed(this.status == VotingStatus.CLOSED)
//                        .winningOption(winningOption != null ?
//                                WinningOptionResponse.builder()
//                                        .optionId(winningOption.getId())
//                                        .optionDate(winningOption.getOptionDate())
//                                        .durationMinutes(winningOption.getDurationMinutes())
//                                        .voteCount(winningOption.getVoteCount())
//                                        .percentage(winningOption.getPercentage())
//                                        .build() : null)
//                        .build();
//            }
//            return this;
//        }
//
//        public VotingResponseBuilder determinePermissions(Long currentUserId, Long organizerId) {
//            this.canManage = currentUserId != null && currentUserId.equals(organizerId);
//            this.canVote = currentUserId != null && !this.hasVoted && this.status == VotingStatus.ACTIVE;
//            return this;
//        }
//    }
//
//    // Metody pomocnicze
//    public boolean isActive() {
//        return status == VotingStatus.ACTIVE;
//    }
//
//    public boolean isClosed() {
//        return status == VotingStatus.CLOSED;
//    }
//
//    public boolean isExpired() {
//        return deadlineDate != null && LocalDateTime.now().isAfter(deadlineDate);
//    }
//
//    public boolean canUserVote(Long userId) {
//        return canVote != null ? canVote :
//                userId != null && !hasVoted && isActive() && !isExpired();
//    }
//
//    public boolean canUserManage(Long userId, Long organizerId) {
//        return canManage != null ? canManage :
//                userId != null && userId.equals(organizerId);
//    }
//
//    public String getTimeRemaining() {
//        if (deadlineDate == null || isClosed()) return null;
//
//        LocalDateTime now = LocalDateTime.now();
//        if (now.isAfter(deadlineDate)) return "Czas minął";
//
//        java.time.Duration duration = java.time.Duration.between(now, deadlineDate);
//        long days = duration.toDays();
//        long hours = duration.toHours() % 24;
//        long minutes = duration.toMinutes() % 60;
//
//        if (days > 0) return days + " dni " + hours + " godz.";
//        if (hours > 0) return hours + " godz. " + minutes + " min";
//        return minutes + " minut";
//    }
//
//    public Integer getTotalVotes() {
//        return stats != null ? stats.getTotalVotes() : 0;
//    }
//
//    public Integer getTotalVoters() {
//        return stats != null ? stats.getTotalVoters() : 0;
//    }
//
//    public WinningOptionResponse getWinningOption() {
//        return stats != null ? stats.getWinningOption() : null;
//    }
//}









// VotingResponse.java
package com.meethub.domain.model.response;

import com.meethub.domain.model.enums.VotingStatus;
import com.meethub.domain.model.enums.VotingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VotingResponse {
    private Long id;
    private String title;
    private String description;
    private VotingStatus status;
    private VotingType type;
    private Integer maxChoices;
    private Boolean allowSuggestions;
    private LocalDateTime deadlineDate;
    private Boolean autoClose;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Relacje - UŻYJ @Builder.Default dla list
    @Builder.Default
    private List<VotingOptionResponse> options = new ArrayList<>();

    @Builder.Default
    private List<VoteResponse> userVotes = new ArrayList<>();

    private VoteStatsResponse stats;
    private UserResponse createdBy;

    // Informacje o użytkowniku
    private Boolean hasVoted;
    private Boolean canVote;
    private Boolean canManage;

    // Metody pomocnicze
    public boolean isActive() {
        return status == VotingStatus.ACTIVE;
    }

    public boolean isClosed() {
        return status == VotingStatus.CLOSED;
    }

    public boolean isExpired() {
        return deadlineDate != null && LocalDateTime.now().isAfter(deadlineDate);
    }

    public boolean canUserVote(Long userId) {
        return canVote != null ? canVote :
                userId != null && !hasVoted && isActive() && !isExpired();
    }

    public boolean canUserManage(Long userId, Long organizerId) {
        return canManage != null ? canManage :
                userId != null && userId.equals(organizerId);
    }

    public String getTimeRemaining() {
        if (deadlineDate == null || isClosed()) return null;

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(deadlineDate)) return "Czas minął";

        java.time.Duration duration = java.time.Duration.between(now, deadlineDate);
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;

        if (days > 0) return days + " dni " + hours + " godz.";
        if (hours > 0) return hours + " godz. " + minutes + " min";
        return minutes + " minut";
    }

    public Integer getTotalVotes() {
        return stats != null ? stats.getTotalVotes() : 0;
    }

    public Integer getTotalVoters() {
        return stats != null ? stats.getTotalVoters() : 0;
    }

    public WinningOptionResponse getWinningOption() {
        return stats != null ? stats.getWinningOption() : null;
    }

    // Metody statyczne do budowania z dodatkową logiką
    public static VotingResponseBuilder builder() {
        return new VotingResponseBuilder();
    }

    // Custom builder methods jako osobne metody
    public static class VotingResponseBuilder {
        // Lombok automatycznie generuje metody buildera
    }

    // Metoda do obliczania statystyk - wywołuj ją po zbudowaniu obiektu
    public void calculateStats() {
        if (this.options != null && !this.options.isEmpty()) {
            int totalVotes = this.options.stream()
                    .mapToInt(opt -> opt.getVoteCount() != null ? opt.getVoteCount() : 0)
                    .sum();

            int totalVoters = this.options.stream()
                    .mapToInt(opt -> opt.getVoteCount() != null ? opt.getVoteCount() : 0)
                    .sum();

            // Oblicz procenty dla każdej opcji
            for (VotingOptionResponse option : this.options) {
                if (option.getVoteCount() != null && totalVotes > 0) {
                    double percentage = (option.getVoteCount() * 100.0) / totalVotes;
                    option.setPercentage(percentage);
                }
            }

            // Znajdź zwycięską opcję
            VotingOptionResponse winningOption = this.options.stream()
                    .filter(opt -> opt.getVoteCount() != null)
                    .max((a, b) -> {
                        int voteCompare = Integer.compare(b.getVoteCount(), a.getVoteCount());
                        if (voteCompare != 0) return voteCompare;
                        double aPercent = a.getPercentage() != null ? a.getPercentage() : 0;
                        double bPercent = b.getPercentage() != null ? b.getPercentage() : 0;
                        return Double.compare(bPercent, aPercent);
                    })
                    .orElse(null);

            this.stats = VoteStatsResponse.builder()
                    .totalVotes(totalVotes)
                    .totalVoters(totalVoters)
                    .totalOptions(this.options.size())
                    .votingEndsIn(this.deadlineDate)
                    .isClosed(this.status == VotingStatus.CLOSED)
                    .winningOption(winningOption != null ?
                            WinningOptionResponse.builder()
                                    .optionId(winningOption.getId())
                                    .optionDate(winningOption.getOptionDate())
                                    .durationMinutes(winningOption.getDurationMinutes())
                                    .voteCount(winningOption.getVoteCount())
                                    .percentage(winningOption.getPercentage())
                                    .build() : null)
                    .build();
        }
    }

    // Metoda do określania uprawnień - wywołuj ją po zbudowaniu obiektu
    public void determinePermissions(Long currentUserId, Long organizerId) {
        this.canManage = currentUserId != null && currentUserId.equals(organizerId);
        this.canVote = currentUserId != null && !this.hasVoted && this.status == VotingStatus.ACTIVE;
    }
}
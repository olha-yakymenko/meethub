// TaskStatsResponse.java
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
public class TaskStatsResponse {

    // Podstawowe informacje o zadaniu
    private Long taskId;
    private String taskTitle;
    private String taskStatus;
    private LocalDateTime taskDeadline;
    private LocalDateTime taskCreatedAt;

    // Statystyki przypisań
    private Integer totalAssignments;
    private Integer completedAssignments;
    private Integer inProgressAssignments;
    private Integer pendingAssignments;
    private Integer rejectedAssignments;

    // Statystyki plików
    private Integer totalFiles;
    private Long totalFileSize; // w bajtach
    private Integer uniqueUsersWithFiles;

    // Wskaźniki efektywności
    private Double completionRate; // w procentach (0-100)
    private Double participationRate; // w procentach (0-100)
    private Double averageFilesPerUser;

    // Informacje o czasie
    private Boolean isOverdue;
    private Long daysUntilDeadline;
    private String timeRemaining;

    // Statystyki zaawansowane
    private Double averageCompletionTime; // w godzinach
    private Integer activeUsersCount;
    private Integer inactiveUsersCount;

    // Metody pomocnicze
    public String getCompletionRateFormatted() {
        return completionRate != null ? String.format("%.1f%%", completionRate) : "0%";
    }

    public String getParticipationRateFormatted() {
        return participationRate != null ? String.format("%.1f%%", participationRate) : "0%";
    }

    public String getTotalFileSizeFormatted() {
        if (totalFileSize == null || totalFileSize == 0) {
            return "0 B";
        }

        if (totalFileSize < 1024) {
            return totalFileSize + " B";
        } else if (totalFileSize < 1024 * 1024) {
            return String.format("%.1f KB", totalFileSize / 1024.0);
        } else if (totalFileSize < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", totalFileSize / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", totalFileSize / (1024.0 * 1024.0 * 1024.0));
        }
    }

    public String getAverageCompletionTimeFormatted() {
        if (averageCompletionTime == null || averageCompletionTime == 0) {
            return "Brak danych";
        }

        if (averageCompletionTime < 1) {
            return String.format("%.0f min", averageCompletionTime * 60);
        } else if (averageCompletionTime < 24) {
            return String.format("%.1f godz.", averageCompletionTime);
        } else {
            return String.format("%.1f dni", averageCompletionTime / 24);
        }
    }

    public String getProgressStatus() {
        if (completionRate == null) return "BRAK_DANYCH";

        if (completionRate >= 90) return "DOSKONAŁY";
        if (completionRate >= 70) return "DOBRY";
        if (completionRate >= 50) return "ŚREDNI";
        if (completionRate >= 25) return "SŁABY";
        return "KRYTYCZNY";
    }

    public String getProgressColor() {
        String status = getProgressStatus();
        switch (status) {
            case "DOSKONAŁY": return "success";
            case "DOBRY": return "info";
            case "ŚREDNI": return "warning";
            case "SŁABY": return "orange";
            case "KRYTYCZNY": return "danger";
            default: return "secondary";
        }
    }

    public boolean isHighPriority() {
        return isOverdue != null && isOverdue ||
                (daysUntilDeadline != null && daysUntilDeadline <= 1);
    }

    public boolean needsAttention() {
        return completionRate != null && completionRate < 50 &&
                (isOverdue != null && isOverdue ||
                        (daysUntilDeadline != null && daysUntilDeadline <= 3));
    }

    // Builder z metodami pomocniczymi
    public static class TaskStatsResponseBuilder {
        Double completionRate;
        Double participationRate;
        Double averageFilesPerUser;
        private Boolean isOverdue;
        Long daysUntilDeadline;
        String timeRemaining;

        public TaskStatsResponseBuilder calculateRates() {
            // Oblicz wskaźnik ukończenia
            if (this.totalAssignments != null && this.totalAssignments > 0) {
                this.completionRate = (this.completedAssignments * 100.0) / this.totalAssignments;
            } else {
                this.completionRate = 0.0;
            }

            // Oblicz wskaźnik uczestnictwa
            if (this.totalAssignments != null && this.uniqueUsersWithFiles != null) {
                this.participationRate = (this.uniqueUsersWithFiles * 100.0) / this.totalAssignments;
            } else {
                this.participationRate = 0.0;
            }

            // Oblicz średnią liczbę plików na użytkownika
            if (this.uniqueUsersWithFiles != null && this.uniqueUsersWithFiles > 0) {
                this.averageFilesPerUser = this.totalFiles.doubleValue() / this.uniqueUsersWithFiles;
            } else {
                this.averageFilesPerUser = 0.0;
            }

            return this;
        }

        public TaskStatsResponseBuilder calculateTimeMetrics(LocalDateTime deadline) {
            if (deadline != null) {
                LocalDateTime now = LocalDateTime.now();
                this.isOverdue = now.isAfter(deadline);

                if (!this.isOverdue) {
                    java.time.Duration duration = java.time.Duration.between(now, deadline);
                    this.daysUntilDeadline = duration.toDays();

                    long hours = duration.toHours() % 24;
                    long minutes = duration.toMinutes() % 60;

                    if (this.daysUntilDeadline > 0) {
                        this.timeRemaining = this.daysUntilDeadline + " dni";
                    } else if (hours > 0) {
                        this.timeRemaining = hours + " godz. " + minutes + " min";
                    } else {
                        this.timeRemaining = minutes + " minut";
                    }
                } else {
                    this.daysUntilDeadline = 0L;
                    this.timeRemaining = "PRZETERMINOWANE";
                }
            }
            return this;
        }

        public TaskStatsResponseBuilder withAssignmentStats(Integer total, Integer completed,
                                                            Integer inProgress, Integer pending,
                                                            Integer rejected) {
            this.totalAssignments = total;
            this.completedAssignments = completed != null ? completed : 0;
            this.inProgressAssignments = inProgress != null ? inProgress : 0;
            this.pendingAssignments = pending != null ? pending : 0;
            this.rejectedAssignments = rejected != null ? rejected : 0;
            return this;
        }

        public TaskStatsResponseBuilder withFileStats(Integer totalFiles, Long totalFileSize,
                                                      Integer uniqueUsers) {
            this.totalFiles = totalFiles != null ? totalFiles : 0;
            this.totalFileSize = totalFileSize != null ? totalFileSize : 0L;
            this.uniqueUsersWithFiles = uniqueUsers != null ? uniqueUsers : 0;
            return this;
        }
    }

    // Metody statyczne do tworzenia obiektów
    public static TaskStatsResponse empty() {
        return TaskStatsResponse.builder()
                .totalAssignments(0)
                .completedAssignments(0)
                .inProgressAssignments(0)
                .pendingAssignments(0)
                .rejectedAssignments(0)
                .totalFiles(0)
                .totalFileSize(0L)
                .uniqueUsersWithFiles(0)
                .completionRate(0.0)
                .participationRate(0.0)
                .averageFilesPerUser(0.0)
                .isOverdue(false)
                .daysUntilDeadline(0L)
                .timeRemaining("Brak terminu")
                .build();
    }

    public static TaskStatsResponse fromBasicStats(Long taskId, String taskTitle, String taskStatus,
                                                   Integer totalAssignments, Integer completedAssignments,
                                                   Integer totalFiles) {
        return TaskStatsResponse.builder()
                .taskId(taskId)
                .taskTitle(taskTitle)
                .taskStatus(taskStatus)
                .totalAssignments(totalAssignments)
                .completedAssignments(completedAssignments)
                .totalFiles(totalFiles)
                .calculateRates()
                .build();
    }
}
package com.meethub.controller.api;

import com.meethub.domain.model.entity.MeetingStatistics;
import com.meethub.domain.model.request.ReportFilter;
import com.meethub.domain.model.response.ApiResponse;
import com.meethub.domain.model.response.MeetingStatisticsResponse;
import com.meethub.domain.model.response.OrganizerReport;
import com.meethub.domain.service.MeetingAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Validated
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "API do analityki spotkań")
public class AnalyticsController {

    private final MeetingAnalyticsService analyticsService;

    // ========== STATYSTYKI SPOTKANIA ==========

    @PostMapping("/meetings/{meetingId}/statistics")
    @Operation(summary = "Generuje statystyki spotkania",
            description = "Generuje i zapisuje statystyki dla spotkania.")
    public ResponseEntity<ApiResponse<MeetingStatisticsResponse>> generateMeetingStatistics(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId) {

        var stats = analyticsService.generateMeetingStatistics(meetingId);
        var response = convertToResponse(stats);
        return ResponseEntity.ok(ApiResponse.success("Statystyki wygenerowane", response));
    }

    @GetMapping("/meetings/{meetingId}/report")
    @Operation(summary = "Pobiera raport spotkania",
            description = "Zwraca wygenerowany raport ze statystykami spotkania.")
    public ResponseEntity<ApiResponse<MeetingStatisticsResponse>> getMeetingReport(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId) {

        var stats = analyticsService.generateMeetingStatistics(meetingId);
        var response = convertToResponse(stats);
        return ResponseEntity.ok(ApiResponse.success("Raport spotkania wygenerowany", response));
    }

    @GetMapping("/meetings/{meetingId}/export/csv")
    @Operation(summary = "Eksportuje statystyki do CSV",
            description = "Eksportuje statystyki spotkania do pliku CSV.")
    public ResponseEntity<Resource> exportMeetingCsv(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId) {

        byte[] csv = analyticsService.exportMeetingStatisticsToCsv(meetingId);
        String filename = "statystyki_spotkania_" + meetingId + "_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(csv.length)
                .body(new ByteArrayResource(csv));
    }

    @GetMapping("/meetings/{meetingId}/export/pdf")
    @Operation(summary = "Eksportuje statystyki do PDF",
            description = "Eksportuje statystyki spotkania do pliku PDF.")
    public ResponseEntity<Resource> exportMeetingPdf(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId) {

        byte[] pdf = analyticsService.exportMeetingStatisticsToPdf(meetingId);
        String filename = "statystyki_spotkania_" + meetingId + "_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(new ByteArrayResource(pdf));
    }

    private MeetingStatisticsResponse convertToResponse(MeetingStatistics stats) {
        if (stats == null) return null;

        return MeetingStatisticsResponse.builder()
                .id(stats.getId())
                .meetingId(stats.getMeeting() != null ? stats.getMeeting().getId() : null)
                .meetingTitle(stats.getMeeting() != null ? stats.getMeeting().getTitle() : null)
                .totalParticipants(stats.getTotalParticipants())
                .attendedParticipants(stats.getAttendedParticipants())
                .confirmedParticipants(stats.getConfirmedParticipants())
                .declinedParticipants(stats.getDeclinedParticipants())
                .pendingParticipants(stats.getPendingParticipants())
                .attendanceRate(stats.getAttendanceRate())
                .confirmationRate(stats.getConfirmationRate())
                .avgResponseTimeMinutes(stats.getAvgResponseTimeMinutes())
                .averageRating(stats.getAverageRating())
                .feedbackCount(stats.getFeedbackCount())
                .status(stats.getStatus() != null ? stats.getStatus().name() : null)
                .finalized(stats.getFinalized())
                .generatedAt(stats.getGeneratedAt())
                .createdAt(stats.getCreatedAt())
                .updatedAt(stats.getUpdatedAt())
                .additionalMetrics(stats.getAdditionalMetrics())
                .build();
    }
}



package com.meethub.controller.api;

import com.meethub.domain.model.entity.MeetingStatistics;
import com.meethub.domain.model.request.MeetingIdRequest;
import com.meethub.domain.model.response.ApiResponse;
import com.meethub.domain.model.response.MeetingStatisticsResponse;
import com.meethub.domain.service.MeetingAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/meetings/statistics")
    @Operation(summary = "Generuje statystyki spotkania",
            description = "Generuje i zapisuje statystyki dla spotkania.")
    public ResponseEntity<ApiResponse<MeetingStatisticsResponse>> generateMeetingStatistics(
            @Valid @RequestBody MeetingIdRequest request) {

        MeetingStatistics stats = analyticsService.generateMeetingStatistics(request.getMeetingId());
        return ResponseEntity.ok(ApiResponse.success("Statystyki wygenerowane", convertToResponse(stats)));
    }

    @PostMapping("/meetings/report")
    @Operation(summary = "Pobiera raport spotkania",
            description = "Zwraca wygenerowany raport ze statystykami spotkania.")
    public ResponseEntity<ApiResponse<MeetingStatisticsResponse>> getMeetingReport(
            @Valid @RequestBody MeetingIdRequest request) {

        MeetingStatistics stats = analyticsService.generateMeetingStatistics(request.getMeetingId());
        return ResponseEntity.ok(ApiResponse.success("Raport spotkania wygenerowany", convertToResponse(stats)));
    }

    @PostMapping("/meetings/export/csv")
    @Operation(summary = "Eksportuje statystyki do CSV",
            description = "Eksportuje statystyki spotkania do pliku CSV.")
    public ResponseEntity<Resource> exportMeetingCsv(@Valid @RequestBody MeetingIdRequest request) {

        byte[] csv = analyticsService.exportMeetingStatisticsToCsv(request.getMeetingId());
        String filename = "statystyki_spotkania_" + request.getMeetingId() + "_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(csv.length)
                .body(new ByteArrayResource(csv));
    }

    @PostMapping("/meetings/export/pdf")
    @Operation(summary = "Eksportuje statystyki do PDF",
            description = "Eksportuje statystyki spotkania do pliku PDF.")
    public ResponseEntity<Resource> exportMeetingPdf(@Valid @RequestBody MeetingIdRequest request) {

        byte[] pdf = analyticsService.exportMeetingStatisticsToPdf(request.getMeetingId());
        String filename = "statystyki_spotkania_" + request.getMeetingId() + "_" +
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

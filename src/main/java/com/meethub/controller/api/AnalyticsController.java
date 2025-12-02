package com.meethub.controller.api;

import ch.qos.logback.core.model.Model;
import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.MeetingParticipant;
import com.meethub.domain.model.entity.MeetingStatistics;
import com.meethub.domain.model.request.ReportFilter;
import com.meethub.domain.model.response.ApiResponse;
import com.meethub.domain.model.response.OrganizerReport;
import com.meethub.domain.model.response.VotingResponse;
import com.meethub.domain.service.MeetingAnalyticsService;
import com.meethub.domain.service.MeetingService;
import com.meethub.security.CustomUserDetailsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Meeting analytics APIs")
public class AnalyticsController {

    private final MeetingAnalyticsService analyticsService;
    private final MeetingService meetingService;

    // ========== STATYSTYKI SPOTKANIA ==========

    @PostMapping("/meetings/{meetingId}/statistics")
    @Operation(summary = "Generate meeting statistics")
    public ResponseEntity<ApiResponse<com.meethub.domain.model.entity.MeetingStatistics>> generateMeetingStatistics(
            @PathVariable Long meetingId) {

        var stats = analyticsService.generateMeetingStatistics(meetingId);
        return ResponseEntity.ok(ApiResponse.success("Statistics generated", stats));
    }

    @GetMapping("/meetings/{meetingId}/export/csv")
    @Operation(summary = "Export meeting statistics as CSV")
    public ResponseEntity<Resource> exportMeetingCsv(
            @PathVariable Long meetingId) {

        byte[] csv = analyticsService.exportMeetingStatisticsToCsv(meetingId);

        String filename = "statystyki_spotkania_" + meetingId + "_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(csv.length)
                .body(new ByteArrayResource(csv));
    }



    @GetMapping("/{meetingId}/debug-stats")
    public ResponseEntity<?> debugMeetingStats(@PathVariable Long meetingId) {
        try {

            // Sprawdź czy istnieją statystyki
            var statsOpt = analyticsService.getMeetingStatistics(meetingId);

            if (statsOpt.isPresent()) {
                MeetingStatistics stats = statsOpt.get();

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("meetingId", meetingId);
                response.put("statisticsExists", true);
                response.put("statistics", Map.of(
                        "id", stats.getId(),
                        "attendanceRate", stats.getAttendanceRate(),
                        "attendedParticipants", stats.getAttendedParticipants(),
                        "totalParticipants", stats.getTotalParticipants(),
                        "generatedAt", stats.getGeneratedAt(),
                        "meetingId", stats.getMeeting().getId()
                ));

                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("meetingId", meetingId);
                response.put("statisticsExists", false);
                response.put("message", "No statistics found for this meeting");

                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Debug error: " + e.getMessage());
            errorResponse.put("meetingId", meetingId);

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/meetings/{meetingId}/export/pdf")
    @Operation(summary = "Export meeting statistics as PDF")
    public ResponseEntity<Resource> exportMeetingPdf(
            @PathVariable Long meetingId) {

        byte[] pdf = analyticsService.exportMeetingStatisticsToPdf(meetingId);

        String filename = "statystyki_spotkania_" + meetingId + "_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(new ByteArrayResource(pdf));
    }

    @GetMapping("/meetings/{meetingId}/report")
    @Operation(summary = "Get meeting report")
    public ResponseEntity<ApiResponse<com.meethub.domain.model.entity.MeetingStatistics>> getMeetingReport(
            @PathVariable Long meetingId) {

        var stats = analyticsService.generateMeetingStatistics(meetingId);
        return ResponseEntity.ok(ApiResponse.success("Meeting report generated", stats));
    }

    // ========== RAPORTY ORGANIZATORA ==========

    @GetMapping("/organizers/{organizerId}/report")
    @Operation(summary = "Get organizer report")
    public ResponseEntity<ApiResponse<OrganizerReport>> getOrganizerReport(
            @PathVariable Long organizerId,
            @ModelAttribute ReportFilter filter) {

        OrganizerReport report = analyticsService.generateOrganizerReport(organizerId, filter);
        return ResponseEntity.ok(ApiResponse.success("Report generated", report));
    }

    @GetMapping("/organizers/{organizerId}/export/csv")
    @Operation(summary = "Export organizer report as CSV")
    public ResponseEntity<Resource> exportOrganizerCsv(
            @PathVariable Long organizerId,
            @ModelAttribute ReportFilter filter) {

        byte[] csv = analyticsService.exportReportToCsv(organizerId, filter);

        String filename = "raport_organizatora_" + organizerId + "_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(csv.length)
                .body(new ByteArrayResource(csv));
    }

    @GetMapping("/organizers/{organizerId}/export/pdf")
    @Operation(summary = "Export organizer report as PDF")
    public ResponseEntity<Resource> exportOrganizerPdf(
            @PathVariable Long organizerId,
            @ModelAttribute ReportFilter filter) {

        byte[] pdf = analyticsService.exportReportToPdf(organizerId, filter);

        String filename = "raport_organizatora_" + organizerId + "_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(new ByteArrayResource(pdf));
    }

    // ========== RAPORTY WŁASNE ==========

    @GetMapping("/my-report")
    @Operation(summary = "Get my organizer report")
    public ResponseEntity<ApiResponse<OrganizerReport>> getMyOrganizerReport(
            @AuthenticationPrincipal Long userId,
            @ModelAttribute ReportFilter filter) {

        OrganizerReport report = analyticsService.generateOrganizerReport(userId, filter);
        return ResponseEntity.ok(ApiResponse.success("Report generated", report));
    }

    @GetMapping("/my-meetings/{meetingId}/report")
    @Operation(summary = "Get my meeting report")
    public ResponseEntity<ApiResponse<com.meethub.domain.model.entity.MeetingStatistics>> getMyMeetingReport(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long meetingId) {

        // Tutaj możesz dodać walidację czy użytkownik ma dostęp do tego spotkania
        var stats = analyticsService.generateMeetingStatistics(meetingId);
        return ResponseEntity.ok(ApiResponse.success("Meeting report generated", stats));
    }

    @GetMapping("/my-meetings/{meetingId}/export/csv")
    @Operation(summary = "Export my meeting statistics as CSV")
    public ResponseEntity<Resource> exportMyMeetingCsv(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long meetingId) {

        // Tutaj możesz dodać walidację czy użytkownik ma dostęp do tego spotkania
        byte[] csv = analyticsService.exportMeetingStatisticsToCsv(meetingId);

        String filename = "moje_statystyki_spotkania_" + meetingId + "_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(csv.length)
                .body(new ByteArrayResource(csv));
    }

    @GetMapping("/my-meetings/{meetingId}/export/pdf")
    @Operation(summary = "Export my meeting statistics as PDF")
    public ResponseEntity<Resource> exportMyMeetingPdf(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long meetingId) {

        // Tutaj możesz dodać walidację czy użytkownik ma dostęp do tego spotkania
        byte[] pdf = analyticsService.exportMeetingStatisticsToPdf(meetingId);

        String filename = "moje_statystyki_spotkania_" + meetingId + "_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(new ByteArrayResource(pdf));
    }


}
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

@Validated // Dodanie walidacji na poziomie kontrolera
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

    // ========== RAPORTY ORGANIZATORA ==========

    @GetMapping("/organizers/{organizerId}/report")
    @Operation(summary = "Pobiera raport organizatora",
            description = "Zwraca raport ze statystykami wszystkich spotkań organizatora.")
    public ResponseEntity<ApiResponse<OrganizerReport>> getOrganizerReport(
            @PathVariable @NotNull(message = "Identyfikator organizatora nie może być pusty")
            @Min(value = 1, message = "Identyfikator organizatora musi być liczbą dodatnią")
            Long organizerId,
            @ModelAttribute @Valid ReportFilter filter) {

        OrganizerReport report = analyticsService.generateOrganizerReport(organizerId, filter);
        return ResponseEntity.ok(ApiResponse.success("Raport organizatora wygenerowany", report));
    }

    @GetMapping("/organizers/{organizerId}/export/csv")
    @Operation(summary = "Eksportuje raport organizatora do CSV",
            description = "Eksportuje raport organizatora do pliku CSV.")
    public ResponseEntity<Resource> exportOrganizerCsv(
            @PathVariable @NotNull(message = "Identyfikator organizatora nie może być pusty")
            @Min(value = 1, message = "Identyfikator organizatora musi być liczbą dodatnią")
            Long organizerId,
            @ModelAttribute @Valid ReportFilter filter) {

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
    @Operation(summary = "Eksportuje raport organizatora do PDF",
            description = "Eksportuje raport organizatora do pliku PDF.")
    public ResponseEntity<Resource> exportOrganizerPdf(
            @PathVariable @NotNull(message = "Identyfikator organizatora nie może być pusty")
            @Min(value = 1, message = "Identyfikator organizatora musi być liczbą dodatnią")
            Long organizerId,
            @ModelAttribute @Valid ReportFilter filter) {

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
    @Operation(summary = "Pobiera własny raport organizatora",
            description = "Zwraca raport dla zalogowanego użytkownika jako organizatora.")
    public ResponseEntity<ApiResponse<OrganizerReport>> getMyOrganizerReport(
            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            Long userId,
            @ModelAttribute @Valid ReportFilter filter) {

        OrganizerReport report = analyticsService.generateOrganizerReport(userId, filter);
        return ResponseEntity.ok(ApiResponse.success("Raport wygenerowany", report));
    }

    @GetMapping("/my-meetings/{meetingId}/report")
    @Operation(summary = "Pobiera raport własnego spotkania",
            description = "Zwraca raport spotkania, którego użytkownik jest organizatorem.")
    public ResponseEntity<ApiResponse<MeetingStatisticsResponse>> getMyMeetingReport(
            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            Long userId,
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId) {

        var stats = analyticsService.generateMeetingStatistics(meetingId);
        var response = convertToResponse(stats);
        return ResponseEntity.ok(ApiResponse.success("Raport spotkania wygenerowany", response));
    }

    @GetMapping("/my-meetings/{meetingId}/export/csv")
    @Operation(summary = "Eksportuje statystyki własnego spotkania do CSV",
            description = "Eksportuje statystyki spotkania użytkownika do CSV.")
    public ResponseEntity<Resource> exportMyMeetingCsv(
            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            Long userId,
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId) {

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
    @Operation(summary = "Eksportuje statystyki własnego spotkania do PDF",
            description = "Eksportuje statystyki spotkania użytkownika do PDF.")
    public ResponseEntity<Resource> exportMyMeetingPdf(
            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            Long userId,
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId) {

        byte[] pdf = analyticsService.exportMeetingStatisticsToPdf(meetingId);
        String filename = "moje_statystyki_spotkania_" + meetingId + "_" +
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





//package com.meethub.controller.api;
//
//import com.meethub.domain.model.entity.MeetingStatistics;
//import com.meethub.domain.model.request.ReportFilter;
//import com.meethub.domain.model.response.ApiResponse;
//import com.meethub.domain.model.response.MeetingStatisticsResponse;
//import com.meethub.domain.model.response.OrganizerReport;
//import com.meethub.domain.service.MeetingAnalyticsService;
//import com.meethub.domain.service.MeetingService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.RequiredArgsConstructor;
//import org.springframework.core.io.ByteArrayResource;
//import org.springframework.core.io.Resource;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.web.bind.annotation.*;
//
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Optional;
//
//@RestController
//@RequestMapping("/api/v1/analytics")
//@RequiredArgsConstructor
//@Tag(name = "Analytics", description = "API do analityki spotkań")
//public class AnalyticsController {
//
//    private final MeetingAnalyticsService analyticsService;
//
//    // ========== STATYSTYKI SPOTKANIA ==========
//
//    @PostMapping("/meetings/{meetingId}/statistics")
//    @Operation(summary = "Generuje statystyki spotkania",
//            description = "Generuje i zapisuje statystyki dla spotkania.")
//    public ResponseEntity<ApiResponse<MeetingStatisticsResponse>> generateMeetingStatistics(
//            @PathVariable Long meetingId) {
//
//        var stats = analyticsService.generateMeetingStatistics(meetingId);
//        var response = convertToResponse(stats);
//        return ResponseEntity.ok(ApiResponse.success("Statystyki wygenerowane", response));
//    }
//
//    @GetMapping("/meetings/{meetingId}/report")
//    @Operation(summary = "Pobiera raport spotkania",
//            description = "Zwraca wygenerowany raport ze statystykami spotkania.")
//    public ResponseEntity<ApiResponse<MeetingStatisticsResponse>> getMeetingReport(
//            @PathVariable Long meetingId) {
//
//        var stats = analyticsService.generateMeetingStatistics(meetingId);
//        var response = convertToResponse(stats);
//        return ResponseEntity.ok(ApiResponse.success("Raport spotkania wygenerowany", response));
//    }
//
//    @GetMapping("/meetings/{meetingId}/export/csv")
//    @Operation(summary = "Eksportuje statystyki do CSV",
//            description = "Eksportuje statystyki spotkania do pliku CSV.")
//    public ResponseEntity<Resource> exportMeetingCsv(
//            @PathVariable Long meetingId) {
//
//        byte[] csv = analyticsService.exportMeetingStatisticsToCsv(meetingId);
//        String filename = "statystyki_spotkania_" + meetingId + "_" +
//                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
//
//        return ResponseEntity.ok()
//                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
//                .contentType(MediaType.parseMediaType("text/csv"))
//                .contentLength(csv.length)
//                .body(new ByteArrayResource(csv));
//    }
//
//    @GetMapping("/meetings/{meetingId}/export/pdf")
//    @Operation(summary = "Eksportuje statystyki do PDF",
//            description = "Eksportuje statystyki spotkania do pliku PDF.")
//    public ResponseEntity<Resource> exportMeetingPdf(
//            @PathVariable Long meetingId) {
//
//        byte[] pdf = analyticsService.exportMeetingStatisticsToPdf(meetingId);
//        String filename = "statystyki_spotkania_" + meetingId + "_" +
//                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
//
//        return ResponseEntity.ok()
//                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
//                .contentType(MediaType.APPLICATION_PDF)
//                .contentLength(pdf.length)
//                .body(new ByteArrayResource(pdf));
//    }
//
//    // ========== RAPORTY ORGANIZATORA ==========
//
//    @GetMapping("/organizers/{organizerId}/report")
//    @Operation(summary = "Pobiera raport organizatora",
//            description = "Zwraca raport ze statystykami wszystkich spotkań organizatora.")
//    public ResponseEntity<ApiResponse<OrganizerReport>> getOrganizerReport(
//            @PathVariable Long organizerId,
//            @ModelAttribute ReportFilter filter) {
//
//        OrganizerReport report = analyticsService.generateOrganizerReport(organizerId, filter);
//        return ResponseEntity.ok(ApiResponse.success("Raport organizatora wygenerowany", report));
//    }
//
//    @GetMapping("/organizers/{organizerId}/export/csv")
//    @Operation(summary = "Eksportuje raport organizatora do CSV",
//            description = "Eksportuje raport organizatora do pliku CSV.")
//    public ResponseEntity<Resource> exportOrganizerCsv(
//            @PathVariable Long organizerId,
//            @ModelAttribute ReportFilter filter) {
//
//        byte[] csv = analyticsService.exportReportToCsv(organizerId, filter);
//        String filename = "raport_organizatora_" + organizerId + "_" +
//                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
//
//        return ResponseEntity.ok()
//                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
//                .contentType(MediaType.parseMediaType("text/csv"))
//                .contentLength(csv.length)
//                .body(new ByteArrayResource(csv));
//    }
//
//    @GetMapping("/organizers/{organizerId}/export/pdf")
//    @Operation(summary = "Eksportuje raport organizatora do PDF",
//            description = "Eksportuje raport organizatora do pliku PDF.")
//    public ResponseEntity<Resource> exportOrganizerPdf(
//            @PathVariable Long organizerId,
//            @ModelAttribute ReportFilter filter) {
//
//        byte[] pdf = analyticsService.exportReportToPdf(organizerId, filter);
//        String filename = "raport_organizatora_" + organizerId + "_" +
//                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
//
//        return ResponseEntity.ok()
//                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
//                .contentType(MediaType.APPLICATION_PDF)
//                .contentLength(pdf.length)
//                .body(new ByteArrayResource(pdf));
//    }
//
//    // ========== RAPORTY WŁASNE ==========
//
//    @GetMapping("/my-report")
//    @Operation(summary = "Pobiera własny raport organizatora",
//            description = "Zwraca raport dla zalogowanego użytkownika jako organizatora.")
//    public ResponseEntity<ApiResponse<OrganizerReport>> getMyOrganizerReport(
//            @AuthenticationPrincipal Long userId,
//            @ModelAttribute ReportFilter filter) {
//
//        OrganizerReport report = analyticsService.generateOrganizerReport(userId, filter);
//        return ResponseEntity.ok(ApiResponse.success("Raport wygenerowany", report));
//    }
//
//    @GetMapping("/my-meetings/{meetingId}/report")
//    @Operation(summary = "Pobiera raport własnego spotkania",
//            description = "Zwraca raport spotkania, którego użytkownik jest organizatorem.")
//    public ResponseEntity<ApiResponse<MeetingStatisticsResponse>> getMyMeetingReport(
//            @AuthenticationPrincipal Long userId,
//            @PathVariable Long meetingId) {
//
//        var stats = analyticsService.generateMeetingStatistics(meetingId);
//        var response = convertToResponse(stats);
//        return ResponseEntity.ok(ApiResponse.success("Raport spotkania wygenerowany", response));
//    }
//
//    @GetMapping("/my-meetings/{meetingId}/export/csv")
//    @Operation(summary = "Eksportuje statystyki własnego spotkania do CSV",
//            description = "Eksportuje statystyki spotkania użytkownika do CSV.")
//    public ResponseEntity<Resource> exportMyMeetingCsv(
//            @AuthenticationPrincipal Long userId,
//            @PathVariable Long meetingId) {
//
//        byte[] csv = analyticsService.exportMeetingStatisticsToCsv(meetingId);
//        String filename = "moje_statystyki_spotkania_" + meetingId + "_" +
//                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
//
//        return ResponseEntity.ok()
//                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
//                .contentType(MediaType.parseMediaType("text/csv"))
//                .contentLength(csv.length)
//                .body(new ByteArrayResource(csv));
//    }
//
//    @GetMapping("/my-meetings/{meetingId}/export/pdf")
//    @Operation(summary = "Eksportuje statystyki własnego spotkania do PDF",
//            description = "Eksportuje statystyki spotkania użytkownika do PDF.")
//    public ResponseEntity<Resource> exportMyMeetingPdf(
//            @AuthenticationPrincipal Long userId,
//            @PathVariable Long meetingId) {
//
//        byte[] pdf = analyticsService.exportMeetingStatisticsToPdf(meetingId);
//        String filename = "moje_statystyki_spotkania_" + meetingId + "_" +
//                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
//
//        return ResponseEntity.ok()
//                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
//                .contentType(MediaType.APPLICATION_PDF)
//                .contentLength(pdf.length)
//                .body(new ByteArrayResource(pdf));
//    }
//
//    private MeetingStatisticsResponse convertToResponse(MeetingStatistics stats) {
//        if (stats == null) return null;
//
//        return MeetingStatisticsResponse.builder()
//                .id(stats.getId())
//                .meetingId(stats.getMeeting() != null ? stats.getMeeting().getId() : null)
//                .meetingTitle(stats.getMeeting() != null ? stats.getMeeting().getTitle() : null)
//                .totalParticipants(stats.getTotalParticipants())
//                .attendedParticipants(stats.getAttendedParticipants())
//                .confirmedParticipants(stats.getConfirmedParticipants())
//                .declinedParticipants(stats.getDeclinedParticipants())
//                .pendingParticipants(stats.getPendingParticipants())
//                .attendanceRate(stats.getAttendanceRate())
//                .confirmationRate(stats.getConfirmationRate())
//                .avgResponseTimeMinutes(stats.getAvgResponseTimeMinutes())
//                .averageRating(stats.getAverageRating())
//                .feedbackCount(stats.getFeedbackCount())
//                .status(stats.getStatus() != null ? stats.getStatus().name() : null)
//                .finalized(stats.getFinalized())
//                .generatedAt(stats.getGeneratedAt())
//                .createdAt(stats.getCreatedAt())
//                .updatedAt(stats.getUpdatedAt())
//                .additionalMetrics(stats.getAdditionalMetrics())
//                .build();
//    }
//}

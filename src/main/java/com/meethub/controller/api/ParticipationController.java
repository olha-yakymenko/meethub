package com.meethub.controller.api;

import com.meethub.domain.model.entity.MeetingParticipant;
import com.meethub.domain.model.response.ApiResponse;
import com.meethub.domain.service.ParticipationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/participations")
@RequiredArgsConstructor
@Tag(name = "Participations", description = "Meeting participation management APIs")
public class ParticipationController {

    private final ParticipationService participationService;

    @PostMapping("/meetings/{meetingId}/confirm")
    @Operation(summary = "Confirm participation in a meeting")
    public ResponseEntity<ApiResponse<MeetingParticipant>> confirmParticipation(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal Long userId) {

        MeetingParticipant participant = participationService.confirmParticipation(meetingId, userId);
        return ResponseEntity.ok(ApiResponse.success("Participation confirmed", participant));
    }

    @PostMapping("/meetings/{meetingId}/decline")
    @Operation(summary = "Decline participation in a meeting")
    public ResponseEntity<ApiResponse<MeetingParticipant>> declineParticipation(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal Long userId) {

        MeetingParticipant participant = participationService.declineParticipation(meetingId, userId);
        return ResponseEntity.ok(ApiResponse.success("Participation declined", participant));
    }

    @GetMapping("/meetings/{meetingId}/response-stats")
    @Operation(summary = "Get response statistics for a meeting")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getResponseStatistics(
            @PathVariable Long meetingId) {

        Map<com.meethub.domain.model.enums.ParticipationStatus, Long> stats =
                participationService.getResponseStatistics(meetingId);

        // Konwertuj enum na string dla lepszej odpowiedzi JSON
        Map<String, Long> responseStats = stats.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().name(),
                        Map.Entry::getValue
                ));

        return ResponseEntity.ok(ApiResponse.success("Response statistics retrieved", responseStats));
    }

    @GetMapping("/meetings/{meetingId}/avg-response-time")
    @Operation(summary = "Get average response time")
    public ResponseEntity<ApiResponse<Double>> getAverageResponseTime(
            @PathVariable Long meetingId) {

        Double avgTime = participationService.getAverageResponseTime(meetingId);
        return ResponseEntity.ok(ApiResponse.success("Average response time retrieved", avgTime));
    }
}
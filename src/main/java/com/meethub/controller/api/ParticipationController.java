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
    @Operation(
            summary = "Potwierdza udział w spotkaniu",
            description = "Potwierdza udział użytkownika w spotkaniu."
    )
    public ResponseEntity<ApiResponse<MeetingParticipant>> confirmParticipation(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal Long userId) {

        MeetingParticipant participant = participationService.confirmParticipation(meetingId, userId);
        return ResponseEntity.ok(ApiResponse.success("Participation confirmed", participant));
    }

    @PostMapping("/meetings/{meetingId}/decline")
    @Operation(
            summary = "Odrzuca udział w spotkaniu",
            description = "Odrzuca udział użytkownika w spotkaniu."
    )
    public ResponseEntity<ApiResponse<MeetingParticipant>> declineParticipation(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal Long userId) {

        MeetingParticipant participant = participationService.declineParticipation(meetingId, userId);
        return ResponseEntity.ok(ApiResponse.success("Participation declined", participant));
    }

    @GetMapping("/meetings/{meetingId}/response-stats")
    @Operation(
            summary = "Pobiera statystyki odpowiedzi",
            description = "Zwraca statystyki odpowiedzi na zaproszenia do spotkania."
    )
    public ResponseEntity<ApiResponse<Map<String, Long>>> getResponseStatistics(
            @PathVariable Long meetingId) {

        Map<com.meethub.domain.model.enums.ParticipationStatus, Long> stats =
                participationService.getResponseStatistics(meetingId);

        Map<String, Long> responseStats = stats.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().name(),
                        Map.Entry::getValue
                ));

        return ResponseEntity.ok(ApiResponse.success("Response statistics retrieved", responseStats));
    }

    @GetMapping("/meetings/{meetingId}/avg-response-time")
    @Operation(
            summary = "Pobiera średni czas odpowiedzi",
            description = "Zwraca średni czas odpowiedzi na zaproszenia do spotkania."
    )
    public ResponseEntity<ApiResponse<Double>> getAverageResponseTime(
            @PathVariable Long meetingId) {

        Double avgTime = participationService.getAverageResponseTime(meetingId);
        return ResponseEntity.ok(ApiResponse.success("Average response time retrieved", avgTime));
    }
}

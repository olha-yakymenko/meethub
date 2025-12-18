package com.meethub.controller.api;

import com.meethub.domain.model.entity.MeetingParticipant;
import com.meethub.domain.model.response.ApiResponse;
import com.meethub.domain.service.AttendanceTokenService;
import com.meethub.domain.service.ParticipationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Validated
@Slf4j
@RestController
@RequestMapping("/api/v1/participations")
@RequiredArgsConstructor
@Tag(name = "Participations", description = "API do zarządzania uczestnictwem w spotkaniach")
public class ParticipationController {

    private final ParticipationService participationService;

    private final AttendanceTokenService tokenService;

    @PostMapping("/meetings/{meetingId}/confirm")
    @Operation(
            summary = "Potwierdza udział w spotkaniu",
            description = "Potwierdza udział użytkownika w spotkaniu."
    )
    public ResponseEntity<ApiResponse<MeetingParticipant>> confirmParticipation(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            @Min(value = 1, message = "Identyfikator użytkownika musi być liczbą dodatnią")
            Long userId) {

        log.info("Potwierdzanie udziału użytkownika {} w spotkaniu {}", userId, meetingId);
        MeetingParticipant participant = participationService.confirmParticipation(meetingId, userId);
        log.info("Użytkownik {} potwierdził udział w spotkaniu {}", userId, meetingId);
        return ResponseEntity.ok(ApiResponse.success("Udział potwierdzony pomyślnie", participant));
    }

    @PostMapping("/meetings/{meetingId}/decline")
    @Operation(
            summary = "Odrzuca udział w spotkaniu",
            description = "Odrzuca udział użytkownika w spotkaniu."
    )
    public ResponseEntity<ApiResponse<MeetingParticipant>> declineParticipation(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            @Min(value = 1, message = "Identyfikator użytkownika musi być liczbą dodatnią")
            Long userId) {

        log.info("Odrzucanie udziału użytkownika {} w spotkaniu {}", userId, meetingId);
        MeetingParticipant participant = participationService.declineParticipation(meetingId, userId);
        log.info("Użytkownik {} odrzucił udział w spotkaniu {}", userId, meetingId);
        return ResponseEntity.ok(ApiResponse.success("Udział odrzucony pomyślnie", participant));
    }

    @GetMapping("/meetings/{meetingId}/response-stats")
    @Operation(
            summary = "Pobiera statystyki odpowiedzi",
            description = "Zwraca statystyki odpowiedzi na zaproszenia do spotkania."
    )
    public ResponseEntity<ApiResponse<Map<String, Long>>> getResponseStatistics(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId) {

        log.info("Pobieranie statystyk odpowiedzi dla spotkania {}", meetingId);
        Map<com.meethub.domain.model.enums.ParticipationStatus, Long> stats =
                participationService.getResponseStatistics(meetingId);

        Map<String, Long> responseStats = stats.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().name(),
                        Map.Entry::getValue
                ));

        log.info("Pobrano statystyki odpowiedzi dla spotkania {}: {} wpisów", meetingId, responseStats.size());
        return ResponseEntity.ok(ApiResponse.success("Statystyki odpowiedzi pobrane pomyślnie", responseStats));
    }

    @GetMapping("/meetings/{meetingId}/avg-response-time")
    @Operation(
            summary = "Pobiera średni czas odpowiedzi",
            description = "Zwraca średni czas odpowiedzi na zaproszenia do spotkania."
    )
    public ResponseEntity<ApiResponse<Double>> getAverageResponseTime(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId) {

        log.info("Pobieranie średniego czasu odpowiedzi dla spotkania {}", meetingId);
        Double avgTime = participationService.getAverageResponseTime(meetingId);
        log.info("Średni czas odpowiedzi dla spotkania {}: {} minut", meetingId, avgTime);
        return ResponseEntity.ok(ApiResponse.success("Średni czas odpowiedzi pobrany pomyślnie", avgTime));
    }
}




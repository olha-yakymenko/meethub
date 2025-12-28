package com.meethub.controller.api;

import com.meethub.domain.model.request.*;
import com.meethub.domain.model.response.ApiResponse;
import com.meethub.domain.model.response.MeetingResponse;
import com.meethub.domain.service.MeetingService;
import com.meethub.domain.model.enums.MeetingStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/meetings")
@RequiredArgsConstructor
@Tag(name = "Meetings", description = "API do zarządzania spotkaniami")
public class MeetingController {

    private final MeetingService meetingService;

    @PostMapping
    @Operation(summary = "Tworzy nowe spotkanie",
            description = "Tworzy nowe spotkanie z podanymi danymi. Wymaga autoryzacji.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Spotkanie utworzone pomyślnie"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Nieprawidłowe dane wejściowe"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Użytkownik nieautoryzowany")
    })
    public ResponseEntity<ApiResponse<MeetingResponse>> createMeeting(
            @RequestBody @Valid CreateMeetingRequest request,
            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            @Min(value = 1, message = "Identyfikator użytkownika musi być liczbą dodatnią")
            Long userId) {

        MeetingResponse meeting = meetingService.createMeeting(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Spotkanie utworzone pomyślnie", meeting));
    }

    @PutMapping("/{meetingId}")
    @Operation(summary = "Aktualizuje istniejące spotkanie",
            description = "Aktualizuje spotkanie o podanym ID. Tylko organizator może edytować.")
    public ResponseEntity<ApiResponse<MeetingResponse>> updateMeeting(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @RequestBody @Valid UpdateMeetingRequest request,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            @Min(value = 1, message = "Identyfikator użytkownika musi być liczbą dodatnią")
            Long userId) {

        MeetingResponse meeting = meetingService.updateMeeting(meetingId, request, userId);
        return ResponseEntity.ok(ApiResponse.success("Spotkanie zaktualizowane pomyślnie", meeting));
    }

    @DeleteMapping("/{meetingId}")
    @Operation(summary = "Usuwa spotkanie",
            description = "Usuwa spotkanie o podanym ID. Tylko organizator może usunąć.")
    public ResponseEntity<ApiResponse<Void>> deleteMeeting(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            @Min(value = 1, message = "Identyfikator użytkownika musi być liczbą dodatnią")
            Long userId) {

        meetingService.deleteMeeting(meetingId, userId);
        return ResponseEntity.ok(ApiResponse.success("Spotkanie usunięte pomyślnie", null));
    }

    @GetMapping("/{meetingId}")
    @Operation(summary = "Pobiera szczegóły spotkania",
            description = "Zwraca szczegóły spotkania o podanym ID.")
    public ResponseEntity<ApiResponse<MeetingResponse>> getMeeting(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId) {

        MeetingResponse meeting = meetingService.getMeetingById(meetingId);
        return ResponseEntity.ok(ApiResponse.success("Szczegóły spotkania pobrane pomyślnie", meeting));
    }

    @GetMapping("/my-meetings")
    @Operation(summary = "Pobiera spotkania użytkownika",
            description = "Zwraca paginowaną listę spotkań zalogowanego użytkownika.")
    public ResponseEntity<ApiResponse<Page<MeetingResponse>>> getUserMeetings(
            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            @Min(value = 1, message = "Identyfikator użytkownika musi być liczbą dodatnią")
            Long userId,

            @Valid Pageable pageable) {

        Page<MeetingResponse> meetings = meetingService.getUserMeetings(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Spotkania użytkownika pobrane pomyślnie", meetings));
    }

    @GetMapping("/public/upcoming")
    @Operation(summary = "Pobiera nadchodzące publiczne spotkania",
            description = "Zwraca listę nadchodzących spotkań publicznych.")
    public ResponseEntity<ApiResponse<List<MeetingResponse>>> getUpcomingPublicMeetings() {
        List<MeetingResponse> meetings = meetingService.getUpcomingPublicMeetings();
        return ResponseEntity.ok(ApiResponse.success("Nadchodzące spotkania publiczne pobrane pomyślnie", meetings));
    }

    @PostMapping("/nearby")
    @Operation(summary = "Znajduje spotkania w pobliżu",
            description = "Zwraca spotkania w określonym promieniu od podanych współrzędnych.")
    public ResponseEntity<ApiResponse<List<MeetingResponse>>> findNearbyMeetings(
            @Valid @RequestBody NearbyMeetingsRequest request) {

        List<MeetingResponse> meetings = meetingService.findNearbyMeetings(
                request.getLatitude(),
                request.getLongitude(),
                request.getRadius()
        );

        return ResponseEntity.ok(ApiResponse.success("Spotkania w pobliżu znalezione pomyślnie", meetings));
    }


    @PatchMapping("/{meetingId}/status")
    public ResponseEntity<ApiResponse<Void>> changeMeetingStatus(
            @PathVariable Long meetingId,
            @Valid @RequestBody ChangeMeetingStatusRequest request,
            @AuthenticationPrincipal Long userId) {

        meetingService.changeMeetingStatus(meetingId, request.getStatus(), userId);
        return ResponseEntity.ok(ApiResponse.success("Status spotkania zmieniony pomyślnie", null));
    }

    @PostMapping("/{meetingId}/duplicate")
    @Operation(summary = "Duplikuje spotkanie",
            description = "Tworzy kopię istniejącego spotkania.")
    public ResponseEntity<ApiResponse<MeetingResponse>> duplicateMeeting(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            @Min(value = 1, message = "Identyfikator użytkownika musi być liczbą dodatnią")
            Long userId) {

        MeetingResponse duplicate = meetingService.duplicateMeeting(meetingId, userId);
        return ResponseEntity.ok(ApiResponse.success("Spotanie zduplikowane pomyślnie", duplicate));
    }

    @GetMapping("/conflicts")
    public ResponseEntity<ApiResponse<List<MeetingResponse>>> findConflictingMeetings(
            @AuthenticationPrincipal Long userId,
            @Valid ConflictingMeetingsRequest request) {

        List<MeetingResponse> conflicts = meetingService.findConflictingMeetings(
                userId, request.getStartDate(), request.getEndDate());
        return ResponseEntity.ok(ApiResponse.success("Konflikty terminów sprawdzone pomyślnie", conflicts));
    }


    @GetMapping("/templates/my")
    @Operation(summary = "Pobiera szablony użytkownika",
            description = "Zwraca listę szablonów spotkań utworzonych przez użytkownika.")
    public ResponseEntity<ApiResponse<List<MeetingResponse>>> getMyTemplates(
            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            @Min(value = 1, message = "Identyfikator użytkownika musi być liczbą dodatnią")
            Long userId) {

        List<MeetingResponse> templates = meetingService.getMeetingTemplates(userId);
        return ResponseEntity.ok(ApiResponse.success("Szablony użytkownika pobrane pomyślnie", templates));
    }

    @PostMapping("/templates/{templateId}/create")
    public ResponseEntity<ApiResponse<MeetingResponse>> createFromTemplate(
            @PathVariable Long templateId,
            @Valid @RequestBody CreateFromTemplateRequest request,
            @AuthenticationPrincipal Long userId) {

        MeetingResponse meeting = meetingService.createFromTemplate(
                templateId, userId, request.getNewStartDate());
        return ResponseEntity.ok(ApiResponse.success("Spotkanie utworzone z szablonu pomyślnie", meeting));
    }

    @GetMapping("/search")
    @Operation(summary = "Wyszukuje spotkania",
            description = "Wyszukuje spotkania według różnych kryteriów.")
    public ResponseEntity<ApiResponse<Page<MeetingResponse>>> searchMeetings(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,

            @Valid Pageable pageable) {

        Page<MeetingResponse> meetings = meetingService.getFilteredMeetings(query, type, status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Wyniki wyszukiwania pobrane pomyślnie", meetings));
    }
}

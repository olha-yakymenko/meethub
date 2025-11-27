package com.meethub.controller.api;

import ch.qos.logback.core.model.Model;
import com.meethub.domain.model.request.CreateMeetingRequest;
import com.meethub.domain.model.request.UpdateMeetingRequest;
import com.meethub.domain.model.response.ApiResponse;
import com.meethub.domain.model.response.MeetingResponse;
import com.meethub.domain.service.MeetingService;
import com.meethub.domain.model.enums.MeetingStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/meetings")
@RequiredArgsConstructor
@Tag(name = "Meetings", description = "Meeting management APIs")
public class MeetingController {

    private final MeetingService meetingService;

    @PostMapping
    @Operation(summary = "Create a new meeting")
    public ResponseEntity<ApiResponse<MeetingResponse>> createMeeting(
            @Valid @RequestBody CreateMeetingRequest request,
            @AuthenticationPrincipal Long userId) {

        MeetingResponse meeting = meetingService.createMeeting(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Meeting created successfully", meeting));
    }

    @PutMapping("/{meetingId}")
    @Operation(summary = "Update an existing meeting")
    public ResponseEntity<ApiResponse<MeetingResponse>> updateMeeting(
            @PathVariable Long meetingId,
            @Valid @RequestBody UpdateMeetingRequest request,
            @AuthenticationPrincipal Long userId) {

        MeetingResponse meeting = meetingService.updateMeeting(meetingId, request, userId);
        return ResponseEntity.ok(ApiResponse.success("Meeting updated successfully", meeting));
    }

    @DeleteMapping("/{meetingId}")
    @Operation(summary = "Delete a meeting")
    public ResponseEntity<ApiResponse<Void>> deleteMeeting(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal Long userId) {

        meetingService.deleteMeeting(meetingId, userId);
        return ResponseEntity.ok(ApiResponse.success("Meeting deleted successfully", null));
    }

    @GetMapping("/{meetingId}")
    @Operation(summary = "Get meeting by ID")
    public ResponseEntity<ApiResponse<MeetingResponse>> getMeeting(@PathVariable Long meetingId) {
        MeetingResponse meeting = meetingService.getMeetingById(meetingId);
        return ResponseEntity.ok(ApiResponse.success("Meeting retrieved successfully", meeting));
    }




    @GetMapping("/my-meetings")
    @Operation(summary = "Get user's meetings with pagination")
    public ResponseEntity<ApiResponse<Page<MeetingResponse>>> getUserMeetings(
            @AuthenticationPrincipal Long userId,
            Pageable pageable) {

        Page<MeetingResponse> meetings = meetingService.getUserMeetings(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Meetings retrieved successfully", meetings));
    }

    @GetMapping("/public/upcoming")
    @Operation(summary = "Get upcoming public meetings")
    public ResponseEntity<ApiResponse<List<MeetingResponse>>> getUpcomingPublicMeetings() {
        List<MeetingResponse> meetings = meetingService.getUpcomingPublicMeetings();
        return ResponseEntity.ok(ApiResponse.success("Public meetings retrieved successfully", meetings));
    }

    @GetMapping("/nearby")
    @Operation(summary = "Find nearby meetings")
    public ResponseEntity<ApiResponse<List<MeetingResponse>>> findNearbyMeetings(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "5000") double radius) {

        List<MeetingResponse> meetings = meetingService.findNearbyMeetings(latitude, longitude, radius);
        return ResponseEntity.ok(ApiResponse.success("Nearby meetings retrieved successfully", meetings));
    }

    @PatchMapping("/{meetingId}/status")
    @Operation(summary = "Change meeting status")
    public ResponseEntity<ApiResponse<Void>> changeMeetingStatus(
            @PathVariable Long meetingId,
            @RequestParam MeetingStatus status,
            @AuthenticationPrincipal Long userId) {

        meetingService.changeMeetingStatus(meetingId, status, userId);
        return ResponseEntity.ok(ApiResponse.success("Meeting status updated successfully", null));
    }

    @PostMapping("/{meetingId}/duplicate")
    @Operation(summary = "Duplicate a meeting")
    public ResponseEntity<ApiResponse<MeetingResponse>> duplicateMeeting(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal Long userId) {

        MeetingResponse duplicate = meetingService.duplicateMeeting(meetingId, userId);
        return ResponseEntity.ok(ApiResponse.success("Meeting duplicated successfully", duplicate));
    }

    @GetMapping("/conflicts")
    @Operation(summary = "Check for conflicting meetings")
    public ResponseEntity<ApiResponse<List<MeetingResponse>>> findConflictingMeetings(
            @AuthenticationPrincipal Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        List<MeetingResponse> conflicts = meetingService.findConflictingMeetings(userId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Conflicts checked successfully", conflicts));
    }
}
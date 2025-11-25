package com.meethub.controller.api;

import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.model.enums.PermissionLevel;
import com.meethub.domain.model.request.InviteParticipantsRequest;
import com.meethub.domain.model.response.ApiResponse;
import com.meethub.domain.model.response.ParticipantResponse;
import com.meethub.domain.model.response.UserResponse;
import com.meethub.domain.service.MeetingParticipantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/meetings/{meetingId}/participants")
@RequiredArgsConstructor
@Tag(name = "Meeting Participants", description = "Meeting participant management APIs")
public class MeetingParticipantController {

    private final MeetingParticipantService participantService;

    @GetMapping
    @Operation(summary = "Get meeting participants")
    public ResponseEntity<ApiResponse<List<ParticipantResponse>>> getParticipants(@PathVariable Long meetingId) {
        List<ParticipantResponse> participants = participantService.getMeetingParticipants(meetingId);
        return ResponseEntity.ok(ApiResponse.success("Participants retrieved successfully", participants));
    }

    @PostMapping("/invite")
    @Operation(summary = "Invite participants to meeting")
    public ResponseEntity<ApiResponse<Void>> inviteParticipants(
            @PathVariable Long meetingId,
            @Valid @RequestBody InviteParticipantsRequest request,
            @AuthenticationPrincipal Long userId) {
        participantService.inviteMultipleParticipants(meetingId, request, userId);
        return ResponseEntity.ok(ApiResponse.success("Participants invited successfully", null));
    }

    @PostMapping("/join")
    @Operation(summary = "Join a public meeting")
    public ResponseEntity<ApiResponse<Void>> joinMeeting(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal Long userId) {
        participantService.joinPublicMeeting(meetingId, userId);
        return ResponseEntity.ok(ApiResponse.success("Joined meeting successfully", null));
    }

    @PatchMapping("/{participantId}/status")
    @Operation(summary = "Update participant status")
    public ResponseEntity<ApiResponse<Void>> updateParticipantStatus(
            @PathVariable Long meetingId,
            @PathVariable Long participantId,
            @RequestParam ParticipationStatus status,
            @RequestParam(required = false) String comment,
            @AuthenticationPrincipal Long userId) {
        participantService.updateParticipantStatus(meetingId, participantId, status, comment, userId);
        return ResponseEntity.ok(ApiResponse.success("Participant status updated successfully", null));
    }

    @PatchMapping("/{participantId}/permission")
    @Operation(summary = "Update participant permission level")
    public ResponseEntity<ApiResponse<Void>> updateParticipantPermission(
            @PathVariable Long meetingId,
            @PathVariable Long participantId,
            @RequestParam PermissionLevel permissionLevel,
            @AuthenticationPrincipal Long userId) {
        participantService.updateParticipantPermission(meetingId, participantId, permissionLevel, userId);
        return ResponseEntity.ok(ApiResponse.success("Participant permission updated successfully", null));
    }

    @DeleteMapping("/{participantId}")
    @Operation(summary = "Remove participant from meeting")
    public ResponseEntity<ApiResponse<Void>> removeParticipant(
            @PathVariable Long meetingId,
            @PathVariable Long participantId,
            @AuthenticationPrincipal Long userId) {
        participantService.removeParticipant(meetingId, participantId, userId);
        return ResponseEntity.ok(ApiResponse.success("Participant removed successfully", null));
    }

    @PostMapping("/invitations/{token}/accept")
    @Operation(summary = "Accept invitation by token")
    public ResponseEntity<ApiResponse<Void>> acceptInvitationByToken(@PathVariable String token) {
        participantService.acceptInvitationByToken(token);
        return ResponseEntity.ok(ApiResponse.success("Invitation accepted successfully", null));
    }

    @GetMapping("/search-users")
    @Operation(summary = "Search users for invitation")
    public ResponseEntity<ApiResponse<List<UserResponse>>> searchUsers(
            @RequestParam String query,
            @PathVariable Long meetingId) {
        List<UserResponse> users = participantService.searchUsersForInvitation(query, meetingId);
        return ResponseEntity.ok(ApiResponse.success("Users found", users));
    }
}
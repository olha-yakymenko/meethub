package com.meethub.controller.api;

import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.model.enums.PermissionLevel;
import com.meethub.domain.model.projection.ParticipantProjection;
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
    @Operation(
            summary = "Pobiera uczestników spotkania",
            description = "Zwraca listę wszystkich uczestników spotkania."
    )
    public ResponseEntity<ApiResponse<List<ParticipantProjection>>> getParticipants(@PathVariable Long meetingId) {
        List<ParticipantProjection> participants = participantService.getMeetingParticipants(meetingId);
        return ResponseEntity.ok(ApiResponse.success("Participants retrieved successfully", participants));
    }

    @PostMapping("/invite")
    @Operation(
            summary = "Zaprasza uczestników do spotkania",
            description = "Wysyła zaproszenia do podanych użytkowników."
    )
    public ResponseEntity<ApiResponse<Void>> inviteParticipants(
            @PathVariable Long meetingId,
            @Valid @RequestBody InviteParticipantsRequest request,
            @AuthenticationPrincipal Long userId) {

        participantService.inviteMultipleParticipants(meetingId, request, userId);
        return ResponseEntity.ok(ApiResponse.success("Participants invited successfully", null));
    }

    @PostMapping("/join")
    @Operation(
            summary = "Dołącza do publicznego spotkania",
            description = "Umożliwia dołączenie do spotkania publicznego."
    )
    public ResponseEntity<ApiResponse<Void>> joinMeeting(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal Long userId) {

        participantService.joinPublicMeeting(meetingId, userId);
        return ResponseEntity.ok(ApiResponse.success("Joined meeting successfully", null));
    }

    @PatchMapping("/{participantId}/status")
    @Operation(
            summary = "Aktualizuje status uczestnika",
            description = "Zmienia status uczestnika (POTWIERDZONY, ODRZUCONY, OCZEKUJĄCY)."
    )
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
    @Operation(
            summary = "Aktualizuje uprawnienia uczestnika",
            description = "Zmienia poziom uprawnień uczestnika (CZYTELNIK, WSPÓŁORGANIZATOR)."
    )
    public ResponseEntity<ApiResponse<Void>> updateParticipantPermission(
            @PathVariable Long meetingId,
            @PathVariable Long participantId,
            @RequestParam PermissionLevel permissionLevel,
            @AuthenticationPrincipal Long userId) {

        participantService.updateParticipantPermission(meetingId, participantId, permissionLevel, userId);
        return ResponseEntity.ok(ApiResponse.success("Participant permission updated successfully", null));
    }

    @DeleteMapping("/{participantId}")
    @Operation(
            summary = "Usuwa uczestnika ze spotkania",
            description = "Usuwa uczestnika z listy uczestników spotkania."
    )
    public ResponseEntity<ApiResponse<Void>> removeParticipant(
            @PathVariable Long meetingId,
            @PathVariable Long participantId,
            @AuthenticationPrincipal Long userId) {

        participantService.removeParticipant(meetingId, participantId, userId);
        return ResponseEntity.ok(ApiResponse.success("Participant removed successfully", null));
    }

    @PostMapping("/invitations/{token}/accept")
    @Operation(
            summary = "Akceptuje zaproszenie przez token",
            description = "Akceptuje zaproszenie do spotkania przy użyciu tokenu."
    )
    public ResponseEntity<ApiResponse<Void>> acceptInvitationByToken(@PathVariable String token) {
        participantService.acceptInvitationByToken(token);
        return ResponseEntity.ok(ApiResponse.success("Invitation accepted successfully", null));
    }

    @GetMapping("/search-users")
    @Operation(
            summary = "Wyszukuje użytkowników do zaproszenia",
            description = "Wyszukuje użytkowników na podstawie zapytania."
    )
    public ResponseEntity<ApiResponse<List<UserResponse>>> searchUsers(
            @RequestParam String query,
            @PathVariable Long meetingId) {

        List<UserResponse> users = participantService.searchUsersForInvitation(query, meetingId);
        return ResponseEntity.ok(ApiResponse.success("Users found", users));
    }

    @GetMapping("/invitations")
    @Operation(
            summary = "Pobiera zaproszenia użytkownika",
            description = "Zwraca listę zaproszeń do spotkań dla zalogowanego użytkownika."
    )
    public ResponseEntity<ApiResponse<List<ParticipantResponse>>> getUserInvitations(
            @AuthenticationPrincipal Long userId) {

        List<ParticipantResponse> invitations = participantService.getUserInvitations(userId);
        return ResponseEntity.ok(ApiResponse.success("Invitations retrieved successfully", invitations));
    }

    @PostMapping("/invitations/{participantId}/respond")
    @Operation(
            summary = "Odpowiada na zaproszenie",
            description = "Pozwala użytkownikowi odpowiedzieć na zaproszenie (zaakceptować/odrzucić)."
    )
    public ResponseEntity<ApiResponse<Void>> respondToInvitation(
            @PathVariable Long participantId,
            @RequestParam ParticipationStatus response,
            @RequestParam(required = false) String comment,
            @AuthenticationPrincipal Long userId) {

        participantService.respondToInvitation(participantId, response, comment, userId);
        return ResponseEntity.ok(ApiResponse.success("Invitation response submitted successfully", null));
    }
}

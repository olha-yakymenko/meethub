package com.meethub.controller.api;

import com.meethub.domain.model.enums.ParticipationStatus;
import com.meethub.domain.model.enums.PermissionLevel;
import com.meethub.domain.model.projection.ParticipantProjection;
import com.meethub.domain.model.request.InviteParticipantsRequest;
import com.meethub.domain.model.request.RespondToInvitationRequest;
import com.meethub.domain.model.request.UpdateParticipantPermissionRequest;
import com.meethub.domain.model.request.UpdateParticipantStatusRequest;
import com.meethub.domain.model.response.ApiResponse;
import com.meethub.domain.model.response.ParticipantResponse;
import com.meethub.domain.model.response.UserResponse;
import com.meethub.domain.service.MeetingParticipantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@Slf4j
@RestController
@RequestMapping("/api/v1/meetings/{meetingId}/participants")
@RequiredArgsConstructor
@Tag(name = "Meeting Participants", description = "API do zarządzania uczestnikami spotkań")
public class MeetingParticipantController {

    private final MeetingParticipantService participantService;

    @GetMapping
    @Operation(
            summary = "Pobiera uczestników spotkania",
            description = "Zwraca listę wszystkich uczestników spotkania."
    )
    public ResponseEntity<ApiResponse<List<ParticipantProjection>>> getParticipants(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId) {

        List<ParticipantProjection> participants = participantService.getMeetingParticipants(meetingId);
        log.info("Pobrano listę uczestników spotkania {}: {} uczestników", meetingId, participants.size());
        return ResponseEntity.ok(ApiResponse.success("Uczestnicy spotkania pobrani pomyślnie", participants));
    }

    @PostMapping("/invite")
    @Operation(
            summary = "Zaprasza uczestników do spotkania",
            description = "Wysyła zaproszenia do podanych użytkowników."
    )
    public ResponseEntity<ApiResponse<Void>> inviteParticipants(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @RequestBody @Valid InviteParticipantsRequest request,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            @Min(value = 1, message = "Identyfikator użytkownika musi być liczbą dodatnią")
            Long userId) {

        participantService.inviteMultipleParticipants(meetingId, request, userId);
        log.info("Użytkownik {} zaprosił uczestników do spotkania {}", userId, meetingId);
        return ResponseEntity.ok(ApiResponse.success("Uczestnicy zaproszeni pomyślnie", null));
    }

    @PostMapping("/join")
    @Operation(
            summary = "Dołącza do publicznego spotkania",
            description = "Umożliwia dołączenie do spotkania publicznego."
    )
    public ResponseEntity<ApiResponse<Void>> joinMeeting(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            @Min(value = 1, message = "Identyfikator użytkownika musi być liczbą dodatnią")
            Long userId) {

        participantService.joinPublicMeeting(meetingId, userId);
        log.info("Użytkownik {} dołączył do publicznego spotkania {}", userId, meetingId);
        return ResponseEntity.ok(ApiResponse.success("Dołączono do spotkania pomyślnie", null));
    }

    @PatchMapping("/{participantId}/status")
    @Operation(
            summary = "Aktualizuje status uczestnika",
            description = "Zmienia status uczestnika (POTWIERDZONY, ODRZUCONY, OCZEKUJĄCY)."
    )
    public ResponseEntity<ApiResponse<Void>> updateParticipantStatus(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            Long meetingId,

            @PathVariable @NotNull(message = "Identyfikator uczestnika nie może być pusty")
            @Min(value = 1, message = "Identyfikator uczestnika musi być liczbą dodatnią")
            Long participantId,

            @RequestParam @NotNull(message = "Status nie może być pusty")
            ParticipationStatus status,

            @RequestParam(required = false)
            @Size(max = 500, message = "Komentarz nie może przekraczać 500 znaków")
            String comment,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            @Min(value = 1, message = "Identyfikator użytkownika musi być liczbą dodatnią")
            Long userId) {

        participantService.updateParticipantStatus(meetingId, participantId, status, comment, userId);
        log.info("Użytkownik {} zaktualizował status uczestnika {} na {} w spotkaniu {}",
                userId, participantId, status, meetingId);
        return ResponseEntity.ok(ApiResponse.success("Status uczestnika zaktualizowany pomyślnie", null));
    }

    @PatchMapping("/{participantId}/permission")
    @Operation(
            summary = "Aktualizuje uprawnienia uczestnika",
            description = "Zmienia poziom uprawnień uczestnika (CZYTELNIK, WSPÓŁORGANIZATOR)."
    )
    public ResponseEntity<ApiResponse<Void>> updateParticipantPermission(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @PathVariable @NotNull(message = "Identyfikator uczestnika nie może być pusty")
            @Min(value = 1, message = "Identyfikator uczestnika musi być liczbą dodatnią")
            Long participantId,

            @RequestParam @NotNull(message = "Poziom uprawnień nie może być pusty")
            PermissionLevel permissionLevel,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            @Min(value = 1, message = "Identyfikator użytkownika musi być liczbą dodatnią")
            Long userId) {

        participantService.updateParticipantPermission(meetingId, participantId, permissionLevel, userId);
        log.info("Użytkownik {} zaktualizował uprawnienia uczestnika {} na {} w spotkaniu {}",
                userId, participantId, permissionLevel, meetingId);
        return ResponseEntity.ok(ApiResponse.success("Uprawnienia uczestnika zaktualizowane pomyślnie", null));
    }

    @DeleteMapping("/{participantId}")
    @Operation(
            summary = "Usuwa uczestnika ze spotkania",
            description = "Usuwa uczestnika z listy uczestników spotkania."
    )
    public ResponseEntity<ApiResponse<Void>> removeParticipant(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @PathVariable @NotNull(message = "Identyfikator uczestnika nie może być pusty")
            @Min(value = 1, message = "Identyfikator uczestnika musi być liczbą dodatnią")
            Long participantId,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            @Min(value = 1, message = "Identyfikator użytkownika musi być liczbą dodatnią")
            Long userId) {

        participantService.removeParticipant(meetingId, participantId, userId);
        log.info("Użytkownik {} usunął uczestnika {} ze spotkania {}", userId, participantId, meetingId);
        return ResponseEntity.ok(ApiResponse.success("Uczestnik usunięty pomyślnie", null));
    }

    @PostMapping("/invitations/{token}/accept")
    @Operation(
            summary = "Akceptuje zaproszenie przez token",
            description = "Akceptuje zaproszenie do spotkania przy użyciu tokenu."
    )
    public ResponseEntity<ApiResponse<Void>> acceptInvitationByToken(
            @PathVariable @NotBlank(message = "Token nie może być pusty")
            @Size(min = 32, max = 64, message = "Token musi mieć od 32 do 64 znaków")
            String token) {

        participantService.acceptInvitationByToken(token);
        log.info("Zaproszenie zaakceptowane za pomocą tokenu");
        return ResponseEntity.ok(ApiResponse.success("Zaproszenie zaakceptowane pomyślnie", null));
    }

    @GetMapping("/search-users")
    @Operation(
            summary = "Wyszukuje użytkowników do zaproszenia",
            description = "Wyszukuje użytkowników na podstawie zapytania."
    )
    public ResponseEntity<ApiResponse<List<UserResponse>>> searchUsers(
            @RequestParam @NotBlank(message = "Zapytanie wyszukiwania nie może być puste")
            @Size(min = 2, max = 100, message = "Zapytanie musi mieć od 2 do 100 znaków")
            String query,

            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId) {

        List<UserResponse> users = participantService.searchUsersForInvitation(query, meetingId);
        log.info("Wyszukano {} użytkowników dla zapytania '{}' do spotkania {}",
                users.size(), query, meetingId);
        return ResponseEntity.ok(ApiResponse.success("Użytkownicy znalezieni pomyślnie", users));
    }

    @GetMapping("/invitations")
    @Operation(
            summary = "Pobiera zaproszenia użytkownika",
            description = "Zwraca listę zaproszeń do spotkań dla zalogowanego użytkownika."
    )
    public ResponseEntity<ApiResponse<List<ParticipantResponse>>> getUserInvitations(
            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            @Min(value = 1, message = "Identyfikator użytkownika musi być liczbą dodatnią")
            Long userId) {

        List<ParticipantResponse> invitations = participantService.getUserInvitations(userId);
        log.info("Pobrano {} zaproszeń dla użytkownika {}", invitations.size(), userId);
        return ResponseEntity.ok(ApiResponse.success("Zaproszenia pobrane pomyślnie", invitations));
    }

    @PostMapping("/invitations/{participantId}/respond")
    @Operation(
            summary = "Odpowiada na zaproszenie",
            description = "Pozwala użytkownikowi odpowiedzieć na zaproszenie (zaakceptować/odrzucić)."
    )
    public ResponseEntity<ApiResponse<Void>> respondToInvitation(
            @PathVariable @NotNull(message = "Identyfikator uczestnika nie może być pusty")
            @Min(value = 1, message = "Identyfikator uczestnika musi być liczbą dodatnią")
            Long participantId,

            @RequestParam @NotNull(message = "Odpowiedź nie może być pusta")
            ParticipationStatus response,

            @RequestParam(required = false)
            @Size(max = 500, message = "Komentarz nie może przekraczać 500 znaków")
            String comment,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            @Min(value = 1, message = "Identyfikator użytkownika musi być liczbą dodatnią")
            Long userId) {

        participantService.respondToInvitation(participantId, response, comment, userId);
        log.info("Użytkownik {} odpowiedział na zaproszenie {} statusem {}",
                userId, participantId, response);
        return ResponseEntity.ok(ApiResponse.success("Odpowiedź na zaproszenie wysłana pomyślnie", null));
    }
}


//package com.meethub.controller.api;
//
//import com.meethub.domain.model.enums.ParticipationStatus;
//import com.meethub.domain.model.enums.PermissionLevel;
//import com.meethub.domain.model.projection.ParticipantProjection;
//import com.meethub.domain.model.request.InviteParticipantsRequest;
//import com.meethub.domain.model.request.RespondToInvitationRequest;
//import com.meethub.domain.model.request.UpdateParticipantPermissionRequest;
//import com.meethub.domain.model.request.UpdateParticipantStatusRequest;
//import com.meethub.domain.model.response.ApiResponse;
//import com.meethub.domain.model.response.ParticipantResponse;
//import com.meethub.domain.model.response.UserResponse;
//import com.meethub.domain.service.MeetingParticipantService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import jakarta.validation.Valid;
//import jakarta.validation.constraints.Min;
//import jakarta.validation.constraints.NotBlank;
//import jakarta.validation.constraints.NotNull;
//import jakarta.validation.constraints.Size;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.validation.annotation.Validated;
//import org.springframework.web.bind.annotation.*;
//
//        import java.util.List;
//
//@Validated
//@Slf4j
//
//@RestController
//@RequestMapping("/api/v1/meetings/{meetingId}/participants")
//@RequiredArgsConstructor
//@Tag(name = "Meeting Participants", description = "API do zarządzania uczestnikami spotkań")
//public class MeetingParticipantController {
//
//    private final MeetingParticipantService participantService;
//
//    @GetMapping
//    public ResponseEntity<ApiResponse<List<ParticipantProjection>>> getParticipants(@PathVariable Long meetingId) {
//        List<ParticipantProjection> participants = participantService.getMeetingParticipants(meetingId);
//        return ResponseEntity.ok(ApiResponse.success("Uczestnicy spotkania pobrani pomyślnie", participants));
//    }
//
//    @PostMapping("/invite")
//    public ResponseEntity<ApiResponse<Void>> inviteParticipants(
//            @PathVariable Long meetingId,
//            @RequestBody @Valid InviteParticipantsRequest request,
//            @AuthenticationPrincipal Long userId) {
//
//        participantService.inviteMultipleParticipants(meetingId, request, userId);
//        return ResponseEntity.ok(ApiResponse.success("Uczestnicy zaproszeni pomyślnie", null));
//    }
//
//    @PostMapping("/join")
//    public ResponseEntity<ApiResponse<Void>> joinMeeting(
//            @PathVariable Long meetingId,
//            @AuthenticationPrincipal Long userId) {
//
//        participantService.joinPublicMeeting(meetingId, userId);
//        return ResponseEntity.ok(ApiResponse.success("Dołączono do spotkania pomyślnie", null));
//    }
//
//    @PatchMapping("/{participantId}/status")
//    public ResponseEntity<ApiResponse<Void>> updateParticipantStatus(
//            @PathVariable Long meetingId,
//            @PathVariable Long participantId,
//            @RequestBody @Valid UpdateParticipantStatusRequest request,
//            @AuthenticationPrincipal Long userId) {
//
//        participantService.updateParticipantStatus(
//                meetingId,
//                participantId,
//                request.getStatus(),
//                request.getComment(),
//                userId
//        );
//        return ResponseEntity.ok(ApiResponse.success("Status uczestnika zaktualizowany pomyślnie", null));
//    }
//
//    @PatchMapping("/{participantId}/permission")
//    public ResponseEntity<ApiResponse<Void>> updateParticipantPermission(
//            @PathVariable Long meetingId,
//            @PathVariable Long participantId,
//            @RequestBody @Valid UpdateParticipantPermissionRequest request,
//            @AuthenticationPrincipal Long userId) {
//
//        participantService.updateParticipantPermission(
//                meetingId,
//                participantId,
//                request.getPermissionLevel(),
//                userId
//        );
//        return ResponseEntity.ok(ApiResponse.success("Uprawnienia uczestnika zaktualizowane pomyślnie", null));
//    }
//
//    @DeleteMapping("/{participantId}")
//    public ResponseEntity<ApiResponse<Void>> removeParticipant(
//            @PathVariable Long meetingId,
//            @PathVariable Long participantId,
//            @AuthenticationPrincipal Long userId) {
//
//        participantService.removeParticipant(meetingId, participantId, userId);
//        return ResponseEntity.ok(ApiResponse.success("Uczestnik usunięty pomyślnie", null));
//    }
//
//    @PostMapping("/invitations/{token}/accept")
//    public ResponseEntity<ApiResponse<Void>> acceptInvitationByToken(@PathVariable String token) {
//        participantService.acceptInvitationByToken(token);
//        return ResponseEntity.ok(ApiResponse.success("Zaproszenie zaakceptowane pomyślnie", null));
//    }
//
//    @GetMapping("/search-users")
//    public ResponseEntity<ApiResponse<List<UserResponse>>> searchUsers(
//            @RequestParam String query,
//            @PathVariable Long meetingId) {
//
//        List<UserResponse> users = participantService.searchUsersForInvitation(query, meetingId);
//        return ResponseEntity.ok(ApiResponse.success("Użytkownicy znalezieni pomyślnie", users));
//    }
//
//    @GetMapping("/invitations")
//    public ResponseEntity<ApiResponse<List<ParticipantResponse>>> getUserInvitations(
//            @AuthenticationPrincipal Long userId) {
//
//        List<ParticipantResponse> invitations = participantService.getUserInvitations(userId);
//        return ResponseEntity.ok(ApiResponse.success("Zaproszenia pobrane pomyślnie", invitations));
//    }
//
//    @PostMapping("/invitations/{participantId}/respond")
//    public ResponseEntity<ApiResponse<Void>> respondToInvitation(
//            @PathVariable Long participantId,
//            @RequestBody @Valid RespondToInvitationRequest request,
//            @AuthenticationPrincipal Long userId) {
//
//        participantService.respondToInvitation(
//                participantId,
//                request.getResponse(),
//                request.getComment(),
//                userId
//        );
//        return ResponseEntity.ok(ApiResponse.success("Odpowiedź na zaproszenie wysłana pomyślnie", null));
//    }
//}

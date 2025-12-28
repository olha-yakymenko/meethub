package com.meethub.controller.api;

import com.meethub.domain.model.request.RecentMessagesRequest;
import com.meethub.domain.service.NotificationService;
import com.meethub.security.CustomUserDetailsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "API do zarządzania powiadomieniami")
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/in-app/messages")
    @Operation(summary = "Pobierz wiadomości IN_APP",
            description = "Zwraca wszystkie wiadomości powiadomień IN_APP")
    public ResponseEntity<List<String>> getInAppMessages(
            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetailsService.CustomUserDetails userDetails) {

        try {
            log.info("Pobieranie wiadomości IN_APP dla użytkownika {}", userDetails.getId());
            List<String> messages = notificationService.getInAppMessages(userDetails.getId());
            log.info("Pobrano {} wiadomości IN_APP dla użytkownika {}", messages.size(), userDetails.getId());
            return ResponseEntity.ok(messages);

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.error("Błąd walidacji podczas pobierania wiadomości: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Błąd pobierania wiadomości: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/in-app/messages/recent")
    public ResponseEntity<List<String>> getRecentInAppMessages(
            @AuthenticationPrincipal @NotNull CustomUserDetailsService.CustomUserDetails userDetails,
            @Valid RecentMessagesRequest request) {

        int limit = request.getLimit();
        log.info("Pobieranie {} ostatnich wiadomości IN_APP dla użytkownika {}", limit, userDetails.getId());
        List<String> messages = notificationService.getRecentInAppMessages(userDetails.getId(), limit);
        return ResponseEntity.ok(messages);
    }

}




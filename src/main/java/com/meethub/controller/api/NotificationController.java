package com.meethub.controller.api;

import com.meethub.domain.service.NotificationService;
import com.meethub.security.CustomUserDetailsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification APIs")
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/in-app/messages")
    @Operation(summary = "Pobierz wiadomości IN_APP",
            description = "Zwraca wszystkie wiadomości powiadomień IN_APP")
    public ResponseEntity<List<String>> getInAppMessages(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {

        try {
            if (userDetails == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            List<String> messages = notificationService.getInAppMessages(userDetails.getId());
            return ResponseEntity.ok(messages);

        } catch (Exception e) {
            log.error("Błąd pobierania wiadomości: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/in-app/messages/recent")
    @Operation(summary = "Pobierz ostatnie wiadomości IN_APP",
            description = "Zwraca ostatnie wiadomości (domyślnie 10)")
    public ResponseEntity<List<String>> getRecentInAppMessages(
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
            @RequestParam(defaultValue = "10") int limit) {

        try {
            if (userDetails == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            List<String> messages = notificationService
                    .getRecentInAppMessages(userDetails.getId(), limit);

            return ResponseEntity.ok(messages);

        } catch (IllegalArgumentException e) {
            log.warn("Nieprawidłowy parametr: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Błąd pobierania wiadomości: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }




}
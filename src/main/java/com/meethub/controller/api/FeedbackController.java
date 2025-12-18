package com.meethub.controller.api;

import com.meethub.domain.model.entity.Feedback;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.request.SubmitFeedbackRequest;
import com.meethub.domain.model.response.ApiResponse;
import com.meethub.domain.service.FeedbackService;
import com.meethub.domain.repository.jpa.UserRepository;
import com.meethub.security.CustomUserDetailsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/feedbacks")
@RequiredArgsConstructor
@Tag(name = "Feedbacks", description = "API do zarządzania opiniami")
@Slf4j
@Transactional
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping(value = "/meetings/{meetingId}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Operation(summary = "Składa opinię o spotkaniu",
            description = "Dodaje opinię (ocenę i komentarz) o spotkaniu. Tylko uczestnicy mogą dodawać opinie.")
    public ResponseEntity<ApiResponse<Void>> submitFeedback(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @RequestParam @NotNull(message = "Ocena nie może być pusta")
            @Min(value = 1, message = "Ocena musi być co najmniej 1")
            @Max(value = 5, message = "Ocena nie może przekraczać 5")
            Integer rating,

            @RequestParam(required = false)
            @Size(max = 1000, message = "Komentarz nie może przekraczać 1000 znaków")
            String comment,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetailsService.CustomUserDetails userDetails) {

        try {
            log.info("Dodawanie opinii dla spotkania: {}, ocena: {}", meetingId, rating);

            // Walidacja użytkownika (dodatkowa walidacja bezpieczeństwa)
            if (userDetails == null) {
                log.warn("Użytkownik nie zalogowany");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Musisz być zalogowany, aby dodać opinię"));
            }

            // Utwórz request z walidacją
            SubmitFeedbackRequest request = SubmitFeedbackRequest.builder()
                    .rating(rating)
                    .comment(comment)
                    .build();

            // Walidacja requestu
            validateFeedbackRequest(request);

            // Wywołaj serwis
            feedbackService.submitFeedback(meetingId, userDetails.getId(), request);

            return ResponseEntity.ok(ApiResponse.success("Opinia została pomyślnie dodana", null));

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.error("Błąd walidacji podczas dodawania opinii", e);
            String errorMessage = e.getConstraintViolations().stream()
                    .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                    .findFirst()
                    .orElse("Nieprawidłowe dane wejściowe");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(errorMessage));

        } catch (Exception e) {
            log.error("Błąd podczas dodawania opinii", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Wystąpił błąd podczas dodawania opinii: " + e.getMessage()));
        }
    }

    @GetMapping("/meetings/{meetingId}")
    @Operation(summary = "Pobiera opinie o spotkaniu",
            description = "Zwraca wszystkie opinie dodane do spotkania.")
    public ResponseEntity<ApiResponse<List<Feedback>>> getMeetingFeedbacks(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId) {

        try {
            log.info("Pobieranie opinii dla spotkania: {}", meetingId);
            List<Feedback> feedbacks = feedbackService.getMeetingFeedbacks(meetingId);
            return ResponseEntity.ok(ApiResponse.success("Opinie zostały pomyślnie pobrane", feedbacks));

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.error("Błąd walidacji podczas pobierania opinii", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Nieprawidłowy identyfikator spotkania"));

        } catch (Exception e) {
            log.error("Błąd podczas pobierania opinii", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Wystąpił błąd podczas pobierania opinii: " + e.getMessage()));
        }
    }

    // Alternatywna wersja metody submitFeedback z JSON request body (bardziej elegancka)
    @PostMapping(value = "/meetings/{meetingId}/submit", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Składa opinię o spotkaniu (JSON)",
            description = "Dodaje opinię o spotkaniu za pomocą JSON request body.")
    public ResponseEntity<ApiResponse<Void>> submitFeedbackJson(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @RequestBody @Valid SubmitFeedbackRequest request,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetailsService.CustomUserDetails userDetails) {

        try {
            log.info("Dodawanie opinii (JSON) dla spotkania: {}, ocena: {}", meetingId, request.getRating());

            feedbackService.submitFeedback(meetingId, userDetails.getId(), request);

            return ResponseEntity.ok(ApiResponse.success("Opinia została pomyślnie dodana", null));

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.error("Błąd walidacji JSON podczas dodawania opinii", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Nieprawidłowe dane w żądaniu"));

        } catch (Exception e) {
            log.error("Błąd podczas dodawania opinii (JSON)", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Wystąpił błąd podczas dodawania opinii: " + e.getMessage()));
        }
    }

    private void validateFeedbackRequest(SubmitFeedbackRequest request) {
        // Dodatkowa walidacja biznesowa
        if (request == null) {
            throw new IllegalArgumentException("Żądanie nie może być puste");
        }

        if (request.getRating() == null) {
            throw new IllegalArgumentException("Ocena jest wymagana");
        }

        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new IllegalArgumentException("Ocena musi być w zakresie 1-5");
        }

        if (request.getComment() != null && request.getComment().length() > 1000) {
            throw new IllegalArgumentException("Komentarz nie może przekraczać 1000 znaków");
        }
    }
}



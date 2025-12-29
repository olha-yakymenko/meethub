package com.meethub.controller.api;

import com.meethub.domain.model.entity.Feedback;
import com.meethub.domain.model.request.SubmitFeedbackRequest;
import com.meethub.domain.model.response.ApiResponse;
import com.meethub.domain.service.FeedbackService;
import com.meethub.security.CustomUserDetailsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/feedbacks")
@RequiredArgsConstructor
@Tag(name = "Feedbacks", description = "API do zarządzania opiniami")
@Slf4j
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping("/meetings/{meetingId}")
    @Operation(summary = "Składa opinię o spotkaniu",
            description = "Dodaje opinię (ocenę i komentarz) o spotkaniu. Tylko uczestnicy mogą dodawać opinie.")
    public ResponseEntity<ApiResponse<Void>> submitFeedback(
            @PathVariable Long meetingId,
            @Valid @RequestBody SubmitFeedbackRequest request,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {

        log.info("Dodawanie opinii dla spotkania: {}, ocena: {}, użytkownik: {}",
                meetingId, request.getRating(), userDetails.getId());

        feedbackService.submitFeedback(meetingId, userDetails.getId(), request);

        return ResponseEntity.ok(ApiResponse.success("Opinia została pomyślnie dodana", null));
    }

    @GetMapping("/meetings/{meetingId}")
    @Operation(summary = "Pobiera opinie o spotkaniu",
            description = "Zwraca wszystkie opinie dodane do spotkania.")
    public ResponseEntity<ApiResponse<List<Feedback>>> getMeetingFeedbacks(
            @PathVariable Long meetingId) {

        log.info("Pobieranie opinii dla spotkania: {}", meetingId);
        List<Feedback> feedbacks = feedbackService.getMeetingFeedbacks(meetingId);
        return ResponseEntity.ok(ApiResponse.success("Opinie zostały pomyślnie pobrane", feedbacks));
    }
}
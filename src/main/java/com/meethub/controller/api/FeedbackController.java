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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/feedbacks")
@RequiredArgsConstructor
@Tag(name = "Feedbacks", description = "Feedback APIs")
@Slf4j
@Transactional
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final UserRepository userRepository;

//    @PostMapping("/meetings/{meetingId}")
//    @Operation(summary = "Submit feedback")
//    public ResponseEntity<ApiResponse<Feedback>> submitFeedback(
//            @PathVariable Long meetingId,
//            @Valid @RequestBody SubmitFeedbackRequest request) {
//
//        try {
//            log.info("Submitting feedback for meeting: {}, rating: {}", meetingId, request.getRating());
//
//            // Pobierz aktualnie zalogowanego użytkownika
//            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//
//            if (authentication == null || !authentication.isAuthenticated()) {
//                log.warn("User not authenticated");
//                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                        .body(ApiResponse.error("Musisz być zalogowany aby dodać opinię"));
//            }
//
//            Object principal = authentication.getPrincipal();
//
//            // Dla form-based auth, principal to zazwyczaj UserDetails lub String (email)
//            String email;
//
//            if (principal instanceof CustomUserDetailsService.CustomUserDetails) {
//                email = ((CustomUserDetailsService.CustomUserDetails) principal).getUsername();
//            } else if (principal instanceof String) {
//                email = (String) principal;
//                if ("anonymousUser".equals(email)) {
//                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                            .body(ApiResponse.error("Musisz być zalogowany"));
//                }
//            } else {
//                log.error("Unknown principal type: {}", principal.getClass());
//                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                        .body(ApiResponse.error("Błąd autentykacji"));
//            }
//
//            // Znajdź użytkownika
//            User user = userRepository.findByEmail(email)
//                    .orElseThrow(() -> new RuntimeException("User not found: " + email));
//
//            log.info("User found: {} (ID: {})", email, user.getId());
//
//            Feedback feedback = feedbackService.submitFeedback(meetingId, user.getId(), request);
//            return ResponseEntity.ok(ApiResponse.success("Opinia została dodana", feedback));
//
//        } catch (Exception e) {
//            log.error("Error submitting feedback", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(ApiResponse.error("Błąd: " + e.getMessage()));
//        }
//    }




//    @PostMapping("/meetings/{meetingId}")
//    @Operation(summary = "Submit feedback")
//    public ResponseEntity<ApiResponse<Feedback>> submitFeedback(
//            @PathVariable Long meetingId,
//            @Valid @RequestBody SubmitFeedbackRequest request,
//            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {  // DODAJ TEN PARAMETR
//
//        try {
//            log.info("Submitting feedback for meeting: {}, rating: {}", meetingId, request.getRating());
//
//            // Walidacja meetingId
//            if (meetingId == null || meetingId <= 0) {
//                log.error("Invalid meetingId: {}", meetingId);
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                        .body(ApiResponse.error("Nieprawidłowe ID spotkania: " + meetingId));
//            }
//
//            // Walidacja użytkownika
//            if (userDetails == null) {
//                log.warn("User not authenticated");
//                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                        .body(ApiResponse.error("Musisz być zalogowany"));
//            }
//
//            log.info("User ID: {}, Email: {}", userDetails.getId(), userDetails.getUsername());
//
//            Feedback feedback = feedbackService.submitFeedback(meetingId, userDetails.getId(), request);
//            return ResponseEntity.ok(ApiResponse.success("Opinia została dodana", feedback));
//
//        } catch (Exception e) {
//            log.error("Error submitting feedback", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(ApiResponse.error("Błąd: " + e.getMessage()));
//        }
//    }


    @PostMapping(value = "/meetings/{meetingId}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Operation(summary = "Submit feedback")
    public ResponseEntity<ApiResponse<Void>> submitFeedback(
            @PathVariable Long meetingId,
            @RequestParam Integer rating,
            @RequestParam(required = false) String comment,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {

        try {
            log.info("Submitting feedback for meeting: {}, rating: {}", meetingId, rating);

            // Walidacja meetingId
            if (meetingId == null || meetingId <= 0) {
                log.error("Invalid meetingId: {}", meetingId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Nieprawidłowe ID spotkania: " + meetingId));
            }

            // Walidacja użytkownika
            if (userDetails == null) {
                log.warn("User not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Musisz być zalogowany"));
            }

            // Walidacja ratingu
            if (rating == null || rating < 1 || rating > 5) {
                log.error("Invalid rating: {}", rating);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Ocena musi być w zakresie 1-5"));
            }

            // Utwórz request
            SubmitFeedbackRequest request = new SubmitFeedbackRequest();
            request.setRating(rating);
            request.setComment(comment);

            // Wywołaj serwis
            feedbackService.submitFeedback(meetingId, userDetails.getId(), request);

            return ResponseEntity.ok(ApiResponse.success("Opinia została dodana", null));

        } catch (Exception e) {
            log.error("Error submitting feedback", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Błąd: " + e.getMessage()));
        }
    }


    @GetMapping("/meetings/{meetingId}")
    @Operation(summary = "Get meeting feedbacks")
    public ResponseEntity<ApiResponse<List<Feedback>>> getMeetingFeedbacks(
            @PathVariable Long meetingId) {
        List<Feedback> feedbacks = feedbackService.getMeetingFeedbacks(meetingId);
        return ResponseEntity.ok(ApiResponse.success("Feedbacks retrieved", feedbacks));
    }
}
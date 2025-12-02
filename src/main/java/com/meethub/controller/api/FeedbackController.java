package com.meethub.controller.api;

import com.meethub.domain.model.entity.Feedback;
import com.meethub.domain.model.request.SubmitFeedbackRequest;
import com.meethub.domain.model.response.ApiResponse;
import com.meethub.domain.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/feedbacks")
@RequiredArgsConstructor
@Tag(name = "Feedbacks", description = "Feedback APIs")
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping("/meetings/{meetingId}")
    @Operation(summary = "Submit feedback")
    public ResponseEntity<ApiResponse<Feedback>> submitFeedback(
            @PathVariable Long meetingId,
            @Valid @RequestBody SubmitFeedbackRequest request,
            @AuthenticationPrincipal Long userId) {

        Feedback feedback = feedbackService.submitFeedback(meetingId, userId, request);
        return ResponseEntity.ok(ApiResponse.success("Feedback submitted", feedback));
    }

    @GetMapping("/meetings/{meetingId}")
    @Operation(summary = "Get meeting feedbacks")
    public ResponseEntity<ApiResponse<List<Feedback>>> getMeetingFeedbacks(
            @PathVariable Long meetingId) {

        List<Feedback> feedbacks = feedbackService.getMeetingFeedbacks(meetingId);
        return ResponseEntity.ok(ApiResponse.success("Feedbacks retrieved", feedbacks));
    }

    @GetMapping("/meetings/{meetingId}/my-feedback")
    @Operation(summary = "Get my feedback")
    public ResponseEntity<ApiResponse<Feedback>> getMyFeedback(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal Long userId) {

        Feedback feedback = feedbackService.getUserFeedback(meetingId, userId);
        return ResponseEntity.ok(ApiResponse.success("Feedback retrieved", feedback));
    }
}
package com.meethub.controller.web;

import com.meethub.domain.model.request.SubmitFeedbackRequest;
import com.meethub.domain.service.FeedbackService;
import com.meethub.security.CustomUserDetailsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/meetings/{meetingId}/feedbacks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Feedback Web", description = "Strony web do obsługi feedbacku")
public class FeedbackWebController {

    private final FeedbackService feedbackService;

    @PostMapping("/submit")
    public String submitFeedback(
            @PathVariable Long meetingId,
            @RequestParam Integer rating,
            @RequestParam(required = false) String comment,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            log.info("Submitting feedback via web for meeting: {}", meetingId);

            if (userDetails == null) {
                redirectAttributes.addFlashAttribute("error", "Musisz być zalogowany");
                return "redirect:/login";
            }

            if (rating == null || rating < 1 || rating > 5) {
                redirectAttributes.addFlashAttribute("error", "Ocena musi być w zakresie 1-5");
                return "redirect:/meetings/" + meetingId;
            }

            SubmitFeedbackRequest request = new SubmitFeedbackRequest();
            request.setRating(rating);
            request.setComment(comment);

            feedbackService.submitFeedback(meetingId, userDetails.getId(), request);

            redirectAttributes.addFlashAttribute("success", "Opinia została dodana pomyślnie!");

        } catch (Exception e) {
            log.error("Error submitting feedback", e);
            redirectAttributes.addFlashAttribute("error", "Błąd: " + e.getMessage());
        }

        return "redirect:/meetings/" + meetingId;
    }
}
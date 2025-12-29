package com.meethub.controller.web;

import com.meethub.domain.model.request.SubmitFeedbackRequest;
import com.meethub.domain.service.FeedbackService;
import com.meethub.security.CustomUserDetailsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/meetings/{meetingId}/feedbacks")
@RequiredArgsConstructor
@Slf4j
public class FeedbackWebController {

    private final FeedbackService feedbackService;

    @PostMapping("/submit")
    public String submitFeedback(
            @PathVariable Long meetingId,
            @Valid @ModelAttribute("feedbackRequest") SubmitFeedbackRequest request,
            BindingResult bindingResult,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            log.warn("Błędy walidacji formularza opinii: {}", bindingResult.getAllErrors());

            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.feedbackRequest", bindingResult);
            redirectAttributes.addFlashAttribute("feedbackRequest", request);

            return "redirect:/meetings/" + meetingId;
        }

        log.info("Przesyłanie opinii dla spotkania {} przez użytkownika {}",
                meetingId, userDetails.getId());
        feedbackService.submitFeedback(meetingId, userDetails.getId(), request);

        redirectAttributes.addFlashAttribute("success", "Opinia została dodana pomyślnie!");
        return "redirect:/meetings/" + meetingId;
    }
}
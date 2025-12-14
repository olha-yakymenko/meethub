package com.meethub.controller.web;

import com.meethub.domain.model.request.SubmitFeedbackRequest;
import com.meethub.domain.service.FeedbackService;
import com.meethub.security.CustomUserDetailsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Validated // DODANE - walidacja dla kontrolera webowego
@Controller
@RequestMapping("/meetings/{meetingId}/feedbacks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Feedback Web", description = "Strony web do obsługi feedbacku")
public class FeedbackWebController {

    private final FeedbackService feedbackService;

    @PostMapping("/submit")
    public String submitFeedback(
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
            CustomUserDetailsService.CustomUserDetails userDetails,

            RedirectAttributes redirectAttributes) {

        try {
            log.info("Przesyłanie opinii przez stronę web dla spotkania: {}, ocena: {}", meetingId, rating);

            // Alternatywnie możesz użyć obiektu formularza z @Valid i BindingResult
            SubmitFeedbackRequest request = new SubmitFeedbackRequest();
            request.setRating(rating);
            request.setComment(comment);

            feedbackService.submitFeedback(meetingId, userDetails.getId(), request);

            redirectAttributes.addFlashAttribute("success", "Opinia została dodana pomyślnie!");
            log.info("Opinia dodana pomyślnie dla spotkania {} przez użytkownika {}", meetingId, userDetails.getId());

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji przy przesyłaniu opinii: {}", e.getMessage());
            String errorMessage = e.getConstraintViolations().stream()
                    .map(violation -> violation.getMessage())
                    .findFirst()
                    .orElse("Nieprawidłowe dane wejściowe");
            redirectAttributes.addFlashAttribute("error", errorMessage);

        } catch (Exception e) {
            log.error("Błąd podczas przesyłania opinii", e);
            redirectAttributes.addFlashAttribute("error", "Wystąpił błąd podczas dodawania opinii: " + e.getMessage());
        }

        return "redirect:/meetings/" + meetingId;
    }

    // Alternatywna wersja z obiektem formularza i BindingResult
    @PostMapping("/submit-form")
    public String submitFeedbackForm(
            @PathVariable Long meetingId,
            @Valid @ModelAttribute("feedbackForm") SubmitFeedbackRequest request,
            BindingResult bindingResult,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            // Błędy walidacji są automatycznie dodawane do BindingResult
            log.warn("Błędy walidacji formularza opinii: {}", bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.feedbackForm", bindingResult);
            redirectAttributes.addFlashAttribute("feedbackForm", request);
            return "redirect:/meetings/" + meetingId;
        }

        try {
            feedbackService.submitFeedback(meetingId, userDetails.getId(), request);
            redirectAttributes.addFlashAttribute("success", "Opinia została dodana pomyślnie!");

        } catch (Exception e) {
            log.error("Błąd podczas przesyłania opinii", e);
            redirectAttributes.addFlashAttribute("error", "Wystąpił błąd: " + e.getMessage());
        }

        return "redirect:/meetings/" + meetingId;
    }
}






//package com.meethub.controller.web;
//
//import com.meethub.domain.model.request.SubmitFeedbackRequest;
//import com.meethub.domain.service.FeedbackService;
//import com.meethub.security.CustomUserDetailsService;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.servlet.mvc.support.RedirectAttributes;
//
//@Controller
//@RequestMapping("/meetings/{meetingId}/feedbacks")
//@RequiredArgsConstructor
//@Slf4j
//@Tag(name = "Feedback Web", description = "Strony web do obsługi feedbacku")
//public class FeedbackWebController {
//
//    private final FeedbackService feedbackService;
//
//    @PostMapping("/submit")
//    public String submitFeedback(
//            @PathVariable Long meetingId,
//            @RequestParam Integer rating,
//            @RequestParam(required = false) String comment,
//            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
//            RedirectAttributes redirectAttributes) {
//
//        try {
//            log.info("Submitting feedback via web for meeting: {}", meetingId);
//
//            if (userDetails == null) {
//                redirectAttributes.addFlashAttribute("error", "Musisz być zalogowany");
//                return "redirect:/login";
//            }
//
//            if (rating == null || rating < 1 || rating > 5) {
//                redirectAttributes.addFlashAttribute("error", "Ocena musi być w zakresie 1-5");
//                return "redirect:/meetings/" + meetingId;
//            }
//
//            SubmitFeedbackRequest request = new SubmitFeedbackRequest();
//            request.setRating(rating);
//            request.setComment(comment);
//
//            feedbackService.submitFeedback(meetingId, userDetails.getId(), request);
//
//            redirectAttributes.addFlashAttribute("success", "Opinia została dodana pomyślnie!");
//
//        } catch (Exception e) {
//            log.error("Error submitting feedback", e);
//            redirectAttributes.addFlashAttribute("error", "Błąd: " + e.getMessage());
//        }
//
//        return "redirect:/meetings/" + meetingId;
//    }
//}
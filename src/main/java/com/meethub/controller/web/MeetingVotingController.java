// MeetingVotingController.java
package com.meethub.controller.web;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.request.*;
import com.meethub.domain.model.response.*;
import com.meethub.domain.repository.jpa.MeetingRepository;
import com.meethub.domain.service.MeetingVotingService;
import com.meethub.security.CustomUserDetailsService.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/meetings/{meetingId}/votings")
@RequiredArgsConstructor
public class MeetingVotingController {

    private final MeetingVotingService votingService;
    private final MeetingRepository meetingRepository;

    // POPRAWIONE: Upewnij się, że ta metoda jest przed metodami z {votingId}
    @GetMapping
    public String getMeetingVotings(@PathVariable Long meetingId,
                                    @AuthenticationPrincipal CustomUserDetails userDetails,
                                    Model model) {
        try {
            List<VotingResponse> votings = votingService.getMeetingVotings(meetingId, userDetails.getId());

            model.addAttribute("votings", votings);
            model.addAttribute("meetingId", meetingId);
            model.addAttribute("userId", userDetails.getId());

            // Sprawdź czy użytkownik jest organizatorem
            Meeting meeting = meetingRepository.findById(meetingId)
                    .orElseThrow(() -> new RuntimeException("Spotkanie nie zostało znalezione"));
            boolean isOrganizer = meeting.getOrganizer().getId().equals(userDetails.getId());
            model.addAttribute("isOrganizer", isOrganizer);

            return "meetings/votings/list";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/meetings/" + meetingId;
        }
    }

    @GetMapping("/create")
    public String showCreateVotingForm(@PathVariable Long meetingId,
                                       @AuthenticationPrincipal CustomUserDetails userDetails,
                                       Model model) {
        try {
            // Sprawdź czy użytkownik jest organizatorem
            Meeting meeting = meetingRepository.findById(meetingId)
                    .orElseThrow(() -> new RuntimeException("Spotkanie nie zostało znalezione"));

            if (!meeting.getOrganizer().getId().equals(userDetails.getId())) {
                throw new RuntimeException("Tylko organizator może tworzyć głosowania");
            }

            model.addAttribute("meetingId", meetingId);
            model.addAttribute("createVotingRequest", new CreateVotingRequest());
            return "meetings/votings/create";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/meetings/" + meetingId + "/votings";
        }
    }

    @PostMapping("/create")
    public String createVoting(@PathVariable Long meetingId,
                               @Valid @ModelAttribute CreateVotingRequest request,
                               BindingResult result,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               Model model,
                               RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("meetingId", meetingId);
            return "meetings/votings/create";
        }

        try {
            VotingResponse voting = votingService.createVoting(meetingId, request, userDetails.getId());
            redirectAttributes.addFlashAttribute("success", "Głosowanie zostało utworzone");
            return "redirect:/meetings/" + meetingId + "/votings/" + voting.getId();
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("meetingId", meetingId);
            return "meetings/votings/create";
        }
    }

    // WAŻNE: Metoda z {votingId} musi być PO metodzie bez parametrów
    @GetMapping("/{votingId}")
    public String getVotingDetails(@PathVariable Long meetingId,
                                   @PathVariable Long votingId,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   Model model) {
        try {
            // NAJPIERW ZAMKNIJ WYGASŁE GŁOSOWANIE (działaj na encji)
            votingService.closeExpiredVotingIfNeeded(votingId);

            System.out.println("JESTSEM TUTAJ");

            // POTEM POBERZ DTO
            VotingResponse voting = votingService.getVotingDetails(votingId, userDetails.getId());

            model.addAttribute("voting", voting);
            model.addAttribute("meetingId", meetingId);
            model.addAttribute("userId", userDetails.getId());

            Meeting meeting = meetingRepository.findById(meetingId)
                    .orElseThrow(() -> new RuntimeException("Spotkanie nie zostało znalezione"));
            model.addAttribute("meeting", meeting);

            return "meetings/votings/details";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/meetings/" + meetingId + "/votings";
        }
    }

    @PostMapping("/{votingId}/vote")
    public String submitVote(@PathVariable Long meetingId,
                             @PathVariable Long votingId,
                             @Valid @ModelAttribute VoteRequest request,
                             BindingResult result,
                             @AuthenticationPrincipal CustomUserDetails userDetails,
                             RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Nieprawidłowe dane głosowania");
            return "redirect:/meetings/" + meetingId + "/votings/" + votingId;
        }

        try {
            votingService.submitVote(votingId, request, userDetails.getId());
            redirectAttributes.addFlashAttribute("success", "Twój głos został zapisany");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/meetings/" + meetingId + "/votings/" + votingId;
    }

    @PostMapping("/{votingId}/close")
    public String closeVoting(@PathVariable Long meetingId,
                              @PathVariable Long votingId,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              RedirectAttributes redirectAttributes) {

        try {
            votingService.closeVoting(votingId, userDetails.getId());
            redirectAttributes.addFlashAttribute("success", "Głosowanie zostało zamknięte");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/meetings/" + meetingId + "/votings/" + votingId;
    }
}
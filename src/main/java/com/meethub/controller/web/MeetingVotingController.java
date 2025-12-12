package com.meethub.controller.web;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.MeetingVoting;
import com.meethub.domain.model.request.CreateVotingRequest;
import com.meethub.domain.model.request.VoteRequest;
import com.meethub.domain.model.response.MeetingResponse;
import com.meethub.domain.model.response.VotingResponse;
import com.meethub.domain.repository.jpa.MeetingRepository;
import com.meethub.domain.service.MeetingParticipantService;
import com.meethub.domain.service.MeetingService;
import com.meethub.domain.service.MeetingVotingService;
import com.meethub.exception.VotingAccessDeniedException;
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
    private final MeetingService meetingService;

    // Lista wszystkich głosowań dla spotkania
    @GetMapping
    public String getMeetingVotings(@PathVariable Long meetingId,
                                    @AuthenticationPrincipal CustomUserDetails userDetails,
                                    Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        Long userId = userDetails.getId();

        try {
            List<VotingResponse> votings = votingService.getMeetingVotings(meetingId, userId);


//            Meeting meeting = meetingRepository.findById(meetingId)
//                    .orElseThrow(() -> new RuntimeException("Spotkanie nie zostało znalezione"));
//            boolean isOrganizer = meeting.getOrganizer().getId().equals(userDetails.getId());
//
//            model.addAttribute("votings", votings);
//            model.addAttribute("meetingId", meetingId);
//            model.addAttribute("userId", userDetails.getId());
//            model.addAttribute("isOrganizer", isOrganizer);

            MeetingResponse meeting = meetingService.getMeetingDetails(meetingId, userId);

            model.addAttribute("votings", votings);
            model.addAttribute("meeting", meeting);
            model.addAttribute("meetingId", meetingId);
            model.addAttribute("userId", userId);
            model.addAttribute("isOrganizer", meeting.isUserIsOrganizer());


            return "meetings/votings/list";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/meetings/" + meetingId;
        }
    }

    // Formularz tworzenia głosowania
//    @GetMapping("/create")
//    public String showCreateVotingForm(@PathVariable Long meetingId,
//                                       @AuthenticationPrincipal CustomUserDetails userDetails,
//                                       Model model) {
//        if (userDetails == null) {
//            return "redirect:/login";
//        }
//
//        try {
//            Meeting meeting = meetingRepository.findById(meetingId)
//                    .orElseThrow(() -> new RuntimeException("Spotkanie nie zostało znalezione"));
//
//            if (!meeting.getOrganizer().getId().equals(userDetails.getId())) {
//                throw new RuntimeException("Tylko organizator może tworzyć głosowania");
//            }
//
//            model.addAttribute("meetingId", meetingId);
//            model.addAttribute("createVotingRequest", new CreateVotingRequest());
//
//            return "meetings/votings/create";
//        } catch (Exception e) {
//            model.addAttribute("error", e.getMessage());
//            return "redirect:/meetings/" + meetingId + "/votings";
//        }
//    }


    @GetMapping("/create")
    public String showCreateVotingForm(@PathVariable Long meetingId,
                                       @AuthenticationPrincipal CustomUserDetails userDetails,
                                       Model model) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            MeetingResponse meeting = meetingService.getMeetingForVotingCreation(meetingId, userDetails.getId());

            model.addAttribute("meetingId", meetingId);
            model.addAttribute("createVotingRequest", new CreateVotingRequest());

            return "meetings/votings/create";

        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/meetings/" + meetingId + "/votings";
        }
    }


    // Tworzenie głosowania (POST)
    @PostMapping("/create")
    public String createVoting(@PathVariable Long meetingId,
                               @Valid @ModelAttribute CreateVotingRequest request,
                               BindingResult result,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }

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

    // Szczegóły głosowania
//    @GetMapping("/{votingId}")
//    public String getVotingDetails(@PathVariable Long meetingId,
//                                   @PathVariable Long votingId,
//                                   @AuthenticationPrincipal CustomUserDetails userDetails,
//                                   Model model) {
//        if (userDetails == null) {
//            return "redirect:/login";
//        }
//
//        try {
//            Meeting meeting = meetingRepository.findById(meetingId)
//                    .orElseThrow(() -> new RuntimeException("Spotkanie nie zostało znalezione"));
//
//            boolean isParticipant = meetingParticipantService.isParticipant(meetingId, userDetails.getId());
//            boolean isOrganizer = meeting.getOrganizer().getId().equals(userDetails.getId());
//
//            if (!isParticipant && !isOrganizer) {
//                throw new RuntimeException("Tylko uczestnicy spotkania mogą przeglądać głosowania");
//            }
//
//            // Zamknij wygasłe głosowanie jeśli potrzebne
//            votingService.closeExpiredVotingIfNeeded(votingId);
//
//            VotingResponse voting = votingService.getVotingDetails(votingId, userDetails.getId());
//
//            model.addAttribute("voting", voting);
//            model.addAttribute("meetingId", meetingId);
//            model.addAttribute("userId", userDetails.getId());
//            model.addAttribute("meeting", meeting);
//            model.addAttribute("isParticipant", isParticipant);
//            model.addAttribute("isOrganizer", isOrganizer);
//
//            return "meetings/votings/details";
//        } catch (Exception e) {
//            model.addAttribute("error", e.getMessage());
//            return "redirect:/meetings/" + meetingId;
//        }
//    }




    @GetMapping("/{votingId}")
    public String getVotingDetails(@PathVariable Long meetingId,
                                   @PathVariable Long votingId,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   Model model) {
        if (userDetails == null) return "redirect:/login";

        try {
            VotingResponse voting = votingService.getVotingDetailsForUser(votingId, userDetails.getId());
            MeetingResponse meeting = meetingService.getMeetingDetails(meetingId, userDetails.getId());

            model.addAttribute("voting", voting);
            model.addAttribute("meeting", meeting);
            model.addAttribute("meetingId", meetingId);
            model.addAttribute("userId", userDetails.getId());
            model.addAttribute("isParticipant", meeting.isUserIsParticipant());
            model.addAttribute("isOrganizer", meeting.isUserIsOrganizer());

            return "meetings/votings/details";
        } catch (VotingAccessDeniedException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/meetings/" + meetingId;
        }
    }





    // Oddanie głosu
//    @PostMapping("/{votingId}/vote")
//    public String submitVote(@PathVariable Long meetingId,
//                             @PathVariable Long votingId,
//                             @Valid @ModelAttribute VoteRequest request,
//                             BindingResult result,
//                             @AuthenticationPrincipal CustomUserDetails userDetails,
//                             RedirectAttributes redirectAttributes) {
//        if (userDetails == null) {
//            return "redirect:/login";
//        }
//
//        if (result.hasErrors()) {
//            redirectAttributes.addFlashAttribute("error", "Nieprawidłowe dane głosowania");
//            return "redirect:/meetings/" + meetingId + "/votings/" + votingId;
//        }
//
//        try {
//            boolean isParticipant = meetingParticipantService.isParticipant(meetingId, userDetails.getId());
//            boolean isOrganizer = meetingRepository.findById(meetingId)
//                    .orElseThrow(() -> new RuntimeException("Spotkanie nie zostało znalezione"))
//                    .getOrganizer().getId().equals(userDetails.getId());
//
//            if (!isParticipant && !isOrganizer) {
//                throw new RuntimeException("Tylko uczestnicy spotkania mogą głosować");
//            }
//
//            votingService.submitVote(votingId, request, userDetails.getId());
//            redirectAttributes.addFlashAttribute("success", "Twój głos został zapisany");
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("error", e.getMessage());
//        }
//
//        return "redirect:/meetings/" + meetingId + "/votings/" + votingId;
//    }



    @PostMapping("/{votingId}/vote")
    public String submitVote(@PathVariable Long meetingId,
                             @PathVariable Long votingId,
                             @Valid @ModelAttribute VoteRequest request,
                             BindingResult result,
                             @AuthenticationPrincipal CustomUserDetails userDetails,
                             RedirectAttributes redirectAttributes) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Nieprawidłowe dane głosowania");
            return "redirect:/meetings/" + meetingId + "/votings/" + votingId;
        }

        try {
            // Walidacja uprawnień i uczestnictwa
            votingService.validateUserCanVote(meetingId, votingId, userDetails.getId());

            // Zapis głosu
            votingService.submitVote(votingId, request, userDetails.getId());

            redirectAttributes.addFlashAttribute("success", "Twój głos został zapisany");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/meetings/" + meetingId + "/votings/" + votingId;
    }




    // Zamknięcie głosowania
    @PostMapping("/{votingId}/close")
    public String closeVoting(@PathVariable Long meetingId,
                              @PathVariable Long votingId,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            votingService.closeVoting(votingId, userDetails.getId());
            redirectAttributes.addFlashAttribute("success", "Głosowanie zostało zamknięte");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/meetings/" + meetingId + "/votings/" + votingId;
    }
}

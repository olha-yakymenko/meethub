package com.meethub.controller.web;

import com.meethub.domain.model.request.CreateVotingRequest;
import com.meethub.domain.model.request.VoteRequest;
import com.meethub.domain.model.response.MeetingResponse;
import com.meethub.domain.model.response.VotingResponse;
import com.meethub.domain.service.MeetingService;
import com.meethub.domain.service.MeetingVotingService;
import com.meethub.exception.VotingAccessDeniedException;
import com.meethub.security.CustomUserDetailsService.CustomUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Validated // DODANE - walidacja dla kontrolera webowego
@Slf4j
@Controller
@RequestMapping("/meetings/{meetingId}/votings")
@RequiredArgsConstructor
public class MeetingVotingController {

    private final MeetingVotingService votingService;
    private final MeetingService meetingService;

    // Lista wszystkich głosowań dla spotkania
    @GetMapping
    public String getMeetingVotings(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetails userDetails,
            Model model) {

        try {
            Long userId = userDetails.getId();
            log.info("Wyświetlanie listy głosowań dla spotkania ID={} przez użytkownika {}", meetingId, userId);

            List<VotingResponse> votings = votingService.getMeetingVotings(meetingId, userId);
            MeetingResponse meeting = meetingService.getMeetingDetails(meetingId, userId);

            model.addAttribute("votings", votings);
            model.addAttribute("meeting", meeting);
            model.addAttribute("meetingId", meetingId);
            model.addAttribute("userId", userId);
            model.addAttribute("isOrganizer", meeting.isUserIsOrganizer());

            log.info("Wyświetlono {} głosowań dla spotkania ID={}", votings.size(), meetingId);
            return "meetings/votings/list";

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji przy wyświetlaniu listy głosowań: {}", e.getMessage());
            model.addAttribute("error", "Nieprawidłowy identyfikator spotkania");
            return "redirect:/meetings";

        } catch (Exception e) {
            log.error("Błąd podczas wyświetlania listy głosowań dla spotkania {}: {}", meetingId, e.getMessage(), e);
            model.addAttribute("error", e.getMessage());
            return "redirect:/meetings/" + meetingId;
        }
    }

    // Formularz tworzenia głosowania
    @GetMapping("/create")
    public String showCreateVotingForm(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetails userDetails,
            Model model) {

        try {
            log.info("Wyświetlanie formularza tworzenia głosowania dla spotkania ID={} przez użytkownika {}",
                    meetingId, userDetails.getId());

            MeetingResponse meeting = meetingService.getMeetingForVotingCreation(meetingId, userDetails.getId());

            model.addAttribute("meetingId", meetingId);
            model.addAttribute("createVotingRequest", new CreateVotingRequest());

            return "meetings/votings/create";

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji przy wyświetlaniu formularza tworzenia głosowania: {}", e.getMessage());
            model.addAttribute("error", "Nieprawidłowy identyfikator spotkania");
            return "redirect:/meetings/" + meetingId + "/votings";

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Błąd uprawnień przy tworzeniu głosowania: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "redirect:/meetings/" + meetingId + "/votings";

        } catch (Exception e) {
            log.error("Błąd podczas wyświetlania formularza tworzenia głosowania dla spotkania {}: {}",
                    meetingId, e.getMessage(), e);
            model.addAttribute("error", "Wystąpił nieoczekiwany błąd");
            return "redirect:/meetings/" + meetingId + "/votings";
        }
    }

    // Tworzenie głosowania (POST)
    @PostMapping("/create")
    public String createVoting(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @Valid @ModelAttribute CreateVotingRequest request,
            BindingResult result,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetails userDetails,

            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            log.info("Tworzenie głosowania dla spotkania ID={} przez użytkownika {}", meetingId, userDetails.getId());

            if (result.hasErrors()) {
                log.warn("Błędy walidacji formularza tworzenia głosowania: {}", result.getAllErrors());
                model.addAttribute("meetingId", meetingId);
                return "meetings/votings/create";
            }

            VotingResponse voting = votingService.createVoting(meetingId, request, userDetails.getId());
            redirectAttributes.addFlashAttribute("success", "Głosowanie zostało utworzone");
            log.info("Głosowanie utworzone: ID={}, tytuł={}", voting.getId(), voting.getTitle());

            return "redirect:/meetings/" + meetingId + "/votings/" + voting.getId();

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji podczas tworzenia głosowania: {}", e.getMessage());
            model.addAttribute("error", "Nieprawidłowe dane w formularzu");
            model.addAttribute("meetingId", meetingId);
            return "meetings/votings/create";

        } catch (Exception e) {
            log.error("Błąd podczas tworzenia głosowania dla spotkania {}: {}", meetingId, e.getMessage(), e);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("meetingId", meetingId);
            return "meetings/votings/create";
        }
    }

    // Szczegóły głosowania
    @GetMapping("/{votingId}")
    public String getVotingDetails(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @PathVariable @NotNull(message = "Identyfikator głosowania nie może być pusty")
            @Min(value = 1, message = "Identyfikator głosowania musi być liczbą dodatnią")
            Long votingId,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetails userDetails,
            Model model) {

        try {
            log.info("Wyświetlanie szczegółów głosowania ID={} ze spotkania ID={} przez użytkownika {}",
                    votingId, meetingId, userDetails.getId());

            VotingResponse voting = votingService.getVotingDetailsForUser(votingId, userDetails.getId());
            MeetingResponse meeting = meetingService.getMeetingDetails(meetingId, userDetails.getId());

            model.addAttribute("voting", voting);
            model.addAttribute("meeting", meeting);
            model.addAttribute("meetingId", meetingId);
            model.addAttribute("userId", userDetails.getId());
            model.addAttribute("isParticipant", meeting.isUserIsParticipant());
            model.addAttribute("isOrganizer", meeting.isUserIsOrganizer());

            log.info("Wyświetlono szczegóły głosowania ID={}, status={}", votingId, voting.getStatus());
            return "meetings/votings/details";

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji przy wyświetlaniu szczegółów głosowania: {}", e.getMessage());
            model.addAttribute("error", "Nieprawidłowy identyfikator");
            return "redirect:/meetings/" + meetingId;

        } catch (VotingAccessDeniedException e) {
            log.warn("Brak dostępu do głosowania ID={} dla użytkownika {}: {}",
                    votingId, userDetails.getId(), e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "redirect:/meetings/" + meetingId;

        } catch (Exception e) {
            log.error("Błąd podczas wyświetlania szczegółów głosowania {}: {}", votingId, e.getMessage(), e);
            model.addAttribute("error", "Wystąpił błąd podczas ładowania głosowania");
            return "redirect:/meetings/" + meetingId;
        }
    }

    // Oddanie głosu
    @PostMapping("/{votingId}/vote")
    public String submitVote(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @PathVariable @NotNull(message = "Identyfikator głosowania nie może być pusty")
            @Min(value = 1, message = "Identyfikator głosowania musi być liczbą dodatnią")
            Long votingId,

            @Valid @ModelAttribute VoteRequest request,
            BindingResult result,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetails userDetails,

            RedirectAttributes redirectAttributes) {

        try {
            log.info("Oddawanie głosu w głosowaniu ID={} przez użytkownika {}", votingId, userDetails.getId());

            if (result.hasErrors()) {
                log.warn("Błędy walidacji formularza głosowania: {}", result.getAllErrors());
                redirectAttributes.addFlashAttribute("error", "Nieprawidłowe dane głosowania");
                return "redirect:/meetings/" + meetingId + "/votings/" + votingId;
            }

            // Walidacja uprawnień i uczestnictwa
            votingService.validateUserCanVote(meetingId, votingId, userDetails.getId());

            // Zapis głosu
            votingService.submitVote(votingId, request, userDetails.getId());

            redirectAttributes.addFlashAttribute("success", "Twój głos został zapisany");
            log.info("Głos użytkownika {} zapisany w głosowaniu ID={}", userDetails.getId(), votingId);

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji podczas oddawania głosu: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Nieprawidłowe dane głosowania");

        } catch (Exception e) {
            log.error("Błąd podczas oddawania głosu w głosowaniu {}: {}", votingId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/meetings/" + meetingId + "/votings/" + votingId;
    }

    // Zamknięcie głosowania
    @PostMapping("/{votingId}/close")
    public String closeVoting(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @PathVariable @NotNull(message = "Identyfikator głosowania nie może być pusty")
            @Min(value = 1, message = "Identyfikator głosowania musi być liczbą dodatnią")
            Long votingId,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            log.info("Zamykanie głosowania ID={} przez użytkownika {}", votingId, userDetails.getId());

            votingService.closeVoting(votingId, userDetails.getId());
            redirectAttributes.addFlashAttribute("success", "Głosowanie zostało zamknięte");
            log.info("Głosowanie ID={} zamknięte pomyślnie", votingId);

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji podczas zamykania głosowania: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Nieprawidłowy identyfikator głosowania");

        } catch (Exception e) {
            log.error("Błąd podczas zamykania głosowania {}: {}", votingId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/meetings/" + meetingId + "/votings/" + votingId;
    }
}












//package com.meethub.controller.web;
//
//import com.meethub.domain.model.entity.Meeting;
//import com.meethub.domain.model.entity.MeetingVoting;
//import com.meethub.domain.model.request.CreateVotingRequest;
//import com.meethub.domain.model.request.VoteRequest;
//import com.meethub.domain.model.response.MeetingResponse;
//import com.meethub.domain.model.response.VotingResponse;
//import com.meethub.domain.repository.jpa.MeetingRepository;
//import com.meethub.domain.service.MeetingParticipantService;
//import com.meethub.domain.service.MeetingService;
//import com.meethub.domain.service.MeetingVotingService;
//import com.meethub.exception.VotingAccessDeniedException;
//import com.meethub.security.CustomUserDetailsService.CustomUserDetails;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.validation.BindingResult;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.servlet.mvc.support.RedirectAttributes;
//
//import java.util.List;
//
//@Controller
//@RequestMapping("/meetings/{meetingId}/votings")
//@RequiredArgsConstructor
//public class MeetingVotingController {
//
//    private final MeetingVotingService votingService;
//    private final MeetingService meetingService;
//
//    // Lista wszystkich głosowań dla spotkania
//    @GetMapping
//    public String getMeetingVotings(@PathVariable Long meetingId,
//                                    @AuthenticationPrincipal CustomUserDetails userDetails,
//                                    Model model) {
//        if (userDetails == null) {
//            return "redirect:/login";
//        }
//        Long userId = userDetails.getId();
//
//        try {
//            List<VotingResponse> votings = votingService.getMeetingVotings(meetingId, userId);
//
//
////            Meeting meeting = meetingRepository.findById(meetingId)
////                    .orElseThrow(() -> new RuntimeException("Spotkanie nie zostało znalezione"));
////            boolean isOrganizer = meeting.getOrganizer().getId().equals(userDetails.getId());
////
////            model.addAttribute("votings", votings);
////            model.addAttribute("meetingId", meetingId);
////            model.addAttribute("userId", userDetails.getId());
////            model.addAttribute("isOrganizer", isOrganizer);
//
//            MeetingResponse meeting = meetingService.getMeetingDetails(meetingId, userId);
//
//            model.addAttribute("votings", votings);
//            model.addAttribute("meeting", meeting);
//            model.addAttribute("meetingId", meetingId);
//            model.addAttribute("userId", userId);
//            model.addAttribute("isOrganizer", meeting.isUserIsOrganizer());
//
//
//            return "meetings/votings/list";
//        } catch (Exception e) {
//            model.addAttribute("error", e.getMessage());
//            return "redirect:/meetings/" + meetingId;
//        }
//    }
//
//    // Formularz tworzenia głosowania
////    @GetMapping("/create")
////    public String showCreateVotingForm(@PathVariable Long meetingId,
////                                       @AuthenticationPrincipal CustomUserDetails userDetails,
////                                       Model model) {
////        if (userDetails == null) {
////            return "redirect:/login";
////        }
////
////        try {
////            Meeting meeting = meetingRepository.findById(meetingId)
////                    .orElseThrow(() -> new RuntimeException("Spotkanie nie zostało znalezione"));
////
////            if (!meeting.getOrganizer().getId().equals(userDetails.getId())) {
////                throw new RuntimeException("Tylko organizator może tworzyć głosowania");
////            }
////
////            model.addAttribute("meetingId", meetingId);
////            model.addAttribute("createVotingRequest", new CreateVotingRequest());
////
////            return "meetings/votings/create";
////        } catch (Exception e) {
////            model.addAttribute("error", e.getMessage());
////            return "redirect:/meetings/" + meetingId + "/votings";
////        }
////    }
//
//
//    @GetMapping("/create")
//    public String showCreateVotingForm(@PathVariable Long meetingId,
//                                       @AuthenticationPrincipal CustomUserDetails userDetails,
//                                       Model model) {
//
//        if (userDetails == null) {
//            return "redirect:/login";
//        }
//
//        try {
//            MeetingResponse meeting = meetingService.getMeetingForVotingCreation(meetingId, userDetails.getId());
//
//            model.addAttribute("meetingId", meetingId);
//            model.addAttribute("createVotingRequest", new CreateVotingRequest());
//
//            return "meetings/votings/create";
//
//        } catch (IllegalArgumentException | IllegalStateException e) {
//            model.addAttribute("error", e.getMessage());
//            return "redirect:/meetings/" + meetingId + "/votings";
//        }
//    }
//
//
//    // Tworzenie głosowania (POST)
//    @PostMapping("/create")
//    public String createVoting(@PathVariable Long meetingId,
//                               @Valid @ModelAttribute CreateVotingRequest request,
//                               BindingResult result,
//                               @AuthenticationPrincipal CustomUserDetails userDetails,
//                               Model model,
//                               RedirectAttributes redirectAttributes) {
//        if (userDetails == null) {
//            return "redirect:/login";
//        }
//
//        if (result.hasErrors()) {
//            model.addAttribute("meetingId", meetingId);
//            return "meetings/votings/create";
//        }
//
//        try {
//            VotingResponse voting = votingService.createVoting(meetingId, request, userDetails.getId());
//            redirectAttributes.addFlashAttribute("success", "Głosowanie zostało utworzone");
//            return "redirect:/meetings/" + meetingId + "/votings/" + voting.getId();
//        } catch (Exception e) {
//            model.addAttribute("error", e.getMessage());
//            model.addAttribute("meetingId", meetingId);
//            return "meetings/votings/create";
//        }
//    }
//
//    // Szczegóły głosowania
////    @GetMapping("/{votingId}")
////    public String getVotingDetails(@PathVariable Long meetingId,
////                                   @PathVariable Long votingId,
////                                   @AuthenticationPrincipal CustomUserDetails userDetails,
////                                   Model model) {
////        if (userDetails == null) {
////            return "redirect:/login";
////        }
////
////        try {
////            Meeting meeting = meetingRepository.findById(meetingId)
////                    .orElseThrow(() -> new RuntimeException("Spotkanie nie zostało znalezione"));
////
////            boolean isParticipant = meetingParticipantService.isParticipant(meetingId, userDetails.getId());
////            boolean isOrganizer = meeting.getOrganizer().getId().equals(userDetails.getId());
////
////            if (!isParticipant && !isOrganizer) {
////                throw new RuntimeException("Tylko uczestnicy spotkania mogą przeglądać głosowania");
////            }
////
////            // Zamknij wygasłe głosowanie jeśli potrzebne
////            votingService.closeExpiredVotingIfNeeded(votingId);
////
////            VotingResponse voting = votingService.getVotingDetails(votingId, userDetails.getId());
////
////            model.addAttribute("voting", voting);
////            model.addAttribute("meetingId", meetingId);
////            model.addAttribute("userId", userDetails.getId());
////            model.addAttribute("meeting", meeting);
////            model.addAttribute("isParticipant", isParticipant);
////            model.addAttribute("isOrganizer", isOrganizer);
////
////            return "meetings/votings/details";
////        } catch (Exception e) {
////            model.addAttribute("error", e.getMessage());
////            return "redirect:/meetings/" + meetingId;
////        }
////    }
//
//
//
//
//    @GetMapping("/{votingId}")
//    public String getVotingDetails(@PathVariable Long meetingId,
//                                   @PathVariable Long votingId,
//                                   @AuthenticationPrincipal CustomUserDetails userDetails,
//                                   Model model) {
//        if (userDetails == null) return "redirect:/login";
//
//        try {
//            VotingResponse voting = votingService.getVotingDetailsForUser(votingId, userDetails.getId());
//            MeetingResponse meeting = meetingService.getMeetingDetails(meetingId, userDetails.getId());
//
//            model.addAttribute("voting", voting);
//            model.addAttribute("meeting", meeting);
//            model.addAttribute("meetingId", meetingId);
//            model.addAttribute("userId", userDetails.getId());
//            model.addAttribute("isParticipant", meeting.isUserIsParticipant());
//            model.addAttribute("isOrganizer", meeting.isUserIsOrganizer());
//
//            return "meetings/votings/details";
//        } catch (VotingAccessDeniedException e) {
//            model.addAttribute("error", e.getMessage());
//            return "redirect:/meetings/" + meetingId;
//        }
//    }
//
//
//
//
//
//    // Oddanie głosu
////    @PostMapping("/{votingId}/vote")
////    public String submitVote(@PathVariable Long meetingId,
////                             @PathVariable Long votingId,
////                             @Valid @ModelAttribute VoteRequest request,
////                             BindingResult result,
////                             @AuthenticationPrincipal CustomUserDetails userDetails,
////                             RedirectAttributes redirectAttributes) {
////        if (userDetails == null) {
////            return "redirect:/login";
////        }
////
////        if (result.hasErrors()) {
////            redirectAttributes.addFlashAttribute("error", "Nieprawidłowe dane głosowania");
////            return "redirect:/meetings/" + meetingId + "/votings/" + votingId;
////        }
////
////        try {
////            boolean isParticipant = meetingParticipantService.isParticipant(meetingId, userDetails.getId());
////            boolean isOrganizer = meetingRepository.findById(meetingId)
////                    .orElseThrow(() -> new RuntimeException("Spotkanie nie zostało znalezione"))
////                    .getOrganizer().getId().equals(userDetails.getId());
////
////            if (!isParticipant && !isOrganizer) {
////                throw new RuntimeException("Tylko uczestnicy spotkania mogą głosować");
////            }
////
////            votingService.submitVote(votingId, request, userDetails.getId());
////            redirectAttributes.addFlashAttribute("success", "Twój głos został zapisany");
////        } catch (Exception e) {
////            redirectAttributes.addFlashAttribute("error", e.getMessage());
////        }
////
////        return "redirect:/meetings/" + meetingId + "/votings/" + votingId;
////    }
//
//
//
//    @PostMapping("/{votingId}/vote")
//    public String submitVote(@PathVariable Long meetingId,
//                             @PathVariable Long votingId,
//                             @Valid @ModelAttribute VoteRequest request,
//                             BindingResult result,
//                             @AuthenticationPrincipal CustomUserDetails userDetails,
//                             RedirectAttributes redirectAttributes) {
//
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
//            // Walidacja uprawnień i uczestnictwa
//            votingService.validateUserCanVote(meetingId, votingId, userDetails.getId());
//
//            // Zapis głosu
//            votingService.submitVote(votingId, request, userDetails.getId());
//
//            redirectAttributes.addFlashAttribute("success", "Twój głos został zapisany");
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("error", e.getMessage());
//        }
//
//        return "redirect:/meetings/" + meetingId + "/votings/" + votingId;
//    }
//
//
//
//
//    // Zamknięcie głosowania
//    @PostMapping("/{votingId}/close")
//    public String closeVoting(@PathVariable Long meetingId,
//                              @PathVariable Long votingId,
//                              @AuthenticationPrincipal CustomUserDetails userDetails,
//                              RedirectAttributes redirectAttributes) {
//        if (userDetails == null) {
//            return "redirect:/login";
//        }
//
//        try {
//            votingService.closeVoting(votingId, userDetails.getId());
//            redirectAttributes.addFlashAttribute("success", "Głosowanie zostało zamknięte");
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("error", e.getMessage());
//        }
//
//        return "redirect:/meetings/" + meetingId + "/votings/" + votingId;
//    }
//}

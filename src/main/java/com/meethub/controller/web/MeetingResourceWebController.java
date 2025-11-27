//package com.meethub.controller.web;
//
//import com.meethub.domain.model.request.MeetingResourceRequest;
//import com.meethub.domain.service.MeetingResourceService;
//import com.meethub.domain.service.MeetingService;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
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
//@RequestMapping("/meetings/{meetingId}/resources")
//@RequiredArgsConstructor
//@Slf4j
//public class MeetingResourceWebController {
//
//    private final MeetingService meetingService;
//    private final MeetingResourceService meetingResourceService;
//
//    @GetMapping("/add")
//    public String showAddResourceForm(@PathVariable Long meetingId,
//                                      Model model,
//                                      @AuthenticationPrincipal Long userId) {
//        try {
//            var meeting = meetingService.getMeeting(meetingId);
//            model.addAttribute("meeting", meeting);
//
//            if (!model.containsAttribute("meetingResourceRequest")) {
//                model.addAttribute("meetingResourceRequest", new MeetingResourceRequest());
//            }
//
//            return "meetings/resources/add-resource";
//        } catch (Exception e) {
//            log.error("Error showing add resource form for meeting: {}", meetingId, e);
//            return "redirect:/meetings?error=Spotkanie nie istnieje";
//        }
//    }
//
////    @PostMapping("/add")
////    public String addResource(@PathVariable Long meetingId,
////                              @ModelAttribute @Valid MeetingResourceRequest request,
////                              BindingResult result,
////                              Model model,
////                              RedirectAttributes redirectAttributes,
////                              @AuthenticationPrincipal Long userId) {
////
////        try {
////            var meeting = meetingService.getMeeting(meetingId);
////            model.addAttribute("meeting", meeting);
////
////            if (result.hasErrors()) {
////                log.warn("Validation errors for resource request: {}", result.getAllErrors());
////                return "meetings/resources/add-resource";
////            }
////
////            meetingResourceService.addResource(meetingId, request, userId);
////            redirectAttributes.addFlashAttribute("success", "Zasób został dodany pomyślnie");
////            return "redirect:/meetings/" + meetingId + "/resources";
////
////        } catch (Exception e) {
////            log.error("Error adding resource to meeting: {}", meetingId, e);
////            model.addAttribute("error", "Błąd podczas dodawania zasobu: " + e.getMessage());
////            return "meetings/resources/add-resource";
////        }
////    }
//
//
//    @PostMapping("/add")
//    public String addResource(@PathVariable Long meetingId,
//                              @ModelAttribute @Valid MeetingResourceRequest request,
//                              BindingResult result,
//                              Model model,
//                              RedirectAttributes redirectAttributes,
//                              @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) { // Zmiana tutaj
//
//        if (userDetails == null) {
//            return "redirect:/login";
//        }
//
//        Long userId = userDetails.getId();
//        log.info("Adding resource for user ID: {}", userId);
//
//        try {
//            var meeting = meetingService.getMeeting(meetingId);
//            model.addAttribute("meeting", meeting);
//
//            if (result.hasErrors()) {
//                log.warn("Validation errors for resource request: {}", result.getAllErrors());
//                return "meetings/resources/add-resource";
//            }
//
//            meetingResourceService.addResource(meetingId, request, userId);
//            redirectAttributes.addFlashAttribute("success", "Zasób został dodany pomyślnie");
//            return "redirect:/meetings/" + meetingId + "/resources";
//
//        } catch (Exception e) {
//            log.error("Error adding resource to meeting: {}", meetingId, e);
//            model.addAttribute("error", "Błąd podczas dodawania zasobu: " + e.getMessage());
//            return "meetings/resources/add-resource";
//        }
//    }
//
//
//    @GetMapping
//    public String getMeetingResources(@PathVariable Long meetingId,
//                                      Model model,
//                                      @AuthenticationPrincipal Long userId) {
//        try {
//            var meeting = meetingService.getMeeting(meetingId);
//            var resources = meetingResourceService.getMeetingResources(meetingId, userId);
//
//            model.addAttribute("meeting", meeting);
//            model.addAttribute("resources", resources);
//            model.addAttribute("resourcesCount", resources != null ? resources.size() : 0);
//
//            return "meetings/resources/resources-list";
//        } catch (Exception e) {
//            log.error("Error getting resources for meeting: {}", meetingId, e);
//            return "redirect:/meetings?error=Nie udało się pobrać zasobów";
//        }
//    }
//
//    @PostMapping("/{resourceId}/delete")
//    public String deleteResource(@PathVariable Long meetingId,
//                                 @PathVariable Long resourceId,
//                                 RedirectAttributes redirectAttributes,
//                                 @AuthenticationPrincipal Long userId) {
//        try {
//            meetingResourceService.deleteResource(resourceId, userId);
//            redirectAttributes.addFlashAttribute("success", "Zasób został usunięty pomyślnie");
//        } catch (Exception e) {
//            log.error("Error deleting resource: {}", resourceId, e);
//            redirectAttributes.addFlashAttribute("error", "Błąd podczas usuwania zasobu: " + e.getMessage());
//        }
//        return "redirect:/meetings/" + meetingId + "/resources";
//    }
//
//    @GetMapping("/{resourceId}/download")
//    public String downloadResourcePage(@PathVariable Long meetingId,
//                                       @PathVariable Long resourceId,
//                                       @AuthenticationPrincipal Long userId) {
//        // Przekierowanie do API endpoint do pobierania
//        return "redirect:/api/meetings/" + meetingId + "/resources/" + resourceId + "/download";
//    }
//
//    @GetMapping("/{resourceId}/preview")
//    public String previewResourcePage(@PathVariable Long meetingId,
//                                      @PathVariable Long resourceId,
//                                      @AuthenticationPrincipal Long userId) {
//        // Przekierowanie do API endpoint do podglądu
//        return "redirect:/api/meetings/" + meetingId + "/resources/" + resourceId + "/preview";
//    }
//}





package com.meethub.controller.web;

import com.meethub.domain.model.request.MeetingResourceRequest;
import com.meethub.domain.service.MeetingResourceService;
import com.meethub.domain.service.MeetingService;
import com.meethub.security.CustomUserDetailsService; // Dodaj ten import
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/meetings/{meetingId}/resources")
@RequiredArgsConstructor
@Slf4j
public class MeetingResourceWebController {

    private final MeetingService meetingService;
    private final MeetingResourceService meetingResourceService;

    @GetMapping("/add")
    public String showAddResourceForm(@PathVariable Long meetingId,
                                      Model model,
                                      @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) { // Zmiana tutaj
        try {
            if (userDetails == null) {
                return "redirect:/login";
            }

            var meeting = meetingService.getMeeting(meetingId);
            model.addAttribute("meeting", meeting);

            if (!model.containsAttribute("meetingResourceRequest")) {
                model.addAttribute("meetingResourceRequest", new MeetingResourceRequest());
            }

            return "meetings/resources/add-resource";
        } catch (Exception e) {
            log.error("Error showing add resource form for meeting: {}", meetingId, e);
            return "redirect:/meetings?error=Spotkanie nie istnieje";
        }
    }

    @PostMapping("/add")
    public String addResource(@PathVariable Long meetingId,
                              @ModelAttribute @Valid MeetingResourceRequest request,
                              BindingResult result,
                              Model model,
                              RedirectAttributes redirectAttributes,
                              @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        Long userId = userDetails.getId();
        log.info("Adding resource for user ID: {}", userId);

        try {
            var meeting = meetingService.getMeeting(meetingId);
            model.addAttribute("meeting", meeting);

            if (result.hasErrors()) {
                log.warn("Validation errors for resource request: {}", result.getAllErrors());
                return "meetings/resources/add-resource";
            }

            meetingResourceService.addResource(meetingId, request, userId);
            redirectAttributes.addFlashAttribute("success", "Zasób został dodany pomyślnie");
            return "redirect:/meetings/" + meetingId + "/resources";

        } catch (Exception e) {
            log.error("Error adding resource to meeting: {}", meetingId, e);
            model.addAttribute("error", "Błąd podczas dodawania zasobu: " + e.getMessage());
            return "meetings/resources/add-resource";
        }
    }

    @GetMapping
    public String getMeetingResources(@PathVariable Long meetingId,
                                      Model model,
                                      @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) { // Zmiana tutaj
        try {
            if (userDetails == null) {
                return "redirect:/login";
            }

            var meeting = meetingService.getMeeting(meetingId);
            var resources = meetingResourceService.getMeetingResources(meetingId, userDetails.getId());

            model.addAttribute("meeting", meeting);
            model.addAttribute("resources", resources);
            model.addAttribute("resourcesCount", resources != null ? resources.size() : 0);

            return "meetings/resources/resources-list";
        } catch (Exception e) {
            log.error("Error getting resources for meeting: {}", meetingId, e);
            return "redirect:/meetings?error=Nie udało się pobrać zasobów";
        }
    }

    @PostMapping("/{resourceId}/delete")
    public String deleteResource(@PathVariable Long meetingId,
                                 @PathVariable Long resourceId,
                                 RedirectAttributes redirectAttributes,
                                 @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) { // Zmiana tutaj
        try {
            if (userDetails == null) {
                return "redirect:/login";
            }

            meetingResourceService.deleteResource(resourceId, userDetails.getId());
            redirectAttributes.addFlashAttribute("success", "Zasób został usunięty pomyślnie");
        } catch (Exception e) {
            log.error("Error deleting resource: {}", resourceId, e);
            redirectAttributes.addFlashAttribute("error", "Błąd podczas usuwania zasobu: " + e.getMessage());
        }
        return "redirect:/meetings/" + meetingId + "/resources";
    }

    @GetMapping("/{resourceId}/download")
    public String downloadResourcePage(@PathVariable Long meetingId,
                                       @PathVariable Long resourceId,
                                       @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) { // Zmiana tutaj
        if (userDetails == null) {
            return "redirect:/login";
        }
        // Przekierowanie do API endpoint do pobierania
        return "redirect:/api/meetings/" + meetingId + "/resources/" + resourceId + "/download";
    }

    @GetMapping("/{resourceId}/preview")
    public String previewResourcePage(@PathVariable Long meetingId,
                                      @PathVariable Long resourceId,
                                      @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) { // Zmiana tutaj
        if (userDetails == null) {
            return "redirect:/login";
        }
        // Przekierowanie do API endpoint do podglądu
        return "redirect:/api/meetings/" + meetingId + "/resources/" + resourceId + "/preview";
    }
}
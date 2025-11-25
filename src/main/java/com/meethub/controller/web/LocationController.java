//package com.meethub.controller.web;
//
//import com.meethub.domain.model.request.CreateLocationRequest;
//import com.meethub.domain.model.request.LocationSearchRequest;
//import com.meethub.domain.model.request.UpdateLocationRequest;
//import com.meethub.domain.model.response.LocationListResponse;
//import com.meethub.domain.model.response.LocationResponse;
//import com.meethub.domain.service.LocationService;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.validation.BindingResult;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.servlet.mvc.support.RedirectAttributes;
//
//import java.math.BigDecimal;
//import java.util.List;
//
//@Slf4j
//@Controller
//@RequestMapping("/locations")
//@RequiredArgsConstructor
//public class LocationController {
//
//    private final LocationService locationService;
//
//    @GetMapping
//    public String listLocations(
//            @RequestParam(required = false) String query,
//            @RequestParam(required = false) String type,
//            @RequestParam(required = false) String city,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "20") int size,
//            Model model) {
//
//        LocationSearchRequest searchRequest = new LocationSearchRequest();
//        searchRequest.setQuery(query);
//        if (type != null) {
//            searchRequest.setType(com.meethub.domain.model.enums.LocationType.valueOf(type));
//        }
//        searchRequest.setCity(city);
//        searchRequest.setPage(page);
//        searchRequest.setSize(size);
//
//        LocationListResponse response = locationService.searchLocations(searchRequest);
//
//        model.addAttribute("locations", response.getLocations());
//        model.addAttribute("currentPage", response.getCurrentPage());
//        model.addAttribute("totalPages", response.getTotalPages());
//        model.addAttribute("totalItems", response.getTotalItems());
//        model.addAttribute("hasNext", response.isHasNext());
//        model.addAttribute("hasPrevious", response.isHasPrevious());
//        model.addAttribute("query", query);
//        model.addAttribute("type", type);
//        model.addAttribute("city", city);
//
//        return "locations/list";
//    }
//
//    @GetMapping("/create")
//    public String showCreateForm(Model model) {
//        model.addAttribute("locationRequest", new CreateLocationRequest());
//        return "locations/create";
//    }
//
//    @PostMapping("/create")
//    public String createLocation(
//            @Valid @ModelAttribute("locationRequest") CreateLocationRequest request,
//            BindingResult result,
//            RedirectAttributes redirectAttributes,
//            Model model) {
//
//        if (result.hasErrors()) {
//            return "locations/create";
//        }
//
//        try {
//            LocationResponse location = locationService.createLocation(request);
//            redirectAttributes.addFlashAttribute("success",
//                    "Lokalizacja '" + location.getName() + "' została utworzona pomyślnie");
//            return "redirect:/locations";
//        } catch (Exception e) {
//            log.error("Error creating location", e);
//            model.addAttribute("error", "Błąd podczas tworzenia lokalizacji: " + e.getMessage());
//            return "locations/create";
//        }
//    }
//
//    @GetMapping("/{id}")
//    public String locationDetails(@PathVariable Long id, Model model) {
//        try {
//            LocationResponse location = locationService.getLocation(id);
//            String mapUrl = locationService.generateMapUrl(id);
//
//            model.addAttribute("location", location);
//            model.addAttribute("mapUrl", mapUrl);
//            return "locations/details.html";
//        } catch (Exception e) {
//            log.error("Error getting location details.html", e);
//            return "redirect:/locations?error=Lokalizacja nie znaleziona";
//        }
//    }
//
//    @GetMapping("/{id}/edit")
//    public String showEditForm(@PathVariable Long id, Model model) {
//        try {
//            LocationResponse location = locationService.getLocation(id);
//            UpdateLocationRequest updateRequest = new UpdateLocationRequest();
//
//            // Mapowanie pól
//            updateRequest.setName(location.getName());
//            updateRequest.setType(location.getType());
//            updateRequest.setAddress(location.getAddress());
//            updateRequest.setCity(location.getCity());
//            updateRequest.setCountry(location.getCountry());
//            updateRequest.setLatitude(location.getLatitude());
//            updateRequest.setLongitude(location.getLongitude());
//            updateRequest.setVirtualMeetingUrl(location.getVirtualMeetingUrl());
//            updateRequest.setAccessCode(location.getAccessCode());
//            updateRequest.setDrivingInstructions(location.getDrivingInstructions());
//            updateRequest.setTimezone(location.getTimezone());
//
//            model.addAttribute("locationId", id);
//            model.addAttribute("locationRequest", updateRequest);
//            return "locations/edit";
//        } catch (Exception e) {
//            log.error("Error loading edit form", e);
//            return "redirect:/locations?error=Lokalizacja nie znaleziona";
//        }
//    }
//
//    @PostMapping("/{id}/edit")
//    public String updateLocation(
//            @PathVariable Long id,
//            @Valid @ModelAttribute("locationRequest") UpdateLocationRequest request,
//            BindingResult result,
//            RedirectAttributes redirectAttributes,
//            Model model) {
//
//        if (result.hasErrors()) {
//            model.addAttribute("locationId", id);
//            return "locations/edit";
//        }
//
//        try {
//            LocationResponse location = locationService.updateLocation(id, request);
//            redirectAttributes.addFlashAttribute("success",
//                    "Lokalizacja '" + location.getName() + "' została zaktualizowana");
//            return "redirect:/locations/" + id;
//        } catch (Exception e) {
//            log.error("Error updating location", e);
//            model.addAttribute("error", "Błąd podczas aktualizacji: " + e.getMessage());
//            model.addAttribute("locationId", id);
//            return "locations/edit";
//        }
//    }
//
//    @PostMapping("/{id}/delete")
//    public String deleteLocation(
//            @PathVariable Long id,
//            RedirectAttributes redirectAttributes) {
//
//        try {
//            locationService.deleteLocation(id);
//            redirectAttributes.addFlashAttribute("success", "Lokalizacja została usunięta");
//        } catch (Exception e) {
//            log.error("Error deleting location", e);
//            redirectAttributes.addFlashAttribute("error",
//                    "Błąd podczas usuwania: " + e.getMessage());
//        }
//
//        return "redirect:/locations";
//    }
//
//    @GetMapping("/nearby")
//    public String findNearbyLocations(
//            @RequestParam BigDecimal lat,
//            @RequestParam BigDecimal lng,
//            @RequestParam(defaultValue = "5.0") Double radius,
//            Model model) {
//
//        try {
//            List<LocationResponse> locations = locationService.findNearbyLocations(lat, lng, radius);
//            model.addAttribute("locations", locations);
//            model.addAttribute("centerLat", lat);
//            model.addAttribute("centerLng", lng);
//            model.addAttribute("radius", radius);
//            return "locations/nearby";
//        } catch (Exception e) {
//            log.error("Error finding nearby locations", e);
//            return "redirect:/locations?error=Błąd wyszukiwania lokalizacji";
//        }
//    }
//
//    @GetMapping("/{id}/map")
//    public String showOnMap(@PathVariable Long id, Model model) {
//        try {
//            LocationResponse location = locationService.getLocation(id);
//            String mapUrl = locationService.generateMapUrl(id);
//            String directionsUrl = locationService.generateDirectionsUrl(id, "");
//
//            model.addAttribute("location", location);
//            model.addAttribute("mapUrl", mapUrl);
//            model.addAttribute("directionsUrl", directionsUrl);
//            return "locations/map";
//        } catch (Exception e) {
//            log.error("Error showing map", e);
//            return "redirect:/locations?error=Błąd ładowania mapy";
//        }
//    }
//
//    @GetMapping("/virtual/generate")
//    public String generateVirtualLocation(
//            @RequestParam String platform,
//            @RequestParam String meetingId,
//            @RequestParam(required = false) String passcode,
//            RedirectAttributes redirectAttributes) {
//
//        try {
//            LocationResponse location = locationService.generateVirtualLocation(platform, meetingId, passcode);
//            redirectAttributes.addFlashAttribute("success",
//                    "Lokalizacja wirtualna została wygenerowana");
//            return "redirect:/locations/" + location.getId();
//        } catch (Exception e) {
//            log.error("Error generating virtual location", e);
//            redirectAttributes.addFlashAttribute("error",
//                    "Błąd generowania lokalizacji: " + e.getMessage());
//            return "redirect:/locations";
//        }
//    }
//}












package com.meethub.controller.web;

import com.meethub.domain.model.enums.LocationType;
import com.meethub.domain.model.request.CreateLocationRequest;
import com.meethub.domain.model.request.LocationSearchRequest;
import com.meethub.domain.model.request.UpdateLocationRequest;
import com.meethub.domain.model.response.LocationListResponse;
import com.meethub.domain.model.response.LocationResponse;
import com.meethub.domain.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    // ✅ POPRAWIONE: Lista lokalizacji - GET /locations
    @GetMapping
    public String listLocations(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String city,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {

        try {
            LocationSearchRequest searchRequest = new LocationSearchRequest();
            searchRequest.setQuery(query);
            if (type != null) {
                try {
                    searchRequest.setType(com.meethub.domain.model.enums.LocationType.valueOf(type));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid location type: {}", type);
                }
            }
            searchRequest.setCity(city);
            searchRequest.setPage(page);
            searchRequest.setSize(size);

            LocationListResponse response = locationService.searchLocations(searchRequest);

            model.addAttribute("locations", response.getLocations());
            model.addAttribute("currentPage", response.getCurrentPage());
            model.addAttribute("totalPages", response.getTotalPages());
            model.addAttribute("totalItems", response.getTotalItems());
            model.addAttribute("hasNext", response.isHasNext());
            model.addAttribute("hasPrevious", response.isHasPrevious());
            model.addAttribute("query", query);
            model.addAttribute("type", type);
            model.addAttribute("city", city);

            return "locations/list";
        } catch (Exception e) {
            log.error("Error loading locations list", e);
            model.addAttribute("error", "Błąd ładowania listy lokalizacji: " + e.getMessage());
            return "locations/list";
        }
    }

    // ✅ POPRAWIONE: Formularz tworzenia - GET /locations/create
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("locationRequest", new CreateLocationRequest());
        return "locations/create";
    }

    @PostMapping("/create")
    public String createLocation(
            @Valid @ModelAttribute("locationRequest") CreateLocationRequest request,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {

        log.info("Creating location with name: {}, type: {}", request.getName(), request.getType());

        // Podstawowa walidacja
        if (result.hasErrors()) {
            log.warn("Validation errors: {}", result.getAllErrors());
            return "locations/create";
        }

        // ✅ WALIDACJA DODATKOWA DLA LOKALIZACJI FIZYCZNEJ
        if (request.getType() == LocationType.PHYSICAL) {
            if (request.getAddress() == null || request.getAddress().trim().isEmpty()) {
                result.rejectValue("address", "NotEmpty", "Adres jest wymagany dla lokalizacji fizycznej");
                model.addAttribute("error", "Adres jest wymagany dla lokalizacji fizycznej");
            }
            if (request.getCity() == null || request.getCity().trim().isEmpty()) {
                result.rejectValue("city", "NotEmpty", "Miasto jest wymagane dla lokalizacji fizycznej");
                model.addAttribute("error", "Miasto jest wymagane dla lokalizacji fizycznej");
            }
        }

        // ✅ WALIDACJA DODATKOWA DLA LOKALIZACJI WIRTUALNEJ
        if (request.getType() == LocationType.VIRTUAL) {
            if (request.getVirtualMeetingUrl() == null || request.getVirtualMeetingUrl().trim().isEmpty()) {
                result.rejectValue("virtualMeetingUrl", "NotEmpty", "URL spotkania jest wymagany dla lokalizacji wirtualnej");
                model.addAttribute("error", "URL spotkania jest wymagany dla lokalizacji wirtualnej");
            }
        }

        if (result.hasErrors()) {
            return "locations/create";
        }

        try {
            LocationResponse location = locationService.createLocation(request);
            redirectAttributes.addFlashAttribute("success",
                    "Lokalizacja '" + location.getName() + "' została utworzona pomyślnie");
            return "redirect:/locations";
        } catch (Exception e) {
            log.error("Error creating location", e);
            model.addAttribute("error", "Błąd podczas tworzenia lokalizacji: " + e.getMessage());
            return "locations/create";
        }
    }

    @GetMapping("/{id}")
    public String locationDetails(@PathVariable Long id, Model model) {
        try {
            LocationResponse location = locationService.getLocation(id);
            String mapUrl = locationService.generateMapUrl(id);

            model.addAttribute("location", location);
            model.addAttribute("mapUrl", mapUrl);
            return "locations/details"; // ✅ USUNIĘTE .html
        } catch (Exception e) {
            log.error("Error getting location details for ID: {}", id, e);
            return "redirect:/locations?error=Lokalizacja nie znaleziona";
        }
    }

    // ✅ POPRAWIONE: Formularz edycji - GET /locations/{id}/edit
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        try {
            LocationResponse location = locationService.getLocation(id);
            UpdateLocationRequest updateRequest = new UpdateLocationRequest();

            // Mapowanie pól
            updateRequest.setName(location.getName());
            updateRequest.setType(location.getType());
            updateRequest.setAddress(location.getAddress());
            updateRequest.setCity(location.getCity());
            updateRequest.setCountry(location.getCountry());
            updateRequest.setLatitude(location.getLatitude());
            updateRequest.setLongitude(location.getLongitude());
            updateRequest.setVirtualMeetingUrl(location.getVirtualMeetingUrl());
            updateRequest.setAccessCode(location.getAccessCode());
            updateRequest.setDrivingInstructions(location.getDrivingInstructions());
            updateRequest.setTimezone(location.getTimezone());

            model.addAttribute("locationId", id);
            model.addAttribute("locationRequest", updateRequest);
            return "locations/edit";
        } catch (Exception e) {
            log.error("Error loading edit form for ID: {}", id, e);
            return "redirect:/locations?error=Lokalizacja nie znaleziona";
        }
    }

    // ✅ POPRAWIONE: Aktualizacja lokalizacji - POST /locations/{id}/edit
    @PostMapping("/{id}/edit")
    public String updateLocation(
            @PathVariable Long id,
            @Valid @ModelAttribute("locationRequest") UpdateLocationRequest request,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("locationId", id);
            return "locations/edit";
        }

        try {
            LocationResponse location = locationService.updateLocation(id, request);
            redirectAttributes.addFlashAttribute("success",
                    "Lokalizacja '" + location.getName() + "' została zaktualizowana");
            return "redirect:/locations/" + id;
        } catch (Exception e) {
            log.error("Error updating location ID: {}", id, e);
            model.addAttribute("error", "Błąd podczas aktualizacji: " + e.getMessage());
            model.addAttribute("locationId", id);
            return "locations/edit";
        }
    }

    // ✅ POPRAWIONE: Usuwanie lokalizacji - POST /locations/{id}/delete
    @PostMapping("/{id}/delete")
    public String deleteLocation(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {
            locationService.deleteLocation(id);
            redirectAttributes.addFlashAttribute("success", "Lokalizacja została usunięta");
        } catch (Exception e) {
            log.error("Error deleting location ID: {}", id, e);
            redirectAttributes.addFlashAttribute("error",
                    "Błąd podczas usuwania: " + e.getMessage());
        }

        return "redirect:/locations";
    }

    // ✅ POPRAWIONE: Lokalizacje w pobliżu - GET /locations/nearby
    @GetMapping("/nearby")
    public String findNearbyLocations(
            @RequestParam(required = false) BigDecimal lat,
            @RequestParam(required = false) BigDecimal lng,
            @RequestParam(defaultValue = "5.0") Double radius,
            Model model) {

        // Domyślne współrzędne (Warszawa)
        if (lat == null) lat = new BigDecimal("52.2297");
        if (lng == null) lng = new BigDecimal("21.0122");

        try {
            List<LocationResponse> locations = locationService.findNearbyLocations(lat, lng, radius);
            model.addAttribute("locations", locations);
            model.addAttribute("centerLat", lat);
            model.addAttribute("centerLng", lng);
            model.addAttribute("radius", radius);
            return "locations/nearby";
        } catch (Exception e) {
            log.error("Error finding nearby locations", e);
            model.addAttribute("error", "Błąd wyszukiwania lokalizacji: " + e.getMessage());
            model.addAttribute("centerLat", lat);
            model.addAttribute("centerLng", lng);
            model.addAttribute("radius", radius);
            model.addAttribute("locations", List.of());
            return "locations/nearby";
        }
    }

    // ✅ POPRAWIONE: Mapa lokalizacji - GET /locations/{id}/map
    @GetMapping("/{id}/map")
    public String showOnMap(@PathVariable Long id, Model model) {
        try {
            LocationResponse location = locationService.getLocation(id);
            String mapUrl = locationService.generateMapUrl(id);
            String directionsUrl = locationService.generateDirectionsUrl(id, "");

            model.addAttribute("location", location);
            model.addAttribute("mapUrl", mapUrl);
            model.addAttribute("directionsUrl", directionsUrl);
            return "locations/map";
        } catch (Exception e) {
            log.error("Error showing map for location ID: {}", id, e);
            return "redirect:/locations?error=Błąd ładowania mapy";
        }
    }

    // ✅ POPRAWIONE: Generowanie lokalizacji wirtualnej - GET /locations/virtual/generate
    @GetMapping("/virtual/generate")
    public String generateVirtualLocation(
            @RequestParam String platform,
            @RequestParam String meetingId,
            @RequestParam(required = false) String passcode,
            RedirectAttributes redirectAttributes) {

        try {
            LocationResponse location = locationService.generateVirtualLocation(platform, meetingId, passcode);
            redirectAttributes.addFlashAttribute("success",
                    "Lokalizacja wirtualna została wygenerowana");
            return "redirect:/locations/" + location.getId();
        } catch (Exception e) {
            log.error("Error generating virtual location", e);
            redirectAttributes.addFlashAttribute("error",
                    "Błąd generowania lokalizacji: " + e.getMessage());
            return "redirect:/locations";
        }
    }
}
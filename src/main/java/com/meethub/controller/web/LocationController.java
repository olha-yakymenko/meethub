package com.meethub.controller.web;

import com.meethub.domain.model.enums.LocationType;
import com.meethub.domain.model.request.CreateLocationRequest;
import com.meethub.domain.model.request.LocationSearchRequest;
import com.meethub.domain.model.request.UpdateLocationRequest;
import com.meethub.domain.model.response.LocationListResponse;
import com.meethub.domain.model.response.LocationResponse;
import com.meethub.domain.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Validated // DODANE - walidacja dla kontrolera webowego
@Slf4j
@Controller
@RequestMapping("/locations")
@RequiredArgsConstructor
@Tag(name = "Lokalizacje", description = "Strony web do zarządzania lokalizacjami spotkań")
public class LocationController {

    private final LocationService locationService;

    @GetMapping
    @Operation(summary = "Lista lokalizacji",
            description = "Wyświetla listę lokalizacji z możliwością filtrowania po nazwie, typie i mieście oraz paginacją.")
    public String listLocations(
            @RequestParam(required = false)
            @Size(max = 100, message = "Zapytanie wyszukiwania nie może przekraczać 100 znaków")
            String query,

            @RequestParam(required = false)
            @Pattern(regexp = "PHYSICAL|VIRTUAL|HYBRID", message = "Nieprawidłowy typ lokalizacji")
            String type,

            @RequestParam(required = false)
            @Size(max = 50, message = "Nazwa miasta nie może przekraczać 50 znaków")
            String city,

            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Numer strony nie może być ujemny")
            int page,

            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Rozmiar strony musi być co najmniej 1")
            @Max(value = 100, message = "Rozmiar strony nie może przekraczać 100")
            int size,
            Model model) {

        try {
            log.info("Wyświetlanie listy lokalizacji: query={}, type={}, city={}, page={}, size={}",
                    query, type, city, page, size);

            LocationSearchRequest searchRequest = new LocationSearchRequest();
            searchRequest.setQuery(query);
            if (type != null) {
                try {
                    searchRequest.setType(LocationType.valueOf(type));
                } catch (IllegalArgumentException e) {
                    log.warn("Nieprawidłowy typ lokalizacji: {}", type);
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

            log.info("Wyświetlono {} lokalizacji na stronie {}", response.getLocations().size(), page);
            return "locations/list";

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji parametrów wyszukiwania: {}", e.getMessage());
            model.addAttribute("error", "Nieprawidłowe parametry wyszukiwania");
            return "locations/list";

        } catch (Exception e) {
            log.error("Błąd ładowania listy lokalizacji", e);
            model.addAttribute("error", "Błąd ładowania listy lokalizacji: " + e.getMessage());
            return "locations/list";
        }
    }

    @GetMapping("/create")
    @Operation(summary = "Formularz dodania lokalizacji",
            description = "Wyświetla formularz do tworzenia nowej lokalizacji (fizycznej lub wirtualnej).")
    public String showCreateForm(Model model) {
        model.addAttribute("locationRequest", new CreateLocationRequest());
        return "locations/create";
    }

    @PostMapping("/create")
    @Operation(summary = "Tworzenie lokalizacji",
            description = "Tworzy nową lokalizację na podstawie danych z formularza i zapisuje ją w systemie.")
    public String createLocation(
            @Valid @ModelAttribute("locationRequest") CreateLocationRequest request,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {

        log.info("Tworzenie lokalizacji: nazwa={}, typ={}", request.getName(), request.getType());

        if (result.hasErrors()) {
            log.warn("Błędy walidacji formularza: {}", result.getAllErrors());
            return "locations/create";
        }

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
            log.info("Lokalizacja utworzona: ID={}, nazwa={}", location.getId(), location.getName());
            return "redirect:/locations";

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji podczas tworzenia lokalizacji: {}", e.getMessage());
            model.addAttribute("error", "Nieprawidłowe dane w formularzu");
            return "locations/create";

        } catch (Exception e) {
            log.error("Błąd podczas tworzenia lokalizacji", e);
            model.addAttribute("error", "Błąd podczas tworzenia lokalizacji: " + e.getMessage());
            return "locations/create";
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Szczegóły lokalizacji",
            description = "Wyświetla szczegóły lokalizacji oraz mapę jej położenia.")
    public String locationDetails(
            @PathVariable @NotNull(message = "Identyfikator lokalizacji nie może być pusty")
            @Min(value = 1, message = "Identyfikator lokalizacji musi być liczbą dodatnią")
            Long id,
            Model model) {

        try {
            log.info("Wyświetlanie szczegółów lokalizacji ID={}", id);
            LocationResponse location = locationService.getLocation(id);
            String mapUrl = locationService.generateMapUrl(id);

            model.addAttribute("location", location);
            model.addAttribute("mapUrl", mapUrl);
            log.info("Wyświetlono szczegóły lokalizacji ID={}, nazwa={}", id, location.getName());
            return "locations/details";

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji ID lokalizacji: {}", id);
            model.addAttribute("error", "Nieprawidłowy identyfikator lokalizacji");
            return "redirect:/locations";

        } catch (Exception e) {
            log.error("Błąd pobierania szczegółów lokalizacji ID: {}", id, e);
            return "redirect:/locations?error=Lokalizacja nie znaleziona";
        }
    }

    @GetMapping("/{id}/edit")
    @Operation(summary = "Formularz edycji lokalizacji",
            description = "Wyświetla formularz umożliwiający edycję danych istniejącej lokalizacji.")
    public String showEditForm(
            @PathVariable @NotNull(message = "Identyfikator lokalizacji nie może być pusty")
            @Min(value = 1, message = "Identyfikator lokalizacji musi być liczbą dodatnią")
            Long id,
            Model model) {

        try {
            log.info("Ładowanie formularza edycji dla lokalizacji ID={}", id);
            LocationResponse location = locationService.getLocation(id);
            UpdateLocationRequest updateRequest = new UpdateLocationRequest();

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

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji ID lokalizacji: {}", id);
            return "redirect:/locations?error=Nieprawidłowy identyfikator lokalizacji";

        } catch (Exception e) {
            log.error("Błąd ładowania formularza edycji dla ID: {}", id, e);
            return "redirect:/locations?error=Lokalizacja nie znaleziona";
        }
    }

    @PostMapping("/{id}/edit")
    @Operation(summary = "Aktualizacja lokalizacji",
            description = "Aktualizuje dane lokalizacji na podstawie formularza edycji.")
    public String updateLocation(
            @PathVariable @NotNull(message = "Identyfikator lokalizacji nie może być pusty")
            @Min(value = 1, message = "Identyfikator lokalizacji musi być liczbą dodatnią")
            Long id,

            @Valid @ModelAttribute("locationRequest") UpdateLocationRequest request,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (result.hasErrors()) {
            log.warn("Błędy walidacji formularza edycji lokalizacji ID={}: {}", id, result.getAllErrors());
            model.addAttribute("locationId", id);
            return "locations/edit";
        }

        try {
            log.info("Aktualizacja lokalizacji ID={}", id);
            LocationResponse location = locationService.updateLocation(id, request);
            redirectAttributes.addFlashAttribute("success",
                    "Lokalizacja '" + location.getName() + "' została zaktualizowana");
            log.info("Lokalizacja ID={} zaktualizowana pomyślnie", id);
            return "redirect:/locations/" + id;

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji podczas aktualizacji lokalizacji ID={}: {}", id, e.getMessage());
            model.addAttribute("error", "Nieprawidłowe dane w formularzu");
            model.addAttribute("locationId", id);
            return "locations/edit";

        } catch (Exception e) {
            log.error("Błąd aktualizacji lokalizacji ID: {}", id, e);
            model.addAttribute("error", "Błąd podczas aktualizacji: " + e.getMessage());
            model.addAttribute("locationId", id);
            return "locations/edit";
        }
    }

    @PostMapping("/{id}/delete")
    @Operation(summary = "Usuwanie lokalizacji",
            description = "Usuwa lokalizację o podanym identyfikatorze.")
    public String deleteLocation(
            @PathVariable @NotNull(message = "Identyfikator lokalizacji nie może być pusty")
            @Min(value = 1, message = "Identyfikator lokalizacji musi być liczbą dodatnią")
            Long id,
            RedirectAttributes redirectAttributes) {

        try {
            log.info("Usuwanie lokalizacji ID={}", id);
            locationService.deleteLocation(id);
            redirectAttributes.addFlashAttribute("success", "Lokalizacja została usunięta");
            log.info("Lokalizacja ID={} usunięta pomyślnie", id);

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji ID lokalizacji do usunięcia: {}", id);
            redirectAttributes.addFlashAttribute("error", "Nieprawidłowy identyfikator lokalizacji");

        } catch (Exception e) {
            log.error("Błąd usuwania lokalizacji ID: {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Błąd podczas usuwania: " + e.getMessage());
        }
        return "redirect:/locations";
    }

    @GetMapping("/nearby")
    @Operation(summary = "Lokalizacje w pobliżu",
            description = "Wyświetla listę lokalizacji w pobliżu podanych współrzędnych geograficznych.")
    public String findNearbyLocations(
            @RequestParam(required = false)
            @DecimalMin(value = "-90.0", message = "Szerokość geograficzna musi być między -90.0 a 90.0")
            @DecimalMax(value = "90.0", message = "Szerokość geograficzna musi być między -90.0 a 90.0")
            BigDecimal lat,

            @RequestParam(required = false)
            @DecimalMin(value = "-180.0", message = "Długość geograficzna musi być między -180.0 a 180.0")
            @DecimalMax(value = "180.0", message = "Długość geograficzna musi być między -180.0 a 180.0")
            BigDecimal lng,

            @RequestParam(defaultValue = "5.0")
            @DecimalMin(value = "0.1", message = "Promień musi być co najmniej 0.1 km")
            @DecimalMax(value = "100.0", message = "Promień nie może przekraczać 100 km")
            Double radius,
            Model model) {

        if (lat == null) lat = new BigDecimal("52.2297");
        if (lng == null) lng = new BigDecimal("21.0122");

        try {
            log.info("Wyszukiwanie lokalizacji w pobliżu: lat={}, lng={}, radius={}km", lat, lng, radius);
            List<LocationResponse> locations = locationService.findNearbyLocations(lat, lng, radius);
            model.addAttribute("locations", locations);
            model.addAttribute("centerLat", lat);
            model.addAttribute("centerLng", lng);
            model.addAttribute("radius", radius);
            log.info("Znaleziono {} lokalizacji w pobliżu", locations.size());
            return "locations/nearby";

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji parametrów wyszukiwania w pobliżu: {}", e.getMessage());
            model.addAttribute("error", "Nieprawidłowe parametry wyszukiwania");
            model.addAttribute("centerLat", lat);
            model.addAttribute("centerLng", lng);
            model.addAttribute("radius", radius);
            model.addAttribute("locations", List.of());
            return "locations/nearby";

        } catch (Exception e) {
            log.error("Błąd wyszukiwania lokalizacji w pobliżu", e);
            model.addAttribute("error", "Błąd wyszukiwania lokalizacji: " + e.getMessage());
            model.addAttribute("centerLat", lat);
            model.addAttribute("centerLng", lng);
            model.addAttribute("radius", radius);
            model.addAttribute("locations", List.of());
            return "locations/nearby";
        }
    }

    @GetMapping("/{id}/map")
    @Operation(summary = "Wyświetlanie lokalizacji na mapie",
            description = "Wyświetla lokalizację na mapie oraz link do wskazówek dojazdu.")
    public String showOnMap(
            @PathVariable @NotNull(message = "Identyfikator lokalizacji nie może być pusty")
            @Min(value = 1, message = "Identyfikator lokalizacji musi być liczbą dodatnią")
            Long id,
            Model model) {

        try {
            log.info("Wyświetlanie mapy dla lokalizacji ID={}", id);
            LocationResponse location = locationService.getLocation(id);
            String mapUrl = locationService.generateMapUrl(id);
            String directionsUrl = locationService.generateDirectionsUrl(id, "");

            model.addAttribute("location", location);
            model.addAttribute("mapUrl", mapUrl);
            model.addAttribute("directionsUrl", directionsUrl);
            return "locations/map";

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji ID lokalizacji do mapy: {}", id);
            return "redirect:/locations?error=Nieprawidłowy identyfikator lokalizacji";

        } catch (Exception e) {
            log.error("Błąd wyświetlania mapy dla lokalizacji ID: {}", id, e);
            return "redirect:/locations?error=Błąd ładowania mapy";
        }
    }

    @GetMapping("/virtual/generate")
    @Operation(summary = "Generowanie lokalizacji wirtualnej",
            description = "Tworzy lokalizację wirtualną dla spotkania na wybranej platformie i opcjonalnym kodzie dostępu.")
    public String generateVirtualLocation(
            @RequestParam @NotBlank(message = "Platforma nie może być pusta")
            @Size(min = 1, max = 50, message = "Platforma musi mieć od 1 do 50 znaków")
            String platform,

            @RequestParam @NotBlank(message = "Identyfikator spotkania nie może być pusty")
            @Size(min = 1, max = 100, message = "Identyfikator spotkania musi mieć od 1 do 100 znaków")
            String meetingId,

            @RequestParam(required = false)
            @Size(max = 50, message = "Kod dostępu nie może przekraczać 50 znaków")
            String passcode,
            RedirectAttributes redirectAttributes) {

        try {
            log.info("Generowanie lokalizacji wirtualnej: platforma={}, meetingId={}", platform, meetingId);
            LocationResponse location = locationService.generateVirtualLocation(platform, meetingId, passcode);
            redirectAttributes.addFlashAttribute("success",
                    "Lokalizacja wirtualna została wygenerowana");
            log.info("Lokalizacja wirtualna wygenerowana: ID={}", location.getId());
            return "redirect:/locations/" + location.getId();

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji parametrów generowania lokalizacji wirtualnej: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Nieprawidłowe parametry generowania");
            return "redirect:/locations";

        } catch (Exception e) {
            log.error("Błąd generowania lokalizacji wirtualnej", e);
            redirectAttributes.addFlashAttribute("error",
                    "Błąd generowania lokalizacji: " + e.getMessage());
            return "redirect:/locations";
        }
    }
}










//package com.meethub.controller.web;
//
//import com.meethub.domain.model.enums.LocationType;
//import com.meethub.domain.model.request.CreateLocationRequest;
//import com.meethub.domain.model.request.LocationSearchRequest;
//import com.meethub.domain.model.request.UpdateLocationRequest;
//import com.meethub.domain.model.response.LocationListResponse;
//import com.meethub.domain.model.response.LocationResponse;
//import com.meethub.domain.service.LocationService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
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
//@Tag(name = "Lokalizacje", description = "Zarządzanie lokalizacjami spotkań i lokalizacjami wirtualnymi")
//public class LocationController {
//
//    private final LocationService locationService;
//
//    @GetMapping
//    @Operation(summary = "Lista lokalizacji",
//            description = "Wyświetla listę lokalizacji z możliwością filtrowania po nazwie, typie i mieście oraz paginacją.")
//    public String listLocations(
//            @RequestParam(required = false) String query,
//            @RequestParam(required = false) String type,
//            @RequestParam(required = false) String city,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "20") int size,
//            Model model) {
//
//        try {
//            LocationSearchRequest searchRequest = new LocationSearchRequest();
//            searchRequest.setQuery(query);
//            if (type != null) {
//                try {
//                    searchRequest.setType(LocationType.valueOf(type));
//                } catch (IllegalArgumentException e) {
//                    log.warn("Invalid location type: {}", type);
//                }
//            }
//            searchRequest.setCity(city);
//            searchRequest.setPage(page);
//            searchRequest.setSize(size);
//
//            LocationListResponse response = locationService.searchLocations(searchRequest);
//
//            model.addAttribute("locations", response.getLocations());
//            model.addAttribute("currentPage", response.getCurrentPage());
//            model.addAttribute("totalPages", response.getTotalPages());
//            model.addAttribute("totalItems", response.getTotalItems());
//            model.addAttribute("hasNext", response.isHasNext());
//            model.addAttribute("hasPrevious", response.isHasPrevious());
//            model.addAttribute("query", query);
//            model.addAttribute("type", type);
//            model.addAttribute("city", city);
//
//            return "locations/list";
//        } catch (Exception e) {
//            log.error("Error loading locations list", e);
//            model.addAttribute("error", "Błąd ładowania listy lokalizacji: " + e.getMessage());
//            return "locations/list";
//        }
//    }
//
//    @GetMapping("/create")
//    @Operation(summary = "Formularz dodania lokalizacji",
//            description = "Wyświetla formularz do tworzenia nowej lokalizacji (fizycznej lub wirtualnej).")
//    public String showCreateForm(Model model) {
//        model.addAttribute("locationRequest", new CreateLocationRequest());
//        return "locations/create";
//    }
//
//    @PostMapping("/create")
//    @Operation(summary = "Tworzenie lokalizacji",
//            description = "Tworzy nową lokalizację na podstawie danych z formularza i zapisuje ją w systemie.")
//    public String createLocation(
//            @Valid @ModelAttribute("locationRequest") CreateLocationRequest request,
//            BindingResult result,
//            RedirectAttributes redirectAttributes,
//            Model model) {
//
//        log.info("Creating location with name: {}, type: {}", request.getName(), request.getType());
//
//        if (result.hasErrors()) {
//            log.warn("Validation errors: {}", result.getAllErrors());
//            return "locations/create";
//        }
//
//        if (request.getType() == LocationType.PHYSICAL) {
//            if (request.getAddress() == null || request.getAddress().trim().isEmpty()) {
//                result.rejectValue("address", "NotEmpty", "Adres jest wymagany dla lokalizacji fizycznej");
//                model.addAttribute("error", "Adres jest wymagany dla lokalizacji fizycznej");
//            }
//            if (request.getCity() == null || request.getCity().trim().isEmpty()) {
//                result.rejectValue("city", "NotEmpty", "Miasto jest wymagane dla lokalizacji fizycznej");
//                model.addAttribute("error", "Miasto jest wymagane dla lokalizacji fizycznej");
//            }
//        }
//
//        if (request.getType() == LocationType.VIRTUAL) {
//            if (request.getVirtualMeetingUrl() == null || request.getVirtualMeetingUrl().trim().isEmpty()) {
//                result.rejectValue("virtualMeetingUrl", "NotEmpty", "URL spotkania jest wymagany dla lokalizacji wirtualnej");
//                model.addAttribute("error", "URL spotkania jest wymagany dla lokalizacji wirtualnej");
//            }
//        }
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
//    @Operation(summary = "Szczegóły lokalizacji",
//            description = "Wyświetla szczegóły lokalizacji oraz mapę jej położenia.")
//    public String locationDetails(@PathVariable Long id, Model model) {
//        try {
//            LocationResponse location = locationService.getLocation(id);
//            String mapUrl = locationService.generateMapUrl(id);
//
//            model.addAttribute("location", location);
//            model.addAttribute("mapUrl", mapUrl);
//            return "locations/details";
//        } catch (Exception e) {
//            log.error("Error getting location details for ID: {}", id, e);
//            return "redirect:/locations?error=Lokalizacja nie znaleziona";
//        }
//    }
//
//    @GetMapping("/{id}/edit")
//    @Operation(summary = "Formularz edycji lokalizacji",
//            description = "Wyświetla formularz umożliwiający edycję danych istniejącej lokalizacji.")
//    public String showEditForm(@PathVariable Long id, Model model) {
//        try {
//            LocationResponse location = locationService.getLocation(id);
//            UpdateLocationRequest updateRequest = new UpdateLocationRequest();
//
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
//            log.error("Error loading edit form for ID: {}", id, e);
//            return "redirect:/locations?error=Lokalizacja nie znaleziona";
//        }
//    }
//
//    @PostMapping("/{id}/edit")
//    @Operation(summary = "Aktualizacja lokalizacji",
//            description = "Aktualizuje dane lokalizacji na podstawie formularza edycji.")
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
//            log.error("Error updating location ID: {}", id, e);
//            model.addAttribute("error", "Błąd podczas aktualizacji: " + e.getMessage());
//            model.addAttribute("locationId", id);
//            return "locations/edit";
//        }
//    }
//
//    @PostMapping("/{id}/delete")
//    @Operation(summary = "Usuwanie lokalizacji",
//            description = "Usuwa lokalizację o podanym identyfikatorze.")
//    public String deleteLocation(@PathVariable Long id, RedirectAttributes redirectAttributes) {
//        try {
//            locationService.deleteLocation(id);
//            redirectAttributes.addFlashAttribute("success", "Lokalizacja została usunięta");
//        } catch (Exception e) {
//            log.error("Error deleting location ID: {}", id, e);
//            redirectAttributes.addFlashAttribute("error", "Błąd podczas usuwania: " + e.getMessage());
//        }
//        return "redirect:/locations";
//    }
//
//    @GetMapping("/nearby")
//    @Operation(summary = "Lokalizacje w pobliżu",
//            description = "Wyświetla listę lokalizacji w pobliżu podanych współrzędnych geograficznych.")
//    public String findNearbyLocations(
//            @RequestParam(required = false) BigDecimal lat,
//            @RequestParam(required = false) BigDecimal lng,
//            @RequestParam(defaultValue = "5.0") Double radius,
//            Model model) {
//
//        if (lat == null) lat = new BigDecimal("52.2297");
//        if (lng == null) lng = new BigDecimal("21.0122");
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
//            model.addAttribute("error", "Błąd wyszukiwania lokalizacji: " + e.getMessage());
//            model.addAttribute("centerLat", lat);
//            model.addAttribute("centerLng", lng);
//            model.addAttribute("radius", radius);
//            model.addAttribute("locations", List.of());
//            return "locations/nearby";
//        }
//    }
//
//    @GetMapping("/{id}/map")
//    @Operation(summary = "Wyświetlanie lokalizacji na mapie",
//            description = "Wyświetla lokalizację na mapie oraz link do wskazówek dojazdu.")
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
//            log.error("Error showing map for location ID: {}", id, e);
//            return "redirect:/locations?error=Błąd ładowania mapy";
//        }
//    }
//
//    @GetMapping("/virtual/generate")
//    @Operation(summary = "Generowanie lokalizacji wirtualnej",
//            description = "Tworzy lokalizację wirtualną dla spotkania na wybranej platformie i opcjonalnym kodzie dostępu.")
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

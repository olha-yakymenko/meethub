package com.meethub.controller.api;

import com.meethub.domain.model.enums.ResourceType;
import com.meethub.domain.model.request.MeetingResourceRequest;
import com.meethub.domain.model.request.UpdateMeetingResourceRequest;
import com.meethub.domain.model.response.ApiResponse;
import com.meethub.domain.model.response.MeetingResourceResponse;
import com.meethub.domain.model.response.MeetingResourceStats;
import com.meethub.domain.service.FileStorageService;
import com.meethub.domain.service.MeetingResourceService;
import com.meethub.security.CustomUserDetailsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Validated // Dodanie walidacji na poziomie kontrolera
@Slf4j
@RestController
@RequestMapping("/api/meetings/{meetingId}/resources")
@RequiredArgsConstructor
@Tag(name = "Meeting Resources", description = "API do zarządzania zasobami spotkania")
public class MeetingResourceController {

    private final MeetingResourceService meetingResourceService;
    private final FileStorageService fileStorageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Dodaje zasób do spotkania",
            description = "Dodaje plik lub link jako zasób spotkania.")
    public ResponseEntity<ApiResponse<MeetingResourceResponse>> addResource(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @Valid @ModelAttribute MeetingResourceRequest request,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetailsService.CustomUserDetails userDetails) {

        log.info("Dodawanie zasobu do spotkania {} przez użytkownika {}", meetingId, userDetails.getId());
        Long userId = userDetails.getId();
        MeetingResourceResponse resource = meetingResourceService.addResource(meetingId, request, userId);
        log.info("Zasób {} dodany pomyślnie do spotkania {}", resource.getId(), meetingId);
        return ResponseEntity.ok(ApiResponse.success("Zasób został dodany pomyślnie", resource));
    }

    @GetMapping
    @Operation(summary = "Pobiera zasoby spotkania",
            description = "Zwraca listę wszystkich zasobów powiązanych ze spotkaniem.")
    public ResponseEntity<ApiResponse<List<MeetingResourceResponse>>> getResources(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetailsService.CustomUserDetails userDetails) {

        log.info("Pobieranie zasobów spotkania {} przez użytkownika {}", meetingId, userDetails.getId());
        List<MeetingResourceResponse> resources = meetingResourceService.getMeetingResources(meetingId, userDetails.getId());
        log.info("Pobrano {} zasobów dla spotkania {}", resources.size(), meetingId);
        return ResponseEntity.ok(ApiResponse.success("Zasoby pobrane pomyślnie", resources));
    }

    @GetMapping("/{resourceId}")
    @Operation(summary = "Pobiera zasób po ID",
            description = "Zwraca szczegóły konkretnego zasobu.")
    public ResponseEntity<ApiResponse<MeetingResourceResponse>> getResource(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @PathVariable @NotNull(message = "Identyfikator zasobu nie może być pusty")
            @Min(value = 1, message = "Identyfikator zasobu musi być liczbą dodatnią")
            Long resourceId,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetailsService.CustomUserDetails userDetails) {

        log.info("Pobieranie zasobu {} ze spotkania {} przez użytkownika {}", resourceId, meetingId, userDetails.getId());
        MeetingResourceResponse resource = meetingResourceService.getResource(resourceId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Zasób pobrany pomyślnie", resource));
    }

    @GetMapping("/type/{resourceType}")
    @Operation(summary = "Pobiera zasoby według typu",
            description = "Zwraca zasoby spotkania filtrowane według typu (PLIK, LINK, itp.).")
    public ResponseEntity<ApiResponse<List<MeetingResourceResponse>>> getResourcesByType(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @PathVariable @NotNull(message = "Typ zasobu nie może być pusty")
            ResourceType resourceType,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetailsService.CustomUserDetails userDetails) {

        log.info("Pobieranie zasobów typu {} ze spotkania {} przez użytkownika {}",
                resourceType, meetingId, userDetails.getId());
        List<MeetingResourceResponse> resources = meetingResourceService.getResourcesByType(meetingId, resourceType, userDetails.getId());
        log.info("Pobrano {} zasobów typu {} dla spotkania {}", resources.size(), resourceType, meetingId);
        return ResponseEntity.ok(ApiResponse.success("Zasoby pobrane pomyślnie", resources));
    }

    @GetMapping("/tag/{tag}")
    @Operation(summary = "Pobiera zasoby według tagu",
            description = "Zwraca zasoby spotkania oznaczone podanym tagiem.")
    public ResponseEntity<ApiResponse<List<MeetingResourceResponse>>> getResourcesByTag(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @PathVariable @NotBlank(message = "Tag nie może być pusty")
            @Size(min = 1, max = 50, message = "Tag musi mieć od 1 do 50 znaków")
            String tag,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetailsService.CustomUserDetails userDetails) {

        log.info("Pobieranie zasobów z tagiem '{}' ze spotkania {} przez użytkownika {}",
                tag, meetingId, userDetails.getId());
        List<MeetingResourceResponse> resources = meetingResourceService.getResourcesByTag(meetingId, tag, userDetails.getId());
        log.info("Pobrano {} zasobów z tagiem '{}' dla spotkania {}", resources.size(), tag, meetingId);
        return ResponseEntity.ok(ApiResponse.success("Zasoby pobrane pomyślnie", resources));
    }

    @PutMapping("/{resourceId}")
    @Operation(summary = "Aktualizuje zasób",
            description = "Aktualizuje metadane zasobu spotkania.")
    public ResponseEntity<ApiResponse<MeetingResourceResponse>> updateResource(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @PathVariable @NotNull(message = "Identyfikator zasobu nie może być pusty")
            @Min(value = 1, message = "Identyfikator zasobu musi być liczbą dodatnią")
            Long resourceId,

            @Valid @RequestBody UpdateMeetingResourceRequest request,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetailsService.CustomUserDetails userDetails) {

        log.info("Aktualizowanie zasobu {} w spotkaniu {} przez użytkownika {}",
                resourceId, meetingId, userDetails.getId());
        MeetingResourceResponse resource = meetingResourceService.updateResource(resourceId, request, userDetails.getId());
        log.info("Zasób {} zaktualizowany pomyślnie", resourceId);
        return ResponseEntity.ok(ApiResponse.success("Zasób zaktualizowany pomyślnie", resource));
    }

    @DeleteMapping("/{resourceId}")
    @Operation(summary = "Usuwa zasób",
            description = "Usuwa zasób ze spotkania.")
    public ResponseEntity<ApiResponse<Void>> deleteResource(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @PathVariable @NotNull(message = "Identyfikator zasobu nie może być pusty")
            @Min(value = 1, message = "Identyfikator zasobu musi być liczbą dodatnią")
            Long resourceId,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetailsService.CustomUserDetails userDetails) {

        log.info("Usuwanie zasobu {} ze spotkania {} przez użytkownika {}",
                resourceId, meetingId, userDetails.getId());
        meetingResourceService.deleteResource(resourceId, userDetails.getId());
        log.info("Zasób {} usunięty pomyślnie", resourceId);
        return ResponseEntity.ok(ApiResponse.success("Zasób usunięty pomyślnie", null));
    }

    @GetMapping("/{resourceId}/download")
    @Operation(summary = "Pobiera plik zasobu",
            description = "Pobiera plik zasobu w formie binarnej.")
    public ResponseEntity<Resource> downloadResource(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @PathVariable @NotNull(message = "Identyfikator zasobu nie może być pusty")
            @Min(value = 1, message = "Identyfikator zasobu musi być liczbą dodatnią")
            Long resourceId,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetailsService.CustomUserDetails userDetails) {

        log.info("Pobieranie pliku zasobu {} ze spotkania {} przez użytkownika {}",
                resourceId, meetingId, userDetails.getId());

        MeetingResourceResponse resource = meetingResourceService.getResource(resourceId, userDetails.getId());
        Resource fileResource = fileStorageService.loadFileAsResource(resource.getFilename());

        log.info("Plik {} pobierany przez użytkownika {}", resource.getOriginalFilename(), userDetails.getId());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getOriginalFilename() + "\"")
                .body(fileResource);
    }

    @GetMapping("/{resourceId}/preview")
    @Operation(summary = "Podgląda zasób (obrazy)",
            description = "Wyświetla podgląd zasobu (działa tylko dla obrazów).")
    public ResponseEntity<Resource> previewResource(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @PathVariable @NotNull(message = "Identyfikator zasobu nie może być pusty")
            @Min(value = 1, message = "Identyfikator zasobu musi być liczbą dodatnią")
            Long resourceId,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetailsService.CustomUserDetails userDetails) {

        log.info("Podgląd zasobu {} ze spotkania {} przez użytkownika {}",
                resourceId, meetingId, userDetails.getId());

        MeetingResourceResponse resource = meetingResourceService.getResource(resourceId, userDetails.getId());

        if (!resource.getMimeType().startsWith("image/")) {
            log.warn("Próba podglądu nie-obrazu: {} przez użytkownika {}", resource.getMimeType(), userDetails.getId());
            throw new RuntimeException("Podgląd dostępny tylko dla obrazów");
        }

        Resource fileResource = fileStorageService.loadFileAsResource(resource.getFilename());
        log.info("Wyświetlono podgląd obrazu {} dla użytkownika {}", resource.getOriginalFilename(), userDetails.getId());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(resource.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(fileResource);
    }

    @GetMapping("/stats")
    @Operation(summary = "Pobiera statystyki zasobów",
            description = "Zwraca statystyki dotyczące zasobów spotkania.")
    public ResponseEntity<ApiResponse<MeetingResourceStats>> getResourceStats(
            @PathVariable @NotNull(message = "Identyfikator spotkania nie może być pusty")
            @Min(value = 1, message = "Identyfikator spotkania musi być liczbą dodatnią")
            Long meetingId,

            @AuthenticationPrincipal @NotNull(message = "Użytkownik musi być zalogowany")
            CustomUserDetailsService.CustomUserDetails userDetails) {

        log.info("Pobieranie statystyk zasobów spotkania {} przez użytkownika {}", meetingId, userDetails.getId());
        MeetingResourceStats stats = meetingResourceService.getMeetingResourceStats(meetingId, userDetails.getId());
        log.info("Statystyki zasobów spotkania {} pobrane pomyślnie", meetingId);
        return ResponseEntity.ok(ApiResponse.success("Statystyki zasobów pobrane pomyślnie", stats));
    }

}










//package com.meethub.controller.api;
//
//import com.meethub.domain.model.enums.ResourceType;
//import com.meethub.domain.model.request.MeetingResourceRequest;
//import com.meethub.domain.model.request.UpdateMeetingResourceRequest;
//import com.meethub.domain.model.response.ApiResponse;
//import com.meethub.domain.model.response.MeetingResourceResponse;
//import com.meethub.domain.model.response.MeetingResourceStats;
//import com.meethub.domain.service.FileStorageService;
//import com.meethub.domain.service.MeetingResourceService;
//import com.meethub.security.CustomUserDetailsService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.core.io.InputStreamResource;
//import org.springframework.core.io.Resource;
//import org.springframework.http.*;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.web.bind.annotation.*;
//
//import java.io.ByteArrayInputStream;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/meetings/{meetingId}/resources")
//@RequiredArgsConstructor
//@Tag(name = "Meeting Resources", description = "Zarządzanie zasobami spotkania")
//public class MeetingResourceController {
//
//    private final MeetingResourceService meetingResourceService;
//    private final FileStorageService fileStorageService;
//
//    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    @Operation(summary = "Dodaje zasób do spotkania",
//            description = "Dodaje plik lub link jako zasób spotkania.")
//    public ResponseEntity<ApiResponse<MeetingResourceResponse>> addResource(
//            @PathVariable Long meetingId,
//            @Valid @ModelAttribute MeetingResourceRequest request,
//            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
//
//        if (userDetails == null) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(ApiResponse.error("Użytkownik musi być zalogowany, aby dodać zasoby"));
//        }
//
//        Long userId = userDetails.getId();
//        MeetingResourceResponse resource = meetingResourceService.addResource(meetingId, request, userId);
//        return ResponseEntity.ok(ApiResponse.success("Zasób został dodany pomyślnie", resource));
//    }
//
//    @GetMapping
//    @Operation(summary = "Pobiera zasoby spotkania",
//            description = "Zwraca listę wszystkich zasobów powiązanych ze spotkaniem.")
//    public ResponseEntity<ApiResponse<List<MeetingResourceResponse>>> getResources(
//            @PathVariable Long meetingId,
//            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
//
//        if (userDetails == null) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(ApiResponse.error("Użytkownik musi być zalogowany"));
//        }
//
//        List<MeetingResourceResponse> resources = meetingResourceService.getMeetingResources(meetingId, userDetails.getId());
//        return ResponseEntity.ok(ApiResponse.success("Zasoby pobrane pomyślnie", resources));
//    }
//
//    @GetMapping("/{resourceId}")
//    @Operation(summary = "Pobiera zasób po ID",
//            description = "Zwraca szczegóły konkretnego zasobu.")
//    public ResponseEntity<ApiResponse<MeetingResourceResponse>> getResource(
//            @PathVariable Long meetingId,
//            @PathVariable Long resourceId,
//            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
//
//        if (userDetails == null) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(ApiResponse.error("Użytkownik musi być zalogowany"));
//        }
//
//        MeetingResourceResponse resource = meetingResourceService.getResource(resourceId, userDetails.getId());
//        return ResponseEntity.ok(ApiResponse.success("Zasób pobrany pomyślnie", resource));
//    }
//
//    @GetMapping("/type/{resourceType}")
//    @Operation(summary = "Pobiera zasoby według typu",
//            description = "Zwraca zasoby spotkania filtrowane według typu (PLIK, LINK, itp.).")
//    public ResponseEntity<ApiResponse<List<MeetingResourceResponse>>> getResourcesByType(
//            @PathVariable Long meetingId,
//            @PathVariable ResourceType resourceType,
//            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
//
//        if (userDetails == null) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(ApiResponse.error("Użytkownik musi być zalogowany"));
//        }
//
//        List<MeetingResourceResponse> resources = meetingResourceService.getResourcesByType(meetingId, resourceType, userDetails.getId());
//        return ResponseEntity.ok(ApiResponse.success("Zasoby pobrane pomyślnie", resources));
//    }
//
//    @GetMapping("/tag/{tag}")
//    @Operation(summary = "Pobiera zasoby według tagu",
//            description = "Zwraca zasoby spotkania oznaczone podanym tagiem.")
//    public ResponseEntity<ApiResponse<List<MeetingResourceResponse>>> getResourcesByTag(
//            @PathVariable Long meetingId,
//            @PathVariable String tag,
//            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
//
//        if (userDetails == null) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(ApiResponse.error("Użytkownik musi być zalogowany"));
//        }
//
//        List<MeetingResourceResponse> resources = meetingResourceService.getResourcesByTag(meetingId, tag, userDetails.getId());
//        return ResponseEntity.ok(ApiResponse.success("Zasoby pobrane pomyślnie", resources));
//    }
//
//    @PutMapping("/{resourceId}")
//    @Operation(summary = "Aktualizuje zasób",
//            description = "Aktualizuje metadane zasobu spotkania.")
//    public ResponseEntity<ApiResponse<MeetingResourceResponse>> updateResource(
//            @PathVariable Long meetingId,
//            @PathVariable Long resourceId,
//            @Valid @RequestBody UpdateMeetingResourceRequest request,
//            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
//
//        if (userDetails == null) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(ApiResponse.error("Użytkownik musi być zalogowany"));
//        }
//
//        MeetingResourceResponse resource = meetingResourceService.updateResource(resourceId, request, userDetails.getId());
//        return ResponseEntity.ok(ApiResponse.success("Zasób zaktualizowany pomyślnie", resource));
//    }
//
//    @DeleteMapping("/{resourceId}")
//    @Operation(summary = "Usuwa zasób",
//            description = "Usuwa zasób ze spotkania.")
//    public ResponseEntity<ApiResponse<Void>> deleteResource(
//            @PathVariable Long meetingId,
//            @PathVariable Long resourceId,
//            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
//
//        if (userDetails == null) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(ApiResponse.error("Użytkownik musi być zalogowany"));
//        }
//
//        meetingResourceService.deleteResource(resourceId, userDetails.getId());
//        return ResponseEntity.ok(ApiResponse.success("Zasób usunięty pomyślnie", null));
//    }
//
//    @GetMapping("/{resourceId}/download")
//    @Operation(summary = "Pobiera plik zasobu",
//            description = "Pobiera plik zasobu w formie binarnej.")
//    public ResponseEntity<Resource> downloadResource(
//            @PathVariable Long meetingId,
//            @PathVariable Long resourceId,
//            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
//
//        if (userDetails == null) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
//        }
//
//        MeetingResourceResponse resource = meetingResourceService.getResource(resourceId, userDetails.getId());
//        Resource fileResource = fileStorageService.loadFileAsResource(resource.getFilename());
//
//        return ResponseEntity.ok()
//                .contentType(MediaType.APPLICATION_OCTET_STREAM)
//                .header(HttpHeaders.CONTENT_DISPOSITION,
//                        "attachment; filename=\"" + resource.getOriginalFilename() + "\"")
//                .body(fileResource);
//    }
//
//    @GetMapping("/{resourceId}/preview")
//    @Operation(summary = "Podgląda zasób (obrazy)",
//            description = "Wyświetla podgląd zasobu (działa tylko dla obrazów).")
//    public ResponseEntity<Resource> previewResource(
//            @PathVariable Long meetingId,
//            @PathVariable Long resourceId,
//            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
//
//        if (userDetails == null) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
//        }
//
//        MeetingResourceResponse resource = meetingResourceService.getResource(resourceId, userDetails.getId());
//
//        if (!resource.getMimeType().startsWith("image/")) {
//            throw new RuntimeException("Podgląd dostępny tylko dla obrazów");
//        }
//
//        Resource fileResource = fileStorageService.loadFileAsResource(resource.getFilename());
//
//        return ResponseEntity.ok()
//                .contentType(MediaType.parseMediaType(resource.getMimeType()))
//                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
//                .body(fileResource);
//    }
//
//    @GetMapping("/stats")
//    @Operation(summary = "Pobiera statystyki zasobów",
//            description = "Zwraca statystyki dotyczące zasobów spotkania.")
//    public ResponseEntity<ApiResponse<MeetingResourceStats>> getResourceStats(
//            @PathVariable Long meetingId,
//            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
//
//        if (userDetails == null) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(ApiResponse.error("Użytkownik musi być zalogowany"));
//        }
//
//        MeetingResourceStats stats = meetingResourceService.getMeetingResourceStats(meetingId, userDetails.getId());
//        return ResponseEntity.ok(ApiResponse.success("Statystyki zasobów pobrane pomyślnie", stats));
//    }
//}

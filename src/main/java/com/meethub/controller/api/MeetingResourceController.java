////// MeetingResourceController.java
////package com.meethub.controller.api;
////
////import org.springframework.ui.Model;
////import com.meethub.domain.model.enums.ResourceType;
////import com.meethub.domain.model.request.MeetingResourceRequest;
////import com.meethub.domain.model.request.UpdateMeetingResourceRequest;
////import com.meethub.domain.model.response.ApiResponse;
////import com.meethub.domain.model.response.MeetingResourceResponse;
////import com.meethub.domain.model.response.MeetingResourceStats;
////import com.meethub.domain.service.FileStorageService;
////import com.meethub.domain.service.MeetingResourceService;
////import com.meethub.domain.service.MeetingService;
////import io.swagger.v3.oas.annotations.Operation;
////import io.swagger.v3.oas.annotations.tags.Tag;
////import jakarta.validation.Valid;
////import lombok.RequiredArgsConstructor;
////import org.springframework.core.io.Resource;
////import org.springframework.http.HttpHeaders;
////import org.springframework.http.MediaType;
////import org.springframework.http.ResponseEntity;
////import org.springframework.security.core.annotation.AuthenticationPrincipal;
////import org.springframework.validation.BindingResult;
////import org.springframework.web.bind.annotation.*;
////
////import java.util.List;
////
////@RestController
////@RequestMapping("/api/meetings/{meetingId}/resources")
////@RequiredArgsConstructor
////@Tag(name = "Meeting Resources", description = "Meeting resources management APIs")
////public class MeetingResourceController {
////
////    private final MeetingResourceService meetingResourceService;
////    private final FileStorageService fileStorageService;
////    private final MeetingService meetingService;
////
////    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
////    @Operation(summary = "Add resource to meeting")
////    public ResponseEntity<ApiResponse<MeetingResourceResponse>> addResource(
////            @PathVariable Long meetingId,
////            @Valid @ModelAttribute MeetingResourceRequest request,
////            @AuthenticationPrincipal Long userId) {
////
////        MeetingResourceResponse resource = meetingResourceService.addResource(meetingId, request, userId);
////        return ResponseEntity.ok(ApiResponse.success("Resource added successfully", resource));
////    }
////
////    @GetMapping
////    @Operation(summary = "Get all meeting resources")
////    public ResponseEntity<ApiResponse<List<MeetingResourceResponse>>> getResources(
////            @PathVariable Long meetingId,
////            @AuthenticationPrincipal Long userId) {
////
////        List<MeetingResourceResponse> resources = meetingResourceService.getMeetingResources(meetingId, userId);
////        return ResponseEntity.ok(ApiResponse.success("Resources retrieved successfully", resources));
////    }
////
////    @GetMapping("/add")
////    public String showAddResourceForm(@PathVariable Long meetingId, Model model) {
////        var meeting = meetingService.getMeeting(meetingId);
////        model.addAttribute("meeting", meeting);
////        model.addAttribute("meetingResourceRequest", new MeetingResourceRequest());
////        return "meetings/resources/add-resource";
////    }
////
////    @PostMapping("/add")
////    public String addResource(@PathVariable Long meetingId,
////                              @ModelAttribute @Valid MeetingResourceRequest request,
////                              BindingResult result,
////                              Model model,
////                              @AuthenticationPrincipal Long userId) {
////
////        var meeting = meetingService.getMeeting(meetingId);
////        model.addAttribute("meeting", meeting);
////
////        if (result.hasErrors()) {
////            return "meetings/resources/add-resource";
////        }
////
////        try {
////            meetingResourceService.addResource(meetingId, request, userId);
////            return "redirect:/meetings/" + meetingId + "/resources?success=Zasób został dodany pomyślnie";
////        } catch (Exception e) {
////            model.addAttribute("error", "Błąd podczas dodawania zasobu: " + e.getMessage());
////            return "meetings/resources/add-resource";
////        }
////    }
////
////    @GetMapping("/{resourceId}")
////    @Operation(summary = "Get meeting resource by ID")
////    public ResponseEntity<ApiResponse<MeetingResourceResponse>> getResource(
////            @PathVariable Long meetingId,
////            @PathVariable Long resourceId,
////            @AuthenticationPrincipal Long userId) {
////
////        MeetingResourceResponse resource = meetingResourceService.getResource(resourceId, userId);
////        return ResponseEntity.ok(ApiResponse.success("Resource retrieved successfully", resource));
////    }
////
////    @GetMapping("/type/{resourceType}")
////    @Operation(summary = "Get meeting resources by type")
////    public ResponseEntity<ApiResponse<List<MeetingResourceResponse>>> getResourcesByType(
////            @PathVariable Long meetingId,
////            @PathVariable ResourceType resourceType,
////            @AuthenticationPrincipal Long userId) {
////
////        List<MeetingResourceResponse> resources = meetingResourceService.getResourcesByType(meetingId, resourceType, userId);
////        return ResponseEntity.ok(ApiResponse.success("Resources retrieved successfully", resources));
////    }
////
////    @PutMapping("/{resourceId}")
////    @Operation(summary = "Update meeting resource")
////    public ResponseEntity<ApiResponse<MeetingResourceResponse>> updateResource(
////            @PathVariable Long meetingId,
////            @PathVariable Long resourceId,
////            @Valid @RequestBody UpdateMeetingResourceRequest request,
////            @AuthenticationPrincipal Long userId) {
////
////        MeetingResourceResponse resource = meetingResourceService.updateResource(resourceId, request, userId);
////        return ResponseEntity.ok(ApiResponse.success("Resource updated successfully", resource));
////    }
////
////    @DeleteMapping("/{resourceId}")
////    @Operation(summary = "Delete meeting resource")
////    public ResponseEntity<ApiResponse<Void>> deleteResource(
////            @PathVariable Long meetingId,
////            @PathVariable Long resourceId,
////            @AuthenticationPrincipal Long userId) {
////
////        meetingResourceService.deleteResource(resourceId, userId);
////        return ResponseEntity.ok(ApiResponse.success("Resource deleted successfully", null));
////    }
////
////    @GetMapping("/{resourceId}/download")
////    @Operation(summary = "Download meeting resource")
////    public ResponseEntity<Resource> downloadResource(
////            @PathVariable Long meetingId,
////            @PathVariable Long resourceId,
////            @AuthenticationPrincipal Long userId) {
////
////        MeetingResourceResponse resource = meetingResourceService.getResource(resourceId, userId);
////        Resource fileResource = fileStorageService.loadFileAsResource(resource.getFilename());
////
////        return ResponseEntity.ok()
////                .contentType(MediaType.APPLICATION_OCTET_STREAM)
////                .header(HttpHeaders.CONTENT_DISPOSITION,
////                        "attachment; filename=\"" + resource.getOriginalFilename() + "\"")
////                .body(fileResource);
////    }
////
////    @GetMapping("/{resourceId}/preview")
////    @Operation(summary = "Preview meeting resource (for images)")
////    public ResponseEntity<Resource> previewResource(
////            @PathVariable Long meetingId,
////            @PathVariable Long resourceId,
////            @AuthenticationPrincipal Long userId) {
////
////        MeetingResourceResponse resource = meetingResourceService.getResource(resourceId, userId);
////
////        if (!resource.getMimeType().startsWith("image/")) {
////            throw new RuntimeException("Preview only available for images");
////        }
////
////        Resource fileResource = fileStorageService.loadFileAsResource(resource.getFilename());
////
////        return ResponseEntity.ok()
////                .contentType(MediaType.parseMediaType(resource.getMimeType()))
////                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
////                .body(fileResource);
////    }
////
////
////    @GetMapping("/stats")
////    @Operation(summary = "Get meeting resources statistics")
////    public ResponseEntity<ApiResponse<MeetingResourceStats>> getResourceStats(
////            @PathVariable Long meetingId,
////            @AuthenticationPrincipal Long userId) {
////
////        MeetingResourceStats stats = meetingResourceService.getMeetingResourceStats(meetingId, userId);
////        return ResponseEntity.ok(ApiResponse.success("Resource stats retrieved successfully", stats));
////    }
////}
////
//
//
//
//
//
//
//
//
//
//
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
//import jakarta.servlet.http.HttpServletRequest;
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
//@Tag(name = "Meeting Resources", description = "Meeting resources management APIs")
//public class MeetingResourceController {
//
//    private final MeetingResourceService meetingResourceService;
//    private final FileStorageService fileStorageService;
//
////    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
////    @Operation(summary = "Add resource to meeting")
////    public ResponseEntity<ApiResponse<MeetingResourceResponse>> addResource(
////            @PathVariable Long meetingId,
////            @Valid @ModelAttribute MeetingResourceRequest request,
////            @AuthenticationPrincipal Long userId) {
////
////        MeetingResourceResponse resource = meetingResourceService.addResource(meetingId, request, userId);
////        return ResponseEntity.ok(ApiResponse.success("Resource added successfully", resource));
////    }
//
//    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    @Operation(summary = "Add resource to meeting")
//    public ResponseEntity<ApiResponse<MeetingResourceResponse>> addResource(
//            @PathVariable Long meetingId,
//            @Valid @ModelAttribute MeetingResourceRequest request,
//            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
//
//        if (userDetails == null) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(ApiResponse.error("User must be authenticated to add resources"));
//        }
//
//        Long userId = userDetails.getId();
////        log.info("Adding resource for user ID: {}", userId);
//
//        MeetingResourceResponse resource = meetingResourceService.addResource(meetingId, request, userId);
//        return ResponseEntity.ok(ApiResponse.success("Resource added successfully", resource));
//    }
//
//
//    @GetMapping
//    @Operation(summary = "Get all meeting resources")
//    public ResponseEntity<ApiResponse<List<MeetingResourceResponse>>> getResources(
//            @PathVariable Long meetingId,
//            @AuthenticationPrincipal Long userId) {
//
//        List<MeetingResourceResponse> resources = meetingResourceService.getMeetingResources(meetingId, userId);
//        return ResponseEntity.ok(ApiResponse.success("Resources retrieved successfully", resources));
//    }
//
//    @GetMapping("/{resourceId}")
//    @Operation(summary = "Get meeting resource by ID")
//    public ResponseEntity<ApiResponse<MeetingResourceResponse>> getResource(
//            @PathVariable Long meetingId,
//            @PathVariable Long resourceId,
//            @AuthenticationPrincipal Long userId) {
//
//        MeetingResourceResponse resource = meetingResourceService.getResource(resourceId, userId);
//        return ResponseEntity.ok(ApiResponse.success("Resource retrieved successfully", resource));
//    }
//
//    @GetMapping("/type/{resourceType}")
//    @Operation(summary = "Get meeting resources by type")
//    public ResponseEntity<ApiResponse<List<MeetingResourceResponse>>> getResourcesByType(
//            @PathVariable Long meetingId,
//            @PathVariable ResourceType resourceType,
//            @AuthenticationPrincipal Long userId) {
//
//        List<MeetingResourceResponse> resources = meetingResourceService.getResourcesByType(meetingId, resourceType, userId);
//        return ResponseEntity.ok(ApiResponse.success("Resources retrieved successfully", resources));
//    }
//
//    @GetMapping("/tag/{tag}")
//    @Operation(summary = "Get meeting resources by tag")
//    public ResponseEntity<ApiResponse<List<MeetingResourceResponse>>> getResourcesByTag(
//            @PathVariable Long meetingId,
//            @PathVariable String tag,
//            @AuthenticationPrincipal Long userId) {
//
//        List<MeetingResourceResponse> resources = meetingResourceService.getResourcesByTag(meetingId, tag, userId);
//        return ResponseEntity.ok(ApiResponse.success("Resources retrieved successfully", resources));
//    }
//
//    @PutMapping("/{resourceId}")
//    @Operation(summary = "Update meeting resource")
//    public ResponseEntity<ApiResponse<MeetingResourceResponse>> updateResource(
//            @PathVariable Long meetingId,
//            @PathVariable Long resourceId,
//            @Valid @RequestBody UpdateMeetingResourceRequest request,
//            @AuthenticationPrincipal Long userId) {
//
//        MeetingResourceResponse resource = meetingResourceService.updateResource(resourceId, request, userId);
//        return ResponseEntity.ok(ApiResponse.success("Resource updated successfully", resource));
//    }
//
//    @DeleteMapping("/{resourceId}")
//    @Operation(summary = "Delete meeting resource")
//    public ResponseEntity<ApiResponse<Void>> deleteResource(
//            @PathVariable Long meetingId,
//            @PathVariable Long resourceId,
//            @AuthenticationPrincipal Long userId) {
//
//        meetingResourceService.deleteResource(resourceId, userId);
//        return ResponseEntity.ok(ApiResponse.success("Resource deleted successfully", null));
//    }
//
//    @GetMapping("/{resourceId}/download")
//    @Operation(summary = "Download meeting resource")
//    public ResponseEntity<Resource> downloadResource(
//            @PathVariable Long meetingId,
//            @PathVariable Long resourceId,
//            @AuthenticationPrincipal Long userId) {
//
//        MeetingResourceResponse resource = meetingResourceService.getResource(resourceId, userId);
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
//    @Operation(summary = "Preview meeting resource (for images)")
//    public ResponseEntity<Resource> previewResource(
//            @PathVariable Long meetingId,
//            @PathVariable Long resourceId,
//            @AuthenticationPrincipal Long userId) {
//
//        MeetingResourceResponse resource = meetingResourceService.getResource(resourceId, userId);
//
//        if (!resource.getMimeType().startsWith("image/")) {
//            throw new RuntimeException("Preview only available for images");
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
//    @Operation(summary = "Get meeting resources statistics")
//    public ResponseEntity<ApiResponse<MeetingResourceStats>> getResourceStats(
//            @PathVariable Long meetingId,
//            @AuthenticationPrincipal Long userId) {
//
//        MeetingResourceStats stats = meetingResourceService.getMeetingResourceStats(meetingId, userId);
//        return ResponseEntity.ok(ApiResponse.success("Resource stats retrieved successfully", stats));
//    }
//
//
////    @GetMapping("/{resourceId}/preview")
////    @Operation(summary = "Preview meeting resource")
////    public ResponseEntity<Resource> previewResource(
////            @PathVariable Long meetingId,
////            @PathVariable Long resourceId,
////            @AuthenticationPrincipal Long userId,
////            HttpServletRequest request) {
////
////        MeetingResourceResponse resource = meetingResourceService.getResource(resourceId, userId);
////        Resource fileResource = fileStorageService.loadFileAsResource(resource.getFilename());
////
////        // Określ czy to przegląd czy pobieranie
////        String rangeHeader = request.getHeader("Range");
////        boolean isRangeRequest = rangeHeader != null && rangeHeader.startsWith("bytes=");
////
////        HttpHeaders headers = new HttpHeaders();
////
////        // Dla obrazów - przegląd w przeglądarce
////        if (resource.getMimeType().startsWith("image/")) {
////            headers.setContentType(MediaType.parseMediaType(resource.getMimeType()));
////            headers.setContentDisposition(ContentDisposition.inline().filename(resource.getOriginalFilename()).build());
////        }
////        // Dla PDF - przegląd w przeglądarce
////        else if (resource.getMimeType().equals("application/pdf")) {
////            headers.setContentType(MediaType.APPLICATION_PDF);
////            headers.setContentDisposition(ContentDisposition.inline().filename(resource.getOriginalFilename()).build());
////        }
////        // Dla tekstów - przegląd w przeglądarce
////        else if (resource.getMimeType().startsWith("text/")) {
////            headers.setContentType(MediaType.TEXT_PLAIN);
////            headers.setContentDisposition(ContentDisposition.inline().filename(resource.getOriginalFilename()).build());
////        }
////        // Dla pozostałych - proponuj pobranie
////        else {
////            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
////            headers.setContentDisposition(ContentDisposition.attachment().filename(resource.getOriginalFilename()).build());
////        }
////
////        // Obsługa partial content dla video/audio
////        if (isRangeRequest && (resource.getMimeType().startsWith("video/") || resource.getMimeType().startsWith("audio/"))) {
////            return handlePartialContent(fileResource, resource, rangeHeader);
////        }
////
////        return ResponseEntity.ok()
////                .headers(headers)
////                .body(fileResource);
////    }
//
//    private ResponseEntity<Resource> handlePartialContent(Resource resource, MeetingResourceResponse resourceInfo, String rangeHeader) {
//        try {
//            long fileSize = Files.size(Paths.get(resource.getFile().getAbsolutePath()));
//            String[] ranges = rangeHeader.substring("bytes=".length()).split("-");
//            long rangeStart = Long.parseLong(ranges[0]);
//            long rangeEnd = ranges.length > 1 ? Long.parseLong(ranges[1]) : fileSize - 1;
//
//            if (rangeEnd >= fileSize) {
//                rangeEnd = fileSize - 1;
//            }
//
//            long contentLength = rangeEnd - rangeStart + 1;
//
//            HttpHeaders headers = new HttpHeaders();
//            headers.add("Content-Type", resourceInfo.getMimeType());
//            headers.add("Content-Length", String.valueOf(contentLength));
//            headers.add("Content-Range", "bytes " + rangeStart + "-" + rangeEnd + "/" + fileSize);
//            headers.add("Accept-Ranges", "bytes");
//            headers.setContentDisposition(ContentDisposition.inline().filename(resourceInfo.getOriginalFilename()).build());
//
//            InputStreamResource inputStreamResource = new InputStreamResource(
//                    new ByteArrayInputStream(Files.readAllBytes(Paths.get(resource.getFile().getAbsolutePath())))
//            );
//
//            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
//                    .headers(headers)
//                    .body(inputStreamResource);
//
//        } catch (IOException e) {
//            throw new RuntimeException("Error handling partial content", e);
//        }
//    }
//}









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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/meetings/{meetingId}/resources")
@RequiredArgsConstructor
@Tag(name = "Meeting Resources", description = "Meeting resources management APIs")
public class MeetingResourceController {

    private final MeetingResourceService meetingResourceService;
    private final FileStorageService fileStorageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Add resource to meeting")
    public ResponseEntity<ApiResponse<MeetingResourceResponse>> addResource(
            @PathVariable Long meetingId,
            @Valid @ModelAttribute MeetingResourceRequest request,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("User must be authenticated to add resources"));
        }

        Long userId = userDetails.getId();
        MeetingResourceResponse resource = meetingResourceService.addResource(meetingId, request, userId);
        return ResponseEntity.ok(ApiResponse.success("Resource added successfully", resource));
    }

    @GetMapping
    @Operation(summary = "Get all meeting resources")
    public ResponseEntity<ApiResponse<List<MeetingResourceResponse>>> getResources(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("User must be authenticated"));
        }

        List<MeetingResourceResponse> resources = meetingResourceService.getMeetingResources(meetingId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Resources retrieved successfully", resources));
    }

    @GetMapping("/{resourceId}")
    @Operation(summary = "Get meeting resource by ID")
    public ResponseEntity<ApiResponse<MeetingResourceResponse>> getResource(
            @PathVariable Long meetingId,
            @PathVariable Long resourceId,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("User must be authenticated"));
        }

        MeetingResourceResponse resource = meetingResourceService.getResource(resourceId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Resource retrieved successfully", resource));
    }

    @GetMapping("/type/{resourceType}")
    @Operation(summary = "Get meeting resources by type")
    public ResponseEntity<ApiResponse<List<MeetingResourceResponse>>> getResourcesByType(
            @PathVariable Long meetingId,
            @PathVariable ResourceType resourceType,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("User must be authenticated"));
        }

        List<MeetingResourceResponse> resources = meetingResourceService.getResourcesByType(meetingId, resourceType, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Resources retrieved successfully", resources));
    }

    @GetMapping("/tag/{tag}")
    @Operation(summary = "Get meeting resources by tag")
    public ResponseEntity<ApiResponse<List<MeetingResourceResponse>>> getResourcesByTag(
            @PathVariable Long meetingId,
            @PathVariable String tag,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("User must be authenticated"));
        }

        List<MeetingResourceResponse> resources = meetingResourceService.getResourcesByTag(meetingId, tag, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Resources retrieved successfully", resources));
    }

    @PutMapping("/{resourceId}")
    @Operation(summary = "Update meeting resource")
    public ResponseEntity<ApiResponse<MeetingResourceResponse>> updateResource(
            @PathVariable Long meetingId,
            @PathVariable Long resourceId,
            @Valid @RequestBody UpdateMeetingResourceRequest request,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("User must be authenticated"));
        }

        MeetingResourceResponse resource = meetingResourceService.updateResource(resourceId, request, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Resource updated successfully", resource));
    }

    @DeleteMapping("/{resourceId}")
    @Operation(summary = "Delete meeting resource")
    public ResponseEntity<ApiResponse<Void>> deleteResource(
            @PathVariable Long meetingId,
            @PathVariable Long resourceId,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("User must be authenticated"));
        }

        meetingResourceService.deleteResource(resourceId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Resource deleted successfully", null));
    }

    @GetMapping("/{resourceId}/download")
    @Operation(summary = "Download meeting resource")
    public ResponseEntity<Resource> downloadResource(
            @PathVariable Long meetingId,
            @PathVariable Long resourceId,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        MeetingResourceResponse resource = meetingResourceService.getResource(resourceId, userDetails.getId());
        Resource fileResource = fileStorageService.loadFileAsResource(resource.getFilename());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getOriginalFilename() + "\"")
                .body(fileResource);
    }

    @GetMapping("/{resourceId}/preview")
    @Operation(summary = "Preview meeting resource (for images)")
    public ResponseEntity<Resource> previewResource(
            @PathVariable Long meetingId,
            @PathVariable Long resourceId,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        MeetingResourceResponse resource = meetingResourceService.getResource(resourceId, userDetails.getId());

        if (!resource.getMimeType().startsWith("image/")) {
            throw new RuntimeException("Preview only available for images");
        }

        Resource fileResource = fileStorageService.loadFileAsResource(resource.getFilename());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(resource.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(fileResource);
    }

    @GetMapping("/stats")
    @Operation(summary = "Get meeting resources statistics")
    public ResponseEntity<ApiResponse<MeetingResourceStats>> getResourceStats(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("User must be authenticated"));
        }

        MeetingResourceStats stats = meetingResourceService.getMeetingResourceStats(meetingId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Resource stats retrieved successfully", stats));
    }

    private ResponseEntity<Resource> handlePartialContent(Resource resource, MeetingResourceResponse resourceInfo, String rangeHeader) {
        try {
            long fileSize = Files.size(Paths.get(resource.getFile().getAbsolutePath()));
            String[] ranges = rangeHeader.substring("bytes=".length()).split("-");
            long rangeStart = Long.parseLong(ranges[0]);
            long rangeEnd = ranges.length > 1 ? Long.parseLong(ranges[1]) : fileSize - 1;

            if (rangeEnd >= fileSize) {
                rangeEnd = fileSize - 1;
            }

            long contentLength = rangeEnd - rangeStart + 1;

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", resourceInfo.getMimeType());
            headers.add("Content-Length", String.valueOf(contentLength));
            headers.add("Content-Range", "bytes " + rangeStart + "-" + rangeEnd + "/" + fileSize);
            headers.add("Accept-Ranges", "bytes");
            headers.setContentDisposition(ContentDisposition.inline().filename(resourceInfo.getOriginalFilename()).build());

            InputStreamResource inputStreamResource = new InputStreamResource(
                    new ByteArrayInputStream(Files.readAllBytes(Paths.get(resource.getFile().getAbsolutePath())))
            );

            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .headers(headers)
                    .body(inputStreamResource);

        } catch (IOException e) {
            throw new RuntimeException("Error handling partial content", e);
        }
    }
}
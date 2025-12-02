// MeetingResourceServiceImpl.java
package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.MeetingResource;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.ResourceType;
import com.meethub.domain.model.request.MeetingResourceRequest;
import com.meethub.domain.model.request.UpdateMeetingResourceRequest;
import com.meethub.domain.model.response.MeetingResourceResponse;
import com.meethub.domain.model.response.MeetingResourceStats;
import com.meethub.domain.model.response.UserResponse;
import com.meethub.domain.repository.jpa.MeetingRepository;
import com.meethub.domain.repository.jpa.MeetingResourceRepository;
import com.meethub.domain.repository.jpa.UserRepository;
import com.meethub.domain.service.MeetingResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingResourceServiceImpl implements MeetingResourceService {

    private final MeetingResourceRepository meetingResourceRepository;
    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;

    @Value("${file.upload-dir:uploads/resources}")
    private String uploadDir;

    @Override
    @Transactional
    public MeetingResourceResponse addResource(Long meetingId, MeetingResourceRequest request, Long userId) {
        log.info("Adding resource to meeting {} by user {}", meetingId, userId);

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found with id: " + meetingId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Sprawdź uprawnienia
        if (!hasPermissionToAddResource(meeting, user)) {
            throw new RuntimeException("No permission to add resources to this meeting");
        }

        // Walidacja pliku
        validateFile(request.getFile());

        // Przygotuj ścieżkę do zapisu
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        ensureUploadDirectoryExists(uploadPath);

        // Generuj unikalną nazwę pliku
        String fileExtension = getFileExtension(request.getFile().getOriginalFilename());
        String uniqueFilename = generateUniqueFilename(fileExtension);
        Path filePath = uploadPath.resolve(uniqueFilename);

        // Zapisz plik na dysk
        try {
            Files.copy(request.getFile().getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            log.debug("File saved successfully: {}", filePath);
        } catch (IOException ex) {
            log.error("Failed to save file: {}", ex.getMessage());
            throw new RuntimeException("Could not store file: " + request.getFile().getOriginalFilename(), ex);
        }

        // Określ typ zasobu
        ResourceType resourceType = determineResourceType(request.getFile().getContentType(), fileExtension);

        // Utwórz encję zasobu
        MeetingResource resource = MeetingResource.builder()
                .meeting(meeting)
                .filename(uniqueFilename)
                .originalFilename(request.getOriginalFilename())
                .filePath(filePath.toString())
                .fileSize(request.getFile().getSize())
                .mimeType(request.getFile().getContentType())
                .resourceType(resourceType)
                .uploadedBy(user)
                .accessLevel(request.getAccessLevel())
                .build();

        // Dodaj tagi jeśli istnieją
        if (request.getTags() != null) {
            resource.setTags(new HashSet<>(request.getTags()));
        }

        MeetingResource savedResource = meetingResourceRepository.save(resource);
        log.info("Resource added successfully with id: {}", savedResource.getId());

        return mapToResponse(savedResource, user, meeting);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeetingResourceResponse> getMeetingResources(Long meetingId, Long userId) {
        log.debug("Getting resources for meeting {} by user {}", meetingId, userId);

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found with id: " + meetingId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        List<MeetingResource> resources = meetingResourceRepository.findByMeetingIdAndIsCurrentTrueOrderByUploadedAtDesc(meetingId);

        return resources.stream()
                .filter(resource -> resource.canUserAccess(user, meeting))
                .map(resource -> mapToResponse(resource, user, meeting))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MeetingResourceResponse getResource(Long resourceId, Long userId) {
        log.debug("Getting resource {} by user {}", resourceId, userId);

        MeetingResource resource = meetingResourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("Resource not found with id: " + resourceId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Sprawdź uprawnienia dostępu
        if (!resource.canUserAccess(user, resource.getMeeting())) {
            throw new RuntimeException("No access to this resource");
        }

        return mapToResponse(resource, user, resource.getMeeting());
    }

    @Override
    @Transactional
    public MeetingResourceResponse updateResource(Long resourceId, UpdateMeetingResourceRequest request, Long userId) {
        log.info("Updating resource {} by user {}", resourceId, userId);

        MeetingResource resource = meetingResourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("Resource not found with id: " + resourceId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Sprawdź uprawnienia
        if (!resource.getUploadedBy().getId().equals(userId) &&
                !resource.getMeeting().getOrganizer().getId().equals(userId)) {
            throw new RuntimeException("Only resource owner or meeting organizer can update it");
        }

        // Aktualizuj pola
        if (request.getTags() != null) {
            resource.setTags(new HashSet<>(request.getTags()));
        }
        if (request.getAccessLevel() != null) {
            resource.setAccessLevel(request.getAccessLevel());
        }

        MeetingResource updatedResource = meetingResourceRepository.save(resource);
        log.info("Resource {} updated successfully", resourceId);

        return mapToResponse(updatedResource, user, resource.getMeeting());
    }

    @Override
    @Transactional
    public void deleteResource(Long resourceId, Long userId) {
        log.info("Deleting resource {} by user {}", resourceId, userId);

        MeetingResource resource = meetingResourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("Resource not found with id: " + resourceId));

        // Sprawdź uprawnienia
        if (!resource.getUploadedBy().getId().equals(userId) &&
                !resource.getMeeting().getOrganizer().getId().equals(userId)) {
            throw new RuntimeException("No permission to delete this resource");
        }

        // Usuń fizyczny plik
        deletePhysicalFile(resource.getFilePath());

        // Usuń z bazy danych
        meetingResourceRepository.delete(resource);

        log.info("Resource {} deleted successfully", resourceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeetingResourceResponse> getResourcesByType(Long meetingId, ResourceType resourceType, Long userId) {
        log.debug("Getting resources of type {} for meeting {} by user {}", resourceType, meetingId, userId);

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found with id: " + meetingId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        List<MeetingResource> resources = meetingResourceRepository.findByMeetingIdAndResourceTypeOrderByUploadedAtDesc(meetingId, resourceType);

        return resources.stream()
                .filter(resource -> resource.canUserAccess(user, meeting) && resource.getIsCurrent())
                .map(resource -> mapToResponse(resource, user, meeting))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeetingResourceResponse> getResourcesByTag(Long meetingId, String tag, Long userId) {
        log.debug("Getting resources with tag '{}' for meeting {} by user {}", tag, meetingId, userId);

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found with id: " + meetingId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        List<MeetingResource> resources = meetingResourceRepository.findByMeetingIdAndTagsContainingOrderByUploadedAtDesc(meetingId, tag);

        return resources.stream()
                .filter(resource -> resource.canUserAccess(user, meeting) && resource.getIsCurrent())
                .map(resource -> mapToResponse(resource, user, meeting))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MeetingResourceStats getMeetingResourceStats(Long meetingId, Long userId) {
        log.debug("Getting resource stats for meeting {} by user {}", meetingId, userId);

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found with id: " + meetingId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Sprawdź czy użytkownik ma dostęp do spotkania
        if (!meeting.getOrganizer().getId().equals(userId) &&
                meeting.getParticipants().stream().noneMatch(p -> p.getUser().getId().equals(userId) && p.isConfirmed())) {
            throw new RuntimeException("No access to meeting statistics");
        }

        // Pobierz wszystkie zasoby spotkania
        List<MeetingResource> allResources = meetingResourceRepository.findByMeetingIdAndIsCurrentTrueOrderByUploadedAtDesc(meetingId);

        // Oblicz statystyki
        Long totalResources = (long) allResources.size();
        Long documentCount = allResources.stream()
                .filter(r -> r.getResourceType() == ResourceType.DOCUMENT)
                .count();
        Long presentationCount = allResources.stream()
                .filter(r -> r.getResourceType() == ResourceType.PRESENTATION)
                .count();
        Long imageCount = allResources.stream()
                .filter(r -> r.getResourceType() == ResourceType.IMAGE)
                .count();
        Long videoCount = allResources.stream()
                .filter(r -> r.getResourceType() == ResourceType.VIDEO)
                .count();
        Long audioCount = allResources.stream()
                .filter(r -> r.getResourceType() == ResourceType.AUDIO)
                .count();

        Long otherCount = totalResources - documentCount - presentationCount - imageCount - videoCount - audioCount;

        // Oblicz całkowity rozmiar
        Long totalSize = allResources.stream()
                .mapToLong(r -> r.getFileSize() != null ? r.getFileSize() : 0)
                .sum();

        return MeetingResourceStats.builder()
                .totalResources(totalResources)
                .documentCount(documentCount)
                .presentationCount(presentationCount)
                .imageCount(imageCount)
                .videoCount(videoCount)
                .audioCount(audioCount)
                .otherCount(otherCount)
                .totalSize(totalSize)
                .build();
    }

    // Metody pomocnicze do zarządzania plikami

    /**
     * Ładuje plik jako Resource do pobrania
     */
    public Resource loadFileAsResource(String filename) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("File not found: " + filename);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File not found: " + filename, ex);
        }
    }

    /**
     * Sprawdza czy plik jest poprawny
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        // Sprawdź rozmiar pliku (max 50MB)
        long maxSize = 50 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new RuntimeException("File size exceeds maximum allowed size (50MB)");
        }

        // Sprawdź typ MIME
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new RuntimeException("Could not determine file type");
        }

        // Lista dozwolonych typów
        Set<String> allowedTypes = Set.of(
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "image/jpeg",
                "image/png",
                "image/gif",
                "video/mp4",
                "audio/mpeg",
                "application/zip"
        );

        if (!allowedTypes.contains(contentType)) {
            throw new RuntimeException("File type not allowed: " + contentType);
        }
    }

    /**
     * Tworzy katalog upload jeśli nie istnieje
     */
    private void ensureUploadDirectoryExists(Path uploadPath) {
        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Created upload directory: {}", uploadPath);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Could not create upload directory", ex);
        }
    }

    /**
     * Usuwa fizyczny plik z dysku
     */
    private void deletePhysicalFile(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
            log.debug("Physical file deleted: {}", filePath);
        } catch (IOException ex) {
            log.warn("Could not delete physical file: {}", filePath, ex);
            // Nie rzucamy wyjątku - ważniejsze jest usunięcie z bazy danych
        }
    }

    // Pozostałe metody pomocnicze
    boolean hasPermissionToAddResource(Meeting meeting, User user) {
        return meeting.getOrganizer().equals(user) ||
                meeting.getParticipants().stream()
                        .anyMatch(p -> p.getUser().equals(user) && p.isConfirmed());
    }

    private String generateUniqueFilename(String extension) {
        return UUID.randomUUID().toString() + "." + extension;
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "bin";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private ResourceType determineResourceType(String mimeType, String fileExtension) {
        if (mimeType == null) {
            return ResourceType.OTHER;
        }

        if (mimeType.startsWith("image/")) {
            return ResourceType.IMAGE;
        } else if (mimeType.startsWith("video/")) {
            return ResourceType.VIDEO;
        } else if (mimeType.startsWith("audio/")) {
            return ResourceType.AUDIO;
        } else if (mimeType.contains("pdf")) {
            return ResourceType.DOCUMENT;
        } else if (mimeType.contains("presentation") ||
                fileExtension.equals("ppt") || fileExtension.equals("pptx")) {
            return ResourceType.PRESENTATION;
        } else if (mimeType.contains("word") ||
                fileExtension.equals("doc") || fileExtension.equals("docx")) {
            return ResourceType.DOCUMENT;
        } else {
            return ResourceType.OTHER;
        }
    }

    MeetingResourceResponse mapToResponse(MeetingResource resource, User currentUser, Meeting meeting) {
        MeetingResourceResponse response = new MeetingResourceResponse();
        response.setId(resource.getId());
        response.setFilename(resource.getFilename());
        response.setOriginalFilename(resource.getOriginalFilename());
        response.setFileSize(resource.getFileSize());
        response.setFileSizeFormatted(resource.getFileSizeFormatted());
        response.setMimeType(resource.getMimeType());
        response.setResourceType(resource.getResourceType());
        response.setTags(resource.getTags());
        response.setVersion(resource.getVersion());
        response.setIsCurrent(resource.getIsCurrent());
        response.setAccessLevel(resource.getAccessLevel());
        response.setUploadedAt(resource.getUploadedAt());

        // Mapowanie użytkownika
        UserResponse userResponse = new UserResponse();
        userResponse.setId(resource.getUploadedBy().getId());
        userResponse.setFirstName(resource.getUploadedBy().getFirstName());
        userResponse.setLastName(resource.getUploadedBy().getLastName());
        userResponse.setEmail(resource.getUploadedBy().getEmail());
        response.setUploadedBy(userResponse);

        // URL do pobrania
        response.setDownloadUrl("/api/v1/meetings/" + meeting.getId() + "/resources/" + resource.getId() + "/download");

        // URL do podglądu (dla obrazów)
        if (resource.isImage()) {
            response.setPreviewUrl("/api/v1/meetings/" + meeting.getId() + "/resources/" + resource.getId() + "/preview");
        }

        // Uprawnienia
        response.setCanEdit(resource.getUploadedBy().getId().equals(currentUser.getId()));
        response.setCanDelete(resource.getUploadedBy().getId().equals(currentUser.getId()) ||
                meeting.getOrganizer().getId().equals(currentUser.getId()));

        return response;
    }
}
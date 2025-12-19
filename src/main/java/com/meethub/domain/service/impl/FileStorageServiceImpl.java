package com.meethub.domain.service.impl;

import com.meethub.domain.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

@Slf4j
@Service
@Validated
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${file.upload-dir:uploads/resources}")
    private String uploadDir;

    private Path fileStorageLocation;

    @Value("${file.cleanup.enabled:true}")
    private boolean cleanupEnabled;

    @Value("${file.cleanup.days-threshold:30}")
    private int daysThreshold;

    Path getFileStorageLocation() {
        if (fileStorageLocation == null) {
            this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
            try {
                Files.createDirectories(this.fileStorageLocation);
                log.info("File storage location initialized: {}", this.fileStorageLocation);
            } catch (Exception ex) {
                throw new RuntimeException(
                        "Could not create the directory where the uploaded files will be stored.", ex);
            }
        }
        return fileStorageLocation;
    }

    @Override
    public String storeFile(MultipartFile file, String filename) {

        try {
            if (filename.contains("..")) {
                throw new RuntimeException("Filename contains invalid path sequence " + filename);
            }

            validateFileSize(file);

            Path targetLocation = getFileStorageLocation().resolve(filename);

            if (Files.exists(targetLocation)) {
                log.warn("File already exists, overwriting: {}", filename);
            }

            if (file.isEmpty()) {
                throw new RuntimeException("Cannot store empty file: " + filename);
            }

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.debug("File stored successfully: {}", targetLocation);
            return targetLocation.toString();

        } catch (IOException ex) {
            log.error("Failed to store file: {}", filename, ex);
            throw new RuntimeException("Could not store file " + filename + ". Please try again!", ex);
        }
    }

    @Override
    public Resource loadFileAsResource(String filename) {

        try {
            Path filePath = getFileStorageLocation().resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return resource;
            }

            throw new RuntimeException("File not found: " + filename);

        } catch (MalformedURLException ex) {
            throw new RuntimeException("File not found: " + filename, ex);
        }
    }

    @Override
    public void deleteFile(String filePath) {

        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                Files.delete(path);
                log.debug("File deleted successfully: {}", filePath);
            } else {
                log.warn("File not found for deletion: {}", filePath);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Could not delete file: " + filePath, ex);
        }
    }

    @Override
    public boolean fileExists(String filename) {

        try {
            Path filePath = getFileStorageLocation().resolve(filename).normalize();
            return Files.exists(filePath);
        } catch (Exception ex) {
            log.error("Error checking if file exists: {}", filename, ex);
            return false;
        }
    }

    @Override
    public Path getFilePath(String filename) {
        return getFileStorageLocation().resolve(filename).normalize();
    }

    @Override
    public void cleanupOrphanedFiles() {

        if (!cleanupEnabled) {
            log.info("File cleanup is disabled");
            return;
        }

        try (Stream<Path> paths = Files.walk(getFileStorageLocation(), 1)) {
            paths.filter(Files::isRegularFile)
                    .filter(this::isFileOrphaned)
                    .forEach(this::deleteOrphanedFile);
        } catch (IOException ex) {
            log.error("Error during orphaned files cleanup", ex);
        }
    }

    private boolean isFileOrphaned(Path filePath) {

        try {
            LocalDateTime lastModified = Files.getLastModifiedTime(filePath)
                    .toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime();

            long daysSinceModified =
                    ChronoUnit.DAYS.between(lastModified, LocalDateTime.now());

            return daysSinceModified > daysThreshold;

        } catch (IOException ex) {
            log.error("Error checking file modification time: {}", filePath, ex);
            return false;
        }
    }

    private void deleteOrphanedFile(Path filePath) {

        try {
            Files.delete(filePath);
            log.info("Deleted orphaned file: {}", filePath.getFileName());
        } catch (IOException ex) {
            log.error("Could not delete orphaned file: {}", filePath, ex);
        }
    }

    private void validateFileSize(MultipartFile file) {

        long maxFileSize = 10 * 1024 * 1024; // 10MB
        if (file.getSize() > maxFileSize) {
            throw new RuntimeException("File size exceeds maximum allowed size of 10MB");
        }

        if (file.getSize() < 1) {
            throw new RuntimeException("File is too small");
        }
    }
}


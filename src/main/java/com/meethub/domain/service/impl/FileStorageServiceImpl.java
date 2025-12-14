package com.meethub.domain.service.impl;

import com.meethub.domain.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

@Slf4j
@Service
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



//// FileStorageServiceImpl.java
//package com.meethub.domain.service.impl;
//
//import com.meethub.domain.service.FileStorageService;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.core.io.Resource;
//import org.springframework.core.io.UrlResource;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.net.MalformedURLException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.StandardCopyOption;
//import java.time.LocalDateTime;
//import java.time.temporal.ChronoUnit;
//import java.util.stream.Stream;
//
//@Slf4j
//@Service
//public class FileStorageServiceImpl implements FileStorageService {
//
//    @Value("${file.upload-dir:uploads/resources}")
//    private String uploadDir;
//
//    private Path fileStorageLocation;
//
//    @Value("${file.cleanup.enabled:true}")
//    private boolean cleanupEnabled;
//
//    @Value("${file.cleanup.days-threshold:30}")
//    private int daysThreshold;
//
//    /**
//     * Inicjalizacja katalogu do przechowywania plików
//     */
//    Path getFileStorageLocation() {
//        if (fileStorageLocation == null) {
//            this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
//            try {
//                Files.createDirectories(this.fileStorageLocation);
//                log.info("File storage location initialized: {}", this.fileStorageLocation);
//            } catch (Exception ex) {
//                throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
//            }
//        }
//        return fileStorageLocation;
//    }
//
//    @Override
//    public String storeFile(MultipartFile file, String filename) {
//        try {
//            // Sprawdź czy nazwa pliku zawiera niebezpieczne znaki
//            if (filename.contains("..")) {
//                throw new RuntimeException("Sorry! Filename contains invalid path sequence " + filename);
//            }
//
//            Path targetLocation = getFileStorageLocation().resolve(filename);
//
//            // Sprawdź czy plik już istnieje
//            if (Files.exists(targetLocation)) {
//                log.warn("File already exists, overwriting: {}", filename);
//            }
//
//            // Kopiuj plik do lokalizacji docelowej
//            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
//
//            log.debug("File stored successfully: {}", targetLocation);
//            return targetLocation.toString();
//
//        } catch (IOException ex) {
//            log.error("Failed to store file: {}", filename, ex);
//            throw new RuntimeException("Could not store file " + filename + ". Please try again!", ex);
//        }
//    }
//
//    @Override
//    public Resource loadFileAsResource(String filename) {
//        try {
//            Path filePath = getFileStorageLocation().resolve(filename).normalize();
//            Resource resource = new UrlResource(filePath.toUri());
//
//            if (resource.exists()) {
//                return resource;
//            } else {
//                log.error("File not found: {}", filename);
//                throw new RuntimeException("File not found: " + filename);
//            }
//        } catch (MalformedURLException ex) {
//            log.error("Malformed URL for file: {}", filename, ex);
//            throw new RuntimeException("File not found: " + filename, ex);
//        }
//    }
//
//    @Override
//    public void deleteFile(String filePath) {
//        try {
//            Path path = Paths.get(filePath);
//            if (Files.exists(path)) {
//                Files.delete(path);
//                log.debug("File deleted successfully: {}", filePath);
//            } else {
//                log.warn("File not found for deletion: {}", filePath);
//            }
//        } catch (IOException ex) {
//            log.error("Could not delete file: {}", filePath, ex);
//            throw new RuntimeException("Could not delete file: " + filePath, ex);
//        }
//    }
//
//    @Override
//    public boolean fileExists(String filename) {
//        try {
//            Path filePath = getFileStorageLocation().resolve(filename).normalize();
//            return Files.exists(filePath);
//        } catch (Exception ex) {
//            log.error("Error checking if file exists: {}", filename, ex);
//            return false;
//        }
//    }
//
//    @Override
//    public Path getFilePath(String filename) {
//        return getFileStorageLocation().resolve(filename).normalize();
//    }
//
//    @Override
//    public void cleanupOrphanedFiles() {
//        if (!cleanupEnabled) {
//            log.info("File cleanup is disabled");
//            return;
//        }
//
//        try {
//            Path storageLocation = getFileStorageLocation();
//
//            try (Stream<Path> paths = Files.walk(storageLocation, 1)) {
//                paths.filter(Files::isRegularFile)
//                        .filter(this::isFileOrphaned)
//                        .forEach(this::deleteOrphanedFile);
//            }
//
//            log.info("Orphaned files cleanup completed");
//        } catch (IOException ex) {
//            log.error("Error during orphaned files cleanup", ex);
//        }
//    }
//
//    /**
//     * Sprawdza czy plik jest porzucony (stary i nieużywany)
//     */
//    private boolean isFileOrphaned(Path filePath) {
//        try {
//            LocalDateTime lastModified = Files.getLastModifiedTime(filePath)
//                    .toInstant()
//                    .atZone(java.time.ZoneId.systemDefault())
//                    .toLocalDateTime();
//
//            long daysSinceModified = ChronoUnit.DAYS.between(lastModified, LocalDateTime.now());
//            return daysSinceModified > daysThreshold;
//
//        } catch (IOException ex) {
//            log.error("Error checking file modification time: {}", filePath, ex);
//            return false;
//        }
//    }
//
//    /**
//     * Usuwa porzucony plik
//     */
//    private void deleteOrphanedFile(Path filePath) {
//        try {
//            Files.delete(filePath);
//            log.info("Deleted orphaned file: {}", filePath.getFileName());
//        } catch (IOException ex) {
//            log.error("Could not delete orphaned file: {}", filePath, ex);
//        }
//    }
//
//    /**
//     * Pobiera rozmiar katalogu z plikami
//     */
//    public long getStorageSize() {
//        try {
//            return Files.walk(getFileStorageLocation())
//                    .filter(Files::isRegularFile)
//                    .mapToLong(this::getFileSize)
//                    .sum();
//        } catch (IOException ex) {
//            log.error("Error calculating storage size", ex);
//            return 0;
//        }
//    }
//
//    private long getFileSize(Path path) {
//        try {
//            return Files.size(path);
//        } catch (IOException ex) {
//            log.error("Error getting file size: {}", path, ex);
//            return 0;
//        }
//    }
//
//    /**
//     * Pobiera statystyki przechowywania plików
//     */
//    public FileStorageStats getStorageStats() {
//        try {
//            long totalSize = getStorageSize();
//            long fileCount = Files.walk(getFileStorageLocation(), 1)
//                    .filter(Files::isRegularFile)
//                    .count();
//
//            return FileStorageStats.builder()
//                    .totalFiles(fileCount)
//                    .totalSize(totalSize)
//                    .storagePath(getFileStorageLocation().toString())
//                    .build();
//
//        } catch (IOException ex) {
//            log.error("Error getting storage stats", ex);
//            return FileStorageStats.builder()
//                    .totalFiles(0L)
//                    .totalSize(0L)
//                    .storagePath(getFileStorageLocation().toString())
//                    .build();
//        }
//    }
//
//    /**
//     * Klasa do przechowywania statystyk przechowywania
//     */
//    public static class FileStorageStats {
//        private Long totalFiles;
//        private Long totalSize;
//        private String storagePath;
//
//        // Builder pattern
//        public static FileStorageStatsBuilder builder() {
//            return new FileStorageStatsBuilder();
//        }
//
//        // Getters
//        public Long getTotalFiles() { return totalFiles; }
//        public Long getTotalSize() { return totalSize; }
//        public String getStoragePath() { return storagePath; }
//
//        // Setters
//        public void setTotalFiles(Long totalFiles) { this.totalFiles = totalFiles; }
//        public void setTotalSize(Long totalSize) { this.totalSize = totalSize; }
//        public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
//
//        // Formatowane wartości
//        public String getTotalSizeFormatted() {
//            if (totalSize == null) return "0 B";
//
//            if (totalSize < 1024) {
//                return totalSize + " B";
//            } else if (totalSize < 1024 * 1024) {
//                return String.format("%.1f KB", totalSize / 1024.0);
//            } else if (totalSize < 1024 * 1024 * 1024) {
//                return String.format("%.1f MB", totalSize / (1024.0 * 1024.0));
//            } else {
//                return String.format("%.1f GB", totalSize / (1024.0 * 1024.0 * 1024.0));
//            }
//        }
//
//        public static class FileStorageStatsBuilder {
//            private Long totalFiles;
//            private Long totalSize;
//            private String storagePath;
//
//            public FileStorageStatsBuilder totalFiles(Long totalFiles) {
//                this.totalFiles = totalFiles;
//                return this;
//            }
//
//            public FileStorageStatsBuilder totalSize(Long totalSize) {
//                this.totalSize = totalSize;
//                return this;
//            }
//
//            public FileStorageStatsBuilder storagePath(String storagePath) {
//                this.storagePath = storagePath;
//                return this;
//            }
//
//            public FileStorageStats build() {
//                FileStorageStats stats = new FileStorageStats();
//                stats.setTotalFiles(totalFiles);
//                stats.setTotalSize(totalSize);
//                stats.setStoragePath(storagePath);
//                return stats;
//            }
//        }
//    }
//}
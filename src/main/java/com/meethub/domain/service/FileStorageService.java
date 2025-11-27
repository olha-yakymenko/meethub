// FileStorageService.java
package com.meethub.domain.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface FileStorageService {

    /**
     * Zapisuje plik na dysku
     */
    String storeFile(MultipartFile file, String filename);

    /**
     * Ładuje plik jako Resource
     */
    Resource loadFileAsResource(String filename);

    /**
     * Usuwa plik z dysku
     */
    void deleteFile(String filePath);

    /**
     * Sprawdza czy plik istnieje
     */
    boolean fileExists(String filename);

    /**
     * Pobiera ścieżkę do pliku
     */
    Path getFilePath(String filename);

    /**
     * Czyści stare/porzucone pliki
     */
    void cleanupOrphanedFiles();
}
// FileStorageService.java
package com.meethub.domain.service;

import jakarta.validation.constraints.*;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

@Validated
public interface FileStorageService {

    /**
     * Zapisuje plik na dysku
     */
    String storeFile(
            @NotNull(message = "Plik nie może być pusty")
            MultipartFile file,

            @NotBlank(message = "Nazwa pliku nie może być pusta")
            @Pattern(
                    regexp = "^[a-zA-Z0-9._\\-\\s]+$",
                    message = "Nazwa pliku może zawierać tylko litery, cyfry, kropki, myślniki i podkreślniki"
            )
            @Size(max = 255, message = "Nazwa pliku nie może przekraczać 255 znaków")
            String filename
    );

    /**
     * Ładuje plik jako Resource
     */
    Resource loadFileAsResource(
            @NotBlank(message = "Nazwa pliku nie może być pusta")
            @Pattern(
                    regexp = "^[a-zA-Z0-9._\\-\\s]+$",
                    message = "Nazwa pliku może zawierać tylko litery, cyfry, kropki, myślniki i podkreślniki"
            )
            @Size(max = 255, message = "Nazwa pliku nie może przekraczać 255 znaków")
            String filename
    );

    /**
     * Usuwa plik z dysku
     */
    void deleteFile(
            @NotBlank(message = "Ścieżka pliku nie może być pusta")
            String filePath
    );

    /**
     * Sprawdza czy plik istnieje
     */
    boolean fileExists(
            @NotBlank(message = "Nazwa pliku nie może być pusta")
            @Pattern(
                    regexp = "^[a-zA-Z0-9._\\-\\s]+$",
                    message = "Nazwa pliku może zawierać tylko litery, cyfry, kropki, myślniki i podkreślniki"
            )
            @Size(max = 255, message = "Nazwa pliku nie może przekraczać 255 znaków")
            String filename
    );

    /**
     * Pobiera ścieżkę do pliku
     */
    Path getFilePath(
            @NotBlank(message = "Nazwa pliku nie może być pusta")
            @Pattern(
                    regexp = "^[a-zA-Z0-9._\\-\\s]+$",
                    message = "Nazwa pliku może zawierać tylko litery, cyfry, kropki, myślniki i podkreślniki"
            )
            @Size(max = 255, message = "Nazwa pliku nie może przekraczać 255 znaków")
            String filename
    );

    /**
     * Czyści stare/porzucone pliki
     */
    void cleanupOrphanedFiles();
}




//// FileStorageService.java
//package com.meethub.domain.service;
//
//import org.springframework.core.io.Resource;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.nio.file.Path;
//
//public interface FileStorageService {
//
//    /**
//     * Zapisuje plik na dysku
//     */
//    String storeFile(MultipartFile file, String filename);
//
//    /**
//     * Ładuje plik jako Resource
//     */
//    Resource loadFileAsResource(String filename);
//
//    /**
//     * Usuwa plik z dysku
//     */
//    void deleteFile(String filePath);
//
//    /**
//     * Sprawdza czy plik istnieje
//     */
//    boolean fileExists(String filename);
//
//    /**
//     * Pobiera ścieżkę do pliku
//     */
//    Path getFilePath(String filename);
//
//    /**
//     * Czyści stare/porzucone pliki
//     */
//    void cleanupOrphanedFiles();
//}
package com.meethub.domain.service;

import jakarta.validation.constraints.*;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

@Validated
public interface FileStorageService {


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


    Resource loadFileAsResource(
            @NotBlank(message = "Nazwa pliku nie może być pusta")
            @Pattern(
                    regexp = "^[a-zA-Z0-9._\\-\\s]+$",
                    message = "Nazwa pliku może zawierać tylko litery, cyfry, kropki, myślniki i podkreślniki"
            )
            @Size(max = 255, message = "Nazwa pliku nie może przekraczać 255 znaków")
            String filename
    );


    void deleteFile(
            @NotBlank(message = "Ścieżka pliku nie może być pusta")
            String filePath
    );


    boolean fileExists(
            @NotBlank(message = "Nazwa pliku nie może być pusta")
            @Pattern(
                    regexp = "^[a-zA-Z0-9._\\-\\s]+$",
                    message = "Nazwa pliku może zawierać tylko litery, cyfry, kropki, myślniki i podkreślniki"
            )
            @Size(max = 255, message = "Nazwa pliku nie może przekraczać 255 znaków")
            String filename
    );


    Path getFilePath(
            @NotBlank(message = "Nazwa pliku nie może być pusta")
            @Pattern(
                    regexp = "^[a-zA-Z0-9._\\-\\s]+$",
                    message = "Nazwa pliku może zawierać tylko litery, cyfry, kropki, myślniki i podkreślniki"
            )
            @Size(max = 255, message = "Nazwa pliku nie może przekraczać 255 znaków")
            String filename
    );


    void cleanupOrphanedFiles();
}



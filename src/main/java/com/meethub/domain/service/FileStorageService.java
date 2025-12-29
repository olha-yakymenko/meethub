package com.meethub.domain.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface FileStorageService {

    String storeFile(MultipartFile file, String filename);
    Resource loadFileAsResource(String filename);
    void deleteFile(String filePath);
    boolean fileExists(String filename);
    Path getFilePath(String filename);
    void cleanupOrphanedFiles();
}
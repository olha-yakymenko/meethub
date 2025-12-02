// FileStorageServiceImplIntegrationTest.java
package com.meethub.domain.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "file.upload-dir=test-integration-uploads",
        "file.cleanup.enabled=true",
        "file.cleanup.days-threshold=1"
})
class FileStorageServiceImplIntegrationTest {

    @Autowired
    private FileStorageServiceImpl fileStorageService;

    private static final String TEST_FILENAME = "integration-test-file.txt";
    private static final String TEST_CONTENT = "Integration test content";

    @BeforeEach
    void setUp() throws IOException {
        // Wyczyść katalog testowy przed każdym testem
        cleanupTestDirectory();
    }

    @AfterEach
    void tearDown() throws IOException {
        // Wyczyść katalog testowy po każdym teście
        cleanupTestDirectory();
    }

    private void cleanupTestDirectory() throws IOException {
        Path testDir = Paths.get("test-integration-uploads");
        if (Files.exists(testDir)) {
            Files.walk(testDir)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            // Ignore cleanup errors
                        }
                    });
            Files.deleteIfExists(testDir);
        }
    }

//    @Test
//    void testFullFileLifecycle() throws IOException {
//        // 1. Store file
//        MockMultipartFile multipartFile = new MockMultipartFile(
//                "file",
//                TEST_FILENAME,
//                "text/plain",
//                TEST_CONTENT.getBytes()
//        );
//
//        String storedPath = fileStorageService.storeFile(multipartFile, TEST_FILENAME);
//        assertNotNull(storedPath);
//        assertTrue(Files.exists(Paths.get(storedPath)));
//
//        // 2. Check file exists
//        assertTrue(fileStorageService.fileExists(TEST_FILENAME));
//
//        // 3. Load file as resource
//        var resource = fileStorageService.loadFileAsResource(TEST_FILENAME);
//        assertNotNull(resource);
//        assertTrue(resource.exists());
//
//        // 4. Get file path
//        Path filePath = fileStorageService.getFilePath(TEST_FILENAME);
//        assertEquals(storedPath, filePath.toString());
//
//        // 5. Get storage stats
//        var stats = fileStorageService.getStorageStats();
//        assertTrue(stats.getTotalFiles() > 0);
//        assertTrue(stats.getTotalSize() > 0);
//
//        // 6. Delete file
//        fileStorageService.deleteFile(storedPath);
//        assertFalse(Files.exists(Paths.get(storedPath)));
//    }

    @Test
    void testMultipleFileOperations() throws IOException {
        // Store multiple files
        for (int i = 1; i <= 3; i++) {
            MockMultipartFile file = new MockMultipartFile(
                    "file" + i,
                    "file" + i + ".txt",
                    "text/plain",
                    ("Content " + i).getBytes()
            );

            fileStorageService.storeFile(file, "file" + i + ".txt");
        }

        // Verify all files exist
        for (int i = 1; i <= 3; i++) {
            assertTrue(fileStorageService.fileExists("file" + i + ".txt"));
        }

        // Verify storage stats
        var stats = fileStorageService.getStorageStats();
        assertEquals(3L, stats.getTotalFiles());
        assertTrue(stats.getTotalSize() > 0);

        // Clean up
        for (int i = 1; i <= 3; i++) {
            fileStorageService.deleteFile(
                    fileStorageService.getFilePath("file" + i + ".txt").toString()
            );
        }
    }
}
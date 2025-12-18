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

    @Test
    public void cleanupTestDirectory() throws IOException {
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

    @Test
    void testFullFileLifecycle() throws IOException {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                TEST_FILENAME,
                "text/plain",
                TEST_CONTENT.getBytes()
        );

        String storedPath = fileStorageService.storeFile(multipartFile, TEST_FILENAME);

        assertAll("Full file lifecycle",
                () -> assertNotNull(storedPath, "Stored path should not be null"),
                () -> assertTrue(Files.exists(Paths.get(storedPath)), "File should exist on disk"),
                () -> assertTrue(fileStorageService.fileExists(TEST_FILENAME), "fileExists should return true"),
                () -> {
                    var resource = fileStorageService.loadFileAsResource(TEST_FILENAME);
                    assertNotNull(resource, "Resource should not be null");
                    assertTrue(resource.exists(), "Resource should exist");
                },
                () -> {
                    Path filePath = fileStorageService.getFilePath(TEST_FILENAME);
                    assertEquals(storedPath, filePath.toString(), "File path should match stored path");
                }
        );
    }


}
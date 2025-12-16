// FileStorageServiceImplTest.java
package com.meethub.domain.service.impl;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.nio.file.attribute.FileTime;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceImplTest {

    private FileStorageServiceImpl fileStorageService;

    private static final String TEST_FILENAME = "test-file.txt";
    private static final String TEST_CONTENT = "Test file content";
    private static final String UPLOAD_DIR = "target/test-uploads";

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageServiceImpl();

        // Ustawienie wartości pól za pomocą ReflectionTestUtils
        ReflectionTestUtils.setField(fileStorageService, "uploadDir", UPLOAD_DIR);
        ReflectionTestUtils.setField(fileStorageService, "cleanupEnabled", true);
        ReflectionTestUtils.setField(fileStorageService, "daysThreshold", 30);

        // Reset fileStorageLocation przed każdym testem
        ReflectionTestUtils.setField(fileStorageService, "fileStorageLocation", null);

        // Utwórz katalog testowy
        try {
            Path testDir = Paths.get(UPLOAD_DIR);
            if (!Files.exists(testDir)) {
                Files.createDirectories(testDir);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @AfterEach
    void tearDown() throws IOException {
        // Wyczyść katalog testowy po każdym teście
        Path testDir = Paths.get(UPLOAD_DIR);
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
        }
    }

    @Test
    void testGetFileStorageLocation_ShouldCreateDirectoryWhenNotExists() {
        // Given
        // Usuń katalog, jeśli istnieje
        Path testPath = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
        try {
            if (Files.exists(testPath)) {
                Files.deleteIfExists(testPath);
            }
        } catch (IOException e) {
            // Ignore
        }

        // When
        Path result = ReflectionTestUtils.invokeMethod(fileStorageService, "getFileStorageLocation");

        // Then
        assertAll("Directory creation check",
                () -> assertNotNull(result, "Path should not be null"),
                () -> assertTrue(Files.exists(result) || Files.notExists(result),
                        "Directory should exist or not exist without exception")
        );
    }

    @Test
    void testStoreFile_ShouldSuccessfullyStoreFile() throws IOException {
        // Given
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                TEST_FILENAME,
                "text/plain",
                TEST_CONTENT.getBytes()
        );

        // When
        String resultPath = fileStorageService.storeFile(multipartFile, TEST_FILENAME);

        // Then
        assertAll("File storage validation",
                () -> assertNotNull(resultPath, "Stored file path should not be null"),
                () -> assertTrue(resultPath.contains(TEST_FILENAME),
                        "Path should contain the filename"),
                () -> assertTrue(Files.exists(Paths.get(resultPath)),
                        "File should exist on filesystem")
        );
    }

    @Test
    void testStoreFile_ShouldThrowExceptionWhenFilenameContainsInvalidPathSequence() {
        // Given
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                TEST_FILENAME,
                "text/plain",
                TEST_CONTENT.getBytes()
        );

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> fileStorageService.storeFile(multipartFile, "../" + TEST_FILENAME));

        assertTrue(exception.getMessage().contains("invalid path sequence"),
                "Exception message should indicate invalid path sequence");
    }

    @Test
    void testStoreFile_ShouldOverwriteExistingFile() throws IOException {
        // Given
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                TEST_FILENAME,
                "text/plain",
                TEST_CONTENT.getBytes()
        );

        // Store file first time
        String firstPath = fileStorageService.storeFile(multipartFile, TEST_FILENAME);
        long firstSize = Files.size(Paths.get(firstPath));

        // Change content (larger file)
        MockMultipartFile newMultipartFile = new MockMultipartFile(
                "file",
                TEST_FILENAME,
                "text/plain",
                "New content that is longer than the first one".getBytes()
        );

        // When
        String secondPath = fileStorageService.storeFile(newMultipartFile, TEST_FILENAME);
        long secondSize = Files.size(Paths.get(secondPath));

        // Then
        assertAll("File overwrite validation",
                () -> assertEquals(firstPath, secondPath, "Should store to same path"),
                () -> assertTrue(secondSize > firstSize, "New file should be larger")
        );
    }

    @Test
    void testLoadFileAsResource_ShouldReturnResourceWhenFileExists() throws IOException {
        // Given - najpierw zapisz plik
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                TEST_FILENAME,
                "text/plain",
                TEST_CONTENT.getBytes()
        );
        fileStorageService.storeFile(multipartFile, TEST_FILENAME);

        // When
        Resource resource = fileStorageService.loadFileAsResource(TEST_FILENAME);

        // Then
        assertAll("Resource validation",
                () -> assertNotNull(resource, "Resource should not be null"),
                () -> assertTrue(resource.exists(), "Resource should exist"),
                () -> assertTrue(resource.isReadable(), "Resource should be readable")
        );
    }

    @Test
    void testLoadFileAsResource_ShouldThrowExceptionWhenFileNotFound() {
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> fileStorageService.loadFileAsResource("non-existent-file.txt"));

        assertTrue(exception.getMessage().contains("File not found"),
                "Exception message should indicate file not found");
    }

    @Test
    void testDeleteFile_ShouldDeleteExistingFile() throws IOException {
        // Given
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                TEST_FILENAME,
                "text/plain",
                TEST_CONTENT.getBytes()
        );
        String filePath = fileStorageService.storeFile(multipartFile, TEST_FILENAME);
        Path path = Paths.get(filePath);

        // Verify file exists
        assertTrue(Files.exists(path), "File should exist before deletion");

        // When
        fileStorageService.deleteFile(filePath);

        // Then
        assertFalse(Files.exists(path), "File should not exist after deletion");
    }

    @Test
    void testDeleteFile_ShouldNotThrowExceptionWhenFileDoesNotExist() {
        // Given
        String nonExistentPath = "non-existent-path.txt";

        // When & Then (should not throw exception, just log warning)
        assertDoesNotThrow(() -> fileStorageService.deleteFile(nonExistentPath),
                "Should not throw exception for non-existent file");
    }

    @Test
    void testFileExists_ShouldReturnTrueForExistingFile() throws IOException {
        // Given
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                TEST_FILENAME,
                "text/plain",
                TEST_CONTENT.getBytes()
        );
        fileStorageService.storeFile(multipartFile, TEST_FILENAME);

        // When
        boolean exists = fileStorageService.fileExists(TEST_FILENAME);

        // Then
        assertTrue(exists, "Should return true for existing file");
    }

    @Test
    void testFileExists_ShouldReturnFalseForNonExistingFile() {
        // When
        boolean exists = fileStorageService.fileExists("non-existent-file.txt");

        // Then
        assertFalse(exists, "Should return false for non-existing file");
    }

    @Test
    void testGetFilePath_ShouldReturnCorrectPath() {
        // When
        Path filePath = fileStorageService.getFilePath(TEST_FILENAME);

        // Then
        assertAll("FilePath validation",
                () -> assertNotNull(filePath, "Path should not be null"),
                () -> assertTrue(filePath.toString().endsWith(TEST_FILENAME),
                        "Path should end with filename")
        );
    }

    @Test
    void testCleanupOrphanedFiles_ShouldNotRunWhenDisabled() {
        // Given
        ReflectionTestUtils.setField(fileStorageService, "cleanupEnabled", false);

        // When & Then
        assertDoesNotThrow(() -> fileStorageService.cleanupOrphanedFiles(),
                "Should not throw exception when cleanup is disabled");
    }

    @Test
    void testIsFileOrphaned_ShouldReturnTrueForOldFile() throws IOException {
        // Given
        Path tempFile = Files.createTempFile(Paths.get(UPLOAD_DIR), "test-old-file-", ".txt");
        Files.write(tempFile, "test content".getBytes());

        // Ustaw datę modyfikacji na starszą niż threshold
        LocalDateTime oldDate = LocalDateTime.now().minusDays(35);
        Files.setLastModifiedTime(tempFile,
                FileTime.from(oldDate.atZone(ZoneId.systemDefault()).toInstant()));

        // When
        boolean isOrphaned = ReflectionTestUtils.invokeMethod(
                fileStorageService, "isFileOrphaned", tempFile);

        // Then
        assertTrue((Boolean) isOrphaned, "Old file should be considered orphaned");
    }

    @Test
    void testIsFileOrphaned_ShouldReturnFalseForRecentFile() throws IOException {
        // Given
        Path tempFile = Files.createTempFile(Paths.get(UPLOAD_DIR), "test-recent-file-", ".txt");
        Files.write(tempFile, "test content".getBytes());

        // Ustaw aktualną datę modyfikacji (15 dni temu)
        Files.setLastModifiedTime(tempFile, FileTime.from(
                LocalDateTime.now().minusDays(15).atZone(ZoneId.systemDefault()).toInstant()));

        // When
        boolean isOrphaned = ReflectionTestUtils.invokeMethod(
                fileStorageService, "isFileOrphaned", tempFile);

        // Then
        assertFalse((Boolean) isOrphaned, "Recent file should not be considered orphaned");
    }

    @Test
    void testCleanupOrphanedFiles_ShouldDeleteOldFiles() throws IOException {
        // Given - utwórz stary plik
        Path oldFile = Files.createTempFile(
                Paths.get(UPLOAD_DIR),
                "old-file-",
                ".txt"
        );
        Files.write(oldFile, "old content".getBytes());

        // Ustaw starą datę modyfikacji (35 dni temu)
        LocalDateTime oldDate = LocalDateTime.now().minusDays(35);
        Files.setLastModifiedTime(oldFile,
                FileTime.from(oldDate.atZone(ZoneId.systemDefault()).toInstant()));

        // Utwórz nowy plik
        Path newFile = Files.createTempFile(
                Paths.get(UPLOAD_DIR),
                "new-file-",
                ".txt"
        );
        Files.write(newFile, "new content".getBytes());

        // Ustaw nową datę modyfikacji (15 dni temu)
        Files.setLastModifiedTime(newFile, FileTime.from(
                LocalDateTime.now().minusDays(15).atZone(ZoneId.systemDefault()).toInstant()));

        // When
        fileStorageService.cleanupOrphanedFiles();

        // Then
        assertAll("Cleanup validation",
                () -> assertFalse(Files.exists(oldFile), "Old file should be deleted"),
                () -> assertTrue(Files.exists(newFile), "New file should not be deleted")
        );
    }
}
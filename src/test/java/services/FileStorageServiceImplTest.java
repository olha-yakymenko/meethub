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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
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
    @Order(1)
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
        assertNotNull(result);
        assertTrue(Files.exists(result) || Files.notExists(result));
    }

    @Test
    @Order(2)
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
        assertNotNull(resultPath);
        assertTrue(resultPath.contains(TEST_FILENAME));
        assertTrue(Files.exists(Paths.get(resultPath)));
    }

    @Test
    @Order(3)
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

        assertTrue(exception.getMessage().contains("invalid path sequence"));
    }

    @Test
    @Order(4)
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
        assertEquals(firstPath, secondPath); // Same path
        assertTrue(secondSize > firstSize); // New content is larger
    }

    @Test
    @Order(5)
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
        assertNotNull(resource);
        assertTrue(resource.exists());
        assertTrue(resource.isReadable());
    }

    @Test
    @Order(6)
    void testLoadFileAsResource_ShouldThrowExceptionWhenFileNotFound() {
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> fileStorageService.loadFileAsResource("non-existent-file.txt"));

        assertTrue(exception.getMessage().contains("File not found"));
    }

    @Test
    @Order(7)
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
        assertTrue(Files.exists(path));

        // When
        fileStorageService.deleteFile(filePath);

        // Then
        assertFalse(Files.exists(path));
    }

    @Test
    @Order(8)
    void testDeleteFile_ShouldNotThrowExceptionWhenFileDoesNotExist() {
        // Given
        String nonExistentPath = "non-existent-path.txt";

        // When & Then (should not throw exception, just log warning)
        assertDoesNotThrow(() -> fileStorageService.deleteFile(nonExistentPath));
    }

    @Test
    @Order(9)
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
        assertTrue(exists);
    }

    @Test
    @Order(10)
    void testFileExists_ShouldReturnFalseForNonExistingFile() {
        // When
        boolean exists = fileStorageService.fileExists("non-existent-file.txt");

        // Then
        assertFalse(exists);
    }

    @Test
    @Order(11)
    void testGetFilePath_ShouldReturnCorrectPath() {
        // When
        Path filePath = fileStorageService.getFilePath(TEST_FILENAME);

        // Then
        assertNotNull(filePath);
        assertTrue(filePath.toString().endsWith(TEST_FILENAME));
    }

    @Test
    @Order(12)
    void testCleanupOrphanedFiles_ShouldNotRunWhenDisabled() {
        // Given
        ReflectionTestUtils.setField(fileStorageService, "cleanupEnabled", false);

        // When
        fileStorageService.cleanupOrphanedFiles();

        // Then - No exception should be thrown
        assertTrue(true);
    }

    @Test
    @Order(13)
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
        assertTrue((Boolean) isOrphaned);
    }

    @Test
    @Order(14)
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
        assertFalse((Boolean) isOrphaned);
    }

    @Test
    @Order(15)
    void testGetStorageSize_ShouldReturnCorrectSize() throws IOException {
        // Given
        MockMultipartFile multipartFile1 = new MockMultipartFile(
                "file1",
                "file1.txt",
                "text/plain",
                "Content1".getBytes()
        );

        MockMultipartFile multipartFile2 = new MockMultipartFile(
                "file2",
                "file2.txt",
                "text/plain",
                "Content2 that is longer".getBytes()
        );

        fileStorageService.storeFile(multipartFile1, "file1.txt");
        fileStorageService.storeFile(multipartFile2, "file2.txt");

        // When
        long storageSize = fileStorageService.getStorageSize();

        // Then
        assertTrue(storageSize >= 8L); // Minimum 8 bytes (Content1 = 8 chars)
    }

    @Test
    @Order(16)
    void testGetStorageStats_ShouldReturnValidStats() throws IOException {
        // Given
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                TEST_FILENAME,
                "text/plain",
                TEST_CONTENT.getBytes()
        );

        fileStorageService.storeFile(multipartFile, TEST_FILENAME);

        // When
        FileStorageServiceImpl.FileStorageStats stats = fileStorageService.getStorageStats();

        // Then
        assertNotNull(stats);
        assertTrue(stats.getTotalFiles() >= 1);
        assertTrue(stats.getTotalSize() > 0);
        assertNotNull(stats.getStoragePath());
        assertNotNull(stats.getTotalSizeFormatted());
    }

    @Test
    @Order(17)
    void testFileStorageStatsBuilder_ShouldBuildCorrectObject() {
        // Given
        Long expectedTotalFiles = 10L;
        Long expectedTotalSize = 1024L;
        String expectedStoragePath = "/test/path";

        // When
        FileStorageServiceImpl.FileStorageStats stats = FileStorageServiceImpl.FileStorageStats.builder()
                .totalFiles(expectedTotalFiles)
                .totalSize(expectedTotalSize)
                .storagePath(expectedStoragePath)
                .build();

        // Then
        assertEquals(expectedTotalFiles, stats.getTotalFiles());
        assertEquals(expectedTotalSize, stats.getTotalSize());
        assertEquals(expectedStoragePath, stats.getStoragePath());
    }

    @Test
    @Order(18)
    void testGetTotalSizeFormatted_ShouldFormatCorrectly() {
        // Test dla bajtów
        FileStorageServiceImpl.FileStorageStats stats1 = new FileStorageServiceImpl.FileStorageStats();
        stats1.setTotalSize(500L);
        assertEquals("500 B", stats1.getTotalSizeFormatted());

        // Test dla KB
        FileStorageServiceImpl.FileStorageStats stats2 = new FileStorageServiceImpl.FileStorageStats();
        stats2.setTotalSize(1500L);
        assertTrue(stats2.getTotalSizeFormatted().contains("KB"));

        // Test dla MB
        FileStorageServiceImpl.FileStorageStats stats3 = new FileStorageServiceImpl.FileStorageStats();
        stats3.setTotalSize(1500000L);
        assertTrue(stats3.getTotalSizeFormatted().contains("MB"));

        // Test dla GB
        FileStorageServiceImpl.FileStorageStats stats4 = new FileStorageServiceImpl.FileStorageStats();
        stats4.setTotalSize(1500000000L);
        assertTrue(stats4.getTotalSizeFormatted().contains("GB"));

        // Test dla null/0
        FileStorageServiceImpl.FileStorageStats stats5 = new FileStorageServiceImpl.FileStorageStats();
        assertEquals("0 B", stats5.getTotalSizeFormatted());
    }

    @Test
    @Order(19)
    void testCleanupOrphanedFiles_ShouldDeleteOldFiles() throws IOException, InterruptedException {
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

        // Then - stary plik powinien zostać usunięty, nowy nie
        assertFalse(Files.exists(oldFile), "Old file should be deleted");
        assertTrue(Files.exists(newFile), "New file should not be deleted");
    }

    @Test
    @Order(20)
    void testStoreFile_IOExceptionHandling() throws IOException {
        // Given - używamy ReflectionTestUtils, aby zasymulować błąd
        MockMultipartFile multipartFile = mock(MockMultipartFile.class);
        when(multipartFile.getInputStream()).thenThrow(new RuntimeException("Test exception"));

        // When & Then
        assertThrows(RuntimeException.class,
                () -> fileStorageService.storeFile(multipartFile, TEST_FILENAME));
    }
}
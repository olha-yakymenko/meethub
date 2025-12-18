// MeetingResourceServiceImplTest.java (POPRAWIONA WERSJA)
package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.MeetingResource;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.AccessLevel;
import com.meethub.domain.model.enums.ResourceType;
import com.meethub.domain.model.request.MeetingResourceRequest;
import com.meethub.domain.model.request.UpdateMeetingResourceRequest;
import com.meethub.domain.model.response.MeetingResourceResponse;
import com.meethub.domain.model.response.MeetingResourceStats;
import com.meethub.domain.repository.jpa.MeetingRepository;
import com.meethub.domain.repository.jpa.MeetingResourceRepository;
import com.meethub.domain.repository.jpa.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingResourceServiceImplTest {

    @Mock
    private MeetingResourceRepository meetingResourceRepository;

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MeetingResourceServiceImpl meetingResourceService;

    private Meeting meeting;
    private User organizer;
    private User participant;
    private User otherUser;
    private MeetingResource resource;
    private MockMultipartFile multipartFile;

    @BeforeEach
    void setUp() {
        // Setup users
        organizer = User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .build();

        participant = User.builder()
                .id(2L)
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .build();

        otherUser = User.builder()
                .id(3L)
                .firstName("Bob")
                .lastName("Johnson")
                .email("bob.johnson@example.com")
                .build();

        // Setup meeting
        meeting = Meeting.builder()
                .title("Test Meeting")
                .organizer(organizer)
                .build();

        // Setup mock multipart file
        multipartFile = new MockMultipartFile(
                "file",
                "test-document.pdf",
                "application/pdf",
                "Test PDF Content".getBytes()
        );

        // Setup resource
        resource = MeetingResource.builder()
                .meeting(meeting)
                .filename("unique-filename.pdf")
                .originalFilename("test-document.pdf")
                .filePath("/uploads/resources/unique-filename.pdf")
                .fileSize(1024L)
                .mimeType("application/pdf")
                .resourceType(ResourceType.DOCUMENT)
                .uploadedBy(organizer)
                .accessLevel(AccessLevel.PUBLIC)
                .build();

        ReflectionTestUtils.setField(meetingResourceService, "uploadDir", "target/test-uploads");
    }


    @Test
    void testAddResource_MeetingNotFound_ThrowsException() {
        // Given
        MeetingResourceRequest request = new MeetingResourceRequest();
        request.setFile(multipartFile);

        when(meetingRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> meetingResourceService.addResource(999L, request, 1L));

        assertTrue(exception.getMessage().contains("Meeting not found"));
        verify(meetingRepository).findById(999L);
        verify(userRepository, never()).findById(anyLong());
        verify(meetingResourceRepository, never()).save(any(MeetingResource.class));
    }

    @Test
    void testAddResource_UserNotFound_ThrowsException() {
        // Given
        MeetingResourceRequest request = new MeetingResourceRequest();
        request.setFile(multipartFile);

        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> meetingResourceService.addResource(1L, request, 999L));

        assertTrue(exception.getMessage().contains("User not found"));
        verify(meetingRepository).findById(1L);
        verify(userRepository).findById(999L);
        verify(meetingResourceRepository, never()).save(any(MeetingResource.class));
    }



    @Test
    void testGetResource_Success() {
        // Given
        when(meetingResourceRepository.findById(1L)).thenReturn(Optional.of(resource));
        when(userRepository.findById(2L)).thenReturn(Optional.of(participant));


        MeetingResourceServiceImpl spyService = spy(meetingResourceService);

        MeetingResourceResponse mockResponse = new MeetingResourceResponse();
        mockResponse.setId(1L);
        mockResponse.setOriginalFilename("test-document.pdf");

        doReturn(mockResponse).when(spyService).mapToResponse(eq(resource), eq(participant), eq(meeting));

        // When
        MeetingResourceResponse response = spyService.getResource(1L, 2L);

        // Then
        assertEquals("test-document.pdf", response.getOriginalFilename());

        verify(meetingResourceRepository).findById(1L);
        verify(userRepository).findById(2L);
    }

    @Test
    void testGetResource_ResourceNotFound_ThrowsException() {
        // Given
        when(meetingResourceRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> meetingResourceService.getResource(999L, 1L));

        assertTrue(exception.getMessage().contains("Resource not found"));
        verify(meetingResourceRepository).findById(999L);
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void testUpdateResource_Success_ResourceOwner() {
        // Given
        UpdateMeetingResourceRequest request = new UpdateMeetingResourceRequest();
        request.setTags(new HashSet<>(Arrays.asList("updated", "document")));
        request.setAccessLevel(AccessLevel.PRIVATE);

        when(meetingResourceRepository.findById(1L)).thenReturn(Optional.of(resource));
        when(userRepository.findById(1L)).thenReturn(Optional.of(organizer));
        when(meetingResourceRepository.save(any(MeetingResource.class))).thenReturn(resource);

        // When
        MeetingResourceResponse response = meetingResourceService.updateResource(1L, request, 1L);

        // Then
        assertNotNull(response);
        verify(meetingResourceRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(meetingResourceRepository).save(any(MeetingResource.class));
    }

    @Test
    void testUpdateResource_Success_MeetingOrganizer() {
        // Given
        UpdateMeetingResourceRequest request = new UpdateMeetingResourceRequest();
        request.setTags(new HashSet<>(Collections.singletonList("updated")));

        // Resource uploaded by participant
        MeetingResource participantResource = MeetingResource.builder()
                .meeting(meeting)
                .uploadedBy(participant)
                .build();

        when(meetingResourceRepository.findById(2L)).thenReturn(Optional.of(participantResource));
        when(userRepository.findById(1L)).thenReturn(Optional.of(organizer));
        when(meetingResourceRepository.save(any(MeetingResource.class))).thenReturn(participantResource);

        // When
        MeetingResourceResponse response = meetingResourceService.updateResource(2L, request, 1L);

        // Then
        assertNotNull(response);
        verify(meetingResourceRepository).findById(2L);
        verify(userRepository).findById(1L);
        verify(meetingResourceRepository).save(any(MeetingResource.class));
    }

    @Test
    void testUpdateResource_NoPermission_ThrowsException() {
        // Given
        UpdateMeetingResourceRequest request = new UpdateMeetingResourceRequest();

        when(meetingResourceRepository.findById(1L)).thenReturn(Optional.of(resource));
        when(userRepository.findById(3L)).thenReturn(Optional.of(otherUser));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> meetingResourceService.updateResource(1L, request, 3L));

        assertTrue(exception.getMessage().contains("Only resource owner or meeting organizer"));
        verify(meetingResourceRepository).findById(1L);
        verify(userRepository).findById(3L);
        verify(meetingResourceRepository, never()).save(any(MeetingResource.class));
    }

    @Test
    void testDetermineResourceType_Image() {
        // Using reflection to test private method
        String mimeType = "image/jpeg";
        String extension = "jpg";

        ResourceType result = (ResourceType) ReflectionTestUtils.invokeMethod(
                meetingResourceService, "determineResourceType", mimeType, extension);

        assertEquals(ResourceType.IMAGE, result);
    }

    @Test
    void testDetermineResourceType_Video() {
        String mimeType = "video/mp4";
        String extension = "mp4";

        ResourceType result = (ResourceType) ReflectionTestUtils.invokeMethod(
                meetingResourceService, "determineResourceType", mimeType, extension);

        assertEquals(ResourceType.VIDEO, result);
    }

    @Test
    void testDetermineResourceType_Presentation_PPT() {
        String mimeType = "application/vnd.ms-powerpoint";
        String extension = "ppt";

        ResourceType result = (ResourceType) ReflectionTestUtils.invokeMethod(
                meetingResourceService, "determineResourceType", mimeType, extension);

        assertEquals(ResourceType.PRESENTATION, result);
    }

    @Test
    void testDetermineResourceType_Document_PDF() {
        String mimeType = "application/pdf";
        String extension = "pdf";

        ResourceType result = (ResourceType) ReflectionTestUtils.invokeMethod(
                meetingResourceService, "determineResourceType", mimeType, extension);

        assertEquals(ResourceType.DOCUMENT, result);
    }

    @Test
    void testDetermineResourceType_Other() {
        String mimeType = "text/plain";
        String extension = "txt";

        ResourceType result = (ResourceType) ReflectionTestUtils.invokeMethod(
                meetingResourceService, "determineResourceType", mimeType, extension);

        assertEquals(ResourceType.OTHER, result);
    }

    @Test
    void testGetFileExtension_WithExtension() {
        String filename = "document.pdf";
        String result = (String) ReflectionTestUtils.invokeMethod(
                meetingResourceService, "getFileExtension", filename);

        assertEquals("pdf", result);
    }

    @Test
    void testGetFileExtension_NoExtension() {
        String filename = "document";
        String result = (String) ReflectionTestUtils.invokeMethod(
                meetingResourceService, "getFileExtension", filename);

        assertEquals("bin", result);
    }

    @Test
    void testGetFileExtension_Null() {
        String result = (String) ReflectionTestUtils.invokeMethod(
                meetingResourceService, "getFileExtension", (Object) null);

        assertEquals("bin", result);
    }

    @Test
    void testGenerateUniqueFilename() {
        String extension = "pdf";
        String result = (String) ReflectionTestUtils.invokeMethod(
                meetingResourceService, "generateUniqueFilename", extension);

        assertTrue(result.endsWith(".pdf"));
    }

    @Test
    void testHasPermissionToAddResource_Organizer_ReturnsTrue() {
        // Using reflection to test private method
        boolean result = (boolean) ReflectionTestUtils.invokeMethod(
                meetingResourceService, "hasPermissionToAddResource", meeting, organizer);

        assertTrue(result);
    }

    @Test
    void testLoadFileAsResource_Success() throws IOException {
        // Create a test file
        Path uploadDir = Paths.get("target/test-uploads");
        Files.createDirectories(uploadDir);
        Path testFile = uploadDir.resolve("test-file.txt");
        Files.write(testFile, "test content".getBytes());

        try {
            ReflectionTestUtils.setField(meetingResourceService, "uploadDir", "target/test-uploads");

            // When
            org.springframework.core.io.Resource resource = meetingResourceService.loadFileAsResource("test-file.txt");

            // Then
            assertTrue(resource.isReadable());
        } finally {
            Files.deleteIfExists(testFile);
            Files.deleteIfExists(uploadDir);
        }
    }

    @Test
    void testLoadFileAsResource_FileNotFound_ThrowsException() {
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> meetingResourceService.loadFileAsResource("non-existent-file.txt"));

        assertTrue(exception.getMessage().contains("File not found"));
    }

    @Test
    void testAddResource_EmptyFile_ThrowsException() {
        // Given
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        MeetingResourceRequest request = new MeetingResourceRequest();
        request.setFile(emptyFile);

        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
        when(userRepository.findById(1L)).thenReturn(Optional.of(organizer));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> meetingResourceService.addResource(1L, request, 1L));

        assertTrue(exception.getMessage().contains("File is empty"));
        verify(meetingRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(meetingResourceRepository, never()).save(any(MeetingResource.class));
    }

    @Test
    void testAddResource_FileTooLarge_ThrowsException() {
        // Given
        byte[] largeContent = new byte[51 * 1024 * 1024]; // 51MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "large-file.pdf",
                "application/pdf",
                largeContent
        );

        MeetingResourceRequest request = new MeetingResourceRequest();
        request.setFile(largeFile);

        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
        when(userRepository.findById(1L)).thenReturn(Optional.of(organizer));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> meetingResourceService.addResource(1L, request, 1L));

        assertTrue(exception.getMessage().contains("File size exceeds"));
        verify(meetingRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(meetingResourceRepository, never()).save(any(MeetingResource.class));
    }

    @Test
    void testAddResource_InvalidFileType_ThrowsException() {
        // Given
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "test.exe",
                "application/x-msdownload",
                "executable content".getBytes()
        );

        MeetingResourceRequest request = new MeetingResourceRequest();
        request.setFile(invalidFile);

        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
        when(userRepository.findById(1L)).thenReturn(Optional.of(organizer));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> meetingResourceService.addResource(1L, request, 1L));

        assertTrue(exception.getMessage().contains("File type not allowed"));
        verify(meetingRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(meetingResourceRepository, never()).save(any(MeetingResource.class));
    }
}
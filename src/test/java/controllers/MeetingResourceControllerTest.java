package com.meethub.controller.api;

import com.meethub.domain.model.enums.ResourceType;
import com.meethub.domain.model.request.MeetingResourceRequest;
import com.meethub.domain.model.request.UpdateMeetingResourceRequest;
import com.meethub.domain.model.response.ApiResponse;
import com.meethub.domain.model.response.MeetingResourceResponse;
import com.meethub.domain.model.response.MeetingResourceStats;
import com.meethub.domain.service.FileStorageService;
import com.meethub.domain.service.MeetingResourceService;
import com.meethub.security.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingResourceControllerTest {

    @Mock
    private MeetingResourceService meetingResourceService;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private MeetingResourceController controller;

    private CustomUserDetailsService.CustomUserDetails mockUserDetails;

    @BeforeEach
    void setUp() {
        mockUserDetails = mock(CustomUserDetailsService.CustomUserDetails.class);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                mockUserDetails, null, null); // authorities mogą być null
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void addResource_ShouldReturnSuccess_WhenValidRequest() {
        // Given
        Long meetingId = 1L;
        MeetingResourceRequest request = new MeetingResourceRequest();

        MeetingResourceResponse mockResponse = MeetingResourceResponse.builder()
                .id(1L)
                .build();

        // Stubowanie tylko w tym teście
        when(mockUserDetails.getId()).thenReturn(1L);

        when(meetingResourceService.addResource(eq(meetingId), any(MeetingResourceRequest.class), eq(1L)))
                .thenReturn(mockResponse);

        // When
        ResponseEntity<ApiResponse<MeetingResourceResponse>> response =
                controller.addResource(meetingId, request, mockUserDetails);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Zasób został dodany pomyślnie", response.getBody().getMessage());
        assertEquals(mockResponse, response.getBody().getData());
    }

    @Test
    void getResources_ShouldReturnResourceList() {
        // Given
        Long meetingId = 1L;
        List<MeetingResourceResponse> mockResources = Arrays.asList(
                MeetingResourceResponse.builder().id(1L).build(),
                MeetingResourceResponse.builder().id(2L).build()
        );

        // Stubowanie tylko w tym teście
        when(mockUserDetails.getId()).thenReturn(1L);

        when(meetingResourceService.getMeetingResources(meetingId, 1L))
                .thenReturn(mockResources);

        // When
        ResponseEntity<ApiResponse<List<MeetingResourceResponse>>> response =
                controller.getResources(meetingId, mockUserDetails);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().getData().size());
    }

    @Test
    void getResource_ShouldReturnSingleResource() {
        // Given
        Long meetingId = 1L;
        Long resourceId = 1L;
        MeetingResourceResponse mockResponse = MeetingResourceResponse.builder()
                .id(resourceId)
                .build();

        // Stubowanie tylko w tym teście
        when(mockUserDetails.getId()).thenReturn(1L);

        when(meetingResourceService.getResource(resourceId, 1L))
                .thenReturn(mockResponse);

        // When
        ResponseEntity<ApiResponse<MeetingResourceResponse>> response =
                controller.getResource(meetingId, resourceId, mockUserDetails);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(resourceId, response.getBody().getData().getId());
    }

    @Test
    void deleteResource_ShouldReturnSuccess() {
        // Given
        Long meetingId = 1L;
        Long resourceId = 1L;

        // Stubowanie tylko w tym teście
        when(mockUserDetails.getId()).thenReturn(1L);

        doNothing().when(meetingResourceService).deleteResource(resourceId, 1L);

        // When
        ResponseEntity<ApiResponse<Void>> response =
                controller.deleteResource(meetingId, resourceId, mockUserDetails);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        verify(meetingResourceService, times(1)).deleteResource(resourceId, 1L);
    }
}
package com.meethub.controller.web;

import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.request.MeetingResourceRequest;
import com.meethub.domain.model.response.MeetingResourceResponse;
import com.meethub.domain.service.MeetingResourceService;
import com.meethub.domain.service.MeetingService;
import com.meethub.security.CustomUserDetailsService;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingResourceWebControllerUnitTest {

    @Mock
    private MeetingService meetingService;

    @Mock
    private MeetingResourceService meetingResourceService;

    @Mock
    private Model model;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private MeetingResourceWebController controller;

    private CustomUserDetailsService.CustomUserDetails userDetails;
    private Meeting mockMeeting;
    private MeetingResourceResponse mockResource;

    @BeforeEach
    void setUp() {
        // Setup user details - tylko metody, które są faktycznie używane
        userDetails = mock(CustomUserDetailsService.CustomUserDetails.class);
        when(userDetails.getId()).thenReturn(1L);

        // Setup meeting
        mockMeeting = mock(Meeting.class);

        // Setup resource
        mockResource = MeetingResourceResponse.builder()
                .id(200L)
                .description("Resource Description")
                .fileSize(1024L)
                .build();
    }

    // ==================== TESTS FOR SHOW ADD RESOURCE FORM ====================

    @Test
    void showAddResourceForm_shouldReturnForm_whenValidInput() {
        // Given
        Long meetingId = 100L;
        when(meetingService.getMeeting(meetingId)).thenReturn(mockMeeting);

        // When
        String viewName = controller.showAddResourceForm(meetingId, model, userDetails);

        // Then
        assertEquals("meetings/resources/add-resource", viewName);
        verify(model).addAttribute("meeting", mockMeeting);
        verify(model).addAttribute(eq("meetingResourceRequest"), any(MeetingResourceRequest.class));
    }

    @Test
    void showAddResourceForm_shouldHandleInvalidMeetingId() {
        // Given
        Long invalidMeetingId = -1L;
        // Kontroler używa @Validated, więc walidacja parametrów działa na poziomie Spring AOP
        // W unit testach nie mamy Spring AOP, więc testujemy zachowanie bez walidacji

        // When
        String viewName = controller.showAddResourceForm(invalidMeetingId, model, userDetails);

        // Then - Bez walidacji Spring AOP, metoda próbuje pobrać spotkanie
        // Mockujemy wyjątek dla -1
        verify(meetingService).getMeeting(invalidMeetingId);
    }


    @Test
    void showAddResourceForm_shouldNotAddRequest_whenAlreadyInModel() {
        // Given
        Long meetingId = 100L;
        when(meetingService.getMeeting(meetingId)).thenReturn(mockMeeting);
        when(model.containsAttribute("meetingResourceRequest")).thenReturn(true);

        // When
        String viewName = controller.showAddResourceForm(meetingId, model, userDetails);

        // Then
        assertEquals("meetings/resources/add-resource", viewName);
        verify(model, never()).addAttribute(eq("meetingResourceRequest"), any());
    }

    // ==================== TESTS FOR ADD RESOURCE ====================

    @Test
    void addResource_shouldAddSuccessfully_whenValidRequest() {
        // Given
        Long meetingId = 100L;
        MeetingResourceRequest request = MeetingResourceRequest.builder()
                .description("Resource Description")
                .build();

        when(meetingService.getMeeting(meetingId)).thenReturn(mockMeeting);
        when(bindingResult.hasErrors()).thenReturn(false);

        // When
        String viewName = controller.addResource(meetingId, request, bindingResult, model,
                redirectAttributes, userDetails);

        // Then
        assertEquals("redirect:/meetings/100/resources", viewName);
        verify(meetingResourceService).addResource(meetingId, request, 1L);
        verify(redirectAttributes).addFlashAttribute("success", "Zasób został dodany pomyślnie");
        verify(model).addAttribute("meeting", mockMeeting);
    }

    @Test
    void addResource_shouldReturnForm_whenValidationErrors() {
        // Given
        Long meetingId = 100L;
        MeetingResourceRequest request = new MeetingResourceRequest();
        when(meetingService.getMeeting(meetingId)).thenReturn(mockMeeting);
        when(bindingResult.hasErrors()).thenReturn(true);

        List<ObjectError> errors = List.of(
                new FieldError("meetingResourceRequest", "name", "Name is required")
        );
        when(bindingResult.getAllErrors()).thenReturn(errors);

        // When
        String viewName = controller.addResource(meetingId, request, bindingResult, model,
                redirectAttributes, userDetails);

        // Then
        assertEquals("meetings/resources/add-resource", viewName);
        verify(meetingResourceService, never()).addResource(anyLong(), any(), anyLong());
        verify(model).addAttribute("meeting", mockMeeting);
    }

    @Test
    void addResource_shouldCallService() {
        // Given
        Long meetingId = 100L;
        MeetingResourceRequest request = MeetingResourceRequest.builder().build();
        when(meetingService.getMeeting(meetingId)).thenReturn(mockMeeting);
        when(bindingResult.hasErrors()).thenReturn(false);

        // When
        String viewName = controller.addResource(meetingId, request, bindingResult, model,
                redirectAttributes, userDetails);

        // Then
        assertEquals("redirect:/meetings/" + meetingId + "/resources", viewName);
        verify(meetingResourceService).addResource(meetingId, request, 1L);
        verify(redirectAttributes).addFlashAttribute("success", "Zasób został dodany pomyślnie");
    }


    @ParameterizedTest
    @ValueSource(longs = {0, -1, -100})
    void addResource_shouldHandleInvalidMeetingId(long invalidMeetingId) {
        // Given
        MeetingResourceRequest request = MeetingResourceRequest.builder()
                .build();

        // When
        String viewName = controller.addResource(invalidMeetingId, request, bindingResult, model,
                redirectAttributes, userDetails);

        // Then - Bez Spring AOP walidacji, metoda próbuje pobrać spotkanie
        verify(meetingService).getMeeting(invalidMeetingId);
    }

    // ==================== TESTS FOR GET MEETING RESOURCES ====================

    @Test
    void getMeetingResources_shouldReturnResourcesList() {
        // Given
        Long meetingId = 100L;
        List<MeetingResourceResponse> resources = Arrays.asList(
                mockResource,
                MeetingResourceResponse.builder()
                        .id(201L)
                        .build()
        );

        when(meetingService.getMeeting(meetingId)).thenReturn(mockMeeting);
        when(meetingResourceService.getMeetingResources(meetingId, 1L)).thenReturn(resources);

        // When
        String viewName = controller.getMeetingResources(meetingId, model, userDetails);

        // Then
        assertEquals("meetings/resources/resources-list", viewName);
        verify(model).addAttribute("meeting", mockMeeting);
        verify(model).addAttribute("resources", resources);
        verify(model).addAttribute("resourcesCount", 2);
    }

    @Test
    void getMeetingResources_shouldHandleEmptyResources() {
        // Given
        Long meetingId = 100L;
        when(meetingService.getMeeting(meetingId)).thenReturn(mockMeeting);
        when(meetingResourceService.getMeetingResources(meetingId, 1L)).thenReturn(Collections.emptyList());

        // When
        String viewName = controller.getMeetingResources(meetingId, model, userDetails);

        // Then
        assertEquals("meetings/resources/resources-list", viewName);
        verify(model).addAttribute("resourcesCount", 0);
    }

    @Test
    void getMeetingResources_shouldHandleNullResources() {
        // Given
        Long meetingId = 100L;
        when(meetingService.getMeeting(meetingId)).thenReturn(mockMeeting);
        when(meetingResourceService.getMeetingResources(meetingId, 1L)).thenReturn(null);

        // When
        String viewName = controller.getMeetingResources(meetingId, model, userDetails);

        // Then
        assertEquals("meetings/resources/resources-list", viewName);
        verify(model).addAttribute("resourcesCount", 0);
    }

    @Test
    void getMeetingResources_shouldHandleInvalidMeetingId() {
        // Given
        Long invalidMeetingId = -1L;

        // When
        String viewName = controller.getMeetingResources(invalidMeetingId, model, userDetails);

        // Then - Bez Spring AOP walidacji
        verify(meetingService).getMeeting(invalidMeetingId);
    }

    @Test
    void getMeetingResources_shouldCallService() {
        // Given
        Long meetingId = 100L;
        when(meetingService.getMeeting(meetingId)).thenReturn(mockMeeting);

        // When
        String viewName = controller.getMeetingResources(meetingId, model, userDetails);

        // Then
        assertEquals("meetings/resources/resources-list", viewName);
        verify(meetingService).getMeeting(meetingId);
        verify(model).addAttribute(eq("meeting"), any());
    }


    // ==================== TESTS FOR DELETE RESOURCE ====================

    @Test
    void deleteResource_shouldDeleteSuccessfully() {
        // Given
        Long meetingId = 100L;
        Long resourceId = 200L;

        // When
        String viewName = controller.deleteResource(meetingId, resourceId, redirectAttributes, userDetails);

        // Then
        assertEquals("redirect:/meetings/100/resources", viewName);
        verify(meetingResourceService).deleteResource(resourceId, 1L);
        verify(redirectAttributes).addFlashAttribute("success", "Zasób został usunięty pomyślnie");
    }

    @Test
    void deleteResource_shouldCallService() {
        // Given
        Long meetingId = 100L;
        Long resourceId = 10L;

        // When
        String viewName = controller.deleteResource(meetingId, resourceId, redirectAttributes, userDetails);

        // Then
        assertEquals("redirect:/meetings/100/resources", viewName);
        verify(meetingResourceService).deleteResource(resourceId, 1L);
        verify(redirectAttributes).addFlashAttribute("success", "Zasób został usunięty pomyślnie");
    }



    @ParameterizedTest
    @ValueSource(longs = {0, -1, -100})
    void deleteResource_shouldHandleInvalidIds(long invalidId) {
        // When
        String viewName = controller.deleteResource(invalidId, invalidId, redirectAttributes, userDetails);

        // Then - Bez Spring AOP walidacji, metoda próbuje usunąć zasób
        verify(meetingResourceService).deleteResource(invalidId, 1L);
    }


    // ==================== TESTS FOR SHOW RESOURCE DETAILS ====================

    @Test
    void showResourceDetails_shouldReturnDetailsPage() {
        // Given
        Long meetingId = 100L;
        Long resourceId = 200L;
        when(meetingService.getMeeting(meetingId)).thenReturn(mockMeeting);
        when(meetingResourceService.getResource(resourceId, 1L)).thenReturn(mockResource);

        // When
        String viewName = controller.showResourceDetails(meetingId, resourceId, model, userDetails);

        // Then
        assertEquals("meetings/resources/resource-details", viewName);
        verify(model).addAttribute("meeting", mockMeeting);
        verify(model).addAttribute("resource", mockResource);
    }

    @Test
    void showResourceDetails_shouldHandleInvalidResourceId() {
        // Given
        Long invalidResourceId = -1L;
        when(meetingService.getMeeting(100L)).thenReturn(mockMeeting);

        // When
        String viewName = controller.showResourceDetails(100L, invalidResourceId, model, userDetails);

        // Then - Bez Spring AOP walidacji
        verify(meetingResourceService).getResource(invalidResourceId, 1L);
    }

    @Test
    void showResourceDetails_shouldCallService() {
        // Given
        Long meetingId = 100L;
        Long resourceId = 200L;
        when(meetingService.getMeeting(meetingId)).thenReturn(mockMeeting);
        when(meetingResourceService.getResource(resourceId, 1L))
                .thenReturn(mockResource);

        // When
        String viewName = controller.showResourceDetails(meetingId, resourceId, model, userDetails);

        // Then
        assertEquals("meetings/resources/resource-details", viewName);
        verify(meetingService).getMeeting(meetingId);
        verify(meetingResourceService).getResource(resourceId, 1L);
        verify(model).addAttribute("resource", mockResource);
    }




    // ==================== TESTS FOR EDGE CASES ====================

    @Test
    void getMeetingResources_shouldHandleServiceReturningNullMeeting() {
        // Given
        Long meetingId = 100L;

        // When
        String viewName = controller.getMeetingResources(meetingId, model, userDetails);

        // Then
        assertEquals("meetings/resources/resources-list", viewName);
        verify(model).addAttribute("meeting", null);
    }
}
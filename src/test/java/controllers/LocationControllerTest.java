package com.meethub.controller.web;

import com.meethub.domain.model.enums.LocationType;
import com.meethub.domain.model.request.CreateLocationRequest;
import com.meethub.domain.model.request.LocationSearchRequest;
import com.meethub.domain.model.request.UpdateLocationRequest;
import com.meethub.domain.model.response.LocationListResponse;
import com.meethub.domain.model.response.LocationResponse;
import com.meethub.domain.service.LocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationControllerTest {

    @Mock
    private LocationService locationService;

    @Mock
    private Model model;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private LocationController controller;

    private LocationResponse testLocation;

    @BeforeEach
    void setUp() {
        testLocation = new LocationResponse();
        testLocation.setId(1L);
        testLocation.setName("Test Location");
        testLocation.setType(LocationType.PHYSICAL);
        testLocation.setAddress("Test Address");
        testLocation.setCity("Test City");
    }

//    @Test
//    void testListLocations_Success() {
//        LocationListResponse locationListResponse = mock(LocationListResponse.class);
//        when(locationListResponse.getLocations()).thenReturn(List.of(testLocation));
//        when(locationListResponse.getCurrentPage()).thenReturn(0);
//        when(locationListResponse.getTotalPages()).thenReturn(1);
//        when(locationListResponse.getTotalItems()).thenReturn(1L);
//        when(locationListResponse.isHasNext()).thenReturn(false);
//        when(locationListResponse.isHasPrevious()).thenReturn(false);
//
//        when(locationService.searchLocations(any(LocationSearchRequest.class)))
//                .thenReturn(locationListResponse);
//
//        String viewName = controller .listLocations("test", "PHYSICAL", "City", 0, 20, model);
//
//        assertEquals("locations/list", viewName);
//    }
//
//    @Test
//    void testListLocations_InvalidType() {
//        LocationListResponse locationListResponse = mock(LocationListResponse.class);
//        when(locationListResponse.getLocations()).thenReturn(List.of(testLocation));
//        when(locationListResponse.getCurrentPage()).thenReturn(0);
//        when(locationListResponse.getTotalPages()).thenReturn(1);
//        when(locationListResponse.getTotalItems()).thenReturn(1L);
//        when(locationListResponse.isHasNext()).thenReturn(false);
//        when(locationListResponse.isHasPrevious()).thenReturn(false);
//
//        when(locationService.searchLocations(any(LocationSearchRequest.class)))
//                .thenReturn(locationListResponse);
//
//        String viewName = controller.listLocations("test", "INVALID_TYPE", "City", 0, 20, model);
//
//        assertEquals("locations/list", viewName);
//    }

    @Test
    void testListLocations_NullParameters() {
        LocationListResponse locationListResponse = mock(LocationListResponse.class);
        when(locationListResponse.getLocations()).thenReturn(List.of(testLocation));
        when(locationListResponse.getCurrentPage()).thenReturn(0);
        when(locationListResponse.getTotalPages()).thenReturn(1);
        when(locationListResponse.getTotalItems()).thenReturn(1L);
        when(locationListResponse.isHasNext()).thenReturn(false);
        when(locationListResponse.isHasPrevious()).thenReturn(false);

        when(locationService.searchLocations(any(LocationSearchRequest.class)))
                .thenReturn(locationListResponse);

        String viewName = controller.listLocations(null, null, null, 0, 20, model);

        assertEquals("locations/list", viewName);
    }

    @Test
    void testListLocations_EdgePageSize() {
        LocationListResponse locationListResponse = mock(LocationListResponse.class);
        when(locationListResponse.getLocations()).thenReturn(List.of(testLocation));
        when(locationListResponse.getCurrentPage()).thenReturn(0);
        when(locationListResponse.getTotalPages()).thenReturn(1);
        when(locationListResponse.getTotalItems()).thenReturn(1L);
        when(locationListResponse.isHasNext()).thenReturn(false);
        when(locationListResponse.isHasPrevious()).thenReturn(false);

        when(locationService.searchLocations(any(LocationSearchRequest.class)))
                .thenReturn(locationListResponse);

        String viewName = controller.listLocations("test", "PHYSICAL", "City", 0, 100, model);

        assertEquals("locations/list", viewName);
    }

    @Test
    void testListLocations_EmptyResponse() {
        LocationListResponse locationListResponse = mock(LocationListResponse.class);
        when(locationListResponse.getLocations()).thenReturn(Collections.emptyList());
        when(locationListResponse.getCurrentPage()).thenReturn(0);
        when(locationListResponse.getTotalPages()).thenReturn(0);
        when(locationListResponse.getTotalItems()).thenReturn(0L);
        when(locationListResponse.isHasNext()).thenReturn(false);
        when(locationListResponse.isHasPrevious()).thenReturn(false);

        when(locationService.searchLocations(any(LocationSearchRequest.class)))
                .thenReturn(locationListResponse);

        String viewName = controller.listLocations("nonexistent", "PHYSICAL", "Nowhere", 0, 20, model);

        assertEquals("locations/list", viewName);
    }

    @Test
    void testShowCreateForm() {
        String viewName = controller.showCreateForm(model);

        assertEquals("locations/create", viewName);
    }

    @Test
    void testCreateLocation_Success() {
        CreateLocationRequest request = new CreateLocationRequest();
        request.setName("Test Location");
        request.setType(LocationType.PHYSICAL);
        request.setAddress("Test Address");
        request.setCity("Test City");

        when(bindingResult.hasErrors()).thenReturn(false);
        when(locationService.createLocation(request)).thenReturn(testLocation);

        String redirect = controller.createLocation(request, bindingResult, redirectAttributes, model);

        assertEquals("redirect:/locations", redirect);
    }

    @Test
    void testCreateLocation_ValidationErrors() {
        CreateLocationRequest request = new CreateLocationRequest();
        when(bindingResult.hasErrors()).thenReturn(true);

        String viewName = controller.createLocation(request, bindingResult, redirectAttributes, model);

        assertEquals("locations/create", viewName);
    }

    @Test
    void testLocationDetails_Success() {
        when(locationService.getLocation(1L)).thenReturn(testLocation);
        when(locationService.generateMapUrl(1L)).thenReturn("http://map.url");

        String viewName = controller.locationDetails(1L, model);

        assertEquals("locations/details", viewName);
    }

    @Test
    void testShowEditForm_Success() {
        when(locationService.getLocation(1L)).thenReturn(testLocation);

        String viewName = controller.showEditForm(1L, model);

        assertEquals("locations/edit", viewName);
    }

    @Test
    void testUpdateLocation_Success() {
        UpdateLocationRequest request = new UpdateLocationRequest();
        when(bindingResult.hasErrors()).thenReturn(false);
        when(locationService.updateLocation(1L, request)).thenReturn(testLocation);

        String redirect = controller.updateLocation(1L, request, bindingResult, redirectAttributes, model);

        assertEquals("redirect:/locations/1", redirect);
    }

    @Test
    void testUpdateLocation_ValidationErrors() {
        UpdateLocationRequest request = new UpdateLocationRequest();
        when(bindingResult.hasErrors()).thenReturn(true);

        String viewName = controller.updateLocation(1L, request, bindingResult, redirectAttributes, model);

        assertEquals("locations/edit", viewName);
    }

    @Test
    void testDeleteLocation_Success() {
        String redirect = controller.deleteLocation(1L, redirectAttributes);
        assertEquals("redirect:/locations", redirect);
    }

    @Test
    void testFindNearbyLocations_Success() {
        List<LocationResponse> locations = List.of(testLocation);
        when(locationService.findNearbyLocations(any(), any(), any()))
                .thenReturn(locations);

        String viewName = controller.findNearbyLocations(
                new BigDecimal("52.2297"),
                new BigDecimal("21.0122"),
                5.0,
                model
        );

        assertEquals("locations/nearby", viewName);
    }

    @Test
    void testFindNearbyLocations_DefaultCoordinates() {
        List<LocationResponse> locations = Collections.emptyList();
        when(locationService.findNearbyLocations(any(), any(), any()))
                .thenReturn(locations);

        String viewName = controller.findNearbyLocations(null, null, 5.0, model);

        assertEquals("locations/nearby", viewName);
    }

    @Test
    void testShowOnMap_Success() {
        when(locationService.getLocation(1L)).thenReturn(testLocation);
        when(locationService.generateMapUrl(1L)).thenReturn("http://map.url");
        when(locationService.generateDirectionsUrl(eq(1L), anyString()))
                .thenReturn("http://directions.url");

        String viewName = controller.showOnMap(1L, model);

        assertEquals("locations/map", viewName);
    }

    @Test
    void testGenerateVirtualLocation_Success() {
        when(locationService.generateVirtualLocation(anyString(), anyString(), anyString()))
                .thenReturn(testLocation);

        String redirect = controller.generateVirtualLocation("Zoom", "12345", "pass123", redirectAttributes);

        assertEquals("redirect:/locations/1", redirect);
    }
}
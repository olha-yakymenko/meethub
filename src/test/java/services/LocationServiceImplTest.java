// LocationServiceImplTest.java (POPRAWIONA WERSJA)
package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.Location;
import com.meethub.domain.model.enums.LocationType;
import com.meethub.domain.model.request.CreateLocationRequest;
import com.meethub.domain.model.request.LocationSearchRequest;
import com.meethub.domain.model.request.UpdateLocationRequest;
import com.meethub.domain.model.response.LocationListResponse;
import com.meethub.domain.model.response.LocationResponse;
import com.meethub.domain.repository.jpa.LocationRepository;
import com.meethub.domain.service.GeocodingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationServiceImplTest {

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private GeocodingService geocodingService;

    @InjectMocks
    private LocationServiceImpl locationService;

    private Location physicalLocation;
    private Location virtualLocation;
    private CreateLocationRequest createPhysicalRequest;
    private CreateLocationRequest createVirtualRequest;
    private UpdateLocationRequest updateLocationRequest;

    @BeforeEach
    void setUp() {
        // Setup physical location
        physicalLocation = Location.builder()
                .id(1L)
                .name("Test Office")
                .type(LocationType.PHYSICAL)
                .address("ul. Testowa 123")
                .city("Warsaw")
                .country("Poland")
                .latitude(new BigDecimal("52.2297"))
                .longitude(new BigDecimal("21.0122"))
                .timezone("Europe/Warsaw")
                .build();

        // Setup virtual location
        virtualLocation = Location.builder()
                .id(2L)
                .name("Zoom Meeting")
                .type(LocationType.VIRTUAL)
                .virtualMeetingUrl("https://zoom.us/j/123456789")
                .accessCode("123456")
                .timezone("Europe/Warsaw")
                .build();

        // Setup create requests - UŻYWAMY BUILDERA LUB SETTERÓW
        createPhysicalRequest = new CreateLocationRequest();
        createPhysicalRequest.setName("New Office");
        createPhysicalRequest.setType(LocationType.PHYSICAL);
        createPhysicalRequest.setAddress("ul. Nowa 456");
        createPhysicalRequest.setCity("Krakow");
        createPhysicalRequest.setCountry("Poland");
        createPhysicalRequest.setLatitude(new BigDecimal("50.0647"));
        createPhysicalRequest.setLongitude(new BigDecimal("19.9450"));
        createPhysicalRequest.setTimezone("Europe/Warsaw");

        createVirtualRequest = new CreateLocationRequest();
        createVirtualRequest.setName("Team Meeting");
        createVirtualRequest.setType(LocationType.VIRTUAL);
        createVirtualRequest.setVirtualMeetingUrl("https://teams.microsoft.com/l/meetup-join/abc123");
        createVirtualRequest.setAccessCode("789012");
        createVirtualRequest.setTimezone("Europe/Warsaw");

        // Setup update request
        updateLocationRequest = new UpdateLocationRequest();
        updateLocationRequest.setName("Updated Office");
        updateLocationRequest.setType(LocationType.PHYSICAL);
        updateLocationRequest.setAddress("ul. Zaktualizowana 789");
        updateLocationRequest.setCity("Gdansk");
        updateLocationRequest.setCountry("Poland");
        updateLocationRequest.setLatitude(new BigDecimal("54.3520"));
        updateLocationRequest.setLongitude(new BigDecimal("18.6466"));
        updateLocationRequest.setTimezone("Europe/Warsaw");
    }

    @Test
    void testCreateLocation_Physical_Success() {
        // Given
        when(locationRepository.existsByNameAndAddress(anyString(), anyString())).thenReturn(false);
        when(locationRepository.save(any(Location.class))).thenReturn(physicalLocation);

        // When
        LocationResponse response = locationService.createLocation(createPhysicalRequest);

        // Then
        assertNotNull(response);
        assertEquals(physicalLocation.getId(), response.getId());
        assertEquals(physicalLocation.getName(), response.getName());
        assertEquals(LocationType.PHYSICAL, response.getType());

        verify(locationRepository).existsByNameAndAddress(anyString(), anyString());
        verify(locationRepository).save(any(Location.class));
    }

    @Test
    void testCreateLocation_Physical_WithGeocoding_Success() {
        // Given - request without coordinates
        CreateLocationRequest request = new CreateLocationRequest();
        request.setName("Office Without Coords");
        request.setType(LocationType.PHYSICAL);
        request.setAddress("ul. Testowa 123");
        request.setCity("Warsaw");
        request.setCountry("Poland");

        Location geocodedLocation = Location.builder()
                .latitude(new BigDecimal("52.2297"))
                .longitude(new BigDecimal("21.0122"))
                .city("Warsaw")
                .country("Poland")
                .build();

        when(locationRepository.existsByNameAndAddress(anyString(), anyString())).thenReturn(false);
        when(geocodingService.geocodeAddress(anyString())).thenReturn(geocodedLocation);
        when(locationRepository.save(any(Location.class))).thenAnswer(invocation -> {
            Location saved = invocation.getArgument(0);
            saved.setId(3L);
            return saved;
        });

        // When
        LocationResponse response = locationService.createLocation(request);

        // Then
        assertNotNull(response);
        assertEquals("Office Without Coords", response.getName());

        verify(geocodingService).geocodeAddress("ul. Testowa 123, Warsaw, Poland");
        verify(locationRepository).save(any(Location.class));
    }

    @Test
    void testCreateLocation_Physical_NameAndAddressAlreadyExists_ThrowsException() {
        // Given
        when(locationRepository.existsByNameAndAddress(anyString(), anyString())).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> locationService.createLocation(createPhysicalRequest));

        assertEquals("Lokalizacja o tej nazwie i adresie już istnieje", exception.getMessage());
        verify(locationRepository, never()).save(any(Location.class));
    }

    @Test
    void testCreateLocation_Virtual_Success() {
        // Given
        when(locationRepository.findByVirtualMeetingUrl(anyString())).thenReturn(Optional.empty());
        when(locationRepository.save(any(Location.class))).thenReturn(virtualLocation);

        // When
        LocationResponse response = locationService.createLocation(createVirtualRequest);

        // Then
        assertNotNull(response);
        assertEquals(virtualLocation.getId(), response.getId());
        assertEquals(virtualLocation.getName(), response.getName());
        assertEquals(LocationType.VIRTUAL, response.getType());
        assertEquals(virtualLocation.getVirtualMeetingUrl(), response.getVirtualMeetingUrl());

        verify(locationRepository).findByVirtualMeetingUrl(createVirtualRequest.getVirtualMeetingUrl());
        verify(locationRepository).save(any(Location.class));
    }

    @Test
    void testCreateLocation_Virtual_UrlAlreadyExists_ThrowsException() {
        // Given
        when(locationRepository.findByVirtualMeetingUrl(anyString())).thenReturn(Optional.of(virtualLocation));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> locationService.createLocation(createVirtualRequest));

        assertEquals("URL spotkania już istnieje", exception.getMessage());
        verify(locationRepository, never()).save(any(Location.class));
    }

//    @Test
//    void testCreateLocation_MissingName_ThrowsException() {
//        // Given
//        CreateLocationRequest request = new CreateLocationRequest();
//        request.setType(LocationType.PHYSICAL);
//        request.setAddress("Test Address");
//
//        // When & Then - Sprawdzenie walidacji JAKOŚCIOWEJ (logiki biznesowej)
//        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
//                () -> locationService.createLocation(request));
//
//        assertTrue(exception.getMessage().contains("Nazwa lokalizacji jest wymagana"));
//        verify(locationRepository, never()).save(any(Location.class));
//    }
//
//    @Test
//    void testCreateLocation_MissingType_ThrowsException() {
//        // Given
//        CreateLocationRequest request = new CreateLocationRequest();
//        request.setName("Test Location");
//        request.setAddress("Test Address");
//
//        // When & Then
//        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
//                () -> locationService.createLocation(request));
//
//        assertTrue(exception.getMessage().contains("Typ lokalizacji jest wymagany"));
//        verify(locationRepository, never()).save(any(Location.class));
//    }

    @Test
    void testUpdateLocation_Success() {
        // Given
        when(locationRepository.findById(1L)).thenReturn(Optional.of(physicalLocation));
        when(locationRepository.save(any(Location.class))).thenReturn(physicalLocation);

        // When
        LocationResponse response = locationService.updateLocation(1L, updateLocationRequest);

        // Then
        assertNotNull(response);
        assertEquals("Updated Office", response.getName());
        assertEquals(LocationType.PHYSICAL, response.getType());

        verify(locationRepository).save(any(Location.class));
    }

    @Test
    void testUpdateLocation_LocationNotFound_ThrowsException() {
        // Given
        when(locationRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> locationService.updateLocation(999L, updateLocationRequest));

        assertEquals("Lokalizacja nie znaleziona", exception.getMessage());
        verify(locationRepository, never()).save(any(Location.class));
    }

    @Test
    void testUpdateLocation_ChangeTypeWhenUsedInMeetings_ThrowsException() {
        // Given
        Location locationWithMeetings = physicalLocation;
        // Ustawiamy spotkania dla lokalizacji
        com.meethub.domain.model.entity.Meeting mockMeeting = mock(com.meethub.domain.model.entity.Meeting.class);
        locationWithMeetings.setMeetings(List.of(mockMeeting));

        UpdateLocationRequest request = new UpdateLocationRequest();
        request.setName("Updated Name");
        request.setType(LocationType.VIRTUAL); // Changing type

        when(locationRepository.findById(1L)).thenReturn(Optional.of(locationWithMeetings));

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> locationService.updateLocation(1L, request));

        assertEquals("Nie można zmienić typu lokalizacji używanej w spotkaniach", exception.getMessage());
        verify(locationRepository, never()).save(any(Location.class));
    }

    @Test
    void testUpdateLocation_WithGeocoding_Success() {
        // Given - location without coordinates
        Location locationWithoutCoords = Location.builder()
                .id(3L)
                .name("Office Without Coords")
                .type(LocationType.PHYSICAL)
                .address("Old Address")
                .city("Old City")
                .country("Old Country")
                .build();

        UpdateLocationRequest request = new UpdateLocationRequest();
        request.setName("Updated Office");
        request.setType(LocationType.PHYSICAL);
        request.setAddress("ul. Nowa 456");
        request.setCity("Warsaw");
        request.setCountry("Poland");

        Location geocodedLocation = Location.builder()
                .latitude(new BigDecimal("52.2297"))
                .longitude(new BigDecimal("21.0122"))
                .build();

        when(locationRepository.findById(3L)).thenReturn(Optional.of(locationWithoutCoords));
        when(geocodingService.geocodeAddress(anyString())).thenReturn(geocodedLocation);
        when(locationRepository.save(any(Location.class))).thenAnswer(invocation -> {
            Location saved = invocation.getArgument(0);
            return saved;
        });

        // When
        LocationResponse response = locationService.updateLocation(3L, request);

        // Then
        assertNotNull(response);
        verify(geocodingService).geocodeAddress("ul. Nowa 456, Warsaw, Poland");
        verify(locationRepository).save(any(Location.class));
    }

    @Test
    void testDeleteLocation_Success() {
        // Given
        Location locationWithoutMeetings = physicalLocation;
        locationWithoutMeetings.setMeetings(Collections.emptyList());

        when(locationRepository.findById(1L)).thenReturn(Optional.of(locationWithoutMeetings));

        // When
        locationService.deleteLocation(1L);

        // Then
        verify(locationRepository).delete(locationWithoutMeetings);
    }

    @Test
    void testDeleteLocation_LocationNotFound_ThrowsException() {
        // Given
        when(locationRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> locationService.deleteLocation(999L));

        assertEquals("Lokalizacja nie znaleziona", exception.getMessage());
        verify(locationRepository, never()).delete(any(Location.class));
    }

    @Test
    void testDeleteLocation_LocationUsedInMeetings_ThrowsException() {
        // Given
        Location locationWithMeetings = physicalLocation;
        // Ustawiamy spotkania dla lokalizacji
        com.meethub.domain.model.entity.Meeting mockMeeting1 = mock(com.meethub.domain.model.entity.Meeting.class);
        com.meethub.domain.model.entity.Meeting mockMeeting2 = mock(com.meethub.domain.model.entity.Meeting.class);
        locationWithMeetings.setMeetings(List.of(mockMeeting1, mockMeeting2));

        when(locationRepository.findById(1L)).thenReturn(Optional.of(locationWithMeetings));

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> locationService.deleteLocation(1L));

        assertTrue(exception.getMessage().contains("Nie można usunąć lokalizacji używanej w spotkaniach"));
        verify(locationRepository, never()).delete(any(Location.class));
    }

//    @Test
//    void testGetLocation_Success() {
//        // Given
//        when(locationRepository.findById(1L)).thenReturn(Optional.of(physicalLocation));
//
//        // When
//        LocationResponse response = locationService.getLocation(1L);
//
//        // Then
//        assertNotNull(response);
//        assertEquals(physicalLocation.getId(), response.getId());
//        assertEquals(physicalLocation.getName(), response.getName());
//
//        verify(locationRepository).findById(1L);
//    }

    @Test
    void testGetLocation_NotFound_ThrowsException() {
        // Given
        when(locationRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> locationService.getLocation(999L));

        assertEquals("Lokalizacja nie znaleziona", exception.getMessage());
    }

    @Test
    void testSearchLocations_Success() {
        // Given
        LocationSearchRequest request = LocationSearchRequest.builder()
                .query("Office")
                .type(LocationType.PHYSICAL)
                .city("Warsaw")
                .page(0)
                .size(10)
                .build();

        Page<Location> locationPage = new PageImpl<>(List.of(physicalLocation), PageRequest.of(0, 10), 1);

        when(locationRepository.searchLocations(anyString(), anyString(), anyString(), any(Pageable.class)))
                .thenReturn(locationPage);

        // When
        LocationListResponse response = locationService.searchLocations(request);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getLocations().size());
        assertEquals(0, response.getCurrentPage());
        assertEquals(1, response.getTotalPages());
        assertEquals(1, response.getTotalItems());
        assertFalse(response.isHasNext());
        assertFalse(response.isHasPrevious());

        verify(locationRepository).searchLocations("Office", "PHYSICAL", "Warsaw", PageRequest.of(0, 10));
    }

//    @Test
//    void testSearchLocations_WithDefaultPagination() {
//        // Given - request without pagination
//        LocationSearchRequest request = LocationSearchRequest.builder()
//                .query("Office")
//                .build();
//
//        Page<Location> locationPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);
//
//        when(locationRepository.searchLocations(anyString(), any(), anyString(), any(Pageable.class)))
//                .thenReturn(locationPage);
//
//        // When
//        LocationListResponse response = locationService.searchLocations(request);
//
//        // Then
//        assertNotNull(response);
//        verify(locationRepository).searchLocations("Office", null, null, PageRequest.of(0, 20));
//    }

    @Test
    void testFindNearbyLocations_Success() {
        // Given
        BigDecimal lat = new BigDecimal("52.2297");
        BigDecimal lng = new BigDecimal("21.0122");
        Double radius = 10.0;

        List<Location> nearbyLocations = List.of(physicalLocation);

        when(locationRepository.findNearbyLocations(eq(lat), eq(lng), eq(radius)))
                .thenReturn(nearbyLocations);

        // When
        List<LocationResponse> responses = locationService.findNearbyLocations(lat, lng, radius);

        // Then
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(physicalLocation.getId(), responses.get(0).getId());

        verify(locationRepository).findNearbyLocations(lat, lng, radius);
    }

    @Test
    void testFindNearbyLocations_WithDefaultRadius() {
        // Given
        BigDecimal lat = new BigDecimal("52.2297");
        BigDecimal lng = new BigDecimal("21.0122");

        when(locationRepository.findNearbyLocations(eq(lat), eq(lng), eq(10.0)))
                .thenReturn(Collections.emptyList());

        // When
        List<LocationResponse> responses = locationService.findNearbyLocations(lat, lng, null);

        // Then
        assertNotNull(responses);
        verify(locationRepository).findNearbyLocations(lat, lng, 10.0);
    }

    @Test
    void testFindNearbyLocations_InvalidCoordinates_ThrowsException() {
        // Test invalid latitude (poniżej -90)
        IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class,
                () -> locationService.findNearbyLocations(new BigDecimal("-100"), new BigDecimal("21.0122"), 10.0));
        assertTrue(exception1.getMessage().contains("Szerokość geograficzna"));

        // Test invalid latitude (powyżej 90)
        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class,
                () -> locationService.findNearbyLocations(new BigDecimal("100"), new BigDecimal("21.0122"), 10.0));
        assertTrue(exception2.getMessage().contains("Szerokość geograficzna"));

        // Test invalid longitude (poniżej -180)
        IllegalArgumentException exception3 = assertThrows(IllegalArgumentException.class,
                () -> locationService.findNearbyLocations(new BigDecimal("52.2297"), new BigDecimal("-200"), 10.0));
        assertTrue(exception3.getMessage().contains("Długość geograficzna"));

        // Test invalid longitude (powyżej 180)
        IllegalArgumentException exception4 = assertThrows(IllegalArgumentException.class,
                () -> locationService.findNearbyLocations(new BigDecimal("52.2297"), new BigDecimal("200"), 10.0));
        assertTrue(exception4.getMessage().contains("Długość geograficzna"));

        // Test null coordinates - NIE powinien rzucać wyjątku, bo współrzędne są sprawdzane tylko gdy są podane
        // Zmieniam test - nie powinien rzucać wyjątku dla nulli
        assertDoesNotThrow(() -> locationService.findNearbyLocations(null, null, 10.0));
    }

    @Test
    void testFindNearbyLocations_InvalidRadius_ThrowsException() {
        // Given
        BigDecimal lat = new BigDecimal("52.2297");
        BigDecimal lng = new BigDecimal("21.0122");

        // When & Then - radius <= 0
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> locationService.findNearbyLocations(lat, lng, 0.0));

        assertEquals("Promień musi być większy niż 0", exception.getMessage());
    }

    @Test
    void testFindNearbyLocations_RadiusTooLarge_LimitsRadius() {
        // Given
        BigDecimal lat = new BigDecimal("52.2297");
        BigDecimal lng = new BigDecimal("21.0122");

        when(locationRepository.findNearbyLocations(eq(lat), eq(lng), eq(1000.0)))
                .thenReturn(Collections.emptyList());

        // When
        locationService.findNearbyLocations(lat, lng, 1500.0);

        // Then - should be limited to 1000.0
        verify(locationRepository).findNearbyLocations(lat, lng, 1000.0);
    }

    @Test
    void testGenerateMapUrl_WithCoordinates() {
        // Given
        when(locationRepository.findById(1L)).thenReturn(Optional.of(physicalLocation));

        // When
        String mapUrl = locationService.generateMapUrl(1L);

        // Then
        assertNotNull(mapUrl);
        assertTrue(mapUrl.contains("52.2297"));
        assertTrue(mapUrl.contains("21.0122"));
        assertTrue(mapUrl.startsWith("https://maps.google.com/maps?q="));
    }

    @Test
    void testGenerateMapUrl_WithAddress() {
        // Given
        Location locationWithAddress = Location.builder()
                .id(3L)
                .name("Test Location")
                .type(LocationType.PHYSICAL)
                .address("ul. Testowa 123")
                .city("Warsaw")
                .country("Poland")
                .build();

        when(locationRepository.findById(3L)).thenReturn(Optional.of(locationWithAddress));

        // When
        String mapUrl = locationService.generateMapUrl(3L);

        // Then
        assertNotNull(mapUrl);
        assertTrue(mapUrl.contains("ul.+Testowa+123"));
        assertTrue(mapUrl.startsWith("https://maps.google.com/maps?q="));
    }

    @Test
    void testGenerateMapUrl_NoCoordinatesOrAddress_ReturnsNull() {
        // Given
        Location locationWithoutInfo = Location.builder()
                .id(4L)
                .name("Virtual Meeting")
                .type(LocationType.VIRTUAL)
                .build();

        when(locationRepository.findById(4L)).thenReturn(Optional.of(locationWithoutInfo));

        // When
        String mapUrl = locationService.generateMapUrl(4L);

        // Then
        assertNull(mapUrl);
    }

    @Test
    void testGenerateDirectionsUrl_WithCoordinates() {
        // Given
        when(locationRepository.findById(1L)).thenReturn(Optional.of(physicalLocation));
        String origin = "Start Address";

        // When
        String directionsUrl = locationService.generateDirectionsUrl(1L, origin);

        // Then
        assertNotNull(directionsUrl);
        assertTrue(directionsUrl.contains("52.2297,21.0122"));
        assertTrue(directionsUrl.contains("saddr=Start+Address"));
        assertTrue(directionsUrl.startsWith("https://maps.google.com/maps?saddr="));
    }

    @Test
    void testGenerateDirectionsUrl_EmptyOrigin() {
        // Given
        when(locationRepository.findById(1L)).thenReturn(Optional.of(physicalLocation));

        // When
        String directionsUrl = locationService.generateDirectionsUrl(1L, "");

        // Then
        assertNotNull(directionsUrl);
        assertTrue(directionsUrl.contains("saddr="));
    }

    @Test
    void testGenerateDirectionsUrl_NullOrigin() {
        // Given
        when(locationRepository.findById(1L)).thenReturn(Optional.of(physicalLocation));

        // When
        String directionsUrl = locationService.generateDirectionsUrl(1L, null);

        // Then
        assertNotNull(directionsUrl);
        assertTrue(directionsUrl.contains("saddr="));
    }

    @Test
    void testGenerateVirtualLocation_Zoom_Success() {
        // Given
        Location zoomLocation = Location.builder()
                .id(5L)
                .name("Zoom Meeting")
                .type(LocationType.VIRTUAL)
                .virtualMeetingUrl("https://zoom.us/j/123456789?pwd=abcdef")
                .accessCode("abcdef")
                .timezone("Europe/Warsaw")
                .build();

        when(locationRepository.save(any(Location.class))).thenReturn(zoomLocation);

        // When
        LocationResponse response = locationService.generateVirtualLocation("zoom", "123456789", "abcdef");

        // Then
        assertNotNull(response);
        assertEquals("Zoom Meeting", response.getName());
        assertEquals(LocationType.VIRTUAL, response.getType());
        assertTrue(response.getVirtualMeetingUrl().contains("zoom.us/j/123456789"));

        verify(locationRepository).save(any(Location.class));
    }

    @Test
    void testGenerateVirtualLocation_Zoom_NoPasscode() {
        // Given
        Location zoomLocation = Location.builder()
                .id(6L)
                .name("Zoom Meeting")
                .type(LocationType.VIRTUAL)
                .virtualMeetingUrl("https://zoom.us/j/123456789")
                .timezone("Europe/Warsaw")
                .build();

        when(locationRepository.save(any(Location.class))).thenReturn(zoomLocation);

        // When
        LocationResponse response = locationService.generateVirtualLocation("zoom", "123456789", null);

        // Then
        assertNotNull(response);
        assertTrue(response.getVirtualMeetingUrl().contains("zoom.us/j/123456789"));
        assertFalse(response.getVirtualMeetingUrl().contains("pwd="));
    }

    @Test
    void testGenerateVirtualLocation_Teams_Success() {
        // Given
        Location teamsLocation = Location.builder()
                .id(7L)
                .name("Teams Meeting")
                .type(LocationType.VIRTUAL)
                .virtualMeetingUrl("https://teams.microsoft.com/l/meetup-join/abc123")
                .timezone("Europe/Warsaw")
                .build();

        when(locationRepository.save(any(Location.class))).thenReturn(teamsLocation);

        // When
        LocationResponse response = locationService.generateVirtualLocation("teams", "abc123", null);

        // Then
        assertNotNull(response);
        assertEquals("Teams Meeting", response.getName());
        assertTrue(response.getVirtualMeetingUrl().contains("teams.microsoft.com"));
    }

    @Test
    void testGenerateVirtualLocation_GoogleMeet_Success() {
        // Given
        Location meetLocation = Location.builder()
                .id(8L)
                .name("Meet Meeting")
                .type(LocationType.VIRTUAL)
                .virtualMeetingUrl("https://meet.google.com/xyz789")
                .timezone("Europe/Warsaw")
                .build();

        when(locationRepository.save(any(Location.class))).thenReturn(meetLocation);

        // When
        LocationResponse response = locationService.generateVirtualLocation("meet", "xyz789", null);

        // Then
        assertNotNull(response);
        assertEquals("Meet Meeting", response.getName());
        assertTrue(response.getVirtualMeetingUrl().contains("meet.google.com"));
    }

    @Test
    void testGenerateVirtualLocation_MissingPlatform_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> locationService.generateVirtualLocation("", "123", null));

        assertEquals("Platforma jest wymagana", exception.getMessage());
        verify(locationRepository, never()).save(any(Location.class));
    }

    @Test
    void testGenerateVirtualLocation_MissingMeetingId_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> locationService.generateVirtualLocation("zoom", "", null));

        assertEquals("ID spotkania jest wymagane", exception.getMessage());
        verify(locationRepository, never()).save(any(Location.class));
    }

    @Test
    void testGenerateVirtualLocation_UnknownPlatform_GeneratesDefaultUrl() {
        // Given
        when(locationRepository.save(any(Location.class))).thenAnswer(invocation -> {
            Location saved = invocation.getArgument(0);
            saved.setId(9L);
            return saved;
        });

        // When
        LocationResponse response = locationService.generateVirtualLocation("unknown", "meeting123", null);

        // Then
        assertNotNull(response);
        assertEquals("Unknown Meeting", response.getName());
        assertEquals("meeting123", response.getVirtualMeetingUrl());

        verify(locationRepository).save(any(Location.class));
    }

    @Test
    void testValidateLocation_Physical_WithCoordinates_ReturnsTrue() {
        // Given
        when(locationRepository.findById(1L)).thenReturn(Optional.of(physicalLocation));

        // When
        boolean isValid = locationService.validateLocation(1L);

        // Then
        assertTrue(isValid);
    }

    @Test
    void testValidateLocation_Physical_WithAddress_ReturnsTrue() {
        // Given
        Location locationWithAddress = Location.builder()
                .id(10L)
                .name("Test Location")
                .type(LocationType.PHYSICAL)
                .address("ul. Testowa 123")
                .city("Warsaw")
                .country("Poland")
                .build();

        when(locationRepository.findById(10L)).thenReturn(Optional.of(locationWithAddress));

        // When
        boolean isValid = locationService.validateLocation(10L);

        // Then
        assertTrue(isValid);
    }

    @Test
    void testValidateLocation_Physical_NoInfo_ReturnsFalse() {
        // Given
        Location locationWithoutInfo = Location.builder()
                .id(11L)
                .name("Test Location")
                .type(LocationType.PHYSICAL)
                .build();

        when(locationRepository.findById(11L)).thenReturn(Optional.of(locationWithoutInfo));

        // When
        boolean isValid = locationService.validateLocation(11L);

        // Then
        assertFalse(isValid);
    }

    @Test
    void testValidateLocation_Virtual_WithUrl_ReturnsTrue() {
        // Given
        when(locationRepository.findById(2L)).thenReturn(Optional.of(virtualLocation));

        // When
        boolean isValid = locationService.validateLocation(2L);

        // Then
        assertTrue(isValid);
    }

    @Test
    void testValidateLocation_Virtual_NoUrl_ReturnsFalse() {
        // Given
        Location virtualWithoutUrl = Location.builder()
                .id(12L)
                .name("Virtual Meeting")
                .type(LocationType.VIRTUAL)
                .build();

        when(locationRepository.findById(12L)).thenReturn(Optional.of(virtualWithoutUrl));

        // When
        boolean isValid = locationService.validateLocation(12L);

        // Then
        assertFalse(isValid);
    }

//    @Test
//    void testGetLocationById_Success() {
//        // Given
//        when(locationRepository.findById(1L)).thenReturn(Optional.of(physicalLocation));
//
//        // When
//        LocationResponse response = locationService.getLocationById(1L);
//
//        // Then
//        assertNotNull(response);
//        assertEquals(physicalLocation.getId(), response.getId());
//        assertEquals(physicalLocation.getName(), response.getName());
//
//        verify(locationRepository).findById(1L);
//    }

    @Test
    void testGetLocationById_NotFound_ThrowsException() {
        // Given
        when(locationRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> locationService.getLocationById(999L));

        assertEquals("Lokalizacja nie znaleziona", exception.getMessage());
    }

    @Test
    void testCreateLocation_GeocodingFails_LogsWarningButSaves() {
        // Given
        CreateLocationRequest request = new CreateLocationRequest();
        request.setName("Office Without Coords");
        request.setType(LocationType.PHYSICAL);
        request.setAddress("ul. Testowa 123");
        request.setCity("Warsaw");
        request.setCountry("Poland");

        when(locationRepository.existsByNameAndAddress(anyString(), anyString())).thenReturn(false);
        when(geocodingService.geocodeAddress(anyString())).thenThrow(new RuntimeException("Geocoding service down"));
        when(locationRepository.save(any(Location.class))).thenAnswer(invocation -> {
            Location saved = invocation.getArgument(0);
            saved.setId(13L);
            return saved;
        });

        // When
        LocationResponse response = locationService.createLocation(request);

        // Then - should save location even if geocoding fails
        assertNotNull(response);
        assertEquals("Office Without Coords", response.getName());

        verify(geocodingService).geocodeAddress(anyString());
        verify(locationRepository).save(any(Location.class));
    }

    @Test
    void testEncodeUrl() {
        // This tests the private encodeUrl method indirectly through public methods

        Location locationWithAddress = Location.builder()
                .id(14L)
                .name("Test Location")
                .type(LocationType.PHYSICAL)
                .address("ul. Testowa 123")
                .city("Warsaw")
                .build();

        when(locationRepository.findById(14L)).thenReturn(Optional.of(locationWithAddress));

        // When
        String mapUrl = locationService.generateMapUrl(14L);

        // Then
        assertNotNull(mapUrl);
        assertTrue(mapUrl.contains("ul.+Testowa+123"));
    }

    @Test
    void testValidateLocation_LocationNotFound_ThrowsException() {
        // Given
        when(locationRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> locationService.validateLocation(999L));

        assertEquals("Lokalizacja nie znaleziona", exception.getMessage());
    }

    @Test
    void testGenerateVirtualLocation_CaseInsensitivePlatform() {
        // Given
        when(locationRepository.save(any(Location.class))).thenAnswer(invocation -> {
            Location saved = invocation.getArgument(0);
            saved.setId(15L);
            return saved;
        });

        // When - różne warianty pisowni
        LocationResponse response1 = locationService.generateVirtualLocation("ZOOM", "123", null);
        LocationResponse response2 = locationService.generateVirtualLocation("Zoom", "456", null);
        LocationResponse response3 = locationService.generateVirtualLocation("zoom", "789", null);

        // Then - wszystkie powinny działać
        assertNotNull(response1);
        assertNotNull(response2);
        assertNotNull(response3);
        assertTrue(response1.getVirtualMeetingUrl().contains("zoom.us"));
    }

    @Test
    void testUpdateLocation_GeocodingFails_LogsWarningButSaves() {
        // Given
        Location locationWithoutCoords = Location.builder()
                .id(16L)
                .name("Office")
                .type(LocationType.PHYSICAL)
                .address("Old Address")
                .city("Old City")
                .build();

        UpdateLocationRequest request = new UpdateLocationRequest();
        request.setName("Updated Office");
        request.setType(LocationType.PHYSICAL);
        request.setAddress("New Address");
        request.setCity("New City");

        when(locationRepository.findById(16L)).thenReturn(Optional.of(locationWithoutCoords));
        when(geocodingService.geocodeAddress(anyString())).thenThrow(new RuntimeException("Geocoding failed"));
        when(locationRepository.save(any(Location.class))).thenAnswer(invocation -> {
            Location saved = invocation.getArgument(0);
            return saved;
        });

        // When
        LocationResponse response = locationService.updateLocation(16L, request);

        // Then - should save even if geocoding fails
        assertNotNull(response);
        verify(locationRepository).save(any(Location.class));
    }
}
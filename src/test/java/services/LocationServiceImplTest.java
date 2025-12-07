package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.Location;
import com.meethub.domain.model.enums.LocationType;
import com.meethub.domain.model.mapper.LocationMapper;
import com.meethub.domain.model.request.CreateLocationRequest;
import com.meethub.domain.model.request.LocationSearchRequest;
import com.meethub.domain.model.request.UpdateLocationRequest;
import com.meethub.domain.model.response.LocationListResponse;
import com.meethub.domain.model.response.LocationResponse;
import com.meethub.domain.repository.jpa.LocationRepository;
import com.meethub.domain.service.GeocodingService;
import com.meethub.exception.BusinessException;
import com.meethub.exception.ResourceNotFoundException;
import com.meethub.exception.ValidationException;
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

    @Mock
    private LocationMapper locationMapper;

    @InjectMocks
    private LocationServiceImpl locationService;

    private Location physicalLocation;
    private Location virtualLocation;
    private LocationResponse physicalLocationResponse;
    private LocationResponse virtualLocationResponse;

    @BeforeEach
    void setUp() {
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
                .meetings(new ArrayList<>())
                .build();

        virtualLocation = Location.builder()
                .id(2L)
                .name("Zoom Meeting")
                .type(LocationType.VIRTUAL)
                .virtualMeetingUrl("https://zoom.us/j/123456789")
                .accessCode("123456")
                .timezone("Europe/Warsaw")
                .meetings(new ArrayList<>())
                .build();

        physicalLocationResponse = LocationResponse.builder()
                .id(1L)
                .name("Test Office")
                .type(LocationType.PHYSICAL)
                .address("ul. Testowa 123")
                .city("Warsaw")
                .country("Poland")
                .latitude(new BigDecimal("52.2297"))
                .longitude(new BigDecimal("21.0122"))
                .build();

        virtualLocationResponse = LocationResponse.builder()
                .id(2L)
                .name("Zoom Meeting")
                .type(LocationType.VIRTUAL)
                .virtualMeetingUrl("https://zoom.us/j/123456789")
                .build();
    }

    @Test
    void createLocation_Physical_Success() {
        // Given
        CreateLocationRequest request = CreateLocationRequest.builder()
                .name("New Office")
                .type(LocationType.PHYSICAL)
                .address("ul. Nowa 456")
                .city("Krakow")
                .country("Poland")
                .build();

        Location newLocation = Location.builder().id(1L).build();
        LocationResponse expectedResponse = LocationResponse.builder().id(1L).name("Test Office").build();

        when(locationRepository.existsByNameAndAddress(anyString(), anyString())).thenReturn(false);
        when(locationMapper.toEntity(any(CreateLocationRequest.class))).thenReturn(newLocation);
        when(locationRepository.save(any(Location.class))).thenReturn(newLocation);
        when(locationMapper.toResponse(any(Location.class))).thenReturn(expectedResponse);

        // When
        LocationResponse response = locationService.createLocation(request);

        // Then
        assertAll(
                () -> assertNotNull(response),
                () -> assertEquals(1L, response.getId()),
                () -> assertEquals("Test Office", response.getName())
        );

        verify(locationRepository).existsByNameAndAddress("New Office", "ul. Nowa 456");
        verify(locationRepository).save(newLocation);
    }

    @Test
    void createLocation_Physical_NameAndAddressAlreadyExists_ThrowsValidationException() {
        // Given
        CreateLocationRequest request = CreateLocationRequest.builder()
                .name("Existing Office")
                .type(LocationType.PHYSICAL)
                .address("ul. Istniejaca 123")
                .build();

        when(locationRepository.existsByNameAndAddress("Existing Office", "ul. Istniejaca 123")).thenReturn(true);

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> locationService.createLocation(request));

        assertEquals("Lokalizacja o tej nazwie i adresie już istnieje", exception.getMessage());
        verify(locationRepository, never()).save(any(Location.class));
    }

    @Test
    void updateLocation_Success() {
        // Given
        UpdateLocationRequest request = UpdateLocationRequest.builder()
                .name("Updated Office")
                .type(LocationType.PHYSICAL)
                .build();

        LocationResponse expectedResponse = LocationResponse.builder().id(1L).name("Updated Office").build();

        when(locationRepository.findById(1L)).thenReturn(Optional.of(physicalLocation));
        when(locationRepository.save(any(Location.class))).thenReturn(physicalLocation);
        when(locationMapper.toResponse(any(Location.class))).thenReturn(expectedResponse);

        // When
        LocationResponse response = locationService.updateLocation(1L, request);

        // Then
        assertAll(
                () -> assertNotNull(response),
                () -> assertEquals(1L, response.getId()),
                () -> assertEquals("Updated Office", response.getName())
        );

        verify(locationRepository).save(physicalLocation);
    }

    @Test
    void updateLocation_LocationNotFound_ThrowsResourceNotFoundException() {
        // Given
        UpdateLocationRequest request = UpdateLocationRequest.builder().name("Updated Office").build();
        when(locationRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> locationService.updateLocation(999L, request));

        assertEquals("Lokalizacja", exception.getMessage());
        verify(locationRepository, never()).save(any(Location.class));
    }

    @Test
    void deleteLocation_Success() {
        // Given
        when(locationRepository.findById(1L)).thenReturn(Optional.of(physicalLocation));

        // When & Then
        assertDoesNotThrow(() -> locationService.deleteLocation(1L));
        verify(locationRepository).delete(physicalLocation);
    }

    @Test
    void deleteLocation_LocationUsedInMeetings_ThrowsBusinessException() {
        // Given
        Location locationWithMeetings = physicalLocation;
        com.meethub.domain.model.entity.Meeting mockMeeting = mock(com.meethub.domain.model.entity.Meeting.class);
        locationWithMeetings.setMeetings(List.of(mockMeeting, mockMeeting));

        when(locationRepository.findById(1L)).thenReturn(Optional.of(locationWithMeetings));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> locationService.deleteLocation(1L));

        assertAll(
                () -> assertTrue(exception.getMessage().contains("Nie można usunąć lokalizacji używanej w spotkaniach")),
                () -> assertTrue(exception.getMessage().contains("2"))
        );

        verify(locationRepository, never()).delete(any(Location.class));
    }

    @Test
    void getLocation_Success() {
        // Given
        when(locationRepository.findById(1L)).thenReturn(Optional.of(physicalLocation));
        when(locationMapper.toResponse(physicalLocation)).thenReturn(physicalLocationResponse);

        // When
        LocationResponse response = locationService.getLocation(1L);

        // Then
        assertAll(
                () -> assertNotNull(response),
                () -> assertEquals(1L, response.getId()),
                () -> assertEquals("Test Office", response.getName())
        );
    }

    @Test
    void searchLocations_Success() {
        // Given
        LocationSearchRequest request = LocationSearchRequest.builder()
                .query("Office")
                .type(LocationType.PHYSICAL)
                .city("Warsaw")
                .page(0)
                .size(10)
                .build();

        Page<Location> locationPage = new PageImpl<>(List.of(physicalLocation), PageRequest.of(0, 10), 1);

        when(locationRepository.searchLocations("Office", "PHYSICAL", "Warsaw", PageRequest.of(0, 10)))
                .thenReturn(locationPage);
        when(locationMapper.toResponse(physicalLocation)).thenReturn(physicalLocationResponse);

        // When
        LocationListResponse response = locationService.searchLocations(request);

        // Then
        assertAll(
                () -> assertNotNull(response),
                () -> assertEquals(1, response.getLocations().size()),
                () -> assertEquals(0, response.getCurrentPage()),
                () -> assertEquals(1, response.getTotalPages()),
                () -> assertEquals(1, response.getTotalItems()),
                () -> assertFalse(response.isHasNext()),
                () -> assertFalse(response.isHasPrevious())
        );
    }

    @Test
    void findNearbyLocations_Success() {
        // Given
        BigDecimal lat = new BigDecimal("52.2297");
        BigDecimal lng = new BigDecimal("21.0122");

        when(locationRepository.findNearbyLocations(lat, lng, 10.0)).thenReturn(List.of(physicalLocation));
        when(locationMapper.toResponse(physicalLocation)).thenReturn(physicalLocationResponse);

        // When
        List<LocationResponse> responses = locationService.findNearbyLocations(lat, lng, 10.0);

        // Then
        assertAll(
                () -> assertNotNull(responses),
                () -> assertEquals(1, responses.size()),
                () -> assertEquals(1L, responses.get(0).getId())
        );
    }

    @Test
    void findNearbyLocations_InvalidCoordinates_ThrowsValidationException() {
        // Test invalid latitude (below -90)
        assertAll(
                () -> {
                    ValidationException exception = assertThrows(ValidationException.class,
                            () -> locationService.findNearbyLocations(new BigDecimal("-100"), new BigDecimal("21.0122"), 10.0));
                    assertEquals("Szerokość geograficzna musi być między -90 a 90", exception.getMessage());
                },
                () -> {
                    ValidationException exception = assertThrows(ValidationException.class,
                            () -> locationService.findNearbyLocations(new BigDecimal("100"), new BigDecimal("21.0122"), 10.0));
                    assertEquals("Szerokość geograficzna musi być między -90 a 90", exception.getMessage());
                }
        );
    }

    @Test
    void generateMapUrl_WithCoordinates() {
        // Given
        when(locationRepository.findById(1L)).thenReturn(Optional.of(physicalLocation));

        // When
        String mapUrl = locationService.generateMapUrl(1L);

        // Then
        assertAll(
                () -> assertNotNull(mapUrl),
                () -> assertTrue(mapUrl.contains("52.2297")),
                () -> assertTrue(mapUrl.contains("21.0122")),
                () -> assertTrue(mapUrl.startsWith("https://maps.google.com/maps?q="))
        );
    }

    @Test
    void generateDirectionsUrl_WithCoordinates() {
        // Given
        when(locationRepository.findById(1L)).thenReturn(Optional.of(physicalLocation));

        // When
        String directionsUrl = locationService.generateDirectionsUrl(1L, "Start Address");

        // Then
        assertAll(
                () -> assertNotNull(directionsUrl),
                () -> assertTrue(directionsUrl.contains("52.2297,21.0122")),
                () -> assertTrue(directionsUrl.contains("saddr=Start+Address")),
                () -> assertTrue(directionsUrl.startsWith("https://maps.google.com/maps?saddr="))
        );
    }

    @Test
    void generateVirtualLocation_Zoom_Success() {
        // Given
        Location zoomLocation = Location.builder()
                .id(5L)
                .name("Zoom Meeting")
                .virtualMeetingUrl("https://zoom.us/j/123456789?pwd=abcdef")
                .build();

        LocationResponse expectedResponse = LocationResponse.builder()
                .id(5L)
                .name("Zoom Meeting")
                .virtualMeetingUrl("https://zoom.us/j/123456789?pwd=abcdef")
                .build();

        when(locationRepository.save(any(Location.class))).thenReturn(zoomLocation);
        when(locationMapper.toResponse(any(Location.class))).thenReturn(expectedResponse);

        // When
        LocationResponse response = locationService.generateVirtualLocation("zoom", "123456789", "abcdef");

        // Then
        assertAll(
                () -> assertNotNull(response),
                () -> assertEquals("Zoom Meeting", response.getName()),
                () -> assertTrue(response.getVirtualMeetingUrl().contains("zoom.us/j/123456789")),
                () -> assertTrue(response.getVirtualMeetingUrl().contains("pwd=abcdef"))
        );
    }

    @Test
    void generateVirtualLocation_MissingPlatform_ThrowsValidationException() {
        // When & Then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> locationService.generateVirtualLocation("", "123", null));

        assertEquals("Platforma jest wymagana", exception.getMessage());
        verify(locationRepository, never()).save(any(Location.class));
    }

    @Test
    void getAllLocations_Success() {
        // Given
        List<Location> locations = List.of(physicalLocation, virtualLocation);
        when(locationRepository.findAll()).thenReturn(locations);
        when(locationMapper.toResponse(physicalLocation)).thenReturn(physicalLocationResponse);
        when(locationMapper.toResponse(virtualLocation)).thenReturn(virtualLocationResponse);

        // When
        List<LocationResponse> responses = locationService.getAllLocations();

        // Then
        assertAll(
                () -> assertNotNull(responses),
                () -> assertEquals(2, responses.size())
        );
    }

    @Test
    void getPhysicalLocations_Success() {
        // Given
        when(locationRepository.findAll()).thenReturn(List.of(physicalLocation));
        when(locationMapper.toResponse(physicalLocation)).thenReturn(physicalLocationResponse);

        // When
        List<LocationResponse> responses = locationService.getPhysicalLocations();

        // Then
        assertAll(
                () -> assertNotNull(responses),
                () -> assertEquals(1, responses.size()),
                () -> assertEquals(LocationType.PHYSICAL, responses.get(0).getType())
        );
    }

    @Test
    void getVirtualLocations_Success() {
        // Given
        when(locationRepository.findAll()).thenReturn(List.of(virtualLocation));
        when(locationMapper.toResponse(virtualLocation)).thenReturn(virtualLocationResponse);

        // When
        List<LocationResponse> responses = locationService.getVirtualLocations();

        // Then
        assertAll(
                () -> assertNotNull(responses),
                () -> assertEquals(1, responses.size()),
                () -> assertEquals(LocationType.VIRTUAL, responses.get(0).getType())
        );
    }

    @Test
    void getLocationEntity_Success() {
        // Given
        when(locationRepository.findById(1L)).thenReturn(Optional.of(physicalLocation));

        // When
        Location location = locationService.getLocationEntity(1L);

        // Then
        assertAll(
                () -> assertNotNull(location),
                () -> assertEquals(1L, location.getId()),
                () -> assertEquals("Test Office", location.getName())
        );
    }

    @Test
    void saveLocationEntity_Success() {
        // Given
        when(locationRepository.save(physicalLocation)).thenReturn(physicalLocation);
        when(locationMapper.toResponse(physicalLocation)).thenReturn(physicalLocationResponse);

        // When
        LocationResponse response = locationService.saveLocationEntity(physicalLocation);

        // Then
        assertAll(
                () -> assertNotNull(response),
                () -> assertEquals(1L, response.getId())
        );
    }

    @Test
    void createLocation_Physical_WithGeocoding_Success() {
        // Given
        CreateLocationRequest request = CreateLocationRequest.builder()
                .name("Office Without Coords")
                .type(LocationType.PHYSICAL)
                .address("ul. Testowa 123")
                .city("Warsaw")
                .country("Poland")
                .build();

        Location locationWithoutCoords = Location.builder()
                .name("Office Without Coords")
                .type(LocationType.PHYSICAL)
                .address("ul. Testowa 123")
                .city("Warsaw")
                .country("Poland")
                .build();

        Location geocodedLocation = Location.builder()
                .latitude(new BigDecimal("52.2297"))
                .longitude(new BigDecimal("21.0122"))
                .build();

        Location savedLocation = Location.builder().id(3L).build();
        LocationResponse expectedResponse = LocationResponse.builder().id(3L).build();

        when(locationRepository.existsByNameAndAddress(anyString(), anyString())).thenReturn(false);
        when(locationMapper.toEntity(any(CreateLocationRequest.class))).thenReturn(locationWithoutCoords);
        when(geocodingService.geocodeAddress("ul. Testowa 123, Warsaw, Poland")).thenReturn(geocodedLocation);
        when(locationRepository.save(any(Location.class))).thenReturn(savedLocation);
        when(locationMapper.toResponse(any(Location.class))).thenReturn(expectedResponse);

        // When
        LocationResponse response = locationService.createLocation(request);

        // Then
        assertAll(
                () -> assertNotNull(response),
                () -> assertEquals(3L, response.getId())
        );

        verify(geocodingService).geocodeAddress("ul. Testowa 123, Warsaw, Poland");
    }

    @Test
    void updateLocation_WithGeocoding_Success() {
        // Given
        Location locationWithoutCoords = Location.builder()
                .id(3L)
                .name("Office Without Coords")
                .type(LocationType.PHYSICAL)
                .address("Old Address")
                .city("Old City")
                .build();

        UpdateLocationRequest request = UpdateLocationRequest.builder()
                .name("Updated Office")
                .type(LocationType.PHYSICAL)
                .address("ul. Nowa 456")
                .city("Warsaw")
                .country("Poland")
                .build();

        Location geocodedLocation = Location.builder()
                .latitude(new BigDecimal("52.2297"))
                .longitude(new BigDecimal("21.0122"))
                .build();

        // Mockowanie mapper.updateEntity aby faktycznie zaktualizować encję
        doAnswer(invocation -> {
            Location location = invocation.getArgument(0);
            UpdateLocationRequest req = invocation.getArgument(1);
            location.setAddress(req.getAddress());
            location.setCity(req.getCity());
            location.setCountry(req.getCountry());
            return null;
        }).when(locationMapper).updateEntity(any(Location.class), any(UpdateLocationRequest.class));

        when(locationRepository.findById(3L)).thenReturn(Optional.of(locationWithoutCoords));
        when(geocodingService.geocodeAddress("ul. Nowa 456, Warsaw, Poland")).thenReturn(geocodedLocation);
        when(locationRepository.save(any(Location.class))).thenReturn(locationWithoutCoords);
        when(locationMapper.toResponse(any(Location.class))).thenReturn(LocationResponse.builder().id(3L).build());

        // When
        LocationResponse response = locationService.updateLocation(3L, request);

        // Then
        assertAll(
                () -> assertNotNull(response),
                () -> verify(geocodingService).geocodeAddress("ul. Nowa 456, Warsaw, Poland")
        );
    }


    @Test
    void findNearbyLocations_WithDefaultRadius() {
        // Given
        BigDecimal lat = new BigDecimal("52.2297");
        BigDecimal lng = new BigDecimal("21.0122");

        when(locationRepository.findNearbyLocations(lat, lng, 10.0)).thenReturn(Collections.emptyList());

        // When
        List<LocationResponse> responses = locationService.findNearbyLocations(lat, lng, null);

        // Then
        assertAll(
                () -> assertNotNull(responses),
                () -> assertTrue(responses.isEmpty())
        );
    }

    @Test
    void generateVirtualLocation_CaseInsensitivePlatform() {
        // Given
        when(locationRepository.save(any(Location.class))).thenAnswer(invocation -> {
            Location saved = invocation.getArgument(0);
            saved.setId(15L);
            return saved;
        });

        when(locationMapper.toResponse(any(Location.class))).thenAnswer(invocation -> {
            Location location = invocation.getArgument(0);
            return LocationResponse.builder()
                    .id(location.getId())
                    .name(location.getName())
                    .virtualMeetingUrl(location.getVirtualMeetingUrl())
                    .build();
        });

        // When - different case variations
        LocationResponse response1 = locationService.generateVirtualLocation("ZOOM", "123", null);
        LocationResponse response2 = locationService.generateVirtualLocation("Zoom", "456", null);
        LocationResponse response3 = locationService.generateVirtualLocation("zoom", "789", null);

        // Then
        assertAll(
                () -> assertNotNull(response1),
                () -> assertNotNull(response2),
                () -> assertNotNull(response3),
                () -> assertTrue(response1.getVirtualMeetingUrl().contains("zoom.us")),
                () -> assertTrue(response2.getVirtualMeetingUrl().contains("zoom.us")),
                () -> assertTrue(response3.getVirtualMeetingUrl().contains("zoom.us"))
        );
    }

    @Test
    void createLocation_Virtual_Success() {
        // Given
        CreateLocationRequest request = CreateLocationRequest.builder()
                .name("Team Meeting")
                .type(LocationType.VIRTUAL)
                .virtualMeetingUrl("https://teams.microsoft.com/l/meetup-join/abc123")
                .build();

        Location newLocation = Location.builder().id(2L).build();

        when(locationRepository.findByVirtualMeetingUrl("https://teams.microsoft.com/l/meetup-join/abc123"))
                .thenReturn(Optional.empty());
        when(locationMapper.toEntity(any(CreateLocationRequest.class))).thenReturn(newLocation);
        when(locationRepository.save(any(Location.class))).thenReturn(newLocation);
        when(locationMapper.toResponse(any(Location.class))).thenReturn(virtualLocationResponse);

        // When
        LocationResponse response = locationService.createLocation(request);

        // Then
        assertAll(
                () -> assertNotNull(response),
                () -> assertEquals(2L, response.getId()),
                () -> assertEquals(LocationType.VIRTUAL, response.getType())
        );
    }
}
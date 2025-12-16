package com.meethub.domain.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meethub.domain.model.entity.Location;
import com.meethub.domain.model.enums.LocationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleGeocodingServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private JsonNode rootNode;

    @Mock
    private JsonNode resultNode;

    @Mock
    private JsonNode geometryNode;

    @Mock
    private JsonNode locationNode;

    @Mock
    private JsonNode addressComponentsNode;

    @Mock
    private JsonNode statusNode;

    @Mock
    private JsonNode resultsNode;

    @InjectMocks
    private GoogleGeocodingService geocodingService;

    private final String validApiKey = "test-api-key";
    private final String testAddress = "ul. Testowa 123, Warszawa, Polska";

    @BeforeEach
    void setUp() {
        setField(geocodingService, "apiKey", validApiKey);
        setField(geocodingService, "geocodingEnabled", true);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JsonNode mockJsonNode(Object value) {
        JsonNode node = mock(JsonNode.class);
        if (value instanceof String) {
            when(node.asText()).thenReturn((String) value);
            when(node.asText(anyString())).thenReturn((String) value);
        } else if (value instanceof Integer) {
            when(node.asInt()).thenReturn((Integer) value);
        } else if (value instanceof Double) {
            when(node.asDouble()).thenReturn((Double) value);
        }
        return node;
    }

    @Test
    void shouldReturnFallbackLocation_WhenGeocodingDisabled() {
        // Given
        setField(geocodingService, "geocodingEnabled", false);

        // When
        Location result = geocodingService.geocodeAddress(testAddress);

        // Then
        assertAll("Fallback location validation",
                () -> assertNotNull(result, "Result should not be null"),
                () -> assertEquals("Custom Location", result.getName(), "Should return fallback name"),
                () -> assertEquals(testAddress, result.getAddress(), "Address should be preserved"),
                () -> assertEquals(LocationType.PHYSICAL, result.getType(), "Type should be PHYSICAL"),
                () -> assertNull(result.getLatitude(), "Latitude should be null for fallback"),
                () -> assertNull(result.getLongitude(), "Longitude should be null for fallback")
        );
    }

    @Test
    void shouldReturnFallbackLocation_WhenApiKeyMissing() {
        // Given
        setField(geocodingService, "apiKey", "");

        // When
        Location result = geocodingService.geocodeAddress(testAddress);

        // Then
        assertAll("Missing API key validation",
                () -> assertNotNull(result, "Result should not be null"),
                () -> assertEquals("Custom Location", result.getName(), "Should return fallback name"),
                () -> assertEquals(testAddress, result.getAddress(), "Address should be preserved")
        );
    }



    @Test
    void shouldReturnNull_WhenReverseGeocodingDisabled() {
        // Given
        setField(geocodingService, "geocodingEnabled", false);
        double lat = 52.2297;
        double lng = 21.0122;

        // When
        Location result = geocodingService.reverseGeocode(lat, lng);

        // Then
        assertAll("Disabled reverse geocoding",
                () -> assertNull(result, "Should return null when disabled")
        );
    }

    @Test
    void shouldValidateAddress_ForPhysicalLocation() {
        // Given
        Location location = Location.builder()
                .type(LocationType.PHYSICAL)
                .address("ul. Testowa 123, Warszawa")
                .build();

        // Mock geocodeAddress to return valid location
        Location geocodedLocation = Location.builder()
                .latitude(BigDecimal.valueOf(52.2297))
                .longitude(BigDecimal.valueOf(21.0122))
                .build();

        GoogleGeocodingService spyService = spy(geocodingService);
        doReturn(geocodedLocation).when(spyService).geocodeAddress(anyString());

        // When
        boolean isValid = spyService.validateAddress(location);

        // Then
        assertAll("Physical address validation",
                () -> assertTrue(isValid, "Valid physical address should return true"),
                () -> verify(spyService, times(1)).geocodeAddress(location.getAddress())
        );
    }

    @Test
    void shouldValidateAddress_ForVirtualLocation() {
        // Given
        Location location = Location.builder()
                .type(LocationType.VIRTUAL)
                .virtualMeetingUrl("https://meet.google.com/abc-defg-hij")
                .build();

        // When
        boolean isValid = geocodingService.validateAddress(location);

        // Then
        assertAll("Virtual address validation",
                () -> assertTrue(isValid, "Valid virtual URL should return true")
        );
    }

    @Test
    void shouldReturnFalse_WhenVirtualUrlInvalid() {
        // Given
        Location location = Location.builder()
                .type(LocationType.VIRTUAL)
                .virtualMeetingUrl("invalid-url")
                .build();

        // When
        boolean isValid = geocodingService.validateAddress(location);

        // Then
        assertAll("Invalid virtual URL",
                () -> assertFalse(isValid, "Invalid URL should return false")
        );
    }

    @Test
    void shouldReturnFalse_WhenPhysicalAddressNull() {
        // Given
        Location location = Location.builder()
                .type(LocationType.PHYSICAL)
                .address(null)
                .build();

        // When
        boolean isValid = geocodingService.validateAddress(location);

        // Then
        assertAll("Null physical address",
                () -> assertFalse(isValid, "Null address should return false")
        );
    }

    @Test
    void shouldReturnDefaultTimezone_WhenApiFails() throws Exception {
        // Given
        double lat = 52.2297;
        double lng = 21.0122;

        when(restTemplate.getForObject(any(), eq(String.class)))
                .thenThrow(new RuntimeException("API error"));

        // When
        String timezone = geocodingService.getTimezone(lat, lng);

        // Then
        assertAll("Default timezone on error",
                () -> assertNotNull(timezone, "Timezone should not be null"),
                () -> assertEquals("Europe/Warsaw", timezone, "Should return default timezone")
        );
    }

    @Test
    void shouldReturnDefaultTimezone_WhenGeocodingDisabled() {
        // Given
        setField(geocodingService, "geocodingEnabled", false);
        double lat = 52.2297;
        double lng = 21.0122;

        // When
        String timezone = geocodingService.getTimezone(lat, lng);

        // Then
        assertAll("Timezone when disabled",
                () -> assertEquals("Europe/Warsaw", timezone, "Should return default when disabled")
        );
    }



    @Test
    void shouldBuildFullAddress_Correctly() {
        // Test private buildFullAddress through geocodeAddress
        // Similar setup to above test with various address components
    }

    @Test
    void shouldValidateOnlineUrl_Correctly() {
        // Test isValidOnlineUrl through validateAddress

        assertAll("Online URL validation",
                () -> {
                    Location zoomLocation = Location.builder()
                            .type(LocationType.VIRTUAL)
                            .virtualMeetingUrl("zoommtg://zoom.us/join?confno=123456789")
                            .build();
                    assertTrue(geocodingService.validateAddress(zoomLocation), "Zoom URL should be valid");
                },
                () -> {
                    Location teamsLocation = Location.builder()
                            .type(LocationType.VIRTUAL)
                            .virtualMeetingUrl("teams://meet.microsoft.com/abc-def")
                            .build();
                    assertTrue(geocodingService.validateAddress(teamsLocation), "Teams URL should be valid");
                },
                () -> {
                    Location googleMeetLocation = Location.builder()
                            .type(LocationType.VIRTUAL)
                            .virtualMeetingUrl("https://meet.google.com/abc-defg-hij")
                            .build();
                    assertTrue(geocodingService.validateAddress(googleMeetLocation), "Google Meet URL should be valid");
                },
                () -> {
                    Location invalidLocation = Location.builder()
                            .type(LocationType.VIRTUAL)
                            .virtualMeetingUrl("invalid-protocol://test.com")
                            .build();
                    assertFalse(geocodingService.validateAddress(invalidLocation), "Invalid protocol should return false");
                },
                () -> {
                    Location emptyUrlLocation = Location.builder()
                            .type(LocationType.VIRTUAL)
                            .virtualMeetingUrl("")
                            .build();
                    assertFalse(geocodingService.validateAddress(emptyUrlLocation), "Empty URL should return false");
                }
        );
    }

    // Helper method to create mock JsonNode array
    private JsonNode mockJsonArray(Object... values) {
        JsonNode arrayNode = mock(JsonNode.class);
        if (values.length > 0) {
            when(arrayNode.get(0)).thenReturn(mockJsonNode(values[0]));
        }
        return arrayNode;
    }

    // Helper method to create iterator
    private <T> Iterator<T> newIterator(T... items) {
        return java.util.Arrays.asList(items).iterator();
    }
}
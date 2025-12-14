//// LocationService.java - NIE dziedziczy po JpaRepository!
//package com.meethub.domain.service;
//
//import com.meethub.domain.model.entity.Location;
//import com.meethub.domain.model.projection.LocationBasicInfo;
//import com.meethub.domain.model.request.CreateLocationRequest;
//import com.meethub.domain.model.request.LocationSearchRequest;
//import com.meethub.domain.model.request.UpdateLocationRequest;
//import com.meethub.domain.model.response.LocationListResponse;
//import com.meethub.domain.model.response.LocationResponse;
//
//import java.math.BigDecimal;
//import java.util.List;
//
//public interface LocationService {
//
//    // ✅ CRUD operations
//    LocationResponse createLocation(CreateLocationRequest request);
//    LocationResponse updateLocation(Long id, UpdateLocationRequest request);
//    void deleteLocation(Long id);
//    LocationResponse getLocation(Long id);
//
//    // ✅ Search and filtering
//    LocationListResponse searchLocations(LocationSearchRequest request);
//    List<LocationResponse> findNearbyLocations(BigDecimal lat, BigDecimal lng, Double radiusKm);
//
//    // ✅ Tools
//    String generateMapUrl(Long locationId);
//    String generateDirectionsUrl(Long locationId, String origin);
//    LocationResponse generateVirtualLocation(String platform, String meetingId, String passcode);
//
//    // ✅ Location retrieval methods
//    List<LocationResponse> getAllLocations();
//    List<LocationResponse> getAvailableLocations();
//    List<LocationResponse> getPhysicalLocations();
//    List<LocationResponse> getVirtualLocations();
//
//    // ✅ Entity methods (if needed)
//    Location getLocationEntity(Long id);
//    LocationResponse saveLocationEntity(Location location);
//
//    List<LocationBasicInfo> getLocationsForSelect();
//    List<LocationBasicInfo> getAllLocationsBasic();
//
//    void validateLocationExists(Long locationId);
//}





package com.meethub.domain.service;

import com.meethub.domain.model.entity.Location;
import com.meethub.domain.model.projection.LocationBasicInfo;
import com.meethub.domain.model.request.CreateLocationRequest;
import com.meethub.domain.model.request.LocationSearchRequest;
import com.meethub.domain.model.request.UpdateLocationRequest;
import com.meethub.domain.model.response.LocationListResponse;
import com.meethub.domain.model.response.LocationResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public interface LocationService {

    // ================= CRUD operations =================
    LocationResponse createLocation(
            @Valid @NotNull(message = "Żądanie utworzenia lokalizacji nie może być puste")
            CreateLocationRequest request
    );

    LocationResponse updateLocation(
            @NotNull(message = "Identyfikator lokalizacji nie może być pusty")
            @Positive(message = "Identyfikator lokalizacji musi być liczbą dodatnią")
            Long id,

            @Valid @NotNull(message = "Żądanie aktualizacji lokalizacji nie może być puste")
            UpdateLocationRequest request
    );

    void deleteLocation(
            @NotNull(message = "Identyfikator lokalizacji nie może być pusty")
            @Positive(message = "Identyfikator lokalizacji musi być liczbą dodatnią")
            Long id
    );

    LocationResponse getLocation(
            @NotNull(message = "Identyfikator lokalizacji nie może być pusty")
            @Positive(message = "Identyfikator lokalizacji musi być liczbą dodatnią")
            Long id
    );

    // ================= Search and filtering =================
    LocationListResponse searchLocations(
            @Valid @NotNull(message = "Żądanie wyszukiwania lokalizacji nie może być puste")
            LocationSearchRequest request
    );

    List<LocationResponse> findNearbyLocations(
            @NotNull(message = "Szerokość geograficzna nie może być pusta")
            @DecimalMin(value = "-90.0", message = "Szerokość geograficzna musi być większa lub równa -90")
            @DecimalMax(value = "90.0", message = "Szerokość geograficzna musi być mniejsza lub równa 90")
            BigDecimal lat,

            @NotNull(message = "Długość geograficzna nie może być pusta")
            @DecimalMin(value = "-180.0", message = "Długość geograficzna musi być większa lub równa -180")
            @DecimalMax(value = "180.0", message = "Długość geograficzna musi być mniejsza lub równa 180")
            BigDecimal lng,

            @DecimalMin(value = "0.1", message = "Promień wyszukiwania musi być co najmniej 0.1 km")
            @DecimalMax(value = "1000.0", message = "Promień wyszukiwania nie może przekraczać 1000 km")
            Double radiusKm
    );

    // ================= Tools =================
    String generateMapUrl(
            @NotNull(message = "Identyfikator lokalizacji nie może być pusty")
            @Positive(message = "Identyfikator lokalizacji musi być liczbą dodatnią")
            Long locationId
    );

    String generateDirectionsUrl(
            @NotNull(message = "Identyfikator lokalizacji nie może być pusty")
            @Positive(message = "Identyfikator lokalizacji musi być liczbą dodatnią")
            Long locationId,

            @Size(max = 500, message = "Miejsce początkowe nie może przekraczać 500 znaków")
            String origin
    );

    LocationResponse generateVirtualLocation(
            @NotBlank(message = "Platforma nie może być pusta")
            @Pattern(regexp = "^(ZOOM|TEAMS|GOOGLE_MEET|MEET|WEBEX|OTHER)$",
                    message = "Nieobsługiwana platforma. Dozwolone wartości: ZOOM, TEAMS, GOOGLE_MEET, MEET, WEBEX, OTHER")
            String platform,

            @NotBlank(message = "ID spotkania nie może być puste")
            @Size(max = 100, message = "ID spotkania nie może przekraczać 100 znaków")
            @Pattern(regexp = "^[a-zA-Z0-9\\-_]+$",
                    message = "ID spotkania może zawierać tylko litery, cyfry, myślniki i podkreślniki")
            String meetingId,

            @Size(max = 50, message = "Kod dostępu nie może przekraczać 50 znaków")
            String passcode
    );

    // ================= Location retrieval =================
    List<LocationResponse> getAllLocations();
    List<LocationResponse> getAvailableLocations();
    List<LocationResponse> getPhysicalLocations();
    List<LocationResponse> getVirtualLocations();

    // ================= Entity methods =================
    Location getLocationEntity(
            @NotNull(message = "Identyfikator lokalizacji nie może być pusty")
            @Positive(message = "Identyfikator lokalizacji musi być liczbą dodatnią")
            Long id
    );

    LocationResponse saveLocationEntity(
            @Valid @NotNull(message = "Lokalizacja nie może być pusta")
            Location location
    );

    List<LocationBasicInfo> getLocationsForSelect();
    List<LocationBasicInfo> getAllLocationsBasic();

    void validateLocationExists(
            @Positive(message = "Identyfikator lokalizacji musi być liczbą dodatnią")
            Long locationId
    );
}

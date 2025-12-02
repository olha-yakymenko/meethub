// LocationServiceImpl.java (poprawiona wersja)
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
import com.meethub.domain.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Validated
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;
    private final GeocodingService geocodingService;

    @Override
    @Transactional
    public LocationResponse createLocation(@Valid CreateLocationRequest request) {
        log.info("Creating location: {}", request.getName());

        // Dodatkowa walidacja logiki biznesowej
        validateCreateRequest(request);

        Location location = mapToEntity(request);

        // Geokodowanie dla lokalizacji fizycznej
        if (location.getType() == LocationType.PHYSICAL && needsGeocoding(location)) {
            performGeocoding(location);
        }

        Location saved = locationRepository.save(location);
        log.info("Location created successfully: {} (ID: {})", saved.getName(), saved.getId());

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public LocationResponse updateLocation(Long id, @Valid UpdateLocationRequest request) {
        log.info("Updating location: {}", id);

        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lokalizacja nie znaleziona"));

        // Sprawdź czy zmiana typu nie powoduje konfliktów
        validateTypeChange(location, request.getType());

        updateLocationEntity(location, request);

        // Ponowne geokodowanie jeśli zmieniono adres
        if (location.getType() == LocationType.PHYSICAL && needsGeocoding(location)) {
            performGeocoding(location);
        }

        Location updated = locationRepository.save(location);
        log.info("Location updated successfully: {} (ID: {})", updated.getName(), updated.getId());

        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteLocation(Long id) {
        log.info("Deleting location: {}", id);

        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lokalizacja nie znaleziona"));

        validateLocationDeletion(location);

        locationRepository.delete(location);
        log.info("Location deleted successfully: {}", id);
    }

    @Override
    public LocationResponse getLocation(Long id) {
        return getLocationById(id);
    }

    @Override
    public LocationListResponse searchLocations(LocationSearchRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage() != null ? request.getPage() : 0,
                request.getSize() != null ? request.getSize() : 20
        );

        String typeString = request.getType() != null ? request.getType().name() : null;

        Page<Location> locationsPage = locationRepository.searchLocations(
                request.getQuery(), typeString, request.getCity(), pageable);

        List<LocationResponse> locations = locationsPage.getContent()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return LocationListResponse.builder()
                .locations(locations)
                .currentPage(locationsPage.getNumber())
                .totalPages(locationsPage.getTotalPages())
                .totalItems(locationsPage.getTotalElements())
                .hasNext(locationsPage.hasNext())
                .hasPrevious(locationsPage.hasPrevious())
                .build();
    }

    @Override
    public List<LocationResponse> findNearbyLocations(BigDecimal lat, BigDecimal lng, Double radiusKm) {
        validateCoordinates(lat, lng);

        Double searchRadius = validateAndLimitRadius(radiusKm);

        log.info("Searching locations near ({}, {}) within {} km", lat, lng, searchRadius);

        try {
            List<Location> locations = locationRepository.findNearbyLocations(lat, lng, searchRadius);
            log.info("Found {} nearby locations", locations.size());

            return locations.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error searching nearby locations: {}", e.getMessage(), e);
            throw new RuntimeException("Błąd podczas wyszukiwania lokalizacji w pobliżu: " + e.getMessage());
        }
    }

    @Override
    public String generateMapUrl(Long locationId) {
        return locationRepository.findById(locationId)
                .map(location -> {
                    if (location.hasCoordinates()) {
                        return String.format("https://maps.google.com/maps?q=%s,%s",
                                location.getLatitude(), location.getLongitude());
                    } else if (location.getFullAddress() != null && !location.getFullAddress().isEmpty()) {
                        return String.format("https://maps.google.com/maps?q=%s",
                                encodeUrl(location.getFullAddress()));
                    }
                    return null;
                })
                .orElse(null);
    }

    @Override
    public String generateDirectionsUrl(Long locationId, String origin) {
        String finalOrigin = origin != null ? origin.trim() : "";

        return locationRepository.findById(locationId)
                .map(location -> {
                    String destination = getDestinationFromLocation(location);
                    if (destination == null) {
                        return null;
                    }

                    return String.format("https://maps.google.com/maps?saddr=%s&daddr=%s",
                            encodeUrl(finalOrigin), destination);
                })
                .orElse(null);
    }

    @Override
    public LocationResponse generateVirtualLocation(String platform, String meetingId, String passcode) {
        validateVirtualLocationParameters(platform, meetingId);

        String virtualMeetingUrl = generatePlatformUrl(platform, meetingId, passcode);
        String locationName = generateLocationNameFromPlatform(platform);

        Location location = Location.builder()
                .name(locationName)
                .type(LocationType.VIRTUAL)
                .virtualMeetingUrl(virtualMeetingUrl)
                .accessCode(passcode)
                .timezone("Europe/Warsaw")
                .build();

        Location saved = locationRepository.save(location);
        log.info("Virtual location generated: {} (ID: {})", saved.getName(), saved.getId());

        return mapToResponse(saved);
    }

    @Override
    public LocationResponse getLocationById(Long id) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lokalizacja nie znaleziona"));
        return mapToResponse(location);
    }

    @Override
    public boolean validateLocation(Long locationId) {
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new IllegalArgumentException("Lokalizacja nie znaleziona"));

        return switch (location.getType()) {
            case VIRTUAL -> isValidVirtualLocation(location);
            case PHYSICAL -> isValidPhysicalLocation(location);
        };
    }

    // METODY POMOCNICZE - PRYWATNE

    private void validateCreateRequest(CreateLocationRequest request) {
        // Walidacja unikalności dla lokalizacji wirtualnej
        if (request.getType() == LocationType.VIRTUAL &&
                request.getVirtualMeetingUrl() != null) {
            locationRepository.findByVirtualMeetingUrl(request.getVirtualMeetingUrl())
                    .ifPresent(location -> {
                        throw new IllegalArgumentException("URL spotkania już istnieje");
                    });
        }

        // Walidacja unikalności dla lokalizacji fizycznej
        if (request.getType() == LocationType.PHYSICAL &&
                request.getName() != null && request.getAddress() != null) {
            if (locationRepository.existsByNameAndAddress(request.getName(), request.getAddress())) {
                throw new IllegalArgumentException("Lokalizacja o tej nazwie i adresie już istnieje");
            }
        }

        // Walidacja współrzędnych
        validateCoordinates(request.getLatitude(), request.getLongitude());
    }

    private void validateTypeChange(Location location, LocationType newType) {
        if (location.getType() != newType && !location.getMeetings().isEmpty()) {
            throw new IllegalStateException("Nie można zmienić typu lokalizacji używanej w spotkaniach");
        }
    }

    private void updateLocationEntity(Location location, UpdateLocationRequest request) {
        location.setName(request.getName());
        location.setType(request.getType());
        location.setAddress(request.getAddress());
        location.setCity(request.getCity());
        location.setCountry(request.getCountry());
        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        location.setVirtualMeetingUrl(request.getVirtualMeetingUrl());
        location.setAccessCode(request.getAccessCode());
        location.setDrivingInstructions(request.getDrivingInstructions());
        location.setTimezone(request.getTimezone());
    }

    private void validateLocationDeletion(Location location) {
        if (!location.getMeetings().isEmpty()) {
            throw new IllegalStateException(
                    "Nie można usunąć lokalizacji używanej w spotkaniach. " +
                            "Lokalizacja jest używana w " + location.getMeetings().size() + " spotkaniach."
            );
        }
    }

    private void validateCoordinates(BigDecimal lat, BigDecimal lng) {
        if (lat == null || lng == null) {
            return; // Współrzędne są opcjonalne
        }

        if (lat.compareTo(new BigDecimal("-90")) < 0 || lat.compareTo(new BigDecimal("90")) > 0) {
            throw new IllegalArgumentException("Szerokość geograficzna musi być między -90 a 90");
        }
        if (lng.compareTo(new BigDecimal("-180")) < 0 || lng.compareTo(new BigDecimal("180")) > 0) {
            throw new IllegalArgumentException("Długość geograficzna musi być między -180 a 180");
        }
    }

    private Double validateAndLimitRadius(Double radiusKm) {
        if (radiusKm == null) {
            return 10.0;
        }

        if (radiusKm <= 0) {
            throw new IllegalArgumentException("Promień musi być większy niż 0");
        }

        if (radiusKm > 1000) {
            log.warn("Promień wyszukiwania ograniczony do 1000 km");
            return 1000.0;
        }

        return radiusKm;
    }

    private void performGeocoding(Location location) {
        try {
            String fullAddress = buildFullAddressForGeocoding(location);
            Location geocoded = geocodingService.geocodeAddress(fullAddress);

            if (geocoded != null && geocoded.getLatitude() != null && geocoded.getLongitude() != null) {
                location.setLatitude(geocoded.getLatitude());
                location.setLongitude(geocoded.getLongitude());

                // Uzupełnij brakujące dane tylko jeśli nie zostały podane
                if (location.getCity() == null && geocoded.getCity() != null) {
                    location.setCity(geocoded.getCity());
                }
                if (location.getCountry() == null && geocoded.getCountry() != null) {
                    location.setCountry(geocoded.getCountry());
                }
                if (location.getTimezone() == null && geocoded.getTimezone() != null) {
                    location.setTimezone(geocoded.getTimezone());
                }

                log.info("Successfully geocoded address: {} -> {}, {}",
                        fullAddress, location.getLatitude(), location.getLongitude());
            }
        } catch (Exception e) {
            log.warn("Geocoding failed for address: {}. Error: {}",
                    location.getAddress(), e.getMessage());
        }
    }

    private String getDestinationFromLocation(Location location) {
        if (location.hasCoordinates()) {
            return location.getLatitude() + "," + location.getLongitude();
        } else if (location.getFullAddress() != null && !location.getFullAddress().isEmpty()) {
            return encodeUrl(location.getFullAddress());
        }
        return null;
    }

    private void validateVirtualLocationParameters(String platform, String meetingId) {
        if (platform == null || platform.trim().isEmpty()) {
            throw new IllegalArgumentException("Platforma jest wymagana");
        }
        if (meetingId == null || meetingId.trim().isEmpty()) {
            throw new IllegalArgumentException("ID spotkania jest wymagane");
        }
    }

    private String generateLocationNameFromPlatform(String platform) {
        String cleanPlatform = platform.trim();
        if (cleanPlatform.isEmpty()) {
            return "Virtual Meeting";
        }

        return cleanPlatform.substring(0, 1).toUpperCase() +
                cleanPlatform.substring(1).toLowerCase() + " Meeting";
    }

    private boolean isValidVirtualLocation(Location location) {
        return location.getVirtualMeetingUrl() != null &&
                !location.getVirtualMeetingUrl().trim().isEmpty();
    }

    private boolean isValidPhysicalLocation(Location location) {
        return location.hasCoordinates() ||
                (location.getAddress() != null && !location.getAddress().trim().isEmpty());
    }

    private Location mapToEntity(CreateLocationRequest request) {
        return Location.builder()
                .name(request.getName().trim())
                .type(request.getType())
                .address(request.getAddress() != null ? request.getAddress().trim() : null)
                .city(request.getCity() != null ? request.getCity().trim() : null)
                .country(request.getCountry() != null ? request.getCountry().trim() : null)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .virtualMeetingUrl(request.getVirtualMeetingUrl())
                .accessCode(request.getAccessCode())
                .drivingInstructions(request.getDrivingInstructions())
                .timezone(request.getTimezone())
                .build();
    }

    private LocationResponse mapToResponse(Location location) {
        String mapUrl = generateMapUrl(location.getId());
        String directionsUrl = generateDirectionsUrl(location.getId(), "");

        return LocationResponse.builder()
                .id(location.getId())
                .name(location.getName())
                .type(location.getType())
                .address(location.getAddress())
                .city(location.getCity())
                .country(location.getCountry())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .virtualMeetingUrl(location.getVirtualMeetingUrl())
                .accessCode(location.getAccessCode())
                .drivingInstructions(location.getDrivingInstructions())
                .timezone(location.getTimezone())
                .mapUrl(mapUrl)
                .directionsUrl(directionsUrl)
                .build();
    }

    private boolean needsGeocoding(Location location) {
        return location.getType() == LocationType.PHYSICAL &&
                (location.getLatitude() == null || location.getLongitude() == null) &&
                location.getAddress() != null && !location.getAddress().trim().isEmpty();
    }

    private String buildFullAddressForGeocoding(Location location) {
        StringBuilder address = new StringBuilder();

        if (location.getAddress() != null) {
            address.append(location.getAddress());
        }
        if (location.getCity() != null) {
            if (address.length() > 0) address.append(", ");
            address.append(location.getCity());
        }
        if (location.getCountry() != null) {
            if (address.length() > 0) address.append(", ");
            address.append(location.getCountry());
        }

        return address.toString();
    }

    private String generatePlatformUrl(String platform, String meetingId, String passcode) {
        String cleanPlatform = platform.toUpperCase().trim();
        String cleanMeetingId = meetingId.trim();
        String cleanPasscode = passcode != null ? passcode.trim() : "";

        return switch (cleanPlatform) {
            case "ZOOM" -> {
                if (!cleanPasscode.isEmpty()) {
                    yield String.format("https://zoom.us/j/%s?pwd=%s", cleanMeetingId, cleanPasscode);
                } else {
                    yield String.format("https://zoom.us/j/%s", cleanMeetingId);
                }
            }
            case "TEAMS" -> String.format("https://teams.microsoft.com/l/meetup-join/%s", cleanMeetingId);
            case "GOOGLE_MEET", "MEET" -> String.format("https://meet.google.com/%s", cleanMeetingId);
            case "WEBEX" -> String.format("https://meet.webex.com/%s", cleanMeetingId);
            default -> {
                log.warn("Unknown platform: {}, using meeting ID as URL", platform);
                yield cleanMeetingId;
            }
        };
    }

    private String encodeUrl(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        return text.trim().replace(" ", "+");
    }
}
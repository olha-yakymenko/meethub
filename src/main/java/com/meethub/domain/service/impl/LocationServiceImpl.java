
// LocationServiceImpl.java
package com.meethub.domain.service.impl;


import com.meethub.domain.model.entity.Location;
import com.meethub.domain.model.enums.LocationType;
import com.meethub.domain.model.mapper.LocationMapper;
import com.meethub.domain.model.projection.LocationBasicInfo;
import com.meethub.domain.model.request.CreateLocationRequest;
import com.meethub.domain.model.request.LocationSearchRequest;
import com.meethub.domain.model.request.UpdateLocationRequest;
import com.meethub.domain.model.response.LocationListResponse;
import com.meethub.domain.model.response.LocationResponse;
import com.meethub.domain.repository.jpa.LocationRepository;
import com.meethub.domain.service.GeocodingService;
import com.meethub.domain.service.LocationService;
import com.meethub.exception.BusinessException;
import com.meethub.exception.ResourceNotFoundException;
import com.meethub.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;
    private final GeocodingService geocodingService;
    private final LocationMapper locationMapper;

    @Override
    @Transactional
    public LocationResponse createLocation(CreateLocationRequest request) {
        log.info("Creating location: {}", request.getName());

        try {
            validateCreateRequest(request);

            Location location = locationMapper.toEntity(request);

            // Geocoding for physical locations
            if (location.getType() == LocationType.PHYSICAL && needsGeocoding(location)) {
                performGeocoding(location);
            }

            Location saved = locationRepository.save(location);
            log.info("Location created successfully: {} (ID: {})", saved.getName(), saved.getId());

            return locationMapper.toResponse(saved);

        } catch (ValidationException | BusinessException e) {
            throw e; // Re-throw business exceptions
        } catch (Exception e) {
            log.error("Error creating location: {}", e.getMessage(), e);
            throw new BusinessException("Nie udało się utworzyć lokalizacji: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public LocationResponse updateLocation(Long id, UpdateLocationRequest request) {
        log.info("Updating location ID: {}", id);

        try {
            Location location = locationRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Lokalizacja"));

            validateTypeChange(location, request.getType());

            locationMapper.updateEntity(location, request);

            // Re-geocode if address changed
            if (location.getType() == LocationType.PHYSICAL && needsGeocoding(location)) {
                performGeocoding(location);
            }

            Location updated = locationRepository.save(location);
            log.info("Location updated successfully: {} (ID: {})", updated.getName(), updated.getId());

            return locationMapper.toResponse(updated);

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating location ID {}: {}", id, e.getMessage(), e);
            throw new BusinessException("Nie udało się zaktualizować lokalizacji: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteLocation(Long id) {
        log.info("Deleting location ID: {}", id);

        try {
            Location location = locationRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Lokalizacja"));

            validateLocationDeletion(location);

            locationRepository.delete(location);
            log.info("Location deleted successfully: {}", id);

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deleting location ID {}: {}", id, e.getMessage(), e);
            throw new BusinessException("Nie udało się usunąć lokalizacji: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public LocationResponse getLocation(Long id) {
        log.info("Getting location ID: {}", id);

        try {
            Location location = locationRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Lokalizacja"));

            return locationMapper.toResponse(location);

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting location ID {}: {}", id, e.getMessage(), e);
            throw new BusinessException("Nie udało się pobrać lokalizacji: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public LocationListResponse searchLocations(LocationSearchRequest request) {
        log.info("Searching locations with query: {}, type: {}, city: {}",
                request.getQuery(), request.getType(), request.getCity());

        try {
            Pageable pageable = PageRequest.of(
                    request.getPage() != null ? request.getPage() : 0,
                    request.getSize() != null ? request.getSize() : 20
            );

            String typeString = request.getType() != null ? request.getType().name() : null;

            Page<Location> locationsPage = locationRepository.searchLocations(
                    request.getQuery(), typeString, request.getCity(), pageable);

            List<LocationResponse> locations = locationsPage.getContent()
                    .stream()
                    .map(locationMapper::toResponse)
                    .collect(Collectors.toList());

            return LocationListResponse.builder()
                    .locations(locations)
                    .currentPage(locationsPage.getNumber())
                    .totalPages(locationsPage.getTotalPages())
                    .totalItems(locationsPage.getTotalElements())
                    .hasNext(locationsPage.hasNext())
                    .hasPrevious(locationsPage.hasPrevious())
                    .build();

        } catch (Exception e) {
            log.error("Error searching locations: {}", e.getMessage(), e);
            throw new BusinessException("Nie udało się wyszukać lokalizacji: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationResponse> getAllLocations() {
        log.info("Getting all locations");

        try {
            List<Location> locations = locationRepository.findAll();

            return locations.stream()
                    .map(locationMapper::toResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting all locations: {}", e.getMessage(), e);
            throw new BusinessException("Nie udało się pobrać lokalizacji: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationResponse> getAvailableLocations() {
        log.info("Getting available locations");

        try {
            List<Location> locations = locationRepository.findAll();

            return locations.stream()
                    .map(locationMapper::toResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting available locations: {}", e.getMessage(), e);
            throw new BusinessException("Nie udało się pobrać dostępnych lokalizacji: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationResponse> getPhysicalLocations() {
        log.info("Getting physical locations");

        try {
            List<Location> locations = locationRepository.findAll();

            return locations.stream()
                    .map(locationMapper::toResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting physical locations: {}", e.getMessage(), e);
            throw new BusinessException("Nie udało się pobrać fizycznych lokalizacji: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationResponse> getVirtualLocations() {
        log.info("Getting virtual locations");

        try {
            List<Location> locations = locationRepository.findAll();

            return locations.stream()
                    .map(locationMapper::toResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting virtual locations: {}", e.getMessage(), e);
            throw new BusinessException("Nie udało się pobrać wirtualnych lokalizacji: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationResponse> findNearbyLocations(BigDecimal lat, BigDecimal lng, Double radiusKm) {
        log.info("Searching nearby locations at ({}, {}) within {} km", lat, lng, radiusKm);

        try {
            validateCoordinates(lat, lng);
            Double searchRadius = validateAndLimitRadius(radiusKm);

            List<Location> locations = locationRepository.findNearbyLocations(lat, lng, searchRadius);

            return locations.stream()
                    .map(locationMapper::toResponse)
                    .collect(Collectors.toList());

        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error finding nearby locations: {}", e.getMessage(), e);
            throw new BusinessException("Nie udało się znaleźć lokalizacji w pobliżu: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String generateMapUrl(Long locationId) {
        try {
            Location location = locationRepository.findById(locationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Lokalizacja"));

            if (location.hasCoordinates()) {
                return String.format("https://maps.google.com/maps?q=%s,%s",
                        location.getLatitude(), location.getLongitude());
            } else if (location.getFullAddress() != null && !location.getFullAddress().isEmpty()) {
                return String.format("https://maps.google.com/maps?q=%s",
                        encodeUrl(location.getFullAddress()));
            }
            return null;

        } catch (Exception e) {
            log.error("Error generating map URL for location ID {}: {}", locationId, e.getMessage(), e);
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String generateDirectionsUrl(Long locationId, String origin) {
        try {
            Location location = locationRepository.findById(locationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Lokalizacja"));

            String destination = getDestinationFromLocation(location);
            if (destination == null) {
                return null;
            }

            String finalOrigin = origin != null ? origin.trim() : "";
            return String.format("https://maps.google.com/maps?saddr=%s&daddr=%s",
                    encodeUrl(finalOrigin), destination);

        } catch (Exception e) {
            log.error("Error generating directions URL for location ID {}: {}", locationId, e.getMessage(), e);
            return null;
        }
    }

    @Override
    @Transactional
    public LocationResponse generateVirtualLocation(String platform, String meetingId, String passcode) {
        log.info("Generating virtual location for platform: {}, meetingId: {}", platform, meetingId);

        try {
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

            return locationMapper.toResponse(saved);

        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error generating virtual location: {}", e.getMessage(), e);
            throw new BusinessException("Nie udało się wygenerować lokalizacji wirtualnej: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Location getLocationEntity(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lokalizacja"));
    }

    @Override
    @Transactional
    public LocationResponse saveLocationEntity(Location location) {
        Location saved = locationRepository.save(location);
        return locationMapper.toResponse(saved);
    }

    // ============ PRIVATE HELPER METHODS ============

    private void validateCreateRequest(CreateLocationRequest request) {
        if (request.getType() == LocationType.VIRTUAL && request.getVirtualMeetingUrl() != null) {
            locationRepository.findByVirtualMeetingUrl(request.getVirtualMeetingUrl())
                    .ifPresent(location -> {
                        throw new ValidationException("URL spotkania już istnieje");
                    });
        }

        if (request.getType() == LocationType.PHYSICAL &&
                request.getName() != null && request.getAddress() != null) {
            if (locationRepository.existsByNameAndAddress(request.getName(), request.getAddress())) {
                throw new ValidationException("Lokalizacja o tej nazwie i adresie już istnieje");
            }
        }

        validateCoordinates(request.getLatitude(), request.getLongitude());
    }

    private void validateTypeChange(Location location, LocationType newType) {
        if (location.getType() != newType && !location.getMeetings().isEmpty()) {
            throw new BusinessException("Nie można zmienić typu lokalizacji używanej w spotkaniach");
        }
    }

    private void validateLocationDeletion(Location location) {
        if (!location.getMeetings().isEmpty()) {
            throw new BusinessException(
                    "Nie można usunąć lokalizacji używanej w spotkaniach. " +
                            "Lokalizacja jest używana w " + location.getMeetings().size() + " spotkaniach."
            );
        }
    }

    private void validateCoordinates(BigDecimal lat, BigDecimal lng) {
        if (lat == null || lng == null) {
            return;
        }

        if (lat.compareTo(new BigDecimal("-90")) < 0 || lat.compareTo(new BigDecimal("90")) > 0) {
            throw new ValidationException("Szerokość geograficzna musi być między -90 a 90");
        }
        if (lng.compareTo(new BigDecimal("-180")) < 0 || lng.compareTo(new BigDecimal("180")) > 0) {
            throw new ValidationException("Długość geograficzna musi być między -180 a 180");
        }
    }

    private Double validateAndLimitRadius(Double radiusKm) {
        if (radiusKm == null) {
            return 10.0;
        }

        if (radiusKm <= 0) {
            throw new ValidationException("Promień musi być większy niż 0");
        }

        if (radiusKm > 1000) {
            log.warn("Promień wyszukiwania ograniczony do 1000 km");
            return 1000.0;
        }

        return radiusKm;
    }

    private void validateVirtualLocationParameters(String platform, String meetingId) {
        if (platform == null || platform.trim().isEmpty()) {
            throw new ValidationException("Platforma jest wymagana");
        }
        if (meetingId == null || meetingId.trim().isEmpty()) {
            throw new ValidationException("ID spotkania jest wymagane");
        }
    }

    private void performGeocoding(Location location) {
        try {
            String fullAddress = location.getFullAddress();
            if (fullAddress == null || fullAddress.trim().isEmpty()) {
                return;
            }

            Location geocoded = geocodingService.geocodeAddress(fullAddress);

            if (geocoded != null && geocoded.getLatitude() != null && geocoded.getLongitude() != null) {
                location.setLatitude(geocoded.getLatitude());
                location.setLongitude(geocoded.getLongitude());

                // Uzupełnij tylko jeśli nie zostały podane
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

    private boolean needsGeocoding(Location location) {
        return location.getType() == LocationType.PHYSICAL &&
                (location.getLatitude() == null || location.getLongitude() == null) &&
                location.getAddress() != null && !location.getAddress().trim().isEmpty();
    }

    private String getDestinationFromLocation(Location location) {
        if (location.hasCoordinates()) {
            return location.getLatitude() + "," + location.getLongitude();
        } else if (location.getFullAddress() != null && !location.getFullAddress().isEmpty()) {
            return encodeUrl(location.getFullAddress());
        }
        return null;
    }

    private String generateLocationNameFromPlatform(String platform) {
        String cleanPlatform = platform.trim();
        if (cleanPlatform.isEmpty()) {
            return "Virtual Meeting";
        }

        return cleanPlatform.substring(0, 1).toUpperCase() +
                cleanPlatform.substring(1).toLowerCase() + " Meeting";
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


    @Override
    @Transactional(readOnly = true)
    public List<LocationBasicInfo> getLocationsForSelect() {
        log.info("Getting locations for select dropdown");
        return locationRepository.findAllForSelect();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationBasicInfo> getAllLocationsBasic() {
        log.info("Getting all locations (basic info)");
        return locationRepository.findAllBasicInfo();
    }

    @Override
    public void validateLocationExists(Long locationId) {

        if (locationId == null) {
            return; // brak lokalizacji = OK
        }

        boolean exists = locationRepository.existsById(locationId);

        if (!exists) {
            throw new IllegalArgumentException("Wybrana lokalizacja nie istnieje");
        }
    }
}

//
//
//
//
//
//
//
//
// package com.meethub.domain.service.impl;
//
//import com.meethub.domain.model.entity.Location;
//import com.meethub.domain.model.enums.LocationType;
//import com.meethub.domain.model.mapper.LocationMapper;
//import com.meethub.domain.model.projection.LocationBasicInfo;
//import com.meethub.domain.model.request.CreateLocationRequest;
//import com.meethub.domain.model.request.LocationSearchRequest;
//import com.meethub.domain.model.request.UpdateLocationRequest;
//import com.meethub.domain.model.response.LocationListResponse;
//import com.meethub.domain.model.response.LocationResponse;
//import com.meethub.domain.repository.jpa.LocationRepository;
//import com.meethub.domain.service.GeocodingService;
//import com.meethub.domain.service.LocationService;
//import com.meethub.exception.BusinessException;
//import com.meethub.exception.ResourceNotFoundException;
//import com.meethub.exception.ValidationException;
//import jakarta.validation.Valid;
//import jakarta.validation.constraints.*;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//
//import java.math.BigDecimal;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class LocationServiceImpl implements LocationService {
//
//    private final LocationRepository locationRepository;
//    private final GeocodingService geocodingService;
//    private final LocationMapper locationMapper;
//
//    @Override
//    @Transactional
//    public LocationResponse createLocation(
//            @Valid @NotNull(message = "Żądanie utworzenia lokalizacji nie może być puste")
//            CreateLocationRequest request) {
//
//        log.info("Creating location: {}", request.getName());
//
//        try {
//            validateCreateRequest(request);
//
//            Location location = locationMapper.toEntity(request);
//
//            // Geocoding for physical locations
//            if (location.getType() == LocationType.PHYSICAL && needsGeocoding(location)) {
//                performGeocoding(location);
//            }
//
//            Location saved = locationRepository.save(location);
//            log.info("Location created successfully: {} (ID: {})", saved.getName(), saved.getId());
//
//            return locationMapper.toResponse(saved);
//
//        } catch (ValidationException | BusinessException e) {
//            throw e; // Re-throw business exceptions
//        } catch (Exception e) {
//            log.error("Error creating location: {}", e.getMessage(), e);
//            throw new BusinessException("Nie udało się utworzyć lokalizacji: " + e.getMessage());
//        }
//    }
//
//    @Override
//    @Transactional
//    public LocationResponse updateLocation(
//            @NotNull(message = "Identyfikator lokalizacji nie może być pusty")
//            @Positive(message = "Identyfikator lokalizacji musi być liczbą dodatnią")
//            Long id,
//
//            @Valid @NotNull(message = "Żądanie aktualizacji lokalizacji nie może być puste")
//            UpdateLocationRequest request) {
//
//        log.info("Updating location ID: {}", id);
//
//        try {
//            Location location = locationRepository.findById(id)
//                    .orElseThrow(() -> new ResourceNotFoundException("Lokalizacja"));
//
//            validateTypeChange(location, request.getType());
//
//            locationMapper.updateEntity(location, request);
//
//            // Re-geocode if address changed
//            if (location.getType() == LocationType.PHYSICAL && needsGeocoding(location)) {
//                performGeocoding(location);
//            }
//
//            Location updated = locationRepository.save(location);
//            log.info("Location updated successfully: {} (ID: {})", updated.getName(), updated.getId());
//
//            return locationMapper.toResponse(updated);
//
//        } catch (ResourceNotFoundException e) {
//            throw e;
//        } catch (Exception e) {
//            log.error("Error updating location ID {}: {}", id, e.getMessage(), e);
//            throw new BusinessException("Nie udało się zaktualizować lokalizacji: " + e.getMessage());
//        }
//    }
//
//    @Override
//    @Transactional
//    public void deleteLocation(
//            @NotNull(message = "Identyfikator lokalizacji nie może być pusty")
//            @Positive(message = "Identyfikator lokalizacji musi być liczbą dodatnią")
//            Long id) {
//
//        log.info("Deleting location ID: {}", id);
//
//        try {
//            Location location = locationRepository.findById(id)
//                    .orElseThrow(() -> new ResourceNotFoundException("Lokalizacja"));
//
//            validateLocationDeletion(location);
//
//            locationRepository.delete(location);
//            log.info("Location deleted successfully: {}", id);
//
//        } catch (ResourceNotFoundException e) {
//            throw e;
//        } catch (Exception e) {
//            log.error("Error deleting location ID {}: {}", id, e.getMessage(), e);
//            throw new BusinessException("Nie udało się usunąć lokalizacji: " + e.getMessage());
//        }
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public LocationResponse getLocation(
//            @NotNull(message = "Identyfikator lokalizacji nie może być pusty")
//            @Positive(message = "Identyfikator lokalizacji musi być liczbą dodatnią")
//            Long id) {
//
//        log.info("Getting location ID: {}", id);
//
//        try {
//            Location location = locationRepository.findById(id)
//                    .orElseThrow(() -> new ResourceNotFoundException("Lokalizacja"));
//
//            return locationMapper.toResponse(location);
//
//        } catch (ResourceNotFoundException e) {
//            throw e;
//        } catch (Exception e) {
//            log.error("Error getting location ID {}: {}", id, e.getMessage(), e);
//            throw new BusinessException("Nie udało się pobrać lokalizacji: " + e.getMessage());
//        }
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public LocationListResponse searchLocations(
//            @Valid @NotNull(message = "Żądanie wyszukiwania lokalizacji nie może być puste")
//            LocationSearchRequest request) {
//
//        log.info("Searching locations with query: {}, type: {}, city: {}",
//                request.getQuery(), request.getType(), request.getCity());
//
//        try {
//            Pageable pageable = PageRequest.of(
//                    request.getPage() != null ? request.getPage() : 0,
//                    request.getSize() != null ? request.getSize() : 20
//            );
//
//            String typeString = request.getType() != null ? request.getType().name() : null;
//
//            Page<Location> locationsPage = locationRepository.searchLocations(
//                    request.getQuery(), typeString, request.getCity(), pageable);
//
//            List<LocationResponse> locations = locationsPage.getContent()
//                    .stream()
//                    .map(locationMapper::toResponse)
//                    .collect(Collectors.toList());
//
//            return LocationListResponse.builder()
//                    .locations(locations)
//                    .currentPage(locationsPage.getNumber())
//                    .totalPages(locationsPage.getTotalPages())
//                    .totalItems(locationsPage.getTotalElements())
//                    .hasNext(locationsPage.hasNext())
//                    .hasPrevious(locationsPage.hasPrevious())
//                    .build();
//
//        } catch (Exception e) {
//            log.error("Error searching locations: {}", e.getMessage(), e);
//            throw new BusinessException("Nie udało się wyszukać lokalizacji: " + e.getMessage());
//        }
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<LocationResponse> getAllLocations() {
//        log.info("Getting all locations");
//
//        try {
//            List<Location> locations = locationRepository.findAll();
//
//            return locations.stream()
//                    .map(locationMapper::toResponse)
//                    .collect(Collectors.toList());
//
//        } catch (Exception e) {
//            log.error("Error getting all locations: {}", e.getMessage(), e);
//            throw new BusinessException("Nie udało się pobrać lokalizacji: " + e.getMessage());
//        }
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<LocationResponse> getAvailableLocations() {
//        log.info("Getting available locations");
//
//        try {
//            List<Location> locations = locationRepository.findAll();
//
//            return locations.stream()
//                    .map(locationMapper::toResponse)
//                    .collect(Collectors.toList());
//
//        } catch (Exception e) {
//            log.error("Error getting available locations: {}", e.getMessage(), e);
//            throw new BusinessException("Nie udało się pobrać dostępnych lokalizacji: " + e.getMessage());
//        }
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<LocationResponse> getPhysicalLocations() {
//        log.info("Getting physical locations");
//
//        try {
//            List<Location> locations = locationRepository.findAll();
//
//            return locations.stream()
//                    .map(locationMapper::toResponse)
//                    .collect(Collectors.toList());
//
//        } catch (Exception e) {
//            log.error("Error getting physical locations: {}", e.getMessage(), e);
//            throw new BusinessException("Nie udało się pobrać fizycznych lokalizacji: " + e.getMessage());
//        }
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<LocationResponse> getVirtualLocations() {
//        log.info("Getting virtual locations");
//
//        try {
//            List<Location> locations = locationRepository.findAll();
//
//            return locations.stream()
//                    .map(locationMapper::toResponse)
//                    .collect(Collectors.toList());
//
//        } catch (Exception e) {
//            log.error("Error getting virtual locations: {}", e.getMessage(), e);
//            throw new BusinessException("Nie udało się pobrać wirtualnych lokalizacji: " + e.getMessage());
//        }
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<LocationResponse> findNearbyLocations(
//            @NotNull(message = "Szerokość geograficzna nie może być pusta")
//            @DecimalMin(value = "-90.0", message = "Szerokość geograficzna musi być większa lub równa -90")
//            @DecimalMax(value = "90.0", message = "Szerokość geograficzna musi być mniejsza lub równa 90")
//            BigDecimal lat,
//
//            @NotNull(message = "Długość geograficzna nie może być pusta")
//            @DecimalMin(value = "-180.0", message = "Długość geograficzna musi być większa lub równa -180")
//            @DecimalMax(value = "180.0", message = "Długość geograficzna musi być mniejsza lub równa 180")
//            BigDecimal lng,
//
//            @DecimalMin(value = "0.1", message = "Promień wyszukiwania musi być co najmniej 0.1 km")
//            @DecimalMax(value = "1000.0", message = "Promień wyszukiwania nie może przekraczać 1000 km")
//            Double radiusKm) {
//
//        log.info("Searching nearby locations at ({}, {}) within {} km", lat, lng, radiusKm);
//
//        try {
//            validateCoordinates(lat, lng);
//            Double searchRadius = validateAndLimitRadius(radiusKm);
//
//            List<Location> locations = locationRepository.findNearbyLocations(lat, lng, searchRadius);
//
//            return locations.stream()
//                    .map(locationMapper::toResponse)
//                    .collect(Collectors.toList());
//
//        } catch (ValidationException e) {
//            throw e;
//        } catch (Exception e) {
//            log.error("Error finding nearby locations: {}", e.getMessage(), e);
//            throw new BusinessException("Nie udało się znaleźć lokalizacji w pobliżu: " + e.getMessage());
//        }
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public String generateMapUrl(
//            @NotNull(message = "Identyfikator lokalizacji nie może być pusty")
//            @Positive(message = "Identyfikator lokalizacji musi być liczbą dodatnią")
//            Long locationId) {
//
//        try {
//            Location location = locationRepository.findById(locationId)
//                    .orElseThrow(() -> new ResourceNotFoundException("Lokalizacja"));
//
//            if (location.hasCoordinates()) {
//                return String.format("https://maps.google.com/maps?q=%s,%s",
//                        location.getLatitude(), location.getLongitude());
//            } else if (location.getFullAddress() != null && !location.getFullAddress().isEmpty()) {
//                return String.format("https://maps.google.com/maps?q=%s",
//                        encodeUrl(location.getFullAddress()));
//            }
//            return null;
//
//        } catch (Exception e) {
//            log.error("Error generating map URL for location ID {}: {}", locationId, e.getMessage(), e);
//            return null;
//        }
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public String generateDirectionsUrl(
//            @NotNull(message = "Identyfikator lokalizacji nie może być pusty")
//            @Positive(message = "Identyfikator lokalizacji musi być liczbą dodatnią")
//            Long locationId,
//
//            @Size(max = 500, message = "Miejsce początkowe nie może przekraczać 500 znaków")
//            String origin) {
//
//        try {
//            Location location = locationRepository.findById(locationId)
//                    .orElseThrow(() -> new ResourceNotFoundException("Lokalizacja"));
//
//            String destination = getDestinationFromLocation(location);
//            if (destination == null) {
//                return null;
//            }
//
//            String finalOrigin = origin != null ? origin.trim() : "";
//            return String.format("https://maps.google.com/maps?saddr=%s&daddr=%s",
//                    encodeUrl(finalOrigin), destination);
//
//        } catch (Exception e) {
//            log.error("Error generating directions URL for location ID {}: {}", locationId, e.getMessage(), e);
//            return null;
//        }
//    }
//
//    @Override
//    @Transactional
//    public LocationResponse generateVirtualLocation(
//            @NotBlank(message = "Platforma nie może być pusta")
//            @Pattern(regexp = "^(ZOOM|TEAMS|GOOGLE_MEET|MEET|WEBEX|OTHER)$",
//                    message = "Nieobsługiwana platforma. Dozwolone wartości: ZOOM, TEAMS, GOOGLE_MEET, MEET, WEBEX, OTHER")
//            String platform,
//
//            @NotBlank(message = "ID spotkania nie może być puste")
//            @Size(max = 100, message = "ID spotkania nie może przekraczać 100 znaków")
//            @Pattern(regexp = "^[a-zA-Z0-9\\-_]+$",
//                    message = "ID spotkania może zawierać tylko litery, cyfry, myślniki i podkreślniki")
//            String meetingId,
//
//            @Size(max = 50, message = "Kod dostępu nie może przekraczać 50 znaków")
//            String passcode) {
//
//        log.info("Generating virtual location for platform: {}, meetingId: {}", platform, meetingId);
//
//        try {
//            validateVirtualLocationParameters(platform, meetingId);
//
//            String virtualMeetingUrl = generatePlatformUrl(platform, meetingId, passcode);
//            String locationName = generateLocationNameFromPlatform(platform);
//
//            Location location = Location.builder()
//                    .name(locationName)
//                    .type(LocationType.VIRTUAL)
//                    .virtualMeetingUrl(virtualMeetingUrl)
//                    .accessCode(passcode)
//                    .timezone("Europe/Warsaw")
//                    .build();
//
//            Location saved = locationRepository.save(location);
//            log.info("Virtual location generated: {} (ID: {})", saved.getName(), saved.getId());
//
//            return locationMapper.toResponse(saved);
//
//        } catch (ValidationException e) {
//            throw e;
//        } catch (Exception e) {
//            log.error("Error generating virtual location: {}", e.getMessage(), e);
//            throw new BusinessException("Nie udało się wygenerować lokalizacji wirtualnej: " + e.getMessage());
//        }
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public Location getLocationEntity(
//            @NotNull(message = "Identyfikator lokalizacji nie może być pusty")
//            @Positive(message = "Identyfikator lokalizacji musi być liczbą dodatnią")
//            Long id) {
//
//        return locationRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Lokalizacja"));
//    }
//
//    @Override
//    @Transactional
//    public LocationResponse saveLocationEntity(
//            @Valid @NotNull(message = "Lokalizacja nie może być pusta")
//            Location location) {
//
//        Location saved = locationRepository.save(location);
//        return locationMapper.toResponse(saved);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<LocationBasicInfo> getLocationsForSelect() {
//        log.info("Getting locations for select dropdown");
//        return locationRepository.findAllForSelect();
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<LocationBasicInfo> getAllLocationsBasic() {
//        log.info("Getting all locations (basic info)");
//        return locationRepository.findAllBasicInfo();
//    }
//
//    @Override
//    public void validateLocationExists(
//            @Positive(message = "Identyfikator lokalizacji musi być liczbą dodatnią")
//            Long locationId) {
//
//        if (locationId == null) {
//            return; // brak lokalizacji = OK
//        }
//
//        boolean exists = locationRepository.existsById(locationId);
//
//        if (!exists) {
//            throw new IllegalArgumentException("Wybrana lokalizacja nie istnieje");
//        }
//    }
//
//    // ============ PRIVATE HELPER METHODS ============
//
//    private void validateCreateRequest(
//            @NotNull(message = "Żądanie utworzenia lokalizacji nie może być puste")
//            CreateLocationRequest request) {
//
//        if (request.getType() == null) {
//            throw new ValidationException("Typ lokalizacji jest wymagany");
//        }
//
//        if (request.getType() == LocationType.VIRTUAL && request.getVirtualMeetingUrl() != null) {
//            locationRepository.findByVirtualMeetingUrl(request.getVirtualMeetingUrl())
//                    .ifPresent(location -> {
//                        throw new ValidationException("URL spotkania już istnieje");
//                    });
//        }
//
//        if (request.getType() == LocationType.PHYSICAL &&
//                request.getName() != null && request.getAddress() != null) {
//            if (locationRepository.existsByNameAndAddress(request.getName(), request.getAddress())) {
//                throw new ValidationException("Lokalizacja o tej nazwie i adresie już istnieje");
//            }
//        }
//
//        validateCoordinates(request.getLatitude(), request.getLongitude());
//
//        // Walidacja nazwy
//        if (request.getName() != null && request.getName().length() > 200) {
//            throw new ValidationException("Nazwa lokalizacji nie może przekraczać 200 znaków");
//        }
//    }
//
//    private void validateTypeChange(
//            @NotNull(message = "Lokalizacja nie może być pusta")
//            Location location,
//
//            LocationType newType) {
//
//        if (newType == null) {
//            throw new ValidationException("Nowy typ lokalizacji jest wymagany");
//        }
//
//        if (location.getType() != newType && !location.getMeetings().isEmpty()) {
//            throw new BusinessException("Nie można zmienić typu lokalizacji używanej w spotkaniach");
//        }
//    }
//
//    private void validateLocationDeletion(
//            @NotNull(message = "Lokalizacja nie może być pusta")
//            Location location) {
//
//        if (!location.getMeetings().isEmpty()) {
//            throw new BusinessException(
//                    "Nie można usunąć lokalizacji używanej w spotkaniach. " +
//                            "Lokalizacja jest używana w " + location.getMeetings().size() + " spotkaniach."
//            );
//        }
//    }
//
//    private void validateCoordinates(
//            BigDecimal lat,
//            BigDecimal lng) {
//
//        if (lat == null || lng == null) {
//            return;
//        }
//
//        if (lat.compareTo(new BigDecimal("-90")) < 0 || lat.compareTo(new BigDecimal("90")) > 0) {
//            throw new ValidationException("Szerokość geograficzna musi być między -90 a 90");
//        }
//        if (lng.compareTo(new BigDecimal("-180")) < 0 || lng.compareTo(new BigDecimal("180")) > 0) {
//            throw new ValidationException("Długość geograficzna musi być między -180 a 180");
//        }
//    }
//
//    private Double validateAndLimitRadius(
//            @DecimalMin(value = "0.1", message = "Promień wyszukiwania musi być co najmniej 0.1 km")
//            @DecimalMax(value = "1000.0", message = "Promień wyszukiwania nie może przekraczać 1000 km")
//            Double radiusKm) {
//
//        if (radiusKm == null) {
//            return 10.0;
//        }
//
//        if (radiusKm <= 0) {
//            throw new ValidationException("Promień musi być większy niż 0");
//        }
//
//        if (radiusKm > 1000) {
//            log.warn("Promień wyszukiwania ograniczony do 1000 km");
//            return 1000.0;
//        }
//
//        return radiusKm;
//    }
//
//    private void validateVirtualLocationParameters(
//            @NotBlank(message = "Platforma jest wymagana")
//            String platform,
//
//            @NotBlank(message = "ID spotkania jest wymagane")
//            String meetingId) {
//
//        if (platform == null || platform.trim().isEmpty()) {
//            throw new ValidationException("Platforma jest wymagana");
//        }
//        if (meetingId == null || meetingId.trim().isEmpty()) {
//            throw new ValidationException("ID spotkania jest wymagane");
//        }
//    }
//
//    private void performGeocoding(
//            @NotNull(message = "Lokalizacja nie może być pusta")
//            Location location) {
//
//        try {
//            String fullAddress = location.getFullAddress();
//            if (fullAddress == null || fullAddress.trim().isEmpty()) {
//                return;
//            }
//
//            Location geocoded = geocodingService.geocodeAddress(fullAddress);
//
//            if (geocoded != null && geocoded.getLatitude() != null && geocoded.getLongitude() != null) {
//                location.setLatitude(geocoded.getLatitude());
//                location.setLongitude(geocoded.getLongitude());
//
//                // Uzupełnij tylko jeśli nie zostały podane
//                if (location.getCity() == null && geocoded.getCity() != null) {
//                    location.setCity(geocoded.getCity());
//                }
//                if (location.getCountry() == null && geocoded.getCountry() != null) {
//                    location.setCountry(geocoded.getCountry());
//                }
//                if (location.getTimezone() == null && geocoded.getTimezone() != null) {
//                    location.setTimezone(geocoded.getTimezone());
//                }
//
//                log.info("Successfully geocoded address: {} -> {}, {}",
//                        fullAddress, location.getLatitude(), location.getLongitude());
//            }
//        } catch (Exception e) {
//            log.warn("Geocoding failed for address: {}. Error: {}",
//                    location.getAddress(), e.getMessage());
//        }
//    }
//
//    private boolean needsGeocoding(
//            @NotNull(message = "Lokalizacja nie może być pusta")
//            Location location) {
//
//        return location.getType() == LocationType.PHYSICAL &&
//                (location.getLatitude() == null || location.getLongitude() == null) &&
//                location.getAddress() != null && !location.getAddress().trim().isEmpty();
//    }
//
//    private String getDestinationFromLocation(
//            @NotNull(message = "Lokalizacja nie może być pusta")
//            Location location) {
//
//        if (location.hasCoordinates()) {
//            return location.getLatitude() + "," + location.getLongitude();
//        } else if (location.getFullAddress() != null && !location.getFullAddress().isEmpty()) {
//            return encodeUrl(location.getFullAddress());
//        }
//        return null;
//    }
//
//    private String generateLocationNameFromPlatform(
//            @NotBlank(message = "Nazwa platformy nie może być pusta")
//            String platform) {
//
//        String cleanPlatform = platform.trim();
//        if (cleanPlatform.isEmpty()) {
//            return "Virtual Meeting";
//        }
//
//        return cleanPlatform.substring(0, 1).toUpperCase() +
//                cleanPlatform.substring(1).toLowerCase() + " Meeting";
//    }
//
//    private String generatePlatformUrl(
//            @NotBlank(message = "Platforma nie może być pusta")
//            String platform,
//
//            @NotBlank(message = "ID spotkania nie może być puste")
//            String meetingId,
//
//            @Size(max = 50, message = "Kod dostępu nie może przekraczać 50 znaków")
//            String passcode) {
//
//        String cleanPlatform = platform.toUpperCase().trim();
//        String cleanMeetingId = meetingId.trim();
//        String cleanPasscode = passcode != null ? passcode.trim() : "";
//
//        return switch (cleanPlatform) {
//            case "ZOOM" -> {
//                if (!cleanPasscode.isEmpty()) {
//                    yield String.format("https://zoom.us/j/%s?pwd=%s", cleanMeetingId, cleanPasscode);
//                } else {
//                    yield String.format("https://zoom.us/j/%s", cleanMeetingId);
//                }
//            }
//            case "TEAMS" -> String.format("https://teams.microsoft.com/l/meetup-join/%s", cleanMeetingId);
//            case "GOOGLE_MEET", "MEET" -> String.format("https://meet.google.com/%s", cleanMeetingId);
//            case "WEBEX" -> String.format("https://meet.webex.com/%s", cleanMeetingId);
//            default -> {
//                log.warn("Unknown platform: {}, using meeting ID as URL", platform);
//                yield cleanMeetingId;
//            }
//        };
//    }
//
//    private String encodeUrl(
//            @NotBlank(message = "Tekst do zakodowania nie może być pusty")
//            String text) {
//
//        if (text == null || text.trim().isEmpty()) {
//            return "";
//        }
//        return text.trim().replace(" ", "+");
//    }
//
//    /**
//     * Walidacja parametrów paginacji
//     */
//    private void validatePaginationParameters(
//            @Min(value = 0, message = "Numer strony nie może być mniejszy od 0")
//            Integer page,
//
//            @Min(value = 1, message = "Rozmiar strony musi być co najmniej 1")
//            @Max(value = 100, message = "Rozmiar strony nie może przekraczać 100")
//            Integer size) {
//
//        if (page != null && page < 0) {
//            throw new ValidationException("Numer strony nie może być ujemny");
//        }
//
//        if (size != null && (size < 1 || size > 100)) {
//            throw new ValidationException("Rozmiar strony musi być między 1 a 100");
//        }
//    }
//
//    /**
//     * Walidacja adresu URL spotkań wirtualnych
//     */
//    private void validateVirtualMeetingUrl(
//            @Size(max = 1000, message = "URL spotkania nie może przekraczać 1000 znaków")
//            @Pattern(regexp = "^(https?://|zoommtg://|teams://|meet\\.google\\.com/).*$",
//                    message = "URL spotkania musi być prawidłowym adresem URL zaczynającym się od http://, https://, zoommtg://, teams:// lub meet.google.com/")
//            String url) {
//
//        if (url == null || url.trim().isEmpty()) {
//            return;
//        }
//
//        if (url.length() > 1000) {
//            throw new ValidationException("URL spotkania jest zbyt długi");
//        }
//
//        if (!url.matches("^(https?://|zoommtg://|teams://|meet\\.google\\.com/).*$")) {
//            throw new ValidationException("Nieprawidłowy format URL spotkania");
//        }
//    }
//}
//

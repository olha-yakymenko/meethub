package com.meethub.domain.service;

import com.meethub.domain.model.request.CreateLocationRequest;
import com.meethub.domain.model.request.LocationSearchRequest;
import com.meethub.domain.model.request.UpdateLocationRequest;
import com.meethub.domain.model.response.LocationListResponse;
import com.meethub.domain.model.response.LocationResponse;

import java.math.BigDecimal;
import java.util.List;

public interface LocationService {

    // Podstawowe operacje CRUD
    LocationResponse createLocation(CreateLocationRequest request);
    LocationResponse updateLocation(Long id, UpdateLocationRequest request);
    void deleteLocation(Long id);
    LocationResponse getLocation(Long id); // Albo getLocationById(Long id) - wybierz jedną nazwę
    LocationResponse getLocationById(Long id); // Jeśli potrzebujesz obu metod

    // Wyszukiwanie i filtrowanie
    LocationListResponse searchLocations(LocationSearchRequest request);
    List<LocationResponse> findNearbyLocations(BigDecimal lat, BigDecimal lng, Double radiusKm);

    // Walidacja i narzędzia
    boolean validateLocation(Long locationId);
    String generateMapUrl(Long locationId);
    String generateDirectionsUrl(Long locationId, String origin);
    LocationResponse generateVirtualLocation(String platform, String meetingId, String passcode);
}
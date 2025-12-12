// LocationService.java - NIE dziedziczy po JpaRepository!
package com.meethub.domain.service;

import com.meethub.domain.model.entity.Location;
import com.meethub.domain.model.projection.LocationBasicInfo;
import com.meethub.domain.model.request.CreateLocationRequest;
import com.meethub.domain.model.request.LocationSearchRequest;
import com.meethub.domain.model.request.UpdateLocationRequest;
import com.meethub.domain.model.response.LocationListResponse;
import com.meethub.domain.model.response.LocationResponse;

import java.math.BigDecimal;
import java.util.List;

public interface LocationService {

    // ✅ CRUD operations
    LocationResponse createLocation(CreateLocationRequest request);
    LocationResponse updateLocation(Long id, UpdateLocationRequest request);
    void deleteLocation(Long id);
    LocationResponse getLocation(Long id);

    // ✅ Search and filtering
    LocationListResponse searchLocations(LocationSearchRequest request);
    List<LocationResponse> findNearbyLocations(BigDecimal lat, BigDecimal lng, Double radiusKm);

    // ✅ Tools
    String generateMapUrl(Long locationId);
    String generateDirectionsUrl(Long locationId, String origin);
    LocationResponse generateVirtualLocation(String platform, String meetingId, String passcode);

    // ✅ Location retrieval methods
    List<LocationResponse> getAllLocations();
    List<LocationResponse> getAvailableLocations();
    List<LocationResponse> getPhysicalLocations();
    List<LocationResponse> getVirtualLocations();

    // ✅ Entity methods (if needed)
    Location getLocationEntity(Long id);
    LocationResponse saveLocationEntity(Location location);

    List<LocationBasicInfo> getLocationsForSelect();
    List<LocationBasicInfo> getAllLocationsBasic();

    void validateLocationExists(Long locationId);
}
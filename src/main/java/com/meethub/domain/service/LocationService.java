// LocationService.java
package com.meethub.domain.service;

import com.meethub.domain.model.entity.Location;
import com.meethub.domain.model.projection.LocationBasicInfo;
import com.meethub.domain.model.request.CreateLocationRequest;
import com.meethub.domain.model.request.LocationSearchRequest;
import com.meethub.domain.model.request.UpdateLocationRequest;
import com.meethub.domain.model.response.LocationListResponse;
import com.meethub.domain.model.response.LocationResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;


public interface LocationService {

    LocationResponse createLocation(@Valid @NotNull CreateLocationRequest request);
    LocationResponse updateLocation(@NotNull Long id, @Valid @NotNull UpdateLocationRequest request);
    void deleteLocation(@NotNull Long id);
    LocationResponse getLocation(@NotNull Long id);
    LocationListResponse searchLocations(@Valid @NotNull LocationSearchRequest request);

    List<LocationResponse> findNearbyLocations(
            @NotNull BigDecimal lat,
            @NotNull BigDecimal lng,
            Double radiusKm
    );

    String generateMapUrl(@NotNull Long locationId);
    String generateDirectionsUrl(@NotNull Long locationId, String origin);

    LocationResponse generateVirtualLocation(
            @NotNull String platform,
            @NotNull String meetingId,
            String passcode
    );

    // Location retrieval
    List<LocationResponse> getAllLocations();
    List<LocationResponse> getAvailableLocations();
    List<LocationResponse> getPhysicalLocations();
    List<LocationResponse> getVirtualLocations();

    // Entity methods
    Location getLocationEntity(@NotNull Long id);
    LocationResponse saveLocationEntity(@Valid @NotNull Location location);
    List<LocationBasicInfo> getLocationsForSelect();
    List<LocationBasicInfo> getAllLocationsBasic();
    void validateLocationExists(@NotNull Long locationId);
}
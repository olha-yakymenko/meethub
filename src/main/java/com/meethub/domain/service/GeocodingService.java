package com.meethub.domain.service;

import com.meethub.domain.model.entity.Location;

public interface GeocodingService {
    Location geocodeAddress(String address);
    Location reverseGeocode(double latitude, double longitude);
    boolean validateAddress(Location location);
    String getTimezone(double latitude, double longitude);
}